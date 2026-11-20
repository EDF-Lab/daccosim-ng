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
import org.junit.Test;

import java.nio.file.Files;

import static eu.simulage.daccosim.HelperTest.linesToString;
import static eu.simulage.daccosim.view.Graphs.environmentGraph;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

public class NormalExporterTest {

	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Test
	public void should_produce_correct_exportation() throws Exception {
		assumeTrue(OS.isWin64());
		Graph graph = environmentGraph()
				.exportAll()
				.settings().stopTime(3).commit();
		GraphExecutor executor = new GraphExecutor(graph);
		executor.execute();
		String expected = "time;dt;Environment.temperature;Building.externalTemperature;Adder.input1;Adder.input2;Adder.output;External.output\n" +
				"0.0;0.0;1.5;11.5;1.5;10.0;11.5;10.0\n" +
				"1.0;1.0;2.5;12.5;2.5;10.0;12.5;10.0\n" +
				"2.0;1.0;3.5;13.5;3.5;10.0;13.5;10.0\n" +
				"3.0;1.0;4.5;14.5;4.5;10.0;14.5;10.0\n";
		assertEquals(expected, linesToString(Files.readAllLines(executor.exportFile().toPath())));
		executor.exportFile().delete();
		executor.logFile().delete();
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Test
	public void should_produce_correct_exportation_with_different_configuration() throws Exception {
		assumeTrue(OS.isWin64());
		eu.simulage.daccosim.view.Graph graph = environmentGraph();
		graph.exportAll();
		graph.export().decimalSeparator(",").cellSeparator("\t");
		graph.settings().stopTime(3);
		GraphExecutor executor = new GraphExecutor(graph.commit());
		executor.execute();
		String expected = "time\tdt\tEnvironment.temperature\tBuilding.externalTemperature\tAdder.input1\tAdder.input2\tAdder.output\tExternal.output\n" +
				"0,0\t0,0\t1,5\t11,5\t1,5\t10,0\t11,5\t10,0\n" +
				"1,0\t1,0\t2,5\t12,5\t2,5\t10,0\t12,5\t10,0\n" +
				"2,0\t1,0\t3,5\t13,5\t3,5\t10,0\t13,5\t10,0\n" +
				"3,0\t1,0\t4,5\t14,5\t4,5\t10,0\t14,5\t10,0\n";
		assertEquals(expected, linesToString(Files.readAllLines(executor.exportFile().toPath())));
		executor.exportFile().delete();
		executor.logFile().delete();
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Test
	public void should_produce_correct_exportation_with_specific_variables() throws Exception {
		assumeTrue(OS.isWin64());
		Graph graph = environmentGraph().export("Building.externalTemperature").settings().stopTime(3).commit();
		GraphExecutor executor = new GraphExecutor(graph);
		executor.execute();
		String expected = "time;dt;Building.externalTemperature\n" +
				"0.0;0.0;11.5\n" +
				"1.0;1.0;12.5\n" +
				"2.0;1.0;13.5\n" +
				"3.0;1.0;14.5\n";
		assertEquals(expected, linesToString(Files.readAllLines(executor.exportFile().toPath())));
		executor.exportFile().delete();
		executor.logFile().delete();
	}

}
