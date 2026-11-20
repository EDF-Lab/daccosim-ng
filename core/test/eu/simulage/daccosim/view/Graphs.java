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

import static eu.simulage.daccosim.DataType.Boolean;
import static eu.simulage.daccosim.DataType.Integer;
import static eu.simulage.daccosim.view.Helper.FMU;
import static eu.simulage.daccosim.view.Helper.*;
import static java.util.stream.IntStream.range;

@SuppressWarnings("SpellCheckingInspection")
public class Graphs {

    public static eu.simulage.daccosim.view.Graph environmentGraph() {
        return graph().
                nodes(
                        FMU("Environment", fmuPath("Environment")).outputs(output("temperature")),
                        FMU("Building", fmuPath("Building")).inputs(input("externalTemperature")),
                        adderOperator("Adder").inputs(input("input1"), input("input2")),
		                externalInput("External").outputs(Helper.output("output", DataType.Real, "continuous", 10.0))).
                arrows(
                        arrow("Environment.temperature", "Adder.input1"),
                        arrow("External.output", "Adder.input2"),
                        arrow("Adder.output", "Building.externalTemperature"));
    }

    public static Graph equationPairGraph() {
        return graph().
                nodes(
                        FMU("Equation1", fmuPath("equation1win3264")).outputs(output("x2")).inputs(input("x1")),
                        FMU("Equation2", fmuPath("equation2win3264")).inputs(input("x2")).outputs(output("x1"))).
                arrows(
                        arrow("Equation1.x2", "Equation2.x2"),
                        arrow("Equation2.x1", "Equation1.x1"));
    }

	static Output output(String varName) {
		return Helper.output(varName, DataType.Real, "continuous");
	}

	public static Graph equationPairGraphForJP() {
        return graph().
                nodes(
                        FMU("Equation1", fmuPath("equation1")).outputs(output("x2")).inputs(input("x1")),
                        FMU("Equation2", fmuPath("equation2")).inputs(input("x2")).outputs(output("x1"))).
                arrows(
                        arrow("Equation1.x2", "Equation2.x2"),
                        arrow("Equation2.x1", "Equation1.x1"));
    }

	static Input input(String name) {
		return Helper.input(name, DataType.Real, "continuous");
	}

	public static Graph tripleEquationGraph() {
        return graph().
                nodes(
                        FMU("Plane1", fmuPath("plane1win3264")).outputs(output("z")).inputs(input("x"), input("y")),
                        FMU("Plane2", fmuPath("plane2win3264")).outputs(output("y")).inputs(input("x"), input("z")),
                        FMU("Plane3", fmuPath("plane3win3264")).outputs(output("x")).inputs(input("y"), input("z"))).
                arrows(
                        arrow("Plane1.z", "Plane2.z"),
                        arrow("Plane1.z", "Plane3.z"),
                        arrow("Plane2.y", "Plane1.y"),
                        arrow("Plane2.y", "Plane3.y"),
                        arrow("Plane3.x", "Plane1.x"),
                        arrow("Plane3.x", "Plane2.x"));
    }

    public static Graph loopGraph() {
        return graph().
                nodes(
                        FMU("Source", fmuPath("Source")).outputs(output("y")),
                        FMU("NewtonTester", fmuPath("NewtonTester")).outputs(output("y1"), output("y2")).inputs(input("x1"), input("x2")),
                        FMU("SeqProp1", fmuPath("Coeff")).inputs(input("x")).outputs(output("y")),
                        FMU("SeqProp2", fmuPath("Coeff")).inputs(input("x")).outputs(output("y"))).
                arrows(
                        arrow("Source.y", "NewtonTester.x1"),
                        arrow("NewtonTester.y2", "SeqProp2.x"),
                        arrow("NewtonTester.y1", "SeqProp1.x"),
                        arrow("SeqProp1.y", "NewtonTester.x2"));
    }

    public static Graph loopAndSequGraph() {
        return graph().
                nodes(
                        FMU("Source", fmuPath("Source")).outputs(output("y")),
                        FMU("NewtonTester1", fmuPath("NewtonTester")).outputs(output("y1"), output("y2")).inputs(input("x1"), input("x2")),
                        FMU("SeqProp1", fmuPath("Coeff")).inputs(input("x")).outputs(output("y")),
                        FMU("SeqProp2", fmuPath("Coeff")).inputs(input("x")).outputs(output("y")),
                        FMU("NewtonTester2", fmuPath("NewtonTester")).outputs(output("y1"), output("y2")).inputs(input("x1"), input("x2")),
                        FMU("SeqProp3", fmuPath("Coeff")).inputs(input("x")).outputs(output("y"))).
                arrows(
                        arrow("SeqProp3.y", "NewtonTester1.x2"),
                        arrow("NewtonTester2.y2", "NewtonTester1.x1"),
                        arrow("NewtonTester1.y2", "SeqProp1.x"),
                        arrow("NewtonTester2.y1", "SeqProp2.x"),
                        arrow("Source.y", "NewtonTester2.x1"),
                        arrow("SeqProp2.y", "NewtonTester2.x2"),
                        arrow("Source.y", "SeqProp3.x"));
    }

