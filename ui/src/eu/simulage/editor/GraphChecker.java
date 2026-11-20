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

package eu.simulage.editor;

import eu.simulage.daccosim.view.Helper;
import eu.simulage.daccosim.view.Operator;
import eu.simulage.editor.specs.*;
import org.javafmi.framework.FmiContainer;
import org.javafmi.modeldescription.v2.Capabilities;
import org.javafmi.modeldescription.v2.ModelDescription;

import java.awt.*;
import java.util.List;
import java.util.*;

import static eu.simulage.daccosim.DaccosimUtils.getModelDescription;
import static eu.simulage.editor.EditorUtils.*;

class GraphChecker {
	@SuppressWarnings("RedundantIfStatement")
	static boolean graphNotCheck(Editor editor) {
		if (editor == null) return true;
		editor.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		boolean result = notCheck(editor);
		editor.setCursor(Cursor.getDefaultCursor());
		return result;
	}

	private static boolean notCheck(Editor editor) {
		removeVariablesToExportIfTheyDontExist();
		removeStepperVariablesIfTheyDontExist();
		if (!fmuPathsExist(editor)) return true;
		if (!thereAreVertexes(editor)) return true;
		if (idsAreRepeated(editor)) return true;
		if (!arrowsAreConnected(editor)) return true;
		if (!arrowsHaveConnections(editor)) return true;
		if (newtonRaphsonUsedWithNoProperFMUs(editor)) return true;
		if (connectionsAreIncorrect(editor)) return true;
		if (operatorInputNotConnected(editor)) return true;
		if (operatorOutputNotConnected(editor)) return true;
		if (externalOutputNotConnected(editor)) return true;
		if (externalInputNotConnected(editor)) return true;
		if (exportEnabledWithEmptyVarList(editor)) return true;
		if (dynamicStepperEnabledWithEmptyVarList(editor)) return true;
		if (dynamicStepperEnabledWithNonValidFmus(editor)) return true;
		return false;
	}

	private static boolean fmuPathsExist(Editor editor) {
		Spec spec = vertexesOf().stream().filter(v -> isAnFmu(v) && (((FmuSpec) v).fileName().isEmpty() || !((FmuSpec) v).file().exists())).findFirst().orElse(null);
		if (spec == null) return true;
		showErrorDialog(editor, "Path of fmu " + spec.getLabel() + " is wrong");
		return false;
	}

	private static boolean thereAreVertexes(Editor editor) {
		if (vertexesOf().size() > 0) return true;
		showErrorDialog(editor, "Graph does not have any block");
		return false;
	}

	private static boolean idsAreRepeated(Editor editor) {
		Set<String> ids = new HashSet<>();
		for (Spec spec : vertexesOf()) {
			String label = spec.getLabel();
			if (!(spec instanceof Operator))
				if (!ids.contains(label)) ids.add(label);
				else {
					showErrorDialog(editor, "Some blocks have the same id: " + label);
					return true;
				}
		}
		return false;
	}

	private static boolean arrowsAreConnected(Editor editor) {
		boolean match = arrowsOf().stream().allMatch(ArrowSpec::isConnected);
		if (!match) showErrorDialog(editor, "Some arrows are not connected");
		return match;
	}

	private static boolean arrowsHaveConnections(Editor editor) {
		boolean match = arrowsOf().stream().noneMatch(a -> a.getConnections().isEmpty());
		if (!match) showErrorDialog(editor, "Some arrows have no connections defined");
		return match;
	}

	private static boolean connectionsAreIncorrect(Editor editor) {
		List<ArrowSpec> arrows = arrowsOf();
		for (ArrowSpec arrow : arrows) {
			if (!connectionsAreIncorrect(editor, arrow)) continue;
			return true;
		}
		return false;
	}

	private static boolean newtonRaphsonUsedWithNoProperFMUs(Editor editor) {
		if (configuration().coInitMethod().equals(ITERATIVE_PROPAGATION)) return false;
		boolean wrong = !getCellSpecs()
				.filter(b -> b instanceof FmuSpec)
				.map(b -> (FmuSpec) b)
				.map(f -> (ModelDescription) getModelDescription(f.file()))
				.map(m -> m.getCoSimulation().capabilities())
				.allMatch(c -> c.check(Capabilities.ProvidesDirectionalDerivative));
		if (wrong) showErrorDialog(editor, "Simulation is using Newton Raphson but there are non-compatible FMUs");
		return wrong;
	}

	private static boolean connectionsAreIncorrect(Editor editor, ArrowSpec arrow) {
		return arrow.getConnections().stream().anyMatch(c -> connectionIsIncorrect(editor, arrow, c));
	}

