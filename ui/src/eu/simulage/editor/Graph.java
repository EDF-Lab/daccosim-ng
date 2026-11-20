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

package eu.simulage.editor;

import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxConnectionConstraint;
import com.mxgraph.view.mxGraph;
import eu.simulage.daccosim.DaccosimUtils;
import eu.simulage.editor.specs.*;
import org.javafmi.modeldescription.ModelDescription;

import java.util.List;
import java.util.Map;

import static eu.simulage.editor.EditorUtils.arrowsWith;
import static java.lang.Math.min;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

class Graph extends mxGraph {

	private static final int TOOLTIP_SIZE = 20;

	public Graph() {
		setResetViewOnRootChange(false);
		setDropEnabled(false);
		setCellsEditable(false);
	}

	@Override
	public String getToolTipForCell(Object cell) {
		Spec spec = (Spec) ((mxCell) cell).getValue();
		if (spec instanceof FmuSpec) return fmuToolTip(((FmuSpec) spec));
		if (spec instanceof ArrowSpec) return arrowToolTip(((ArrowSpec) spec));
		if (spec instanceof ExternalInputSpec) return externalInputToolTip(((ExternalInputSpec) spec));
		if (spec instanceof ExternalOutputSpec) return externalOutputToolTip(((ExternalOutputSpec) spec));
		if (spec instanceof OperatorSpec) return operatorTooltip(((OperatorSpec) spec));
		return null;
	}

	@SuppressWarnings("SpellCheckingInspection")
	private String fmuToolTip(FmuSpec spec) {
		if (spec.fileName().isEmpty() || !spec.file().exists()) return "Choose a valid FMU for this block";
		ModelDescription modelDescription = DaccosimUtils.getModelDescription(spec.file());
		if (modelDescription == null) return "FMU couldn't be read. Check log for more details";
		Map<String, Integer> varMap = stream(modelDescription.getModelVariables())
				.filter(s -> !s.getName().equals(DaccosimUtils.SysVarResetFromMaster))
				.collect(toMap(s -> (s.getCausality() + s.getTypeName()).toLowerCase(), s -> 1, (s1, s2) -> s1 + s2));
		StringBuilder builder = new StringBuilder();
		builder.append("<tr><th width='60px'></th><th width='30px'>Input</th><th width='30px'>Output</th><th width='30px'>Param</th><th width='30px'>Local</th>");
		printRow("Real", varMap, builder);
		printRow("Integer", varMap, builder);
		printRow("Boolean", varMap, builder);
		printRow("String", varMap, builder);
		printRow("Enumeration", varMap, builder);
		return "<html><table cellpadding='0' cellspacing='0' cellborder='0'><col align='left'><col align='right'>" + builder.toString() + "</table></html>";
	}

	@SuppressWarnings("SpellCheckingInspection")
	private void printRow(String type, Map<String, Integer> varMap, StringBuilder builder) {
		builder.append("<tr><td><b>").append(type).append("</b></td><td align='center'>");
		type = type.toLowerCase();
		builder.append(varMap.getOrDefault("input" + type, 0)).append("</td><td align='center'>");
		builder.append(varMap.getOrDefault("output" + type, 0)).append("</td><td align='center'>");
		builder.append(varMap.getOrDefault("parameter" + type, 0) + varMap.getOrDefault("calculatedparameter" + type, 0)).append("</td><td align='center'>");
		builder.append(varMap.getOrDefault("local" + type, 0)).append("</td></tr>");
	}

	private String arrowToolTip(ArrowSpec spec) {
		if (spec.getConnections().isEmpty()) return "No connections defined";
		StringBuilder builder = new StringBuilder();
		spec.getConnections().subList(0, min(TOOLTIP_SIZE, spec.getConnections().size()))
				.forEach(c -> builder.append("<br>").append(c.getFrom()).append(" ->").append(c.getTo()));
		if (spec.getConnections().size() > 5) builder.append("<br>...");
		return "<html>" + builder.toString().substring(4) + "</html>";
	}

	private String externalInputToolTip(ExternalInputSpec spec) {
		StringBuilder builder = new StringBuilder("outputs: ");
		List<String> outputs = spec.getOutputs().stream().map(VariableSpec::getName).collect(toList());
		builder.append(String.join(", ", outputs.subList(0, min(5, spec.getOutputs().size()))));
		if (outputs.size() > 5) builder.append("...");
		return "<html>" + builder.toString() + "</html>";
	}

