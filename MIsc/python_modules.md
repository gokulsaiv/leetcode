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
# 1. THE RECURSIVE FILE SEARCH & FILTER PATTERN (FIND)
# ==========================================================
def find_recent_large_logs(root_dir, min_size_mb=1):
    """
    Replicates 'find' with filters for size and modification time.
    Uses 'yield' to be memory efficient (Generator).
    """
    limit_time = datetime.now() - timedelta(days=1)
    
    for root, dirs, files in os.walk(root_dir):
        for name in files:
            path = os.path.join(root, name)
            try:
                # Filter by extension, size (> 1MB), and time (last 24h)
                if name.endswith('.log') and os.path.getsize(path) > (min_size_mb * 1024 * 1024):
                    if datetime.fromtimestamp(os.path.getmtime(path)) > limit_time:
                        yield path
            except OSError: 
                # Essential for SysDev: handle permission denied/missing files
                continue 

# ==========================================================
# 2. THE MEMORY-EFFICIENT LOG PARSER (STREAMING)
# ==========================================================
def process_logs(file_paths):
    """
    Replicates 'grep | awk | uniq -c'.
    Processes files line-by-line to handle GIGABYTE-sized logs.
    """
    ip_counter = Counter()
    # Pattern: Look for IP inside brackets [] after an ERROR tag
    # r'' = raw string, .*? = non-greedy match, [\d.]+ = digits and dots
    log_pattern = re.compile(r'ERROR.*?\[([\d.]+)\]')

    for path in file_paths:
        try:
            with open(path, 'r') as f:
                for line in f:  # The "Streaming" part - constant memory usage
                    match = log_pattern.search(line)
                    if match:
                        ip = match.group(1)
                        ip_counter[ip] += 1
        except Exception as e:
            print(f"Skipping {path}: {e}")
    
    return ip_counter.most_common(5) # Returns Top 5 sorted by frequency

# ==========================================================
# 3. SYSTEM COMMAND HANDLING (SUBPROCESS)
# ==========================================================
def get_system_metrics():
    """
    Demonstrates running shell commands and capturing output.
    """
    # capture_output=True grabs stdout/stderr, text=True returns string
    result = subprocess.run(['df', '-h', '/'], capture_output=True, text=True)
    if result.returncode == 0:
        return result.stdout
    return "Error fetching disk metrics"

# ==========================================================
# 4. JSON & DIRECTORY UTILITIES
# ==========================================================
def handle_config_and_disk():
    """
    Checks disk usage via shutil and parses JSON strings.
    """
    # High-level disk check
    usage = shutil.disk_usage("/")
    percent_free = (usage.free / usage.total) * 100
    
    # JSON Parsing (Standard for log config or API responses)
    raw_json = '{"status": "healthy", "version": 2.0}'
    data = json.loads(raw_json)
    return data['status'], percent_free

# ==========================================================
# 5. CLI TOOL STRUCTURE & EXECUTION (ARGPARSE)
# ==========================================================
if __name__ == "__main__":
    # Signal handling for graceful exit (Ctrl+C)
    signal.signal(signal.SIGINT, lambda sig, frame: exit(0))

    parser = argparse.ArgumentParser(description="SysDev log analyzer")
    parser.add_argument("--path", required=True, help="Directory to scan")
    args = parser.parse_args()

    # 1. Check system state
    print(f"--- Disk Metrics ---\n{get_system_metrics()}")
    
    status, free = handle_config_and_disk()
    print(f"System Status: {status} | Disk Free: {free:.2f}%")

    # 2. Find and Process logs
    print(f"\nScanning for large logs in: {args.path}...")
    log_files = find_recent_large_logs(args.path)
    top_ips = process_logs(log_files)
    
    # 3. Final Report
    print("\n--- Top Error-Generating IPs ---")
    if not top_ips:
        print("No errors found.")
    for ip, count in top_ips:
        print(f"IP: {ip:<15} | Occurrences: {count}")
