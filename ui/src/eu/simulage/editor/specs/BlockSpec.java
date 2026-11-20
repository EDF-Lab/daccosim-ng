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

package eu.simulage.editor.specs;

import eu.simulage.editor.Spec;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public abstract class BlockSpec implements Spec {

	public abstract List<VariableSpec> getInputs();

	public abstract List<VariableSpec> getOutputs();

	@SuppressWarnings("WeakerAccess")
	public abstract List<VariableSpec> getVariables();


	public List<String> allVariables() {
		List<String> result = new ArrayList<>();
		result.addAll(qnInputs());
		result.addAll(qnOutputs());
		result.addAll(qnVariables());
		return result;
	}

	private List<VariableSpec> allVariableSpecs() {
		List<VariableSpec> result = new ArrayList<>();
		result.addAll(getInputs());
		result.addAll(getOutputs());
		result.addAll(getVariables());
		return result;
	}

	public List<String> qnInputs() {
		return qnOf(getInputs());
	}

	public List<String> qnOutputs() {
		return qnOf(getOutputs());
	}

	public List<String> qnVariables() {
		return qnOf(getVariables());
	}

	private List<String> qnOf(List<VariableSpec> inputs) {
		return inputs.stream().map(v -> getLabel() + "." + v.getName()).collect(toList());
	}

	public VariableSpec variable(String name) {
		return allVariableSpecs().stream().filter(v -> v.getName().equals(name)).findFirst().orElse(null);
	}
}
