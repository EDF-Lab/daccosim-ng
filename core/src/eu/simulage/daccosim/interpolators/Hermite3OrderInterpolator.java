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

package eu.simulage.daccosim.interpolators;

import eu.simulage.daccosim.model.Variable;

import java.util.ArrayList;
import java.util.List;

import static eu.simulage.daccosim.DaccosimUtils.areEquals;
import static java.lang.Math.pow;

public class Hermite3OrderInterpolator implements Interpolator {

	private final Variable variable;
	private final List<Double> times = new ArrayList<>();
	private final List<Double> values = new ArrayList<>();
	private final List<Double> derivatives = new ArrayList<>();
	private final List<Double> secondDerivatives = new ArrayList<>();
	private final List<ValueRecord> valueRecords = new ArrayList<>();

	public Hermite3OrderInterpolator(Variable variable) {
		this.variable = variable;
	}

	@Override
	public Object interpolateAt(double time) {
		int index = indexOf(times, time);
		if (index == times.size() || index == -1) return null;
		Object value = valueAt(time, 0);
		if (value != null) return value;

		double l0 = (time - times.get(index + 1)) / (times.get(index) - times.get(index + 1));
		double q01 = (1. / (times.get(index) - times.get(index + 1))) * (-3 * time + 4 * times.get(index) - times.get(index + 1));
		double h01 = (time - times.get(index)) * q01 * pow(l0, 3);
		double q00 = (1. / pow(times.get(index) - times.get(index + 1), 2)) * (6 * pow(time, 2) + (3 * times.get(index + 1) - 15 * times.get(index)) * time + 9 * pow(times.get(index), 2) - 3 * times.get(index) * times.get(index + 1) + pow(times.get(index) - times.get(index + 1), 2));
		double h00 = q00 * pow(l0, 3);
		double q02 = 0.5;
		double h02 = pow(time - times.get(index), 2) * q02 * pow(l0, 3);
		double term0 = values.get(index) * h00 + derivatives.get(index) * h01 + secondDerivatives.get(index) * h02;

		double l1 = (time - times.get(index)) / (times.get(index + 1) - times.get(index));
		double q11 = (1. / (times.get(index + 1) - times.get(index))) * (-3 * time + 4 * times.get(index + 1) - times.get(index));
		double h11 = (time - times.get(index + 1)) * q11 * pow(l1, 3);
		double q10 = (1. / pow(times.get(index + 1) - times.get(index), 2)) * (6 * pow(time, 2) + (3 * times.get(index) - 15 * times.get(index + 1)) * time + 9 * pow(times.get(index + 1), 2) - 3 * times.get(index) * times.get(index + 1) + pow(times.get(index + 1) - times.get(index), 2));
		double h10 = q10 * pow(l1, 3);
		double q12 = 0.5;
		double h12 = pow(time - times.get(index + 1), 2) * q12 * pow(l1, 3);
		double term1 = values.get(index + 1) * h10 + derivatives.get(index + 1) * h11 + secondDerivatives.get(index + 1) * h12;
		return term0 + term1;
	}

	@Override
	public void update(double time) {
		if (!times.isEmpty() && times.get(times.size() - 1) == time) {
			times.remove(times.size() - 1);
			values.remove(values.size() - 1);
			derivatives.remove(derivatives.size() - 1);
			secondDerivatives.remove(secondDerivatives.size() - 1);
		}
		times.add(time);
		values.add((Double) variable.value());
		derivatives.add(variable.derivative());
		secondDerivatives.add(variable.secondDerivative());

		if (valueRecords.size() == 0 || !areEquals(valueRecords.get(valueRecords.size() - 1).time(), time))
			valueRecords.add(new ValueRecord(time));
		valueRecords.get(valueRecords.size() - 1).values().add(variable.value());
	}

	@Override
	public List<ValueRecord> values() {
		return valueRecords;
	}
}
