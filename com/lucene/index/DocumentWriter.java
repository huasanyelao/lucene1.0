/* DocumentWriter.java
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
import java.io.Reader;
import java.io.StringReader;
import java.util.Hashtable;
import java.util.Enumeration;

import com.lucene.document.Document;
import com.lucene.document.Field;
import com.lucene.analysis.Analyzer;
import com.lucene.analysis.TokenStream;
import com.lucene.analysis.Token;
import com.lucene.store.Directory;
import com.lucene.store.OutputStream;
import com.lucene.search.Similarity;

public final class DocumentWriter {
  private Analyzer analyzer;
  private Directory directory;
  private FieldInfos fieldInfos;
  private int maxFieldLength;
  
  public DocumentWriter(Directory d, Analyzer a, int mfl) {
    directory = d;
    analyzer = a;
    maxFieldLength = mfl;
  }
  
  public final void addDocument(String segment, Document doc)
       throws IOException {
    // write field names
    fieldInfos = new FieldInfos();
    fieldInfos.add(doc);
    fieldInfos.write(directory, segment + ".fnm");

    // write field values
    FieldsWriter fieldsWriter =
      new FieldsWriter(directory, segment, fieldInfos);
    try {
      fieldsWriter.addDocument(doc);
    } finally {
      fieldsWriter.close();
    }
      
    // invert doc into postingTable
    postingTable.clear();			  // clear postingTable
    fieldLengths = new int[fieldInfos.size()];	  // init fieldLengths
    invertDocument(doc);

    // sort postingTable into an array
    Posting[] postings = sortPostingTable();

    /*
    for (int i = 0; i < postings.length; i++) {
      Posting posting = postings[i];
      System.out.print(posting.term);
      System.out.print(" freq=" + posting.freq);
      System.out.print(" pos=");
      System.out.print(posting.positions[0]);
      for (int j = 1; j < posting.freq; j++)
	System.out.print("," + posting.positions[j]);
      System.out.println("");
    }
    */

    // write postings
    writePostings(postings, segment);

    // write norms of indexed fields
    writeNorms(doc, segment);
    
  }

  // Keys are Terms, values are Postings.
  // Used to buffer a document before it is written to the index.
  private final Hashtable postingTable = new Hashtable();
  private int[] fieldLengths;

  // Tokenizes the fields of a document into Postings.
  private final void invertDocument(Document doc)
       throws IOException {
    Enumeration fields  = doc.fields();
    while (fields.hasMoreElements()) {
      Field field = (Field)fields.nextElement();
      String fieldName = field.name();
      int fieldNumber = fieldInfos.fieldNumber(fieldName);

      int position = fieldLengths[fieldNumber];	  // position in field

      if (field.isIndexed()) {
	if (!field.isTokenized()) {		  // un-tokenized field
	  addPosition(fieldName, field.stringValue(), position++);
	} else {
	  Reader reader;			  // find or make Reader
	  if (field.readerValue() != null)
	    reader = field.readerValue();
	  else if (field.stringValue() != null)
	    reader = new StringReader(field.stringValue());
	  else
	    throw new IllegalArgumentException
	      ("field must have either String or Reader value");

	  // Tokenize field and add to postingTable
	  TokenStream stream = analyzer.tokenStream(fieldName, reader);
	  try {
	    for (Token t = stream.next(); t != null; t = stream.next()) {
	      addPosition(fieldName, t.termText(), position++);
	      if (position > maxFieldLength) break;
	    }
	  } finally {
	    stream.close();
	  }
	}

	fieldLengths[fieldNumber] = position;	  // save field length
      }
    }
  }

  private final Term termBuffer = new Term("", ""); // avoid consing

  private final void addPosition(String field, String text, int position) {
    termBuffer.set(field, text);
    Posting ti = (Posting)postingTable.get(termBuffer);
    if (ti != null) {				  // word seen before
      int freq = ti.freq;
      if (ti.positions.length == freq) {	  // positions array is full
	int[] newPositions = new int[freq * 2];	  // double size
	int[] positions = ti.positions;
	for (int i = 0; i < freq; i++)		  // copy old positions to new
	  newPositions[i] = positions[i];
	ti.positions = newPositions;
      }
      ti.positions[freq] = position;		  // add new position
      ti.freq = freq + 1;			  // update frequency
    }
    else {					  // word not seen before
      Term term = new Term(field, text, false);
      postingTable.put(term, new Posting(term, position));
    }
  }

  private final Posting[] sortPostingTable() {
    // copy postingTable into an array
    Posting[] array = new Posting[postingTable.size()];
    Enumeration postings = postingTable.elements();
    for (int i = 0; postings.hasMoreElements(); i++)
      array[i] = (Posting)postings.nextElement();

    // sort the array
    quickSort(array, 0, array.length - 1);

    return array;
  }

  static private final void quickSort(Posting[] postings, int lo, int hi) {
    if(lo >= hi)
      return;

    int mid = (lo + hi) / 2;

    if(postings[lo].term.compareTo(postings[mid].term) > 0) {
      Posting tmp = postings[lo];
      postings[lo] = postings[mid];
      postings[mid] = tmp;
    }

    if(postings[mid].term.compareTo(postings[hi].term) > 0) {
      Posting tmp = postings[mid];
      postings[mid] = postings[hi];
      postings[hi] = tmp;
      
      if(postings[lo].term.compareTo(postings[mid].term) > 0) {
	Posting tmp2 = postings[lo];
        postings[lo] = postings[mid];
        postings[mid] = tmp2;
      }
    }

    int left = lo + 1;
    int right = hi - 1;

    if (left >= right)
      return; 

    Term partition = postings[mid].term;
    
    for( ;; ) {
      while(postings[right].term.compareTo(partition) > 0)
	--right;
      
      while(left < right && postings[left].term.compareTo(partition) <= 0)
	++left;
      
      if(left < right) {
        Posting tmp = postings[left];
        postings[left] = postings[right];
        postings[right] = tmp;
        --right;
      } else {
	break;
      }
    }
    
    quickSort(postings, lo, left);
    quickSort(postings, left + 1, hi);
  }

  private final void writePostings(Posting[] postings, String segment)
       throws IOException {
    OutputStream freq = null, prox = null;
    TermInfosWriter tis = null;

    try {
      freq = directory.createFile(segment + ".frq");
      prox = directory.createFile(segment + ".prx");
      tis = new TermInfosWriter(directory, segment, fieldInfos);
      TermInfo ti = new TermInfo();

      for (int i = 0; i < postings.length; i++) {
	Posting posting = postings[i];

	// add an entry to the dictionary with pointers to prox and freq files
	ti.set(1, freq.getFilePointer(), prox.getFilePointer());
	tis.add(posting.term, ti);
	
	// add an entry to the freq file
	int f = posting.freq;
	if (f == 1)				  // optimize freq=1
	  freq.writeVInt(1);			  // set low bit of doc num.
	else {
	  freq.writeVInt(0);			  // the document number
	  freq.writeVInt(f);			  // frequency in doc
	}
	
	int lastPosition = 0;			  // write positions
	int[] positions = posting.positions;
	for (int j = 0; j < f; j++) {		  // use delta-encoding
	  int position = positions[j];
	  prox.writeVInt(position - lastPosition);
	  lastPosition = position;
	}
      }
    }
    finally {
      if (freq != null) freq.close();
      if (prox != null) prox.close();
      if (tis  != null)  tis.close();
    }
  }

  private final void writeNorms(Document doc, String segment)
       throws IOException {
    Enumeration fields  = doc.fields();
    while (fields.hasMoreElements()) {
      Field field = (Field)fields.nextElement();
      if (field.isIndexed()) {
	int fieldNumber = fieldInfos.fieldNumber(field.name());
	OutputStream norm = directory.createFile(segment + ".f" + fieldNumber);
	try {
	  norm.writeByte(Similarity.norm(fieldLengths[fieldNumber]));
	} finally {
	  norm.close();
	}
      }
    }
  }
}

final class Posting {				  // info about a Term in a doc
  Term term;					  // the Term
  int freq;					  // its frequency in doc
  int[] positions;				  // positions it occurs at
  
  Posting(Term t, int position) {
    term = t;
    freq = 1;
    positions = new int[1];
    positions[0] = position;
  }
}
