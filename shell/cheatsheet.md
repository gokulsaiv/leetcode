# SysDev-2 Bash Cheat Sheet (Interview-Oriented)

> **Goal:** Practical commands and patterns you’ll actually use in SysDev-2 interviews: monitoring, filtering logs, sanitizing configs, and safe file operations.

Think in **pipelines**:

```
produce data → filter → transform → summarize
```

---

## 🔍 `grep` — filter lines

**Mental model:** Keep only the lines you care about.

### High-ROI flags

* `-i` ignore case
* `-w` whole word
* `-v` invert match
* `-E` extended regex (`|`, `+`, `?`)

### Common patterns

```
grep ERROR app.log
grep -i error app.log
grep -v DEBUG app.log
grep -w error app.log
grep -E 'error|fail|timeout' app.log
```

---

## ✂️ `cut` — extract columns (simple)

**Mental model:** Give me column X.

### Fields

```
cut -d',' -f1          # CSV
cut -d':' -f1,3        # passwd-like
cut -f1-3              # TSV (tab is default)
```

### Characters

```
cut -c1-4              # first 4 chars
cut -c5-               # from char 5 to end
```

---

## ✏️ `sed` — stream edit (search/replace)

**Mental model:** Modify text on the fly.

### Core form

```
s<delim>pattern<delim>replacement<delim>flags
```

### High-ROI patterns

```
sed 's/foo/bar/'                 # replace first
sed 's/foo/bar/g'               # replace all
sed 's/=.*$/=******/'           # mask secrets
sed '/^#/d'                     # delete comments
sed '/^$/d'                     # delete empty lines
sed -n '5,10p'                  # print lines 5–10
sed 's/[[:space:]]\+/ /g'       # normalize spaces
```

### Delimiter trick (paths)

```
sed 's|/usr/bin|/opt/bin|'
```

---

## 🧮 `awk` — logic + math (per line)

**Mental model:** If condition → do action.

### Core form

```
awk 'pattern { action }'
```

### Built-ins

* `$1, $2, $NF` columns
* `NF` number of fields
* `NR` line number

### High-ROI patterns

```
awk '{print $1}'
awk '$3 > 80 {print $2, $3}'
awk '{sum += $2} END {print sum}'
awk '{count[$1]++} END {for (k in count) print k, count[k]}'
```

### CPU usage from `top`

```
top -bn1 | awk '/Cpu\(s\)/ {print 100 - $8}'
```

---

## 🔎 `find` — files by metadata

**Mental model:** Find files by name/age/size safely.

### Core patterns

```
find . -name '*.log'
find . -mtime +7
find . -size +100M
find . -type f
```

### Interview-safe printing (don’t parse `ls`)

```
find . -name '*.log' -mtime +7 -printf '%p %s\n'
```

---

## 🔗 `xargs` — stdin → args

**Mental model:** Turn lines into command arguments.

```
find . -name '*.log' | xargs rm
```

Safer (handles spaces):

```
find . -name '*.log' -print0 | xargs -0 rm
```

---

## 📊 Monitoring (must-know)

### CPU

```
top -bn1
mpstat
```

Extract CPU usage:

```
top -bn1 | awk '/Cpu\(s\)/ {print 100 - $8}'
```

### Memory

```
free -h
free -m | awk 'NR==2 {print $3/$2*100}'
```

### Disk

```
df -h
df -h | awk '$5+0 > 80 {print $0}'
```

### Processes

```
ps aux
ps aux | awk '$3 > 80 {print $2,$3,$11}'   # CPU hogs
ps aux | awk '$4 > 30 {print $2,$4,$11}'   # MEM hogs
```

---

## ⏱ Loops & Alerts

### Infinite loop

```
while true; do
  command
  sleep 5
done
```

### Threshold check

```
if [ "$value" -gt 80 ]; then
  echo "High usage"
fi
```

---

## 🧠 Regex Mini-Set (only this)

```
.*        → anything
^word     → starts with
word$     → ends with
=.*       → from = to end
\berror\b → whole word
[0-9]+    → digits
```

---

## 🎯 What this prepares you for

* CPU / memory / disk monitoring
* Log filtering & aggregation
* Secret masking in configs
* Safe file cleanup
* Resource-heavy process detection

