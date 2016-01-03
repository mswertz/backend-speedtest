package org.molgenis.mysql_speedtest;

import org.apache.commons.lang3.time.StopWatch;

import com.orientechnologies.orient.core.intent.OIntentMassiveInsert;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class OrientDbSpeedTest
{

	public static void main(String[] args) throws Exception
	{
		OServer server = OServerMain.create();
		server.startup("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + "<orient-server>" + "<network>"
				+ "<protocols>"
				+ "<protocol name=\"binary\" implementation=\"com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary\"/>"
				+ "<protocol name=\"http\" implementation=\"com.orientechnologies.orient.server.network.protocol.http.ONetworkProtocolHttpDb\"/>"
				+ "</protocols>" + "<listeners>"
				+ "<listener ip-address=\"0.0.0.0\" port-range=\"2424-2430\" protocol=\"binary\"/>"
				+ "<listener ip-address=\"0.0.0.0\" port-range=\"2480-2490\" protocol=\"http\"/>" + "</listeners>"
				+ "</network>" + "<users>" + "<user name=\"root\" password=\"ThisIsA_TEST\" resources=\"*\"/>"
				+ "</users>" + "<properties>"
				+ "<entry name=\"orientdb.www.path\" value=\"C:/work/dev/orientechnologies/orientdb/releases/1.0rc1-SNAPSHOT/www/\"/>"
				+ "<entry name=\"orientdb.config.file\" value=\"C:/work/dev/orientechnologies/orientdb/releases/1.0rc1-SNAPSHOT/config/orientdb-server-config.xml\"/>"
				+ "<entry name=\"server.cache.staticResources\" value=\"false\"/>"
				+ "<entry name=\"log.console.level\" value=\"info\"/>"
				+ "<entry name=\"log.file.level\" value=\"fine\"/>"
				// The following is required to eliminate an error or warning "Error on resolving property:
				// ORIENTDB_HOME"
				+ "<entry name=\"plugin.dynamic\" value=\"false\"/>" + "</properties>" + "</orient-server>");
		server.activate();

		OrientGraphFactory factory = new OrientGraphFactory("plocal:graph/db").setupPool(1, 10);
		factory.declareIntent(new OIntentMassiveInsert());
		// EVERY TIME YOU NEED A GRAPH INSTANCE
		
		System.out.println("Start test");
		OrientGraphNoTx graph = factory.getNoTx();
		try
		{
			int size = 10000;
			StopWatch s = new StopWatch();
			s.start();

			for (int i = 0; i < size; i++)
			{
				OrientVertex v = graph.addVertex(null); // 1st OPERATION: IMPLICITLY BEGIN A TRANSACTION
				for (int j = 0; j < 100; j++)
				{
					v.setProperty("name" + j, "value" + j);
				}
				if(i % 1000 == 0)
				{
					graph.commit();
					printTime("orientdb100", i, s);
				}
			}
			graph.commit();

			printTime("orientdb100", size, s);
			System.out.println("Finished test");
		}
		catch (Exception e)
		{

		}
		finally
		{
			graph.shutdown();
		}

		server.shutdown();

	}

	public static void printTime(String type, int count, StopWatch s)
	{
		// correct for ms so times 1000.0
		System.out.println("time " + s.getTime() + "ms, is " + count * 1000.0 / s.getTime() + " inserts per second");
	}

}
