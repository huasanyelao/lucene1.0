/* TermInfosWriter.java
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
import com.lucene.store.OutputStream;
import com.lucene.store.Directory;

/** This stores a monotonically increasing set of <Term, TermInfo> pairs in a
  Directory.  A TermInfos can be written once, in order.  */

final public class TermInfosWriter {
  private FieldInfos fieldInfos;
  private OutputStream output;
  private Term lastTerm = new Term("", "");
  private TermInfo lastTi = new TermInfo();
  private int size = 0;
  
  static final int INDEX_INTERVAL = 128;
  private long lastIndexPointer = 0;
  private boolean isIndex = false;

  private TermInfosWriter other = null;

  public TermInfosWriter(Directory directory, String segment, FieldInfos fis)
       throws IOException, SecurityException {
    initialize(directory, segment, fis, false);
    other = new TermInfosWriter(directory, segment, fis, true);
    other.other = this;
  }

  private TermInfosWriter(Directory directory, String segment, FieldInfos fis,
			  boolean isIndex) throws IOException {
    initialize(directory, segment, fis, isIndex);
  }

  private void initialize(Directory directory, String segment, FieldInfos fis,
		     boolean isi) throws IOException {
    fieldInfos = fis;
    isIndex = isi;
    output = directory.createFile(segment + (isIndex ? ".tii" : ".tis"));
    output.writeInt(0);				  // leave space for size
  }

  /** Adds a new <Term, TermInfo> pair to the set.
    Term must be lexicographically greater than all previous Terms added.
    TermInfo pointers must be positive and greater than all previous.*/
  final public void add(Term term, TermInfo ti)
       throws IOException, SecurityException {
    if (!isIndex && term.compareTo(lastTerm) <= 0)
      throw new IOException("term out of order");
    if (ti.freqPointer < lastTi.freqPointer)
      throw new IOException("freqPointer out of order");
    if (ti.proxPointer < lastTi.proxPointer)
      throw new IOException("proxPointer out of order");

    if (!isIndex && size % INDEX_INTERVAL == 0)
      other.add(lastTerm, lastTi);		  // add an index term

    writeTerm(term);				  // write term
    output.writeVInt(ti.docFreq);		  // write doc freq
    output.writeVLong(ti.freqPointer - lastTi.freqPointer); // write pointers
    output.writeVLong(ti.proxPointer - lastTi.proxPointer);

    if (isIndex) {
      output.writeVLong(other.output.getFilePointer() - lastIndexPointer);
      lastIndexPointer = other.output.getFilePointer(); // write pointer
    }

    lastTi.set(ti);
    size++;
  }

  private final void writeTerm(Term term)
       throws IOException {
    int start = stringDifference(lastTerm.text, term.text);
    int length = term.text.length() - start;
    
    output.writeVInt(start);			  // write shared prefix length
    output.writeVInt(length);			  // write delta length
    output.writeChars(term.text, start, length);  // write delta chars

    output.writeVInt(fieldInfos.fieldNumber(term.field)); // write field num

    lastTerm = term;
  }

  private static final int stringDifference(String s1, String s2) {
    int len1 = s1.length();
    int len2 = s2.length();
    int len = len1 < len2 ? len1 : len2;
    for (int i = 0; i < len; i++)
      if (s1.charAt(i) != s2.charAt(i))
	return i;
    return len;
  }

  /** Called to complete TermInfos creation. */
  final public void close() throws IOException, SecurityException {
    output.seek(0);				  // write size at start
    output.writeInt(size);
    output.close();
    
    if (!isIndex)
      other.close();
  }
}