> **Tip:** Don’t memorize—recognize patterns and explain intent. That’s the bar.

---

## 🧩 HackerRank Bash Problems — What They Actually Teach (Mapped to SysDev Skills)

This section maps the **HackerRank Bash problems you listed** to the **real skills interviewers care about**. Use this to understand *why* you solved them and *what to reuse*.

---

### 🧮 Arithmetic & Conditionals (Core Bash Thinking)

**Problems**

* Compute the Average
* Arithmetic Operations
* The World of Numbers
* Comparing Numbers
* Getting started with conditionals
* More on Conditionals

**What you learned (keep this)**

* Bash integer arithmetic: `$(( ))`
* Floating point via `awk` / `bc -l`
* `if / elif / else`
* Numeric vs string comparison (`-lt`, `-gt`, `-eq`)

**Interview reuse**

* Threshold checks (CPU > 80%)
* Averages (mean latency, mean CPU)
* Guard conditions in scripts

---

### 🔁 Loops & Control Flow

**Problems**

* Looping and Skipping
* Looping with Numbers

**What you learned**

* `for` and `while` loops
* Skipping values with `continue`
* Controlled iteration

**Interview reuse**

* Polling scripts
* Periodic monitoring
* Retry logic

---

### 🗂 Text Extraction — `cut`, `head`, `tail`

**Problems**

* Cut #1 – #9
* Head of a Text File #1, #2
* Middle of a Text File
* Tail of a Text File #1, #2

**What you learned**

* Column extraction with delimiters
* Character ranges
* Line ranges

**Interview reuse**

* Parsing CSV / TSV
* Inspecting large logs
* Sampling data safely

---

### 🔤 Character Transforms — `tr`

**Problems**

* 'Tr' Command #1, #2, #3

**What you learned**

* Case conversion
* Character replacement
* Deletion

**Interview reuse**

* Normalizing logs
* Sanitizing input
* Pre-processing text

---

### 🔃 Sorting & Deduplication

**Problems**

* Sort Command #1 – #7
* 'Uniq' Command #1 – #4

**What you learned**

* Lexicographic vs numeric sort
* Reverse sort
* Unique counting

**Interview reuse**

* Error aggregation
* Top-N reports
* Frequency analysis

---

### 📋 Merging & Arrays

**Problems**

* Paste - 1 – 4
* Read in an Array
* Slice an Array
* Filter an Array with Patterns
* Concatenate an array with itself
* Display an element of an array
* Count elements in an Array

**What you learned**

* Reading stdin into arrays
* Array slicing
* Pattern-based filtering

**Interview reuse**

* Batch processing
* Grouped operations
* Script parameter handling

---

### 🧠 `awk` — Logic, Math, Aggregation

**Problems**

* 'Awk' - 1, 3, 4
* Compute the Average (alt solution)
* Lonely Integer - Bash!

**What you learned**

* Pattern–action model
* Column math
* Aggregation

**Interview reuse**

* CPU / memory calculations
* Metrics aggregation
* Conditional reporting

---

### 🔍 `grep` — Filtering

**Problems**

* 'Grep' #1, #2, #3
* 'Grep' - A, B

**What you learned**

* Case-insensitive match
* Word boundaries
* Inverted matches

**Interview reuse**

* Error isolation
* Noise removal
* Alert triggers

---

### ✏️ `sed` — Stream Editing

**Problems**

* 'Sed' command #1 – #5

**What you learned**

* Substitution
* Deletion
* Word boundaries
* Case-insensitive replace

**Interview reuse**

* Masking secrets
* Config cleanup
* Log normalization

---

### 🌳 Recursive / Advanced

**Problems**

* Functions and Fractals - Recursive Trees - Bash!

**What this really shows**

* You can understand recursion
* You can follow non-trivial Bash logic

**Interview note**

* Nice-to-have, not required for SysDev-2

---

## 🎯 Final Interview Perspective

If you solved **most of these**, you already demonstrated:

* Control flow
* Text processing
* Aggregation
* Monitoring logic

Interviewers **do not expect** you to recall problem statements.
They expect you to **reuse the patterns** above.

> You didn’t just solve HackerRank problems — you trained the exact muscles SysDev interviews use.
