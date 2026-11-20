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

import static eu.simulage.daccosim.HelperTest.checkHeatTransfer;
import static eu.simulage.daccosim.Stepper.Method.AdamsBashforth;
import static eu.simulage.daccosim.view.Graphs.simpleHeatTransfer;
import static org.junit.Assume.assumeTrue;


public class AdamsBashforthTest {

	@Test
	@Ignore
	public void cosimulation_speed_test() {
		Graph graph = simpleHeatTransfer()
				.settings().stopTime(86400).coInitialize("NewtonRaphson", 1e-5, 100)
				.stepper().method(AdamsBashforth).order(3).stepSize(10).stepperVariables("sinus.Text", "wall.Q_b", "wall.Q_a", "zoneA.Tin", "zoneB.Tin")
				.commit();
		GraphExecutor executor = new GraphExecutor(graph);
		long init = System.nanoTime();
		executor.execute();
		long end = System.nanoTime();
		executor.logFile().delete();
		System.out.println(((end - init) / 1e9));
	}

	@Test
	@Ignore
	public void simple_heat_transfer_when_first_step_is_with_size_10() {
		assumeTrue(OS.isWin64());
		Graph graph = simpleHeatTransfer()
				.settings().stopTime(86400).coInitialize("NewtonRaphson", 1e-5, 100)
				.stepper().method(AdamsBashforth).order(3).stepSize(10).stepperVariables("sinus.Text", "wall.Q_b", "wall.Q_a", "zoneA.Tin", "zoneB.Tin")
				.commit();
		GraphExecutor executor = new GraphExecutor(graph);
		executor.load();
		executor.init();
		checkHeatTransfer(executor, 0.0, 283.15, 293.15, 293.15, 283.15, 293.15, 283.15, 293.15);
		executor.firstStep();

		checkHeatTransfer(executor, 10.0, 283.15727220457563, 293.1162095811426, 293.1162095811426, 283.15727220457563, 293.1162095811426, 283.15727220457563, 293.1162095811426);
		executor.step();
		checkHeatTransfer(executor, 20.0, 283.1645444053054, 293.0825355769544, 293.0825355769544, 283.1645444053054, 293.0825355769544, 283.1645444053054, 293.0825355769544);
		executor.step();
		executor.step();
		executor.step();
		checkHeatTransfer(executor, 41.0, 283.179815997211, 293.0121113819337, 293.0121113819337, 283.179815997211, 293.0121113819337, 283.179815997211, 293.0121113819337);
		executor.step();
		checkHeatTransfer(executor, 42.0, 283.1805432144207, 293.0087784346269, 293.0087784346269, 283.1805432144207, 293.0087784346269, 283.1805432144207, 293.0087784346269);
		executor.step();
		executor.step();
		executor.step();
		checkHeatTransfer(executor, 45.33219138403189, 283.18296644015373, 292.99767834382965, 292.99767834382965, 283.18296644015373, 292.99767834382965, 283.18296644015373, 292.99767834382965);
		executor.step();
		checkHeatTransfer(executor, 46.664382768063774, 283.18393523164633, 292.9932473471124, 292.9932473471124, 283.18393523164633, 292.9932473471124, 283.18393523164633, 292.9932473471124);
		executor.step();
		executor.step();
		executor.step();
		checkHeatTransfer(executor, 50.51169181214743, 283.1867330562617, 292.98045827503483, 292.98045827503483, 283.1867330562617, 292.98045827503483, 283.1867330562617, 292.98045827503483);
		executor.step();
		checkHeatTransfer(executor, 51.69461808816732, 283.1875932985845, 292.97653188066636, 292.97653188066636, 283.1875932985845, 292.97653188066636, 283.1875932985845, 292.97653188066636);
		executor.step();
		executor.step();
		executor.step();
		checkHeatTransfer(executor, 55.30384378591011, 283.1902179817072, 292.96455843837356, 292.96455843837356, 283.1902179817072, 292.96455843837356, 283.1902179817072, 292.96455843837356);
		executor.step();
		checkHeatTransfer(executor, 56.547216931613136, 283.1911221806964, 292.96043988480295, 292.96043988480295, 283.1911221806964, 292.96043988480295, 283.1911221806964, 292.96043988480295);
		executor.step();
		executor.step();
		executor.step();
		checkHeatTransfer(executor, 60.255832893278395, 283.19381913799896, 292.9481619891948, 292.9481619891948, 283.19381913799896, 292.9481619891948, 283.19381913799896, 292.9481619891948);
	}

