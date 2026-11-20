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

import java.util.ArrayList;
import java.util.List;

import static eu.simulage.editor.EditorUtils.newLabel;
import static java.util.Collections.emptyList;

public class ExternalOutputSpec extends BlockSpec {

	private static final String ID_PREFIX = "eo";
	private String label = newLabel(ID_PREFIX);
	private List<VariableSpec> inputs = new ArrayList<>();

	public ExternalOutputSpec() {
	}

	public ExternalOutputSpec(ExternalOutputSpec toBeCloned) {
		this.label = toBeCloned.label;
		this.inputs.addAll(toBeCloned.inputs);
	}


	@Override
	public String getLabel() {
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

	@Override
	public String toString() {
		return label;
	}

	public List<VariableSpec> getInputs() {
		return inputs;
	}

	@SuppressWarnings("unused")
	public void setInputs(List<VariableSpec> inputs) {
		this.inputs = inputs;
	}

	@Override
	public List<VariableSpec> getOutputs() {
		return emptyList();
	}

	@Override
	public List<VariableSpec> getVariables() {
		return emptyList();
	}

}
