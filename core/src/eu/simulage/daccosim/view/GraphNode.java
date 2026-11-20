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

package eu.simulage.daccosim.view;

import java.util.ArrayList;
import java.util.List;

public abstract class GraphNode {

	private final String id;
	private List<Input> inputs = new ArrayList<>();
	private List<Output> outputs = new ArrayList<>();
	private List<Variable> variables = new ArrayList<>();

	public GraphNode(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public List<Input> inputs() {
		return inputs;
	}

	void inputs(List<Input> inputs) {
		this.inputs = inputs;
	}

	public List<Output> outputs() {
		return outputs;
	}
	void outputs(List<Output> outputs) {
		this.outputs = outputs;
	}

	public List<Variable> variables() {
		return variables;
	}

	void variables(List<Variable> variables) {
		this.variables = variables;
	}
}
