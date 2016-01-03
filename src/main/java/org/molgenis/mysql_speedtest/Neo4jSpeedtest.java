package org.molgenis.mysql_speedtest;

import org.apache.commons.lang3.time.StopWatch;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class Neo4jSpeedtest
{
	public static void main(String[] args)
	{
		@SuppressWarnings("deprecation")
		GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase("target/db");

		Transaction tx = null;
		try
		{
			StopWatch s = new StopWatch();
			s.start();
			tx = graphDb.beginTx();

			int size = 100000;
			for (int i = 0; i < size; i++)
			{
				Node node = graphDb.createNode();

				for (int j = 0; j < 100; j++)
				{
					node.setProperty("col" + j, "value" + j);
				}
			}
			tx.success();
			printTime("Neo4jHundredVarchar", size, s);

		}
		catch (Exception e)
		{
			System.out.println(e);
		}
		finally
		{
			tx.close();
		}

		registerShutdownHook(graphDb);
	}

	private static void registerShutdownHook(final GraphDatabaseService graphDb)
	{
		// Registers a shutdown hook for the Neo4j instance so that it
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
		// running application).
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				graphDb.shutdown();
			}
		});
	}

	public static void printTime(String type, int count, StopWatch s)
	{
		// correct for ms so times 1000.0
		System.out.println("time " + s.getTime() + "ms, is " + count * 1000.0 / s.getTime() + " inserts per second");
	}
}
