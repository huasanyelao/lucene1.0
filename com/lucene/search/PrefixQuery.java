/* PrefixQuery.java
 *
 * Copyright (c) 2001 Douglass R. Cutting.
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
import com.lucene.index.TermEnum;
import com.lucene.index.TermDocs;
import com.lucene.index.IndexReader;

/** A Query that matches documents containing terms with a specified prefix. */
final public class PrefixQuery extends Query {
  private Term prefix;
  private IndexReader reader;
  private float boost = 1.0f;
  private BooleanQuery query;

  /** Constructs a query for terms starting with <code>prefix</code>. */
  public PrefixQuery(Term prefix) {
    this.prefix = prefix;
    this.reader = reader;
  }

  /** Sets the boost for this term to <code>b</code>.  Documents containing
    this term will (in addition to the normal weightings) have their score
    multiplied by <code>boost</code>. */
  public void setBoost(float boost) {
    this.boost = boost;
  }

  /** Returns the boost for this term. */
  public float getBoost() {
    return boost;
  }
  
  final void prepare(IndexReader reader) {
    this.query = null;
    this.reader = reader;
  }

  final float sumOfSquaredWeights(Searcher searcher)
    throws IOException {
    return getQuery().sumOfSquaredWeights(searcher);
  }

  void normalize(float norm) {
    try {
      getQuery().normalize(norm);
    } catch (IOException e) {
      throw new RuntimeException(e.toString());
    }
  }

  Scorer scorer(IndexReader reader) throws IOException {
    return getQuery().scorer(reader);
  }

  private BooleanQuery getQuery() throws IOException {
    if (query == null) {
      BooleanQuery q = new BooleanQuery();
      TermEnum termEnum = reader.terms(prefix);
      try {
	String prefixText = prefix.text();
	String prefixField = prefix.field();
	do {
	  Term term = termEnum.term();
	  if (term != null &&
	      term.text().startsWith(prefixText) &&
	      term.field() == prefixField) {
	    TermQuery tq = new TermQuery(term);	  // found a match
	    tq.setBoost(boost);			  // set the boost
	    q.add(tq, false, false);		  // add to q
	    //System.out.println("added " + term);
	  } else {
	    break;
	  }
	} while (termEnum.next());
      } finally {
	termEnum.close();
      }
      query = q;
    }
    return query;
  }

  /** Prints a user-readable version of this query. */
  public String toString(String field) {
    StringBuffer buffer = new StringBuffer();
    if (!prefix.field().equals(field)) {
      buffer.append(prefix.field());
      buffer.append(":");
    }
    buffer.append(prefix.text());
    buffer.append('*');
    if (boost != 1.0f) {
      buffer.append("^");
      buffer.append(Float.toString(boost));
    }
    return buffer.toString();
  }
}
