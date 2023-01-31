package Non.blocking.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Queue;

/**
 * This class is Runnable, to run with the accepter thread (the thread that accepts new connexions).
 * {@code run()}: When a new connection comes through the ServerSocketChannel, a SocketChannel will be
 * created and added to the queue ({@code ArrayBlockingQueue}). This queue is shared with {@SocketProcessor}.
 * (funcional)
 * @author xsala (With Jenkov's reference)
 *
 */
public class SocketAccepter implements Runnable {

	/**{@code ServerSocketChannel} port*/
	private int tcpPort = 0;
	
	/**The {@ServerSocketChannel}. It wait for new connections creating a {@code SocketChannel}. The {@code SocketChannel}'s created with an incoming connection is put to a {@code Queue}*/
	private ServerSocketChannel serverSocket = null;
	
	/**A {@code Queue} with all the Socket of the incoming connections. This class {@code Non.blocking.server.Socket} contains the {@SocketChannel}'s of the incoming connections.*/
	private Queue<Socket> socketQueue = null;
	
	/**If it is turn {@code true} the SocketAccepter will turn off*/
	private volatile boolean hasToStop = false;
	
	/**
	 * Constructor. 
	 * @param tcpPort The server port for waiting for incoming connections.
	 * @param socketQueue Maybe {@code ArrayBlockingQueue}.
	 */
	public SocketAccepter (int tcpPort, Queue<Socket> socketQueue) {
		this.tcpPort = tcpPort;
		this.socketQueue = socketQueue;
	}
	@Override
	public void run() {
		try {
			serverSocket = ServerSocketChannel.open();
			serverSocket.bind(new InetSocketAddress(tcpPort));
		
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		while (!hasToStop) {
			try {
				
				SocketChannel socketChannel = this.serverSocket.accept(); //blocking mode, will wait until a new connection enters.
				socketQueue.add(new Socket(socketChannel)); //It's an ArrayBlockingQueue, so, if the queue is full the thread will wait until the socket can be put.
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
	}
	
	public void stop() {
		hasToStop = true;
	}

}
