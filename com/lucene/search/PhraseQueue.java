/* PhraseQueue.java
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
import com.lucene.util.PriorityQueue;

final class PhraseQueue extends PriorityQueue {
  PhraseQueue(int size) {
    initialize(size);
  }

  protected final boolean lessThan(Object o1, Object o2) {
    PhrasePositions pp1 = (PhrasePositions)o1;
    PhrasePositions pp2 = (PhrasePositions)o2;
    if (pp1.doc == pp2.doc) 
      return pp1.position < pp2.position;
    else
      return pp1.doc < pp2.doc;
  }
}
