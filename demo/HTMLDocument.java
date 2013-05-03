/* HTMLDocument.java
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

import java.io.*;
import com.lucene.document.*;
import demo.HTMLParser.HTMLParser;

/** A utility for making Lucene Documents for HTML documents. */

public class HTMLDocument {
  static char dirSep = System.getProperty("file.separator").charAt(0);

  public static String uid(File f) {
    // Append path and date into a string in such a way that lexicographic
    // sorting gives the same results as a walk of the file hierarchy.  Thus
    // null (\u0000) is used both to separate directory components and to
    // separate the path from the date.
    return f.getPath().replace(dirSep, '\u0000') +
      "\u0000" +
      DateField.timeToString(f.lastModified());
  }

  public static String uid2url(String uid) {
    String url = uid.replace('\u0000', '/');	  // replace nulls with slashes
    return url.substring(0, url.lastIndexOf('/')); // remove date from end
  }

  public static Document Document(File f)
       throws IOException, InterruptedException  {
    // make a new, empty document
    Document doc = new Document();

    // Add the url as a field named "url".  Use an UnIndexed field, so
    // that the url is just stored with the document, but is not searchable.
    doc.add(Field.UnIndexed("url", f.getPath().replace(dirSep, '/')));

    // Add the last modified date of the file a field named "modified".  Use a
    // Keyword field, so that it's searchable, but so that no attempt is made
    // to tokenize the field into words.
    doc.add(Field.Keyword("modified",
			  DateField.timeToString(f.lastModified())));

    // Add the uid as a field, so that index can be incrementally maintained.
    // This field is not stored with document, it is indexed, but it is not
    // tokenized prior to indexing.
    doc.add(new Field("uid", uid(f), false, true, false));

    HTMLParser parser = new HTMLParser(f);

    // Add the tag-stripped contents as a Reader-valued Text field so it will
    // get tokenized and indexed.
    doc.add(Field.Text("contents", parser.getReader()));

    // Add the summary as an UnIndexed field, so that it is stored and returned
    // with hit documents for display.
    doc.add(Field.UnIndexed("summary", parser.getSummary()));

    // Add the title as a separate Text field, so that it can be searched
    // separately.
    doc.add(Field.Text("title", parser.getTitle()));

    // return the document
    return doc;
  }

  private HTMLDocument() {}
}
    
