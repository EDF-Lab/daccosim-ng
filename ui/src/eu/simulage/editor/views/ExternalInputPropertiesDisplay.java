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

import eu.simulage.editor.EditorUtils;
import eu.simulage.editor.specs.ExternalInputSpec;
import eu.simulage.editor.specs.VariableSpec;

import javax.swing.*;
import java.awt.*;

import static eu.simulage.editor.DialogUtils.labelOf;
import static eu.simulage.editor.DialogUtils.removeAddButtons;
import static eu.simulage.editor.EditorUtils.getEditor;
import static eu.simulage.editor.EditorUtils.updateVariablesToExport;
import static java.awt.GridBagConstraints.*;
import static java.util.Arrays.asList;
import static java.util.stream.IntStream.range;

public class ExternalInputPropertiesDisplay extends ExternalPropertiesDisplay {

	private final ExternalInputSpec spec;
	private final JDialog dialog;

	public ExternalInputPropertiesDisplay(JDialog dialog, ExternalInputSpec spec) {
		super(dialog);
		this.dialog = dialog;
		this.spec = spec;
		setLayout(new GridBagLayout());
		add(labelOf("Label: "),
				new GridBagConstraints(0, 0, 1, 1, 0, 0, LINE_START, NONE, new Insets(0, 10, 0, 0), 0, 0));
		add(label = new JTextField(this.spec.getLabel()),
				new GridBagConstraints(1, 0, 2, 1, 0.9, 0, CENTER, HORIZONTAL, new Insets(0, 5, 0, 10), 0, 0));
		add(labelOf("Filter: "),
				new GridBagConstraints(0, 1, 1, 1, 0, 0, LINE_START, NONE, new Insets(10, 10, 0, 0), 0, 0));
		add(filterTextField(),
				new GridBagConstraints(1, 1, 2, 1, 0.9, 0, CENTER, HORIZONTAL, new Insets(10, 5, 0, 10), 0, 0));
		add(variables(asList("Type", "Output name", "Variability", "Initial value"), this.spec.getOutputs()),
				new GridBagConstraints(0, 2, 3, 1, 1, 1, CENTER, BOTH, new Insets(10, 10, 0, 10), 0, 0));
		add(removeAddButtons((e) -> addRow(), (e) -> removeRow()),
				new GridBagConstraints(0, 3, 2, 1, 0, 0, LINE_START, NONE, new Insets(0, 5, 0, 0), 0, 0));
		add(count = labelOf("count: " + spec.getOutputs().size()),
				new GridBagConstraints(1, 3, 1, 1, 0.9, 0, LINE_END, NONE, new Insets(0, 0, 0, 15), 0, 0));
		setVisible(true);
	}

	public void saveForm() {
		if (formNotCheck(spec)) return;
		tableSorter.setRowFilter(null);
		String previousLabel = spec.getLabel();
		spec.setLabel(label.getText());
		spec.getOutputs().clear();
		range(0, variables.getRowCount())
				.forEach(i -> spec.getOutputs().add(new VariableSpec(nameAt(variables, i), typeAt(variables, i), variabilityAt(variables, i), valueAt(variables, i))));
		EditorUtils.removeConnectionsAndOutputs(spec);
		updateVariablesToExport(previousLabel, spec.getLabel());
		getEditor().setModified(true);
		dialog.dispose();
	}

	@Override
	String varNamePrefix() {
		return "output_";
	}
}

