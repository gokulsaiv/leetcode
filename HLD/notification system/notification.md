# Notification System Design (Amazon SD2)

This document captures a **complete, defensible Notification System design** suitable for an Amazon SysDev-2 / SDE-2 interview. It includes:

* Final architecture
* Delivery guarantees
* Failure handling
* Retry strategy
* Idempotency model
* Reusable interviewer-defense statements

---

## 1. Problem Statement

Design a backend **Notification System** that:

* Sends notifications to users
* Supports multiple channels (Email, Push, WhatsApp, Slack, etc.)
* Handles high scale (millions of users, thousands QPS)
* Is resilient to failures
* Provides reasonable delivery guarantees

No UI is required. The system is invoked by multiple upstream services.

---

## 2. Functional Requirements

* Accept notification requests from multiple services
* Fan-out a single logical notification to multiple channels
* Deliver notifications asynchronously
* Retry failed notifications
* Prevent uncontrolled duplication
* Allow dropping notifications after bounded retries (for non-critical cases)

---

## 3. Non-Functional Requirements

* High availability
* Horizontal scalability
* Fault tolerance
* Observability (metrics, logs, alerts)
* Clear delivery guarantees

---

## 4. Key Design Decisions (High-Level)

* Kafka is used **only for transport**, not for state
* DynamoDB is the **single source of truth** for delivery state and idempotency
* Delivery is **at-least-once with deduplication**
* Retry traffic is **isolated** from fresh traffic
* External providers are assumed unreliable

---

## 5. Delivery Guarantees (Explicit Contract)

> **The system provides at-least-once delivery with deduplication.**

Exactly-once delivery is **not claimed**, because:

* External systems (email, push, SMS) do not support transactional guarantees
* Network failures and retries can cause duplicates

Duplicates are considered acceptable for most notification types.

---

## 6. Notification Classification

Notifications are classified into two categories:

### Critical Notifications

* Password resets
* Security alerts
* Order confirmations

Properties:

* Longer retry window
* Higher retry count
* Longer state retention in DynamoDB

### Non-Critical Notifications

* Marketing
* Recommendations
* Social updates

Properties:

* Bounded retries
* May be dropped after retry exhaustion

---

## 7. High-Level Architecture

```
Upstream Services
        |
        v
Load Balancer
        |
        v
Routing Service
        |
        v
Kafka Topics (per channel)
  - email-topic
  - push-topic
  - whatsapp-topic
        |
        v
Channel Consumers
        |
        v
External Providers
```

Retry path:

```
Channel Consumer Failure
        |
        v
Retry Topic (per channel)
        |
        v
Retry Consumer Group
```

---

## 8. Kafka Design

* Separate topic per channel
* Independent retry topic per channel
* Independent consumer groups per topic
* Offsets are managed **per topic**, not across topics

Kafka guarantees:

* At-least-once consumption
* No delivery semantics awareness

---

## 9. Idempotency & State Management

### Source of Truth

**DynamoDB** is the authoritative store.

Kafka offsets are *never* treated as delivery state.

### Partition Key

* `notification_id` (UUID)
* Ensures even distribution and avoids hot partitions

---

## 10. Notification State Machine

Possible states:

* `NEW`
* `PROCESSING`
* `RETRYING`
* `SUCCESS` (terminal)
* `FAILED` (terminal)

Only terminal states (`SUCCESS`, `FAILED`) end processing.

---

## 11. Processing Ownership (Lease Model)

To prevent duplicate delivery:

Each processing attempt includes:

* `owner_id` (consumer instance ID)
* `lease_expiry_timestamp`

Flow:

1. Consumer performs **conditional write** to acquire lease
2. If lease expired, another consumer may take ownership
3. Lease is renewed or released upon completion

Timeout alone is **not sufficient**.

---

## 12. Consumer Flow (Main Topic)

1. Consume message from Kafka
2. Read state from DynamoDB
3. If `SUCCESS` → skip & commit offset
4. Attempt lease acquisition (`PROCESSING`)
5. Deliver notification
6. On success → update state to `SUCCESS`
7. Commit Kafka offset

---

## 13. Retry Flow

* Failures are published to **channel-specific retry topics**
* Retry consumers are isolated from main consumers
* Retry strategy:

  * Exponential backoff
  * Jitter
  * Max retry count

After retry exhaustion:

* State updated to `FAILED`
* Offset committed
* Message dropped

---

## 14. Why Retry Isolation Matters

* Prevents retry storms from blocking fresh traffic
* Reduces tail latency
* Improves operational stability

---

## 15. Observability

The system emits:

* Delivery success metrics
* Retry count metrics
* Terminal failure metrics
* Alerts on abnormal failure rates

All notification states are queryable via DynamoDB for audits.

---

## 16. What This System Intentionally Does NOT Guarantee

* Exactly-once delivery
* Strict ordering across channels
* Infinite retries

These are conscious tradeoffs.

---

## 17. Reusable Interview Defense Statements

### Delivery Guarantees

> "This system provides at-least-once delivery with deduplication. Exactly-once delivery is not achievable across external providers."

### Kafka vs State

> "Kafka offsets indicate consumption, not delivery success. Delivery state lives outside Kafka."

### Idempotency Store

> "DynamoDB is the authoritative idempotency store; Kafka is transport-only."

### Duplicate Acceptance

> "We tolerate rare duplicates because deduplication is cheaper than enforcing exactly-once semantics."

### Retry Isolation

> "Retry traffic is isolated so downstream failures do not increase tail latency for new notifications."

### Backoff Strategy

> "Exponential backoff with jitter prevents synchronized retries during partial outages."

### Processing Safety

> "Processing ownership is guarded by leases, not just status flags, to prevent duplicate delivery from slow consumers."

### Observability

> "All terminal failures emit metrics and alerts, and delivery states are queryable for debugging."

### Design Philosophy

> "This design trades strict guarantees for scalability and operability while remaining correct under failure."

---

## 18. Final Interview Summary

* Scalable
* Failure-aware
* Honest guarantees
* Operationally observable

**This design is sufficient to pass an Amazon SysDev-2 / SDE-2 system design interview.**
