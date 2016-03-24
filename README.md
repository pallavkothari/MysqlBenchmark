# MysqlBenchmark

Current analysis includes a single-threaded write workload (500k records). 
BASELINE => pk is a bigint(8)
UUID_CHAR => pk is char(36)
UUID_OPTIMIZED => pk is binary(16)

Note: this workload is for a single table, with 3 AK unique indexes in addition to the PK. 



Starting BASELINE...

BASELINE took ==================> 1.177 min

-- Counters --------------------------------------------------------------------
BASELINE-counter
             count = 500000

-- Meters ----------------------------------------------------------------------
BASELINE-meter
             count = 50
         mean rate = 0.70 events/second
     1-minute rate = 0.62 events/second
     5-minute rate = 0.46 events/second
    15-minute rate = 0.42 events/second




Starting UUID_CHAR...

UUID_CHAR took ==================> 2.352 min


-- Counters --------------------------------------------------------------------
UUID_CHAR-counter
             count = 500000

-- Meters ----------------------------------------------------------------------
UUID_CHAR-meter
             count = 50
         mean rate = 0.23 events/second
     1-minute rate = 0.25 events/second
     5-minute rate = 0.12 events/second
    15-minute rate = 0.05 events/second



Starting UUID_OPTIMIZED...

UUID_OPTIMIZED took ==================> 1.187 min

-- Counters --------------------------------------------------------------------
UUID_OPTIMIZED-counter
             count = 500000

-- Meters ----------------------------------------------------------------------
UUID_OPTIMIZED-meter
             count = 50
         mean rate = 0.70 events/second
     1-minute rate = 0.60 events/second
     5-minute rate = 0.46 events/second
    15-minute rate = 0.42 events/second