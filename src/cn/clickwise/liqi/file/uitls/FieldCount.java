package cn.clickwise.liqi.file.uitls;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Map.Entry;

import love.cq.util.MapCount;

import cn.clickwise.liqi.str.basic.SSO;

public class FieldCount {

	public void fieldCount(File input_file,int field_num,int index)
	{
		FileReader fr = null;
		FileInputStream fis = null;
		InputStreamReader isr = null;

		BufferedReader br = null;
		String record="";
		String[] seg_arr=null;
		String field="";
		MapCount<String> mc=new MapCount<String>();
		try {
			// fr=new FileReader(input_file);
			fis = new FileInputStream(input_file.getAbsolutePath());
			isr = new InputStreamReader(fis);
			br = new BufferedReader(isr);
			while((record=br.readLine())!=null)
			{
			  if(SSO.tioe(record))
			  {
				  continue;
			  }
			  record=record.trim();
			  seg_arr=record.split("\001");
			  if(seg_arr.length!=field_num)
			  {
				  continue;
			  }
			  
			  field=seg_arr[index];
			  if(SSO.tioe(field))
			  {
				  continue;
			  }
			  field=field.trim();
			  mc.add(field);
			}
			
			br.close();
			fr.close();
			for(Entry<String,Integer> c:mc.get().entrySet())
			{
				System.out.println(c.getKey()+":"+c.getValue());
			}
			
		} catch (Exception e) {
		}
	}
	
	public static void main(String[] args)
	{
		if(args.length!=3)
		{
			System.err.println("Usage:<input_file> <field_num> <key_index>");
			System.exit(1);
		}
		
		File input=new File(args[0]);
		FieldCount fc=new FieldCount();
		fc.fieldCount(input, Integer.parseInt(args[1]),Integer.parseInt(args[2]));
		
		
	}
	
}
