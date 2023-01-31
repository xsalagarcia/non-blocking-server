package Non.blocking.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

public class MessageWriter {

	
	public ByteBuffer messageByteBufferInProgress = null;
	
	public Queue<ByteBuffer> messageBufferQueue = new LinkedList<ByteBuffer>();
	
	/**Number of bytes writen from {@code this.messageByteBufferInProgress}.*/
	private int bytesWriten = 0;
	
	/**
	 * Puts the messageByteBufer in read mode.
	 * Enqueues a new message (@code ByteBuffer}. If there is not a {@code this.messageByteBufferInProgress}, will be it.
	 * If there is a {@code this.messageByteBufferInProgress}, the message will be enqueued at {@code this.messageBufferQueue}.
	 * @param messageByteBuffer A message.
	 * @throws Exception If there was too many messages on the queue.
	 */
	public void enqueue(ByteBuffer messageByteBuffer) throws Exception{ //if exception is thrown, there are too many messages in the queue.
		messageByteBuffer.flip();
		if (messageByteBufferInProgress == null) {
			this.messageByteBufferInProgress = messageByteBuffer;
		} else {
			messageBufferQueue.add(messageByteBuffer); //will throw Exception if it's not possible to add a new element.
		}
	}
	
	
	/**
	 * Tries to send the {@code messageByteBufferInProgres}.
	 * If it's fully sent, removes it put the next from {@code this.messageBufferQueue} or null if the queue is empty.
	 * @param socket
	 * @throws NotYetConnectedException If this channel is not yet connectedIOException 
	 * @throws IOException If some other I/O error occurs
	 */
	public void write (Socket socket) throws IOException { 
		
		bytesWriten += socket.write(messageByteBufferInProgress);
		
		if (bytesWriten >= this.messageByteBufferInProgress.capacity()) { //the message has fully sent.
			messageByteBufferInProgress = messageBufferQueue.poll(); //the next message or null.
		}
		
	}
	
	/**
	 * Checks if {@code MessageWriter} is empty.
	 * @return true if it's empty. Otherwise false.
	 */
	public boolean isEmpty() {
		return this.messageBufferQueue.isEmpty() && this.messageByteBufferInProgress == null;
	}
}
