# Circuit Breaker — Stress-Proof Interview Notes (Amazon L5)

---

## One-line definition

**Circuit Breaker prevents an application from repeatedly calling a downstream service that is likely to fail.**

It converts *slow failure* into *fast failure*.

---

## Why it exists (business + technical)

When a downstream service is down:

1. You **waste latency** (waiting for timeouts).
2. You **block threads** (resource starvation).
3. You **increase load on a broken service** (slower recovery).
4. You **cascade failure** to healthy services.

Circuit breaker stops this spiral.

---

## Who is being protected?

- Your service (threads, CPU, latency)
- The downstream service (recovery chance)
- The overall system (no cascading failure)

---

## Core idea

> “If failures cross a threshold, assume the dependency is unhealthy and stop calling it.”

---

## States (must know)

### 1. Closed (normal)
- All calls allowed
- Failures are counted

### 2. Open (protective)
- No calls go to downstream
- Fail immediately (fast failure)
- Threads are not blocked

### 3. Half-Open (testing)
- Allow limited test calls
- Measure success

If success → Closed  
If failure → Open

---

## State transitions

Closed → Open  
When failure threshold is crossed

Open → Half-Open  
After sleep window expires

Half-Open → Closed  
If test calls succeed

Half-Open → Open  
If test calls fail

---

## Important parameters (L5 depth)

- Failure threshold (count or %)
- Sliding window size
- Open state sleep duration
- Success threshold in half-open
- Timeout for downstream calls

---

## What Circuit Breaker is NOT

- ❌ Not rate limiting  
- ❌ Not retry  
- ❌ Not load balancing  
- ❌ Not concurrency control  

It is **failure detection + traffic stop mechanism**

---

## Circuit Breaker vs Retry

Retry:
> “Maybe it works next time.”

Circuit Breaker:
> “This dependency is unhealthy. Stop trying.”

Retries without circuit breaker amplify outages.

---

## Circuit Breaker vs Bulkhead

Bulkhead:
- Limits *how much damage a slow service can cause*

Circuit breaker:
- Stops *calling a broken service*

They are complementary.

---

## Typical architecture placement

Service → Timeout → Circuit Breaker → Retry → Downstream

Timeout first  
Breaker second  
Retry last (limited)

---

## Interview decision triggers

Use Circuit Breaker when:

- Downstream frequently fails
- Timeouts are high
- Cascading failures observed
- System enters retry storms
- Threads get exhausted during outages

---

## Metrics interviewers like

- failure_rate
- slow_call_rate
- open_state_duration
- rejected_calls
- recovery_success_rate

---

## Killer interview lines

Use 1–2 of these:

- “Circuit breaker converts unpredictable latency into predictable failure.”
- “It protects thread pools by avoiding useless waiting.”
- “Bulkheads limit damage; circuit breakers prevent damage.”
- “Without circuit breakers, retries turn incidents into disasters.”
- “It gives downstream services time to heal.”

---

## Mental model

Electrical circuit analogy:

Closed → current flows  
Open → current stops  
Half-open → small test current

Or:

Doctor analogy:

Closed → patient healthy  
Open → patient in ICU, no visitors  
Half-open → limited check-ups

---

## Final mental shortcut

If the problem mentions:

- repeated failures
- timeouts
- retries
- cascading failure
- downstream outage

→ **Circuit Breaker**

---

## One-sentence summary (memorize)

**A circuit breaker stops calling a dependency when failures indicate it is unhealthy, preventing wasted latency, blocked threads, and cascading failures.**

---
