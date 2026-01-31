# Low-Level Design Concurrency — Complete Interview Notes (Correctness, Coordination, Scarcity)

This is a single-file reference for concurrency in Low-Level Design (LLD) and machine coding interviews. It focuses on multi-threaded, single-process systems. The goal is to recognize concurrency problem categories, detect bug patterns, and apply the correct primitives with proper code structure.

Concurrency problems in LLD interviews fall into three main categories:

1. Correctness — shared state corruption (race conditions)
2. Coordination — safe work handoff between threads
3. Scarcity — limiting access to finite resources

Each category has recognizable patterns and standard solutions.

---

# CATEGORY 1 — CORRECTNESS PROBLEMS

Correctness issues occur when multiple threads access shared mutable state and at least one thread writes. Without synchronization, interleavings cause wrong results.

Signal words in interviews:
thread-safe, shared map, concurrent requests, multiple users at same time, update counter, reserve slot.

Two dominant bug patterns appear repeatedly.

---

## Correctness Pattern A — Check Then Act

Shape:
- Check a condition
- Perform an action based on that condition
- Another thread can change state between those steps

Example — Seat Booking (NOT thread-safe)

Java:
```java
public void bookSeat(String seatId) {
    if (seats.get(seatId).isAvailable()) {
        seats.get(seatId).book();
    }
}
```

Race:
Thread A checks → available  
Thread B checks → available  
Both book → double booking

Fix — Make check+act atomic using a lock

Java:
```java
public void bookSeat(String seatId) {
    synchronized (this) {
        if (seats.get(seatId).isAvailable()) {
            seats.get(seatId).book();
        }
    }
}
```

Python:
```python
def book_seat(seat_id):
    with lock:
        if seats[seat_id].is_available():
            seats[seat_id].book()
```

Rule:
If correctness depends on a condition staying true, protect check and action together.

Common interview cases:
- Parking slot allocation
- Inventory reservation
- Rate limiter allow/deny
- Cache populate-if-missing

---

## Correctness Pattern B — Read Modify Write

Shape:
- Read value
- Modify value
- Write back

Looks atomic but is not.

Example — Counter Increment (NOT thread-safe)

Java:
```java
count++;
```

Race:
Both threads read 5 → both write 6 → lost update

Fix Option 1 — Atomic Variable (single value only)

Java:
```java
AtomicInteger count = new AtomicInteger(0);
count.incrementAndGet();
```

Fix Option 2 — Lock

Java:
```java
synchronized (this) {
    count++;
}
```

Python:
```python
with lock:
    count += 1
```

Use atomic when:
- Single variable
- Simple increment/set

Use lock when:
- Multiple fields must stay consistent
- Complex logic involved

Example — Bank Transfer needs lock:

```java
synchronized (this) {
    from.balance -= amt;
    to.balance += amt;
}
```

---

## Correctness — Deadlock Prevention

Deadlock occurs when threads hold locks in opposite order.

Bad:
Thread A: lock A → lock B  
Thread B: lock B → lock A

Fix:
Always acquire locks in fixed global order.

---

# CATEGORY 2 — COORDINATION PROBLEMS

Coordination issues happen when threads pass work to other threads.

Signal words:
background worker, async processing, job queue, task pipeline, email sender, log processor.

Classic shape: Producer → Queue → Consumer

---

## Wrong Solution — Busy Waiting

Python:
```python
while True:
    if queue:
        task = queue.pop()
        process(task)
```

Problem:
- Spins forever
- Burns CPU

---

## Weak Solution — Sleep Polling

```python
while True:
    task = try_get()
    if not task:
        time.sleep(0.1)
```

Problem:
- Wastes cycles
- Adds delay

---

## Correct Solution — Blocking Queue

Blocking queue sleeps consumers automatically when empty and wakes them when work arrives.

Java Example:

```java
BlockingQueue<Task> q = new ArrayBlockingQueue<>(100);

public void producer(Task t) throws Exception {
    q.put(t);
}

public void consumer() throws Exception {
    while (true) {
        Task t = q.take();
        process(t);
    }
}
```

Python Example:

```python
from queue import Queue

q = Queue(maxsize=100)

def producer(task):
    q.put(task)

def consumer():
    while True:
        task = q.get()
        process(task)
```

No spinning. No polling. Efficient sleep/wakeup.

---

## Coordination — Backpressure (Interview Gold Point)

If producers are faster than consumers:
Queue grows → memory crash.

Fix:
Use bounded queue.

Behavior:
When full → producer blocks → system slows input.

Always bound queues in interview designs.

---

## Coordination — Graceful Shutdown Pattern

Use poison pill task:

```python
STOP = object()

q.put(STOP)

task = q.get()
if task is STOP:
    break
```

Shows production thinking.

---

# CATEGORY 3 — SCARCITY PROBLEMS

Scarcity problems occur when only N threads may access a resource simultaneously.

Signal words:
limit concurrent calls, max connections, rate limit, download slots.

Primitive: Semaphore

---

## Semaphore Solution

Semaphore = permit counter.

Java Example:

```java
Semaphore sem = new Semaphore(5);

public void download() throws Exception {
    sem.acquire();
    try {
        doDownload();
    } finally {
        sem.release();
    }
}
```

Python Example:

```python
sem.acquire()
try:
    download()
finally:
    sem.release()
```

---

## Critical Bug — Permit Leak

If release not in finally:
Exception → permit never returned → system stalls.

Always release in finally.

---

## Scarcity Variant — Object Pool Using Blocking Queue

For reusable objects like DB connections.

Java:

```java
BlockingQueue<Connection> pool = new ArrayBlockingQueue<>(10);

for (int i = 0; i < 10; i++)
    pool.put(new Connection());

Connection c = pool.take();
try {
    query(c);
} finally {
    pool.put(c);
}
```

Python:

```python
pool = Queue(maxsize=10)

for _ in range(10):
    pool.put(Connection())

conn = pool.get()
try:
    query(conn)
finally:
    pool.put(conn)
```

---

# PRIMITIVE SELECTION CHEAT SHEET

Shared state race → Lock / Atomic  
Producer-consumer → Blocking Queue  
Concurrency limit → Semaphore  
Reusable limited objects → Blocking Queue Pool

---

# INTERVIEW RECOGNITION CHECKLIST

Ask:

Is shared mutable state accessed?
→ Correctness → Lock/Atomic

Is work flowing across threads?
→ Coordination → Blocking Queue

Is there a fixed concurrency limit?
→ Scarcity → Semaphore/Pool

---

# INTERVIEW PHRASES THAT SCORE POINTS

Say these when relevant:

race condition  
atomic operation  
critical section  
backpressure  
bounded queue  
permit leak  
deadlock ordering  
graceful shutdown  
idempotent worker

---

# FINAL INTERVIEW STRATEGY

Step 1 — classify category  
Step 2 — name bug pattern  
Step 3 — choose primitive  
Step 4 — show code guard  
Step 5 — mention failure mode  
Step 6 — mention tradeoff  

Concurrency stops being scary once you see the shapes. It’s the same three beasts wearing different hats.

End of file.
