/* TermDocs.java
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
import com.lucene.document.Document;

/** TermDocs provides an interface for enumerating &lt;document, frequency&gt;
  pairs for a term.  <p> The document portion names each document containing
  the term.  Documents are indicated by number.  The frequency portion gives
  the number of times the term occurred in each document.  <p> The pairs are
  ordered by document number.

  @see IndexReader#termDocs
  */

public interface TermDocs {
  /** Returns the current document number.  <p> This is invalid until {@link
      #next()} is called for the first time.*/
  public int doc();

  /** Returns the frequency of the term within the current document.  <p> This
    is invalid until {@link #next()} is called for the first time.*/
  public int freq();

  /** Moves to the next pair in the enumeration.  <p> Returns true iff there is
    such a next pair in the enumeration. */
  public boolean next() throws IOException;

  /** Attempts to read multiple entries from the enumeration, up to length of
   * <i>docs</i>.  Document numbers are stored in <i>docs</i>, and term
   * frequencies are stored in <i>freqs</i>.  The <i>freqs</i> array must be as
   * long as the <i>docs</i> array.
   *
   * <p>Returns the number of entries read.  Zero is only returned when the
   * stream has been exhausted.  */
  public int read(int[] docs, int[] freqs) throws IOException;

  /** Skips entries to the first beyond the current whose document number is
   * greater than or equal to <i>target</i>. <p>Returns true iff there is such
   * an entry.  <p>Behaves as if written: <pre>
   *   public boolean skipTo(int target) {
   *     do {
   *       if (!next())
   * 	     return false;
   *     } while (target > doc());
   *     return true;
   *   }
   * </pre>
   * Some implementations are considerably more efficient than that.
   */
  public boolean skipTo(int target) throws IOException;

  /** Frees associated resources. */
  public void close() throws IOException;
}


