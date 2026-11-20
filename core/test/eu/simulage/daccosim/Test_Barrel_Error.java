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
import org.javafmi.wrapper.Simulation;
import org.javafmi.wrapper.v3.Access;

public class Test_Barrel_Error {

	public static void main(String[] args) {
		Simulation barrel = new Simulation("D:\\Users\\jevora\\repositories\\daccosim-simulage\\BarrelG.fmu");
		Access barrelAccess = new Access(barrel);

		barrelAccess.setUpExperiment(true, barrelAccess.getModelDescription().getDefaultExperiment().getTolerance(), 0, false, 0);
		System.out.println("Entering initialization mode for barrel");
		barrelAccess.enterInitializationMode();
		System.out.println("Setting size to 100");
		barrel.write("Size").with(100);
		barrelAccess.exitInitializationMode();
		System.out.println("Exit initialization mode for barrel");

		System.out.println("Barrel - Event stepping to 0.3: " + barrelAccess.hybridDoStep(0, 0.3).isOccurredEvent());
		System.out.println("Barrel - Event stepping to 0.6: " + barrelAccess.hybridDoStep(0.3, 0.3).isOccurredEvent());
		System.out.println("Barrel - Event stepping to 0.9: " + barrelAccess.hybridDoStep(0.6, 0.3).isOccurredEvent());
		FmiProxyV3.EventInfo info = new FmiProxyV3.EventInfo(true, false, false, false, false, 0.0);
		while (info.newDiscreteStatesNeeded) {
			barrelAccess.newDiscreteStates(info);
			System.out.println("barrel - more discrete states? " + info.newDiscreteStatesNeeded);
		}
	}
}
