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

package eu.simulage.daccosim.exporters;

import eu.simulage.daccosim.DaccosimException;
import eu.simulage.daccosim.model.Export;
import eu.simulage.daccosim.model.Graph;
import eu.simulage.daccosim.model.GraphNode;
import eu.simulage.daccosim.model.Variable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class NormalExporter implements Exporter {

	public static int BufferSize = 8 * 1024;
	private final List<Variable> variableList;
	private final File file;
	private final BufferedWriter writer;
	private final Export export;
	private final Formatter doubleFormatter;

	public NormalExporter(Graph graph) {
		try {
			this.export = graph.export();
			this.variableList = !isExportNull() ? variablesOf(graph) : new ArrayList<>();
			this.file = new File(folder(), filename());
			this.writer = !variableList.isEmpty() ? new BufferedWriter(new FileWriter(file), BufferSize) : null;
			this.doubleFormatter = !isExportNull() ? (export.decimalSeparator().equals(".") ?
					Object::toString : o -> o.toString().replace(".", export.decimalSeparator())) : null;
			writeHeaders();
		} catch (IOException e) {
			throw new DaccosimException(e.getMessage());
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	String folder() {
		String path = export.folder();
		new File(path).mkdirs();
		return path;
	}

	@SuppressWarnings("SpellCheckingInspection")
	String filename() {
		return (export.prefix().isEmpty() ? "" : export.prefix() + "-") +
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + ".csv";
	}

	private boolean isExportNull() {
		return export == null || export.variables().isEmpty();
	}

	private void writeHeaders() {
		if (isExportNull()) return;
		StringBuilder builder = new StringBuilder();
		builder.append("time").append(export.cellSeparator()).append("dt");
		variableList.forEach(c -> builder.append(export.cellSeparator()).append(c.path()));
		write(builder.append("\n").toString());
	}

	private void write(String string) {
		try {
			writer.write(string);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private List<Variable> variablesOf(Graph graph) {
		return isExportAll(graph.export()) ? allVariables(graph) : variablesOf(graph.export().variables(), graph);
	}

	private List<Variable> allVariables(Graph graph) {
		return graph.nodes().stream().map(this::variables).flatMap(Collection::stream).collect(toList());
	}

	private List<Variable> variablesOf(List<String> variables, Graph graph) {
		List<Variable> result = new ArrayList<>();
		variables.forEach(v -> result.add(graph.variable(v)));
		return result;
	}

	private List<Variable> variables(GraphNode node) {
		List<Variable> result = new ArrayList<>(node.inputs());
		result.addAll(node.outputs());
		result.addAll(node.variables());
		return result;
	}

	private boolean isExportAll(Export export) {
		return !export.variables().isEmpty() && export.variables().get(0).equals("all");
	}

	public void export(double time, double dt) {
		if (isExportNull()) return;
		StringBuilder builder = new StringBuilder();
		builder.append(format(time)).append(export.cellSeparator()).append(format(dt));
		for (Variable variable : variableList) builder.append(export.cellSeparator()).append(format(variable.value()));
		write(builder.append("\n").toString());
	}

	private String format(Object value) {
		return value == null ? "null" :
				value instanceof Double ? doubleFormatter.format(value) :
						value instanceof Boolean ? value.equals(true) ? "1" : "0" :
								value.toString();
	}

	public void terminate() {
		if (isExportNull()) return;
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public File file() {
		return file;
	}

	interface Formatter {
		String format(Object object);
	}
}
