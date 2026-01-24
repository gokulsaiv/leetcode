# Rate Limiter vs Bulkhead — Stress-Proof Interview Notes (L5)

---

## One-line definitions

**Rate Limiter**  
Controls how fast clients can send requests to my service (requests per time window).

**Bulkhead**  
Controls how many resources (threads / concurrent calls) a workload or dependency can use inside my service.

---

## Core difference (memorize this)

**Time vs Space**

- Rate limiter → time (requests / second, minute)
- Bulkhead → space (threads, in-flight requests)

If the problem mentions **“too many over time”** → Rate limiter  
If the problem mentions **“too many at the same time”** → Bulkhead

---

## Who is being protected?

- Rate limiter protects **my service from clients**
- Bulkhead protects **my service from itself and from slow downstream services**

---

## Concurrency rule

- Rate limiter → does **not** care about concurrency  
- Bulkhead → does **not** care about time windows

---

## Bulkhead types

### 1. Semaphore Bulkhead
Limits concurrent calls to a downstream service.

Use when:
> “This dependency can only handle N parallel requests.”

### 2. Thread-Pool Bulkhead
Limits threads per endpoint / workload.

Use when:
> “One slow API must not block other fast APIs.”

---

## Noisy neighbor relation

Noisy neighbor = one workload consumes all resources.

Bulkhead = technical mechanism to prevent this at thread / concurrency level.

---

## Decision table (interview quick mapping)

| Problem mentions | Use |
|------------------|------|
Clients abusing API | Rate limiter |
Traffic spikes | Rate limiter |
Requests per second limit | Rate limiter |
Downstream capacity limit | Semaphore bulkhead |
Slow dependency | Thread-pool bulkhead |
Thread starvation | Thread-pool bulkhead |
Service self-protection | Bulkhead |

---

## Killer interview lines

- “Rate limiting is about fairness over time; bulkheads are about survival under contention.”
- “Rate limiters protect the boundary. Bulkheads protect internal resources.”
- “If time is the problem, I use rate limiting. If threads are the problem, I use bulkheads.”
- “A slow dependency should fail locally, not globally — bulkheads enforce that.”

---

## Common traps to avoid

- ❌ Rate limiter controls concurrency  
- ❌ Bulkhead protects from clients  
- ❌ They are interchangeable

Correct:
> Rate limiter at ingress, bulkhead at dependency boundaries.

---

## Mental model

Restaurant analogy:

- Rate limiter → how many people can enter per minute
- Bulkhead → how many tables each group can occupy

Different problems. Same goal: system survival.

---
