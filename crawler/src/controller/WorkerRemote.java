package controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import message.AssignmentMessage;
import message.Message;
import message.ResultMessage;
import message.ShutdownMessage;

import common.Constants;
import common.Result;

public class WorkerRemote {
	private String workerName;
	private String userName;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private Controller controller;
	private ExecutorService threadPool = Executors.newSingleThreadExecutor();
	
	private volatile boolean running = false;
	private volatile boolean sending = true;
	
	// Out queue, in queue, and pending hash set
	// Replace these with fastutil classes
	private LinkedList<Integer> outQueue = new LinkedList<Integer>();
	private LinkedList<Result> inQueue = new LinkedList<Result>();
	private HashSet<Integer> pending = new HashSet<Integer>();
	
	public WorkerRemote(String workerName, String userName, Controller controller, ObjectInputStream in, ObjectOutputStream out) {
		this.workerName = workerName;
		this.userName = userName;
		this.in = in;
		this.out = out;
		this.controller = controller;
	}
	
	public String getWorkerName() { return this.workerName; }
	public String getUserName() { return this.userName; }
	public boolean isRunning() { return this.running; }
	public boolean isSending() { return this.sending; }

	// Push outQueue
	public void pushIds(List<Integer> ids) {
		synchronized (this.outQueue) {
			for (int id : ids) this.outQueue.add(id);
		}
	}
	
	public void pushId(int id) {
		synchronized(this.outQueue) {
			this.outQueue.add(id);
		}
	}
	
	// Pop inQueue
	public Result popResult() {
		synchronized (this.inQueue) {
			if (!this.inQueue.isEmpty()) return this.inQueue.removeFirst();
		}
		return null;
	}
	
	// Pop all inQueue
	public LinkedList<Result> popResults() {
		LinkedList<Result> retval = new LinkedList<Result>();
		synchronized(this.inQueue) {
			for (Result result : this.inQueue) retval.add(result);
			this.inQueue.clear();
		}
		return retval;
	}
	
	// Pop outQueue, add pending, send (thread)
	// Called by sendThread
	public void sendJob() throws IOException {
		if (this.sending) {
			synchronized (this.outQueue) {
				// Check for anything to assign
				AssignmentMessage am = null;
				if (!this.outQueue.isEmpty()) {
					int[] ids = new int[this.outQueue.size()];
					for (int i = 0; i < ids.length; i++) ids[i] = this.outQueue.removeFirst();
					synchronized(this.pending) {
						for (int id : ids) this.pending.add(id);
					}
					am = new AssignmentMessage(Constants.SECRET, ids);
				}
				
				// Create and send assignment message if we created one
				if (am != null) {
					this.out.writeObject(am);
					this.out.flush();
					this.sending = false;
					System.out.println("Sent " + am.getIds().length);
				}
			}
		}
	}
	
	// Receive, remove pending, push inQueue
	// Called by receiveThread
	public void receiveResults() throws IOException, ClassNotFoundException {
		if (!this.sending) {
			Object o = this.in.readObject();
			if (o instanceof ResultMessage && this.controller.validate((Message)o)) {
				Result[] results = ((ResultMessage)o).getResults();
				synchronized(this.inQueue) {
					synchronized(this.pending) {
						for (Result result : results) {
							this.pending.remove(result.getId());
							this.inQueue.add(result);
							if (this.pending.size() == 0) this.sending = true;
						}
						System.out.println("Received " + results.length);
					}
				}
			}
		}
	}
	
	// None pending
	public boolean nonePending() {
		synchronized(this.outQueue) {
			synchronized(this.pending) {
				return (this.outQueue.size() == 0 && this.pending.size() == 0);
			}
		}
	}
	
	// What ids are pending?
	public List<Integer> pendingIds() {
		LinkedList<Integer> retval = new LinkedList<Integer>();
		synchronized(this.outQueue) {			
			synchronized(this.pending) {
				for (int i : this.pending) retval.add(i);
			}
		}
		return retval;
	}
	
	// Start the thread after construction - this will work only once
	public void start() {
		this.running = true;
		this.threadPool.execute(new SocketThread(this));
	}
	
	// sendShutdown = true for normal shutdown
	public void stop(Boolean sendShutdown) {
		try {
			if (sendShutdown) {
				ShutdownMessage sm = new ShutdownMessage(Constants.SECRET);
				this.out.writeObject(sm);
				this.out.flush();
			}
			this.running = false;
			this.in.close();
			this.out.close();
		} catch (IOException e) {
			System.out.println("Shut down worker error!");
		}
	}
}

class SocketThread implements Runnable {
	private WorkerRemote worker;
	public SocketThread(WorkerRemote worker) {
		this.worker = worker;
	}
	
	public void run() {
		while (this.worker.isRunning()) {
			try {
				if (this.worker.isSending()) {
					this.worker.sendJob();
				} else {
					this.worker.receiveResults();
				}
				Thread.sleep(1000);
			} catch (Exception e) {
				this.worker.stop(false);
			}
		}
	}
}
