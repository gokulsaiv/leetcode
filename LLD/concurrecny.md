# Low-Level Design Concurrency — Complete Interview Reference (Correctness, Coordination, Scarcity)

This document is a single, continuous reference for concurrency in Low-Level Design (LLD) and machine coding interviews. It focuses on in-process multi-threaded systems. It covers concurrency bug patterns, primitives, fixes, failure modes, and interview reasoning depth expected at senior SDE levels.

Concurrency problems in LLD almost always fall into three categories: correctness, coordination, and scarcity. Correctness deals with shared state corruption. Coordination deals with passing work safely between threads. Scarcity deals with limiting access to finite resources. Correct classification leads directly to the right primitive and design.

Concurrency in LLD assumes multiple threads inside a single process sharing memory and objects. There is no network or distributed consensus layer here. Failures are races, deadlocks, starvation, memory pressure, and ordering bugs — not partitions or replication lag.

Correctness problems occur when multiple threads read and write shared mutable state without atomicity. If at least one thread writes and there is no synchronization, results are undefined. The root cause is non-atomic multi-step operations.

The most common correctness pattern is check-then-act. The code checks a condition and then performs an action based on that condition. Between those two steps another thread may change the state. Example: checking if a seat is available and then booking it. Two threads can both observe availability and both book. This appears in seat booking, parking allocation, inventory reservation, rate limit checks, cache populate-if-missing, and lazy initialization. The fix is to make the check and the action atomic using a lock. Both operations must be inside the same critical section. Only one thread is allowed to execute that block at a time.

The second dominant correctness pattern is read-modify-write. A value is read, changed, and written back. It looks like one operation but is actually three. Incrementing a counter is the canonical case. Two threads can read the same value, compute the same result, and overwrite each other, causing lost updates. This appears in counters, balances, quotas, retries, and metrics. The fix is either a lock or an atomic variable. Atomic variables use CPU compare-and-swap instructions to make a single-variable update atomic. They are fast but limited to one variable. If multiple related fields must remain consistent, a lock is required.

Lock scope must be correct. Too small and races remain. Too large and performance suffers or deadlocks appear. The critical section should include all dependent reads and writes and exclude slow operations like I/O and network calls. Deadlocks occur when locks are acquired in inconsistent order across threads. Prevent them using global lock ordering or by reducing nested locks.

Coordination problems arise when threads pass work to other threads. This is the producer-consumer shape. Examples include background job execution, async email sending, logging pipelines, and task schedulers. The system must allow workers to wait efficiently and must handle overload safely.

Busy waiting — looping and checking a queue repeatedly — wastes CPU. Sleep polling — checking and then sleeping — adds latency and still wastes cycles. The correct primitive is a blocking queue. A blocking queue causes consumer threads to sleep automatically when empty and wake instantly when producers add work. This removes spinning and polling. Blocking queues should almost always be bounded. Without a bound, a fast producer can cause unbounded memory growth and crash the process. A bounded queue introduces backpressure: when full, producers block and the system slows intake naturally. Backpressure is a key interview concept.

Coordination designs should also consider shutdown behavior. Workers should be able to exit cleanly using poison-pill tasks or shutdown flags. Mentioning graceful shutdown shows production awareness.

Scarcity problems occur when a resource has a strict concurrency limit. Examples include external APIs with request caps, download slots, database connections, GPU workers, and thread-limited subsystems. The primitive for this is a semaphore. A semaphore holds a fixed number of permits. Threads must acquire a permit before using the resource and release it after. If no permits are available, threads wait. The critical rule is that release must occur in a finally block. Otherwise permit leaks occur, capacity shrinks permanently, and the system eventually stalls.

When the scarce resource is an object rather than a count — such as database connections — use a blocking queue as an object pool. Initialize the queue with fixed resource objects. Threads take one, use it, and return it. This is the same acquire-use-release lifecycle as a semaphore but with reusable stateful objects.

Primitive selection summary: use locks or atomics for correctness, blocking queues for coordination, semaphores for scarcity limits, and blocking queues as pools for reusable scarce objects. Exception safety always requires acquire-try-finally-release structure for locks, semaphores, connections, and handles.

Performance tradeoffs matter in senior interviews. Locks are simple but can cause contention and blocking. Atomics are fast and non-blocking but limited in scope. Blocking queues provide efficient coordination but may block producers under load. Semaphores provide throttling but may not guarantee fairness without configuration. Naming these tradeoffs signals maturity.

Interview recognition flow is simple: if shared mutable state is accessed by multiple threads, think correctness and synchronization. If work flows between threads, think coordination and blocking queues. If there is a concurrency limit, think scarcity and semaphores or pools.

High-scoring interview vocabulary includes race condition, atomicity, critical section, backpressure, deadlock ordering, permit leak, bounded queue, graceful shutdown, and idempotent tasks. Explicitly naming the pattern and the primitive earns clarity points.

Final interview approach: identify the category, name the failure mode, choose the primitive, explain why it works, guard failure paths with finally blocks, and mention performance tradeoffs. That turns concurrency answers from hand-wavy to mechanically convincing.

End of file.
