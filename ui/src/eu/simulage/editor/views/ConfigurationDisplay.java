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

import eu.simulage.daccosim.Pipeline;
import eu.simulage.editor.DaccosimDialog;
import eu.simulage.editor.Spec;
import eu.simulage.editor.specs.BlockSpec;
import eu.simulage.editor.specs.ConfigurationSpec;
import eu.simulage.editor.specs.FmuSpec;
import org.javafmi.modeldescription.v2.ModelDescription;
import org.javafmi.wrapper.Simulation;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static eu.simulage.SimulationProvider.getSimulation;
import static eu.simulage.editor.DialogUtils.*;
import static eu.simulage.editor.EditorUtils.*;
import static java.awt.GridBagConstraints.*;
import static java.awt.event.ItemEvent.SELECTED;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.*;
import static java.util.stream.IntStream.range;

public class ConfigurationDisplay extends JPanel {

	private static final String[] DECIMAL_SEPARATORS_OPT_1 = new String[]{".", ","};
	private static final String[] DECIMAL_SEPARATORS_OPT_2 = new String[]{"."};
	private final JDialog dialog;
	private final JTabbedPane tabbedPane;
	protected JLabel varToExportCount;
	protected JLabel stepperVarsCount;
	private ConfigurationSpec spec;
	private Pipeline pipeline;
	private JTextField startTime;
	private JTextField stopTime;
	private JTextField tolerance;
	private JTextField maxIter;
	private JComboBox coInitMethod;
	private JComboBox<String> methodCombo;
	private JTextField stepSize;
	private JTextField safetyFactor;
	private JCheckBox transmitDerivativesCheckBox;
	private JCheckBox exportCheckBox;
	private JComboBox<String> cellSeparator;
	private JComboBox<String> decimalSeparator;
	private JComboBox<String> interpolationComboBox;
	private JTextField interpolationValue;
	private JTable varToExport;
	private JTable pipelineTable;
	private JTable stepperVars;

	public ConfigurationDisplay(DaccosimDialog dialog, ConfigurationSpec configurationSpec) {
		this.dialog = dialog;
		this.spec = configurationSpec;
		this.pipeline = new Pipeline(configurationSpec.pipeline());
		setLayout(new GridBagLayout());
		tabbedPane = new JTabbedPane();
		tabbedPane.add("General", generalPanel());
		tabbedPane.add("Vars to export", varsToExport());
		tabbedPane.add("Stepper vars", stepperVars());
		tabbedPane.add("Pipeline", pipeline());
		tabbedPane.addChangeListener(e -> dialog.resetButton().setVisible(tabbedPane.getSelectedIndex() == 0));
		processMethodSelection(new ItemEvent(methodCombo, 1, spec.stepperMethod(), SELECTED));
		add(tabbedPane,
				new GridBagConstraints(0, 0, 1, 1, 1, 1, CENTER, BOTH, new Insets(5, 5, 5, 5), 0, 0));
		dialog.pack();
		processExportCheckBox(spec.export());
		setVisible(true);
	}