	private String externalOutputToolTip(ExternalOutputSpec spec) {
		StringBuilder builder = new StringBuilder("input: ");
		List<String> inputs = spec.getInputs().stream().map(VariableSpec::getName).collect(toList());
		builder.append(String.join(", ", inputs.subList(0, min(5, inputs.size()))));
		if (inputs.size() > 5) builder.append("...");
		return "<html>" + builder.toString() + "</html>";
	}

	private String operatorTooltip(OperatorSpec spec) {
		return spec.getLabel();
	}

	@Override
	public Object[] cloneCells(Object[] cells, boolean allowInvalidEdges) {
		Object[] objects = super.cloneCells(cells, allowInvalidEdges);
		stream(objects).map(o -> (mxCell) o).forEach(c -> {
			if (c.getValue() instanceof FmuSpec) c.setValue(new FmuSpec((FmuSpec) c.getValue()));
			if (c.getValue() instanceof AdderSpec) c.setValue(new AdderSpec((AdderSpec) c.getValue()));
			if (c.getValue() instanceof OffsetSpec) c.setValue(new OffsetSpec((OffsetSpec) c.getValue()));
			if (c.getValue() instanceof MultiplierSpec) c.setValue(new MultiplierSpec((MultiplierSpec) c.getValue()));
			if (c.getValue() instanceof GainSpec) c.setValue(new GainSpec((GainSpec) c.getValue()));
			if (c.getValue() instanceof ExternalInputSpec)
				c.setValue(new ExternalInputSpec((ExternalInputSpec) c.getValue()));
			if (c.getValue() instanceof ExternalOutputSpec)
				c.setValue(new ExternalOutputSpec((ExternalOutputSpec) c.getValue()));
			if (c.getValue() instanceof ArrowSpec) c.setValue(new ArrowSpec((ArrowSpec) c.getValue()));
		});
		return objects;
	}

	public Object createEdge(Object parent, String id, Object value, Object source, Object target, String style) {
		mxCell cell = (mxCell) super.createEdge(parent, id, value, source, target, "link");
		cell.setValue(new ArrowSpec());
		return cell;
	}

	@Override
	public void cellsAdded(Object[] cells, Object parent, Integer index, Object source, Object target, boolean absolute, boolean constrain) {
		super.cellsAdded(cells, parent, index, source, target, absolute, constrain);
		if (((mxCell) cells[0]).isEdge() && (source == null || target == null))
			removeEdge(cells[0]);
	}

	@Override
	public boolean isTerminalPointMovable(Object cell, boolean source) {
		return true;
	}

	@Override
	public boolean isCellMovable(Object cell) {
		return (!((mxCell) cell).isEdge() || getSelectionCount() != 1) && super.isCellMovable(cell);
	}


	@SuppressWarnings("ConstantConditions")
	@Override
	public void setConnectionConstraint(Object edge, Object terminal, boolean source, mxConnectionConstraint constraint) {
		super.setConnectionConstraint(edge, terminal, source, constraint);
		if (source && (terminal == null || ((mxCell) terminal).getValue() instanceof ExternalOutputSpec)) {
			removeEdge(edge);
			return;
		}
		if (arrowIsAlreadyDefined((mxCell) edge, (mxCell) terminal, source)) {
			removeEdge(edge);
			return;
		}
		if (arrowIsAlreadyDefined((mxCell) edge, (mxCell) terminal, source)) {
			removeEdge(edge);
			return;
		}
		if (!source && (terminal == null || ArrowSpec.availableInputs((Spec) ((mxCell) terminal).getValue()).isEmpty())) {
			removeEdge(edge);
			return;
		}
		if (source && (terminal == null || ((BlockSpec) ((mxCell) terminal).getValue()).getOutputs().isEmpty())) {
			removeEdge(edge);
			return;
		}
		((ArrowSpec) ((mxCell) edge).getValue()).getConnections().clear();
	}

	private void removeEdge(Object edge) {
		getModel().beginUpdate();
		removeCells(new Object[]{edge});
		getModel().endUpdate();
	}

	private boolean arrowIsAlreadyDefined(mxCell edge, mxCell terminal, boolean source) {
		Spec sourceSpec = source ?
				terminal != null ? (Spec) terminal.getValue() : null :
				((ArrowSpec) edge.getValue()).source();
		Spec targetSpec = !source ? terminal != null ?
				(Spec) terminal.getValue() : null :
				((ArrowSpec) edge.getValue()).target();
		return sourceSpec != null && targetSpec != null && arrowsWith(sourceSpec, targetSpec).size() > 0;
	}

}
