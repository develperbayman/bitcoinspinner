/**
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bccapi.core;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Used for representing Bitcoin's compact size.
 */
public class CompactInt {

   /**
    * Read a CompactInt from a stream.
    * 
    * @param stream
    *           The stream to read from
    * @return the long value representing the CompactInt read.
    * @throws IOException
    */
   public static long fromStream(DataInputStream stream) throws IOException {
      int first = stream.read();
      long value;
      if (first < 253) {
         // Regard this byte as a 8 bit value.
         value = first;
      } else if (first == 253) {
         // Regard the following two bytes as a 16 bit value
         value = BitUtils.uint16FromStream(stream);
      } else if (first == 254) {
         // Regard the following four bytes as a 32 bit value
         value = BitUtils.uint32FromStream(stream);
      } else {
         // Regard the following four bytes as a 64 bit value
         value = BitUtils.uint64FromStream(stream);
      }
      return value;
   }

   /**
    * Write a long value to a stream as a CompaceInt
    * 
    * @param value
    *           The value to write.
    * @param stream
    *           The stream to write to.
    * @throws IOException
    */
   public static void toStream(long value, OutputStream stream) throws IOException {
      stream.write(toBytes(value));
   }

   /**
    * Turn a long value into an array of bytes containing the CompactInt
    * representation.
    * 
    * @param value
    *           The value to turn into an array of bytes.
    * @return an array of bytes.
    */
   public static byte[] toBytes(long value) {
      if (isLessThan(value, 253)) {
         return new byte[] { (byte) value };
      } else if (isLessThan(value, 65536)) {
         return new byte[] { (byte) 253, (byte) (value), (byte) (value >> 8) };
      } else if (isLessThan(value, 4294967295L)) {
         byte[] bytes = new byte[5];
         bytes[0] = (byte) 254;
         BitUtils.uint32ToByteArrayLE(value, bytes, 1);
         return bytes;
      } else {
         byte[] bytes = new byte[9];
         bytes[0] = (byte) 255;
         BitUtils.uint32ToByteArrayLE(value, bytes, 1);
         BitUtils.uint32ToByteArrayLE(value >>> 32, bytes, 5);
         return bytes;
      }
   }

   /**
    * Determine whether one long is less than another long when comparing as
    * unsigned longs.
    */
   private static boolean isLessThan(long n1, long n2) {
      return (n1 < n2) ^ ((n1 < 0) != (n2 < 0));
   }

}
