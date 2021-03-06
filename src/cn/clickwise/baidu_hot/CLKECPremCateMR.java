package cn.clickwise.baidu_hot;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class CLKECPremCateMR {

	private static class PrepareMapper extends Mapper<Object, Text, Text, Text> {

		private Text word = new Text();
		private Text word1 = new Text();

		public void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {

			String[] seg_arr = (value.toString()).split("\001");
			String title = "";
			String cookie_str = "";
			String s_cate = "";
			if (seg_arr != null && seg_arr.length == 2) {
				title = seg_arr[0].trim();
				cookie_str = seg_arr[1].trim();
				s_cate = simple_cate(title);
				word.set(title);
				word1.set("\001" + s_cate + "\001" + cookie_str);
				//if (s_cate.equals("女")) {
					context.write(word, word1);
				//}
			}

		}

		public String simple_cate(String title) {
			String scate = "未确定";
			if ((title.indexOf("女") != -1) && (title.indexOf("男") == -1)
					&& (title.indexOf("童") == -1)) {
				scate = "女";
			}

			if ((title.indexOf("男") != -1) && (title.indexOf("女") == -1)
					&& (title.indexOf("童") == -1)) {
				scate = "男";
			}

			if ((title.indexOf("童") != -1)) {
				scate = "噪音";
			}
			if ((title.indexOf("男") != -1)&&(title.indexOf("女") != -1)) {
				scate = "噪音";
			}		
			
			return scate;
		}

	}

	private static class PrepareReducer extends Reducer<Text, Text, Text, Text> {

		protected void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {

			Iterator<Text> it = values.iterator();

			while (it.hasNext()) {
				context.write(key, it.next());
			}

		}

	}

	public static void main(String[] args) throws Exception {

		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args)
				.getRemainingArgs();
		if (otherArgs.length != 3) {
			System.err.println("Usage: CLKECPremCateMR <day> <input> <output>");
			System.exit(2);
		}
		String day = otherArgs[0];
		Job job = new Job(conf, "CLKECPremCateMR_" + day);
		job.setJarByClass(CLKECPremCateMR.class);
		job.setMapperClass(PrepareMapper.class);
		job.setReducerClass(PrepareReducer.class);
		job.setNumReduceTasks(2);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path(otherArgs[1]));
		FileOutputFormat.setOutputPath(job, new Path(otherArgs[2]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);

	}

}
