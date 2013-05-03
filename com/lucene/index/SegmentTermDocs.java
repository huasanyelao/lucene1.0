/* SegmentTermDocs.java
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
import com.lucene.store.InputStream;

class SegmentTermDocs implements TermDocs {
  protected SegmentReader parent;
  private InputStream freqStream;
  private int freqCount;
  private BitVector deletedDocs;
  int doc = 0;
  int freq;

  SegmentTermDocs(SegmentReader p) throws IOException {
    parent = p;
    freqStream = parent.getFreqStream();
    deletedDocs = parent.deletedDocs;
  }

  SegmentTermDocs(SegmentReader p, TermInfo ti) throws IOException {
    this(p);
    seek(ti);
  }
  
  void seek(TermInfo ti) throws IOException {
    freqCount = ti.docFreq;
    doc = 0;
    freqStream.seek(ti.freqPointer);
  }
  
  public void close() throws IOException {
    freqStream.close();
  }

  public final int doc() { return doc; }
  public final int freq() { return freq; }

  protected void skippingDoc() throws IOException {
  }

  public boolean next() throws IOException {
    while (true) {
      if (freqCount == 0)
	return false;

      int docCode = freqStream.readVInt();
      doc += docCode >>> 1;			  // shift off low bit
      if ((docCode & 1) != 0)			  // if low bit is set
	freq = 1;				  // freq is one
      else
	freq = freqStream.readVInt();		  // else read freq
 
      freqCount--;
    
      if (deletedDocs == null || !deletedDocs.get(doc))
	break;
      skippingDoc();
    }
    return true;
  }

  /** Optimized implementation. */
  public int read(final int[] docs, final int[] freqs)
      throws IOException {
    final int end = docs.length;
    int i = 0;
    while (i < end && freqCount > 0) {

      // manually inlined call to next() for speed
      final int docCode = freqStream.readVInt();
      doc += docCode >>> 1;			  // shift off low bit
      if ((docCode & 1) != 0)			  // if low bit is set
	freq = 1;				  // freq is one
      else
	freq = freqStream.readVInt();		  // else read freq
      freqCount--;
   
      if (deletedDocs == null || !deletedDocs.get(doc)) {
	docs[i] = doc;
	freqs[i] = freq;
	++i;
      }
     }
    return i;
  }

  /** As yet unoptimized implementation. */
  public boolean skipTo(int target) throws IOException {
    do {
      if (!next())
	return false;
    } while (target > doc);
    return true;
  }
}
