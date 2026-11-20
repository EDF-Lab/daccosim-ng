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

import com.mxgraph.swing.handler.mxKeyboardHandler;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.util.mxGraphActions;

import javax.swing.*;
import java.awt.event.KeyEvent;

class EditorKeyboardHandler extends mxKeyboardHandler {

	EditorKeyboardHandler(mxGraphComponent graphComponent) {
		super(graphComponent);
	}

	protected InputMap getInputMap(int condition) {
		InputMap map = super.getInputMap(condition);
		if (condition == JComponent.WHEN_FOCUSED) {
			map.put(KeyStroke.getKeyStroke("control S"), "save");
			map.put(KeyStroke.getKeyStroke("control shift S"), "saveAs");
			map.put(KeyStroke.getKeyStroke("control N"), "new");
			map.put(KeyStroke.getKeyStroke("control O"), "open");
			map.put(KeyStroke.getKeyStroke("control Z"), "undo");
			map.put(KeyStroke.getKeyStroke("control Y"), "redo");
			map.put(KeyStroke.getKeyStroke("control shift V"), "selectVertices");
			map.put(KeyStroke.getKeyStroke("control shift E"), "selectEdges");
			map.put(KeyStroke.getKeyStroke("UP"), "goUp");
			map.put(KeyStroke.getKeyStroke("DOWN"), "goDown");
			map.put(KeyStroke.getKeyStroke("RIGHT"), "goRight");
			map.put(KeyStroke.getKeyStroke("LEFT"), "goLeft");
			map.put(KeyStroke.getKeyStroke("control TAB"), "selectNext");
			map.put(KeyStroke.getKeyStroke("control shift TAB"), "selectPrevious");

			map.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "properties");
		}

		return map;
	}

	protected ActionMap createActionMap() {
		ActionMap map = super.createActionMap();

		map.put("goUp", new EditorActions.MoveCells(0, -10));
		map.put("goDown", new EditorActions.MoveCells(0, 10));
		map.put("goLeft", new EditorActions.MoveCells(-10, 0));
		map.put("goRight", new EditorActions.MoveCells(10, 0));
		map.put("save", new EditorActions.SaveAction(false));
		map.put("saveAs", new EditorActions.SaveAction(true));
		map.put("new", new EditorActions.NewAction());
		map.put("open", new EditorActions.OpenAction());
		map.put("undo", new EditorActions.HistoryAction(true));
		map.put("redo", new EditorActions.HistoryAction(false));
		map.put("selectVertices", mxGraphActions.getSelectVerticesAction());
		map.put("selectEdges", mxGraphActions.getSelectEdgesAction());
		map.put("properties", new EditorActions.EditPropertiesAction());

		return map;
	}

}
