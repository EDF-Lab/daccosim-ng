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

import eu.simulage.daccosim.model.nodes.v2.FMUJMS;
import eu.simulage.daccosim.view.Graph;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.security.AuthenticationUser;
import org.apache.activemq.security.SimpleAuthenticationPlugin;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;
import java.nio.file.Files;
import java.util.List;

import static eu.simulage.daccosim.HelperTest.linesToString;
import static eu.simulage.daccosim.view.DistributedGraphs.*;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class DistributedGraphExecutorTest {

	public static void main(String[] args) {
//		createJMSThread();
//		new GraphExecutor(environmentBuildingGraph().commit()).execute();
//		new GraphExecutor(environmentEnvironmentGraph().commit()).execute();
//		Graph graph = environmentCoordinatorGraph();
//		graph.settings().stopTime(25000);
//		new GraphExecutor(graph.commit()).execute();
	}


	@Test
	@Ignore
	public void environment_graph_in_three_pieces() throws Exception {
		createJMSThread();
		Thread thread = new Thread(() -> new GraphExecutor(environmentBuildingGraph().commit()).execute());
		Thread thread1 = new Thread(() -> new GraphExecutor(environmentEnvironmentGraph().commit()).execute());
		Graph graph = environmentCoordinatorGraph().exportAll();
		GraphExecutor executor = new GraphExecutor(graph.commit());
		Thread thread2 = new Thread(executor::execute);
		thread2.start();
		thread.start();
		thread1.start();
		thread2.join();
		thread.join();
		thread1.join();
		String expectedResult =
				"time;dt;Environment.temperature;Building.externalTemperature;Adder.input1;Adder.input2;Adder.output;External.output\n" +
						"0.0;0.0;1.5;0.0;0.0;0.0;0.0;10.0\n" +
						"1.0;1.0;2.5;0.0;2.5;10.0;12.5;10.0\n" +
						"2.0;1.0;3.5;12.5;3.5;10.0;13.5;10.0\n" +
						"3.0;1.0;4.5;13.5;4.5;10.0;14.5;10.0\n" +
						"4.0;1.0;5.5;14.5;5.5;10.0;15.5;10.0\n" +
						"5.0;1.0;6.5;15.5;6.5;10.0;16.5;10.0\n" +
						"6.0;1.0;7.5;16.5;7.5;10.0;17.5;10.0\n" +
						"7.0;1.0;8.5;17.5;8.5;10.0;18.5;10.0\n" +
						"8.0;1.0;9.5;18.5;9.5;10.0;19.5;10.0\n" +
						"9.0;1.0;10.5;19.5;10.5;10.0;20.5;10.0\n" +
						"10.0;1.0;11.5;20.5;11.5;10.0;21.5;10.0\n";
		assertEquals(expectedResult, linesToString(Files.readAllLines(executor.exportFile().toPath())));
		executor.exportFile().delete();
		executor.logFile().delete();
	}

	@Test
	@Ignore
	public void environment_graph_in_three_pieces_performance() throws Exception {
		createJMSThread();
		Thread thread = new Thread(() -> new GraphExecutor(environmentBuildingGraph().commit()).execute());
		Thread thread1 = new Thread(() -> new GraphExecutor(environmentEnvironmentGraph().commit()).execute());
		Graph graph = environmentCoordinatorGraph();
		graph.settings().stopTime(25000);
		GraphExecutor executor = new GraphExecutor(graph.commit());
		long init = System.nanoTime();
		Thread thread2 = new Thread(executor::execute);
		thread2.start();
		thread.start();
		thread1.start();
		thread2.join();
		thread.join();
		thread1.join();
		long endTime = System.nanoTime();
		System.out.println(((endTime - init) / 1e9));
		System.out.println("Messages sent: " + FMUJMS.messages[0]);
	}

	private static void createJMSThread() {
		try {
			BrokerService service = new BrokerService();
			TransportConnector transportConnector = new TransportConnector();
			transportConnector.setUri(new URI("tcp://0.0.0.0:61616"));
			transportConnector.setName("OWireConn");
			service.setPersistent(false);
			service.addConnector(transportConnector);
			service.setPlugins(new BrokerPlugin[]{new SimpleAuthenticationPlugin(users())});
			service.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static List<?> users() {
		return singletonList(new AuthenticationUser("daccosim", "daccosim", "admin"));
	}
}
