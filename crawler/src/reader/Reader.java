package reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import common.FollowerResult;
import common.Tweet;
import common.TweetsResult;

public class Reader {
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		if (args.length == 1) {
			ObjectInputStream input = new ObjectInputStream(new FileInputStream(args[0]));
			Object object = null;
			while ((object = input.readObject()) != null) {
				System.out.println();
				System.out.println(object);
				if (object instanceof FollowerResult) {
					FollowerResult result = (FollowerResult)object;
					if (result.isSuccessful()) {
						System.out.println("FollowerResult: " + result.getId() + " " + result.getFollowerIds().length);
					} else {
						System.out.println("Failed FollowerResult for user " + result.getId() + " " + result.getStatus());
					}
				} else if (object instanceof TweetsResult) {
					TweetsResult result = (TweetsResult)object;
					if (result.getBiography() != null && result.getBiography().id > 0) {
						System.out.println("TweetsResult");
						System.out.println("USER " + result.getBiography().id);
						System.out.println("NAME " + result.getBiography().name);
						System.out.println("CREATED "  + result.getBiography().createdAt.toString());
						System.out.println("NUM TWEETS " + result.getBiography().statusCount + " " + result.getTweets().length);
					}
					
					if (result.isSuccessful()) {
						for (Tweet tweet : result.getTweets()) System.out.println(tweet.id + " " + tweet.createdAt.toString() + ": " + tweet.text);
					} else {
						System.out.println("Failed TweetsResult for user " + result.getId() + " " + result.getStatus());
					}
				} else {
					System.out.println("BAD KIND");
					System.exit(-1);
				}
			}
		} else {
			System.out.println("Usage: file");
		}
	}
}
