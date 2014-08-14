import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class MySMTPServer {

	public static void main(String[] args) {			
		try {
			ServerSocket smtpServer = new ServerSocket(6013);
			
			while (true) {
				Socket clientConnection = smtpServer.accept();
	
				Thread clientThread = new Thread(new SMTPThread(clientConnection));
				clientThread.start();
			}
		} catch (IOException exception) {
			System.err.println("An I/O error has occurred. See:");
			System.err.println(exception.getLocalizedMessage());
		} catch (Exception exception) {
			System.err.println("An error has occurred. See:");
			System.err.println(exception.getLocalizedMessage());
		}
		
	}

}
