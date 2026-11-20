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

package eu.simulage.daccosim;

import eu.simulage.daccosim.exporters.NormalExporter;
import eu.simulage.daccosim.model.*;
import eu.simulage.daccosim.model.nodes.v2.FMULocal;
import org.javafmi.kernel.Files;
import org.javafmi.kernel.Unzipper;
import org.javafmi.modeldescription.FmiVersion;
import org.javafmi.modeldescription.ModelDescription;
import org.javafmi.modeldescription.ModelDescriptionDeserializer;
import org.javafmi.proxy.ModelDescriptionPeeker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static eu.simulage.daccosim.Stepper.Method.AdamsBashforth;
import static eu.simulage.daccosim.Stepper.Method.ConstantStep;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

public class DaccosimUtils {

	public static final String VERSION = "%%VERSION%%";
	public static final String DATE = "%%DATE%%";
	public static final String YEAR = "%%YEAR%%";
	public static final String SysVarResetFromMaster = "__resetFromMaster";
	private static Map<String, ModelDescription> cache = new HashMap<>();

	static String printParameters(Graph graph, File outputFile, boolean debugMode) {
		StringBuilder builder = new StringBuilder();
		writeVar(builder, "Daccosim NG " + YEAR, "version " + VERSION + " released " + DATE);
		writeTimeBounds(graph, builder);
		writeCoInit(graph, builder);
		writeStepper(graph, builder);
		writeMultithread(builder);
		writeExport(graph, outputFile, builder);
		if (debugMode) writeDebugModeMetrics(graph, builder);
		return builder.toString();
	}

	private static void writeDebugModeMetrics(Graph graph, StringBuilder builder) {
		List<FMULocal> allFmus = graph.nodes().stream()
				.filter(n -> n instanceof FMULocal)
				.map(n -> ((FMULocal) n))
				.collect(Collectors.toList());
		Set<String> distinctFmus = allFmus.stream().map(FMULocal::path).collect(toSet());
		writeVar(builder, "Nb FMUs (distinct - all)", distinctFmus.size() + " - " + allFmus.size());

		for (FMULocal fmu : allFmus) {
			if (!distinctFmus.contains(fmu.path())) continue;
			distinctFmus.remove(fmu.path());
			writeVar(builder, "FMU", new File(fmu.path()).getName());
			writeVar(builder, "\tInputs", fmu.inputs().size());
			writeVar(builder, "\tOutputs", fmu.outputs().size());
			ModelDescription description = getModelDescription(new File(fmu.path()));
			writeVar(builder, "\tInternal", stream(description.getModelVariables()).filter(s -> s.getCausality().equals("local")).count());
			writeVar(builder, "\tDifferential Equations", stream(description.getModelVariables()).filter(s -> s.getName().startsWith("der(")).count());
		}
		if (graph.export() != null) writeVar(builder, "Number of Logged variables", graph.export().variables().size());
		writeVar(builder, "Number of FMU-to-FMU connections", graph.arrows().stream().filter(a -> a.fromNode() instanceof FMUProxy && a.toNode() instanceof FMUProxy).count());
	}

	private static void writeExport(Graph graph, File outputFile, StringBuilder builder) {
		if (graph.export() != null && !graph.export().variables().isEmpty()) {
			writeVar(builder, "Output buffer size", NormalExporter.BufferSize);
			writeVar(builder, "CSV cell separator", graph.export().cellSeparator());
			writeVar(builder, "CSV decimal separator", graph.export().decimalSeparator());
			writeVar(builder, "Output file", outputFile.getAbsolutePath());
			if (graph.export().interpolationInterval() != 0)
				writeVar(builder, "Interpolation interval", graph.export().interpolationInterval());
		}
	}

	private static void writeMultithread(StringBuilder builder) {
		writeVar(builder, "Loading multithreaded", "false");
		writeVar(builder, "Stepping multithreaded", Iterator.mode() == Iterator.Mode.MC);
		writeVar(builder, "Termination multithreaded", Iterator.mode() == Iterator.Mode.MC);
	}

	private static void writeStepper(Graph graph, StringBuilder builder) {
		Graph.Settings.Stepper stepper = graph.settings().stepper();
		Stepper.Method method = stepper.method();
		writeVar(builder, "Stepper method", labelOf(method));
		if (method.equals(ConstantStep)) writeVar(builder, "StepSize", stepper.stepSize());
		if (!method.equals(ConstantStep)) writeVar(builder, "SafetyFactor", stepper.safetyFactor());
		if (method.equals(AdamsBashforth)) writeVar(builder, "Order", stepper.order());
		List<String> staticFmus = graph.nodes().stream()
				.filter(f -> f instanceof FMUProxy && ((FMUProxy) f).static_())
				.map(GraphNode::id)
				.collect(Collectors.toList());
		if (!staticFmus.isEmpty()) writeVar(builder, "Static fmus", String.join(", ", staticFmus));
		writeVar(builder, "Transmit derivatives", graph.settings().stepper().transmitDerivatives());
	}

