package demo.HTMLParser;
import java.io.*;

class ParserThread extends Thread {		  
  HTMLParser parser;

  ParserThread(HTMLParser p) {
    parser = p;
  }

  public void run() {				  // convert pipeOut to pipeIn
    try {
      try {					  // parse document to pipeOut
	parser.HTMLDocument(); 
      } catch (ParseException e) {
	System.out.println("Parse Aborted: " + e.getMessage());
      } catch (TokenMgrError e) {
	System.out.println("Parse Aborted: " + e.getMessage());
      } finally {
	parser.pipeOut.close();
	synchronized (parser) {
	  parser.summary.setLength(parser.SUMMARY_LENGTH);
	  parser.titleComplete = true;
	  parser.notifyAll();
	}
      }
    } catch (IOException e) {
	e.printStackTrace();
    }
  }
}
