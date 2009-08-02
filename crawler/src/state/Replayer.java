package state;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import common.FollowerResult;
import common.Status;


public class Replayer {
	private State state;
	private String dirName;
	
	// dirName indicates old log files
	public Replayer(String dirName) {
		this.state = new ArrayState(100000000);
		this.dirName = dirName;
	}
	
	// Get the replayed state
	public State getState() { return this.state; }
	
	// Replay a directory recursively - set all id statuses and rebuild the queue as appropriate
	public void replay() throws IOException { 
		this.processDir(new File(dirName));
		this.state.rebuild();
	}
	
	private void processDir(File dir) throws IOException {
		if (dir.isDirectory()) {
			for (File file : dir.listFiles()) {
				if (file.isDirectory()) processDir(file);
				else processFile(file);
			}
		} 
	}
	
	// Process a file
	private void processFile(File file) throws IOException {
		if (file.getName().endsWith(".txt")) {
			System.out.println("Processing " + file.getName());
			DataInputStream stream = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
			FollowerResult result;
			while ((result = readResult(stream)) != null) {
				if (result.isSuccessful()) {
					this.state.setComplete(result.getId());
					for (int id : result.getFollowerIds()) this.state.setQueued(id);
				} else {
					this.state.setQueued(result.getId());
				}
			}
			stream.close();
		}
	}
	
	// Read in a result from a stream
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
}
