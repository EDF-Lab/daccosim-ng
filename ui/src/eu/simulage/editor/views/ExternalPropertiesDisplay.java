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
import eu.simulage.editor.DialogUtils;
import eu.simulage.editor.EditorUtils;
import eu.simulage.editor.Spec;
import eu.simulage.editor.specs.VariableSpec;
import org.javafmi.framework.FmiContainer;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static eu.simulage.editor.DialogUtils.*;
import static eu.simulage.editor.EditorUtils.defaultValueOf;
import static eu.simulage.editor.EditorUtils.showErrorDialog;
import static java.util.Arrays.stream;

abstract class ExternalPropertiesDisplay extends JPanel {
	final JDialog dialog;
	protected JLabel count;
	JTextField label;
	JTable variables;
	TableRowSorter<TableModel> tableSorter;

	ExternalPropertiesDisplay(JDialog dialog) {
		this.dialog = dialog;
	}

	static String typeAt(JTable table, int row) {
		return table.getValueAt(row, 0).toString();
	}

	static String nameAt(JTable table, int row) {
		return table.getValueAt(row, 1).toString();
	}

	static String variabilityAt(JTable table, int row) {
		return table.getValueAt(row, 2).toString();
	}

	static String valueAt(JTable table, int row) {
		return table.getValueAt(row, 3).toString();
	}

	Component variables(List<String> headers, List<VariableSpec> connectorList) {
		variables = new JTable(new DefaultTableModel(headers.toArray(new String[0]), 0));
		addTooltip(variables);
		addTableListener();
		variables.getColumn("Type").setCellEditor(new DefaultCellEditor(DialogUtils.typeComboBox()));
		variables.getColumn("Variability").setCellEditor(new DefaultCellEditor(externalPropertiesVariabilityComboBox()));
		variables.getTableHeader().setBackground(Color.LIGHT_GRAY);
		variables.getTableHeader().setOpaque(false);
		tableSorter = addSorter(variables);
		connectorList.forEach(c -> ((DefaultTableModel) variables.getModel()).addRow(new Object[]{c.getType(), c.getName(), c.getVariability(), c.getValue()}));
		JScrollPane jScrollPane = new JScrollPane(variables);
		jScrollPane.setPreferredSize(new Dimension(440, 450));
		return jScrollPane;
	}

	private void addTableListener() {
		variables.getModel().addTableModelListener(e -> {
			if (e.getColumn() != 0) return;
			String type = typeAt(variables, variables.convertRowIndexToView(e.getFirstRow()));
			variables.getModel().setValueAt(defaultValueOf(type), e.getFirstRow(), 3);
		});
	}

	void addRow() {
		((DefaultTableModel) variables.getModel()).addRow(new String[]{DataType.Real.toString(), newVariableName(), "continuous", "0.0"});
		count.setText("count: " + variables.getModel().getRowCount());
	}

	private String newVariableName() {
		return String.format("%s%03d", varNamePrefix(), (variables.getRowCount() + 1));
	}

	abstract String varNamePrefix();

	void removeRow() {
		List<Integer> indexesToDelete = stream(variables.getSelectedRows()).boxed()
				.map(i -> variables.convertRowIndexToModel(i))
				.sorted().collect(Collectors.toList());
		Collections.reverse(indexesToDelete);
		indexesToDelete.forEach(i -> ((DefaultTableModel) variables.getModel()).removeRow(i));
		count.setText("count: " + variables.getModel().getRowCount());
	}

	boolean formNotCheck(Spec spec) {
		return !labelIsCorrect(spec) || !outputsAreCorrect();
	}

	private boolean labelIsCorrect(Spec spec) {
		if (label.getText().isEmpty()) {
			showErrorDialog(dialog, "label cannot be empty");
			return false;
		}
		if (EditorUtils.labelCount(label.getText(), spec) > 0) {
			showErrorDialog(dialog, "this label already exists");
			return false;
		}
		return true;
	}

	private boolean outputsAreCorrect() {
		return checkNames() && checkVariability() && checkValues();
	}

	private boolean checkNames() {
		Set<String> names = new HashSet<>();
		for (int i = 0; i < variables.getRowCount(); i++) {
			if (names.contains(nameAt(variables, i))) {
				showErrorDialog(dialog, "name " + nameAt(variables, i) + " is repeated");
				return false;
			} else names.add(nameAt(variables, i));
		}
		return true;
	}

	private boolean checkVariability() {
		for (int i = 0; i < variables.getRowCount(); i++) {
			if (DataType.valueOf(typeAt(variables, i)) == DataType.Real) continue;
			if (!variabilityAt(variables, i).equals(FmiContainer.Variability.continuous.name())) continue;
			showErrorDialog(dialog, nameAt(variables, i) + " has a variability (" + variabilityAt(variables, i) + ") not valid for its type (" + typeAt(variables, i) + ")");
			return false;
		}
		return true;
	}

	private boolean checkValues() {
		for (int i = 0; i < variables.getRowCount(); i++) {
			if (checkers.get(DataType.valueOf(typeAt(variables, i))).check(valueAt(variables, i))) continue;
			showErrorDialog(dialog, nameAt(variables, i) + " has a value (" + valueAt(variables, i) + ") not valid for its type (" + typeAt(variables, i) + ")");
			return false;
		}
		return true;
	}

	JTextField filterTextField() {
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
				tableSorter.setRowFilter(RowFilter.regexFilter(text, 1));
			}
		});
		return filter;
	}

}
