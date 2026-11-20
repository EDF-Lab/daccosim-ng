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

package eu.simulage.daccosim.model.nodes.v2;

import eu.simulage.daccosim.DaccosimException;
import eu.simulage.daccosim.DataType;
import eu.simulage.daccosim.GraphExecutor;
import eu.simulage.daccosim.model.*;
import eu.simulage.daccosim.model.inputs.FMUInput;
import eu.simulage.daccosim.model.outputs.FMUOutput;
import eu.simulage.daccosim.model.variables.FMUVariable;
import org.javafmi.modeldescription.ScalarVariable;
import org.javafmi.modeldescription.v2.Capabilities;
import org.javafmi.modeldescription.v2.ModelDescription;
import org.javafmi.proxy.Status;
import org.javafmi.proxy.v2.State;
import org.javafmi.wrapper.Simulation;
import org.javafmi.wrapper.v2.Access;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.javafmi.modeldescription.v2.Capabilities.ProvidesDirectionalDerivative;
import static org.javafmi.proxy.v2.State.newEmptyState;

public class FMULocal extends GraphNode implements FMUProxy {

	private final String path;
	private final Map<String, Double> derivativesMap = new HashMap<>();
	private final Map<String, Object> variablesMap = new HashMap<>();
	private final Map<String, Object> inputBuffer = new HashMap<>();
	private Simulation simulation;
	private ModelDescription modelDescription;
	private Access access;
	private double currentTime = 0;
	private State state = newEmptyState();
	private double stateTime;
	private boolean inInit = true;
	private List<InitialValue> beforeInitValues;
	private List<InitialValue> inInitValues;
	private List<String> flowVariables = new ArrayList<>();
	private boolean static_;

	public FMULocal(String id, String path) {
		super(id);
		this.path = path;
	}

	String variableType(String id) {
		return simulation.getModelDescription().getModelVariable(id).getTypeName();
	}

	public String path() {
		return path;
	}

	@Override
	public void load() {
		this.simulation = new Simulation(path);
		this.modelDescription = (ModelDescription) simulation.getModelDescription();
		this.access = new Access(simulation);
	}

	@Override
	public void init() {
		inInit = true;
		process("setUpExperiment", access.setUpExperiment(true, modelDescription.getDefaultExperiment().getTolerance(), graph().settings().startTime(), false, graph().settings().stopTime()));
		for (InitialValue beforeInitValue : beforeInitValues) {
			Input input = input(beforeInitValue.name());
			if (input != null) set(input, beforeInitValue.value());
			else set(variable(beforeInitValue.name()), beforeInitValue.value());
		}
		pushInputBuffer();
		process("enterInitializationMode", access.enterInitializationMode());
		pullVariables(asList(inputs(), variables(), outputs()));
		for (ScalarVariable v : simulation.getModelDescription().getModelVariables())
			if (v.getCausality().equals("input") && v.getStart() != null) {
				Input input = input(v.getName());
				if (input == null) throw new DaccosimException(v.getName() + " is defined as a ScalarVariable with start value in the model description. However, the model does not contain this variable registered for the FMU " + id());
				input.value(v.getStart());
				GraphExecutor.logger().fine(input.path() + " = " + v.getStart());
			}
		for (InitialValue b : inInitValues) set(input(b.name()), b.value());
		pushInputBuffer();
		currentTime = graph().settings().startTime();
	}

	private void process(String primitive, Status status) {
		if (status == Status.OK) return;
		GraphExecutor.logger().severe("Primitive " + primitive + " has finished with code \"" + status.toString() + "\" in fmu " + id() + " at " + currentTime);
		if (primitive.equals("doStep")) {
			GraphExecutor.logger().fine("===== Start of inputs involved in failing doStep ======");
			inputs().forEach(i -> GraphExecutor.logger().fine(i.id() + ": " + i.value()));
			GraphExecutor.logger().fine("===== End of inputs involved in failing doStep ======");
		}
	}

	private void set(Variable variable, Object value) {
		if (variable.type() == DataType.Integer && value instanceof Double)
			variable.value(((Double) value).intValue());
		else variable.value(value);
	}

	@Override
	public void step(double stepSize) {
		if (static_) {
			pushInputBuffer();
			// SENDING AN INPUT IS MANDATORY IN CASE MAT FMU ONLY HAVE INPUTs TO RE-INIT
			pullVariables(asList(inputs().isEmpty() ? emptyList() : Collections.singletonList(inputs().get(0)), variables(), outputs()));
			return;
		}
		exitInitializationMode();
		pushInputBuffer();
		process("doStep", access.doStep(currentTime, stepSize));
		currentTime += stepSize;
		derivativesMap.clear();
		pullVariables(asList(variables(), outputs()));
	}

