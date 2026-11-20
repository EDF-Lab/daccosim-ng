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

package eu.simulage.daccosim.model.variables;

import eu.simulage.daccosim.DataType;
import eu.simulage.daccosim.model.FMUProxy;
import eu.simulage.daccosim.model.Variable;

public class FMUVariable extends Variable {
	private final FMUProxy fmu;

	public FMUVariable(String id, DataType type, FMUProxy proxy, String variability) {
		super(id, type);
		this.fmu = proxy;
		this.variability(variability);
	}

	public Object value() {
		return fmu.pull(id());
	}

	@Override
	public void value(Object value) {
		fmu.push(id(), value);
	}

	public Double derivative() {
		return (Double) fmu.pull("der(" + id() + ")");
	}

	@Override
	public Double secondDerivative() {
		return (Double) fmu.pull("der(d" + id() + ")");
	}
}
