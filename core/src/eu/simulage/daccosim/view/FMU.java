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

import static java.util.Arrays.asList;

public class FMU extends GraphNode {

	private String path;
	private List<InitialValue> beforeInitValues = new ArrayList<>();
	private List<InitialValue> inInitValues = new ArrayList<>();
	private List<String> flowVariables = new ArrayList<>();
	private boolean static_;

	public FMU(String id, String path) {
		super(id);
		this.path = path;
	}

	public String path() {
		return path;
	}

	public FMU outputs(Output... outputs) {
		super.outputs(asList(outputs));
		return this;
	}

	public FMU inputs(Input... inputs) {
		super.inputs(asList(inputs));
		return this;
	}

	public FMU variables(Variable... variables) {
		super.variables(asList(variables));
		return this;
	}


	public List<InitialValue> beforeInitValues() {
		return beforeInitValues;
	}

	public FMU beforeInitValues(InitialValue... initialValues) {
		this.beforeInitValues = asList(initialValues);
		return this;
	}

	public List<InitialValue> inInitValues() {
		return inInitValues;
	}

	public FMU inInitValues(InitialValue... initialValues) {
		this.inInitValues = asList(initialValues);
		return this;
	}

	public void path(String path) {
		this.path = path;
	}

	public FMU flowVariables(String... flowVariables) {
		this.flowVariables = asList(flowVariables);
		return this;
	}

	public List<String> flowVariables() {
		return flowVariables == null ? flowVariables = new ArrayList<>() : flowVariables;
	}

	public FMU static_(boolean static_) {
		this.static_ = static_;
		return this;
	}

	public boolean static_() {
		return static_;
	}
}
