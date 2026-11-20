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
import eu.simulage.editor.Spec;
import eu.simulage.editor.specs.ArrowSpec;
import eu.simulage.editor.specs.FmuSpec;
import eu.simulage.editor.specs.VariableSpec;
import org.javafmi.framework.FmiContainer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Set;

import static eu.simulage.editor.DialogUtils.*;
import static eu.simulage.editor.EditorUtils.getEditor;
import static eu.simulage.editor.EditorUtils.showErrorDialog;
import static java.awt.GridBagConstraints.*;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.range;

public class ArrowPropertiesDisplay extends JPanel {

	private static final int INPUT_COLUMN = 2;
	private static final int OUTPUT_COLUMN = 1;
	private final JDialog dialog;
	private final ArrowSpec spec;
	private final JLabel connectionsCount;
	private JTable connections;

	public ArrowPropertiesDisplay(JDialog dialog, ArrowSpec spec) {
		this.dialog = dialog;
		this.spec = spec;
		setLayout(new GridBagLayout());
		add(connections(),
				new GridBagConstraints(0, 0, 2, 1, 1, 1, CENTER, BOTH, new Insets(0, 10, 0, 10), 0, 0));

		add(removeAddButtons((e) -> addConn(), (e) -> removeConn()),
				new GridBagConstraints(0, 1, 1, 1, 0.5, 0, LINE_START, NONE, new Insets(0, 5, 0, 0), 0, 0));
		add(connectionsCount = new JLabel(connectionsCountText()),
				new GridBagConstraints(1, 1, 1, 1, 0.5, 0, LINE_END, NONE, new Insets(0, 0, 0, 10), 0, 0));
	}

	private String connectionsCountText() {
		long count = range(0, connections.getRowCount())
				.filter(i -> !connections.getValueAt(i, OUTPUT_COLUMN).toString().isEmpty()).count();
		return "connections: " + count + "/" + connections.getRowCount();
	}

	private void addConn() {
		if (connections.getSelectedRow() == -1) {
			showErrorDialog(this.dialog, "Select an input first");
			return;
		}
		Set<DataType> dataTypes = stream(connections.getSelectedRows()).boxed()
				.map(i -> targetVarSpec(connections.getValueAt(i, INPUT_COLUMN).toString()).dataType())
				.collect(toSet());
		if (dataTypes.size() > 1) {
			showErrorDialog(this.dialog, "Selected inputs must have the same data type");
			return;
		}
		Set<String> variabilities = stream(connections.getSelectedRows()).boxed()
				.map(i -> targetVarSpec(connections.getValueAt(i, INPUT_COLUMN).toString()).getVariability())
				.collect(toSet());
		if (variabilities.size() > 1) {
			showErrorDialog(this.dialog, "Selected inputs must have the same variability");
			return;
		}
		Set<Boolean> flowVariables = stream(connections.getSelectedRows()).boxed()
				.map(i -> isFlowVariable(spec.target(), connections.getValueAt(i, INPUT_COLUMN).toString()))
				.collect(toSet());
		if (flowVariables.size() > 1) {
			showErrorDialog(this.dialog, "All selected inputs must be flow or normal variables");
			return;
		}
		showSelectorDialog(dataTypes.iterator().next(), variabilities.iterator().next(), flowVariables.iterator().next());
	}

	private void showSelectorDialog(DataType dataTypes, String variability, Boolean flowVariables) {
		List<VariableSpec> specs = spec.allOutputs().stream()
				.filter(v -> v.dataType().equals(dataTypes))
				.filter(v -> checkVariability(v.getVariability(), variability))
				.collect(toList());
		specs = flowVariables ?
				specs.stream().filter(v -> isFlowVariable(spec.source(), v.getName())).collect(toList()) :
				specs.stream().filter(v -> !isFlowVariable(spec.source(), v.getName())).collect(toList());
		String title = connections.getSelectedRows().length == 1 ?
				"Select output for " + spec.target().getLabel() + "." + connections.getValueAt(connections.getSelectedRows()[0], INPUT_COLUMN) :
				"Select output for input selection";
		SelectorDialog dialog = new SelectorDialog(this.dialog, title, specs.stream().map(VariableSpec::getName).collect(toList()), false);
		if (dialog.selectedItems().isEmpty()) return;
		setSourceValue(dialog.selectedItems().get(0));
		connectionsCount.setText(connectionsCountText());
	}

	private boolean checkVariability(String outputVariability, String inputVariability) {
		return inputVariability.equals(FmiContainer.Variability.continuous.name()) ||
				outputVariability.equals(inputVariability) ||
				outputVariability.equals(FmiContainer.Variability.constant.name());
	}

	private void removeConn() {
		setSourceValue("");
		connectionsCount.setText(connectionsCountText());
	}

	private void setSourceValue(String value) {
		for (int index : connections.getSelectedRows()) connections.setValueAt(value, index, OUTPUT_COLUMN);
	}

	private Component connections() {
		connections = new JTable(new DefaultTableModel(new Object[]{"Type", spec.source().label() + " outputs", spec.target().label() + " inputs"}, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		});
		addTooltip(connections);
		addSorter(connections, 2);
		connections.getTableHeader().setBackground(Color.LIGHT_GRAY);
		connections.getTableHeader().setOpaque(false);
		connections.getColumnModel().getColumn(0).setMaxWidth(50);
		connections.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				if (e.getClickCount() == 2) addConn();
			}
		});
		spec.availableInputs().stream()
				.filter(v -> spec.getConnections().stream().noneMatch(c -> c.getTo().equals(v.getName())))
				.forEach(v -> tableModel().addRow(new Object[]{v.dataType().toString(), "", v.getName()}));
		spec.getConnections()
				.forEach(c -> tableModel().addRow(new Object[]{targetVarSpec(c.getTo()).dataType().name(), c.getFrom(), c.getTo()}));
		return new JScrollPane(connections);
	}

	private DefaultTableModel tableModel() {
		return (DefaultTableModel) connections.getModel();
	}

	public void saveForm() {
		spec.getConnections().clear();
		range(0, connections.getRowCount())
				.filter(i -> !connections.getValueAt(i, OUTPUT_COLUMN).toString().isEmpty())
				.forEach(i -> spec.addConnection(connections.getValueAt(i, OUTPUT_COLUMN).toString(),
						connections.getValueAt(i, INPUT_COLUMN).toString()));
		getEditor().setModified(true);
		dialog.dispose();
	}

	private VariableSpec targetVarSpec(String value) {
		return spec.allInputs().stream().filter(v -> v.getName().equals(value)).findFirst().orElse(null);
	}

	private boolean isFlowVariable(Spec spec, String value) {
		return spec instanceof FmuSpec && ((FmuSpec) spec).getFlowVariables().contains(value);
	}

}