    public static Graph treeGraph() {
        return graph().
                nodes(
                        FMU("Source", fmuPath("Source")).outputs(output("y")),
                        FMU("SeqProp1", fmuPath("SeqProp")).inputs(input("x")).outputs(output("y")),
                        FMU("SeqProp2", fmuPath("SeqProp")).inputs(input("x")).outputs(output("y")),
                        FMU("SeqProp3", fmuPath("SeqProp")).inputs(input("x")).outputs(output("y")),
                        FMU("SeqProp4", fmuPath("SeqProp")).inputs(input("x")).outputs(output("y")),
                        FMU("SeqProp5", fmuPath("SeqProp")).inputs(input("x")).outputs(output("y")),
                        FMU("SeqProp6", fmuPath("SeqProp")).inputs(input("x")).outputs(output("y")),
                        FMU("SeqProp7", fmuPath("SeqProp")).inputs(input("x")).outputs(output("y")),
                        FMU("SeqProp8", fmuPath("SeqProp")).inputs(input("x")).outputs(output("y")),
                        adderOperator("Adder").inputs(input("input0"), input("input1"), input("input2"), input("input3"))).
                arrows(
                        arrow("Source.y", "SeqProp1.x"),
                        arrow("SeqProp1.y", "SeqProp3.x"),
                        arrow("SeqProp1.y", "SeqProp2.x"),
                        arrow("SeqProp3.y", "SeqProp6.x"),
                        arrow("SeqProp3.y", "SeqProp7.x"),
                        arrow("SeqProp7.y", "Adder.input3"),
                        arrow("SeqProp6.y", "Adder.input2"),
                        arrow("SeqProp2.y", "SeqProp5.x"),
                        arrow("SeqProp2.y", "SeqProp4.x"),
                        arrow("SeqProp5.y", "Adder.input1"),
                        arrow("SeqProp4.y", "Adder.input0"),
                        arrow("Adder.output", "SeqProp8.x"));
    }

    public static Graph twoParallelLoopsGraph() {
        return graph().
                nodes(
                        FMU("Source", fmuPath("Source")).outputs(output("y")),
                        FMU("NewtonTester1", fmuPath("NewtonTester")).outputs(output("y1"), output("y2")).inputs(input("x1"), input("x2")),
                        FMU("NewtonTester2", fmuPath("NewtonTester")).outputs(output("y1"), output("y2")).inputs(input("x1"), input("x2")),
                        FMU("NewtonTester3", fmuPath("NewtonTester")).outputs(output("y1"), output("y2")).inputs(input("x1"), input("x2")),
                        FMU("SeqProp1", fmuPath("Coeff")).inputs(input("x")).outputs(output("y")),
                        FMU("SeqProp2", fmuPath("Coeff")).inputs(input("x")).outputs(output("y"))).
                arrows(
                        arrow("Source.y", "NewtonTester1.x1"),
                        arrow("Source.y", "NewtonTester2.x1"),
                        arrow("SeqProp2.y", "NewtonTester2.x2"),
                        arrow("NewtonTester1.y1", "SeqProp1.x"),
                        arrow("SeqProp1.y", "NewtonTester1.x2"),
                        arrow("NewtonTester2.y1", "SeqProp2.x"),
                        arrow("NewtonTester2.y2", "NewtonTester3.x1"),
                        arrow("NewtonTester1.y2", "NewtonTester3.x2"));
    }

