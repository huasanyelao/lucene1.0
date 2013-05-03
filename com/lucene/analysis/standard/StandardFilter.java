/* StandardFilter.java
 *
 * Copyright (c) 2000 Douglass R. Cutting.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package com.lucene.analysis.standard;
import com.lucene.analysis.*;

/** Normalizes tokens extracted with {@link StandardTokenizer}. */

public final class StandardFilter extends TokenFilter
  implements StandardTokenizerConstants  {


  /** Construct filtering <i>in</i>. */
  public StandardFilter(TokenStream in) {
    input = in;
  }

  private static final String APOSTROPHE_TYPE = tokenImage[APOSTROPHE];
  private static final String ACRONYM_TYPE = tokenImage[ACRONYM];
  
  /** Returns the next token in the stream, or null at EOS.
   * <p>Removes <tt>'s</tt> from the end of words.
   * <p>Removes dots from acronyms.
   */
  public final com.lucene.analysis.Token next() throws java.io.IOException {
    com.lucene.analysis.Token t = input.next();

    if (t == null)
      return null;

    String text = t.termText();
    String type = t.type();

    if (type == APOSTROPHE_TYPE &&		  // remove 's
	(text.endsWith("'s") || text.endsWith("'S"))) {
      return new com.lucene.analysis.Token
	(text.substring(0,text.length()-2),
	 t.startOffset(), t.endOffset(), type);

    } else if (type == ACRONYM_TYPE) {		  // remove dots
      StringBuffer trimmed = new StringBuffer();
      for (int i = 0; i < text.length(); i++) {
	char c = text.charAt(i);
	if (c != '.')
	  trimmed.append(c);
      }
      return new com.lucene.analysis.Token
	(trimmed.toString(), t.startOffset(), t.endOffset(), type);

    } else {
      return t;
    }
  }
}