	private static boolean connectionIsIncorrect(Editor editor, ArrowSpec arrow, ConnectionSpec connection) {
		VariableSpec fromVariable = variableOf(arrow.source(), connection.getFrom());
		VariableSpec toVariable = variableOf(arrow.target(), connection.getTo());
		if (fromVariable == null) {
			showErrorDialog(editor, arrow.source().label() + "." + connection.getFrom() + " used in a connection does not exist");
			return true;
		}
		if (toVariable == null) {
			showErrorDialog(editor, arrow.target().label() + "." + connection.getTo() + " used in a connection does not exist");
			return true;
		}
		if (fromVariable.dataType() != toVariable.dataType()) {
			showErrorDialog(editor, "Type mismatch in connection: " + arrow.source().label() + "." + connection.getFrom() + " -> " +
					arrow.target().label() + "." + connection.getTo());
			return true;
		}
		if (variabilityIsIncorrect(fromVariable, toVariable)) {
			showErrorDialog(editor, "Variability mismatch in connection: " + arrow.source().label() + "." + connection.getFrom() + " -> " +
					arrow.target().label() + "." + connection.getTo());
			return true;
		}
		return false;
	}

	private static boolean variabilityIsIncorrect(VariableSpec output, VariableSpec input) {
		return !(input.getVariability().equals(FmiContainer.Variability.continuous.name()) ||
				output.getVariability().equals(input.getVariability()) ||
				output.getVariability().equals(FmiContainer.Variability.constant.name()));
	}

	private static boolean operatorInputNotConnected(Editor editor) {
		Set<Spec> specSet = arrowTargets();
		boolean match = vertexesOf().stream().anyMatch(c -> isAnOperator(c) && !specSet.contains(c));
		if (match) showErrorDialog(editor, "Some operators have no input connected");
		return match;
	}

	private static boolean isAnOperator(Spec spec) {
		return spec instanceof OperatorSpec;
	}

	private static boolean isAnFmu(Spec spec) {
		return spec instanceof FmuSpec;
	}

	private static boolean operatorOutputNotConnected(Editor editor) {
		Set<Spec> specSet = EditorUtils.arrowSources();
		boolean match = vertexesOf().stream().anyMatch(c -> isAnOperator(c) && !specSet.contains(c));
		if (match) showErrorDialog(editor, "Some operators have no output connected");
		return match;
	}

	private static boolean externalInputNotConnected(Editor editor) {
		Set<Spec> specSet = EditorUtils.arrowSources();
		boolean match = vertexesOf().stream().anyMatch(c -> c instanceof ExternalInputSpec && !specSet.contains(c));
		if (match) showErrorDialog(editor, "Some external inputs are not connected");
		return match;
	}

	private static boolean externalOutputNotConnected(Editor editor) {
		Set<Spec> specSet = arrowTargets();
		boolean match = vertexesOf().stream().anyMatch(c -> c instanceof ExternalOutputSpec && !specSet.contains(c));
		if (match) showErrorDialog(editor, "Some external outputs are not connected");
		return match;
	}

	private static void removeVariablesToExportIfTheyDontExist() {
		if (!EditorUtils.configuration().export()) return;
		List<String> variables = EditorUtils.configuration().variablesToExport();
		Set<String> allVariables = allVariables();
		new ArrayList<>(variables).stream().filter(v -> !allVariables.contains(v)).forEach(variables::remove);
		EditorUtils.configuration().variablesToExport(variables);
	}

	private static void removeStepperVariablesIfTheyDontExist() {
		if (!EditorUtils.configuration().export()) return;
		Map<String, Double> variables = EditorUtils.configuration().stepperVariables();
		Set<String> allVariables = allVariables();
		new ArrayList<>(variables.keySet()).stream().filter(v -> !allVariables.contains(v)).forEach(variables::remove);
		new ArrayList<>(variables.keySet()).stream().filter(v -> !allVariables.contains(Helper.nodeId(v)+".der(" + Helper.ioId(v) + ")")).forEach(variables::remove);
		EditorUtils.configuration().stepperVariables(variables);
	}

	private static boolean exportEnabledWithEmptyVarList(Editor editor) {
		boolean result = EditorUtils.configuration().export() && EditorUtils.configuration().variablesToExport().isEmpty();
		if (result) showErrorDialog(editor, "Simulation is marked for exportation but variable list is empty");
		return result;
	}

	private static boolean dynamicStepperEnabledWithEmptyVarList(Editor editor) {
		boolean result = !EditorUtils.configuration().stepperMethod().equals(CONSTANT) && EditorUtils.configuration().stepperVariables().isEmpty();
		if (result) showErrorDialog(editor, "Simulation is using a dynamic stepper but stepper variable list is empty");
		return result;
	}

	private static boolean dynamicStepperEnabledWithNonValidFmus(Editor editor) {
		if (configuration().stepperMethod().equals(CONSTANT)) return false;
		boolean result = getCellSpecs()
				.filter(b -> b instanceof FmuSpec)
				.map(b -> (FmuSpec) b)
				.map(f -> (ModelDescription) getModelDescription(f.file()))
				.map(m -> m.getCoSimulation().capabilities())
				.anyMatch(c -> !c.check(Capabilities.CanGetAndSetFMUState) || !c.check(Capabilities.CanHandleVariableCommunicationStepSize));
		if (result) showErrorDialog(editor, "Simulation is using a dynamic stepper but there are non-compatible FMUs");
		return result;
	}

}
