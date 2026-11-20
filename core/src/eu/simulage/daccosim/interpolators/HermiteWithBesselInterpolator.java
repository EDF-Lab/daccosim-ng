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

public class HermiteWithBesselInterpolator implements Interpolator {

	private final Variable variable;
	private final List<Double> times = new ArrayList<>();
	private final List<Double> values = new ArrayList<>();
	private final List<Double> derivatives = new ArrayList<>();
	private final List<ValueRecord> valueRecords = new ArrayList<>();
	private double stopTime;
	private boolean simulationFinished = false;

	public HermiteWithBesselInterpolator(Variable variable, double stopTime) {
		this.variable = variable;
		this.stopTime = stopTime;
	}

	@Override
	public Double interpolateAt(double time) {
		// TODO improve for the last part to make the linear interpolation
		if (times.size() < 3) return null;
		int index = indexOf(times, time);
		if (simulationFinished && index + 1 >= derivatives.size()) return interpolateLinear(index, time);
		if (index == -1) return null;
		Object value = valueAt(time, 0);
		if (value != null) return (Double) value;
		if (index + 1 == derivatives.size()) return null;
		if (index == 0) return interpolateLinear(0, time);
		return hermite(time, index);
	}

	private Double hermite(double time, int index) {
		double x0 = times.get(index);
		double x1 = times.get(index + 1);
		double q0 = pow((time - x1) / (x0 - x1), 2);
		double q1 = pow((time - x0) / (x1 - x0), 2);

		double p0 = valueAt(index, time) + (time - x0) * (derivatives.get(index) - ((2 * valueAt(index, time) / (x0 - x1))));
		double p1 = valueAt(index + 1, time) + (time - x1) * (derivatives.get(index + 1) - ((2 * valueAt(index + 1, time) / (x1 - x0))));
		return q0 * p0 + q1 * p1;
	}

	private double valueAt(int index, double time) {
		double indexTime = times.get(index);
		return time < indexTime ? values.get(index) : (double) valueAt(indexTime, Integer.MAX_VALUE);
	}

	private Double firstBesselPoint(double time) {
		double x0 = times.get(0);
		double x1 = times.get(1);
		double termA = (1 / Math.pow(x1 - x0, 2)) * (valueAt(0, time) - valueAt(1, time) + derivatives.get(1) * (x1 - x0));
		double termB = (1 / Math.pow(x1 - x0, 2)) * (derivatives.get(1) * (x0 * x0 - x1 * x1) + 2 * (valueAt(1, time) * x1 - valueAt(0, time) - x0));
		double termC = (1 / Math.pow(x1 - x0, 2)) * (valueAt(1, time) * x0 * x0 + valueAt(0, time) * x1 * x1 + derivatives.get(1) * x0 * x1 * x1 - derivatives.get(1) * x0 * x0 * x1 - 2 * valueAt(1, time) * x0 * x1);
		return termA * time * time + termB * time + termC;
	}

	private double interpolateLinear(int index, double time) {
		return valueAt(index, time) + ((valueAt(index + 1, time) - valueAt(index, time)) / (times.get(index + 1) - times.get(index))) * (time - times.get(index));
	}

	@Override
	public void update(double time) {
		if(lastTime(time)) simulationFinished = true;
		if(!times.isEmpty() && times.get(times.size() - 1) == time){
//			times.remove(times.size() - 1);
//			values.remove(values.size() - 1);
//			derivatives.remove(derivatives.size() - 1);
		}
		else {
			times.add(time);
			values.add((Double) variable.value());
			if (times.size() >= 3 && !lastTime(time)) derivatives.add(interpolateBessel(times.size() - 2));
			else if (derivatives.size() == 0) derivatives.add(Double.NaN);
		}
		if (valueRecords.size() == 0 || !areEquals(valueRecords.get(valueRecords.size() - 1).time(), time))
			valueRecords.add(new ValueRecord(time));
		valueRecords.get(valueRecords.size() - 1).values().add(variable.value());
	}


	@Override
	public List<ValueRecord> values() {
		return valueRecords;
	}

	private double interpolateBessel(int index) {
		double term1 = (values.get(index - 1) * (times.get(index) - times.get(index + 1))) / ((times.get(index - 1) - times.get(index)) * (times.get(index - 1) - times.get(index + 1)));
		double term2 = (values.get(index) / ((times.get(index) - times.get(index - 1)) * (times.get(index) - times.get(index + 1)))) * (2 * times.get(index) - times.get(index + 1) - times.get(index - 1));
		double term3 = (values.get(index + 1) * (times.get(index) - times.get(index - 1))) / ((times.get(index + 1) - times.get(index - 1)) * (times.get(index + 1) - times.get(index)));
		return term1 + term2 + term3;
	}

	private boolean lastTime(double time) {
		return Math.abs(time - stopTime) < 1e-6;
	}

}
