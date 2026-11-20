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

package eu.simulage.editor.specs;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxICell;
import eu.simulage.editor.EditorUtils;
import eu.simulage.editor.Spec;

import java.util.ArrayList;
import java.util.List;

import static eu.simulage.editor.EditorUtils.*;
import static java.util.stream.Collectors.toList;

public class ArrowSpec implements Spec {

	private static final String ID_PREFIX = "arrow";
	private String label = EditorUtils.newLabel(ID_PREFIX);
	private List<ConnectionSpec> connections = new ArrayList<>();

	@SuppressWarnings("WeakerAccess")
	public ArrowSpec() {
	}

	@SuppressWarnings("CopyConstructorMissesField")
	public ArrowSpec(ArrowSpec toBeCloned) {
		getConnections().addAll(toBeCloned.getConnections().stream()
				.map(c -> new ConnectionSpec(c.getFrom(), c.getTo())).collect(toList()));
	}

	public static List<VariableSpec> availableInputs(Spec value) {
		List<ArrowSpec> arrows = arrowsWithTarget(value);
		return ((BlockSpec) value).getInputs().stream()
				.filter(i -> value instanceof FmuSpec && ((FmuSpec) value).getFlowVariables().contains(i.getName()) ||
						arrows.stream().noneMatch(a -> a.isInputConnected(i.getName())))
				.collect(toList());
	}

	public List<VariableSpec> allInputs() {
		return ((BlockSpec) target()).getInputs();
	}

	@SuppressWarnings("unused")
	public List<ConnectionSpec> getConnections() {
		return connections;
	}

	@SuppressWarnings("unused")
	public void setConnections(List<ConnectionSpec> connections) {
		this.connections = connections;
	}

	private mxCell cell() {
		return cellWithValue(this);
	}

	public boolean isConnected() {
		return cell().getSource() != null && cell().getTarget() != null;
	}

	private String labelOf(mxICell source) {
		return ((Spec) source.getValue()).getLabel();
	}

	public Spec source() {
		return cell().getSource() != null ? (Spec) cell().getSource().getValue() : null;
	}

	public Spec target() {
		return cell().getTarget() != null ? (Spec) cell().getTarget().getValue() : null;
	}

	public List<VariableSpec> allOutputs() {
		Object value = cell().getSource().getValue();
		return ((BlockSpec) value).getOutputs();
	}

	public List<VariableSpec> availableInputs() {
		return availableInputs(target());
	}

	private boolean isInputConnected(String input) {
		return getConnections().stream().anyMatch(c -> c.getTo().equals(input));
	}

	@Override
	public String toString() {
		return "";
	}


	@Override
	public String getLabel() {
		if (EditorUtils.labelCount(this.label) > 1) this.label = newLabel(ID_PREFIX);
		return label;
	}

	@Override
	public void setLabel(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}

	public List<ConnectionSpec> formalConnections() {
		return getConnections().stream()
				.map(c -> new ConnectionSpec(formalFrom(c.getFrom()), formalTo(c.getTo())))
				.collect(toList());
	}

	private String formalFrom(String from) {
		return labelOf(cell().getSource()) + "." + from;
	}

	private String formalTo(String to) {
		return labelOf(cell().getTarget()) + "." + to;
	}

	public void addConnection(String from, String to) {
		getConnections().add(new ConnectionSpec(from, to));
	}

}
