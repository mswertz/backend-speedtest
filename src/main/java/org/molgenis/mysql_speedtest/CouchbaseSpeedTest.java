package org.molgenis.mysql_speedtest;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.time.StopWatch;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;

import rx.Observable;
import rx.functions.Func1;

public class CouchbaseSpeedTest
{
	public static void main(String[] args)
	{
		// Connect to localhost
		Cluster cluster = CouchbaseCluster.create();

		Bucket bucket = cluster.openBucket();

		StopWatch s = new StopWatch();
		s.start();

		int size = 100000;
		// batching
		List<JsonDocument> documents = new ArrayList<JsonDocument>();
		for (int i = 1; i < size; i++)
		{
			JsonObject o = JsonObject.empty().put("no", i);
			for (int j = 0; j < 100; j++)
			{
				o.put("col" + j, "value" + j);
			}
			documents.add(JsonDocument.create("" + i, o));
			if (i % 100 == 0)
			{
				batchUpdate(documents, bucket);
				documents = new ArrayList<JsonDocument>();
			}
		}
		batchUpdate(documents, bucket);
		
		printTime("CouchbaseHundredVarchar", size, s);

		// Disconnect and clear all allocated resources
		cluster.disconnect();
	}

	private static void batchUpdate(List<JsonDocument> documents, final Bucket bucket)
	{
		Observable.from(documents).flatMap(new Func1<JsonDocument, Observable<JsonDocument>>()
		{
			public Observable<JsonDocument> call(final JsonDocument docToInsert)
			{
				return bucket.async().insert(docToInsert);
			}
		}).last().toBlocking().single();
	}

	public static void printTime(String type, int count, StopWatch s)
	{
		// correct for ms so times 1000.0
		System.out.println(type + " inserted " + count + " in " + s.getTime() + "ms, is " + count * 1000.0 / s.getTime()
				+ " inserts per second");
	}
}
