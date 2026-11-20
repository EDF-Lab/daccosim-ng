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

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import static eu.simulage.editor.EditorUtils.newLabel;
import static java.lang.Double.parseDouble;

@SuppressWarnings("unused")
public class OffsetSpec extends OperatorSpec {

	private static final String ID_PREFIX = "offset";
	private String label = newLabel(ID_PREFIX);
	private String value = "0.0";
	private List<VariableSpec> outputs = new ArrayList<>();

	public OffsetSpec() {
	}

	public OffsetSpec(OffsetSpec toBeCloned) {
		super(toBeCloned);
		this.value = toBeCloned.value;
		toBeCloned.outputs.forEach(i -> outputs.add(new VariableSpec(i)));
	}

	@Override
	public List<VariableSpec> getOutputs() {
		return outputs;
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

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String label() {
		return label;
	}

	@Override
	public String toString() {
		return DataType.valueOf(type).equals(DataType.Boolean) ? "v\n" + value :
				"+\n" + (value.length() > 5 ?
						new Formatter(Locale.ENGLISH).format("%1.1e", parseDouble(value)) :
						value);
	}
}
