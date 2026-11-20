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

import org.javafmi.wrapper.Simulation;
import org.javafmi.wrapper.v2.Access;

public class Test_Energy_Plus {

	public static void main(String[] args) throws InterruptedException {
		Simulation simulation = new Simulation("C:\\Users\\jevora\\Downloads\\dng_tests\\4bats\\Incas.fmu");
		Access access = new Access(simulation);
		access.setUpExperiment(false, 1.0E-6D, 0, true, 60*60*72);
		access.enterInitializationMode();
		access.exitInitializationMode();
		int time = 0;
		printVariables(access, time);
		for (int i = 0; i < 5; i++) {
			double step = 60;
			access.doStep(time ,step);
			time += step;
			printVariables(access, time);
		}
	}

	private static void printVariables(Access access, int time) {
//		System.out.println(time + " - IBB_T_RdC - " + access.getReal(100001)[0]);
		System.out.println(time + " - IBB_T_Rp1 - " + access.getReal(100002)[0]);
//		System.out.println(time + " - TOutDryBulb_C - " + access.getReal(100003)[0]);
//		System.out.println(time + " - Wind_Speed - " + access.getReal(100004)[0]);
	}

}
