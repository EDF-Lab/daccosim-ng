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
import com.mxgraph.swing.util.mxGraphActions;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxResources;
import eu.simulage.editor.specs.FmuSpec;

import javax.swing.*;

import static eu.simulage.editor.Main.imageOf;

class EditorPopupMenu extends JPopupMenu {

	@SuppressWarnings("SpellCheckingInspection")
	EditorPopupMenu(Editor editor) {
		if (editor.getGraphComponent().getGraph().isSelectionEmpty()) return;
		add(editor.bind(mxResources.get("undo"), new EditorActions.HistoryAction(true), imageOf("undo.gif")));
		add(editor.bind(mxResources.get("redo"), new EditorActions.HistoryAction(false), imageOf("redo.gif")));
		addSeparator();
		add(editor.bind(mxResources.get("cut"), TransferHandler.getCutAction(), imageOf("cut.gif")));
		add(editor.bind(mxResources.get("copy"), TransferHandler.getCopyAction(), imageOf("copy.gif")));
		add(editor.bind(mxResources.get("paste"), TransferHandler.getPasteAction(), imageOf("paste.gif")));
		addSeparator();
		add(editor.bind(mxResources.get("delete"), mxGraphActions.getDeleteAction(), imageOf("delete.gif")));
		addSeparator();
		add(editor.bind(mxResources.get("selectAll"), mxGraphActions.getSelectAllAction()));
		add(editor.bind(mxResources.get("selectNone"), mxGraphActions.getSelectNoneAction()));
		addSeparator();
		add(editor.bind(mxResources.get("fillcolor"), new EditorActions.ColorAction("Fillcolor", mxConstants.STYLE_FILLCOLOR), imageOf("fillcolor.gif")));
		add(editor.bind(mxResources.get("fontcolor"), new EditorActions.ColorAction("Fontcolor", mxConstants.STYLE_FONTCOLOR), imageOf("fontcolor.gif")));
		addSeparator();
		Object spec = ((mxCell) editor.getGraphComponent().getGraph().getSelectionCell()).getValue();
		if (spec instanceof FmuSpec) {
			add(editor.bind(mxResources.get("showFmuInfo"), new EditorActions.FmuInfoAction()));
			add(editor.bind(mxResources.get("defineFlowVariables"), new EditorActions.DefineFlowVariables()));
			add(editor.bind(mxResources.get("saveFmuAs"), new EditorActions.SaveFmuAction()));
		}
		if (((mxCell) editor.getGraphComponent().getGraph().getSelectionCell()).isEdge()) {
			add(editor.bind(mxResources.get("fixPoints"), new EditorActions.FixArrowPointsAction()));
		}

		add(editor.bind(mxResources.get("editProperties"), new EditorActions.EditPropertiesAction()));
	}
}
