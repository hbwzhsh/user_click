package cn.clickwise.clickad.feathouse;

/**
 * 用户名的系列操作
 * 
 * @author zkyz
 */
public class KeyOpera {

	public static Area getAreaFromUid(String uid) {
		//to do
		return null;
	}
	
	public static String areaDayKey(int day,Area area)
	{
		//to do
		return "";
	}

	public static String getTimeColunm()
	{
		long score=(long)((System.currentTimeMillis()/1000)+(Math.random()*100));
		return "testcol"+score+"";
	}
}