package cn.clickwise.liqi.mapreduce.app.bkw_analysis;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import redis.clients.jedis.Jedis;

public class CLKGenderCateMR {

	private static class PrepareMapper extends Mapper<Object, Text, Text, Text> {
		private Text word = new Text();
		private Text word1 = new Text();
		public Jedis jedis;
		public static double[] line_weights;
		public static String Version;
		public static int NUM_CLASS;
		public static int NUM_WORDS;
		public static int loss_function;
		public static int kernel_type;
		public static int para_d;
		public static int para_g;
		public static int para_s;
		public static int para_r;
		public static String para_u;
		public static int NUM_FEATURES;
		public static int train_num;
		public static int suv_num;
		public static double b;
		public static double alpha;
		public static int qid;
		public static String[] label_names = { "男", "女", "噪音" };
		public String redis_gender_dict_ip = "";
		public String redis_cated_titles_ip = "";

		public int redis_port = 6379;
		public int redis_gender_dict_db = 0;
		public String seg_server = "";
		public int seg_port = 0;
		public String tag_server = "";
		public int tag_port = 0;
		public Jedis cated_redis;
		public int redis_cated_titles_db = 0;

		public void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {
			// String model_path="/home/hadoop/lq/svm_model_dir/model";
			String model_path = "gender_model";
			try {
				load_config();
				read_model(model_path);
			} catch (Exception e) {

			}
			String val = value.toString();
			String arr[] = val.split("\001");
			String title = "";
			String cookie_str = "";
			String sample = "";
			jedis = new Jedis(redis_gender_dict_ip, redis_port, 100000);// redis服务器地址
			jedis.ping();
			jedis.select(redis_gender_dict_db);
			Label label_pre = new Label();

			cated_redis = new Jedis(redis_cated_titles_ip, redis_port, 100000);
			cated_redis.ping();
			cated_redis.select(redis_cated_titles_db);

			String cate_name = "";
			String old_cate = "";
			double ran = 0;
			int rani = 0;
			String tcook="";
			try {
				if (arr.length > 1) {
					title = arr[0].trim();
					
                    System.out.println("title:"+title);
                    cookie_str = arr[1].trim();
                    String[] count_seg=null;
                    if((cookie_str!=null)&&(!cookie_str.equals("")))
                    {
                    	count_seg=cookie_str.split("\\s+");
                    }
                    //&&(count_seg.length<1000)
					if ((title != null) && (!(title.equals("")))&&(count_seg!=null)) {
						cookie_str = arr[1].trim();
						cookie_str="";
						for(int tk=1;tk<arr.length;tk++)
						{
							tcook=arr[tk];
							tcook=tcook.trim();
							if((tcook==null)||(tcook.equals("")))
							{
								continue;
							}
							cookie_str=cookie_str+tcook+" ";
						}
						cookie_str=cookie_str.trim();
						System.out.println("cookie_str:"+cookie_str);
						try {
							old_cate = cated_redis.get(title);
						} catch (Exception re) {
							ran = Math.random();
							// System.out.println("ran:" + ran);
							rani = -1;
							rani = (int) (ran * 10000);
							Thread.sleep(rani);
						}
						if (old_cate == null
								|| ((old_cate.trim()).length() == 0)) {

							sample = getSample(title);
							label_pre = docate(sample);
							cate_name = getCateName(label_pre);
							if (!(cate_name.equals("NA"))) {
								try {
									cated_redis.set(title, cate_name + "\001"
											+ cookie_str);
								} catch (Exception re) {
									ran = Math.random();
									// System.out.println("ran:" + ran);
									rani = -1;
									rani = (int) (ran * 10000);
									Thread.sleep(rani);
								}
							} else {
								try {
									cated_redis.set(title, "无类别" + "\001"
											+ cookie_str);
								} catch (Exception re) {
									ran = Math.random();
									rani = -1;
									rani = (int) (ran * 10000);
									Thread.sleep(rani);
								}
							}

							word.set(title);
							word1.set("\001" + cate_name + "\001" + cookie_str);
							context.write(word, word1);

						} else {
							old_cate = old_cate.trim();
							if ((old_cate!=null)&&(!old_cate.equals(""))&&(cookie_str!=null)&&(!cookie_str.equals(""))) {

								word = new Text();
								word1 = new Text();
								word.set(title);
								word1.set("\001" + old_cate + "\001"
										+ cookie_str);
								context.write(word, word1);
							}
						}
					}
				}

			} catch (Exception e) {

			}
		}

