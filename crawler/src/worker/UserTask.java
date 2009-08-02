package worker;

import java.util.ArrayList;
import java.util.List;

import common.Biography;
import common.Status;
import common.Tweet;
import common.TweetsResult;

public class UserTask extends Task {
	private Boolean crawled;
	private static int PAGE_SIZE = 200;

	public UserTask(int id, String username, String password) {
		super(id, username, password);
		this.crawled = false;
	}

	// Do the crawl, and set finished to true at the end
	public void run() {
		int page = 1;
		Biography biography = new Biography();
		List<Tweet> tweetList = new ArrayList<Tweet>();
		
		// TODO figure out what's going on with 401/403/404, and getting bio (id=238)
		try {
			while (!this.crawled) {
				int beforeCount = tweetList.size();
				int code = twitterClient.getTweets(id, PAGE_SIZE, page, tweetList, biography);
				int pageSize = tweetList.size() - beforeCount;
				
				if (code == 200) {
					if (page == 1 && pageSize == 0) {
						code = twitterClient.getBiography(id, biography);
						this.crawled = true;
					} else if ((page == 1 && pageSize < PAGE_SIZE) || (pageSize == 0)) {
						this.crawled = true;
					} else {
						Thread.sleep(200);
						page++;
					}
				} else if (code == 401) {
					twitterClient.getBiography(id, biography);
					this.result = new TweetsResult(this.id, Status.NOT_AUTHORIZED); 
					this.crawled = true;
				} else {
					switch (code) {
						case 400: Thread.sleep(1000); this.failCount++; break;
						case 403: this.result = new TweetsResult(this.id, Status.INVALID_ACCOUNT); this.crawled = true; break;
						case 404: this.result = new TweetsResult(this.id, Status.NOT_FOUND); this.crawled = true; break;
						default: this.failCount++; break;
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Unexpected exception for " + id + " :" + e);
		}
		
		if (this.result == null) {
			this.result = new TweetsResult(id, Status.SUCCESS,  biography, tweetList.toArray(new Tweet[] {})); 
		}
		
		this.finished = true;
	}	
}
