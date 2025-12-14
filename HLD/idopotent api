1. Core Concepts: Idempotency vs. Concurrency

Concurrency: Deals with managing access to a shared resource by multiple users simultaneously (e.g., two users booking the same cinema seat).

Idempotency: Enables a client to safely retry an operation multiple times without changing the result beyond the initial application. It guarantees that a duplicate request does not result in a duplicate operation (e.g., charging a user twice).


Goal: One execution per unique operation, regardless of how many times the request is sent.

HTTP Method Behavior:

Idempotent by Default: GET, PUT, DELETE. Retrying these does not alter the server state differently than the first successful call.

Non-Idempotent: POST. By default, POST creates a new resource. Retries (due to timeouts) create duplicates.

2. The Problem: Why Duplicate Requests Occur
There are two main scenarios where duplicate POST requests cause issues:

Sequential (Retry on Timeout):

Client sends request -> Server processes it successfully -> Network timeout occurs before response reaches Client -> Client retries.


Result: Server processes the same request twice.

Parallel (Double Click):

Client accidentally sends two identical requests at the exact same millisecond (e.g., accidental double-click, different browser tabs).


Result: Race condition where both requests enter processing before either completes.

3. The Solution: Idempotency Keys
To handle this, we utilize an Idempotency Key (usually a UUID) passed in the HTTP Header.

The Agreement:


Client Responsibility: The client generates a unique key (UUID) for every new operation.

Scope: A new key is generated for every distinct action. Retries of the same action use the same key.

The Server-Side Workflow:

Validation: Server checks if the Idempotency-Key header exists. If missing -> Return 400 Bad Request.


Check Storage: Server looks up the key in a database or cache.

Scenario A: Key Not Found (First Request):

Create entry in DB with status "Created" or "Acquired".

Execute the business logic (e.g., Add to Cart).

Update entry status to "Consumed".

Return 201 Created.

Scenario B: Key Found (Duplicate Request):

If Status is "Consumed": The previous request succeeded. Return the cached success response (200 OK).

If Status is "Created" (In Progress): The previous request is still running. Return 409 Conflict to tell the client to wait, or block until finished.

4. Handling Concurrency (Parallel Requests)
If two requests with the same key arrive simultaneously, a race condition occurs where both might read "Key Not Found" and proceed.

Solution: Mutual Exclusion (Locking)

Identify the Critical Section: The logic where we check for the key and write the initial entry.

Mechanism: Apply a lock (Mutex, Semaphore, or Database Row Lock) on the Idempotency Key. Only one thread can enter the critical section to create the "Created" entry. The second thread will wait and then see the entry exists.

5. Amazon SysDE II Level Extensions (Advanced HLD)
The transcript touches on distributed clusters but misses several production-grade considerations required for an Amazon interview. Here is the gap analysis:

A. Distributed Storage Strategy (Transcript vs. Reality)

Transcript: Suggests using a Cache because DB replication across clusters/regions is slow.

Amazon Approach:

DynamoDB with Conditional Writes: Instead of "Read -> Check -> Write" (which requires locking), use atomic conditional writes.

Operation: PutItem where attribute_not_exists(idempotency_key).

Benefit: This makes the operation atomic without needing a separate distributed lock (Mutex) or complex cache logic. If the write fails, you know it's a duplicate.

B. Failure Scenarios (The "Error" Path)
The transcript covers Success (Consumed) and In-Progress (Created), but omits Failures.

Scenario: Server creates key -> Business Logic Crashes/Fails -> Key is stuck in "Created".

Fix: If business logic fails, catch the exception and delete the key or mark it as "Failed". This allows the client to retry successfully.

C. Time To Live (TTL)
You cannot store idempotency keys forever; storage costs will explode.

Strategy: Implement a TTL (e.g., 24 or 48 hours).

Logic: If a client retries after 48 hours, it is treated as a new request (or the client is forced to refresh). Redis and DynamoDB support native TTL.

D. The Response Payload
When returning a cached response (Scenario B), you must return the exact same body as the original success.

Implementation: The Idempotency Storage row should look like this:

Key: UUID

Status: Consumed

Response_Payload: JSON blob of the original 201 response.

E. Client-Side Jitter
When a client receives a 409 Conflict (request in progress), they should not retry immediately.

Pattern: Implement Exponential Backoff with Jitter to prevent thundering herd problems on the server.

Summary of the Data Model (Amazon Style)
Column	Type	Description
idempotency_key	String (PK)	Client-generated UUID.
status	String	IN_PROGRESS, COMPLETED, FAILED.
response_body	JSON	Cached response to return for duplicates.
expiration_time	Timestamp	TTL for auto-deletion.
user_id	String	For sharding/partitioning if necessary.
