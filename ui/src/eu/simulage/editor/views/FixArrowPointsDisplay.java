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

import com.mxgraph.model.mxICell;
import com.mxgraph.view.mxGraph;
import eu.simulage.editor.DaccosimDialog;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.stream.Collectors;

import static eu.simulage.editor.EditorUtils.*;
import static java.awt.GridBagConstraints.*;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;

public class FixArrowPointsDisplay extends JPanel {

	private final DaccosimDialog dialog;
	private final mxICell arrow;
	private final JCheckBox sourceCheck;
	private final JCheckBox targetCheck;
	private JTextField sourceX;
	private JTextField sourceY;
	private JTextField targetX;
	private JTextField targetY;

	public FixArrowPointsDisplay(DaccosimDialog dialog, mxICell arrow) {
		this.dialog = dialog;
		this.arrow = arrow;
		setLayout(new GridBagLayout());
		add(sourceCheck = new JCheckBox("Fix source", this.arrow.getStyle().contains("exitX")),
				new GridBagConstraints(0, 0, 2, 1, 0, 0, LINE_START, NONE, new Insets(0, 0, 0, 0), 0, 0));
		add(targetCheck = new JCheckBox("Fix target", this.arrow.getStyle().contains("entryX")),
				new GridBagConstraints(2, 0, 2, 1, 0, 0, LINE_START, NONE, new Insets(0, 15, 0, 0), 0, 0));

		sourceCheck.addItemListener(e -> {
			sourceX.setEnabled(!sourceX.isEnabled());
			sourceY.setEnabled(!sourceY.isEnabled());
		});
		targetCheck.addItemListener(e -> {
			targetX.setEnabled(!targetX.isEnabled());
			targetY.setEnabled(!targetY.isEnabled());
		});

		add(new JLabel("Source X"),
				new GridBagConstraints(0, 1, 1, 1, 0, 0, LINE_START, NONE, new Insets(5, 5, 0, 0), 0, 0));
		add(sourceX = new JTextField(cellStyle().containsKey("exitX") ? "" + (int) (parseDouble(cellStyle().get("exitX").toString()) * 100) : ""),
				new GridBagConstraints(1, 1, 1, 1, 1, 0, CENTER, HORIZONTAL, new Insets(5, 5, 0, 20), 0, 0));
		add(new JLabel("Target X"),
				new GridBagConstraints(2, 1, 1, 1, 0, 0, LINE_START, NONE, new Insets(5, 20, 0, 0), 0, 0));
		add(targetX = new JTextField(cellStyle().containsKey("entryX") ? "" + (int) (parseDouble(cellStyle().get("entryX").toString()) * 100) : ""),
				new GridBagConstraints(3, 1, 1, 1, 0, 0, CENTER, HORIZONTAL, new Insets(5, 5, 0, 10), 0, 0));

		add(new JLabel("Source Y"),
				new GridBagConstraints(0, 2, 1, 1, 0, 0, LINE_START, NONE, new Insets(5, 5, 0, 0), 0, 0));
		add(sourceY = new JTextField(cellStyle().containsKey("exitY") ? "" + (int) (100 - 100 * parseDouble(cellStyle().get("exitY").toString())) : ""),
				new GridBagConstraints(1, 2, 1, 1, 1, 0, CENTER, HORIZONTAL, new Insets(5, 5, 0, 20), 0, 0));
		add(new JLabel("Target Y"),
				new GridBagConstraints(2, 2, 1, 1, 0, 0, LINE_START, NONE, new Insets(5, 20, 0, 0), 0, 0));
		add(targetY = new JTextField(cellStyle().containsKey("entryY") ? "" + (int) (100 - 100 * parseDouble(cellStyle().get("entryY").toString())) : ""),
				new GridBagConstraints(3, 2, 1, 1, 1, 0, CENTER, HORIZONTAL, new Insets(5, 5, 0, 10), 0, 0));

		add(new JLabel("*Values must be integers between 0-100"),
				new GridBagConstraints(0, 3, 4, 1, 1, 0, LINE_START, NONE, new Insets(25, 5, 0, 10), 0, 0));

		if (!sourceCheck.isSelected()) {
			sourceX.setEnabled(false);
			sourceY.setEnabled(false);
		}
		if (!targetCheck.isSelected()) {
			targetX.setEnabled(false);
			targetY.setEnabled(false);
		}
	}

	public void okButton() {
		if (sourceCheck.isSelected() && isNotInt("Source X", sourceX)) return;
		if (sourceCheck.isSelected() && isNotInt("Source Y", sourceY)) return;
		if (targetCheck.isSelected() && isNotInt("Target X", targetX)) return;
		if (targetCheck.isSelected() && isNotInt("Target Y", targetY)) return;
		mxGraph graph = getEditor().getGraphComponent().getGraph();
		graph.getModel().beginUpdate();
		Map<String, Object> style = cellStyle();
		asList("exitX", "exitY", "entryX", "entryY").forEach(style::remove);
		boolean vertical = false;
		if (sourceCheck.isSelected()) {
			style.put("exitX", parseInt(sourceX.getText()) / 100.0);
			int exitY = 100 - parseInt(sourceY.getText());
			style.put("exitY", exitY / 100.0);
			vertical = exitY == 0 || exitY == 100;
		}
		if (targetCheck.isSelected()) {
			style.put("entryX", parseInt(targetX.getText()) / 100.0);
			int targetY = 100 - parseInt(this.targetY.getText());
			style.put("entryY", targetY / 100.0);
			vertical = targetY == 0 || targetY == 100;
		}
		style.put("elbow", vertical ? "vertical" : "horizontal");
		graph.getModel().setStyle(arrow, String.join(";", style.entrySet().stream()
				.map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.toList())));
		graph.getModel().endUpdate();
		dialog.dispose();
	}

	private Map<String, Object> cellStyle() {
		return getEditor().getGraphComponent().getGraph().getCellStyle(arrow);
	}

	private boolean isNotInt(String fieldName, JTextField field) {
		if (!field.isEnabled() || isInteger(field.getText())) return false;
		showErrorDialog(dialog, "The field " + fieldName + " must be an integer number between 0 and 100 (%)");
		return true;
	}

	public void cancel() {
		dialog.dispose();
	}

}

