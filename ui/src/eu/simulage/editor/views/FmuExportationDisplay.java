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
import eu.simulage.editor.EditorUtils;
import eu.simulage.editor.FmuExporter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

public class FmuExportationDisplay extends JPanel {

	private final DaccosimDialog dialog;
	private final File file;
	private JLabel link;

	public FmuExportationDisplay(DaccosimDialog dialog, File file) {
		this.dialog = dialog;
		this.file = file;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(Box.createVerticalGlue());
		add(link = new JLabel("Please wait...", SwingConstants.CENTER));
		link.setAlignmentX(CENTER_ALIGNMENT);
		add(Box.createVerticalGlue());
		dialog.okButton().setVisible(false);
	}

	public void execute() {
		EditorUtils.doInBackground(dialog, () -> {
			try {
				FmuExporter.exportFmu(file);
				updateLabel();
			} catch (Throwable e) {
				e.printStackTrace();
				add(errorLabel());
			}
			dialog.cancelButton().setText("Close");
			dialog.cancelButton().setVisible(true);
		});
	}

	public void cancel() {
		dialog.dispose();
	}

	private void updateLabel() {
		link.setText("FMU exported: " + file.getName());
		link.setCursor(new Cursor(Cursor.HAND_CURSOR));
		link.setForeground(Color.BLUE);
		link.setAlignmentX(Component.CENTER_ALIGNMENT);
		link.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					Desktop.getDesktop().open(file.getParentFile());
					dialog.dispose();
				} catch (IOException ignored) {
				}
			}
		});
	}

	private Component errorLabel() {
		link = new JLabel("Error building FMU. Check log for details.");
		link.setAlignmentX(Component.CENTER_ALIGNMENT);
		return link;
	}
}

