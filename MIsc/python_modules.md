````md
# SysDev-2 Python Systems Scripting Reference (Amazon-Oriented)

This document shows **one realistic, end-to-end SysDev-style Python script** and a **compact reference of high-ROI Python modules/functions** commonly useful in Amazon SysDev / Infra interviews.

---

## End-to-End Example: Log Analysis & System Inspection Tool

```python
import os              # Filesystem: walk, path.join, path.getsize, getmtime
import re              # Regex: search, findall, compile
import json            # Data: loads (string to dict), dumps (dict to string)
import subprocess      # CLI: run(['ls', '-l']), capture_output=True
import shutil          # High-level: disk_usage, copy, move
import argparse        # Tooling: ArgumentParser, add_argument
import signal          # Reliability: handling SIGINT (Ctrl+C)
from collections import Counter, defaultdict  # Aggregation: counting, grouping
from datetime import datetime, timedelta      # Time: strptime, timedelta math

# ==========================================================
# 1. RECURSIVE FILE SEARCH & FILTER ("find" pattern)
# ==========================================================

def find_recent_large_logs(root_dir, min_size_mb=1):
    """
    Replicates `find` with filters for size and modification time.
    Uses a generator (yield) to remain memory efficient.
    """
    limit_time = datetime.now() - timedelta(days=1)

    for root, dirs, files in os.walk(root_dir):
        for name in files:
            path = os.path.join(root, name)
            try:
                if name.endswith('.log') and os.path.getsize(path) > (min_size_mb * 1024 * 1024):
                    if datetime.fromtimestamp(os.path.getmtime(path)) > limit_time:
                        yield path
            except OSError:
                # Permission denied / deleted file — common in prod systems
                continue

# ==========================================================
# 2. STREAMING LOG PARSER ("grep | awk | uniq -c")
# ==========================================================

def process_logs(file_paths):
    """
    Processes logs line-by-line (constant memory usage).
    Extracts IPs from ERROR lines and returns top offenders.
    """
    ip_counter = Counter()
    log_pattern = re.compile(r'ERROR.*?\[([\d.]+)\]')

    for path in file_paths:
        try:
            with open(path, 'r') as f:
                for line in f:
                    match = log_pattern.search(line)
                    if match:
                        ip_counter[match.group(1)] += 1
        except Exception as e:
            print(f"Skipping {path}: {e}")

    return ip_counter.most_common(5)

# ==========================================================
# 3. SYSTEM COMMAND EXECUTION (subprocess)
# ==========================================================

def get_system_metrics():
    """Runs a shell command and captures output safely."""
    result = subprocess.run(['df', '-h', '/'], capture_output=True, text=True)
    return result.stdout if result.returncode == 0 else "Error fetching disk metrics"

# ==========================================================
# 4. JSON & DISK UTILITIES
# ==========================================================

def handle_config_and_disk():
    usage = shutil.disk_usage("/")
    percent_free = (usage.free / usage.total) * 100

    raw_json = '{"status": "healthy", "version": 2.0}'
    data = json.loads(raw_json)
    return data['status'], percent_free

# ==========================================================
# 5. CLI ENTRYPOINT & SIGNAL HANDLING
# ==========================================================

if __name__ == "__main__":
    signal.signal(signal.SIGINT, lambda sig, frame: exit(0))

    parser = argparse.ArgumentParser(description="SysDev log analyzer")
    parser.add_argument("--path", required=True, help="Directory to scan")
    args = parser.parse_args()

    print(f"--- Disk Metrics ---\n{get_system_metrics()}")

    status, free = handle_config_and_disk()
    print(f"System Status: {status} | Disk Free: {free:.2f}%")

    print(f"\nScanning for large logs in: {args.path}...")
    log_files = find_recent_large_logs(args.path)
    top_ips = process_logs(log_files)

    print("\n--- Top Error-Generating IPs ---")
    if not top_ips:
        print("No errors found.")
    for ip, count in top_ips:
        print(f"IP: {ip:<15} | Occurrences: {count}")
````

---

## High-ROI Python Modules for SysDev-2 Interviews

### Filesystem & OS

* `os.walk()` — recursive directory traversal
* `os.path.exists()` / `isfile()` / `isdir()`
* `os.stat()` — file metadata
* `shutil.disk_usage()` — disk monitoring

### Process & System

* `subprocess.run()` — run shell commands safely
* `subprocess.Popen()` — streaming stdout
* `signal.signal()` — graceful shutdown
* `resource.getrusage()` — CPU / memory (advanced)

### Text & Logs

* `re.compile()` — reusable regex
* `str.split()` — fast parsing
* `collections.Counter` — frequency counts

### Time

* `datetime.now()` / `timedelta`
* `time.sleep()` — polling loops

### Data Formats

* `json.loads()` / `json.dumps()`
* `csv.reader()` — structured logs

### CLI Tools

* `argparse.ArgumentParser`
* `sys.argv` (basic)

### Networking (Nice-to-have)

* `socket` — connectivity checks
* `requests` — service health checks (if allowed)

---

## Interview Perspective (Important)

If you can:

* explain **why generators are used**
* explain **why logs are streamed line-by-line**
* explain **why subprocess is safer than os.system**

You are already **above the SysDev-2 bar**.

This document is **not for memorization**.
It is a **pattern reference**.

> Strong SysDev candidates don’t know everything — they know how to build safe, observable tools.

```
```
