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

public class Test_issue_27 {

	public static void main(String[] args) {
		Simulation simulation1 = new Simulation("C:\\Users\\jevora\\Downloads\\dng_tests\\euler_new_test\\Euler\\Pulse.fmu");
		Simulation simulation2 = new Simulation("C:\\Users\\jevora\\Downloads\\dng_tests\\euler_new_test\\Euler\\Pulse.fmu");
		simulation1.init(0);
		simulation2.init(0);
	}
}
