# Logger System – SysDev‑2 High Level Design

This document summarizes the **end‑to‑end Logger System design** discussed, including architecture decisions, data modeling, failure handling, and **interview‑defense points**. It is written to be **defensible under SysDev‑2 pushback**.

---

## 1. Problem Overview

We are designing a **centralized logging system** for a large distributed environment.

### Key characteristics

* Thousands of services emitting logs
* Write‑heavy workload (≈10:1 or higher write:read)
* Logs used for debugging, incident response, audits, and postmortems
* 30‑day retention
* Horizontally scalable
* Fault tolerant under node, service, and network failures

---

## 2. Core Assumptions

* Each log entry ≈ **1 KB** (message + metadata)
* Logs are **append‑only**
* Eventual consistency is acceptable
* Silent log loss is **not acceptable**
* Duplicate logs are acceptable (at‑least‑once semantics)

Estimated volume example:

* ~10M logs/day → ~10 GB/day
* 30 days retention → ~300 GB raw data (before indexes & replication)

---

## 3. CAP Theorem Positioning

* **Partition tolerance (P)** is mandatory
* System is **AP‑leaning**
* Prioritize **write availability and durability** over strong consistency
* Logs may appear with delay; correctness over immediacy

---

## 4. High‑Level Architecture

### Separation by pipeline stage (not CRUD)

```
Producers
   ↓
Log Agent / Library
   ↓
Stateless Ingestion Service
   ↓
Kafka (Durable Buffer)
   ↓
├─ Storage Writer Consumer Group → Cassandra (Logs)
└─ Metrics Consumer Group       → Metrics DB

Reads:
Client → Query Service → (Cassandra + Search Index)
```

Key principle:

> Writes, reads, and metrics are **isolated** so failures or load in one path do not affect the others.

---

## 5. Write Path Design

### 5.1 Log Producers

* Application uses non‑blocking logging
* Logs written to stdout/file/in‑memory buffer
* Never blocks request path

### 5.2 Log Agent / Sidecar

* Collects logs locally
* Batches and retries
* Buffers temporarily on disk during failures
* Shields application from network issues

### 5.3 Ingestion Service

* Stateless, horizontally scalable
* Behind a load balancer (not a generic API Gateway)
* Responsibilities:

  * Authentication
  * Validation
  * Metadata enrichment
  * Rate limiting
* Publishes logs to Kafka

---

## 6. Durable Buffer – Kafka

Kafka is used as a **durable write‑ahead log**, not as storage.

### Why Kafka

* Durable persistence before DB
* Backpressure absorption
* Decouples producers from storage
* Handles burst traffic
* Survives service and network failures

### Guarantees

* At‑least‑once delivery
* Logs are safe once Kafka acknowledges

### Partitioning

* Kafka partition key aligns with storage:
  `(service_name + time_bucket)`
* Preserves ordering per service per time window

---

## 7. Log Storage – Cassandra

### Why Cassandra

* Extremely high write throughput
* Horizontal scalability via consistent hashing
* Peer‑to‑peer (no master/slave)
* Flexible schema for varied service metadata
* Well‑suited for time‑series data

### Cassandra Mental Model

* Partition key = **routing**
* Clustering key = **ordering**
* Schema designed by **query patterns**

### Primary Key Design

```
PRIMARY KEY ((service_name, time_bucket), timestamp DESC)
```

* **Partition key**: `(service_name, time_bucket)`
* **Clustering key**: `timestamp`

### Time Buckets

* Rounded time windows (hour/day)
* Purpose:

  * Avoid hot partitions
  * Bound partition size
  * Enable efficient range scans
  * Enable cheap retention cleanup

Bucket size chosen based on service traffic.

### Reads in Cassandra

* Service‑scoped + time‑bounded queries
* Direct routing to owning nodes
* Ordered range scans
* No scatter‑gather for primary queries

### What Cassandra is NOT used for

* Full‑text search
* Global cross‑service scans
* Heavy secondary indexes

---

## 8. Retention Strategy

### Logs (Cassandra)

* Time‑based partitions
* Drop entire partitions older than 30 days
* Avoid per‑row TTL to reduce tombstones

### Metrics DB

* TTL‑based expiration is acceptable
* Much lower volume and update rate

---

## 9. Metrics Pipeline

* Separate Kafka consumer group
* Processes logs asynchronously
* Computes per‑service metrics (counts, error rates, etc.)
* Writes aggregated metrics to a **key‑value or document DB** (e.g., DynamoDB)

Key properties:

* Metrics are **derived data**
* Best‑effort processing
* Failures do not affect ingestion

---

## 10. Read / Query Path

### Query Service

Responsibilities:

* Query validation and limits (time range caps)
* Routing queries to correct backend
* Fan‑out and aggregation
* Isolates read load from write path

### Query Routing

* Service + time only → Cassandra
* Text search / filters → Search index + hydration from Cassandra

### Read Characteristics

* Read availability is best‑effort
* During incidents, reads can be degraded to protect ingestion

---

## 11. Failure Handling Summary

### Kafka Down

* Producers buffer locally
* Rate limiting / backpressure
* Alerting

### Cassandra Down or Slow

* Kafka absorbs backlog
* Consumers lag safely
* No data loss

### Metrics DB Down

* Metrics consumer lags
* No impact on ingestion

### Duplicate Logs

* Accepted
* Optional de‑duplication using IDs if required

---

## 12. Why Not Use a Single System (e.g., Elastic Only)

* High cost at scale
* Write amplification
* Operational complexity
* Mixing ingestion durability with search concerns

Separation of:

* **Durable storage** (Cassandra)
* **Buffering** (Kafka)
* **Search** (index layer)

is deliberate.

---

## 13. Interview Defense Section (Critical)

### Common Pushbacks & Defenses

**Q: What if Kafka is down for hours?**

* Local buffering at agents
* Backpressure and rate limiting
* Alerting
* Prefer degradation over data loss

**Q: What if Cassandra is unavailable?**

* Kafka retains data
* Consumers retry
* Storage catches up later

**Q: Why accept duplicates?**

* At‑least‑once is safer than at‑most‑once
* Loss is worse than duplication for logs

**Q: How do you prevent reads from taking the system down?**

* Dedicated query service
* Query limits and fan‑out control
* Read degradation before write degradation

**Q: Why Cassandra for logs?**

* Write‑optimized, scalable, predictable performance
* Designed around time‑series access patterns

---

## 14. Final One‑Line Summary (Interview‑Grade)

> “We design the logging system as a durable ingestion pipeline: logs flow from agents through a stateless ingestion layer into Kafka for durability and backpressure, are written asynchronously to Cassandra using time‑bucketed partitions, queried via an isolated read service, and processed independently for metrics—ensuring high availability, scalability, and resilience under failure.”

---

**End of Document**
