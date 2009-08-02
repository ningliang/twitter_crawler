package controller;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import message.Message;
import state.ArrayState;
import state.State;

import common.Constants;
import common.Logger;
import common.Result;

public class Controller {
	private static final int JOB_MAX = 20000;
	private static final int TOTAL = 80000000;
	
	private ServerSocket serverSocket;
	private ExecutorService threadPool = Executors.newCachedThreadPool();
	private List<WorkerRemote> workers = new LinkedList<WorkerRemote>();
	private Registrar registrar;
	
	// Hash sets that keep track of jobs
	private int seed;
	private State state = new ArrayState(TOTAL);
	private long startTime;
	
	private Logger statusLogger = new Logger("controller_log.txt");
	
	// Constructor
	public Controller(int listenPort, int seed) {
		try {
			this.serverSocket = new ServerSocket(listenPort);
			this.registrar = new Registrar(this, this.serverSocket);
			this.seed = seed;
		} catch (Exception e) {
			this.statusLogger.logError(e.toString());
		}
	}
	
	// Run given completed/queued files
	public Controller(int listenPort, String completedFile, String queuedFile) {
		try {
			this.serverSocket = new ServerSocket(listenPort);
			this.registrar = new Registrar(this, this.serverSocket);
			
			// Get the queued and completed ids
			BufferedReader completed = new BufferedReader(new FileReader(completedFile));
			BufferedReader queued = new BufferedReader(new FileReader(queuedFile));
			String nextLine = null;
			while ((nextLine = completed.readLine()) != null) this.state.setComplete(Integer.parseInt(nextLine));
			while ((nextLine = queued.readLine()) != null) this.state.setQueued(Integer.parseInt(nextLine));
			completed.close();
			queued.close();
			this.state.rebuild();	
			System.out.println(this.state.getCompletedCount() + " completed.");
			System.out.println(this.state.getQueuedCount() + " queued.");
		} catch (Exception e) {
			this.statusLogger.logError(e.toString());
		}
	}
	
	// Run
	public void start() {
		try {
			// Initialize registrar, seed, start time
			this.threadPool.execute(this.registrar);
			this.startTime = System.currentTimeMillis();
			if (this.state.getCompletedCount() == 0 && this.seed > 0) this.state.enqueueId(this.seed);
			
			// Main job loop
			while (this.state.getQueuedCount() > 0 || this.state.getPendingCount() > 0) {
				this.retrieveResults();
				this.handleFailures();
				this.assignJobs();
			}
			
			// Shut down workers
			synchronized(this.workers) { for (WorkerRemote worker : this.workers) worker.stop(true); }
			this.statusLogger.close();
			this.threadPool.awaitTermination(Constants.TIMEOUT, TimeUnit.MILLISECONDS);
			
			// Done!
			this.statusLogger.logStatus("DONE: " + (this.state.getCompletedCount()) + " crawled.");
			this.statusLogger.close();
		} catch (Exception e) {
			this.statusLogger.logError(e.toString());
		}
	}
	
	// Check for results, enter them into state
	private void retrieveResults() {
		synchronized(this.workers) {
			Iterator<WorkerRemote> iter = this.workers.iterator();
			while (iter.hasNext()) {
				WorkerRemote current = iter.next();
				LinkedList<Result> results = current.popResults();
				if (results.size() > 0) {
					for (Result result : results) {
						int id = result.getId();
						
						// Update state
						this.state.completeId(id);
						if (result.isSuccessful()) {
							for (int followerId : result.toQueue()) this.state.enqueueId(followerId);
						}
						
						// Log status every 10k 
						long completedCount = this.state.getCompletedCount();
						if (completedCount % 10000 == 0 && completedCount > 0) {
							long nowMilli = System.currentTimeMillis();
							this.statusLogger.logStatus("Completed " + completedCount + " at " + ((nowMilli - this.startTime)/1000) + " seconds");
						}
					}
				}
			}
		}
	}
	
	// Assign jobs to workers with no pending ids
	private void assignJobs() {
		synchronized(this.workers) {
			Iterator<WorkerRemote> iter = this.workers.iterator();
			while (iter.hasNext()) {
				WorkerRemote current = iter.next();
				if (current.isRunning() && current.nonePending() && this.state.getQueuedCount() > 0) {
					// Update the state
					List<Integer> toCrawl = this.state.checkoutIds(JOB_MAX);
					
					// Add to current
					current.pushIds(toCrawl);
					
					// Log a status message
					this.statusLogger.logStatus(this.state.getQueuedCount() + " in queue, " + this.workers.size() + " workers.");
					this.statusLogger.logStatus("Assigned " + toCrawl.size() + " ids to " + current.getWorkerName() + " with account " + current.getUserName());
				}
			}	
		}
	}
	
	// Handle failed workers by rolling back
	private void handleFailures() {
		synchronized (this.workers) {
			Iterator<WorkerRemote> iter = this.workers.iterator();
			while (iter.hasNext()) {
				WorkerRemote current = iter.next();
				if (!current.isRunning()) {
					List<Integer> pending = current.pendingIds();
					for (Integer id : pending) this.state.rollbackId(id);
					iter.remove();
					this.statusLogger.logStatus("Worker " + current.getWorkerName() + " with account " + current.getUserName() + " dropped out, rolled back.");
				}
			}
		}
	}
	
	// Add a worker to the worker set
	// Accesses synchronized workers
	public void addWorker(WorkerRemote worker) {
		worker.start();
		synchronized(this.workers) {
			this.workers.add(worker);
		}
		this.statusLogger.logStatus("Added worker " + worker.getWorkerName() + " using account " + worker.getUserName());
	}
	
	// Validate a message
	public boolean validate(Message m) { return m.valid(Constants.SECRET); }	
	
	// Main program
	public static void main(String[] args) throws UnknownHostException {
		if (args.length == 2) {
			Controller c = new Controller(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
			c.start();
		} else if (args.length == 3) {
			Controller c = new Controller(Integer.parseInt(args[0]), args[1], args[2]);
			c.start();
		} else {
			printUsage();
		}
	}
	
	public static void printUsage() {
		System.out.println("Don't forget to use -XmsM and -XmxM options, where M is like 256m, 1g, etc.");
		System.out.println("Usage: port seed");
		System.out.println("port completed queued");
		System.out.println("For a good seed, try 12854372 or 813286");
	}
}
