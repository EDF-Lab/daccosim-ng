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

package eu.simulage.shell;

import org.javafmi.wrapper.Simulation;

public class ManualMa {

	public static void main(String[] args) {
		Simulation process = new Simulation("D:\\Users\\Downloads\\daccosim-cases\\processwin3264.fmu");
		process.init(0, 20);
		double time = 0;
		double valveOpening = 0.0;
		while (time < 20) {
			process.write("valveOpening").with(valveOpening);
			process.doStep(0.01);
			System.out.println(process.read("level").asDouble() + "");
			time += 0.01;
		}
	}
}
