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

package eu.simulage.daccosim.model.outputs;

import eu.simulage.daccosim.DataType;
import eu.simulage.daccosim.model.FMUProxy;
import eu.simulage.daccosim.model.Output;

public class FMUOutput extends Output {

	private final FMUProxy fmu;

	public FMUOutput(String id, DataType type, FMUProxy fmu, String variability) {
		super(id, type);
		this.fmu = fmu;
		this.variability(variability);
	}

	@Override
	public Object value() {
		return fmu.pull(id());
	}

	@Override
	public void value(Object value) {
		fmu.push(id(), value);
	}

	public Double derivative() {
		return fmu.derivativeOf(id());
	}

	public Double transmissionDerivative() {
		return fmu.transmissionDerivative(id());
	}
}
