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

package eu.simulage.editor.views;

import eu.simulage.daccosim.DaccosimUtils;
import eu.simulage.daccosim.DataType;
import eu.simulage.editor.EditorUtils;
import eu.simulage.editor.specs.FmuSpec;
import eu.simulage.editor.specs.VariableSpec;
import org.javafmi.modeldescription.ScalarVariable;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static eu.simulage.SimulationProvider.getSimulation;
import static eu.simulage.SimulationProvider.removeSimulation;
import static eu.simulage.editor.DialogUtils.*;
import static eu.simulage.editor.EditorUtils.*;
import static java.awt.GridBagConstraints.*;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.range;

public class FmuPropertiesDisplay extends JPanel {

	private static final String UNDEFINED = "undefined";
	private static final String BEFORE = "Before";
	private static final String IN_INIT = "In Init";
	private static File previousDir = new File(".");
	private final FmuSpec spec;
	private final JDialog dialog;
	private final JTextField label;
	private final JCheckBox staticCheckBox;
	private final JLabel count;
	private JTable varsInit;
	private boolean invalid = false;

	public FmuPropertiesDisplay(JDialog dialog, FmuSpec spec) {
		this.dialog = dialog;
		this.spec = spec;
		controlCloseButton(dialog);
		setLayout(new GridBagLayout());
		add(labelOf("Label:"),
				new GridBagConstraints(0, 0, 1, 1, 0, 0.025, LINE_START, NONE, new Insets(0, 10, 0, 0), 0, 0));
		add(label = new JTextField(this.spec.getLabel().isEmpty() ? "" : this.spec.getLabel()),
				new GridBagConstraints(1, 0, 1, 1, 0.9, 0.025, CENTER, HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
		add(loadButton(),
				new GridBagConstraints(2, 0, 1, 1, 0, 0.025, LINE_END, NONE, new Insets(0, 0, 0, 10), 0, 0));
		add(staticCheckBox = staticCheckBox(),
				new GridBagConstraints(3, 0, 1, 1, 0, 0.025, LINE_END, NONE, new Insets(0, 0, 0, 10), 0, 0));

		add(variableInitTable(),
				new GridBagConstraints(0, 1, 4, 1, 1, 0.95, PAGE_START, BOTH, new Insets(10, 10, 0, 10), 0, 0));

		add(count = labelOf(counterText()),
				new GridBagConstraints(2, 2, 2, 1, 0.025, 0, LINE_END, NONE, new Insets(0, 0, 0, 15), 0, 0));

		add(removeAddButtons((e) -> addVariable(), (e) -> removeVariable()),
				new GridBagConstraints(0, 3, 4, 1, 1, 0.025, LINE_START, NONE, new Insets(0, 5, 0, 5), 0, 0));

		dialog.pack();
		setVisible(true);
		SwingUtilities.invokeLater(() -> {
			if (spec.getFilename().isEmpty() || !spec.file().exists()) openFileChooser();
		});
	}

	private String counterText() {
		if (spec.fileName().isEmpty()) return "count 0/0";
		return "count: " + (spec.getBeforeInitValues().size() + spec.getInInitValues().size()) + "/" + (inInitVariables().size() + beforeInitVariables().size());
	}

	private JCheckBox staticCheckBox() {
		return new JCheckBox("static", spec.isStatic_());
	}

	private JButton loadButton() {
		JButton button = new JButton("...");
		button.addActionListener(e1 -> openFileChooser());
		return button;
	}

	private List<String> beforeInitVariables() {
		return stream(fmuVariables())
				.filter(this::isToBeSetBeforeInit)
				.filter(m -> !m.getName().equals(DaccosimUtils.SysVarResetFromMaster))
				.map(ScalarVariable::getName)
				.collect(toList());
	}

	private boolean isToBeSetBeforeInit(ScalarVariable m) {
		return ((m.getVariability().equalsIgnoreCase("fixed") || m.getVariability().equalsIgnoreCase("tunable")) && m.getCausality().equalsIgnoreCase("parameter"))
				|| ((m.getVariability().equalsIgnoreCase("fixed") || m.getVariability().equalsIgnoreCase("tunable")) && (m.getCausality().equalsIgnoreCase("calculatedParameter") || m.getCausality().equalsIgnoreCase("local")) && m.getInitial().equalsIgnoreCase("approx"))
				|| ((m.getVariability().equalsIgnoreCase("discrete") || m.getVariability().equalsIgnoreCase("continuous")) && m.getCausality().equalsIgnoreCase("local") && (m.getInitial().equalsIgnoreCase("exact") || m.getInitial().equalsIgnoreCase("approx")));
	}

	private ScalarVariable[] fmuVariables() {
		return requireNonNull(getSimulation(spec.file())).getModelDescription().getModelVariables();
	}

	private void addVariable() {
		if (spec.getFilename().isEmpty() || !spec.file().exists()) {
			EditorUtils.showErrorDialog(this, "Please, select an FMU first");
			return;
		}
		Set<String> alreadySelected = range(0, varsInit.getRowCount()).boxed()
				.map(this::nameAt).collect(Collectors.toSet());

		List<String> variables = new ArrayList<>(beforeInitVariables());
		variables.addAll(inInitVariables());
		variables = variables.stream().filter(v -> !alreadySelected.contains(v)).collect(Collectors.toList());

		SelectorDialog display = new SelectorDialog(dialog, variables, true);
		addNewVariables(display);
		count.setText(counterText());
	}

	private void addNewVariables(SelectorDialog display) {
		display.selectedItems().stream().map(this::fmuVariable)
				.forEach(v -> (varsInitModel()).addRow(new Object[]{v.getTypeName(),
						isToBeSetBeforeInit(v) ? BEFORE : IN_INIT,
						v.getName(),
						v.getStart() == null ? UNDEFINED : v.getStart().toString()}));
	}

	private void removeVariable() {
		List<Integer> indexesToDelete = stream(varsInit.getSelectedRows()).boxed()
				.map(i -> varsInit.convertRowIndexToModel(i))
				.sorted().collect(Collectors.toList());
		Collections.reverse(indexesToDelete);
		indexesToDelete.forEach(i -> ((DefaultTableModel) varsInit.getModel()).removeRow(i));
		count.setText(counterText());
	}

	private DefaultTableModel varsInitModel() {
		return (DefaultTableModel) varsInit.getModel();
	}

	private void controlCloseButton(JDialog dialog) {
		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				invalid = spec.getFilename().isEmpty();
			}
		});
	}

	@SuppressWarnings("SpellCheckingInspection")
	private void openFileChooser() {
		JFileChooser chooser = new JFileChooser(directory());
		chooser.setDialogTitle(spec.getFilename().isEmpty() ? "Choose an FMU" : "Choose the FMU for " + spec.getLabel());
		chooser.setFileFilter(new FileNameExtensionFilter("Functional-Mockup Unit (.fmu)", "fmu"));
		process(chooser.showDialog(dialog, "Ok"), chooser);
	}

	private File directory() {
		return !getEditor().isFileUntitled() ? getEditor().getWorkingDirectory() : previousDir;
	}

	private void process(int status, JFileChooser chooser) {
		if (status == JFileChooser.APPROVE_OPTION) {
			File selectedFile = chooser.getSelectedFile();
			previousDir = selectedFile.getParentFile();
			spec.setFilename(getEditor().getFile().addFmu(selectedFile).getName());
			if (label.getText().isEmpty()) label.setText(selectedFile.getName().replace(".fmu", "").replace(".", "_"));
			doInBackground(dialog, () -> checkFmu(spec.file()));
		}
	}

	@SuppressWarnings("ConstantConditions")
	private void checkFmu(File selectedFile) {
		final String[] out = {""};
		PrintStream oldOut = System.out;
		System.setOut(new PrintStream(new OutputStream() {
			@Override
			public void write(int b) {
				out[0] += (char) b;
			}
		}));
		try {
			getSimulation(selectedFile);
			checkInitVariables();
			count.setText(counterText());
		} catch (Throwable e) {
			showErrorDialog(dialog, e.getMessage() + (out[0].isEmpty() ? "" : ". This is the output of the FMU: \n" + out[0]));
			getEditor().getFile().removeFmu(spec.fileName());
			spec.setFilename("");
			label.setText("");
			clearTable(varsInitModel());
			removeSimulation(selectedFile);
		}
		System.setOut(oldOut);
	}

	private void checkInitVariables() {
		removeInvalidRows(beforeInitVariables(), tableIndexes(BEFORE));
		removeInvalidRows(inInitVariables(), tableIndexes(IN_INIT));
	}

	private void removeInvalidRows(List<String> existingVariables, IntStream indexes) {
		List<Integer> toDelete = indexes.boxed().filter(i -> !existingVariables.contains(nameAt(i))).collect(toList());
		Collections.reverse(toDelete);
		toDelete.forEach(i -> varsInitModel().removeRow(i));
	}

	private List<String> inInitVariables() {
		return stream(fmuVariables())
				.filter(m -> m.getCausality().equalsIgnoreCase("input"))
				.filter(m -> !m.getName().equals(DaccosimUtils.SysVarResetFromMaster))
				.map(ScalarVariable::getName)
				.collect(toList());
	}

	private void showVars() {
		if (spec.getFilename().isEmpty() || !spec.file().exists()) return;
		DefaultTableModel model = varsInitModel();
		clearTable(model);
		spec.getBeforeInitValues().stream().collect(toMap(VariableSpec::getName, VariableSpec::getValue))
				.forEach((k, v) -> model.addRow(new Object[]{typeOf(k), BEFORE, k, v == null ? UNDEFINED : v}));
		spec.getInInitValues().stream().collect(toMap(VariableSpec::getName, VariableSpec::getValue))
				.forEach((k, v) -> model.addRow(new Object[]{typeOf(k), IN_INIT, k, v == null ? UNDEFINED : v}));
	}

	private String typeOf(String name) {
		return fmuVariable(name).getTypeName();
	}

	private void clearTable(DefaultTableModel model) {
		while (model.getRowCount() > 0) model.removeRow(0);
	}

	private JScrollPane variableInitTable() {
		varsInit = new JTable(new DefaultTableModel(new String[]{"Type", "Mode", "Variable name", "Init value"}, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return column > 2;
			}
		});
		addTooltip(varsInit);
		addSorter(varsInit);
		varsInit.getTableHeader().setBackground(Color.LIGHT_GRAY);
		varsInit.getTableHeader().setOpaque(false);
		showVars();
		return new JScrollPane(varsInit);
	}

	private ScalarVariable fmuVariable(String varName) {
		return requireNonNull(getSimulation(spec.file())).getModelDescription().getModelVariable(varName);
	}

	public void cancelAction() {
		invalid = true;
		SwingUtilities.invokeLater(dialog::dispose);
	}

	public boolean saveForm() {
		if (spec.getFilename().isEmpty() || !spec.file().exists()) {
			showErrorDialog(dialog, "An FMU must be chosen");
			return false;
		}
		if (!startValuesAreCorrect()) return false;
		if (labelExists()) return false;
		String previousLabel = spec.getLabel();
		spec.setLabel(!this.label.getText().isEmpty() ? this.label.getText() : spec.fileName());
		spec.getBeforeInitValues().clear();
		tableIndexes(BEFORE).filter(i -> startValueChanged(nameAt(i), typeAt(i), valueAt(i)))
				.forEach(i -> spec.getBeforeInitValues().add(new VariableSpec(nameAt(i), typeAt(i), "discrete", valueAt(i))));
		spec.getInInitValues().clear();
		tableIndexes(IN_INIT).filter(i -> startValueChanged(nameAt(i), typeAt(i), valueAt(i)))
				.forEach(i -> spec.getInInitValues().add(new VariableSpec(nameAt(i), typeAt(i), "discrete", valueAt(i))));
		removeConnectionsAndOutputs(spec);
		updateVariablesToExport(previousLabel, spec.getLabel());
		spec.setStatic_(staticCheckBox.isSelected());
		getEditor().setModified(true);
		dialog.dispose();
		return true;
	}

	private IntStream tableIndexes(String type) {
		return range(0, varsInit.getRowCount()).filter(i -> modeAt(i).equals(type));
	}

	private boolean labelExists() {
		long count = EditorUtils.labelCount(label.getText(), spec);
		if (count > 0) showErrorDialog(dialog, "Label " + label.getText() + " already in use");
		return count > 0;
	}

	@SuppressWarnings("ConstantConditions")
	private boolean startValueChanged(String name, String type, String value) {
		if (value.equals(UNDEFINED)) return false;
		ScalarVariable variable = getSimulation(spec.file()).getModelDescription().getModelVariable(name);
		return variable.getStart() == null || !variable.getStart().equals(valueOf(type, value));
	}

	private boolean startValuesAreCorrect() {
		for (int i = 0; i < varsInit.getRowCount(); i++) {
			if (checkers.get(DataType.valueOf(typeAt(i))).check(valueAt(i))) continue;
			showErrorDialog(dialog, "Value of " + nameAt(i) + " must be " + typeAt(i));
			return false;
		}
		return true;
	}

	public boolean isComplete() {
		return !invalid && spec.getFilename() != null;
	}


	private String typeAt(int row) {
		return varsInit.getValueAt(row, 0).toString();
	}

	private String modeAt(int row) {
		String value = varsInit.getValueAt(row, 1).toString();
		return value.equals(UNDEFINED) ? defaultValueOf(typeAt(row)) : value;
	}

	private String nameAt(int row) {
		return varsInit.getValueAt(row, 2).toString();
	}

	private String valueAt(int row) {
		String value = varsInit.getValueAt(row, 3).toString();
		return value.equals(UNDEFINED) ? defaultValueOf(typeAt(row)) : value;
	}

}

