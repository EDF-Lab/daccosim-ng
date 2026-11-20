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
import eu.simulage.editor.ExecutionThread;
import org.apache.commons.io.IOUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static eu.simulage.daccosim.GraphExecutor.logger;
import static java.lang.Double.parseDouble;
import static org.javafmi.kernel.OS.isWin32;
import static org.javafmi.kernel.OS.isWin64;

public class ExecutionDisplay extends JPanel {

	private final JDialog dialog;
	private final ExecutionThread thread;
	private JTextArea console;
	private JLabel link;

	public ExecutionDisplay(DaccosimDialog dialog, ExecutionThread thread) {
		this.dialog = dialog;
		this.thread = thread;
		dialog.okButton().setVisible(false);
		controlCloseButton();
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(console());
		add(finishedLabel());
		thread.addLoggerListener(s -> {
			console.append(s + "\n");
			console.setCaretPosition(console.getText().length());
		});
		thread.setOnFinishListener(() -> {
			if (thread.exception() != null)
				EditorUtils.showErrorDialog(dialog, "There was an exception: " + thread.exception() + ".\n\nIf memory exception, try shell.jar with -Xmx.\n\nIf other exception, please, report.");
			prepareFinishLabel(dialog, thread);
			dialog.cancelButton().setText("Close");
			link.setVisible(true);
		});
		thread.execute();
	}

	private void prepareFinishLabel(DaccosimDialog dialog, ExecutionThread thread) {
		if (thread.exportFile() != null) link.setText(wordWrap(thread.exportFile().getAbsolutePath()));
		link.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					if (isWindows() && openModelicaIsInstalled() && simulationEvolves()) {
						if (!scriptFile().exists()) writeScriptFile();
						Process process = new ProcessBuilder("powershell.exe", "-File", scriptFile().getAbsolutePath(), thread.exportFile().getAbsolutePath()).start();
						logger().info(new String(IOUtils.toByteArray(process.getInputStream())));
						logger().info(new String(IOUtils.toByteArray(process.getErrorStream())));
					} else Desktop.getDesktop().open(thread.exportFile());
					dialog.dispose();
				} catch (IOException exception) {
					exception.printStackTrace();
				}
			}

			private boolean simulationEvolves() {
				double startTime = parseDouble(EditorUtils.configuration().startTime());
				double stopTime = parseDouble(EditorUtils.configuration().stopTime());
				return startTime != stopTime;
			}

			private boolean openModelicaIsInstalled() {
				return System.getenv("OPENMODELICAHOME") != null;
			}

			private boolean isWindows() {
				return isWin64() || isWin32();
			}
		});
	}

	private void writeScriptFile() {
		try {
			Files.copy(ExecutionDisplay.class.getResourceAsStream("/daccosim_viewer.ps1"), scriptFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			logger().severe(e.getMessage());
		}
	}

	private File scriptFile() {
		return new File("daccosim_viewer.ps1");
	}

	private String wordWrap(String absolutePath) {
		StringBuilder builder = new StringBuilder("<html><center>");
		int lineSize = 80;
		while (absolutePath.length() > lineSize) {
			builder.append(absolutePath, 0, lineSize).append("<br>");
			absolutePath = absolutePath.substring(lineSize);
		}
		return builder.append(absolutePath).append("</center></html>").toString();
	}

	private void controlCloseButton() {
		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				cancel();
			}
		});
	}

	public void okButton() {
		if (thread.isDone()) dialog.dispose();
	}

	public void cancel() {
		if (!thread.isDone()) thread.stop();
		dialog.dispose();
	}

	private JScrollPane console() {
		console = new JTextArea();
		console.setFont(new Font("arial", Font.PLAIN, 10));
		console.setEditable(false);
		JScrollPane pane = new JScrollPane(console);
		pane.setMinimumSize(new Dimension(480, 370));
		pane.setMaximumSize(new Dimension(480, 400));
		return pane;
	}

	private Component finishedLabel() {
		link = new JLabel("Simulation finished with no output", SwingConstants.CENTER);
		link.setCursor(new Cursor(Cursor.HAND_CURSOR));
		link.setForeground(Color.BLUE);
		link.setAlignmentX(Component.CENTER_ALIGNMENT);
		link.setVisible(false);
		return link;
	}
}

