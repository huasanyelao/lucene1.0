/* BooleanQuery.java
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
import com.lucene.index.IndexReader;

/** A Query that matches documents matching boolean combinations of other
  queries, typically {@link TermQuery}s or {@link PhraseQuery}s.
  */
final public class BooleanQuery extends Query {
  private Vector clauses = new Vector();

  /** Constructs an empty boolean query. */
  public BooleanQuery() {}

  /** Adds a clause to a boolean query.  Clauses may be:
    <ul>
    <li><code>required</code> which means that documents which <i>do not</i>
    match this sub-query will <it>not</it> match the boolean query;
    <li><code>prohibited</code> which means that documents which <i>do</i>
    match this sub-query will <it>not</it> match the boolean query; or
    <li>neither, in which case matched documents are neither prohibited from
    nor required to match the sub-query.
    </ul>
    It is an error to specify a clause as both <code>required</code> and
    <code>prohibited</code>.
    */
  public final void add(Query query, boolean required, boolean prohibited) {
    clauses.addElement(new BooleanClause(query, required, prohibited));
  }

  /** Adds a clause to a boolean query. */
  public final void add(BooleanClause clause) {
    clauses.addElement(clause);
  }

  void prepare(IndexReader reader) {
    for (int i = 0 ; i < clauses.size(); i++) {
      BooleanClause c = (BooleanClause)clauses.elementAt(i);
      c.query.prepare(reader);
    }
  }

  final float sumOfSquaredWeights(Searcher searcher)
       throws IOException {
    float sum = 0.0f;

    for (int i = 0 ; i < clauses.size(); i++) {
      BooleanClause c = (BooleanClause)clauses.elementAt(i);
      if (!c.prohibited)
	sum += c.query.sumOfSquaredWeights(searcher); // sum sub-query weights
    }

    return sum;
  }

  final void normalize(float norm) {
    for (int i = 0 ; i < clauses.size(); i++) {
      BooleanClause c = (BooleanClause)clauses.elementAt(i);
      if (!c.prohibited)
	c.query.normalize(norm);
    }
  }

  final Scorer scorer(IndexReader reader)
       throws IOException {

    if (clauses.size() == 1) {			  // optimize 1-term queries
      BooleanClause c = (BooleanClause)clauses.elementAt(0);
      if (!c.prohibited)			  // just return term scorer
	return c.query.scorer(reader);
    }

    BooleanScorer result = new BooleanScorer();

    int theMask = 1, thisMask;
    for (int i = 0 ; i < clauses.size(); i++) {
      BooleanClause c = (BooleanClause)clauses.elementAt(i);
      if (c.required || c.prohibited) {
	thisMask = theMask;
	theMask = theMask << 1;
      } else
	thisMask = 0;
      
      Scorer subScorer = c.query.scorer(reader);
      if (subScorer != null)
	result.add(subScorer, c.required, c.prohibited);
      else if (c.required)
	return null;
    }
    if (theMask == 0)
      throw new IndexOutOfBoundsException
	("More than 32 required/prohibited clauses in query.");

    return result;
  }

  /** Prints a user-readable version of this query. */
  public String toString(String field) {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0 ; i < clauses.size(); i++) {
      BooleanClause c = (BooleanClause)clauses.elementAt(i);
      if (c.prohibited)
	buffer.append("-");
      else if (c.required)
	buffer.append("+");

      Query subQuery = c.query;
      if (subQuery instanceof BooleanQuery) {	  // wrap sub-bools in parens
	BooleanQuery bq = (BooleanQuery)subQuery;
	buffer.append("(");
	buffer.append(c.query.toString(field));
	buffer.append(")");
      } else
	buffer.append(c.query.toString(field));

      if (i != clauses.size()-1)
	buffer.append(" ");
    }
    return buffer.toString();
  }

}
