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

import eu.simulage.daccosim.model.Graph;
import org.javafmi.kernel.OS;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

import static eu.simulage.daccosim.HelperTest.checkEventsVariables;
import static eu.simulage.daccosim.HelperTest.checkTankYPipeVariables;
import static eu.simulage.daccosim.view.Graphs.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

public class GraphExecutorTest {

	@Test
	public void executing_environment_with_adder() {
		assumeTrue(OS.isWin64());
		Graph graph = environmentGraph().commit();
		GraphExecutor graphExecutor = new GraphExecutor(graph);
		graphExecutor.load();
		graphExecutor.init();
		HelperTest.checkEnvironmentVariables(graph, 1.5, 1.5, 10.0, 11.5, 11.5);
		graphExecutor.firstStep();
		HelperTest.checkEnvironmentVariables(graph, 2.5, 2.5, 10.0, 12.5, 12.5);
		graphExecutor.step();
		HelperTest.checkEnvironmentVariables(graph, 3.5, 3.5, 10.0, 13.5, 13.5);
		graphExecutor.step();
		HelperTest.checkEnvironmentVariables(graph, 4.5, 4.5, 10.0, 14.5, 14.5);
	}


	@Test
	public void executing_with_tank_y_pipe() {
		assumeTrue(OS.isWin64());
		Graph graph = tankYPipeGraph()
				.settings().coInitialize("NewtonRaphson", 1e-5, 100)
				.stepper().stepSize(0.1).commit();
		GraphExecutor graphExecutor = new GraphExecutor(graph);
		graphExecutor.load();
		graphExecutor.init();
		checkTankYPipeVariables(graph, 1.534405422, 3, 0.767202711, 3, 0.767202711, 0.767202711, 3, 0.767202711, 0.767202711, 0.767202711, 1.534405422);
		graphExecutor.firstStep();
		checkTankYPipeVariables(graph, 1.5344054223053307, 3.0381463934580704, 0.7720649729112657, 2.966830676753975, 0.7629496567789581, 0.7720649729112657, 2.9867446597145433, 0.7655059126068156, 0.7655059126068156, 0.7629496567789581, 1.5344054223053307);
		graphExecutor.step();
		checkTankYPipeVariables(graph, 1.5284555693857738, 3.0760918484055266, 0.7768714312273071, 2.933840294163219, 0.7586958980479752, 0.7768714312273071, 2.9735753947688424, 0.7638163997019487, 0.7638163997019487, 0.7586958980479752, 1.5284555693857738);
		graphExecutor.step();
		checkTankYPipeVariables(graph, 1.522512297749924, 3.1134552504598, 0.781575281172719, 2.901040903914724, 0.7544429901245481, 0.781575281172719, 2.960462381484066, 0.7621303820522929, 0.7621303820522929, 0.7544429901245481, 1.522512297749924);
		graphExecutor.step();
		checkTankYPipeVariables(graph, 1.516573372176841, 3.1502880361330914, 0.786184782789207, 2.8684388975952753, 0.7501917832849099, 0.786184782789207, 2.9474130410819477, 0.7604488402649308, 0.7604488402649308, 0.7501917832849099, 1.516573372176841);
	}

	@Test
	public void executing_with_events() {
		assumeTrue(OS.isWin64());
		Graph graph = eventsGraph()
				.settings().coInitialize("NewtonRaphson", 1e-5, 100)
				.stepper().stepSize(0.01).commit();
		GraphExecutor graphExecutor = new GraphExecutor(graph);
		graphExecutor.load();
		graphExecutor.init();
		checkEventsVariables(graph, 12.0, 2.0);
		graphExecutor.firstStep();
		checkEventsVariables(graph, 11.999506986266777, 2.000000000000125);
		graphExecutor.step();
		checkEventsVariables(graph, 11.998102514969371, 2.000000000008);
		graphExecutor.step();
		checkEventsVariables(graph, 11.99578429731357, 2.000000000091125);
		graphExecutor.step();
		checkEventsVariables(graph, 11.992549664998538, 2.000000000512);
	}

	@Test
	public void executing_with_tank_barrel() {
		assumeTrue(OS.isWin64());
		eu.simulage.daccosim.view.Graph graph = tankBarrelGraph();
		graph.settings().stopTime(18);
		graph.settings().stepper().stepSize(0.01);
		GraphExecutor graphExecutor = new GraphExecutor(graph.commit());
		graphExecutor.load();
		graphExecutor.init();
		graphExecutor.firstStep();
		for (int i = 0; i < 50; i++) graphExecutor.step();
		assertEquals(0.0, (double) graphExecutor.graph().node("water").input("input").value(), 1e-4);
		graphExecutor.step();
		assertEquals(0.021186451, (double) graphExecutor.graph().node("water").input("input").value(), 1e-4);
		for (int i = 0; i < 50; i++) graphExecutor.step();
		assertEquals(0.003256475, (double) graphExecutor.graph().node("water").input("input").value(), 1e-4);
	}

	@Test
	@Ignore
	public void executing_heavy_use_case() throws Exception {
		eu.simulage.daccosim.view.Graph view = heavyUseCase()
				.settings().stopTime(10).coInitialize("NewtonRaphson", 1e-5, 100)
				.stepper().stepSize(1).graph();
		Files.write(new File("heavy.js").toPath(), view.serialize());
		System.out.println("File written");
		long init = System.nanoTime();
		Graph graph = eu.simulage.daccosim.view.Graph.from(new File("heavy.js")).commit();
		long end = System.nanoTime();
		System.out.println("File read in " + ((end - init) / 1e9) + " seconds");
		init = System.nanoTime();
		GraphExecutor graphExecutor = new GraphExecutor(graph);
		graphExecutor.load();
		graphExecutor.init();
		end = System.nanoTime();
		System.out.println("Graph loaded in " + ((end - init) / 1e9) + " seconds");
		init = System.nanoTime();
		graphExecutor.firstStep();
		end = System.nanoTime();
		System.out.println("Graph step 0 in " + ((end - init) / 1e9) + " seconds");
		for (int i = 1; i < 10; i++) {
			init = System.nanoTime();
			graphExecutor.step();
			end = System.nanoTime();
			System.out.println("Graph step " + i + " in " + ((end - init) / 1e9) + " seconds");
		}
		init = System.nanoTime();
		graphExecutor.terminate();
		end = System.nanoTime();
		System.out.println("Graph terminated in " + ((end - init) / 1e9) + " seconds");

	}

	@Test
	@Ignore
	public void heavy_use_case_executed_in_parallel_previous_daccosim_in_single_core_new_version() {
		assumeTrue(OS.isLinux64());
		eu.simulage.daccosim.view.Graph from = eu.simulage.daccosim.view.Graph.from(new File("heavy442.js"));
		from.settings().coInitialize("Derivatives", 20, 100);
		from.exportAll();
		Graph graph = from.commit();
		GraphExecutor.execute(graph);
	}

	@Test
	public void executing_A_test_without_co_init() {
		assumeTrue(OS.isWin64());
		Graph graph = coInitUseCaseWithAFmu()
				.exportAll()
				.settings().stopTime(0.5)
				.stepper().stepSize(0.1)
				.commit();
		GraphExecutor graphExecutor = new GraphExecutor(graph);
		graphExecutor.execute();
	}

}
