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

import org.javafmi.proxy.v2.State;
import org.javafmi.wrapper.Simulation;
import org.javafmi.wrapper.v2.Access;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.javafmi.proxy.v2.State.newEmptyState;

public class Test_Signal_Size {

	public static State state = newEmptyState();

	public static void main(String[] args) throws InterruptedException {
		System.out.println("======================================================= STEP_SIZE: 0.01");
		with0p01();
		System.out.println("======================================================= STEP_SIZE: 0.3");
		with0p3();
	}

	private static void with0p01() {
		Simulation simulation = new Simulation("C:\\Users\\jevora\\Downloads\\JPhSaut\\C1C2.fmu");
		simulation.init(0, 17);
		Access access = new Access(simulation);
		double time = 0;
		for (int i = 0; i < 17 * 100; i++) {
			double step = 0.01;
			state = access.getState(state);
			access.doStep(time, step);
			if(i == 680){
				System.out.println("time: " + (time + step) + ". Size: " + simulation.read("Size").asDouble() + ". Doing rollback");
				access.setState(state);
				System.out.println("time: " + (time) + ". Size: " + simulation.read("Size").asDouble() + ". After rollback. Next step size: " + (6.8 - time));
				access.doStep(time, step = 6.8 - time);
			}
			time += step;
			System.out.println("time: " + time + ". Size: " + simulation.read("Size").asDouble());
		}
	}

	private static void with0p3() {
		Simulation simulation = new Simulation("C:\\Users\\jevora\\Downloads\\JPhSaut\\C1C2.fmu");
		simulation.init(0, 17);
		Access access = new Access(simulation);
		double time = 0;
		for (int i = 0; i < 57; i++) {
			double step = 0.3;
			state = access.getState(state);
			access.doStep(time, step);
			if(i == 22){
				System.out.println("time: " + (time + step) + ". Size: " + simulation.read("Size").asDouble() + ". Doing rollback");
				access.setState(state);
				System.out.println("time: " + (time) + ". Size: " + simulation.read("Size").asDouble() + ". After rollback. Next step size: " + (6.8 - time));
				access.doStep(time, step = 6.8 - time);
			}
			time += step;
			System.out.println("time: " + time + ". Size: " + simulation.read("Size").asDouble());
		}
	}

}
