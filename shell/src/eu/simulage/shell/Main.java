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

package eu.simulage.shell;

import eu.simulage.daccosim.Iterator;
import eu.simulage.daccosim.*;
import eu.simulage.daccosim.exporters.NormalExporter;
import eu.simulage.daccosim.view.FMU;
import eu.simulage.daccosim.view.Graph;
import org.javafmi.kernel.Unzipper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.simulage.daccosim.GraphExecutor.logger;
import static java.lang.Boolean.parseBoolean;
import static java.util.Arrays.stream;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.stream.Collectors.joining;
import static org.javafmi.kernel.Files.deleteAll;

public class Main {

	private static File file;
	private static File workingDirectory;

	@SuppressWarnings("ConstantConditions")
	public static void main(String[] args) {
		long start = System.nanoTime();
		Map<String, String> argMap = toMap(args);
		if (!argMap.containsKey("input")) {
			System.out.println("Path to .dng .dsg .simx or .dngx file must be provided in argument 'input'");
			return;
		}
		file = new File(argMap.get("input"));
		workingDirectory = file.getAbsoluteFile().getParentFile();
		setThreadingMode(argMap);
		setOutputBuffer(argMap);
		for (Graph graph : getGraphs(argMap)) {
			GraphExecutor executor = new GraphExecutor(graph.commit());
			executor.debugMode(parseBoolean(argMap.getOrDefault("debugMode", "false")));
			executor.execute();
		}
		long end = System.nanoTime();
		logger().info("Total time (model loading + cosim execution): " + ((end - start) / 1e6) + " ms");
	}

	@SuppressWarnings("ConstantConditions")
	private static Iterable<Graph> getGraphs(Map<String, String> argMap) {
		File file = new File(argMap.get("input"));
		String filename = file.getName().toLowerCase();
		return filename.endsWith("dng") ? Collections.singletonList(importDng(argMap, file)) :
				filename.endsWith("simx") ? Collections.singletonList(importSimx(file, argMap)) :
						filename.endsWith("dngx") ? Collections.singletonList(importDngx(file, argMap)) :
								filename.endsWith("dcs") ? importDcs(file) :
										Collections.singletonList(from(file, argMap));
	}

	private static Graph from(File file, Map<String, String> argMap) {
		return configure(argMap, checkPaths(file, Graph.from(file)));
	}

	private static Graph importDng(Map<String, String> argMap, File file) {
		return configure(argMap, checkPaths(file, DngFile.importDng(file)));
	}

	private static Graph configure(Map<String, String> argMap, Graph graph) {
		File file = new File(argMap.get("input"));
		if (graph.export().prefix().isEmpty())
			graph.export().prefix(file.getName().substring(0, file.getName().lastIndexOf(".")));
		if (argMap.containsKey("initializer"))
			graph.settings().coInitialization().method(CoInitializer.Method.valueOf(argMap.get("initializer")));
		if (argMap.containsKey("tolerance"))
			graph.settings().coInitialization().residualsTolerance(Double.parseDouble(argMap.get("tolerance")));
		if (argMap.containsKey("maxIterations"))
			graph.settings().coInitialization().maxIterations(Integer.parseInt(argMap.get("maxIterations")));
		if (argMap.containsKey("stepper"))
			graph.settings().stepper().method(Stepper.Method.valueOf(argMap.get("stepper")));
		if (argMap.containsKey("stepSize"))
			graph.settings().stepper().stepSize(Double.parseDouble(argMap.get("stepSize")));
		return graph;
	}