		public String getSample(String filter_content) throws Exception {
			String sample = "";
			String seg_s = seg(filter_content);
			seg_s = seg_s.trim();
			if (seg_s.equals("")) {
				return "";
			}

			String tag_s = tag(seg_s);
			tag_s = tag_s.trim();
			if (tag_s.equals("")) {
				return "";
			}

			String key_s = "";
			key_s = keyword_extract(tag_s);
			key_s = key_s.trim();
			if (key_s.equals("")) {
				return "";
			}

			sample = get_word_id(key_s);
			sample = sample.trim();
			if (sample.equals("")) {
				return "";
			}

			return sample;

		}

		public String seg(String s) throws Exception {
				
			s = s + "\n";
			String seg_s = "";
			String server = seg_server;
			int port = seg_port;
			try{
			Socket socket = new Socket(server, port);
            socket.setSoTimeout(10000);
			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();
			out.write(s.getBytes());
			out.flush();

			byte[] receiveBuf = new byte[10032 * 8];
			in.read(receiveBuf);

			seg_s = new String(receiveBuf);
			socket.close();
			}
			catch(Exception e){Thread.sleep(1000);}
			return seg_s;
		
		}

		public String tag(String seg_s) throws Exception {
			String tag_s = "";
			String server = tag_server;
			int port = tag_port;
			try{
			Socket socket = new Socket(server, port);
			socket.setSoTimeout(10000);
			seg_s = seg_s + "\n";
			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();
			out.write(seg_s.getBytes());
			out.flush();

			byte[] receiveBuf = new byte[10032];
			in.read(receiveBuf);

			tag_s = new String(receiveBuf);
			socket.close();
			}
			catch(Exception e){Thread.sleep(1000);}
			return tag_s;

		}