    public static Graph twoParallelLoopsAndOutputLoopGraph() {
        return graph().
                nodes(
                        FMU("Source", fmuPath("Source")).outputs(output("y")),
                        FMU("NewtonTester1", fmuPath("NewtonTester")).outputs(output("y1"), output("y2")).inputs(input("x1"), input("x2")),
                        FMU("NewtonTester2", fmuPath("NewtonTester")).outputs(output("y1"), output("y2")).inputs(input("x1"), input("x2")),
                        FMU("NewtonTester3", fmuPath("NewtonTester")).outputs(output("y1"), output("y2")).inputs(input("x1"), input("x2")),
                        FMU("SeqProp1", fmuPath("Coeff")).inputs(input("x")).outputs(output("y")),
                        FMU("SeqProp2", fmuPath("Coeff")).inputs(input("x")).outputs(output("y")),
                        FMU("SeqProp3", fmuPath("Coeff")).inputs(input("x")).outputs(output("y")),
                        FMU("SeqProp4", fmuPath("SeqProp")).inputs(input("x")).outputs(output("y")),
                        adderOperator("Adder").inputs(input("input0"), input("input1"), input("input2"), input("input3"))).
                arrows(
                        arrow("Source.y", "NewtonTester1.x1"),
                        arrow("Source.y", "NewtonTester2.x1"),
                        arrow("SeqProp2.y", "NewtonTester2.x2"),
                        arrow("NewtonTester1.y1", "SeqProp1.x"),
                        arrow("SeqProp1.y", "NewtonTester1.x2"),
                        arrow("NewtonTester2.y1", "SeqProp2.x"),
                        arrow("NewtonTester1.y2", "Adder.input0"),
                        arrow("NewtonTester2.y2", "Adder.input1"),
                        arrow("Adder.output", "NewtonTester3.x1"),
                        arrow("NewtonTester3.y2", "SeqProp3.x"),
                        arrow("SeqProp3.y", "NewtonTester3.x2"),
                        arrow("NewtonTester3.y1", "SeqProp4.x"));
    }

    public static Graph twoSerialLoopsGraph() {
        return graph().
                nodes(
                        FMU("Source", fmuPath("Source")).outputs(output("y")),
                        FMU("NewtonTester1", fmuPath("NewtonTester")).outputs(output("y1"), output("y2")).inputs(input("x1"), input("x2")),
                        FMU("NewtonTester2", fmuPath("NewtonTester")).outputs(output("y1"), output("y2")).inputs(input("x1"), input("x2")),
                        FMU("SeqProp1", fmuPath("Coeff")).inputs(input("x"), input("k")).outputs(output("y")),
                        FMU("SeqProp2", fmuPath("Coeff")).inputs(input("x")).outputs(output("y")),
                        FMU("SeqProp3", fmuPath("SeqProp")).inputs(input("x")).outputs(output("y"))).
                arrows(
                        arrow("Source.y", "NewtonTester2.x1"),
                        arrow("SeqProp2.y", "NewtonTester2.x2"),
                        arrow("NewtonTester1.y1", "SeqProp1.x"),
                        arrow("SeqProp1.y", "NewtonTester1.x2"),
                        arrow("NewtonTester2.y1", "SeqProp2.x"),
                        arrow("NewtonTester2.y2", "NewtonTester1.x1"),
                        arrow("NewtonTester1.y2", "SeqProp3.x"));
    }

    public static Graph twoEquationsGraph() {
        return graph().
                nodes(
                        FMU("x1minusx2", fmuPath("x1minusx2win3264")).outputs(output("x2")).inputs(input("x1")),
                        FMU("eightdivx2", fmuPath("eightdivx2win3264")).outputs(output("x1")).inputs(input("x2"))).
                arrows(
                        arrow("x1minusx2.x2", "eightdivx2.x2"),
                        arrow("eightdivx2.x1", "x1minusx2.x1"));
    }


    public static Graph tankYPipeGraph() {
        return graph().
                nodes(
                        FMU("Tank1", fmuPath("tankwin3264")).outputs(output("outputRate"), output("level")).inputs(input("inputRate")),
                        FMU("Tank2", fmuPath("tankwin3264")).outputs(output("outputRate"), output("level")).inputs(input("inputRate")),
                        FMU("Tank3", fmuPath("tankwin3264")).outputs(output("outputRate"), output("level")).inputs(input("inputRate")),
                        FMU("YPipe", fmuPath("ypipewin3264")).outputs(output("outputRate")).inputs(input("inputRate1"), input("inputRate2")),
		                externalInput("inputRate1").outputs(Helper.output("output", DataType.Real, "continuous", 0.5)),
		                externalInput("inputRate2").outputs(Helper.output("output", DataType.Real, "continuous", 0.1)),
                        externalOutput("outputRate").inputs(input("input"))).
                arrows(
                        arrow("Tank1.outputRate", "YPipe.inputRate1"),
                        arrow("Tank2.outputRate", "YPipe.inputRate2"),
                        arrow("YPipe.outputRate", "Tank3.inputRate"),
                        arrow("inputRate1.output", "Tank1.inputRate"),
                        arrow("inputRate2.output", "Tank2.inputRate"),
                        arrow("Tank3.outputRate", "outputRate.input"));
    }

