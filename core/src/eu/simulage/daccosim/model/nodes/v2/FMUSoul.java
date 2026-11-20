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

import eu.simulage.daccosim.DaccosimException;
import eu.simulage.daccosim.model.Input;
import eu.simulage.daccosim.model.Output;
import eu.simulage.daccosim.model.Variable;
import eu.simulage.daccosim.model.variables.FMUVariable;
import org.javafmi.modeldescription.ScalarVariable;

import javax.jms.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Stream;

import static eu.simulage.daccosim.DataType.Real;
import static eu.simulage.daccosim.GraphExecutor.logger;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class FMUSoul extends FMULocal implements FMUJMS {

	private static final String[] producers = {VARIABLE_TYPES_REPLY, LOAD_FINISHED, INIT_FINISHED, DO_STEP_FINISHED, TERMINATE_FINISHED, SAVE_STATE_FINISHED, RESTORE_STATE_FINISHED, PUSH_FINISHED, PULL_REPLY};
	private final Map<String, MessageProducer> producerMap = new HashMap<>();
	private final Session session;

	public FMUSoul(String id, String path, Session session) {
		super(id, path);
		this.session = session;
		createProducers();
		createConsumers();
	}

	private void createConsumers() {
		setListenerTo(consumerOf(VARIABLE_TYPES), m -> publish(VARIABLE_TYPES_REPLY, variableTypes()));
		setListenerTo(consumerOf(LOAD), m -> {
			load();
			publish(LOAD_FINISHED, emptyMessage());
		});
		setListenerTo(consumerOf(INIT), m -> {
			init();
			publish(INIT_FINISHED, emptyMessage());
		});
		setListenerTo(consumerOf(DO_STEP), m -> {
			step(Double.parseDouble(textFrom(m)));
			publish(DO_STEP_FINISHED, emptyMessage());
		});
		setListenerTo(consumerOf(SAVE_STATE), m -> {
			step(Double.parseDouble(textFrom(m)));
			publish(SAVE_STATE_FINISHED, emptyMessage());
		});
		setListenerTo(consumerOf(RESTORE_STATE), m -> {
			step(Double.parseDouble(textFrom(m)));
			publish(RESTORE_STATE_FINISHED, emptyMessage());
		});
		setListenerTo(consumerOf(TERMINATE), m -> {
			terminate();
			publish(TERMINATE_FINISHED, emptyMessage());
			try {
				session.close();
				System.out.println("Terminated " + id());
			} catch (JMSException e) {
				e.printStackTrace();
			}
		});
		setListenerTo(consumerOf(DERIVATIVES), m -> {
			StringBuilder builder = new StringBuilder();
			real(inputs()).forEach(i -> real(outputs())
					.forEach(o -> builder.append(i.id()).append(o.id()).append(":")
							.append(derivativeOf(singletonList(i.id()), singletonList(o.id())).values().iterator().next())));
			real(outputs()).forEach(o -> builder.append(o.id()).append(":").append(derivativeOf(o.id())));
			publish(DERIVATIVES_REPLY, messageFrom(builder.toString()));
		});
		setListenerTo(consumerOf(PUSH), m -> {
			String[] push = textFrom(m).split(":");
			push(push[0], objectMapper.get(variableType(push[0])).fromText(push[1]));
			publish(PUSH_FINISHED, emptyMessage());
		});
		setListenerTo(consumerOf(PULL), m -> publish(PULL_REPLY, messageFrom(pull(textFrom(m)) + "")));
	}

	private Stream<? extends Variable> real(List<? extends Variable> connectors) {
		return connectors.stream().filter(c -> variableType(c.id()).equals(Real.toString()));
	}

	private Message variableTypes() {
		StringBuilder builder = new StringBuilder();
		for (ScalarVariable variable : simulation().getModelDescription().getModelVariables())
			builder.append(variable.getName()).append(":").append(variable.getTypeName()).append("\n");
		return messageFrom(builder.toString());
	}

	private void setListenerTo(MessageConsumer messageConsumer, MessageHandler handler) {
		try {
			messageConsumer.setMessageListener(m -> {
				try {
					handler.handle(m);
				} catch (Throwable t) {
					logger().log(Level.SEVERE, t.getMessage(), t);
				}
			});
		} catch (JMSException e) {
			throw new DaccosimException(e.getMessage());
		}
	}

	private void createProducers() {
		stream(producers).forEach(c -> producerMap.put(c, producerOf(c)));
	}

	@Override
	public String topicPrefix() {
		return id() + ".";
	}

	@Override
	public Map<String, MessageConsumer> consumerMap() {
		return null;
	}

	@Override
	public Map<String, MessageProducer> producerMap() {
		return producerMap;
	}

	@Override
	public Session session() {
		return session;
	}

	public FMUSoul outputs(Output... outputs) {
		super.outputs(outputs);
		return this;
	}

	public FMUSoul inputs(Input... inputs) {
		super.inputs(inputs);
		return this;
	}

	public FMUSoul variables(Variable... variable) {
		super.variables(stream(variable).map(v -> new FMUVariable(v.id(), v.type(), this, v.variability())).collect(toList()));
		return this;
	}

	interface MessageHandler {
		void handle(Message message);
	}

}
