/* SegmentMerger.java
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

import java.util.Vector;
import java.io.IOException;

import com.lucene.store.Directory;
import com.lucene.store.OutputStream;
import com.lucene.store.InputStream;
import com.lucene.document.Document;
import com.lucene.util.PriorityQueue;
import com.lucene.util.BitVector;

public final class SegmentMerger {
  private Directory directory;
  private String segment;

  private Vector readers = new Vector();
  private FieldInfos fieldInfos;
  
  public SegmentMerger(Directory dir, String name) {
    directory = dir;
    segment = name;
  }

  public final void add(SegmentReader reader) {
    readers.addElement(reader);
  }

  public final SegmentReader segmentReader(int i) {
    return (SegmentReader)readers.elementAt(i);
  }

  public final void merge() throws IOException {
    try {
      mergeFields();
      mergeTerms();
      mergeNorms();
      
    } finally {
      for (int i = 0; i < readers.size(); i++) {  // close readers
	SegmentReader reader = (SegmentReader)readers.elementAt(i);
	reader.close();
      }
    }
  }

  private final void mergeFields() throws IOException {
    fieldInfos = new FieldInfos();		  // merge field names
    for (int i = 0; i < readers.size(); i++) {
      SegmentReader reader = (SegmentReader)readers.elementAt(i);
      fieldInfos.add(reader.fieldInfos);
    }
    fieldInfos.write(directory, segment + ".fnm");
    
    FieldsWriter fieldsWriter =			  // merge field values
      new FieldsWriter(directory, segment, fieldInfos);
    try {
      for (int i = 0; i < readers.size(); i++) {
	SegmentReader reader = (SegmentReader)readers.elementAt(i);
	BitVector deletedDocs = reader.deletedDocs;
	int maxDoc = reader.maxDoc();
	for (int j = 0; j < maxDoc; j++)
	  if (deletedDocs == null || !deletedDocs.get(j)) // skip deleted docs
	    fieldsWriter.addDocument(reader.document(j));
      }
    } finally {
      fieldsWriter.close();
    }
  }

  private OutputStream freqOutput = null;
  private OutputStream proxOutput = null;
  private TermInfosWriter termInfosWriter = null;
  private SegmentMergeQueue queue = null;

  private final void mergeTerms() throws IOException {
    try {
      freqOutput = directory.createFile(segment + ".frq");
      proxOutput = directory.createFile(segment + ".prx");
      termInfosWriter =
	new TermInfosWriter(directory, segment, fieldInfos);
      
      mergeTermInfos();
      
    } finally {
      if (freqOutput != null) 		freqOutput.close();
      if (proxOutput != null) 		proxOutput.close();
      if (termInfosWriter != null) 	termInfosWriter.close();
      if (queue != null)		queue.close();
    }
  }

  private final void mergeTermInfos() throws IOException {
    queue = new SegmentMergeQueue(readers.size());
    int base = 0;
    for (int i = 0; i < readers.size(); i++) {
      SegmentReader reader = (SegmentReader)readers.elementAt(i);
      SegmentTermEnum termEnum = (SegmentTermEnum)reader.terms();
      SegmentMergeInfo smi = new SegmentMergeInfo(base, termEnum, reader);
      base += reader.numDocs();
      if (smi.next())
	queue.put(smi);				  // initialize queue
      else
	smi.close();
    }

    SegmentMergeInfo[] match = new SegmentMergeInfo[readers.size()];
    
    while (queue.size() > 0) {
      int matchSize = 0;			  // pop matching terms
      match[matchSize++] = (SegmentMergeInfo)queue.pop();
      Term term = match[0].term;
      SegmentMergeInfo top = (SegmentMergeInfo)queue.top();
      
      while (top != null && term.compareTo(top.term) == 0) {
	match[matchSize++] = (SegmentMergeInfo)queue.pop();
	top = (SegmentMergeInfo)queue.top();
      }

      mergeTermInfo(match, matchSize);		  // add new TermInfo
      
      while (matchSize > 0) {
	SegmentMergeInfo smi = match[--matchSize];
	if (smi.next())
	  queue.put(smi);			  // restore queue
	else
	  smi.close();				  // done with a segment
      }
    }
  }

  private final TermInfo termInfo = new TermInfo(); // minimize consing

  private final void mergeTermInfo(SegmentMergeInfo[] smis, int n)
       throws IOException {
    long freqPointer = freqOutput.getFilePointer();
    long proxPointer = proxOutput.getFilePointer();

    int df = appendPostings(smis, n);		  // append posting data

    if (df > 0) {
      // add an entry to the dictionary with pointers to prox and freq files
      termInfo.set(df, freqPointer, proxPointer);
      termInfosWriter.add(smis[0].term, termInfo);
    }
  }
       
  private final int appendPostings(SegmentMergeInfo[] smis, int n)
       throws IOException {
    int lastDoc = 0;
    int df = 0;					  // number of docs w/ term
    for (int i = 0; i < n; i++) {
      SegmentMergeInfo smi = smis[i];
      SegmentTermPositions postings = smi.postings;
      int base = smi.base;
      int[] docMap = smi.docMap;
      smi.termEnum.termInfo(termInfo);
      postings.seek(termInfo);
      while (postings.next()) {
	int doc;
	if (docMap == null)
	  doc = base + postings.doc;		  // no deletions
	else
	  doc = base + docMap[postings.doc];	  // re-map around deletions

	if (doc < lastDoc)
	  throw new IllegalStateException("docs out of order");

	int docCode = (doc - lastDoc) << 1;	  // use low bit to flag freq=1
	lastDoc = doc;

	int freq = postings.freq;
	if (freq == 1) {
	  freqOutput.writeVInt(docCode | 1);	  // write doc & freq=1
	} else {
	  freqOutput.writeVInt(docCode);	  // write doc
	  freqOutput.writeVInt(freq);		  // write frequency in doc
	}
	  
	int lastPosition = 0;			  // write position deltas
	for (int j = 0; j < freq; j++) {
	  int position = postings.nextPosition();
	  proxOutput.writeVInt(position - lastPosition);
	  lastPosition = position;
	}

	df++;
      }
    }
    return df;
  }

  private final void mergeNorms() throws IOException {
    for (int i = 0; i < fieldInfos.size(); i++) {
      FieldInfo fi = fieldInfos.fieldInfo(i);
      if (fi.isIndexed) {
	OutputStream output = directory.createFile(segment + ".f" + i);
	try {
	  for (int j = 0; j < readers.size(); j++) {
	    SegmentReader reader = (SegmentReader)readers.elementAt(j);
	    BitVector deletedDocs = reader.deletedDocs;
	    InputStream input = reader.normStream(fi.name);
            int maxDoc = reader.maxDoc();
	    try {
	      for (int k = 0; k < maxDoc; k++) {
		byte norm = input != null ? input.readByte() : (byte)0;
		if (deletedDocs == null || !deletedDocs.get(k))
		  output.writeByte(norm);
	      }
	    } finally {
	      if (input != null)
		input.close();
	    }
	  }
	} finally {
	  output.close();
	}
      }
    }
  }
}
