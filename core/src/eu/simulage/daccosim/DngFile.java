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

import eu.simulage.daccosim.view.FMU;
import eu.simulage.daccosim.view.*;
import org.javafmi.framework.FmiContainer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.simulage.daccosim.CoInitializer.Method.IterativePropagation;
import static eu.simulage.daccosim.CoInitializer.Method.NewtonRaphson;
import static eu.simulage.daccosim.Stepper.Method.*;
import static eu.simulage.daccosim.view.Helper.*;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.stream.IntStream.range;

public class DngFile {

	private static final String FMU_ERROR_MSG = "FMU definition incorrect. Example: FMU <block_id> <file_path> [static]";
	private static final String EXTERNAL_INPUT_ERROR_MSG = "ExternalInput definition incorrect. Example: ExternalInput <block_id>";
	private static final String EXTERNAL_OUTPUT_ERROR_MSG = "ExternalOutput definition incorrect. Example: ExternalOutput <block_id>";
	private static final String ADDER_ERROR_MSG = "Adder definition incorrect. Example: Adder <block_id> <data_type>";
	private static final String MULTIPLIER_ERROR_MSG = "Multiplier definition incorrect. Example: Multiplier <block_id> <data_type>";
	private static final String OFFSET_ERROR_MSG = "Offset definition incorrect. Example: Offset <block_id> <data_type> <value>";
	private static final String GAIN_ERROR_MSG = "Gain definition incorrect. Example: Gain <block_id> <data_type> <value>";
	private static final String INPUT_ERROR_MSG = "Input definition incorrect. Example: Input <block_id> <var_name> <type> <variability> [flow]";
	private static final String OUTPUT_ERROR_MSG = "Output definition incorrect. Example: Output <block_id> <var_name> <type> <variability> [flow]";
	private static final String VARIABLE_ERROR_MSG = "Variable definition incorrect. Example: Variable <block_id> <var_name> <type> <variability>";
	private static final String CONNECTION_ERROR_MSG = "Connection definition incorrect. Example: Connection <output> <input>";
	private static final String CONNECTION_TYPE_MISMATCH_MSG = "Connection type mismatch. From type is %s. To type is %s.";
	private static final String CONNECTION_VARIABILITY_MISMATCH_MSG = "Connection variability mismatch. From variability is %s. To variability is %s.";
	private static final String INITIAL_VALUE_ERROR_MSG = "Initial value definition incorrect. Example: InitialValue <var_id> <value> [beforeInit|inInit]";
	private static final String INITIAL_VALUE_FMU_ERROR_MSG = "Initial value definition incorrect. FMU variables must have the mode: beforeInit or inInit";
	private static final String LOG_ERROR_MSG = "Log definition incorrect. Example: Log <var_id_1> [var_id_2] [var_id_3]... | Log all";
	private static final String EXPORT_ERROR_MSG = "Export definition incorrect. Example: Export <cell_separator> <decimal_separator> [interpolationInterval]";
	private static final String NEWTON_RAPHSON_ERROR_MSG = "Newton Raphson initializer definition incorrect. Example: NewtonRaphsonInitializer <maxIterations> <tolerance>";
	private static final String ITERATIVE_PROPAGATION_ERROR_MSG = "Iterative Propagation initializer definition incorrect. Example: IterativePropagationInitializer <maxIterations> <tolerance>";
	private static final String SIMULATION_ERROR_MSG = "Simulation definition incorrect. Example: Simulation <startTime> <stopTime>";
	private static final String CONSTANT_STEPPER_ERROR_MSG = "ConstantStepper definition incorrect. Example: ConstantStepper <stepSize> [transmitDerivatives]";
	private static final String EULER_STEPPER_ERROR_MSG = "EulerStepper definition incorrect. Example: EulerStepper <safetyFactor> [transmitDerivatives]";
	private static final String ADAMS_STEPPER_ERROR_MSG = "AdamsStepper definition incorrect. Example: AdamsStepper <safetyFactor> <order> [transmitDerivatives]";
	private static final String STEPPER_VARIABLE_ERROR_MSG = "StepperVariable definition incorrect. Example: StepperVariable <varId> <tolerance>";
	private static final String PIPELINE_ERROR_MSG = "Pipeline definition incorrect. Example: Pipeline Fmu1&Fmu2@Fmu3";
	private static AtomicInteger count = new AtomicInteger();
	private static final Map<String, Command> commands = new HashMap<String, Command>() {{
		put("FMU", (definition, graph) -> {
			if (definition.length < 3 || definition.length > 4) throw new DaccosimException(FMU_ERROR_MSG);
			assertNodeNotExist(graph, definition[1]);
			FMU fmu = FMU(definition[1], definition[2]);
			graph.nodes().add(fmu);
			if(count.incrementAndGet() % 1000 == 0) System.out.println("Nodes loaded: " + count.get());
			if (definition.length == 4) fmu.static_(definition[3].equalsIgnoreCase("static"));
		});
		put("ExternalInput", (definition, graph) -> {
			if (definition.length != 2) throw new DaccosimException(EXTERNAL_INPUT_ERROR_MSG);
			assertNodeNotExist(graph, definition[1]);
			graph.nodes().add(externalInput(definition[1]));
		});
		put("ExternalOutput", (definition, graph) -> {
			if (definition.length != 2) throw new DaccosimException(EXTERNAL_OUTPUT_ERROR_MSG);
			assertNodeNotExist(graph, definition[1]);
			graph.nodes().add(externalOutput(definition[1]));
		});
		put("Adder", (definition, graph) -> {
			if (definition.length != 3) throw new DaccosimException(ADDER_ERROR_MSG);
			assertNodeNotExist(graph, definition[1]);
			graph.nodes().add(adderOperator(definition[1]).dataType(definition[2]));
		});
		put("Multiplier", (definition, graph) -> {
			if (definition.length != 3) throw new DaccosimException(MULTIPLIER_ERROR_MSG);
			assertNodeNotExist(graph, definition[1]);
			graph.nodes().add(multiplierOperator(definition[1]).dataType(definition[2]));
		});
		put("Offset", (definition, graph) -> {
			if (definition.length != 4) throw new DaccosimException(OFFSET_ERROR_MSG);
			assertNodeNotExist(graph, definition[1]);
			graph.nodes().add(offsetOperator(definition[1], valueOf(definition[2], definition[3])).dataType(definition[2]));
		});
		put("Gain", (definition, graph) -> {
			if (definition.length != 4) throw new DaccosimException(GAIN_ERROR_MSG);
			assertNodeNotExist(graph, definition[1]);
			graph.nodes().add(gainOperator(definition[1], valueOf(definition[2], definition[3])).dataType(definition[2]));
		});
		put("Input", (definition, graph) -> {
			if (definition.length < 5 || definition.length > 6) throw new DaccosimException(INPUT_ERROR_MSG);
			GraphNode node = node(graph, definition[1]);
			DataType type = DataType.valueOf(definition[3]);
			assertVariableNotExist(node, definition[2]);
			assertVariableHasCorrectVariability(definition[2], type, definition[4]);
			assertFlowVariableIsCorrect(definition[2], type, definition[4], definition.length == 6);
			node.inputs().add(input(definition[2], type, definition[4]));
			if (definition.length == 6 && node instanceof FMU) ((FMU) node).flowVariables().add(definition[2]);
		});
		put("Output", (definition, graph) -> {
			if (definition.length < 5 || definition.length > 6) throw new DaccosimException(OUTPUT_ERROR_MSG);
			GraphNode node = node(graph, definition[1]);
			DataType type = DataType.valueOf(definition[3]);
			assertVariableNotExist(node, definition[2]);
			assertVariableHasCorrectVariability(definition[2], type, definition[4]);
			assertFlowVariableIsCorrect(definition[2], type, definition[4], definition.length == 6);
			node.outputs().add(output(definition[2], type, definition[4]));
			if (definition.length == 6 && node instanceof FMU) ((FMU) node).flowVariables().add(definition[2]);
		});
		put("Variable", (definition, graph) -> {
			if (definition.length < 5 || definition.length > 6) throw new DaccosimException(VARIABLE_ERROR_MSG);
			GraphNode node = node(graph, definition[1]);
			DataType type = DataType.valueOf(definition[3]);
			assertVariableNotExist(node, definition[2]);
			assertVariableHasCorrectVariability(definition[2], type, definition[4]);
			node.variables().add(var(definition[2], type, definition[4]));
		});
		put("Connection", (definition, graph) -> {
			if (definition.length != 3) throw new DaccosimException(CONNECTION_ERROR_MSG);
			Variable from = variable(node(graph, nodeId(definition[1])), ioId(definition[1]));
			Variable to = variable(node(graph, nodeId(definition[2])), ioId(definition[2]));
			if (from.type() != to.type())
				throw new DaccosimException(String.format(CONNECTION_TYPE_MISMATCH_MSG, from.type(), to.type()));
			if (!checkVariability(from, to))
				throw new DaccosimException(String.format(CONNECTION_VARIABILITY_MISMATCH_MSG, from.variability(), to.variability()));
			graph.arrows().add(arrow(definition[1], definition[2]));
		});
		put("InitialValue", (definition, graph) -> {
			if (definition.length < 3 || definition.length > 4) throw new DaccosimException(INITIAL_VALUE_ERROR_MSG);
			GraphNode node = node(graph, Helper.nodeId(definition[1]));
			Variable variable = variable(node, Helper.ioId(definition[1]));
			if (node instanceof ExternalOutput)
				((ExternalOutput) node).initialValues().add(initialValue(variable.id(), valueOf(variable.type().toString(), definition[2])));
			if (node instanceof ExternalInput)
				((ExternalInput) node).initialValues().add(initialValue(variable.id(), valueOf(variable.type().toString(), definition[2])));
			if (node instanceof FMU) {
				if (definition.length != 4) throw new DaccosimException(INITIAL_VALUE_FMU_ERROR_MSG);
				if (definition[3].equals("beforeInit"))
					((FMU) node).beforeInitValues().add(initialValue(variable.id(), valueOf(variable.type().toString(), definition[2])));
				else if (definition[3].equals("inInit"))
					((FMU) node).inInitValues().add(initialValue(variable.id(), valueOf(variable.type().toString(), definition[2])));
			}
		});
		put("Log", (definition, graph) -> {
			if (definition.length < 1) throw new DaccosimException(LOG_ERROR_MSG);
			List<String> variables = new ArrayList<>(asList(definition).subList(1, definition.length));
			if(variables.get(0).equalsIgnoreCase("all"))
				variables = graph.nodes().stream().flatMap(n -> allVariables(n).stream().map(variable -> n.id() + "." + variable.id())).collect(Collectors.toList());
			else variables.forEach(v -> variable(node(graph, nodeId(v)), ioId(v)));
			if (graph.export() == null) graph.export(new String[0]);
			graph.export().variables().addAll(variables);
		});
		put("Export", (definition, graph) -> {
			if (definition.length < 3 || definition.length > 4) throw new DaccosimException(EXPORT_ERROR_MSG);
			if (graph.export() == null) graph.export(new String[0]);
			graph.export().cellSeparator(definition[1]);
			graph.export().decimalSeparator(definition[2]);
			if (definition.length == 4) graph.export().interpolationInterval(Double.parseDouble(definition[3]));
		});
		put("NewtonRaphsonInitializer", (definition, graph) -> {
			if (definition.length != 3) throw new DaccosimException(NEWTON_RAPHSON_ERROR_MSG);
			graph.settings().coInitialize(NewtonRaphson.name(), parseDouble(definition[2]), parseInt(definition[1]));
		});
		put("IterativePropagationInitializer", (definition, graph) -> {
			if (definition.length != 3)
				throw new DaccosimException(ITERATIVE_PROPAGATION_ERROR_MSG);
			graph.settings().coInitialize(IterativePropagation.name(), parseDouble(definition[2]), parseInt(definition[1]));
		});
		put("Simulation", (definition, graph) -> {
			if (definition.length != 3) throw new DaccosimException(SIMULATION_ERROR_MSG);
			graph.settings().startTime(parseDouble(definition[1])).stopTime(parseDouble(definition[2]));
		});
		put("ConstantStepper", (definition, graph) -> {
			if (definition.length < 2 || definition.length > 3) throw new DaccosimException(CONSTANT_STEPPER_ERROR_MSG);
			graph.settings().stepper(ConstantStep).stepSize(parseDouble(definition[1]))
					.transmitDerivatives(definition.length == 3);
		});
		put("EulerStepper", (definition, graph) -> {
			if (definition.length < 3 || definition.length > 4) throw new DaccosimException(EULER_STEPPER_ERROR_MSG);
			graph.settings().stepper(Euler).safetyFactor(parseDouble(definition[1]))
					.transmitDerivatives(definition.length == 4);
		});
		put("AdamsStepper", (definition, graph) -> {
			if (definition.length < 4 || definition.length > 5) throw new DaccosimException(ADAMS_STEPPER_ERROR_MSG);
			graph.settings().stepper(AdamsBashforth).safetyFactor(parseDouble(definition[1])).order(parseInt(definition[2]))
					.transmitDerivatives(definition.length == 5);
		});
		put("StepperVariable", (definition, graph) -> {
			if (definition.length != 3) throw new DaccosimException(STEPPER_VARIABLE_ERROR_MSG);
			variable(node(graph, nodeId(definition[1])), ioId(definition[1]));
			graph.settings().stepper().stepperVariables().put(definition[1], Double.parseDouble(definition[2]));
		});
		put("Pipeline", (definition, graph) -> {
			if (definition.length != 2) throw new DaccosimException(PIPELINE_ERROR_MSG);
			graph.settings().pipeline(definition[1]);
		});
	}};

