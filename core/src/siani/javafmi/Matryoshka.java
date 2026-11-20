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

package siani.javafmi;

import eu.simulage.daccosim.CoInitializer;
import eu.simulage.daccosim.DataType;
import eu.simulage.daccosim.GraphExecutor;
import eu.simulage.daccosim.model.*;
import eu.simulage.daccosim.model.nodes.ExternalInput;
import eu.simulage.daccosim.model.nodes.ExternalOutput;
import eu.simulage.daccosim.steppers.Constant;
import eu.simulage.daccosim.view.FMU;
import org.javafmi.framework.FmiSimulation;
import org.javafmi.framework.ModelDescription;
import org.javafmi.kernel.JarClassLoader;

import java.io.File;
import java.io.IOException;
import java.lang.Integer;
import java.lang.String;
import java.lang.reflect.Field;
import java.util.*;

import static eu.simulage.daccosim.DaccosimUtils.extract;
import static eu.simulage.daccosim.DataType.Boolean;
import static eu.simulage.daccosim.DataType.Integer;
import static eu.simulage.daccosim.DataType.*;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.javafmi.framework.FmiContainer.Variability.discrete;

public class Matryoshka extends FmiSimulation {

	private Graph graph;
	private List<ModelDescription.ScalarVariable> varsForDerivatives;
	private Map<Integer, String> variablesMap = new HashMap<>();
	private Map<String, Double> derivativesMap = new HashMap<>();
	private Map<DataType, VariableCreator> variableCreator;
	private Map<String, String> initMap;
	private GraphExecutor graphExecutor;
	private double stepSize = 0;
	private boolean hasCoInitialized = false;
	private boolean isStepping = false;
	private boolean isDirty = false;

	public Matryoshka() {
	}

	private HashMap<DataType, VariableCreator> buildVariableCreator() {
		return new HashMap<DataType, VariableCreator>() {{
			put(Integer, (v, c) -> {
				IntegerVariable variable = variable(v.path()).asInteger().causality(c).variability(discrete);
				if (initMap.containsKey(v.path())) variable.start(parseInt(initMap.get(v.path())));
				return variable;
			});
			put(Boolean, (v, c) -> {
				BooleanVariable variable = variable(v.path()).asBoolean().causality(c).variability(discrete);
				if (initMap.containsKey(v.path())) variable.start(parseBoolean(initMap.get(v.path())));
				return variable;
			});
			put(Real, (v, c) -> {
				RealVariable variable = variable(v.path()).asReal().causality(c).variability(Variability.valueOf(v.variability()));
				if (initMap.containsKey(v.path())) variable.start(Double.parseDouble(initMap.get(v.path())));
				return variable;
			});
			put(DataType.String, (v, c) -> {
				StringVariable variable = variable(v.path()).asString().causality(c).variability(discrete);
				if (initMap.containsKey(v.path())) variable.start(initMap.get(v.path()));
				return variable;
			});
			put(DataType.Enumeration, (v, c) -> {
				IntegerVariable variable = variable(v.path()).asInteger().causality(c).variability(discrete);
				if (initMap.containsKey(v.path())) variable.start(parseInt(initMap.get(v.path())));
				return variable;
			});
		}};
	}

	private Graph graph() {
		if (variableCreator == null) variableCreator = buildVariableCreator();
		if (initMap == null) initMap = getInitValues();
		return graph == null ? graph = viewGraph().commit() : graph;
	}

	private eu.simulage.daccosim.view.Graph viewGraph() {
		return fixFiles(eu.simulage.daccosim.view.Graph.from(getJson()));
	}

