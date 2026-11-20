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
import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.util.mxGraphTransferable;
import com.mxgraph.swing.util.mxSwingConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource;
import com.mxgraph.util.mxRectangle;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serializable;

import static eu.simulage.editor.EditorUtils.*;
import static eu.simulage.editor.Main.*;
import static java.awt.Font.PLAIN;

class EditorPalette extends JPanel {

	private JLabel selectedEntry = null;
	private final mxEventSource eventSource = new mxEventSource(this);

	@SuppressWarnings("SpellCheckingInspection")
	EditorPalette() {
		setBackground(new Color(220, 220, 220));
		setLayout(new FlowLayout(FlowLayout.LEADING, 5, 5));
		addMouseListener(new MouseListener() {

			public void mousePressed(MouseEvent e) {
				clearSelection();
			}

			public void mouseClicked(MouseEvent e) {
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}

			public void mouseReleased(MouseEvent e) {
			}

		});
		setTransferHandler(new TransferHandler() {
			public boolean canImport(JComponent comp, DataFlavor[] flavors) {
				return true;
			}
		});

		addTemplate("Fmu", new ImageIcon(imageOf("rectangle.png")), FMU, 120);
		addTemplate("External Input", new ImageIcon(imageOf("triangle.png")), EXTERNAL_INPUT, 80);
		addTemplate("External Output", new ImageIcon(imageOf("itriangle.png")), EXTERNAL_OUTPUT, 80);
		addTemplate("Adder", new ImageIcon(imageOf("adder.png")), ADDER, 80);
		addTemplate("Offset", new ImageIcon(imageOf("offset.png")), OFFSET, 80);
		addTemplate("Multiplier", new ImageIcon(imageOf("multiplier.png")), MULTIPLIER, 80);
		addTemplate("Gain", new ImageIcon(imageOf("gain.png")), GAIN, 80);
	}

	private void clearSelection() {
		setSelectionEntry(null, null);
	}

	private void setSelectionEntry(JLabel entry, mxGraphTransferable t) {
		JLabel previous = selectedEntry;
		selectedEntry = entry;
		if (previous != null) {
			previous.setBorder(null);
			previous.setOpaque(false);
		}
		if (selectedEntry != null) {
			selectedEntry.setBorder(ShadowBorder.getSharedInstance());
			selectedEntry.setOpaque(true);
		}
		eventSource.fireEvent(new mxEventObject(mxEvent.SELECT, "entry", selectedEntry, "transferable", t, "previous", previous));
	}

	private void addTemplate(final String name, ImageIcon icon, String style, int width) {
		mxCell cell = new mxCell("", new mxGeometry(0, 0, width, 80), style);
		cell.setVertex(true);
		mxRectangle bounds = (mxGeometry) cell.getGeometry().clone();
		final mxGraphTransferable t = new mxGraphTransferable(new Object[]{cell}, bounds);
		if (icon != null && (icon.getIconWidth() > 32 || icon.getIconHeight() > 32))
			icon = new ImageIcon(icon.getImage().getScaledInstance(32, 32, 0));
		final JLabel entry = new JLabel(icon);
		entry.setPreferredSize(new Dimension(75, 75));
		entry.setBackground(EditorPalette.this.getBackground().brighter());
		entry.setFont(new Font(entry.getFont().getFamily(), PLAIN, 10));
		entry.setVerticalTextPosition(JLabel.BOTTOM);
		entry.setHorizontalTextPosition(JLabel.CENTER);
		entry.setIconTextGap(0);
		entry.setToolTipText(name);
		entry.setText(name);
		entry.addMouseListener(new MouseListener() {

			public void mousePressed(MouseEvent e) {
				setSelectionEntry(entry, t);
			}

			public void mouseClicked(MouseEvent e) {
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}

			public void mouseReleased(MouseEvent e) {
			}

		});
		DragGestureListener dragGestureListener = e -> e.startDrag(null, mxSwingConstants.EMPTY_IMAGE, new Point(), t, null);
		DragSource dragSource = new DragSource();
		dragSource.createDefaultDragGestureRecognizer(entry,
				DnDConstants.ACTION_COPY, dragGestureListener);
		add(entry);
	}

	static class ShadowBorder implements Border, Serializable {
		private static final ShadowBorder sharedInstance = new ShadowBorder();
		private final Insets insets;

		private ShadowBorder() {
			insets = new Insets(0, 0, 2, 2);
		}

		private static Color average(Color c1, Color c2) {
			int red = c1.getRed() + (c2.getRed() - c1.getRed()) / 2;
			int green = c1.getGreen() + (c2.getGreen() - c1.getGreen()) / 2;
			int blue = c1.getBlue() + (c2.getBlue() - c1.getBlue()) / 2;
			return new Color(red, green, blue);
		}

		static ShadowBorder getSharedInstance() {
			return sharedInstance;
		}

		public Insets getBorderInsets(Component c) {
			return insets;
		}

		public boolean isBorderOpaque() {
			return false;
		}

		public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
			Color bg = c.getBackground();
			if (c.getParent() != null) bg = c.getParent().getBackground();
			if (bg != null) {
				Color mid = bg.darker();
				Color edge = average(mid, bg);

				g.setColor(bg);
				g.drawLine(0, h - 2, w, h - 2);
				g.drawLine(0, h - 1, w, h - 1);
				g.drawLine(w - 2, 0, w - 2, h);
				g.drawLine(w - 1, 0, w - 1, h);

				// draw the drop-shadow
				g.setColor(mid);
				g.drawLine(1, h - 2, w - 2, h - 2);
				g.drawLine(w - 2, 1, w - 2, h - 2);

				g.setColor(edge);
				g.drawLine(2, h - 1, w - 2, h - 1);
				g.drawLine(w - 1, 2, w - 1, h - 2);
			}
		}
	}
}
