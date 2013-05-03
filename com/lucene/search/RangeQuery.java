/* GreaterThanQuery.java
 *
 * Copyright (c) 2001 Scott D Ganyo.
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

/** A Query that matches documents within an exclusive range. */
public final class RangeQuery extends Query
{
    private Term lowerTerm;
    private Term upperTerm;
    private boolean inclusive;
    private IndexReader reader;
    private float boost = 1.0f;
    private BooleanQuery query;
    
    /** Constructs a query selecting all terms greater than 
     * <code>lowerTerm</code> but less than <code>upperTerm</code>.
     * There must be at least one term and either term may be null--
     * in which case there is no bound on that side, but if there are 
     * two term, both terms <b>must</b> be for the same field.
     */
    public RangeQuery(Term lowerTerm, Term upperTerm, boolean inclusive)
    {
        if (lowerTerm == null && upperTerm == null)
        {
            throw new IllegalArgumentException("At least one term must be non-null");
        }
        if (lowerTerm != null && upperTerm != null && lowerTerm.field() != upperTerm.field())
        {
            throw new IllegalArgumentException("Both terms must be for the same field");
        }
        this.lowerTerm = lowerTerm;
        this.upperTerm = upperTerm;
        this.inclusive = inclusive;
    }
    
    /** Sets the boost for this term to <code>b</code>.  Documents containing
    this term will (in addition to the normal weightings) have their score
    multiplied by <code>boost</code>. */
    public void setBoost(float boost)
    {
        this.boost = boost;
    }
    
    /** Returns the boost for this term. */
    public float getBoost()
    {
        return boost;
    }
    
    final void prepare(IndexReader reader)
    {
        this.query = null;
        this.reader = reader;
    }
    
    final float sumOfSquaredWeights(Searcher searcher) throws IOException
    {
        return getQuery().sumOfSquaredWeights(searcher);
    }
    
    void normalize(float norm)
    {
        try
        {
            getQuery().normalize(norm);
        } 
        catch (IOException e)
        {
            throw new RuntimeException(e.toString());
        }
    }
    
    Scorer scorer(IndexReader reader) throws IOException
    {
        return getQuery().scorer(reader);
    }
    
    private BooleanQuery getQuery() throws IOException
    {
        if (query == null)
        {
            BooleanQuery q = new BooleanQuery();
            // if we have a lowerTerm, start there. otherwise, start at beginning
            if (lowerTerm == null) lowerTerm = new Term(getField(), "");
            TermEnum termEnum = reader.terms(lowerTerm);
            try
            {
                String lowerText = null;
                String field;
                boolean checkLower = false;
                if (!inclusive) // make adjustments to set to exclusive
                {
                    if (lowerTerm != null)
                    {
                        lowerText = lowerTerm.text();
                        checkLower = true;
                    }
                    if (upperTerm != null)
                    {
                        // set upperTerm to an actual term in the index
                        TermEnum uppEnum = reader.terms(upperTerm);
                        upperTerm = uppEnum.term();
                    }
                }
                String testField = getField();
                do
                {
                    Term term = termEnum.term();
                    if (term != null && term.field() == testField)
                    {
                        if (!checkLower || term.text().compareTo(lowerText) > 0) 
                        {
                            checkLower = false;
                            // if exclusive and this is last term, don't count it and break
                            if (!inclusive && (upperTerm != null) && (upperTerm.compareTo(term) <= 0)) break;
                            TermQuery tq = new TermQuery(term);	  // found a match
                            tq.setBoost(boost);               // set the boost
                            q.add(tq, false, false);		  // add to q
                            // if inclusive just added last term, break out
                            if (inclusive && (upperTerm != null) && (upperTerm.compareTo(term) <= 0)) break;
                        }
                    } 
                    else
                    {
                        break;
                    }
                }
                while (termEnum.next());
            } 
            finally
            {
                termEnum.close();
            }
            query = q;
        }
        return query;
    }
    
    private String getField()
    {
        return (lowerTerm != null ? lowerTerm.field() : upperTerm.field());
    }
    
    /** Prints a user-readable version of this query. */
    public String toString(String field)
    {
        StringBuffer buffer = new StringBuffer();
        if (!getField().equals(field))
        {
            buffer.append(getField());
            buffer.append(":");
        }
        buffer.append(inclusive ? "[" : "{");
        buffer.append(lowerTerm != null ? lowerTerm.text() : "null");
        buffer.append("-");
        buffer.append(upperTerm != null ? upperTerm.text() : "null");
        buffer.append(inclusive ? "]" : "}");
        if (boost != 1.0f)
        {
            buffer.append("^");
            buffer.append(Float.toString(boost));
        }
        return buffer.toString();
    }
}
