import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.util.concurrent.*;

import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public class Utils {
	public static void main(String[] args) {
		dense(args[0]);
	}
	
	@SuppressWarnings("unchecked")
	public static void dense(String fn) {
		ArrayList<Double>[] data = null;
		try {
			BufferedReader in = new BufferedReader(new FileReader(fn));
			String line = null;
			
			while ( (line = in.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("#") || line.equals("")) {
					if (line.indexOf("average") >= 0) {
						break;
					}
					continue;
				}
				String[] arr = line.split("[\\s|\\t]+");
				if (data == null) {
					data = new ArrayList[arr.length];
				}
				for (int i = 0; i < arr.length; ++i) {
					if (data[i] == null) {
						data[i] = new ArrayList<Double>();
					}
					data[i].add(Double.parseDouble(arr[i]));
				}
			}
			
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for (int i = 0; i < data.length; ++i) {
			double ave = 0.0, des = 0.0;
			for (double d : data[i]) {
//				System.out.print(d + " ");
				ave += d;
				des += d*d;
			}
			double n = data[i].size();
			
			ave /= n;
			des = Math.sqrt(des/n - ave*ave);
			System.out.println(" : " + ave + " +/- " + des);
		}
	}
	
	
	static Map<String, Logger> loggerList = new HashMap<String, Logger>();
	
	public static void saveObject(String filename, Object data, boolean compressed) {
		if (filename == null || data == null) 
			return;
		
		FileOutputStream fout = null;
		GZIPOutputStream gzout = null;
		ObjectOutputStream out = null;
		try {
			fout = new FileOutputStream(filename);
			if (compressed) {
				gzout = new GZIPOutputStream(fout);
				out = new ObjectOutputStream(gzout);
			} else {
				out = new ObjectOutputStream(fout);
			}
			
			out.writeObject(data);
			out.flush();
			out.close();
			
			if (compressed)
				gzout.close();
			
			fout.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Object loadObject(String filename, boolean compressed) {
		if (filename == null || !(new File(filename)).exists())
			return null;
		
		FileInputStream fin = null;
		GZIPInputStream gzin = null;
		ObjectInputStream in = null;
		
		try {
			fin = new FileInputStream(filename);
			if (compressed) {
				gzin = new GZIPInputStream(fin);
				in = new ObjectInputStream(gzin);
			} else {
				in = new ObjectInputStream(fin);
			}
			
			Object data = in.readObject();
			in.close();
			
			if (compressed)
				gzin.close();
			
			fin.close();
			
			return data;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static Handler stdout = null;
	
	public static<T> Logger getLogger(T t) {
		String name = t.getClass().getName();
		
		Logger logger = null;
		if (loggerList.containsKey(name)) {
			logger = loggerList.get(name);
		} else {
			logger = Logger.getLogger(name);
			logger.setUseParentHandlers(false);
			
			if (stdout == null) {
				stdout = logPrint();
			}
			
//			logger.addHandler(logFile(name));
			logger.addHandler(stdout);
			
			//OFF SEVERE WARNING INFO CONFIG FINE FINER FINEST ALL
			logger.setLevel(Level.INFO);
			
			loggerList.put(name, logger);
		}
		return logger;
	}
	
	static Handler logPrint() {
		Handler handler = new ConsoleHandler();
//		handler.setFormatter(new SimpleFormatter());
		handler.setFormatter(new PrintFormatter());
		return handler;
	}
	
	static Handler logFile(String name) {
		Handler handler = null;
		try {
//			%t = a temporary directory
//			%h = the user's home directory
//			%g = generation number (for log rotation)
//			%u = a unique number
			handler = new FileHandler(name + ".log");
		} catch (SecurityException e) { 
			e.printStackTrace();
		} catch (IOException e) { 
			e.printStackTrace(); 
		}
		handler.setFormatter(new SimpleFormatter());
//		handler.setFormatter(new FileFormatter());
		return handler;
	}
	
	static class PrintFormatter extends Formatter {
		// This method is called for every log records
		public String format(LogRecord rec) {
			StringBuffer buffer = new StringBuffer(1000);
			//buffer.append(rec.getLevel() + ": ");
			//buffer.append(calcDate(rec.getMillis()));
			buffer.append(formatMessage(rec));
			//buffer.append("\t" + rec.getSourceClassName() + "@" + rec.getSourceMethodName());
			buffer.append("\n");
			return buffer.toString();
		}
		
		protected String calcDate(long millisecs) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd,yyyy HH:mm");
			return dateFormat.format(new Date(millisecs));
		}
		
		// This method is called just after the handler using this
		// formatter is created
		public String getHead(Handler h) {
			return "";
		}

		// This method is called just after the handler using this
		// formatter is closed
		public String getTail(Handler h) {
			return "\n";
		}
	}
	
	static class FileFormatter extends Formatter {
		// This method is called for every log records
		public String format(LogRecord rec) {
			StringBuffer buffer = new StringBuffer(1000);
			buffer.append(rec.getLevel());
			buffer.append(calcDate(rec.getMillis()));
			buffer.append(formatMessage(rec));
			return buffer.toString();
		}
		
		protected String calcDate(long millisecs) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd,yyyy HH:mm");
			return dateFormat.format(new Date(millisecs));
		}
		
		// This method is called just after the handler using this
		// formatter is created
		public String getHead(Handler h) {
			return null;
		}

		// This method is called just after the handler using this
		// formatter is closed
		public String getTail(Handler h) {
			return null;
		}
	}
}

//class Config {
//	public final static int TYPE_TXT = 0;
//	public final static int TYPE_XML = 1;
//	
//	Properties config;
//	
//	public static boolean test(String fn) {
//		return (new File(fn)).exists();
//	}
//	
//	public Config() {
//		config = new Properties();
//	}
//	
//	public Config(String fn) {
//		config = new Properties();
//		load(fn);
//	}
//	
//	public String get(String key) {
//		return config.getProperty(key);
//	}
//	
//	public int[] getInt(String key, int def) {
//		int[] value = null;
//		String str = get(key, null);
//		try {
//			if (str == null) {
//				value = new int[1];
//				value[0] = def;
//			} else {
//				int tmp = Integer.parseInt(str);
//				value = new int[1];
//				value[0] = tmp;
//			}
//		} catch (NumberFormatException e) {
//			String[] arr = str.split("[,| ]");
//			value = new int[arr.length];
//			for (int i = 0; i < arr.length; ++i) {
//				value[i] = Integer.parseInt(arr[i]);
//			}
//		} catch (Exception e) {
//			value = new int[1];
//			value[0] = def;
//		}
//		
//		return value;
//	}
//	
//	public String get(String key, String def) {
//		return config.getProperty(key, def);
//	}
//	
//	public void set(String key, String value) {
//		config.setProperty(key, value);
//	}
//	
//	public void print() {
//		config.list(System.out);
//	}
//	
//	public String toString() {
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		PrintStream ps = new PrintStream(out);
//		config.list(ps);
//		ps.close();
//		return out.toString();
//	}
//	
//	public void load(String file) {
//		load(file, TYPE_TXT);
//	}
//	
//	public void load(String file, int type) {
//		try {
//			if (type == TYPE_XML) {
//				config.loadFromXML(new FileInputStream(file));
//			} else {
//				config.load(new FileInputStream(file));
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//	
//	public void save(String file, String header) {
//		save(file, header, TYPE_TXT);
//	}
//	
//	public void save(String file, String header, int type) {
//		try {
//			if (type == TYPE_XML) {
//				config.storeToXML(new FileOutputStream(file), header);
//			} else {
//				config.store(new FileOutputStream(file), header);
//			}
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//}

class Data {
	static String header = "";
	static String format = "";
	
	static BufferedWriter out = null; 
	
	static void setHeader(String str) {
		header = str;
	}
	
	static void setFormat(String str) {
		format = str;
	}
	
	static void setFile(String str) {
		try {
			out = new BufferedWriter(new FileWriter(str));
			out.write(header + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static void append(String str) {
		try {
			out.write(str + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static void flush() {
		try {
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static void close() {
		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		out = null;
	}
}

class Timer {
	static boolean cout = true;
	static Stack<Long> timer = new Stack<Long>();
	
	static void start() {
		//long t = System.nanoTime();
		long t = System.currentTimeMillis();
		timer.push(t);
	}
	
	static double end(String st) {
		//long t1 = System.nanoTime();
		long t1 = System.currentTimeMillis();
		long t0 = timer.pop();
		double df = (double)(t1 - t0) / 1000.0;
		if (cout && st != null && df > 1e-4) {
			System.out.println(st + ": " + df);
		}
		return df;
	}
	
	static void set(boolean out) {
		cout = out;
	}
}

class Memory {
	final static long MB = 1024L * 1024L;
	static Stack<Long> memory = new Stack<Long>();
	
	static void start() {
		Runtime rt = Runtime.getRuntime();
		long m = rt.totalMemory() - rt.freeMemory();
		memory.push(m);
	}
	
	static long end(String st) {
		return end(st, 1);
	}
	
	static long end(String st, long ft) {
		Runtime rt = Runtime.getRuntime();
		long m1 = rt.totalMemory() - rt.freeMemory();
		long m0 = memory.pop();
		long df = (m1 - m0) / ft;
		if (st != null)
			System.out.println(st + ": " + df);
		return df;
	}
}

interface CoreTask {
	public void doTask();
}

class MultiCore implements Runnable {
//	private static CountDownLatch signal = null;
	private static ExecutorService exe = null;
	private static int numOfTask = 0;
	public static int numOfCPU = 1;
	
	public static boolean isRuning = true;
	
	static {
		numOfCPU = Runtime.getRuntime().availableProcessors();
	}
	
	private CoreTask task = null;
	
	public MultiCore(CoreTask t) {
		task = t;
	}
	
	public void run() {
		task.doTask();
//		signal.countDown();
	}
	
	public static void init() {
		isRuning = true;
		exe = Executors.newFixedThreadPool(numOfCPU);
//		signal = new CountDownLatch(numOfCPU);
	}
	
	public static boolean execute(CoreTask task) {
		if (numOfTask < numOfCPU) {
			exe.execute(new MultiCore(task));
			++numOfTask;
			return true;
		}
		return false;
	}
	
	public static void sleep(int s) {
		try {
			Thread.sleep(s * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}
	
	public static void close() {
//		try {
//			signal.await();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		isRuning = false;
		exe.shutdown();
	}
}
