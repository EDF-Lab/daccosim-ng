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

import org.javafmi.modeldescription.v2.ModelDescription;
import org.javafmi.proxy.FmiProxyV3;
import org.javafmi.proxy.v2.State;
import org.javafmi.wrapper.Simulation;
import org.javafmi.wrapper.v3.Access;

public class Test_C1_Tank_Barrel {

	public static void main(String[] args) {
		Simulation c1 = new Simulation("D:\\Users\\jevora\\repositories\\daccosim-simulage\\C1.fmu");
		Simulation tank = new Simulation("D:\\Users\\jevora\\repositories\\daccosim-simulage\\TankG.fmu");
		Simulation barrel = new Simulation("D:\\Users\\jevora\\repositories\\daccosim-simulage\\BarrelG.fmu");
		State tankState = State.newEmptyState();
		State c1State = State.newEmptyState();
		c1.init(0);
		tank.init(0);
		Access c1Access = new Access(c1);
		Access tankAccess = new Access(tank);
		Access barrelAccess = new Access(barrel);

		barrelAccess.setUpExperiment(true, ((ModelDescription) barrel.getModelDescription()).getDefaultExperiment().getTolerance(), 0, false, 0);
		System.out.println("Entering initialization mode for Barrel");
		barrelAccess.enterInitializationMode();
		System.out.println("Setting Barrel.Size to 100");
		barrel.write("Size").with(100);
		barrelAccess.exitInitializationMode();
		System.out.println("Exit initialization mode for Barrel");

		tankState = tankAccess.getState(tankState);
		c1State = c1Access.getState(c1State);

		System.out.println("C1 - Event stepping to 0.3: " + c1Access.hybridDoStep(0, 0.3).isOccurredEvent());
		System.out.println("Tank - Event stepping to 0.3: " + tankAccess.hybridDoStep(0, 0.3).isOccurredEvent());
		System.out.println("Barrel - Event stepping to 0.3: " + barrelAccess.hybridDoStep(0, 0.3).isOccurredEvent());

		tankAccess.setState(tankState);
		tankAccess.hybridDoStep(0, 0);
		FmiProxyV3.EventInfo info = new FmiProxyV3.EventInfo(true, false, false, false, false, 0.0);
		tankAccess.newDiscreteStates(info);

		c1Access.setState(c1State);
		c1Access.hybridDoStep(0, 0);
		info = new FmiProxyV3.EventInfo(true, false, false, false, false, 0.0);
		c1Access.newDiscreteStates(info);
	}
}
