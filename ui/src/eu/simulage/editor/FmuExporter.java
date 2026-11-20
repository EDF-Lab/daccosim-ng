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

package eu.simulage.editor;

import eu.simulage.MxGraphToDsg;
import eu.simulage.daccosim.model.Arrow;
import eu.simulage.daccosim.model.GraphNode;
import eu.simulage.daccosim.model.Input;
import eu.simulage.daccosim.model.Variable;
import eu.simulage.daccosim.model.nodes.ExternalInput;
import eu.simulage.daccosim.model.nodes.ExternalOutput;
import eu.simulage.daccosim.model.nodes.v2.FMULocal;
import eu.simulage.daccosim.view.FMU;
import eu.simulage.daccosim.view.Graph;
import org.apache.commons.io.IOUtils;
import org.javafmi.builder.Platform;
import org.javafmi.builder.ShellFmuBuilder;
import org.javafmi.framework.FmiContainer;
import org.javafmi.modeldescription.ModelDescriptionDeserializer;
import org.javafmi.modeldescription.v2.ModelDescription;
import org.javafmi.modeldescription.v2.ModelStructure;
import org.javafmi.modeldescription.v2.ScalarVariable;
import org.javafmi.modeldescription.v2.Unknown;
import siani.javafmi.Matryoshka;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.simulage.daccosim.GraphExecutor.logger;
import static eu.simulage.daccosim.Stepper.Method.ConstantStep;
import static eu.simulage.editor.EditorUtils.daccosimJarBytes;
import static eu.simulage.editor.EditorUtils.getEditor;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class FmuExporter {
    private final File selectedFile;
    private final List<Platform> platforms;
    private final Graph graph;
    private final String[] fmuFiles;
    private final Map<String, ModelDescription> modelDescriptionMap = new HashMap<>();

    private FmuExporter(File selectedFile) {
        this.selectedFile = selectedFile;
        this.graph = MxGraphToDsg.toDaccosimGraph(EditorUtils.getEditor());
        this.fmuFiles = fmuFiles();
        this.platforms = platforms();
    }

    public static void exportFmu(File selectedFile) throws IOException {
        logger().info("Exporting fmu");
        new FmuExporter(selectedFile).export();
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static void modifyMatryoshkaJar(File jarFile, File dsgFile, File initFile) {
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        try (FileSystem zipfs = FileSystems.newFileSystem(URI.create("jar:" + jarFile.toURI().toString()), env)) {
            addFile(dsgFile, zipfs);
            addFile(initFile, zipfs);
            removeActiveMq(zipfs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addFile(File newFile, FileSystem zipfs) throws IOException {
        Path pathInZipfile = zipfs.getPath("/" + newFile.getName());
        Files.copy(newFile.toPath(), pathInZipfile, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void removeActiveMq(FileSystem zipfs) {
        try {
            Path pathInZipfile = zipfs.getPath("/org/apache/activemq");
            Files.walk(pathInZipfile)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void moveFile(FileSystem zipFs, String first, String second) {
        try {
            Files.move(zipFs.getPath(first), zipFs.getPath(second));
        } catch (IOException ignored) {
        }
    }

    private static String filenameNoExtension(File selectedFile) {
        return selectedFile.getName().replace(".fmu", "");
    }

    private static InputStream modelDescriptionStream(File fmuFile) {
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        try (FileSystem zipFs = FileSystems.newFileSystem(URI.create("jar:" + fmuFile.toURI().toString()), env)) {
            Path pathInZipFile = zipFs.getPath("/modelDescription.xml");
            byte[] bytes = IOUtils.toByteArray(Files.newInputStream(pathInZipFile));
            return new ByteArrayInputStream(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<Platform> platforms() {
        AtomicBoolean addWindows = new AtomicBoolean(true);
        AtomicBoolean addLinux = new AtomicBoolean(true);
        stream(fmuFiles).forEach(f -> {
            Map<String, String> env = new HashMap<>();
            env.put("create", "true");
            try (FileSystem zipFs = FileSystems.newFileSystem(URI.create("jar:" + new File(f).toURI().toString()), env)) {
                if (!Files.exists(zipFs.getPath("/binaries/win64"))) addWindows.set(false);
                if (!Files.exists(zipFs.getPath("/binaries/linux64"))) addLinux.set(false);
            } catch (IOException e) {
                logger().severe(e.getMessage());
            }
        });
        return Stream.of(addWindows.get() ? Platform.Win64 : null, addLinux.get() ? Platform.Linux64 : null)
                .filter(Objects::nonNull).collect(toList());
    }

    private void modifyFmu(File fmuFile, File selectedFile) {
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        try {
            try (FileSystem zipFs = FileSystems.newFileSystem(URI.create("jar:" + fmuFile.toURI().toString()), env)) {
                modifyModelDescription(selectedFile, zipFs);
                modifyBinaries(selectedFile, zipFs);
            }
            Files.move(fmuFile.toPath(), selectedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void modifyBinaries(File selectedFile, FileSystem zipFs) {
        moveFile(zipFs, "/binaries/win64/Matryoshka.dll", "/binaries/win64/Matryoshka_" + filenameNoExtension(selectedFile) + ".dll");
        moveFile(zipFs, "/binaries/linux64/Matryoshka.so", "/binaries/linux64/Matryoshka_" + filenameNoExtension(selectedFile) + ".so");
    }

    private void modifyModelDescription(File selectedFile, FileSystem zipFs) throws IOException {
        Path pathInZipFile = zipFs.getPath("/modelDescription.xml");
        String content = new String(Files.readAllBytes(pathInZipFile));
//		content = content.replaceAll("modelName=\"[^\"]*\"", "modelName=\"" + filenameNoExtension(selectedFile) + "\"");
//        content = content.replaceAll("generationTool=\"[^\"]*\"", "generationTool=\"SIANI FMU Builder v2.6.1\"");
        content = content.replaceAll("modelIdentifier=\"[^\"]*\"", "modelIdentifier=\"Matryoshka_" + filenameNoExtension(selectedFile) + "\"");
        content = content.replace("canHandleMultipleSetPerTimeInstant=\"true\"", "");
        content = content.replace("initial=\"calculated\"", "");
        content = content.replaceAll("previous=\"[^\"]*\"", "");
        Files.write(pathInZipFile, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void export() throws IOException {
        File tempFolder = temporalFolder();
        File dsgFile = writeDsgFile(tempFolder);
        File initFile = writeInitFile(tempFolder);
        File jarFile = prepareMatryoshkaJar(tempFolder, dsgFile, initFile);
        logger().info("Building fmu");
        File fmuFile = new ShellFmuBuilder(jarFile.getAbsolutePath())
                .fmuName(Matryoshka.class.getSimpleName())
                .platforms(platforms)
                .includes(fmuFiles)
                .build();
        addModelStructure(fmuFile);
        modifyFmu(fmuFile, selectedFile);
        logger().info("Fmu exported: " + selectedFile.getAbsolutePath());
    }

    private File prepareMatryoshkaJar(File tempFolder, File modelFile, File initFile) throws IOException {
        File jarFile = new File(tempFolder, Matryoshka.class.getSimpleName() + ".jar");
        Files.write(jarFile.toPath(), daccosimJarBytes());
        modifyMatryoshkaJar(jarFile, modelFile, initFile);
        return jarFile;
    }

    private File writeDsgFile(File tempFolder) throws IOException {
        modifyGraph();
        File modelFile = new File(tempFolder, "res/model.dsg");
        //noinspection ResultOfMethodCallIgnored
        modelFile.getParentFile().mkdirs();
        Files.write(modelFile.toPath(), graph.serialize());
        return modelFile;
    }

    private File writeInitFile(File tempFolder) throws IOException {
        File initFile = new File(tempFolder, "res/.init");
        StringBuilder builder = new StringBuilder();
        graph.commit().nodes().stream()
                .filter(n -> n instanceof FMULocal)
                .forEach(n -> {
                    for (ScalarVariable modelVariable : getModelDescription((FMULocal) n).getModelVariables()) {
                        if (!modelVariable.hasStartValue()) continue;
                        builder.append(n.id()).append(".").append(modelVariable.getName()).append(";").append(modelVariable.getStart()).append("\n");
                    }
                });
        Files.write(initFile.toPath(), builder.toString().getBytes());
        return initFile;
    }

    private File temporalFolder() throws IOException {
        File tempFolder = Files.createTempDirectory("matryoshka-res").toFile();
        tempFolder.deleteOnExit();
        return tempFolder;
    }

    private void addModelStructure(File fmuFile) {
        ModelStructure modelStructure = new ModelStructureCalculator(graph).execute();
        StringBuilder builder = new StringBuilder("<ModelStructure>\n\t\t<Outputs>\n");
        modelStructure.getOutputs().forEach(o -> builder.append("\t\t\t<Unknown index=\"").append(o.getIndex()).append("\" dependencies=\"").append(o.getDependencies()).append("\"/>\n"));
        String structureString = builder.append("\t\t</Outputs>\n\t</ModelStructure>\n").toString();
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        try (FileSystem zipFs = FileSystems.newFileSystem(URI.create("jar:" + fmuFile.toURI().toString()), env)) {
            Path pathInZipFile = zipFs.getPath("/modelDescription.xml");
            String content = new String(Files.readAllBytes(pathInZipFile));
            int toRemoveStart = content.indexOf("<ModelStructure>");
            int toRemoveEnd = content.indexOf("</fmiModelDescription>");
            content = content.substring(0, toRemoveStart) + structureString + content.substring(toRemoveEnd);
            Files.write(pathInZipFile, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void modifyGraph() {
        graph.nodes().stream()
                .filter(n -> n instanceof FMU)
                .map(n -> (FMU) n)
                .forEach(n -> n.path(new File(n.path()).getName()));
        graph.settings().stepper(ConstantStep);
        graph.settings().stopTime(Double.MAX_VALUE);
        graph.settings().startTime(0);
        graph.clearExport();
    }

    private String[] fmuFiles() {
        return graph.nodes().stream()
                .filter(n -> n instanceof FMU)
                .map(n -> getEditor().getFile().fmuFile(new File(((FMU) n).path()).getName()).getAbsolutePath()).toArray(String[]::new);
    }

    private ModelDescription getModelDescription(FMULocal parent) {
        String name = new File(parent.path()).getName();
        if (!modelDescriptionMap.containsKey(name))
            modelDescriptionMap.put(name, ModelDescriptionDeserializer.deserialize(modelDescriptionStream(getEditor().getFile().fmuFile(name))).asV2());
        return modelDescriptionMap.get(name);
    }

    class ModelStructureCalculator {
        private eu.simulage.daccosim.model.Graph graph;
        private Map<Integer, String> variablesMap = new HashMap<>();
        private Map<ModelDescription, Map<String, Integer>> indexMap = new HashMap<>();

        ModelStructureCalculator(Graph graph) {
            this.graph = graph.commit();
            this.graph.nodes().forEach(n -> {
                if (n instanceof ExternalInput)
                    n.outputs().forEach(o -> variablesMap.put(variablesMap.size() + 1, o.path()));
                else if (n instanceof ExternalOutput)
                    n.inputs().forEach(i -> variablesMap.put(variablesMap.size() + 1, i.path()));
                else allVariables(n).forEach(v -> variablesMap.put(variablesMap.size() + 1, v.path()));
            });
            this.graph.nodes().stream()
                    .filter(n -> n instanceof FMULocal)
                    .map(n -> (FMULocal) n)
                    .forEach(n -> {
                        ModelDescription description = getModelDescription(n);
                        indexMap.put(description, new HashMap<>());
                        ScalarVariable[] modelVariables = description.getModelVariables();
                        for (int i = 0; i < modelVariables.length; i++)
                            indexMap.get(description).put(modelVariables[i].getName(), i + 1);
                    });
        }

        private List<Variable> allVariables(GraphNode n) {
            List<Variable> result = new ArrayList<>(n.inputs());
            result.addAll(n.variables());
            result.addAll(n.outputs());
            return result;
        }

        ModelStructure execute() {
            ModelStructure structure = new ModelStructure();
            structure.setOutputs(new ArrayList<>());
            graph.nodes().stream().filter(n -> n instanceof ExternalOutput)
                    .forEach(n -> n.inputs().forEach(i -> structure.getOutputs().add(dependenciesOf(i))));
            return structure;
        }

        private Unknown dependenciesOf(Variable output) {
            Set<String> alreadyVisited = new HashSet<>();
            List<String> currentlyVisiting = new ArrayList<>(singletonList(output.path()));
            List<String> result = new ArrayList<>();
            while (!currentlyVisiting.isEmpty()) {
                for (java.lang.String variable : new ArrayList<>(currentlyVisiting)) {
                    if (!alreadyVisited.contains(variable))
                        if (isFromExternalInput(variable)) result.add(variable);
                        else currentlyVisiting.addAll(feedersOf(variable));
                    alreadyVisited.add(variable);
                    currentlyVisiting.remove(variable);
                }
            }
            return serializeDependencies(output, result);
        }

        private Unknown serializeDependencies(Variable output, List<String> result) {
            Unknown unknown = new Unknown();
            unknown.setIndex(valueReference(output.path()));
            unknown.setDependencies(result.stream()
                    .map(s -> valueReference(s) + "")
                    .collect(joining(" ")));
            return unknown;
        }

        private boolean isFromExternalInput(String variable) {
            return graph.variable(variable).parent() instanceof ExternalInput;
        }

        private List<String> feedersOf(String variable) {
            Variable var = graph.variable(variable);
            if (var instanceof Input) {
                Arrow arrow = graph.arrows().stream()
                        .filter(a -> a.to().path().equals(variable))
                        .findFirst().orElse(null);
                return arrow != null ? singletonList(arrow.from().path()) : emptyList();
            }
            if (var.parent() instanceof FMULocal) {
                ModelDescription description = getModelDescription((FMULocal) var.parent());
                int startRef = indexOf(description, var.id());
                if (startRef == -1) return stream(description.getModelVariables())
                        .filter(v -> v.getCausality().equals(FmiContainer.Causality.input.name()))
                        .map(v -> var.parent().id() + "." + v.getName())
                        .collect(toList());
                Set<Integer> alreadyVisited = new HashSet<>();
                List<Integer> currentlyVisiting = new ArrayList<>(singletonList(startRef));
                List<Integer> result = new ArrayList<>();
                while (!currentlyVisiting.isEmpty()) {
                    for (java.lang.Integer index : new ArrayList<>(currentlyVisiting)) {
                        if (!alreadyVisited.contains(index))
                            if (!variableOf(description, index).getCausality().equals("input"))
                                currentlyVisiting.addAll(dependenciesOf(description, index));
                            else result.add(index);
                        alreadyVisited.add(index);
                        currentlyVisiting.remove(index);
                    }
                }
                return result.stream().map(s -> var.parent().id() + "." + description.getModelVariables()[s - 1].getName())
                        .collect(Collectors.toList());
            }
            return var.parent().inputs().stream().map(Variable::path).collect(Collectors.toList());
        }

        private ScalarVariable variableOf(ModelDescription description, Integer index) {
            return description.getModelVariables()[index - 1];
        }

        private List<Integer> dependenciesOf(ModelDescription description, Integer index) {
            ScalarVariable variable = description.getModelVariables()[index - 1];
            ScalarVariable derivative = modelVariable(description, variable);
            Set<Integer> result = new HashSet<>(dependenciesOf(description, variable));
            result.addAll(dependenciesOf(description, derivative));
            return new ArrayList<>(result);
        }

        private ScalarVariable modelVariable(ModelDescription description, ScalarVariable variable) {
            try {
                return description.getModelVariable("der(" + variable.getName() + ")");
            } catch (Exception e) {
                return null;
            }
        }

        private List<Integer> dependenciesOf(ModelDescription description, ScalarVariable variable) {
            if (variable == null) return new ArrayList<>();
            int index = indexOf(description, variable.getName());
            List<Integer> dependencies = dependenciesOf(index, description.getModelStructure().getOutputs());
            dependencies.addAll(dependenciesOf(index, description.getModelStructure().getUnknowns()));
            dependencies.addAll(dependenciesOf(index, description.getModelStructure().getDerivatives()));
            return dependencies;
        }

        private List<Integer> dependenciesOf(int index, List<Unknown> unknowns) {
            if (unknowns == null) return new ArrayList<>();
            return unknowns.stream()
                    .filter(o -> o.getIndex() == index)
                    .filter(o -> o.getDependencies() != null && !o.getDependencies().isEmpty())
                    .map(Unknown::getDependencies)
                    .map(o -> o.split(" "))
                    .flatMap(Arrays::stream)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        }

        private int indexOf(ModelDescription description, String id) {
            return indexMap.get(description).getOrDefault(id, -1);
        }

        private java.lang.Integer valueReference(String variable) {
            return variablesMap.entrySet().stream()
                    .filter(e -> e.getValue().equals(variable))
                    .findFirst().get().getKey();
        }


    }
}
