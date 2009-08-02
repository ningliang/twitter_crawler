package common;

public class FollowerResult extends Result {
	private static final long serialVersionUID = -8308933226435790140L;
	private int[] followerIds;
	
	public FollowerResult(int id, Status status, int[] followerIds) {
		super(id, status);
		this.followerIds = followerIds;
	}
	
	public FollowerResult(int id, Status status) {
		super(id, status);
		this.followerIds = null;
	}
	
	public int[] getFollowerIds() { return this.followerIds; }
	public int[] toQueue() { return this.followerIds; }
}
