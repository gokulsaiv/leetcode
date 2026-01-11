# Distributed Cache System Design (Amazon SDE-2 / SysDev-2 HLD)

This document summarizes a **complete distributed cache system design**, exactly at the depth expected in an **Amazon SDE-2 / System Development Engineer-2 HLD interview**. It is meant for **revision and recall**, not first-time learning.

---

## 1. Problem Definition

Design a **distributed in-memory cache** similar to Redis/Memcached with the following goals:

* Extremely low latency (single-digit ms)
* Read-heavy workload
* Horizontally scalable
* Highly available
* Cache is **not** the source of truth
* Data loss is acceptable, **wrong data is not**

Supported operations:

* `GET(key)`
* `PUT(key, value, ttl)`

---

## 2. Core Assumptions

### Traffic Assumptions

* Daily cache workload: **~10M operations/day**
* Read : Write ratio ≈ **10 : 1** (read-heavy)
* Average QPS ≈ **~100 QPS**
* Peak QPS (burst traffic) ≈ **~1,000 QPS** (10× peak factor)

> Design is driven by **peak QPS**, not averages.

### Data Assumptions

* Keys: strings
* Values: blobs
* Stored **entirely in memory** (disk not on read path)
* Upper bound on value size (e.g., 100KB–1MB)

---

## 3. Consistency & Availability Model

* Prioritize **Availability + Low Latency** over strict consistency
* System is **partition tolerant**
* Stale data within TTL is acceptable
* Eventual consistency is acceptable

This is an **AP system** under CAP theorem.

---

## 4. High-Level Architecture

### Components

1. **Clients / Application Services**
2. **Routing Layer / Cache Client Library**

   * Knows cluster topology
   * Routes requests to correct cache node
3. **Cache Nodes (In-Memory)**

   * Store key-value pairs
   * Handle eviction
4. **Coordinator / Control Plane**

   * Node membership
   * Health checks
   * Leader election

---

## 5. Key Distribution Strategy

### Naive Approach (Rejected)

* `hash(key) % N`
* Problem: massive re-sharding when nodes scale up/down

### Final Approach: Consistent Hashing

* Hash ring with nodes placed on the ring
* Keys are hashed and mapped clockwise to nearest node
* Adding/removing a node only reassigns ~1/N keys

Benefits:

* Minimal rebalancing
* Horizontal scalability
* Graceful node churn

---

## 6. Handling Hot Keys

### Problem

* Some keys receive disproportionately high traffic
* Single node becomes a bottleneck

### Solutions

1. **Key Sharding / Key Salting**

   * Split hot key into multiple logical keys
   * Distribute reads across nodes

2. **Replication for Hot Keys**

   * Maintain multiple replicas of hot keys
   * Reads served from any replica

### Trade-offs

* Higher write latency
* Possible temporary inconsistency

Consistency options:

* Strong consistency (write to all replicas synchronously)
* Eventual consistency (write to quorum, async replication)

---

## 7. Eviction & Cleanup Strategy

Each cache node has **limited memory** → eviction is mandatory.

### Node-Level Eviction Policies

* **LRU (Least Recently Used)**

  * Simple
  * Low overhead

* **LFU (Least Frequently Used)**

  * Retains frequently accessed keys
  * More complex (frequency tracking)

Eviction decisions are:

* Local to each node
* Independent of the routing layer

---

## 8. Replication Strategy

* Each shard/node can have **replicas**
* Leader–Follower (Primary–Replica) model

### Replication Characteristics

* Asynchronous replication
* Cache data loss acceptable on failure

Failure scenario:

* Leader crashes before replica sync → data lost
* Acceptable since cache is not source of truth

---

## 9. Node Health Monitoring

### Heartbeat Mechanism

* Coordinator periodically checks node health
* Missed heartbeats → node marked unhealthy

### Failure Handling

* Remove failed node from hash ring
* Reroute traffic
* Promote replica if applicable

---

## 10. Leader Election

Leader election logic lives in:

* Dedicated **Coordinator service**

  * or external systems like ZooKeeper / etcd

Responsibilities:

* Cluster membership
* Leader promotion
* Prevent split-brain scenarios

---

## 11. API Design (HLD-Level)

Simple, minimal APIs are sufficient.

### Create / Update Key

```
POST /cache
{
  "key": "user:123",
  "value": "...",
  "ttl": 300
}
```

Responses:

* `201 Created`
* `4xx / 5xx` on failure

### Get Key

```
GET /cache/{key}
```

Responses:

* `200 OK` + value
* `404 Not Found` if missing

> APIs are usually **not deeply evaluated** in Amazon HLD, but clarity helps.

---

## 12. Latency Metrics (p50 / p99)

### Definitions

* **p50**: Median latency (50% requests faster)
* **p99**: Tail latency (99% requests faster)

### Interview-Ready Explanation

* Monitor p50/p99 via metrics
* Set SLOs (e.g., p99 < 10ms)
* If p99 degrades:

  * Add nodes
  * Fix hot keys
  * Reduce hops

No need for special APIs for percentiles.

---

## 13. Failure Scenarios Considered

* Node crash
* Hot key overload
* Network partition
* Cache stampede
* Node scale up/down

All handled with:

* Consistent hashing
* Replication
* TTLs
* Availability-first design

---

## 14. Final Interview Summary (One-Liner)

> "This is an in-memory, distributed, highly available cache optimized for low latency and read-heavy traffic, using consistent hashing for scalability, node-level eviction for memory management, and eventual consistency with replication for fault tolerance."

---

## 15. Interviewer Expectation Check ✅

* ✔ Trade-offs discussed
* ✔ CAP theorem applied correctly
* ✔ Failure modes addressed
* ✔ No unnecessary over-engineering
* ✔ Amazon SDE-2 depth

---

**This design is COMPLETE for an Amazon SDE-2 / SysDev-2 HLD interview.**

Use this for revision, not memorization.
