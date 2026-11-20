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
import eu.simulage.daccosim.view.Graphs;
import org.javafmi.kernel.OS;
import org.junit.Ignore;
import org.junit.Test;

import static eu.simulage.daccosim.HelperTest.checkEnvironmentVariables;
import static eu.simulage.daccosim.view.Graphs.coInitUseCaseWithBFmu;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeTrue;

@SuppressWarnings("SpellCheckingInspection")
public class CoInitializerTest {

	@Test
	public void executing_environment_with_adder_and_co_initialization() {
		assumeTrue(OS.isWin64());
		Graph graph = Graphs.environmentGraph().settings().coInitialize("NewtonRaphson", 1e-5, 100).commit();
		GraphExecutor graphExecutor = new GraphExecutor(graph);
		graphExecutor.load();
		graphExecutor.init();
		checkEnvironmentVariables(graph, 1.5, 1.5, 10.0, 11.5, 11.5);
	}

	@Test
	public void equation_pair_with_co_initialization() {
		assumeTrue(OS.isWin64());
		Graph graph = Graphs.equationPairGraph().settings().coInitialize("NewtonRaphson", 1e-5, 100).commit();
		GraphExecutor graphExecutor = new GraphExecutor(graph);
		graphExecutor.load();
		graphExecutor.init();
		assertEquals(4.55717, (Double) graph.node("Equation1").input("x1").value(), 1e-5);
		assertEquals(0.09286, (Double) graph.node("Equation1").output("x2").value(), 1e-5);
		assertEquals(4.55717, (Double) graph.node("Equation2").output("x1").value(), 1e-5);
		assertEquals(0.09286, (Double) graph.node("Equation2").input("x2").value(), 1e-5);
	}

	@Test
	public void equation_pair_with_co_initialization_with_IP() {
		assumeTrue(OS.isWin64());
		Graph graph = Graphs.equationPairGraph().settings().coInitialize("IterativePropagation", 1e-5, 100).commit();
		GraphExecutor graphExecutor = new GraphExecutor(graph);
		graphExecutor.load();
		graphExecutor.init();
		assertEquals(4.55717, (Double) graph.node("Equation1").input("x1").value(), 1e-5);
		assertEquals(0.09286, (Double) graph.node("Equation1").output("x2").value(), 1e-5);
		assertEquals(4.55717, (Double) graph.node("Equation2").output("x1").value(), 1e-5);
		assertEquals(0.09286, (Double) graph.node("Equation2").input("x2").value(), 1e-5);
	}

	@Test
	public void triple_equation_with_co_initialization() {
		assumeTrue(OS.isWin64());
		Graph graph = Graphs.tripleEquationGraph().settings().coInitialize("NewtonRaphson", 1e-5, 100).commit();
		GraphExecutor graphExecutor = new GraphExecutor(graph);
		graphExecutor.load();
		graphExecutor.init();
		assertEquals(5.999999, (Double) graph.node("Plane1").input("x").value(), 1e-5);
		assertEquals(2.999999, (Double) graph.node("Plane1").input("y").value(), 1e-5);
		assertEquals(-3.00000, (Double) graph.node("Plane1").output("z").value(), 1e-5);
		assertEquals(5.999999, (Double) graph.node("Plane2").input("x").value(), 1e-5);
		assertEquals(2.999999, (Double) graph.node("Plane2").output("y").value(), 1e-5);
		assertEquals(-3.00000, (Double) graph.node("Plane2").input("z").value(), 1e-5);
		assertEquals(5.999999, (Double) graph.node("Plane3").output("x").value(), 1e-5);
		assertEquals(2.999999, (Double) graph.node("Plane3").input("y").value(), 1e-5);
		assertEquals(-3.00000, (Double) graph.node("Plane3").input("z").value(), 1e-5);
	}

	@Test
	public void loop_with_co_initialization() {
		assumeTrue(OS.isWin64());
		Graph graph = Graphs.loopGraph().settings().coInitialize("NewtonRaphson", 1e-5, 100).commit();
		GraphExecutor graphExecutor = new GraphExecutor(graph);
		graphExecutor.load();
		graphExecutor.init();
		assertEquals(1.45472E-10, (Double) graph.node("SeqProp2").input("x").value(), 1e-5);
		assertEquals(2.90945E-10, (Double) graph.node("SeqProp2").output("y").value(), 1e-5);
		assertEquals(-5.99999, (Double) graph.node("SeqProp1").input("x").value(), 1e-5);
		assertEquals(-11.99999, (Double) graph.node("SeqProp1").output("y").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("Source").output("y").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("NewtonTester").input("x1").value(), 1e-5);
		assertEquals(-11.99999, (Double) graph.node("NewtonTester").input("x2").value(), 1e-5);
		assertEquals(-5.99999, (Double) graph.node("NewtonTester").output("y1").value(), 1e-5);
		assertEquals(1.45472E-10, (Double) graph.node("NewtonTester").output("y2").value(), 1e-5);
	}

