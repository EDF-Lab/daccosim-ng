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

public class Test_1_0 {

	public static final String SEP = ";";
	public static LinkedHashMap<String, Double> variables = new LinkedHashMap<>();
	public static State state = newEmptyState();

	public static void main(String[] args) throws InterruptedException {
		System.out.println("Loading");
		Simulation simulation = new Simulation("C:\\Users\\jevora\\Downloads\\Car.fmu");
		System.out.println("Doing init");
		simulation.init(0);
		System.out.println("Terminating");
		simulation.terminate();
		System.out.println("Terminated");
	}
}
