# Algorithm Speed Cheat Sheet (Interview Ready)

## Rule Zero
Assume the time limit is roughly 10^8 operations.

---

## Input Size → Feasible Approaches

### N <= 20
- Exponential solutions are allowed
- 2^N, N!
- Backtracking
- Subsets and permutations
- Bitmask DP
- Recursion

### N <= 100
- O(N^3) sometimes acceptable
- DP on strings
- Interval DP
- Floyd–Warshall

### N <= 1,000
- O(N^2) is safe
- Nested loops
- Simple DP
- Graph BFS / DFS

### N <= 100,000
- O(N log N) or O(N) only
- Sorting
- Binary search
- Sliding window
- Two pointers
- HashMap
- Greedy
- Monotonic stack / deque

### N <= 1,000,000
- Strict O(N)
- Single pass
- Prefix sums
- Counting
- Frequency arrays

### N >= 10,000,000
- Very tight constraints
- Only O(N)
- Low constant factors
- Streaming-style solutions

### N ≈ 1,000,000,000
- Do not iterate
- Math-based solutions
- Binary search on answer
- Logarithmic thinking
- Observation-driven logic

---

## Common Constraint Signals → Mental Jump

- N around 10^5 → sorting or linear scan, no nested loops
- N around 10^9 → math or binary search, not iteration
- 2^N mentioned → N is small, brute force smartly
- k = 2 or k = 3 → DP state compression opportunity
- Continuous subarray or window → sliding window or two pointers
- Min or max in range → monotonic stack or deque

---

## Interview Usage

Do not mention a cheat sheet.

Say:
"Given the constraints, an O(N^2) solution will not scale, so I am targeting an O(N) or O(N log N) approach."

---

## Memory Anchor

Constraints choose the algorithm.  
The algorithm chooses the data structure.
