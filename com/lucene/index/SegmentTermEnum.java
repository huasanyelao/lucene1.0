/* SegmentTermEnum.java
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

final public class SegmentTermEnum extends TermEnum implements Cloneable {
  private InputStream input;
  private FieldInfos fieldInfos;
  int size;
  int position = -1;

  private Term term = new Term("", "");
  private TermInfo termInfo = new TermInfo();

  boolean isIndex = false;
  long indexPointer = 0;
  Term prev;

  private char[] buffer = {};

  public SegmentTermEnum(InputStream i, FieldInfos fis, boolean isi)
       throws IOException {
    input = i;
    fieldInfos = fis; 
    size = input.readInt();
    isIndex = isi;
  }
  
  protected Object clone() {
    SegmentTermEnum clone = null;
    try {
      clone = (SegmentTermEnum)super.clone();
    } catch (CloneNotSupportedException e) {}

    clone.input = (InputStream)input.clone();
    clone.termInfo = new TermInfo(termInfo);
    clone.growBuffer(term.text.length());

    return clone;
  }

  final public void seek(long pointer, int p, Term t, TermInfo ti)
       throws IOException {
    input.seek(pointer);
    position = p;
    term = t;
    prev = null;
    termInfo.set(ti);
    growBuffer(term.text.length());		  // copy term text into buffer
  }

  /** Increments the enumeration to the next element.  True if one exists.*/
  public final boolean next() throws IOException {
    if (position++ >= size-1) {
      term = null;
      return false;
    }

    prev = term;
    term = readTerm();

    termInfo.docFreq = input.readVInt();	  // read doc freq
    termInfo.freqPointer += input.readVLong();	  // read freq pointer
    termInfo.proxPointer += input.readVLong();	  // read prox pointer
    
    if (isIndex)
      indexPointer += input.readVLong();	  // read index pointer

    return true;
  }

  private final Term readTerm() throws IOException {
    int start = input.readVInt();
    int length = input.readVInt();
    int totalLength = start + length;
    if (buffer.length < totalLength)
      growBuffer(totalLength);
    
    input.readChars(buffer, start, length);
    return new Term(fieldInfos.fieldName(input.readVInt()),
		    new String(buffer, 0, totalLength), false);
  }

  private final void growBuffer(int length) {
    buffer = new char[length];
    for (int i = 0; i < term.text.length(); i++)  // copy contents
      buffer[i] = term.text.charAt(i);
  }

  /** Returns the current Term in the enumeration.
    Initially invalid, valid after next() called for the first time.*/
  public final Term term() {
    return term;
  }

  /** Returns the current TermInfo in the enumeration.
    Initially invalid, valid after next() called for the first time.*/
  final public TermInfo termInfo() {
    return new TermInfo(termInfo);
  }

  /** Sets the argument to the current TermInfo in the enumeration.
    Initially invalid, valid after next() called for the first time.*/
  final public void termInfo(TermInfo ti) {
    ti.set(termInfo);
  }

  /** Returns the docFreq from the current TermInfo in the enumeration.
    Initially invalid, valid after next() called for the first time.*/
  public final int docFreq() {
    return termInfo.docFreq;
  }

  /* Returns the freqPointer from the current TermInfo in the enumeration.
    Initially invalid, valid after next() called for the first time.*/
  final long freqPointer() {
    return termInfo.freqPointer;
  }

  /* Returns the proxPointer from the current TermInfo in the enumeration.
    Initially invalid, valid after next() called for the first time.*/
  final long proxPointer() {
    return termInfo.proxPointer;
  }

  /** Closes the enumeration to further activity, freeing resources. */
  public final void close() throws IOException {
    input.close();
  }
}
