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

import Jama.Matrix;
import eu.simulage.daccosim.model.*;
import org.javafmi.modeldescription.v2.Capabilities;
import org.javafmi.modeldescription.v2.ModelDescription;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static eu.simulage.daccosim.DaccosimUtils.flowInputsIn;
import static eu.simulage.daccosim.DaccosimUtils.getModelDescription;
import static eu.simulage.daccosim.GraphExecutor.logger;
import static eu.simulage.daccosim.Iterator.streamOf;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.range;

public class CoInitializer {

	private final Graph graph;
	private final List<Arrow> arrows;
	private final Set<String> flowInputs;
	private final Map<GraphNode, Set<String>> inputs = new HashMap<>();
	private final Map<GraphNode, Set<String>> outputs = new HashMap<>();

	private CoInitializer(Graph graph) {
		this.graph = graph;
		this.arrows = graph.arrows().stream().filter(a -> a.from().type().equals(DataType.Real)).collect(toList());
		this.flowInputs = DaccosimUtils.flowInputsIn(graph);
	}

	public static void coInitialize(Graph graph) {
		new CoInitializer(graph).coInitialize();
	}

	private void coInitialize() {
		List<List<Arrow>> loops = groupArrows();
		logger().info("Number of SCCs: " + loops.size());
		if (graph.settings().coInitialization().method() == Method.NewtonRaphson && checkCapabilitiesNewtonRaphson())
			coInitializeWithNewtonRaphson(loops);
		else coInitializeWithIterativePropagation(loops);
	}

	private boolean checkCapabilitiesNewtonRaphson() {
		boolean result = doCheckCapabilitiesNewtonRaphson();
		if (!result) logger().info("Newton Raphson is selected as co-init method but FMUs does not provide directional derivatives. Switching to IP.");
		return result;
	}

	private boolean doCheckCapabilitiesNewtonRaphson() {
		return graph.nodes().stream().filter(b -> b instanceof FMUProxy)
				.allMatch(f -> ((FMUProxy) f).canProvideDirectionalDerivatives());
	}

	private void coInitializeWithNewtonRaphson(List<List<Arrow>> loops) {
		OutputTransmitter transmitter = new OutputTransmitter(graph, nonLoopedArrows(loops));
		transmitter.execute();
		List<Object> values = streamOf(arrows).map(a -> a.from().value()).collect(toList());
		List<Object> newValues = values;
		ArrayList<List<Arrow>> clone = new ArrayList<>(loops);
		for (int j = 0; j < graph.settings().coInitialization().maxIterations(); j++) {
			for (int i = 0, listsSize = clone.size(); i < listsSize; i++) {
				values = newValues;
				int iters = coInitialize(clone.get(i));
				logger().info("SSC " + (i + 1) + " - Newton Raphson had " + iters + " iterations. Pipeline stage " + (i + 1));
				if (iters == graph.settings().coInitialization().maxIterations()) {
					logger().info("Newton Raphson has not converged. Exiting simulation");
					throw new DaccosimException("Co-initialization has not converged");
				}
				newValues = streamOf(arrows).map(a -> a.from().value()).collect(toList());
				transmitter.execute();
			}
			double count = residuals(values, newValues);
			if (loops.size() <= 1 || count < graph.settings().coInitialization().residualsTolerance()) {
				transmitter.execute();
				return;
			}
		}
	}

	private void coInitializeWithIterativePropagation(List<List<Arrow>> loops) {
		List<Arrow> arrows = nonLoopedArrows(loops);
		OutputTransmitter transmitter = new OutputTransmitter(graph, arrows);
		List<Object> values = streamOf(arrows).map(a -> a.from().value()).collect(toList());
		List<Object> newValues = values;
		Set<String> flowVariables = flowInputsIn(graph);
		for (int k = 0; k < graph.settings().coInitialization().maxIterations(); k++) {
			for (int i = 0; i < loops.size(); i++) {
				values = newValues;
				int iters = processIterativePropagationOnScc(loops.get(i), i, flowVariables);
				if (loops.size() > 1) transmitter.execute();
				if (iters == graph.settings().coInitialization().maxIterations()) {
					if(doCheckCapabilitiesNewtonRaphson()){
						logger().info("Iterative Propagation has not converged. Switching to Newton-Raphson");
						graph.nodes().stream().filter(n -> n instanceof FMUProxy).forEach(n -> ((FMUProxy) n).reset());
						coInitializeWithNewtonRaphson(groupArrows());
						return;
					}
					else{
						logger().info("Iterative Propagation has not converged. Newton-Raphson cannot be used as there are non-compatible FMUs");
						return;
					}
				}
				newValues = streamOf(arrows).map(a -> a.from().value()).collect(toList());
			}
			double count = residuals(values, newValues);
			if (loops.size() > 1 || count < graph.settings().coInitialization().residualsTolerance()) {
				transmitter.execute();
				return;
			}
		}
	}