	@Test
	public void loop_and_sequ_with_co_initialization() {
		assumeTrue(OS.isWin64());
		Graph graph = Graphs.loopAndSequGraph().settings().coInitialize("NewtonRaphson", 1e-5, 100).commit();
		GraphExecutor graphExecutor = new GraphExecutor(graph);
		graphExecutor.load();
		graphExecutor.init();
		assertEquals(3.0, (Double) graph.node("NewtonTester2").input("x1").value(), 1e-5);
		assertEquals(-11.999999999854529, (Double) graph.node("NewtonTester2").input("x2").value(), 1e-5);
		assertEquals(-5.999999999854529, (Double) graph.node("NewtonTester2").output("y1").value(), 1e-5);
		assertEquals(1.454711906490047E-10, (Double) graph.node("NewtonTester2").output("y2").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("SeqProp3").input("x").value(), 1e-5);
		assertEquals(6.0, (Double) graph.node("SeqProp3").output("y").value(), 1e-5);
		assertEquals(-5.999999999939694, (Double) graph.node("SeqProp2").input("x").value(), 1e-5);
		assertEquals(-11.999999999879387, (Double) graph.node("SeqProp2").output("y").value(), 1e-5);
		assertEquals(6.000000000581885, (Double) graph.node("SeqProp1").input("x").value(), 1e-5);
		assertEquals(12.00000000116377, (Double) graph.node("SeqProp1").output("y").value(), 1e-5);
		assertEquals(1.454711906490047E-10, (Double) graph.node("NewtonTester1").input("x1").value(), 1e-5);
		assertEquals(6.0, (Double) graph.node("NewtonTester1").input("x2").value(), 1e-5);
		assertEquals(6.000000000290942, (Double) graph.node("NewtonTester1").output("y1").value(), 1e-5);
		assertEquals(6.000000000581885, (Double) graph.node("NewtonTester1").output("y2").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("Source").output("y").value(), 1e-5);
	}

	@Test
	public void tree_with_co_initialization() {
		assumeTrue(OS.isWin64());
		Graph graph = Graphs.treeGraph().settings().coInitialize("NewtonRaphson", 1e-5, 100).commit();
		GraphExecutor graphExecutor = new GraphExecutor(graph);
		graphExecutor.load();
		graphExecutor.init();
		assertEquals(3.0, (Double) graph.node("Adder").input("input0").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("Adder").input("input1").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("Adder").input("input2").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("Adder").input("input3").value(), 1e-5);
		assertEquals(12.0, (Double) graph.node("Adder").output("output").value(), 1e-5);
		assertEquals(12.0, (Double) graph.node("SeqProp8").input("x").value(), 1e-5);
		assertEquals(12.0, (Double) graph.node("SeqProp8").output("y").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("SeqProp7").input("x").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("SeqProp7").output("y").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("SeqProp6").input("x").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("SeqProp6").output("y").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("SeqProp5").input("x").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("SeqProp5").output("y").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("SeqProp4").input("x").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("SeqProp4").output("y").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("SeqProp3").input("x").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("SeqProp3").output("y").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("SeqProp2").input("x").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("SeqProp2").output("y").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("SeqProp1").input("x").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("SeqProp1").output("y").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("Source").output("y").value(), 1e-5);
	}


