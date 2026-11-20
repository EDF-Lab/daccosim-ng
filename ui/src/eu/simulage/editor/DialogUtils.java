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

import eu.simulage.daccosim.DataType;
import org.javafmi.framework.FmiContainer.Variability;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static javax.swing.event.TableModelEvent.INSERT;


public class DialogUtils {
	public static final Map<DataType, Checker> checkers = new HashMap<DataType, Checker>() {{
		put(DataType.Real, EditorUtils::isDouble);
		put(DataType.Integer, EditorUtils::isInteger);
		put(DataType.Boolean, EditorUtils::isBoolean);
		put(DataType.String, (v) -> true);
	}};

	public static JPanel removeAddButtons(ActionListener addAction, ActionListener removeAction) {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		JButton add = new JButton("+");
		add.setMinimumSize(new Dimension(50, 30));
		add.setMaximumSize(new Dimension(50, 30));
		add.addActionListener(addAction);
		panel.add(add);
		JButton remove = new JButton("-");
		remove.setMinimumSize(new Dimension(50, 30));
		remove.setMaximumSize(new Dimension(50, 30));
		remove.addActionListener(removeAction);
		panel.add(remove);
		return panel;
	}

	public static JComboBox<String> typeComboBox() {
		return new JComboBox<>(new String[]{DataType.Real.toString(), DataType.Integer.toString(), DataType.Boolean.toString(), DataType.String.toString()});
	}


	public static JComboBox<String> externalPropertiesVariabilityComboBox() {
		return new JComboBox<>(new String[]{Variability.discrete.name(), Variability.continuous.name()});
	}

	public static JComboBox<String> operableTypesComboBox() {
		return new JComboBox<>(new String[]{DataType.Real.toString(), DataType.Integer.toString(), DataType.Boolean.toString()});
	}

	public static JLabel labelOf(String text) {
		JLabel jLabel = new JLabel(text);
		jLabel.setFont(new Font("arial", Font.BOLD, 12));
		jLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		return jLabel;
	}

	public static void addTooltip(JTable table) {
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				label.setToolTipText(value.toString());
				return label;
			}
		});
	}

	public static TableRowSorter<TableModel> addSorter(JTable table) {
		return addSorter(table, -1);
	}

	public static TableRowSorter<TableModel> addSorter(JTable table, int toSort) {
		TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
		table.setRowSorter(sorter);
		for (int i = table.getModel().getColumnCount() - 1; i > -1; i--) addSorter(sorter, i);
		if (toSort != -1) table.getRowSorter().toggleSortOrder(toSort);
		return sorter;
	}

	private static void addSorter(TableRowSorter<TableModel> sorter, int column) {
		sorter.setComparator(column, (Comparator<String>) (o1, o2) -> {
			if (!o1.matches(".+\\[[0-9]+]") || !o2.matches(".+\\[[0-9]+]"))
				return o1.compareTo(o2);
			if (!o1.substring(0, o1.lastIndexOf("[")).equals(o2.substring(0, o2.lastIndexOf("["))))
				return o1.compareTo(o2);
			return Integer.compare(
					parseInt(o1.substring(o1.lastIndexOf("[") + 1, o1.length() - 1)),
					parseInt(o2.substring(o2.lastIndexOf("[") + 1, o2.length() - 1)));
		});
	}

	public static void addFocusListener(JTable table) {
		table.getModel().addTableModelListener(e -> {
			if (e.getType() == INSERT) {
				SwingUtilities.invokeLater(() -> {
					int viewRow = table.convertRowIndexToView(e.getFirstRow());
					table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
				});
			}
		});
	}

	public interface Checker {
		boolean check(String value);
	}
}
