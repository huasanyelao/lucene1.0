/* SearchTest.java
 *
 * Copyright (c) 1997, 2000 Douglass R. Cutting.
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

class SearchTest {
  public static void main(String[] args) {
    try {
      Directory directory = new RAMDirectory();  
      Analyzer analyzer = new SimpleAnalyzer();
      IndexWriter writer = new IndexWriter(directory, analyzer, true);

      String[] docs = {
	"a b c d e",
	"a b c d e a b c d e",
	"a b c d e f g h i j",
	"a c e",
	"e c a",
	"a c e a c e",
	"a c e a b c"
      };
      for (int j = 0; j < docs.length; j++) {
	Document d = new Document();
	d.add(Field.Text("contents", docs[j]));
	writer.addDocument(d);
      }
      writer.close();

      Searcher searcher = new IndexSearcher(directory);
      
      String[] queries = {
// 	"a b",
// 	"\"a b\"",
// 	"\"a b c\"",
// 	"a c",
// 	"\"a c\"",
	"\"a c e\"",
      };
      Hits hits = null;

      QueryParser parser = new QueryParser("contents", analyzer);
      parser.setPhraseSlop(4);
      for (int j = 0; j < queries.length; j++) {
	Query query = parser.parse(queries[j]);
	System.out.println("Query: " + query.toString("contents"));

      //DateFilter filter =
      //  new DateFilter("modified", Time(1997,0,1), Time(1998,0,1));
      //DateFilter filter = DateFilter.Before("modified", Time(1997,00,01));
      //System.out.println(filter);

	hits = searcher.search(query, null);

	System.out.println(hits.length() + " total results");
	for (int i = 0 ; i < hits.length() && i < 10; i++) {
	  Document d = hits.doc(i);
	  System.out.println(i + " " + hits.score(i)
// 			   + " " + DateField.stringToDate(d.get("modified"))
			     + " " + d.get("contents"));
	}
      }
      searcher.close();
      
    } catch (Exception e) {
      System.out.println(" caught a " + e.getClass() +
			 "\n with message: " + e.getMessage());
    }
  }

  static long Time(int year, int month, int day) {
    GregorianCalendar calendar = new GregorianCalendar();
    calendar.set(year, month, day);
    return calendar.getTime().getTime();
  }
}
