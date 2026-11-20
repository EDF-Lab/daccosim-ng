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
import eu.simulage.daccosim.DataType;
import eu.simulage.daccosim.Stepper;
import eu.simulage.daccosim.view.FMU;
import eu.simulage.daccosim.view.*;
import eu.simulage.editor.Editor;
import eu.simulage.editor.Spec;
import eu.simulage.editor.specs.*;

import java.io.File;
import java.util.List;

import static eu.simulage.daccosim.GraphExecutor.logger;
import static eu.simulage.daccosim.Stepper.Method.*;
import static eu.simulage.daccosim.view.Helper.ioId;
import static eu.simulage.daccosim.view.Helper.nodeId;
import static eu.simulage.daccosim.view.Operator.Type.*;
import static eu.simulage.editor.EditorUtils.*;

public class DsgToMxGraph {
	private final Graph graph;
	private final mxGraph mxGraph;

	private DsgToMxGraph(Editor editor, Graph graph) {
		this.graph = graph;
		this.mxGraph = editor.getGraphComponent().getGraph();
	}

	public static void importInEditor(Editor editor, Graph graph) {
		new DsgToMxGraph(editor, graph).execute();
	}

	private void execute() {
		mxGraph.getModel().beginUpdate();
		clearMxGraph();
		createNodes();
		createArrows();
		createConfiguration();
		mxGraph.getModel().endUpdate();
	}

	private void clearMxGraph() {
		mxCell root = new mxCell();
		root.insert(new mxCell());
		mxGraph.getModel().setRoot(root);
	}

	private void createConfiguration() {
		ConfigurationSpec spec = new ConfigurationSpec();
		spec.startTime(graph.settings().startTime() + "");
		spec.stopTime(graph.settings().stopTime() + "");
		spec.pipeline(graph.settings().pipeline());
		if (graph.settings().coInitialization() != null)
			spec.maxIter(graph.settings().coInitialization().maxIterations() + "");
		if (graph.settings().coInitialization() != null)
			spec.tolerance(graph.settings().coInitialization().residualsTolerance() + "");
		spec.stepperMethod(methodOf(graph.settings().stepper().method()));
		spec.stepSize(graph.settings().stepper().stepSize() + "");
		spec.safetyFactor(graph.settings().stepper().safetyFactor() + "");
		spec.order(graph.settings().stepper().order() + "");
		spec.transmitDerivatives(graph.settings().stepper().transmitDerivatives());
		spec.stepperVariables(graph.settings().stepper().stepperVariables());
		if (graph.export() != null && !graph.export().variables().isEmpty()) {
			spec.variablesToExport(graph.export().variables());
			spec.cellSeparator(graph.export().cellSeparator());
			spec.decimalSeparator(graph.export().decimalSeparator());
			if(graph.export().interpolationInterval() != 0){
				spec.interpolationOption("Interval");
				spec.interpolationData(graph.export().interpolationInterval() + "");
			}
		} else spec.export(false);
		setConfiguration(spec);
	}


	private String methodOf(Stepper.Method method) {
		return method.equals(ConstantStep) ? CONSTANT :
				method.equals(Euler) ? EULER : ADAMS;
	}

	private void createNodes() {
		final int[] x = {10};
		final int[] y = {10};
		graph.nodes().forEach(n -> {
			createNode(n, x[0], y[0]);
			if (x[0] + 250 > getEditor().getGraphComponent().getWidth()) {
				x[0] = 10;
				y[0] += 85;
			} else x[0] += 150;
		});
	}

	private void createArrows() {
		graph.arrows().forEach(this::createArrow);
	}

	private void createNode(GraphNode node, int x, int y) {
		if (node instanceof FMU) fmuFrom((FMU) node, x, y);
		else if (node instanceof Operator && ((Operator) node).type().equals(Adder))
			adderFrom((Operator) node, x, y);
		else if (node instanceof Operator && ((Operator) node).type().equals(Offset))
			offsetFrom((Operator) node, x, y);
		else if (node instanceof Operator && ((Operator) node).type().equals(Multiplier))
			multiplierFrom((Operator) node, x, y);
		else if (node instanceof Operator && ((Operator) node).type().equals(Gain))
			gainFrom((Operator) node, x, y);
		else if (node instanceof ExternalInput)
			externalInputFrom((ExternalInput) node, x, y);
		else if (node instanceof ExternalOutput)
			externalOutputFrom((ExternalOutput) node, x, y);
		else logger().info("Unknown kind of block");
	}

	private void adderFrom(Operator node, int x, int y) {
		AdderSpec spec = new AdderSpec();
		fillOperator(node, spec);
		mxGraph.insertVertex(mxGraph.getDefaultParent(), node.id(), spec, x, y, 80, 80, "adder");
	}