	private static boolean checkVariability(Variable output, Variable input) {
		return input.variability().equals(FmiContainer.Variability.continuous.name()) ||
				output.variability().equals(input.variability()) ||
				output.variability().equals(FmiContainer.Variability.constant.name());
	}

	private static void assertVariableHasCorrectVariability(String varName, DataType type, String variability) {
		if (type == DataType.Real) return;
		if (!variability.equals("continuous")) return;
		throw new DaccosimException(varName + " has a variability (" + variability + ") not valid for its type (" + type + ")");
	}

	private static void assertFlowVariableIsCorrect(String varName, DataType type, String variability, boolean flow) {
		if (!flow) return;
		if (type == DataType.Real && variability.equals("continuous")) return;
		throw new DaccosimException(varName + " has a variability (" + variability + ") and a type (" + type + ") not compatible with being flow variable.");
	}

	private static void assertNodeNotExist(Graph graph, String nodeId) {
		if (graph.node(nodeId) == null) return;
		throw new DaccosimException("Node " + nodeId + " is already defined");
	}

	private static void assertVariableNotExist(GraphNode node, String ioId) {
		if (allVariables(node).stream().noneMatch(v -> v.id().equals(ioId))) return;
		throw new DaccosimException("Variable " + node.id() + "." + ioId + " is already defined");
	}

