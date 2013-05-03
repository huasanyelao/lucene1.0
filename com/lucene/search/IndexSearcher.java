/* IndexSearcher.java
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
import java.util.BitSet;

import com.lucene.store.Directory;
import com.lucene.document.Document;
import com.lucene.index.IndexReader;
import com.lucene.index.Term;
import com.lucene.util.PriorityQueue;

/** Implements search over a single IndexReader. */
public final class IndexSearcher extends Searcher {
  IndexReader reader;

  /** Creates a searcher searching the index in the named directory. */
  public IndexSearcher(String path) throws IOException {
    this(IndexReader.open(path));
  }
    
  /** Creates a searcher searching the index in the provided directory. */
  public IndexSearcher(Directory directory) throws IOException {
    this(IndexReader.open(directory));
  }
    
  /** Creates a searcher searching the provided index. */
  public IndexSearcher(IndexReader r) {
    reader = r;
  }
    
  /** Frees resources associated with this Searcher. */
  public final void close() throws IOException {
    reader.close();
  }

  final int docFreq(Term term) throws IOException {
    return reader.docFreq(term);
  }

  final Document doc(int i) throws IOException {
    return reader.document(i);
  }

  final int maxDoc() throws IOException {
    return reader.maxDoc();
  }

  final TopDocs search(Query query, Filter filter, final int nDocs)
       throws IOException {
    Scorer scorer = Query.scorer(query, this, reader);
    if (scorer == null)
      return new TopDocs(0, new ScoreDoc[0]);

    final BitSet bits = filter != null ? filter.bits(reader) : null;
    final HitQueue hq = new HitQueue(nDocs);
    final int[] totalHits = new int[1];
    scorer.score(new HitCollector() {
	private float minScore = 0.0f;
	public final void collect(int doc, float score) {
	  if (score > 0.0f &&			  // ignore zeroed buckets
	      (bits==null || bits.get(doc))) {	  // skip docs not in bits
	    totalHits[0]++;
	    if (score >= minScore) {
	      hq.put(new ScoreDoc(doc, score));	  // update hit queue
	      if (hq.size() > nDocs) {		  // if hit queue overfull
		hq.pop();			  // remove lowest in hit queue
		minScore = ((ScoreDoc)hq.top()).score; // reset minScore
	      }
	    }
	  }
	}
      }, reader.maxDoc());

    ScoreDoc[] scoreDocs = new ScoreDoc[hq.size()];
    for (int i = hq.size()-1; i >= 0; i--)	  // put docs in array
      scoreDocs[i] = (ScoreDoc)hq.pop();
    
    return new TopDocs(totalHits[0], scoreDocs);
  }

  /** Lower-level search API.
   *
   * <p>{@link HitCollector#collect(int,float)} is called for every non-zero
   * scoring document.
   *
   * <p>Applications should only use this if they need <it>all</it> of the
   * matching documents.  The high-level search API ({@link
   * Searcher#search(Query)}) is usually more efficient, as it skips
   * non-high-scoring hits.  */
  public final void search(Query query, HitCollector results)
      throws IOException {
    search(query, null, results);
  }

  /** Lower-level search API.
   *
   * <p>{@link HitCollector#collect(int,float)} is called for every non-zero
   * scoring document.
   *
   * <p>Applications should only use this if they need <it>all</it> of the
   * matching documents.  The high-level search API ({@link
   * Searcher#search(Query)}) is usually more efficient, as it skips
   * non-high-scoring hits.  */
  public final void search(Query query, Filter filter,
			   final HitCollector results) throws IOException {
    HitCollector collector = results;
    if (filter != null) {
      final BitSet bits = filter.bits(reader);
      collector = new HitCollector() {
	  public final void collect(int doc, float score) {
	    if (bits.get(doc)) {		  // skip docs not in bits
	      results.collect(doc, score);
	    }
	  }
	};
    }

    Scorer scorer = Query.scorer(query, this, reader);
    if (scorer == null)
      return;
    scorer.score(collector, reader.maxDoc());
  }

}
