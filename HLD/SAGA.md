SAGA – ASYNC MENTAL MODEL (ORCHESTRATION + CHOREOGRAPHY)

1) Core idea
- A Saga is a sequence of LOCAL transactions across services.
- There is NO distributed transaction and NO global rollback.
- Progress happens via EVENTS, not blocking calls.
- Ordering is logical, not synchronous.

2) What “async” really means
- No thread waits.
- No service blocks.
- Only Saga STATE waits (stored as data, not execution).
- Services react when events arrive.

3) Service behavior
- Each service executes a local transaction (T1, T2, …).
- Local transactions COMMIT immediately or FAIL completely.
- After commit, the service is free to do other work.
- No partial execution exists inside a service.

4) Orchestrated Saga
- A central ORCHESTRATOR exists.
- Orchestrator maintains Saga state as a persistent state machine.
- It sends COMMANDS to services and reacts to EVENTS.
- It does NOT execute business logic or hold locks.
- If a service fails, orchestrator triggers COMPENSATION.

5) Choreographed Saga
- No orchestrator.
- No global Saga state.
- Each service knows only:
  - its own local transaction
  - which events it reacts to
- Saga flow emerges from event reactions.
- Compensation logic is distributed across services.

6) Failure handling
- Rollback across services is IMPOSSIBLE.
- Already committed transactions stay committed.
- Failures are handled via COMPENSATING TRANSACTIONS.
- Compensation is a new forward transaction (undo by doing).

7) Example correction
- ❌ “Rollback T1 if B fails”
- ✅ “Run compensation C1 for T1 if B fails”

8) Key insight
- Dependency ≠ synchrony
- Waiting for an event ≠ blocking
- Sagas scale because time is allowed to move forward

9) One-line interview truth
- “A Saga is an event-driven distributed state machine that coordinates local transactions asynchronously using compensation instead of rollback.”

# Saga Pattern — L5 Deep Dive (Interview-Ready Notes)

## 1. The **Critical Missing Piece**: Observability

In an **async Saga**, things *will* get lost.

If a service consumes an event but crashes before emitting the next one, the Saga **hangs forever** unless you explicitly design for it.

### Timeout-Based Compensations (L5 Addition)

- The **Orchestrator must use timeouts**
- If Service B does not respond within **N seconds**, assume failure
- Immediately trigger the **compensation for Service A**

> A Saga without timeouts is a distributed deadlock waiting to happen.

### Monitoring & Tracing

**Problem:**  
How do you trace *one business request* across 5+ services?

**Solution:**  
Use **Correlation IDs**.

- Generate a `Correlation_ID` (or `Saga_ID`) at the start
- Pass it through **every command, event, log, and metric**
- Enables:
  - Distributed tracing (Jaeger, Zipkin, OpenTelemetry)
  - End-to-end debugging
  - Clear failure diagnosis

---

## 2. Handling the **Intermediate State** (Isolation)

This is the **biggest weakness** of Sagas.

In ACID:
- No one sees half-completed work

In Sagas:
- Everyone sees it 😬

### The Problem

Example:
- Saga books a **flight** → success
- Saga books a **hotel** → failure
- Another user books the *same flight* before compensation runs

You now have:
- Overbooking
- Inconsistent business state
- Angry customers

### L5 Solution: **Semantic Locking**

Instead of relying on database isolation, use **business-level locks**.

**Approach:**
- Mark resources as:
  - `PENDING`
  - `RESERVED`
  - `IN_PROGRESS`
- Other Sagas see this state and back off

**Key Idea:**
> The transaction may be committed, but the *business intent* is not final.

---

## 3. **Idempotency Is Mandatory**

In distributed systems, messages are delivered **at least once**.

Duplicates are not a bug — they are *guaranteed*.

### Interview Truth™

> Every service in my Saga must be **idempotent**.

If the Orchestrator sends:
- `DebitAccount` **twice**
- due to retries, timeouts, or network flickers

The service must:
- Debit **exactly once**

### Mechanism

- Store processed commands using:
  - `Saga_ID`
  - `Step_ID`
- Enforce a **unique constraint** in the database
- If the same command arrives again → **no-op**

**Rule:**
> If your Saga isn’t idempotent, it’s broken.

---

## 4. Orchestration vs. Choreography (**The L5 Pivot**)

Don’t just define them — know **when to use each**.

### Choreography

**Best for:**
- Simple flows (2–3 steps)
- High throughput systems

**Pros:**
- Loose coupling
- No central controller
- High performance

**Cons:**
- Hard to visualize end-to-end flow
- Risk of **Event Spaghetti**
- Debugging becomes painful as the system grows

---

### Orchestration

**Best for:**
- Complex business workflows (4+ steps)
- Strong ordering & conditional logic

**Pros:**
- Clear control flow
- Easier debugging
- Centralized business rules

**Cons:**
- Orchestrator can become a **Fat Service**
- Knows too much about downstream behavior

**L5 Insight:**
> Complexity doesn’t disappear — it just moves.

---

## 5. Failure Scenarios You *Must* Be Ready For

Interviewers love these.

### ❓ What if the Orchestrator crashes?

**Correct Answer:**
- The Orchestrator must **persist its state**
- Store Saga progress in:
  - DynamoDB
  - Postgres
  - Any durable DB
- Only send commands *after* state is persisted

On restart:
- Reload state
- Resume from last known step

---

### ❓ What if a Compensation fails?

This is a **Critical Failure**.

**Reality:**
- You cannot compensate forever
- You cannot “compensate a compensation” infinitely

**Required Handling:**
- Send message to a **Dead Letter Queue (DLQ)**
- Trigger:
  - Alerts
  - Paging
  - Manual intervention

> At some point, a human must fix the business inconsistency.

---

## Final L5 Takeaway

A Saga is not just a pattern — it’s an **operational commitment**.

If you don’t design for:
- Observability
- Timeouts
- Idempotency
- Intermediate state
- Crash recovery

Then your Saga *will* fail — just not during the demo.

