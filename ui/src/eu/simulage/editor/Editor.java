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

import com.mxgraph.model.mxCell;
import com.mxgraph.swing.handler.mxRubberband;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.mxGraphOutline;
import com.mxgraph.swing.util.mxSwingConstants;
import com.mxgraph.util.*;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxUndoableEdit.mxUndoableChange;
import com.mxgraph.view.mxGraph;
import eu.simulage.daccosim.DaccosimUtils;
import eu.simulage.editor.EditorActions.EditPropertiesAction;
import eu.simulage.editor.specs.FmuSpec;
import org.javafmi.modeldescription.ModelDescription;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.LINE_START;

public class Editor extends JPanel {

	private static final String DEFAULT_NOTES_TEXT = "Write notes of your model here";

	static {
		mxSwingConstants.SHADOW_COLOR = Color.LIGHT_GRAY;
		mxConstants.W3C_SHADOWCOLOR = "#D3D3D3";
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | UnsupportedLookAndFeelException | IllegalAccessException ignored) {
		}
		JOptionPane.setDefaultLocale(Locale.ENGLISH);
	}

	mxGraphComponent graphComponent;
	private JFrame frame;
	private mxUndoManager undoManager;
	private boolean modified = false;
	private SimxFile currentFile = untitledFile();
	private JTextArea notes;
	private JLabel statusLabel;
	private List<String> recentFiles = new ArrayList<>();
	private File recentFile = new File(".recent");
	private EditorMenuBar menubar;

	Editor(mxGraphComponent component) {
		loadRecentFiles();
		mxResources.getBundles().clear();
		mxResources.add("editor");
		this.graphComponent = component;
		this.undoManager = createUndoManager();
		createLayout();
		installHandlers();
		installListeners();
		updateTitle();
		createFrame();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> currentFile.close()));
	}

	static SimxFile untitledFile() {
		return new SimxFile("_untitled_.simx").create();
	}

	private mxUndoManager createUndoManager() {
		final mxUndoManager undoManager = new mxUndoManager();
		mxIEventListener undoHandler = (source, evt) -> {
			List<mxUndoableChange> changes = ((mxUndoableEdit) evt.getProperty("edit")).getChanges();
			graph().setSelectionCells(graph().getSelectionCellsForChanges(changes));
		};
		undoManager.addListener(mxEvent.UNDO, undoHandler);
		undoManager.addListener(mxEvent.REDO, undoHandler);
		undoHandler = (source, evt) -> undoManager.undoableEditHappened((mxUndoableEdit) evt.getProperty("edit"));
		graph().getModel().addListener(mxEvent.CHANGE, (source, evt) -> setModified(true));
		graph().getModel().addListener(mxEvent.UNDO, undoHandler);
		graph().getView().addListener(mxEvent.UNDO, undoHandler);
		return undoManager;
	}

	private void createLayout() {
		JSplitPane outer = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new EditorPalette(), graphComponent);
		outer.setDividerLocation(100);
		outer.setDividerSize(6);
		outer.setEnabled(false);
		outer.setBorder(null);
		outer.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				outer.setDividerLocation(100);
			}
		});
		setLayout(new BorderLayout());
		add(outer, BorderLayout.CENTER);
		add(new EditorToolBar(this), BorderLayout.NORTH);

		JPanel downPart = new JPanel();
		downPart.setLayout(new BoxLayout(downPart, BoxLayout.Y_AXIS));
		downPart.add(new JScrollPane(notes = new JTextArea(DEFAULT_NOTES_TEXT, 2, 5)));
		notes.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				setModified(true);
				EditorUtils.configuration().notes(notes.getText());
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				setModified(true);
				EditorUtils.configuration().notes(notes.getText());
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				setModified(true);
				EditorUtils.configuration().notes(notes.getText());
			}
		});
		JPanel statusPanel = new JPanel(new GridBagLayout());
		statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		statusPanel.setBackground(new Color(220, 220, 220));
		statusPanel.add(statusLabel = new JLabel("Nothing selected"),
				new GridBagConstraints(0, 0, 1, 1, 1, 1, LINE_START, HORIZONTAL, new Insets(3, 10, 3, 0), 0, 0));
		statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
		downPart.add(statusPanel);
		add(downPart, BorderLayout.SOUTH);
	}

	private void installHandlers() {
		new mxRubberband(graphComponent);
		new EditorKeyboardHandler(graphComponent);
	}


	private void showGraphPopupMenu(MouseEvent e) {
		Point pt = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), graphComponent);
		new EditorPopupMenu(this).show(graphComponent, pt.x, pt.y);
		e.consume();
	}

	private void installListeners() {
		graphComponent.addMouseWheelListener(e -> {
			if (e.getSource() instanceof mxGraphOutline || e.isControlDown()) {
				if (e.getWheelRotation() < 0) graphComponent.zoomIn();
				else graphComponent.zoomOut();
			}
		});
		graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {

			public void mousePressed(MouseEvent e) {
				mouseReleased(e);
			}

			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) showGraphPopupMenu(e);
				processStatus();
				if (e.getClickCount() == 2)
					new EditPropertiesAction().actionPerformed(new ActionEvent(graphComponent, 0, "edit"));
				e.consume();
			}
		});

	}

	void setStatusLabel(String label) {
		statusLabel.setText(label);
	}

	void processStatus() {
		if (graphComponent.getGraph().getSelectionCount() == 0)
			statusLabel.setText("Nothing selected");
		else if (graphComponent.getGraph().getSelectionCount() == 1 && ((mxCell) graphComponent.getGraph().getSelectionCell()).getValue() instanceof FmuSpec)
			statusLabel.setText(infoOf((FmuSpec) ((mxCell) graphComponent.getGraph().getSelectionCell()).getValue()));
		else statusLabel.setText(graphComponent.getGraph().getSelectionCount() + " elements selected");
	}

	private String infoOf(FmuSpec spec) {
		if (spec.fileName().isEmpty() || !spec.file().exists()) return "FMU file not selected";
		ModelDescription modelDescription = DaccosimUtils.getModelDescription(spec.file());
		if (modelDescription == null) return "FMU couldn't be read. Check log for more details";
		return
				"modelName = " + modelDescription.getModelName() +
						"          fmiVersion = " + modelDescription.getFmiVersion() +
						"          generationTool = " + modelDescription.getGenerationTool() +
						(modelDescription instanceof org.javafmi.modeldescription.v2.ModelDescription ? "          generationDateAndTime = " + ((org.javafmi.modeldescription.v2.ModelDescription) modelDescription).getGenerationDateAndTime() : "");
	}

	public SimxFile getFile() {
		return currentFile;
	}

	void setFile(SimxFile file) {
		SimxFile oldValue = currentFile;
		currentFile = file;

		firePropertyChange("currentFile", oldValue, file);

		if (currentFile == null) currentFile = untitledFile();
		if (oldValue != file) {
			oldValue.close();
			updateTitle();
		}
	}

	public File getWorkingDirectory() {
		return currentFile.getParentFile();
	}

	boolean isModified() {
		return modified;
	}

	public void setModified(boolean modified) {
		boolean oldValue = this.modified;
		this.modified = modified;
		firePropertyChange("modified", oldValue, modified);
		if (oldValue != modified) updateTitle();
	}

	public mxGraphComponent getGraphComponent() {
		return graphComponent;
	}

	Action bind(String name, final Action action) {
		return bind(name, action, null);
	}

	Action bind(String name, final Action action, URL iconUrl) {
		AbstractAction newAction = new AbstractAction(name, (iconUrl != null) ? new ImageIcon(iconUrl) : null) {
			public void actionPerformed(ActionEvent e) {
				action.actionPerformed(new ActionEvent(getGraphComponent(), e.getID(), e.getActionCommand()));
			}
		};
		newAction.putValue(Action.SHORT_DESCRIPTION, action.getValue(Action.SHORT_DESCRIPTION));
		return newAction;
	}

	void updateTitle() {
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(this);
		if (frame != null) {
			String title = !isFileUntitled() ? currentFile.getAbsolutePath() : mxResources.get("newDiagram");
			if (modified) title += "*";
			frame.setTitle(title + " - " + mxResources.get("appTitle"));
		}
	}

	public boolean isFileUntitled() {
		return currentFile.getName().equals("_untitled_.simx");
	}

	void exit() {
		if (!canClose()) return;
		getFile().close();
		frame.dispose();
		System.exit(0);
	}

	private boolean canClose() {
		File file = new File(".notAskOnExit");
		JCheckBox checkbox = new JCheckBox("Do not show this message again.");
		boolean close = file.exists() ||
				JOptionPane.showConfirmDialog(this, new Object[]{"Are you sure you wish to exit?", checkbox}, "Exit", JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION;
		if (checkbox.isSelected()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return close;
	}

	private void createFrame() {
		frame = new JFrame();
		frame.setIconImage(new ImageIcon(Main.class.getResource("/images/logo.png")).getImage());
		frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		frame.setSize(new Dimension(800, 700));
		frame.setLocationRelativeTo(null);
		frame.getContentPane().add(this);
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				new EditorActions.ExitAction().actionPerformed(new ActionEvent(Editor.this, 0, "exit"));
			}
		});
		frame.setJMenuBar(menubar = new EditorMenuBar(this));
		frame.setVisible(true);
		updateTitle();
	}

	private mxGraph graph() {
		return graphComponent.getGraph();
	}

	mxUndoManager undoManager() {
		return this.undoManager;
	}

	void resetNotes() {
		setNotes(DEFAULT_NOTES_TEXT);
	}

	void setNotes(String notes) {
		SwingUtilities.invokeLater(() -> {
			this.notes.setText(notes);
			setModified(false);
		});
	}

	private void loadRecentFiles() {
		if (!recentFile.exists()) return;
		recentFiles.addAll(readRecentFiles());
	}

	private List<String> readRecentFiles() {
		try {
			return Files.readAllLines(recentFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	List<String> recentFiles() {
		return recentFiles;
	}

	void removeRecent(String filepath) {
		recentFiles.remove(filepath);
		menubar.reload(recentFiles);
		writeRecentFiles();
	}

	void addRecent(String filepath) {
		recentFiles.remove(filepath);
		if (recentFiles.size() == 10) recentFiles.remove(0);
		recentFiles.add(filepath);
		writeRecentFiles();
		menubar.reload(recentFiles);
	}

	private void writeRecentFiles() {
		try {
			Files.write(recentFile.toPath(), String.join("\n", recentFiles()).getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
