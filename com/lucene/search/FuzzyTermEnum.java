/* FuzzyTermEnum.java
 *
 * Copyright (c) 1997, 2000 Dave Kor Kian Wei.
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
import com.lucene.index.IndexReader;
import com.lucene.index.Term;
import com.lucene.index.TermEnum;

/** Subclass of FilteredTermEnum for enumerating all terms that are similiar to the specified filter term.

  <p>Term enumerations are always ordered by Term.compareTo().  Each term in
  the enumeration is greater than all that precede it.  */
final public class FuzzyTermEnum extends FilteredTermEnum {
    double distance;
    boolean fieldMatch = false;
    boolean endEnum = false;

    Term searchTerm = null;
    String field = "";
    String text = "";
    int textlen;
    
    public FuzzyTermEnum(IndexReader reader, Term term) throws IOException {
        super(reader, term);
        searchTerm = term;
        field = searchTerm.field();
        text = searchTerm.text();
        textlen = text.length();
        setEnum(reader.terms(new Term(searchTerm.field(), "")));
    }
    
    /**
     The termCompare method in FuzzyTermEnum uses Levenshtein distance to 
     calculate the distance between the given term and the comparing term. 
     */
    final protected boolean termCompare(Term term) {
        if (field == term.field()) {
            String target = term.text();
            int targetlen = target.length();
            int dist = editDistance(text, target, textlen, targetlen);
            distance = 1 - ((double)dist / (double)Math.min(textlen, targetlen));
            return (distance > FUZZY_THRESHOLD);
        }
        endEnum = true;
        return false;
    }
    
    final protected float difference() {
        return (float)((distance - FUZZY_THRESHOLD) * SCALE_FACTOR);
    }
    
    final public boolean endEnum() {
        return endEnum;
    }
    
    /******************************
     * Compute Levenshtein distance
     ******************************/
    
    public static final double FUZZY_THRESHOLD = 0.5;
    public static final double SCALE_FACTOR = 1.0f / (1.0f - FUZZY_THRESHOLD);
    
    /**
     Finds and returns the smallest of three integers 
     */
    private final static int min(int a, int b, int c) {
        int t = (a < b) ? a : b;
        return (t < c) ? t : c;
    }
    
    /**
     * This static array saves us from the time required to create a new array
     * everytime editDistance is called.
     */
    private int e[][] = new int[0][0];
    
    /**
     Levenshtein distance also known as edit distance is a measure of similiarity
     between two strings where the distance is measured as the number of character 
     deletions, insertions or substitutions required to transform one string to 
     the other string. 
     <p>This method takes in four parameters; two strings and their respective 
     lengths to compute the Levenshtein distance between the two strings.
     The result is returned as an integer.
     */ 
    private final int editDistance(String s, String t, int n, int m) {
        if (e.length <= n || e[0].length <= m) {
            e = new int[Math.max(e.length, n+1)][Math.max(e.length, m+1)];
        }
        int d[][] = e; // matrix
        int i; // iterates through s
        int j; // iterates through t
        char s_i; // ith character of s
        
        if (n == 0) return m;
        if (m == 0) return n;
        
        // init matrix d
        for (i = 0; i <= n; i++) d[i][0] = i;
        for (j = 0; j <= m; j++) d[0][j] = j;
        
        // start computing edit distance
        for (i = 1; i <= n; i++) {
            s_i = s.charAt(i - 1);
            for (j = 1; j <= m; j++) {
                if (s_i != t.charAt(j-1))
                    d[i][j] = min(d[i-1][j], d[i][j-1], d[i-1][j-1])+1;
                else d[i][j] = min(d[i-1][j]+1, d[i][j-1]+1, d[i-1][j-1]);
            }
        }
        
        // we got the result!
        return d[n][m];
    }
    
  public void close() throws IOException {
      super.close();
      searchTerm = null;
      field = null;
      text = null;
  }
}
