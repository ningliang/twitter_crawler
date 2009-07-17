package common;

import java.io.Serializable;

public class Result implements Serializable {

	private static final long serialVersionUID = -8308933226435790140L;
	private Status status;
	private int[] followerIds;
	private int id;
	
	public Result(int id, Status status, int[] followerIds) {
		this.id = id;
		this.status = status;
		this.followerIds = followerIds;
	}
	
	public Result(int id, Status status) {
		this(id, status, null);
	}
	
	public Status getStatus() { return this.status; }
	public int getId() { return this.id; }
	public int[] getFollowerIds() { return this.followerIds; }
	public boolean isSuccessful() { return this.status == Status.SUCCESS; }	
}
