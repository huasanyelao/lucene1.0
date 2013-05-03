/* DateFilter.java
 *
 * Copyright (c) 1998, 2000 Douglass R. Cutting.
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
package com.lucene.search;

import java.util.BitSet;
import java.util.Date;
import java.io.IOException;

import com.lucene.document.DateField;
import com.lucene.index.Term;
import com.lucene.index.TermDocs;
import com.lucene.index.TermEnum;
import com.lucene.index.IndexReader;

/** A Filter that restricts search results to a range of time.

   <p>For this to work, documents must have been indexed with a {@link
   DateField}.  */

public final class DateFilter extends Filter {
  String field;

  String start = DateField.MIN_DATE_STRING();
  String end = DateField.MAX_DATE_STRING();

  private DateFilter(String f) {
    field = f;
  }

  /** Constructs a filter for field <code>f</code> matching dates between
    <code>from</code> and <code>to</code>. */
  public DateFilter(String f, Date from, Date to) {
    field = f;
    start = DateField.dateToString(from);
    end = DateField.dateToString(to);
  }
  /** Constructs a filter for field <code>f</code> matching times between
    <code>from</code> and <code>to</code>. */
  public DateFilter(String f, long from, long to) {
    field = f;
    start = DateField.timeToString(from);
    end = DateField.timeToString(to);
  }

  /** Constructs a filter for field <code>f</code> matching dates before
    <code>date</code>. */
  public static DateFilter Before(String field, Date date) {
    DateFilter result = new DateFilter(field);
    result.end = DateField.dateToString(date);
    return result;
  }
  /** Constructs a filter for field <code>f</code> matching times before
    <code>time</code>. */
  public static DateFilter Before(String field, long time) {
    DateFilter result = new DateFilter(field);
    result.end = DateField.timeToString(time);
    return result;
  }

  /** Constructs a filter for field <code>f</code> matching dates before
    <code>date</code>. */
  public static DateFilter After(String field, Date date) {
    DateFilter result = new DateFilter(field);
    result.start = DateField.dateToString(date);
    return result;
  }
  /** Constructs a filter for field <code>f</code> matching times before
    <code>time</code>. */
  public static DateFilter After(String field, long time) {
    DateFilter result = new DateFilter(field);
    result.start = DateField.timeToString(time);
    return result;
  }

  /** Returns a BitSet with true for documents which should be permitted in
    search results, and false for those that should not. */
  final public BitSet bits(IndexReader reader) throws IOException {
    BitSet bits = new BitSet(reader.maxDoc());
    TermEnum termEnum = reader.terms(new Term(field, start));
    try {
      Term stop = new Term(field, end);
      while (termEnum.term().compareTo(stop) <= 0) {
	TermDocs termDocs = reader.termDocs(termEnum.term());
	try {
	  while (termDocs.next())
	    bits.set(termDocs.doc());
	} finally {
	  termDocs.close();
	}
	if (!termEnum.next()) {
	  break;
	}
      }
    } finally {
      termEnum.close();
    }
    return bits;
  }

  public final String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append(field);
    buffer.append(":");
    buffer.append(DateField.stringToDate(start).toString());
    buffer.append("-");
    buffer.append(DateField.stringToDate(end).toString());
    return buffer.toString();
  }
}
