/* Hits.java
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
import java.util.Vector;
import java.util.BitSet;
import com.lucene.document.Document;
import com.lucene.index.IndexReader;

/** A ranked list of documents, used to hold search results. */
public final class Hits {
  private Query query;
  private Searcher searcher;
  private Filter filter = null;

  private int length;				  // the total number of hits
  private Vector hitDocs = new Vector();	  // cache of hits retrieved

  private HitDoc first;				  // head of LRU cache
  private HitDoc last;				  // tail of LRU cache
  private int numDocs = 0;			  // number cached
  private int maxDocs = 200;			  // max to cache

  Hits(Searcher s, Query q, Filter f) throws IOException {
    query = q;
    searcher = s;
    filter = f;
    getMoreDocs(50);				  // retrieve 100 initially
  }

  // Tries to add new documents to hitDocs.
  // Ensures that the hit numbered <code>min</code> has been retrieved.
  private final void getMoreDocs(int min) throws IOException {
    if (hitDocs.size() > min)
      min = hitDocs.size();

    int n = min * 2;				  // double # retrieved
    TopDocs topDocs = searcher.search(query, filter, n);
    length = topDocs.totalHits;
    ScoreDoc[] scoreDocs = topDocs.scoreDocs;

    float scoreNorm = 1.0f;
    if (length > 0 && scoreDocs[0].score > 1.0f)
      scoreNorm = 1.0f / scoreDocs[0].score;

    int end = scoreDocs.length < length ? scoreDocs.length : length;
    for (int i = hitDocs.size(); i < end; i++)
      hitDocs.addElement(new HitDoc(scoreDocs[i].score*scoreNorm,
				    scoreDocs[i].doc));
  }

  /** Returns the total number of hits available in this set. */
  public final int length() {
    return length;
  }

  /** Returns the nth document in this set.
    <p>Documents are cached, so that repeated requests for the same element may
    return the same Document object. */ 
  public final Document doc(int n) throws IOException {
    HitDoc hitDoc = hitDoc(n);

    // Update LRU cache of documents
    remove(hitDoc);				  // remove from list, if there
    addToFront(hitDoc);				  // add to front of list
    if (numDocs > maxDocs) {			  // if cache is full
      HitDoc oldLast = last;
      remove(last);				  // flush last
      oldLast.doc = null;			  // let doc get gc'd
    }

    if (hitDoc.doc == null)
      hitDoc.doc = searcher.doc(hitDoc.id);	  // cache miss: read document
      
    return hitDoc.doc;
  }

  /** Returns the score for the nth document in this set. */ 
  public final float score(int n) throws IOException {
    return hitDoc(n).score;
  }

  private final HitDoc hitDoc(int n) throws IOException {
    if (n >= length)
      throw new IndexOutOfBoundsException("Not a valid hit number: " + n);
    if (n >= hitDocs.size())
      getMoreDocs(n);

    return (HitDoc)hitDocs.elementAt(n);
  }

  private final void addToFront(HitDoc hitDoc) {  // insert at front of cache
    if (first == null)
      last = hitDoc;
    else
      first.prev = hitDoc;
    
    hitDoc.next = first;
    first = hitDoc;
    hitDoc.prev = null;

    numDocs++;
  }

  private final void remove(HitDoc hitDoc) {	  // remove from cache
    if (hitDoc.doc == null)			  // it's not in the list
      return;					  // abort

    if (hitDoc.next == null)
      last = hitDoc.prev;
    else
      hitDoc.next.prev = hitDoc.prev;
    
    if (hitDoc.prev == null)
      first = hitDoc.next;
    else
      hitDoc.prev.next = hitDoc.next;

    numDocs--;
  }
}

final class HitDoc {
  float score;
  int id;
  Document doc = null;

  HitDoc next;					  // in doubly-linked cache
  HitDoc prev;					  // in doubly-linked cache

  HitDoc(float s, int i) {
    score = s;
    id = i;
  }
}
