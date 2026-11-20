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


import com.google.gson.Gson;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxResources;
import com.mxgraph.view.mxGraph;
import eu.simulage.daccosim.DataType;
import eu.simulage.editor.specs.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.simulage.daccosim.DaccosimUtils.extract;
import static eu.simulage.daccosim.DataType.Real;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javax.swing.JOptionPane.*;

public class EditorUtils {
	public static final String CONSTANT = "Constant";
	public static final String EULER = "Euler";
	public static final String ADAMS = "Adams Bashforth";
	public static final String NEWTON_RAPHSON = "Newton Raphson";
	public static final String ITERATIVE_PROPAGATION = "Iterative Propagation";
	static final String ADDER = "adder";
	static final String OFFSET = "offset";
	static final String MULTIPLIER = "multiplier";
	static final String GAIN = "gain";
	static final String EXTERNAL_OUTPUT = "externaloutput";
	static final String EXTERNAL_INPUT = "externalinput";
	public static final String FMU = "fmu";
	public static final String MATRYOSHKA = "matryoshka";
	protected static final Runnable noop = () -> {
	};
	private static mxGraphComponent graphComponent;
	private static ConfigurationSpec configuration = new ConfigurationSpec();

	static void fillWith(mxGraphComponent graphComponent) {
		EditorUtils.graphComponent = graphComponent;
	}

	public static mxCell cellWithLabel(String label) {
		return stream(graph().getChildCells(graph().getDefaultParent()))
				.map(c -> ((mxCell) c))
				.filter(c -> c.getValue() instanceof Spec && ((Spec) c.getValue()).getLabel().equals(label))
				.findFirst()
				.orElse(null);
	}

	public static mxCell cellWithValue(Object object) {
		return stream(graph().getChildCells(graph().getDefaultParent()))
				.map(c -> ((mxCell) c))
				.filter(c -> c.getValue().equals(object))
				.findFirst()
				.orElse(null);
	}

	public static long labelCount(String label, Spec... excludes) {
		List<Spec> specs = asList(excludes);
		return getCellSpecs()
				.filter(spec -> !specs.contains(spec) && spec.label().equals(label))
				.count();
	}

	private static mxGraph graph() {
		return graphComponent.getGraph();
	}

	private static List<ArrowSpec> arrowsWithSource(Spec source) {
		return getArrows().filter(a -> a.source() != null && a.source().equals(source)).collect(toList());
	}

	public static List<ArrowSpec> arrowsWithTarget(Spec target) {
		return getArrows().filter(a -> a.target() != null && a.target().equals(target)).collect(toList());
	}

	public static List<ArrowSpec> arrowsWith(Spec source, Spec target) {
		return getArrows()
				.filter(a -> a.source() != null && a.target() != null)
				.filter(a -> a.source().equals(source) && a.target().equals(target))
				.collect(toList());
	}

	public static Stream<BlockSpec> getCellSpecs() {
		return stream(graph().getChildCells(graph().getDefaultParent()))
				.filter(c -> ((mxCell) c).getValue() instanceof BlockSpec)
				.map(c -> (BlockSpec) ((mxCell) c).getValue());
	}

	private static Stream<ArrowSpec> getArrows() {
		return stream(graph().getChildEdges(graph().getDefaultParent()))
				.map(c -> (ArrowSpec) ((mxCell) c).getValue());
	}

	public static String newLabel(String name, Spec... excluded) {
		String toTest = name;
		int count = 0;
		while (labelCount(toTest, excluded) > 0)
			toTest = name + "_" + count++;
		return toTest;
	}

	public static ConfigurationSpec configuration() {
		return configuration;
	}

	static String serializedConfiguration() {
		return new Gson().toJson(configuration);
	}

	static void setConfiguration(String configuration) {
		setConfiguration(new Gson().fromJson(configuration, ConfigurationSpec.class));
	}

	public static void setConfiguration(ConfigurationSpec spec) {
		configuration = spec;
	}

	static SaveChangesResult saveChangesDialog(Editor editor) {
		int dialog = showConfirmDialog(editor, mxResources.get("saveChanges"), "Choose an option", YES_NO_CANCEL_OPTION);
		if (dialog == YES_OPTION) {
			new EditorActions.SaveAction(false).actionPerformed(new ActionEvent(graphComponent, 0, "save"));
			return SaveChangesResult.Saved;
		}
		return dialog == CANCEL_OPTION ? SaveChangesResult.Cancelled : SaveChangesResult.NotSaved;
	}