	private String getJson() {
		try {
			return new String(extract(Matryoshka.class.getResourceAsStream("/model.dsg")));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private Map<String, String> getInitValues() {
		try {
			return stream(new String(extract(Matryoshka.class.getResourceAsStream("/.init"))).split("\n"))
					.filter(l -> !l.isEmpty())
					.collect(toMap(l -> l.split(";")[0], l -> l.split(";")[1]));
		} catch (IOException e) {
			return Collections.emptyMap();
		}
	}

	@Override
	public Model define() {
		Model model = model(Matryoshka.class.getSimpleName());
		model.description("Daccosim NG Matryoshka FMU");
		graph().nodes().forEach(n -> {
			if (n instanceof ExternalInput) n.outputs().forEach(o -> addInput(model, o));
			else if (n instanceof ExternalOutput) n.inputs().forEach(i -> addOutput(model, i));
			else allVariables(n).forEach(v -> addVariable(model, v));
		});
		model.canGetAndSetFMUstate(false);
		model.canSerializeFMUstate(false);
		model.providesDirectionalDerivative(false);
		model.canBeInstantiatedOnlyOncePerProcess(true);
		model.canHandleVariableCommunicationStepSize(true);
		model.canInterpolateInputs(true);
		model.maxOutputDerivativeOrder(1);
		return model;
	}

	private void addInput(Model model, Output output) {
		model.add(variableCreator.get(output.type()).create(output, Causality.input));
	}

	private void addOutput(Model model, Input input) {
		model.add(variableCreator.get(input.type()).create(input, Causality.output));
	}

	private void addVariable(Model model, Variable variable) {
		model.add(variableCreator.get(variable.type()).create(variable, Causality.local));
	}

	private List<Variable> allVariables(GraphNode n) {
		List<Variable> result = new ArrayList<>(n.inputs());
		result.addAll(n.variables());
		result.addAll(n.outputs());
		return result;
	}

	@Override
	public Status init() {
		fixFiles(eu.simulage.daccosim.view.Graph.from(getJson()));
		graph().nodes().forEach(n -> allVariables(n).forEach(this::register));
		graphExecutor = new GraphExecutor(graph(), new Constant(graph()) {
			@Override
			protected double stepSize() {
				return stepSize;
			}
		});
		graphExecutor.isLogInFile(false);
		graphExecutor.load();
		graphExecutor.init();
		hasCoInitialized = true;
		varsForDerivatives = modelDescription().variables().stream()
				.filter(v -> v instanceof ModelDescription.RealVariable)
				.filter(v -> v.causality().equals(Causality.output.name())).collect(toList());
		variablesMap = modelDescription().variables().stream()
				.collect(toMap(ModelDescription.ScalarVariable::valueReference, ModelDescription.ScalarVariable::name));
		return Status.OK;
	}

	private eu.simulage.daccosim.view.Graph fixFiles(eu.simulage.daccosim.view.Graph graph) {
		graph.nodes().stream().filter(n -> n instanceof FMU)
				.forEach(f -> ((FMU) f).path(pathOf(((FMU) f).path())));
		return graph;
	}

	private String pathOf(String path) {
		if (new File(path).exists() || !this.getClass().getClassLoader().getClass().getName().equals(JarClassLoader.class.getName()))
			return path;
		try {
			ClassLoader classLoader = this.getClass().getClassLoader();
			Field field = classLoader.getClass().getDeclaredField("jarFile");
			field.setAccessible(true);
			File file = new File(field.get(classLoader).toString());
			return new File(file.getParentFile(), path).getAbsolutePath();
		} catch (NoSuchFieldException | IllegalAccessException e) {
			logger().error(e.getMessage());
			return path;
		}
	}

	private void register(Variable variable) {
		register(variable.path(), new org.javafmi.framework.Variable() {
			@Override
			public Object getValue() {
				if (isDirty && hasCoInitialized && !isStepping) {
					isDirty = false;
					CoInitializer.coInitialize(graph);
				}
				return variable.value();
			}

			@Override
			public Status setValue(Object o) {
				isDirty = true;
				variable.value(o);
				return Status.OK;
			}
		});
	}

	@Override
	public Status doStep(double stepSize) {
		isStepping = true;
		varsForDerivatives.forEach(v -> derivativesMap.put(v.name(), (double) graph().variable(v.name()).value()));
		this.stepSize = stepSize;
		if (graphExecutor.time() == 0) graphExecutor.firstStep();
		else graphExecutor.step();
		return Status.OK;
	}

	@Override
	public double[] getRealOutputDerivatives(int[] valueReferences, int[] order) {
		double[] result = new double[valueReferences.length];
		for (int i = 0; i < valueReferences.length; i++) {
			String variableName = variablesMap.get(valueReferences[i]);
			double current = (double) graph.variable(variableName).value();
			result[i] = stepSize != 0 ? (current - derivativesMap.getOrDefault(variableName, current)) / stepSize : 0;
		}
		return result;
	}

	@Override
	public Status setRealInputDerivatives(int[] valueReferences, int[] order, double[] values) {
		for (int i = 0; i < valueReferences.length; i++) {
			int finalI = i;
			String variableName = variablesMap.get(valueReferences[finalI]);
			graph().arrows().stream()
					.filter(a -> a.from().path().equals(variableName))
					.filter(a -> a.toNode() instanceof FMUProxy)
					.forEach(a -> ((FMUProxy) a.toNode()).transmitDerivative(a.to().id(), values[finalI]));
		}
		return Status.OK;
	}

	@Override
	public Status reset() {
		return Status.OK;
	}

	@Override
	public Status terminate() {
		graphExecutor.terminate();
		return Status.OK;
	}

	private interface VariableCreator {
		ScalarVariable create(Variable variable, Causality causality);
	}

}
