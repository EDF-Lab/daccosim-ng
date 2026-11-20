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
import eu.simulage.daccosim.model.Input;
import eu.simulage.daccosim.model.nodes.Operator;
import eu.simulage.daccosim.model.outputs.OperatorOutput;

import static java.util.Collections.singletonList;

public class Multiplier extends Operator {

	public Multiplier(String id, DataType dataType) {
		super(id, dataType);
		Calculator calculator = () -> {
			double result = 1;
			for (Input input : inputs()) result *= (double) input.value();
			return inputs().isEmpty() ? 0 : result;
		};
		if (dataType.equals(DataType.Integer)) calculator = () -> {
			int result = 1;
			for (Input input : inputs()) result *= (int) input.value();
			return inputs().isEmpty() ? 0 : result;
		};
		if (dataType.equals(DataType.Boolean)) calculator = () -> {
			boolean result = true;
			for (Input input : inputs()) result = result && (boolean) input.value();
			return !inputs().isEmpty() && result;
		};
		super.outputs(singletonList(new OperatorOutput("output", dataType, calculator)));
	}

	@Override
	public void init() {
		super.init();
	}

}
