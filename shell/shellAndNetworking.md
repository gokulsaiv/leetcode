# 🛡️ Amazon SysDE II: Technical Interview Survival Guide

## 1. The "Frequency Counter" (Log Parsing)
**Scenario:** "Find the top 3 IP addresses with the most 500 errors."

* **The Pipeline:**
    ```bash
    grep "500" /var/log/access.log | awk '{print $1}' | sort | uniq -c | sort -nr | head -n 3
    ```
* **The Breakdown:**
    * `grep "500"`: Filter for errors.
    * `awk '{print $1}'`: Extract the first column (usually the IP).
    * `sort`: Groups identical items (required for `uniq`).
    * `uniq -c`: Counts occurrences (e.g., "5 10.0.0.1").
    * `sort -nr`: Sorts **N**umerically and in **R**everse (highest count first).

---

## 2. Disk Space & File Mysteries
**Scenario:** "The disk is 100% full, but `du` shows plenty of space."

* **The "Ghost Space" Fix:**
    Find files that were deleted (`rm`) but are still being written to by a running process.
    ```bash
    lsof +L1 | grep "(deleted)"
    ```
* **The Heavy Hitters:**
    Find the top 10 largest files on the system:
    ```bash
    du -ah / | sort -rh | head -n 10
    ```
* **The Safe Clear:**
    Never `rm` a log file being written to. Instead, truncate it:
    ```bash
    echo > /var/log/app.log
    ```

---

## 3. Networking & Connectivity (OSI Layers)
**Scenario:** "SSH is hanging or a service is unreachable."

* **Layer 3 (Connectivity):** `ping <IP>`
* **Layer 4 (Port check):** `nc -zv <IP> 22` (Checks if port 22 is open/listening).
* **Layer 7 (Application):** `ssh -vvv user@host` (Verbose mode shows exactly where the handshake fails).
* **Local Bindings:** `netstat -tulpn` or `ss -tulpn` (Shows what processes are listening on which ports).

---

## 4. Process Troubleshooting
**Scenario:** "The server is slow/unresponsive."

* **Memory Usage:** `ps aux --sort=-%mem | head -n 5`
* **CPU Usage:** `ps aux --sort=-%cpu | head -n 5`
* **Zombies:** If you see `<defunct>`, you cannot kill it. You must kill the **Parent PID**.
* **Deep Dive:** Use `strace -p <PID>` to see real-time system calls (read/write/open) if a process is "stuck."

---

## 5. Python for SysDE (Memory Efficient)
**Scenario:** "Write a script to process a massive log file."

```python
# Rule: Never use .read() or .readlines() on logs (OOM risk)
counts = {}
with open('large.log', 'r') as f:
    for line in f: # Streams line-by-line
        if "ERROR" in line:
            # Logic: split by delimiter and count
            parts = line.split(" ")
            key = parts[0]
            counts[key] = counts.get(key, 0) + 1

# Top Sort logic
sorted_items = sorted(counts.items(), key=lambda x: x[1], reverse=True)
