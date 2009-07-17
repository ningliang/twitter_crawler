package message;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class Message implements Serializable {

	private static final long serialVersionUID = -8006817488241702192L;
	private String pad;
	private String signature;
	
	public Message(String key) {
		this.pad = UUID.randomUUID().toString();
		this.signature = this.calculateSignature(key);
	}
	
	public String getPad() { return this.pad; }
	
	// Given a key, does the message validate
	public boolean valid(String key) {
		return this.signature.equals(this.calculateSignature(key));
	}
	
	// Calculate signature
	public String calculateSignature(String key) {
		String retval = "";
		MessageDigest digest;
		try {
			String encrypt = this.pad + key;
			digest = MessageDigest.getInstance("MD5");
			digest.reset();
			digest.update(encrypt.getBytes());
			BigInteger number = new BigInteger(1, digest.digest());
			retval = number.toString(16);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return retval;
	}
}