	@Test
	public void two_parallel_loops_with_co_initialization() {
		assumeTrue(OS.isWin64());
		Graph graph = Graphs.twoParallelLoopsGraph().settings().coInitialize("NewtonRaphson", 1e-5, 100).commit();
		GraphExecutor graphExecutor = new GraphExecutor(graph);
		graphExecutor.load();
		graphExecutor.init();
		assertEquals(1.454711906490047E-10, (Double) graph.node("NewtonTester3").input("x1").value(), 1e-5);
		assertEquals(1.454729670058441E-10, (Double) graph.node("NewtonTester3").input("x2").value(), 1e-5);
		assertEquals(4.3641534830385353E-10, (Double) graph.node("NewtonTester3").output("y1").value(), 1e-5);
		assertEquals(7.27357729601863E-10, (Double) graph.node("NewtonTester3").output("y2").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("NewtonTester2").input("x1").value(), 1e-5);
		assertEquals(-11.999999999854529, (Double) graph.node("NewtonTester2").input("x2").value(), 1e-5);
		assertEquals(-5.999999999854529, (Double) graph.node("NewtonTester2").output("y1").value(), 1e-5);
		assertEquals(1.454711906490047E-10, (Double) graph.node("NewtonTester2").output("y2").value(), 1e-5);
		assertEquals(-5.999999999939694, (Double) graph.node("SeqProp2").input("x").value(), 1e-5);
		assertEquals(-11.999999999879387, (Double) graph.node("SeqProp2").output("y").value(), 1e-5);
		assertEquals(-5.999999999939693, (Double) graph.node("SeqProp1").input("x").value(), 1e-5);
		assertEquals(-11.999999999879385, (Double) graph.node("SeqProp1").output("y").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("NewtonTester1").input("x1").value(), 1e-5);
		assertEquals(-11.999999999854527, (Double) graph.node("NewtonTester1").input("x2").value(), 1e-5);
		assertEquals(-5.999999999854527, (Double) graph.node("NewtonTester1").output("y1").value(), 1e-5);
		assertEquals(1.454729670058441E-10, (Double) graph.node("NewtonTester1").output("y2").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("Source").output("y").value(), 1e-5);
	}

	@Test
	public void two_parallel_loops_and_output_loop_with_co_initialization() {
		assumeTrue(OS.isWin64());
		Graph graph = Graphs.twoParallelLoopsAndOutputLoopGraph().settings().coInitialize("NewtonRaphson", 1e-5, 100).commit();
		GraphExecutor graphExecutor = new GraphExecutor(graph);
		graphExecutor.load();
		graphExecutor.init();
		assertEquals(1.454711906490047E-10, (Double) graph.node("Adder").input("input0").value(), 1e-5);
		assertEquals(1.454711906490047E-10, (Double) graph.node("Adder").input("input1").value(), 1e-5);
		assertEquals(2.909423812980094E-10, (Double) graph.node("Adder").output("output").value(), 1e-5);
		assertEquals(2.909423812980094E-10, (Double) graph.node("NewtonTester3").input("x1").value(), 1e-5);
		assertEquals(-2.3346415911618124E-9, (Double) graph.node("NewtonTester3").input("x2").value(), 1e-5);
		assertEquals(-1.7527568285657935E-9, (Double) graph.node("NewtonTester3").output("y1").value(), 1e-5);
		assertEquals(-1.1708720659697747E-9, (Double) graph.node("NewtonTester3").output("y2").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("NewtonTester2").input("x1").value(), 1e-5);
		assertEquals(-11.999999999854529, (Double) graph.node("NewtonTester2").input("x2").value(), 1e-5);
		assertEquals(-5.999999999854529, (Double) graph.node("NewtonTester2").output("y1").value(), 1e-5);
		assertEquals(1.454711906490047E-10, (Double) graph.node("NewtonTester2").output("y2").value(), 1e-5);
		assertEquals(-1.7527568285657935E-9, (Double) graph.node("SeqProp4").input("x").value(), 1e-5);
		assertEquals(-1.7527568285657935E-9, (Double) graph.node("SeqProp4").output("y").value(), 1e-5);
		assertEquals(-1.1690965973087941E-9, (Double) graph.node("SeqProp3").input("x").value(), 1e-5);
		assertEquals(-2.3381931946175882E-9, (Double) graph.node("SeqProp3").output("y").value(), 1e-5);
		assertEquals(-5.999999999939694, (Double) graph.node("SeqProp2").input("x").value(), 1e-5);
		assertEquals(-11.999999999879387, (Double) graph.node("SeqProp2").output("y").value(), 1e-5);
		assertEquals(-5.999999999939694, (Double) graph.node("SeqProp1").input("x").value(), 1e-5);
		assertEquals(-11.999999999879387, (Double) graph.node("SeqProp1").output("y").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("NewtonTester1").input("x1").value(), 1e-5);
		assertEquals(-11.999999999854529, (Double) graph.node("NewtonTester1").input("x2").value(), 1e-5);
		assertEquals(-5.999999999854529, (Double) graph.node("NewtonTester1").output("y1").value(), 1e-5);
		assertEquals(1.454711906490047E-10, (Double) graph.node("NewtonTester1").output("y2").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("Source").output("y").value(), 1e-5);
	}

