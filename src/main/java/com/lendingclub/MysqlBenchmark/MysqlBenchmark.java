package com.lendingclub.MysqlBenchmark;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

/**
 * 
 * @author pkothari
 */
public class MysqlBenchmark {
	
	private static final int TOTAL_RECORDS = 500_000;
	private static final int BATCH_SIZE = 1_000;
	private static final int NUM_WRITERS = 10;
	private static final int NUM_READERS = 10*NUM_WRITERS;
	private static final int VARCHAR_COL_LENGTH = 100; 
	
	private static final MetricRegistry metrics = new MetricRegistry(); 
	private static final ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics).convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build();

	private static final String DB_NAME = "BENCHMARK";
	private static final String TABLE_1 = "T1";
	private static final String TABLE_2 = "T2";
	private static final String TABLE_3 = "T3";
	
	private static final String dbUrl = "jdbc:mysql://localhost/";
	private static final String username = "root";
	private static final String password = "";
	private static final Stopwatch stopwatch = Stopwatch.createUnstarted();

	private static final BlockingQueue<String> guids = new LinkedBlockingDeque<>(TOTAL_RECORDS);
	
	enum Strategy {
		BASELINE {
			@Override
			public String getDDL() {
				return "CREATE TABLE IF NOT EXISTS " + getTable() 
						+ " ("
						+ "id bigint unsigned NOT NULL auto_increment, "
						+ "c1 bigint not null,"
						+ "c2 bigint not null,"
						+ "c3 bigint not null,"
						+ commonDDLSuffix();
			}

			@Override
			public String getInsertSql() {
				return "insert into " + getTable() + "(" + getColList() +") values " + getBindParams(20);
			}

			@Override
			public void addRecordToBatch(PreparedStatement stmt, String nextGuid) throws SQLException, InterruptedException {
				stmt.clearParameters();
				int nextId = Integer.valueOf(nextGuid);
				stmt.setInt(1, nextId);
				stmt.setInt(2, nextId);
				stmt.setInt(3, nextId);
				setStrings(stmt, 4, 20);
				stmt.addBatch();
			}

			@Override
			public void afterBatch() {
				Lazy1.inserts1.mark();
				Lazy1.writes1.inc(BATCH_SIZE);
			}

			@Override
			public String getTable() {
				return TABLE_1;
			}

			@Override
			public void afterRead() {
				Lazy1.reads1.mark();
			}
		}, 
		
		UUID_CHAR {
			@Override
			public String getDDL() {
				return "CREATE TABLE IF NOT EXISTS " + getTable() 
						+ " ("
						+ "id char(36) not null, "
						+ "c1 char(36) not null, "
						+ "c2 char(36) not null, "
						+ "c3 char(36) not null, "
						+ commonDDLSuffix();
			}

			@Override
			public String getInsertSql() {
				return "insert into " + getTable() + " (id, " + getColList() + ") values " + getBindParams(21);
			}

			@Override
			public void addRecordToBatch(PreparedStatement stmt, String nextGuid) throws SQLException, InterruptedException {
				stmt.clearParameters();
				stmt.setString(1, nextGuid);
				stmt.setString(2, nextGuid);
				stmt.setString(3, nextGuid);
				stmt.setString(4, nextGuid);
				setStrings(stmt, 5, 21);
				stmt.addBatch();
			}

			@Override
			public void afterBatch() {
				Lazy2.inserts2.mark();
				Lazy2.writes2.inc(BATCH_SIZE);
			}

			@Override
			public String getTable() {
				return TABLE_2;
			}

			@Override
			public void afterRead() {
				Lazy2.reads2.mark();
			}
		},
		
		UUID_OPTIMIZED {

			@Override
			public String getDDL() {
				return "CREATE TABLE IF NOT EXISTS " + getTable() 
						+ " ("
						+ "id binary(16) not null, "
						+ "c1 binary(16) not null, "
						+ "c2 binary(16) not null, "
						+ "c3 binary(16) not null, "
						+ commonDDLSuffix();
			}

			@Override
			public String getInsertSql() {
				return "insert into " + getTable() + " (id, " + getColList() + ") values " + getBindParams(21);
			}

			@Override
			public void addRecordToBatch(PreparedStatement stmt, String nextGuid) throws SQLException, InterruptedException {
				stmt.clearParameters();
				byte[] val = new BigInteger(nextGuid, 16).toByteArray();
				stmt.setBytes(1, val);
				stmt.setBytes(2, val);
				stmt.setBytes(3, val);
				stmt.setBytes(4, val);
				setStrings(stmt, 5, 21);
				stmt.addBatch();
			}

			@Override
			public void afterBatch() {
				Lazy3.inserts3.mark();
				Lazy3.writes3.inc(BATCH_SIZE);
			}

			@Override
			public String getTable() {
				return TABLE_3;
			}

			@Override
			public void afterRead() {
				Lazy3.reads3.mark();
			}
			
		},
		;

		public abstract String getDDL();

		public abstract String getInsertSql();

		public abstract void addRecordToBatch(PreparedStatement stmt, String val) throws SQLException, InterruptedException;

		public abstract void afterBatch();
		
		// returns c1,c2,..c20
		private static String getColList() {
			return IntStream.rangeClosed(1, 20).mapToObj(i -> "c" + i).collect(Collectors.joining(","));
		}
		
		private static String getBindParams(int n) {
			return IntStream.rangeClosed(1, n).mapToObj(i -> "?").collect(Collectors.joining(",", "(", ")"));
		}
		
		private static void setStrings(PreparedStatement stmt, int start, int end) throws SQLException {
			for (int i = start; i <= end; i++) {
				stmt.setString(i, randomVarchar());
			}
		}
		
		private static Random random = new Random();
		
		private static String randomVarchar() {
			return random.ints('0', 'z').mapToObj(i -> (char)i).limit(random.nextInt(VARCHAR_COL_LENGTH)).collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();
		}

		private static String commonDDLSuffix() {
			return makeCols(4, 20)
					+ " , PRIMARY KEY(ID), "
					+ " UNIQUE KEY akc1 (c1), "
					+ " UNIQUE KEY akc2 (c2), "
					+ " UNIQUE KEY akc3 (c3) "
					+ " )"
					+ "ENGINE=InnoDB DEFAULT CHARSET=utf8";
		}
		
		private static String makeCols(int start, int end) {
			return IntStream.rangeClosed(start, end).mapToObj(i -> "c" + i + " VARCHAR(" + VARCHAR_COL_LENGTH +")").collect(Collectors.joining(","));
		}

		public String getSQL() {
			return String.format(
					"select sum(length(c20)) "
					+ "from %s force index (primary) "
					+ "where id < (select max(id) from %s)", getTable(), getTable());
		}

		public abstract String getTable();

		public abstract void afterRead(); 
	}
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		startReporter();

		ExecutorService threadPool = Executors.newFixedThreadPool(NUM_WRITERS + NUM_READERS);
		ExecutorCompletionService<Void> completionService = new ExecutorCompletionService<>(threadPool);
		
