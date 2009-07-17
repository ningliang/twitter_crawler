package worker;

import java.util.ArrayList;
import java.util.List;

import common.Result;
import common.Status;

public class Task implements Runnable {
	private int id;
	private Result result = null;
	private String username;
	private String password;
	
	private volatile boolean crawled = false;
	private volatile boolean finished = false;
	private int failCount = 0;
	
	private static final int MAX_FAILS = 8;
	private static final int PAGE_SIZE = 5000;
	
	public Task(int id, String username, String password) {
		this.id = id;
		this.username = username;
		this.password = password;
	}
	
	public int getId() { return this.id; }
	public Result getResult() { return this.result; }
	public boolean isFinished() { return this.finished; }
	
	public void run() {
		TwitterClient client = new TwitterClient(this.username, this.password);
		int page = 1;
		List<Integer> idList = new ArrayList<Integer>();
		
		try {
			while (!this.crawled) {
				if (this.failCount > MAX_FAILS) {
					this.result = new Result(this.id, Status.FAILED);
					this.crawled = true;
				} else {
					List<Integer> followersPage = new ArrayList<Integer>();
					int responseCode = client.getFollowersIDs(this.id, page, followersPage);
					if (responseCode == 200) {
						idList.addAll(followersPage);
						if ((followersPage.size() < PAGE_SIZE && page == 1) || (followersPage.size() == 0)) {
							this.crawled = true;
						} else {
							page++;
							Thread.sleep(200);
						}
					} else {
						switch (responseCode) {
							case 400: Thread.sleep(1000); this.failCount++; break;
							case 401: this.result = new Result(this.id, Status.NOT_AUTHORIZED); this.crawled = true; break;
							case 403: this.result = new Result(this.id, Status.INVALID_ACCOUNT); this.crawled = true; break;
							case 404: this.result = new Result(this.id, Status.NOT_FOUND); this.crawled = true; break;
							default: this.failCount++; break;
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Unexpected exception for " + this.id + ": " + e);
			System.out.println(e);
		}
		
		if (this.result == null) {
			int[] followers = new int[idList.size()];
			for (int i = 0; i < followers.length; i++) followers[i] = idList.remove(0);
			this.result = new Result(this.id, Status.SUCCESS, followers);
		}
		
		this.finished = true;
	}
}