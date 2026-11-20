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
import static eu.simulage.daccosim.Stepper.Method.Euler;
import static eu.simulage.daccosim.view.Graphs.simpleHeatTransfer;
import static org.junit.Assume.assumeTrue;

public class EulerTest {

    @Test
    @Ignore
    public void cosimulation_speed_test() {
        Graph graph = simpleHeatTransfer()
		        .settings().stopTime(86400).coInitialize("NewtonRaphson", 1e-5, 100)
                .stepper().method(Euler).stepSize(10).stepperVariables("sinus.Text", "wall.Q_b", "wall.Q_a", "zoneA.Tin", "zoneB.Tin")
                .commit();
        GraphExecutor executor = new GraphExecutor(graph);
        long init = System.nanoTime();
        executor.execute();
        executor.logFile().delete();
        long end = System.nanoTime();
        System.out.println(((end - init) / 1e9));
    }

    @Test
    @Ignore
    public void simple_heat_transfer_when_first_step_is_with_size_10() {
        assumeTrue(OS.isWin64());
        Graph graph = simpleHeatTransfer()
		        .settings().stopTime(86400).coInitialize("NewtonRaphson", 1e-5, 100)
                .stepper().method(Euler).stepSize(10).stepperVariables("sinus.Text", "wall.Q_b", "wall.Q_a", "zoneA.Tin", "zoneB.Tin")
                .commit();
        GraphExecutor executor = new GraphExecutor(graph);
        executor.load();
        executor.init();
        checkHeatTransfer(executor, 0.0,283.15, 293.15,293.15, 283.15,293.15, 283.15,293.15);
        executor.firstStep();
	    checkHeatTransfer(executor, 10.0, 283.15727220457563, 293.1162095811426, 293.1162095811426, 283.15727220457563, 293.1162095811426, 283.15727220457563, 293.1162095811426);
        executor.step();
	    checkHeatTransfer(executor, 20.0, 283.1645444053054, 293.0825355769544, 293.0825355769544, 283.1645444053054, 293.0825355769544, 283.1645444053054, 293.0825355769544);
        executor.step();
	    checkHeatTransfer(executor, 40.31531493076862, 283.17931808235426, 293.0147835714938, 293.0147835714938, 283.17931808235426, 293.0147835714938, 283.17931808235426, 293.0147835714938);
        executor.step();
	    checkHeatTransfer(executor, 77.4345433888264, 283.20631169142587, 292.89292757766606, 292.89292757766606, 283.20631169142587, 292.89292757766606, 283.20631169142587, 292.89292757766606);
        executor.step();
	    checkHeatTransfer(executor, 127.94916452612296, 283.24304591553926, 292.72974587280424, 292.72974587280424, 283.24304591553926, 292.72974587280424, 283.24304591553926, 292.72974587280424);
        executor.step();
	    checkHeatTransfer(executor, 178.46378566341951, 283.27977888401807, 292.5684165027219, 292.5684165027219, 283.27977888401807, 292.5684165027219, 283.27977888401807, 292.5684165027219);
        executor.step();
	    checkHeatTransfer(executor, 204.21297518249736, 283.29850240760373, 292.4882625118869, 292.4882625118869, 283.29850240760373, 292.4882625118869, 283.29850240760373, 292.4882625118869);
        executor.step();
	    checkHeatTransfer(executor, 236.77964819441166, 283.32218251037193, 292.38839772241886, 292.38839772241886, 283.32218251037193, 292.38839772241886, 283.32218251037193, 292.38839772241886);
        executor.step();
	    checkHeatTransfer(executor, 293.4997085335797, 283.36342280572933, 292.21620529963855, 292.21620529963855, 283.36342280572933, 292.21620529963855, 283.36342280572933, 292.21620529963855);
        executor.step();
	    checkHeatTransfer(executor, 322.08829130168607, 283.38420779815385, 292.13150335995493, 292.13150335995493, 283.38420779815385, 292.13150335995493, 283.38420779815385, 292.13150335995493);
        executor.step();
	    checkHeatTransfer(executor, 358.089855276735, 283.4103808601242, 292.0267816552089, 292.0267816552089, 283.4103808601242, 292.0267816552089, 283.4103808601242, 292.0267816552089);
    }

	@Ignore
	@Test
	public void simple_heat_transfer() {
		assumeTrue(OS.isWin64());
		Graph graph = simpleHeatTransfer()
				.settings().stopTime(86400).coInitialize("NewtonRaphson", 1e-5, 100)
				.stepper().method(Euler).stepSize(10).stepperVariables("sinus.Text", "wall.Q_b", "wall.Q_a", "zoneA.Tin", "zoneB.Tin")
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
		checkHeatTransfer(executor, 4.604926107268017, 283.1533487967033, 293.13439608925376, 293.13439608925376, 283.1533487967033, 293.13439608925376, 283.1533487967033, 293.13439608925376);
		executor.step();
		checkHeatTransfer(executor, 12.429809056157854, 283.15903921099505, 293.1079859779864, 293.1079859779864, 283.15903921099505, 293.1079859779864, 283.15903921099505, 293.1079859779864);
		executor.step();
		checkHeatTransfer(executor, 32.42788367681226, 283.1735822006263, 293.0410631905558, 293.0410631905558, 283.1735822006263, 293.0410631905558, 283.1735822006263, 293.0410631905558);
		executor.step();
		checkHeatTransfer(executor, 78.16858823908721, 283.20684549536065, 292.8909233128923, 292.8909233128923, 283.20684549536065, 292.8909233128923, 283.20684549536065, 292.8909233128923);
		executor.step();
		checkHeatTransfer(executor, 123.90929280136217, 283.2401081611197, 292.74334613596335, 292.74334613596335, 283.2401081611197, 292.74334613596335, 283.2401081611197, 292.74334613596335);
		executor.step();
		checkHeatTransfer(executor, 170.9467626189062, 283.274312791851, 292.5931636908741, 292.5931636908741, 283.274312791851, 292.5931636908741, 283.274312791851, 292.5931636908741);
		executor.step();
		checkHeatTransfer(executor, 196.6593112182002, 283.2930098117639, 292.5128613160401, 292.5128613160401, 283.2930098117639, 292.5128613160401, 283.2930098117639, 292.5128613160401);
		executor.step();
		checkHeatTransfer(executor, 234.41098560044983, 283.3204602231368, 292.39676550885775, 292.39676550885775, 283.3204602231368, 292.39676550885775, 283.3204602231368, 292.39676550885775);
		executor.step();
		checkHeatTransfer(executor, 289.43898614776504, 283.360470428436, 292.22929924748894, 292.22929924748894, 283.360470428436, 292.22929924748894, 283.360470428436, 292.22929924748894);
	}

}
