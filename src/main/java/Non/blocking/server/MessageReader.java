package Non.blocking.server;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * An instance of {@code MessageReader} is associated to a Socket. This instance reads and storages partial messages and reads and processes full messages.
 * @author xsala
 *
 */
public class MessageReader {

	/**Five bytes. First byte messageType, next four bytes length*/
	private ByteBuffer byteBufferTypeAndLength = ByteBuffer.allocate(5); 
	
	/**For the message body reception*/
	private ByteBuffer byteBuffer = null;
	
	/**MessageType that has to bee read*/
	private MessageType messageType = null;
	
	/**The length of the message*/
	private int length = 0;
	
	/**Destination of the message*/
	private String destination = null;
	
	/**Content type. From MessageType values > 1*/
	private byte[] content = null;
	
	
	
	
	public MessageReader() {
		
	}
	
	
	
	/**
	 * Tries to loggin.
	 * @param socket
	 * @return null or string with userName.
	 * @throws Exception with communication problems (SocketChannel closed, or doesn't try to loggin i.e.)
	 */
	public String loggin (Socket socket) throws Exception {
		
		String userName = null;
		
		if (messageType == null) {
			//Has to read the first five bytes
			socket.socketChannel.read(this.byteBufferTypeAndLength); //throws exception with connection problems.
			
			if (byteBufferTypeAndLength.position() ==4) { //the first five bytes have been read.
				
				byteBufferTypeAndLength.flip(); //ready to read.
				
				messageType = MessageType.getTypeByByte( byteBufferTypeAndLength.get()); //first byte is messageType
				
				if (messageType != MessageType.LOGGIN) { //It's not trying to loggin.
					throw new Exception ("[ERROR] Not loggin");
				}
					
				length = byteBufferTypeAndLength.getInt();
				
				byteBufferTypeAndLength.clear();
				byteBufferTypeAndLength.flip(); //ready to write.
			}
		}
		
		
		
		if (messageType == MessageType.LOGGIN) {
			
			if (byteBuffer == null) {
				byteBuffer = ByteBuffer.allocate(length);
			}
			
			socket.socketChannel.read(this.byteBuffer);
			
			
			//userName is fully read.
			if (byteBuffer.position() == byteBuffer.limit()-1) {
				
				byteBuffer.flip(); //ready to read
				userName = new String( byteBuffer.array(), StandardCharsets.UTF_8);
				
				//tries to logg the socket. If the user is already logged, throws exception. Socket will be closed.
				if (socket.loggedSockets.containsKey(userName)) {
					throw new Exception ("[ERROR] User is already logged");
				} else {
					socket.loggedSockets.put(userName, socket);
					socket.nonLoggedSockets.remove(socket);
				}
				
				
				resetByteBufferAndMessageType();
			}
	
			
		} else {
			throw new Exception ("[Error] Not loggin");
		}
		
		return userName;
	}
	
	
	
	/**
	 * Tries to read a message. First, we need MessageType == TO, second, 
	 * @param socket
	 * @throws Exception
	 */
	public void readMessage (Socket socket) throws Exception {
		
		//The first five bytes haven't been read.
		if (messageType == null) { 
			socket.socketChannel.read(this.byteBufferTypeAndLength); //throws exception with connection problems.
		

			//the first five bytes have been read.
			if (byteBufferTypeAndLength.position() ==4) { 
		
				byteBufferTypeAndLength.flip(); //ready to read
				
				messageType = MessageType.getTypeByByte( byteBufferTypeAndLength.get());
				
				if (messageType != MessageType.TO) {
					byteBufferTypeAndLength.clear();
					byteBufferTypeAndLength.flip();
					messageType = null;
					throw new Exception ("[ERROR] Without destination");
				}
			
				length = byteBufferTypeAndLength.getInt();
				
				byteBufferTypeAndLength.clear();
				byteBufferTypeAndLength.flip();
			}
		}
		
		
		//It has read the first five bytes, but doesn't have destination. Reads destination.
		if (messageType == MessageType.TO && destination == null) { 
			if (byteBuffer == null) {
				byteBuffer = ByteBuffer.allocate(length);
			}
			
			socket.socketChannel.read(this.byteBuffer);
			
			//destination is fully read
			if (byteBuffer.position() == byteBuffer.limit()-1) {
				
				byteBuffer.flip(); //ready to read
				destination = new String( byteBuffer.array(), StandardCharsets.UTF_8);
				resetByteBufferAndMessageType();
			}	
		}
		
		//It has destination. Reads the content.
		if (destination != null) {
			if (byteBuffer == null) {
				byteBuffer = ByteBuffer.allocate(length);
			}
			
			socket.socketChannel.read(this.byteBuffer);
			
			//destination is fully read
			if (byteBuffer.position() == byteBuffer.limit()-1) {
				
				byteBuffer.flip(); //ready to read
				content = byteBuffer.array();
				resetByteBufferAndMessageType();
				sendMessage(socket);
			}	
		}
		
	}
	
	
	
	/**
	 * Turns {@code byteBuffer} null, {@code length} 0 and {@code messageType} null.
	 * Ready for a new message to be read.
	 */
	private void resetByteBufferAndMessageType () {
		messageType = null;
		length = 0;
		byteBuffer = null;
	}
	
	
	/**
	 * Puts
	 * @param socket
	 */
	private void sendMessage(Socket socket) {
		ByteBuffer bb = ByteBuffer.allocate(content.length);
		bb.put(content);
		try {
			socket.loggedSockets.get(destination).messageWriter.enqueue(bb);
		} catch(Exception e) {
			//TODO too many messages in the queue
		}
	}
	
	
	public static enum MessageType {
		LOGGIN(0),
		TO(1),
		TEXT(2);
		
		byte byteValue;
		MessageType (int byteValue) {
			this.byteValue =(byte) byteValue;
		}
		
		public static MessageType getTypeByByte(byte b) {
			for (MessageType mt :MessageType.values()) {
				if (mt.byteValue == b) return mt;
			}
			return null;
		}
	}
	
	
	public static final byte[] intToByteArray(int value) {
	    return new byte[] {
	            (byte)(value >>> 24),
	            (byte)(value >>> 16),
	            (byte)(value >>> 8),
	            (byte)value};
	}
	

	
	public static byte[] intToByteArray2(int value) {
		return ByteBuffer.allocate(4).putInt(value).array();
	}
	
	
}
