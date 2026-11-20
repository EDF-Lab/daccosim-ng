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

public class HermiteInterpolator implements Interpolator {

	private final Variable variable;
	private final List<Double> times = new ArrayList<>();
	private final List<Double> values = new ArrayList<>();
	private final List<Double> derivatives = new ArrayList<>();
	private final List<ValueRecord> valueRecords = new ArrayList<>();

	public HermiteInterpolator(Variable variable) {
		this.variable = variable;
	}

	@Override
	public Object interpolateAt(double time) {
		int index = indexOf(times, time);
		if (index == times.size() || index == -1) return null;
		Object value = valueAt(time, 0);
		if (value != null) return value;

		double x0 = times.get(index);
		double x1 = times.get(index + 1);
		double x = time;
		double q0 = pow((x - x1) / (x0 - x1), 2);
		double q1 = pow((x - x0) / (x1 - x0), 2);

		double p0 = valueAt(index, time) + (x - x0) * (derivatives.get(index) - ((2 * valueAt(index, time) / (x0 - x1))));
		double p1 = valueAt(index + 1, time) + (x - x1) * (derivatives.get(index + 1) - ((2 * valueAt(index + 1, time) / (x1 - x0))));
		return q0 * p0 + q1 * p1;
	}

	private double valueAt(int index, double time) {
		double indexTime = times.get(index);
		return time < indexTime ? values.get(index) : (double) valueAt(indexTime, Integer.MAX_VALUE);
	}

	@Override
	public void update(double time) {
		if (!times.isEmpty() && times.get(times.size() - 1) == time) {
			times.remove(times.size() - 1);
			values.remove(values.size() - 1);
			derivatives.remove(derivatives.size() - 1);
		}
		times.add(time);
		values.add((Double) variable.value());
		derivatives.add(variable.derivative());

		if (valueRecords.size() == 0 || !areEquals(valueRecords.get(valueRecords.size() - 1).time(), time))
			valueRecords.add(new ValueRecord(time));
		valueRecords.get(valueRecords.size() - 1).values().add(variable.value());
	}

	@Override
	public List<ValueRecord> values() {
		return valueRecords;
	}
}
