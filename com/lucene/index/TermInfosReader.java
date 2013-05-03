/* TermInfosReader.java
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

import com.lucene.store.Directory;
import com.lucene.store.InputStream;

/** This stores a monotonically increasing set of <Term, TermInfo> pairs in a
 * Directory.  Pairs are accessed either by Term or by ordinal position the
 * set.  */

final public class TermInfosReader {
  private Directory directory;
  private String segment;
  private FieldInfos fieldInfos;

  private SegmentTermEnum termEnum;
  private int size;

  public TermInfosReader(Directory dir, String seg, FieldInfos fis)
       throws IOException {
    directory = dir;
    segment = seg;
    fieldInfos = fis;

    termEnum = new SegmentTermEnum(directory.openFile(segment + ".tis"),
			       fieldInfos, false);
    size = termEnum.size;
    readIndex();
  }

  final public void close() throws IOException {
    if (termEnum != null)
      termEnum.close();
  }

  /** Returns the number of term/value pairs in the set. */
  final public int size() {
    return size;
  }

  Term[] indexTerms = null;
  TermInfo[] indexInfos;
  long[] indexPointers;

  private final void readIndex() throws IOException {
    SegmentTermEnum indexEnum =
      new SegmentTermEnum(directory.openFile(segment + ".tii"),
			  fieldInfos, true);
    try {
      int indexSize = indexEnum.size;

      indexTerms = new Term[indexSize];
      indexInfos = new TermInfo[indexSize];
      indexPointers = new long[indexSize];

      for (int i = 0; indexEnum.next(); i++) {
	indexTerms[i] = indexEnum.term();
	indexInfos[i] = indexEnum.termInfo();
	indexPointers[i] = indexEnum.indexPointer;
      }
    } finally {
      indexEnum.close();
    }
  }

  /** Returns the offset of the greatest index entry which is less than term.*/
  private final int getIndexOffset(Term term) throws IOException {
    int lo = 0;					  // binary search indexTerms[]
    int hi = indexTerms.length - 1;

    while (hi >= lo) {
      int mid = (lo + hi) >> 1;
      int delta = term.compareTo(indexTerms[mid]);
      if (delta < 0)
	hi = mid - 1;
      else if (delta > 0)
	lo = mid + 1;
      else
	return mid;
    }
    return hi;
  }

  private final void seekEnum(int indexOffset) throws IOException {
    termEnum.seek(indexPointers[indexOffset],
	      (indexOffset * TermInfosWriter.INDEX_INTERVAL) - 1,
	      indexTerms[indexOffset], indexInfos[indexOffset]);
  }

  /** Returns the TermInfo for a Term in the set, or null. */
  final public synchronized TermInfo get(Term term) throws IOException {
    if (size == 0) return null;
    
    // optimize sequential access: first try scanning cached termEnum w/o seeking
    if (termEnum.term() != null			  // term is at or past current
	&& ((termEnum.prev != null && term.compareTo(termEnum.prev) > 0)
	    || term.compareTo(termEnum.term()) >= 0)) { 
      int enumOffset = (termEnum.position/TermInfosWriter.INDEX_INTERVAL)+1;
      if (indexTerms.length == enumOffset	  // but before end of block
	  || term.compareTo(indexTerms[enumOffset]) < 0)
	return scanEnum(term);			  // no need to seek
    }
    
    // random-access: must seek
    seekEnum(getIndexOffset(term));
    return scanEnum(term);
  }
  
  /** Scans within block for matching term. */
  private final TermInfo scanEnum(Term term) throws IOException {
    while (term.compareTo(termEnum.term()) > 0 && termEnum.next()) {}
    if (termEnum.term() != null && term.compareTo(termEnum.term()) == 0)
      return termEnum.termInfo();
    else
      return null;
  }

  /** Returns the nth term in the set. */
  final synchronized Term get(int position) throws IOException {
    if (size == 0) return null;

    if (termEnum != null && termEnum.term() != null && position >= termEnum.position &&
	position < (termEnum.position + TermInfosWriter.INDEX_INTERVAL))
      return scanEnum(position);		  // can avoid seek

    seekEnum(position / TermInfosWriter.INDEX_INTERVAL); // must seek
    return scanEnum(position);
  }

  private final Term scanEnum(int position) throws IOException {
    while(termEnum.position < position)
      if (!termEnum.next())
	return null;

    return termEnum.term();
  }

  /** Returns the position of a Term in the set or -1. */
  final synchronized int getPosition(Term term) throws IOException {
    if (size == 0) return -1;

    int indexOffset = getIndexOffset(term);
    seekEnum(indexOffset);

    while(term.compareTo(termEnum.term()) > 0 && termEnum.next()) {}

    if (term.compareTo(termEnum.term()) == 0)
      return termEnum.position;
    else
      return -1;
  }

  /** Returns an enumeration of all the Terms and TermInfos in the set. */
  final public synchronized SegmentTermEnum terms() throws IOException {
    if (termEnum.position != -1)			  // if not at start
      seekEnum(0);				  // reset to start
    return (SegmentTermEnum)termEnum.clone();
  }

  /** Returns an enumeration of terms starting at or after the named term. */
  final public synchronized SegmentTermEnum terms(Term term) throws IOException {
    get(term);					  // seek termEnum to term
    return (SegmentTermEnum)termEnum.clone();
  }


}