	private double residuals(List<Object> values, List<Object> newValues) {
		double count = 0;
		for (int i = 0; i < values.size(); i++)
			count += Math.abs((double) values.get(i) - (double) newValues.get(i));
		return count;
	}

	private int processIterativePropagationOnScc(List<Arrow> loop, int sccId, Set<String> flowVariables) {
		Map<Input, List<Integer>> connections = DaccosimUtils.asMap(loop);
		logHeaders(loop);
		log(loop, "0");
		if (loop.isEmpty()) {
			logger().info("SCC " + (sccId + 1) + " - Iterative Propagation had 0 iterations. Pipeline stage " + (sccId + 1));
			return 0;
		}
		List<Object> fromValues = streamOf(loop).map(a -> a.from().value()).collect(toList());
		List<Object> newFromValues = fromValues;
		int iters = 0;
		for (int j = 0; j < graph.settings().coInitialization().maxIterations(); j++) {
			setVariables(connections, flowVariables, fromValues = newFromValues);
			newFromValues = streamOf(loop).map(a -> a.from().value()).collect(toList());
			double residuals = residual(difference(fromValues, newFromValues));
			log(residuals, false);
			iters++;
			log(graph.arrows(), iters + "");
			if (residuals < graph.settings().coInitialization().residualsTolerance()) {
				log(residuals, true);
				break;
			}
		}
		logger().info("SCC " + (sccId + 1) + " - Iterative Propagation had " + iters + " iterations. Pipeline stage " + (sccId + 1));
		return iters;
	}

	private List<List<Arrow>> groupArrows() {
		if (graph.settings().pipeline().isEmpty() || new Pipeline(graph.settings().pipeline()).stages().size() == 1) {
			List<List<Arrow>> scComponents = new Tarjan().getSCComponents(graph);
			Collections.reverse(scComponents);
			return scComponents;
		}
		return groupArrows(graph.settings().pipeline());
	}

	private List<List<Arrow>> groupArrows(String pipelineDef) {
		return new Pipeline(pipelineDef).stages().stream().map(s -> {
			HashSet<String> nodes = new HashSet<>(s.nodes());
			return graph.arrows().stream()
					.filter(a -> nodes.contains(a.fromNode().id()) && nodes.contains(a.toNode().id()))
					.collect(toList());
		}).collect(toList());
	}

	private double residual(double[] differences) {
		double total = 0;
		for (double residual : differences) total += Math.abs(residual);
		return total / (differences.length == 0 ? 1 : differences.length);
	}

	private double[] difference(List<Object> fromValues, List<Object> newFromValues) {
		double[] residuals = new double[fromValues.size()];
		for (int i = 0; i < fromValues.size(); i++)
			if (!(fromValues.get(i) instanceof Double)) residuals[i] = 0.0;
			else residuals[i] = (Double) newFromValues.get(i) - (Double) fromValues.get(i);
		return residuals;
	}

	private void setVariables(Map<Input, List<Integer>> connections, Set<String> flowVariables, List<Object> newValues) {
		List<Object> oldValues = connections.keySet().stream().map(Variable::value).collect(toList());
		AtomicInteger i = new AtomicInteger(-1);
		connections.forEach((input, indexes) -> {
			i.getAndIncrement();
			if (input.type() != DataType.Real) return;
			double oldValue = (double) oldValues.get(i.get());
			double newValue = (double) newValues.get(indexes.get(0));
			if (flowVariables.contains(input.path()))
				newValue = indexes.stream().mapToDouble(j -> -(Double) newValues.get(j)).sum();
			input.value(oldValue + (newValue - oldValue));
		});
	}

