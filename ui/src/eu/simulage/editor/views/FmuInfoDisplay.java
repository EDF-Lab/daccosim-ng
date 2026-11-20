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

import eu.simulage.daccosim.DaccosimUtils;
import eu.simulage.editor.specs.FmuSpec;
import org.javafmi.modeldescription.v2.ModelDescription;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

import static eu.simulage.editor.DialogUtils.addTooltip;
import static java.awt.GridBagConstraints.CENTER;
import static java.awt.GridBagConstraints.HORIZONTAL;
import static org.javafmi.modeldescription.v2.Capabilities.*;

public class FmuInfoDisplay extends JPanel {

	private final FmuSpec spec;

	public FmuInfoDisplay(FmuSpec spec) {
		this.spec = spec;
		setLayout(new GridBagLayout());
		JScrollPane comp = new JScrollPane(infoTable());
		comp.setPreferredSize(new Dimension(650, 405));
		comp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		add(comp, new GridBagConstraints(0, 0, 1, 1, 1, 1, CENTER, HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
	}

	private Component infoTable() {
		JTable table = new JTable(new DefaultTableModel(new String[]{"Property", "Value"}, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		});
		addTooltip(table);
		fillProperties(table);
		table.getTableHeader().setBackground(Color.LIGHT_GRAY);
		table.getTableHeader().setOpaque(false);
		table.getTableHeader().setPreferredSize(new Dimension(625, 25));
		table.setRowHeight(20);
		table.getColumn("Property").setWidth(270);
		table.getColumn("Property").setMinWidth(270);
		table.getColumn("Property").setMaxWidth(270);
		table.getColumn("Property").setPreferredWidth(270);
		table.setPreferredSize(new Dimension(625, 405));
		return table;
	}


	@SuppressWarnings("ConstantConditions")
	private void fillProperties(JTable table) {
		org.javafmi.modeldescription.ModelDescription modelDescription = DaccosimUtils.getModelDescription(spec.file());
		add(table, "modelName", modelDescription.getModelName());
		add(table, "generationTool", modelDescription.getGenerationTool());
		add(table, "fmiVersion", modelDescription.getFmiVersion());
		if (modelDescription instanceof ModelDescription) {
			ModelDescription description = (ModelDescription) modelDescription;
			add(table, "generationDateAndTime", description.getGenerationDateAndTime());
			add(table, CanBeInstantiatedOnlyOncePerProcess.getName(), description.check(CanBeInstantiatedOnlyOncePerProcess) + "");
			add(table, NeedsExecutionTool.getName(), description.check(NeedsExecutionTool) + "");
			add(table, CanHandleVariableCommunicationStepSize.getName(), description.check(CanHandleVariableCommunicationStepSize) + "");
			add(table, CanInterpolateInputs.getName(), description.check(CanInterpolateInputs) + "");
			add(table, CanRunAsynchronuously.getName(), description.check(CanRunAsynchronuously) + "");
			add(table, CanNotUseMemoryManagementFunctions.getName(), description.check(CanNotUseMemoryManagementFunctions) + "");
			add(table, CanGetAndSetFMUState.getName(), description.check(CanGetAndSetFMUState) + "");
			add(table, CanSerializeFMUState.getName(), description.check(CanSerializeFMUState) + "");
			add(table, ProvidesDirectionalDerivative.getName(), description.check(ProvidesDirectionalDerivative) + "");
			add(table, "maxOutputDerivativeOrder", description.getCoSimulation().getMaxOutputDerivativeOrder() + "");
			add(table, "defaultExperimentTolerance", description.getDefaultExperiment().getTolerance() + "");
			add(table, "defaultExperimentStartTime", description.getDefaultExperiment().getStartTime() + "");
			add(table, "defaultExperimentStopTime", description.getDefaultExperiment().getStopTime() + "");
			add(table, "defaultExperimentStepSize", description.getDefaultExperiment().getStepSize() + "");
			add(table, "canHandleHybridStepping", DaccosimUtils.isHybridCoSim((ModelDescription) modelDescription) + "");
		}
	}

	private void add(JTable table, String property, String value) {
		((DefaultTableModel) table.getModel()).addRow(new String[]{property, value});
	}
}

