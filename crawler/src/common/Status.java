package common;

public enum Status {
	SUCCESS,
	INVALID_ACCOUNT,
	NOT_FOUND,
	NOT_AUTHORIZED,
	FAILED;
	
	public int toInt() {
		int retval;
		switch (this) {
			case SUCCESS: retval = 0; break;
			case INVALID_ACCOUNT: retval = 1; break;
			case NOT_FOUND: retval = 2; break;
			case NOT_AUTHORIZED: retval = 3; break;
			case FAILED: retval = 4; break;
			default: retval = 4; break;
		}
		return retval;
	}
	
	public static Status fromInt(int value) {
		Status retval;
		switch (value) {
			case 0: retval = SUCCESS; break;
			case 1: retval = INVALID_ACCOUNT; break;
			case 2: retval = NOT_FOUND; break;
			case 3: retval = NOT_AUTHORIZED; break;
			case 4: retval = FAILED; break;
			default: retval = FAILED; break;
		}
		return retval;
	}
}
