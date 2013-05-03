/* StopAnalyzer.java
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
import java.io.Reader;
import java.util.Hashtable;

/** Filters LetterTokenizer with LowerCaseFilter and StopFilter. */

public final class StopAnalyzer extends Analyzer {
  private Hashtable stopTable;

  /** An array containing some common English words that are not usually useful
    for searching. */
  public static final String[] ENGLISH_STOP_WORDS = {
    "a", "and", "are", "as", "at", "be", "but", "by",
    "for", "if", "in", "into", "is", "it",
    "no", "not", "of", "on", "or", "s", "such",
    "t", "that", "the", "their", "then", "there", "these",
    "they", "this", "to", "was", "will", "with"
  };

  /** Builds an analyzer which removes words in ENGLISH_STOP_WORDS. */
  public StopAnalyzer() {
    stopTable = StopFilter.makeStopTable(ENGLISH_STOP_WORDS);
  }

  /** Builds an analyzer which removes words in the provided array. */
  public StopAnalyzer(String[] stopWords) {
    stopTable = StopFilter.makeStopTable(stopWords);
  }

  /** Filters LowerCaseTokenizer with StopFilter. */
  public final TokenStream tokenStream(String fieldName, Reader reader) {
    return new StopFilter(new LowerCaseTokenizer(reader), stopTable);
  }
}

