
![Caching Diagram](HLD/cache.png)
📌 Top Caching Strategies (From ByteByteGo)

Here are the five core strategies:

Read paths:

Cache-Aside

Read-Through

Write paths:

Write-Around

Write-Through

Write-Back
(The diagram in the post shows how they interplay.) 
blog.bytebytego.com

🧠 1. Cache-Aside (Lazy Loading)

Flow:
The application checks the cache first.
If the data is not present (cache miss), it loads from the database, stores it in the cache, then returns to the client.
Next access will hit the cache. 
blog.bytebytego.com

Advantages
• Simple and flexible: the app controls caching logic directly. 
blog.bytebytego.com

• Good for read-heavy workloads where data doesn’t change often. 
blog.bytebytego.com

• Cache can fail without crashing the system because fallback is direct DB access. 
blog.bytebytego.com

Disadvantages
• More complex application code — you have to manage both cache and database. 
blog.bytebytego.com

• Data can get stale if the database is updated independently (consistency issue). 
blog.bytebytego.com

🧠 2. Read-Through

Flow:
The cache is responsible for reads.
If the key is missing, the cache automatically fetches from storage, fills itself, then returns the data. The app just reads from the cache. 
blog.bytebytego.com

Advantages
• Simplifies application logic (app never deals with the database). 
blog.bytebytego.com

• Good for high read throughput with many misses — cache keeps itself warmed. 
blog.bytebytego.com

Disadvantages
• Cache is tightly coupled with storage logic — more complex cache implementation. 
blog.bytebytego.com

• Harder to tune and maintain consistency if write patterns change frequently. 
blog.bytebytego.com

🧠 3. Write-Around

Flow:
Writes bypass the cache and go directly to the database.
Cache isn’t updated on writes; it will only get filled when a subsequent read happens. 
blog.bytebytego.com

Advantages
• Avoids unnecessary cache churn for data that isn’t immediately read after being written. 
blog.bytebytego.com

• Good for write-heavy systems where the data isn’t reused soon. 
LinkedIn

Disadvantages
• Cold cache after writes — first reads will miss and hit the main DB, causing latency spikes. 
blog.bytebytego.com

🧠 4. Write-Through

Flow:
Every write goes to both the cache and the database at the same time.
The write is considered complete only after both succeed. 
blog.bytebytego.com

Advantages
• Strong consistency — cache always reflects the current database state. 
LinkedIn

• Reads are faster because the data is always in cache after write. 
LinkedIn

Disadvantages
• Higher write latency — two writes instead of one. 
LinkedIn

• Costlier if write frequency is high. 
LinkedIn

🧠 5. Write-Back (Deferred Writes)

Flow:
Writes go to the cache first (fast).
The cache lazily flushes changes back to the database asynchronously — often in batches. 
blog.bytebytego.com

Advantages
• Very low write latency — app returns quickly. 
LinkedIn

• Batching writes can reduce database load, good for high-throughput services. 
LinkedIn

Disadvantages
• Data loss risk if cache fails before writing back to DB. 
blog.bytebytego.com

• Harder to reason about consistency because cache and DB can diverge temporarily. 
blog.bytebytego.com

🧠 Comparison Summary

| Strategy       | Best For                  | Pros                          | Cons                                 |
|----------------|---------------------------|-------------------------------|---------------------------------------|
| Cache-Aside    | Read-heavy apps           | Simple, flexible              | Code complexity, stale data risk      |
| Read-Through   | Many cache misses         | App gets simpler              | Cache-store coupling                  |
| Write-Around   | Write-heavy, low reuse    | Less cache churn              | Cold cache, more DB reads             |
| Write-Through  | Strong consistency        | Cache always up to date       | Higher write cost/latency             |
| Write-Back     | High write throughput     | Very fast writes              | Data loss risk, complexity            |


