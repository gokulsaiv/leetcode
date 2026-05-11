## Easy

```java
class Solution {
    public List<Integer> findValidElements(int[] nums) {
    int[] fornt_max = new int[nums.length];
    int[] rear_max = new int[nums.length];
    int fmax = nums[0];
    int rmax = nums[nums.length -1];
    List<Integer> ans = new ArrayList<>();

    for(int i = 1 ; i < nums.length -1;i++){
        fornt_max[i] = fmax;
        fmax = Math.max(fmax,nums[i]);
        rear_max[nums.length - i - 1] = rmax;
        rmax = Math.max(rmax,nums[nums.length - i - 1]);
    }

    for(int i = 0 ; i < nums.length ; i++){
        if( i == 0 || i == nums.length -1)ans.add(nums[i]);
        else if(nums[i] > fornt_max[i] || nums[i] > rear_max[i] )ans.add(nums[i]);
    }
    return ans;

    }
}
```
