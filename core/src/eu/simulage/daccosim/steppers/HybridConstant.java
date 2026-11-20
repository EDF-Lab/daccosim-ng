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
import eu.simulage.daccosim.model.FMUProxy;
import eu.simulage.daccosim.model.Graph;
import eu.simulage.daccosim.model.GraphNode;
import org.javafmi.proxy.FmiProxyV3;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static eu.simulage.daccosim.GraphExecutor.logger;
import static eu.simulage.daccosim.Iterator.streamOf;
import static java.util.stream.Collectors.toList;

public class HybridConstant extends Stepper {

	public HybridConstant(Graph graph) {
		super(graph);
	}

	@Override
	public void init() {
		super.init();
		this.stepSize = graph.settings().stepper().stepSize();
	}

	@Override
	public double firstStep() {
		return step();
	}

	@Override
	public double step() {
		return doStep();
	}

	protected StepConfig step(List<GraphNode> stage, double stepSize) {
		List<FmiProxyV3.HybridDoStepStatus> events = hybridDoStep(stage, stepSize);
		if (thereAreEvents(events)) {
			double eventTime = events.stream().filter(Objects::nonNull)
					.filter(FmiProxyV3.HybridDoStepStatus::isOccurredEvent)
					.mapToDouble(FmiProxyV3.HybridDoStepStatus::getEndTime).min().orElse(-1);
			return new StepConfig(false, eventTime - time, IntStream.range(0, stage.size()).boxed()
					.filter(i -> events.get(i) != null && events.get(i).getEndTime() == eventTime).map(stage::get).collect(toList()));
		} else {
			transmitOutputs();
			return new StepConfig(true, stepSize);
		}
	}


	@Override
	protected double suggestedStep(List<GraphNode> graphNodes, double stepSize) {
		return graph.settings().stepper().stepSize();
	}

	@Override
	public void logExecution() {
		if (rollbacksCount() > 0)
			logger().info("Rollbacks: " + rollbackCount + ". Hybrid stepping rollbacks: " + rollbacksCount());
	}

}
