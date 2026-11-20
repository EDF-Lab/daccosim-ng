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

import com.mxgraph.swing.util.mxGraphActions;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxResources;
import eu.simulage.editor.EditorActions.*;

import javax.swing.*;
import java.util.List;

import static eu.simulage.editor.Main.imageOf;

class EditorMenuBar extends JMenuBar {

	private JMenu recentFilesMenu;
	private Editor editor;

	EditorMenuBar(final Editor editor) {
		this.editor = editor;
		createFileMenu();
		createEditMenu();
		createSimulageMenu();
		createHelpMenu();
	}

	@SuppressWarnings("SpellCheckingInspection")
	private void createFileMenu() {
		JMenu menu = add(new JMenu(mxResources.get("file")));
		menu.add(editor.bind(mxResources.get("new"), new NewAction(), imageOf("new.gif")));
		menu.add(editor.bind(mxResources.get("openFile"), new OpenAction(), imageOf("open.gif")));
		menu.add(recentFilesMenu = new JMenu(mxResources.get("openRecent")));
		reload(editor.recentFiles());
		menu.addSeparator();
		menu.add(editor.bind(mxResources.get("save"), new SaveAction(false), imageOf("save.gif")));
		menu.add(editor.bind(mxResources.get("saveAs"), new SaveAction(true), imageOf("saveas.gif")));
		menu.addSeparator();
		menu.add(editor.bind(mxResources.get("importDng"), new DngxImportAction()));
		menu.add(editor.bind(mxResources.get("exportFmu"), new EditorActions.FmuExportAction()));
		menu.addSeparator();
		menu.add(editor.bind(mxResources.get("exit"), new ExitAction()));
	}

	private void createEditMenu() {
		JMenu menu = add(new JMenu(mxResources.get("edit")));
		menu.add(editor.bind(mxResources.get("undo"), new HistoryAction(true), imageOf("undo.gif")));
		menu.add(editor.bind(mxResources.get("redo"), new HistoryAction(false), imageOf("redo.gif")));
		menu.addSeparator();
		menu.add(editor.bind(mxResources.get("cut"), TransferHandler.getCutAction(), imageOf("cut.gif")));
		menu.add(editor.bind(mxResources.get("copy"), TransferHandler.getCopyAction(), imageOf("copy.gif")));
		menu.add(editor.bind(mxResources.get("paste"), TransferHandler.getPasteAction(), imageOf("paste.gif")));
		menu.addSeparator();
		menu.add(editor.bind(mxResources.get("delete"), mxGraphActions.getDeleteAction(), imageOf("delete.gif")));
		menu.addSeparator();
		menu.add(editor.bind(mxResources.get("selectAll"), mxGraphActions.getSelectAllAction()));
		menu.add(editor.bind(mxResources.get("selectNone"), mxGraphActions.getSelectNoneAction()));
		menu.addSeparator();
		menu.add(editor.bind(mxResources.get("fillcolor"), new ColorAction("Fillcolor", mxConstants.STYLE_FILLCOLOR), imageOf("fillcolor.gif")));
		menu.add(editor.bind(mxResources.get("fontcolor"), new ColorAction("Fontcolor", mxConstants.STYLE_FONTCOLOR), imageOf("fontcolor.gif")));
		menu.addSeparator();
		menu.add(editor.bind(mxResources.get("editProperties"), new EditPropertiesAction()));
	}

	private void createSimulageMenu() {
		JMenu menu = add(new JMenu(mxResources.get("simulation")));
		menu.add(editor.bind(mxResources.get("validate"), new ValidateAction(), imageOf("check.gif")));
		menu.add(editor.bind(mxResources.get("settings"), new ShowSettingsAction(), imageOf("settings.gif")));
		menu.add(editor.bind(mxResources.get("run"), new RunAction(), imageOf("play.png")));
	}

	private void createHelpMenu() {
		JMenu menu = add(new JMenu(mxResources.get("help")));
		menu.add(editor.bind(mxResources.get("aboutGraphEditor"), new EditorActions.ShowAboutAction(), imageOf("logo_16.png")));
	}

	void reload(List<String> recentFiles) {
		recentFilesMenu.removeAll();
		for (int i = recentFiles.size() - 1; i >= 0; i--)
			recentFilesMenu.add(editor.bind(recentFiles.get(i), new OpenRecentAction(recentFiles.get(i))));
	}
}