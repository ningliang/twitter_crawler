package common;

import java.io.Serializable;
import java.util.Date;

import org.json.simple.JSONObject;

public class Tweet implements Serializable{
	private static final long serialVersionUID = 1372064081812194622L;
	public Date createdAt;
	public long id;
	public long userId;
	public String text;
	public String source;
	public Boolean truncated;
	public Boolean favorited;
	
	public long inReplyToStatusId;
	public long inReplyToUserId;
	public String inReplyToScreenName;
	
	public JSONObject toJSON() {
		JSONObject object = new JSONObject();
		object.put("createdAt", createdAt.getTime());
		object.put("id", id);
		object.put("userId", userId);
		object.put("text", text);
		object.put("source", source);
		object.put("truncated", truncated);
		object.put("favorited", favorited);
		object.put("inReplyToStatusId", inReplyToStatusId);
		object.put("inReplyToUserId", inReplyToUserId);
		object.put("inReplyToScreenName", inReplyToScreenName);
		return object;
	}
}
