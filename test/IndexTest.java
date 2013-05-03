package test;

import com.lucene.analysis.SimpleAnalyzer;
import com.lucene.index.IndexWriter;
import com.lucene.index.TermPositions;
import com.lucene.document.Document;
import demo.FileDocument;

import java.io.File;
import java.util.Date;

class IndexTest {
  public static void main(String[] args) {
    try {
      Date start = new Date();

      IndexWriter writer = new IndexWriter("F:\\test", new SimpleAnalyzer(),
					   true);

      writer.mergeFactor = 20;

      indexDocs(writer, new File("F:\\recipes"));

      writer.optimize();
      writer.close();

      Date end = new Date();

      System.out.print(end.getTime() - start.getTime());
      System.out.println(" total milliseconds");

      Runtime runtime = Runtime.getRuntime();

      System.out.print(runtime.freeMemory());
      System.out.println(" free memory before gc");
      System.out.print(runtime.totalMemory());
      System.out.println(" total memory before gc");

      runtime.gc();

      System.out.print(runtime.freeMemory());
      System.out.println(" free memory after gc");
      System.out.print(runtime.totalMemory());
      System.out.println(" total memory after gc");

    } catch (Exception e) {
      System.out.println(" caught a " + e.getClass() +
			 "\n with message: " + e.getMessage());
    }
  }

  public static void indexDocs(IndexWriter writer, File file)
       throws Exception {
    if (file.isDirectory()) {
      String[] files = file.list();
      for (int i = 0; i < files.length; i++)
	indexDocs(writer, new File(file, files[i]));
    } else {
      System.out.println("adding " + file);
      writer.addDocument(FileDocument.Document(file));
    }
  }
}
