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
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.handler.mxSelectionCellsHandler;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxGraph;
import eu.simulage.editor.specs.*;
import org.w3c.dom.Document;
import siani.javafmi.Matryoshka;

import java.awt.*;

class GraphComponent extends mxGraphComponent {

	GraphComponent(mxGraph graph) {
		super(graph);
		setToolTips(true);
		setGridVisible(true);
		getConnectionHandler().setCreateTarget(false);
		mxCodec codec = new mxCodec();
		Document doc = mxUtils.loadDocument(Main.class.getResource("/daccosim-style.xml").toString());
		codec.decode(doc.getDocumentElement(), graph.getStylesheet());
		getViewport().setOpaque(true);
		getViewport().setBackground(Color.WHITE);
	}

	public Object[] importCells(Object[] cells, double dx, double dy, Object target, Point location) {
		mxICell cell = (mxICell) cells[0];
		if (!(cell.getValue() instanceof String)) return super.importCells(cells, dx, dy, target, location);

		if (cell.getStyle().equals(EditorUtils.FMU) || cell.getStyle().equals(EditorUtils.MATRYOSHKA)) {
			cell.setValue(Dialogs.showFmuForm(this, new FmuSpec()));
			if (cell.getValue() == null) return null;
			if (((FmuSpec) cell.getValue()).isMatryoshka()) cell.setStyle(EditorUtils.MATRYOSHKA);
		}
		if (cell.getStyle().equals(EditorUtils.ADDER)) cell.setValue(new AdderSpec());
		if (cell.getStyle().equals(EditorUtils.OFFSET)) cell.setValue(new OffsetSpec());
		if (cell.getStyle().equals(EditorUtils.MULTIPLIER)) cell.setValue(new MultiplierSpec());
		if (cell.getStyle().equals(EditorUtils.GAIN)) cell.setValue(new GainSpec());
		if (cell.getStyle().equals(EditorUtils.EXTERNAL_INPUT)) cell.setValue(new ExternalInputSpec());
		if (cell.getStyle().equals(EditorUtils.EXTERNAL_OUTPUT)) cell.setValue(new ExternalOutputSpec());
		if (cell.isEdge()) cell.setValue(new ArrowSpec());

		return super.importCells(cells, dx, dy, target, location);
	}

	@Override
	protected mxSelectionCellsHandler createSelectionCellsHandler() {
		return new mxSelectionCellsHandler(this){

			@Override
			public void refresh() {
				super.refresh();
				EditorUtils.getEditor(GraphComponent.this).processStatus();
			}
		};
	}
}