	private void offsetFrom(Operator node, int x, int y) {
		OffsetSpec spec = new OffsetSpec();
		fillOperator(node, spec);
		spec.setValue(node.value().toString());
		mxGraph.insertVertex(mxGraph.getDefaultParent(), node.id(), spec, x, y, 80, 80, "offset");
	}

	private void multiplierFrom(Operator node, int x, int y) {
		MultiplierSpec spec = new MultiplierSpec();
		fillOperator(node, spec);
		mxGraph.insertVertex(mxGraph.getDefaultParent(), node.id(), spec, x, y, 80, 80, "multiplier");
	}

	private void gainFrom(Operator node, int x, int y) {
		GainSpec spec = new GainSpec();
		fillOperator(node, spec);
		spec.setValue(node.value().toString());
		mxGraph.insertVertex(mxGraph.getDefaultParent(), node.id(), spec, x, y, 80, 80, "gain");
	}

	private void fillOperator(Operator node, OperatorSpec spec) {
		spec.setLabel(node.id());
		spec.setType(node.dataType().toString());
		node.inputs().forEach(i -> spec.getInputs().add(new VariableSpec(i.id(), i.type(), i.variability(), i.value().toString())));
		if (spec instanceof GainSpec || spec instanceof OffsetSpec)
			node.outputs().forEach(i -> spec.getOutputs().add(new VariableSpec(i.id(), i.type(), i.variability(), i.value().toString())));
	}

	private void fmuFrom(FMU fmu, int x, int y) {
		FmuSpec spec = new FmuSpec();
		spec.setLabel(fmu.id());
		spec.setFilename(new File(fmu.path()).getName());
		spec.setStatic_(fmu.static_());
		fmu.beforeInitValues().forEach(b -> spec.getBeforeInitValues().add(new VariableSpec(b.name(), typeOf(b.value()), "discrete", b.value().toString())));
		fmu.inInitValues().forEach(b -> spec.getInInitValues().add(new VariableSpec(b.name(), typeOf(b.value()), "discrete", b.value().toString())));
		fmu.flowVariables().forEach(v -> spec.getFlowVariables().add(v));
		mxGraph.insertVertex(mxGraph.getDefaultParent(), fmu.id(), spec, x, y, 120, 80, spec.isMatryoshka() ? "matryoshka" : "fmu");
	}

	private DataType typeOf(Object value) {
		return value instanceof Integer ? DataType.Integer :
				value instanceof Double ? DataType.Real :
						value instanceof Boolean ? DataType.Boolean :
								value instanceof String ? DataType.String : DataType.Enumeration;
	}

	private void externalOutputFrom(ExternalOutput node, int x, int y) {
		ExternalOutputSpec spec = new ExternalOutputSpec();
		spec.setLabel(node.id());
		node.inputs().forEach(i -> spec.getInputs().add(new VariableSpec(i.id(), i.type(), i.variability(), getValue(node, i))));
		mxGraph.insertVertex(mxGraph.getDefaultParent(), node.id(), spec, x, y, 80, 80, "externaloutput");
	}

	private String getValue(ExternalOutput node, Variable var) {
		return getValue(node.initialValues(), var);
	}

	private String getValue(ExternalInput node, Variable var) {
		return getValue(node.initialValues(), var);
	}

	private String getValue(List<InitialValue> initialValues, Variable var) {
		return initialValues.stream()
				.filter(i -> i.name().equals(var.id()))
				.findFirst()
				.map(i -> i.value().toString()).orElse(var.value().toString());
	}

	private void externalInputFrom(ExternalInput node, int x, int y) {
		ExternalInputSpec spec = new ExternalInputSpec();
		spec.setLabel(node.id());
		node.outputs().forEach(o -> spec.getOutputs().add(new VariableSpec(o.id(), o.type(), o.variability(), getValue(node, o))));
		mxGraph.insertVertex(mxGraph.getDefaultParent(), node.id(), spec, x, y, 80, 80, "externalinput");
	}

	private void createArrow(Arrow arrow) {
		arrowSpecOf(cellWithLabel(nodeId(arrow.from())), cellWithLabel(nodeId(arrow.to())))
				.addConnection(ioId(arrow.from()), ioId(arrow.to()));
	}

	private ArrowSpec arrowSpecOf(mxCell source, mxCell target) {
		List<ArrowSpec> arrows = arrowsWith((Spec) source.getValue(), (Spec) target.getValue());
		if (!arrows.isEmpty()) return arrows.get(0);
		ArrowSpec spec = new ArrowSpec();
		return (ArrowSpec) ((mxCell) mxGraph.insertEdge(mxGraph.getDefaultParent(), spec.label(), spec, source, target)).getValue();
	}

}
