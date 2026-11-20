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

import java.io.Serializable;

public class VariableSpec implements Serializable {

	private String name;
	private String type;
	private String value;
	private String variability;

	public VariableSpec() {
	}

	public VariableSpec(VariableSpec toBeCloned) {
		this(toBeCloned.name, toBeCloned.type, toBeCloned.variability, toBeCloned.value);
	}

	public VariableSpec(String name, String type, String variability, String value) {
		this.name = name;
		this.type = type;
		this.variability = variability;
		this.value = value;
	}

	public VariableSpec(String name, DataType type, String variability, String value) {
		this(name, type.toString(), variability, value);
	}

	public String getName() {
		return name;
	}

	@SuppressWarnings("unused")
	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getVariability() {
		return variability;
	}

	public void setVariability(String variability) {
		this.variability = variability;
	}

	public DataType dataType() {
		return DataType.valueOf(type);
	}
}
