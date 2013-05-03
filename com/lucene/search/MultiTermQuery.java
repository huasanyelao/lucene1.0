/* MultiTermQuery.java
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
import java.util.Vector;
import com.lucene.index.IndexReader;
import com.lucene.index.Term;
import com.lucene.index.TermEnum;

/** A Query that matches documents containing a subset of terms provided by a 
 FilteredTermEnum enumeration. MultiTermQuery is not designed to be used by 
 itself. The reason being that it is not intialized with a FilteredTermEnum 
 enumeration. A FilteredTermEnum enumeration needs to be provided. For example,
 WildcardQuery and FuzzyQuery extends MultiTermQuery to provide WildcardTermEnum
 and FuzzyTermEnum respectively. */
public class MultiTermQuery extends Query {
    private Term term;
    private FilteredTermEnum termEnum;
    private IndexReader reader;
    private float boost = 1.0f;
    private BooleanQuery query;
    
    /** Enable or disable lucene style toString(field) format */
    private static boolean LUCENE_STYLE_TOSTRING = false;
    
    /** Constructs a query for terms matching <code>term</code>. */
    public MultiTermQuery(Term term) {
        this.term = term;
        this.query = query;
    }
    
    /** Set the TermEnum to be used */
    protected void setEnum(FilteredTermEnum termEnum) {
        this.termEnum = termEnum;
    }
    
    /** Sets the boost for this term to <code>b</code>.  Documents containing
     * this term will (in addition to the normal weightings) have their score
     * multiplied by <code>boost</code>. */
    final public void setBoost(float boost) {
        this.boost = boost;
    }
    
    /** Returns the boost for this term. */
    final public float getBoost() {
        return boost;
    }
    
    final float sumOfSquaredWeights(Searcher searcher) throws IOException {
        return getQuery().sumOfSquaredWeights(searcher);
    }
    
    final void normalize(float norm) {
        try {
            getQuery().normalize(norm);
        } catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
    }
    
    final Scorer scorer(IndexReader reader) throws IOException {
        return getQuery().scorer(reader);
    }
    
    final private BooleanQuery getQuery() throws IOException {
        if (query == null) {
            BooleanQuery q = new BooleanQuery();
            try {
                do {
                    Term t = termEnum.term();
                    if (t != null) {
                        TermQuery tq = new TermQuery(t);	// found a match
                        tq.setBoost(boost * termEnum.difference()); // set the boost
                        q.add(tq, false, false);		// add to q
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
        if (!LUCENE_STYLE_TOSTRING) {
            Query q = null;
            try {
                q = getQuery();
            } catch (Exception e) {}
            if (q != null) {
                return "(" + q.toString(field) + ")";
            }
        }
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
