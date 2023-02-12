package Non.blocking.server.messages;

import Non.blocking.server.Server;
import Non.blocking.server.ServerException;
import Non.blocking.server.Socket;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * An instance of {@code MessageReader} is associated to a Socket. This instance reads and storages partial messages and reads and processes full messages.
 * @author xsala
 *
 */
public class MessageReader {

	/**Five bytes. First byte messageType, next four bytes length*/
	private ByteBuffer byteBufferTypeAndLength = ByteBuffer.allocate(5);

	/**For the message bundle reception*/
	private ByteBuffer byteBufferBundle = null;

	/**MessageType that has to bee read*/
	private MessageType messageType = null;

	/**MessageType associated to the content*/
	private MessageType contentType = null;

	/**The length of the message*/
	private int length = 0;

	/**Destination of the message*/
	private String destination = null;

	/**Content type. From MessageType values > 1*/
	private byte[] bundle = null;

	private Socket socket = null;

	private String userName = null;

	public MessageReader(Socket socket) {
		this.socket = socket;
	}


	/**
	 * Tries to read a message. First, it needs MessageType == TO.
	 * Second, tries to get the message.
	 * Third, processes the message.
	 * @throws Exception
	 */
	public void readMessage () throws ServerException {

		//The first five bytes haven't been read.
		if (messageType == null) {
			readTypeAndLength();
		}

		if (messageType != null && bundle == null) {
			if (byteBufferBundle == null) {
				byteBufferBundle = ByteBuffer.allocate(length);
			}

			readBundle();
		}

		if (bundle != null) {
			rebuildAndProcess();
		}
	}



	/**Tries to read the MessageType. First five bytes: Type and length.
	 * @throws Exception
	 */
	private void readTypeAndLength() throws ServerException{
		try {
			socket.socketChannel.read(this.byteBufferTypeAndLength); //throws exception with connection problems.
		}catch (Exception e){
			throw new ServerException(ServerException.Type.CONNECTION_PROBLEM);
		}

		//the first five bytes have been read.
		if (byteBufferTypeAndLength.position() >= byteBufferTypeAndLength.limit()) {

			byteBufferTypeAndLength.flip(); //ready to read

			messageType = MessageType.getTypeByByte( byteBufferTypeAndLength.get());

			length = byteBufferTypeAndLength.getInt();

			byteBufferTypeAndLength.clear(); //clean and ready to write


			if (messageType == null) throw new ServerException(ServerException.Type.UNKNOWN_MESSAGE_TYPE);

		}
	}


	private void readBundle() throws ServerException {

		try {
			socket.socketChannel.read(this.byteBufferBundle);
		} catch (Exception e) {
			throw new ServerException(ServerException.Type.CONNECTION_PROBLEM);
		}

		//destination is fully read
		if (byteBufferBundle.position() >= byteBufferBundle.limit()) {

			byteBufferBundle.flip(); //ready to read
			bundle = byteBufferBundle.array();

			length = 0;
			byteBufferBundle = null;
		}
	}


	private void rebuildAndProcess() throws ServerException {

		Message message = null;
		if (messageType == MessageType.SECRET) {
			bundle = MessageUtilities.decryptWithPrivateKey(bundle);
			message = Message.rebuildMessage(messageType, bundle); //without MAC check
		} else {
			bundle = socket.decryptWithSecretKey(bundle);
			message = Message.rebuildAndCheckMessage(messageType, bundle, socket); //with socket as param, includes MAC check.

		}

		messageType = null;
		length = 0;
		bundle = null;

		MessageProcessor.processIncomingMessage(message, socket);
	}


}
