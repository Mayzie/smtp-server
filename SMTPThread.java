import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class SMTPThread implements Runnable {
	boolean closed; 
	Socket clientSocket;
	
	BufferedReader clientSocketIn;
	PrintWriter clientSocketOut;
	
	String lastCommand;
	
	String domainName;
	String mailFrom;
	List<String> mailTo;
	List<String> mailData;

	public SMTPThread(Socket socket) {
		this.clientSocket = socket;
		this.closed = false;
		
		this.mailTo = new ArrayList<String>();
		this.mailData = new ArrayList<String>();
	}
	
	private void reset() {
		this.mailFrom = null;
		this.mailTo.clear();
		this.mailData.clear();
	}
	
	private void parseCommand() {
		if(this.lastCommand != null) {
			try {
				if(this.lastCommand.startsWith("HELO ") || this.lastCommand.startsWith("EHLO ")) {
					System.out.println("In HELO");
					this.domainName = this.lastCommand.substring(5);
					reset();
					
					this.clientSocketOut.println("250 Hello " + this.domainName);
				} else if(this.lastCommand.startsWith("MAIL")) {
					if(this.lastCommand.startsWith("MAIL FROM:")) {
						String tempEmail = this.lastCommand.substring(10);
						if(tempEmail.matches("(?i)^.*@.*(sydney|usyd)\\.edu\\.au$")) {
							this.mailFrom = this.lastCommand.substring(10);							
							this.clientSocketOut.println("250 Your email is " + this.mailFrom);
						} else {
							this.clientSocketOut.println("553 Domain not accepted");
						}
					} else {
						this.clientSocketOut.println("502 I recognise MAIL, but not what comes after");
					}
				} else if(this.lastCommand.startsWith("RCPT TO:")) {
					this.mailTo.add(this.lastCommand.substring(8));
					
					this.clientSocketOut.println("250 The recipients email is " + this.mailTo);
				} else if(this.lastCommand.equals("DATA")) {
					if((this.mailFrom == null) || (this.mailFrom.isEmpty())) {
						this.clientSocketOut.println("503 Whoops, looks like you forgot a sender (MAIL FROM)");
					} else if(this.mailTo.isEmpty()) {
						this.clientSocketOut.println("503 Whoops, looks like you forgot to add a recipient (RCPT TO)");
					} else {
						this.clientSocketOut.println("354 Enter email body followed by <CRLF>.<CRLF>");
						
						try {
							this.mailData = new ArrayList<String>();
							String line;
							while((line = this.clientSocketIn.readLine()) != null && !(line.equals("."))) {
								this.mailData.add(line);
							}
							
							SMTPWrite email = new SMTPWrite();
							email.write("Message " + Integer.toString(email.getLocalID()));
							email.writeLines(this.mailData);
							email.close();
							
							reset();
							
							this.clientSocketOut.println("250 Data received, your message has now been saved");
						} catch (Exception exception) {
							this.clientSocketOut.println("554 Transaction failed");
						}
					}
				} else if(this.lastCommand.equals("NOOP")) {
					this.clientSocketOut.println("250 Recevied your NOOP");
				} else if(this.lastCommand.equals("RSET")) {
					this.clientSocketOut.println("250 OK");
				} else if(this.lastCommand.equals("QUIT")) {
					this.clientSocketOut.println("221 Goodbye =(");
					
					this.closed = true;
					this.clientSocket.close();
				} else {
					this.clientSocketOut.println("500 I do not understand that command");
				}
			} catch (Exception exception) {
				this.clientSocketOut.println("503 Something went wrong somewhere");
				
				System.err.println(exception.getLocalizedMessage());
			}
		}
	}

	@Override
	public void run() {
		if(this.clientSocket != null) {
			try {
				this.clientSocketIn = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
				this.clientSocketOut = new PrintWriter(this.clientSocket.getOutputStream(), true);
				
				this.clientSocketOut.println("220 Hello. Welcome to my SMTP server.");
				while((this.lastCommand = this.clientSocketIn.readLine()) != null) {
					parseCommand();
				}
			} catch (IOException exception) {
				if(!(this.closed)) {
					System.err.println("An I/O error has occurred when reading from the socket. See:");
					System.err.println(exception.getLocalizedMessage());
				}
			}
			
			// Close socket.
			if(!(this.closed)) {
				try {					
					this.clientSocket.close();
					
					this.closed = true;
				} catch (IOException exception) {
					System.err.println("An error has occurred when closing the socket.");
				}
			}
		}
	}
}
