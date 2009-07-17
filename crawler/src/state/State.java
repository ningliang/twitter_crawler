package state;

import java.util.List;

public interface State {
	// Counts
	public long getCompletedCount();
	public long getPendingCount();
	public long getQueuedCount();
	
	// Id workflow
	public List<Integer> checkoutIds(int max); 
	public void enqueueId(int id);
	public void enqueueIds(List<Integer> ids);
	public void completeId(int id);
	public void completeIds(List<Integer> ids);
	public void rollbackId(int id);
	public void rollbackIds(List<Integer> ids);
	
	// Direct access
	public void setComplete(int id); 
	public void setQueued(int id);
	public void rebuild();
	
	// Get all queued / pending
	public List<Integer> getQueued();
	public List<Integer> getCompleted();
}