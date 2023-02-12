package Non.blocking.server;

import Non.blocking.server.messages.*;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;

/**
 * The class has a SocketChannel. A read method and write method.
 * Necessary methods and attributes for each client connection.
 * @author xsala
 *
 */
public class Socket {

	/**A number that identifies an instance of {@code Socket}*/
	public long socketId;

	/**userName reference associated to the Socket. When the connection is logged this value will contain the userName*/
	public String userName = null;

	/**Associated SocketChannel*/
	public SocketChannel socketChannel = null;

	/**Associated MessageReader*/
	public MessageReader messageReader = new MessageReader(this);

	/**Associated MessageWriter*/
	public MessageWriter messageWriter = new MessageWriter(this);

	/**A map of all loggedSockets*/
	public Map<String, Socket> loggedSockets = null;

	/**A set of all nonLoggedSockets*/
	public Set<Socket> nonLoggedSockets = null;

	private Cipher secretKeyCipherDecryptor = null;

	private Cipher secretKeyCipherEncryptor = null;

	/**The MAC algorithm for client-server communication*/
	private static Mac mac = null;

	private int allowedErrors = 3; //just for some kind of errors, managed on read().


	public Socket(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}

	/**
	 * Calls messageReader.readMessge(). If there are messages to read, it will try to read these.
	 */
	public void read() {
			try {
				messageReader.readMessage();
			} catch (ServerException e) {
				if (allowedErrors > 0 &&
					(e.type == ServerException.Type.LOG_IN_PROBLEM ||
					e.type == ServerException.Type.LOG_IN_NEEDED  ||
					e.type == ServerException.Type.WRONG_DESTINATION)){

					Message nackMessage = MessageUtilities.createNACK("noName", e.messageId, e.type.message);

					try {
						allowedErrors--;
						messageWriter.enqueueMessage(nackMessage.getMessageInByteBuffer(this));
					} catch (ServerException ex) {
						ex.printStackTrace();
					}

				} else {

					try {
						socketChannel.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
					e.printStackTrace();
				}
			}

	}

	/**
	 * Calls messageWriter.write(). That is, it tries to send the messages which are at the queue to be sent.
	 */
	public void write()  {
		try {
			messageWriter.write();
		} catch (IOException e) {
			// TODO HERE MANAGE ALL KIND OF WRITING EXCEPTIONS.
			e.printStackTrace();
		}
	}



	/**
	 * Checks if the socket has messages to send.
	 * @return true if it has messges to send. That is, messageWriter has some in progress.
	 */
	public boolean hasMessagesToSend() {
		return !messageWriter.isEmpty();
	}


	/**
	 * //TODO
	 */
	/*public void sendAllUsers() {
		byte[] bytes =  loggedSockets.keySet().toArray().toString().getBytes(StandardCharsets.UTF_8);

		try {
			messageWriter.enqueueMessage(MessageType.TEXT, bytes);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/

	/**Moves this Socket to the nonLoggedSockets and makes the userName null.*/
	public void unlog() {
		loggedSockets.remove(userName);
		nonLoggedSockets.add(this);
		userName = null;
	}

	/**
	 * Sets socket as a logged. Puts it to loggedSockets and removes from nonLogged.
	 * @param userName
	 * @param logInMessageId
	 * @throws ServerException if some other socket is already logged with the userName received as a parameter.
	 */
	public void logMe(String userName, long logInMessageId) throws ServerException {
		if (loggedSockets.get(userName) == null){

			this.userName = userName;
			loggedSockets.put(userName, this);
			nonLoggedSockets.remove(this);

			messageWriter.enqueueMessage(MessageUtilities.createACK(userName,
					logInMessageId).getMessageInByteBuffer(this));

		} else { //The user is already logged.

			messageWriter.enqueueMessage(MessageUtilities.createNACK(userName,
					logInMessageId, "User is already logged with other client.").
					getMessageInByteBuffer(this));

			throw new ServerException(ServerException.Type.LOG_IN_PROBLEM);
		}
	}


	/**
	 * Sends a welcome message to the client.
	 */
	public void sendWelcome() {
		try {
			messageWriter.enqueueMessage(MessageUtilities.createWelcome(socketId).getMessageInByteBuffer(this));
		} catch (Exception e) {
			try {
				socketChannel.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			e.printStackTrace();

		}
	}

	/**
	 * Generates MAC or byte[0]=0 if mac object is null.
	 * @param datas
	 * @return
	 */
	public byte[] generateMac(byte[][] datas){
		if (mac == null) return new byte[]{0}; //no MAC generator.

		for (byte[] data : datas){
			mac.update(data);
		}
		return mac.doFinal();
	}

	/**
	 * Creates and initiates mac object from a given key.
	 * @param keyForMac
	 */
	public void setMacGenerator(byte[] keyForMac) {
		try {
			mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(keyForMac, "RawBytes"));
		} catch (NoSuchAlgorithmException | InvalidKeyException e){
			e.printStackTrace();
		}
	}


	public void setSecretKeyCiphers(byte[] secretKeyInBytes) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
		SecretKey secretKey = new SecretKeySpec(secretKeyInBytes, MessageUtilities.DEFAULT_SECRET_KEY_ALGORITHM);
		secretKeyCipherDecryptor = Cipher.getInstance(MessageUtilities.DEFAULT_SECRET_KEY_ALGORITHM);
		secretKeyCipherDecryptor.init(Cipher.DECRYPT_MODE, secretKey);
		secretKeyCipherEncryptor = Cipher.getInstance(MessageUtilities.DEFAULT_SECRET_KEY_ALGORITHM);
		secretKeyCipherEncryptor.init(Cipher.ENCRYPT_MODE, secretKey);
	}

	public boolean hasSecretKey() {
		return secretKeyCipherEncryptor != null;
	}

	public byte[] decryptWithSecretKey(byte[] bytes) throws ServerException {
		try {
			return secretKeyCipherDecryptor.doFinal(bytes);
		} catch (Exception e) {
			throw new ServerException(ServerException.Type.SECRET_KEY_PROBLEM);
		}
	}

	public byte[] encryptWithSecretKey(byte[] bytes) throws ServerException {
		try {
			return secretKeyCipherEncryptor.doFinal(bytes);
		} catch (IllegalBlockSizeException |BadPaddingException e) {
			throw new ServerException(ServerException.Type.SECRET_KEY_PROBLEM);
		}
	}
}
