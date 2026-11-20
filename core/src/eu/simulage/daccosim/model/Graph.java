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

import eu.simulage.daccosim.CoInitializer;
import eu.simulage.daccosim.DaccosimException;
import eu.simulage.daccosim.DsgImporter;
import eu.simulage.daccosim.view.Helper;

import java.util.*;

import static eu.simulage.daccosim.CoInitializer.Method.IterativePropagation;
import static java.util.Arrays.asList;

public class Graph {

	private Settings settings;
	private Map<String, GraphNode> nodes = new LinkedHashMap<>();
	private List<GraphNode> nodeList = new ArrayList<>();
	private List<Arrow> arrows = new ArrayList<>();
	private Export export;

	public static Graph from(eu.simulage.daccosim.view.Graph graph) {
		return DsgImporter.importFrom(graph);
	}

	public Settings settings() {
		return settings;
	}

	public void settings(Settings settings) {
		this.settings = settings;
	}

	public List<GraphNode> nodes() {
		if(nodes.size() != nodeList.size()) nodeList = new ArrayList<>(nodes.values());
		return nodeList;
	}

	@SuppressWarnings("UnusedReturnValue")
	public Graph nodes(GraphNode... nodes) {
		for (GraphNode node : nodes) this.nodes.put(node.id(), node);
		this.nodes.values().forEach(node -> node.graph(this));
		return this;
	}

	public List<Arrow> arrows() {
		return arrows;
	}

	@SuppressWarnings("UnusedReturnValue")
	public Graph arrows(Arrow... arrows) {
		this.arrows = asList(arrows);
		this.arrows.forEach(arrow -> arrow.graph(this));
		return this;
	}

	public GraphNode node(String id) {
		return nodes.get(id);
	}

	public Export export() {
		return export;
	}

	@SuppressWarnings("UnusedReturnValue")
	public Graph export(Export export) {
		this.export = export;
		return this;
	}

	private GraphNode findNode(String nodeId) {
		GraphNode node = node(nodeId);
		if (node == null) throw new DaccosimException("Node " + nodeId + " not found");
		return node;
	}

	Output findOutput(String ioPath) {
		String ioId = Helper.ioId(ioPath);
		Output output = findNode(Helper.nodeId(ioPath)).output(ioId);
		if (output == null) throw new DaccosimException("Output " + ioPath + " not found");
		return output;
	}

	Input findInput(String ioPath) {
		String ioId = Helper.ioId(ioPath);
		Input input = findNode(Helper.nodeId(ioPath)).input(ioId);
		if (input == null) throw new DaccosimException("Input " + ioPath + " not found");
		return input;
	}

	public Variable variable(String ioPath) {
		String ioId = Helper.ioId(ioPath);
		Input input = findNode(Helper.nodeId(ioPath)).input(ioId);
		if (input != null) return input;
		Output output = findNode(Helper.nodeId(ioPath)).output(ioId);
		if (output != null) return output;
		Variable variable = findNode(Helper.nodeId(ioPath)).variable(ioId);
		if (variable != null) return variable;
		throw new DaccosimException("Connector " + ioPath + " not found");
	}

	public static class Settings {
		private final double startTime;
		private final double stopTime;
		private String pipeline = "";
		private final CoInitialization coInitialization;
		private final Stepper stepper;
		private final JmsDataConnection jmsDataConnection;

		public Settings(double startTime, double stopTime, String pipeline, CoInitialization coInitialization, Stepper stepper, JmsDataConnection jmsDataConnection) {
			this.startTime = startTime;
			this.stopTime = stopTime;
			this.pipeline = pipeline;
			this.coInitialization = coInitialization;
			this.stepper = stepper;
			this.jmsDataConnection = jmsDataConnection;
		}


		public double startTime() {
			return startTime;
		}

		public double stopTime() {
			return stopTime;
		}

		public String pipeline() {
			return pipeline == null ? "" : pipeline;
		}

		public boolean isCoInitialized() {
			return coInitialization != null;
		}

		public CoInitialization coInitialization() {
			return coInitialization;
		}

		public Stepper stepper() {
			return stepper;
		}

		public JmsDataConnection jmsDataConnection() {
			return jmsDataConnection;
		}

		public static class CoInitialization {
			private final CoInitializer.Method method;
			private final double residualsTolerance;
			private final int maxIterations;

			public CoInitialization(CoInitializer.Method method, double residualsTolerance, int maxIterations) {
				this.method = method != null ? method : IterativePropagation;
				this.residualsTolerance = residualsTolerance;
				this.maxIterations = maxIterations;
			}

			public double residualsTolerance() {
				return residualsTolerance;
			}

			public int maxIterations() {
				return maxIterations;
			}

			public CoInitializer.Method method() {
				return method;
			}

		}

		public static class Stepper {
			private final eu.simulage.daccosim.Stepper.Method method;
			private final int order;
			private final double stepSize;
			private final double safetyFactor;
			private final Map<String, Double> stepperVariables;
			private final boolean transmitDerivatives;

			public Stepper(eu.simulage.daccosim.Stepper.Method method, int order, double stepSize, double safetyFactor, Map<String, Double> stepperVariables, boolean transmitDerivatives) {
				this.method = method;
				this.order = order;
				this.stepSize = stepSize;
				this.safetyFactor = safetyFactor;
				this.stepperVariables = stepperVariables;
				this.transmitDerivatives = transmitDerivatives;
			}

			public eu.simulage.daccosim.Stepper.Method method() {
				return method;
			}

			public int order() {
				return order;
			}

			public double safetyFactor() {
				return safetyFactor;
			}

			public double stepSize() {
				return stepSize;
			}

			public Map<String, Double> stepperVariables() {
				return stepperVariables;
			}

			public boolean transmitDerivatives() {
				return transmitDerivatives;
			}
		}

		@SuppressWarnings("unused")
		public static class JmsDataConnection {

			private final String url;
			private final String user;
			private final String password;
			private final boolean coordinator;

			public JmsDataConnection(String url, String user, String password, boolean coordinator) {
				this.url = url;
				this.user = user;
				this.password = password;
				this.coordinator = coordinator;
			}

			public String url() {
				return url;
			}

			public String user() {
				return user;
			}

			public String password() {
				return password;
			}

			public boolean isCoordinator() {
				return coordinator;
			}
		}

	}
}
