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

package eu.simulage.daccosim.model.nodes.operators;

import eu.simulage.daccosim.DataType;
import eu.simulage.daccosim.model.Output;
import eu.simulage.daccosim.model.nodes.Operator;
import eu.simulage.daccosim.model.outputs.OperatorOutput;

import java.util.stream.Collectors;

import static java.util.stream.IntStream.range;

public class Gain extends Operator {

	private Object value;

	public Gain(String id, DataType dataType) {
		super(id, dataType);
		outputs(new Output("output", dataType));
	}

	@Override
	public void init() {
		super.init();
	}

	public Gain value(Object value) {
		this.value = value;
		return this;
	}

	public Object value() {
		return this.value;
	}

	public Gain outputs(Output... outputs) {
		super.outputs(range(0, outputs.length).boxed()
				.map(i -> new OperatorOutput(outputs[i].id(), outputs[i].type(), calculator(i)))
				.collect(Collectors.toList()));
		return this;
	}

	private Calculator calculator(Integer index) {
		if (dataType.equals(DataType.Integer)) return () -> (int) inputs().get(index).value() * (int) value;
		if (dataType.equals(DataType.Boolean)) return () -> (boolean) inputs().get(index).value() && (boolean) value;
		return () -> (double) inputs().get(index).value() * (double) value;
	}
}
