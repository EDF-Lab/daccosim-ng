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

import static eu.simulage.daccosim.view.Helper.FMU;
import static eu.simulage.daccosim.view.Helper.*;

public class DistributedGraphs {

    private static final String url = "tcp://127.0.0.1:61616";
    private static final String user = "daccosim";
    private static final String password = "daccosim";

    public static Graph environmentCoordinatorGraph() {
        return graph()
                .settings().jms(url, user, password, true)
                .nodes(
                        FMU("Environment", fmuPath("Environment")).outputs(Graphs.output("temperature")),
                        FMU("Building", fmuPath("Building")).inputs(Graphs.input("externalTemperature")),
                        adderOperator("Adder").inputs(Graphs.input("input1"), Graphs.input("input2")),
		                externalInput("External").outputs(output("output", DataType.Real, "continuous", 10.0)))
                .arrows(
                        arrow("Environment.temperature", "Adder.input1"),
                        arrow("External.output", "Adder.input2"),
                        arrow("Adder.output", "Building.externalTemperature"));
    }

    public static Graph environmentBuildingGraph() {
        return graph()
                .settings().jms(url, user, password, false)
                .nodes(FMU("Building", fmuPath("Building")).inputs(Graphs.input("externalTemperature")));
    }

    public static Graph environmentEnvironmentGraph() {
        return graph()
                .settings().jms(url, user, password, false)
                .nodes(FMU("Environment", fmuPath("Environment")).outputs(Graphs.output("temperature")));
    }

    private static String fmuPath(String fmuName) {
        return DistributedGraphs.class.getResource("/" + fmuName + ".fmu").getFile();
    }

}
