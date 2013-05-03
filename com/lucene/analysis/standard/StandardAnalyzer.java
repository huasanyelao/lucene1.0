/* StandardAnalyzer.java
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
import java.io.Reader;
import java.util.Hashtable;

/** Filters {@link StandardTokenizer} with {@link StandardFilter}, {@link
 * LowerCaseFilter} and {@link StopFilter}. */
public final class StandardAnalyzer extends Analyzer {
  private Hashtable stopTable;

  /** An array containing some common English words that are not usually useful
    for searching. */
  public static final String[] STOP_WORDS = {
    "a", "and", "are", "as", "at", "be", "but", "by",
    "for", "if", "in", "into", "is", "it",
    "no", "not", "of", "on", "or", "s", "such",
    "t", "that", "the", "their", "then", "there", "these",
    "they", "this", "to", "was", "will", "with"
  };

  /** Builds an analyzer. */
  public StandardAnalyzer() {
    this(STOP_WORDS);
  }

  /** Builds an analyzer with the given stop words. */
  public StandardAnalyzer(String[] stopWords) {
    stopTable = StopFilter.makeStopTable(stopWords);
  }

  /** Constructs a {@link StandardTokenizer} filtered by a {@link
   * StandardFilter}, a {@link LowerCaseFilter} and a {@link StopFilter}. */
  public final TokenStream tokenStream(String fieldName, Reader reader) {
    TokenStream result = new StandardTokenizer(reader);
    result = new StandardFilter(result);
    result = new LowerCaseFilter(result);
    result = new StopFilter(result, stopTable);
    return result;
  }
}
