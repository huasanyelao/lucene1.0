/* PhrasePositions.java
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

import java.io.IOException;
import com.lucene.index.*;

final class PhrasePositions {
  int doc;					  // current doc
  int position;					  // position in doc
  int count;					  // remaining pos in this doc
  int offset;					  // position in phrase
  TermPositions tp;				  // stream of positions
  PhrasePositions next;				  // used to make lists

  PhrasePositions(TermPositions t, int o) throws IOException {
    tp = t;
    offset = o;
    next();
  }

  final void next() throws IOException {	  // increments to next doc
    if (!tp.next()) {
      tp.close();				  // close stream
      doc = Integer.MAX_VALUE;			  // sentinel value
      return;
    }
    doc = tp.doc();
    position = 0;
  }

  final void firstPosition() throws IOException {
    count = tp.freq();				  // read first pos
    nextPosition();
  }

  final boolean nextPosition() throws IOException {
    if (count-- > 0) {				  // read subsequent pos's
      position = tp.nextPosition() - offset;
      return true;
    } else
      return false;
  }
}
