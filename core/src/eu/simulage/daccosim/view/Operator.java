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

package eu.simulage.daccosim.view;

import eu.simulage.daccosim.DataType;

import static eu.simulage.daccosim.DataType.Real;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class Operator extends GraphNode {

	private final Type type;
	private Object value;
	private DataType dataType = Real;

	public Operator(String id, Type type) {
		super(id);
		this.type = type;
	}

	public Type type() {
		return type;
	}

	public Operator inputs(Input... inputs) {
		super.inputs(asList(inputs));
		return this;
	}

	public Operator outputs(Output... outputs) {
		super.outputs(asList(outputs));
		return this;
	}

	public Operator output(Output output) {
		super.outputs(singletonList(output));
		return this;
	}

	public Object value() {
		return value;
	}

	public Operator value(Object value) {
		this.value = value;
		return this;
	}

	public Operator dataType(String type) {
		dataType = DataType.valueOf(type);
		return this;
	}

	public Operator dataType(DataType type) {
		dataType = type;
		return this;
	}

	public DataType dataType() {
		return dataType;
	}

	public enum Type {Adder, Multiplier, Gain, Offset}
}
