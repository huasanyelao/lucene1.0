/* WildcardQuery.java
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

import com.lucene.index.IndexReader;
import com.lucene.index.Term;
import java.io.IOException;

/** Implements the wildcard search query */
final public class WildcardQuery extends MultiTermQuery {
    private Term wildcardTerm;

    public WildcardQuery(Term term) {
        super(term);
        wildcardTerm = term;
    }

    final void prepare(IndexReader reader) {
        try {
            setEnum(new WildcardTermEnum(reader, wildcardTerm));
        } catch (IOException e) {}
    }
    
}
