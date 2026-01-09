# SysDev-2 Shell Scripting Cheat Sheet (High-ROI)

This sheet focuses on **file discovery, text filtering, resource monitoring, and safe command chaining**.
No fluff. Only interview-relevant commands and options.

---

## 1. find (File Discovery) — CORE

### Basic
find . -name 'test_*.py'
find /root -type f -iname '*.sh'

### Important Options
-name      # glob pattern (NOT regex)
-iname     # case-insensitive
-type f    # files only
-type d    # directories only
-mtime -7  # modified in last 7 days
-mtime +30 # modified more than 30 days ago
-newermt   # filter by date string
-print0    # null-separated output (SAFE)

### Interview-Safe Examples
find . -name '*.sh' -type f 2>/dev/null
find . -name '*.sh' -mtime -30
find . -name '*.sh' -newermt '2024-06-01'

---

## 2. grep (Filtering Text)

### Core Flags
-i   # ignore case
-w   # whole word
-v   # invert match
-E   # extended regex (use | without escaping)
-l   # print filenames only
-z   # handle null-separated input

### Common Patterns
grep -Ei '\b(the|that|then|those)\b'
grep -v 'ERROR'
grep -l 'TODO' *.sh

### Interview Tip
Prefer:
grep -iwE "word1|word2"
over complex regex unless needed.

---

## 3. awk (Field Extraction + Math) — VERY IMPORTANT

### Basics
awk '{print $1}'          # first column
awk '{print $NF}'         # last column
awk '{sum+=$1} END {print sum}'

### Float Math (Bash CANNOT do this)
awk "BEGIN {print 100 - 98.2}"

### Filtering
awk '$3=="all" {print $13}'

---

## 4. sed (Simple Text Transformation)

### Core Use
sed 's/old/new/'
sed '/pattern/d'

### Examples
sed 's/foo/bar/g'
sed '/DEBUG/d'

(Interviews expect **basic sed**, not wizardry.)

---

## 5. sort / uniq (Aggregation)

### sort
sort
sort -n        # numeric
sort -r        # reverse
sort -k2,2     # sort by column 2

### uniq
uniq
uniq -c        # count
uniq -d        # duplicates only

### Common Pattern
sort | uniq -c | sort -nr

---

## 6. xargs (Text → Arguments) — CRITICAL

### What it does
Converts stdin into command-line arguments.

### Unsafe (breaks on spaces)
find . -name '*.sh' | xargs cat

### SAFE (always prefer)
find . -name '*.sh' -print0 | xargs -0 cat
find . -name '*.tmp' -print0 | xargs -0 rm

### Interview Rule
Prefer `find -exec` over `xargs` when safety matters.

---

## 7. find -exec (BEST PRACTICE)

### Syntax
-exec <command> {} +

### Examples
find . -name '*.sh' -exec cat {} +
find . -name '*.tmp' -exec rm {} +

### Why Interviewers Like It
- Safe for spaces
- No argument splitting issues
- Clear intent

---

## 8. Arrays in Bash (Collecting Results)

### Best Way
mapfile -t files < <(find . -name '*.sh')

### Access
${files[@]}     # all elements
${#files[@]}    # length

### Pipe Array to Command
printf "%s\n" "${files[@]}" | cat

### Pass as Arguments
cat -- "${files[@]}"

---

## 9. CPU Monitoring (VM-Focused)

### top (Interactive → Batch)
top -bn1

### Extract CPU Idle
top -bn1 | awk '/Cpu\(s\)/ {print $8}'

### Main CPU Usage
CPU = 100 - idle

### mpstat (Preferred)
mpstat
mpstat 1
mpstat -P ALL 1

### vmstat (VM Gold)
vmstat 1
# Look at: us sy id wa st

### Interview Insight
High `st` (steal time) = hypervisor contention.

---

## 10. Bash Gotchas (MUST KNOW)

### Spaces Matter
[ $a -lt 5 ]   # correct
[$a -lt 5]     # WRONG

### Numeric vs String
-lt -gt -eq    # numbers
<  >           # strings (inside [ ])

### Bash Has NO Floats
$((1.8))       # INVALID

### Float → Int
printf "%.0f\n" "$x"

---

## 11. Redirections (Daily Use)

> file       # overwrite
>> file      # append
2> err.log   # stderr
&> all.log   # stdout + stderr
2>/dev/null  # silence errors

---

## Interview Gold Sentences (Use These)

- "I avoid parsing ls; I use find with -exec for safety."
- "Bash doesn't support floats, so I delegate math to awk."
- "On VMs, I always check CPU steal time."
- "xargs converts stdin into command-line arguments."

---

## Final Rule (Remember This)
Pipes move TEXT.
xargs / -exec move FILES.
Choose correctly.

---
