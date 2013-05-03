/* TermEnum.java
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

/** Abstract class for enumerating terms.

  <p>Term enumerations are always ordered by Term.compareTo().  Each term in
  the enumeration is greater than all that precede it.  */

public abstract class TermEnum {
  /** Increments the enumeration to the next element.  True if one exists.*/
  abstract public boolean next() throws IOException;

  /** Returns the current Term in the enumeration.
    Initially invalid, valid after next() called for the first time.*/
  abstract public Term term();

  /** Returns the docFreq of the current Term in the enumeration.
    Initially invalid, valid after next() called for the first time.*/
  abstract public int docFreq();

  /** Closes the enumeration to further activity, freeing resources. */
  abstract public void close() throws IOException;
}