		public String keyword_extract(String text) {
			String k_s = "";
			String[] seg_arr = text.split("\\s+");
			Vector new_word_arr = new Vector();
			String[] history_word_arr = new String[7];
			for (int i = 0; i < history_word_arr.length; i++) {
				history_word_arr[i] = "";
			}

			String key_word = "";
			String subkey1 = "", subkey2 = "", subkey4 = "", subkey5 = "", subkey6 = "", subkey7 = "", subkey8 = "";

			for (int i = 0; i < seg_arr.length; i++) {
				// System.out.println(i + ":" + seg_arr[i]);
				if (((seg_arr[i].indexOf("/NN")) != -1)
						|| ((seg_arr[i].indexOf("/NR")) != -1)) {
					key_word = seg_arr[i];
					if ((seg_arr[i].indexOf("/NN")) != -1) {
						key_word = key_word.replaceAll("/NN", "");
					} else if ((seg_arr[i].indexOf("/NR")) != -1) {
						key_word = key_word.replaceAll("/NR", "");
					}
					key_word = key_word.trim();
					if (key_word.length() > 1) {
						new_word_arr.add(key_word);
						if ((key_word.length()) == 3) {
							subkey1 = key_word.substring(0, 2);
							subkey2 = key_word.substring(1, 3);
							new_word_arr.add(subkey1);
							new_word_arr.add(subkey2);
						}

						if ((key_word.length()) == 4) {
							subkey4 = key_word.substring(0, 2);
							subkey5 = key_word.substring(1, 3);
							subkey6 = key_word.substring(2, 4);
							subkey7 = key_word.substring(0, 3);
							subkey8 = key_word.substring(1, 4);
							new_word_arr.add(subkey4);
							new_word_arr.add(subkey5);
							new_word_arr.add(subkey6);
							new_word_arr.add(subkey7);
							new_word_arr.add(subkey8);
						}
					}

				} else if (seg_arr[i].length() > 5) {
					key_word = seg_arr[i];
					key_word = key_word.replaceAll("/.*", "");
					key_word = key_word.trim();
					new_word_arr.add(key_word);
				}

				if (i > 4) {
					history_word_arr[0] = seg_arr[i - 5];
					history_word_arr[1] = seg_arr[i - 4];
					history_word_arr[2] = seg_arr[i - 3];
					history_word_arr[3] = seg_arr[i - 2];
					history_word_arr[4] = seg_arr[i - 1];
					history_word_arr[5] = seg_arr[i];
					if (((history_word_arr[0].indexOf("/NN")) != -1)
							&& ((history_word_arr[1].indexOf("/NN")) != -1)
							&& ((history_word_arr[2].indexOf("/NN")) != -1)
							&& ((history_word_arr[3].indexOf("/NN")) != -1)
							&& ((history_word_arr[4].indexOf("/NN")) != -1)
							&& ((history_word_arr[5].indexOf("/NN")) != -1)) {
						history_word_arr[0] = history_word_arr[0].replaceAll(
								"/NN", "").trim();
						history_word_arr[1] = history_word_arr[1].replaceAll(
								"/NN", "").trim();
						history_word_arr[2] = history_word_arr[2].replaceAll(
								"/NN", "").trim();
						history_word_arr[3] = history_word_arr[3].replaceAll(
								"/NN", "").trim();
						history_word_arr[4] = history_word_arr[4].replaceAll(
								"/NN", "").trim();
						history_word_arr[5] = history_word_arr[5].replaceAll(
								"/NN", "").trim();
						new_word_arr.add(history_word_arr[0]
								+ history_word_arr[1] + history_word_arr[2]
								+ history_word_arr[3] + history_word_arr[4]
								+ history_word_arr[5]);
					}
					history_word_arr[0] = "";
					history_word_arr[1] = "";
					history_word_arr[2] = "";
					history_word_arr[3] = "";
					history_word_arr[4] = "";
					history_word_arr[5] = "";
				}

				if (i > 3) {
					history_word_arr[0] = seg_arr[i - 4];
					history_word_arr[1] = seg_arr[i - 3];
					history_word_arr[2] = seg_arr[i - 2];
					history_word_arr[3] = seg_arr[i - 1];
					history_word_arr[4] = seg_arr[i];
					if (((history_word_arr[0].indexOf("/NN")) != -1)
							&& ((history_word_arr[1].indexOf("/NN")) != -1)
							&& ((history_word_arr[2].indexOf("/NN")) != -1)
							&& ((history_word_arr[3].indexOf("/NN")) != -1)
							&& ((history_word_arr[4].indexOf("/NN")) != -1)) {
						history_word_arr[0] = history_word_arr[0].replaceAll(
								"/NN", "").trim();
						history_word_arr[1] = history_word_arr[1].replaceAll(
								"/NN", "").trim();
						history_word_arr[2] = history_word_arr[2].replaceAll(
								"/NN", "").trim();
						history_word_arr[3] = history_word_arr[3].replaceAll(
								"/NN", "").trim();
						history_word_arr[4] = history_word_arr[4].replaceAll(
								"/NN", "").trim();

						new_word_arr.add(history_word_arr[0]
								+ history_word_arr[1] + history_word_arr[2]
								+ history_word_arr[3] + history_word_arr[4]);
					}

					history_word_arr[0] = "";
					history_word_arr[1] = "";
					history_word_arr[2] = "";
					history_word_arr[3] = "";
					history_word_arr[4] = "";
				}

				if (i > 2) {
					history_word_arr[0] = seg_arr[i - 3];
					history_word_arr[1] = seg_arr[i - 2];
					history_word_arr[2] = seg_arr[i - 1];
					history_word_arr[3] = seg_arr[i];
					if (((history_word_arr[0].indexOf("/NN")) != -1)
							&& ((history_word_arr[1].indexOf("/NN")) != -1)
							&& ((history_word_arr[2].indexOf("/NN")) != -1)
							&& ((history_word_arr[3].indexOf("/NN")) != -1)) {
						history_word_arr[0] = history_word_arr[0].replaceAll(
								"/NN", "").trim();
						history_word_arr[1] = history_word_arr[1].replaceAll(
								"/NN", "").trim();
						history_word_arr[2] = history_word_arr[2].replaceAll(
								"/NN", "").trim();
						history_word_arr[3] = history_word_arr[3].replaceAll(
								"/NN", "").trim();
						new_word_arr.add(history_word_arr[0]
								+ history_word_arr[1] + history_word_arr[2]
								+ history_word_arr[3]);
					}

					history_word_arr[0] = "";
					history_word_arr[1] = "";
					history_word_arr[2] = "";
					history_word_arr[3] = "";

				}

				if (i > 1) {
					history_word_arr[0] = seg_arr[i - 2];
					history_word_arr[1] = seg_arr[i - 1];
					history_word_arr[2] = seg_arr[i];
					if (((history_word_arr[0].indexOf("/NN")) != -1)
							&& ((history_word_arr[1].indexOf("/NN")) != -1)
							&& ((history_word_arr[2].indexOf("/NN")) != -1)) {
						history_word_arr[0] = history_word_arr[0].replaceAll(
								"/NN", "").trim();
						history_word_arr[1] = history_word_arr[1].replaceAll(
								"/NN", "").trim();
						history_word_arr[2] = history_word_arr[2].replaceAll(
								"/NN", "").trim();
						new_word_arr.add(history_word_arr[0]
								+ history_word_arr[1] + history_word_arr[2]);
					}

					history_word_arr[0] = "";
					history_word_arr[1] = "";
					history_word_arr[2] = "";
				}

				if (i > 0) {
					history_word_arr[0] = seg_arr[i - 1];
					history_word_arr[1] = seg_arr[i];
					// System.out
					// .println("history_word_arr[0]:" + history_word_arr[0]);
					// System.out
					// .println("history_word_arr[1]:" + history_word_arr[1]);
					// System.out.println((history_word_arr[0].indexOf("/NN")) +
					// ":"
					// + (history_word_arr[1].indexOf("/NN")));
					if (((history_word_arr[0].indexOf("/NN")) != -1)
							&& ((history_word_arr[1].indexOf("/NN")) != -1)) {
						// System.out.println("add the:"
						// + (history_word_arr[0] + history_word_arr[1]));
						history_word_arr[0] = history_word_arr[0].replaceAll(
								"/NN", "").trim();
						history_word_arr[1] = history_word_arr[1].replaceAll(
								"/NN", "").trim();
						new_word_arr.add(history_word_arr[0]
								+ history_word_arr[1]);
					}

					history_word_arr[0] = "";
					history_word_arr[1] = "";

				}
			}

			String temp_CC = "";
			for (int i = 0; i < new_word_arr.size(); i++) {
				temp_CC = new_word_arr.get(i) + "";
				if (!(Pattern.matches("[a-zA-Z%0-9\\\\\\\\_\\#]*", temp_CC))) {
					k_s = k_s + temp_CC + " ";
				}
			}

			return k_s;
		}