	private int coInitialize(List<Arrow> loop) {
		initMaps(loop);
		Map<Input, List<Integer>> arrows = DaccosimUtils.asMap(loop);
		int iteration = 0;
		logHeaders(loop);
		log(loop, "0");
		double residuals = 0;
		for (; iteration < graph.settings().coInitialization().maxIterations(); iteration++) {
			Matrix residualsArray = difference(arrows, loop);
			residuals = residual(residualsArray.getColumnPackedCopy());
			log(residuals, false);
			if (residuals < graph.settings().coInitialization().residualsTolerance()) break;
			List<Double> corrections = calculateCorrections(loop, residualsArray);
			applyCorrections(loop, corrections);
			log(loop, (iteration + 1) + "");
		}
		log(residuals, true);
		return iteration;
	}

	private void initMaps(List<Arrow> loop) {
		range(0, loop.size()).forEach(i -> range(0, loop.size())
				.forEach(j -> {
					GraphNode node = loop.get(i).toNode();
					if (!outputs.containsKey(node)) outputs.put(node, new HashSet<>());
					if (!inputs.containsKey(node)) inputs.put(node, new HashSet<>());
					outputs.get(node).add(loop.get(j).from().id());
					inputs.get(node).add(loop.get(i).to().id());
				}));
	}

	private void applyCorrections(List<Arrow> loop, List<Double> corrections) {
		Set<String> inputsCorrected = new HashSet<>();
		List<Object> values = loop.stream().map(l -> l.to().value()).collect(toList());
		for (int i = 0; i < loop.size(); i++) {
			if (inputsCorrected.contains(loop.get(i).to().path())) continue;
			if (!(values.get(i) instanceof Double)) continue;
			loop.get(i).to().value((double) values.get(i) + corrections.get(i));
			inputsCorrected.add(loop.get(i).to().path());
		}
	}

	private void logHeaders(List<Arrow> loop) {
		logger().fine("loop\t" + loop.stream().map(a -> a.from().path()).collect(Collectors.joining("\t")));
	}

	private void log(List<Arrow> loop, String preface) {
		logger().fine(preface + "\t" + loop.stream().map(a -> a.from().value().toString()).collect(Collectors.joining("\t")));
	}

	private List<Arrow> nonLoopedArrows(List<List<Arrow>> excludedArrows) {
		List<Arrow> arrows = new ArrayList<>(this.arrows);
		excludedArrows.forEach(arrows::removeAll);
		return arrows;
	}

	private Matrix difference(Map<Input, List<Integer>> arrowMap, List<Arrow> arrows) {
		Matrix residuals = new Matrix(arrows.size(), 1);
		range(0, arrows.size()).forEach(i -> {
			Arrow arrow = arrows.get(i);
			if (!flowInputs.contains(arrow.to().path()))
				residuals.set(i, 0, arrow.to().value() instanceof Double ? -((double) arrow.to().value() - (double) arrow.from().value()) : 0.0);
			else
				residuals.set(i, 0, -((double) arrow.to().value() - arrowMap.get(arrow.to()).stream().mapToDouble(j -> -(Double) arrows.get(j).from().value()).sum()));
		});
		return residuals;
	}

	private void log(double residuals, boolean last) {
		if (last) logger().info("Final residual value: " + residuals);
		else logger().fine("R " + residuals);
	}

	private List<Double> calculateCorrections(List<Arrow> loop, Matrix residuals) {
		Matrix correction = solve(loop, residuals);
		List<Double> corrections = new ArrayList<>();
		for (int i = 0; i < correction.getRowDimension(); i++) corrections.add(correction.get(i, 0));
		return corrections;
	}

	private Matrix solve(List<Arrow> loop, Matrix residuals) {
		try {
			return jacobian(loop).solve(residuals);
		} catch (RuntimeException e) {
			logger().fine(e.getMessage());
			return residuals;
		}
	}

