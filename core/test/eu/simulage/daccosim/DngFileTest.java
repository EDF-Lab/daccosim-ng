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
import eu.simulage.daccosim.model.nodes.ExternalInput;
import eu.simulage.daccosim.model.nodes.operators.Gain;
import eu.simulage.daccosim.model.nodes.operators.Offset;
import eu.simulage.daccosim.view.Graphs;
import eu.simulage.daccosim.view.Helper;
import org.javafmi.kernel.OS;
import org.junit.Test;

import static eu.simulage.daccosim.DataType.Real;
import static eu.simulage.daccosim.Stepper.Method.ConstantStep;
import static eu.simulage.daccosim.view.Graphs.fmuPath;
import static eu.simulage.daccosim.view.Helper.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

@SuppressWarnings("SpellCheckingInspection")
public class DngFileTest {

	@SuppressWarnings("ConstantConditions")
	@Test
	public void should_parse_text_correctly() {
		assumeTrue(OS.isWin64());
		String text = "" +
				"FMU Tank " + fmuPath("TankGwin3264") + "\n" +
				"Output Tank outFlow Real continuous\n" +
				"Input Tank Valve Boolean discrete\n" +
				"FMU Barrel " + fmuPath("BarrelGwin3264") + "\n" +
				"Output Barrel Water Real continuous\n" +
				"Input Barrel inFlow Real continuous\n" +
				"Input Barrel Size Integer discrete\n" +
				"Input Barrel Abort Boolean discrete\n" +
				"FMU C1 " + fmuPath("C1win3264") + "\n" +
				"Output C1 Valve Boolean discrete\n" +
				"FMU C2 " + fmuPath("C2win3264") + "\n" +
				"Output C2 Abort Boolean discrete\n" +
				"Output C2 Size Integer discrete\n" +
				"ExternalOutput water\n" +
				"Input water input Real continuous\n" +
				"Connection C1.Valve Tank.Valve\n" +
				"Connection C2.Abort Barrel.Abort\n" +
				"Connection C2.Size Barrel.Size\n" +
				"Connection Tank.outFlow Barrel.inFlow\n" +
				"Connection Barrel.Water water.input\n" +
				"ConstantStepper 0.01\n" +
				"Simulation 0 18\n";
		Graph graph = DngFile.importDng(text).commit();
		assertEquals(5, graph.nodes().size());
		assertEquals(1, graph.node("Tank").inputs().size());
		assertEquals(1, graph.node("Tank").outputs().size());
		assertEquals(Real, graph.variable("Tank.outFlow").type());
		assertEquals(DataType.Boolean, graph.variable("Tank.Valve").type());
		assertEquals(3, graph.node("Barrel").inputs().size());
		assertEquals(1, graph.node("Barrel").outputs().size());
		assertEquals(Real, graph.variable("Barrel.Water").type());
		assertEquals(Real, graph.variable("Barrel.inFlow").type());
		assertEquals(DataType.Integer, graph.variable("Barrel.Size").type());
		assertEquals(DataType.Boolean, graph.variable("Barrel.Abort").type());
		assertEquals(0, graph.node("C1").inputs().size());
		assertEquals(1, graph.node("C1").outputs().size());
		assertEquals(DataType.Boolean, graph.variable("C1.Valve").type());
		assertEquals(0, graph.node("C2").inputs().size());
		assertEquals(2, graph.node("C2").outputs().size());
		assertEquals(DataType.Boolean, graph.variable("C2.Abort").type());
		assertEquals(DataType.Integer, graph.variable("C2.Size").type());
		assertEquals(5, graph.arrows().size());

		GraphExecutor graphExecutor = new GraphExecutor(graph);
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

	@SuppressWarnings("ConstantConditions")
	@Test
	public void should_parse_text_correctly_containing_operators() {
		String text = "" +
				"ExternalInput ei" + "\n" +
				"Output ei output Real continuous\n" +
				"InitialValue ei.output 2\n" +
				"Offset offset Real 3\n" +
				"Input offset input Real continuous\n" +
				"Output offset output Real continuous\n" +
				"Gain gain Real 3\n" +
				"Input gain input Real continuous\n" +
				"Output gain output Real continuous\n" +
				"Adder adder Real\n" +
				"Input adder input1 Real continuous\n" +
				"Input adder input2 Real continuous\n" +
				"Output adder output Real continuous\n" +
				"Multiplier multiplier Real\n" +
				"Input multiplier input1 Real continuous\n" +
				"Input multiplier input2 Real continuous\n" +
				"Output multiplier output Real continuous\n" +
				"Connection ei.output offset.input\n" +
				"Connection ei.output gain.input\n" +
				"Connection offset.output adder.input1\n" +
				"Connection gain.output adder.input2\n" +
				"Connection offset.output multiplier.input1\n" +
				"Connection gain.output multiplier.input2\n" +
				"ConstantStepper 1 transmitDerivatives\n" +
				"NewtonRaphsonInitializer 100 1e-5\n" +
				"Simulation 0 1\n";
		Graph graph = DngFile.importDng(text).commit();
		assertEquals(5, graph.nodes().size());
		assertEquals(0, graph.node("ei").inputs().size());
		assertEquals(1, graph.node("ei").outputs().size());
		assertEquals(2.0, ((ExternalInput) graph.node("ei")).initialValues().get(0).value());
		assertEquals(Real, graph.variable("ei.output").type());
		assertEquals(1, graph.node("offset").inputs().size());
		assertEquals(1, graph.node("offset").outputs().size());
		assertEquals(3.0, ((Offset) graph.node("offset")).value());
		assertEquals(Real, graph.variable("offset.input").type());
		assertEquals(Real, graph.variable("offset.output").type());
		assertEquals(1, graph.node("gain").inputs().size());
		assertEquals(1, graph.node("gain").outputs().size());
		assertEquals(3.0, ((Gain) graph.node("gain")).value());
		assertEquals(Real, graph.variable("gain.input").type());
		assertEquals(Real, graph.variable("gain.output").type());
		assertEquals(2, graph.node("adder").inputs().size());
		assertEquals(1, graph.node("adder").outputs().size());
		assertEquals(Real, graph.variable("adder.input1").type());
		assertEquals(Real, graph.variable("adder.input2").type());
		assertEquals(Real, graph.variable("adder.output").type());
		assertEquals(2, graph.node("multiplier").inputs().size());
		assertEquals(1, graph.node("multiplier").outputs().size());
		assertEquals(Real, graph.variable("multiplier.input1").type());
		assertEquals(Real, graph.variable("multiplier.input2").type());
		assertEquals(Real, graph.variable("multiplier.output").type());
		assertTrue(graph.settings().stepper().transmitDerivatives());
		assertEquals(6, graph.arrows().size());

		GraphExecutor graphExecutor = new GraphExecutor(graph);
		graphExecutor.execute();
		graphExecutor.logFile().delete();
		assertEquals(11.0, graph.variable("adder.output").value());
		assertEquals(30.0, graph.variable("multiplier.output").value());
	}

	@Test
	public void should_export_graph_correctly() {
		String text = "" +
				"FMU Tank \"" + fmuPath("TankGwin3264") + "\"\n" +
				"Output Tank outFlow Real continuous\n" +
				"Input Tank Valve Boolean discrete\n" +
				"FMU Barrel \"" + fmuPath("BarrelGwin3264") + "\"\n" +
				"Output Barrel Water Real continuous\n" +
				"Input Barrel inFlow Real continuous\n" +
				"Input Barrel Size Integer discrete\n" +
				"Input Barrel Abort Boolean discrete\n" +
				"FMU C1 \"" + fmuPath("C1win3264") + "\"\n" +
				"Output C1 Valve Boolean discrete\n" +
				"FMU C2 \"" + fmuPath("C2win3264") + "\"\n" +
				"Output C2 Abort Boolean discrete\n" +
				"Output C2 Size Integer discrete\n" +
				"ExternalOutput water\n" +
				"Input water input Real continuous\n" +
				"Connection C1.Valve Tank.Valve\n" +
				"Connection C2.Abort Barrel.Abort\n" +
				"Connection C2.Size Barrel.Size\n" +
				"Connection Tank.outFlow Barrel.inFlow\n" +
				"Connection Barrel.Water water.input\n" +
				"IterativePropagationInitializer 100 100.0\n" +
				"ConstantStepper 0.01 transmitDerivatives\n" +
				"Simulation 0.0 18.0\n";

		eu.simulage.daccosim.view.Graph graph = Graphs.tankBarrelGraph();
		graph.settings().startTime(0).stopTime(18);
		graph.settings().stepper(ConstantStep).stepSize(0.01).transmitDerivatives(true);
		assertEquals(text, new String(DngFile.exportDng(graph)));
	}

	@Test
	public void should_export_graph_correctly_with_operators() {
		String text = "" +
				"ExternalInput ei" + "\n" +
				"Output ei output Real continuous\n" +
				"InitialValue ei.output 2.0\n" +
				"Offset offset Real 3.0\n" +
				"Output offset output Real continuous\n" +
				"Input offset input Real continuous\n" +
				"Gain gain Real 3.0\n" +
				"Output gain output Real continuous\n" +
				"Input gain input Real continuous\n" +
				"Adder adder Real\n" +
				"Output adder output Real continuous\n" +
				"Input adder input1 Real continuous\n" +
				"Input adder input2 Real continuous\n" +
				"Multiplier multiplier Real\n" +
				"Output multiplier output Real continuous\n" +
				"Input multiplier input1 Real continuous\n" +
				"Input multiplier input2 Real continuous\n" +
				"Connection ei.output offset.input\n" +
				"Connection ei.output gain.input\n" +
				"Connection offset.output adder.input1\n" +
				"Connection gain.output adder.input2\n" +
				"Connection offset.output multiplier.input1\n" +
				"Connection gain.output multiplier.input2\n" +
				"NewtonRaphsonInitializer 100 1.0E-5\n" +
				"ConstantStepper 1.0\n" +
				"Simulation 0.0 1.0\n";

		eu.simulage.daccosim.view.Graph graph = Helper.graph()
				.nodes(
						externalInput("ei").outputs(output("output", Real, "continuous")).initialValues(initialValue("output", 2.0)),
						offsetOperator("offset", 3.0).dataType(Real).inputs(input("input", Real, "continuous")).output(output("output", Real, "continuous")),
						gainOperator("gain", 3.0).dataType(Real).inputs(input("input", Real, "continuous")).output(output("output", Real, "continuous")),
						adderOperator("adder").dataType(Real).inputs(input("input1", Real, "continuous"), input("input2", Real, "continuous")).output(output("output", Real, "continuous")),
						multiplierOperator("multiplier").dataType(Real).inputs(input("input1", Real, "continuous"), input("input2", Real, "continuous")).output(output("output", Real, "continuous")))
				.arrows(
						arrow("ei.output", "offset.input"),
						arrow("ei.output", "gain.input"),
						arrow("offset.output", "adder.input1"),
						arrow("gain.output", "adder.input2"),
						arrow("offset.output", "multiplier.input1"),
						arrow("gain.output", "multiplier.input2"));
		graph.settings().startTime(0).stopTime(1);
		graph.settings().stepper(ConstantStep).stepSize(1);
		graph.settings().coInitialize("NewtonRaphson", 1e-5, 100);
		assertEquals(text, new String(DngFile.exportDng(graph)));
	}

	@Test
	public void should_provoke_an_exception_if_a_node_is_used_before_declaration() {
		String text = "" +
				"Output ei output Real continuous\n" +
				"ExternalInput ei\n";
		try {
			DngFile.importDng(text).commit();
			fail();
		} catch (DaccosimException e) {
			assertEquals("Problem in line 1. ei node is not defined or it is used before its definition", e.getMessage());
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public void should_provoke_an_exception_if_a_variable_is_used_before_declaration() {
		String text = "" +
				"ExternalInput ei\n" +
				"Connection ei.output offset.input\n" +
				"Output ei output Real continuous\n";
		try {
			DngFile.importDng(text).commit();
			fail();
		} catch (DaccosimException e) {
			assertEquals("Problem in line 2. ei.output variable is not defined or it is used before its definition", e.getMessage());
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public void should_provoke_an_exception_if_node_is_defined_twice() {
		String text = "" +
				"ExternalInput ei\n" +
				"ExternalInput ei\n" +
				"Output ei output Real continuous\n";
		try {
			DngFile.importDng(text).commit();
			fail();
		} catch (DaccosimException e) {
			assertEquals("Problem in line 2. Node ei is already defined", e.getMessage());
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public void should_provoke_an_exception_if_variable_is_defined_twice() {
		String text = "" +
				"ExternalInput ei\n" +
				"Output ei output Real continuous\n" +
				"Output ei output Real continuous\n";
		try {
			DngFile.importDng(text).commit();
			fail();
		} catch (DaccosimException e) {
			assertEquals("Problem in line 3. Variable ei.output is already defined", e.getMessage());
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public void should_provoke_an_exception_if_variable_is_logged_without_definition() {
		String text = "" +
				"Log ei.output\n";
		try {
			DngFile.importDng(text).commit();
			fail();
		} catch (DaccosimException e) {
			assertEquals("Problem in line 1. ei node is not defined or it is used before its definition", e.getMessage());
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public void should_provoke_an_exception_if_variables_in_connection_are_not_the_same_type() {
		String text = "" +
				"ExternalInput ei" + "\n" +
				"Output ei output Integer dicrete\n" +
				"Offset offset Real 3.0\n" +
				"Input offset input Real dicrete\n" +
				"Connection ei.output offset.input\n";
		try {
			DngFile.importDng(text).commit();
			fail();
		} catch (DaccosimException e) {
			assertEquals("Problem in line 5. Connection type mismatch. From type is Integer. To type is Real.", e.getMessage());
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public void should_provoke_an_exception_if_variables_in_connection_are_not_the_variability() {
		String text = "" +
				"ExternalInput ei" + "\n" +
				"Output ei output Real continuous\n" +
				"Offset offset Real 3.0\n" +
				"Input offset input Real discrete\n" +
				"Connection ei.output offset.input\n";
		try {
			DngFile.importDng(text).commit();
			fail();
		} catch (DaccosimException e) {
			assertEquals("Problem in line 5. Connection variability mismatch. From variability is continuous. To variability is discrete.", e.getMessage());
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public void should_provoke_an_exception_if_variable_has_an_invalid_variability() {
		String text = "" +
				"ExternalOutput eo" + "\n" +
				"Input eo output Integer continuous\n";
		try {
			DngFile.importDng(text).commit();
			fail();
		} catch (DaccosimException e) {
			assertEquals("Problem in line 2. output has a variability (continuous) not valid for its type (Integer)", e.getMessage());
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public void should_provoke_an_exception_if_variable_has_an_invalid_variability_and_type_for_flow_variable() {
		String text = "" +
				"ExternalOutput eo" + "\n" +
				"Input eo output Integer discrete flow\n";
		try {
			DngFile.importDng(text).commit();
			fail();
		} catch (DaccosimException e) {
			assertEquals("Problem in line 2. output has a variability (discrete) and a type (Integer) not compatible with being flow variable.", e.getMessage());
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public void should_accep_flow_variable() {
		String text = "" +
				"ExternalOutput eo" + "\n" +
				"Input eo output Real continuous flow\n";
		try {
			DngFile.importDng(text).commit();
		} catch (Exception e) {
			fail();
		}
	}
}