	@Ignore
	@Test
	public void simple_heat_transfer() {
		assumeTrue(OS.isWin64());
		Graph graph = simpleHeatTransfer()
				.settings().stopTime(86400).coInitialize("NewtonRaphson", 1e-5, 100)
				.stepper().method(AdamsBashforth).order(3).stepSize(10).stepperVariables("sinus.Text", "wall.Q_b", "wall.Q_a", "zoneA.Tin", "zoneB.Tin")
				.commit();
		GraphExecutor executor = new GraphExecutor(graph);
		executor.load();
		executor.init();
		checkHeatTransfer(executor, 0.0, 283.15, 293.15, 293.15, 283.15, 293.15, 283.15, 293.15);
		executor.firstStep();
		checkHeatTransfer(executor, 1.0, 283.150727220521, 293.1466082203478, 293.1466082203478, 283.150727220521, 293.1466082203478, 283.150727220521, 293.1466082203478);
		executor.step();
		checkHeatTransfer(executor, 2.0, 283.1514544410382, 293.14321761419944, 293.14321761419944, 283.1514544410382, 293.14321761419944, 283.1514544410382, 293.14321761419944);
		executor.step();
		executor.step();
		executor.step();
		checkHeatTransfer(executor, 5.0, 283.1536361025282, 293.133053749117, 293.133053749117, 283.1536361025282, 293.133053749117, 283.1536361025282, 293.133053749117);
		executor.step();
		checkHeatTransfer(executor, 6.0, 283.1543633229915, 293.1296694418763, 293.1296694418763, 283.1543633229915, 293.1296694418763, 283.1543633229915, 293.1296694418763);
		executor.step();
		executor.step();
		executor.step();
		checkHeatTransfer(executor, 9.303471879023014, 283.156765675157, 293.1184975505911, 293.1184975505911, 283.156765675157, 293.1184975505911, 283.156765675157, 293.1184975505911);
		executor.step();
		checkHeatTransfer(executor, 10.606943758046029, 283.157713586408, 293.11409511297063, 293.11409511297063, 283.157713586408, 293.11409511297063, 283.157713586408, 293.11409511297063);
		executor.step();
		executor.step();
		executor.step();
		checkHeatTransfer(executor, 14.38262517276094, 283.160459338274, 293.10135217039334, 293.10135217039334, 283.160459338274, 293.10135217039334, 283.160459338274, 293.10135217039334);
		executor.step();
		checkHeatTransfer(executor, 15.551362829429824, 283.1613092677786, 293.09741318729556, 293.09741318729556, 283.1613092677786, 293.09741318729556, 283.1613092677786, 293.09741318729556);
		executor.step();
		executor.step();
		executor.step();
		checkHeatTransfer(executor, 19.112566927364277, 283.16389904641613, 293.0854189476808, 293.0854189476808, 283.16389904641613, 293.0854189476808, 283.16389904641613, 293.0854189476808);
		executor.step();
		checkHeatTransfer(executor, 20.33629571196096, 283.1647889661854, 293.08130307614016, 293.08130307614016, 283.1647889661854, 293.08130307614016, 283.1647889661854, 293.08130307614016);
		executor.step();
		executor.step();
		executor.step();
		checkHeatTransfer(executor, 23.988235713971857, 283.16744472844175, 293.0690282428751, 293.0690282428751, 283.16744472844175, 293.0690282428751, 283.16744472844175, 293.0690282428751);
	}


}
