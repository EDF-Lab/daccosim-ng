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

package eu.simulage.daccosim.model.nodes.v2;

import eu.simulage.daccosim.model.GraphNode;
import eu.simulage.daccosim.model.Input;
import eu.simulage.daccosim.model.Output;
import eu.simulage.daccosim.model.Variable;
import eu.simulage.daccosim.model.inputs.FMUInput;
import eu.simulage.daccosim.model.outputs.FMUOutput;
import eu.simulage.daccosim.model.variables.FMUVariable;
import org.javafmi.modeldescription.v2.ModelDescription;

import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.javafmi.modeldescription.v2.Capabilities.ProvidesDirectionalDerivative;

public class FMUStub extends GraphNode implements FMUJMS {

    private static final String[] producers = {VARIABLE_TYPES, LOAD, INIT, DO_STEP, TERMINATE, SAVE_STATE, RESTORE_STATE, PUSH, PULL};
    private static final String[] consumers = {VARIABLE_TYPES_REPLY, LOAD_FINISHED, INIT_FINISHED, DO_STEP_FINISHED, TERMINATE_FINISHED, SAVE_STATE_FINISHED, RESTORE_STATE_FINISHED, PUSH_FINISHED, PULL_REPLY};
    private final Map<String, MessageConsumer> consumerMap = new HashMap<>();
    private final Map<String, MessageProducer> producerMap = new HashMap<>();
    private final Session session;
    private Map<String, Double> derivativesMap = new HashMap<>();
    private final Map<String, Object> variablesMap = new HashMap<>();
    private Map<String, String> variableTypes;

    public FMUStub(String id, Session session) {
        super(id);
        this.session = session;
        stream(consumers).forEach(c -> consumerMap.put(c, consumerOf(c)));
        stream(producers).forEach(c -> producerMap.put(c, producerOf(c)));
    }

    private String variableType(String id) {
        if (variableTypes != null) return variableTypes.get(id);
        publish(VARIABLE_TYPES, emptyMessage());
        variableTypes = stream(textFrom(wait(VARIABLE_TYPES_REPLY)).split("\n"))
                .collect(Collectors.toMap(v -> v.split(":")[0], v -> v.split(":")[1]));
        return variableTypes.get(id);
    }

    @Override
    public void load() {
        Message wait = null;
        while (wait == null) {
            publish(LOAD, emptyMessage());
            wait = wait(LOAD_FINISHED, 100);
        }
    }

    @Override
    public void init() {
        publish(INIT, emptyMessage());
        wait(INIT_FINISHED);
    }

    @Override
    public void step(double stepSize) {
        clearCache();
        publish(DO_STEP, messageFrom(stepSize + ""));
        wait(DO_STEP_FINISHED);
    }

    @Override
    public void terminate() {
        publish(TERMINATE, emptyMessage());
        wait(TERMINATE_FINISHED);
    }

    @Override
    public Map<String, Double> derivativeOf(List<String> input, List<String> output) {
        fillDerivatives();
        return derivativesMap;
    }

    @Override
    public double derivativeOf(String output) {
        if (derivativesMap.isEmpty()) fillDerivatives();
        return derivativesMap.containsKey(output) ? derivativesMap.get(output) : 0;
    }

    private void fillDerivatives() {
        publish(DERIVATIVES, emptyMessage());
        derivativesMap = toMap(wait(DERIVATIVES_REPLY));
    }

    public void saveState() {
        publish(SAVE_STATE, emptyMessage());
        wait(SAVE_STATE_FINISHED);
    }

    public void restoreState() {
        publish(RESTORE_STATE, emptyMessage());
        wait(RESTORE_STATE_FINISHED);
    }

    @Override
    public void reset() {
        publish(RESET, emptyMessage());
        wait(RESET_FINISHED);
    }

    public void push(String id, Object value) {
        clearCache();
        publish(PUSH, messageFrom(id + ":" + value));
        wait(PUSH_FINISHED);
    }

    public Object pull(String id) {
        if(variablesMap.containsKey(id)) return variablesMap.get(id);
        publish(PULL, messageFrom(id));
        Object value = objectMapper.get(variableType(id)).fromText(textFrom(wait(PULL_REPLY)));
        variablesMap.put(id, value);
        return value;
    }

    @Override
    public void transmitDerivative(String input, double value) {
        throw new UnsupportedOperationException();
    }

	@Override
	public List<String> flowVariables() {
		throw new UnsupportedOperationException("Not implemented yet");
	}

    @Override
    public boolean allowDerivativeTransmission(String variableName) {
        return false;
    }

    @Override
    public Double transmissionDerivative(String variableName) {
        return null;
    }

    @Override
    public boolean static_() {
        return false;
    }

    private void clearCache() {
        derivativesMap.clear();
        variablesMap.clear();
    }

    public FMUStub outputs(Output... outputs) {
	    super.outputs(stream(outputs).map(o -> new FMUOutput(o.id(), o.type(), this, o.variability())).collect(toList()));
        return this;
    }

    public FMUStub inputs(Input... inputs) {
	    super.inputs(stream(inputs).map(i -> new FMUInput(i.id(), i.type(), this, i.variability())).collect(toList()));
        return this;
    }

    public FMUStub variables(Variable... variable) {
	    super.variables(stream(variable).map(v -> new FMUVariable(v.id(), v.type(), this, v.variability())).collect(toList()));
        return this;
    }

	@SuppressWarnings({"SameReturnValue", "unused"})
    private Map<String, Double> toMap(Message derivativesCalculated) {
        // TODO
        return null;
    }

    @Override
    public boolean canProvideDirectionalDerivatives() {
        // TODO
        return false;
    }

    @Override
    public String topicPrefix() {
        return id() + ".";
    }

    @Override
    public Map<String, MessageConsumer> consumerMap() {
        return consumerMap;
    }

    @Override
    public Map<String, MessageProducer> producerMap() {
        return producerMap;
    }

    @Override
    public Session session() {
        return session;
    }
}
