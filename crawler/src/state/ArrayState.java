package state;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ArrayState implements State {
	private static final byte INVALID = 0;
	private static final byte QUEUED = 1;
	private static final byte PENDING = 2;
	private static final byte COMPLETED = 3;
	
	private byte[] state;
	private LinkedList<Integer> queue = new LinkedList<Integer>();
	private long completedCount;
	private long pendingCount;
	
	public ArrayState(int maxCount) {
		this.state = new byte[maxCount];
	}

	public List<Integer> checkoutIds(int max) {
		LinkedList<Integer> retval = new LinkedList<Integer>();
		while (retval.size() < max && !this.queue.isEmpty()) {
			int id = this.queue.removeFirst();
			retval.add(id);
			this.state[id] = PENDING;
			this.pendingCount++;
		}
		return retval;
	}

	// Transfer from pending to completed
	public void completeId(int id) {
		if (this.state[id] == PENDING) {
			this.completedCount++;
			this.pendingCount--;
			this.state[id] = COMPLETED;
		}
	}
		
	// Add a new id to queue
	public void enqueueId(int id) {
		if (!this.hasId(id) && id < state.length) {
			this.state[id] = QUEUED;
			this.queue.add(id);
		}
	}
	
	// Transfer from pending to queued
	public void rollbackId(int id) {
		if (this.state[id] == PENDING) {
			this.pendingCount--;
			this.state[id] = QUEUED;
			this.queue.add(id);
		}
	}
	
	// Bulk methods
	public void completeIds(List<Integer> ids) { for (int id : ids) this.completeId(id); }
	public void enqueueIds(List<Integer> ids) {	for (int id : ids) this.enqueueId(id); }
	public void rollbackIds(List<Integer> ids) { for (int id : ids) this.rollbackId(id); }
	
	private Boolean hasId(int id) {	return (this.state[id] != INVALID); }

	public long getCompletedCount() { return this.completedCount; }
	public long getPendingCount() { return this.pendingCount; }
	public long getQueuedCount() { return this.queue.size(); }
	
	// Direct access
	public void setQueued(int id) { if (!hasId(id) && id < state.length) this.state[id] = QUEUED; }
	public void setComplete(int id) { this.state[id] = COMPLETED; }
	public void rebuild() {
		// Reset the state
		this.queue.clear();
		this.completedCount = 0;
		this.pendingCount = 0;
		
		// Loop through array, setting aggregates as appropriate
		for (int id = 0; id < this.state.length; id++) {
			if (this.state[id] == QUEUED) {
				this.queue.add(id);
			} else if (this.state[id] == COMPLETED) {
				this.completedCount++;
			} else if (this.state[id] == PENDING) {
				this.pendingCount++;
			}
		}
	}

	public List<Integer> getCompleted() {
		List<Integer> completed = new ArrayList<Integer>();
		for (int id = 0; id < this.state.length; id++) {
			if (this.state[id] == COMPLETED) completed.add(id);
		}
		return completed;
	}

	public List<Integer> getQueued() {
		List<Integer> queued = new ArrayList<Integer>();
		for (int id = 0; id < this.state.length; id++) {
			if (this.state[id] == QUEUED || this.state[id] == PENDING) queued.add(id);
		}
		return queued;
	}
}