	private void exitInitializationMode() {
		if (inInit) process("exitInitializationMode", access.exitInitializationMode());
		inInit = false;
	}

	@Override
	public void terminate() {
		exitInitializationMode();
		process("terminate", simulation.terminate());
	}

	public FMULocal inputs(Input... inputs) {
		super.inputs(stream(inputs).map(i -> new FMUInput(i.id(), i.type(), this, i.variability())).collect(toList()));
		return this;
	}

	public FMULocal outputs(Output... outputs) {
		super.outputs(stream(outputs).map(o -> new FMUOutput(o.id(), o.type(), this, o.variability())).collect(toList()));
		return this;
	}

	public FMULocal variables(Variable... variable) {
		super.variables(stream(variable).map(v -> new FMUVariable(v.id(), v.type(), this, v.variability())).collect(toList()));
		return this;
	}

	private int valueReference(String varName) {
		try {
			return simulation.getModelDescription().getValueReference(varName);
		} catch (Exception e) {
			return -1;
		}
	}

	@Override
	public Map<String, Double> derivativeOf(List<String> inputs, List<String> outputs) {
		return calculateDerivatives(inputs, outputs);
	}

	private Map<String, Double> calculateDerivatives(List<String> inputs, List<String> outputs) {
		Map<String, Double> derivativesMap = new HashMap<>();
		org.javafmi.wrapper.v2.Access access = new org.javafmi.wrapper.v2.Access(simulation);
		List<String> realOutputs = outputs.stream().filter(o -> outputs().stream().anyMatch(o1 -> o1.id().equals(o))).collect(toList());
		for (String input : inputs) {
			double[] result = new double[realOutputs.size()];
			if (inputs().stream().anyMatch(i -> i.id().equals(input))) {
				Arrays.fill(result, 1.0);
				result = access.getDirectionalDerivative(
						realOutputs.stream().mapToInt(o -> simulation.getModelDescription().getValueReference(o)).toArray(),
						new int[]{simulation.getModelDescription().getValueReference(input)},
						result);
			}
			for (int i = 0; i < realOutputs.size(); i++)
				derivativesMap.put(input + realOutputs.get(i), -result[i]);
		}
		outputs.stream().filter(o -> outputs().stream().noneMatch(o1 -> o1.id().equals(o)))
				.forEach(o -> inputs.forEach(i -> derivativesMap.put(i + o, -0.0)));
		return derivativesMap;
	}

	@Override
	public double derivativeOf(String output) {
		//		if (derivativesMap.containsKey(output)) return derivativesMap.get(output);
//		double value = access.getRealOutputDerivatives(new String[]{output})[0];
//		derivativesMap.put(output, value);
//		return value; // TODO Remove?
		Object value = pull("der(" + output + ")");
		return value != null ? (double) value : Double.NaN;
	}

	@Override
	public void saveState() {
		exitInitializationMode();
		pushInputBuffer();
		inputBuffer.clear();
		state = access.getState(state);
		stateTime = currentTime;
	}

	private void pushInputBuffer() {
		Map<DataType, List<Map.Entry<String, Object>>> inputMap = inputBuffer.entrySet().stream().collect(groupingBy(e -> find(e.getKey()).type()));
		if (inputMap.get(DataType.Real) != null)
			process("setReal", access.setReal(inputMap.get(DataType.Real).stream().mapToInt(e -> valueReference(e.getKey())).toArray(), inputMap.get(DataType.Real).stream().mapToDouble(e -> (double) e.getValue()).toArray()));
		if (inputMap.get(DataType.Integer) != null)
			process("setInteger", access.setInteger(inputMap.get(DataType.Integer).stream().mapToInt(e -> valueReference(e.getKey())).toArray(), inputMap.get(DataType.Integer).stream().mapToInt(e -> (int) e.getValue()).toArray()));
		if (inputMap.get(DataType.Boolean) != null)
			process("setBoolean", access.setBoolean(inputMap.get(DataType.Boolean).stream().mapToInt(e -> valueReference(e.getKey())).toArray(), toBooleanArray(inputMap.get(DataType.Boolean))));
		if (inputMap.get(DataType.String) != null)
			process("setString", access.setString(inputMap.get(DataType.String).stream().mapToInt(e -> valueReference(e.getKey())).toArray(), inputMap.get(DataType.String).stream().map(e -> (String) e.getValue()).toArray(String[]::new)));
		inputBuffer.forEach(variablesMap::put);
		inputBuffer.clear();
	}

