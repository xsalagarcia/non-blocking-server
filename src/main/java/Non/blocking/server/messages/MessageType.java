package Non.blocking.server.messages;


public enum MessageType {
	//Welcome message, when socket accepted. Sends public key + socketId (the client will generate a SecretKey with socketId and long from client)
	WELCOME(0, 1*1024),
	//contains long form client + secret key.
	SECRET(1, 1*1024),
	//Response message for checking welcome: MAC of PublicKey, generated SecretKey, keyForMac
	CHECK_WELCOME (2, 1*1024),
	//A loggin message. bundle: userNameLength(2 bytes, short), userName, password
	LOG_IN(3, 1*1024),
	//new user creation TODO
	NEW_USER(4, 1*1024),
	//a string representing a destination.
	ENCRYPTED_TEXT(5, 1*1024),
	//A string
	TEXT(6, 1*1024),
	//solicits a response with all users.
	ALL_USERS(7, 0),
	ACK(8, 0), //acknowledgement. Not encrypted
	NACK(9, 0) ; //negative acknowledgement.

	public final byte byteValue;
	public final int maxLength;

	MessageType (int byteValue, int maxLength) {
		this.byteValue =(byte) byteValue;
		this.maxLength = maxLength;
	}

	/**
	 * Gets the {@code MessageType} according to byte. If it doesn't exist, returns null.
	 * @param b Byte value corresponding to the message type.
	 * @return
	 */
	public static MessageType getTypeByByte(byte b) {
		for (MessageType mt :MessageType.values()) {
			if (mt.byteValue == b) return mt;
		}
		return null;
	}

}
