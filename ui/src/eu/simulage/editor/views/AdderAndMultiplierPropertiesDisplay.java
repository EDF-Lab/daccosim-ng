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

import eu.simulage.editor.specs.OperatorSpec;
import eu.simulage.editor.specs.VariableSpec;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

import static eu.simulage.editor.DialogUtils.*;
import static eu.simulage.editor.EditorUtils.*;
import static java.awt.GridBagConstraints.*;
import static java.util.stream.IntStream.range;

public class AdderAndMultiplierPropertiesDisplay extends JPanel {

	private final JDialog dialog;
	private final OperatorSpec spec;
	private final JTextField label;
	private final JComboBox<String> type;
	private JTable inputs;

	public AdderAndMultiplierPropertiesDisplay(JDialog dialog, OperatorSpec spec) {
		this.dialog = dialog;
		this.spec = spec;
		setLayout(new GridBagLayout());

		add(labelOf("Label: "),
				new GridBagConstraints(0, 0, 1, 1, 0, 0, LINE_START, NONE, new Insets(0, 10, 0, 0), 0, 0));
		add(label = new JTextField(this.spec.getLabel().isEmpty() ? "" : this.spec.getLabel()),
				new GridBagConstraints(1, 0, 1, 1, 1, 0, CENTER, HORIZONTAL, new Insets(0, 5, 0, 10), 0, 0));

		add(labelOf("Type: "),
				new GridBagConstraints(0, 1, 1, 1, 0, 0, LINE_START, NONE, new Insets(10, 10, 0, 0), 0, 0));
		add(type = operableTypesComboBox(),
				new GridBagConstraints(1, 1, 1, 1, 0, 0, CENTER, HORIZONTAL, new Insets(10, 5, 0, 10), 0, 0));
		type.setSelectedItem(this.spec.getType());

		add(inputList(),
				new GridBagConstraints(0, 2, 2, 1, 1, 1, CENTER, BOTH, new Insets(10, 10, 0, 10), 0, 0));

		add(removeAddButtons((e) -> addInput(), (e) -> removeInput()),
				new GridBagConstraints(0, 3, 2, 1, 0, 0, LINE_START, NONE, new Insets(0, 5, 0, 0), 0, 0));
		setVisible(true);
	}

	private Component inputList() {
		inputs = new JTable(new DefaultTableModel(new String[]{"Inputs"}, 0));
		addTooltip(inputs);
		inputs.getTableHeader().setBackground(Color.LIGHT_GRAY);
		inputs.getTableHeader().setOpaque(false);
		spec.getInputs().forEach(i -> ((DefaultTableModel) inputs.getModel()).addRow(new String[]{i.getName()}));
		JScrollPane pane = new JScrollPane(inputs);
		pane.setMinimumSize(new Dimension(400, 125));
		pane.setMaximumSize(new Dimension(400, 125));
		return pane;
	}

	private void addInput() {
		int rowCount = 0;
		String newVarName = "input_" + rowCount++;
		while (tableContains(newVarName)) newVarName = "input_" + rowCount++;
		((DefaultTableModel) inputs.getModel()).addRow(new String[]{newVarName});
	}

	private boolean tableContains(String newVarName) {
		return range(0, inputs.getRowCount()).anyMatch(i -> inputs.getValueAt(i, 0).equals(newVarName));
	}

	private void removeInput() {
		while (inputs.getSelectedRow() != -1 && inputs.getRowCount() > 2)
			((DefaultTableModel) inputs.getModel()).removeRow(inputs.getSelectedRow());
	}


	private boolean checkNames() {
		Set<String> names = new HashSet<>();
		for (int i = 0; i < inputs.getRowCount(); i++) {
			if (names.contains(nameAt(inputs, i))) {
				showErrorDialog(dialog, "Input " + nameAt(inputs, i) + " is repeated");
				return false;
			} else names.add(nameAt(inputs, i));
		}
		return true;
	}


	private String nameAt(JTable channels, int row) {
		return channels.getValueAt(row, 0).toString();
	}

	@SuppressWarnings("ConstantConditions")
	public void saveForm() {
		if (!checkNames()) return;
		spec.getInputs().clear();
		String previousLabel = spec.getLabel();
		if (!label.getText().isEmpty()) spec.setLabel(label.getText());
		spec.setType(type.getSelectedItem().toString());
		range(0, inputs.getRowCount())
				.forEach(i -> spec.getInputs().add(new VariableSpec(
						inputs.getValueAt(i, 0).toString(),
						spec.getType(),
						defaultVariabilityOf(spec.getType()),
						defaultValueOf(spec.getType()))));
		removeConnectionsAndOutputs(spec);
		updateVariablesToExport(previousLabel, spec.getLabel());
		getEditor().setModified(true);
		dialog.dispose();
	}

}