		public String get_word_id(String s) {
			String words[] = s.split("[\\s]+");
			String res = "";
			String ids = "";
			HashMap<Long, Integer> cnts = new HashMap<Long, Integer>();
			for (int i = 0; i < words.length; i++) {
				try {
					ids = jedis.get(words[i]);
				} catch (Exception re) {

				}
				if (ids == null) {
					continue;
				}
				Long id = Long.parseLong(ids);
				if (id != null) {
					Integer cnt = cnts.get(id);
					if (cnt == null)
						cnts.put(id, 1);
					else
						cnts.put(id, cnt + 1);
				}
			}
			List<Long> keys = new ArrayList<Long>(cnts.keySet());
			Collections.sort(keys, new Comparator<Long>() {
				public int compare(Long l1, Long l2) {
					if (l1 > l2)
						return 1;
					else if (l1 < l2)
						return -1;
					return 0;
				}
			});

			for (int i = 0; i < keys.size(); i++) {
				Long l = keys.get(i);
				if (i == 0)
					res += l + ":" + cnts.get(l);
				else
					res += " " + l + ":" + cnts.get(l);
			}
			return res;
		}

		public Label docate(String sample_line) {

			Label y = null;
			Word[] sample = null;

			String[] sample_arr = sample_line.split("\\s+");
			sample = new Word[sample_arr.length];
			for (int i = 0; i < sample.length; i++) {
				sample[i] = new Word();
			}

			String temp_token = "";
			int temp_index = 0;
			double temp_weight = 0.0;

			for (int i = 0; i < sample_arr.length; i++) {
				// System.out.println(i+" "+sample_arr[i]);
				temp_token = sample_arr[i];
				if (Pattern.matches("\\d+:[\\d\\.]+", temp_token)) {
					temp_index = Integer.parseInt(temp_token.substring(0,
							temp_token.indexOf(":")));
					temp_weight = Double.parseDouble(temp_token.substring(
							temp_token.indexOf(":") + 1, temp_token.length()));
					sample[i].wnum = temp_index;
					sample[i].weight = temp_weight;
				}
			}

			// sample=getWords(sample_line);
			y = classify_struct_example(sample);
			// System.out.println("y.first_label:"+y.first_class+"  y.second_label:"+y.second_class);
			return y;
		}

