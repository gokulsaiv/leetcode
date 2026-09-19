# System Design: URL Shortener ID Generation (SDE 2)

## The Core Requirement
Generate a highly available, globally unique, 7-character short URL without introducing database bottlenecks or collisions.
*   **Format:** Base62 (`[a-z, A-Z, 0-9]`)
*   **Capacity:** 7 characters in Base62 = $62^7 \approx 3.52$ trillion unique combinations.

---

## Why "Snowflake + Hash + Truncate" Fails
While Snowflake IDs are great for decentralization, passing them through a hash function and truncating the output to 7 characters destroys the architecture.
*   **The Birthday Paradox:** Truncating a hash immediately introduces collisions. 
*   **Performance Hit:** Resolving collisions requires database read-checks to see if the hash is already taken, placing heavy read-load on the database during the write path.
*   **Rule of Thumb:** Never truncate a hash if you need guaranteed uniqueness. 

---

## The Standard Solution: Ticket Server + Base62 Encoding
Instead of hashing, use a centralized counter to generate unique integers, then convert them directly to Base62. Base62 is a 1-to-1 radix conversion, meaning if the input integer is unique, the output string is mathematically guaranteed to be unique.

### Architecture Flow
1. **The Ticket Server:** A dedicated, single-node MySQL database containing exactly **one row**. It uses `REPLACE INTO` to increment a counter. 
2. **Batch Allocation:** Application servers do not query the DB per request. They request a batch of IDs (e.g., 10,000 at a time) and hold them in local memory.
3. **In-Memory Generation:** When a user requests a short URL, the app server grabs the next available integer from its local memory batch and runs the Base62 conversion.
4. **Storage:** The app server saves the final mapping (`Short_Slug -> Long_URL`) to a horizontally scaled NoSQL database (e.g., DynamoDB, Cassandra).

---

## SDE 2 Edge Cases & "Cheat Codes"

### 1. Guaranteeing Exactly 7 Characters (No Padding)
If you start counting at `1`, your first URLs will only be 1 or 2 characters long. To ensure every URL is exactly 7 characters from day one, initialize the Ticket Server with a massive starting offset:
*   Set `AUTO_INCREMENT = 56800235584` (which is $62^6$).
*   The very first ID generated converts cleanly to a 7-character string.
*   *Bonus:* This provides security by obscurity, hiding your actual link volume from competitors.

### 2. Preventing Database Integer Overflow
Do not use a standard 32-bit `INT` (maxes out at 2.1 billion). You must explicitly state you are using a **64-bit `BIGINT`**. 
*   `BIGINT` maxes out at 9.22 quintillion.
*   Since a 7-character Base62 string maxes out at 3.52 trillion, the application will run out of 7-character strings millions of years before the database counter overflows.

---

## Global Scaling Strategies (Cross-Region Generation)
If you have data centers in the US, EU, and Asia, a single global Ticket Server introduces massive network latency. Use one of these two approaches to scale globally:

### Approach A: Database Offset & Step-Size
Deploy an independent Ticket Server in every region. To prevent them from generating the same IDs, configure the step-size to equal the total number of datacenters.
*   **US Server (Offset 1, Step 3):** Generates 1, 4, 7, 10...
*   **EU Server (Offset 2, Step 3):** Generates 2, 5, 8, 11...
*   **Asia Server (Offset 3, Step 3):** Generates 3, 6, 9, 12...
*   *Pros:* Zero cross-region network latency; mathematically prevents collisions.

### Approach B: Offline Key Generation Service (KGS)
Decouple generation from the user request entirely.
1. A background worker continuously generates random, mathematically unique 7-character Base62 strings.
2. It stores them in a small `unused_keys` database table.
3. Batches of these keys are pushed to local Redis clusters in every region.
4. When a user creates a link, the local app server executes an atomic `POP` command from Redis to instantly grab a pre-validated key.
*   *Pros:* Zero generation latency on the write path; highly fault-tolerant (if the generator dies, the system runs off the Redis buffer until it empties).
