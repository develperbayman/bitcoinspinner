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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for reading from an {@link InputStream}. When reading bytes and
 * the requested number of bytes cannot be obtained IOException is thrown.
 */
public class StreamReader {
   private InputStream _stream;

   /**
    * Construct a {@link StreamReader} from an {@link InputStream}.
    * 
    * @param stream
    *           The {@link InputStream} to use.
    */
   public StreamReader(InputStream stream) {
      _stream = stream;
   }

   /**
    * Read one byte and return it as an integer between 0 and 255 or throw.
    * 
    * @return an integer between 0 and 255.
    * @throws IOException
    */
   public int read() throws IOException {
      int data = _stream.read();
      if (data == -1) {
         throw new IOException();
      }
      return data & 0xFF;
   }

   /**
    * Read a number of bytes or throw.
    * 
    * @param size
    *           The number of bytes read
    * @return The array of bytes read.
    * @throws IOException
    */
   public byte[] readBytes(int size) throws IOException {
      byte[] buf = new byte[size];
      int toRead = size;
      int done = 0;
      while (toRead > 0) {
         int read = _stream.read(buf, done, toRead);
         if (read == -1) {
            throw new IOException();
         }
         done += read;
         toRead -= read;
      }
      return buf;
   }

   /**
    * Get the number of bytes available.
    * 
    * @return The number of bytes available.
    * @throws IOException
    */
   public int available() throws IOException {
      return _stream.available();
   }

   /**
    * Read a stream until EOF and return the resulting bytes as a {$link
    * String}.
    * 
    * @param in
    *           The {@link InputStream} to read from.
    * @return A {$link String}.
    * @throws IOException
    */
   public static String readFully(InputStream in) throws IOException {
      return new String(readAllBytes(in));
   }

   /**
    * Read a stream until EOF and return the resulting array of bytes. The
    * {@link InputStream} to read from.
    * 
    * @param stream The stream to read from.
    * @return An array of bytes.
    * @throws IOException
    */
   public static byte[] readAllBytes(InputStream stream) throws IOException {
      if (stream == null) {
         return null;
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
      byte[] buf = new byte[1024];
      while (true) {
         int size = stream.read(buf);
         if (size == -1) {
            return out.toByteArray();
         }
         out.write(buf, 0, size);
      }
   }

}