	private static Variable variable(GraphNode node, String varName) {
		return allVariables(node).stream().filter(v -> v.id().equals(varName)).findFirst().orElseThrow(() -> new DaccosimException(node.id() + "." + varName + " variable is not defined or it is used before its definition"));
	}

	private static List<Variable> allVariables(GraphNode node) {
		List<Variable> variables = new ArrayList<>();
		variables.addAll(node.inputs());
		variables.addAll(node.variables());
		variables.addAll(node.outputs());
		return variables;
	}

	private static Object valueOf(String type, String value) {
		try {
			return type.equals("Real") ? parseDouble(value) :
					type.equals("Integer") ? parseInt(value) :
							type.equals("Boolean") ? parseBoolean(value) :
									value;
		} catch (Exception e) {
			throw new DaccosimException("Problem reading value " + value + " as " + type + " type. Exception message: " + e.getMessage());
		}
	}

	private static GraphNode node(Graph graph, String nodeId) {
		GraphNode node = graph.node(nodeId);
		if(node == null)
			throw new DaccosimException(nodeId + " node is not defined or it is used before its definition");
		return node;
	}

	public static Graph importDng(File dngFile) {
		try {
			return importDng(new String(Files.readAllBytes(dngFile.toPath())));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Graph importDng(String text) {
		Graph graph = new Graph();
		importDngFile(asList(text.split("\n")), graph);
		return graph;
	}

	private static void importDngFile(List<String> lines, Graph graph) {
		for (int i = 0; i < lines.size(); i++) {
			if (cleanLine(lines.get(i)).isEmpty()) continue;
			try {
				String[] split = splitLine(lines.get(i));
				command(split).execute(split, graph);
			} catch (Exception e) {
				throw new DaccosimException("Problem in line " + (i + 1) + ". " + e.getMessage());
			}
		}
	}

	private static String[] splitLine(String l) {
		List<String> result = new ArrayList<>();
		Matcher matcher = Pattern.compile("\"([^\"]+)\"|\\s*([^\\s\"]+)\\s*").matcher(cleanLine(l));
		while (matcher.find()) for (int i = 1; i <= matcher.groupCount(); i++) result.add(matcher.group(i));
		return result.stream().filter(Objects::nonNull).toArray(String[]::new);
	}

	public static byte[] exportDng(Graph graph) {
		StringBuilder result = new StringBuilder();
		exportBlocks(graph, result);
		exportConnections(graph, result);
		exportSettings(graph, result);
		return result.toString().getBytes();
	}

	private static void exportBlocks(Graph graph, StringBuilder result) {
		graph.nodes().forEach(n -> {
			if (n instanceof FMU)
				result.append("FMU ").append(n.id()).append(" \"").append(((FMU) n).path()).append("\"").append(((FMU) n).static_() ? " static" : "").append("\n");
			else if (n instanceof ExternalOutput) result.append("ExternalOutput ").append(n.id()).append("\n");
			else if (n instanceof ExternalInput) result.append("ExternalInput ").append(n.id()).append("\n");
			else if (n instanceof Operator) {
				Operator op = (Operator) n;
				switch (op.type()) {
					case Adder:
						result.append("Adder ").append(n.id()).append(" ").append(op.dataType().name()).append("\n");
						break;
					case Multiplier:
						result.append("Multiplier ").append(n.id()).append(" ").append(op.dataType().name()).append("\n");
						break;
					case Offset:
						result.append("Offset ").append(n.id()).append(" ").append(op.dataType().name()).append(" ").append(op.value()).append("\n");
						break;
					case Gain:
						result.append("Gain ").append(n.id()).append(" ").append(op.dataType().name()).append(" ").append(op.value()).append("\n");
						break;
				}
			} else return;
			exportVariables(n, result);
		});
	}

	private static void exportConnections(Graph graph, StringBuilder result) {
		graph.arrows().forEach(a -> result.append("Connection ").append(a.from()).append(" ").append(a.to()).append("\n"));
	}

	private static void exportSettings(Graph graph, StringBuilder result) {
		if (graph.export() != null && !graph.export().variables().isEmpty()) {
			if (graph.export().prefix().isEmpty()) graph.export().prefix("unknown");
			result.append("Export ").append(graph.export().cellSeparator()).append(" ").append(graph.export().decimalSeparator()).append(" ").append(graph.export().interpolationInterval()).append("\n");
			result.append("Log");
			graph.export().variables().forEach(v -> result.append(" ").append(v));
			result.append("\n");
		}
		if (graph.settings().coInitialization() != null) {
			result.append(graph.settings().coInitialization().method().name()).append("Initializer ").append(graph.settings().coInitialization().maxIterations()).append(" ").append(graph.settings().coInitialization().residualsTolerance());
			result.append("\n");
		}
		if (graph.settings().stepper().method() == ConstantStep)
			result.append("ConstantStepper ").append(graph.settings().stepper().stepSize())
					.append(graph.settings().stepper().transmitDerivatives() ? " transmitDerivatives" : "").append("\n");
		if (graph.settings().stepper().method() == Euler)
			result.append("EulerStepper ").append(graph.settings().stepper().safetyFactor())
					.append(graph.settings().stepper().transmitDerivatives() ? " transmitDerivatives" : "").append("\n");
		if (graph.settings().stepper().method() == AdamsBashforth)
			result.append("AdamsStepper ").append(graph.settings().stepper().safetyFactor()).append(" ").append(graph.settings().stepper().order())
					.append(graph.settings().stepper().transmitDerivatives() ? " transmitDerivatives" : "").append("\n");
		graph.settings().stepper().stepperVariables().forEach((k, v) -> result.append("StepperVariable ").append(k).append(" ").append(v).append("\n"));
		result.append("Simulation ").append(graph.settings().startTime()).append(" ").append(graph.settings().stopTime()).append("\n");
		if (!graph.settings().pipeline().isEmpty())
			result.append("Pipeline ").append(graph.settings().pipeline()).append("\n");
	}

	@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
	private static void exportVariables(GraphNode node, StringBuilder builder) {
		node.outputs().forEach(o -> {
			builder.append("Output " + node.id() + " " + o.id() + " " + o.type() + " " + o.variability());
			if (node instanceof FMU && ((FMU) node).flowVariables().contains(o.id())) builder.append(" flow");
			builder.append("\n");
		});
		node.inputs().forEach(i -> {
			builder.append("Input " + node.id() + " " + i.id() + " " + i.type() + " " + i.variability());
			if (node instanceof FMU && ((FMU) node).flowVariables().contains(i.id())) builder.append(" flow");
			builder.append("\n");
		});
		node.variables().forEach(v -> builder.append("Variable " + node.id() + " " + v.id() + " " + v.type() + " " + v.variability() + "\n"));
		if (node instanceof ExternalOutput)
			((ExternalOutput) node).initialValues().forEach(i -> builder.append("InitialValue " + node.id() + "." + i.name() + " " + i.value() + "\n"));
		if (node instanceof ExternalInput)
			((ExternalInput) node).initialValues().forEach(i -> builder.append("InitialValue " + node.id() + "." + i.name() + " " + i.value() + "\n"));
		if (node instanceof FMU) {
			((FMU) node).beforeInitValues().forEach(i -> builder.append("InitialValue " + node.id() + "." + i.name() + " " + i.value() + " beforeInit\n"));
			((FMU) node).inInitValues().forEach(i -> builder.append("InitialValue " + node.id() + "." + i.name() + " " + i.value() + " inInit\n"));
		}
	}

	private static Command command(String[] split) {
		if (!commands.containsKey(split[0])) throw new DaccosimException("Definition " + split[0] + " does not exist");
		return commands.get(split[0]);
	}

	private static String cleanLine(String line) {
		return line.replaceAll("//.*|(?s)/\\*.*?\\*/", "").trim();
	}


	private interface Command {
		void execute(String[] command, Graph graph);
	}
}
