/* SegmentsReader.java
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
import java.util.Hashtable;

import com.lucene.store.Directory;
import com.lucene.document.Document;

final class SegmentsReader extends IndexReader {
  protected SegmentReader[] readers;
  protected int[] starts;			  // 1st docno for each segment
  private Hashtable normsCache = new Hashtable();
  private int maxDoc = 0;
  private int numDocs = -1;

  SegmentsReader(SegmentReader[] r) throws IOException {
    readers = r;
    starts = new int[readers.length + 1];	  // build starts array
    for (int i = 0; i < readers.length; i++) {
      starts[i] = maxDoc;
      maxDoc += readers[i].maxDoc();		  // compute maxDocs
    }
    starts[readers.length] = maxDoc;
  }

  public final int numDocs() {
    if (numDocs == -1) {			  // check cache
      int n = 0;				  // cache miss--recompute
      for (int i = 0; i < readers.length; i++)
	n += readers[i].numDocs();		  // sum from readers
      numDocs = n;
    }
    return numDocs;
  }

  public final int maxDoc() {
    return maxDoc;
  }

  public final Document document(int n) throws IOException {
    int i = readerIndex(n);			  // find segment num
    return readers[i].document(n - starts[i]);	  // dispatch to segment reader
  }

  public final boolean isDeleted(int n) {
    int i = readerIndex(n);			  // find segment num
    return readers[i].isDeleted(n - starts[i]);	  // dispatch to segment reader
  }

  public final void delete(int n) throws IOException {
    numDocs = -1;				  // invalidate cache
    int i = readerIndex(n);			  // find segment num
    readers[i].delete(n - starts[i]);		  // dispatch to segment reader
  }

  private final int readerIndex(int n) {	  // find reader for doc n:
    int lo = 0;					  // search starts array
    int hi = readers.length - 1;		  // for first element less
						  // than n, return its index
    while (hi >= lo) {
      int mid = (lo + hi) >> 1;
      int midValue = starts[mid];
      if (n < midValue)
	hi = mid - 1;
      else if (n > midValue)
	lo = mid + 1;
      else
	return mid;
    }
    return hi;
  }

  public final synchronized byte[] norms(String field) throws IOException {
    byte[] bytes = (byte[])normsCache.get(field);
    if (bytes != null)
      return bytes;				  // cache hit

    bytes = new byte[maxDoc()];
    for (int i = 0; i < readers.length; i++)
      readers[i].norms(field, bytes, starts[i]);
    normsCache.put(field, bytes);		  // update cache
    return bytes;
  }

  public final TermEnum terms() throws IOException {
    return new SegmentsTermEnum(readers, starts, null);
  }

  public final TermEnum terms(Term term) throws IOException {
    return new SegmentsTermEnum(readers, starts, term);
  }

  public final int docFreq(Term t) throws IOException {
    int total = 0;				  // sum freqs in segments
    for (int i = 0; i < readers.length; i++)
      total += readers[i].docFreq(t);
    return total;
  }

  public final TermDocs termDocs(Term term) throws IOException {
    return new SegmentsTermDocs(readers, starts, term);
  }

  public final TermPositions termPositions(Term term) throws IOException {
    return new SegmentsTermPositions(readers, starts, term);
  }

  public final void close() throws IOException {
    for (int i = 0; i < readers.length; i++)
      readers[i].close();
  }
}

class SegmentsTermEnum extends TermEnum {
  private SegmentMergeQueue queue;

  private Term term;
  private int docFreq;

  SegmentsTermEnum(SegmentReader[] readers, int[] starts, Term t)
       throws IOException {
    queue = new SegmentMergeQueue(readers.length);
    for (int i = 0; i < readers.length; i++) {
      SegmentReader reader = readers[i];
      SegmentTermEnum termEnum;

      if (t != null) {
	termEnum = (SegmentTermEnum)reader.terms(t);
      } else
	termEnum = (SegmentTermEnum)reader.terms();
      
      SegmentMergeInfo smi = new SegmentMergeInfo(starts[i], termEnum, reader);
      if (t == null ? smi.next() : termEnum.term() != null)
	queue.put(smi);				  // initialize queue
      else
	smi.close();
    }

    if (t != null && queue.size() > 0) {
      SegmentMergeInfo top = (SegmentMergeInfo)queue.top();
      term = top.termEnum.term();
      docFreq = top.termEnum.docFreq();
    }
  }

  public final boolean next() throws IOException {
    SegmentMergeInfo top = (SegmentMergeInfo)queue.top();
    if (top == null) {
      term = null;
      return false;
    }
      
    term = top.term;
    docFreq = 0;
    
    while (top != null && term.compareTo(top.term) == 0) {
      queue.pop();
      docFreq += top.termEnum.docFreq();	  // increment freq
      if (top.next())
	queue.put(top);				  // restore queue
      else
	top.close();				  // done with a segment
      top = (SegmentMergeInfo)queue.top();
    }
    return true;
  }

  public final Term term() {
    return term;
  }

  public final int docFreq() {
    return docFreq;
  }

  public final void close() throws IOException {
    queue.close();
  }
}

class SegmentsTermDocs implements TermDocs {
  protected SegmentReader[] readers;
  protected int[] starts;
  protected Term term;

  protected int base = 0;
  protected int pointer = 0;

  SegmentsTermDocs(SegmentReader[] r, int[] s, Term t) {
    readers = r;
    starts = s;
    term = t;
  }

  protected SegmentTermDocs current;
  
  public final int doc() {
    return base + current.doc;
  }
  public final int freq() {
    return current.freq;
  }

  public final boolean next() throws IOException {
    if (current != null && current.next()) {
      return true;
    } else if (pointer < readers.length) {
      if (current != null)
	current.close();
      base = starts[pointer];
      current = termDocs(readers[pointer++]);
      return next();
    } else
      return false;
  }

  /** Optimized implementation. */
  public final int read(final int[] docs, final int[] freqs)
      throws IOException {
    while (true) {
      while (current == null) {
	if (pointer < readers.length) {		  // try next segment
	  base = starts[pointer];
	  current = termDocs(readers[pointer++]);
	} else {
	  return 0;
	}
      }
      int end = current.read(docs, freqs);
      if (end == 0) {				  // none left in segment
	current.close();
	current = null;
      } else {					  // got some
	final int b = base;			  // adjust doc numbers
	for (int i = 0; i < end; i++)
	  docs[i] += b;
	return end;
      }
    }
  }

  /** As yet unoptimized implementation. */
  public boolean skipTo(int target) throws IOException {
    do {
      if (!next())
	return false;
    } while (target > doc());
    return true;
  }

  protected SegmentTermDocs termDocs(SegmentReader reader)
       throws IOException {
    return (SegmentTermDocs)reader.termDocs(term);
  }

  public final void close() throws IOException {
    if (current != null)
      current.close();
  }
}

class SegmentsTermPositions extends SegmentsTermDocs implements TermPositions {
  SegmentsTermPositions(SegmentReader[] r, int[] s, Term t) {
    super(r,s,t);
  }

  protected final SegmentTermDocs termDocs(SegmentReader reader)
       throws IOException {
    return (SegmentTermDocs)reader.termPositions(term);
  }

  public final int nextPosition() throws IOException {
    return ((SegmentTermPositions)current).nextPosition();
  }
}
