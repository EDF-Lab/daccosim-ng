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

import eu.simulage.daccosim.DataType;

import static eu.simulage.daccosim.DataType.Real;
import static eu.simulage.daccosim.view.Operator.Type.*;

public class Helper {
	public static Graph graph() {
		return new Graph();
	}

	public static FMU FMU(String id, String path) {
		return new FMU(id, path);
	}

	public static Output output(String id, DataType type, String variability) {
		return (Output) new Output(id, type).variability(variability);
	}

	public static Output output(String id, DataType type, String variability, Object value) {
		return (Output) new Output(id, type).value(value).variability(variability);
	}

	public static Input input(String id, DataType type, String variability) {
		return (Input) new Input(id, type).variability(variability);
	}

	public static Input input(String id, DataType type, Object value, String variability) {
		return (Input) new Input(id, type).value(value).variability(variability);
	}

	public static Variable var(String id, DataType type, String variability) {
		return new Variable(id, type).variability(variability);
	}

	public static Variable var(String id, DataType type) {
		return new Variable(id, type);
	}

	public static Operator adderOperator(String id) {
		return new Operator(id, Adder);
	}

	public static Operator offsetOperator(String id, Object value) {
		return new Operator(id, Offset).value(value);
	}

	public static Operator multiplierOperator(String id) {
		return new Operator(id, Multiplier);
	}

	public static Operator gainOperator(String id, Object value) {
		return new Operator(id, Gain).value(value);
	}

	public static Arrow arrow(String from, String to) {
		return new Arrow(from, to);
	}

	public static ExternalInput externalInput(String id) {
		return new ExternalInput(id);
	}

	public static ExternalOutput externalOutput(String id) {
		return new ExternalOutput(id);
	}

	public static InitialValue initialValue(String name, Object value) {
		return new InitialValue(name, value);
	}

	public static Object defaultValueOf(DataType type) {
		if (type == null) return null;
		return type.equals(Real) ? 0.0 :
				type.equals(DataType.Integer) ? 0 :
						type.equals(DataType.Boolean) ? false :
								"";
	}

	public static String nodeId(String ioPath) {
		int index = ioPath.indexOf(".");
		return index != -1 ? ioPath.substring(0, index) : ioPath;
	}

	public static String ioId(String ioPath) {
		return ioPath.substring(ioPath.indexOf(".") + 1);
	}
}
