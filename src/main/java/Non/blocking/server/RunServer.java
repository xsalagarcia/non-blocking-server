package Non.blocking.server;

import java.io.IOException;

public class RunServer {

	public static void main(String[] args) {
		Server s = new Server(9999);
		
		try {
			s.start();
			
			try {
				Thread.sleep(60000);
				s.socketAccepter.stop();
				s.socketProcessor.stop();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

}