		public Label classify_struct_example(Word[] sample) {

			Label y = null;
			double score = 0;

			Label best_label = null;
			double best_score = -1;

			Word[] fvec = null;
			for (int i = 0; i < 4; i++) {
				y = new Label();
				y.first_class = (i + 1);
				fvec = psi(sample, y);
				score = classify_example(fvec);
				if (score > best_score) {
					best_score = score;
					best_label = y;
				}
			}
			best_label.score = best_score;
			return best_label;
		}

		public class Word {
			int wnum;
			double weight;
		}

		public class Label {
			int first_class;
			double score;
		}

		public Word[] psi(Word[] sample, Label y) {
			Word[] fvec = null;
			int veclength = (sample.length) * NUM_CLASS;
			fvec = new Word[veclength];
			for (int i = 0; i < veclength; i++) {
				fvec[i] = new Word();
			}

			int c1 = y.first_class;
			Word temp_word = null;
			int fi = 0;
			// System.out.println();
			for (int i = 0; i < sample.length; i++) {
				temp_word = sample[i];
				// System.out.print(temp_word.wnum+":"+temp_word.weight+" ");
			}
			// System.out.println();
			// System.out.println("y.first_class:"+y.first_class+" "+y.second_class);
			// 第一级类别特征
			for (int i = 0; i < sample.length; i++) {
				temp_word = sample[i];
				fvec[fi].wnum = temp_word.wnum + (c1 - 1) * NUM_WORDS;
				fvec[fi].weight = temp_word.weight;
				// System.out.print(fvec[fi].wnum+":"+fvec[fi].weight+" ");
				fi++;

			}

			// System.out.println();
			return fvec;
		}

		public double classify_example(Word[] fvec) {
			double score = 0;
			Word samp_word = null;

			for (int i = 0; i < fvec.length; i++) {
				samp_word = fvec[i];
				if (samp_word.wnum < NUM_FEATURES) {
					score = score + samp_word.weight
							* line_weights[samp_word.wnum];
				}
			}

			return score;
		}

		public String getCateName(Label y) {
			String cate_name = "";
			int tempid = y.first_class;
			if ((tempid >= 1) && (tempid <= 4)) {
				cate_name = label_names[tempid - 1];
			} else {
				cate_name = "NA";
			}

			return cate_name;
		}

