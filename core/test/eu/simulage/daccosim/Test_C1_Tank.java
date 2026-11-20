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

import org.javafmi.proxy.FmiProxyV3;
import org.javafmi.proxy.v2.State;
import org.javafmi.wrapper.Simulation;
import org.javafmi.wrapper.v3.Access;

public class Test_C1_Tank {

	public static void main(String[] args) {
		Simulation c1 = new Simulation("D:\\Users\\jevora\\repositories\\daccosim-simulage\\C1.fmu");
		Simulation tank = new Simulation("D:\\Users\\jevora\\repositories\\daccosim-simulage\\TankG.fmu");
		State c1State = State.newEmptyState();
		State tankState = State.newEmptyState();
		c1.init(0);
		tank.init(0);
		Access c1Access = new Access(c1);
		Access tankAccess = new Access(tank);
		c1State = c1Access.getState(c1State);
		tankState = tankAccess.getState(tankState);

		printVariables(c1, tank);

		System.out.println("C1 - Event stepping to 0.3: " + c1Access.hybridDoStep(0, 0.3).isOccurredEvent());
		System.out.println("Tank - Event stepping to 0.3: " + tankAccess.hybridDoStep(0, 0.3).isOccurredEvent());
		propagateValve(c1, tank);
		printVariables(c1, tank);
		c1State = c1Access.getState(c1State);
		tankState = tankAccess.getState(tankState);

		FmiProxyV3.HybridDoStepStatus status = c1Access.hybridDoStep(0.3, 0.3);
		System.out.println("C1 - Event stepping to 0.6: " + status.isOccurredEvent());
		System.out.println("Tank - Event stepping to 0.6: " + tankAccess.hybridDoStep(0.3, 0.3).isOccurredEvent());

		System.out.println("======= START EVENT PROCESSING AT " + status.getEndTime() + " =======");
		System.out.println("\tRolling back tank fmu");
		tankAccess.setState(tankState);
		System.out.println("\tTank - Event stepping to 0.5: " + tankAccess.hybridDoStep(0.3, 0.2).isOccurredEvent());
		propagateValve(c1, tank);
		printVariables(c1, tank);
		processEvents("c1", c1Access);
		propagateValve(c1, tank);
		processEvents("tank", tankAccess);
		printVariables(c1, tank);
		System.out.println("======= END EVENT PROCESSING AT " + status.getEndTime() + " =======");

		System.out.println("C1 - Event stepping to 0.8: " + c1Access.hybridDoStep(0.5, 0.3).isOccurredEvent());
		status = tankAccess.hybridDoStep(0.5, 0.3);
		System.out.println("Tank - Event stepping to 0.8: " + status.isOccurredEvent());
		propagateValve(c1, tank);
		printVariables(c1, tank);
	}

	private static boolean processEvents(String id, Access access) {
		int counter = -1;
		FmiProxyV3.EventInfo info = new FmiProxyV3.EventInfo(true, false, false, false, false, 0.0);
		while (info.newDiscreteStatesNeeded) {
			System.out.println("\t" + id + " - more discrete states? " + info.newDiscreteStatesNeeded);
			access.newDiscreteStates(info);
			counter++;
		}
		System.out.println("\t" + id + " - more discrete states? " + info.newDiscreteStatesNeeded);
		return counter > 0;
	}

	private static void propagateValve(Simulation c1, Simulation tank) {
		System.out.println("\tPropagating C1.Valve to Tank.Valve");
		tank.write("Valve").with(c1.read("Valve").asBoolean());
	}

	private static void printVariables(Simulation c1, Simulation tank) {
		System.out.println("\tC1.valve: " + c1.read("Valve").asBoolean() +
				"\tTank.outFlow: " + tank.read("outFlow").asDouble());
	}
}
