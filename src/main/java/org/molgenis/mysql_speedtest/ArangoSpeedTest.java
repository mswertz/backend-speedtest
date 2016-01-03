package org.molgenis.mysql_speedtest;

import java.util.HashMap;

import org.apache.commons.lang3.time.StopWatch;

import com.arangodb.ArangoConfigure;
import com.arangodb.ArangoDriver;

public class ArangoSpeedTest
{
	@SuppressWarnings("deprecation")
	public static void main(String[] args)
	{
		ArangoConfigure configure = new ArangoConfigure();
		configure.init();
		configure.setPort(8000);
		configure.setMaxPerConnection(1000);
		//configure.setBatchSize(100);
		ArangoDriver arangoDriver = new ArangoDriver(configure);

		String dbName = "speedtest";
		try
		{
			arangoDriver.deleteDatabase(dbName);
			arangoDriver.createDatabase(dbName);
			System.out.println("Database created: " + dbName);

			String type = "ArangoHundredVarchar";
			int size = 10000;
			

			arangoDriver.deleteCollection(type);
			arangoDriver.createCollection(type);

			StopWatch s = new StopWatch();
			s.start();

//			arangoDriver.startBatchMode();
			for (int i = 1; i <= size; i++)
			{
				HashMap<String, Object> myObject = new HashMap<String, Object>();
				myObject.put("" + i, i);
				for (int j = 0; j < 100; j++)
				{
					myObject.put("col" + j, "value" + j);
				}
				arangoDriver.createDocument(type, myObject, true, false);

//				if (i % 100 == 0)
//				{
//					arangoDriver.executeBatch();
//					arangoDriver.startBatchMode();
//				}
			}
//			arangoDriver.executeBatch();
			
			printTime(type, size, s);
		}
		catch (Exception e)
		{
			System.out.println("Failed to create database " + dbName + "; " + e.getMessage());
		}

	}

	public static void printTime(String type, int count, StopWatch s)
	{
		// correct for ms so times 1000.0
		System.out.println(type + " inserted " + count + " in " + s.getTime() + "ms, is " + count * 1000.0 / s.getTime()
				+ " inserts per second");
	}
}
