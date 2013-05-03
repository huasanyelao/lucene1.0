/* FieldInfos.java
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

import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.io.IOException;

import com.lucene.document.Document;
import com.lucene.document.Field;

import com.lucene.store.Directory;
import com.lucene.store.OutputStream;
import com.lucene.store.InputStream;

 final public class FieldInfos {
  private Vector byNumber = new Vector();
  private Hashtable byName = new Hashtable();

  public FieldInfos() {
    add("", false);
  }

  public FieldInfos(Directory d, String name) throws IOException {
    InputStream input = d.openFile(name);
    try {
      read(input);
    } finally {
      input.close();
    }
  }

  /** Adds field info for a Document. */
  public final void add(Document doc) {
    Enumeration fields  = doc.fields();
    while (fields.hasMoreElements()) {
      Field field = (Field)fields.nextElement();
      add(field.name(), field.isIndexed());
    }
  }

  /** Merges in information from another FieldInfos. */
  public final void add(FieldInfos other) {
    for (int i = 0; i < other.size(); i++) {
      FieldInfo fi = other.fieldInfo(i);
      add(fi.name, fi.isIndexed);
    }
  }

  public final void add(String name, boolean isIndexed) {
    FieldInfo fi = fieldInfo(name);
    if (fi == null)
      addInternal(name, isIndexed);
    else if (fi.isIndexed != isIndexed)
      throw new IllegalStateException("field " + name +
				      (fi.isIndexed ? " must" : " cannot") +
				      " be an indexed field.");
  }

  private final void addInternal(String name, boolean isIndexed) {
    FieldInfo fi = new FieldInfo(name, isIndexed, byNumber.size());
    byNumber.addElement(fi);
    byName.put(name, fi);
  }

  final int fieldNumber(String fieldName) {
    FieldInfo fi = fieldInfo(fieldName);
    if (fi != null)
      return fi.number;
    else
      return -1;
  }

  final FieldInfo fieldInfo(String fieldName) {
    return (FieldInfo)byName.get(fieldName);
  }

  final String fieldName(int fieldNumber) {
    return fieldInfo(fieldNumber).name;
  }

  final FieldInfo fieldInfo(int fieldNumber) {
    return (FieldInfo)byNumber.elementAt(fieldNumber);
  }

  final int size() {
    return byNumber.size();
  }

  final void write(Directory d, String name) throws IOException {
    OutputStream output = d.createFile(name);
    try {
      write(output);
    } finally {
      output.close();
    }
  }

  final void write(OutputStream output) throws IOException {
    output.writeVInt(size());
    for (int i = 0; i < size(); i++) {
      FieldInfo fi = fieldInfo(i);
      output.writeString(fi.name);
      output.writeByte((byte)(fi.isIndexed ? 1 : 0));
    }
  }

  private final void read(InputStream input) throws IOException {
    int size = input.readVInt();
    for (int i = 0; i < size; i++)
      addInternal(input.readString().intern(),
		  input.readByte() != 0);
  }
}
