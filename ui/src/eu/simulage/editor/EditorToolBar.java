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

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.util.mxGraphActions;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxResources;
import com.mxgraph.view.mxGraphView;
import eu.simulage.editor.EditorActions.HistoryAction;
import eu.simulage.editor.EditorActions.NewAction;
import eu.simulage.editor.EditorActions.OpenAction;
import eu.simulage.editor.EditorActions.SaveAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

import static eu.simulage.editor.Main.imageOf;
import static javax.swing.JOptionPane.showMessageDialog;

class EditorToolBar extends JToolBar {

	private boolean ignoreZoomChange = false;

	EditorToolBar(final Editor editor) {
		super(SwingConstants.HORIZONTAL);
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3), getBorder()));
		setFloatable(false);
		add(editor.bind("New", new NewAction(), imageOf("new.gif")));
		add(editor.bind("Open", new OpenAction(), imageOf("open.gif")));
		add(editor.bind("Save", new SaveAction(false), imageOf("save.gif")));
		addSeparator();
		add(editor.bind("Cut", TransferHandler.getCutAction(), imageOf("cut.gif")));
		add(editor.bind("Copy", TransferHandler.getCopyAction(), imageOf("copy.gif")));
		add(editor.bind("Paste", TransferHandler.getPasteAction(), imageOf("paste.gif")));
		addSeparator();
		add(editor.bind("Delete", mxGraphActions.getDeleteAction(), imageOf("delete.gif")));
		addSeparator();
		add(editor.bind("Undo", new HistoryAction(true), imageOf("undo.gif")));
		add(editor.bind("Redo", new HistoryAction(false), imageOf("redo.gif")));
		add(createZoomCombo(editor));
		addSeparator();
		add(editor.bind("Validate", new EditorActions.ValidateAction(), imageOf("check.gif")));
		add(editor.bind("Settings", new EditorActions.ShowSettingsAction(), imageOf("settings.gif")));
		add(editor.bind("Play", new EditorActions.RunAction(), imageOf("play.png")));
	}

	private JComboBox createZoomCombo(Editor editor) {
		final mxGraphView view = editor.getGraphComponent().getGraph()
				.getView();
		@SuppressWarnings("unchecked") final JComboBox zoomCombo = new JComboBox(new Object[]{"400%", "200%", "150%", "100%", "75%", "50%",
				mxResources.get("page"), mxResources.get("width"), mxResources.get("actualSize")});
		zoomCombo.setEditable(true);
		zoomCombo.setMinimumSize(new Dimension(75, 0));
		zoomCombo.setPreferredSize(new Dimension(75, 0));
		zoomCombo.setMaximumSize(new Dimension(75, 100));
		zoomCombo.setMaximumRowCount(9);
		mxIEventListener scaleTracker = (sender, evt) -> {
			ignoreZoomChange = true;
			try {
				zoomCombo.setSelectedItem((int) Math.round(100 * view.getScale()) + "%");
			} finally {
				ignoreZoomChange = false;
			}
		};
		view.getGraph().getView().addListener(mxEvent.SCALE, scaleTracker);
		view.getGraph().getView().addListener(mxEvent.SCALE_AND_TRANSLATE, scaleTracker);
		scaleTracker.invoke(null, null);

		zoomCombo.addActionListener((ActionEvent e) -> {
			mxGraphComponent graphComponent = editor.getGraphComponent();
			if (!ignoreZoomChange) {
				@SuppressWarnings("ConstantConditions")
				String zoom = zoomCombo.getSelectedItem().toString();

				if (zoom.equals(mxResources.get("page"))) {
					graphComponent.setPageVisible(true);
					graphComponent.setZoomPolicy(mxGraphComponent.ZOOM_POLICY_PAGE);
				} else if (zoom.equals(mxResources.get("width"))) {
					graphComponent.setPageVisible(true);
					graphComponent.setZoomPolicy(mxGraphComponent.ZOOM_POLICY_WIDTH);
				} else if (zoom.equals(mxResources.get("actualSize"))) graphComponent.zoomActual();
				else {
					try {
						zoom = zoom.replace("%", "");
						double scale = Math.min(16, Math.max(0.01, Double.parseDouble(zoom) / 100));
						graphComponent.zoomTo(scale, graphComponent.isCenterZoom());
					} catch (Exception ex) {
						showMessageDialog(editor, ex.getMessage());
					}
				}
			}
		});
		return zoomCombo;
	}

}
