package test.unit.com.lucene.queryParser;

import java.io.*;
import junit.framework.*;

import com.lucene.*;
import com.lucene.queryParser.*;
import com.lucene.search.*;
import com.lucene.analysis.*;
import com.lucene.analysis.Token;

public class TestQueryParser extends TestCase {

   public TestQueryParser(String name) {
      super(name);
   }

  public static Analyzer qpAnalyzer = new QPTestAnalyzer();

  public static class QPTestFilter extends TokenFilter {

    /**
     * Filter which discards the token 'stop' and which expands the
     * token 'phrase' into 'phrase1 phrase2'
     */
    public QPTestFilter(TokenStream in) {
      input = in;
    }
    
    boolean inPhrase = false;
    int savedStart=0, savedEnd=0;

    public Token next() throws IOException {
      if (inPhrase) {
        inPhrase = false;
        return new Token("phrase2", savedStart, savedEnd);
      }
      else
        for (Token token = input.next(); token != null; token = input.next())
          if (token.termText().equals("phrase")) {
            inPhrase = true;
            savedStart = token.startOffset();
            savedEnd = token.endOffset();
            return new Token("phrase1", savedStart, savedEnd);
          }
          else if (!token.termText().equals("stop"))
            return token;
      return null;
    }
  }
  
  public static class QPTestAnalyzer extends Analyzer {

    public QPTestAnalyzer() {
    }

    /** Filters LowerCaseTokenizer with StopFilter. */
    public final TokenStream tokenStream(String fieldName, Reader reader) {
      return new QPTestFilter(new LowerCaseTokenizer(reader));
    }
  }
  
   /**
    * initialize this TemplateTester by creating a WebMacro instance
    * and a default Context.
    */
  public void init () throws Exception
  {
  }
  
  public void assertQueryEquals(String query, Analyzer a, String result) 
  throws Exception {
    if (a == null)
      a = new SimpleAnalyzer();
    QueryParser qp = new QueryParser("field", a);
    Query q = qp.parse(query);
    String s = q.toString("field");
    if (!s.equals(result)) {
      System.err.println("Query /" + query + "/ yielded /" + s 
                         + "/, expecting /" + result + "/");
      assert(false);
    }
  }

  public void testSimple() throws Exception {
    assertQueryEquals("term term term", null, "term term term");
    assertQueryEquals("term term1 term2", null, "term term term");
    assertQueryEquals("term 1.0 1 2", null, "term");

    assertQueryEquals("a AND b", null, "+a +b");
    assertQueryEquals("a AND NOT b", null, "+a -b");
    assertQueryEquals("a AND -b", null, "+a -b");
    assertQueryEquals("a AND !b", null, "+a -b");
    assertQueryEquals("a && b", null, "+a +b");
    assertQueryEquals("a&&b", null, "+a +b");
    assertQueryEquals("a && ! b", null, "+a -b");

    assertQueryEquals("a OR b", null, "a b");
    assertQueryEquals("a || b", null, "a b");
    assertQueryEquals("a OR !b", null, "a -b");
    assertQueryEquals("a OR ! b", null, "a -b");
    assertQueryEquals("a OR -b", null, "a -b");

    assertQueryEquals("+term -term term", null, "+term -term term");
    assertQueryEquals("foo:term AND field:anotherTerm", null, 
                      "+foo:term +anotherterm");
    assertQueryEquals("term AND \"phrase phrase\"", null, 
                      "+term +\"phrase phrase\"");

    assertQueryEquals("germ term^2.0", null, "germ term^2.0");
    assertQueryEquals("term^2.0", null, "term^2.0");

    assertQueryEquals("(foo OR bar) AND (baz OR boo)", null, 
                      "+(foo bar) +(baz boo)");
    assertQueryEquals("((a OR b) AND NOT c) OR d", null, 
                      "(+(a b) -c) d");
    assertQueryEquals("+(apple \"steve jobs\") -(foo bar baz)", null, 
                      "+(apple \"steve jobs\") -(foo bar baz)");
    assertQueryEquals("+title:(dog OR cat) -author:\"bob dole\"", null, 
                      "+(title:dog title:cat) -author:\"bob dole\"");
  }

  public void testQPA() throws Exception {
    assertQueryEquals("term term term", qpAnalyzer, "term term term");
    assertQueryEquals("term +stop term", qpAnalyzer, "term term");
    assertQueryEquals("term -stop term", qpAnalyzer, "term term");
    assertQueryEquals("drop AND stop AND roll", qpAnalyzer, "+drop +roll");
    assertQueryEquals("term phrase term", qpAnalyzer, 
                      "term \"phrase1 phrase2\" term");
    assertQueryEquals("term AND NOT phrase term", qpAnalyzer, 
                      "+term -\"phrase1 phrase2\" term");
    assertQueryEquals("stop", qpAnalyzer, "");
  }
}

