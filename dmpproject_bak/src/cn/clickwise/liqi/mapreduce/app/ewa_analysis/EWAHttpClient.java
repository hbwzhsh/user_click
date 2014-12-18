package cn.clickwise.liqi.mapreduce.app.ewa_analysis;
import org.apache.hadoop.hbase.util.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;


public class EWAHttpClient {

public static void main(String[] args)
{
	HttpClient httpclient = new DefaultHttpClient();
	//String seg_s="\"date\":\"20140224\",\"keyword\":\"大\"";
	String seg_s="蓝鼠名品 2014夏 新款  欧美 纯色 百搭 大码 宽松 短袖 连衣裙 长裙 E237";
	String encode_seg_s=Base64.encodeBytes(seg_s.getBytes());
	//System.out.println("encode_seg_s:"+encode_seg_s);
	encode_seg_s=encode_seg_s.replaceAll("\\s+", "");
	//encode_seg_s=encode_seg_s.replaceAll("[\n]+", "");
	System.out.println("encode_seg_s:"+encode_seg_s);
    String prefix="http://42.62.29.24:8900/cate_tb/do?t=";
	// String prefix="http://192.168.110.186:11011/get";
	//String prefix="http://222.85.64.100:8901/cate_sw/do?t=";
	String url=prefix+encode_seg_s;
	
	String con="";
	try{
		HttpGet httpget = new HttpGet(url);
		HttpPost httppost=new HttpPost(url);
		System.out.println("executing request " + httpget.getURI());
		// 执行get请求.
		HttpResponse response = httpclient.execute(httppost);

		// 获取响应状态
		int statusCode = response.getStatusLine().getStatusCode();
		System.out.println("statusCode:"+statusCode);
		if (statusCode == HttpStatus.SC_OK) {
			// 获取响应实体
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				// 打印响应内容长度
				// System.out.println("Response content length: "
				// + entity.getContentLength());
				// 打印响应内容
				// System.out.println("Response content: "
				// + EntityUtils.toString(entity));
				// pw.println("Response content: "+EntityUtils.toString(entity));
				con = EntityUtils.toString(entity);
				//con="6Z6L57G7566x5YyFfOeyvuWTgeeUt+WMhXzkvJHpl7LnlLfljIUB5pac6Leo5YyF5YyFIOWls+WMheaWsOasvjIwMTPmlrDmrL4=";
				con=con.replaceAll("\\s+", "");
				System.out.println("con:"+con);
				String de_con=new String(Base64.decode(con));
    			System.out.println("de_con:"+de_con);
			}
		}
		//con="6Z6L57G7566x5YyFfOeyvuWTgeeUt+WMhXzkvJHpl7LnlLfljIUB5pac6Leo5YyF5YyFIOWls+WMheaWsOasvjIwMTPmlrDmrL4=";

		
	}
	catch(Exception e)
	{
		System.out.println(e.getMessage());
	}
	
	
}
	
	
}
