/*
 *  Copyright 2013-2018 - Monentia
 *
 *   Daccosim is a collaborative development effort between EDF (France),
 *   CentraleSupélec (France), EIFER Institute (Germany), SIANI institute (Spain)
 *   and Monentia S.L. (Spain)
 *
 *  This File is part of Daccosim Project
 *
 * Daccosim Project is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License.
 *
 *  Daccosim Project is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Daccosim. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.simulage.daccosim;

import eu.simulage.daccosim.model.*;
import eu.simulage.daccosim.steppers.DynamicStepper;
import eu.simulage.daccosim.steppers.HybridDynamicStepper;
import org.javafmi.proxy.FmiProxyV3;
import org.javafmi.proxy.Status;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static eu.simulage.daccosim.DaccosimUtils.areDifferent;
import static eu.simulage.daccosim.GraphExecutor.logger;
import static eu.simulage.daccosim.Iterator.streamOf;
import static java.util.stream.Collectors.toList;

public abstract class Stepper {
	protected List<StepperVariable> stepperVariables = new ArrayList<>();
	protected List<FMUProxy> resetFMUs = new ArrayList<>();
	protected final Map<Variable, Double> previousValues = new ConcurrentHashMap<>();
	protected final Map<Variable, Double> previousDerivatives = new ConcurrentHashMap<>();
	protected List<List<GraphNode>> pipeline = new ArrayList<>();
	protected final Graph graph;
	private final OutputTransmitter transmitter;
	protected double time;
	protected double stepSize;
	protected int dynamicSteppingRollbacks = 0;
	protected boolean processEvent = false;
	protected int rollbackCount = 0;

	protected Stepper(Graph graph) {
		this.graph = graph;
		this.time = graph.settings().startTime();
		this.transmitter = new OutputTransmitter(graph);
	}

	public void init() {
		Pipeline pipeline = new Pipeline(graph.settings().pipeline());
		if (pipeline.stages().isEmpty())
			this.pipeline.add(graph.nodes());
		else
			pipeline.stages().forEach(s -> this.pipeline.add(graph.nodes().stream()
					.filter(n -> n instanceof FMUProxy && s.nodes().contains(n.id())).collect(Collectors.toList())));
		resetFMUs = fmusToReset();
		stepperVariables = graph.settings().stepper().stepperVariables().entrySet().stream()
				.map(v -> {
					Variable variable = graph.variable(v.getKey());
					if (variable == null) return null;
					return new StepperVariable(variable, v.getValue());
				})
				.filter(Objects::nonNull)
				.collect(toList());
	}

	private List<FMUProxy> fmusToReset() {
		return this instanceof DynamicStepper || this instanceof HybridDynamicStepper ?
				graph.nodes().stream().filter(n -> n instanceof FMUProxy && ((FMUProxy) n).pull(DaccosimUtils.SysVarResetFromMaster) != null).map(n -> (FMUProxy) n).collect(toList()) :
				Collections.emptyList();
	}


	public void readValuesAndDerivatives() {
		previousValues.clear();
		previousDerivatives.clear();
		streamOf(stepperVariables).forEach(stepperVariable -> {
			previousValues.put(stepperVariable.variable, (Double) stepperVariable.variable.value());
			previousDerivatives.put(stepperVariable.variable, stepperVariable.variable.derivative());
		});
	}

	protected double doStep() {
		if (processEvent) {
			processEvent();
			processEvent = false;
			return 0;
		}
		readValuesAndDerivatives();
		StepConfig config = new StepConfig(true, stepSize());
		saveState(graph.nodes());
		List<GraphNode> nodesWithEvents = config.nodesWithEvents;
//		int dynamicRollbacks = 0;
		for (int i = 0; i < pipeline.size(); i++) {
			config = step(filterNodes(pipeline.get(i), nodesWithEvents), config.stepSize);
			if (!config.goAhead){ // && dynamicRollbacks == 0) {
				rollbackCount++;
				nodesWithEvents = config.nodesWithEvents;
				for (int j = 0; j <= i; j++) rollback(filterNodes(pipeline.get(j), nodesWithEvents));
				processEvent = !nodesWithEvents.isEmpty();
//				if(config.nodesWithEvents.isEmpty()) dynamicRollbacks++;
				i = -1;
			}
		}
		resetFmus();
		updateTime(config.stepSize);
		this.stepSize = getSuggestedStep(graph.nodes(), config.stepSize);
		return config.stepSize;
	}

	private double getSuggestedStep(List<GraphNode> nodes, double stepSize) {
		double value = suggestedStep(nodes, stepSize);
		return Double.isNaN(value) ? stepSize : value;
	}

	protected void rollback(List<GraphNode> graphNodes) {
		streamOf(graphNodes).filter(n -> n instanceof FMUProxy).forEach(n -> ((FMUProxy) n).restoreState());
	}

	private void resetFmus() {
		streamOf(resetFMUs).forEach(f -> f.push(DaccosimUtils.SysVarResetFromMaster, !((boolean) f.pull(DaccosimUtils.SysVarResetFromMaster))));
	}

	public static List<GraphNode> filterNodes(List<GraphNode> graphNodes, List<GraphNode> exclude) {
		return streamOf(graphNodes).filter(n -> n instanceof FMUProxy).filter(n -> !exclude.contains(n)).collect(toList());
	}

	protected void transmitOutputs() {
		transmitter.execute();
	}

	protected abstract double suggestedStep(List<GraphNode> graphNodes, double stepSize);

	protected abstract StepConfig step(List<GraphNode> nodes, double stepSize);

	protected void processEvent() {
		int count = 0;
		while (thereAreNewStates() > 0) {
			count++;
			transmitOutputs();
		}
		if (count == 0) logger().info("Event raised without new discrete states at " + time);
	}

	protected List<FmiProxyV3.HybridDoStepStatus> hybridDoStep(List<GraphNode> nodes, double stepSize) {
		stepSize = time + stepSize <= graph.settings().stopTime() ? stepSize : graph.settings().stopTime() - time;
		double finalStepSize = stepSize;
		return streamOf(nodes).map(n -> {
			if (n instanceof FMU3Proxy) return ((FMU3Proxy) n).hybridDoStep(finalStepSize);
			n.step(finalStepSize);
			return null;
		}).collect(toList());
	}

	private int thereAreNewStates() {
		return streamOf(graph.nodes())
				.filter(n -> n instanceof FMU3Proxy)
				.mapToInt(n -> {
					FMU3Proxy fmu = ((FMU3Proxy) n);
					FmiProxyV3.EventInfo info = new FmiProxyV3.EventInfo(true, false, false, false, false, 0.0);
					int counter = -1;
					boolean stateChanged = false;
					List<Object> outputs = n.outputs().stream().map(Variable::value).collect(toList());
					while (info.newDiscreteStatesNeeded) {
						Status status = fmu.newDiscreteStates(info);
						if (status == Status.WARNING) break;
						stateChanged = stateChanged || info.nominalsOfContinuousStatesChanged || info.valuesOfContinuousStatesChanged;
						counter++;
					}
					boolean newState = areDifferent(outputs, n.outputs().stream().map(Variable::value).collect(toList()));
					if (counter > 0) {
						logger().info(counter + (counter != 1 ? " events" : " event") + " at " + time + " in " + n.id());
						return counter;
					} else if (stateChanged) {
						logger().info("1 event in " + n.id());
						return 1;
					} else if (newState) {
						logger().info("state changed in " + n.id() + " without being informed. Detected by Daccosim NG");
						return 1;
					}
					return 0;
				}).sum();
	}

	public abstract double firstStep();

	protected void updateTime(double stepSize) {
		time += bound(stepSize);
	}

	protected double bound(double stepSize) {
		return Math.min(stepSize, graph.settings().stopTime() - time);
	}

	public abstract double step();

	public int rollbacksCount() {
		return rollbackCount;
	}

	protected double stepSize() {
		return bound(this.stepSize);
	}

	protected double safetyFactor() {
		return graph.settings().stepper().safetyFactor();
	}

	protected int order() {
		return graph.settings().stepper().order();
	}

	public double time() {
		return Math.min(time, graph.settings().stopTime());
	}

	public void logExecution() {
		if (rollbacksCount() > 0) logger().info("Rollbacks: " + rollbacksCount());
	}

	public void saveState(List<GraphNode> graphNodes) {
		streamOf(graphNodes).filter(n -> n instanceof FMUProxy).forEach(n -> ((FMUProxy) n).saveState());
	}

	protected boolean thereAreEvents(List<FmiProxyV3.HybridDoStepStatus> collect) {
		return collect.stream()
				.filter(Objects::nonNull)
				.anyMatch(FmiProxyV3.HybridDoStepStatus::isOccurredEvent);
	}

	public enum Method {
		ConstantStep, Euler, AdamsBashforth
	}

	public static class StepConfig {
		public final boolean goAhead;
		public final double stepSize;
		public final List<GraphNode> nodesWithEvents;

		public StepConfig(boolean goAhead, double stepSize, List<GraphNode> nodesWithEvents) {
			this.goAhead = goAhead;
			this.stepSize = stepSize;
			this.nodesWithEvents = nodesWithEvents;
		}

		public StepConfig(boolean goAhead, double stepSize) {
			this.goAhead = goAhead;
			this.stepSize = stepSize;
			this.nodesWithEvents = new ArrayList<>();
		}
	}

	public static class StepperVariable {

		public final Variable variable;
		public final double tolerance;

		StepperVariable(Variable variable, double tolerance) {
			this.variable = variable;
			this.tolerance = tolerance;
		}
	}
}
