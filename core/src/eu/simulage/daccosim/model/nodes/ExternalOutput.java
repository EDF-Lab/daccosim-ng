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

import eu.simulage.daccosim.model.GraphNode;
import eu.simulage.daccosim.model.InitialValue;
import eu.simulage.daccosim.model.Input;

import java.util.List;

import static java.util.Arrays.asList;

public class ExternalOutput extends GraphNode {

	private List<InitialValue> initialValues;

	public ExternalOutput(String id) {
		super(id);
	}

	@Override
	public void init() {
		initialValues.forEach(b -> input(b.name()).value(b.value()));
	}

	@Override
	public void step(double stepSize) {
	}

	public ExternalOutput inputs(Input... inputs) {
		super.inputs(asList(inputs));
		return this;
	}

	public ExternalOutput initialValues(List<InitialValue> initialValues) {
		this.initialValues = initialValues;
		return this;
	}
}
