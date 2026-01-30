# Autocomplete System – High Level Design (Amazon SysDev Interview)

## 1. Problem Overview

Design an autocomplete system that returns top-K suggestions in real time
as a user types a prefix.

The system must:
- Serve results with very low latency
- Handle very high read traffic
- Scale horizontally
- Remain highly available
- Tolerate eventual consistency

Autocomplete suggestions are ranked by popularity (frequency of searches / clicks).

---

## 2. Key Assumptions

- Read-heavy system
- ~50M DAU
- ~1.5–2B autocomplete queries/day
- Peak QPS ~200k
- K = 10
- Eventual consistency is acceptable
- English only
- No personalization (global ranking)

---

## 3. Why a Naive Database Approach Fails

Initial baseline:
- Store all search terms in a DB
- Query using `LIKE 'prefix%' ORDER BY score LIMIT K`

Issues:
- Prefix scans over millions of rows are expensive
- Sorting at query time increases latency
- Per-keystroke amplification overloads the DB
- Hot prefixes cause uneven load
- Disk-backed reads are too slow

Conclusion:
- Autocomplete must be **memory-first**
- Query-time computation must be eliminated

---

## 4. Core Data Structure Choice: Trie

We use a **Trie (Prefix Tree)** because:
- Prefix lookup is O(length of prefix)
- It naturally represents prefix-based search
- It avoids scanning unrelated data

### Augmentation
A plain trie is insufficient.

Each trie node stores:
- A **precomputed top-K list** of most popular full strings
- Sorted by popularity
- Fixed size (K = 10)

This converts autocomplete from:
- A runtime search problem  
to:
- A simple lookup problem

---

## 5. Read Path (Critical Path)

1. User types a prefix
2. Request hits Autocomplete Read Service
3. Service traverses the trie for the prefix
4. Returns the precomputed top-K list from the node

Properties:
- O(length of prefix)
- No DFS
- No sorting
- No DB access
- Fully in-memory
- Extremely low latency

---

## 6. Write Path and Popularity Updates

Popularity signals come from:
- User searches
- Clicks
- Impressions

Key decision:
- **Writes are asynchronous**
- Reads are never blocked by writes

A single update to a word of length L:
- Touches L trie nodes
- Write amplification = O(L × K) ≈ O(L)
- Happens **off the read path**

---

## 7. Event Collection

The read service:
- Serves autocomplete requests
- Emits events (search, click, impression)
- Does not write to DB synchronously

Events are:
- Logged / streamed
- Buffered
- Durable
- Decoupled from serving traffic

---

## 8. Service Separation (Clear Responsibilities)

### 1. Autocomplete Read Service
- Serves queries from in-memory trie
- Emits events
- No blocking writes

### 2. Event Pipeline
- Durable stream / log
- Absorbs traffic spikes
- Decouples reads from writes

### 3. Aggregation / Scoring Service
- Consumes events
- Computes popularity scores (with decay if needed)
- Writes canonical ranking data to DB
- CPU-heavy, batch/stream oriented

### 4. Database (Source of Truth)
- Stores ranked autocomplete data
- Durable
- Used for recovery and rebuilds

### 5. Trie Builder / Cache Updater
- Periodically reads from DB
- Builds a **new immutable trie**
- Does not affect live traffic during build

---

## 9. Trie Rebuild Without Downtime

Approach:
- Build new trie **asynchronously**
- Keep old trie serving traffic
- Once build completes:
  - Atomically swap pointer/reference
- Old trie is garbage-collected later

Key properties:
- No downtime
- No partial state exposure
- No locking on read path
- Reads always see a consistent trie

This is a classic:
- Double buffering
- Immutable snapshot
- Atomic pointer swap pattern

---

## 10. Memory Scaling and Sharding

Problem:
- Trie may not fit on a single machine

Solution:
- **Horizontal partitioning**
- Split trie by prefix ranges (subtrees)
- Each shard owns a subset of prefixes

---

## 11. Hot Prefix Handling

Issue:
- Some prefixes are extremely popular (e.g., "a", "pro", "amazon")

Solution:
- Replicate hot shards
- Serve reads from any healthy replica
- Balance load across replicas

---

## 12. Routing and Failure Handling

Routing approach:
- **Consistent hashing**
- Maps prefix → shard
- Minimizes data movement when nodes join/leave

Benefits:
- Handles node failures gracefully
- Supports dynamic scaling
- Avoids centralized routing bottlenecks

---

## 13. Consistency Model

- Read path: strongly consistent per snapshot
- Write path: eventually consistent
- Users may see slightly stale suggestions
- Staleness is acceptable for autocomplete

---

## 14. Final Design Summary

- Trie-based, in-memory serving
- Precomputed top-K at each node
- Async event-driven updates
- DB as source of truth
- Periodic immutable trie rebuilds
- Atomic swap for zero downtime
- Sharded and replicated for scale
- Consistent hashing for routing

This design optimizes for:
- Low latency
- High availability
- Horizontal scalability
- Operational simplicity on the read path
