package com.bccapi.core;

import java.util.Collection;

public class StringUtils {

   /**
    * Join a collection of strings with the given separator.
    * 
    * @param strings
    *           The strings to join
    * @param separator
    *           The separator to use
    * @return The concatenation of the collection of strings with the given
    *         separator.
    */
   public static String join(Collection<String> strings, String separator) {
      StringBuilder sb = new StringBuilder();
      boolean first = true;
      for (String s : strings) {
         if (first) {
            first = false;
         } else {
            sb.append(separator);
         }
         sb.append(s);
      }
      return sb.toString();
   }

   /**
    * Return a string that is no longer than capSize, and pad with "..." if
    * returning a substring.
    * 
    * @param str
    *           The string to cap
    * @param capSize
    *           The maximum cap size
    * @return The string capped at capSize.
    */
   public static String cap(String str, int capSize) {
      if (str.length() <= capSize) {
         return str;
      }
      if (capSize <= 3) {
         return str.substring(0, capSize);
      }
      return str.substring(0, capSize - 3) + "...";
   }

}
