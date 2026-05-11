class Solution {
    int str_index = 0;
    public int compress(char[] chars) {
        int i = 0 ,j = 0 , n  = chars.length;
        if(n == 1)return 1;
        while(j < n){
            while(j < n && chars[j] == chars[i])j++;
            int len = j - i;
            compressChars(len,chars[i],chars);
            i = j;
        }
        return str_index;
        
    }
    public void compressChars(int num , char ch,char[] chars){
        if(num == 1){
            chars[str_index++] = ch;
            return;
        }
        chars[str_index++] = ch;
        String s = Integer.toString(num);
        for(int i = 0 ; i < s.length() ; i++) {
            chars[str_index++] = s.charAt(i);
        }
    }
}
