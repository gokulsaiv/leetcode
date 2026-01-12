# AWK Crash Summary (SysDev‑2 Focus)

This document summarizes **what you learned in awk**, the **mental models**, and **reusable code patterns** that are relevant for **SysDev‑2 / production debugging interviews**.

---

## 1. What AWK Really Is

AWK is a **stream‑processing language**.

Execution model:

* Read one line (record)
* Split it into fields `$1 … $NF`
* Run your program on that line
* Repeat for every line
* Run `END` block once at the end

Key idea:

> You are writing a **program over a stream**, not chaining commands.

---

## 2. Fields and Records

* `$0` → entire line
* `$1, $2, …` → whitespace‑separated fields
* `NF` → number of fields in the current line

Example:

```bash
awk '{ print $1, $NF }'
```

Important behavior:

* Missing fields become empty
* Empty fields are treated as `0` in numeric comparisons

---

## 3. Conditions (`if / else`) in AWK

`if / else` **must be inside an action block** `{}`.

❌ Invalid:

```bash
awk 'if ($2 < 50) print "Fail"'
```

✅ Correct:

```bash
awk '{
    if ($2 < 50)
        print "Fail"
    else
        print "Pass"
}'
```

Reason:

* AWK expects `{ action }` at the top level
* Control flow only works **inside** actions

---

## 4. Boolean Logic on Columns

Example: fail if **any** subject < 50

```bash
awk '{
    if ($2 < 50 || $3 < 50 || $4 < 50)
        print $1, ":", "Fail"
    else
        print $1, ":", "Pass"
}'
```

Guard against malformed input:

```bash
awk '{
    if (NF < 4)
        print $1, "Invalid"
}'
```

---

## 5. Variables in AWK (Critical Concept)

* No declaration required
* Variables persist **across lines by default**

Example (sum):

```bash
awk '{ sum += $2 } END { print sum }'
```

Mental rule:

> Variables accumulate unless you explicitly reset them.

---

## 6. Associative Arrays (Maps)

AWK arrays are **key‑value maps**, not lists.

Example: count log levels

```bash
awk '{ count[$3]++ } END { print count["ERROR"] }'
```

Properties:

* Keys are strings
* Missing keys default to `0`
* Perfect for logs and metrics

---

## 7. `split()` — Structured Parsing

Syntax:

```bash
split(string, array, delimiter)
```

Delimiter can be a **regex**.

Example (log parsing):

```bash
split($0, a, "latency=")
split(a[2], b, "ms")
latency = b[1]
```

Key points:

* Arrays are **1‑indexed**
* `split()` returns number of parts
* Clear and readable parsing > clever field hacks

---

## 8. One‑Pass Stream Processing (SysDev Signal)

Avoid multiple scans like:

```bash
grep ... | awk ... | wc -l
```

Prefer single‑pass aggregation:

```bash
awk '{
    if (condition)
        count[$key]++
}
END {
    for (k in count)
        print k, count[k]
}'
```

Why it matters:

* Scales to large logs
* Shows system‑level thinking

---

## 9. `END` Block (Finalization Phase)

Runs **once**, after all input is processed.

Used for:

* totals
* max / min
* summaries

Example (max CPU):

```bash
awk '{
    if ($2 > max) max = $2
}
END {
    print "MAX_CPU:", max
}'
```

---

## 10. Real Problems You Can Now Solve

You are now comfortable with:

* Log filtering
* Threshold detection (latency, CPU)
* Error aggregation
* Max / min metrics
* Handling malformed data
* Interview‑style system debugging via logs

---

## One‑Line Summary

> AWK is a per‑line program where variables and arrays persist across records, `split()` provides structure, and `END` produces final results.

This is **exactly** the level expected for SysDev‑2 interviews.
