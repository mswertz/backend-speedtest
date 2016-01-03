package org.molgenis.mysql_speedtest;

import org.apache.commons.lang3.time.StopWatch;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

/**
 * Hello world!
 *
 */
public class CassandraSpeedTest
{
	public static void main(String[] args)
	{
		Cluster cluster = null;
		try
		{
			cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
			Metadata metadata = cluster.getMetadata();
			System.out.printf("Connected to cluster: %s\n", metadata.getClusterName());
			for (Host host : metadata.getAllHosts())
			{
				System.out.printf("Datacenter: %s; Host: %s; Rack: %s\n", host.getDatacenter(), host.getAddress(),
						host.getRack());
			}

			Session session = cluster.connect();
			session.execute("DROP KEYSPACE IF EXISTS speedtest");
			session.execute("CREATE KEYSPACE speedtest WITH replication "
					+ "= {'class':'SimpleStrategy', 'replication_factor':3};");
			
			String val = "";
			String col = "";
			String decl = "";
			Object[] vals = new Object[101];
			for(int j=0; j<100; j++)
			{
				col +=",col"+j;
				val +=",?";
				decl += ", col"+j+" varchar";
				vals[j+1]="value"+j;
			}
			String create = "CREATE TABLE speedtest.VarcharTest (" + "id int PRIMARY KEY" +  decl + ")";
			System.out.println(create);
			session.execute(create);
			PreparedStatement statement = session.prepare("INSERT INTO speedtest.VarcharTest (id"+col+") VALUES (?"+val+")");
			BatchStatement batch = new BatchStatement();

			int size = 100000;
			System.out.println("start");
			StopWatch s = new StopWatch();
			s.start();
			for (int i = 1; i <= size; i++)
			{			
				vals[0]=i;
				batch.add(statement.bind(vals));
				if (i % 10 == 0)
				{
					session.execute(batch);
					batch = new BatchStatement();
				}
			}
			session.execute(batch);

			printTime("CassandraHundredVarchar", size, s);

			s = new StopWatch();
			s.start();

			statement = session.prepare("select count(*) from speedtest.intTest");
			ResultSet results = session.execute(statement.bind());
			Row row = results.one();

			System.out.println("keyspace contains: " + row.getLong("count") + " in " + s + "ms");

		}
		catch (Exception e)
		{
			System.out.println("error: " + e.getMessage());
		}
		finally
		{
			cluster.close();
		}
	}

	public static void printTime(String type, int count, StopWatch s)
	{
		// correct for ms so times 1000.0
		System.out.println("time " + s.getTime() + "ms, is " + count * 1000.0 / s.getTime() + " inserts per second");
	}
}
