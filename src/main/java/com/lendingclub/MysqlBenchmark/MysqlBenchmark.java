package com.lendingclub.MysqlBenchmark;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Stopwatch;

/**
 * TODO 
 * - introduce some parallelism 
 * - read 10x whatever you write 
 * - expand row sizes, select sum(length(colN))
 * - make sure queries are not served from memory 
 * 
 * @author pkothari
 */
public class MysqlBenchmark {
	
	private static final int TOTAL_RECORDS = 20_000;
	private static final int BATCH_SIZE = 10_000;

	private static final MetricRegistry metrics = new MetricRegistry(); 
	private static final ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics).convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build();
	private static final Meter inserts1 = metrics.meter(Strategy.BASELINE.name() + "-meter");
	private static final Meter inserts2 = metrics.meter(Strategy.UUID_CHAR.name() + "-meter");
	private static final Meter inserts3 = metrics.meter(Strategy.UUID_OPTIMIZED.name() + "-meter");
	private static final Counter writes1 = metrics.counter(Strategy.BASELINE.name() + "-counter"); 
	private static final Counter writes2 = metrics.counter(Strategy.UUID_CHAR.name() + "-counter");
	private static final Counter writes3 = metrics.counter(Strategy.UUID_OPTIMIZED.name() + "-counter");

	private static final String DB_NAME = "BENCHMARK";
	private static final String TABLE_1 = "T1";
	private static final String TABLE_2 = "T2";
	private static final String TABLE_3 = "T3";
	
	private static final String dbUrl = "jdbc:mysql://localhost/";
	private static final String username = "root";
	private static final String password = "";
	private static final Stopwatch stopwatch = Stopwatch.createUnstarted();

	private static final BlockingQueue<String> guids = new LinkedBlockingDeque<>(TOTAL_RECORDS);
	private static final AtomicInteger id = new AtomicInteger(0);
	
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
			public void addRecordToBatch(PreparedStatement stmt) throws SQLException, InterruptedException {
				stmt.clearParameters();
				int nextId = id.incrementAndGet();
				stmt.setInt(1, nextId);
				stmt.setInt(2, nextId);
				stmt.setInt(3, nextId);
				stmt.addBatch();
			}

			@Override
			public void afterBatch() {
				inserts1.mark();
				writes1.inc(BATCH_SIZE);
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
			public void addRecordToBatch(PreparedStatement stmt) throws SQLException, InterruptedException {
				stmt.clearParameters();
				String val = guids.take();
				stmt.setString(1, val);
				stmt.setString(2, val);
				stmt.setString(3, val);
				stmt.setString(4, val);
				stmt.addBatch();
			}

			@Override
			public void afterBatch() {
				inserts2.mark();
				writes2.inc(BATCH_SIZE);
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
			public void addRecordToBatch(PreparedStatement stmt) throws SQLException, InterruptedException {
				stmt.clearParameters();
				byte[] val = new BigInteger(guids.take(), 16).toByteArray();
				stmt.setBytes(1, val);
				stmt.setBytes(2, val);
				stmt.setBytes(3, val);
				stmt.setBytes(4, val);
				stmt.addBatch();
			}

			@Override
			public void afterBatch() {
				inserts3.mark();
				writes3.inc(BATCH_SIZE);
			}
			
		},
		;

		public abstract String getDDL();

		public abstract String getInsertSql();

		public abstract void addRecordToBatch(PreparedStatement stmt) throws SQLException, InterruptedException;

		public abstract void afterBatch();
	}
	
	public static void main(String[] args) {
		startReporter();

		MysqlBenchmark b = new MysqlBenchmark();
	
		Strategy s = Strategy.UUID_OPTIMIZED;
//		for (Strategy s : Strategy.values()) {
			System.out.println("\n\nStarting " + s.name() + "...\n\n");
			populateGuids(s);
			stopwatch.reset().start();
			b.doWriteInBatches(s);
			System.out.println("\n\n" + s.name() + " took ==================> " + stopwatch);
			reporter.report();		
			metrics.removeMatching(new MetricFilter() {
				@Override
				public boolean matches(String name, Metric metric) {
					return name.startsWith(s.name());
				}
			});
//		}
	}

	private static void populateGuids(Strategy s) {
		guids.clear();
		for (int i=0; i < TOTAL_RECORDS; i++) 
			guids.offer(s == Strategy.UUID_CHAR ? 
					UUID.randomUUID().toString()
					: LcOptimizedGuids.asHexString());
	}

	private void doWriteInBatches(Strategy strategy) {
		try (Connection conn = getConnection(); 
				PreparedStatement stmt = conn.prepareStatement(strategy.getInsertSql())) {
			for (int total = 0; total < TOTAL_RECORDS; total += BATCH_SIZE) {				
				for (int j=0; j < BATCH_SIZE; j++) {
					strategy.addRecordToBatch(stmt);
				}
				stmt.executeBatch();
				strategy.afterBatch();
			}
		} catch (SQLException | InterruptedException e) {
			oops(e);
		}
	}

	private void query(String sql) {
		try (Connection conn = getConnection(); 
				PreparedStatement s = conn.prepareStatement(sql);
				ResultSet rs = s.executeQuery()) {
			while (rs.next()) {
				System.out.println(rs.getInt(1));
			}
		} catch (SQLException e) {
			oops(e);
		}
	}

	private static void startReporter() {
		reporter.start(5, TimeUnit.SECONDS);
	}

	
	private Connection getConnection() {
		try {
			Connection conn = DriverManager.getConnection(dbUrl, username, password);
			
			maybeClean(conn);
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

	private ReentrantLock lock = new ReentrantLock();
	private boolean isFirst = true; 
	private void maybeClean(Connection conn) {
		if (lock.tryLock() && isFirst) {
			try (Statement stmt = conn.createStatement()) {
				stmt.execute("DROP DATABASE IF EXISTS " + DB_NAME);
			} catch (SQLException e) {
			}
			isFirst = false;
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
}
