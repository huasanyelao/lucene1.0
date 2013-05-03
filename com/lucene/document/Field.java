/* Field.java
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
import java.io.Reader;

/**
  A field is a section of a Document.  Each field has two parts, a name and a
  value.  Values may be free text, provided as a String or as a Reader, or they
  may be atomic keywords, which are not further processed.  Such keywords may
  be used to represent dates, urls, etc.  Fields are optionally stored in the
  index, so that they may be returned with hits on the document.
  */

public final class Field {
  private String name = "body";
  private String stringValue = null;
  private Reader readerValue = null;
  private boolean isStored = false;
  private boolean isIndexed = true;
  private boolean isTokenized = true;

  /** Constructs a String-valued Field that is not tokenized, but is indexed
    and stored.  Useful for non-text fields, e.g. date or url.  */
  public static final Field Keyword(String name, String value) {
    return new Field(name, value, true, true, false);
  }

  /** Constructs a String-valued Field that is not tokenized or indexed,
    but is stored in the index, for return with hits. */
  public static final Field UnIndexed(String name, String value) {
    return new Field(name, value, true, false, false);
  }

  /** Constructs a String-valued Field that is tokenized and indexed,
    and is stored in the index, for return with hits.  Useful for short text
    fields, like "title" or "subject". */
  public static final Field Text(String name, String value) {
    return new Field(name, value, true, true, true);
  }

  /** Constructs a String-valued Field that is tokenized and indexed,
    but that is not stored in the index. */
  public static final Field UnStored(String name, String value) {
    return new Field(name, value, false, true, true);
  }

  /** Constructs a Reader-valued Field that is tokenized and indexed, but is
    not stored in the index verbatim.  Useful for longer text fields, like
    "body". */
  public static final Field Text(String name, Reader value) {
    return new Field(name, value);
  }

  /** The name of the field (e.g., "date", "subject", "title", "body", etc.)
    as an interned string. */
  public String name() 		{ return name; }

  /** The value of the field as a String, or null.  If null, the Reader value
    is used.  Exactly one of stringValue() and readerValue() must be set. */
  public String stringValue()		{ return stringValue; }
  /** The value of the field as a Reader, or null.  If null, the String value
    is used.  Exactly one of stringValue() and readerValue() must be set. */
  public Reader readerValue()	{ return readerValue; }

  public Field(String name, String string,
	       boolean store, boolean index, boolean token) {
    if (name == null)
      throw new IllegalArgumentException("name cannot be null");
    if (string == null)
      throw new IllegalArgumentException("value cannot be null");

    this.name = name.intern();			  // field names are interned
    this.stringValue = string;
    this.isStored = store;
    this.isIndexed = index;
    this.isTokenized = token;
  }
  Field(String name, Reader reader) {
    if (name == null)
      throw new IllegalArgumentException("name cannot be null");
    if (reader == null)
      throw new IllegalArgumentException("value cannot be null");

    this.name = name.intern();			  // field names are interned
    this.readerValue = reader;
  }

  /** True iff the value of the field is to be stored in the index for return
    with search hits.  It is an error for this to be true if a field is
    Reader-valued. */
  public final boolean	isStored() 	{ return isStored; }

  /** True iff the value of the field is to be indexed, so that it may be
    searched on. */
  public final boolean 	isIndexed() 	{ return isIndexed; }

  /** True iff the value of the field should be tokenized as text prior to
    indexing.  Un-tokenized fields are indexed as a single word and may not be
    Reader-valued. */
  public final boolean 	isTokenized() 	{ return isTokenized; }

  /** Prints a Field for human consumption. */
  public final String toString() {
    if (isStored && isIndexed && !isTokenized)
      return "Keyword<" + name + ":" + stringValue + ">";
    else if (isStored && !isIndexed && !isTokenized)
      return "Unindexed<" + name + ":" + stringValue + ">";
    else if (isStored && isIndexed && isTokenized && stringValue!=null)
      return "Text<" + name + ":" + stringValue + ">";
    else if (!isStored && isIndexed && isTokenized && readerValue!=null)
      return "Text<" + name + ":" + readerValue + ">";
    else
      return super.toString();
  }

}
