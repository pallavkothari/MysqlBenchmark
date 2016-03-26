# MysqlBenchmark   
 
  Current analysis includes a multi-threaded write workload (1M records).    
BASELINE => pk is a bigint(8)   
UUID_CHAR => pk is char(36)   
UUID_OPTIMIZED => pk is binary(16)   
 
p.s.: this workload is for a single table, with 3 AK unique indexes in addition to the PK.     
p.p.s.: each "event" below refers to a batch of 10k rows. So 0.7 events/s => 7k records/s.   
 
  
 
  
 Starting BASELINE...  
 
  
 pool-1-thread-4 finished!!   
pool-1-thread-8 finished!!   
pool-1-thread-6 finished!!   
pool-1-thread-2 finished!!   
pool-1-thread-10 finished!!   
pool-1-thread-5 finished!!   
pool-1-thread-1 finished!!   
pool-1-thread-3 finished!!   
pool-1-thread-9 finished!!   
pool-1-thread-7 finished!!   
 
  
 BASELINE took ==================> 47.08 s  
3/25/16 6:16:00 PM =============================================================  
 
 -- Counters --------------------------------------------------------------------  
BASELINE-counter  
             count = 1000000  
 
 -- Meters ----------------------------------------------------------------------  
BASELINE-meter  
             count = 100  
         mean rate = 2.35 events/second  
     1-minute rate = 3.12 events/second  
     5-minute rate = 3.78 events/second  
    15-minute rate = 3.92 events/second  
 
  
  
  
 Starting UUID_CHAR...  
 
  
 pool-1-thread-6 finished!!   
pool-1-thread-2 finished!!   
pool-1-thread-10 finished!!   
pool-1-thread-1 finished!!   
pool-1-thread-3 finished!!   
pool-1-thread-7 finished!!   
pool-1-thread-8 finished!!   
pool-1-thread-4 finished!!   
pool-1-thread-5 finished!!   
pool-1-thread-9 finished!!   
 
  
 UUID_CHAR took ==================> 2.745 min  
3/25/16 6:18:47 PM =============================================================  
 
 -- Counters --------------------------------------------------------------------  
UUID_CHAR-counter  
             count = 1000000  
 
 -- Meters ----------------------------------------------------------------------  
UUID_CHAR-meter  
             count = 100  
         mean rate = 0.63 events/second  
     1-minute rate = 0.52 events/second  
     5-minute rate = 1.41 events/second  
    15-minute rate = 1.77 events/second  
 
  
  
  
 Starting UUID_OPTIMIZED...  
 
  
 pool-1-thread-8 finished!!   
pool-1-thread-5 finished!!   
pool-1-thread-6 finished!!   
pool-1-thread-7 finished!!   
pool-1-thread-1 finished!!   
pool-1-thread-3 finished!!   
pool-1-thread-2 finished!!   
pool-1-thread-10 finished!!   
pool-1-thread-4 finished!!   
pool-1-thread-9 finished!!   
 
  
 UUID_OPTIMIZED took ==================> 49.80 s  
3/25/16 6:19:38 PM =============================================================  
 
 -- Counters --------------------------------------------------------------------  
UUID_OPTIMIZED-counter  
             count = 1000000  
 
 -- Meters ----------------------------------------------------------------------  
UUID_OPTIMIZED-meter  
             count = 100  
         mean rate = 2.24 events/second  
     1-minute rate = 2.43 events/second  
     5-minute rate = 2.56 events/second  
    15-minute rate = 2.58 events/second  
 
  
  
  
  
*************************************************************** 
  
 
  
 
 
Old results (single thread, 500k records):  
 
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