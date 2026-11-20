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
import eu.simulage.editor.DaccosimDialog;
import eu.simulage.editor.specs.FmuSpec;
import eu.simulage.editor.specs.VariableSpec;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static eu.simulage.editor.DialogUtils.addTooltip;
import static eu.simulage.editor.DialogUtils.removeAddButtons;
import static eu.simulage.editor.EditorUtils.getEditor;
import static java.awt.GridBagConstraints.*;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

public class FlowVariablesDisplay extends JPanel {

	private final FmuSpec spec;
	private DaccosimDialog dialog;
	private JTable input;
	private JTable output;

	public FlowVariablesDisplay(DaccosimDialog dialog, FmuSpec spec) {
		this.dialog = dialog;
		this.spec = spec;

		setLayout(new GridBagLayout());
		add(new JScrollPane(input = buildTable("Inputs", this.spec.getInputFlowVariables())),
				new GridBagConstraints(0, 0, 1, 1, 1, 1, CENTER, BOTH, new Insets(10, 10, 0, 10), 0, 0));
		add(new JScrollPane(output = buildTable("Outputs", this.spec.getOutputFlowVariables())),
				new GridBagConstraints(1, 0, 1, 1, 1, 1, CENTER, BOTH, new Insets(10, 10, 0, 10), 0, 0));
		add(removeAddButtons((e) -> addInputs(), (e) -> removeInputs()),
				new GridBagConstraints(0, 1, 1, 1, 0, 0, CENTER, NONE, new Insets(0, 5, 0, 0), 0, 0));
		add(removeAddButtons((e) -> addOutputs(), (e) -> removeOutputs()),
				new GridBagConstraints(1, 1, 1, 1, 0, 0, CENTER, NONE, new Insets(0, 5, 0, 0), 0, 0));
	}

	private void addInputs() {
		addVariablesIn(this.input, spec.getInputs());
	}

	private void removeInputs() {
		removeSelectedRowsIn(this.input);
	}

	private void addOutputs() {
		addVariablesIn(this.output, spec.getOutputs());
	}

	private void removeOutputs() {
		removeSelectedRowsIn(this.output);
	}

	private void addVariablesIn(JTable table, List<VariableSpec> allVariables) {
		Set<String> alreadySelected = range(0, table.getRowCount()).boxed()
				.map(i -> table.getValueAt(i, 0).toString()).collect(Collectors.toSet());

		List<String> variables = allVariables.stream()
				.filter(v -> v.getType().equals(DataType.Real.name()))
				.map(VariableSpec::getName)
				.filter(v -> !alreadySelected.contains(v))
				.collect(toList());

		SelectorDialog display = new SelectorDialog(dialog, variables, true);
		display.selectedItems().forEach(i -> ((DefaultTableModel) table.getModel()).addRow(new String[]{i}));
	}

	private void removeSelectedRowsIn(JTable table) {
		List<Integer> indexesToDelete = stream(table.getSelectedRows()).boxed()
				.map(table::convertRowIndexToModel)
				.sorted().collect(Collectors.toList());
		Collections.reverse(indexesToDelete);
		indexesToDelete.forEach(i -> ((DefaultTableModel) table.getModel()).removeRow(i));
	}

	private JTable buildTable(String header, List<String> content) {
		JTable table = new JTable(new DefaultTableModel(new String[]{header}, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		});
		addTooltip(table);
		table.getTableHeader().setBackground(Color.LIGHT_GRAY);
		table.getTableHeader().setOpaque(false);
		content.forEach(v -> ((DefaultTableModel) table.getModel()).addRow(new String[]{v}));
		return table;
	}

	public void cancelAction() {
		SwingUtilities.invokeLater(dialog::dispose);
	}

	public void saveForm() {
		spec.getFlowVariables().clear();
		range(0, input.getRowCount())
				.forEach(i -> spec.getFlowVariables().add(input.getValueAt(i, 0).toString()));
		range(0, output.getRowCount())
				.forEach(i -> spec.getFlowVariables().add(output.getValueAt(i, 0).toString()));
		getEditor().setModified(true);
		dialog.dispose();
	}
}

