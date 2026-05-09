# Search Suggestions System

**Link:** https://leetcode.com/problems/search-suggestions-system/

## Solution

```java
class Solution {
    Trie head;
    public void  insertIntoTrie(String product){
        Trie chead = head;
        for(int i = 0 ; i < product.length() ; i++){
            if(chead.a[product.charAt(i) - 'a']==null){
                Trie n = new Trie();
                chead.a[product.charAt(i) - 'a'] = n; 
            }
            chead = chead.a[product.charAt(i) - 'a'];
            if(chead.s.size() < 3){
                chead.s.add(product);
            }
            
            }
            
        
    }
    public void traverse(List<List<String>> ans, String searchWord){
        Trie chead = head;
        boolean notFound = false;
        for( int i = 0 ; i < searchWord.length() ; i++){
            char ch = searchWord.charAt(i);
            if(!notFound)chead = chead.a[ch - 'a'];
            if(chead == null){
                ans.add(new ArrayList<>());
                notFound = true;
            }else{
            ans.add(chead.s);
            }
            
        }
    }
    public List<List<String>> suggestedProducts(String[] products, String searchWord) {
        head = new Trie();
        Arrays.sort(products);
        for(int i = 0 ; i < products.length ; i++){
            insertIntoTrie(products[i]);
        }
        List<List<String>> ans = new ArrayList<>();
        traverse(ans,searchWord);
       
        return ans;
        
    }
}
class Trie{
    Trie[] a = new Trie[26];
    List<String> s = new ArrayList<>();

}
```

## Explanation

This solution uses a **Trie (Prefix Tree)** data structure to efficiently find product suggestions based on search prefixes.

**Key Points:**
- Build a Trie from all products (sorted alphabetically)
- At each node, store up to 3 products (lexicographically smallest due to sorting)
- For each character in the search word, traverse the Trie and return the stored products
- Time Complexity: O(n * m * log(n) + k) where n = number of products, m = average length, k = search length
- Space Complexity: O(n * m) for the Trie structure
