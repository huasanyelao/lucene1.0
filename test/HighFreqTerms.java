package test;

import java.io.File;

import com.lucene.util.PriorityQueue;
import com.lucene.store.Directory;
import com.lucene.store.FSDirectory;
import com.lucene.index.IndexReader;
import com.lucene.index.Term;
import com.lucene.index.TermEnum;

class HighFreqTerms {
  public static int numTerms = 100;

  public static void main(String[] args) {
    try {
      Directory directory = new FSDirectory(new File("demo index"), false);
      IndexReader reader = IndexReader.open(directory);

      TermInfoQueue tiq = new TermInfoQueue(numTerms);
      TermEnum terms = reader.terms();

      int minFreq = 0;
      while (terms.next()) {
	if (terms.docFreq() > minFreq) {
	  tiq.put(new TermInfo(terms.term(), terms.docFreq()));
	  if (tiq.size() > numTerms) {		  // if tiq overfull
	    tiq.pop();				  // remove lowest in tiq
	    minFreq = ((TermInfo)tiq.top()).docFreq; // reset minFreq
	  }
	}
      }

      while (tiq.size() != 0) {
	TermInfo termInfo = (TermInfo)tiq.pop();
	System.out.println(termInfo.term + " " + termInfo.docFreq);
      }

      reader.close();
      directory.close();

    } catch (Exception e) {
      System.out.println(" caught a " + e.getClass() +
			 "\n with message: " + e.getMessage());
    }
  }
}

final class TermInfo {
  TermInfo(Term t, int df) {
    term = t;
    docFreq = df;
  }
  int docFreq;
  Term term;
}

final class TermInfoQueue extends PriorityQueue {
  TermInfoQueue(int size) {
    initialize(size);
  }
  protected final boolean lessThan(Object a, Object b) {
    TermInfo termInfoA = (TermInfo)a;
    TermInfo termInfoB = (TermInfo)b;
    return termInfoA.docFreq < termInfoB.docFreq;
  }
}

