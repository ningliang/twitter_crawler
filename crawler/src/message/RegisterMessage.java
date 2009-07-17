package message;

public class RegisterMessage extends Message {

	private static final long serialVersionUID = -2000064988639689316L;
	private String workerName;
	private String userName;
	
	public RegisterMessage(String key, String workerName, String userName) {
		super(key);
		this.workerName = workerName;
		this.userName = userName;
	}
	
	public String getWorkerName() { return this.workerName; } 
	public String getUserName() { return this.userName; }
}
