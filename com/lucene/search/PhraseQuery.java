/* PhraseQuery.java
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
import java.util.Vector;

import com.lucene.index.Term;
import com.lucene.index.TermDocs;
import com.lucene.index.TermPositions;
import com.lucene.index.IndexReader;

/** A Query that matches documents containing a particular sequence of terms.
  This may be combined with other terms with a {@link BooleanQuery}.
  */
final public class PhraseQuery extends Query {
  private String field;
  private Vector terms = new Vector();
  private float idf = 0.0f;
  private float weight = 0.0f;

  private float boost = 1.0f;
  private int slop = 0;


  /** Constructs an empty phrase query. */
  public PhraseQuery() {
  }

  /** Sets the boost for this term to <code>b</code>.  Documents containing
    this term will (in addition to the normal weightings) have their score
    multiplied by <code>b</code>. */
  public final void setBoost(float b) { boost = b; }
  /** Gets the boost for this term.  Documents containing
    this term will (in addition to the normal weightings) have their score
    multiplied by <code>b</code>.   The boost is 1.0 by default.  */
  public final float getBoost() { return boost; }
  
  /** Sets the number of other words permitted between words in query phrase.
    If zero, then this is an exact phrase search.  For larger values this works
    like a <code>WITHIN</code> or <code>NEAR</code> operator.

    <p>The slop is in fact an edit-distance, where the units correspond to
    moves of terms in the query phrase out of position.  For example, to switch
    the order of two words requires two moves (the first move places the words
    atop one another), so to permit re-orderings of phrases, the slop must be
    at least two.

    <p>More exact matches are scored higher than sloppier matches, thus search
    results are sorted by exactness.

    <p>The slop is zero by default, requiring exact matches.*/
  public final void setSlop(int s) { slop = s; }
  /** Returns the slop.  See setSlop(). */
  public final int getSlop() { return slop; }

  /** Adds a term to the end of the query phrase. */
  public final void add(Term term) {
    if (terms.size() == 0)
      field = term.field();
    else if (term.field() != field)
      throw new IllegalArgumentException
	("All phrase terms must be in the same field: " + term);

    terms.addElement(term);
  }

  final float sumOfSquaredWeights(Searcher searcher) throws IOException {
    for (int i = 0; i < terms.size(); i++)	  // sum term IDFs
      idf += Similarity.idf((Term)terms.elementAt(i), searcher);

    weight = idf * boost;
    return weight * weight;			  // square term weights
  }

  final void normalize(float norm) {
    weight *= norm;				  // normalize for query
    weight *= idf;				  // factor from document
  }

  final Scorer scorer(IndexReader reader) throws IOException {
    if (terms.size() == 0)			  // optimize zero-term case
      return null;
    if (terms.size() == 1) {			  // optimize one-term case
      Term term = (Term)terms.elementAt(0);
      TermDocs docs = reader.termDocs(term);
      if (docs == null)
	return null;
      return new TermScorer(docs, reader.norms(term.field()), weight);
    }

    TermPositions[] tps = new TermPositions[terms.size()];
    for (int i = 0; i < terms.size(); i++) {
      TermPositions p = reader.termPositions((Term)terms.elementAt(i));
      if (p == null)
	return null;
      tps[i] = p;
    }

    if (slop == 0)				  // optimize exact case
      return new ExactPhraseScorer(tps, reader.norms(field), weight);
    else
      return
	new SloppyPhraseScorer(tps, slop, reader.norms(field), weight);

  }

  /** Prints a user-readable version of this query. */
  public final String toString(String f) {
    StringBuffer buffer = new StringBuffer();
    if (!field.equals(f)) {
      buffer.append(field);
      buffer.append(":");
    }

    buffer.append("\"");
    for (int i = 0; i < terms.size(); i++) {
      buffer.append(((Term)terms.elementAt(i)).text());
      if (i != terms.size()-1)
	buffer.append(" ");
    }
    buffer.append("\"");

    if (boost != 1.0f) {
      buffer.append("^");
      buffer.append(Float.toString(boost));
    }

    return buffer.toString();
  }
}
