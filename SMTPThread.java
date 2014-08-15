import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class SMTPThread implements Runnable {
	private static final int FROM = 0;
	private static final int TO = 1;
	private static final int DATE = 2;
	private static final int SUBJECT = 3;
	
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
		
		reset();
	}
	
	/**
	 * (Re)initialises session data obtained from MAIL FROM, RCPT TO, and DATA.
	 */
	private void reset() {
		this.mailFrom = null;
		this.mailTo = new ArrayList<String>();
		this.mailData = new ArrayList<String>();
	}
	
	/**
	 * Parses lines sent to it via the socket. If lines are recognised commands, then it will act on those commands.
	 */
	private void parseCommand() {
		if(this.lastCommand != null) {
			try {
				if(this.lastCommand.toUpperCase().startsWith("HELO ") || 
						this.lastCommand.toUpperCase().startsWith("EHLO ")) {	// Check for "HELO" or "EHLO"
					this.domainName = this.lastCommand.substring(5);
					reset();
					
					this.clientSocketOut.println("250 Hello " + this.domainName);
				} else if(this.lastCommand.toUpperCase().startsWith("MAIL")) {	// Check for "MAIL"
					if(this.lastCommand.toUpperCase().startsWith("MAIL FROM:")) {	// Check that the command is "MAIL FROM"
						String tempEmail = this.lastCommand.substring(10);
						if(tempEmail.matches("(?i)^([A-z0-9]+(\\.[A-z0-9]+)?)+@(([A-z0-9]+|[A-z0-9]+-[A-z0-9]+)\\.)*(sydney|usyd)\\.edu\\.au$")) {	// Validate that the email belongs to USyd.
							this.mailFrom = this.lastCommand.substring(10);							
							this.clientSocketOut.println("250 Your email is " + this.mailFrom);
						} else {
							this.clientSocketOut.println("550 Domain not accepted");
						}
					} else {
						this.clientSocketOut.println("502 I recognise MAIL, but not what comes after");
					}
				} else if(this.lastCommand.toUpperCase().startsWith("RCPT TO:")) {	// Check for "RCPT TO"
					this.mailTo.add(this.lastCommand.substring(8));
					
					this.clientSocketOut.println("250 The recipients email is " + this.mailTo);
				} else if(this.lastCommand.toUpperCase().equals("DATA")) {	// Check for "DATA"
					if((this.mailFrom == null) || (this.mailFrom.isEmpty())) {	// Check that we have been given information from "MAIL FROM"
						this.clientSocketOut.println("503 Whoops, looks like you forgot a sender (MAIL FROM)");
					} else if(this.mailTo.isEmpty()) {	// Check that we have been given information from "RCPT TO"
						this.clientSocketOut.println("503 Whoops, looks like you forgot to add a recipient (RCPT TO)");
					} else {
						this.clientSocketOut.println("354 Enter email body followed by <CRLF>.<CRLF>");
						
						try {
							this.mailData.clear();
							// This is to ensure that the "From:", "To:", etc fields are in the correct order.
							for(int i = 0; i < 4; ++i) {
								this.mailData.add(null);
							}
							
							String line, tempLine;
							while((line = this.clientSocketIn.readLine()) != null && !(line.equals("."))) {
								// Parse the mail data.
								tempLine = line.toUpperCase();
								
								if((this.mailData.get(FROM) == null) && tempLine.startsWith("FROM: ")) {
									this.mailData.set(FROM, line);
								} else if((this.mailData.get(TO) == null) && tempLine.startsWith("TO: ")) {
									this.mailData.set(TO, line);
								} else if((this.mailData.get(DATE) == null) && tempLine.startsWith("DATE: ")) {
									this.mailData.set(DATE, line);
								} else if((this.mailData.get(SUBJECT) == null) && tempLine.startsWith("SUBJECT: ")) {
									this.mailData.set(SUBJECT, line);
								} else {
									this.mailData.add(line);
								}
							}
							
							// Check if the client has specified the sender, recipient, date, and subject.
							boolean validEmail = true;
							for(int i = 0; i < 4; ++i) {
								if(this.mailData.get(i) == null) {
									validEmail = false;
									break;
								}
							}
							
							if(!(validEmail)) {
								this.clientSocketOut.println("554 You have failed to specify a sender, recipient, date, or subject, in the mail data");
								
								return;
							}
							
							// Write to file.
							SMTPWrite email = new SMTPWrite();
							email.writeLn("Message " + Integer.toString(email.getLocalID()));
							for(int i = 0; i < 4; ++i) {
								if(this.mailData.get(i) != null) 
									email.writeLn(this.mailData.get(i));
							}
							email.write("Body: ");
							for(int i = 4; i < this.mailData.size(); ++i) {
								email.writeLn(this.mailData.get(i));
							}
							email.close();
							
							reset();
							
							this.clientSocketOut.println("250 Data received, your message has now been saved");
						} catch (Exception exception) {
							this.clientSocketOut.println("554 Transaction failed");
						}
					}
				} else if(this.lastCommand.toUpperCase().equals("NOOP")) {	// Check for "NOOP"
					this.clientSocketOut.println("250 Recevied your NOOP");
				} else if(this.lastCommand.toUpperCase().equals("RSET")) {	// Check for "RSET"
					this.clientSocketOut.println("250 OK");
				} else if(this.lastCommand.toUpperCase().equals("QUIT")) {	// Check for "QUIT"
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
				
				// Retrieve new lines from the socket.
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
