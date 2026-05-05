# 1707. Maximum XOR With an Element From Array

**Link:** https://leetcode.com/problems/maximum-xor-with-an-element-from-array/description/

```java
class Solution {
    Trie head;
    public void insertIntoTrie(int num){
         Trie chead = head;
        for(int i = 31 ; i >= 0 ; i-- ){
            int bit = (num >> i) & 1;
            if(chead.node[bit] == null){
                Trie n = new Trie();
                chead.node[bit] = n;
            }
            chead = chead.node[bit];
        }
    }
    public int returnMaXor(int x){
        int ans = 0;
        Trie chead = head;
        for(int i = 31 ; i >= 0 ; i-- ){
            int xbit = (x >> i) & 1;
            int complement = xbit ^ 1;
            if(chead.node[complement] != null){
                    chead = chead.node[complement];
                    ans = ans + (int)Math.pow(2,i); 
            }
            else{
                    chead = chead.node[xbit];
            }

        }
        return ans;

    }
    public int[] maximizeXor(int[] nums, int[][] queries) {
        head = new Trie();
        Arrays.sort(nums);
        int[][] new_q = new int[queries.length][3];
        int[] ans = new int[queries.length];
        for(int i = 0 ; i < queries.length; i++){
            int x = queries[i][0];
            int m = queries[i][1];
            new_q[i][0] = x;
            new_q[i][1] = m;
            new_q[i][2] = i;

        }
        Arrays.sort(new_q,(a,b)-> Integer.compare(a[1],b[1]));
        Arrays.fill(ans,-1);

       int j = 0;
      
        
        for (int i = 0; i < new_q.length; i++) {
            int x = new_q[i][0];
            int m = new_q[i][1];
            int originalIdx = new_q[i][2];

            // Add all numbers from nums that are <= current limit m
            while (j < nums.length && nums[j] <= m) {
                insertIntoTrie(nums[j]);
                j++;
            }

            // If j is still 0, it means no number in nums was <= m
            if (j == 0) {
                ans[originalIdx] = -1;
            } else {
                ans[originalIdx] = returnMaXor(x);
            }
        }

        return ans;

    }
}
class Trie{
    Trie[] node = new Trie[2];
    
}
```
