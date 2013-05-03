package test.com.lucene.index;

import com.lucene.analysis.SimpleAnalyzer;
import com.lucene.analysis.Analyzer;
import com.lucene.store.FSDirectory;
import com.lucene.store.Directory;
import com.lucene.document.Document;
import com.lucene.index.*;
import demo.FileDocument;

import java.io.File;
import java.util.Date;


class DocTest {
  public static void main(String[] args) {
    try {
      Directory directory = FSDirectory.getDirectory("test", true);
      directory.close();

      indexDoc("one", "test.txt");
      printSegment("one");
      indexDoc("two", "test2.txt");
      printSegment("two");
      
      merge("one", "two", "merge");
      printSegment("merge");

      merge("one", "two", "merge2");
      printSegment("merge2");

      merge("merge", "merge2", "merge3");
      printSegment("merge3");

    } catch (Exception e) {
      System.out.println(" caught a " + e.getClass() +
			 "\n with message: " + e.getMessage());
    }
  }

  public static void indexDoc(String segment, String fileName)
       throws Exception {
    Directory directory = FSDirectory.getDirectory("test", false);
    Analyzer analyzer = new SimpleAnalyzer();
    DocumentWriter writer = new DocumentWriter(directory, analyzer, 1000);

    File file = new File(fileName);
    Document doc = FileDocument.Document(file);

    writer.addDocument(segment, doc);

    directory.close();
  }

  static void merge(String seg1, String seg2, String merged)
       throws Exception {
    Directory directory = FSDirectory.getDirectory("test", false);

    SegmentReader r1 = new SegmentReader(new SegmentInfo(seg1, 1, directory));
    SegmentReader r2 = new SegmentReader(new SegmentInfo(seg2, 1, directory));

    SegmentMerger merger = new SegmentMerger(directory, merged);
    merger.add(r1);
    merger.add(r2);
    merger.merge();

    directory.close();
  }

  static void printSegment(String segment)
       throws Exception {
    Directory directory = FSDirectory.getDirectory("test", false);
    SegmentReader reader =
      new SegmentReader(new SegmentInfo(segment, 1, directory));

    for (int i = 0; i < reader.numDocs(); i++)
      System.out.println(reader.document(i));
    
    TermEnum tis = reader.terms();
    while (tis.next()) {
      System.out.print(tis.term());
      System.out.println(" DF=" + tis.docFreq());
      
      TermPositions positions = reader.termPositions(tis.term());
      try {
	while (positions.next()) {
	  System.out.print(" doc=" + positions.doc());
	  System.out.print(" TF=" + positions.freq());
	  System.out.print(" pos=");
	  System.out.print(positions.nextPosition());
	  for (int j = 1; j < positions.freq(); j++)
	    System.out.print("," + positions.nextPosition());
	  System.out.println("");
	}
      } finally {
	positions.close();
      }
    }
    tis.close();
    reader.close();
    directory.close();
  }
}
