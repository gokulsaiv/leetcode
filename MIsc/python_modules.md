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
# 1. THE RECURSIVE FILE SEARCH & FILTER PATTERN
# ==========================================================
def find_recent_large_logs(root_dir, min_size_mb=1):
    limit_time = datetime.now() - timedelta(days=1)
    
    for root, dirs, files in os.walk(root_dir):
        for name in files:
            path = os.path.join(root, name)
            try:
                # Filter by extension, size, and time
                if name.endswith('.log') and os.path.getsize(path) > (min_size_mb * 1024 * 1024):
                    if datetime.fromtimestamp(os.path.getmtime(path)) > limit_time:
                        yield path
            except OSError: continue # Skip files we can't access

# ==========================================================
# 2. THE MEMORY-EFFICIENT LOG PARSER (STREAMING)
# ==========================================================
def process_logs(file_paths):
    ip_counter = Counter()
    # Pattern: Look for IP inside brackets [] after an ERROR tag
    # Uses raw string r'' and non-greedy .*?
    log_pattern = re.compile(r'ERROR.*?\[([\d.]+)\]')

    for path in file_paths:
        with open(path, 'r') as f:
            for line in f:  # Streams line-by-line (Constant Memory)
                match = log_pattern.search(line)
                if match:
                    ip = match.group(1)
                    ip_counter[ip] += 1
    
    return ip_counter.most_common(5) # Returns Top 5

# ==========================================================
# 3. SYSTEM COMMAND HANDLING (SUBPROCESS)
# ==========================================================
def get_system_metrics():
    # Use list for command, capture_output for result, text=True for string
    result = subprocess.run(['df', '-h', '/'], capture_output=True, text=True)
    if result.returncode == 0:
        return result.stdout
    return "Error fetching disk metrics"

# ==========================================================
# 4. JSON & DIRECTORY UTILITIES
# ==========================================================
def handle_config_and_disk():
    # Disk space check using shutil
    usage = shutil.disk_usage("/")
    percent_free = (usage.free / usage.total) * 100
    
    # JSON Parsing
    raw_json = '{"status": "healthy", "version": 2.0}'
    data = json.loads(raw_json)
    return data['status'], percent_free

# ==========================================================
# 5. CLI TOOL STRUCTURE (ARGPARSE)
# ==========================================================
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="SysDev log analyzer")
    parser.add_argument("--path", required=True, help="Path to logs")
    args = parser.parse_args()

    # Execution flow
    print(f"Checking Disk: {get_system_metrics()}")
    
    log_files = find_recent_large_logs(args.path)
    top_ips = process_logs(log_files)
    
    for ip, count in top_ips:
        print(f"IP: {ip} | Errors: {count}")
