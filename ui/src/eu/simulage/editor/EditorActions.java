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

import com.mxgraph.io.mxCodec;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxResources;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;
import eu.simulage.DsgToMxGraph;
import eu.simulage.daccosim.DaccosimException;
import eu.simulage.daccosim.DngFile;
import eu.simulage.daccosim.view.FMU;
import eu.simulage.daccosim.view.Graph;
import eu.simulage.editor.specs.*;
import org.javafmi.kernel.Unzipper;
import org.w3c.dom.Document;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.function.Consumer;

import static eu.simulage.SimulationProvider.unsafeGetSimulation;
import static eu.simulage.daccosim.GraphExecutor.logger;
import static eu.simulage.editor.Dialogs.*;
import static eu.simulage.editor.Editor.untitledFile;
import static eu.simulage.editor.EditorUtils.*;
import static java.util.Arrays.stream;
import static javax.swing.JOptionPane.*;
import static javax.swing.SwingUtilities.windowForComponent;

class EditorActions {

	static final String SIM_EXT = "sim";
	static final String DNG_EXT = "dng";
	static final String DSG_EXT = "dsg";
	static final String SIMX_EXT = "simx";
	private static final String DNGX_EXT = "dngx";
	private static final String FMU_EXT = "fmu";
	private static final String CONFIG_SEPARATOR = "\n@@@@@@\n";
	private static final FileNameExtensionFilter FMU_FILTER = new FileNameExtensionFilter(mxResources.get("fmuFile"), FMU_EXT);
	private static final FileNameExtensionFilter SIMX_FILTER = new FileNameExtensionFilter(mxResources.get("simxFile"), SIMX_EXT);
	private static final FileNameExtensionFilter DNGX_FILTER = new FileNameExtensionFilter(mxResources.get("dngxFile"), DNGX_EXT);

	static String simFileOf(mxGraph graph) {
		mxCodec codec = new mxCodec();
		String xml = mxXmlUtils.getXml(codec.encode(graph.getModel()));
		xml += CONFIG_SEPARATOR + EditorUtils.serializedConfiguration();
		return xml;
	}

	private static void saveFile(Editor editor, String path) {
		getEditor().getFile().rename(path).save();
		getEditor().updateTitle();
		editor.setModified(false);
		logger().info("Saved file:" + path);
	}

