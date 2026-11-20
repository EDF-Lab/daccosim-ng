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

import eu.simulage.daccosim.DataType;

import static eu.simulage.daccosim.view.Helper.defaultValueOf;

public class Variable {
	private final String id;
	private final DataType type;
	private Object value;
	private GraphNode graphNode;
	private String variability = "discrete";

	public Variable(String id, DataType type) {
		this.id = id;
		this.type = type;
		this.value = defaultValueOf(type);
	}

	public String id() {
		return id;
	}

	public DataType type() {
		return type;
	}

	public String path() {
		return graphNode.id() + "." + id;
	}

	public GraphNode parent() {
		return graphNode;
	}

	public void parent(GraphNode graphNode) {
		this.graphNode = graphNode;
	}

	public Object value() {
		return value;
	}

	public void value(Object value) {
		this.value = value;
	}

	public Double derivative() {
		return Double.NaN;
	}

	public Double secondDerivative() {
		return Double.NaN;
	}

	public void variability(String variability) {
		this.variability = variability;
	}

	public String variability() {
		return variability;
	}
}
