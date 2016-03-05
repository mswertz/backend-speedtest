package org.molgenis.mysql_speedtest;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.lang3.time.StopWatch;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

/**
 * Hello world!
 *
 */
public class PsqlSpeedTest
{
	static String url = "jdbc:postgresql://localhost/umcg-mswertz";

	public static void main(String[] args) throws SQLException
	{
		System.out.println("Start benchmark!");
		int size = 100000;

		// 4: using copy
		String type = "PostgresqlCopyHundredVarchar";
		try
		{
			Connection conn = DriverManager.getConnection(url);
			CopyManager copyManager = new CopyManager((BaseConnection) conn);

			// create
			int cols = 100;
			String colDef = "id serial primary key";
			String colClause = "id";
			for (int i = 0; i < cols; i++)
			{
				colDef += ", col" + i + " varchar(255)";
				colClause += ",col" + i;
			}
			Statement stmt = conn.createStatement();
			stmt.executeUpdate("DROP TABLE IF EXISTS " + type);
			stmt.executeUpdate("CREATE TABLE " + type + "(" + colDef + ") WITH (OIDS=FALSE)");

			StopWatch s = new StopWatch();
			s.start();
			StringBuilder sb = new StringBuilder();
			for (int i = 1; i <= size; i++)
			{
				sb.append(i);
				for (int j = 0; j < cols; j++)
				{
					sb.append(", value" + j);
				}
				sb.append("\n");

				if (i % 100 == 0)
				{
					// System.out.println("COPY "+type+"("+colClause+") FROM STDIN");
					copyManager.copyIn("COPY " + type + "(" + colClause + ") FROM STDIN WITH DELIMITER ','",
							new StringReader(sb.toString()));
					sb = new StringBuilder();
				}
			}
			copyManager.copyIn("COPY " + type + "(" + colClause + ") FROM STDIN WITH DELIMITER ','",
					new StringReader(sb.toString()));
			printTime(type, size, s);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		// runBenchmark(size, new BenchMark(){
		// int cols = 100;
		// public String getTableName()
		// {
		// return "PostgresqlHundredVarcharColumn";
		// }
		//
		// public String getAttributes()
		// {
		// String intClause = "";
		// for(int i = 0; i < cols; i++){
		// intClause += ", no"+i+" varchar(255)";
		// }
		// return "id serial primary key"+intClause;
		// }
		//
		// public String getInsertSql()
		// {
		// String intClause = "";
		// String intValue = "";
		// for(int i = 0; i < cols; i++){
		// intClause += ", no"+i;
		// intValue += ", ?";
		// }
		// return "("+intClause.substring(1)+") VALUES ("+intValue.substring(1)+")";
		// }
		// public void prepare(PreparedStatement pstmt, int j) throws SQLException
		// {
		// for(int i = 1; i <= cols; i++){
		// pstmt.setString(i, ""+j);
		// }
		// }
		// });
		//
		// runBenchmark(size, new BenchMark(){
		// int cols = 100;
		// public String getTableName()
		// {
		// return "PostgresqlHundredIntColumn";
		// }
		//
		// public String getAttributes()
		// {
		// String intClause = "";
		// for(int i = 0; i < cols; i++){
		// intClause += ", no"+i+" int";
		// }
		// return "id serial primary key"+intClause;
		// }
		//
		// public String getInsertSql()
		// {
		// String intClause = "";
		// String intValue = "";
		// for(int i = 0; i < cols; i++){
		// intClause += ", no"+i;
		// intValue += ", ?";
		// }
		// return "("+intClause.substring(1)+") VALUES ("+intValue.substring(1)+")";
		// }
		// public void prepare(PreparedStatement pstmt, int j) throws SQLException
		// {
		// for(int i = 1; i <= cols; i++){
		// pstmt.setInt(i, j);
		// }
		// }
		// });

		runBenchmark(size, new BenchMark()
		{
			public String getTableName()
			{
				return "PostgresqlOneIntColumn";
			}

			public String getAttributes()
			{
				return "id serial primary key, no int";
			}

			public String getInsertSql()
			{
				return "(no) VALUES (?)";
			}

			public void prepare(PreparedStatement pstmt, int i) throws SQLException
			{
				pstmt.setInt(1, i);
			}
		});

		// test count/read
		Connection conn = DriverManager.getConnection(url);
		
		StopWatch s = new StopWatch();
		s.start();

		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("select count(*) from PostgresqlOneIntColumn where no > 50000");
		rs.next();
		int count = rs.getInt(1);
		rs.close();
		stmt.close();

		stmt = conn.createStatement();
		rs = stmt.executeQuery("select * from PostgresqlOneIntColumn where no > 50000");

		while (rs.next())
		{
			rs.getInt(1);
		}

		System.out.println(" Count+retrieved " + count + " in " + s.getTime() + "ms, is " + count * 1000.0 / s.getTime()
		+ " records per second");

	}

	public static interface BenchMark
	{
		public String getTableName();

		public String getAttributes();

		public String getInsertSql();

		public void prepare(PreparedStatement pstmt, int i) throws SQLException;
	}

	public static void runBenchmark(int size, BenchMark bc) throws SQLException
	{
		Connection conn = null;

		try
		{
			System.out.println("Start " + bc.getTableName());
			conn = DriverManager.getConnection(url);

			Statement stmt = conn.createStatement();
			stmt.executeUpdate("DROP TABLE IF EXISTS " + bc.getTableName());
			stmt.executeUpdate("CREATE TABLE " + bc.getTableName() + "(" + bc.getAttributes() + ")");

			PreparedStatement pstmt = conn
					.prepareStatement("INSERT INTO " + bc.getTableName() + " " + bc.getInsertSql());
			StopWatch s = new StopWatch();
			s.start();

			conn.setAutoCommit(false);

			for (int i = 1; i <= size; i++)
			{
				// delegate to benchmark
				bc.prepare(pstmt, i);
				pstmt.addBatch();
				if (i % 1000 == 0)
				{
					pstmt.executeBatch();
				}
			}
			pstmt.executeBatch();
			conn.commit();

			printTime(bc.getTableName(), size, s);
			System.out.println("Completed " + bc.getTableName());

			// s.stop();

		}
		catch (SQLException ex)
		{

			// handle any errors
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
		}
		finally
		{
			conn.close();
		}
	}

	public static void printTime(String type, int count, StopWatch s)
	{
		// correct for ms so times 1000.0
		System.out.println(type + count + " in " + s.getTime() + "ms, is " + count * 1000.0 / s.getTime()
				+ " records per second");
	}
}
