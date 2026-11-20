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

import eu.simulage.daccosim.exporters.Exporter;
import eu.simulage.daccosim.exporters.ExporterWithInterpolation;
import eu.simulage.daccosim.exporters.NormalExporter;
import eu.simulage.daccosim.model.FMU3Proxy;
import eu.simulage.daccosim.model.FMUProxy;
import eu.simulage.daccosim.model.Graph;
import eu.simulage.daccosim.model.GraphNode;
import eu.simulage.daccosim.steppers.*;
import org.javafmi.proxy.v2.FmuProxy;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

import static eu.simulage.daccosim.Iterator.streamOf;
import static eu.simulage.daccosim.Stepper.Method.ConstantStep;
import static eu.simulage.daccosim.Stepper.Method.Euler;
import static java.lang.System.nanoTime;
import static java.util.concurrent.Executors.newScheduledThreadPool;

public class GraphExecutor {

	private static final String DACCOSIM_LOGGER = "daccosim";
	private static final Logger logger;
	private static final double ToMs = 1e6;

	static {
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$s] %5$s %n");
		Locale.setDefault(new Locale("en", "EN"));
		logger = Logger.getLogger(DACCOSIM_LOGGER);
		logger.setUseParentHandlers(false);
		if (logger.getHandlers().length == 0)
			logger.addHandler(new StreamHandler(System.out, new SimpleFormatter()) {
				@Override
				public synchronized void publish(LogRecord record) {
					boolean matryoshkaLog = Arrays.stream(Thread.currentThread().getStackTrace()).anyMatch(t -> t.getClassName().equals("siani.javafmi.Matryoshka"));
					if (matryoshkaLog && record.getLevel().intValue() <= Level.INFO.intValue()) return;
					super.publish(record);
					flush();
				}
			});
		System.setOut(new PrintStream(new OutputStream() {
			String line = "";

			@Override
			public void write(int b) {
				if ((char) b == '\n' && !line.trim().isEmpty()) {
					logger().info(line);
					line = "";
				} else if ((char) b != '\r') line += (char) b;
			}
		}));
	}

	private final Graph graph;
	private final Stepper stepper;
	private final Exporter exporter;
	private int numberOfSteps = 0;
	private double stopTime;
	private boolean isLogInFile = true;
	private ScheduledExecutorService service;
	private FileHandler logHandler;
	private boolean debugMode = false;

	public GraphExecutor(Graph graph) {
		this(graph, null);
	}

	public GraphExecutor(Graph graph, Stepper stepper) {
		this.graph = graph;
		this.stepper = stepper == null ? buildStepper() : stepper;
		this.exporter = graph.export().interpolationInterval() == 0 ? new NormalExporter(graph) : new ExporterWithInterpolation(graph, graph.export().interpolationInterval());
	}

	public static void execute(Graph graph) {
		new GraphExecutor(graph).execute();
	}

	public static Logger logger() {
		return logger;
	}

	public void execute() {
		if (graph.settings().jmsDataConnection() != null && !graph.settings().jmsDataConnection().isCoordinator())
			return;
		if (isLogInFile) {
			logHandler = logFileHandler();
			if (logHandler != null) logger().addHandler(logHandler);
		}
		logger().info("Stepper class: " + stepper.getClass().getSimpleName());
		logger().info("Co-sim started" + DaccosimUtils.printParameters(graph, exportFile(), debugMode));
		long start = nanoTime();
		load();
		long load = nanoTime();
		logger().info("FMU loading time: " + ((load - start) / ToMs) + " ms");
		if (!init()) {
			logger().info("Init process has failed. Co-simulation is exiting");
			terminate();
			return;
		}
		long init = nanoTime();
		logger().info("FMU init time: " + ((init - load) / ToMs) + " ms");
		service = newScheduledThreadPool(1);
		service.scheduleAtFixedRate(logCosimulationProgression(), 30, 30, TimeUnit.SECONDS);
		if (graph.settings().startTime() < stopTime) coSimulate();
		long coSimTime = nanoTime();
		logger().info("FMU co-sim time: " + ((coSimTime - init) / ToMs) + " ms");
		terminate();
		long end = nanoTime();
		logger().info("FMU termination time: " + ((end - coSimTime) / ToMs) + " ms");
		logger().info("Co-sim finished with " + numberOfSteps + " steps. Total time (load + init + co-sim + terminate): " + ((end - start) / ToMs) + " ms");
		stepper.logExecution();
		shutdown();
	}

	private Runnable logCosimulationProgression() {
		return new Runnable() {
			double lastTime = 0;

			@Override
			public void run() {
				double time = time();
				double realTimeFactor = (time - lastTime) / 30.;
				lastTime = time;
				logger().info("Simulation time: " + time + ". Real time factor: " + realTimeFactor);
			}
		};
	}

	private FileHandler logFileHandler() {
		try {
			String pattern = logFile().getAbsolutePath();
			FileHandler fileHandler = new FileHandler(pattern);
			fileHandler.setFormatter(new SimpleFormatter());
			return fileHandler;
		} catch (IOException e) {
			logger().severe(e.getMessage());
			return null;
		}
	}

	private void coSimulate() {
		double step = firstStep();
		exporter.export(stepper.time(), step);
		while (stepper.time() < stopTime) {
			step = step();
			exporter.export(stepper.time(), step);
		}
	}

	public void terminate() {
		try {
			streamOf(graph.nodes()).forEach(GraphNode::terminate);
		} catch (Exception ignored) {
		}
	}

	private void shutdown() {
		exporter.terminate();
		logHandler.close();
		if (service != null) service.shutdownNow();
	}

	public void load() {
//		try {
//			streamOf(graph.nodes()).forEach(GraphNode::load);
//		} catch (Throwable e) {
//			logger().warning("Impossible to load FMUs in multi-core mode. Switching to single-core");
		graph.nodes().forEach(GraphNode::load);
//		}
	}

	public boolean init() {
		try {
			streamOf(graph.nodes()).forEach(GraphNode::init);
			if (graph.settings().isCoInitialized()) CoInitializer.coInitialize(graph);
			stepper.init();
			exporter.export(graph.settings().startTime(), 0);
			stopTime = graph.settings().stopTime();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			logger().severe(e.getMessage());
			return false;
		}
	}

	public double firstStep() {
		numberOfSteps++;
		return stepper.firstStep();
	}

	public double step() {
		double step = stepper.step();
		numberOfSteps += step > 0 ? 1 : 0;
		return step;
	}

	private Stepper buildStepper() {
		return graph.nodes().stream().filter(n -> n instanceof FMUProxy)
				.noneMatch(n -> n instanceof FMU3Proxy) ? stepperV2() : stepperV3();
	}

	private Stepper stepperV2() {
		Stepper.Method method = graph.settings().stepper().method();
		return method == ConstantStep ? new Constant(graph) :
				method == Euler ? new Euler(graph) :
						method == Stepper.Method.AdamsBashforth ? new AdamsBashforth(graph) :
								null;
	}

	private Stepper stepperV3() {
		Stepper.Method method = graph.settings().stepper().method();
		return method == ConstantStep ? new HybridConstant(graph) :
				method == Euler ? new HybridEuler(graph) :
						method == Stepper.Method.AdamsBashforth ? new HybridAdamsBashforth(graph) :
								null;
	}

	public Graph graph() {
		return graph;
	}

	public double time() {
		return stepper.time();
	}

	File exportFile() {
		return exporter.file();
	}

	public void isLogInFile(boolean value) {
		isLogInFile = value;
	}

	File logFile() {
		return new File(exportFile().getAbsolutePath().replace(".csv", ".log"));
	}

	public void debugMode(boolean debugMode) {
		this.debugMode = debugMode;
		logger().setLevel(debugMode ? Level.ALL : Level.INFO);
		Arrays.stream(logger().getHandlers()).forEach(h -> h.setLevel(debugMode ? Level.ALL : Level.INFO));
		FmuProxy.LOGGING_ON = debugMode;
	}

}
