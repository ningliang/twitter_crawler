package common;

import java.io.Serializable;
import java.util.Date;

public class Tweet implements Serializable{
	private static final long serialVersionUID = 1372064081812194622L;
	public Date createdAt;
	public long id;
	public String text;
	public String source;
	public Boolean truncated;
	public Boolean favorited;
	
	public long inReplyToStatusId;
	public long inReplyToUserId;
	public String inReplyToScreenName;
}
