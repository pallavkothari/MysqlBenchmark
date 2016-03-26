package com.lendingclub.MysqlBenchmark;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

/**
 * TODO 
 * - read 10x whatever you write 
 * - expand row sizes, select sum(length(colN))
 * - make sure queries are not served from memory 
 * 
 * @author pkothari
 */
public class MysqlBenchmark {
	
	private static final int NUM_THREADS = 10;
	private static final int TOTAL_RECORDS = 1_000_000;
	private static final int BATCH_SIZE = 10_000;

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
				return "CREATE TABLE IF NOT EXISTS " + TABLE_1 
						+ " ("
						+ "id bigint unsigned NOT NULL auto_increment, "
						+ "c1 bigint not null,"
						+ "c2 bigint not null,"
						+ "c3 bigint not null,"
						+ "c4 VARCHAR(10), c5 VARCHAR(10), c6 VARCHAR(10), c7 VARCHAR(10), c8 VARCHAR(10), c9 VARCHAR(10), c10 VARCHAR(10), c11 VARCHAR(10), "
						+ "c12 VARCHAR(10), c13 VARCHAR(10), c14 VARCHAR(10), c15 VARCHAR(10), c16 VARCHAR(10), c17 VARCHAR(10), c18 VARCHAR(10), c19 VARCHAR(10), c20 VARCHAR(10), "
						+ " PRIMARY KEY(ID), "
						+ " UNIQUE KEY akc1 (c1), "
						+ " UNIQUE KEY akc2 (c2), "
						+ " UNIQUE KEY akc3 (c3) "
						+ " )"
						+ "ENGINE=InnoDB DEFAULT CHARSET=utf8";
			}

			@Override
			public String getInsertSql() {
				return "insert into " + TABLE_1 + "(c1, c2, c3) values (?, ?, ?)";
			}

			@Override
			public void addRecordToBatch(PreparedStatement stmt, String nextGuid) throws SQLException, InterruptedException {
				stmt.clearParameters();
				int nextId = Integer.valueOf(nextGuid);
				stmt.setInt(1, nextId);
				stmt.setInt(2, nextId);
				stmt.setInt(3, nextId);
				stmt.addBatch();
			}

			@Override
			public void afterBatch() {
				Lazy1.inserts1.mark();
				Lazy1.writes1.inc(BATCH_SIZE);
			}
		}, 
		
		UUID_CHAR {
			@Override
			public String getDDL() {
				return "CREATE TABLE IF NOT EXISTS " + TABLE_2 
						+ " ("
						+ "id char(36) not null, "
						+ "c1 char(36) not null, "
						+ "c2 char(36) not null, "
						+ "c3 char(36) not null, "
						+ "c4 VARCHAR(10), c5 VARCHAR(10), c6 VARCHAR(10), c7 VARCHAR(10), c8 VARCHAR(10), c9 VARCHAR(10), c10 VARCHAR(10), c11 VARCHAR(10), "
						+ "c12 VARCHAR(10), c13 VARCHAR(10), c14 VARCHAR(10), c15 VARCHAR(10), c16 VARCHAR(10), c17 VARCHAR(10), c18 VARCHAR(10), c19 VARCHAR(10), c20 VARCHAR(10), "
						+ " PRIMARY KEY(ID), "
						+ "UNIQUE KEY akc1 (c1), "
						+ "UNIQUE KEY akc2 (c2), "
						+ "UNIQUE KEY akc3 (c3) "
						+ " )"
						+ "ENGINE=InnoDB DEFAULT CHARSET=utf8";
			}

			@Override
			public String getInsertSql() {
				return "insert into " + TABLE_2 + " (id, c1, c2, c3) values (?, ?, ?, ?)";
			}

			@Override
			public void addRecordToBatch(PreparedStatement stmt, String nextGuid) throws SQLException, InterruptedException {
				stmt.clearParameters();
				stmt.setString(1, nextGuid);
				stmt.setString(2, nextGuid);
				stmt.setString(3, nextGuid);
				stmt.setString(4, nextGuid);
				stmt.addBatch();
			}

			@Override
			public void afterBatch() {
				Lazy2.inserts2.mark();
				Lazy2.writes2.inc(BATCH_SIZE);
			}
		},
		
		UUID_OPTIMIZED {

			@Override
			public String getDDL() {
				return "CREATE TABLE IF NOT EXISTS " + TABLE_3 
						+ " ("
						+ "id binary(16) not null, "
						+ "c1 binary(16) not null, "
						+ "c2 binary(16) not null, "
						+ "c3 binary(16) not null, "
						+ "c4 VARCHAR(10), c5 VARCHAR(10), c6 VARCHAR(10), c7 VARCHAR(10), c8 VARCHAR(10), c9 VARCHAR(10), c10 VARCHAR(10), c11 VARCHAR(10), "
						+ "c12 VARCHAR(10), c13 VARCHAR(10), c14 VARCHAR(10), c15 VARCHAR(10), c16 VARCHAR(10), c17 VARCHAR(10), c18 VARCHAR(10), c19 VARCHAR(10), c20 VARCHAR(10), "
						+ " PRIMARY KEY(ID), "
						+ "UNIQUE KEY akc1 (c1), "
						+ "UNIQUE KEY akc2 (c2), "
						+ "UNIQUE KEY akc3 (c3) "
						+ " )"
						+ "ENGINE=InnoDB DEFAULT CHARSET=utf8";
			}

			@Override
			public String getInsertSql() {
				return "insert into " + TABLE_3 + " (id, c1, c2, c3) values (?, ?, ?, ?)";
			}

			@Override
			public void addRecordToBatch(PreparedStatement stmt, String nextGuid) throws SQLException, InterruptedException {
				stmt.clearParameters();
				byte[] val = new BigInteger(nextGuid, 16).toByteArray();
				stmt.setBytes(1, val);
				stmt.setBytes(2, val);
				stmt.setBytes(3, val);
				stmt.setBytes(4, val);
				stmt.addBatch();
			}

			@Override
			public void afterBatch() {
				Lazy3.inserts3.mark();
				Lazy3.writes3.inc(BATCH_SIZE);
			}
			
		},
		;

		public abstract String getDDL();

		public abstract String getInsertSql();

		public abstract void addRecordToBatch(PreparedStatement stmt, String val) throws SQLException, InterruptedException;

		public abstract void afterBatch();
	}
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		startReporter();

		MysqlBenchmark b = new MysqlBenchmark();
	
		ExecutorService threadPool = Executors.newFixedThreadPool(NUM_THREADS);
		ExecutorCompletionService<Void> completionService = new ExecutorCompletionService<>(threadPool);
		
