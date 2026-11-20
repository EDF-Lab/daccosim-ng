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

import static eu.simulage.daccosim.DataType.Real;
import static eu.simulage.editor.EditorUtils.defaultValueOf;
import static eu.simulage.editor.EditorUtils.defaultVariabilityOf;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public abstract class OperatorSpec extends BlockSpec {

	protected String type = Real.toString();
	protected List<VariableSpec> inputs = new ArrayList<>();

	public OperatorSpec() {
	}

	public OperatorSpec(OperatorSpec toBeCloned) {
		type = toBeCloned.type;
		toBeCloned.inputs.forEach(i -> inputs.add(new VariableSpec(i)));
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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
		return singletonList(new VariableSpec("output", type, defaultVariabilityOf(type), defaultValueOf(type)));
	}

	@Override
	public List<VariableSpec> getVariables() {
		return emptyList();
	}

}
