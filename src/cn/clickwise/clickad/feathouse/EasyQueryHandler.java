package cn.clickwise.clickad.feathouse;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;

import cn.clickwise.lib.string.SSO;
import cn.clickwise.rpc.FileCopyToCommand;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

/**
 * 查询用户的特征
 * 
 * @author zkyz
 * 
 */
public class EasyQueryHandler extends Handler {

	private CassandraQuery cq;

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		// TODO Auto-generated method stub
		URI uri = exchange.getRequestURI();
		System.out.println("uri:" + uri);
		String uid = SSO.afterStr(uri.toString(), "uid=");
		query(uid,exchange);
	}

	@Override
	public void setCassandraQuery(CassandraQuery cq) {
		this.cq = cq;
	}

	public void query(String uid,HttpExchange exchange)
	{
		System.out.println("in query:"+uid);
		Key key=new Key(uid);
		List<Record> result = cq.queryUid(key);
		System.out.println("after query:"+uid);
		try{
		
		Headers headers=exchange.getResponseHeaders();
		//exchange.getResponseHeaders().add("Content-type", "text/plain; charset=utf-8");
		
		headers.set("Content-type", "text/plain; charset=utf-8");
		exchange.sendResponseHeaders(1000, 1000L);
		OutputStream os = exchange.getResponseBody();
		
		PrintWriter pw=new PrintWriter(os);
		String encode="";
		for(int j=0;j<result.size();j++)
		{
		//	os.write((new String(((result.get(j).toString())+"\n").getBytes(),"GBK")).getBytes());
			//pw.println("<html>");
			//pw.println("<head>");
			//pw.println("<head>");
			//pw.println("<meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" />");
			//pw.println("<title>title</title>");
			//pw.println("</head>");
			//pw.println("<body>");
			pw.println(result.get(j).toString());
			//pw.println("</body>");
			//pw.println("<head>");
			//pw.println("</html>");
			pw.flush();
		}
		
		os.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		
	}
}
