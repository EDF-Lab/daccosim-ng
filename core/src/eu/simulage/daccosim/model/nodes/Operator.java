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

package eu.simulage.daccosim.model.nodes;

import eu.simulage.daccosim.DataType;
import eu.simulage.daccosim.model.GraphNode;
import eu.simulage.daccosim.model.Input;

import java.util.List;

import static eu.simulage.daccosim.DataType.Real;
import static java.util.Arrays.asList;

public abstract class Operator extends GraphNode {

	protected final DataType dataType;

	protected Operator(String id, DataType dataType) {
		super(id);
		this.dataType = dataType;
	}

	@Override
	public void init() {
		inputs().forEach(i -> i.value(dataType == Real ? 0.0 : dataType == DataType.Integer ? 0 : false));
	}

	@Override
	public void step(double stepSize) {
	}

	public Operator inputs(Input... inputs) {
		List<Input> list = asList(inputs);
		list.forEach(input -> input.value(0.0));
		super.inputs(list);
		return this;
	}

	public interface Calculator {
		Object calculate();
	}
}
