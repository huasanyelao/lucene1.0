/* FilteredTermEnum.java
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

/** Abstract class for enumerating a subset of all terms. 

  <p>Term enumerations are always ordered by Term.compareTo().  Each term in
  the enumeration is greater than all that precede it.  */
public abstract class FilteredTermEnum extends TermEnum {
    private Term currentTerm = null;
    private TermEnum actualEnum = null;
    
    public FilteredTermEnum(IndexReader reader, Term term) throws IOException {}

    /** Equality compare on the term */
    protected abstract boolean termCompare(Term term);
    
    /** Equality measure on the term */
    protected abstract float difference();

    /** Indiciates the end of the enumeration has been reached */
    protected abstract boolean endEnum();
    
    protected void setEnum(TermEnum actualEnum) throws IOException {
        this.actualEnum = actualEnum;
        // Find the first term that matches
        Term term = actualEnum.term();
        if (termCompare(term)) 
            currentTerm = term;
        else next();
    }
    
    /** 
     * Returns the docFreq of the current Term in the enumeration.
     * Initially invalid, valid after next() called for the first time. 
     */
    public int docFreq() {
        if (actualEnum == null) return -1;
        return actualEnum.docFreq();
    }
    
    /** Increments the enumeration to the next element.  True if one exists. */
    public boolean next() throws IOException {
        if (actualEnum == null) return false; // the actual enumerator is not initialized!
        currentTerm = null;
        while (currentTerm == null) {
            if (endEnum()) return false;
            if (actualEnum.next()) {
                Term term = actualEnum.term();
                if (termCompare(term)) {
                    currentTerm = term;
                    return true;
                }
            }
            else return false;
        }
        currentTerm = null;
        return false;
    }
    
    /** Returns the current Term in the enumeration.
     * Initially invalid, valid after next() called for the first time. */
    public Term term() {
        return currentTerm;
    }
    
    /** Closes the enumeration to further activity, freeing resources.  */
    public void close() throws IOException {
        actualEnum.close();
        currentTerm = null;
        actualEnum = null;
    }
}
