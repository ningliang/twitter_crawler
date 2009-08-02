package common;

import java.io.Serializable;

public abstract class Result implements Serializable {
	private static final long serialVersionUID = 7489507413955150330L;
	private int id;
	private Status status;
	
	public Result(int id, Status status) {
		this.id = id;
		this.status = status;
	}
	
	public abstract int[] toQueue();
	
	public Status getStatus() { return this.status; }
	public int getId() { return this.id; }
	public boolean isSuccessful() { return this.status == Status.SUCCESS; }	
}
