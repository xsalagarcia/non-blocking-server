package Non.blocking.server;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Represents the server. It contains instances of SocketAccepter and SocketProcessor. It runs these instances with
 * a socket queue that communicates both.
 * @author xsala
 *
 */
public class Server {

	public SocketAccepter socketAccepter = null;
	public SocketProcessor socketProcessor = null;

	private int tcpPort = 0;



	/**
	 * Constructor. To start the server we need a port, a factory of MessageReader and MessageProcessor.
	 * MessageReader and MessageProcessor are interfaces, so, we can start the server with different protocols.
	 * @param tcpPort {@code Server.tcpPort} is the listener port of the server.
	 */
	public Server (int tcpPort) {
		this.tcpPort = tcpPort;
	}

	/**
	 * Initializes an {@code ArrayBlockingQueue<Socket>} and with this, initializes a {@code SocketAccepter} with {@code Server.tcpPort}
	 * and {@SocketProcessor} with the {@code ArrayBlockingQueue<Socket>}, a {@code MessageBuffer} readBuffer and {@code MessageBuffer} writeBuffer,
	 * the messageReaderFactory and the messageProcessorFactory.
	 * @throws IOException
	 */
	public void start() throws IOException {


		/**The queue of sockets. {@code SocketAccepter} will pot sockets here, {@code SocketProcessor} in another thread will process the sockets.
		 * Being an {code ArrayBlockingQueue}: Once created, the capacity cannot be changed. Attempts to put an element into a full queue will result in the operation blocking; attempts to take an element from an empty queue will similarly block, BUT WE CAN USE poll() and will return the element or null, without blocking.*/
		Queue<Socket> socketQueue = new ArrayBlockingQueue<Socket>(1024);

		socketAccepter = new SocketAccepter(tcpPort, socketQueue);


		socketProcessor = new SocketProcessor(socketQueue);

		Thread accepterThread = new Thread(this.socketAccepter);
		Thread processorThread = new Thread(this.socketProcessor);


		//When to stop?//TODO
		accepterThread.start();
		processorThread.start();
	}

}
