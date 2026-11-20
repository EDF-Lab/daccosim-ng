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

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static eu.simulage.daccosim.Iterator.Mode.MC;
import static eu.simulage.daccosim.Iterator.Mode.SC;

public class Iterator {

	private static Mode mode = MC;
	private static StreamGenerator generator;
	private static IntStreamGenerator intStreamGenerator;

	static {
		generator = Collection::parallelStream;
		intStreamGenerator = l -> IntStream.range(0, l.size()).parallel();
	}

	public static void forceSingleThread() {
		generator = Collection::stream;
		intStreamGenerator = l -> IntStream.range(0, l.size());
		mode = SC;
	}

	public static Mode mode() {
		return mode;
	}

	public static <T> Stream<T> streamOf(Collection<T> tList) {
		return generator.generate(tList);
	}

	public static IntStream range(List<?> values) {
		return intStreamGenerator.generate(values);
	}

	enum Mode {MC, SC}

	private interface StreamGenerator {
		<T> Stream<T> generate(Collection<T> tList);
	}

	private interface IntStreamGenerator {
		IntStream generate(Collection<?> tList);
	}
}
