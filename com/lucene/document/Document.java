/* Document.java
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
package com.lucene.document;
import java.util.Enumeration;

/** Documents are the unit of indexing and search.
 *
 * A Document is a set of fields.  Each field has a name and a textual value.
 * A field may be stored with the document, in which case it is returned with
 * search hits on the document.  Thus each document should typically contain
 * stored fields which uniquely identify it.
 * */

public final class Document {
  DocumentFieldList fieldList = null;

  /** Constructs a new document with no fields. */
  public Document() {}

  /** Adds a field to a document.  Several fields may be added with
   * the same name.  In this case, if the fields are indexed, their text is
   * treated as though appended for the purposes of search. */
  public final void add(Field field) {
    fieldList = new DocumentFieldList(field, fieldList);
  }

  /** Returns a field with the given name if any exist in this document, or
    null.  If multiple fields may exist with this name, this method returns the
    last added such added. */
  public final Field getField(String name) {
    for (DocumentFieldList list = fieldList; list != null; list = list.next)
      if (list.field.name().equals(name))
	return list.field;
    return null;
  }

  /** Returns the string value of the field with the given name if any exist in
    this document, or null.  If multiple fields may exist with this name, this
    method returns the last added such added. */
  public final String get(String name) {
    Field field = getField(name);
    if (field != null)
      return field.stringValue();
    else
      return null;
  }

  /** Returns an Enumeration of all the fields in a document. */
  public final Enumeration fields() {
    return new DocumentFieldEnumeration(this);
  }

  /** Prints the fields of a document for human consumption. */
  public final String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("Document<");
    for (DocumentFieldList list = fieldList; list != null; list = list.next) {
      buffer.append(list.field.toString());
      if (list.next != null)
	buffer.append(" ");
    }
    buffer.append(">");
    return buffer.toString();
  }

}

final class DocumentFieldList {
  DocumentFieldList(Field f, DocumentFieldList n) {
    field = f;
    next = n;
  }
  Field field;
  DocumentFieldList next;
}

final class DocumentFieldEnumeration implements Enumeration {
  DocumentFieldList fields;
  DocumentFieldEnumeration(Document d) {
    fields = d.fieldList;
  }

  public final boolean hasMoreElements() {
    return fields == null ? false : true;
  }

  public final Object nextElement() {
    Field result = fields.field;
    fields = fields.next;
    return result;
  }
}