    public static Graph eventsGraph() {
        return graph().
                nodes(
                        FMU("M1", fmuPath("M1win3264")).outputs(output("y")),
                        FMU("M2", fmuPath("M2win3264")).outputs(output("y")),
                        FMU("M3", fmuPath("M3win3264")).outputs(output("y")));
    }

    public static Graph tankBarrelGraph() {
        return graph().
                nodes(
		                FMU("Tank", fmuPath("TankGwin3264")).outputs(output("outFlow")).inputs(Helper.input("Valve", Boolean, "discrete")),
		                FMU("Barrel", fmuPath("BarrelGwin3264")).outputs(output("Water")).inputs(input("inFlow"), Helper.input("Size", Integer, "discrete"), Helper.input("Abort", Boolean, "discrete")),
		                FMU("C1", fmuPath("C1win3264")).outputs(Helper.output("Valve", Boolean, "discrete")),
		                FMU("C2", fmuPath("C2win3264")).outputs(Helper.output("Abort", Boolean, "discrete"), Helper.output("Size", Integer, "discrete")),
                        externalOutput("water").inputs(input("input"))).
				arrows(
						arrow("C1.Valve", "Tank.Valve"),
						arrow("C2.Abort", "Barrel.Abort"),
						arrow("C2.Size", "Barrel.Size"),
						arrow("Tank.outFlow", "Barrel.inFlow"),
						arrow("Barrel.Water", "water.input")
				);
    }

    public static eu.simulage.daccosim.view.Graph heavyUseCase() {
        return graph()
                .nodes(
                        FMU("gene1toN", fmuPath("gene1toNwin3264"))
                                .outputs(range(1, 20001).boxed().map(i -> output("x[" + i + "]")).toArray(Output[]::new)),
                        FMU("Ndiv10000", fmuPath("Ndiv10000win3264"))
                                .inputs(range(1, 20001).boxed().map(i -> input("x[" + i + "]")).toArray(Input[]::new))
                                .outputs(new Output("y", DataType.Real)))
                .arrows(
                        range(1, 20001).boxed().map(i -> arrow("gene1toN.x[" + i + "]", "Ndiv10000.x[" + i + "]")).toArray(Arrow[]::new));
    }

    public static Graph simpleHeatTransfer() {
        return graph()
                .nodes(
                        FMU("sinus", fmuPath("PackageBuildingCases_ComponentsFMU1_SineFMUwin3264")).outputs(output("Text")),
                        FMU("wall", fmuPath("PackageBuildingCases_ComponentsFMU1_wallFmu1win3264")).outputs(output("Q_a"), output("Q_b")).inputs(input("T_a"), input("T_b")),
                        FMU("zoneA", fmuPath("PackageBuildingCases_ComponentsFMU1_zoneFmu1win3264")).outputs(output("Tin")).inputs(input("Text"), input("Qin")),
                        FMU("zoneB", fmuPath("PackageBuildingCases_ComponentsFMU1_zoneFmu1biswin3264")).outputs(output("Tin")).inputs(input("Text"), input("Qin")))
                .arrows(
                        arrow("sinus.Text", "zoneA.Text"),
                        arrow("sinus.Text", "zoneB.Text"),
                        arrow("zoneA.Tin", "wall.T_a"),
                        arrow("zoneB.Tin", "wall.T_b"),
                        arrow("wall.Q_a", "zoneA.Qin"),
                        arrow("wall.Q_b", "zoneB.Qin"));
    }

    public static Graph coInitUseCaseWithAFmu() {
        return graph()
                .nodes(
                        FMU("a", fmuPath("A")).inputs(input("x")).outputs(output("y")),
		                externalInput("ei").outputs(Helper.output("output", DataType.Real, "continuous", 5.0)),
                        externalOutput("eo").inputs(input("input").value(0.0)))
                .arrows(
                        arrow("ei.output", "a.x"),
                        arrow("a.y", "eo.input"));
    }

    public static Graph coInitUseCaseWithBFmu() {
        return graph()
                .nodes(
                        FMU("b", fmuPath("B")).inputs(input("x")).outputs(output("y")),
		                externalInput("ei").outputs(Helper.output("output", DataType.Real, "continuous", 5.0)),
                        externalOutput("eo").inputs(input("input")))
                .arrows(
                        arrow("ei.output", "b.x"),
                        arrow("b.y", "eo.input"));
    }

    public static String fmuPath(String fmuName) {
        return Graphs.class.getResource("/" + fmuName + ".fmu").getFile();
    }

    private static ExternalInput externalInput(String id) {
        return new ExternalInput(id);
    }

}
