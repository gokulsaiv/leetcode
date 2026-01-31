# Distributed Transaction Control — 2PC, 3PC, Failures & Recovery (Amazon L5/L6 SysDev Level)

---

# 1. Why Distributed Transaction Protocols Exist

A distributed transaction spans multiple nodes, services, or databases and must satisfy atomicity across them. Local ACID guarantees are not enough when multiple independent resource managers are involved.

Core requirement:
- Either ALL participants commit
- Or ALL participants abort

Distributed difficulty comes from:
- No shared memory
- No global clock
- Message delay/loss
- Process crashes
- Network partitions

Distributed commit protocols coordinate agreement on the final outcome.

Primary protocols:
- Two Phase Commit (2PC)
- Three Phase Commit (3PC)

In real production systems, these are often replaced or complemented by:
- Consensus protocols (Raft/Paxos)
- Sagas
- Idempotent workflows
- Outbox patterns

---

# 2. System Model Assumptions (State Explicitly in Interview)

Typical assumptions for 2PC/3PC:

- Nodes may crash and recover
- Stable storage exists (write-ahead logs)
- Messages are not corrupted
- Messages may be delayed or lost
- Duplicate messages possible
- Crash-stop model (node stops, does not behave maliciously)
- Coordinator exists (single leader)

Important: 2PC assumes asynchronous network → blocking possible.
3PC assumes bounded delay network → rarely realistic.

---

# 3. Roles in Distributed Commit

Coordinator (Transaction Manager):
- Drives protocol
- Collects votes
- Decides commit/abort
- Logs global decision

Participants (Resource Managers):
- Execute local transaction
- Vote YES/NO
- Follow coordinator decision
- Maintain recovery logs

---

# 4. Two Phase Commit (2PC) — Overview

2PC guarantees atomic commit across distributed participants using two rounds of messaging and durable logging.

Phases:
1. Prepare (Voting Phase)
2. Commit/Abort (Decision Phase)

Key property:
- Safe but blocking

---

# 5. Two Phase Commit — Phase 1 (Prepare / Voting)

Coordinator sends to all participants:
PREPARE (or VOTE-REQUEST)

Participant actions:
- Validate transaction
- Execute locally but do not commit
- Acquire locks
- Write PREPARED record to stable log
- Reply with:
  YES → ready to commit
  NO → cannot commit

Rules:
- If any NO → global abort
- If all YES → move to commit phase

Participant enters READY state after voting YES.

READY is a blocking state.

---

# 6. Two Phase Commit — Phase 2 (Decision)

Coordinator decides:

If all YES:
GLOBAL_COMMIT

Else:
GLOBAL_ABORT

Coordinator:
- Writes decision to stable log
- Sends decision to all participants

Participants:
- On COMMIT → commit locally, release locks, log COMMIT
- On ABORT → rollback, release locks, log ABORT
- Send ACK

Coordinator completes after ACKs received (optional optimization).

---

# 7. 2PC State Machines

Coordinator states:
INIT → WAIT → COMMIT / ABORT → DONE

Participant states:
INIT → READY → COMMIT
            → ABORT

READY state occurs after voting YES and before final decision received.

This state causes blocking risk.

---

# 8. Logging Requirements (Critical for Recovery)

Participants must log:
- START
- PREPARED
- COMMIT or ABORT

Coordinator must log:
- START
- GLOBAL_COMMIT or GLOBAL_ABORT

Rules:
- Log must be forced to disk before sending next protocol message
- Recovery decisions depend entirely on logs

This is Write-Ahead Logging principle.

---

# 9. 2PC Failure Scenarios

## Participant crashes before voting

Effect:
- No vote received
- Coordinator times out
- Treat as NO
- Global abort

Safe and non-blocking.

---

## Participant crashes after voting YES (READY state)

State:
- PREPARED logged
- Locks held
- Waiting for decision

On recovery:
- Reads log → sees PREPARED
- Must contact coordinator

If coordinator unreachable:
- Participant must BLOCK
- Cannot decide alone

This is the classic 2PC blocking condition.

---

## Coordinator crashes before sending decision

Participants:
- Some may be READY
- Waiting for decision

On coordinator recovery:
- Check log

If no global decision logged:
- Abort transaction
- Send GLOBAL_ABORT

---

## Coordinator crashes after logging COMMIT but before notifying all

Some participants committed, some not.

