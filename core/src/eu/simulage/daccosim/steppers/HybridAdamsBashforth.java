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
import eu.simulage.daccosim.model.Variable;

import java.util.List;

import static eu.simulage.daccosim.Iterator.streamOf;

public class HybridAdamsBashforth extends HybridDynamicStepper {

	private final StepSizeGetter step;
	private double previousStep = 0;
	private int stepCount;
	private int stepCountAfterLastCalculation;

	public HybridAdamsBashforth(Graph graph) {
		super(graph);
		step = order() == 1 ? suggester1() : order() == 2 ? suggester2() : order() > 2 ? suggesterK() : suggesterMax();
		stepSize = stepSize();
	}

	private static double value(Variable variable) {
		return (double) variable.value();
	}

	private StepSizeGetter suggester1() {
		return (stepperVariable, currentStepSize) -> {
			double errorEstimate = Math.abs(value(stepperVariable.variable) - previousValue(stepperVariable.variable) - currentStepSize * previousDerivatives.get(stepperVariable.variable));
			return suggestedStepSize(stepperVariable, errorEstimate, currentStepSize);
		};
	}

	private StepSizeGetter suggester2() {
		return (stepperVariable, currentStepSize) -> {
			double[] derivatives = new double[2], stepSizes = new double[2];
			for (int k = 0; k < order(); ++k) {
				derivatives[k] = previousDerivatives.get(stepperVariable.variable);
				stepSizes[k] = previousStep;
			}
			double currentValue = value(stepperVariable.variable);
			double h_k = stepSizes[0];
			double alpha = h_k / stepSizes[1];
			double A_k = 1.0 + alpha / 2.0;
			double B_k = -alpha / 2.0;
			double estimate = previousValues.get(stepperVariable.variable) + h_k * A_k * derivatives[0] + h_k * B_k * derivatives[1];
			double errorEstimate = Math.abs(currentValue - estimate);
			return suggestedStepSize(stepperVariable, errorEstimate, currentStepSize);
		};
	}

	private StepSizeGetter suggesterK() {
		return (stepperVariable, currentStepSize) -> {
			double[] derivatives = new double[order()], stepSize = new double[order()];
			for (int k = 0; k < order(); ++k) {
				derivatives[k] = previousDerivatives.get(stepperVariable.variable);
				stepSize[k] = previousStep;
			}
			double currentValue = value(stepperVariable.variable);
			double[] b = calculateAMCoefficients(order(), stepSize);
			double h_n = stepSize[0];
			double delta = 0;
			for (int k = 0; k < order(); ++k)
				delta += b[k] * derivatives[k] * h_n;
			double errorEstimate = Math.abs(currentValue - previousValue(stepperVariable.variable) - delta);

			return suggestedStepSize(stepperVariable, errorEstimate, currentStepSize);
		};
	}

	private StepSizeGetter suggesterMax() {
		return (stepperVariable, currentStepSize) -> maxStep();
	}

	@Override
	public double firstStep() {
		super.firstStep();
		stepCount = stepCountAfterLastCalculation = 1;
		return previousStep = stepSize;
	}

	@Override
	public double step() {
		previousStep = super.step();
		stepCount++;
		return previousStep;
	}

	protected double suggestedStep(List<GraphNode> nodes, double stepSize) {
		if (stepCount < order() || stepCountAfterLastCalculation < order()) {
			stepCountAfterLastCalculation++;
			return this.stepSize;
		}
		return streamOf(stepperVariables)
				.filter(s -> nodes.contains(s.variable.parent()))
				.mapToDouble(s -> step.suggest(s, stepSize))
				.min().orElse(Double.MAX_VALUE);
	}

	@Override
	protected void rollback(List<GraphNode> graphNodes) {
		stepCountAfterLastCalculation = 0;
		super.rollback(graphNodes);
	}

	private double previousValue(Variable output) {
		return previousValues.get(output);
	}

	private double suggestedStepSize(StepperVariable stepperVariable, double errorEstimate, double currentStepSize) {
		Variable variable = stepperVariable.variable;
		double tolerance = stepperVariable.tolerance;
		double mean_reference = Math.abs(value(variable) + previousValues.get(variable)) / 2;
		if (mean_reference == 0.0) mean_reference = 1;
		double rel_errorEstimate = errorEstimate / mean_reference;

		double suggestedStepSize = Math.min(maxStep(), safetyFactor() * currentStepSize * (tolerance / rel_errorEstimate));
		return errorEstimate < tolerance * value(variable) + tolerance && suggestedStepSize < currentStepSize ? currentStepSize : suggestedStepSize;
	}

	private double[][] calculateBeta(int order, double[] stepSizes) {
		double[][] beta = new double[order][order];
		for (int k = 0; k < order; ++k) beta[0][k] = 1;
		for (int j = 1; j < order; ++j) {
			for (int n = 0; n < order - j; ++n) {
				double num = 0;
				double den = 0;
				for (int t = 0; t <= j - 1; ++t) num += stepSizes[t];
				for (int t = 1; t <= j; ++t) den += stepSizes[t];
				beta[j][n] = beta[j - 1][n] * num / den;
			}
		}
		return beta;
	}

	private double[][][] calculatePhi(int order, double[][] beta) {
		double[][][] phi = new double[order][order][order];
		for (int k = 0; k < order; ++k) phi[0][k][k] = 1;
		for (int j = 1; j < order; ++j)
			for (int k = 0; k < order - j; ++k)
				for (int l = 0; l < order; ++l)
					phi[j][k][l] = phi[j - 1][k][l] - beta[j - 1][k + 1] * phi[j - 1][k + 1][l];
		return phi;
	}

	private double[] calculateG(int order, double[] stepSizes) {
		double h_n = stepSizes[0];
		double[][] c = new double[order][order + 1];
		for (int q = 1; q < order + 1; ++q) {
			c[0][q] = 1.0 / q;
			c[1][q] = 1.0 / (q * (q + 1));
		}
		for (int j = 2; j < order; ++j) {
			for (int q = 1; q < order + 1 - j; ++q) {
				double den = 0;
				for (int n = 0; n <= j - 1; ++n) den += stepSizes[n];
				c[j][q] = c[j - 1][q] - c[j - 1][q + 1] * h_n / den;
			}
		}
		double[] res = new double[order];
		for (int j = 0; j < order; ++j) res[j] = c[j][1];
		return res;
	}

	private double[] calculateAMCoefficients(int order, double[] stepSizes) {
		double[][] beta = calculateBeta(order, stepSizes);
		double[][][] phi = calculatePhi(order, beta);
		double[] g = calculateG(order, stepSizes);
		double[] coefficients = new double[order];
		for (int i = 0; i < order; ++i)
			for (int j = 0; j < order; ++j) coefficients[i] += beta[j][0] * g[j] * phi[j][0][i];
		return coefficients;
	}

	interface StepSizeGetter {
		double suggest(StepperVariable variable, double currentStepSize);
	}

}
