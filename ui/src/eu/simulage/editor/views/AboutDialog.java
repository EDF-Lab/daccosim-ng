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

import eu.simulage.daccosim.DaccosimUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class AboutDialog extends JPanel {

	public AboutDialog() {
		JPanel imagePanel = getImagePanel();
		imagePanel.add(textPanel(), BorderLayout.SOUTH);
		add(imagePanel);
		setVisible(true);
	}

	private JPanel textPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(label("\n"));
		panel.add(label("Daccosim New Generation editor " + DaccosimUtils.YEAR, 16, Font.BOLD));
		panel.add(label("co-simulation made simpler", 14, Font.PLAIN));
		panel.add(label(DaccosimUtils.VERSION + " - " + DaccosimUtils.DATE));
		panel.add(label("\n"));
		panel.add(label("Daccosim is a collaborative development effort between"));
		panel.add(label("Électricité de France (France)"));
		panel.add(label("CentraleSupélec (France)"));
		panel.add(label("EIFER - European Institute for Energy Research (Germany)"));
		panel.add(label("SIANI - Institute of Intelligent Systems and Numerical Applications in Engineering (Spain)"));
		panel.add(label("Monentia SL(Spain)"));
		JPanel license = new JPanel();
		license.add(label("Daccosim is licensed under the"));
		license.add(linkLabel("https://www.gnu.org/licenses/lgpl", "LGPL v3"));
		panel.add(license);
		JPanel javaFmi = new JPanel();
		javaFmi.add(label("As this software utilizes it, we are grateful to the authors and the contributors of"));
		javaFmi.add(linkLabel("https://bitbucket.org/siani/javafmi/wiki/Home", "JavaFMI"));
		panel.add(javaFmi);
		panel.add(label(System.getProperty("os.name") + " - Java version " + System.getProperty("java.version", "undefined")));
		panel.add(label("\n"));
		panel.add(linkLabel("http://bitbucket.org/simulage/daccosim", null));
		panel.add(label("\n"));
		return panel;
	}

	private JLabel linkLabel(String url, String text) {
		JLabel link = new JLabel(text == null ? url : text);
		link.setCursor(new Cursor(Cursor.HAND_CURSOR));
		link.setForeground(Color.BLUE);
		link.setAlignmentX(Component.CENTER_ALIGNMENT);
		link.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					Desktop.getDesktop().browse(new URI(url));
				} catch (URISyntaxException | IOException ignored) {
				}
			}
		});
		return link;
	}

	private JPanel getImagePanel() {
		final ImageIcon backgroundImage = new ImageIcon(AboutDialog.class.getResource("/images/splash.png"));
		JPanel imagePanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				g.drawImage(backgroundImage.getImage(), 20, 90, 450, 135, null);
			}

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(500, 535);
			}
		};
		imagePanel.setLayout(new BorderLayout());
		return imagePanel;
	}

	private JLabel label(String text) {
		return label(text, 11, Font.PLAIN);
	}

	private JLabel label(String text, int size, int style) {
		JLabel jLabel = new JLabel(text);
		jLabel.setFont(new Font("arial", style, size));
		jLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		return jLabel;
	}

}
