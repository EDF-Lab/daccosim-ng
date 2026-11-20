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

package eu.simulage.daccosim.model;

import java.util.List;

public class Export {

	private final String cellSeparator;
	private final String decimalSeparator;
	private final List<String> variables;
	private String prefix = "";
	private String folder = ".";
	private double interpolationInterval = 0.0;

	public Export(String cellSeparator, String decimalSeparator, List<String> variables) {
		this.cellSeparator = cellSeparator;
		this.decimalSeparator = decimalSeparator;
		this.variables = variables;
	}

	public String cellSeparator() {
		return cellSeparator;
	}

	public String decimalSeparator() {
		return decimalSeparator;
	}

	public List<String> variables() {
		return variables;
	}

	public Export prefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	public String prefix() {
		return this.prefix;
	}

	public String folder() {
		return folder;
	}

	public Export folder(String folder) {
		this.folder = folder;
		return this;
	}

	public Export interpolationInterval(double interpolationInterval) {
		this.interpolationInterval = interpolationInterval;
		return this;
	}

	public double interpolationInterval() {
		return interpolationInterval;
	}
}
