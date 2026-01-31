# Amazon L5 System Design – Complete Mental Model (HLD)

This document is a **thinking framework**, not a list of patterns.
It is designed to help you **reason clearly under pressure** during an Amazon L5 (SDE II) High-Level Design interview.

The goal is not to “use all patterns”, but to **make deliberate choices**, explain trade-offs, and defend them calmly.

This file covers **9 core topics** that together allow you to answer *almost any* HLD question.

---

## 1. Identify the User and the Critical Path

### What this means
Before designing anything, you must understand:
- **Who is using the system**
- **What action actually matters**

Not all users and actions are equally important.

### Examples
- In an e-commerce system:
  - Browsing is important
  - Checkout is **critical**
- In a ride-hailing system:
  - Driver matching is critical
  - Analytics dashboards are not

### Why interviewers care
They want to see that you:
- Protect the most important flows
- Are willing to degrade less important features

### Good interview signal
> “I’ll first identify the revenue-critical or user-critical path and make sure that path stays available even during failures.”

---

## 2. Estimate Scale (Back-of-the-Envelope)

### What this means
You need **rough numbers**, not perfect math.

You should estimate:
- Requests per second (QPS)
- Read vs write ratio
- Data growth over time
- Memory vs disk usage

### Simple example
- 10 million users
- 10% active daily → 1 million DAU
- Each user does ~10 actions/day
- Peak traffic = ~5× average

This helps you decide:
- Cache or not
- SQL or NoSQL
- Single region or multi-region

### Why interviewers care
Designs without scale assumptions are **not real designs**.

### Good interview signal
> “At this scale, a single database node will not be enough, so we need partitioning.”

---

## 3. Choose Data Stores Based on Access Patterns

### What this means
You do **not** choose databases by popularity.
You choose them by:
- Read-heavy vs write-heavy
- Range queries vs key lookups
- Strong vs eventual consistency

### Examples
- User profile lookup → Key-value store
- Orders with date filters → B+ Tree based database
- Event logs → LSM-based store

### Important idea
Each database choice is a **trade-off**, not a “best” solution.

### Good interview signal
> “I’m choosing this database because it optimizes our dominant access pattern, and I accept these trade-offs.”

---

## 4. Caching Strategy (What, Where, and Why)

### What caching is
Caching stores frequently accessed data closer to the user or service to:
- Reduce latency
- Reduce database load

### What to cache
- Read-heavy data
- Data that does not change frequently
- Expensive computations

### Where to cache
- Client-side (browser / app)
- CDN (static or semi-static content)
- Service-level cache (Redis / Memcached)

### Cache challenges
- Cache invalidation
- Stale data
- Cache stampede

### Good interview signal
> “I’m caching this data because it is read-heavy and tolerates slight staleness.”

---

## 5. Backpressure (Preventing System Collapse)

### What backpressure is
Backpressure is the system’s ability to **slow down or reject work** when it is overloaded.

Without backpressure:
- Queues grow infinitely
- Memory explodes
- Latency becomes unbounded
- The system crashes

### Where backpressure is applied

#### Client-side
- Return HTTP 429
- Client retries with exponential backoff

#### Gateway / Load Balancer
- Rate limiting
- Per-user quotas

#### Service-level
- Bounded queues
- Thread pool limits
- Fail fast when saturated

### Important warning
Retries without limits can **make outages worse**.

### Good interview signal
> “I prefer failing fast early rather than letting retries amplify load.”

---

## 6. Failure Modes (What Breaks First)

### What this means
Every system will fail.
The question is **how** and **where**.

### Common failures
- Database latency spikes
- Partial network failures
- Dependency timeouts
- Node crashes

### What interviewers expect
You to describe:
- At least 2 realistic failures
- What users experience
- How the system reacts

### Good interview signal
> “If this dependency becomes slow, requests will pile up unless we apply backpressure.”

---

## 7. Degradation Strategies (Graceful Failure)

### What degradation means
Degradation answers:
> “What does the system do instead of completely failing?”

### Common strategies

#### Read-only mode
- Disable writes
- Allow reads

#### Stale data
- Serve cached or slightly outdated data

#### Feature shedding
- Turn off non-critical features
- Protect core flows

#### Clear failure
- Fast error responses
- No partial corruption

### Why this matters
Users prefer:
- Partial functionality
- Over total downtime

### Good interview signal
> “I degrade non-essential features first to protect the core user journey.”

---

## 8. Observability (Knowing What Is Happening)

### What observability means
Observability answers:
1. Is the system healthy?
2. Where is it slow or broken?
3. Why did it happen?

### Metrics
- Request rate
- Error rate
- Latency percentiles (p50, p90, p99)
- Queue depth
- Retry count

### Logs
- Structured logs
- Correlation IDs
- Used for debugging, not monitoring

### Tracing
- Track a request across services
- Find the slowest dependency

### Alerts
- Based on symptoms, not CPU usage
- Actionable alerts only

### Good interview signal
> “I monitor p99 latency and error rate to detect user-visible issues.”

---

## 9. Explicit Trade-offs and Ownership Thinking

### What this means
You must clearly state:
- What you optimized for
- What you knowingly accepted as a downside

### Examples
- “We accept eventual consistency for speed.”
- “We trade storage for faster reads.”
- “We prioritize availability over freshness.”

### Ownership mindset
Think in terms of:
- Blast radius
- Independent services
- Safe deployments
- Rollbacks

### Good interview signal
> “This is a conscious trade-off, and here’s why it’s acceptable.”

---

## Final Mental Checklist (Use This During the Interview)

1. Who is the user and what is critical?
2. What is the scale?
3. What are the main access patterns?
4. Why this data store?
5. What is cached and where?
6. Where is backpressure applied?
7. What fails first?
8. How does the system degrade?
9. What trade-offs did I choose?

If you can calmly walk through these points, you are operating at **Amazon L5 level**.
