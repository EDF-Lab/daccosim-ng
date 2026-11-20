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

public class DirectInterpolator implements Interpolator {

	private final Variable variable;
	private final List<Double> times = new ArrayList<>();
	private final List<Object> values = new ArrayList<>();
	private final List<ValueRecord> valueRecords = new ArrayList<>();

	public DirectInterpolator(Variable variable) {
		this.variable = variable;
	}

	@Override
	public Object interpolateAt(double time) {
		int index = indexOf(times, time);
		if (index == times.size() || index == -1) return null;
		Object value = valueAt(time, 0);
		if (value != null) return value;
		return values.get(index);
	}

	@Override
	public void update(double time) {
		if (!times.isEmpty() && times.get(times.size() - 1) == time) {
			times.remove(times.size() - 1);
			values.remove(values.size() - 1);
		}
		times.add(time);
		values.add(variable.value());
		if (valueRecords.size() == 0 || !areEquals(valueRecords.get(valueRecords.size() - 1).time(), time))
			valueRecords.add(new ValueRecord(time));
		valueRecords.get(valueRecords.size() - 1).values().add(variable.value());
	}

	@Override
	public List<ValueRecord> values() {
		return valueRecords;
	}

}
