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

import eu.simulage.daccosim.DataType;
import eu.simulage.editor.EditorUtils;

import static eu.simulage.daccosim.DataType.Real;
import static eu.simulage.editor.EditorUtils.newLabel;

@SuppressWarnings("unused")
public class MultiplierSpec extends OperatorSpec {

	private static final String ID_PREFIX = "multiplier";
	private String label = newLabel(ID_PREFIX);

	public MultiplierSpec() {
		inputs.add(new VariableSpec("input_0", Real.toString(), "continuous", "0.0"));
		inputs.add(new VariableSpec("input_1", Real.toString(), "continuous", "0.0"));
	}

	public MultiplierSpec(MultiplierSpec toBeCloned) {
		super(toBeCloned);
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

	@Override
	public String toString() {
		return DataType.valueOf(type).equals(DataType.Boolean) ? "^" : "x";
	}

}
