/* SegmentTermPositions.java
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

import com.lucene.store.InputStream;

final class SegmentTermPositions
extends SegmentTermDocs implements TermPositions {
  private InputStream proxStream;
  private int proxCount;
  private int position;
  
  SegmentTermPositions(SegmentReader p) throws IOException {
    super(p);
    proxStream = parent.getProxStream();
  }

  SegmentTermPositions(SegmentReader p, TermInfo ti)
       throws IOException {
    this(p);
    seek(ti);
  }

  final void seek(TermInfo ti) throws IOException {
    super.seek(ti);
    proxStream.seek(ti.proxPointer);
  }

  public final void close() throws IOException {
    super.close();
    proxStream.close();
  }

  public final int nextPosition() throws IOException {
    proxCount--;
    return position += proxStream.readVInt();
  }

  protected final void skippingDoc() throws IOException {
    for (int f = freq; f > 0; f--)		  // skip all positions
      proxStream.readVInt();
  }

  public final boolean next() throws IOException {
    for (int f = proxCount; f > 0; f--)		  // skip unread positions
      proxStream.readVInt();

    if (super.next()) {				  // run super
      proxCount = freq;				  // note frequency
      position = 0;				  // reset position
      return true;
    }
    return false;
  }

  public final int read(final int[] docs, final int[] freqs)
      throws IOException {
    throw new RuntimeException();
  }
}
