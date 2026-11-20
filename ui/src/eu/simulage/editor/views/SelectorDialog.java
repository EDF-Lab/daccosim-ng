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

import eu.simulage.editor.DaccosimDialog;
import eu.simulage.editor.DialogUtils;
import eu.simulage.editor.EditorUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static eu.simulage.editor.DialogUtils.*;
import static java.awt.GridBagConstraints.*;
import static java.util.Arrays.stream;
import static java.util.stream.IntStream.range;

class SelectorDialog extends DaccosimDialog {

	private final List<String> items;
	Set<Integer> checked = new HashSet<>();
	private final List<String> selectedItems = new ArrayList<>();
	private final boolean multiple;
	private JLabel count;
	private JTable itemsTable;
	private TableRowSorter<TableModel> tableSorter;

	SelectorDialog(JDialog owner, List<String> items, boolean multiple) {
		this(owner, multiple ? "Select variables" : "Select variable", items, multiple);
	}

	SelectorDialog(JDialog owner, String title, List<String> items, boolean multiple) {
		super(owner);
		this.items = items;
		this.multiple = multiple;
		setTitle(title);
		setSize(new Dimension(350, 350));
		setLocationRelativeTo(owner);
		withButtons();
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(labelOf("Filter"),
				new GridBagConstraints(0, 0, 1, 1, 0, 0, CENTER, NONE, new Insets(0, 10, 0, 0), 0, 0));
		panel.add(filterTextField(),
				new GridBagConstraints(1, 0, 1, 1, 0.9, 0, CENTER, HORIZONTAL, new Insets(0, 5, 0, 10), 0, 0));

		panel.add(itemsTable(),
				new GridBagConstraints(0, 1, 2, 1, 1, 0.9, CENTER, BOTH, new Insets(5, 10, 0, 10), 0, 0));

		if (multiple)
			panel.add(count = labelOf("count: 0/" + itemsTable.getRowCount()),
					new GridBagConstraints(1, 2, 1, 1, 1, 0, LINE_END, NONE, new Insets(0, 0, 0, 15), 0, 0));
		add(panel);
		okRunnable(this::saveForm);
		cancelRunnable(this::dispose);
		setVisible(true);
	}

	private JTextField filterTextField() {
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
				tableSorter.setRowFilter(RowFilter.regexFilter(text, multiple ? 1 : 0));
				count.setText("count: " + checked.size() + "/" + itemsTable.getRowCount());
			}
		});
		return filter;
	}

	List<String> selectedItems() {
		return selectedItems;
	}

	private Component itemsTable() {
		DefaultTableModel model = multiple ? multipleModel() : singleModel();
		itemsTable = new JTable(model);
		if (!multiple) itemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		else itemsTable.getColumnModel().getColumn(0).setMaxWidth(30);
		itemsTable.setRowHeight(20);
		addTooltip(itemsTable);
		tableSorter = addSorter(itemsTable);
		DialogUtils.addFocusListener(itemsTable);

		if (!multiple)
			itemsTable.addMouseListener(new MouseAdapter() {
				public void mouseReleased(MouseEvent e) {
					if (e.getClickCount() == 2) saveForm();
				}
			});
		else itemsTable.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() != KeyEvent.VK_SPACE) return;
				Boolean previousBoolean = stream(itemsTable.getSelectedRows()).boxed()
						.map(i -> (boolean) itemsTable.getValueAt(i, 0))
						.reduce((b1, b2) -> b1 || b2)
						.orElse(false);
				stream(itemsTable.getSelectedRows()).forEach(i -> itemsTable.setValueAt(!previousBoolean, i, 0));
			}
		});
		itemsTable.getTableHeader().setBackground(Color.LIGHT_GRAY);
		itemsTable.getTableHeader().setOpaque(false);
		return new JScrollPane(itemsTable);
	}

	private DefaultTableModel multipleModel() {
		return new DefaultTableModel(new Object[]{"", "Name"}, 0) {

			@Override
			public int getColumnCount() {
				return 2;
			}

			@Override
			public int getRowCount() {
				return items.size();
			}

			@Override
			public void setValueAt(Object aValue, int row, int column) {
				super.setValueAt(aValue, row, column);
				if (column == 0) {
					if ((boolean) aValue) checked.add(row);
					else checked.remove(row);
					count.setText("count: " + checked.size() + "/" + itemsTable.getRowCount());
				}
			}

			@Override
			public Object getValueAt(int row, int column) {
				return column == 0 ? checked.contains(row) : items.get(row);
			}

			@Override
			public boolean isCellEditable(int row, int column) {
				return column < 1;
			}

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return columnIndex == 0 ? Boolean.class : String.class;
			}
		};
	}

	private DefaultTableModel singleModel() {
		return new DefaultTableModel(new Object[]{"Name"}, 0) {

			@Override
			public int getColumnCount() {
				return 1;
			}

			@Override
			public int getRowCount() {
				return items.size();
			}

			@Override
			public Object getValueAt(int row, int column) {
				return items.get(row);
			}

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return String.class;
			}
		};
	}

	private void saveForm() {
		RowFilter<? super TableModel, ? super Integer> filter = tableSorter.getRowFilter();
		tableSorter.setRowFilter(null);
		if (!multiple)
			if (itemsTable.getSelectedRow() == -1) {
				EditorUtils.showErrorDialog(this, "There is no row selected");
				tableSorter.setRowFilter(filter);
				return;
			} else selectedItems.add(valueAt(itemsTable.getSelectedRow()).toString());
		else range(0, itemsTable.getRowCount())
				.filter(i -> ((boolean) itemsTable.getValueAt(i, 0)))
				.forEach(i -> selectedItems.add(itemsTable.getValueAt(i, 1).toString()));
		this.dispose();
	}

	private Object valueAt(int row) {
		return itemsTable.getModel().getValueAt(itemsTable.getRowSorter().convertRowIndexToModel(row), 1);
	}

}

