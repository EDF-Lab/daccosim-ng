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

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class Export {

	private String cellSeparator = ";";
	private String decimalSeparator = ".";
	private String prefix = "";
	private List<String> variables = new ArrayList<>();
	private String folder = ".";
	private double interpolationInterval = 0.0;

	public String cellSeparator() {
		return cellSeparator;
	}

	public Export cellSeparator(String cellSeparator) {
		this.cellSeparator = cellSeparator;
		return this;
	}

	public String decimalSeparator() {
		return decimalSeparator;
	}

	public Export decimalSeparator(String decimalSeparator) {
		this.decimalSeparator = decimalSeparator;
		return this;
	}

	public List<String> variables() {
		return variables;
	}

	@SuppressWarnings("UnusedReturnValue")
	public Export variables(String... variables) {
		this.variables = asList(variables);
		return this;
	}

	public Export prefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	public String prefix() {
		return prefix;
	}

	public String folder() {
		return folder;
	}

	public Export folder(String folder) {
		this.folder = folder;
		return this;
	}

	public void interpolationInterval(double interpolationInterval) {
		this.interpolationInterval = interpolationInterval;
	}

	public double interpolationInterval() {
		return this.interpolationInterval;
	}
}
