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

import eu.simulage.daccosim.DaccosimUtils;

import java.util.ArrayList;
import java.util.List;

public interface Interpolator {

	Object interpolateAt(double time);

	default int indexOf(List<Double> times, double time) {
		int index = -1;
		if (times.size() == 0) return -1;
		if (times.get(0) == time) return 0;
		for (int i = times.size() - 2; i >= 0; i--) {
			if (time < times.get(i) || time > times.get(i + 1)) continue;
			index = i;
		}
		return index;
	}

	void update(double time);

	default Object valueAt(double time, int subIndex) {
		List<ValueRecord> values = values();
		if (values.size() == 0) return null;
		for (int i = values.size() - 1; i >= 0; i--) {
			if (DaccosimUtils.areEquals(values.get(i).time, time)) {
				if(subIndex == Integer.MAX_VALUE) return values.get(i).lastValue();
				if (subIndex >= values.get(i).values.size()) return null;
				return values.get(i).values.get(subIndex);
			}
			if (time > values.get(i).time) break;
		}
		return null;
	}

	List<ValueRecord> values();

	class ValueRecord {
		private double time;
		private List<Object> values = new ArrayList<>();

		public ValueRecord(double time) {
			this.time = time;
		}

		public double time() {
			return time;
		}

		public List<Object> values() {
			return values;
		}

		public Object lastValue() {
			return values.get(values.size() - 1);
		}
	}
}
