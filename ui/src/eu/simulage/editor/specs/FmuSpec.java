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

import eu.simulage.daccosim.DaccosimUtils;
import org.javafmi.modeldescription.ScalarVariable;
import org.javafmi.wrapper.Simulation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static eu.simulage.SimulationProvider.getSimulation;
import static eu.simulage.editor.EditorUtils.defaultValueOf;
import static eu.simulage.editor.EditorUtils.getEditor;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;

@SuppressWarnings("ConstantConditions")
public class FmuSpec extends BlockSpec {

	private String label = "";
	private String filename = "";
	private List<VariableSpec> beforeInitValues = new ArrayList<>();
	private List<VariableSpec> inInitValues = new ArrayList<>();
	private List<String> flowVariables = new ArrayList<>();
	private boolean static_ = false;

	@SuppressWarnings("WeakerAccess")
	public FmuSpec() {
	}

	public FmuSpec(FmuSpec toBeCloned) {
		this.filename = toBeCloned.filename;
		this.label = toBeCloned.label;
		this.beforeInitValues.addAll(toBeCloned.beforeInitValues);
		this.inInitValues.addAll(toBeCloned.inInitValues);
	}

	private static boolean isInput(ScalarVariable s) {
		return s.getCausality().equalsIgnoreCase("input");
	}

	private static boolean isOutput(ScalarVariable s) {
		return s.getCausality().equalsIgnoreCase("output");
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getLabel() {
		return label;
	}

	@Override
	public void setLabel(String label) {
		this.label = label.replace(".", "_");
	}

	@Override
	public String label() {
		return label;
	}

	public boolean isStatic_() {
		return static_;
	}

	public void setStatic_(boolean static_) {
		this.static_ = static_;
	}

	@SuppressWarnings("unused")
	public List<VariableSpec> getBeforeInitValues() {
		return beforeInitValues;
	}

	@SuppressWarnings("unused")
	public void setBeforeInitValues(List<VariableSpec> beforeInitValues) {
		this.beforeInitValues = beforeInitValues;
	}

	@SuppressWarnings("unused")
	public List<VariableSpec> getInInitValues() {
		return inInitValues;
	}

	@SuppressWarnings("unused")
	public void setInInitValues(List<VariableSpec> inInitValues) {
		this.inInitValues = inInitValues;
	}

	public File file() {
		return getEditor().getFile().fmuFile(fileName());
	}

	public String fileName() {
		return filename;
	}

	@Override
	public String toString() {
		return label;
	}

	public boolean isMatryoshka() {
		return getSimulation(file()).getModelDescription().getModelIdentifier().startsWith("Matryoshka_");
	}

	public List<VariableSpec> getOutputs() {
		return stream(getSimulation(file()).getModelDescription().getModelVariables())
				.filter(FmuSpec::isOutput)
				.filter(f -> !static_ || f.getVariability().equals("continuous"))
				.map(s -> new VariableSpec(s.getName(), s.getTypeName(), s.getVariability(), defaultValueOf(s.getTypeName())))
				.collect(Collectors.toList());
	}

	public List<VariableSpec> getInputs() {
		return stream(getSimulation(file()).getModelDescription().getModelVariables())
				.filter(FmuSpec::isInput)
				.filter(i -> !i.getName().equals(DaccosimUtils.SysVarResetFromMaster))
				.map(s -> new VariableSpec(s.getName(), s.getTypeName(), s.getVariability(), s.getStart() == null ? defaultValueOf(s.getTypeName()) : s.getStart().toString()))
				.collect(Collectors.toList());
	}

	public List<VariableSpec> formalInputs() {
		return stream(getSimulation(file()).getModelDescription().getModelVariables())
				.filter(FmuSpec::isInput)
				//TODO remove (it's not necessary?) .filter(i -> !i.getName().equals("__resetFromMaster"))
				.map(s -> new VariableSpec(s.getName(), s.getTypeName(), s.getVariability(), defaultValueOf(s.getTypeName())))
				.collect(Collectors.toList());
	}

	public List<VariableSpec> formalOutputs() {
		return stream(getSimulation(file()).getModelDescription().getModelVariables())
				.filter(FmuSpec::isOutput)
				.map(s -> new VariableSpec(s.getName(), s.getTypeName(), s.getVariability(), defaultValueOf(s.getTypeName())))
				.collect(Collectors.toList());
	}

	public List<VariableSpec> getVariables() {
		Simulation simulation = getSimulation(file());
		return simulation == null ? emptyList() :
				stream(simulation.getModelDescription().getModelVariables())
						.filter(s -> !isInput(s) && !isOutput(s))
						.map(s -> new VariableSpec(s.getName(), s.getTypeName(), s.getVariability(), defaultValueOf(s.getTypeName())))
						.collect(Collectors.toList());
	}

	public List<String> getFlowVariables() {
		return flowVariables;
	}

	public List<String> getInputFlowVariables() {
		return flowVariables.stream()
				.filter(v -> getInputs().stream().anyMatch(i -> i.getName().equals(v)))
				.collect(Collectors.toList());
	}

	public List<String> getOutputFlowVariables() {
		return flowVariables.stream()
				.filter(v -> getOutputs().stream().anyMatch(i -> i.getName().equals(v)))
				.collect(Collectors.toList());
	}

	@Override
	public List<String> allVariables() {
		return !static_ ? super.allVariables() : qnOutputs();
	}
}
