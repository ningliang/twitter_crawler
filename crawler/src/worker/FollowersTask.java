package worker;

import java.util.ArrayList;
import java.util.List;

import common.FollowerResult;
import common.Status;

public class FollowersTask extends Task {
	private static final int PAGE_SIZE = 5000;
	private Boolean crawled;
	
	public FollowersTask(int id, String username, String password) {
		super(id, username, password);
		this.crawled = false;
	}
	
	// Do the crawl, and set finished to true at the end
	public void run() {
		int page = 1;
		List<Integer> idList = new ArrayList<Integer>();
		
		try {
			while (!this.crawled) {
				if (this.failCount > MAX_FAILS) {
					this.result = new FollowerResult(this.id, Status.FAILED);
					this.crawled = true;
				} else {
					int beforeCount = idList.size();
					int code = twitterClient.getFollowersIDs(this.id, page, idList);
					int pageSize = idList.size() - beforeCount;
					if (code == 200) {
						if ((pageSize < PAGE_SIZE && page == 1) || (pageSize == 0)) {
							this.crawled = true;
						} else {
							page++;
							Thread.sleep(200);
						}
					} else {
						switch (code) {
							case 400: Thread.sleep(1000); this.failCount++; break;
							case 401: this.result = new FollowerResult(this.id, Status.NOT_AUTHORIZED); this.crawled = true; break;
							case 403: this.result = new FollowerResult(this.id, Status.INVALID_ACCOUNT); this.crawled = true; break;
							case 404: this.result = new FollowerResult(this.id, Status.NOT_FOUND); this.crawled = true; break;
							default: this.failCount++; break;
						}
					}
				}
			} 
		} catch (Exception e) {
			System.out.println("Unexpected exception for " + this.id + ": " + e);
		}
		
		if (this.result == null) {
			int[] followers = new int[idList.size()];
			for (int i = 0; i < followers.length; i++) followers[i] = idList.remove(0);
			this.result = new FollowerResult(this.id, Status.SUCCESS, followers);
		}
		
		this.finished = true;
	}
}