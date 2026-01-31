# Amazon L5 HLD – Advanced Runtime & Failure Thinking

This document focuses ONLY on the parts of system design that typically
decide an Amazon L5 (SDE II) HLD interview.

It assumes you already know:
- Databases and access patterns
- Caching basics
- Load balancing
- Rate limiting
- Back-of-the-envelope estimation

This file is about **what happens when the system is under stress**.

---

## 5. Backpressure (The Difference Between Survival and Collapse)

### What Backpressure Really Means

Backpressure is the system’s ability to **say NO** when it cannot safely
process more work.

Without backpressure:
- Requests keep coming in
- Queues keep growing
- Latency explodes
- Memory fills up
- Eventually, everything crashes

Backpressure is not an optimization.
It is a **safety mechanism**.

---

### Why Backpressure Matters at Amazon Scale

At high scale, failures are rarely binary.
More often:
- A downstream service becomes slow (not dead)
- Retries increase traffic
- Queues silently grow
- Latency spikes propagate upstream

This is how **cascading failures** happen.

Backpressure stops the cascade early.

---

### Where Backpressure Must Exist (Very Important)

#### 1. At the Edge (Client / Gateway)

- Return HTTP `429 Too Many Requests`
- Tell the client to slow down
- Client retries with **exponential backoff + jitter**

Why this is powerful:
- Cheapest place to reject traffic
- Protects the entire backend

Interview-ready line:
> “I prefer rejecting traffic early at the edge rather than letting retries amplify load deeper in the system.”

---

#### 2. At the Service Level

Key rules:
- **Never use unbounded queues**
- **Never allow infinite request buffering**

Common techniques:
- Fixed-size thread pools
- Bounded request queues
- Fail fast when capacity is reached

If a service is overloaded, rejecting requests is healthier than accepting them.

---

#### 3. Between Services (Downstream-Aware Backpressure)

When Service A calls Service B:
- If B becomes slow → A must stop sending traffic
- Circuit breaker opens
- A switches to degraded behavior

This prevents:
- Thread exhaustion
- Retry storms
- Full system collapse

---

### Dangerous Anti-Patterns (Say This Explicitly)

- Blind retries without limits
- Infinite queues
- “Auto-scaling will fix it” (it won’t, under cascading failure)

Interview-safe line:
> “Auto-scaling helps with sustained load, not with sudden dependency failures.”

---

## 6. Failure Modes (What Breaks First and Why)

### What Interviewers Are Looking For

They want to see that you:
- Expect failures
- Think about partial failures
- Understand how failures spread

They are NOT looking for perfection.

---

### Common Real-World Failure Modes

#### 1. Slow Dependencies (Most Common)

- Database latency spikes
- Cache misses increase
- Network jitter

Why this is dangerous:
- Slow responses hold threads
- Thread pools fill up
- Upstream services block

This is more dangerous than a hard failure.

---

#### 2. Retry Amplification

- One request fails
- Multiple retries are triggered
- Traffic multiplies
- System overloads itself

This is why retries **must be bounded and delayed**.

---

#### 3. Partial Data Failures

Examples:
- Order written, inventory update failed
- Payment succeeded, confirmation message lost

These are **inconsistent but real states**.

---

### How to Talk About Failures in Interviews

Always answer:
1. What fails?
2. What does the user see?
3. How does the system react?

Interview-safe framing:
> “The system remains available, but some operations are delayed or degraded.”

---

## 7. Degradation Strategies (Graceful Failure)

### What Degradation Means

Degradation answers:
> “When things go wrong, what does the system do instead of dying?”

A system that degrades gracefully **keeps user trust**.

---

### Common Degradation Patterns

#### 1. Read-Only Mode

- Disable writes
- Allow reads
- Useful during database incidents or migrations

User experience:
- “You can view data, but cannot make changes.”

---

#### 2. Stale Data Mode

- Serve cached or slightly outdated data
- Accept eventual consistency temporarily

Why this works:
- Many users prefer slightly old data over errors

---

#### 3. Feature Shedding

- Turn off non-critical features
- Protect core flows (checkout, payments, bookings)

Example:
- Recommendations off
- Search limited
- Checkout always on

Interview line:
> “I protect the revenue-critical path first.”

---

#### 4. Fast Failure with Clear Errors

- Quick, explicit error responses
- No partial success
- No silent corruption

---

## 8. Observability (Knowing What Is Going Wrong)

### What Observability Is NOT

- Not just logs
- Not just dashboards
- Not just CPU metrics

Observability is about **understanding system behavior**.

---

### The Three Pillars (In Practice)

#### 1. Metrics (First Line of Defense)

Key metrics to always mention:
- Request rate (QPS)
- Error rate (4xx / 5xx)
- Latency percentiles (p50, p90, p99)
- Queue depth
- Retry counts
- Circuit breaker state

Why p99 matters:
- Users experience tail latency, not averages

---

#### 2. Logs (For Explanation, Not Detection)

- Structured logs (JSON)
- Correlation IDs
- Used to understand *why* something failed

Good framing:
> “Metrics tell me something is wrong; logs tell me why.”

---

#### 3. Distributed Tracing (Senior Signal)

- Track requests across services
- Identify slow dependencies
- Essential in microservice architectures

Mentioning tracing briefly already signals maturity.

---

### Alerts (Very Important)

Good alerts:
- Based on user-visible symptoms
- Actionable
- Low noise

Bad alerts:
- CPU > 80%
- Memory usage alone

---

## 9. Consistency Decisions Under Failure

### Key Idea

Consistency is not global.
It is **per data type**.

You must explicitly choose where:
- Strong consistency is required
- Eventual consistency is acceptable

---

### Examples

- Payments → Strong consistency
- Inventory → Often eventual with reconciliation
- Notifications → Eventual
- Analytics → Eventual

Interview-safe line:
> “I scope consistency based on user impact and business risk.”

---

### Handling Inconsistencies

- Idempotent APIs
- Retry-safe operations
- Background reconciliation jobs
- Compensating actions (Saga pattern)

---

## 10. Trade-offs and Ownership Thinking (L5 Signal)

### What This Really Means

You must show that:
- You know no design is perfect
- You consciously chose certain downsides
- You can defend those choices

---

### Common Trade-offs to State Explicitly

- Availability over consistency
- Storage cost over latency
- Write complexity over read simplicity
- Freshness over performance (or vice versa)

---

### Ownership Mindset

Think about:
- Blast radius
- Independent deployability
- Rollback safety
- Operational simplicity

Interview-ready line:
> “This is a conscious trade-off, and I’m comfortable with it given the requirements.”

---

## Final Mental Loop (Run This When Pushed)

When an interviewer challenges your design, loop through:
1. Is this overload or failure?
2. Where is backpressure applied?
3. What degrades first?
4. What does the user see?
5. How do we observe and react?
6. What trade-off did I choose?

If you can calmly answer these, you are operating at **Amazon L5 level**.
