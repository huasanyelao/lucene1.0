package test;

import com.lucene.analysis.SimpleAnalyzer;
import com.lucene.analysis.Analyzer;
import com.lucene.analysis.TokenStream;
import com.lucene.analysis.Token;

import java.io.Reader;
import java.io.StringReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Random;

class AnalysisTest {
  public static void main(String[] args) {
    try {
      test("This is a test", true);
      test(new File("words.txt"), false);
    } catch (Exception e) {
      System.out.println(" caught a " + e.getClass() +
			 "\n with message: " + e.getMessage());
    }
  }

  static void test(File file, boolean verbose)
       throws Exception {
    long bytes = file.length();
    System.out.println(" Reading test file containing " + bytes + " bytes.");

    FileInputStream is = new FileInputStream(file);
    BufferedReader ir = new BufferedReader(new InputStreamReader(is));
    
    test(ir, verbose, bytes);

    ir.close();
  }

  static void test(String text, boolean verbose) throws Exception {
    System.out.println(" Tokenizing string: " + text);
    test(new StringReader(text), verbose, text.length());
  }

  static void test(Reader reader, boolean verbose, long bytes)
       throws Exception {
    Analyzer analyzer = new SimpleAnalyzer();
    TokenStream stream = analyzer.tokenStream(null, reader);

    Date start = new Date();

    int count = 0;
    for (Token t = stream.next(); t!=null; t = stream.next()) {
      if (verbose) {
	System.out.println("Text=" + t.termText()
			   + " start=" + t.startOffset()
			   + " end=" + t.endOffset());
      }
      count++;
    }

    Date end = new Date();

    long time = end.getTime() - start.getTime();
    System.out.println(time + " milliseconds to extract " + count + " tokens");
    System.out.println((time*1000.0)/count + " microseconds/token");
    System.out.println((bytes * 1000.0 * 60.0 * 60.0)/(time * 1000000.0)
		       + " megabytes/hour");
  }
}
