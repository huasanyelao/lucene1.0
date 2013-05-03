/* FieldsWriter.java
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

import java.util.Enumeration;
import java.util.Hashtable;
import java.io.IOException;

import com.lucene.store.Directory;
import com.lucene.store.OutputStream;
import com.lucene.document.Document;
import com.lucene.document.Field;

final class FieldsWriter {
  private FieldInfos fieldInfos;
  private OutputStream fieldsStream;
  private OutputStream indexStream;
  
  FieldsWriter(Directory d, String segment, FieldInfos fn)
       throws IOException {
    fieldInfos = fn;
    fieldsStream = d.createFile(segment + ".fdt");
    indexStream = d.createFile(segment + ".fdx");
  }

  final void close() throws IOException {
    fieldsStream.close();
    indexStream.close();
  }

  final void addDocument(Document doc) throws IOException {
    indexStream.writeLong(fieldsStream.getFilePointer());
    
    int storedCount = 0;
    Enumeration fields  = doc.fields();
    while (fields.hasMoreElements()) {
      Field field = (Field)fields.nextElement();
      if (field.isStored())
	storedCount++;
    }
    fieldsStream.writeVInt(storedCount);
    
    fields  = doc.fields();
    while (fields.hasMoreElements()) {
      Field field = (Field)fields.nextElement();
      if (field.isStored()) {
	fieldsStream.writeVInt(fieldInfos.fieldNumber(field.name()));

	byte bits = 0;
	if (field.isTokenized())
	  bits |= 1;
	fieldsStream.writeByte(bits);

	fieldsStream.writeString(field.stringValue());
      }
    }
  }
}
