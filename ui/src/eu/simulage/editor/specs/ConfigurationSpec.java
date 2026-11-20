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

import eu.simulage.daccosim.view.Helper;

import java.util.*;

import static eu.simulage.editor.EditorUtils.CONSTANT;
import static eu.simulage.editor.EditorUtils.ITERATIVE_PROPAGATION;
import static java.util.stream.Collectors.toList;

public class ConfigurationSpec {

	private String startTime = "0.0";
	private String stopTime = "0.0";
	private String coInitMethod = ITERATIVE_PROPAGATION;
	private String tolerance = "1e-5";
	private String maxIter = "20";
	private String stepperMethod = CONSTANT;
	private String stepSize = "1";
	private String safetyFactor = "0.9";
	private String order = "3";
	private boolean transmitDerivatives = false;
	private boolean export = true;
	private String cellSeparator = ";";
	private String decimalSeparator = ".";
	private String interpolationOption = "None";
	private String interpolationData = "0.0";
	private List<String> variablesToExport = new ArrayList<>();
	private Map<String, Double> stepperVariables = new HashMap<>();
	private String pipeline = "";
	private String notes;

	public String startTime() {
		return startTime;
	}

	public void startTime(String startTime) {
		this.startTime = startTime;
	}

	public String stopTime() {
		return stopTime;
	}

	public void stopTime(String stopTime) {
		this.stopTime = stopTime;
	}

	public String coInitMethod() {
		return coInitMethod;
	}

	public void coInitMethod(String coInitMethod) {
		this.coInitMethod = coInitMethod;
	}

	public String tolerance() {
		return tolerance;
	}

	public void tolerance(String tolerance) {
		this.tolerance = tolerance;
	}

	public String maxIter() {
		return maxIter;
	}

	public void maxIter(String maxIter) {
		this.maxIter = maxIter;
	}

	public String stepperMethod() {
		return stepperMethod;
	}

	public void stepperMethod(String stepperMethod) {
		this.stepperMethod = stepperMethod;
	}

	public String stepSize() {
		return stepSize;
	}

	public void stepSize(String stepSize) {
		this.stepSize = stepSize;
	}

	public String safetyFactor() {
		return safetyFactor;
	}

	public void safetyFactor(String safetyFactor) {
		this.safetyFactor = safetyFactor;
	}

	public String order() {
		return order;
	}

	public void order(String order) {
		this.order = order;
	}

	public boolean transmitDerivatives() {
		return transmitDerivatives;
	}

	public void transmitDerivatives(boolean transmitDerivatives) {
		this.transmitDerivatives = transmitDerivatives;
	}

	public boolean export() {
		return export;
	}

	public void export(boolean export) {
		this.export = export;
	}

	public String cellSeparator() {
		return cellSeparator;
	}

	public void cellSeparator(String cellSeparator) {
		this.cellSeparator = cellSeparator;
	}

	public String decimalSeparator() {
		return decimalSeparator;
	}

	public void decimalSeparator(String decimalSeparator) {
		this.decimalSeparator = decimalSeparator;
	}

	public String interpolationOption() {
		return interpolationOption;
	}

	public void interpolationOption(String interpolationOption) {
		this.interpolationOption = interpolationOption;
	}

	public String interpolationData() {
		return interpolationData;
	}

	public void interpolationData(String interpolationData) {
		this.interpolationData = interpolationData;
	}

	public List<String> variablesToExport() {
		return variablesToExport;
	}

	public void variablesToExport(List<String> variablesToExport) {
		this.variablesToExport = variablesToExport;
	}

	public Map<String, Double> stepperVariables() {
		return stepperVariables;
	}

	public void stepperVariables(Map<String, Double> stepperVariables) {
		this.stepperVariables = stepperVariables;
	}

	public Collection<? extends String> variablesToExport(BlockSpec spec) {
		return variablesToExport.stream()
				.filter(v -> v.startsWith(spec.getLabel()))
				.map(Helper::ioId)
				.collect(toList());
	}

	public Collection<? extends String> stepperVariables(BlockSpec spec) {
		return stepperVariables.keySet().stream()
				.filter(v -> v.startsWith(spec.getLabel()))
				.map(Helper::ioId)
				.collect(toList());
	}

	public String notes() {
		return notes;
	}

	@SuppressWarnings("UnusedReturnValue")
	public ConfigurationSpec notes(String notes) {
		this.notes = notes;
		return this;
	}

	public String pipeline() {
		return pipeline;
	}

	public ConfigurationSpec pipeline(String pipeline) {
		this.pipeline = pipeline;
		return this;
	}
}
