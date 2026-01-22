# SQL Design Strategy (PostgreSQL / MySQL)

## 1. Core Mechanics: B-Tree & WAL
- **Storage**: Data is stored in B-Trees. Every write requires updating the tree and the index, which involves random I/O.
- **Durability**: Uses a Write-Ahead Log (WAL). The write is confirmed once it hits the sequential log, even if the B-Tree update happens later.

## 2. When to Choose SQL
- **Complex Joins**: You need to combine data from multiple tables frequently.
- **ACID Compliance**: You are handling transactions (e.g., Payments, Ledger) where "eventual consistency" is a failure.
- **Predictable Schema**: The data structure is stable and fits a tabular format.

## 3. L5 Scaling Strategy: Partitioning & Sharding
- **Vertical Partitioning**: Moving "heavy" columns (like a `blob`) to a separate table to keep the main B-Tree lean.
- **Horizontal Partitioning (Range/List)**: Splitting a large table into smaller physical chunks based on a key (e.g., `user_id` or `created_at`).
- **Sharding**: Moving partitions to different physical nodes to overcome the CPU/RAM limits of a single master.

## 4. The "Defense" (Handling Pushback)
- **Interviewer**: "SQL doesn't scale as well as NoSQL. Why use it?"
- **Defense**: "While NoSQL scales horizontally more easily, SQL provides strong consistency and relational integrity out of the box. By using Read Replicas for traffic and Table Partitioning for manageability, we can scale SQL to handle millions of users while maintaining 100% data accuracy."


# DynamoDB Design Strategy (Amazon Native)

## 1. Core Mechanics: Consistent Hashing
- **Distribution**: Uses the Partition Key (PK) to hash data across physical partitions. Each partition is ~10GB.
- **Performance**: O(1) lookups for single items. Performance remains constant whether you have 1GB or 100TB.

## 2. When to Choose DynamoDB
- **Massive Concurrency**: You expect spikes (e.g., Prime Day) and don't want to manage cluster resizing.
- **Simple Query Patterns**: You mostly fetch data by a unique ID or a "Prefix" search.
- **Serverless Alignment**: You want a fully managed service with built-in TTL and backups.

## 3. L5 Scaling Strategy: Adaptive Capacity & GSI
- **Global Secondary Indexes (GSI)**: Used to support secondary query patterns. GSIs are eventually consistent and have their own throughput.
- **Adaptive Capacity**: DynamoDB automatically moves "hot" items to different partitions if one partition is being hammered.


# Document DB Design Strategy (MongoDB)

## 1. Core Mechanics: BSON & WiredTiger
- **Storage**: Stores data as BSON (Binary JSON). This allows for "Flexible Schema" where documents in the same collection can have different fields.
- **Compression**: The WiredTiger engine provides excellent data compression, saving disk space compared to SQL.

## 2. When to Choose Document DB
- **Hierarchical Data**: Your data naturally fits a tree structure (e.g., a Product Catalog with varied attributes).
- **Rapid Prototyping**: The schema is evolving quickly and you don't want the downtime of SQL `ALTER TABLE` commands.
- **High Volume Writes**: Generally faster than SQL because it can be tuned for "Fire and Forget" (unacknowledged) writes.

## 3. L5 Scaling Strategy: Sharding & Replica Sets
- **Replica Sets**: Standard for High Availability (Primary-Secondary-Arbiter).
- **Shard Key Selection**: Crucial. A poor shard key (like a monotonically increasing ID) creates a "Hot Shard." You need a high-cardinality key for even distribution.

## 4. The "Defense" (Handling Pushback)
- **Interviewer**: "Why not just use a JSONB column in PostgreSQL?"
- **Defense**: "While PostgreSQL supports JSONB, MongoDB is built from the ground up to shard documents across clusters. If our volume exceeds what a single high-end SQL instance can handle, MongoDB’s native horizontal sharding is more robust than a DIY SQL sharding implementation."

## 4. The "Defense" (Handling Pushback)
- **Interviewer**: "What if we need to run a complex report on this data?"
- **Defense**: "DynamoDB is an OLTP (Transaction) store, not an OLAP (Analytics) store. For reporting, I would enable DynamoDB Streams to pipe data into S3/Athena or OpenSearch. We don't compromise the performance of the live application for the sake of internal reporting."


# Redis Design Strategy

## 1. Core Mechanics: Single-Threaded RAM
- **Storage**: Data lives entirely in RAM. Persistence (RDB/AOF) is optional.
- **Performance**: 100k+ operations per second on a single thread because there is no disk I/O and no "Lock Contention."

## 2. When to Choose Redis
- **Caching**: Storing results of expensive DB queries or API calls.
- **Rate Limiting**: Using atomic counters (e.g., `INCR`) to protect downstream services.
- **Session Management**: Storing temporary user state that must be shared across multiple app servers.

## 3. L5 Strategy: Data Structures & Eviction
- **Sorted Sets (ZSET)**: Perfect for leaderboards or priority queues.
- **Eviction Policies**: `allkeys-lru` (Least Recently Used) is standard for caches to ensure the memory doesn't overflow.

## 4. The "Defense" (Handling Pushback)
- **Interviewer**: "Redis is in-memory. What if the node crashes?"
- **Defense**: "Redis should be used for transient data or data that can be re-hydrated from a primary DB. For high availability, I would use Redis Sentinel or ElastiCache with Multi-AZ enabled. If 100% persistence is required, Redis is the wrong tool; it's a performance accelerator, not a source of truth."
