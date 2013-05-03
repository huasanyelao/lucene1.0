/* StopFilter.java
 *
 * Copyright (c) 1997, 2000 Douglass R. Cutting.
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

/** Removes stop words from a token stream. */

public final class StopFilter extends TokenFilter {

  private Hashtable table;

  /** Constructs a filter which removes words from the input
    TokenStream that are named in the array of words. */
  public StopFilter(TokenStream in, String[] stopWords) {
    input = in;
    table = makeStopTable(stopWords);
  }

  /** Constructs a filter which removes words from the input
    TokenStream that are named in the Hashtable. */
  public StopFilter(TokenStream in, Hashtable stopTable) {
    input = in;
    table = stopTable;
  }
  
  /** Builds a Hashtable from an array of stop words, appropriate for passing
    into the StopFilter constructor.  This permits this table construction to
    be cached once when an Analyzer is constructed. */
  public final static Hashtable makeStopTable(String[] stopWords) {
    Hashtable stopTable = new Hashtable(stopWords.length);
    for (int i = 0; i < stopWords.length; i++)
      stopTable.put(stopWords[i], stopWords[i]);
    return stopTable;
  }

  /** Returns the next input Token whose termText() is not a stop word. */
  public final Token next() throws IOException {
    // return the first non-stop word found
    for (Token token = input.next(); token != null; token = input.next())
      if (table.get(token.termText) == null)
	return token;
    // reached EOS -- return null
    return null;
  }
}
