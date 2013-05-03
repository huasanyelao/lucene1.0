/* WildcardTermEnum.java
 *
 * Copyright (c) 1997, 2000 Dave Kor Kian Wei.
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
package com.lucene.search;

import java.io.IOException;
import com.lucene.index.IndexReader;
import com.lucene.index.Term;
import com.lucene.index.TermEnum;

/** Subclass of FilteredTermEnum for enumerating all terms that match the specified wildcard filter term.

  <p>Term enumerations are always ordered by Term.compareTo().  Each term in
  the enumeration is greater than all that precede it.  */
public class WildcardTermEnum extends FilteredTermEnum {
  Term searchTerm;
  String field = "";
  String text = "";
  String pre = "";
  int preLen = 0;
  boolean fieldMatch = false;
  boolean endEnum = false;
  
  /** Creates new WildcardTermEnum */
  public WildcardTermEnum(IndexReader reader, Term term) throws IOException {
      super(reader, term);
      searchTerm = term;
      field = searchTerm.field();
      text = searchTerm.text();

      int sidx = text.indexOf(WILDCARD_STRING);
      int cidx = text.indexOf(WILDCARD_CHAR);
      int idx = sidx;
      if (idx == -1) idx = cidx;
      else if (cidx >= 0) idx = Math.min(idx, cidx);

      pre = searchTerm.text().substring(0,idx);
      preLen = pre.length();
      text = text.substring(preLen);
      setEnum(reader.terms(new Term(searchTerm.field(), pre)));
  }
  
  final protected boolean termCompare(Term term) {
      if (field == term.field()) {
          String searchText = term.text();
          if (searchText.startsWith(pre)) {
            return wildcardEquals(text, 0, searchText, preLen);
          }
      }
      endEnum = true;
      return false;
  }
  
  final public float difference() {
    return 1.0f;
  }
  
  final public boolean endEnum() {
    return endEnum;
  }
  
  /********************************************
   * String equality with support for wildcards
   ********************************************/
  
  public static final char WILDCARD_STRING = '*';
  public static final char WILDCARD_CHAR = '?';
  
  public static final boolean wildcardEquals(String pattern, int patternIdx, String string, int stringIdx) {
    for ( int p = patternIdx; ; ++p ) {
      for ( int s = stringIdx; ; ++p, ++s ) {
        boolean sEnd = (s >= string.length());
        boolean pEnd = (p >= pattern.length());
        
        if (sEnd && pEnd) return true;
        if (sEnd || pEnd) break;
        if (pattern.charAt(p) == WILDCARD_CHAR) continue;
        if (pattern.charAt(p) == WILDCARD_STRING) {
          int i;
          ++p;
          for (i = string.length(); i >= s; --i)
            if (wildcardEquals(pattern, p, string, i))
              return true;
          break;
        }
        if (pattern.charAt(p) != string.charAt(s)) break;
      }
      return false;
    }
  }
  
  public void close() throws IOException {
      super.close();
      searchTerm = null;
      field = null;
      text = null;
  }
}
