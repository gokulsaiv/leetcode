# Database Replication & Defense Guide (L5 SysDev-2)

This guide covers the mechanical differences, replication strategies, and "rebuttal" logic for the core storage layers used in high-scale system design.

---

## 1. Relational Database (SQL - PostgreSQL/MySQL)
**Best for:** Structured data, strict consistency (ACID), and complex relational queries.

### **Replication: Leader-Follower (Master-Slave)**
* **How it works:** One node (Leader) accepts all writes. It logs changes to a Write-Ahead Log (WAL) and sends them to Followers (Read Replicas).
* **Type:** Primarily **Asynchronous** (fast) or **Semi-Synchronous** (waiting for at least one follower to acknowledge).
* **Pros:** * **Single Truth:** No write conflicts; easier to maintain data integrity.
    * **Read Scaling:** Offload reads to multiple followers easily.
* **Cons:** * **Write Bottleneck:** Writes are limited to the capacity of the single Leader node.
    * **Failover Lag:** If the Leader dies, a new one must be elected (via Paxos/Raft), causing a brief write outage.



### **The L5 Defense**
* **Pushback:** "SQL doesn't scale writes horizontally like NoSQL. Why choose it?"
* **Rebuttal:** "While NoSQL scales writes easier, SQL provides the strong consistency required for ledgers or billing. We can scale writes by using **Sharding** (partitioning the data across multiple masters) or **Vertical Scaling** (moving to high-end hardware). For the 99% of applications that aren't at 'Facebook-scale,' the consistency and relational power of SQL are more valuable than premature horizontal scaling."

---

## 2. Cassandra (Column-Family / Wide-Column)
**Best for:** High-volume writes, time-series data, and multi-region availability.

### **Replication: Peer-to-Peer (Multi-Master)**
* **How it works:** There is no "Leader." Any node in the cluster can accept a write. Data is distributed via **Consistent Hashing** on a token ring.
* **Type:** **Masterless.** A write is sent to $N$ replicas; the system uses a **Quorum** ($W + R > N$) for consistency.
* **Pros:** * **High Write Availability:** No single point of failure; zero-downtime during node failures.
    * **Geo-Distribution:** Users can write to the nearest data center without waiting for a central leader halfway across the world.
* **Cons:** * **Conflict Complexity:** Simultaneous writes to the same key can lead to data divergence (handled via "Last Write Wins").
    * **Eventual Consistency:** Reads might return stale data unless you use high-consistency levels.



### **The L5 Defense**
* **Pushback:** "What if I need to search by a field that isn't the partition key?"
* **Rebuttal:** "Cassandra is designed for **Query-First Data Modeling**. If we need to search by other fields, we create **Materialized Views** (duplicate tables indexed by that field) or pipe the data into an indexing engine like **OpenSearch**. We choose Cassandra for its 'never-fail' write path, not for arbitrary ad-hoc queries."

---

## 3. DynamoDB (NoSQL - Key-Value)
**Best for:** Global scale, sub-millisecond latency, and hands-off operational management.

### **Replication: Hybrid (Leader-based per Region / Peer-to-Peer Global)**
* **How it works:** Inside a region, AWS manages a leader-follower setup across 3 Availability Zones. For **Global Tables**, it uses Peer-to-Peer replication between regions.
* **Type:** **Managed Masterless (Global).**
* **Pros:** * **Zero-Maintenance:** Scaling is handled by AWS automatically.
    * **Global Footprint:** Global Tables allow multi-region, multi-active deployments with 99.999% availability.
* **Cons:** * **Cost:** High write-throughput across global regions is expensive.
    * **Scan Limitations:** Performance degrades significantly for queries that aren't keyed.



### **The L5 Defense**
* **Pushback:** "Why use DynamoDB instead of just hosting Cassandra?"
* **Rebuttal:** "DynamoDB removes the operational burden of cluster management, node patching, and manual rebalancing. For a SysDev-2 role, I prioritize **Operational Excellence**. Using a managed service allows the team to focus on application logic while leveraging Amazon's internal expertise in high-availability partitioning."

---

## 4. MongoDB (Document Store)
**Best for:** Flexible schemas, unstructured metadata, and hierarchical data structures.

### **Replication: Leader-Follower (Replica Sets)**
* **How it works:** Uses **Replica Sets** with a single Primary. Secondary nodes replicate the Primary's `oplog`.
* **Type:** **Election-based Leader-Follower.**
* **Pros:** * **Schema Flexibility:** Store complex, nested JSON objects without migrations.
    * **Automatic Failover:** Uses the **Raft** algorithm to elect a new primary in seconds if the leader goes down.
* **Cons:** * **Write Suspension:** During a leader election, the cluster cannot accept writes.
    * **Resource Heavy:** MongoDB’s storage engine (WiredTiger) is RAM-intensive.



### **The L5 Defense**
* **Pushback:** "Why not just use a JSONB column in Postgres?"
* **Rebuttal:** "While Postgres supports JSONB, MongoDB is a **Native Document Store**. It is built for horizontal sharding from the ground up, allowing us to distribute document chunks across multiple shards. If our metadata volume is massive and unstructured, MongoDB's sharding is more mature and easier to scale than sharding a relational DB."

---

## 5. Redis (In-Memory / Buffer)
**Best for:** Caching, live tailing, and real-time coordination.

### **Replication: Leader-Follower**
* **How it works:** Asynchronous replication from a Master to Slaves. Redis Sentinel or Redis Cluster manages the failover.
* **Type:** **Asynchronous Leader-Follower.**
* **Pros:** * **Speed:** Near-zero latency since data is in RAM.
    * **Complex Structures:** Native support for Sets, Sorted Sets, and Streams.
* **Cons:** * **Volatility:** In-memory storage means data loss is possible during a crash before persistence (RDB/AOF) hits the disk.
    * **RAM Limits:** Scalability is limited by available memory.



### **The L5 Defense**
* **Pushback:** "If Redis is in-memory, what happens if the whole cluster goes down?"
* **Rebuttal:** "Redis is used for **Ephemeral State** or as a performance accelerator. For persistence, I would use **AOF (Append-Only File)** with 'everysec' sync to minimize data loss. However, for mission-critical data, Redis should always be backed by a primary persistent store like DynamoDB or SQL."

---

## Summary Decision Matrix

| Requirement | Preferred DB | Replication | Defense Keyword |
| :--- | :--- | :--- | :--- |
| **Financial/Transactions** | SQL | Leader-Follower | ACID Compliance |
| **Massive Write Throughput** | Cassandra | Peer-to-Peer | LSM-Tree Efficiency |
| **Global Scaling/No Ops** | DynamoDB | Hybrid | Fully Managed / AZ-Aware |
| **Unstructured Metadata** | MongoDB | Leader-Follower | Sharding Readiness |
| **Sub-ms Latency/Cache** | Redis | Leader-Follower | In-Memory Acceleration |
