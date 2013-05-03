/* DateField.java
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
package com.lucene.document;
import java.util.Date;

/** Provides support for converting dates to strings and vice-versa.  The
   * strings are structured so that lexicographic sorting orders by date.  This
   * makes them suitable for use as field values and search terms.  */
public class DateField {
  private DateField() {};

  // make date strings long enough to last a millenium
  private static int DATE_LEN = Long.toString(1000L*365*24*60*60*1000,
					       Character.MAX_RADIX).length();

  public static String MIN_DATE_STRING() {
    return timeToString(0);
  }

  public static String MAX_DATE_STRING() {
    char[] buffer = new char[DATE_LEN];
    char c = Character.forDigit(Character.MAX_RADIX-1, Character.MAX_RADIX);
    for (int i = 0 ; i < DATE_LEN; i++)
      buffer[i] = c;
    return new String(buffer);
  }
  
  /** Converts a Date to a string suitable for indexing. */
  public static String dateToString(Date date) {
    return timeToString(date.getTime());
  }
  /** Converts a millisecond time to a string suitable for indexing. */
  public static String timeToString(long time) {
    if (time < 0)
      throw new RuntimeException("time too early");

    String s = Long.toString(time, Character.MAX_RADIX);

    if (s.length() > DATE_LEN)
      throw new RuntimeException("time too late");

    while (s.length() < DATE_LEN)
      s = "0" + s;				  // pad with leading zeros

    return s;
  }

  /** Converts a string-encoded date into a millisecond time. */
  public static long stringToTime(String s) {
    return Long.parseLong(s, Character.MAX_RADIX);
  }
  /** Converts a string-encoded date into a Date object. */
  public static Date stringToDate(String s) {
    return new Date(stringToTime(s));
  }
}