	private Matrix jacobian(List<Arrow> loop) {
		Matrix jacobian = new Matrix(loop.size(), loop.size());
		Map<GraphNode, Map<String, Double>> derivatives = new HashMap<>();
		outputs.keySet().parallelStream().forEach(n -> derivatives.put(n, derivativesOf(n, outputs.get(n), inputs.get(n))));
		range(0, loop.size()).forEach(i -> range(0, loop.size())
				.forEach(j -> jacobian.set(j, i, i != j ? derivativeOf(derivatives, loop, i, j) : 1.0)));
//		int bound = loop.size(); // TODO remove after tests
//		for (int idx = 0; idx < bound; idx++) {
//			int i = idx;
//			int bound1 = loop.size();
//			for (int j = 0; j < bound1; j++) {
//				System.out.print(jacobian.get(j, i) + ";");
//			}
//			System.out.println("");
//		}
//		System.out.println("----------------------\n");
		return jacobian;
	}

	private Map<String, Double> derivativesOf(GraphNode node, Set<String> outputs, Set<String> inputs) {
		if (node instanceof FMUProxy)
			return ((FMUProxy) node).derivativeOf(new ArrayList<>(inputs), new ArrayList<>(outputs));
		return emptyMap();
	}

	private double derivativeOf(Map<GraphNode, Map<String, Double>> derivatives, List<Arrow> loop, int i, int j) {
		GraphNode node = loop.get(i).toNode();
		return derivatives.get(node).getOrDefault(loop.get(i).to().id() + loop.get(j).from().id(), 0.0);
	}

	public enum Method {IterativePropagation, NewtonRaphson}

	private static class Tarjan {
		private int V, preCount;
		private int[] low;
		private boolean[] visited;
		private List<Integer>[] graph;
		private List<List<Integer>> sccComp;
		private Stack<Integer> stack;

		List<List<Arrow>> getSCComponents(Graph graph) {
			V = graph.nodes().size();
			this.graph = toTarjanGraph(graph);
			low = new int[V];
			visited = new boolean[V];
			stack = new Stack<>();
			sccComp = new ArrayList<>();

			for (int v = 0; v < V; v++)
				if (!visited[v])
					dfs(v);

			return toArrows(graph, sccComp);
		}

		private List<List<Arrow>> toArrows(Graph graph, List<List<Integer>> sccComp) {
			List<List<Arrow>> result = new ArrayList<>();
			Map<GraphNode, Integer> nodeIndex = range(0, graph.nodes().size()).boxed().collect(toMap(i -> graph.nodes().get(i), i -> i));
			for (int i = 0; i < sccComp.size(); i++) {
				result.add(new ArrayList<>());
				for (Arrow arrow : graph.arrows()) {
					if (sccComp.get(i).contains(nodeIndex.get(arrow.fromNode())) &&
							sccComp.get(i).contains(nodeIndex.get(arrow.toNode())))
						result.get(i).add(arrow);
				}
			}
			return result.stream().filter(l -> !l.isEmpty()).collect(Collectors.toList());
		}

		@SuppressWarnings("unchecked")
		private List<Integer>[] toTarjanGraph(Graph graph) {
			List<Integer>[] result = new List[graph.nodes().size()];
			Map<GraphNode, Integer> nodeIndex = range(0, graph.nodes().size()).boxed().collect(toMap(i -> graph.nodes().get(i), i -> i));
			range(0, graph.nodes().size()).forEach(i -> result[i] = new ArrayList<>());
			for (Arrow arrow : graph.arrows())
				result[nodeIndex.get(arrow.fromNode())].add(nodeIndex.get(arrow.toNode()));
			return result;
		}

		private void dfs(int v) {
			low[v] = preCount++;
			visited[v] = true;
			stack.push(v);
			int min = low[v];
			for (int w : graph[v]) {
				if (!visited[w])
					dfs(w);
				if (low[w] < min)
					min = low[w];
			}
			if (min < low[v]) {
				low[v] = min;
				return;
			}
			List<Integer> component = new ArrayList<>();
			int w;
			do {
				w = stack.pop();
				component.add(w);
				low[w] = V;
			} while (w != v);
			sccComp.add(component);
		}
	}
}
