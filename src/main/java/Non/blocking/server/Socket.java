package Non.blocking.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The class has a SocketChannel. A read method and write method.
 * (funcional)
 * @author xsala (With Jenkov's reference).
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
	public MessageReader messageReader = new MessageReader();
	
	/**Associated MessageWriter*/
	public MessageWriter messageWriter = new MessageWriter();
	
	/**A map of all loggedSockets*/
	public Map<String, Socket> loggedSockets = null;
	
	/**A set of all nonLoggedSockets*/
	public Set<Socket> nonLoggedSockets = null;
	
	public Socket() {
		
	}
	
	public Socket(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}
	
	/**
	 * Given a {@code ByteBuffer}, reads the content and puts into the {@code ByteBuffer} entered as a parameter.
	 * @param byteBuffer The {@code ByteBuffer} where the content of {@code SocketChannel} is put.
	 * @return The number of read bytes.
	 * @throws IOException
	 */
	public void read() throws IOException {
		
		
		
		if (userName == null) { 
			//tries to loggin. If communication problems or incoming messages are not trying to loggin the socket will be closed.
			try {
				messageReader.loggin(this);
			} catch (Exception e) {
				socketChannel.close();
			}	
		} else {
			//if it's logged, tries to read a message. 
			try {
				messageReader.readMessage(this);
			} catch (Exception e) {
				// TODO Auto-generated catch block.
				e.printStackTrace();
			}
		}
		
		
		
		

	}
	
	/**
	 * Given a {@code ByteBuffer}, writes the {@code ByteBuffer} content to the {@code SocketChannel} of the present class.
	 * @param byteBuffer The {@code ByteBuffer} which contents the data to put in the {@code SocketChannel}.
	 * @return The number of written bytes.
	 * @throws IOException
	 */
	public int write(ByteBuffer byteBuffer) throws IOException {
		
		int bytesWritten = this.socketChannel.write(byteBuffer);
		int totalBytesWritten = bytesWritten;
		
		while (bytesWritten > 0 && byteBuffer.hasRemaining()) {
			bytesWritten = this.socketChannel.write(byteBuffer);
			totalBytesWritten += bytesWritten;
		}
		return totalBytesWritten;
	}
	
	
	/**
	 * Checks if the {@code Socket} has messages to send.
	 * @return true if it has messges to send. That is, messageWriter has some in progress.
	 */
	public boolean hasMessagesToSend() {
		return !messageWriter.isEmpty();
	}
	
}
