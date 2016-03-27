# MysqlBenchmark       

## Setup 

- This benchmark tests read/write performance against 3 tables, which only differ by their primary key
- T1 uses bigint : an auto_increment sequence 
- T2 uses char(36) : a string representation of a type-4 UUID
- T3 uses binary(16) : a re-ordered type-1 UUID
- InnoDB includes the PK in each secondary index, so each table is set up with 3 additional unique indexes
- We start off with 10 writer threads, which try to insert 500k records in batches of 1k
- Readers can end up blocking writers in InnoDB, so we maintain a 10:1 ratio between readers and writers
    - The query used forces a range scan on the full PK index
    - Reader pause 100ms after each query

### Optimized UUID
The GUID stored in the 16-byte binary variant is a type-1 (time-based) UUID, re-ordered to generate a monotonically increasing sequence, as described [here](https://www.percona.com/blog/2014/12/19/store-uuid-optimized-way/).

```
mysql> desc t1;
+-------+---------------------+------+-----+---------+----------------+
| Field | Type                | Null | Key | Default | Extra          |
+-------+---------------------+------+-----+---------+----------------+
| id    | bigint(20) unsigned | NO   | PRI | NULL    | auto_increment |
| c1    | bigint(20)          | NO   | UNI | NULL    |                |
| c2    | bigint(20)          | NO   | UNI | NULL    |                |
| c3    | bigint(20)          | NO   | UNI | NULL    |                |
| c4    | varchar(100)        | YES  |     | NULL    |                |
| c5    | varchar(100)        | YES  |     | NULL    |                |
| c6    | varchar(100)        | YES  |     | NULL    |                |
| c7    | varchar(100)        | YES  |     | NULL    |                |
| c8    | varchar(100)        | YES  |     | NULL    |                |
| c9    | varchar(100)        | YES  |     | NULL    |                |
| c10   | varchar(100)        | YES  |     | NULL    |                |
| c11   | varchar(100)        | YES  |     | NULL    |                |
| c12   | varchar(100)        | YES  |     | NULL    |                |
| c13   | varchar(100)        | YES  |     | NULL    |                |
| c14   | varchar(100)        | YES  |     | NULL    |                |
| c15   | varchar(100)        | YES  |     | NULL    |                |
| c16   | varchar(100)        | YES  |     | NULL    |                |
| c17   | varchar(100)        | YES  |     | NULL    |                |
| c18   | varchar(100)        | YES  |     | NULL    |                |
| c19   | varchar(100)        | YES  |     | NULL    |                |
| c20   | varchar(100)        | YES  |     | NULL    |                |
+-------+---------------------+------+-----+---------+----------------+
21 rows in set (0.01 sec)

mysql> desc t2;
+-------+--------------+------+-----+---------+-------+
| Field | Type         | Null | Key | Default | Extra |
+-------+--------------+------+-----+---------+-------+
| id    | char(36)     | NO   | PRI | NULL    |       |
| c1    | char(36)     | NO   | UNI | NULL    |       |
| c2    | char(36)     | NO   | UNI | NULL    |       |
| c3    | char(36)     | NO   | UNI | NULL    |       |
| c4    | varchar(100) | YES  |     | NULL    |       |
| c5    | varchar(100) | YES  |     | NULL    |       |
| c6    | varchar(100) | YES  |     | NULL    |       |
| c7    | varchar(100) | YES  |     | NULL    |       |
| c8    | varchar(100) | YES  |     | NULL    |       |
| c9    | varchar(100) | YES  |     | NULL    |       |
| c10   | varchar(100) | YES  |     | NULL    |       |
| c11   | varchar(100) | YES  |     | NULL    |       |
| c12   | varchar(100) | YES  |     | NULL    |       |
| c13   | varchar(100) | YES  |     | NULL    |       |
| c14   | varchar(100) | YES  |     | NULL    |       |
| c15   | varchar(100) | YES  |     | NULL    |       |
| c16   | varchar(100) | YES  |     | NULL    |       |
| c17   | varchar(100) | YES  |     | NULL    |       |
| c18   | varchar(100) | YES  |     | NULL    |       |
| c19   | varchar(100) | YES  |     | NULL    |       |
| c20   | varchar(100) | YES  |     | NULL    |       |
+-------+--------------+------+-----+---------+-------+
21 rows in set (0.00 sec)

mysql> desc t3;
+-------+--------------+------+-----+---------+-------+
| Field | Type         | Null | Key | Default | Extra |
+-------+--------------+------+-----+---------+-------+
| id    | binary(16)   | NO   | PRI | NULL    |       |
| c1    | binary(16)   | NO   | UNI | NULL    |       |
| c2    | binary(16)   | NO   | UNI | NULL    |       |
| c3    | binary(16)   | NO   | UNI | NULL    |       |
| c4    | varchar(100) | YES  |     | NULL    |       |
| c5    | varchar(100) | YES  |     | NULL    |       |
| c6    | varchar(100) | YES  |     | NULL    |       |
| c7    | varchar(100) | YES  |     | NULL    |       |
| c8    | varchar(100) | YES  |     | NULL    |       |
| c9    | varchar(100) | YES  |     | NULL    |       |
| c10   | varchar(100) | YES  |     | NULL    |       |
| c11   | varchar(100) | YES  |     | NULL    |       |
| c12   | varchar(100) | YES  |     | NULL    |       |
| c13   | varchar(100) | YES  |     | NULL    |       |
| c14   | varchar(100) | YES  |     | NULL    |       |
| c15   | varchar(100) | YES  |     | NULL    |       |
| c16   | varchar(100) | YES  |     | NULL    |       |
| c17   | varchar(100) | YES  |     | NULL    |       |
| c18   | varchar(100) | YES  |     | NULL    |       |
| c19   | varchar(100) | YES  |     | NULL    |       |
| c20   | varchar(100) | YES  |     | NULL    |       |
+-------+--------------+------+-----+---------+-------+
21 rows in set (0.00 sec)


mysql> explain select sum(length(c20)) from t3 force index (primary) where id < (select max(id) from t3);
+----+-------------+-------+-------+---------------+---------+---------+------+--------+------------------------------+
| id | select_type | table | type  | possible_keys | key     | key_len | ref  | rows   | Extra                        |
+----+-------------+-------+-------+---------------+---------+---------+------+--------+------------------------------+
|  1 | PRIMARY     | t3    | range | PRIMARY       | PRIMARY | 16      | NULL | 223656 | Using where                  |
|  2 | SUBQUERY    | NULL  | NULL  | NULL          | NULL    | NULL    | NULL |   NULL | Select tables optimized away |
+----+-------------+-------+-------+---------------+---------+---------+------+--------+------------------------------+

```
    
**********************************************************************************    
####test 1 (10 writers, 100 readers, 500k records, written in batches of 1k)    
**********************************************************************************    
    
```    
Starting BASELINE...    
    
    
    
    
BASELINE took ==================> 9.718 min    
3/27/16 2:41:19 PM =============================================================    
    
-- Counters --------------------------------------------------------------------    
BASELINE-counter    
             count = 500000    
    
-- Meters ----------------------------------------------------------------------    
BASELINE-reads    
             count = 44278    
         mean rate = 75.99 events/second    
     1-minute rate = 7.26 events/second    
     5-minute rate = 161.00 events/second    
    15-minute rate = 490.35 events/second    
BASELINE-writes    
             count = 500    
         mean rate = 0.86 events/second    
     1-minute rate = 0.53 events/second    
     5-minute rate = 0.99 events/second    
    15-minute rate = 1.45 events/second    
    
    
    
    
Starting UUID_CHAR...    
    
    
    
    
UUID_CHAR took ==================> 14.52 min    
3/27/16 2:55:52 PM =============================================================    
    
-- Counters --------------------------------------------------------------------    
UUID_CHAR-counter    
             count = 500000    
    
-- Meters ----------------------------------------------------------------------    
UUID_CHAR-reads    
             count = 26914    
         mean rate = 30.89 events/second    
     1-minute rate = 3.96 events/second    
     5-minute rate = 52.98 events/second    
    15-minute rate = 310.62 events/second    
UUID_CHAR-writes    
             count = 500    
         mean rate = 0.57 events/second    
     1-minute rate = 0.48 events/second    
     5-minute rate = 0.48 events/second    
    15-minute rate = 0.34 events/second    
    
    
    
    
Starting UUID_OPTIMIZED...    
    
    
    
    
UUID_OPTIMIZED took ==================> 6.671 min    
3/27/16 3:02:33 PM =============================================================    
    
-- Counters --------------------------------------------------------------------    
UUID_OPTIMIZED-counter    
             count = 500000    
    
-- Meters ----------------------------------------------------------------------    
UUID_OPTIMIZED-reads    
             count = 27216    
         mean rate = 68.00 events/second    
     1-minute rate = 17.50 events/second    
     5-minute rate = 248.01 events/second    
    15-minute rate = 544.00 events/second    
UUID_OPTIMIZED-writes    
             count = 500    
         mean rate = 1.25 events/second    
     1-minute rate = 1.37 events/second    
     5-minute rate = 1.21 events/second    
    15-minute rate = 1.09 events/second    
```    
    
    
    
    
**********************************************************************************    
####test 2 (10 writers, 100 readers, 500k records, written in batches of 10k instead of 1k)    
**********************************************************************************    
    
```    
Starting BASELINE...    
    
    
    
    
BASELINE took ==================> 8.059 min    
3/27/16 1:36:03 PM =============================================================    
    
-- Counters --------------------------------------------------------------------    
BASELINE-counter    
             count = 500000    
    
-- Meters ----------------------------------------------------------------------    
BASELINE-reads    
             count = 51760    
         mean rate = 107.13 events/second    
     1-minute rate = 21.43 events/second    
     5-minute rate = 241.82 events/second    
    15-minute rate = 588.20 events/second    
BASELINE-writes    
             count = 50    
         mean rate = 0.10 events/second    
     1-minute rate = 0.19 events/second    
     5-minute rate = 0.09 events/second    
    15-minute rate = 0.04 events/second    
    
    
    
    
Starting UUID_CHAR...    
    
    
    
    
UUID_CHAR took ==================> 13.38 min    
3/27/16 1:49:27 PM =============================================================    
    
-- Counters --------------------------------------------------------------------    
UUID_CHAR-counter    
             count = 500000    
    
-- Meters ----------------------------------------------------------------------    
UUID_CHAR-reads    
             count = 33883    
         mean rate = 42.21 events/second    
     1-minute rate = 3.29 events/second    
     5-minute rate = 74.60 events/second    
    15-minute rate = 378.55 events/second    
UUID_CHAR-writes    
             count = 50    
         mean rate = 0.06 events/second    
     1-minute rate = 0.11 events/second    
     5-minute rate = 0.06 events/second    
    15-minute rate = 0.03 events/second    
    
    
    
    
Starting UUID_OPTIMIZED...    
    
    
    
    
UUID_OPTIMIZED took ==================> 11.96 min    
3/27/16 2:01:27 PM =============================================================    
    
-- Counters --------------------------------------------------------------------    
UUID_OPTIMIZED-counter    
             count = 500000    
    
-- Meters ----------------------------------------------------------------------    
UUID_OPTIMIZED-reads    
             count = 80904    
         mean rate = 112.78 events/second    
     1-minute rate = 14.31 events/second    
     5-minute rate = 104.51 events/second    
    15-minute rate = 336.35 events/second    
UUID_OPTIMIZED-writes    
             count = 50    
         mean rate = 0.07 events/second    
     1-minute rate = 0.20 events/second    
     5-minute rate = 0.09 events/second    
    15-minute rate = 0.04 events/second    
```    
    
    
    
    
**********************************************************************************    
####test 3 : 10 writers, 100 readers, 100k records, written in batches of 1k    
**********************************************************************************    
    
```    
Starting BASELINE...    
    
    
    
    
BASELINE took ==================> 2.206 min    
3/27/16 2:04:31 PM =============================================================    
    
-- Counters --------------------------------------------------------------------    
BASELINE-counter    
             count = 100000    
    
-- Meters ----------------------------------------------------------------------    
BASELINE-reads    
             count = 28972    
         mean rate = 219.41 events/second    
     1-minute rate = 231.04 events/second    
     5-minute rate = 626.30 events/second    
    15-minute rate = 769.19 events/second    
BASELINE-writes    
             count = 100    
         mean rate = 0.76 events/second    
     1-minute rate = 0.81 events/second    
     5-minute rate = 1.53 events/second    
    15-minute rate = 1.82 events/second    
    
    
    
    
Starting UUID_CHAR...    
    
    
    
    
UUID_CHAR took ==================> 2.225 min    
3/27/16 2:06:45 PM =============================================================    
    
-- Counters --------------------------------------------------------------------    
UUID_CHAR-counter    
             count = 100000    
    
-- Meters ----------------------------------------------------------------------    
UUID_CHAR-reads    
             count = 19162    
         mean rate = 143.61 events/second    
     1-minute rate = 170.57 events/second    
     5-minute rate = 564.09 events/second    
    15-minute rate = 709.91 events/second    
UUID_CHAR-writes    
             count = 100    
         mean rate = 0.75 events/second    
     1-minute rate = 0.60 events/second    
     5-minute rate = 0.24 events/second    
    15-minute rate = 0.09 events/second    
    
    
    
    
Starting UUID_OPTIMIZED...    
    
    
    
    
UUID_OPTIMIZED took ==================> 1.546 min    
3/27/16 2:08:18 PM =============================================================    
    
-- Counters --------------------------------------------------------------------    
UUID_OPTIMIZED-counter    
             count = 100000    
    
-- Meters ----------------------------------------------------------------------    
UUID_OPTIMIZED-reads    
             count = 19309    
         mean rate = 208.22 events/second    
     1-minute rate = 314.66 events/second    
     5-minute rate = 691.41 events/second    
    15-minute rate = 801.04 events/second    
UUID_OPTIMIZED-writes    
             count = 100    
         mean rate = 1.08 events/second    
     1-minute rate = 0.95 events/second    
     5-minute rate = 0.99 events/second    
    15-minute rate = 1.00 events/second    
```    
    
    
    
    
**********************************************************************************    
####test 4 (10 writers, 100 readers, 100k records, written in batches of 1k)    
**********************************************************************************    

```    
Starting BASELINE...    
    
    
    
    
BASELINE took ==================> 2.272 min    
3/27/16 2:13:05 PM =============================================================    
    
-- Counters --------------------------------------------------------------------    
BASELINE-counter    
             count = 100000    
    
-- Meters ----------------------------------------------------------------------    
BASELINE-reads    
             count = 29039    
         mean rate = 213.53 events/second    
     1-minute rate = 217.33 events/second    
     5-minute rate = 620.53 events/second    
    15-minute rate = 770.45 events/second    
BASELINE-writes    
             count = 100    
         mean rate = 0.74 events/second    
     1-minute rate = 0.80 events/second    
     5-minute rate = 1.52 events/second    
    15-minute rate = 1.81 events/second    
    
    
    
    
Starting UUID_CHAR...    
    
    
    
    
UUID_CHAR took ==================> 2.285 min    
3/27/16 2:15:22 PM =============================================================    
    
-- Counters --------------------------------------------------------------------    
UUID_CHAR-counter    
             count = 100000    
    
-- Meters ----------------------------------------------------------------------    
UUID_CHAR-reads    
             count = 19582    
         mean rate = 142.88 events/second    
     1-minute rate = 161.58 events/second    
     5-minute rate = 553.76 events/second    
    15-minute rate = 703.34 events/second    
UUID_CHAR-writes    
             count = 100    
         mean rate = 0.73 events/second    
     1-minute rate = 0.61 events/second    
     5-minute rate = 0.24 events/second    
    15-minute rate = 0.09 events/second    
    
    
    
    
Starting UUID_OPTIMIZED...    
    
    
    
    
UUID_OPTIMIZED took ==================> 1.603 min    
3/27/16 2:16:58 PM =============================================================    
    
-- Counters --------------------------------------------------------------------    
UUID_OPTIMIZED-counter    
             count = 100000    
    
-- Meters ----------------------------------------------------------------------    
UUID_OPTIMIZED-reads    
             count = 19764    
         mean rate = 205.52 events/second    
     1-minute rate = 295.68 events/second    
     5-minute rate = 677.37 events/second    
    15-minute rate = 791.82 events/second    
UUID_OPTIMIZED-writes    
             count = 100    
         mean rate = 1.04 events/second    
     1-minute rate = 0.78 events/second    
     5-minute rate = 0.40 events/second    
    15-minute rate = 0.27 events/second    
```    
    
    
*********************************************************************************************    
     
####Write-only workload 

The following analysis is from a WRITE-ONLY workload with 10 threads. No reads at all.     
    
BASELINE => pk is a bigint(8)       
UUID_CHAR => pk is char(36)       
UUID_OPTIMIZED => pk is binary(16)       
     
p.s.: this workload is for a single table, with 3 AK unique indexes in addition to the PK.         
p.p.s.: each "event" below refers to a batch of 10k rows. So 0.7 events/s => 7k records/s.       
     
      
     
```      
 Starting BASELINE...      
     
           
      
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
```     
      
      
      
      
***************************************************************     
      
     
####Single-threaded write-only workload      
     
     
Old results (single thread, 500k records):      

```     
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
```
