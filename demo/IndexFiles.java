package demo;

import com.lucene.analysis.StopAnalyzer;
import com.lucene.index.IndexWriter;

import java.io.File;
import java.util.Date;

class IndexFiles {
  public static void main(String[] args) {
    try {
      Date start = new Date();

      IndexWriter writer = new IndexWriter("index", new StopAnalyzer(), true);
      writer.mergeFactor = 20;

      indexDocs(writer, new File(args[0]));

      writer.optimize();
      writer.close();

      Date end = new Date();

      System.out.print(end.getTime() - start.getTime());
      System.out.println(" total milliseconds");

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
