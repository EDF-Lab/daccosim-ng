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

package eu.simulage;

import eu.simulage.daccosim.DaccosimException;
import eu.simulage.daccosim.DaccosimUtils;
import org.javafmi.kernel.OS;
import org.javafmi.modeldescription.ModelDescription;
import org.javafmi.wrapper.Simulation;
import org.javafmi.wrapper.v2.Access;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipFile;

import static eu.simulage.daccosim.view.Helper.graph;

public class SimulationProvider {
	private static final Map<String, Simulation> simulationMap = new ConcurrentHashMap<>();

	public static Simulation getSimulation(File selectedFile) {
		try {
			return doGetSimulation(selectedFile);
		} catch (Exception e) {
			if (e instanceof DaccosimException) throw e;
			else throw new DaccosimException(e.getMessage());
		}
	}

	@SuppressWarnings("UnusedReturnValue")
	public static Simulation unsafeGetSimulation(File selectedFile) {
		return doGetSimulation(selectedFile);
	}

	private static Simulation doGetSimulation(File selectedFile) {
		if (!selectedFile.exists() || !selectedFile.getName().matches("(?i).+\\.fmu")) return null;
		if (simulationMap.containsKey(fileId(selectedFile)))
			return simulationMap.get(fileId(selectedFile));
		ModelDescription description = DaccosimUtils.getModelDescription(selectedFile);
		if (!simulationMap.containsKey(description.getGuid()))
			simulationMap.put(description.getGuid(), simulationOf(selectedFile));
		simulationMap.put(fileId(selectedFile), simulationMap.get(description.getGuid()));
		return simulationMap.get(description.getGuid());
	}

	private static String fileId(File selectedFile) {
		return selectedFile.getAbsolutePath() + "_" + selectedFile.lastModified();
	}

	private static Simulation simulationOf(File selectedFile) {
		if (!isSystemCompatible(selectedFile))
			throw new DaccosimException("Invalid FMU: " + selectedFile.getName() + "\nBinaries for " + osSystem() + OS.architecture() + " not found");
		try {
			AtomicReference<Simulation> simulation = new AtomicReference<>();
			checkPrimitive("fmi2Instantiate() - fmu: " + selectedFile.getName(), () -> simulation.set(new Simulation(selectedFile.getAbsolutePath())));
			if (simulation.get().getModelDescription() instanceof org.javafmi.modeldescription.v2.ModelDescription)
				checkV2(simulation.get(), selectedFile);
			else {
				checkPrimitive("fmiInit - fmu: " + selectedFile.getName(), () -> simulation.get().init(0));
				checkPrimitive("fmiTerminate  - fmu: " + selectedFile.getName(), () -> simulation.get().terminate());
			}
			return simulation.get();
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new DaccosimException(e.getMessage());
		}
	}

	private static void checkV2(Simulation simulation, File selectedFile) {
		Access access = new Access(simulation);
		checkPrimitive("fmi2SetUpExperiment() - fmu: " + selectedFile.getName(), () -> access.setUpExperiment(true, ((org.javafmi.modeldescription.v2.ModelDescription)simulation.getModelDescription()).getDefaultExperiment().getTolerance(), graph().settings().startTime(), false, 10));
		checkPrimitive("fmi2EnterInitializationMode() - fmu: " + selectedFile.getName(), access::enterInitializationMode);
		checkPrimitive("fmi2ExitInitializationMode() - fmu: " + selectedFile.getName(), access::exitInitializationMode);
		checkPrimitive("fmi2Terminate() - fmu: " + selectedFile.getName(), access::terminate);
		checkPrimitive("fmi2FreeInstance() - fmu: " + selectedFile.getName(), access::freeInstance);
	}

	public static void checkPrimitive(String message, Runnable runnable){
		System.err.println(message);
		runnable.run();
		System.err.println("done");
	}

	private static boolean isSystemCompatible(File selectedFile) {
		try (ZipFile file = new ZipFile(selectedFile)) {
			String folderToFind = ("binaries/" + osSystem() + OS.architecture()).toLowerCase();
			return file.stream().anyMatch(e -> e.getName().toLowerCase().startsWith(folderToFind));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	private static String osSystem() {
		return OS.name().equalsIgnoreCase("windows") ? "win" : OS.name();
	}

	public static void removeSimulation(File file) {
		simulationMap.remove(fileId(file));
	}
}
