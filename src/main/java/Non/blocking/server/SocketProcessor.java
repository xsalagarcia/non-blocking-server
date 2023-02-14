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
 * inboundSocketQueue is an ArrayBlockingQueue shared with {@code SocketAccepter}.
 * puts the {@code Socket} (contains {@code SocketChannel} to the queue.
 * (TODO)
 * @author xsala
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

	private long nextSocketId = 0;



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
				Thread.sleep(1000); //Give me some rest time, says the thread.
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
		deleteNonConnecteds();// <--Should be changed. Non connected clients are not detected with socketChannel.isConnected()
		takeNewSockets();
		readFromSockets();
		writeToSockets();
	}

	/**
	 * Deletes non connected sockets from {@code loggedSockets} and {@code nonLoggedSockets}.
	 * Doesn't detect if client is closed. Only detects if it's closed by server.
	 */
	private void deleteNonConnecteds() {

		Iterator<Socket> iterator = loggedSockets.values().iterator();
		while (iterator.hasNext()) {
			Socket s = iterator.next();
			if (!s.socketChannel.isConnected()){
				removeKeys(s);
				iterator.remove();
			}
			//readSelector.selectedKeys()

		}

		iterator = nonLoggedSockets.iterator();
		while (iterator.hasNext()) {
			Socket s = iterator.next();
			if (!s.socketChannel.isConnected()) {
				removeKeys(s);
				iterator.remove();
			}
		}
	}


	private void removeKeys (Socket s ){
		SelectionKey sk = s.socketChannel.keyFor(readSelector);
		if (sk!=null){
			sk.cancel();
		}
		sk = s.socketChannel.keyFor(writeSelector);
		if (sk != null) {
			sk.cancel();
		}
	}


	/**
	 * Takes all new sockets if exists, from {@code this.inboundSocketQueue}.
	 * Sets to non-blocking mode and puts into {@code this.nonLoggedSockets}.
	 * Sets nonLogged an loggedSockets to the new socket.
	 * Registers the new socket's SocketChannel to the readSelector and writeSelector.
	 * @throws IOException
	 */
	private void takeNewSockets() throws IOException {
		Socket newSocket = this.inboundSocketQueue.poll(); //it doesn't block. Return null or retrieves and removes the head of the queue.


		//Loop while we have new sockets.
		while (newSocket != null) {


			//turns the SocketChannel to non-blocking.
			newSocket.socketChannel.configureBlocking(false);

			newSocket.socketId = nextSocketId++;

			//puts the new socket into the nonLoggedSocketsSet.
			this.nonLoggedSockets.add(newSocket);

			//sets logged and nonLoggedSockets at newSocket
			newSocket.nonLoggedSockets = nonLoggedSockets;
			newSocket.loggedSockets = loggedSockets;

			newSocket.sendWelcome();

			//associates the new socket with the readSelector.
			SelectionKey key = newSocket.socketChannel.register(this.readSelector, SelectionKey.OP_READ);

			//For socket recuperation at this.readFromSocket().
			key.attach(newSocket);

			//associates the new socket with the readSelector.
			key = newSocket.socketChannel.register(this.writeSelector, SelectionKey.OP_WRITE);

			//For socket recuperation at this.readFromSocket().
			key.attach(newSocket);

			//This will get another socket from inboundSocketQueue contains more, and will repeat while loop.
			//If there is no more sockets, will return null and exists from loop.
			newSocket = this.inboundSocketQueue.poll();

		}
	}

	private void writeToSockets() throws IOException {
		int writeReady = this.writeSelector.selectNow();

		if (writeReady > 0) {

			Set<SelectionKey> selectedKeys = this.writeSelector.selectedKeys();
			Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

			while (keyIterator.hasNext()) {
				SelectionKey key = keyIterator.next();

				writeToSocket(key);

				keyIterator.remove();
			}
		selectedKeys.clear();
		}
	}

	/**
	 * Gets all the sockets ready for read.
	 * For each ready for read socket calls {@code readFromSocket()}
	 * @throws IOException
	 */
	private void readFromSockets() throws IOException {
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

			selectedKeys.clear(); //now we don't have selectedKeys//?if we have removed..?
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


	private void writeToSocket(SelectionKey key) {

		Socket socket = (Socket) key.attachment();

		socket.write();
	}

	public void stop() {
		hasToStop = true;
	}





}
