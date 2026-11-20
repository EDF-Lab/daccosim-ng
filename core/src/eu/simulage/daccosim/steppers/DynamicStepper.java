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

import eu.simulage.daccosim.Stepper;
import eu.simulage.daccosim.model.Graph;
import eu.simulage.daccosim.model.GraphNode;

import java.util.Collections;
import java.util.List;

import static eu.simulage.daccosim.Iterator.streamOf;

public abstract class DynamicStepper extends Stepper {

	DynamicStepper(Graph graph) {
		super(graph);
	}

	@Override
	public void init() {
		super.init();
		stepSize = (graph.settings().stopTime() - graph.settings().startTime()) / 1000;
	}

	private void performStep(List<GraphNode> graphNodes, double stepSize) {
		double finalStepSize = bound(stepSize);
		streamOf(graphNodes).forEach(n -> n.step(finalStepSize));
	}

	@Override
	public double firstStep() {
//		for (List<GraphNode> graphNodes : pipeline) performStep(graphNodes, stepSize);
//		updateTime(stepSize);
//		return stepSize;
		return doStep();
	}

	protected StepConfig step(List<GraphNode> nodes, double stepSize) {
		performStep(nodes, stepSize);
		double suggested = suggestedStep(nodes, stepSize);
 		if (suggested >= stepSize) transmitOutputs();
		return new StepConfig(suggested >= stepSize, Math.min(suggested, stepSize), Collections.emptyList());
	}

	@Override
	public double step() {
		transmitOutputs();
		return doStep();
	}

	protected double maxStep() {
		return graph.settings().stopTime() - time();
	}
}
