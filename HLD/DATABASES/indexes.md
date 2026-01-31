# L5 Indexing Cheat Sheet – Access Pattern vs Storage Engine Tradeoffs

This is a revision-focused guide for answering indexing questions at an L5 level. It is structured around two axes:

* **Access Pattern** — how data is read
* **Storage Engine Behavior** — how data is written and maintained

Goal: explain not just *what* an index does, but **what breaks first**, **where IO goes**, and **which workloads it favors**.

---

# 1. B+Tree Index (Read-Optimized Default)

If the interviewer does not specify a database or index type, assume **B+Tree**.

## Structure Logic

* Balanced tree
* Internal nodes store keys only
* Leaf nodes store actual row pointers (or data in clustered indexes)
* Leaf nodes are linked as a sorted linked list

## Access Strengths

Best for:

* Equality lookups: `id = 5`
* Range queries: `age BETWEEN 20 AND 30`
* ORDER BY queries
* Prefix searches

Range scans are efficient because leaf nodes are sequentially linked.

## Write Path Behavior

Writes are **in-place updates**:

* Traverse tree root → leaf
* Insert into leaf
* If full → node split
* Split may propagate upward
* Multiple random disk IOs possible

## L5 Depth Points

### Composite Index & Left Prefix Rule

For index (A, B, C):

* Efficient: A
* Efficient: A + B
* Efficient: A + B + C
* Not efficient: B alone
* Not efficient: C alone

Because tree ordering is lexicographic on leading columns.

### Clustered vs Secondary

* Clustered index stores table data at leaf
* Secondary index stores pointer → extra lookup (double read)

### Covering Index

If all requested columns exist in index → no table lookup → major read win.

## Failure Modes Under Load

* Write-heavy workloads cause page splits
* Random IO dominates
* Lock contention on hot ranges

## Tradeoff Summary

* Excellent for reads and ranges
* Predictable latency
* Write amplification from splits + rebalancing

---

# 2. LSM Tree (Write-Optimized Engine Index)

Used in: Cassandra, RocksDB, LevelDB, Scylla, Dynamo-style stores.

## Structure Logic

Never updates in place.

Write flow:

1. Write to WAL (durability)
2. Insert into MemTable (sorted in memory)
3. When MemTable fills → flush to disk as SSTable (sorted file)
4. Background compaction merges SSTables

All disk writes are **sequential**.

## Access Strengths

Best for:

* High write throughput
* Time-series data
* Logs
* Metrics
* Event ingestion

## Read Path Behavior

Reads check multiple layers:

* MemTable
* Recent SSTables
* Older SSTables

Without optimization → many disk reads per lookup.

## L5 Depth Points

### Read Amplification

One logical read may check many SSTables.
This increases:

* Disk IO
* Tail latency

### Bloom Filters

Per-SSTable probabilistic filter:

* Fast “key definitely not here” test
* Avoids unnecessary disk reads
* False positives allowed, false negatives not allowed

### Compaction Strategy Matters

Compaction merges files and removes tombstones.

Types:

* Size-tiered — better write throughput
* Leveled — better read performance

### Tombstones & Deletes

Deletes are markers, not removals.
Actual deletion happens during compaction.

## Failure Modes Under Load

* Compaction storms → IO spikes
* Read latency variance
* Disk bandwidth saturation

## Tradeoff Summary

* Excellent write throughput
* Sequential IO friendly
* Variable read latency
* Background compaction cost

---

# 3. Inverted Index (Search & Multi-Value Specialist)

Used in: Elasticsearch, Lucene, Postgres GIN, search engines.

## Structure Logic

Maps **value → list of document IDs**.

Example:

```
"error" → [doc2, doc8, doc91]
"timeout" → [doc1, doc2]
```

Instead of scanning documents, we jump directly to matches.

## Access Strengths

Best for:

* Full text search
* Tag filtering
* JSON / array fields
* Multi-valued attributes

Supports:

* contains
* match
* token search

## Write Path Behavior

Writes are expensive:

* Tokenize document
* Update posting lists for each term
* Many index updates per document write

## L5 Depth Points

### Posting Lists

Each term stores sorted docID list → supports fast boolean ops:

* AND
* OR
* NOT
  via list intersection/union.

### Segment Merge

Indexes are written in segments and merged later (similar spirit to LSM compaction).

### Scoring & Ranking Layer

Search engines add TF-IDF / BM25 scoring — not just lookup.

## Failure Modes Under Load

* Heavy update workloads degrade performance
* Large memory need for term dictionaries
* Segment merge IO spikes

## Tradeoff Summary

* Excellent for search/filter
* Terrible for frequent updates
* Storage heavy

---

# 4. Hash Index (Exact Lookup Specialist)

Used in: Redis, in-memory DBs, some Postgres hash indexes.

## Structure Logic

Key → hash → bucket → record pointer

Essentially a hashmap persisted to memory/disk.

## Access Strengths

Best for:

* Exact equality lookups
* Unique keys
* Primary key fetch

Lookup is constant time on average.

## Write Path Behavior

* Compute hash
* Insert into bucket
* Handle collisions (chaining or open addressing)

## L5 Depth Points

### Collision Handling Matters

Performance depends on:

* Hash quality
* Load factor
* Resize policy

### No Ordering

Keys are not sorted → no range scan possible.

### Resize Cost

Rehashing large tables is expensive and blocking unless incremental.

## Failure Modes Under Load

* Memory pressure
* Collision chain growth
* Rehash latency spikes

## Tradeoff Summary

* Fastest equality lookup
* No range support
* Memory sensitive

---

# L5 Comparison Matrix

| Index Type | Best For           | Read Behavior               | Write Behavior      | Main Cost       | Scaling Pattern    |
| ---------- | ------------------ | --------------------------- | ------------------- | --------------- | ------------------ |
| B+Tree     | General relational | O(log n), strong for ranges | In-place, random IO | Page splits     | Read-heavy         |
| LSM Tree   | Logs, time-series  | Variable, multi-level reads | Sequential append   | Compaction IO   | Write-heavy        |
| Inverted   | Text / JSON search | Posting list scans          | Multi-term updates  | Index bloat     | Distributed search |
| Hash       | Exact match        | O(1) average                | Simple insert       | Memory & resize | Memory-bound       |

---

# Interview Power Close Line

When explaining any index at L5 depth, always state:

* What workload it favors
* Where amplification occurs (read or write)
* What background maintenance costs exist
* What query types it cannot serve efficiently

That converts a definition answer into a tradeoff answer — which is what senior interviews actually score.