	@Test
	public void two_serial_loops_with_co_initialization() {
		assumeTrue(OS.isWin64());
		Graph graph = Graphs.twoSerialLoopsGraph().settings().coInitialize("NewtonRaphson", 1e-5, 100).commit();
		GraphExecutor graphExecutor = new GraphExecutor(graph);
		Iterator.forceSingleThread();
		graphExecutor.load();
		graph.node("SeqProp1").input("k").value(1.0);
		graphExecutor.init();
		assertEquals(3.0, (Double) graph.node("NewtonTester2").input("x1").value(), 1e-5);
		assertEquals(-11.999999999854529, (Double) graph.node("NewtonTester2").input("x2").value(), 1e-5);
		assertEquals(-5.999999999854529, (Double) graph.node("NewtonTester2").output("y1").value(), 1e-5);
		assertEquals(1.454711906490047E-10, (Double) graph.node("NewtonTester2").output("y2").value(), 1e-5);
		assertEquals(1.0000000005818848, (Double) graph.node("SeqProp3").input("x").value(), 1e-5);
		assertEquals(1.0000000005818848, (Double) graph.node("SeqProp3").output("y").value(), 1e-5);
		assertEquals(-5.999999999939694, (Double) graph.node("SeqProp2").input("x").value(), 1e-5);
		assertEquals(-11.999999999879387, (Double) graph.node("SeqProp2").output("y").value(), 1e-5);
		assertEquals(1.0, (Double) graph.node("SeqProp1").input("x").value(), 1e-5);
		assertEquals(1.0, (Double) graph.node("SeqProp1").output("y").value(), 1e-5);
		assertEquals(1.454711906490047E-10, (Double) graph.node("NewtonTester1").input("x1").value(), 1e-5);
		assertEquals(1.0, (Double) graph.node("NewtonTester1").input("x2").value(), 1e-5);
		assertEquals(1.0000000002909424, (Double) graph.node("NewtonTester1").output("y1").value(), 1e-5);
		assertEquals(1.0000000005818848, (Double) graph.node("NewtonTester1").output("y2").value(), 1e-5);
		assertEquals(3.0, (Double) graph.node("Source").output("y").value(), 1e-5);
	}

	@Test
	public void two_equations_x2_equals_0_with_co_initialization() {
		Graph graph = Graphs.twoEquationsGraph().settings().coInitialize("NewtonRaphson", 1e-5, 100).commit();
		GraphExecutor graphExecutor = new GraphExecutor(graph);
		graphExecutor.load();
		assertFalse(graphExecutor.init());
	}

	@Test
	@Ignore
	public void two_equations_x2_equals_1_with_co_initialization() {
		Graph graph = Graphs.twoEquationsGraph().settings().coInitialize("NewtonRaphson", 1e-5, 100).commit();
		GraphExecutor graphExecutor = new GraphExecutor(graph);
		graphExecutor.load();
		graph.node("eightdivx2").input("x2").value(1.0);
		graphExecutor.init();
		assertEquals(2, (Double) graph.node("eightdivx2").input("x2").value(), 1e-5);
		assertEquals(4, (Double) graph.node("eightdivx2").output("x1").value(), 1e-5);
		assertEquals(4, (Double) graph.node("x1minusx2").input("x1").value(), 1e-5);
		assertEquals(2, (Double) graph.node("x1minusx2").output("x2").value(), 1e-5);
	}

	@Test
	@Ignore
	public void two_equations_x2_equals_10_with_co_initialization() {
		Graph graph = Graphs.twoEquationsGraph().settings().coInitialize("NewtonRaphson", 1e-5, 100).commit();
		GraphExecutor graphExecutor = new GraphExecutor(graph);
		graphExecutor.load();
		graph.node("eightdivx2").input("x2").value(10.0);
		graphExecutor.init();
		assertEquals(-4, (Double) graph.node("eightdivx2").input("x2").value(), 1e-5);
		assertEquals(-2, (Double) graph.node("eightdivx2").output("x1").value(), 1e-5);
		assertEquals(-2, (Double) graph.node("x1minusx2").input("x1").value(), 1e-5);
		assertEquals(-4, (Double) graph.node("x1minusx2").output("x2").value(), 1e-5);
	}


	@Test
	@Ignore
	public void executing_B_test() {
		Graph graph = coInitUseCaseWithBFmu()
				.settings().coInitialize("NewtonRaphson", 1e-5, 100).stopTime(0.5)
				.stepper().stepSize(0.1).commit();
		GraphExecutor graphExecutor = new GraphExecutor(graph);
		graphExecutor.execute();
	}


	@Test
	@Ignore
	public void equation_pair_with_co_initialization_for_jp() {
		eu.simulage.daccosim.view.Graph view = Graphs.equationPairGraphForJP();
		view.settings().stopTime(0.1);
		view.settings().stepper().stepSize(0.01);
		view.exportAll();
		view.export().decimalSeparator(",");
		Graph graph = view.settings().coInitialize("NewtonRaphson", 1e-5, 100).commit();
		GraphExecutor graphExecutor = new GraphExecutor(graph);
		graphExecutor.execute();
	}

}
