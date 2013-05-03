/* SegmentMergeQueue.java
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
package com.lucene.index;
import java.io.IOException;
import com.lucene.util.PriorityQueue;

final class SegmentMergeQueue extends PriorityQueue {
  SegmentMergeQueue(int size) {
    initialize(size);
  }

  protected final boolean lessThan(Object a, Object b) {
    SegmentMergeInfo stiA = (SegmentMergeInfo)a;
    SegmentMergeInfo stiB = (SegmentMergeInfo)b;
    int comparison = stiA.term.compareTo(stiB.term);
    if (comparison == 0)
      return stiA.base < stiB.base; 
    else
      return comparison < 0;
  }

  final void close() throws IOException {
    while (top() != null)
      ((SegmentMergeInfo)pop()).close();
  }

}
