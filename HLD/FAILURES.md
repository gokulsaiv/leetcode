# Database Read & Write Failure Modes – L5 Interview Pushbacks

This document is intentionally adversarial.
It is written in the voice of a skeptical L5/L6 interviewer whose job is to stress your system until it breaks.
If you can defend against most of these, you are thinking at the right depth.

---

## 1. Hot Keys & Skew

### Problem

A small subset of keys receives a disproportionate amount of reads or writes.
Examples: celebrity user, popular product, global counter, feature flag.

### Failure Modes

* Single shard CPU saturation
* Lock contention
* Tail latency explosion
* Replica lag amplification

### Interview Pushbacks

* Why does consistent hashing not save you here?
* What happens if the hot key is 1% of keys but 90% of traffic?
* What breaks first: CPU, lock manager, or network?

### Mitigations

* Key sharding (logical fan-out)
* Write-behind aggregation
* Probabilistic counters (HyperLogLog, sketches)
* Cache with request coalescing

---

## 2. Read-Write Amplification

### Problem

A single logical operation fans out into many physical IOs.

### Failure Modes

* Disk IO saturation
* Compaction storms
* Cache eviction of useful data

### Interview Pushbacks

* How many disk writes does one logical write generate?
* What happens during compaction + peak traffic?
* Can your SLA survive worst-case amplification?

### Mitigations

* LSM tuning (memtable size, compaction strategy)
* Batch writes
* Column-family separation

---

## 3. Write Contention & Locks

### Problem

Multiple writers compete for the same rows / ranges.

### Failure Modes

* Lock queues
* Deadlocks
* Priority inversion

### Interview Pushbacks

* Are locks pessimistic or optimistic?
* How do retries affect tail latency?
* Can two transactions starve each other?

### Mitigations

* Optimistic concurrency control
* Versioned writes
* Append-only models

---

## 4. Read-After-Write Consistency

### Problem

Client expects to read its own write immediately.

### Failure Modes

* Replica lag
* Cache inconsistency
* User-visible anomalies

### Interview Pushbacks

* Which replica serves the read?
* What if the leader crashes after ACK but before replication?
* Do you guarantee monotonic reads?

### Mitigations

* Read-your-writes routing
* Session stickiness
* Quorum reads

---

## 5. Replica Lag

### Problem

Followers fall behind the leader.

### Failure Modes

* Stale reads
* Sudden thundering herd on leader
* Failover serving old data

### Interview Pushbacks

* How do you measure lag?
* What happens during network jitter?
* Do you ever stop serving reads from replicas?

### Mitigations

* Adaptive read routing
* Bounded staleness
* Backpressure on writes

---

## 6. Thundering Herd on Reads

### Problem

Many clients request the same missing cache key.

### Failure Modes

* DB overload
* Cache stampede
* Cascading failure

### Interview Pushbacks

* What if the key expires at midnight for everyone?
* Does your cache support request coalescing?

### Mitigations

* Mutex locks in cache
* Early refresh
* Probabilistic TTL jitter

---

## 7. Write Acknowledgement Semantics

### Problem

Client receives ACK but durability is unclear.

### Failure Modes

* Phantom writes
* Data loss during crash
* Double writes on retry

### Interview Pushbacks

* What does ACK actually mean?
* Is data on disk or memory?
* How do retries stay idempotent?

### Mitigations

* Write-ahead logging
* Idempotency keys
* Exactly-once semantics (bounded)

---

## 8. Network Partitions

### Problem

Nodes cannot communicate.

### Failure Modes

* Split brain
* Divergent histories
* Lost writes

### Interview Pushbacks

* Which side continues writes?
* How do you reconcile conflicts?
* Is your system CP or AP?

### Mitigations

* Leader election with fencing tokens
* Vector clocks
* Conflict resolution strategies

---

## 9. Failover & Leader Election

### Problem

Leader crashes or becomes unreachable.

### Failure Modes

* Long unavailability window
* Multiple leaders
* Stale leader serving writes