		public void read_model(String model_path) throws Exception {

			InputStream model_is = this.getClass().getResourceAsStream(
					"/" + model_path);
			InputStreamReader model_isr = new InputStreamReader(model_is);
			// File model_file = new File(model_path);
			// FileReader fr = new FileReader(model_file);
			BufferedReader br = new BufferedReader(model_isr);
			Version = cut_comment(br.readLine());
			NUM_CLASS = Integer.parseInt(cut_comment(br.readLine()));
			NUM_WORDS = Integer.parseInt(cut_comment(br.readLine()));
			// System.out.println("NUM_WORDS:" + NUM_WORDS);
			loss_function = Integer.parseInt(cut_comment(br.readLine()));
			kernel_type = Integer.parseInt(cut_comment(br.readLine()));
			para_d = Integer.parseInt(cut_comment(br.readLine()));
			para_g = Integer.parseInt(cut_comment(br.readLine()));
			para_s = Integer.parseInt(cut_comment(br.readLine()));
			para_r = Integer.parseInt(cut_comment(br.readLine()));
			para_u = cut_comment(br.readLine());
			NUM_FEATURES = Integer.parseInt(cut_comment(br.readLine()));
			// System.out.println("NUM_FEATURES:" + NUM_FEATURES);
			train_num = Integer.parseInt(cut_comment(br.readLine()));
			suv_num = Integer.parseInt(cut_comment(br.readLine()));
			b = Double.parseDouble(cut_comment(br.readLine()));
			line_weights = new double[NUM_FEATURES + 2];
			for (int i = 0; i < line_weights.length; i++) {
				line_weights[i] = 0;
			}
			String line = br.readLine();
			StringTokenizer st = new StringTokenizer(line, " ");
			// System.out.println("st.count:" + st.countTokens());
			// System.out.println("end:" + line.substring(0, 1000));

			int current_pos = 0;
			int forward_num = 0;
			String temp_token = "";
			int temp_index;
			double temp_weight;
			int max_index = -1;
			int search_blank = 0;
			// System.out.println("line.length:" + line.length());
			while (current_pos < (line.length())) {
				// if((current_pos%10000==0))
				// {
				// System.out.println("current_pos:"+current_pos);
				// }
				forward_num = 0;
				temp_token = "";
				while ((current_pos + forward_num) < (line.length())) {
					// if(current_pos>26080000)
					// {
					// System.out.println("current_pos+forward_num:"+(current_pos+forward_num));
					// System.out.println("cc:"+line.charAt(current_pos+forward_num));
					// }

					if (((line.charAt(current_pos + forward_num)) != ' ')
							&& ((line.charAt(current_pos + forward_num)) != '#')) {
						temp_token = temp_token
								+ line.charAt(current_pos + forward_num);
						forward_num++;
					} else {
						temp_token = temp_token.trim();
						// if(current_pos>26080000)
						// System.out.println("temp_token:"+temp_token);
						if (((temp_token.indexOf(":")) == -1)
								&& (!temp_token.equals(""))) {
							alpha = Double.parseDouble(temp_token);
						} else if ((temp_token.indexOf("qid")) != -1) {
							qid = Integer.parseInt(temp_token
									.substring(temp_token.indexOf(":") + 1),
									temp_token.length());
						} else if (Pattern
								.matches("\\d+:[\\d\\.]+", temp_token)) {
							temp_index = Integer.parseInt(temp_token.substring(
									0, temp_token.indexOf(":")));
							temp_weight = Double.parseDouble(temp_token
									.substring(temp_token.indexOf(":") + 1,
											temp_token.length()));
							line_weights[temp_index] = temp_weight;
							if (temp_index > max_index) {
								max_index = temp_index;
							}
						}
						search_blank = 0;
						while ((current_pos + forward_num + search_blank) < line
								.length()) {
							if (line.charAt(current_pos + forward_num
									+ search_blank) == ' ') {
								search_blank++;
							} else {
								break;
							}
						}
						// if((current_pos%10000==0)||current_pos>26080000)
						// {
						// System.out.println("forward_num+search_blank:"+(forward_num+search_blank));
						// }
						if ((line.charAt(current_pos + forward_num)) == '#') {
							forward_num++;
						}
						current_pos = current_pos + forward_num + search_blank;
						break;
					}
				}
			}
			// fr.close();
			model_is.close();
			model_isr.close();
			br.close();
			// System.out.println("max_index:" + max_index);
		}

		public String cut_comment(String s) {
			String cut_s = "";
			if ((s.indexOf("#")) != -1) {
				cut_s = s.substring(0, s.indexOf("#"));
			} else {
				cut_s = s;
			}
			cut_s = cut_s.trim();
			return cut_s;
		}

		public void load_config() throws Exception {
			Properties prop = new Properties();
			// URL is = this.getClass().getResource("conf/config.properties");
			InputStream model_is = this.getClass().getResourceAsStream(
					"/jbkw_config.properties");
			prop.load(model_is);

			redis_gender_dict_ip = prop.getProperty("redis_gender_dict_ip");
			redis_cated_titles_ip = prop.getProperty("redis_cated_titles_ip");

			redis_port = Integer.parseInt(prop.getProperty("redis_port"));
			redis_gender_dict_db = Integer.parseInt(prop
					.getProperty("redis_gender_dict_db"));
			seg_server = prop.getProperty("seg_server");
			seg_port = Integer.parseInt(prop.getProperty("seg_port"));
			tag_server = prop.getProperty("tag_server");
			tag_port = Integer.parseInt(prop.getProperty("tag_port"));
			redis_cated_titles_db = Integer.parseInt(prop
					.getProperty("redis_cated_titles_db"));

		}
		
