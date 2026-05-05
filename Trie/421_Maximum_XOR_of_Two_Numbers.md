# 421. Maximum XOR of Two Numbers in an Array

## Problem Links
- **LeetCode Problem**: [Maximum XOR of Two Numbers in an Array](https://leetcode.com/problems/maximum-xor-of-two-numbers-in-an-array/description/)
- **GitHub Code**: [View Original File](https://github.com/gokulsaiv/leetcode/blob/main/Trie/421.%20Maximum%20XOR%20of%20Two%20Numbers%20in%20an%20Array(%20Google))

## Solution

This solution uses a **Trie (Binary Tree)** data structure to efficiently find the maximum XOR of two numbers in an array.

### Approach
1. Build a Trie where each node represents a bit (0 or 1)
2. Insert all numbers into the Trie in binary form
3. For each number, traverse the Trie trying to take the opposite bit at each level to maximize XOR
4. Return the maximum XOR value found

### Code

```java
class Solution {
    Trie head;

    public void insertIntoTrie(int nums){
        Trie chead = head;
        for(int j = 31 ; j >= 0 ; j--){
            int bit = (nums >> j) & 1;
            
            if(chead.node[bit] == null){
                Trie n = new Trie();
                chead.node[bit] = n;
            }
            chead = chead.node[bit];
        }
        
    }
    public int findMaximum(int num){
        Trie chead = head;
        int res = 0;
        for(int i = 31 ; i >= 0 ; i --){
            int bit = (num >> i) & 1;
            int compliment = bit ^ 1;
            if(chead.node[compliment] != null){
                res = res + (int)Math.pow(2,i);
                chead = chead.node[compliment];
            }
            else{
                
               chead = chead.node[bit];
            }
        }
        return res;
    }
    public int findMaximumXOR(int[] nums) {
        head =  new Trie();
        int max = 0;
        for(int i = 0 ; i < nums.length ; i++){
            insertIntoTrie(nums[i]);
        }
         for(int i = 0 ; i < nums.length ; i++){
            max = Math.max(max,findMaximum(nums[i]));
        }
        return max;
    }
}

class Trie { 
    Trie[] node = new Trie[2];
}
```

### Complexity Analysis
- **Time Complexity**: O(n × 32) = O(n) where n is the number of elements (32 bits for integers)
- **Space Complexity**: O(n × 32) for the Trie structure

### Company
- Google

---
*Last Updated: 2026-05-05*
