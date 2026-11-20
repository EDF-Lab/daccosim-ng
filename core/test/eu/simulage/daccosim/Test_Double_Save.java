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

import static org.javafmi.proxy.v2.State.newEmptyState;

public class Test_Double_Save {

	public static void main(String[] args) throws InterruptedException {
		Simulation simulation = new Simulation("C:\\Users\\jevora\\Downloads\\JPhSaut\\TankBarrel.fmu");
		simulation.init(0, 17);
		Access access = new Access(simulation);
		access.doStep(0, 0.5);
		State state = State.newEmptyState();
		access.getState(state);
		System.out.println("Barrel.Water: " + simulation.read("Barrel.Water").asDouble());
		simulation.write("Valve").with(true);
		access.doStep(0.5, 0.2);
		System.out.println("Valve: " + simulation.read("Valve").asBoolean());
		System.out.println("Barrel.Water: " + simulation.read("Barrel.Water").asDouble());
		System.out.println("Der.Barrel.Water: " + simulation.read("der(Barrel.Water)").asDouble());
		access.setState(state);
		access.getState(state);
		access.getState(state);
		simulation.write("Valve").with(true);
		access.doStep(0.5, 0.2);
		System.out.println("Valve: " + simulation.read("Valve").asBoolean());
		System.out.println("Barrel.Water: " + simulation.read("Barrel.Water").asDouble());
		System.out.println("Der.Barrel.Water: " + simulation.read("der(Barrel.Water)").asDouble());
		access.setState(state);
		access.getState(state);
		access.getState(state);
		simulation.write("Valve").with(true);
		access.doStep(0.5, 0.2);
		System.out.println("Valve: " + simulation.read("Valve").asBoolean());
		System.out.println("Barrel.Water: " + simulation.read("Barrel.Water").asDouble());
		System.out.println("Der.Barrel.Water: " + simulation.read("der(Barrel.Water)").asDouble());
	}
}
