package Non.blocking.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * This class is Runnable, to run with the processor thread (the thread that processes the connections).
 * {@code this.inboundSocketQueue} is a {@code ArrayBlockingQueue} shared with {@SocketAccepter}. {@code SocketAccepter} 
 * puts the {@code Socket} (contains {@code SocketChannel} to the queue.
 * (TODO)
 * @author xsala (with Jenkov's reference)
 *
 */
public class SocketProcessor implements Runnable {
	
	private volatile boolean hasToStop = false;
	
	/**The queue of {@code Socket}'s that is shared with {@code SocketAccepter}, see {@code Server.socketQueue}*/
	private Queue<Socket> inboundSocketQueue = null;

	/**A Map with all active logged {@code Socket}'s. <Socket.socketId, Socket>*/
	private Map<String, Socket> loggedSockets = new HashMap<String, Socket>();
	
	/**A map with all active non-logged {@code Socket}'s*/
	private Set<Socket> nonLoggedSockets = new HashSet<Socket>();

	
	/**{@code Selector} with {@code Socket.SocketChannel}'s with {@code SelectionKey.READ_OP}.*/
	private Selector readSelector = null;
	
	/**{@code Selector} with {@code Socket.SocketChannel}'s with {@code SelectionKey.WRITE_OP}.*/
	private Selector writeSelector = null;

	
	
	public SocketProcessor(Queue<Socket> inboundSocketQueue) throws IOException {
		this.inboundSocketQueue = inboundSocketQueue; //passed an ArrayBlockingQueue
		this.readSelector = Selector.open(); //creates a selector for reading.
		this.writeSelector = Selector.open(); //creates a selector for writing.
	}

	
	
	@Override
	public void run() {
		while (!hasToStop) {
			try {
				executeCycle();
			} catch(IOException e) {
				e.printStackTrace();
			}
			
			try {
				Thread.sleep(100); //Give me some rest time, says the thread.
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	
	/**
	 * Execute a cycle calling {@code takeNewSockets()}, {@code readFromSockets} and {@code writeToSockets}
	 * @throws IOException
	 */
	private void executeCycle() throws IOException {
		deleteNonConnecteds();
		takeNewSockets();
		readFromSockets();
		writeToSockets();
	}
	
	/**
	 * Deletes non connected sockets from {@code loggedSockets} and {@code nonLoggedSockets}.
	 */
	private void deleteNonConnecteds() {
		Iterator<Socket> iterator = loggedSockets.values().iterator();
		while (iterator.hasNext()) {
			if (!iterator.next().socketChannel.isConnected()) iterator.remove();
		}
		
		iterator = nonLoggedSockets.iterator();
		while (iterator.hasNext()) {
			if (!iterator.next().socketChannel.isConnected()) iterator.remove();
		}
	}
	
	/**
	 * Takes all new sockets if exists, from {@code this.inboundSocketQueue}.
	 * Sets to non-blocking mode and puts into {@code this.nonLoggedSockets}.
	 * Sets nonLogged an loggedSockets to the new socket.
	 * Registers the new socket's SocketChannel to the readSelector and writeSelector.
	 * 
	 * @throws IOException
	 */
	public void takeNewSockets() throws IOException {
		Socket newSocket = this.inboundSocketQueue.poll(); //it doesn't block. Return null or retrieves and removes the head of the queue.
		
		
		//Loop while we have new sockets.
		while (newSocket != null) {
			
			
			//turns the SocketChannel to non-blocking.
			newSocket.socketChannel.configureBlocking(false);
			
			//puts the new socket into the nonLoggedSocketsSet.
			this.nonLoggedSockets.add(newSocket);
			
			//sets logged and nonLoggedSockets at newSocket
			newSocket.nonLoggedSockets = nonLoggedSockets;
			newSocket.loggedSockets = loggedSockets;
			
			//associates the new socket with the readSelector.
			SelectionKey key = newSocket.socketChannel.register(this.readSelector, SelectionKey.OP_READ);
			
			//For socket recuperation at this.readFromSocket().
			key.attach(newSocket);
			
			//associates the new socket with the readSelector.
			key = newSocket.socketChannel.register(this.writeSelector, SelectionKey.OP_READ);
			
			//For socket recuperation at this.readFromSocket().
			key.attach(newSocket);

			//This will get another socket from inboundSocketQueue contains more, and will repeat while loop.
			//If there is no more sockets, will return null and exists from loop.
			newSocket = this.inboundSocketQueue.poll(); 
			
		}
	}
	
	/**
	 * Gets all the sockets ready for read.
	 * For each ready for read socket calls {@code readFromSocket()}
	 * @throws IOException
	 */
	public void readFromSockets() throws IOException {
		//gets the number of sockets ready for reading.
		int readReady = this.readSelector.selectNow();
		
		if (readReady > 0) {
			
			//gets all the SelectionKey associated with the channels ready for reading.
			Set<SelectionKey> selectedKeys = this.readSelector.selectedKeys();
			Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
			
			while(keyIterator.hasNext()) {
				
				SelectionKey key = keyIterator.next();
				
				//calling readFromSocket reads the SocketChannel.
				readFromSocket(key);
				
				//We've read the SocketChannel, thus we have to remove it from the Set<SelectionKey>
				keyIterator.remove();
			}
			
			//selectedKeys.clear(); //now we don't have selectedKeys//?if we have removed..?
		}
	}
	
	/**
	 * Reads the ChannelSocket associated with the key at the parameter.
	 * @param key Contains the {@code Socket} to read.
	 * @throws IOException
	 */
	private void readFromSocket(SelectionKey key ) throws IOException{
		
		//getting the object associated with the key.
		Socket socket = (Socket) key.attachment();
		
		socket.read();

	}
	
	
	private void writeToSockets() {
	//TODO	
	}
	
	public void stop() {
		hasToStop = true;
	}
	
		
	
	

}