		public String merge_cookie_str(String old_cookie_str,String cookie_str)
		{
			String new_cookie_str="";
			Hashtable cookie_hash=new Hashtable();
			String[] seg_arr=null;
			seg_arr=old_cookie_str.split("\\s+");
			String cookie="";
			for(int i=0;i<seg_arr.length;i++)
			{
				cookie=seg_arr[i].trim();
				//System.out.println("cookie:"+cookie);
				if(!cookie.equals(""))
				{
					if(!(cookie_hash.containsKey(cookie)))
					{
						cookie_hash.put(cookie, cookie);
					}
				}				
			}
			
			
			seg_arr=cookie_str.split("\\s+");
			for(int i=0;i<seg_arr.length;i++)
			{
				cookie=seg_arr[i].trim();
				if(!cookie.equals(""))
				{
					if(!(cookie_hash.containsKey(cookie)))
					{
						cookie_hash.put(cookie, cookie);
					}
				}				
			}
			
			
			Enumeration enum_cook=cookie_hash.keys();
			while(enum_cook.hasMoreElements())
			{
				cookie=enum_cook.nextElement()+"";
				cookie=cookie.trim();
				if(!(cookie.equals("")))
				{
					new_cookie_str=new_cookie_str+cookie+" ";
				}
			}
			
			new_cookie_str=new_cookie_str.trim();			
			return new_cookie_str;
		}
		

	}

	private static class PrepareReducer extends
			Reducer<Text, Text, IntWritable, Text> {
		private Text result = new Text();
		private IntWritable pvs_key = new IntWritable();

		protected void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			String anchor_text = key.toString();
			int sum_pvs = 0;
			Iterator<Text> it = values.iterator();
			String temp_s = "";
			String[] seg_arr = null;
			String host = "";
			String url = "";
			String pvs = "";
			String uvs = "";
			String ips = "";

			String url_s = "";
			url_s = anchor_text + "\001";
			Vector ips_urls = new Vector();

			while (it.hasNext()) {
				temp_s = it.next().toString();
				temp_s = temp_s.trim();
				if (temp_s.length() < 1) {
					continue;
				}
				seg_arr = temp_s.split("\001");
				if (seg_arr.length != 5) {
					continue;
				}
				host = seg_arr[0].trim();
				url = seg_arr[1].trim();
				pvs = seg_arr[2].trim();
				uvs = seg_arr[3].trim();
				ips = seg_arr[4].trim();
				sum_pvs += Integer.parseInt(uvs);
				// url_s=url_s+url+" ";
				ips_urls.add(uvs + "\001" + url);
			}
			Vector n_ips_urls = null;
			// n_ips_urls=rankVector(ips_urls);
			String[] n_seg_arr = null;
			String temp_ips_url = "";
			for (int i = 0; i < n_ips_urls.size(); i++) {
				temp_ips_url = n_ips_urls.get(i) + "";
				temp_ips_url = temp_ips_url.trim();
				n_seg_arr = temp_ips_url.split("\001");
				if (n_seg_arr.length < 2) {
					continue;
				}
				url_s = url_s + n_seg_arr[1] + " ";
			}

			url_s = url_s.trim();
			pvs_key.set(1000000 - sum_pvs);
			result.set(url_s);

			context.write(pvs_key, result);
		}
	}

	public static void main(String[] args) throws Exception {

		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args)
				.getRemainingArgs();
		if (otherArgs.length != 3) {
			System.err.println("Usage: CLKGenderCateMR <day> <input> <output>");
			System.exit(2);
		}
		// PrepareMapper.init_dict("dict_video.txt");
		String day = otherArgs[0];
		Job job = new Job(conf, "CLKGenderCateMR_" + day);
		job.setJarByClass(CLKGenderCateMR.class);
		job.setMapperClass(PrepareMapper.class);

		// job.setReducerClass(PrepareReducer.class);
		job.setNumReduceTasks(0);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);

		// job.setOutputKeyClass(IntWritable.class);
		// job.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path(otherArgs[1]));
		FileOutputFormat.setOutputPath(job, new Path(otherArgs[2]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);

	}

}
