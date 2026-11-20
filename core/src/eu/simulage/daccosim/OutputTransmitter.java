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

package eu.simulage.daccosim;

import eu.simulage.daccosim.model.Arrow;
import eu.simulage.daccosim.model.FMUProxy;
import eu.simulage.daccosim.model.Graph;
import eu.simulage.daccosim.model.Input;
import eu.simulage.daccosim.model.inputs.FMUInput;
import eu.simulage.daccosim.model.outputs.FMUOutput;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static eu.simulage.daccosim.DaccosimUtils.areDifferent;
import static eu.simulage.daccosim.DaccosimUtils.flowInputsIn;
import static eu.simulage.daccosim.Iterator.streamOf;
import static java.util.stream.Collectors.toList;

public class OutputTransmitter {

	private final List<Arrow> arrows;
	private final Map<Input, List<Integer>> connections;
	private final Set<String> flowVariables;
	private final boolean transmitDerivatives;
	private List<Arrow> arrowsWithDerivatives;

	public OutputTransmitter(Graph graph) {
		this(graph, graph.arrows());
	}

	OutputTransmitter(Graph graph, List<Arrow> arrows) {
		this.flowVariables = flowInputsIn(graph);
		this.arrows = arrows;
		this.connections = DaccosimUtils.asMap(arrows);
		this.transmitDerivatives = graph.settings().stepper().transmitDerivatives();
	}

	public void execute() {
		List<Object> fromValues = streamOf(arrows).map(a -> a.from().value()).collect(toList());
		List<Object> newFromValues = fromValues;
		do {
			write(fromValues = newFromValues);
			newFromValues = streamOf(arrows).map(a -> a.from().value()).collect(toList());
		} while (areDifferent(fromValues, newFromValues));
		streamOf(arrowsWithDerivatives())
				.forEach(a -> ((FMUInput) a.to()).transmissionDerivative(((FMUOutput) a.from()).transmissionDerivative()));
	}

	public void executeOnce() {
		write(streamOf(arrows).map(a -> a.from().value()).collect(toList()));
	}

	private List<Arrow> arrowsWithDerivatives() {
		if (!transmitDerivatives) return Collections.emptyList();
		if (arrowsWithDerivatives != null) return arrowsWithDerivatives;
		arrowsWithDerivatives = arrows.stream()
				.filter(a -> a.from().type() == DataType.Real)
				.filter(a -> a.fromNode() instanceof FMUProxy && a.toNode() instanceof FMUProxy)
				.filter(a -> ((FMUProxy) a.fromNode()).allowDerivativeTransmission(a.from().id()) && ((FMUProxy) a.toNode()).allowDerivativeTransmission(a.to().id()))
				.collect(toList());
		return arrowsWithDerivatives;
	}

	private void write(List<Object> values) {
		Iterator.streamOf(connections.entrySet()).forEach(e -> e.getKey().value(!flowVariables.contains(e.getKey().path()) ?
				values.get(e.getValue().get(0)) : e.getValue().stream().mapToDouble(i -> -(Double) values.get(i)).sum()));
	}
}
