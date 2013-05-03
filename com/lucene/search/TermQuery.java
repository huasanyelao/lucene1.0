/* TermQuery.java
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
import com.lucene.index.Term;
import com.lucene.index.TermDocs;
import com.lucene.index.IndexReader;

/** A Query that matches documents containing a term.
  This may be combined with other terms with a {@link BooleanQuery}.
  */
final public class TermQuery extends Query {
  private Term term;
  private float boost = 1.0f;
  private float idf = 0.0f;
  private float weight = 0.0f;

  /** Constructs a query for the term <code>t</code>. */
  public TermQuery(Term t) {
    term = t;
  }

  /** Sets the boost for this term to <code>b</code>.  Documents containing
    this term will (in addition to the normal weightings) have their score
    multiplied by <code>b</code>. */
  public void setBoost(float b) { boost = b; }
  /** Gets the boost for this term.  Documents containing
    this term will (in addition to the normal weightings) have their score
    multiplied by <code>b</code>.   The boost is 1.0 by default.  */
  public float getBoost() { return boost; }
  
  final float sumOfSquaredWeights(Searcher searcher) throws IOException {
    idf = Similarity.idf(term, searcher);
    weight = idf * boost;
    return weight * weight;			  // square term weights
  }

  final void normalize(float norm) {
    weight *= norm;				  // normalize for query
    weight *= idf;				  // factor from document
  }

  Scorer scorer(IndexReader reader)
       throws IOException {
    TermDocs termDocs = reader.termDocs(term);

    if (termDocs == null)
      return null;
    
    return new TermScorer(termDocs, reader.norms(term.field()), weight);
  }

  /** Prints a user-readable version of this query. */
  public String toString(String field) {
    StringBuffer buffer = new StringBuffer();
    if (!term.field().equals(field)) {
      buffer.append(term.field());
      buffer.append(":");
    }
    buffer.append(term.text());
    if (boost != 1.0f) {
      buffer.append("^");
      buffer.append(Float.toString(boost));
    }
    return buffer.toString();
  }
}