	private void pullVariables(List<List<? extends Variable>> list) {
		List<Variable> finalList = new ArrayList<>();
		for (List<? extends Variable> variables : list) finalList.addAll(variables);
		Map<DataType, List<Variable>> variableMap = finalList.stream().collect(groupingBy(Variable::type));
		if (variableMap.get(DataType.Real) != null) {
			double[] reals = access.getReal(variableMap.get(DataType.Real).stream().mapToInt(e -> valueReference(e.id())).toArray());
			for (int i = 0; i < reals.length; i++)
				this.variablesMap.put(variableMap.get(DataType.Real).get(i).id(), reals[i]);
		}
		if (variableMap.get(DataType.Integer) != null) {
			int[] integers = access.getInteger(variableMap.get(DataType.Integer).stream().mapToInt(e -> valueReference(e.id())).toArray());
			for (int i = 0; i < integers.length; i++)
				this.variablesMap.put(variableMap.get(DataType.Integer).get(i).id(), integers[i]);
		}
		if (variableMap.get(DataType.Enumeration) != null) {
			int[] integers = access.getInteger(variableMap.get(DataType.Enumeration).stream().mapToInt(e -> valueReference(e.id())).toArray());
			for (int i = 0; i < integers.length; i++)
				this.variablesMap.put(variableMap.get(DataType.Enumeration).get(i).id(), integers[i]);
		}
		if (variableMap.get(DataType.Boolean) != null) {
			boolean[] booleans = access.getBoolean(variableMap.get(DataType.Boolean).stream().mapToInt(e -> valueReference(e.id())).toArray());
			for (int i = 0; i < booleans.length; i++)
				this.variablesMap.put(variableMap.get(DataType.Boolean).get(i).id(), booleans[i]);
		}
		if (variableMap.get(DataType.String) != null) {
			String[] strings = access.getString(variableMap.get(DataType.String).stream().mapToInt(e -> valueReference(e.id())).toArray());
			for (int i = 0; i < strings.length; i++)
				this.variablesMap.put(variableMap.get(DataType.String).get(i).id(), strings[i]);
		}
	}


	private boolean[] toBooleanArray(List<Map.Entry<String, Object>> entries) {
		boolean[] result = new boolean[entries.size()];
		for (int i = 0; i < result.length; i++) result[i] = (boolean) entries.get(i).getValue();
		return result;
	}

	@Override
	public void restoreState() {
		clearCache();
		process("setState", access.setState(state));
		currentTime = stateTime;
	}

	@Override
	public void reset() {
		simulation.reset();
		init();
		// if reset is called after co-init, reset clock too
	}

	private void clearCache() {
		derivativesMap.clear();
		variablesMap.clear();
		pullVariables(Collections.singletonList(inputs()));
	}

	@Override
	public synchronized void push(String id, Object value) {
		if (variablesMap.containsKey(id) && variablesMap.get(id) == value) return;
//		if (inInit) clearCache();
		inputBuffer.put(id, value);
		variablesMap.put(id, value);
	}

	@Override
	public synchronized Object pull(String id) {
		if (inInit && !inputBuffer.isEmpty()) {
			pushInputBuffer();
			pullVariables(asList(variables(), outputs()));
		}
		if (!variablesMap.containsKey(id)) pullVariables(asList(variables(), outputs()));
		return variablesMap.get(id);
	}

	@Override
	public void transmitDerivative(String input, double value) {
		Status status = access.setRealInputDerivatives(new String[]{input}, new double[]{value});
		if (status != Status.OK)
			GraphExecutor.logger().warning("Set derivatives in" + id() + " failed with code " + status);
	}

	public FMULocal beforeInitValues(List<InitialValue> initialValues) {
		this.beforeInitValues = initialValues;
		return this;
	}

	public FMULocal inInitValues(List<InitialValue> initialValues) {
		this.inInitValues = initialValues;
		return this;
	}

	public Simulation simulation() {
		return simulation;
	}

	public FMULocal flowVariables(List<String> flowVariables) {
		this.flowVariables = flowVariables;
		return this;
	}

	public boolean static_() {
		return static_;
	}

	@Override
	public boolean canProvideDirectionalDerivatives() {
		return ((ModelDescription)simulation.getModelDescription()).check(ProvidesDirectionalDerivative);
	}

	public FMULocal static_(boolean static_) {
		this.static_ = static_;
		return this;
	}

	@Override
	public List<String> flowVariables() {
		return flowVariables;
	}

	@Override
	public boolean allowDerivativeTransmission(String variableName) {
		return (input(variableName) != null && modelDescription.getCoSimulation().capabilities().check(Capabilities.CanInterpolateInputs)) ||
				(output(variableName) != null && modelDescription.getCoSimulation().getMaxOutputDerivativeOrder() >= 1);
	}

	@Override
	public Double transmissionDerivative(String variableName) {
		return access.getRealOutputDerivatives(new String[]{variableName})[0];
	}

}
