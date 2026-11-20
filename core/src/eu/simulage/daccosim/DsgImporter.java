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

import eu.simulage.daccosim.model.nodes.v2.FMULocal;
import eu.simulage.daccosim.model.nodes.v2.FMUSoul;
import eu.simulage.daccosim.model.nodes.v2.FMUStub;
import eu.simulage.daccosim.view.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.javafmi.modeldescription.v2.ModelDescription;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class DsgImporter {
	private static final Map<Class<?>, NodeImporter> importerMap;

	static {
		importerMap = new HashMap<>();
		importerMap.put(FMULocal.class, (node, importer) -> importFMULocal((FMU) node));
		importerMap.put(FMUStub.class, (node, importer) -> importFMUStub((FMU) node, importer));
		importerMap.put(FMUSoul.class, (node, importer) -> importFMUSoul((FMU) node, importer));
		importerMap.put(Operator.class, (node, importer) -> importOperator((Operator) node));
		importerMap.put(ExternalInput.class, (node, importer) -> importExternalInput((ExternalInput) node));
		importerMap.put(ExternalOutput.class, (node, importer) -> importExternalOutput((ExternalOutput) node));
	}

	private final eu.simulage.daccosim.model.Graph result = new eu.simulage.daccosim.model.Graph();
	private Session session;
	private Graph graph;
	private Connection connection;

	private DsgImporter(Graph graph) {
		this.graph = graph;
	}

	public static eu.simulage.daccosim.model.Graph importFrom(Graph graph) {
		return new DsgImporter(graph).execute();
	}

	private static eu.simulage.daccosim.model.nodes.Operator importOperator(Operator operator) {
		switch (operator.type()) {
			case Adder:
				return importAdder(operator);
			case Offset:
				return importOffset(operator);
			case Multiplier:
				return importMultiplier(operator);
			case Gain:
				return importGain(operator);
			default:
				throw new DaccosimException("Operator type not found: " + operator.type());
		}
	}

	private static eu.simulage.daccosim.model.nodes.Operator importAdder(Operator operator) {
		return new eu.simulage.daccosim.model.nodes.operators.Adder(operator.id(), operator.dataType())
				.inputs(importInputs(operator.inputs()));
	}

	private static eu.simulage.daccosim.model.nodes.Operator importOffset(Operator operator) {
		return new eu.simulage.daccosim.model.nodes.operators.Offset(operator.id(), operator.dataType())
				.outputs(importOutputs(operator.outputs()))
				.value(operator.value())
				.inputs(importInputs(operator.inputs()));

	}

	private static eu.simulage.daccosim.model.nodes.Operator importMultiplier(Operator operator) {
		return new eu.simulage.daccosim.model.nodes.operators.Multiplier(operator.id(), operator.dataType())
				.inputs(importInputs(operator.inputs()));
	}

	private static eu.simulage.daccosim.model.nodes.Operator importGain(Operator operator) {
		return new eu.simulage.daccosim.model.nodes.operators.Gain(operator.id(), operator.dataType())
				.outputs(importOutputs(operator.outputs()))
				.value(operator.value())
				.inputs(importInputs(operator.inputs()));

	}

	private static eu.simulage.daccosim.model.nodes.ExternalInput importExternalInput(ExternalInput node) {
		return new eu.simulage.daccosim.model.nodes.ExternalInput(node.id())
				.outputs(importOutputs(node.outputs()))
				.initialValues(importInitialValues(node.initialValues()));
	}

	private static eu.simulage.daccosim.model.nodes.ExternalOutput importExternalOutput(ExternalOutput node) {
		return new eu.simulage.daccosim.model.nodes.ExternalOutput(node.id())
				.inputs(importInputs(node.inputs()))
				.initialValues(importInitialValues(node.initialValues()));
	}

	private static eu.simulage.daccosim.model.GraphNode importFMULocal(FMU node) {
		File fmuFile = new File(node.path());
		return !fmuFile.exists() || !DaccosimUtils.isHybridCoSim((ModelDescription) DaccosimUtils.getModelDescription(fmuFile)) ?
				new FMULocal(node.id(), node.path())
						.inputs(importInputs(node.inputs()))
						.outputs(importOutputs(node.outputs()))
						.variables(importVariables(node.variables()))
						.beforeInitValues(importInitialValues(node.beforeInitValues()))
						.inInitValues(importInitialValues(node.inInitValues()))
						.flowVariables(node.flowVariables())
						.static_(node.static_())
				: new eu.simulage.daccosim.model.nodes.v3.FMULocal(node.id(), node.path()).inputs(importInputs(node.inputs()))
				.outputs(importOutputs(node.outputs()))
				.variables(importVariables(node.variables()))
				.beforeInitValues(importInitialValues(node.beforeInitValues()))
				.inInitValues(importInitialValues(node.inInitValues()))
				.flowVariables(node.flowVariables())
				.static_(node.static_());
	}

	private static FMUStub importFMUStub(FMU node, DsgImporter importer) {
		return new FMUStub(node.id(), importer.session)
				.inputs(importInputs(node.inputs()))
				.outputs(importOutputs(node.outputs()))
				.variables(importVariables(node.variables()));
	}

	private static FMUSoul importFMUSoul(FMU node, DsgImporter importer) {
		FMUSoul soul = new FMUSoul(node.id(), node.path(), importer.session)
				.inputs(importInputs(node.inputs()))
				.outputs(importOutputs(node.outputs()))
				.variables(importVariables(node.variables()));
		soul.beforeInitValues(importInitialValues(node.beforeInitValues()));
		soul.inInitValues(importInitialValues(node.inInitValues()));
		return soul;
	}

	private static eu.simulage.daccosim.model.Input[] importInputs(List<Input> inputs) {
		return inputs.stream().map(DsgImporter::importInput).toArray(eu.simulage.daccosim.model.Input[]::new);
	}

	private static eu.simulage.daccosim.model.Output[] importOutputs(List<Output> outputs) {
		return outputs.stream().map(DsgImporter::importOutput).toArray(eu.simulage.daccosim.model.Output[]::new);
	}

	private static eu.simulage.daccosim.model.Variable[] importVariables(List<Variable> variables) {
		return variables.stream().map(DsgImporter::importVariable).toArray(eu.simulage.daccosim.model.Variable[]::new);
	}

	private static eu.simulage.daccosim.model.Input importInput(Input input) {
		eu.simulage.daccosim.model.Input newInput = new eu.simulage.daccosim.model.Input(input.id(), input.type());
		newInput.value(input.value());
		newInput.variability(input.variability());
		return newInput;
	}

	private static eu.simulage.daccosim.model.Output importOutput(Output output) {
		eu.simulage.daccosim.model.Output newOutput = new eu.simulage.daccosim.model.Output(output.id(), output.type());
		newOutput.value(output.value());
		newOutput.variability(output.variability());
		return newOutput;
	}

	private static eu.simulage.daccosim.model.Variable importVariable(Variable variable) {
		eu.simulage.daccosim.model.Variable newVariable = new eu.simulage.daccosim.model.Variable(variable.id(), variable.type());
		newVariable.variability(variable.variability());
		return newVariable;
	}

	private static List<eu.simulage.daccosim.model.InitialValue> importInitialValues(List<InitialValue> initialValues) {
		return initialValues.stream().map(i -> new eu.simulage.daccosim.model.InitialValue(i.name(), i.value())).collect(toList());
	}

	private eu.simulage.daccosim.model.Graph execute() {
		result.settings(importSettings(graph.settings()));
		result.nodes(importNodes(graph.nodes()));
		result.arrows(importArrows(graph.arrows()));
		result.export(importExport(graph.export()));
		if (connection != null) try {
			connection.start();
		} catch (JMSException e) {
			e.printStackTrace();
		}
		return result;
	}

	private eu.simulage.daccosim.model.Graph.Settings importSettings(Graph.Settings settings) {
		return new eu.simulage.daccosim.model.Graph.Settings(settings.startTime(), settings.stopTime(), settings.pipeline(), importCoInitialization(settings.coInitialization()), importStepper(settings.stepper()), importJmsDataConnection(settings.jmsDataConnection()));
	}

	private eu.simulage.daccosim.model.Graph.Settings.CoInitialization importCoInitialization(Graph.Settings.CoInitialization coInit) {
		return coInit != null ?
				new eu.simulage.daccosim.model.Graph.Settings.CoInitialization(coInit.method(), coInit.residualsTolerance(), coInit.maxIterations())
				: null;
	}


	private eu.simulage.daccosim.model.Graph.Settings.Stepper importStepper(Graph.Settings.Stepper stepper) {
		return new eu.simulage.daccosim.model.Graph.Settings.Stepper(stepper.method(), stepper.order(), stepper.stepSize(), stepper.safetyFactor(), stepper.stepperVariables(), stepper.transmitDerivatives());
	}

	private eu.simulage.daccosim.model.Graph.Settings.JmsDataConnection importJmsDataConnection(Graph.Settings.JmsDataConnection jmsDataConnection) {
		try {
			if (jmsDataConnection == null) return null;
			ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(jmsDataConnection.user(), jmsDataConnection.password(), jmsDataConnection.url());
			factory.setUseAsyncSend(true);
			factory.setOptimizeAcknowledge(true);
			factory.setAlwaysSessionAsync(false);
			connection = factory.createConnection();
			connection.start();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			return new eu.simulage.daccosim.model.Graph.Settings.JmsDataConnection(jmsDataConnection.url(), jmsDataConnection.user(), jmsDataConnection.password(), jmsDataConnection.isCoordinator());
		} catch (Exception e) {
			throw new DaccosimException(e.getMessage());
		}
	}

	private eu.simulage.daccosim.model.GraphNode[] importNodes(List<GraphNode> nodes) {
		return nodes.stream().map(this::importNode).toArray(eu.simulage.daccosim.model.GraphNode[]::new);
	}

	private eu.simulage.daccosim.model.Arrow[] importArrows(List<Arrow> arrows) {
		return arrows.stream().map(this::importArrow).toArray(eu.simulage.daccosim.model.Arrow[]::new);
	}

	private eu.simulage.daccosim.model.GraphNode importNode(GraphNode node) {
		NodeImporter importer = nodeImporterOf(node);
		if (importer != null) return importer.importNode(node, this);
		throw new DaccosimException("Node class in definition is not compatible: " + node.getClass().getName());
	}

	private NodeImporter nodeImporterOf(GraphNode node) {
		return !(node instanceof FMU) ? importerMap.get(node.getClass()) :
				graph.settings().jmsDataConnection() == null ? importerMap.get(FMULocal.class) :
						graph.settings().jmsDataConnection().isCoordinator() ? importerMap.get(FMUStub.class) :
								importerMap.get(FMUSoul.class);
	}

	private eu.simulage.daccosim.model.Arrow importArrow(Arrow arrow) {
		return new eu.simulage.daccosim.model.Arrow(arrow.from(), arrow.to());
	}

	private eu.simulage.daccosim.model.Export importExport(Export export) {
		return new eu.simulage.daccosim.model.Export(export.cellSeparator(), export.decimalSeparator(), export.variables())
				.folder(export.folder()).prefix(export.prefix()).interpolationInterval(export.interpolationInterval());
	}

	interface NodeImporter {
		eu.simulage.daccosim.model.GraphNode importNode(GraphNode node, DsgImporter importer);
	}
}
