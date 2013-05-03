/* HitCollector.java
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

/** Lower-level search API.
 * @see IndexSearcher#search(Query,HitCollector)
 */
public abstract class HitCollector {
  /** Called once for every non-zero scoring document, with the document number
   * and its score.
   *
   * <P>If, for example, an application wished to collect all of the hits for a
   * query in a BitSet, then it might:<pre>
   *   Searcher = new IndexSearcher(indexReader);
   *   final BitSet bits = new BitSet(indexReader.maxDoc());
   *   searcher.search(query, new HitCollector() {
   *       public void collect(int doc, float score) {
   *         bits.set(doc);
   *       }
   *     });
   * </pre>
   */
  public abstract void collect(int doc, float score);
}
