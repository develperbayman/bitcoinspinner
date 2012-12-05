/**
 * Copyright 2011 bccapi.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
 * Utilities for converting between byte arrays and unsigned integer values.
 */
public class BitUtils {

   public static long uint16ToLong(byte[] buf, int offset) {
      return ((buf[offset++] & 0xFFL) << 0) | ((buf[offset++] & 0xFFL) << 8);
   }

   public static long uint32ToLong(byte[] buf, int offset) {
      return ((buf[offset++] & 0xFFL) << 0) | ((buf[offset++] & 0xFFL) << 8) | ((buf[offset++] & 0xFFL) << 16)
            | ((buf[offset] & 0xFFL) << 24);
   }

   public static long uint64ToLong(byte[] buf, int offset) {
      return ((buf[offset++] & 0xFFL) << 0) | ((buf[offset++] & 0xFFL) << 8) | ((buf[offset++] & 0xFFL) << 16)
            | ((buf[offset++] & 0xFFL) << 24) | ((buf[offset++] & 0xFFL) << 32) | ((buf[offset++] & 0xFFL) << 40)
            | ((buf[offset++] & 0xFFL) << 48) | ((buf[offset++] & 0xFFL) << 56);
   }

   public static long uint16FromStream(StreamReader reader) throws IOException {
      return ((reader.read() & 0xFFL) << 0) | ((reader.read() & 0xFFL) << 8);
   }

   public static long uint16FromStream(DataInputStream stream) throws IOException {
      return ((stream.read() & 0xFFL) << 0) | ((stream.read() & 0xFFL) << 8);
   }

   public static long uint32FromStream(StreamReader reader) throws IOException {
      return ((reader.read() & 0xFFL) << 0) | ((reader.read() & 0xFFL) << 8) | ((reader.read() & 0xFFL) << 16)
            | ((reader.read() & 0xFFL) << 24);
   }

   public static long uint32FromStream(DataInputStream stream) throws IOException {
      return ((stream.read() & 0xFFL) << 0) | ((stream.read() & 0xFFL) << 8) | ((stream.read() & 0xFFL) << 16)
            | ((stream.read() & 0xFFL) << 24);
   }

   public static long uint64FromStream(StreamReader reader) throws IOException {
      return ((reader.read() & 0xFFL) << 0) | ((reader.read() & 0xFFL) << 8) | ((reader.read() & 0xFFL) << 16)
            | ((reader.read() & 0xFFL) << 24) | ((reader.read() & 0xFFL) << 32) | ((reader.read() & 0xFFL) << 40)
            | ((reader.read() & 0xFFL) << 48) | ((reader.read() & 0xFFL) << 56);
   }

   public static long uint64FromStream(DataInputStream stream) throws IOException {
      return ((stream.read() & 0xFFL) << 0) | ((stream.read() & 0xFFL) << 8) | ((stream.read() & 0xFFL) << 16)
            | ((stream.read() & 0xFFL) << 24) | ((stream.read() & 0xFFL) << 32) | ((stream.read() & 0xFFL) << 40)
            | ((stream.read() & 0xFFL) << 48) | ((stream.read() & 0xFFL) << 56);
   }

   public static void uint32ToStream(long value, OutputStream stream) throws IOException {
      stream.write((byte) (0xFF & (value >> 0)));
      stream.write((byte) (0xFF & (value >> 8)));
      stream.write((byte) (0xFF & (value >> 16)));
      stream.write((byte) (0xFF & (value >> 24)));
   }

   public static void uint64ToStream(long value, OutputStream stream) throws IOException {
      stream.write((byte) (0xFF & (value >> 0)));
      stream.write((byte) (0xFF & (value >> 8)));
      stream.write((byte) (0xFF & (value >> 16)));
      stream.write((byte) (0xFF & (value >> 24)));
      stream.write((byte) (0xFF & (value >> 32)));
      stream.write((byte) (0xFF & (value >> 40)));
      stream.write((byte) (0xFF & (value >> 48)));
      stream.write((byte) (0xFF & (value >> 56)));
   }

   public static void uint32ToByteArrayLE(long value, byte[] output, int offset) {
      output[offset + 0] = (byte) (0xFF & (value >> 0));
      output[offset + 1] = (byte) (0xFF & (value >> 8));
      output[offset + 2] = (byte) (0xFF & (value >> 16));
      output[offset + 3] = (byte) (0xFF & (value >> 24));
   }
   
   public static boolean areEqual(byte[] a, byte[] b){
      if(a == null && b == null){
         return true;
      }
      if(a == null || b == null){
         return false;
      }
      if(a.length != b.length){
         return false;
      }
      for(int i=0;i<a.length;i++){
         if(a[i]!= b[i]){
            return false;
         }
      }
      return true;
   }
   
   public static byte[] copyByteArray(byte[] source) {
      byte[] buf = new byte[source.length];
      System.arraycopy(source, 0, buf, 0, buf.length);
      return buf;
   }   

}
