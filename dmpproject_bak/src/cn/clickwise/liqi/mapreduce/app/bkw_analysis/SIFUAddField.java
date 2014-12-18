package cn.clickwise.liqi.mapreduce.app.bkw_analysis;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Vector;


public class SIFUAddField {

  public void addFiled(String sel_file,String field_file,String output_file) throws Exception
  {
	  FileReader sel_fr=new FileReader(new File(sel_file));
	  BufferedReader sel_br=new BufferedReader(sel_fr);
	  
	  FileReader field_fr=new FileReader(new File(field_file));
	  BufferedReader field_br=new BufferedReader(field_fr);
	  
	  FileWriter fw=new FileWriter(new File(output_file));
	  PrintWriter pw=new PrintWriter(fw);
	  
	  Hashtable sel_hash=new Hashtable();
	  String sel_line="";
	  Vector sel_vec=new Vector();
	  while((sel_line=sel_br.readLine())!=null)
	  {
		  sel_line=sel_line.trim();
		  
		  if((sel_line==null)||(sel_line.equals("")))
		  {
			  continue;
		  }
		  sel_vec.add(sel_line);
		  sel_line="http://"+sel_line;
		  if(!(sel_hash.containsKey(sel_line)))
		  {
			  sel_hash.put(sel_hash, 1);
		  }		  
	  }
	  
	  sel_fr.close();
	  sel_br.close();
	  
	  
	  String field_line="";
	  String[] seg_arr=null;
	  
	  String url="";
	  boolean is_seled=false;
	  String sel_url="";
	  while((field_line=field_br.readLine())!=null)
	  {
          field_line=field_line.trim();
		  if(( field_line==null)||( field_line.equals("")))
		  {
			  continue;
		  }
		  seg_arr=field_line.split("\\s+");
		  if(seg_arr.length<1)
		  {
			  continue;
		  }
		  url=seg_arr[0];
		  url=url.trim();
		  
		  is_seled=false;
		  for(int i=0;i<sel_vec.size();i++)
		  {
			  sel_url=sel_vec.get(i)+"";
			  sel_url=sel_url.trim();
			  if(url.indexOf(sel_url)>0)
			  {
				  is_seled=true;
			  }
		  }
		  
		  if(is_seled)
		  {
			  pw.println(field_line);
		  }
		  
		  
	  }
	  
	  
	  field_fr.close();
	  field_br.close();
	  fw.close();
	  pw.close();
	  
	  
	  
  }
	
	public static void main(String[] args) throws Exception
	{
		String sel_file="D:/project/spread_data/zimeiti/zimeiti_0218/p57.txt";
		String field_file="D:/project/spread_data/zimeiti/zimeiti_0218/url_20140211.txt";
		String output_file="D:/project/spread_data/zimeiti/zimeiti_0218/p57_20140211.txt";
		SIFUAddField sifu=new SIFUAddField();
		sifu.addFiled(sel_file, field_file, output_file);
		
	}
	
}
