# Indexing for Amazon L5 (SDE II) / L6 (Senior) System Design Interviews

This guide is structured to help you perform at the Amazon L5 (SDE II) / L6 (Senior) level. At this level, interviewers move beyond "what is an index" to "trade-offs, internals, and failure modes." You are expected to choose the right tool based on read/write patterns, consistency requirements, and latency constraints.

---

## 1. The Core Engines: How Data is Actually Stored

### A. B+ Trees (The SQL Standard)

Used in: MySQL (InnoDB), PostgreSQL, Oracle.

* Structure: A balanced tree where all data/pointers to data reside in leaf nodes. Internal nodes only store keys for navigation. Leaf nodes are linked (doubly linked list) for fast sequential access.
* Best For: Read-heavy systems, Range queries (SELECT * FROM orders WHERE date > '2023-01-01').
* Time Complexity: O(\log N) for search, insert, and delete.
* L5 Insight:
  * Why B+ over B-Tree? B+ Trees store more keys in internal nodes (since they don't store data there), leading to a "shorter" tree (less height). This means fewer disk I/O operations to reach data.
  * Fill Factor: Pages are rarely 100% full to allow updates without immediate splitting.

---

### B. LSM Trees (Log-Structured Merge Trees)

Used in: Cassandra, DynamoDB, RocksDB, HBase.

* Structure: Optimized for high write throughput.
  * MemTable: Writes go to an in-memory balanced tree (or skip list).
  * SSTable (Sorted String Table): When MemTable is full, it flushes to disk as an immutable, sorted file.
  * Compaction: Background process merges old SSTables to discard deleted/overwritten data.
* Best For: Write-heavy workloads (IoT logs, Chat history, clickstreams).
* L5 Insight (The "Gotcha"):
  * Write Amplification: One logical write results in multiple physical writes (flush + compaction levels).
  * Read Penalty: Reads are slower than B-Trees because the system must check the MemTable and all SSTables. Optimization: Uses Bloom Filters to quickly check if a key exists in an SSTable before scanning it.

---

### C. Skip Lists

Used in: Redis (Sorted Sets - ZSET).

* Structure: A probabilistic data structure built on multiple layers of linked lists. Higher layers skip fewer elements.
* Why not a Balanced Tree? Skip lists are easier to implement and require less locking overhead during concurrent updates compared to rebalancing a Red-Black or AVL tree.
* Time Complexity: Average O(\log N).

---

### D. Inverted Indexes

Used in: Elasticsearch, Lucene, Solr.

* Structure: Maps content (words/tokens) to their location (document IDs).
  * Token: "Amazon" -> DocIDs: [1, 5, 99]
* Best For: Full-text search, complex filtering (e.g., "Find products with 'Blue' AND 'Wireless'").

---

## 2. SQL vs. NoSQL Indexing Strategies

### SQL (Relational)

* Clustered Index: The physical order of rows on the disk is the index. A table can have only one clustered index (usually the Primary Key).
* Non-Clustered Index: A separate structure (B-Tree) containing the Index Key and a pointer to the main table (Clustered Index Key).
* Composite Index: Indexing multiple columns (e.g., (Last_Name, First_Name)).
  * Leftmost Prefix Rule: An index on (A, B, C) can serve queries for A or A, B but not for B or C alone.
* Covering Index (The Performance Hack): If an index contains all the columns requested in the SELECT query, the DB serves data directly from the index (RAM) without touching the main table (Disk). This is an Index Only Scan.

---

### NoSQL (DynamoDB & Cassandra focus)

NoSQL databases require you to design indexes before you write code, based strictly on access patterns.

---

### A. DynamoDB (Amazon Favorite)

* LSI (Local Secondary Index):
  * Constraint: Must be defined at table creation time. Cannot be added/removed later.
  * Scope: Uses the same Partition Key as the base table but a different Sort Key.
  * Storage: Stored in the same partition as the data. Strong consistency is possible.
* GSI (Global Secondary Index):
  * Constraint: Can be added/deleted anytime.
  * Scope: Can have a different Partition Key and Sort Key.
  * Storage: Replicated to its own partition space. Eventually Consistent only.
  * L5 Tip: GSIs have their own Read/Write Capacity Units (RCU/WCU). If a GSI is throttled, it can block writes to the main table!

---

### B. Cassandra

* Secondary Indexes: Generally considered an anti-pattern for high-cardinality data.
  * Why? A query on a secondary index requires hitting every node (Scatter-Gather), which kills performance.
* Materialized Views: The preferred alternative. It creates a new, read-optimized table automatically updated by Cassandra.
  * Trade-off: duplicated storage and write latency (DB has to write to two tables).

---

## 3. Specialized Indexing (The "Expert" Zone)

### Spatial Indexing (Location Data)

If asked to design Uber/Grab/Yelp:

* Geohash: Encodes lat/long into a string (e.g., "u4pru"). Points share prefixes if they are close. Good for basic proximity.
  * Problem: Edge cases at grid boundaries (two close points might have totally different hashes).
* Quadtree: Recursively divides a 2D map into 4 quadrants. Adaptive resolution (dense areas have deeper trees).
  * Best for: Dynamic environments where user density varies significantly (e.g., Cities vs. Rural).
* R-Tree: Groups objects using bounding rectangles. Standard in PostGIS.

---

### Bitmap Indexes

* Use Case: Low cardinality columns (Gender, Status: Active/Inactive) in Data Warehouses.
* Internal: Uses bit arrays (0 or 1) for each distinct value.
* Trap: Terrible for write-heavy OLTP systems because updating a bitmap often locks the entire row/segment.

---

## 4. Interview Cheat Sheet: Trade-offs & Pitfalls

| Concept | The "L5/L6 Answer" |
|---|---|
| Write Heavy System | Use LSM Trees (Cassandra/DynamoDB). Avoid too many indexes (each index add = extra write). |
| Read Heavy System | Use B+ Trees or Covering Indexes. Consider caching (Redis) for hot rows. |
| High Cardinality | B-Trees are great. Bitmap indexes are terrible. |
| Pagination | OFFSET is slow (scans N rows). Use Seek Method (Keyset Pagination) via indexes: WHERE id > last_seen_id LIMIT 10. |
| Consistency | DynamoDB LSIs allow strong consistency; GSIs are eventual. Know this distinction. |
| Bloom Filters | Used to avoid disk lookups for keys that don't exist. Tunable false positive rate (trade memory for accuracy). |

---

## 5. Sample Amazon Scenario Question

Interviewer: "We need to query orders by UserID (fast) and also by OrderDate (fast). The table is huge. How do we design this in DynamoDB?"

Bad Answer:  
"Just add an index on OrderDate."

L5 Answer:  
"Since DynamoDB is partition-based, querying effectively requires hitting a specific partition.

* Primary Key: UserID (Partition Key) + OrderID (Sort Key) for fast user lookups.
* Access Pattern: If we need to find all orders on a specific date globally, we need a GSI with OrderDate as the Partition Key.
* Hot Partition Issue: If OrderDate is the key, today's date will get ALL traffic (hot key). We might need to 'shard' the date (e.g., 2023-10-27_suffix) to distribute writes."

---

## Next Step

Would you like me to walk through a specific System Design Diagram (e.g., "Designing a search for an E-commerce catalog") applying these indexing strategies?
