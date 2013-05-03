/* IndexReader.java
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
import java.io.File;
import com.lucene.store.Directory;
import com.lucene.store.FSDirectory;
import com.lucene.document.Document;

/** IndexReader is an abstract class, providing an interface for accessing an
  index.  Search of an index is done entirely through this abstract interface,
  so that any subclass which implements it is searchable.

  <p> Concrete subclasses of IndexReader are usually constructed with a call to
  the static method {@link #open}.

  <p> For efficiency, in this API documents are often referred to via
  <it>document numbers</it>, non-negative integers which each name a unique
  document in the index.  These document numbers are ephemeral--they may change
  as documents are added to and deleted from an index.  Clients should thus not
  rely on a given document having the same number between sessions. */

abstract public class IndexReader {
  protected IndexReader() {};

  /** Returns an IndexReader reading the index in an FSDirectory in the named
  path. */
  public static IndexReader open(String path) throws IOException {
    return open(FSDirectory.getDirectory(path, false));
  }

  /** Returns an IndexReader reading the index in an FSDirectory in the named
  path. */
  public static IndexReader open(File path) throws IOException {
    return open(FSDirectory.getDirectory(path, false));
  }

  /** Returns an IndexReader reading the index in the given Directory. */
  public static IndexReader open(Directory directory) throws IOException {
    synchronized (directory) {
      SegmentInfos infos = new SegmentInfos();
      infos.read(directory);
      if (infos.size() == 1)			  // index is optimized
	return new SegmentReader(infos.info(0), true);
      
      SegmentReader[] readers = new SegmentReader[infos.size()];
      for (int i = 0; i < infos.size(); i++)
	readers[i] = new SegmentReader(infos.info(i), i == infos.size() - 1);
      return new SegmentsReader(readers);
    }
  }

  /** Returns the time the index in the named directory was last modified. */
  public static long lastModified(String directory) throws IOException {
    return lastModified(new File(directory));
  }

  /** Returns the time the index in the named directory was last modified. */
  public static long lastModified(File directory) throws IOException {
    return FSDirectory.fileModified(directory, "segments");
  }

  /** Returns the time the index in this directory was last modified. */
  public static long lastModified(Directory directory) throws IOException {
    return directory.fileModified("segments");
  }

  /** Returns the number of documents in this index. */
  abstract public int numDocs();
  /** Returns one greater than the largest possible document number.
    This may be used to, e.g., determine how big to allocate an array which
    will have an element for every document number in an index.
   */
  abstract public int maxDoc();
  /** Returns the stored fields of the <code>n</code><sup>th</sup>
      <code>Document</code> in this index. */
  abstract public Document document(int n) throws IOException;

  /** Returns true if document <i>n</i> has been deleted */
  abstract public boolean isDeleted(int n);

  /** Returns the byte-encoded normalization factor for the named field of
    every document.  This is used by the search code to score documents.
    @see com.lucene.search.Similarity#norm
    */
  abstract public byte[] norms(String field) throws IOException;

  /** Returns an enumeration of all the terms in the index.
    The enumeration is ordered by Term.compareTo().  Each term
    is greater than all that precede it in the enumeration. 
   */
  abstract public TermEnum terms() throws IOException;
  /** Returns an enumeration of all terms after a given term.
    The enumeration is ordered by Term.compareTo().  Each term
    is greater than all that precede it in the enumeration. 
   */
  abstract public TermEnum terms(Term t) throws IOException;

  /** Returns the number of documents containing the term <code>t</code>. */
  abstract public int docFreq(Term t) throws IOException;

  /** Returns an enumeration of all the documents which contain
    <code>Term</code>. For each document, the document number, the frequency of
    the term in that document is also provided, for use in search scoring.
    Thus, this method implements the mapping:
    <p><ul>
    Term &nbsp;&nbsp; =&gt; &nbsp;&nbsp; &lt;docNum, freq&gt;<sup>*</sup>
    </ul>
    <p>The enumeration is ordered by document number.  Each document number
    is greater than all that precede it in the enumeration. */
  abstract public TermDocs termDocs(Term t) throws IOException;

  /** Returns an enumeration of all the documents which contain
    <code>Term</code>.  For each document, in addition to the document number
    and frequency of the term in that document, a list of all of the ordinal
    positions of the term in the document is available.  Thus, this method
    implements the mapping:

    <p><ul>
    Term &nbsp;&nbsp; =&gt; &nbsp;&nbsp; &lt;docNum, freq,
          &lt;pos<sub>1</sub>, pos<sub>2</sub>, ...
	  pos<sub>freq-1</sub>&gt;
	&gt;<sup>*</sup>
    </ul>
    <p> This positional information faciliates phrase and proximity searching.
    <p>The enumeration is ordered by document number.  Each document number is
    greater than all that precede it in the enumeration. */
  abstract public TermPositions termPositions(Term t) throws IOException;

  /** Deletes the document numbered <code>docNum</code>.  Once a document is
    deleted it will not appear in TermDocs or TermPostitions enumerations.
    Attempts to read its field with the {@link #document}
    method will result in an error.  The presence of this document may still be
    reflected in the {@link #docFreq} statistic, though
    this will be corrected eventually as the index is further modified.  */
  abstract public void delete(int docNum) throws IOException;

  /** Deletes all documents containing <code>term</code>.
    This is useful if one uses a document field to hold a unique ID string for
    the document.  Then to delete such a document, one merely constructs a
    term with the appropriate field and the unique ID string as its text and
    passes it to this method.  Returns the number of documents deleted. */
  public final int delete(Term term) throws IOException {
    TermDocs docs = termDocs(term);
    if ( docs == null ) return 0;
    int n = 0;
    try {
      while (docs.next()) {
	delete(docs.doc());
	n++;
      }
    } finally {
      docs.close();
    }
    return n;
  }

  /** Closes files associated with this index.
    Also saves any new deletions to disk.
    No other methods should be called after this has been called. */
  abstract public void close() throws IOException;
}
