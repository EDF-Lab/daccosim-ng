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
import org.hamcrest.CoreMatchers;

import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

class HelperTest {
	static void checkEnvironmentVariables(Graph graph, double environmentTemperature, double adderInput1, double adderInput2, double adderOutput, double buildingExternalTemperature) {
		assertThat(graph.node("Environment").output("temperature").value(), is(environmentTemperature));
		assertThat(graph.node("External").output("output").value(), is(10.0));
		assertThat(graph.node("Adder").input("input1").value(), is(adderInput1));
		assertThat(graph.node("Adder").input("input2").value(), is(adderInput2));
		assertThat(graph.node("Adder").output("output").value(), is(adderOutput));
		assertThat(graph.node("Building").input("externalTemperature").value(), is(buildingExternalTemperature));
	}

	static void checkTankYPipeVariables(Graph graph, double tank3InputRate, double tank3Level, double tank3OutputRate, double tank2Level, double tank2OutputRate, double outputRate, double tank1Level, double tank1OutputRate, double yPipeInputRate1, double yPipeInputRate2, double yPipeOutputRate) {
		assertEquals(tank3InputRate, (double) graph.node("Tank3").input("inputRate").value(), 1e-5);
		assertEquals(tank3Level, (double) graph.node("Tank3").output("level").value(), 1e-5);
		assertEquals(tank3OutputRate, (double) graph.node("Tank3").output("outputRate").value(), 1e-5);
		assertEquals(0.1, (double) graph.node("Tank2").input("inputRate").value(), 1e-5);
		assertEquals(tank2Level, (double) graph.node("Tank2").output("level").value(), 1e-5);
		assertEquals(tank2OutputRate, (double) graph.node("Tank2").output("outputRate").value(), 1e-5);
		assertEquals(0.5, (double) graph.node("inputRate1").output("output").value(), 1e-5);
		assertEquals(0.1, (double) graph.node("inputRate2").output("output").value(), 1e-5);
		assertEquals(outputRate, (double) graph.node("outputRate").input("input").value(), 1e-5);
		assertEquals(0.5, (double) graph.node("Tank1").input("inputRate").value(), 1e-5);
		assertEquals(tank1Level, (double) graph.node("Tank1").output("level").value(), 1e-5);
		assertEquals(tank1OutputRate, (double) graph.node("Tank1").output("outputRate").value(), 1e-5);
		assertEquals(yPipeInputRate1, (double) graph.node("YPipe").input("inputRate1").value(), 1e-5);
		assertEquals(yPipeInputRate2, (double) graph.node("YPipe").input("inputRate2").value(), 1e-5);
		assertEquals(yPipeOutputRate, (double) graph.node("YPipe").output("outputRate").value(), 1e-5);
	}

	static void checkEventsVariables(Graph graph, double m1Y, double m2Y) {
		assertEquals(m1Y, (double) graph.node("M1").output("y").value(), 1e-5);
		assertEquals(m2Y, (double) graph.node("M2").output("y").value(), 1e-5);
		assertEquals(0.0, (double) graph.node("M3").output("y").value());
	}

	static void checkHeatTransfer(GraphExecutor executor, double time, double sinusText, double wallT_a, double wallT_b, double zoneBText, double zoneBTin, double zoneAText, double zoneATin) {
//		assertThat(executor.time(), CoreMatchers.is(time));
//		assertThat(executor.graph().node("sinus").output("Text").value(), CoreMatchers.is(sinusText));
//		assertThat(executor.graph().node("wall").output("Q_a").value(), CoreMatchers.is(-0.0));
//		assertThat(executor.graph().node("wall").output("Q_b").value(), CoreMatchers.is(0.0));
//		assertThat(executor.graph().node("wall").input("T_a").value(), CoreMatchers.is(wallT_a));
//		assertThat(executor.graph().node("wall").input("T_b").value(), CoreMatchers.is(wallT_b));
//		assertThat(executor.graph().node("zoneB").input("Qin").value(), CoreMatchers.is(0.0));
//		assertThat(executor.graph().node("zoneB").input("Text").value(), CoreMatchers.is(zoneBText));
//		assertThat(executor.graph().node("zoneB").output("Tin").value(), CoreMatchers.is(zoneBTin));
//		assertThat(executor.graph().node("zoneA").input("Qin").value(), CoreMatchers.anyOf(is(0.0), is(-0.0)));
//		assertThat(executor.graph().node("zoneA").input("Text").value(), CoreMatchers.is(zoneAText));
//		assertThat(executor.graph().node("zoneA").output("Tin").value(), CoreMatchers.is(zoneATin));
		System.out.print(executor.time() + "\t");
		System.out.print(executor.graph().node("sinus").output("Text").value() + "\t");
		System.out.print(executor.graph().node("wall").output("Q_a").value() + "\t");
		System.out.print(executor.graph().node("wall").output("Q_b").value() + "\t");
		System.out.print(executor.graph().node("wall").input("T_a").value() + "\t");
		System.out.print(executor.graph().node("wall").input("T_b").value() + "\t");
		System.out.print(executor.graph().node("zoneB").input("Qin").value() + "\t");
		System.out.print(executor.graph().node("zoneB").input("Text").value() + "\t");
		System.out.print(executor.graph().node("zoneB").output("Tin").value() + "\t");
		System.out.print(executor.graph().node("zoneA").input("Qin").value() + "\t");
		System.out.print(executor.graph().node("zoneA").input("Text").value() + "\t");
		System.out.println(executor.graph().node("zoneA").output("Tin").value());
	}

	static String linesToString(List<String> lines) {
		StringBuilder result = new StringBuilder();
		lines.forEach(l -> result.append(l).append("\n"));
		return result.toString();
	}

}
