# System Design: Database Selection Cheat Sheet

This guide breaks down the core database families, their internal mechanics, and exactly when to use them in a system design interview.

## 1. Relational / SQL (PostgreSQL, MySQL)
**Core Characteristics:** ACID compliant, rigid schema, stores data in rows and columns, uses B-Tree indexes, scales vertically (scaling horizontally requires complex sharding). Strong consistency.
*   **When to Use:** When data has strict relationships, requires complex `JOIN`s, or demands absolute transactional integrity (financials).
*   **When NOT to Use:** Unstructured data, evolving schemas, or massive read/write scales where joining tables across shards becomes a bottleneck.
*   **Design Point of View:** Use when queries rely on filtering, aggregating, and joining multiple entities.
*   **Example Data (E-commerce Ledger):**
    *   `Users` table: `id`, `name`, `email`
    *   `Orders` table: `order_id`, `user_id` (FK), `total`, `status`
    *   *Query:* "Get all completed orders for User 123 and sum the total."

## 2. Key-Value (DynamoDB, Riak)
**Core Characteristics:** Distributed hash table. Data is accessed *exclusively* via a Primary Key (Partition Key + optional Sort Key). highly scalable, predictable single-digit millisecond latency.
*   **When to Use:** When access patterns are extremely simple and known in advance. Lookups are strictly by ID. Great for session storage, shopping carts, and user profiles.
*   **When NOT to Use:** If you need to query by non-primary key attributes frequently, or if you need complex relational joins.
*   **Design Point of View:** Use when your entire query is `GET /data/{id}` or `PUT /data/{id}`. The data itself is treated as a single opaque blob/JSON.
*   **Example Data (User Session Store):**
    *   `Key`: `session_token_9876`
    *   `Value`: `{ "user_id": "123", "expires": "2026-09-21T12:00:00Z", "cart_items": [...] }`

## 3. Wide-Column (Cassandra, ScyllaDB)
**Core Characteristics:** Masterless (decentralized ring) architecture, highly available (AP in CAP theorem). Uses Log-Structured Merge (LSM) trees, making write speeds astronomically fast. Data is partitioned by a Partition Key and ordered on disk by a Clustering Key.
*   **When to Use:** Heavy write-intensive workloads (writes outnumber reads), time-series logs, or when you need multi-region active-active high availability. 
*   **When NOT to Use:** If you need ACID transactions, `JOIN`s, or if you don't know your read queries in advance.
*   **Design Point of View:** In Cassandra, you *must* model your tables around your read queries, not your data relationships. One query = one table.
*   **Example Data (Messaging Chat History):**
    *   `Partition Key`: `channel_id` (distributes data across nodes)
    *   `Clustering Key`: `timestamp` DESC (sorts messages on disk)
    *   *Query:* `SELECT * FROM messages WHERE channel_id = 'eng-team' ORDER BY timestamp DESC LIMIT 50;` (Blazing fast because it reads a contiguous block on disk).

## 4. Document / NoSQL (MongoDB, Couchbase)
**Core Characteristics:** Stores data as flexible JSON/BSON documents. Schemaless. Supports rich secondary indexes (unlike pure Key-Value stores). 
*   **When to Use:** When data structure varies between records, schema changes rapidly, or you want to store a deeply nested object that would require 5 SQL tables to reconstruct.
*   **When NOT to Use:** For highly connected graph data or strict financial ledgers.
*   **Design Point of View:** Use when an entity is accessed as a complete aggregate.
*   **Example Data (Product Catalog):**
    *   Document 1: `{ "id": "p1", "type": "TV", "specs": { "resolution": "4K", "refresh_rate": "120Hz" }}`
    *   Document 2: `{ "id": "p2", "type": "Shirt", "specs": { "size": "L", "material": "Cotton" }}`
    *   *Note:* Doing this in SQL requires messy Entity-Attribute-Value (EAV) anti-patterns. Document DBs handle this natively.

## 5. Time-Series (InfluxDB, Prometheus, TimescaleDB)
**Core Characteristics:** Optimized purely for timestamped data. Uses specialized compression (like Gorilla compression) to store floats/timestamps. Built-in functions for time-windowing, downsampling, and data retention (auto-deleting old data).
*   **When to Use:** System metrics, IoT sensor data, stock market ticks, application telemetry.
*   **When NOT to Use:** Updating or deleting individual records. Time-series data is append-only by nature.
*   **Design Point of View:** Use when every single data point represents a state at a specific millisecond, and you need to aggregate them (e.g., P99 latency over the last 5 minutes).
*   **Example Data (Server CPU Monitoring):**
    *   `Timestamp`: `1695214800`
    *   `Tags` (Indexed): `host=server-1, region=us-east`
    *   `Fields` (Unindexed values): `cpu_usage=85.4, mem_usage=42.1`

## 6. In-Memory Cache (Redis, Memcached)
**Core Characteristics:** Stores data entirely in RAM. Sub-millisecond latency. Redis supports advanced data structures (Hashes, Sets, Sorted Sets, Bitmaps). Ephemeral by default (though Redis can persist).
*   **When to Use:** Database query caching, rate limiting, real-time leaderboards, pub/sub message brokering, geo-spatial lookups.
*   **When NOT to Use:** As a primary system of record for critical data (unless strict persistence is configured, which impacts performance), or for datasets larger than available RAM.
*   **Design Point of View:** Use strictly as a performance layer in front of a slower primary database, or for counting/ranking operations that would choke a standard DB.
*   **Example Data (Real-time Gaming Leaderboard via Redis Sorted Set):**
    *   `ZADD global_leaderboard 5000 "player_1"` (Adds player_1 with score 5000)
    *   `ZREVRANGE global_leaderboard 0 9` (Instantly fetches top 10 players, completely bypassing the SQL database).

---

### Quick Decision Matrix

| Requirement | Primary Choice | Why? |
| :--- | :--- | :--- |
| **Strict ACID & Joins** | PostgreSQL / MySQL | Enforces relational integrity natively. |
| **Simple Lookups at massive scale** | DynamoDB | Predictable latency; scales infinitely by Partition Key. |
| **Heavy Writes / High Availability** | Cassandra | LSM trees absorb writes; masterless ring prevents downtime. |
| **Flexible Schema / Catalogs** | MongoDB | BSON documents allow nested, varying attributes. |
| **Metrics / Telemetry** | InfluxDB | Built-in downsampling and time-window compression. |
| **Sub-millisecond access / Counting** | Redis | In-memory speeds; native Sorted Sets and counters. |