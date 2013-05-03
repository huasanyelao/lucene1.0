/* BooleanClause.java
 *
 * Copyright (c) 1998, 2000 Douglass R. Cutting.
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

/** A clause in a BooleanQuery. */
public final class BooleanClause {
  /** The query whose matching documents are combined by the boolean query. */
  public Query query;
  /** If true, documents documents which <i>do not</i>
    match this sub-query will <it>not</it> match the boolean query. */
  public boolean required = false;
  /** If true, documents documents which <i>do</i>
    match this sub-query will <it>not</it> match the boolean query. */
  public boolean prohibited = false;
  
  /** Constructs a BooleanClause with query <code>q</code>, required
    <code>r</code> and prohibited <code>p</code>. */ 
  public BooleanClause(Query q, boolean r, boolean p) {
    query = q;
    required = r;
    prohibited = p;
  }
}
