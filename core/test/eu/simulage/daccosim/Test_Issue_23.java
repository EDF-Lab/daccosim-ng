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

import java.util.LinkedHashMap;

import static org.javafmi.proxy.v2.State.newEmptyState;

public class Test_Issue_23 {

	public static final String SEP = ";";
	public static LinkedHashMap<String, Double> variables = new LinkedHashMap<>();
	public static State state = newEmptyState();

	public static void main(String[] args) throws InterruptedException {
		Simulation simulation = new Simulation("C:\\Users\\jevora\\Downloads\\issue 23\\Barrel.fmu");
		simulation.init(0, 17);
		Access access = new Access(simulation);
		FmiProxyV3.EventInfo info;

		state = access.getState(state);
		FmiProxyV3.HybridDoStepStatus status = access.hybridDoStep(0, 0.3);
		System.out.println("Event: " + status.isOccurredEvent());

		status = access.hybridDoStep(0.3, 0.2);
		System.out.println("Event: " + status.isOccurredEvent() + " Time: " + status.getEndTime());

		System.out.println("-------");
		sequenceCausingWrongBehaviour(simulation, access);
		System.out.println("-------");
		sequenceCausingWrongBehaviour(simulation, access);
	}

	private static void sequenceCausingWrongBehaviour(Simulation simulation, Access access) {
		FmiProxyV3.HybridDoStepStatus status;
		FmiProxyV3.EventInfo info;
		System.out.println("Writing Tank.inFlow after event...");
		simulation.write("inFlow").with(2.125);

		info = new FmiProxyV3.EventInfo(true, false, false, false, false, 0.0);
		access.newDiscreteStates(info);
		System.out.println("New states in Barrel?: " + info.newDiscreteStatesNeeded);

		System.out.println("Reading values from FMU after states: " + simulation.read("der(Water)").asDouble());
		System.out.println("Writing inFlow in Barrel coming from stage 1 (tank)...");
		simulation.write("inFlow").with(1.9422572681745303);

		status = access.hybridDoStep(0.5, 0.3);
		System.out.println("Event: " + status.isOccurredEvent() + " Time: " + status.getEndTime());

		info = new FmiProxyV3.EventInfo(true, false, false, false, false, 0.0);
		access.newDiscreteStates(info);
		System.out.println("New states in Barrel? " + info.newDiscreteStatesNeeded); // Shouldn't be false
	}

}
