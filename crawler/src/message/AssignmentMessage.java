package message;

public class AssignmentMessage extends Message {

	private static final long serialVersionUID = -3750552302068228139L;
	private int[] ids;
	
	public AssignmentMessage(String key, int[] ids) {
		super(key);
		this.ids = ids;
	}
	
	public int[] getIds() { return this.ids; }
}
