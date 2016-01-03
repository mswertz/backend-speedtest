package org.molgenis.mysql_speedtest;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.time.StopWatch;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;

public class ElasticSpeedTest
{
	public static void main(String[] args)
	{
		Node node = null;
		try
		{
			node = nodeBuilder().node();
			Client client = node.client();
			//client.admin().indices().delete(new DeleteIndexRequest("speedtest")).actionGet();
			CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate("speedtest");
			CreateIndexResponse createResponse = createIndexRequestBuilder.execute().actionGet();

			System.out.println("Start");
			run(client, "ElasticHundredVarchar", new JsonBuilder()
			{
				public Map<String, Object> get(int i) throws IOException
				{
					Map<String, Object> result = new HashMap<String, Object>();
					result.put("no", i);
					for (int j = 0; j < 100; j++)
					{
						result.put("col" + j, "value" + j);
					}
					return result;
				}
			});
			run(client, "ElasticHundredInt", new JsonBuilder()
			{
				public Map<String, Object> get(int i) throws IOException
				{
					Map<String, Object> result = new HashMap<String, Object>();
					result.put("no", i);
					for (int j = 0; j < 100; j++)
					{
						result.put("col" + j, j);
					}
					return result;
				}
			});
			run(client, "ElasticOneInt", new JsonBuilder()
			{
				public Map<String, Object> get(int i) throws IOException
				{
					Map<String, Object> result = new HashMap<String, Object>();
					result.put("no", i);
					result.put("value", i);
					return result;
				}
			});
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			// on shutdown
			if (node != null) node.close();
		}
	}

	private interface JsonBuilder
	{
		Map<String, Object> get(int i) throws IOException;
	}

	private static void run(Client client, String type, JsonBuilder builder) throws IOException
	{
		StopWatch s = new StopWatch();
		s.start();
		int size = 100000;

		BulkRequestBuilder bulkRequest = client.prepareBulk();
		for (int i = 1; i <= size; i++)
		{
			bulkRequest.add(client.prepareIndex("speedtest", type, "" + i).setSource(builder.get(i)));

			if (i % 100 == 0)
			{
				BulkResponse bulkResponse = bulkRequest.execute().actionGet();
				if (bulkResponse.hasFailures())
				{
					throw new RuntimeException("has failures" + bulkResponse.buildFailureMessage());
				}
				// bulkRequest = client.prepareBulk();
			}
		}
		BulkResponse bulkResponse = bulkRequest.execute().actionGet();
		if (bulkResponse.hasFailures())
		{
			System.out.println(bulkResponse.buildFailureMessage());
		}
		printTime(type, size, s);
	}

	public static void printTime(String type, int count, StopWatch s)
	{
		// correct for ms so times 1000.0
		System.out.println(type + " inserted " + count + " in " + s.getTime() + "ms, is " + count * 1000.0 / s.getTime()
				+ " inserts per second");
	}
}