### Interview Pushbacks

* How long is your RTO?
* What if old leader comes back?
* Who decides the new leader?

### Mitigations

* Consensus (Raft / Paxos)
* Epoch-based fencing
* Health-based quorum

---

## 10. Disk Full

### Problem

Storage reaches capacity.

### Failure Modes

* Writes fail silently
* WAL cannot append
* Node crashes

### Interview Pushbacks

* Do reads still work?
* What happens to in-flight writes?
* How early do you detect this?

### Mitigations

* Disk watermarks
* Write shedding
* Auto-expansion / tiering

---

## 11. Slow Queries & Tail Latency

### Problem

Rare slow queries dominate P99.

### Failure Modes

* Thread pool exhaustion
* Head-of-line blocking

### Interview Pushbacks

* Why does average latency look fine?
* What blocks the fast requests?

### Mitigations

* Query timeouts
* Separate pools for slow paths
* Hedged reads

---

## 12. Schema Changes

### Problem

Online schema migration under traffic.

### Failure Modes

* Locking tables
* Partial reads
* Data corruption

### Interview Pushbacks

* Can old and new schema coexist?
* How do you rollback?

### Mitigations

* Backward-compatible schemas
* Dual writes
* Expand–migrate–contract

---

## 13. Cache Invalidation

### Problem

Keeping cache and DB consistent.

### Failure Modes

* Stale reads
* Over-invalidation

### Interview Pushbacks

* Why is invalidation hard?
* What if invalidation message is lost?

### Mitigations

* Write-through cache
* TTL + eventual correctness
* Versioned cache keys

---

## 14. Batch Writes & Partial Failure

### Problem

Some writes in a batch succeed, others fail.

### Failure Modes

* Inconsistent state
* Client confusion

### Interview Pushbacks

* Do you retry entire batch?
* How do you expose partial success?

### Mitigations

* Per-item status
* Transaction boundaries

---

## 15. Clock Skew

### Problem

Nodes disagree on time.

### Failure Modes

* Incorrect ordering
* TTL misfires

### Interview Pushbacks

* Why not trust timestamps?
* What breaks if clocks drift?

### Mitigations

* Logical clocks
* Hybrid clocks

---

## 16. Backpressure & Load Shedding

### Problem

System is overloaded.

### Failure Modes

* Queue buildup
* OOM
* Cascading failures

### Interview Pushbacks

* Where do you apply backpressure?
* Which clients get rejected first?

### Mitigations

* Bounded queues
* Priority-based shedding

---

## 17. Multi-Region Writes

### Problem

Writes across geographies.

### Failure Modes

* High latency
* Conflicts
* Data divergence

### Interview Pushbacks

* Is write latency acceptable?
* How do you resolve conflicts?

### Mitigations

* Single-writer per key
* CRDTs
* Geo-partitioning

---

## 18. Data Corruption

### Problem

Bits rot, disks lie.

### Failure Modes

* Silent corruption
* Inconsistent replicas

### Interview Pushbacks

* How do you detect corruption?
* Which replica is correct?

### Mitigations

* Checksums
* Merkle trees
* Anti-entropy repairs

---

## 19. Retry Storms

### Problem

Clients retry aggressively.

### Failure Modes

* Traffic amplification
* Feedback loops

### Interview Pushbacks

* Do retries make things worse?
* How do you cap retries?

### Mitigations

* Exponential backoff
* Jitter
* Circuit breakers

---

## 20. Observability Gaps

### Problem

You cannot see what is failing.

### Failure Modes

* Blind debugging
* Wrong fixes

### Interview Pushbacks

* What metrics detect hot keys?
* How do you trace a slow read?

### Mitigations

* Per-key metrics sampling
* Distributed tracing

---

## Final L5 Meta-Pushback

> "You’ve described solutions. Now tell me what you *acceptably let fail*."

Strong designs do not eliminate failure.
They choose *which* failures are allowed, *where*, and *with what blast radius*.

If you can explain that calmly… you’re thinking like an L5.
