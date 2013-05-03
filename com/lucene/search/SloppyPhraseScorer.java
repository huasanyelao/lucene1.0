/* SloppyPhraseScorer.java
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
import java.util.Vector;
import com.lucene.util.*;
import com.lucene.index.*;

final class SloppyPhraseScorer extends PhraseScorer {
  private int slop;

  SloppyPhraseScorer(TermPositions[] tps, int s, byte[] n, float w)
       throws IOException {
    super(tps, n, w);
    slop = s;
  }

  protected final float phraseFreq() throws IOException {
    pq.clear();
    int end = 0;
    for (PhrasePositions pp = first; pp != null; pp = pp.next) {
      pp.firstPosition();
      if (pp.position > end)
	end = pp.position;
      pq.put(pp);				  // build pq from list
    }

    float freq = 0.0f;
    boolean done = false;
    do {
      PhrasePositions pp = (PhrasePositions)pq.pop();
      int start = pp.position;
      int next = ((PhrasePositions)pq.top()).position;
      for (int pos = start; pos <= next; pos = pp.position) {
	start = pos;				  // advance pp to min window
	if (!pp.nextPosition()) {
	  done = true;				  // ran out of a term -- done
	  break;
	}
      }

      int matchLength = end - start;
      if (matchLength <= slop)
	freq += 1.0 / (matchLength + 1);	  // penalize longer matches

      if (pp.position > end)
	end = pp.position;
      pq.put(pp);				  // restore pq
    } while (!done);
    
    return freq;
  }
}
