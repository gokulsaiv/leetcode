# Social Media Feed System — SysDev-2 HLD Summary

This document summarizes the complete High-Level Design discussion for a
Social Media Feed system, focused on Amazon SysDev-2 (L5) expectations.
The design prioritizes clarity, scalability, and realistic tradeoffs.

---

## 1. Problem Statement

Design a social media feed system (Twitter / Instagram-like) where users can:
- Create posts (text + optional image)
- Follow other users
- View a feed of recent posts from people they follow

Feed should:
- Be reverse chronological
- Be low-latency for reads
- Scale to millions of users
- Tolerate slight delays in consistency

---

## 2. Assumptions & Constraints

- ~10M Daily Active Users
- ~1M posts per day
- Read-heavy system (reads >> writes)
- Users can follow up to thousands of users
- Some users (celebrities) have millions of followers
- Eventual consistency is acceptable
- Availability is preferred over strong consistency

---

## 3. CAP Theorem Choice

- Partition tolerance is mandatory (distributed system)
- Choose **Availability over Consistency**
- Slightly stale feeds are acceptable
- Users being unable to load feed is NOT acceptable

**Result**: AP system with eventual consistency

---

## 4. Core Design Decision: Feed Generation Strategy

### Fanout
Fanout means copying a new post into the feeds of followers.

Two extremes:
- Fanout-on-write → fast reads, expensive writes
- Fanout-on-read → cheap writes, slow reads

### Chosen Approach: Hybrid

- **Normal / active users**:
  - Fanout-on-write
  - Precompute feeds asynchronously
- **High-fanout (celebrity) users**:
  - Fanout-on-read
  - Avoid pushing to millions of feeds
- **Inactive users**:
  - No precomputation
  - Feed built on-demand

This avoids write amplification while keeping reads fast.

---

## 5. User Activity Classification

Primary signal:
- **Last active time**

Strategy:
- Users active within last ~30–45 days:
  - Feed is precomputed and cached
- Inactive users:
  - Feed computed on first read
  - Cached with short TTL

This saves compute and cache resources.

---

## 6. Data Separation (Very Important)

### Feed System Stores (Metadata Only)
- post_id
- user_id
- timestamp
- text
- image URL(s)

**Per post size**: ~5 KB

### Media System Stores
- Actual image bytes (up to ~500 KB)
- Stored in object storage (S3-like)
- Served via CDN

Feed system NEVER stores image bytes.

---

## 7. Capacity Estimation (Feed Metadata Only)

- 1M posts/day × 5 KB ≈ 5 GB/day
- 90-day retention ≈ 450 GB raw
- With replication + indexes (~3×):
  - ~1–1.5 TB total

This is manageable and scalable.

---

## 8. High-Level Components

### API Gateway
- Authentication
- Authorization
- Rate limiting
- Request routing

### Load Balancers
- Distribute traffic across service instances
- Health checks and failover

### Core Services
- **Post Service**: handles post creation
- **Feed Service**: serves user feeds
- **Feed Builder Service**: constructs feeds and updates cache
- **Media Service**: processes and stores images

---

## 9. Data Stores

### Follow Graph
- KV store or document DB
- Key: user_id
- Value: list of followee_ids
- Optimized for fast lookups (no joins)

### Posts Store
- Wide-column DB (e.g., Cassandra)
- Partition: user_id
- Cluster: timestamp DESC
- Append-heavy, time-ordered reads

### Feed Store
- Wide-column DB (Cassandra)
- Partition: user_id
- Cluster: timestamp DESC
- Stores precomputed feed entries

### Cache
- KV store (Redis/Memcache-like)
- Key: feed:{user_id}
- Value: top-K recent feed items
- TTL-based

---

## 10. Write Path (Post Creation Flow)

1. Client sends post request to Post Service
2. If image exists:
   - Image sent to Media Service
   - Media Service stores image in object storage
   - Returns image URL
3. Post metadata (text + image URL) stored in Posts DB
4. Post Service publishes an event to message queue (Kafka-like)
5. Background feed workers consume event:
   - Fetch follower list
   - Update feeds for eligible users
   - Skip or treat high-fanout users differently

Fanout is **asynchronous** to avoid blocking writes.

---

## 11. Read Path (Feed Fetch Flow)

1. Client requests feed from Feed Service
2. Feed Service checks cache
3. Cache hit:
   - Return feed immediately
4. Cache miss:
   - Feed Builder Service invoked
   - For active users:
     - Read from Feed DB
   - For inactive / high-fanout users:
     - Aggregate on-demand from Posts DB
   - Result cached
   - Feed returned

Reads are synchronous, fast, and cache-first.

---

## 12. Staleness & Consistency

- Feeds may be delayed by seconds or minutes
- Ordering may not be globally perfect
- Different devices may briefly see different feeds
- Empty feeds are avoided by fallback computation

This is acceptable given AP choice.

---

## 13. Hot Key Problem (Awareness)

Hot keys can occur for:
- Celebrity users
- Very popular feed keys

Mitigation:
- Avoid fanout for high-fanout users
- Use fanout-on-read for those users
- Optionally shard or special-case their feeds

Mentioned when relevant, not over-engineered.

---

## 14. Rate Limiting

Applied at API Gateway:
- Per user / per token
- Protects against abuse and spikes
- Ensures downstream stability

---

## 15. Summary of Key Tradeoffs

- Fast reads over perfect freshness
- Availability over strong consistency
- Async processing over sync fanout
- Metadata in feed DB, media in object storage
- Precompute only for users where it matters

---

## 16. Interview Framing Tip

Always answer using:
- Decision
- Reason
- Tradeoff

Example:
"I use async fanout because reads dominate. The tradeoff is slight delay in feed updates."

---

## Final Note

This design meets SysDev-2 expectations:
- Clear core decisions
- Correct tradeoffs
- Scalable and realistic
- No over-engineering


Client
  ↓
API Gateway
  ↓
Post Service → Posts DB
  ↓
Kafka
  ↓
Feed Builder
  ↓
Feed Store → Cache
  ↓
Feed Service → Client

