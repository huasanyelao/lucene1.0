/* Filter.java
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

import java.util.BitSet;
import java.io.IOException;
import com.lucene.index.IndexReader;

/** Abstract base class providing a mechanism to restrict searches to a subset
 of an index. */
abstract public class Filter {
  /** Returns a BitSet with true for documents which should be permitted in
    search results, and false for those that should not. */
  abstract public BitSet bits(IndexReader reader) throws IOException;
}
