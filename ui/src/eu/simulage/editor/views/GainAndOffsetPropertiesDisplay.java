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

import eu.simulage.daccosim.DataType;
import eu.simulage.editor.specs.GainSpec;
import eu.simulage.editor.specs.OffsetSpec;
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

public class GainAndOffsetPropertiesDisplay extends JPanel {

	private final JDialog dialog;
	private final OperatorSpec spec;
	private final JTextField label;
	private final JTextField value;
	private final JComboBox<String> type;
	private JTable channels;

	public GainAndOffsetPropertiesDisplay(JDialog dialog, OperatorSpec spec) {
		this.dialog = dialog;
		this.spec = spec;
		setLayout(new GridBagLayout());
		add(label("Label: "),
				new GridBagConstraints(0, 0, 1, 1, 0, 0, LINE_START, NONE, new Insets(0, 10, 0, 0), 0, 0));
		add(label = new JTextField(this.spec.getLabel().isEmpty() ? "" : this.spec.getLabel()),
				new GridBagConstraints(1, 0, 1, 1, 1, 0, CENTER, HORIZONTAL, new Insets(0, 5, 0, 10), 0, 0));

		add(label("Type: "),
				new GridBagConstraints(0, 1, 1, 1, 0, 0, LINE_START, NONE, new Insets(5, 10, 0, 0), 0, 0));
		add(type = operableTypesComboBox(),
				new GridBagConstraints(1, 1, 1, 1, 1, 0, CENTER, HORIZONTAL, new Insets(5, 5, 0, 10), 0, 0));
		type.setSelectedItem(this.spec.getType());

		add(label((spec instanceof GainSpec ? "Gain" : "Offset") + " value: "),
				new GridBagConstraints(0, 2, 1, 1, 0, 0, LINE_START, NONE, new Insets(5, 10, 0, 0), 0, 0));
		add(value = new JTextField(this.spec.getLabel()),
				new GridBagConstraints(1, 2, 1, 1, 1, 0, CENTER, HORIZONTAL, new Insets(5, 5, 0, 10), 0, 0));
		value.setText(spec instanceof GainSpec ? ((GainSpec) spec).getValue() : ((OffsetSpec) spec).getValue());

		add(channelList(),
				new GridBagConstraints(0, 3, 2, 1, 1, 1, CENTER, BOTH, new Insets(10, 10, 0, 10), 0, 0));

		add(removeAddButtons((e) -> addChannel(), (e) -> removeChannel()),
				new GridBagConstraints(0, 4, 2, 1, 0, 0, LINE_START, NONE, new Insets(0, 5, 0, 0), 0, 0));

		setVisible(true);
	}

	private Component channelList() {
		channels = new JTable(new DefaultTableModel(new String[]{"Channels"}, 0));
		addTooltip(channels);
		channels.getTableHeader().setBackground(Color.LIGHT_GRAY);
		channels.getTableHeader().setOpaque(false);
		spec.getInputs().forEach(i -> ((DefaultTableModel) channels.getModel()).addRow(new String[]{i.getName().substring(0, i.getName().length() - 3)}));
		JScrollPane pane = new JScrollPane(channels);
		pane.setMinimumSize(new Dimension(400, 125));
		pane.setMaximumSize(new Dimension(400, 125));
		return pane;
	}

	private void addChannel() {
		((DefaultTableModel) channels.getModel()).addRow(new String[]{"channel_" + channels.getRowCount()});
	}

	private void removeChannel() {
		int[] selectedRows = channels.getSelectedRows();
		for (int i = selectedRows.length - 1; i >= 0; i--)
			((DefaultTableModel) channels.getModel()).removeRow(selectedRows[i]);
	}


	private boolean checkNames() {
		Set<String> names = new HashSet<>();
		for (int i = 0; i < channels.getRowCount(); i++) {
			if (names.contains(nameAt(channels, i))) {
				showErrorDialog(dialog, "Channel " + nameAt(channels, i) + " is repeated");
				return false;
			} else names.add(nameAt(channels, i));
		}
		return true;
	}

	@SuppressWarnings("ConstantConditions")
	public void saveForm() {
		if (!checkers.get(DataType.valueOf(type.getSelectedItem().toString())).check(value.getText())) {
			showErrorDialog(dialog, "Value (" + value.getText() + ") not valid for its type (" + type.getSelectedItem().toString() + ")");
			return;
		}
		if (!checkNames()) return;
		String previousLabel = spec.getLabel();
		if (!label.getText().isEmpty()) spec.setLabel(label.getText());
		spec.setType(type.getSelectedItem().toString());
		if (spec instanceof GainSpec) ((GainSpec) spec).setValue(value.getText());
		else ((OffsetSpec) spec).setValue(value.getText());
		spec.getInputs().clear();
		spec.getOutputs().clear();
		range(0, channels.getRowCount())
				.forEach(i -> {
					spec.getInputs().add(new VariableSpec(nameAt(channels, i) + "_in", spec.getType(), defaultVariabilityOf(spec.getType()), defaultValueOf(spec.getType())));
					spec.getOutputs().add(new VariableSpec(nameAt(channels, i) + "_out", spec.getType(), defaultVariabilityOf(spec.getType()), defaultValueOf(spec.getType())));
				});
		removeConnectionsAndOutputs(spec);
		updateVariablesToExport(previousLabel, spec.getLabel());
		getEditor().setModified(true);
		dialog.dispose();
	}

	private String nameAt(JTable channels, int row) {
		return channels.getValueAt(row, 0).toString();
	}

	private JLabel label(String text) {
		JLabel jLabel = new JLabel(text);
		jLabel.setFont(new Font("arial", Font.BOLD, 12));
		jLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		jLabel.setPreferredSize(new Dimension(80, 20));
		return jLabel;
	}

}

