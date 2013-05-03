/* SearchTest.java
 *
 * Copyright (c) 1998, 2000 Douglass R. Cutting.
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

import com.lucene.store.Directory;
import com.lucene.store.FSDirectory;
import com.lucene.index.IndexReader;
import com.lucene.index.Term;

class DeleteFiles {
  public static void main(String[] args) {
    try {
      Directory directory = FSDirectory.getDirectory("demo index", false);
      IndexReader reader = IndexReader.open(directory);

//       Term term = new Term("path", "pizza");
//       int deleted = reader.delete(term);

//       System.out.println("deleted " + deleted +
// 			 " documents containing " + term);

      for (int i = 0; i < reader.maxDoc(); i++)
	reader.delete(i);

      reader.close();
      directory.close();

    } catch (Exception e) {
      System.out.println(" caught a " + e.getClass() +
			 "\n with message: " + e.getMessage());
    }
  }
}