	private static String saveFileChooser(Editor editor) {
		JFileChooser fc = new JFileChooser(editor.getWorkingDirectory());
		if (!editor.isFileUntitled()) fc.setSelectedFile(editor.getFile().simxFile());
		fc.addChoosableFileFilter(SIMX_FILTER);
		fc.setFileFilter(SIMX_FILTER);
		if (fc.showDialog(editor, mxResources.get("save")) != JFileChooser.APPROVE_OPTION) return null;

		String path = fc.getSelectedFile().getAbsolutePath();
		if (!path.toLowerCase().endsWith(SIMX_EXT)) path += "." + SIMX_EXT;
		if (new File(path).exists() && showConfirmDialog(editor,
				mxResources.get("overwriteExistingFile"), "Choose an option", YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION)
			return null;
		return path;
	}

	private static void checkFmus(SimxFile file, Editor editor) {
		try {
			checkFmus(file, editor, true);
		} catch (Exception e) {
			showErrorDialog(editor, "Multi-core loaded failed. Trying single-core\n" + e.getMessage());
			try {
				checkFmus(file, editor, false);
			} catch (Exception e1) {
				showErrorDialog(editor, e.getMessage());
			}
		}
	}

	private static void checkFmus(SimxFile file, Editor editor, boolean parallel) {
		int[] loadedFmus = {0};
		Consumer<File> consumer = fmu -> {
			unsafeGetSimulation(fmu);
			loadedFmus[0]++;
			editor.setStatusLabel("FMUs loaded " + loadedFmus[0] + "/" + file.fmus().size());
		};
		if (parallel) file.fmus().parallelStream().forEach(consumer);
		else file.fmus().forEach(consumer);
	}

	private static void resetEditor(Editor editor) {
		editor.resetNotes();
		editor.setModified(false);
		editor.undoManager().clear();
		editor.getGraphComponent().zoomAndCenter();
	}

	private static void openFile(Editor editor, String path) {
		EditorUtils.doInBackground(editor, () -> {
			try {
				SimxFile file = new SimxFile(path).open();
				readSimFile(editor, file.simFile());
				editor.setFile(file);
				checkFmus(file, editor);
				editor.setStatusLabel("Simx file loaded: " + file.getAbsolutePath());
			} catch (Throwable throwable) {
				throwable.printStackTrace();
				showErrorDialog(editor, throwable.getMessage() == null ? EditorUtils.stackToString(throwable) : throwable.getMessage());
			}
		});
	}

	private static void readSimFile(Editor editor, File selectedFile) {
		try {
			String fileContent = mxUtils.readFile(selectedFile.getAbsolutePath());
			String xml = fileContent.substring(0, fileContent.indexOf(CONFIG_SEPARATOR));
			Document document = mxXmlUtils.parseXml(xml);
			mxCodec codec = new mxCodec(document);
			codec.decode(document.getDocumentElement(), editor.graphComponent.getGraph().getModel());
			setConfiguration(fileContent.substring(fileContent.indexOf(CONFIG_SEPARATOR) + CONFIG_SEPARATOR.length()));
			resetEditor(editor);
			editor.setNotes(EditorUtils.configuration().notes());
		} catch (IOException ex) {
			logger().info("Error opening file: " + selectedFile.getAbsolutePath());
			ex.printStackTrace();
			showMessageDialog(editor.getGraphComponent(), ex.toString(), mxResources.get("error"), JOptionPane.ERROR_MESSAGE);
		}
	}

	public static class ExitAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			Editor editor = getEditor(e);

			if (editor != null && (!editor.isModified() || saveChangesDialog(editor) != SaveChangesResult.Cancelled))
				editor.exit();
		}

	}

	public static class MoveCells extends AbstractAction {

		private final double dx;
		private final double dy;

		MoveCells(double dx, double dy) {
			this.dx = dx;
			this.dy = dy;
		}

		public void actionPerformed(ActionEvent e) {
			Editor editor = getEditor(e);
			if (editor == null) return;
			mxGraph graph = editor.graphComponent.getGraph();
			if (graph.getSelectionCount() == 1 && ((mxCell) graph.getSelectionCell()).isEdge()) return;
			graph.moveCells(graph.getSelectionCells(), dx, dy);
		}

	}

	public static class SaveAction extends AbstractAction {
		final boolean showDialog;

		SaveAction(boolean showDialog) {
			this.showDialog = showDialog;
		}

		public void actionPerformed(ActionEvent e) {
			logger().info("Starting saving file");
			Editor editor = getEditor(e);
			if (editor == null) return;
			if (GraphChecker.graphNotCheck(editor)) return;
			String path =
					showDialog || editor.isFileUntitled() ?
							saveFileChooser(editor) : editor.getFile().getAbsolutePath();
			if (path == null) return;
			editor.addRecent(path);
			saveFile(editor, path);
		}

	}

	public static class HistoryAction extends AbstractAction {
		final boolean undo;

		HistoryAction(boolean undo) {
			this.undo = undo;
		}

		public void actionPerformed(ActionEvent e) {
			Editor editor = getEditor(e);

			if (editor != null) {
				if (undo) editor.undoManager().undo();
				else editor.undoManager().redo();
			}
		}
	}

	public static class NewAction extends AbstractAction {

		public void actionPerformed(ActionEvent e) {
			Editor editor = getEditor(e);
			if (editor == null) return;
			if (editor.isModified() && saveChangesDialog(editor) == SaveChangesResult.Cancelled) return;
			mxGraph graph = editor.getGraphComponent().getGraph();
			mxCell root = new mxCell();
			root.insert(new mxCell());
			graph.getModel().setRoot(root);
			resetEditor(editor);
			editor.setFile(untitledFile());
			setConfiguration(new ConfigurationSpec());
		}
	}

	public static class OpenAction extends AbstractAction {

		public void actionPerformed(ActionEvent e) {
			logger().info("Starting opening file");
			Editor editor = getEditor(e);
			if (editor == null) return;
			String path = showFileChooser(editor);
			if (path == null) return;
			openFile(editor, path);
			editor.addRecent(path);
			logger().info("File opened " + path);
		}

		private String showFileChooser(Editor editor) {
			if (editor.isModified() && saveChangesDialog(editor) == SaveChangesResult.Cancelled) return null;
			JFileChooser fc = new JFileChooser(editor.getWorkingDirectory());
			fc.setAcceptAllFileFilterUsed(false);
			fc.addChoosableFileFilter(SIMX_FILTER);
			fc.setFileFilter(SIMX_FILTER);
			int rc = fc.showDialog(editor, mxResources.get("openFile"));
			if (rc != JFileChooser.APPROVE_OPTION) return null;
			return fc.getSelectedFile().getAbsolutePath();
		}

	}

	public static class OpenRecentAction extends AbstractAction {

		private String filepath;

		OpenRecentAction(String filepath) {
			this.filepath = filepath;
		}

		public void actionPerformed(ActionEvent e) {
			Editor editor = getEditor(e);
			if (editor == null) return;
			if (editor.isModified() && saveChangesDialog(editor) == SaveChangesResult.Cancelled) return;
			logger().info("Starting opening file");
			if (!new File(filepath).exists()) {
				showErrorDialog(editor, "File not found " + filepath);
				editor.removeRecent(filepath);
			}
			openFile(editor, filepath);
			logger().info("File opened " + filepath);
		}

	}

	public static class ColorAction extends AbstractAction {

		final String name;
		final String key;

		ColorAction(String name, String key) {
			this.name = name;
			this.key = key;
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getSource() instanceof mxGraphComponent) {
				mxGraphComponent graphComponent = (mxGraphComponent) e.getSource();
				mxGraph graph = graphComponent.getGraph();

				if (graph.isSelectionEmpty()) return;
				Color newColor = JColorChooser.showDialog(graphComponent, name, null);
				if (newColor != null) graph.setCellStyles(key, mxUtils.hexString(newColor));
			}
		}
	}

	public static class EditPropertiesAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() instanceof mxGraphComponent) {
				mxGraphComponent graphComponent = (mxGraphComponent) e.getSource();
				mxGraph graph = graphComponent.getGraph();

				if (graph.getSelectionCount() == 1) {
					if (((mxICell) graph.getSelectionCell()).getValue() instanceof FmuSpec)
						Dialogs.showFmuForm(graphComponent, ((FmuSpec) ((mxICell) graph.getSelectionCell()).getValue()));
					else if (((mxICell) graph.getSelectionCell()).getValue() instanceof OffsetSpec)
						Dialogs.showOffsetForm(graphComponent, ((OffsetSpec) ((mxICell) graph.getSelectionCell()).getValue()));
					else if (((mxICell) graph.getSelectionCell()).getValue() instanceof GainSpec)
						Dialogs.showGainForm(graphComponent, ((GainSpec) ((mxICell) graph.getSelectionCell()).getValue()));
					else if (((mxICell) graph.getSelectionCell()).getValue() instanceof OperatorSpec)
						Dialogs.showOperatorForm(graphComponent, ((OperatorSpec) ((mxICell) graph.getSelectionCell()).getValue()));
					else if (((mxICell) graph.getSelectionCell()).getValue() instanceof ExternalInputSpec)
						Dialogs.showExternalInputForm(graphComponent, ((ExternalInputSpec) ((mxICell) graph.getSelectionCell()).getValue()));
					else if (((mxICell) graph.getSelectionCell()).getValue() instanceof ExternalOutputSpec)
						Dialogs.showExternalOutputForm(graphComponent, ((ExternalOutputSpec) ((mxICell) graph.getSelectionCell()).getValue()));
					else if (((mxICell) graph.getSelectionCell()).getValue() instanceof ArrowSpec)
						Dialogs.showArrowForm(graphComponent, ((ArrowSpec) ((mxICell) graph.getSelectionCell()).getValue()));
					graph.refresh();
				}
			}
		}
	}

	public static class FixArrowPointsAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() instanceof mxGraphComponent) {
				mxGraphComponent graphComponent = (mxGraphComponent) e.getSource();
				mxGraph graph = graphComponent.getGraph();
				if (graph.getSelectionCount() != 1 || !((mxICell) graph.getSelectionCell()).isEdge()) return;
				Dialogs.showFixArrowPointsDisplay(getEditor(e), ((mxICell) graph.getSelectionCell()));
			}
		}
	}

	static class ShowSettingsAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
			Dialogs.showConfiguration((Component) e.getSource());
		}
	}

	static class RunAction extends AbstractAction {

		@Override
		public void actionPerformed(ActionEvent e) {
			Editor editor = getEditor(e);
			if (editor == null) return;
			if (GraphChecker.graphNotCheck(editor)) return;
			if (editor.isFileUntitled()) {
				int dialog = showConfirmDialog(editor, "Co-simulation graph must be saved to be executed. Do you want to save it?", "Choose an option", YES_NO_OPTION);
				if (dialog == NO_OPTION) return;
				String path = saveFileChooser(editor);
				if (path == null)
					showMessageDialog(editor, "Co-simulation graph was not saved", "Impossible to run", ERROR_MESSAGE);
				else doExecute(editor, path);
			} else doExecute(editor, getEditor().getFile().getAbsolutePath());
		}

		private void doExecute(Editor editor, String path) {
			doInBackground(editor, () -> {
				if (editor.isModified() || editor.isFileUntitled()) saveFile(editor, path);
				showExecDisplay(editor, new ExecutionThread());
			});
		}
	}

	static class ValidateAction extends AbstractAction {

		@Override
		public void actionPerformed(ActionEvent e) {
			logger().info("Starting co-simulation validation");
			Editor editor = getEditor(e);
			if (editor == null) return;
			if (GraphChecker.graphNotCheck(editor)) return;
			showMessageDialog(windowForComponent(editor), "Graph is correctly validated", "Info", JOptionPane.INFORMATION_MESSAGE);
			logger().info("Finished co-simulation validation");
		}

	}

	static class ShowAboutAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
			Dialogs.showAbout((Component) e.getSource());
		}
	}

	static class FmuInfoAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
			Editor editor = getEditor(e);
			if (editor == null) return;
			showFmuInfo(editor, ((FmuSpec) ((mxCell) editor.getGraphComponent().getGraph().getSelectionCell()).getValue()));
		}
	}

	static class DefineFlowVariables extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
			Editor editor = getEditor(e);
			if (editor == null) return;
			defineFlowVariables(editor, ((FmuSpec) ((mxCell) editor.getGraphComponent().getGraph().getSelectionCell()).getValue()));
		}
	}

	public static class DngxImportAction extends AbstractAction {

		@SuppressWarnings("ConstantConditions")
		public void actionPerformed(ActionEvent e) {
			logger().info("Starting importing dngx file");
			Editor editor = getEditor(e);
			if (editor == null) return;
			String path = showFileChooser(editor);
			if (path == null) return;
			EditorUtils.doInBackground(editor, () -> {
				try {
					SimxFile file = untitledFile();
					editor.setFile(file);
					Path directory = Files.createTempDirectory("zip");
					new Unzipper(new File(path)).unzipAll(directory.toFile());
					File dngFile = stream(Objects.requireNonNull(directory.toFile().listFiles())).filter(f -> f.getName().endsWith(DNG_EXT)).findFirst().orElse(null);
					if (dngFile == null) throw new DaccosimException("Dng file not found in " + path);
					Graph graph = DngFile.importDng(dngFile);
					importFmus(dngFile, file, graph);
					checkFmus(file, editor);
					DsgToMxGraph.importInEditor(editor, graph);
					resetEditor(editor);
					editor.setModified(true);
					editor.setStatusLabel("Dngx file imported: " + path);
				} catch (Throwable throwable) {
					throwable.printStackTrace();
					showErrorDialog(editor, throwable.getMessage() == null ? EditorUtils.stackToString(throwable) : throwable.getMessage());
				}
			});
			logger().info("Dng file imported " + path);
		}

		private void importFmus(File dngFile, SimxFile simxFile, Graph graph) {
			graph.nodes().stream()
					.filter(n -> n instanceof eu.simulage.daccosim.view.FMU)
					.map(n -> (FMU) n)
					.forEach(fmu -> {
						File fmuFile = new File(dngFile.getParentFile(), fmu.path());
						if (!fmuFile.exists())
							throw new DaccosimException("Fmu " + fmuFile.getName() + " not found inside dngx");
						simxFile.addFmu(fmuFile);
					});
		}

		private String showFileChooser(Editor editor) {
			if (editor.isModified() && saveChangesDialog(editor) == SaveChangesResult.Cancelled) return null;
			JFileChooser fc = new JFileChooser(editor.getWorkingDirectory());
			fc.setAcceptAllFileFilterUsed(false);
			fc.addChoosableFileFilter(DNGX_FILTER);
			fc.setFileFilter(DNGX_FILTER);
			fc.setDialogTitle(mxResources.get("importDng"));
			int rc = fc.showDialog(editor, "Import");
			if (rc != JFileChooser.APPROVE_OPTION) return null;
			return fc.getSelectedFile().getAbsolutePath();
		}
	}

	static class FmuExportAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
			Editor editor = getEditor(e);
			if (editor == null) return;
			if (GraphChecker.graphNotCheck(editor)) return;
			String file = showFileChooser(editor, editor.graphComponent);
			if (file != null) Dialogs.showFmuExportDisplay(editor, new File(file));
		}

		private String showFileChooser(Editor editor, mxGraphComponent graphComponent) {
			JFileChooser fc = new JFileChooser(editor.getWorkingDirectory());
			fc.setAcceptAllFileFilterUsed(false);
			fc.addChoosableFileFilter(FMU_FILTER);
			if (editor.getFile() != null)
				fc.setSelectedFile(new File(editor.getFile().getAbsolutePath().replaceAll("." + SIMX_EXT, "." + FMU_EXT)));
			fc.setFileFilter(FMU_FILTER);
			int rc = fc.showDialog(editor, mxResources.get("exportFmu"));
			if (rc != JFileChooser.APPROVE_OPTION) return null;
			String filename = fc.getSelectedFile().getAbsolutePath();
			if (!filename.toLowerCase().endsWith(FMU_EXT)) filename += "." + FMU_EXT;

			if (new File(filename).exists() && showConfirmDialog(graphComponent,
					mxResources.get("overwriteExistingFile"), "Choose an option", YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION)
				return null;
			return filename;
		}
	}

	public static class SaveFmuAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
			logger().info("Saving fmu");
			Editor editor = getEditor(e);
			if (editor == null) return;
			mxGraphComponent graphComponent = (mxGraphComponent) e.getSource();
			mxGraph graph = graphComponent.getGraph();
			Object spec = ((mxICell) graph.getSelectionCell()).getValue();
			if (!(spec instanceof FmuSpec)) return;
			FmuSpec fmuSpec = (FmuSpec) spec;
			if (fmuSpec.fileName().isEmpty() || !fmuSpec.file().exists()) {
				showErrorDialog(editor, "Fmu filename is empty or file cannot be found");
				return;
			}
			String file = showFileChooser(editor, editor.graphComponent, fmuSpec);
			if (file == null) return;
			try {
				Files.copy(fmuSpec.file().toPath(), new File(file).toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e1) {
				logger().severe(e1.getMessage());
			}
			logger().info("Fmu saved on " + file);
		}

		private String showFileChooser(Editor editor, mxGraphComponent graphComponent, FmuSpec fmuSpec) {
			JFileChooser fc = new JFileChooser(editor.getWorkingDirectory());
			fc.setAcceptAllFileFilterUsed(false);
			fc.addChoosableFileFilter(FMU_FILTER);
			if (editor.getFile() != null)
				fc.setSelectedFile(new File(editor.getFile().getAbsolutePath().replaceAll("." + SIM_EXT, "." + FMU_EXT)));
			fc.setFileFilter(FMU_FILTER);
			fc.setSelectedFile(new File(editor.getWorkingDirectory(), fmuSpec.label() + "." + FMU_EXT));
			int rc = fc.showDialog(editor, mxResources.get("exportFmu"));
			if (rc != JFileChooser.APPROVE_OPTION) return null;
			String filename = fc.getSelectedFile().getAbsolutePath();
			if (!filename.toLowerCase().endsWith(FMU_EXT)) filename += "." + FMU_EXT;

			if (new File(filename).exists() && showConfirmDialog(graphComponent,
					mxResources.get("overwriteExistingFile"), "Choose an option", YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION)
				return null;
			return filename;
		}
	}
}
