/* SegmentMergeInfo.java
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
import com.lucene.util.BitVector;

final class SegmentMergeInfo {
  Term term;
  int base;
  SegmentTermEnum termEnum;
  SegmentReader reader;
  SegmentTermPositions postings;
  int[] docMap = null;				  // maps around deleted docs

  SegmentMergeInfo(int b, SegmentTermEnum te, SegmentReader r)
    throws IOException {
    base = b;
    reader = r;
    termEnum = te;
    term = te.term();
    postings = new SegmentTermPositions(r);

    if (reader.deletedDocs != null) {
      // build array which maps document numbers around deletions 
      BitVector deletedDocs = reader.deletedDocs;
      int maxDoc = reader.maxDoc();
      docMap = new int[maxDoc];
      int j = 0;
      for (int i = 0; i < maxDoc; i++) {
	if (deletedDocs.get(i))
	  docMap[i] = -1;
	else
	  docMap[i] = j++;
      }
    }
  }

  final boolean next() throws IOException {
    if (termEnum.next()) {
      term = termEnum.term();
      return true;
    } else {
      term = null;
      return false;
    }
  }

  final void close() throws IOException {
    termEnum.close();
    postings.close();
  }
}

