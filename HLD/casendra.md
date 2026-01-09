# Cassandra Design Notes for Logger System (SysDev-2 HLD)

## 1. Why Cassandra for a Logging System

This logging system is:
- Write-heavy (≈10:1 or higher write-to-read ratio)
- Append-only
- Time-series in nature
- Requires high availability and horizontal scalability
- Requires flexible schema (different services emit different metadata)

Cassandra fits because:
- It provides very high write throughput using LSM-tree storage
- It scales horizontally via consistent hashing
- It supports tunable consistency
- It models time-series data efficiently when designed correctly
- It avoids single-master bottlenecks

This is an AP-leaning system under CAP:
- Partition tolerance is mandatory
- We prioritize write availability
- Eventual consistency is acceptable for logs

---

## 2. Cassandra Mental Model (Critical for Interviews)

Cassandra is NOT a relational database.

Key principles:
- Partition key = routing decision
- Clustering key = ordering within a partition
- Schema is designed by query patterns, not entities
- There is no global index
- Efficient queries MUST include the full partition key

Think of Cassandra as:
"A distributed hash table where each key maps to an ordered list of values."

---

## 3. Partition Key and Data Distribution

### Partition Key Definition

We use a **composite partition key**:

(service_name, time_bucket)

Example schema:

PRIMARY KEY ((service_name, time_bucket), timestamp)

Where:
- service_name identifies the emitting service
- time_bucket is a rounded time window (hour/day)
- timestamp is the exact log time

### How Routing Works

1. Cassandra hashes (service_name + time_bucket)
2. The hash maps to a token on the token ring
3. The token determines which node(s) store the partition
4. Writes and reads go ONLY to those nodes

There is NO cluster-wide search.

---

## 4. What is a Time Bucket and Why It Exists

A time bucket is a rounded time window such as:
- Hourly: YYYY-MM-DD-HH
- Daily: YYYY-MM-DD

Purpose of time buckets:
- Prevent hot partitions for noisy services
- Keep partition sizes bounded
- Enable efficient time-based range scans
- Make retention and deletion cheap (drop whole partitions)

Time bucket choice depends on write volume per service:
- Low traffic → daily
- Medium traffic → hourly
- Very high traffic → smaller buckets (e.g., 5–15 min)

Rule of thumb:
Keep partition size under ~100–200 MB.

---

## 5. Write Path Behavior

For each log entry:
- Client sends (service_name, timestamp, metadata, message)
- time_bucket is derived from timestamp
- Partition key is computed
- Write is routed directly to owning nodes
- Writes are sequential and fast (memtable → SSTable)

Replication:
- Replication factor (e.g., RF=3)
- Tunable consistency (e.g., LOCAL_QUORUM for durability)

This ensures:
- High write throughput
- Durability
- Availability during node failures

---

## 6. Read Path Behavior (Important Clarification)

### Primary Query Pattern

"Get logs for service X between time A and time B"

Query includes:
- service_name
- relevant time_bucket(s)
- timestamp range

What happens internally:
1. Cassandra hashes the partition key
2. Routes request to specific node(s)
3. Performs ordered range scan on timestamp
4. Returns results

This is NOT scatter-gather.
This is targeted, predictable, and fast.

### When Reads Become Expensive

Cassandra performs poorly for:
- Global queries across all services
- Full-text search
- Queries without partition key
- Heavy use of secondary indexes

These are intentionally NOT optimized.

---

## 7. Why We Avoid Secondary Indexes

Cassandra secondary indexes:
- Are node-local
- Cause scatter-gather queries
- Perform poorly with high cardinality
- Break down at scale

For logging systems:
- Thousands of services
- High write volume
- High cardinality fields

Secondary indexes are avoided.
Partition design replaces indexing.

---

## 8. Handling Retention (30 Days)

Retention is time-based:
- Data is stored in time buckets
- Buckets older than 30 days are dropped
- No row-by-row deletes
- O(1) cleanup

This avoids:
- Delete storms
- Full table scans
- Performance degradation

Optionally:
- Older logs can be archived to cold storage
- Searchable system retains only hot data

---

## 9. Consistency and Availability Tradeoff

This is an AP-leaning system:
- Write availability is prioritized
- Logs may appear with slight delay
- Eventual consistency is acceptable

Strong consistency is not required because:
- Logs are not used for transactions
- Logs are append-only
- Ordering is resolved at query time

---

## 10. Known Limitations (Explicitly Acknowledge)

Cassandra is NOT used for:
- Full-text search
- Regex or keyword queries
- Cross-service global scans

Production logging systems therefore use:
- Cassandra for durable, scalable log storage
- A separate search/index system for text queries

Acknowledging this shows maturity.

---

## 11. Interview-Grade Summary Statement

"We use Cassandra as a write-optimized, horizontally scalable log store. Logs are partitioned by service and time bucket to ensure even distribution and bounded partitions. Reads are service-scoped and time-bounded, allowing targeted range scans without scatter-gather. We prioritize availability and durability with eventual consistency, and delegate full-text search to a separate indexing system."

