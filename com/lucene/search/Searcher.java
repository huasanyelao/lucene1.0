/* Searcher.java
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
import com.lucene.document.Document;
import com.lucene.index.Term;

/** The abstract base class for search implementations.
  <p>Subclasses implement search over a single index, over multiple indices,
  and over indices on remote servers.
 */
public abstract class Searcher {

  /** Returns the documents matching <code>query</code>. */
  public final Hits search(Query query) throws IOException {
    return search(query, null);
  }

  /** Returns the documents matching <code>query</code> and
    <code>filter</code>. */
  public final Hits search(Query query, Filter filter) throws IOException {
    return new Hits(this, query, filter);
  }

  /** Frees resources associated with this Searcher. */
  abstract public void close() throws IOException;

  abstract int docFreq(Term term) throws IOException;
  abstract int maxDoc() throws IOException;
  abstract TopDocs search(Query query, Filter filter, int n)
       throws IOException;
  abstract Document doc(int i) throws IOException;

}
