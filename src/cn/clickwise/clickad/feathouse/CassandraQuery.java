package cn.clickwise.clickad.feathouse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.Cassandra.Client;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.thrift.SliceRange;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.clickwise.lib.string.SSO;
import cn.clickwise.lib.time.TimeOpera;

public class CassandraQuery extends DataQuery {

	private Client client = null;

	private static final String UTF8 = "UTF8";

	private static final ConsistencyLevel CL = ConsistencyLevel.ONE;

	private ColumnParent cp = null;

	static Logger logger = LoggerFactory.getLogger(CassandraQuery.class);

	@Override
	public State connect(Connection con) {
		State state = new State();

		try {
			TTransport tr = new TSocket(con.getHost(), con.getPort());
			TFramedTransport tf = new TFramedTransport(tr);
			TProtocol proto = new TBinaryProtocol(tf);
			client = new Client(proto);
			tf.open();
			client.set_keyspace(con.getKeySpace());
			setCp(new ColumnParent(con.getCfName()));

			state.setStatValue(StateValue.Normal);

		} catch (Exception e) {
			state.setStatValue(StateValue.Error);
			e.printStackTrace();
		}

		return state;
	}

	@Override
	public List<Record> queryUid(Key key) {
		// TODO Auto-generated method stub
        List<Record> recordList=new ArrayList<Record>();
		SlicePredicate predicate = new SlicePredicate();
		SliceRange sliceRange = new SliceRange();
		sliceRange.setStart(new byte[0]);
		sliceRange.setFinish(new byte[0]);
		predicate.setSlice_range(sliceRange);
		ByteBuffer sendBuffer = null;
		try {
			sendBuffer = ByteBuffer.wrap(key.key.getBytes(UTF8));
			List<ColumnOrSuperColumn> results = client.get_slice(sendBuffer,
					cp, predicate, CL.ONE);

			for(ColumnOrSuperColumn result:results){
				Column column=result.column;
				recordList.add(new Record(key.key,new String(column.getValue(),UTF8)));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return recordList;
	}

	@Override
	public List<Record> queryUidTop(Key key, int top) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	State resetStatistics(Key key) {
		// TODO Auto-generated method stub
		return null;
	}

	public ColumnParent getCp() {
		return cp;
	}

	public void setCp(ColumnParent cp) {
		this.cp = cp;
	}

	public static void main(String[] args)
	{
		if (args.length != 1) {
			System.err.println("Usage:[host]");
			System.exit(1);
		}

		CassandraQuery cq = new CassandraQuery();
		Connection con = new Connection();
		con.setHost(args[0]);
		con.setPort(9160);
		con.setCfName("Urls");
		con.setKeySpace("urlstore");
		con.setColumnName("title");
		cq.connect(con);
		
		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(isr);

		OutputStreamWriter osw = new OutputStreamWriter(System.out);
		PrintWriter pw = new PrintWriter(osw);

		String line = "";
		  
	    try {
	    	long total_time=0;
	    	long query_count=0;
			while ((line = br.readLine()) != null) {
				if(Math.random()<0.98)
				{
					continue;
				}
				if (SSO.tioe(line)) {
					continue;
				}
		        line=line.trim();
				try{
					Key key=new Key(line);
					long start=TimeOpera.getCurrentTimeLong();
				    List<Record> result=cq.queryUid(key);
				    long end=TimeOpera.getCurrentTimeLong();
				    total_time+=(end-start);
				    query_count++;
				    System.out.println("Use time:"+(end-start)+" ms");
				    
				    for(int i=0;i<result.size();i++)
				    {
				    	System.out.println(result.get(i).toString());
				    }
				}
				catch(Exception e)
				{
				  Thread.sleep(1000);	
				}
				// pw.println(seg.segAnsi(line));
			}
            System.out.println("average query time:"+((double)total_time/(double)query_count));
			isr.close();
			osw.close();
			br.close();
			pw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
}
