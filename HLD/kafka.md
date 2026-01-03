# Kafka — Complete SDE-2 Interview Summary (Single File)

This file summarizes all Kafka concepts discussed, written at the exact level needed for SDE-2 system design interviews.

---

# 1. Kafka Partition = Durable Queue on Disk

A Kafka partition behaves like a durable, ordered queue, except messages are **not deleted** after consumption.

Messages are always appended to the end:

[ msg0 ][ msg1 ][ msg2 ][ msg3 ][ msg4 ] ...
                 ↑
           consumer offset

Key properties:
- FIFO order (per partition)
- Append-only writes
- Stored on disk (durable)
- Replayable
- Multiple consumer groups can read independently

---

# 2. Why Kafka Is Fast Even Though It Writes to Disk

Kafka’s performance comes from smart architecture choices:

### A) Append-Only Log
Kafka never modifies old data. It only appends new messages to the end of a log file.  
Sequential writes are the fastest disk operation.

### B) OS Page Cache
Kafka writes go into Linux page cache (memory).  
The OS flushes them to disk efficiently in the background → fast but durable.

### C) Batching
Producers batch many messages into one write:
[m1 m2 m3 m4] → one append  
This reduces syscalls and increases throughput massively.

### D) Zero-Copy (sendfile)
Kafka uses zero-copy to send data from disk → network socket without copying into JVM memory:
disk → kernel → socket

### E) Sequential Reads
Consumers read data in order, which is extremely efficient on SSDs due to sequential access patterns.

---

# 3. Log Segments (High-Level)

Kafka splits each partition log into multiple segment files:

00000000.log  
00001000.log  
00002000.log

Benefits:
- Easy deletion (delete whole segment)
- Supports retention
- Prevents gigantic file sizes
- Efficient to scan

---

# 4. Offsets

Offsets are simply logical positions in a partition log:
0, 1, 2, 3, 4, ...

Facts:
- Offsets belong to partitions, not global across Kafka.
- Offsets are not tied to individual consumers.
- BUT each consumer group maintains its own committed offset (progress marker).

---

# 5. Consumer Groups & Offset Storage

Offsets are stored in Kafka’s internal topic:
__consumer_offsets

Each consumer group tracks its own progress.

Example:

Partition 0 data:
[0][1][2][3][4][5][6]...

Group A offset = 105  
Group B offset = 27  

This enables:
- Independent pipelines
- Reprocessing
- Backfilling
- Analytics reading the same topic as real-time services

Kafka does NOT delete messages after consumption. Only retention policies delete them.

---

# 6. Delivery Guarantees (Consumer-Side)

Kafka’s delivery semantics depend on **when the offset is committed**.

### At-most-once
Commit BEFORE processing.
No duplicates.
BUT message loss possible.

### At-least-once
Commit AFTER processing.
No message loss.
BUT duplicates possible.
Most commonly used.

### Exactly-once
Uses idempotent producers + transactions + atomic offset commits.
No loss, no duplicates.
Highest cost, used for financial data integrity.

---

# 7. Retention & Log Compaction

Kafka deletes data only via retention, not consumption.

### A) Time-Based Retention
Delete segments older than X ms:
retention.ms = 604800000   (7 days)

### B) Size-Based Retention
Delete old segments when total topic size exceeds limit:
retention.bytes = 1GB

### C) Log Compaction
Kafka keeps only the latest record per key.
Used for state stores, user profiles, idempotent updates.

---

# 8. Zookeeper (Old Kafka Architecture)

Older Kafka versions used Zookeeper to handle:
- Broker registration
- Controller election
- Leader election for partitions
- In-Sync Replica (ISR) tracking
- Topic metadata
- Cluster membership

Zookeeper handled metadata only.  
It did NOT store messages or offsets.

### Modern Kafka (KRaft Mode)
- Removes Zookeeper entirely.
- Replaces it with Kafka’s own Raft-based metadata quorum.
- Simpler, faster, more robust.

---

# 9. Final Mental Model (Easy to Remember)

- A Kafka partition = ordered append-only log on disk.  
- Messages stay even after being consumed.  
- Offsets = consumer group pointer, not message IDs.  
- Fast writes come from sequential I/O, batching, zero-copy, and page cache.  
- Retention removes old segments, not consumption.  
- Zookeeper was metadata storage (old Kafka), replaced by KRaft.  

This is the full SDE-2 interview-ready understanding of Kafka.

