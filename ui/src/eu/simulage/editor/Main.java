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

import com.mxgraph.io.mxCodecRegistry;
import com.mxgraph.io.mxObjectCodec;
import eu.simulage.editor.specs.*;
import eu.simulage.editor.views.AboutDialog;
import org.javafmi.kernel.Files;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.URL;
import java.time.Instant;

public class Main extends Editor {

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private Main() throws FileNotFoundException {
		super(new GraphComponent(new Graph()));
		EditorUtils.fillWith(graphComponent);
		addCodecsForSerialization();
		new File("editor.log").delete();
		System.setOut(new PrintStream(new File("editor.log")));
		System.setErr(new PrintStream(new File("editor.log")));
		new Thread(this::purgeTemp).start();
	}

	public static void main(String[] args) throws FileNotFoundException {
		splashScreen();
		new Main();
	}

	private static void splashScreen() {
		JWindow window = new JWindow();
		window.getContentPane().add(new AboutDialog());
		window.setSize(500, 540);
		window.setLocationRelativeTo(null);
		window.setVisible(true);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		window.setVisible(false);
	}

	static URL imageOf(String name) {
		return Main.class.getResource("/images/" + name);
	}

	private void purgeTemp() {
		File file = new File(System.getProperty("java.io.tmpdir"));
		File[] files = file.listFiles();
		if (files == null) return;
		long now = Instant.now().toEpochMilli();
		for (File folder : files) {
			if (!folder.getName().startsWith("md") && !folder.getName().startsWith("fmu_") && !folder.getName().startsWith("simx"))
				continue;
			if (now - folder.lastModified() < 7 * 24 * 3600 * 1000L) continue;
			Files.deleteAll(folder);
		}
	}

	private void addCodecsForSerialization() {
		mxCodecRegistry.addPackage("eu.simulage.daccosim.specs");
		mxCodecRegistry.register(new mxObjectCodec(new FmuSpec()));
		mxCodecRegistry.register(new mxObjectCodec(new AdderSpec()));
		mxCodecRegistry.register(new mxObjectCodec(new OffsetSpec()));
		mxCodecRegistry.register(new mxObjectCodec(new MultiplierSpec()));
		mxCodecRegistry.register(new mxObjectCodec(new GainSpec()));
		mxCodecRegistry.register(new mxObjectCodec(new ArrowSpec()));
		mxCodecRegistry.register(new mxObjectCodec(new ConnectionSpec()));
		mxCodecRegistry.register(new mxObjectCodec(new ExternalOutputSpec()));
		mxCodecRegistry.register(new mxObjectCodec(new VariableSpec()));
		mxCodecRegistry.register(new mxObjectCodec(new ExternalInputSpec()));
	}


}
