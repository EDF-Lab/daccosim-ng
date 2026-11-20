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
import org.javafmi.proxy.v2.State;
import org.javafmi.wrapper.Simulation;
import org.javafmi.wrapper.v3.Access;

public class Test_C1_error {

	public static void main(String[] args) {
		Simulation c1 = new Simulation("D:\\Users\\jevora\\repositories\\daccosim-simulage\\C1.fmu");
		State c1State = State.newEmptyState();
		Access c1Access = new Access(c1);

		c1Access.setUpExperiment(true, ((ModelDescription) c1Access.getModelDescription()).getDefaultExperiment().getTolerance(), 0, false, 0);
		System.out.println("Entering initialization mode for C1");
		c1Access.enterInitializationMode();
		c1State = c1Access.getState(c1State);
		c1Access.exitInitializationMode();
		System.out.println("Exit initialization mode for C1");


		System.out.println("C1 - Event stepping to 0.3: " + c1Access.hybridDoStep(0, 0.3).isOccurredEvent());
		c1Access.setState(c1State);
		c1Access.hybridDoStep(0, 0);
	}
}
