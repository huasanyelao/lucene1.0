/* Query.java
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
package com.lucene.search;

import java.io.IOException;
import java.util.Hashtable;
import com.lucene.document.Document;
import com.lucene.index.IndexReader;

/** The abstract base class for queries.
  <p>Instantiable subclasses are:
  <ul>
  <li> {@link TermQuery}
  <li> {@link PhraseQuery}
  <li> {@link BooleanQuery}
  </ul>
  <p>A parser for queries is contained in:
  <ul>
  <li><a href="doc/lucene.queryParser.QueryParser.html">QueryParser</a>
  </ul>
  */
abstract public class Query {

  // query weighting
  abstract float sumOfSquaredWeights(Searcher searcher) throws IOException;
  abstract void normalize(float norm);

  // query evaluation
  abstract Scorer scorer(IndexReader reader) throws IOException;

  void prepare(IndexReader reader) {}

  static Scorer scorer(Query query, Searcher searcher, IndexReader reader)
    throws IOException {
    query.prepare(reader);
    float sum = query.sumOfSquaredWeights(searcher);
    float norm = 1.0f / (float)Math.sqrt(sum);
    query.normalize(norm);
    return query.scorer(reader);
  }

  /** Prints a query to a string, with <code>field</code> as the default field
    for terms.
    <p>The representation used is one that is readable by
    <a href="doc/lucene.queryParser.QueryParser.html">QueryParser</a>
    (although, if the query was created by the parser, the printed
    representation may not be exactly what was parsed). */
  abstract public String toString(String field);
}