	private static String labelOf(Stepper.Method method) {
		return method == AdamsBashforth ? "Adams Bashforth" : method.name();
	}

	private static void writeCoInit(Graph graph, StringBuilder builder) {
		if (graph.settings().isCoInitialized()) {
			writeVar(builder, "CoInit method", labelOf(graph.settings().coInitialization().method()));
			writeVar(builder, "CoInit max iterations", graph.settings().coInitialization().maxIterations());
			writeVar(builder, "CoInit tolerance", graph.settings().coInitialization().residualsTolerance());
		}
	}

	private static String labelOf(CoInitializer.Method method) {
		return method == CoInitializer.Method.IterativePropagation ? "Iterative Propagation" : "Newton-Raphson";
	}

	private static void writeTimeBounds(Graph graph, StringBuilder builder) {
		writeVar(builder, "Start time", graph.settings().startTime());
		writeVar(builder, "Stop time", graph.settings().stopTime());
	}

	private static void writeVar(StringBuilder builder, String varName, Object value) {
		builder.append("\n\t").append(varName).append(": ").append(value);
	}

	public static byte[] extract(InputStream inputStream) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int read;
		while ((read = inputStream.read(buffer, 0, buffer.length)) != -1) {
			baos.write(buffer, 0, read);
		}
		baos.flush();
		return baos.toByteArray();
	}

	public static ModelDescription getModelDescription(File selectedFile) {
		ModelDescription modelDescription = modelDescriptionOf(selectedFile);
		if (modelDescription == null)
			throw new DaccosimException("Selected file " + selectedFile.getName() + " does not contain a modelDescription.xml");
		return modelDescription;
	}

	private static ModelDescription modelDescriptionOf(File fmuFile) {
		if (cache.containsKey(fmuFileId(fmuFile))) return cache.get(fmuFileId(fmuFile));
		File file = new Unzipper(fmuFile).unzip("modelDescription.xml", Files.createTempDirectory("md"));
		String version = ModelDescriptionPeeker.peekVersion(file);
		if (version.equals(FmiVersion.One))
			throw new DaccosimException("Selected file " + fmuFile.getName() + " is FMI v1.0. It must be +2.0");
		if (version.equals(FmiVersion.Two)) {
			org.javafmi.modeldescription.v2.ModelDescription modelDescription = ModelDescriptionDeserializer.deserialize(file).asV2();
			cache.put(fmuFileId(fmuFile), modelDescription);
			if (modelDescription.getCoSimulation() == null)
				throw new DaccosimException("Selected file " + fmuFile.getName() + " does not support co-simulation");
			return modelDescription;
		}
		file.deleteOnExit();
		return null;
	}

	private static String fmuFileId(File fmuFile) {
		return fmuFile.getAbsolutePath() + "_" + fmuFile.lastModified();
	}

	public static boolean isHybridCoSim(org.javafmi.modeldescription.v2.ModelDescription md) {
		return !md.getVendorAnnotations().isEmpty() && md.getVendorAnnotations().get(0).getHybridCoSim() != null;
	}

	static boolean areDifferent(List<Object> values, List<Object> newValues) {
		for (int i = 0; i < values.size(); i++) if (!areEquals(values.get(i), newValues.get(i))) return true;
		return false;
	}

	public static boolean areEquals(Object o1, Object o2) {
		return o1 instanceof Double ? (Math.abs((double) o1 - (double) o2)) < 1e-6 : o1.equals(o2);
	}

	static Set<String> flowInputsIn(Graph graph) {
		return graph.nodes().stream()
				.filter(n -> n instanceof FMUProxy)
				.map(n -> (FMUProxy) n)
				.map(n -> n.flowVariables().stream().map(((GraphNode) n)::input).filter(Objects::nonNull).map(Variable::path))
				.flatMap(s -> s)
				.collect(toSet());
	}

	static Map<Input, List<Integer>> asMap(List<Arrow> arrows) {
		Map<Input, List<Integer>> result = new LinkedHashMap<>();
		for (int i = 0; i < arrows.size(); i++) {
			if (!result.containsKey(arrows.get(i).to())) result.put(arrows.get(i).to(), new ArrayList<>());
			result.get(arrows.get(i).to()).add(i);
		}
		return result;
//		return IntStream.range(0, arrows.size()).boxed()
//				.collect(toMap(i -> arrows.get(i).to(), Collections::singletonList, DaccosimUtils::join));
	}

	private static List<Integer> join(List<Integer> v1, List<Integer> v2) {
		List<Integer> list = new ArrayList<>(v1);
		list.addAll(v2);
		return list;
	}
}
