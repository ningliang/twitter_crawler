package worker;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import message.AcknowledgementMessage;
import message.AssignmentMessage;
import message.Message;
import message.RegisterMessage;
import message.ResultMessage;
import message.ShutdownMessage;

import common.Constants;
import common.Logger;
import common.Result;
import common.ResultLogger;
import common.TweetsResult;

public class Worker {
	private String hostName;
	private int hostPort;
	private String logDir;
	private boolean connected = false;
	private String username;
	private String password;
	private long crawlCount = 0;
	private int runningCount = 0;
	private long beginTime;
	private CrawlType crawlType;
	
	// Constants
	private final static int MAX_CONCURRENCY = 40;
	private final static int REQUESTS_PER_HOUR = 19000;
	private int SLEEP_INTERVAL = Math.round((float)3600000 / (float)REQUESTS_PER_HOUR);
	
	// Connection details
	private Socket socket;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private volatile boolean sending = false;
	
	// Message queues
	private LinkedList<Integer> inQueue = new LinkedList<Integer>();
	private LinkedList<Result> outQueue = new LinkedList<Result>();
	
	// Tasks
	private ExecutorService threadPool = Executors.newFixedThreadPool(40);
	private LinkedList<Task> tasks = new LinkedList<Task>();
	
	// Logs
	private Logger statusLog = new Logger("worker_log.txt");
	private ResultLogger resultLog;
	
	// Constructor
	public Worker(String hostName, int hostPort, String logDir, String username, String password, CrawlType crawlType) {
		this.hostName = hostName;
		this.hostPort = hostPort;
		this.username = username;
		this.password = password;
		this.logDir = logDir;
		this.crawlType = crawlType;
	}
	
	// Get the count
	public long getCount() { return this.crawlCount; }
	
	// Connect function
	private void connect() {
		try {
			// Connect
			this.socket = new Socket(this.hostName, this.hostPort);
			this.socket.setSoTimeout(Constants.TIMEOUT);
			this.out = new ObjectOutputStream(this.socket.getOutputStream());
			this.in = new ObjectInputStream(this.socket.getInputStream());
			
			// Send a register message
			this.out.writeObject(new RegisterMessage(Constants.SECRET, InetAddress.getLocalHost().getCanonicalHostName(), this.username));
			this.out.flush();
			
			// Wait for an acknowledgement
			Object o = this.in.readObject();
			if (!(o instanceof AcknowledgementMessage)) throw new RuntimeException("Not a response message.");
			else if (!this.validate((AcknowledgementMessage)o)) throw new RuntimeException("Invalid message.");
			else this.connected = true;
		} catch (Exception e) {
			try {
				this.statusLog.logError(e.toString());
				if (this.socket != null) this.socket.close();
			} catch (IOException f) {}
		}
	}
	
	public void receive() throws IOException, ClassNotFoundException {
		if (!this.sending) {
			Object o = this.in.readObject();
			if (o instanceof AssignmentMessage && this.validate((Message)o)) {
				AssignmentMessage am = (AssignmentMessage)o;
				int[] ids = am.getIds();
				synchronized (this.inQueue) {
					for (int id : ids) this.inQueue.add(id);
				}
				this.statusLog.logStatus("Received " + ids.length + " ids to crawl.");
				this.sending = true;
			} else if (o instanceof ShutdownMessage && this.validate((Message)o)) {
				this.stop();
			}
		}
	}
	
	public void send() throws IOException {
		if (this.sending) {
			synchronized(this.outQueue) {
				if (this.outQueue.size() > 0) {
					// Write to file
					for (Result result : this.outQueue) this.resultLog.addResult(result);
					
					// Write to object
					Result[] results = new Result[this.outQueue.size()];
					for (int i = 0; i < results.length; i++) {
						Result result = this.outQueue.removeFirst();
						results[i] = new TweetsResult(result.getId(), result.getStatus(), null, null);
					}
					ResultMessage rm = new ResultMessage(Constants.SECRET, results);
					this.out.writeObject(rm);
					this.out.flush();
					
					System.out.println("Sent and wrote " + results.length + ".");
				}
				
				if (this.inQueue.size() == 0 && this.tasks.size() == 0) this.sending = false;
			}
		}
	}
	
