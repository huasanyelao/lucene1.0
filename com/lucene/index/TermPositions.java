/* TermPositions.java
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


/** TermPositions provides an interface for enumerating the &lt;document,
  frequency, &lt;position&gt;* &gt; tuples for a term.  <p> The document and
  frequency are as for a TermDocs.  The positions portion lists the ordinal
  positions of each occurence of a term in a document.
  @see IndexReader#termPositions
  */

public interface TermPositions extends TermDocs {
  /** Returns next position in the current document.  It is an error to call
    this more than {@link #freq()} times
    without calling {@link #next()}<p> This is
    invalid until {@link #next()} is called for
    the first time.*/
  public int nextPosition() throws IOException;
}  
