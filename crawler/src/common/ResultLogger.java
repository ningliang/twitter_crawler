package common;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.zip.GZIPOutputStream;

public class ResultLogger {
	private LinkedList<Result> queue = new LinkedList<Result>();
	private String baseName;
	private DataOutputStream output;
	private static final String EXTENSION = ".txt";
	private Boolean gzip;
	
	// Counters and flushing parameters
	private long loggedCount = 0;
	private int segmentCount = 0;
	private static long SEGMENT_THRESHOLD = 1000000;
	
	private static final int FLUSH_THRESHOLD = 100000;
	
	// Constructor
	public ResultLogger(String baseName, Boolean gzip) {
		this.baseName = baseName;
		this.gzip = gzip;
		try {
			this.output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(this.segmentName(), true)));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/** Logging functions **/
	
	public void addResult(Result result) {
		this.queue.add(result);
		if (this.queue.size() == FLUSH_THRESHOLD) this.flush();		
	}
	
	private void flush() {
		try {
			int writeCount = this.queue.size();
			while (this.queue.size() > 0) this.writeResult(this.queue.removeFirst());
			this.output.flush();
			this.loggedCount += writeCount;
			if (this.loggedCount % SEGMENT_THRESHOLD == 0 && this.loggedCount > 0) this.segment();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	private void writeResult(Result result) throws IOException {
		this.output.writeInt(result.getId());
		this.output.writeInt(result.getStatus().toInt());
		if (result.isSuccessful()) {
			int[] followerIds = result.getFollowerIds();
			this.output.writeInt(followerIds.length);
			for (int followerId : followerIds) this.output.writeInt(followerId);
		}
	}
	
	private void segment() {
		try {
			// Close segment, start a gzip thread
			this.output.close();
			
			// Increment segment count and create new segment
			this.segmentCount += 1;
			this.output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(this.segmentName(), true)));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	// Close and gzip
	public void close() {
		try {
			this.flush();
			this.output.close();
			if (this.gzip) {
				Thread gzipper = new Thread(new GzipFileTask(this.segmentName()));
				gzipper.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	private String segmentName() { return (this.baseName + "_" + this.segmentCount + EXTENSION); }
	
	/** Accessors **/
	
	public long getLoggedCount() { return this.loggedCount; }
	public int queueSize() { return this.queue.size(); }
}

class GzipFileTask implements Runnable {
	private String fileName;
	
	GzipFileTask(String fileName) {
		this.fileName = fileName;
	}
	
	public void run() {
		try {
			String outFile = this.fileName + ".gz";
			FileInputStream in = new FileInputStream(this.fileName);
			GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(outFile));
			
			byte[] buffer = new byte[4096];
			int length = 0;
			while ((length = in.read(buffer)) > 0) out.write(buffer, 0, length); 
			in.close();
			out.finish();
			out.close();
			
			(new File(this.fileName)).delete();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}