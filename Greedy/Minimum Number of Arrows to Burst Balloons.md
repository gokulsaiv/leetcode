[Minimum Number of Arrows to Burst Balloons](https://leetcode.com/problems/minimum-number-of-arrows-to-burst-balloons/?envType=problem-list-v2&envId=greedy)

```java
class Solution {
    public int findMinArrowShots(int[][] points) {
        Arrays.sort(points,(a,b) -> {
            // if(a[1] == b[1])return Integer.compare(b[1] , a[1]);
            return Integer.compare(a[1] , b[1]);
        });
        
        int arrowLastFired = points[0][1] , arrow = 1;
        for(int i =1 ; i < points.length ; i++){
            if(arrowLastFired >= points[i][0] && arrowLastFired <= points[i][1]){
                continue;
            }
            
            else {
                arrowLastFired = points[i][1];
                arrow++;
            }
            
        }
        return arrow;
    }
}
```

**Example:**
```
1 6
2 8
7 12
10 16
```
