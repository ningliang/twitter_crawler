package message;

import common.Result;

public class ResultMessage extends Message {

	private static final long serialVersionUID = -5961194666100212846L;
	private Result[] results;
	
	public ResultMessage(String key, Result[] results) {
		super(key);
		this.results = results;
	}
	
	public Result[] getResults() { return this.results; }
}
