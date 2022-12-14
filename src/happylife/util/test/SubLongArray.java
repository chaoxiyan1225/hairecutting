package happylife.util.test;

import java.util.HashMap;

public class SubLongArray {
	
	
	public static int getLongSubArray(int[] srcs)
	{
		if( srcs == null || srcs.length ==0)
			return 0;
		
		HashMap<Integer, Integer> pos = new HashMap<>();
		int left =0 ,maxLen =0;
		for(int i=0 ; i < srcs.length ; i++)
		{
			if(pos.containsKey(srcs[i]))
			{
				left = Math.max(left, pos.get(srcs[i])+1);
				//System.out.print("contains"+srcs[i]+" left = "+left);
			}
			
			pos.put(srcs[i], i);
			maxLen = Math.max(maxLen, i - left + 1);
		}
		
		return maxLen;
		
	}
	
	public static int getLongSubArray(String srcs)
	{
		if( srcs == null || srcs.length() ==0)
			return 0;
		
		HashMap<Character, Integer> pos = new HashMap<>();
		int left =0 ,maxLen =0;
		for(int i=0 ; i < srcs.length() ; i++)
		{
			if(pos.containsKey(srcs.charAt(i)))
			{
				left = Math.max(left, pos.get(srcs.charAt(i))+1);
			}
			
			pos.put(srcs.charAt(i), i);
			maxLen = Math.max(maxLen, i - left + 1);
		}
		
		return maxLen;
		
	}
	
	public static void main(String[] args)
	{
		
		int[] a1 = {1, 2, 3 ,4 ,1,2,3,1,2};
		System.out.println(getLongSubArray(a1));
		
		String ss = "abcdabcdab";
		
		System.out.println(getLongSubArray(ss));
		
	}
	
	

}
