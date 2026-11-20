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

import com.mxgraph.model.mxICell;
import eu.simulage.editor.specs.*;
import eu.simulage.editor.views.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;

import static eu.simulage.editor.EditorUtils.showErrorDialog;

class Dialogs {
	static FmuSpec showFmuForm(Component component, FmuSpec spec) {
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(component);
		if (frame == null) return null;
		DaccosimDialog dialog = new DaccosimDialog(frame, "FMU properties").withButtons();
		FmuPropertiesDisplay display = new FmuPropertiesDisplay(dialog, spec);
		dialog.okRunnable(display::saveForm).cancelRunnable(display::cancelAction);
		dialog.add(display);
		dialog.setSize(500, 500);
		dialog.setLocationRelativeTo(frame);
		dialog.setVisible(true);
		return display.isComplete() ? spec : null;
	}

	static void showFmuInfo(Component component, FmuSpec spec) {
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(component);
		if (frame == null) return;
		DaccosimDialog dialog = new DaccosimDialog(frame, "FMU model description");
		dialog.add(new FmuInfoDisplay(spec));
		dialog.setSize(700, 460);
		dialog.setLocationRelativeTo(frame);
		dialog.setVisible(true);
	}

	static void defineFlowVariables(Component component, FmuSpec spec) {
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(component);
		if (frame == null) return;
		DaccosimDialog dialog = new DaccosimDialog(frame, "Define flow variables").withButtons();
		FlowVariablesDisplay display = new FlowVariablesDisplay(dialog, spec);
		dialog.okRunnable(display::saveForm).cancelRunnable(display::cancelAction);
		dialog.add(display);
		dialog.setSize(510, 390);
		dialog.setLocationRelativeTo(frame);
		dialog.setVisible(true);
	}

	static void showOperatorForm(Component component, OperatorSpec spec) {
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(component);
		if (frame == null) return;
		DaccosimDialog dialog = new DaccosimDialog(frame, (spec instanceof AdderSpec ? "Adder" : "Multiplier") + " properties").withButtons();
		AdderAndMultiplierPropertiesDisplay display = new AdderAndMultiplierPropertiesDisplay(dialog, spec);
		dialog.setSize(500, 300);
		dialog.setLocationRelativeTo(frame);
		dialog.okRunnable(display::saveForm);
		dialog.add(display);
		dialog.setVisible(true);
	}

	static void showOffsetForm(Component component, OffsetSpec spec) {
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(component);
		if (frame == null) return;
		DaccosimDialog dialog = new DaccosimDialog(frame, "Offset properties").withButtons();
		GainAndOffsetPropertiesDisplay display = new GainAndOffsetPropertiesDisplay(dialog, spec);
		dialog.setSize(500, 400);
		dialog.setLocationRelativeTo(frame);
		dialog.okRunnable(display::saveForm);
		dialog.add(display);
		dialog.setVisible(true);
	}

	static void showGainForm(Component component, GainSpec spec) {
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(component);
		if (frame == null) return;
		DaccosimDialog dialog = new DaccosimDialog(frame, "Gain properties").withButtons();
		GainAndOffsetPropertiesDisplay display = new GainAndOffsetPropertiesDisplay(dialog, spec);
		dialog.setSize(500, 400);
		dialog.setLocationRelativeTo(frame);
		dialog.okRunnable(display::saveForm);
		dialog.add(display);
		dialog.setVisible(true);
	}

	static void showExternalInputForm(Component component, ExternalInputSpec spec) {
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(component);
		if (frame == null) return;
		DaccosimDialog dialog = new DaccosimDialog(frame, "External input properties").withButtons();
		ExternalInputPropertiesDisplay display = new ExternalInputPropertiesDisplay(dialog, spec);
		dialog.okRunnable(display::saveForm);
		dialog.setSize(500, 500);
		dialog.setLocationRelativeTo(frame);
		dialog.add(display);
		dialog.setVisible(true);
	}

	static void showExternalOutputForm(Component component, ExternalOutputSpec spec) {
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(component);
		if (frame == null) return;
		DaccosimDialog dialog = new DaccosimDialog(frame, "External output properties").withButtons();
		ExternalOutputPropertiesDisplay display = new ExternalOutputPropertiesDisplay(dialog, spec);
		dialog.okRunnable(display::saveForm);
		dialog.setSize(500, 500);
		dialog.setLocationRelativeTo(frame);
		dialog.add(display);
		dialog.setVisible(true);
	}

	static void showArrowForm(Component component, ArrowSpec spec) {
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(component);
		if (frame == null) return;
		if (!spec.isConnected()) {
			showErrorDialog(frame, "Arrow is not connected");
			return;
		}
		DaccosimDialog dialog = new DaccosimDialog(frame, "Arrow properties").withButtons();
		ArrowPropertiesDisplay display = new ArrowPropertiesDisplay(dialog, spec);
		dialog.okRunnable(display::saveForm);
		dialog.setSize(500, 500);
		dialog.setLocationRelativeTo(frame);
		dialog.add(display);
		dialog.setVisible(true);
	}

	static void showAbout(Component component) {
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(component);
		if (frame != null) {
			JDialog dialog = new DaccosimDialog(frame, "About");
			dialog.setSize(500, 580);
			dialog.setLocationRelativeTo(frame);
			dialog.add(new AboutDialog());
			dialog.setVisible(true);
		}
	}

	static void showConfiguration(Component component) {
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(component);
		if (frame != null) {
			DaccosimDialog dialog = new DaccosimDialog(frame, "Co-simulation configuration").withButtons();
			ConfigurationDisplay display = new ConfigurationDisplay(dialog, EditorUtils.configuration());
			dialog.withReset(display::reset);
			dialog.add(display);
			dialog.okRunnable(display::saveForm);
			dialog.setSize(500, 540);
			dialog.setLocationRelativeTo(frame);
			dialog.setVisible(true);
		}
	}

	static void showExecDisplay(Component component, ExecutionThread executionThread) {
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(component);
		if (frame != null) {
			DaccosimDialog dialog = new DaccosimDialog(frame, "Co-simulation execution").withButtons();
			ExecutionDisplay display = new ExecutionDisplay(dialog, executionThread);
			dialog.okRunnable(display::okButton);
			dialog.cancelRunnable(display::cancel);
			dialog.add(display);
			dialog.setSize(500, 500);
			dialog.setLocationRelativeTo(frame);
			dialog.setVisible(true);
		}
	}

	static void showFmuExportDisplay(Component component, File file) {
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(component);
		if (frame != null) {
			DaccosimDialog dialog = new DaccosimDialog(frame, "Graph is being exported. Please wait").withButtons();
			FmuExportationDisplay display = new FmuExportationDisplay(dialog, file);
			dialog.cancelRunnable(display::cancel);
			dialog.add(display);
			dialog.setSize(400, 100);
			dialog.setLocationRelativeTo(frame);
			dialog.setResizable(false);
			display.execute();
			dialog.setVisible(true);
		}
	}

	static void showFixArrowPointsDisplay(Editor editor, mxICell value) {
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(editor);
		if (frame != null) {
			DaccosimDialog dialog = new DaccosimDialog(frame, "Fix arrow points").withButtons();
			FixArrowPointsDisplay display = new FixArrowPointsDisplay(dialog, value);
			dialog.okRunnable(display::okButton);
			dialog.cancelRunnable(display::cancel);
			dialog.add(display);
			dialog.setSize(300, 200);
			dialog.setLocationRelativeTo(frame);
			dialog.setVisible(true);
		}
	}
}
