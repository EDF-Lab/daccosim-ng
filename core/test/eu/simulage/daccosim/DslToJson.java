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

import com.google.gson.GsonBuilder;
import eu.simulage.daccosim.view.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;

import static eu.simulage.daccosim.Stepper.Method.ConstantStep;
import static java.util.Arrays.stream;

@SuppressWarnings("SpellCheckingInspection")
public class DslToJson {

    private static final Graph graph = new Graph();

    public static void main(String[] args) throws IOException, URISyntaxException {
        List<String> lines = Files.readAllLines(new File(DslToJson.class.getResource("/Model442.dsl").toURI()).toPath());
        for (String line : lines) processLine(line);
        graph.settings().stopTime(86400).stepper(ConstantStep).stepSize(60);
	    GsonBuilder builder = new GsonBuilder().registerTypeAdapter(GraphNode.class, new Adapters.GraphNodeAdapter());
        Files.write(new File("heavy442.js").toPath(), builder.create().toJson(graph).getBytes());
    }

    private static void processLine(String line) {
        if (line.contains("addFMU")) addFmu(line);
        else if (line.contains("addLink")) addLink(line);
    }

    private static void addFmu(String line) {
        String[] split = line.split(";");
        graph.nodes().add(new FMU(split[2], filename(split[1])));
    }

    private static void addLink(String line) {
        String[] split = line.split(";");
        FMU from = fmu(split[1]);
        FMU to = fmu(split[2]);
        createOutputs(from, split[3]);
        createInputs(to, split[4]);
        createLinks(from, split[3], to, split[4]);
    }

    private static void createOutputs(FMU from, String outputs) {
        stream(variables(outputs)).forEach(o -> from.outputs().add(new Output(o, DataType.Real)));
    }

    private static void createInputs(FMU to, String inputs) {
        stream(variables(inputs)).forEach(o -> to.inputs().add(new Input(o, DataType.Real)));
    }

    private static void createLinks(FMU from, String outputs, FMU to, String inputs) {
        String[] outputSplit = variables(outputs);
        String[] inputSplit = variables(inputs);
        for (int i = 0; i < outputSplit.length; i++)
            graph.arrows().add(new Arrow(from.id() + "." + outputSplit[i], to.id() + "." + inputSplit[i]));
    }

    private static String[] variables(String outputs) {
        return trim(outputs.replaceAll("[{}]", "").split("\""));
    }

    private static String[] trim(String[] split) {
        return stream(split).filter(s -> !s.isEmpty() && !s.equals(",")).toArray(String[]::new);
    }

    private static FMU fmu(String id) {
        return (FMU) graph.nodes().stream().filter(n -> n.id().equals(id)).findFirst().orElse(null);
    }

    private static String filename(String path) {
        return "/home/oroncal/IdeaProjects/daccosim/core/target/test-classes/"  + path.substring(path.lastIndexOf("/") + 1);
    }

}
