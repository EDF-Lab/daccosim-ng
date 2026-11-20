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

import eu.simulage.MxGraphToDsg;
import eu.simulage.SimulationProvider;
import eu.simulage.daccosim.DaccosimUtils;
import eu.simulage.daccosim.DngFile;
import eu.simulage.daccosim.view.Graph;
import eu.simulage.editor.specs.FmuSpec;
import org.apache.commons.io.FileUtils;
import org.javafmi.kernel.Unzipper;
import org.javafmi.kernel.Zipper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;

import static eu.simulage.daccosim.GraphExecutor.logger;
import static eu.simulage.editor.EditorActions.*;
import static eu.simulage.editor.EditorUtils.*;
import static java.nio.file.Files.write;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

public class SimxFile {

	private File simxFile;
	private File tempFolder;
	private File simFile;
	private File dngFile;
	private File dsgFile;

	SimxFile(String pathname) {
		simxFile = new File(pathname);
	}

	private static byte[] exportDng(Graph graph) {
		return merge(dngPreface().getBytes(), DngFile.exportDng(graph));
	}

	private static String dngPreface() {
		return "// " + Instant.now().toString() + "\n" + "// Generated with Daccosim NG " + DaccosimUtils.VERSION + "\n";
	}

	private static byte[] merge(byte[] preface, byte[] dngContent) {
		byte[] full = new byte[preface.length + dngContent.length];
		System.arraycopy(preface, 0, full, 0, preface.length);
		System.arraycopy(dngContent, 0, full, preface.length, dngContent.length);
		return full;
	}

	private static void writeFile(Path path, String content) {
		writeFile(path, content.getBytes());
	}

	private static void writeFile(Path path, byte[] bytes) {
		try {
			write(path, bytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
		} catch (IOException e) {
			logger().severe(e.getMessage());
		}
	}

	@SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
	void save() {
		try {
			List<String> fmuFiles = vertexesOf().stream().filter(v -> v instanceof FmuSpec).map(v -> ((FmuSpec) v).fileName()).collect(toList());
			stream(fmuFolder().listFiles()).filter(f -> !fmuFiles.contains(f.getName())).forEach(File::delete);
			Graph graph = MxGraphToDsg.toDaccosimGraph(getEditor());
			writeFile(simFile.toPath(), simFileOf(getEditor().graphComponent.getGraph()));
			writeFile(dngFile.toPath(), exportDng(graph));
			writeFile(dsgFile.toPath(), graph.serialize());
			moveFile(new Zipper().zipDirectory(tempFolder, this.simxFile.getName()), this.simxFile);
		} catch (Throwable e) {
			showErrorDialog(null, "There was a problem saving the file. Is it already opened by other application?");
			e.printStackTrace();
		}
	}

	private File moveFile(File from, File to) throws IOException {
		if (!from.exists()) return to;
		return Files.move(from.toPath(), to.toPath(), REPLACE_EXISTING).toFile();
	}

	@SuppressWarnings("ConstantConditions")
	SimxFile open() {
		tempFolder = temporalFolder();
		new Unzipper(simxFile).unzipAll(tempFolder);
		simFile = stream(tempFolder.listFiles()).filter(f -> f.getName().endsWith(SIM_EXT)).findFirst().orElse(null);
		dngFile = stream(tempFolder.listFiles()).filter(f -> f.getName().endsWith(DNG_EXT)).findFirst().orElse(newDngFile());
		dsgFile = stream(tempFolder.listFiles()).filter(f -> f.getName().endsWith(DSG_EXT)).findFirst().orElse(newDsgFile());
		if (simFile == null) showErrorDialog(getEditor(), "Sim file not found inside simx");
		return this;
	}

	@SuppressWarnings("ConstantConditions")
	SimxFile create() {
		tempFolder = temporalFolder();
		simFile = newSimFile();
		dngFile = newDngFile();
		dsgFile = newDsgFile();
		if (simFile == null) showErrorDialog(getEditor(), "Sim file not found inside simx");
		return this;
	}

	private File newDsgFile() {
		return new File(tempFolder, fileNameNoExt() + "." + DSG_EXT);
	}

	private File newSimFile() {
		return new File(tempFolder, fileNameNoExt() + "." + SIM_EXT);
	}

	private File temporalFolder() {
		try {
			return Files.createTempDirectory("simx").toFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	SimxFile rename(String newPath) {
		try {
			if (simxFile.getAbsolutePath().equals(newPath)) return this;
			simxFile = new File(newPath);
			simFile = moveFile(simFile, newSimFile());
			dngFile = moveFile(dngFile, newDngFile());
			dsgFile = moveFile(dsgFile, newDsgFile());
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return this;
	}

	private File newDngFile() {
		return new File(tempFolder, fileNameNoExt() + "." + DNG_EXT);
	}

	File simFile() {
		return simFile;
	}

	String getAbsolutePath() {
		return simxFile.getAbsolutePath();
	}

	private String fileNameNoExt() {
		return simxFile.getName().replace("." + SIMX_EXT, "");
	}

	File getParentFile() {
		return simxFile.getAbsoluteFile().getParentFile();
	}

	public String getName() {
		return simxFile.getName();
	}

	public File fmuFile(String fmuName) {
		return new File(fmuFolder(), fmuName);
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private File fmuFolder() {
		File fmu = new File(tempFolder, "fmu");
		fmu.mkdirs();
		return fmu;
	}

	public File addFmu(File fmuFile) {
		File file = new File(fmuFolder(), fmuFile.getName());
		try {
			if (!FileUtils.contentEquals(file, fmuFile)) {
				SimulationProvider.removeSimulation(file);
				Files.copy(fmuFile.toPath(), file.toPath(), REPLACE_EXISTING);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return file;
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public void removeFmu(String filename) {
		new File(fmuFolder(), filename).delete();
	}

	public void close() {
		org.javafmi.kernel.Files.deleteAll(tempFolder);
	}

	public File simxFile() {
		return simxFile;
	}

	@SuppressWarnings("ConstantConditions")
	public List<File> fmus() {
		return asList(fmuFolder().listFiles());
	}
}
