link : https://leetcode.com/problems/reorder-list/description/
/**
 * Definition for singly-linked list.
 * public class ListNode {
 *     int val;
 *     ListNode next;
 *     ListNode() {}
 *     ListNode(int val) { this.val = val; }
 *     ListNode(int val, ListNode next) { this.val = val; this.next = next; }
 * }
 */
class Solution {
    // even calse i wil be at the new head;
    // odd case it wil be at the on behind the head 
    public ListNode reverseSll(ListNode root){
        if(root.next == null)return root;
        ListNode newHead = reverseSll(root.next);
        root.next.next = root;
        root.next = null;
        return newHead;
    }
    public void reorderList(ListNode head) {
        if(head.next == null)return;
        ListNode slow = head , fast = head, prev = null , shead =null;
        while(fast!=null && fast.next!=null){
            prev = slow;
            slow = slow.next;
            fast = fast.next.next;
        }

        boolean isOdd = fast!=null ? true : false;
        if(!isOdd){
            shead = prev.next;
            prev.next = null;
            shead = reverseSll(shead);
        }
        else {
            shead = slow.next;
            slow.next = null;
            shead = reverseSll(shead);
        }
       
        ListNode newList = new ListNode(-1);
        
        ListNode chead = newList;
        while(head != null && shead != null){
            
            chead.next = head;
            head = head.next;
            chead = chead.next;
            
            chead.next = shead;
            shead = shead.next;
            chead = chead.next;
            

            
            
        }
        chead.next = head;
        head = newList.next;
        
    }
}
