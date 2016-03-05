package org.molgenis.mysql_speedtest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.lang3.time.StopWatch;

/**
 * Hello world!
 *
 */
public class MysqlSpeedTest
{
	static String url = "jdbc:mysql://localhost/mysqlspeedtest?"
			+ "user=molgenis&password=molgenis&rewriteBatchedStatements=true";
	
	public static void main(String[] args) throws SQLException
	{
		System.out.println("Start benchmark!");
		int size = 100000;
		
		runBenchmark(size, new BenchMark(){
			int cols = 100;
			public String getTableName()
			{
				return "MysqlHundredVarcharColumn";
			}

			public String getAttributes()
			{
				String intClause = "";
				for(int i = 0; i < cols; i++){
					intClause += ", no"+i+" varchar(255)";
				}
				return "id int auto_increment primary key"+intClause;
			}

			public String getInsertSql()
			{
				String intClause = "";
				String intValue = "";
				for(int i = 0; i < cols; i++){
					intClause += ", no"+i;
					intValue += ", ?";
				}
				return "("+intClause.substring(1)+") VALUES ("+intValue.substring(1)+")";
			}
			public void prepare(PreparedStatement pstmt, int j) throws SQLException
			{
				for(int i = 1; i <= cols; i++){
					pstmt.setString(i, ""+j);
				}
			}
		});
		
		runBenchmark(size, new BenchMark(){
			int cols = 100;
			public String getTableName()
			{
				return "MysqlHundredIntColumn";
			}

			public String getAttributes()
			{
				String intClause = "";
				for(int i = 0; i < cols; i++){
					intClause += ", no"+i+" int";
				}
				return "id int auto_increment primary key"+intClause;
			}

			public String getInsertSql()
			{
				String intClause = "";
				String intValue = "";
				for(int i = 0; i < cols; i++){
					intClause += ", no"+i;
					intValue += ", ?";
				}
				return "("+intClause.substring(1)+") VALUES ("+intValue.substring(1)+")";
			}
			public void prepare(PreparedStatement pstmt, int j) throws SQLException
			{
				for(int i = 1; i <= cols; i++){
					pstmt.setInt(i, j);
				}
			}
		});
		
		runBenchmark(size, new BenchMark(){
			public String getTableName()
			{
				return "MysqlOneIntColumn";
			}

			public String getAttributes()
			{
				return "id int auto_increment primary key, no int";
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
		ResultSet rs = stmt.executeQuery("select count(*) from MysqlOneIntColumn where no > 50000");
		rs.next();
		int count = rs.getInt(1);
		rs.close();
		stmt.close();

		stmt = conn.createStatement();
		rs = stmt.executeQuery("select * from MysqlOneIntColumn where no > 50000");

		while (rs.next())
		{
			rs.getInt(1);
		}

		System.out.println(" Count+retrieve " + count + " in " + s.getTime() + "ms, is " + count * 1000.0 / s.getTime()
		+ " records per second");
	}
	
	public static interface BenchMark{
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
			System.out.println("Start "+bc.getTableName());
			conn = DriverManager.getConnection(url);

			Statement stmt = conn.createStatement();
			stmt.executeUpdate("DROP TABLE IF EXISTS "+bc.getTableName());
			stmt.executeUpdate("CREATE TABLE "+bc.getTableName()+"("+bc.getAttributes()+") ENGINE=InnoDB");

			PreparedStatement pstmt = conn.prepareStatement("INSERT INTO "+bc.getTableName()+" "+bc.getInsertSql());
			StopWatch s = new StopWatch();
			s.start();
			
			conn.setAutoCommit(false);
			

			for (int i = 1; i <= size; i++)
			{
				//delegate to benchmark
				bc.prepare(pstmt, i);
				pstmt.addBatch();
				if(i % 1000 == 0)
				{
					pstmt.executeBatch();
				}
//				if(i % 10000 == 0)
//				{
//					printTime(bc.getTableName(), i, s);
//				}
			}
			pstmt.executeBatch();
			conn.commit();
			
			printTime(bc.getTableName(), size, s);
			System.out.println("Completed "+bc.getTableName());

			//s.stop();
			
		}
		catch (SQLException ex)
		{
			
			// handle any errors
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
		}
		finally {
			conn.close();
		}
	}
	public static void printTime(String type, int count, StopWatch s)
	{
		// correct for ms so times 1000.0
		System.out.println(type +" inserted "+ count+" in " + s.getTime() + "ms, is " + count * 1000.0 / s.getTime() + " inserts per second");
	}
}
