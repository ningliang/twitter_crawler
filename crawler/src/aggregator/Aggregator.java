package aggregator;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import common.FollowerResult;
import common.ResultLogger;
import common.Status;

public class Aggregator {
	private ResultLogger resultLogger;
	private String outputDir;
	private Statistics statistics;
	private List<FollowerResult> results = new LinkedList<FollowerResult>();
	private static long PUSH_THRESHOLD = 100000;
	private Set<Integer> recordedIds = new HashSet<Integer>();
	
	public Aggregator(String outputDir) {
		this.outputDir = outputDir;
		this.statistics = new Statistics();
	}
	
	public void open() { if (resultLogger == null) this.resultLogger = new ResultLogger(outputDir + "/" + System.currentTimeMillis(), false); }
	public void close() { 
		if (resultLogger != null) {
			this.flush();
			this.resultLogger.close(); 
		}
	}
	
	public void processDir(File directory) {
		try {
			if (directory.isDirectory()) {
				for (File file : directory.listFiles()) {
					if (file.isDirectory()) processDir(file);
					else processFile(file);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public Statistics getStatistics() { return this.statistics; }
	
	private void processFile(File file) throws IOException {
		if (file.getName().endsWith(".txt")) {
			System.out.println("Processing " + file.getName());
			DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
			FollowerResult result;
			while ((result = readResult(input)) != null) {
				// Check the hash - add the result if we haven't seen it before
				if (!this.recordedIds.contains(result.getId())) {
					this.statistics.addResult(result);
					this.results.add(result);
					this.recordedIds.add(result.getId());
					if (results.size() % PUSH_THRESHOLD == 0 && results.size() > 0) this.flush();
				}
			}
			input.close();
		}
	}
	
	public void flush() { while (results.size() > 0) this.resultLogger.addResult(results.remove(0)); }
	
	// Returns a result, null, or throws an exception
	private FollowerResult readResult(DataInputStream input) throws IOException {
		FollowerResult result = null;
		try {
			int id = input.readInt();			
			if (id != -1) {			
				Status status = Status.fromInt(input.readInt());
				if (status == Status.SUCCESS) {
					int[] followerIds = new int[input.readInt()];
					for (int i = 0; i < followerIds.length; i++) followerIds[i] = input.readInt();
					result = new FollowerResult(id, status, followerIds);
				} else {
					result = new FollowerResult(id, status);
				}
			}
		} catch (EOFException e) { 
			result = null; 
		} 
		
		return result;
	}
	
	public static void main(String[] args) {
		if (args.length == 2) {
			Aggregator aggregator = new Aggregator(args[1]);
			aggregator.open();
			aggregator.processDir(new File(args[0]));
			aggregator.close();
			System.out.println(aggregator.getStatistics().toString());
		} else {
			System.out.println("Usage: inputDir logDir");
		}
	}
}
