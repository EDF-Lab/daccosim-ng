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

import com.google.gson.GsonBuilder;
import eu.simulage.daccosim.CoInitializer;
import eu.simulage.daccosim.DaccosimException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static eu.simulage.daccosim.Stepper.Method.ConstantStep;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

public class Graph {

	private final Settings settings = new Settings();
	private Map<String, GraphNode> nodes = new LinkedHashMap<>();
	private List<Arrow> arrows = new ArrayList<>();
	private Export export = null;

	public static Graph from(File file) {
		try (FileReader reader = new FileReader(file)) {
			return new GsonBuilder()
					.registerTypeAdapter(InitialValue.class, new Adapters.InitialValueJsonDeserializer())
					.registerTypeAdapter(Input.class, new Adapters.InputJsonDeserializer())
					.registerTypeAdapter(Output.class, new Adapters.OutputJsonDeserializer())
					.registerTypeAdapter(GraphNode.class, new Adapters.GraphNodeAdapter())
					.create().fromJson(reader, Graph.class);
		} catch (IOException e) {
			throw new DaccosimException(e.getMessage());
		}
	}

	public static Graph from(String text) {
		return new GsonBuilder().registerTypeAdapter(GraphNode.class, new Adapters.GraphNodeAdapter())
				.create().fromJson(text, Graph.class);
	}

	public List<GraphNode> nodes() {
		return new ArrayList<GraphNode>(nodes.values()) {
			@Override
			public boolean add(GraphNode o) {
				Graph.this.nodes.put(o.id(), o);
				return super.add(o);
			}
		};
	}

	public Graph nodes(GraphNode... nodes) {
		for (GraphNode node : nodes) this.nodes.put(node.id(), node);
		return this;
	}

	public List<Arrow> arrows() {
		return arrows;
	}

	public Graph arrows(Arrow... arrows) {
		this.arrows = asList(arrows);
		return this;
	}

	public Settings settings() {
		return settings;
	}

	public eu.simulage.daccosim.model.Graph commit() {
		return eu.simulage.daccosim.model.Graph.from(this);
	}

	public byte[] serialize() {
		return new GsonBuilder().registerTypeAdapter(GraphNode.class, new Adapters.GraphNodeAdapter()).create().toJson(this).getBytes();
	}

	public Graph exportAll() {
		export = export == null ? new Export() : export;
		export.variables().clear();
		export.variables("all");
		return this;
	}

	public Graph export(String... variables) {
		export = export == null ? new Export() : export;
		export.variables().clear();
		export.variables().addAll(asList(variables));
		return this;
	}

	public Export export() {
		return export == null ? export = new Export() : export;
	}

	public void clearExport() {
		export = null;
	}

	public GraphNode node(String nodeId) {
		return nodes.get(nodeId);
	}

	public class Settings {
		private CoInitialization coInitialization = new CoInitialization();
		private double startTime = 0;
		private double stopTime = 10;
		private String pipeline = "";
		private Stepper stepper = new Stepper();
		private JmsDataConnection jmsDataConnection;

		public CoInitialization coInitialization() {
			return coInitialization;
		}

		public Settings coInitialize() {
			this.coInitialization = new CoInitialization();
			return this;
		}

		public void clearCoInitialization() {
			this.coInitialization = null;
		}

		public Settings coInitialize(String method, double residualsTolerance, int maxIterations) {
			this.coInitialization = new CoInitialization();
			this.coInitialization.method = CoInitializer.Method.valueOf(method.replace(" ", ""));
			this.coInitialization.residualsTolerance = residualsTolerance;
			this.coInitialization.maxIterations = maxIterations;
			return this;
		}

		public double startTime() {
			return startTime;
		}

		public Settings startTime(double startTime) {
			this.startTime = startTime;
			return this;
		}

		public double stopTime() {
			return stopTime;
		}

		public Settings stopTime(double stopTime) {
			this.stopTime = stopTime;
			return this;
		}

		public String pipeline() {
			return this.pipeline;
		}

		public void pipeline(String pipeline) {
			this.pipeline = pipeline;
		}

		public Stepper stepper() {
			return this.stepper;
		}

		public Stepper stepper(eu.simulage.daccosim.Stepper.Method method) {
			return this.stepper = new Stepper().method(method);
		}

		public eu.simulage.daccosim.model.Graph commit() {
			return Graph.this.commit();
		}

		public Graph jms(String url, String user, String password, boolean coordinator) {
			jmsDataConnection = new JmsDataConnection(url, user, password, coordinator);
			return Graph.this;
		}

		public JmsDataConnection jmsDataConnection() {
			return jmsDataConnection;
		}

		public class CoInitialization {
			public CoInitializer.Method method = CoInitializer.Method.IterativePropagation;
			private double residualsTolerance = 100;
			private int maxIterations = 100;

			public double residualsTolerance() {
				return residualsTolerance;
			}

			public int maxIterations() {
				return maxIterations;
			}

			public CoInitializer.Method method() {
				return method;
			}

			public void method(CoInitializer.Method method) {
				this.method = method;
			}

			public void residualsTolerance(Double tolerance) {
				this.residualsTolerance = tolerance;
			}

			public void maxIterations(Integer maxIterations) {
				this.maxIterations = maxIterations;
			}
		}

		public class Stepper {
			private eu.simulage.daccosim.Stepper.Method method = ConstantStep;
			private int order = 3;
			private double stepSize = 1;
			private double safetyFactor = 0.9;
			private Map<String, Double> stepperVariables = new HashMap<>();
			private boolean transmitDerivatives = false;

			public eu.simulage.daccosim.Stepper.Method method() {
				return method;
			}

			public Stepper method(eu.simulage.daccosim.Stepper.Method method) {
				this.method = method;
				return this;
			}

			public int order() {
				return order;
			}

			public Stepper order(int order) {
				this.order = order;
				return this;
			}

			public double safetyFactor() {
				return safetyFactor;
			}

			public Stepper safetyFactor(double safetyFactor) {
				this.safetyFactor = safetyFactor;
				return this;
			}

			public double stepSize() {
				return stepSize;
			}

			public Stepper stepSize(double stepSize) {
				this.stepSize = stepSize;
				return this;
			}

			public Stepper stepperVariables(String... stepperVariables) {
				this.stepperVariables = stream(stepperVariables).collect(toMap(s -> s, s -> 1e-5));
				return this;
			}

			public Stepper stepperVariables(Map<String, Double> stepperVariables) {
				this.stepperVariables = stepperVariables;
				return this;
			}

			public Map<String, Double> stepperVariables() {
				return stepperVariables;
			}

			public boolean transmitDerivatives() {
				return this.transmitDerivatives;
			}

			public eu.simulage.daccosim.model.Graph commit() {
				return Graph.this.commit();
			}

			public Graph graph() {
				return Graph.this;
			}

			public void transmitDerivatives(boolean transmitDerivatives) {
				this.transmitDerivatives = transmitDerivatives;
			}
		}

		public class JmsDataConnection {

			private final String url;
			private final String user;
			private final String password;
			private final boolean coordinator;

			JmsDataConnection(String url, String user, String password, boolean coordinator) {
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
