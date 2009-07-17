package common;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

public class Logger {
	private String fileName;
	private PrintWriter writer;
	
	public Logger(String fileName) {
		this.fileName = fileName;
		try {
			this.writer = new PrintWriter(new BufferedOutputStream(new FileOutputStream(this.fileName, true))); 
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public void logStatus(String status) { 
		String message = timeStamp() + ": STATUS " + status;
		this.logMessage(message);
	}
	
	public void logError(String error) { 
		String message = timeStamp() + ": ERROR " + error; 
		this.logMessage(message);
	}
	
	private void logMessage(String message) {
		try {
			System.out.println(message);
			this.writer.println(message);
			this.writer.flush();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	private static String timeStamp() { return (new Date()).toString(); }
	
	public void close() throws IOException {
		this.writer.flush();
		this.writer.close();
	}
}