Recovery:
- Coordinator reads GLOBAL_COMMIT log
- Resends COMMIT to all

Participants must handle duplicate commit messages (idempotent handling).

---

## Network Partition

Coordinator isolated from participants.

Participants in READY:
- Cannot commit
- Cannot abort
- Must wait

System becomes blocked.

This shows:
2PC sacrifices availability under partition (CAP tradeoff).

---

# 10. Why 2PC Is Blocking — Formal Reason

In READY state:
- Participant voted YES
- Promised it can commit
- Cannot abort safely (others may commit)
- Cannot commit safely (others may abort)

No unilateral safe action exists.

Thus participant must wait → blocking.

Blocking is fundamental to 2PC in asynchronous networks with failures.

---

# 11. 2PC Performance Costs

- Two full network round trips
- Forced disk writes at each phase
- Locks held across phases
- Coordinator bottleneck
- Poor latency in geo-distributed systems
- Reduces concurrency

Optimizations exist but blocking risk remains.

---

# 12. Heuristic Decisions (Interview Bonus Topic)

Some systems allow heuristic completion:
- Participant decides commit/abort after long timeout

Danger:
- Can break atomicity
- Leads to inconsistency

Used only when business logic tolerates inconsistency.

---

# 13. Three Phase Commit (3PC) — Motivation

Goal:
- Remove blocking problem of 2PC
- Add intermediate state so participants can decide safely after timeout

Adds one more phase:
CAN_COMMIT → PRE_COMMIT → DO_COMMIT

Key idea:
No participant is left in an uncertain state.

But requires stronger timing assumptions.

---

# 14. 3PC Phases

## Phase 1 — CanCommit

Coordinator → CAN_COMMIT?

Participants:
- Check feasibility
- No locks yet
- Reply YES or NO

NO → abort immediately.

---

## Phase 2 — PreCommit

If all YES:
Coordinator → PRE_COMMIT

Participants:
- Acquire locks
- Write PRECOMMIT log
- ACK

Now transaction is guaranteed to commit unless coordinator fails before this phase finishes everywhere.

State = safe-to-commit.

---

## Phase 3 — DoCommit

Coordinator → DO_COMMIT

Participants:
- Commit
- Release locks
- Log COMMIT

---

# 15. 3PC State Model

INIT → WAIT → PRECOMMIT → COMMIT
               → ABORT

Key difference:
PRECOMMIT is a non-blocking state.

Timeout rules allow safe decisions.

---

# 16. 3PC Failure Handling

Coordinator fails before PRECOMMIT:
- Participants timeout
- Abort safely

Coordinator fails after PRECOMMIT:
- Participants timeout
- Commit safely

Because all participants already agreed and prepared.

---

# 17. Why 3PC Is Rare in Production

3PC assumes:
- Bounded message delay
- No network partitions beyond timeout window

Real networks are asynchronous:
- Delays unbounded
- Partitions common

Under partition, 3PC can still break safety.

Thus rarely used in real distributed databases.

Consensus protocols are preferred.

---

# 18. 2PC vs 3PC Comparison

2PC:
- 2 phases
- Blocking
- Works in async networks
- Widely implemented
- Simpler

3PC:
- 3 phases
- Non-blocking under bounded delay assumption
- Unsafe under real partitions
- Rare in practice

---

# 19. Modern Alternatives (Interview Depth Signal)

Instead of 2PC/3PC, modern systems often use:

Consensus-based commit:
- Raft / Paxos replicated log
- Global decision replicated before commit

Saga pattern:
- Sequence of local transactions
- Compensating actions for rollback
- Eventual consistency

Transactional Outbox:
- Commit DB + event atomically
- Asynchronous propagation

Idempotent operations + retries:
- Avoid distributed locking

Try-Confirm-Cancel (TCC):
- Reserve → confirm → cancel workflow

---

# 20. Interview Talking Points (L5/L6 Signals)

Mention these to show depth:

- 2PC blocking is fundamental, not an implementation bug
- READY state is the blocking state
- Write-ahead logging is mandatory
- Coordinator is single point of failure (unless replicated)
- 3PC depends on timing assumptions → unrealistic
- 2PC hurts availability under partition (CAP)
- Modern microservices avoid 2PC using Sagas
- Consensus systems embed commit into replicated log
- Idempotency is critical for retry safety

---

# End of Notes
