# 🚀 L5 SysDev II / SDE II Interview Cheat Sheet

## 1. Networking & Debugging Toolbelt
*Use these to move from Layer 2 (Physical) to Layer 7 (Application).*

| Tool | Key Flags | Scenario / Use Case |
| :--- | :--- | :--- |
| **`ss`** | `-tunlp` | See who is listening. Check if bound to `127.0.0.1` vs `0.0.0.0`. |
| **`ip route`** | `get <ip>` | Check which interface/gateway the OS uses for a destination. |
| **`nc` (netcat)**| `-zv <ip> <port>` | Fast "Port Open" check. Success = Service Up; Timeout = Firewall. |
| **`tcpdump`** | `-i any host <ip>` | The "Truth." See if SYN is sent and if SYN-ACK comes back. |
| **`arp` / `ip n`**| `-an` | Check Layer 2. If it's `<incomplete>`, the host is physically gone. |
| **`traceroute`**| `-n -T -p <port>` | Find where the packet dies. `!H` means the gateway can't find the host. |
| **`strace`** | `-p <pid>` | Trace system calls. See if a "hung" process is stuck on a file/socket. |
| **`lsof`** | `-i :<port>` | Find exactly which PID owns a port. |

---

## 2. Distributed Patterns: The Saga (L5 Depth)
*For coordinating transactions across microservices without 2PC.*

### **Core Mental Model**
* **No Global Rollback:** You use **Compensating Transactions** (undoing $T_1$ by running $C_1$).
* **Orchestration:** Central brain, better for complex flows (4+ steps).
* **Choreography:** Event-driven, better for simple flows. Risks "Event Spaghetti."

### **The "Must-Haves"**
1. **Idempotency:** Every service must handle retries without duplicate side effects (use `Saga_ID` + `Step_ID`).
2. **Observability:** Pass **Correlation IDs** in every header to trace the flow across services.
3. **Timeouts:** If the Orchestrator doesn't hear back, it *must* assume failure and trigger compensation.
4. **Semantic Locking:** Since Sagas lack isolation, mark records as `PENDING` to prevent other processes from using them.

---

## 3. Database Indexing: Performance & Trade-offs
*Interviewers care about "Read-Heavy" vs "Write-Heavy" decisions.*

| Index Type | Mechanics | Best For... | The "L5" Trade-off |
| :--- | :--- | :--- | :--- |
| **B+Tree** | Balanced Tree + Linked Leaves | Range queries, ID lookups. | High "Write Amplification" due to node splits. |
| **LSM-Tree** | MemTable + SSTable (Appends) | High-volume writes (Logs/Metrics). | High "Read Amplification" (needs Bloom Filters). |
| **Inverted** | Word -> [Document IDs] | Full-text search, Tag filtering. | Very slow to update/delete. |
| **Composite** | Index on multiple columns | Multi-filter queries. | Must follow **Left-Prefix Rule**. Order matters! |

---

## 4. The "L5" Problem-Solving Framework
*When asked to debug or design, follow this "Senior" path:*

### **Step 1: Isolate the Layer**
* **Connection Refused?** Host is up, service is down/not listening.
* **Connection Timeout?** Firewall (Security Group) or local `iptables` DROP.
* **No Route to Host?** Routing table issue or ARP failure (Host is physically gone).

### **Step 2: Check the "Blast Radius"**
* If a disk is full, don't just delete logs. Check the **Log Rotation policy**.
* If a service is slow, don't just restart it. Look at **p99 latency** and **I/O Wait**.

### **Step 3: Disagree and Commit**
* **The Story:** "My manager suggested [Manual Process], but I advocated for [Automated System] because manual processes don't scale. I built a POC to prove the value, but I committed to the team's needs during the build."

---

## 5. Critical Interview Scenarios

### **Scenario: "No route to host"**
1. **Check Routing:** `ip route get <dest>`.
2. **Check ARP:** `arp -an`. If incomplete, the host isn't responding to Layer 2.
3. **Traceroute:** Find the last responding hop. If it ends in `!H`, that gateway can't find the host.

### **Scenario: "Thundering Herd"**
* **Problem:** 10,000 clients retry a failed connection at the exact same time, crashing the DB.
* **Fix:** Implement **Exponential Backoff with Jitter**.

### **Scenario: "The Zombie Process"**
* **Problem:** `ps` shows a process that won't die.
* **Analysis:** It's likely in **Uninterruptible Sleep (D state)**, waiting on dead NFS/Disk I/O. `kill -9` won't work; you must fix the underlying I/O.