	private JPanel generalPanel() {
		JPanel panel = new JPanel(new GridBagLayout());

		panel.add(label("Start time: "),
				new GridBagConstraints(0, 0, 1, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
		panel.add(startTime = new JTextField(spec.startTime()),
				new GridBagConstraints(1, 0, 1, 1, 1, 0, CENTER, HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
		panel.add(label("Stop time: "),
				new GridBagConstraints(2, 0, 1, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(5, 10, 0, 0), 0, 0));
		panel.add(stopTime = new JTextField(spec.stopTime()),
				new GridBagConstraints(3, 0, 1, 1, 1, 0, CENTER, HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));

		panel.add(label("Co-Init", Font.BOLD, 14),
				new GridBagConstraints(0, 1, 1, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(15, 0, 0, 0), 0, 0));
		panel.add(new JSeparator(SwingConstants.HORIZONTAL),
				new GridBagConstraints(1, 1, 3, 1, 1, 0, CENTER, HORIZONTAL, new Insets(15, 5, 0, 0), 0, 0));

		panel.add(label("Method: "),
				new GridBagConstraints(0, 2, 1, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
		panel.add(coInitMethod = new JComboBox<>(new String[]{ITERATIVE_PROPAGATION, NEWTON_RAPHSON}),
				new GridBagConstraints(1, 2, 3, 1, 1, 0, CENTER, HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
		coInitMethod.setSelectedItem(spec.coInitMethod());

		panel.add(label("Tolerance: "),
				new GridBagConstraints(0, 3, 1, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
		panel.add(tolerance = new JTextField(spec.tolerance()),
				new GridBagConstraints(1, 3, 3, 1, 1, 0, CENTER, HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));

		panel.add(label("Max Iterations: "),
				new GridBagConstraints(0, 4, 1, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
		panel.add(maxIter = new JTextField(spec.maxIter()),
				new GridBagConstraints(1, 4, 3, 1, 1, 0, CENTER, HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));

		panel.add(label("Stepper", Font.BOLD, 14),
				new GridBagConstraints(0, 6, 1, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(15, 0, 0, 0), 0, 0));
		panel.add(new JSeparator(SwingConstants.HORIZONTAL),
				new GridBagConstraints(1, 6, 3, 1, 1, 0, CENTER, HORIZONTAL, new Insets(15, 5, 0, 0), 0, 0));

		panel.add(label("Method: "),
				new GridBagConstraints(0, 7, 1, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
		panel.add(methodCombo = new JComboBox<>(new String[]{CONSTANT, EULER, ADAMS}),
				new GridBagConstraints(1, 7, 3, 1, 1, 0, CENTER, HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
		methodCombo.setSelectedItem(spec.stepperMethod());
		methodCombo.addItemListener(this::processMethodSelection);

		panel.add(label("Step size: "),
				new GridBagConstraints(0, 8, 1, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
		panel.add(stepSize = new JTextField(spec.stepSize()),
				new GridBagConstraints(1, 8, 3, 1, 1, 0, CENTER, HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));

		panel.add(label("Safety factor: "),
				new GridBagConstraints(0, 10, 1, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
		panel.add(safetyFactor = new JTextField(spec.safetyFactor()),
				new GridBagConstraints(1, 10, 3, 1, 1, 0, CENTER, HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));

		panel.add(transmitDerivativesCheckBox = transmitDerivativesCheckBox(),
				new GridBagConstraints(0, 12, 1, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));

		panel.add(exportCheckBox = exportCheckBox(),
				new GridBagConstraints(0, 13, 1, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(15, 0, 0, 0), 0, 0));
		panel.add(new JSeparator(SwingConstants.HORIZONTAL),
				new GridBagConstraints(1, 13, 3, 1, 1, 0, CENTER, HORIZONTAL, new Insets(15, 5, 0, 0), 0, 0));

		panel.add(label("Cell sep: "),
				new GridBagConstraints(0, 14, 1, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(5, 10, 0, 0), 0, 0));
		panel.add(this.cellSeparator = cellSeparatorCombo(),
				new GridBagConstraints(1, 14, 3, 1, 1, 0, CENTER, HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
		panel.add(label("Decimal sep: "),
				new GridBagConstraints(0, 15, 1, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(5, 10, 0, 0), 0, 0));
		panel.add(this.decimalSeparator = new JComboBox<>(DECIMAL_SEPARATORS_OPT_1),
				new GridBagConstraints(1, 15, 3, 1, 1, 0, CENTER, HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));

		panel.add(label("Interpolation"),
				new GridBagConstraints(0, 16, 1, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(5, 10, 0, 0), 0, 0));
		panel.add(interpolationComboBox = new JComboBox<>(new String[]{"None", "Interval"}),
				new GridBagConstraints(1, 16, 1, 1, 1, 0, CENTER, HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
		panel.add(interpolationValue = new JTextField(spec.interpolationData()),
				new GridBagConstraints(2, 16, 2, 1, 1, 0, CENTER, HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
		interpolationComboBox.setSelectedItem(spec.interpolationOption());
		interpolationComboBox.addItemListener(e -> {
			if (e.getStateChange() != SELECTED) return;
			interpolationValue.setEnabled(!e.getItem().equals("None"));
		});
		interpolationValue.setEnabled(false);
		this.cellSeparator.setSelectedItem(spec.cellSeparator());
		this.decimalSeparator.setSelectedItem(spec.decimalSeparator());
		return panel;
	}

	private JComboBox<String> cellSeparatorCombo() {
		JComboBox<String> cellSeparator = new JComboBox<>(new String[]{";", ","});
		cellSeparator.addItemListener(e -> {
			if (e.getStateChange() == SELECTED && e.getItem().toString().equals(",")) {
				decimalSeparator.setModel(new DefaultComboBoxModel<>(DECIMAL_SEPARATORS_OPT_2));
			}
			if (e.getStateChange() == SELECTED && e.getItem().toString().equals(";")) {
				decimalSeparator.setModel(new DefaultComboBoxModel<>(DECIMAL_SEPARATORS_OPT_1));
			}
		});
		return cellSeparator;
	}

	private JCheckBox transmitDerivativesCheckBox() {
		JCheckBox transmitDerivatives = new JCheckBox("Transmit derivatives");
		transmitDerivatives.setSelected(spec.transmitDerivatives());
		transmitDerivatives.setFont(fontWith(Font.PLAIN, 12));
		return transmitDerivatives;
	}

	private JCheckBox exportCheckBox() {
		JCheckBox exportCheckBox = new JCheckBox("Export");
		exportCheckBox.setSelected(spec.export());
		exportCheckBox.setFont(fontWith(Font.BOLD, 14));
		exportCheckBox.addItemListener(e -> processExportCheckBox(e.getStateChange() == ItemEvent.SELECTED));
		return exportCheckBox;
	}

	private void processExportCheckBox(boolean selected) {
		cellSeparator.setEnabled(selected);
		decimalSeparator.setEnabled(selected);
		tabbedPane.setEnabledAt(1, selected);
		interpolationComboBox.setEnabled(selected);
		interpolationValue.setEnabled(selected);
	}

	private JPanel varsToExport() {
		JPanel panel = new JPanel(new GridBagLayout());
		varToExport = createVariablesToExportMap(spec.variablesToExport());
		panel.add(labelOf("Filter"),
				new GridBagConstraints(0, 0, 1, 1, 0, 0, CENTER, NONE, new Insets(0, 10, 0, 0), 0, 0));
		panel.add(filterTextField(addSorter(varToExport)),
				new GridBagConstraints(1, 0, 2, 1, 0.9, 0, CENTER, HORIZONTAL, new Insets(0, 5, 0, 10), 0, 0));
		panel.add(new JScrollPane(varToExport),
				new GridBagConstraints(0, 1, 2, 1, 1, 1, CENTER, BOTH, new Insets(5, 5, 0, 5), 0, 0));
		panel.add(withAllVaraiblesToExport(),
				new GridBagConstraints(0, 2, 2, 1, 1, 0, LINE_START, NONE, new Insets(0, 0, 0, 0), 0, 0));
		panel.add(varToExportCount = labelOf("count: " + varToExport.getModel().getRowCount() + "/" + totalVarToExport()),
				new GridBagConstraints(1, 2, 1, 1, 0.9, 0, LINE_END, NONE, new Insets(0, 0, 0, 15), 0, 0));
		return panel;
	}

	private JPanel withAllVaraiblesToExport() {
		JPanel panel = removeAddButtons((e) -> addVarToExport(), (e) -> removeVarToExport());
		JButton add = new JButton("all");
		add.setMinimumSize(new Dimension(50, 30));
		add.setMaximumSize(new Dimension(50, 30));
		add.addActionListener(e -> {
			Set<String> alreadySelected = alreadySelectedVariablesToExport();
			exportableVariables().stream().filter(v -> !alreadySelected.contains(v))
					.forEach(c -> ((DefaultTableModel) varToExport.getModel()).addRow(new String[]{c}));
			varToExportCount.setText("count: " + varToExport.getRowCount() + "/" + totalVarToExport());
		});
		panel.add(add);
		return panel;
	}

	private Set<String> alreadySelectedVariablesToExport() {
		return range(0, varToExport.getRowCount()).boxed()
				.map(i -> varToExport.getValueAt(i, 0).toString()).collect(toSet());
	}

	private int totalVarToExport() {
		return exportableVariables().size();
	}

	private JPanel pipeline() {
		JPanel panel = new JPanel(new GridBagLayout());
		pipelineTable = createPipelineTable();
		panel.add(new JScrollPane(pipelineTable),
				new GridBagConstraints(0, 0, 2, 1, 1, 1, CENTER, BOTH, new Insets(5, 5, 0, 5), 0, 0));
		return panel;
	}

	private JTable createPipelineTable() {
		DefaultTableModel model = new DefaultTableModel(new String[]{"Stage", "FMU"}, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return column == 0;
			}
		};
		JTable table = new JTable(model);
		addTooltip(table);
		addSorter(table);
		table.getTableHeader().setBackground(Color.LIGHT_GRAY);
		table.getTableHeader().setOpaque(false);
		if (pipeline.stages().isEmpty() || nodesHaveChanged())
			getCellSpecs().forEach(v -> model.addRow(new String[]{1 + "", v.label()}));
		else
			pipeline.stages().forEach(s -> s.nodes().forEach(f -> model.addRow(new String[]{s.id(), f})));
		return table;
	}

	private boolean nodesHaveChanged() {
		if (pipeline.nodesCount() != getCellSpecs().count()) return true;
		Set<String> nodes = getCellSpecs().map(Spec::label).collect(toSet());
		return !pipeline.stages().stream().allMatch(s -> nodes.containsAll(s.nodes()));
	}

	private JPanel stepperVars() {
		JPanel panel = new JPanel(new GridBagLayout());
		stepperVars = createVariablesStepperMap(spec.stepperVariables());
		panel.add(labelOf("Filter"),
				new GridBagConstraints(0, 0, 1, 1, 0, 0, CENTER, NONE, new Insets(0, 10, 0, 0), 0, 0));
		panel.add(filterTextField(addSorter(stepperVars)),
				new GridBagConstraints(1, 0, 2, 1, 0.9, 0, CENTER, HORIZONTAL, new Insets(0, 5, 0, 10), 0, 0));
		panel.add(new JScrollPane(stepperVars),
				new GridBagConstraints(0, 1, 2, 1, 1, 1, CENTER, BOTH, new Insets(5, 5, 0, 5), 0, 0));
		panel.add(removeAddButtons((e) -> addStepperVar(), (e) -> removeStepperVar()),
				new GridBagConstraints(0, 2, 2, 1, 1, 0, LINE_START, NONE, new Insets(0, 0, 0, 0), 0, 0));
		panel.add(stepperVarsCount = labelOf("count: " + stepperVars.getModel().getRowCount() + "/" + totalStepperVars()),
				new GridBagConstraints(1, 2, 1, 1, 0.9, 0, LINE_END, NONE, new Insets(0, 0, 0, 15), 0, 0));
		return panel;
	}

	private int totalStepperVars() {
		return stepperVariables().size();
	}

	private JTextField filterTextField(TableRowSorter<TableModel> tableSorter) {
		JTextField filter = new JTextField();
		filter.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				doFilter();
			}

			public void removeUpdate(DocumentEvent e) {
				doFilter();
			}

			public void insertUpdate(DocumentEvent e) {
				doFilter();
			}

			void doFilter() {
				if (filter.getText().isEmpty()) tableSorter.setRowFilter(null);
				String text = "(?i)" + filter.getText()
						.replace(".", "\\.")
						.replace("[", "\\[")
						.replace("]", "\\]")
						.replace("*", "");
				tableSorter.setRowFilter(RowFilter.regexFilter(text, 0));
				varToExportCount.setText("count: " + varToExport.getRowCount() + "/" + totalVarToExport());
			}
		});
		return filter;
	}

	private JTable createVariablesToExportMap(List<String> variablesToExport) {
		DefaultTableModel model = new DefaultTableModel(new String[]{"Variable name"}, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		JTable table = new JTable(model);
		addTooltip(table);
		table.getTableHeader().setBackground(Color.LIGHT_GRAY);
		table.getTableHeader().setOpaque(false);
		variablesToExport.forEach(v -> model.addRow(new String[]{v}));
		return table;
	}

	private JTable createVariablesStepperMap(Map<String, Double> content) {
		DefaultTableModel model = new DefaultTableModel(new String[]{"Variable name", "Tolerance"}, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return column == 1;
			}
		};
		JTable table = new JTable(model);
		addTooltip(table);
		addSorter(table);
		table.getColumn("Tolerance").setMaxWidth(70);
		table.getTableHeader().setBackground(Color.LIGHT_GRAY);
		table.getTableHeader().setOpaque(false);
		content.forEach((k, v) -> model.addRow(new String[]{k, v + ""}));
		return table;
	}

	private void addVarToExport() {
		Set<String> alreadySelected = alreadySelectedVariablesToExport();
		List<String> exportableVariables = exportableVariables().stream()
				.filter(v -> !alreadySelected.contains(v))
				.collect(Collectors.toList());
		SelectorDialog dialog = new SelectorDialog(this.dialog, exportableVariables, true);
		dialog.selectedItems().forEach(c -> ((DefaultTableModel) varToExport.getModel()).addRow(new String[]{c}));
		varToExportCount.setText("count: " + varToExport.getRowCount() + "/" + totalVarToExport());
	}

	private void removeVarToExport() {
		List<Integer> indexesToDelete = stream(varToExport.getSelectedRows()).boxed()
				.map(i -> varToExport.convertRowIndexToModel(i))
				.sorted().collect(Collectors.toList());
		Collections.reverse(indexesToDelete);
		indexesToDelete.forEach(i -> ((DefaultTableModel) varToExport.getModel()).removeRow(i));
		varToExportCount.setText("count: " + varToExport.getRowCount() + "/" + totalVarToExport());
	}


	private void addStepperVar() {
		Set<String> alreadySelected = range(0, stepperVars.getRowCount()).boxed()
				.map(i -> stepperVars.getValueAt(i, 0).toString()).collect(toSet());
		List<String> variablesForStepper = stepperVariables().stream()
				.filter(v -> !alreadySelected.contains(v))
				.collect(Collectors.toList());
		SelectorDialog dialog = new SelectorDialog(this.dialog, variablesForStepper, true);
		dialog.selectedItems()
				.forEach(c -> ((DefaultTableModel) stepperVars.getModel()).addRow(new String[]{c, "1.0E-5"}));
		stepperVarsCount.setText("count: " + stepperVars.getModel().getRowCount() + "/" + totalStepperVars());
	}

	private void removeStepperVar() {
		List<Integer> indexesToDelete = stream(stepperVars.getSelectedRows()).boxed()
				.map(i -> stepperVars.convertRowIndexToModel(i))
				.sorted().collect(Collectors.toList());
		Collections.reverse(indexesToDelete);
		indexesToDelete.forEach(i -> ((DefaultTableModel) stepperVars.getModel()).removeRow(i));
		stepperVarsCount.setText("count: " + stepperVars.getModel().getRowCount() + "/" + totalStepperVars());
	}

	private List<String> exportableVariables() {
		return getCellSpecs().map(BlockSpec::allVariables).flatMap(Collection::stream).collect(toList());
	}

	private List<String> stepperVariables() {
		Set<String> result = new LinkedHashSet<>();
		getCellSpecs().filter(s -> s instanceof FmuSpec).map(s -> (FmuSpec) s).forEach(s -> {
			Simulation simulation = getSimulation(s.file());
			if (simulation == null) return;
			ModelDescription modelDescription = (ModelDescription) simulation.getModelDescription();
			Set<String> variables = new HashSet<>();
			stream(modelDescription.getModelVariables()).forEach(v -> variables.add(v.getName()));
			stream(modelDescription.getModelVariables())
					.filter(v -> variables.contains("der(" + v.getName() + ")"))
					.forEach(v -> result.add(s.label() + "." + v.getName()));
		});
		return new ArrayList<>(result);
	}

	private void processMethodSelection(ItemEvent e) {
		if (e.getStateChange() != SELECTED) return;
		stepperVars.getTableHeader().getColumnModel().getColumn(1).setHeaderValue("Tolerance");
		if (e.getItem().toString().equals(CONSTANT)) {
			tabbedPane.setEnabledAt(2, false);
			stream(new Component[]{stepSize}).forEach(c -> c.setEnabled(true));
			stream(new Component[]{safetyFactor}).forEach(c -> c.setEnabled(false));
		}
		if (e.getItem().toString().equals(EULER)) {
			prepareStepperVariablesTab();
			stream(new Component[]{safetyFactor})
					.forEach(c -> c.setEnabled(true));
			stream(new Component[]{stepSize}).forEach(c -> c.setEnabled(false));
		}
		if (e.getItem().toString().equals(ADAMS)) {
			prepareStepperVariablesTab();
			stream(new Component[]{safetyFactor})
					.forEach(c -> c.setEnabled(true));
			stream(new Component[]{stepSize}).forEach(c -> c.setEnabled(false));
		}
	}

	private void prepareStepperVariablesTab() {
		tabbedPane.setEnabledAt(2, true);
		if (spec.stepperVariables().size() > 0) return;
		while (stepperVars.getRowCount() > 0) ((DefaultTableModel) stepperVars.getModel()).removeRow(0);
		stepperVariables().forEach(v -> ((DefaultTableModel) stepperVars.getModel()).addRow(new String[]{v, "1.0E-5"}));
	}

	@SuppressWarnings("ConstantConditions")
	public void saveForm() {
		if (!checkIsDouble("startTime", startTime)) return;
		if (!checkIsDouble("stopTime", stopTime)) return;
		if (!checkStopAndStartTimeAreCorrect()) return;
		if (!checkIsDouble("tolerance", tolerance)) return;
		if (!checkIsInt("maxIter", maxIter)) return;
		if (!checkCorrectStepSize()) return;
		if (!checkSafetyFactor()) return;
		if (!checkIsDouble("interpolation value", interpolationValue)) return;
		if (!checkVariableStepperList()) return;
		if (!checkPipeline()) return;
		spec.startTime(startTime.getText());
		spec.stopTime(stopTime.getText());
		spec.coInitMethod(coInitMethod.getSelectedItem().toString());
		spec.tolerance(tolerance.getText());
		spec.maxIter(maxIter.getText());
		spec.stepperMethod(methodComboItem());
		spec.stepSize(stepSize.getText());
		spec.safetyFactor(safetyFactor.getText());
		spec.order("3");
		spec.transmitDerivatives(transmitDerivativesCheckBox.isSelected());
		spec.export(exportCheckBox.isSelected());
		spec.cellSeparator(cellSeparator.getSelectedItem().toString());
		spec.decimalSeparator(decimalSeparator.getSelectedItem().toString());
		spec.interpolationOption(interpolationComboBox.getSelectedItem().toString());
		spec.interpolationData(interpolationValue.getText());
		spec.variablesToExport(range(0, varToExport.getModel().getRowCount()).boxed()
				.map(i -> varToExport.getModel().getValueAt(i, 0).toString()).collect(Collectors.toList()));
		spec.stepperVariables(spec.stepperMethod().equals(CONSTANT) ? emptyMap() : range(0, stepperVars.getModel().getRowCount()).boxed()
				.collect(toMap(i -> stepperVars.getModel().getValueAt(i, 0).toString(), i -> Double.parseDouble(stepperVars.getModel().getValueAt(i, 1).toString()))));
		Pipeline pipeline = new Pipeline();
		range(0, pipelineTable.getModel().getRowCount()).boxed()
				.map(i -> new String[]{(String) pipelineTable.getModel().getValueAt(i, 0), (String) pipelineTable.getModel().getValueAt(i, 1)})
				.sorted(Comparator.comparing(s -> parseInt(s[0])))
				.forEach(s -> pipeline.stage(parseInt(s[0])).add(s[1]));
		configuration().pipeline(pipeline.toString());
		getEditor().setModified(true);
		dialog.dispose();
	}

	private boolean checkPipeline() {
		for (int i = 0; i < pipelineTable.getModel().getRowCount(); i++) {
			try {
				parseInt((String) pipelineTable.getModel().getValueAt(i, 0));
			} catch (NumberFormatException e) {
				showErrorDialog(dialog, "The row " + (i + 1) + " contains a not valid number in stage column");
				return false;
			}
		}
		List<String[]> rows = range(0, pipelineTable.getModel().getRowCount()).boxed()
				.map(i -> new String[]{(String) pipelineTable.getModel().getValueAt(i, 0), (String) pipelineTable.getModel().getValueAt(i, 1)})
				.sorted(Comparator.comparing(s -> parseInt(s[0])))
				.collect(toList());
		int stage = rows.isEmpty() ? 1 : parseInt(rows.get(0)[0]);
		if (stage != 1) {
			showErrorDialog(dialog, "First stage must be 1");
			return false;
		}
		for (String[] row : rows) {
			int nextStage = parseInt(row[0]);
			if (stage == nextStage || stage + 1 == nextStage) {
				stage = nextStage;
				continue;
			}
			showErrorDialog(dialog, "The stages must be consecutive");
			return false;
		}
		return true;
	}

	private boolean checkCorrectStepSize() {
		if (!checkIsDouble("stepSize", stepSize)) return false;
		if (parseDouble(stepSize.getText()) < 1e-200) {
			showErrorDialog(dialog, "Step size must be greater than 0");
			return false;
		}
		return true;
	}

	private boolean checkSafetyFactor() {
		if (!safetyFactor.isEnabled()) return true;
		if (!checkIsDouble("safetyFactor", safetyFactor)) return false;
		double safetyFactor = parseDouble(this.safetyFactor.getText());
		if (safetyFactor < 0.8 || safetyFactor > 0.9) {
			showErrorDialog(dialog, "Safety factor must be between 0.8 and 0.9");
			return false;
		}
		return true;
	}

	private boolean checkVariableStepperList() {
		if (CONSTANT.equals(methodComboItem())) return true;
		if (stepperVars.getRowCount() == 0) {
			showErrorDialog(dialog, "List for stepper variables is empty. It must be filled when a variable stepper is selected.");
			return false;
		}
		for (int i = 0; i < stepperVars.getRowCount(); i++) {
			if (isDouble(stepperVars.getValueAt(i, 1))) continue;
			showErrorDialog(dialog, "Tolerance must be a double number. " + stepperVars.getValueAt(i, 1) + " is not valid.");
			return false;
		}
		return true;
	}

	private String methodComboItem() {
		return methodCombo.getSelectedItem() != null ? methodCombo.getSelectedItem().toString() : null;
	}

	private boolean checkIsDouble(String fieldName, JTextField field) {
		if (!field.isEnabled() || isDouble(field.getText())) return true;
		showErrorDialog(dialog, "The field " + fieldName + " must be a real number");
		return false;
	}

	private boolean checkIsInt(String fieldName, JTextField field) {
		if (!field.isEnabled() || isInteger(field.getText())) return true;
		showErrorDialog(dialog, "The field " + fieldName + " must be an integer number");
		return false;
	}

	private boolean checkStopAndStartTimeAreCorrect() {
		double startTime = parseDouble(this.startTime.getText());
		double stopTime = parseDouble(this.stopTime.getText());
		if (stopTime < startTime) {
			showErrorDialog(dialog, "Stop time cannot be smaller than the start time");
			return false;
		}
		return true;
	}


	private JLabel label(String text) {
		return label(text, Font.PLAIN, 12);
	}

	private JLabel label(String text, int style, int size) {
		JLabel jLabel = new JLabel(text);
		jLabel.setFont(fontWith(style, size));
		jLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		return jLabel;
	}

	private Font fontWith(int style, int size) {
		return new Font("arial", style, size);
	}

	public void reset() {
		ConfigurationSpec spec = new ConfigurationSpec();
		startTime.setText(spec.startTime());
		stopTime.setText(spec.stopTime());
		tolerance.setText(spec.tolerance());
		maxIter.setText(spec.maxIter());
		methodCombo.setSelectedItem(spec.stepperMethod());
		stepSize.setText(spec.stepSize());
		safetyFactor.setText(spec.safetyFactor());
		exportCheckBox.setSelected(spec.export());
		cellSeparator.setSelectedItem(spec.cellSeparator());
		decimalSeparator.setSelectedItem(spec.decimalSeparator());
	}
}

