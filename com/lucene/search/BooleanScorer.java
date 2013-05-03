/* BooleanScorer.java
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
package com.lucene.search;

import java.io.IOException;
import com.lucene.index.*;

final class BooleanScorer extends Scorer {
  private int currentDoc;

  private SubScorer scorers = null;
  private BucketTable bucketTable = new BucketTable(this);

  private int maxCoord = 1;
  private float[] coordFactors = null;

  private int requiredMask = 0;
  private int prohibitedMask = 0;
  private int nextMask = 1;

  static final class SubScorer {
    public Scorer scorer;
    public boolean required = false;
    public boolean prohibited = false;
    public HitCollector collector;
    public SubScorer next;

    public SubScorer(Scorer scorer, boolean required, boolean prohibited,
		     HitCollector collector, SubScorer next) {
      this.scorer = scorer;
      this.required = required;
      this.prohibited = prohibited;
      this.collector = collector;
      this.next = next;
    }
  }

  final void add(Scorer scorer, boolean required, boolean prohibited) {
    int mask = 0;
    if (required || prohibited) {
      if (nextMask == 0)
	throw new IndexOutOfBoundsException
	  ("More than 32 required/prohibited clauses in query.");
      mask = nextMask;
      nextMask = nextMask << 1;
    } else
      mask = 0;

    if (!prohibited)
      maxCoord++;

    if (prohibited)
      prohibitedMask |= mask;			  // update prohibited mask
    else if (required)
      requiredMask |= mask;			  // update required mask

    scorers = new SubScorer(scorer, required, prohibited,
			    bucketTable.newCollector(mask), scorers);
  }

  private final void computeCoordFactors() throws IOException {
    coordFactors = new float[maxCoord];
    for (int i = 0; i < maxCoord; i++)
      coordFactors[i] = Similarity.coord(i, maxCoord);
  }

  final void score(HitCollector results, int maxDoc) throws IOException {
    if (coordFactors == null)
      computeCoordFactors();

    while (currentDoc < maxDoc) {
      currentDoc = Math.min(currentDoc+BucketTable.SIZE, maxDoc);
      for (SubScorer t = scorers; t != null; t = t.next)
	t.scorer.score(t.collector, currentDoc);
      bucketTable.collectHits(results);
    }
  }

  static final class Bucket {
    int	doc = -1;				  // tells if bucket is valid
    float	score;				  // incremental score
    int	bits;					  // used for bool constraints
    int	coord;					  // count of terms in score
    Bucket 	next;				  // next valid bucket
  }

  /** A simple hash table of document scores within a range. */
  static final class BucketTable {
    public static final int SIZE = 1 << 10;
    public static final int MASK = SIZE - 1;

    final Bucket[] buckets = new Bucket[SIZE];
    Bucket first = null;			  // head of valid list
  
    private BooleanScorer scorer;

    public BucketTable(BooleanScorer scorer) {
      this.scorer = scorer;
    }

    public final void collectHits(HitCollector results) {
      final int required = scorer.requiredMask;
      final int prohibited = scorer.prohibitedMask;
      final float[] coord = scorer.coordFactors;

      for (Bucket bucket = first; bucket!=null; bucket = bucket.next) {
	if ((bucket.bits & prohibited) == 0 &&	  // check prohibited
	    (bucket.bits & required) == required){// check required
	  results.collect(bucket.doc,		  // add to results
			  bucket.score * coord[bucket.coord]);
	}
      }
      first = null;				  // reset for next round
    }

    public final int size() { return SIZE; }

    public HitCollector newCollector(int mask) {
      return new Collector(mask, this);
    }
  }

  static final class Collector extends HitCollector {
    private BucketTable bucketTable;
    private int mask;
    public Collector(int mask, BucketTable bucketTable) {
      this.mask = mask;
      this.bucketTable = bucketTable;
    }
    public final void collect(final int doc, final float score) {
      final BucketTable table = bucketTable;
      final int i = doc & BucketTable.MASK;
      Bucket bucket = table.buckets[i];
      if (bucket == null)
	table.buckets[i] = bucket = new Bucket();
      
      if (bucket.doc != doc) {			  // invalid bucket
	bucket.doc = doc;			  // set doc
	bucket.score = score;			  // initialize score
	bucket.bits = mask;			  // initialize mask
	bucket.coord = 1;			  // initialize coord
	
	bucket.next = table.first;		  // push onto valid list
	table.first = bucket;
      } else {					  // valid bucket
	bucket.score += score;			  // increment score
	bucket.bits |= mask;			  // add bits in mask
	bucket.coord++;				  // increment coord
      }
    }
  }
}
