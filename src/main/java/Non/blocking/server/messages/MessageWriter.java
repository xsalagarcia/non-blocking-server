package Non.blocking.server.messages;

import Non.blocking.server.ServerException;
import Non.blocking.server.Socket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

public class MessageWriter {


	private ByteBuffer messageByteBufferInProgress = null;

	private Queue<ByteBuffer> messageBufferQueue = new LinkedList<ByteBuffer>();

	private Socket socket = null;

	public MessageWriter(Socket socket) {
		this.socket = socket;
	}

	/**
	 * Puts the messageByteBufer in read mode (does a flip!).
	 * Enqueues a new message (@code ByteBuffer}. If there is not a {@code this.messageByteBufferInProgress}, will be it.
	 * If there is a {@code this.messageByteBufferInProgress}, the message will be enqueued at {@code this.messageBufferQueue}.
	 * @param messageByteBuffer A message.
	 */
	private void enqueue(ByteBuffer messageByteBuffer) {
		messageByteBuffer.flip();
		if (messageByteBufferInProgress == null) {
			this.messageByteBufferInProgress = messageByteBuffer;
		} else {
			messageBufferQueue.add(messageByteBuffer); //will throw Exception if it's not possible to add a new element to the queue.
		}
	}


	/**
	 * Tries to send the {@code messageByteBufferInProgres}.
	 * If it's fully sent, removes it put the next from {@code this.messageBufferQueue} or null if the queue is empty.
	 * @throws IOException If some other I/O error occurs
	 */
	public void write () throws ServerException {

		if (this.messageByteBufferInProgress != null) {
			try {
				socket.socketChannel.write(this.messageByteBufferInProgress);
			} catch (Exception e){
				throw new ServerException(ServerException.Type.CONNECTION_PROBLEM);
			}
			if (!this.messageByteBufferInProgress.hasRemaining()) {
				this.messageByteBufferInProgress = messageBufferQueue.poll();
			}
		} else {
			this.messageByteBufferInProgress = messageBufferQueue.poll();
		}
	}

	/**
	 * Tries to enqueue a message to send.
	 * @param bb
	 */
	public void enqueueMessage(ByteBuffer bb) {
		enqueue(bb);
	}

	/**
	 * Checks if {@code MessageWriter} is empty.
	 * @return true if it's empty. Otherwise false.
	 */
	public boolean isEmpty() {
		return this.messageBufferQueue.isEmpty() && this.messageByteBufferInProgress == null;
	}
}
