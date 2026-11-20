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

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static eu.simulage.daccosim.GraphExecutor.logger;
import static eu.simulage.editor.EditorUtils.daccosimJarBytes;
import static eu.simulage.editor.EditorUtils.getEditor;
import static org.javafmi.kernel.Files.deleteAll;


public class ExecutionThread extends SwingWorker<Void, Void> {

	private static final String CORE_JAR = "core.jar";
	@SuppressWarnings("CanBeFinal")
	private static File daccosimFolder;

	static {
		try {
			daccosimFolder = Files.createTempDirectory("daccosim").toFile();
			Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteAll(daccosimFolder)));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private final List<OnLoggedString> onLoggedStringList = new ArrayList<>();
	private Runnable onFinish;
	private Process process;
	private boolean daccosimJarWritten = false;
	private File outputFile;
	private String exception;

	@Override
	protected Void doInBackground() throws Exception {
		logger().info("Starting co-simulation execution");
		process = createProcess();
		Thread inputThread = readingThreadOf(process.getInputStream());
		Thread errorThread = readingThreadOf(process.getErrorStream());
		process.waitFor();
		inputThread.join();
		errorThread.join();
		onFinish.run();
		logger().info("Finished co-simulation execution");
		return null;
	}

	private Thread readingThreadOf(InputStream stream) {
		Thread thread = new Thread(() -> {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, System.getProperty("os.name").startsWith("Windows") ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_8))) {
				String line = reader.readLine();
				while (line != null) {
					if (line.startsWith("Exception in thread")) exception = line;
					for (OnLoggedString loggedString : onLoggedStringList) loggedString.log(line);
					gatherFile(line);
					line = reader.readLine();
				}
			} catch (IOException e) {
				logger().severe(e.getMessage());
			}
		});
		thread.start();
		return thread;
	}

	private void gatherFile(String line) {
		if (line.contains("Output file")) outputFile = new File(line.replaceAll(".*Output file: ", "").trim());
	}

	private Process createProcess() throws IOException {
		extractExecJar();
		return new ProcessBuilder("java", "-jar", daccosimJar().getAbsolutePath(), "input:" + getEditor().getFile().getAbsolutePath())
				.directory(getEditor().getFile().getParentFile()).start();
	}

	private void extractExecJar() {
		try {
			if (daccosimJarWritten) return;
			Files.write(daccosimJar().toPath(), daccosimJarBytes());
			daccosimJarWritten = true;
		} catch (IOException e) {
			logger().severe(e.getMessage());
		}
	}

	private File daccosimJar() {
		return new File(daccosimFolder, CORE_JAR);
	}

	public void addLoggerListener(OnLoggedString onLoggedString) {
		onLoggedStringList.add(onLoggedString);
	}

	public void setOnFinishListener(Runnable onFinish) {
		this.onFinish = onFinish;
	}

	public void stop() {
		process.destroy();
	}

	public File exportFile() {
		return outputFile;
	}

	public String exception() {
		return exception;
	}

	public interface OnLoggedString {
		void log(String string);
	}

}
