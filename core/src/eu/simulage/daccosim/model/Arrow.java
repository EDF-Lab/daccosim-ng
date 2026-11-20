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

public class Arrow {

    private Output from;
    private Input to;

    public Arrow(String from, String to) {
        this.from = new Output(from, null);
        this.to = new Input(to, null);
    }

    public Arrow graph(Graph graph) {
        this.from = graph.findOutput(from.id());
        this.to = graph.findInput(to.id());
        return this;
    }

    public GraphNode toNode() {
        return to.parent();
    }

    public GraphNode fromNode() {
        return from.parent();
    }

    public Input to() {
        return to;
    }

    public Output from() {
        return from;
    }
}
