package common;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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
	
	public JSONObject toJSON() {
		JSONObject object = new JSONObject();
		object.put("id", this.getId());		
		if (this.isSuccessful()) {
			JSONArray array = new JSONArray();
			for (int i : followerIds) array.add(i);
			object.put("followers", array);
		}
		return object;
	}
}
