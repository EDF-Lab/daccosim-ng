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

package eu.simulage.daccosim.steppers;

import eu.simulage.daccosim.Stepper;
import eu.simulage.daccosim.model.Graph;
import eu.simulage.daccosim.model.GraphNode;
import org.javafmi.proxy.FmiProxyV3;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static eu.simulage.daccosim.GraphExecutor.logger;
import static java.util.stream.Collectors.toList;

public abstract class HybridDynamicStepper extends Stepper {

	HybridDynamicStepper(Graph graph) {
		super(graph);
	}

	@Override
	public void init() {
		super.init();
		stepSize = (graph.settings().stopTime() - graph.settings().startTime()) / 1000;
	}

	@Override
	public double firstStep() {
		return doStep();
	}

	protected StepConfig step(List<GraphNode> stage, double stepSize) {
		List<FmiProxyV3.HybridDoStepStatus> events = hybridDoStep(stage, stepSize);
		double suggested = time > 0 ? suggestedStep(stage, stepSize) : stepSize;
		if (thereAreEvents(events) && suggestedStepOf(events) < suggested) {
			double eventTime = events.stream().filter(Objects::nonNull)
					.filter(FmiProxyV3.HybridDoStepStatus::isOccurredEvent)
					.mapToDouble(FmiProxyV3.HybridDoStepStatus::getEndTime).min().orElse(-1);
			return new StepConfig(false, eventTime - time, IntStream.range(0, stage.size()).boxed().filter(i -> events.get(i) != null && events.get(i).getEndTime() == eventTime).map(stage::get).collect(toList()));
		} else if (suggested < stepSize) {
			dynamicSteppingRollbacks++;
			return new StepConfig(false, suggested, Collections.emptyList());
		}
		transmitOutputs();
		return new StepConfig(true, stepSize);
	}

	private double suggestedStepOf(List<FmiProxyV3.HybridDoStepStatus> events) {
		return events.stream().filter(Objects::nonNull)
				.filter(FmiProxyV3.HybridDoStepStatus::isOccurredEvent)
				.mapToDouble(FmiProxyV3.HybridDoStepStatus::getEndTime).min().orElse(-1) - time;
	}

	@Override
	public double step() {
		transmitOutputs();
		return doStep();
	}

	double maxStep() {
		return graph.settings().stopTime() - time();
	}

	@Override
	public void logExecution() {
		if (rollbacksCount() > 0) {
			logger().info("Rollbacks: " + rollbackCount +
					". Dynamic stepping rollbacks: " + dynamicSteppingRollbacks +
					". Hybrid stepping rollbacks: " + (rollbackCount - dynamicSteppingRollbacks));
		}
	}

}
