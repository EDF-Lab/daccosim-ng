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

import java.util.List;

import static eu.simulage.daccosim.Iterator.streamOf;

public class Constant extends Stepper {

	public Constant(Graph graph) {
		super(graph);
	}

	@Override
	public void init() {
		super.init();
		this.stepSize = graph.settings().stepper().stepSize();
	}

	@Override
	protected double suggestedStep(List<GraphNode> graphNodes, double stepSize) {
		return stepSize;
	}

	@Override
	protected StepConfig step(List<GraphNode> nodes, double stepSize) {
		streamOf(nodes).forEach(n -> n.step(stepSize));
		transmitOutputs();
		return new StepConfig(true, stepSize);
	}

	@Override
	public double firstStep() {
		return doStep();
	}

	@Override
	public double step() {
		return doStep();
	}

	@Override
	public int rollbacksCount() {
		return 0;
	}

	@Override
	public void saveState(List<GraphNode> graphNodes) {
	}
}
