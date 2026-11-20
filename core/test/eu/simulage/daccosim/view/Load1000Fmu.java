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

package eu.simulage.daccosim.view;

import com.sun.jna.Library;
import com.sun.jna.NativeLibrary;
import org.javafmi.kernel.OS;
import org.javafmi.wrapper.Simulation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Load1000Fmu {

	public static void main(String[] args) throws IOException {
		try {
			check("heatedTankCvode");
		} catch (Throwable e) {
			check("Matryoshka_barrel_MAT");
		}
//		withSimulation();
	}

	private static void check(String prefix) throws IOException {
		File file = new File("C:\\Users\\jevora\\Downloads\\dng_tests\\dllsCopies");
		file.mkdirs();
		for (int i = 1; i < 10000; i++) {
			String filename = prefix + i + ".dll";
			Files.copy(new File("C:\\Users\\jevora\\Downloads\\dng_tests\\" + prefix + ".dll").toPath(),
					new File(file, filename).toPath(), StandardCopyOption.REPLACE_EXISTING);
			NativeLibrary.getInstance(new File(file, filename).getAbsolutePath());
			System.out.println("Loaded: " + filename);
		}
	}

	private static void withSimulation() {
		List<Simulation> simulationList = new ArrayList<>();
		for (int i = 1; i < 1052; i++) {
			System.out.println("Loading " + i);
			Simulation simulation = new Simulation("C:\\Users\\jevora\\Downloads\\dng_tests\\heatedTankCvode.fmu");
			simulation.init(0);
			simulationList.add(simulation);
			System.out.println("Loaded " + i);
		}
	}
}
