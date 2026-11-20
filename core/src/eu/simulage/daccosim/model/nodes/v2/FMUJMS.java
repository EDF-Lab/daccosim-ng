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
import eu.simulage.daccosim.DataType;
import eu.simulage.daccosim.model.FMUProxy;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

public interface FMUJMS extends FMUProxy {
	String VARIABLE_TYPES = "variable_types";
	String VARIABLE_TYPES_REPLY = "variable_types_reply";
	String LOAD = "load";
	String LOAD_FINISHED = "load_finished";
	String INIT = "init";
	String INIT_FINISHED = "init_finished";
	String DO_STEP = "do_step";
	String DO_STEP_FINISHED = "do_step_finished";
	String TERMINATE = "terminate";
	String TERMINATE_FINISHED = "terminate_finished";
	String SAVE_STATE = "save_state";
	String SAVE_STATE_FINISHED = "save_state_finished";
	String RESTORE_STATE = "restore_state";
	String RESTORE_STATE_FINISHED = "restore_state_finished";
	String RESET = "reset";
	String RESET_FINISHED = "reset_finished";
	String PUSH = "push";
	String PUSH_FINISHED = "push_finished";
	String PULL = "pull";
	String PULL_REPLY = "pull_reply";
	String DERIVATIVES = "derivatives";
	String DERIVATIVES_REPLY = "derivatives_reply";
	Map<String, TextToObjectMapper> objectMapper = new HashMap<String, TextToObjectMapper>() {{
		put(DataType.Real.toString(), Double::parseDouble);
		put(DataType.Integer.toString(), Integer::parseInt);
		put(DataType.Boolean.toString(), Boolean::parseBoolean);
		put(DataType.String.toString(), (value) -> value);
	}};
	int[] messages = new int[]{0};

	String topicPrefix();

	Map<String, MessageConsumer> consumerMap();

	Map<String, MessageProducer> producerMap();

	Session session();

	default MessageConsumer consumerOf(String suffix) {
		try {
			return session().createConsumer(session().createTopic(topicPrefix() + suffix));
		} catch (JMSException e) {
			throw new DaccosimException(e.getMessage());
		}
	}

	default MessageProducer producerOf(String suffix) {
		try {
			return session().createProducer(session().createTopic(topicPrefix() + suffix));
		} catch (JMSException e) {
			throw new DaccosimException(e.getMessage());
		}
	}

	default Message wait(String consumer) {
		try {
			return consumerMap().get(consumer).receive();
		} catch (JMSException e) {
			e.printStackTrace();
		}
		return null;
	}

	default Message wait(String consumer, long millis) {
		try {
			return consumerMap().get(consumer).receive(millis);
		} catch (JMSException e) {
			e.printStackTrace();
		}
		return null;
	}

	default void publish(String producer, Message message) {
		try {
			producerMap().get(producer).send(message);
			messages[0]++;
		} catch (JMSException e) {
			throw new DaccosimException(e.getMessage());
		}
	}

	default Message emptyMessage() {
		try {
			return session().createMessage();
		} catch (JMSException e) {
			throw new DaccosimException(e.getMessage());
		}
	}

	default String textFrom(Message message) {
		try {
			return ((TextMessage) message).getText();
		} catch (JMSException e) {
			throw new DaccosimException(e.getMessage());
		}
	}

	default Message messageFrom(String text) {
		try {
			ActiveMQTextMessage message = new ActiveMQTextMessage();
			message.setText(text);
			return message;
		} catch (MessageNotWriteableException e) {
			throw new DaccosimException(e.getMessage());
		}
	}

	interface TextToObjectMapper {
		Object fromText(String value);
	}
}