	public void start() {
		this.connect();
		this.beginTime = System.currentTimeMillis();
		if (this.connected) {
			this.statusLog.logStatus("Worker connected!");
			this.resultLog = new ResultLogger(logDir + "/" + System.currentTimeMillis(), true);
			
			this.threadPool.execute(new SocketThread(this));
			this.crawlCount = 0;
			
			try {
				while (this.connected) {
					while (this.inQueue.size() > 0 && runningCount < MAX_CONCURRENCY) {
						spawnTask();
						Thread.sleep(SLEEP_INTERVAL);
					}
					retrieveResults();
				}
				
				// We're done, so clean up
				//this.threadPool.awaitTermination(120000, TimeUnit.MILLISECONDS);
				//retrieveResults();
				this.threadPool.shutdownNow();
				this.resultLog.close();
			} catch (Exception e) {
				this.statusLog.logError("Error in main loop: " + e);
				System.exit(-1);
			}
		} else {
			this.statusLog.logError("Failed to connect.");
		}
	}
	
	private void spawnTask() {
		synchronized(this.inQueue) {
			if (this.inQueue.size() > 0) {
				int nextId = this.inQueue.removeFirst();
				Task task = null;
				switch (this.crawlType) {
					case USER_TWEETS_AND_BIOS: task = new UserTask(nextId, this.username, this.password); break;
					default: task = new FollowersTask(nextId, this.username, this.password); break;
				}		
				this.threadPool.execute(task);
				this.tasks.add(task);
				this.runningCount++;
			}
		}
	}
	
	private void retrieveResults() {
		Iterator<Task> iter = this.tasks.iterator();
		while (iter.hasNext()) {
			Task task = iter.next();
			if (task.isFinished()) {
				synchronized(this.outQueue) {
					this.outQueue.add(task.getResult());
				}
				iter.remove();
				runningCount--;
				this.crawlCount++;
				// Log statement
				if (this.crawlCount % 1000 == 0 && this.crawlCount > 0) {
					long currentTime = System.currentTimeMillis();
					this.statusLog.logStatus("Crawled " + crawlCount + " in " + ((currentTime - beginTime)/1000) + " seconds.");
				}
			}										
		}
	}
	
	public void stop() {
		try {
			this.connected = false;
			this.in.close();
			this.out.close();
		} catch (IOException e) {}
	}
	
	public boolean isSending() { 
		return this.sending;
	}
		
	// We don't need to synchronize the worker version - only one thread trades messages
	public boolean validate(Message m) { return m.valid(Constants.SECRET); }
	
	// Main program
	public static void main(String[] args) throws UnknownHostException {
		if (args.length == 6) {
			while (true) {
				try {
					CrawlType crawlType = CrawlType.USER_TWEETS_AND_BIOS;
					if (args[5].equals("usertweets")) {
						crawlType = CrawlType.USER_TWEETS_AND_BIOS;
					} else if (args[5].equals("followerids")) {
						crawlType = CrawlType.FOLLOWER_IDS;
					} else {
						System.out.println("Unknown crawltype " + args[5] + ", using default crawltype 'usertweets'");
					}
					Worker w = new Worker(args[0], Integer.parseInt(args[1]), args[2], args[3], args[4], crawlType);
					w.start();
					Runtime.getRuntime().gc();
					Thread.sleep(10000);
				} catch (Exception e) {
					System.out.println(e);
					e.printStackTrace();
				}
			}
		} else {
			System.out.println("Don't forget to use -XmsM and -XmxM options, where M is like 256m, 1g, etc.");
			System.out.println("Usage: hostname port log username password crawltype");
			System.out.println("       where crawltype is one of usertweets, followerids");
		}
	}
}

class SocketThread implements Runnable {
	private Worker worker;
	SocketThread(Worker worker) {
		this.worker = worker;
	}
	
	public void run() {
		while (true) {
			try {
				if (this.worker.isSending()) {
					this.worker.send();
				} else {
					this.worker.receive();					
				}
				Thread.sleep(1000);
			} catch (Exception e) {
				System.out.println("Error in socket thread: " + e);
				e.printStackTrace();
				this.worker.stop();
				break;
			}
		}
	}
}
