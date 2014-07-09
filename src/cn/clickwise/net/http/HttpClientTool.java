package cn.clickwise.net.http;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.HttpClient;

public class HttpClientTool {

	HttpClient httpclient = new DefaultHttpClient();
	
	public String postUrl(String url)
	{
		String con="";
		try{
			HttpPost httppost=new HttpPost(url);
			HttpResponse response = httpclient.execute(httppost);

			// 获取响应状态
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == HttpStatus.SC_OK) {
				// 获取响应实体
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					con = EntityUtils.toString(entity);
					con=con.replaceAll("\\s+", "");
				}
			}			
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}	
		return con;
	}
	
	
	
	
	
}

