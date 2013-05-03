/* PorterStemFilter.java
 *
 * Copyright (c) 2000 Brian Goetz.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */
package com.lucene.analysis;

import java.io.IOException;
import java.util.Hashtable;

/** Transforms the token stream as per the Porter stemming algorithm. 
    Note: the input to the stemming filter must already be in lower case, 
    so you will need to use LowerCaseFilter or LowerCaseTokenizer farther
    down the Tokenizer chain in order for this to work properly!  

    To use this filter with other analyzers, you'll want to write an 
    Analyzer class that sets up the TokenStream chain as you want it.  
    To use this with LowerCaseTokenizer, for example, you'd write an
    analyzer like this:

    class MyAnalyzer extends Analyzer {
      public final TokenStream tokenStream(String fieldName, Reader reader) {
        return new PorterStemFilter(new LowerCaseTokenizer(reader));
      }
    } 

*/

public final class PorterStemFilter extends TokenFilter {
  private PorterStemmer stemmer;

  public PorterStemFilter(TokenStream in) {
    stemmer = new PorterStemmer();
    input = in;
  }

  /** Returns the next input Token, after being stemmed */
  public final Token next() throws IOException {
    Token token = input.next();
    if (token == null)
      return null;
    else {
      String s = stemmer.stem(token.termText);
      if (s != token.termText) // Yes, I mean object reference comparison here
        token.termText = s;
      return token;
    }
  }
}
