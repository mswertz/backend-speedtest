package org.molgenis.mysql_speedtest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.StopWatch;
import org.lightcouch.CouchDbClient;

public class CouchdbSpeedTest
{
	public static void main(String[] args)
	{		
		
		CouchDbClient dbClient = new CouchDbClient("speedtest", true, "http", "127.0.0.1", 5984, null, null);

		List<Map<String,String>> data = new ArrayList<Map<String,String>>();

		StopWatch s = new StopWatch();
		s.start();
		int size = 100000;
		for (int i = 1; i < size; i++)
		{
			Map<String,String> values = new HashMap<String,String>();
			for(int j = 0; j < 100; j++)
			{
				values.put("col"+j, "value"+j);
			}
			data.add(values);
			
			if (i % 100 == 0)
			{
				dbClient.bulk(data, true);
				data = new ArrayList<Map<String,String>>();
			}
		}
		dbClient.bulk(data, true);
		
		printTime("CouchdbHundredVarchar", size, s);

		dbClient.shutdown();
	}
	
	public static void printTime(String type, int count, StopWatch s)
	{
		// correct for ms so times 1000.0
		System.out.println(type +" inserted "+ count+" in " + s.getTime() + "ms, is " + count * 1000.0 / s.getTime() + " inserts per second");
	}
}
