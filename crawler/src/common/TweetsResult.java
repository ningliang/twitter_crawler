package common;

public class TweetsResult extends Result {
	private static final long serialVersionUID = -844826308662041186L;
	private Tweet[] tweets;
	private Biography biography;
	
	public TweetsResult(int id, Status status, Biography biography, Tweet[] tweets) {
		super(id, status);
		this.biography = biography;
		this.tweets = tweets;
	}
	
	public TweetsResult(int id, Status status) {
		super(id, status);
	}

	public Tweet[] getTweets() { return this.tweets; }
	public void setTweets(Tweet[] tweets) { this.tweets = tweets; }
	public Biography getBiography() { return this.biography; }
	public void setBiography(Biography biography) { this.biography = biography; }
	public int[] toQueue() { return new int[] {}; }
}
