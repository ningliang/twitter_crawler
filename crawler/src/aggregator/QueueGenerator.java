package aggregator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import state.Replayer;
import state.State;

// Takes a directory of log files and generates a set of queue files
// Generates three files - completed, failed and queued
public class QueueGenerator {
	private Replayer replayer;
	
	public QueueGenerator(String dirName) {
		this.replayer = new Replayer(dirName);
	}
	
	public void generate() {
		try {
			this.replayer.replay();
			State state = this.replayer.getState();
			List<Integer> toWrite = state.getCompleted();
			writeIds(toWrite, "completed.txt");
			toWrite = state.getQueued();
			writeIds(toWrite, "queued.txt");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	private void writeIds(List<Integer> ids, String outfile) throws IOException {
		BufferedWriter output = new BufferedWriter(new FileWriter(outfile, true));
		for (Integer id : ids) output.write(id + "\n");
		output.close();
	}
	
	public static void main(String[] args) {
		if (args.length == 1) {
			QueueGenerator queueGenerator = new QueueGenerator(args[0]);
			queueGenerator.generate();
		} else {
			System.out.println("Usage: logDir");
		}
	}
}
