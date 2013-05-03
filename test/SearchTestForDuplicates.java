/* SearchTestForDuplicates.java
 *
 * Copyright (c) 1997, 2000 Douglass R. Cutting.
 * Copyright (c) 2001 Robert Leftwich
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package test;

import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;

import com.lucene.store.*;
import com.lucene.document.*;
import com.lucene.analysis.*;
import com.lucene.index.*;
import com.lucene.search.*;
import com.lucene.queryParser.*;

class SearchTestForDuplicates {

  final static String PRIORITY_FIELD ="priority";
  final static String ID_FIELD ="id";
  final static String HIGH_PRIORITY ="high";
  final static String MED_PRIORITY ="medium";
  final static String LOW_PRIORITY ="low";

  public static void main(String[] args) {
    try {
      Directory directory = new RAMDirectory();
      Analyzer analyzer = new SimpleAnalyzer();
      IndexWriter writer = new IndexWriter(directory, analyzer, true);

      final int MAX_DOCS = 225;

      for (int j = 0; j < MAX_DOCS; j++) {
        Document d = new Document();
        d.add(Field.Text(PRIORITY_FIELD, HIGH_PRIORITY));
        d.add(Field.Text(ID_FIELD, Integer.toString(j)));
        writer.addDocument(d);
      }
      writer.close();

      // try a search without OR
      Searcher searcher = new IndexSearcher(directory);
      Hits hits = null;

      QueryParser parser = new QueryParser(PRIORITY_FIELD, analyzer);

      Query query = parser.parse(HIGH_PRIORITY);
      System.out.println("Query: " + query.toString(PRIORITY_FIELD));

      hits = searcher.search(query, null);
      printHits(hits);

      searcher.close();

      // try a new search with OR
      searcher = new IndexSearcher(directory);
      hits = null;

      parser = new QueryParser(PRIORITY_FIELD, analyzer);

      query = parser.parse(HIGH_PRIORITY + " OR " + MED_PRIORITY);
      System.out.println("Query: " + query.toString(PRIORITY_FIELD));

      hits = searcher.search(query, null);
      printHits(hits);

      searcher.close();

    } catch (Exception e) {
      System.out.println(" caught a " + e.getClass() +
                         "\n with message: " + e.getMessage());
    }
  }

  private static void printHits( Hits hits ) throws IOException {
    System.out.println(hits.length() + " total results\n");
    for (int i = 0 ; i < hits.length(); i++) {
      if ( i < 10 || (i > 94 && i < 105) ) {
        Document d = hits.doc(i);
        System.out.println(i + " " + d.get(ID_FIELD));
      }
    }
  }

}
