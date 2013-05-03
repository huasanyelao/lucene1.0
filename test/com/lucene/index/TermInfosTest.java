package test.com.lucene.index;

import com.lucene.index.FieldInfos;
import com.lucene.index.SegmentTermEnum;
import com.lucene.index.Term;
import com.lucene.index.TermEnum;
import com.lucene.index.TermInfo;
import com.lucene.index.TermInfosReader;
import com.lucene.index.TermInfosWriter;
import com.lucene.store.Directory;
import com.lucene.store.FSDirectory;

import java.util.Date;
import java.util.Random;
import java.util.Vector;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;

class TermInfosTest {
  public static void main(String[] args) {
    try {
      test();
    } catch (Exception e) {
      System.out.println(" caught a " + e.getClass() +
			 "\n with message: " + e.getMessage());
    }
  }

  public static void test()
       throws Exception {
    
    File file = new File("words.txt");
    System.out.println(" reading word file containing " +
		       file.length() + " bytes");

    Date start = new Date();

    Vector keys = new Vector();
    FileInputStream ws = new FileInputStream(file);
    BufferedReader wr = new BufferedReader(new InputStreamReader(ws));

    for (String key = wr.readLine(); key!=null; key = wr.readLine())
      keys.addElement(new Term("word", key));
    wr.close();

    Date end = new Date();

    System.out.print(end.getTime() - start.getTime());
    System.out.println(" milliseconds to read " + keys.size() + " words");

    start = new Date();

    Random gen = new Random(1251971);
    long fp = (gen.nextInt() & 0xF) + 1;
    long pp = (gen.nextInt() & 0xF) + 1;
    int[] docFreqs = new int[keys.size()];
    long[] freqPointers = new long[keys.size()];
    long[] proxPointers = new long[keys.size()];
    for (int i = 0; i < keys.size(); i++) {
      docFreqs[i] = (gen.nextInt() & 0xF) + 1;
      freqPointers[i] = fp;
      proxPointers[i] = pp;
      fp += (gen.nextInt() & 0xF) + 1;;
      pp += (gen.nextInt() & 0xF) + 1;;
    }

    end = new Date();

    System.out.print(end.getTime() - start.getTime());
    System.out.println(" milliseconds to generate values");

    start = new Date();
    
    Directory store = new FSDirectory(new File("test.store"), true);
    FieldInfos fis = new FieldInfos();

    TermInfosWriter writer = new TermInfosWriter(store, "words", fis);
    fis.add("word", false);

    for (int i = 0; i < keys.size(); i++)
      writer.add((Term)keys.elementAt(i),
		 new TermInfo(docFreqs[i], freqPointers[i], proxPointers[i]));

    writer.close();

    end = new Date();

    System.out.print(end.getTime() - start.getTime());
    System.out.println(" milliseconds to write table");

    System.out.println(" table occupies " +
		       store.fileLength("words.tis") + " bytes");

    start = new Date();

    TermInfosReader reader = new TermInfosReader(store, "words", fis);

    end = new Date();

    System.out.print(end.getTime() - start.getTime());
    System.out.println(" milliseconds to open table");

    start = new Date();

    SegmentTermEnum termEnum = (SegmentTermEnum)reader.terms();
    for (int i = 0; i < keys.size(); i++) {
    	termEnum.next();
      Term key = (Term)keys.elementAt(i);
      if (!key.equals(termEnum.term()))
	throw new Exception("wrong term: " + termEnum.term()
			    + ", expected: " + key
			    + " at " + i);
      TermInfo ti = termEnum.termInfo();
      if (ti.docFreq != docFreqs[i])
	throw
	  new Exception("wrong value: " + Long.toString(ti.docFreq, 16)
			+ ", expected: " + Long.toString(docFreqs[i], 16)
			+ " at " + i);
      if (ti.freqPointer != freqPointers[i])
	throw
	  new Exception("wrong value: " + Long.toString(ti.freqPointer, 16)
			+ ", expected: " + Long.toString(freqPointers[i], 16)
			+ " at " + i);
      if (ti.proxPointer != proxPointers[i])
	throw
	  new Exception("wrong value: " + Long.toString(ti.proxPointer, 16)
			+ ", expected: " + Long.toString(proxPointers[i], 16)
			+ " at " + i);
    }

    end = new Date();

    System.out.print(end.getTime() - start.getTime());
    System.out.println(" milliseconds to iterate over " +
		       keys.size() + " words");

    start = new Date();

    for (int i = 0; i < keys.size(); i++) {
      Term key = (Term)keys.elementAt(i);
      TermInfo ti = reader.get(key);
      if (ti.docFreq != docFreqs[i])
	throw
	  new Exception("wrong value: " + Long.toString(ti.docFreq, 16)
			+ ", expected: " + Long.toString(docFreqs[i], 16)
			+ " at " + i);
      if (ti.freqPointer != freqPointers[i])
	throw
	  new Exception("wrong value: " + Long.toString(ti.freqPointer, 16)
			+ ", expected: " + Long.toString(freqPointers[i], 16)
			+ " at " + i);
      if (ti.proxPointer != proxPointers[i])
	throw
	  new Exception("wrong value: " + Long.toString(ti.proxPointer, 16)
			+ ", expected: " + Long.toString(proxPointers[i], 16)
			+ " at " + i);
    }

    end = new Date();

    System.out.print((end.getTime() - start.getTime()) / (float)keys.size());
    System.out.println(" average milliseconds per lookup");

    TermEnum e = reader.terms(new Term("word", "azz"));
    System.out.println("Word after azz is " + e.term().text);

    reader.close();

    store.close();
  }
}
