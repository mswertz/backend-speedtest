package org.molgenis.mysql_speedtest;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.time.StopWatch;
import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoSpeedTest
{
	public static void main(String[] args)
	{
		try
		{
			MongoClient mongoClient = new MongoClient();
			MongoDatabase database = mongoClient.getDatabase("mydb");

			// test one
			run(database, "MongoOneInt", new DocumentGenerator()
			{
				public Document getDocument(int i)
				{
					return new Document("i", i);
				}
			});
			
			// test two
			run(database, "MongoHundredVarchar", new DocumentGenerator()
			{
				public Document getDocument(int i)
				{
					
					Document d = new Document("id", i);
					for(int j = 0; j< 100; j++)
					{
						d.append("col"+j, "value"+j);
					}
					return d;
				}
			});
			
			// test two
			run(database, "MongoHundredInt", new DocumentGenerator()
			{
				public Document getDocument(int i)
				{
					
					Document d = new Document("id", i);
					for(int j = 0; j< 100; j++)
					{
						d.append("col"+j, j);
					}
					return d;
				}
			});

			mongoClient.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}

	public interface DocumentGenerator
	{

		Document getDocument(int i);
	}

	private static void run(MongoDatabase database, String type, DocumentGenerator gen)
	{
		MongoCollection<Document> collection = database.getCollection(type);
		StopWatch s = new StopWatch();
		s.start();
		List<Document> documents = new ArrayList<Document>();
		int size = 100000;
		for (int i = 1; i <= size; i++)
		{
			documents.add(gen.getDocument(i));
			if (i % 1000 == 0 && documents.size() > 0)
			{
				collection.insertMany(documents);
				documents = new ArrayList<Document>();
			}
		}
		if (documents.size() > 0) collection.insertMany(documents);
		printTime(type, size, s);
		
		collection.drop();

	}

	public static void printTime(String type, int count, StopWatch s)
	{
		// correct for ms so times 1000.0
		System.out.println(type + " inserted " + count + " in " + s.getTime() + "ms, is " + count * 1000.0 / s.getTime()
				+ " inserts per second");
	}
}
