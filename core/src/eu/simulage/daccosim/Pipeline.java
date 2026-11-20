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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;

public class Pipeline {

	private static final String SEP = "@";
	private List<Stage> stages = new ArrayList<>();
	private int counter = 1;

	public Pipeline(String pipeline) {
		if (pipeline == null || pipeline.isEmpty()) return;
		stages = stream(pipeline.split(SEP)).map(Stage::new).collect(Collectors.toList());
	}

	public Pipeline() {
	}

	public List<Stage> stages() {
		return stages;
	}

	public int nodesCount() {
		return stages.stream().mapToInt(s -> s.nodes.size()).sum();
	}

	@Override
	public String toString() {
		return stages.stream().map(Stage::toString).collect(Collectors.joining(SEP));
	}

	public Stage stage(int stage) {
		if (stages.size() < stage) stages.add(new Stage());
		return stages.get(stage - 1);
	}

	public class Stage {
		private static final String SEP = "&";
		private final List<String> nodes;
		private final int id = counter++;

		public Stage(String stage) {
			nodes = asList(stage.split(SEP));
		}

		public Stage() {
			nodes = new ArrayList<>();
		}

		public List<String> nodes() {
			return nodes;
		}

		public String id() {
			return id + "";
		}

		@Override
		public String toString() {
			return String.join(SEP, nodes);
		}

		public void add(String node) {
			nodes.add(node);
		}
	}
}
