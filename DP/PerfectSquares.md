# Perfect Squares (LeetCode #279)

## Problem
Given an integer `n`, return the least number of perfect square numbers (numbers like 1, 4, 9, 16, ...) that sum to `n`.

## Approach
This solution uses **Dynamic Programming with Memoization** to solve the problem:

1. **Generate all perfect squares** up to `n`
2. **Use recursion with memoization** to find the minimum count
3. **For each number**, we have two choices:
   - **Pick**: Use the current perfect square and recursively solve for the remaining sum
   - **Skip**: Move to the previous perfect square

## Solution

```java
class Solution {

    public void fillSquare(int limit, List<Integer> nums, int n) {
        for (int i = 1; i <= limit; i++) {
            int square = i * i;

            if (square > n) return;

            nums.add(square);
        }
    }

    public int solve(int i, int sum, List<Integer> nums, int[][] dp) {

        if (sum == 0) return 0;

        if (i == 0) {
            return sum; // since nums[0] = 1
        }

        if (dp[i][sum] != -1) {
            return dp[i][sum];
        }

        int pick = Integer.MAX_VALUE;

        if (nums.get(i) <= sum) {

            int next = solve(i, sum - nums.get(i), nums, dp);

            if (next != Integer.MAX_VALUE) {
                pick = 1 + next;
            }
        }

        int skip = solve(i - 1, sum, nums, dp);

        return dp[i][sum] = Math.min(pick, skip);
    }

    public int numSquares(int n) {

        List<Integer> nums = new ArrayList<>();

        fillSquare((int)Math.sqrt(n), nums, n);

        int[][] dp = new int[nums.size()][n + 1];

        for (int[] row : dp) {
            Arrays.fill(row, -1);
        }

        return solve(nums.size() - 1, n, nums, dp);
    }
}
```

## Complexity Analysis

| Aspect | Complexity |
|--------|-----------|
| **Time** | O(n × √n) |
| **Space** | O(n × √n) for DP table |

## Example

```
Input: n = 7
Output: 2
Explanation: 7 = 4 + 3 = 2² + 1² + 1² (minimum 2 perfect squares)

Input: n = 12
Output: 3
Explanation: 12 = 4 + 4 + 4 = 2² + 2² + 2²
```

## Key Points
- ✅ Handles memoization to avoid recalculating subproblems
- ✅ Efficiently generates all perfect squares up to n
- ✅ Works for all positive integers