	public static void showErrorDialog(Component component, String message) {
		JTextArea area = new JTextArea("ERROR. " + message);
		area.setLineWrap(true);
		area.setEditable(false);
		JScrollPane pane = new JScrollPane(area);
		pane.setPreferredSize(new Dimension(300, 200));
		showMessageDialog(component, pane, "Error", ERROR_MESSAGE);
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static boolean isDouble(Object value) {
		try {
			Double.parseDouble(value.toString());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static boolean isInteger(Object value) {
		try {
			Integer.parseInt(value.toString());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	static boolean isBoolean(Object value) {
		return value.toString().equalsIgnoreCase("false") || value.toString().equalsIgnoreCase("true");
	}

	public static void doInBackground(Component component, Runnable runnable) {
		doInBackground(component, noop, runnable, noop);
	}

	@SuppressWarnings("SameParameterValue")
	private static void doInBackground(Component component, Runnable before, Runnable runnable, Runnable after) {
		component.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		before.run();
		new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() {
				runnable.run();
				return null;
			}

			@Override
			protected void done() {
				super.done();
				after.run();
				component.setCursor(Cursor.getDefaultCursor());
			}

		}.execute();
	}

	public static void removeConnectionsAndOutputs(BlockSpec spec) {
		arrowsWithSource(spec).forEach(a -> a.getConnections().removeAll(connWithoutSources(spec.getOutputs(), a.getConnections())));
		arrowsWithTarget(spec).forEach(a -> a.getConnections().removeAll(connWithoutTargets(spec.getInputs(), a.getConnections())));
	}

	public static void updateVariablesToExport(String previousLabel, String newLabel) {
		EditorUtils.configuration().variablesToExport(EditorUtils.configuration().variablesToExport().stream()
				.map(l -> l.startsWith(previousLabel + ".") ? l.replace(previousLabel + ".", newLabel + ".") : l)
				.collect(Collectors.toList()));
	}

	private static List<ConnectionSpec> connWithoutSources(List<VariableSpec> sources, List<ConnectionSpec> connections) {
		List<String> varNames = sources.stream().map(VariableSpec::getName).collect(toList());
		return connections.stream().filter(c -> !varNames.contains(c.getFrom())).collect(toList());
	}

	private static List<ConnectionSpec> connWithoutTargets(List<VariableSpec> targets, List<ConnectionSpec> connections) {
		List<String> varNames = targets.stream().map(VariableSpec::getName).collect(toList());
		return connections.stream().filter(c -> !varNames.contains(c.getTo())).collect(toList());
	}

	static List<Spec> vertexesOf() {
		return stream(graph().getChildVertices(graph().getDefaultParent()))
				.map(o -> ((Spec) ((mxCell) o).getValue()))
				.collect(toList());
	}

	static List<ArrowSpec> arrowsOf() {
		return stream(graph().getChildEdges(graph().getDefaultParent()))
				.map(o -> ((ArrowSpec) ((mxCell) o).getValue()))
				.collect(toList());
	}

	static Set<Spec> arrowTargets() {
		return arrowsOf().stream()
				.filter(a -> !a.getConnections().isEmpty())
				.map(ArrowSpec::target)
				.collect(toSet());
	}

	static Set<Spec> arrowSources() {
		return arrowsOf().stream()
				.filter(a -> !a.getConnections().isEmpty())
				.map(ArrowSpec::source)
				.collect(toSet());
	}

	public static String defaultValueOf(String type) {
		return type.equals(Real.toString()) ? "0.0" :
				type.equals(DataType.Integer.toString()) ? "0" :
						type.equals(DataType.Boolean.toString()) ? "false" :
								"";
	}

	public static String defaultVariabilityOf(String type) {
		return type.equals(Real.toString()) ? "continuous" :
				type.equals(DataType.Integer.toString()) ? "discrete" :
						type.equals(DataType.Boolean.toString()) ? "discrete" :
								"discrete";
	}

	static Editor getEditor(ActionEvent e) {
		if (e.getSource() instanceof Component) return getEditor((Component) e.getSource());
		return null;
	}

	public static Editor getEditor(Component component) {
		while (component != null && !(component instanceof Editor)) component = component.getParent();
		return (Editor) component;
	}

	public static Editor getEditor() {
		return getEditor(graphComponent);
	}

	public static Object valueOf(VariableSpec spec) {
		return valueOf(spec.getType(), spec.getValue());
	}

	public static Object valueOf(String type, String value) {
		return type.equals("Real") ? parseDouble(value) :
				type.equals("Integer") ? parseInt(value) :
						type.equals("Boolean") ? parseBoolean(value) :
								value;
	}

	static byte[] daccosimJarBytes() {
		try {
			return extract(FmuExporter.class.getResourceAsStream("/Matryoshka.jar"));
		} catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	static String stackToString(Throwable throwable) {
		StringWriter stack = new StringWriter();
		throwable.printStackTrace(new PrintWriter(stack));
		return stack.toString();
	}

	static Set<String> allVariables() {
		try {
			return getCellSpecs().flatMap(c -> c.allVariables().stream()).collect(toSet());
		} catch (NullPointerException e) {
			e.printStackTrace();
			return Collections.emptySet();
		}
	}

	static VariableSpec variableOf(Spec spec, String variableName) {
		return spec instanceof BlockSpec ? ((BlockSpec) spec).variable(variableName) : null;
	}

	enum SaveChangesResult {
		Saved, NotSaved, Cancelled
	}
}
