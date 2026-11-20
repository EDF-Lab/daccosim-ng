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

package eu.simulage.daccosim.model;

import eu.simulage.daccosim.DaccosimException;

import java.util.ArrayList;
import java.util.List;

public abstract class GraphNode {

	private Graph graph;
	private final String id;
	private List<Input> inputs = new ArrayList<>();
	private List<Output> outputs = new ArrayList<>();
	private List<Variable> variables = new ArrayList<>();

	protected GraphNode(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public abstract void step(double stepSize);

	public void load() {
	}

	public void init() {
	}

	public void terminate() {
	}

	protected Graph graph() {
		return graph;
	}

	GraphNode graph(Graph graph) {
		this.graph = graph;
		return this;
	}

	public List<Input> inputs() {
		return inputs;
	}

	protected void inputs(List<Input> inputs) {
		this.inputs = inputs;
		this.inputs.forEach(input -> input.parent(this));
	}

	public List<Output> outputs() {
		return outputs;
	}

	protected void outputs(List<Output> outputs) {
		this.outputs = outputs;
		outputs.forEach(output -> output.parent(this));
	}

	public void variables(List<Variable> variables) {
		this.variables = variables;
		variables.forEach(var -> var.parent(this));
	}

	public List<Variable> variables() {
		return variables;
	}

	public Input input(String id) {
		return inputs.stream().filter(i -> i.id().equals(id)).findFirst().orElse(null);
	}

	public Output output(String id) {
		return outputs.stream().filter(o -> o.id().equals(id)).findFirst().orElse(null);
	}

	public Variable variable(String id) {
		return variables.stream().filter(i -> i.id().equals(id)).findFirst().orElse(null);
	}

	protected Variable find(String ioId) {
		Input input = input(ioId);
		if (input != null) return input;
		Variable variable = variable(ioId);
		if (variable != null) return variable;
		Output output = output(ioId);
		if (output != null) return output;
		throw new DaccosimException("Variable " + ioId + " not found in node" + id());
	}
}
