package aggregator;

import common.FollowerResult;
import common.Status;

public class Statistics {
	private long successCount;
	private long invalidCount;
	private long notFoundCount;
	private long notAuthorizedCount;
	private long failedCount;
	
	private double totalFollowerCount;
	
	public void addResult(FollowerResult result) {
		switch (result.getStatus()) {
			case SUCCESS: successCount++; break;
			case INVALID_ACCOUNT: invalidCount++; break;
			case NOT_FOUND: notFoundCount++; break;
			case NOT_AUTHORIZED: notAuthorizedCount++; break;
			case FAILED: failedCount++; break;
			default: break;
		}
		
		if (result.getStatus() == Status.SUCCESS) {
			totalFollowerCount += result.getFollowerIds().length;
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("TOTAL CRAWLED: " + sum() + "\n");
		sb.append("AVERAGE FOLLOWER COUNT: " + (totalFollowerCount / (double)successCount) + "\n"); 
		sb.append("SUCCESS: " + successCount + "\n");
		sb.append("INVALID: " + invalidCount + "\n");
		sb.append("NOT_FOUND: " + notFoundCount + "\n");
		sb.append("NOT_AUTHORIZED: " + notAuthorizedCount + "\n");
		sb.append("FAILED: " + failedCount + "\n");
		return sb.toString();
	}
	
	private long sum() {
		return successCount + invalidCount + notFoundCount + notAuthorizedCount + failedCount;
	}
}