//		Strategy s = Strategy.UUID_OPTIMIZED;
		for (Strategy s : Strategy.values()) {
			
			System.out.println("\n\nStarting " + s.name() + "...\n\n");
			populateGuids(s);
			stopwatch.reset().start();

			for (int i=0; i < NUM_THREADS; i++) {
				completionService.submit(new BatchWriter(s), null);
			}
			
			for (int i=0; i < NUM_THREADS; i++) {
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

	private static final class BatchWriter implements Runnable {

		private Strategy strategy;

		public BatchWriter(Strategy s) {
			strategy = s;
		}

		@Override
		public void run() {
			try (Connection conn = getConnection(); 
					PreparedStatement stmt = conn.prepareStatement(strategy.getInsertSql())) {
				while (!guids.isEmpty()) {
					doOneBatch(strategy, stmt);
				}
			} catch (SQLException | InterruptedException e) {
				oops(e);
			}
			
			System.out.println(Thread.currentThread().getName() + " finished!! ");
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

	private static void startReporter() {
//		reporter.start(5, TimeUnit.SECONDS);
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
		private static final Meter inserts1 = metrics.meter(Strategy.BASELINE.name() + "-meter");
		private static final Counter writes1 = metrics.counter(Strategy.BASELINE.name() + "-counter"); 
	}
	static class Lazy2 {
		private static final Meter inserts2 = metrics.meter(Strategy.UUID_CHAR.name() + "-meter");
		private static final Counter writes2 = metrics.counter(Strategy.UUID_CHAR.name() + "-counter");
	}
	static class Lazy3 {
		private static final Meter inserts3 = metrics.meter(Strategy.UUID_OPTIMIZED.name() + "-meter");
		private static final Counter writes3 = metrics.counter(Strategy.UUID_OPTIMIZED.name() + "-counter");		
	}
	
}
