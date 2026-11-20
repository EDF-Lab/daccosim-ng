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

package eu.simulage.daccosim.steppers;

import eu.simulage.daccosim.model.Graph;
import eu.simulage.daccosim.model.GraphNode;

import java.util.List;

import static eu.simulage.daccosim.Iterator.streamOf;
import static java.lang.Double.MAX_VALUE;

public class Euler extends DynamicStepper {

	public Euler(Graph graph) {
		super(graph);
	}

	protected double suggestedStep(List<GraphNode> nodes, double stepSize) {
		return streamOf(stepperVariables)
				.filter(s -> nodes.contains(s.variable.parent()))
				.mapToDouble(v -> suggestedStep(v, stepSize)).min().orElse(MAX_VALUE);
	}

	private double suggestedStep(StepperVariable stepperVariable, double stepSize) {
		double value = (double) stepperVariable.variable.value();
		double previousValue = previousValues.get(stepperVariable.variable);
		double derivative = previousDerivatives.get(stepperVariable.variable);
		double tolerance = stepperVariable.tolerance;

		double errorEstimate = Math.abs(value - previousValue - stepSize * derivative);
		double mean_reference = Math.abs(value + previousValue) / 2;
		double rel_errorEstimate = mean_reference == 0 ? errorEstimate : errorEstimate / mean_reference;
		double suggestedStep = rel_errorEstimate == 0.0 ? maxStep() : safetyFactor() * stepSize * (tolerance / rel_errorEstimate);
		return errorEstimate < tolerance * value + tolerance && suggestedStep < stepSize ? stepSize : suggestedStep;
	}

}