	private static Iterable<Graph> importDcs(File file) {
		try {
			List<String> lines = Files.readAllLines(file.toPath());
			AtomicInteger index = new AtomicInteger(0);
			return () -> new java.util.Iterator<Graph>() {
				@Override
				public boolean hasNext() {
					return index.get() < lines.size();
				}

				@Override
				public Graph next() {
					Map<String, String> map = toMap(splitLine(lines.get(index.get())));
					if (map.size() > 1) {
						String parameters = map.entrySet().stream().filter(e -> !e.getKey().equals("input"))
								.map(e -> "\t" + e.getKey() + ": " + e.getValue()).collect(joining("\n"));
						logger().info("Executing graph overriding the following options: " + map.get("input") + "\n" + parameters);
					}
					System.out.println("#### Executing DCS line " + index.incrementAndGet());
					return getGraphs(map).iterator().next();
				}
			};
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void setOutputBuffer(Map<String, String> argMap) {
		NormalExporter.BufferSize = Integer.parseInt(argMap.getOrDefault("outputBuffer", "8")) * 1024;
	}

	private static void setThreadingMode(Map<String, String> argMap) {
		if (argMap.getOrDefault("multithreaded", "true").equals("false")) Iterator.forceSingleThread();
	}

	private static Map<String, String> toMap(String[] args) {
		try {
			return stream(args).collect(Collectors.toMap(s -> s.split(":", 2)[0], s -> s.split(":", 2)[1]));
		} catch (Throwable e) {
			System.out.println("Arguments are not well formatted. Example: input:file.dngx multithreaded:false. Options available\n" +
					"\t input:<file.simx/dngx>\n" +
					"\t multithreaded:<true/false>" +
					"\t debugMode:<true/false>" +
					"\t outputBuffer:32 (expressed in KB)" +
					"\t initializer:IterativePropagation" +
					"\t tolerance:1e-5" +
					"\t maxIterations:20" +
					"\t stepper:Constant" +
					"\t stepSize:10");
			return Collections.emptyMap();
		}
	}

	private static Graph importSimx(File file, Map<String, String> argMap) {
		try {
			logger().info("Loading simx");
			Path directory = Files.createTempDirectory("simx");
			new Unzipper(file).unzipAll(directory.toFile());
			File dsgFile = stream(Objects.requireNonNull(directory.toFile().listFiles())).filter(f -> f.getName().endsWith(".dsg")).findFirst().orElseThrow(() -> new DaccosimException("Simx file format is invalid. Simx file must contain a .dsg file"));
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				deleteAll(directory.toFile());
			}));
			logger().info("simx loaded");
			return configure(argMap, checkPaths(dsgFile, Graph.from(dsgFile)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static Graph importDngx(File file, Map<String, String> argMap) {
		try {
			logger().info("Unzipping DNGX");
			Path directory = Files.createTempDirectory("dngx");
			new Unzipper(file).unzipAll(directory.toFile());
			File dngFile = stream(Objects.requireNonNull(directory.toFile().listFiles())).filter(f -> f.getName().endsWith(".dng")).findFirst().orElse(null);
			if (dngFile == null) throw new DaccosimException("Dng file not found in " + file.getAbsolutePath());
			logger().info("DNGX unzipped");
			Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteAll(directory.toFile())));
			ScheduledExecutorService service = newScheduledThreadPool(1);
			service.scheduleAtFixedRate(() -> logger().info("Reading dng file "), 30, 30, TimeUnit.SECONDS);
			Graph graph = DngFile.importDng(dngFile);
			service.shutdownNow();
			return configure(argMap, checkPaths(dngFile, graph));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static Graph checkPaths(File file, Graph graph) {
		if (filesAreAbsolute(graph)) return graph;
		graph.nodes().stream().filter(n -> n instanceof FMU).map(n -> (FMU) n)
				.forEach(f -> f.path(new File(file.getParentFile(), f.path()).getAbsolutePath()));
		if (graph.export() == null) graph.export(new String[0]);
		graph.export().folder(workingDirectory.getAbsolutePath());
		return graph;
	}

	private static boolean filesAreAbsolute(Graph graph) {
		return graph.nodes().stream().filter(n -> n instanceof FMU)
				.map(n -> (FMU) n)
				.anyMatch(f -> new File(f.path()).isAbsolute());
	}

	private static String[] splitLine(String l) {
		List<String> result = new ArrayList<>();
		Matcher matcher = Pattern.compile("\"([^\"]+)\"|\\s*([^\\s\"]+)\\s*").matcher(cleanLine(l));
		while (matcher.find()) for (int i = 1; i <= matcher.groupCount(); i++) result.add(matcher.group(i));
		return result.stream().filter(Objects::nonNull).toArray(String[]::new);
	}

	private static String cleanLine(String line) {
		return line.replaceAll("//.*|(?s)/\\*.*?\\*/", "").trim();
	}


}
