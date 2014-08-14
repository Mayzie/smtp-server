import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;


public class SMTPWrite {
	private static int globalID;	
	private PrintWriter emailFile;
	
	public int localID;
	
	public SMTPWrite() throws FileNotFoundException {
		new File("emails/").mkdirs();	// Create the `emails' directory if it doesn't exist.
		
		synchronized(this) {
			this.localID = globalID++;
		}
		
		this.emailFile = new PrintWriter("emails/email" + Integer.toString(this.localID) + ".txt");
	}
	
	public void close() {
		if(this.emailFile != null)
			this.emailFile.close();
	}
	
	public int getLocalID() {
		return this.localID;
	}
	
	public void write(String line) {
		if(this.emailFile != null) {
			this.emailFile.println(line);
		}
	}
	
	public void writeLines(List<String> lines, String prefix) {
		if(this.emailFile != null) {
			for(String line : lines) {
				if((prefix != null) && !(prefix.isEmpty()))
					this.emailFile.print(prefix);
				this.emailFile.println(line);
			}
		}
	}
	
	public void writeLines(List<String> lines) {
		writeLines(lines, null);
	}
}
