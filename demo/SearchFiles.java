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
package demo;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.lucene.analysis.Analyzer;
import com.lucene.analysis.StopAnalyzer;
import com.lucene.document.Document;
import com.lucene.search.Searcher;
import com.lucene.search.IndexSearcher;
import com.lucene.search.Query;
import com.lucene.search.Hits;
import com.lucene.queryParser.QueryParser;

class SearchFiles {
  public static void main(String[] args) {
    try {
      Searcher searcher = new IndexSearcher("index");
      Analyzer analyzer = new StopAnalyzer();

      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      while (true) {
	System.out.print("Query: ");
	String line = in.readLine();

	if (line.length() == -1)
	  break;

	Query query = QueryParser.parse(line, "contents", analyzer);
	System.out.println("Searching for: " + query.toString("contents"));

	Hits hits = searcher.search(query);
	System.out.println(hits.length() + " total matching documents");

	final int HITS_PER_PAGE = 10;
	for (int start = 0; start < hits.length(); start += HITS_PER_PAGE) {
	  int end = Math.min(hits.length(), start + HITS_PER_PAGE);
	  for (int i = start; i < end; i++)
	    System.out.println(i + ". " + hits.doc(i).get("path"));
	  if (hits.length() > end) {
	    System.out.print("more (y/n) ? ");
	    line = in.readLine();
	    if (line.length() == 0 || line.charAt(0) == 'n')
	      break;
	  }
	}
      }
      searcher.close();

    } catch (Exception e) {
      System.out.println(" caught a " + e.getClass() +
			 "\n with message: " + e.getMessage());
    }
  }
}