//		Strategy s = Strategy.UUID_OPTIMIZED;
		for (Strategy s : Strategy.values()) {
			
			System.out.println("\n\nStarting " + s.name() + "...\n\n");
			populateGuids(s);
			stopwatch.reset().start();

			for (int i=0; i < NUM_WRITERS; i++)
				completionService.submit(new BatchWriter(s));
			
			for (int i=0; i < NUM_READERS; i++) 
				completionService.submit(new Reader(s));
			
			for (int i=0; i < NUM_WRITERS + NUM_READERS; i++) {
				completionService.take().get();
			}
			
			System.out.println("\n\n" + s.name() + " took ==================> " + stopwatch);
			reporter.report();		
			metrics.removeMatching(new MetricFilter() {
				@Override
				public boolean matches(String name, Metric metric) {
					return name.startsWith(s.name());
				}
			});
		}
			threadPool.shutdown();
	}

	private static void populateGuids(Strategy s) {
		guids.clear();
		for (int i=0; i < TOTAL_RECORDS; i++) 
			switch (s) {
			case BASELINE:
				guids.offer(String.valueOf(i));
				break;
			case UUID_CHAR:
				guids.offer(UUID.randomUUID().toString());
				break;
			case UUID_OPTIMIZED:
				guids.offer(LcOptimizedGuids.asHexString());
				break;
			}
	}

	private static final class BatchWriter implements Callable<Void> {

		private Strategy strategy;

		public BatchWriter(Strategy s) {
			strategy = s;
		}

		@Override
		public Void call() {
			try (Connection conn = getConnection(); 
					PreparedStatement stmt = conn.prepareStatement(strategy.getInsertSql())) {
				while (!guids.isEmpty()) {
					doOneBatch(strategy, stmt);
				}
			} catch (SQLException | InterruptedException e) {
				oops(e);
			}
			return null;
			
//			System.out.println(Thread.currentThread().getName() + " finished!! ");
		}
		
		private void doOneBatch(Strategy strategy, PreparedStatement stmt) throws SQLException, InterruptedException {
			ArrayList<String> oneBatch = Lists.newArrayListWithCapacity(BATCH_SIZE);
			guids.drainTo(oneBatch, BATCH_SIZE);
 			for (String val : oneBatch) {
				strategy.addRecordToBatch(stmt, val);
			}
			stmt.executeBatch();
			strategy.afterBatch();
		}
	}

	private static class Reader implements Callable<Void> {

		private Strategy s;

		public Reader(Strategy s) {
			this.s = s;
		}

		@Override
		public Void call() throws Exception {
			try (Connection conn = getConnection();
					PreparedStatement stmt = conn.prepareStatement(s.getSQL())) {
				while (!guids.isEmpty()) {
					ResultSet rs = stmt.executeQuery();
					rs.close();
					s.afterRead();
					Thread.sleep(100);
				}
			}
			return null;
		}

	}
	
	private static void startReporter() {
//		reporter.start(15, TimeUnit.SECONDS);
	}
	
	private static Connection getConnection() {
		try {
			Connection conn = DriverManager.getConnection(dbUrl, username, password);
			
			try (Statement stmt = conn.createStatement()) {
				stmt.execute("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
				stmt.execute("USE " + DB_NAME);
				for (Strategy s : Strategy.values()) {
					stmt.execute(s.getDDL());
				}
			}
			return conn;
		} catch (SQLException e) {
			oops(e);
			return null;
		} 
	}

	static {
		try(Connection conn = DriverManager.getConnection(dbUrl, username, password);
				Statement stmt = conn.createStatement()	) {
			stmt.execute("DROP DATABASE IF EXISTS " + DB_NAME);
		} catch (SQLException e1) {
			oops(e1);
		}
	}

	private static void oops(Exception e) {
		e.printStackTrace();
		System.exit(1);
	}

	static {
		String dbClass = "com.mysql.jdbc.Driver";
		try {
			Class.forName(dbClass);
		} catch (ClassNotFoundException e) {
			oops(e);
		}
	}
	
	static class Lazy1 {
		private static final Meter inserts1 = metrics.meter(Strategy.BASELINE.name() + "-writes");
		private static final Meter reads1 = metrics.meter(Strategy.BASELINE.name() + "-reads");
		private static final Counter writes1 = metrics.counter(Strategy.BASELINE.name() + "-counter"); 
	}
	static class Lazy2 {
		private static final Meter inserts2 = metrics.meter(Strategy.UUID_CHAR.name() + "-writes");
		private static final Meter reads2 = metrics.meter(Strategy.UUID_CHAR.name() + "-reads");
		private static final Counter writes2 = metrics.counter(Strategy.UUID_CHAR.name() + "-counter");
	}
	static class Lazy3 {
		private static final Meter inserts3 = metrics.meter(Strategy.UUID_OPTIMIZED.name() + "-writes");
		private static final Meter reads3 = metrics.meter(Strategy.UUID_OPTIMIZED.name() + "-reads");
		private static final Counter writes3 = metrics.counter(Strategy.UUID_OPTIMIZED.name() + "-counter");		
	}
	
}
