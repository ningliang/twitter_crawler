package worker;

import common.Result;

public abstract class Task implements Runnable {
	protected int id;
	private String username;
	private String password;
	protected Result result;
	
	protected TwitterClient twitterClient;
	protected static final int MAX_FAILS = 8;
	protected volatile boolean finished = false;
	protected int failCount = 0;
	
	public Task(int id, String username, String password) {
		this.id = id;
		this.username = username;
		this.password = password;
		this.twitterClient = new TwitterClient(this.username, this.password);
	}
	
	public int getId() { return this.id; }
	public Result getResult() { return this.result; }
	public boolean isFinished() { return this.finished; }
	
	public abstract void run();
}