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

package eu.simulage;

import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;
import eu.simulage.daccosim.Stepper;
import eu.simulage.daccosim.view.FMU;
import eu.simulage.daccosim.view.*;
import eu.simulage.editor.Editor;
import eu.simulage.editor.specs.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static eu.simulage.daccosim.Stepper.Method.*;
import static eu.simulage.daccosim.view.Helper.*;
import static eu.simulage.editor.EditorUtils.valueOf;
import static eu.simulage.editor.EditorUtils.*;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class MxGraphToDsg {
	private final eu.simulage.daccosim.view.Graph graph;
	private final Editor editor;
	private final mxGraph mxGraph;

	private MxGraphToDsg(Editor editor, mxGraph mxGraph) {
		this.editor = editor;
		this.mxGraph = mxGraph;
		this.graph = new eu.simulage.daccosim.view.Graph();
	}

	public static eu.simulage.daccosim.view.Graph toDaccosimGraph(Editor editor) {
		return new MxGraphToDsg(editor, editor.getGraphComponent().getGraph()).execute();
	}

	private eu.simulage.daccosim.view.Graph execute() {
		return addSettings(graph.nodes(createNodes()).arrows(createArrows()));
	}

	private Graph addSettings(Graph graph) {
		graph.settings()
				.startTime(parseDouble(configuration().startTime()))
				.stopTime(parseDouble(configuration().stopTime()))
				.pipeline(configuration().pipeline());
		graph.settings().coInitialize(configuration().coInitMethod(), parseDouble(configuration().tolerance()), parseInt(configuration().maxIter()));
		graph.settings().stepper(methodOf(configuration().stepperMethod()))
				.stepSize(parseDouble(configuration().stepSize()))
				.safetyFactor(parseDouble(configuration().safetyFactor()))
				.order(parseInt(configuration().order()))
				.stepperVariables(configuration().stepperVariables())
				.transmitDerivatives(configuration().transmitDerivatives());
		if (!configuration().export() || configuration().variablesToExport().isEmpty()) return graph;
		graph.export(configuration().variablesToExport().toArray(new String[0]));
		graph.export().decimalSeparator(configuration().decimalSeparator())
				.cellSeparator(configuration().cellSeparator())
				.folder(".")
				.prefix(prefix())
				.interpolationInterval(configuration().interpolationOption().equals("None") ? 0 :
						configuration().interpolationOption().equals("Interval") ? parseDouble(configuration().interpolationData()) :
								(graph.settings().stopTime() - graph.settings().startTime()) / parseDouble(configuration().interpolationData()));
		return graph;
	}


	private String prefix() {
		return editor.getFile().getName().replace(".simx", "");
	}

	private Stepper.Method methodOf(String methodName) {
		return methodName.equals(CONSTANT) ? ConstantStep :
						methodName.equals(EULER) ? Euler : AdamsBashforth;
	}

	private GraphNode[] createNodes() {
		return stream(mxGraph.getChildVertices(mxGraph.getDefaultParent()))
				.map(v -> (mxCell) v)
				.filter(mxCell::isVertex)
				.map(this::createNode)
				.toArray(GraphNode[]::new);
	}

	private Arrow[] createArrows() {
		return stream(mxGraph.getChildEdges(mxGraph.getDefaultParent()))
				.map(v -> (mxCell) v)
				.filter(mxCell::isEdge)
				.map(this::createArrows)
				.flatMap(Collection::stream)
				.toArray(Arrow[]::new);
	}

	private GraphNode createNode(mxCell vertex) {
		if (vertex.getValue() instanceof FmuSpec) return fmuFrom((FmuSpec) vertex.getValue());
		if (vertex.getValue() instanceof AdderSpec) return adderFrom((AdderSpec) vertex.getValue());
		if (vertex.getValue() instanceof OffsetSpec) return offsetFrom((OffsetSpec) vertex.getValue());
		if (vertex.getValue() instanceof MultiplierSpec) return multiplierFrom((MultiplierSpec) vertex.getValue());
		if (vertex.getValue() instanceof GainSpec) return gainFrom((GainSpec) vertex.getValue());
		if (vertex.getValue() instanceof ExternalInputSpec)
			return externalInputFrom((ExternalInputSpec) vertex.getValue());
		if (vertex.getValue() instanceof ExternalOutputSpec)
			return externalOutputFrom((ExternalOutputSpec) vertex.getValue());
		System.out.println("Unknown kind of vertex");
		return null;
	}

	private GraphNode adderFrom(AdderSpec spec) {
		return adderOperator(spec.getLabel())
				.dataType(spec.getType())
				.inputs(spec.getInputs().stream().map(i -> input(i.getName(), i.dataType(), i.getVariability())).toArray(Input[]::new))
				.outputs(spec.getOutputs().stream().map(i -> output(i.getName(), i.dataType(), i.getVariability())).toArray(Output[]::new));
	}

	private GraphNode offsetFrom(OffsetSpec spec) {
		return offsetOperator(spec.getLabel(), valueOf(spec.getType(), spec.getValue()))
				.dataType(spec.getType())
				.inputs(spec.getInputs().stream().map(i -> input(i.getName(), i.dataType(), i.getVariability())).toArray(Input[]::new))
				.outputs(spec.getOutputs().stream().map(i -> output(i.getName(), i.dataType(), i.getVariability())).toArray(Output[]::new));
	}

	private GraphNode multiplierFrom(MultiplierSpec spec) {
		return multiplierOperator(spec.getLabel())
				.dataType(spec.getType())
				.inputs(spec.getInputs().stream().map(i -> input(i.getName(), i.dataType(), i.getVariability())).toArray(Input[]::new))
				.outputs(spec.getOutputs().stream().map(i -> output(i.getName(), i.dataType(), i.getVariability())).toArray(Output[]::new));
	}

	private GraphNode gainFrom(GainSpec spec) {
		return gainOperator(spec.getLabel(), valueOf(spec.getType(), spec.getValue()))
				.dataType(spec.getType())
				.inputs(spec.getInputs().stream().map(i -> input(i.getName(), i.dataType(), i.getVariability())).toArray(Input[]::new))
				.outputs(spec.getOutputs().stream().map(i -> output(i.getName(), i.dataType(), i.getVariability())).toArray(Output[]::new));
	}

	private FMU fmuFrom(FmuSpec value) {
		return Helper.FMU(value.getLabel(), "fmu/" + value.getFilename())
				.outputs(value.formalOutputs().stream().map(o -> output(o.getName(), o.dataType(), o.getVariability())).toArray(Output[]::new))
				.inputs(value.formalInputs().stream().map(i -> input(i.getName(), i.dataType(), i.getVariability())).toArray(Input[]::new))
				.variables(allVariables(value).stream().map(v -> var(v.getName(), v.dataType())).toArray(Variable[]::new))
				.beforeInitValues(value.getBeforeInitValues().stream().map(v -> initialValue(v.getName(), valueOf(v))).toArray(InitialValue[]::new))
				.inInitValues(value.getInInitValues().stream().map(v -> initialValue(v.getName(), valueOf(v))).toArray(InitialValue[]::new))
				.flowVariables(value.getFlowVariables().toArray(new String[0]))
				.static_(value.isStatic_());
	}

	private List<VariableSpec> allVariables(FmuSpec value) {
		Set<String> variables = value.getBeforeInitValues().stream().map(VariableSpec::getName).collect(toSet());
		variables.addAll(value.getInInitValues().stream().map(VariableSpec::getName).collect(toList()));
		variables.addAll(configuration().variablesToExport(value));
		variables.addAll(configuration().stepperVariables(value));
		return value.getVariables().stream().filter(v ->
				variables.contains(v.getName()) || v.getName().startsWith("der(")).collect(toList());
	}

	private GraphNode externalOutputFrom(ExternalOutputSpec spec) {
		return externalOutput(spec.getLabel())
				.inputs(spec.getInputs().stream().map(o -> input(o.getName(), o.dataType(), o.getVariability())).toArray(Input[]::new))
				.initialValues(spec.getInputs().stream().map(v -> initialValue(v.getName(), valueOf(v))).toArray(InitialValue[]::new));
	}

	private GraphNode externalInputFrom(ExternalInputSpec spec) {
		return externalInput(spec.getLabel())
				.outputs(spec.getOutputs().stream().map(o -> output(o.getName(), o.dataType(), o.getVariability(), valueOf(o))).toArray(Output[]::new))
				.initialValues(spec.getOutputs().stream().map(v -> initialValue(v.getName(), valueOf(v))).toArray(InitialValue[]::new));
	}

	private List<Arrow> createArrows(mxCell mxCell) {
		return createArrows((ArrowSpec) mxCell.getValue());
	}

	private List<Arrow> createArrows(ArrowSpec value) {
		return value.formalConnections().stream().map(c -> arrow(c.getFrom(), c.getTo())).collect(toList());
	}

}
