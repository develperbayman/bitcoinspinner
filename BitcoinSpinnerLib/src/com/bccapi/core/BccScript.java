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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for handling bitcoin scripts
 */
public class BccScript {

   public static class BccScriptException extends Exception {
      private static final long serialVersionUID = 1L;

      public BccScriptException(String msg) {
         super(msg);
      }

      public BccScriptException() {
      }

   }

   public static final byte[] EMPTY_ARRAY = new byte[0];
   public static final int OP_PUSHDATA1 = 76;
   public static final int OP_PUSHDATA2 = 77;
   public static final int OP_PUSHDATA4 = 78;
   public static final int OP_DUP = 118;
   public static final int OP_HASH160 = 169;
   public static final int OP_EQUAL = 135;
   public static final int OP_EQUALVERIFY = 136;
   public static final int OP_CHECKSIG = 172;

   protected List<byte[]> _chunks;

   public static List<byte[]> chunksFromScriptBytes(byte[] script) {
      List<byte[]> chunks = new ArrayList<byte[]>();
      StreamReader reader = new StreamReader(new ByteArrayInputStream(script));
      try {
         while (reader.available() > 0) {

            // Get opcode
            int opcode = reader.read();
            if (opcode >= 0xF0) {
               opcode = (opcode << 8) | reader.read();
            }

            if (opcode > 0 && opcode < OP_PUSHDATA1) {
               chunks.add(reader.readBytes(opcode));
            } else if (opcode == OP_PUSHDATA1) {
               int size = reader.read();
               chunks.add(reader.readBytes(size));
            } else if (opcode == OP_PUSHDATA2) {
               int size = (int) BitUtils.uint16FromStream(reader);
               chunks.add(reader.readBytes(size));
            } else if (opcode == OP_PUSHDATA4) {
               // We do not support chunks this big
               return null;
            } else {
               chunks.add(new byte[] { (byte) opcode });
            }
         }
      } catch (IOException e) {
         return null;
      }
      return chunks;
   }

   protected static final boolean isOP(byte[] chunk, int op) {
      return chunk.length == 1 && (0xFF & (int) chunk[0]) == op;
   }

   protected BccScript(List<byte[]> chunks) {
      _chunks = chunks;
   }

   public BccScript() {
      _chunks = new ArrayList<byte[]>();
   }

   protected void addChunk(byte[] chunk) {
      _chunks.add(chunk);
   }

   protected void addOpcode(int opcode) {
      _chunks.add(new byte[] { (byte) opcode });
   }

   /**
    * Get the script as an array of bytes
    * 
    * @return The script as an array of bytes
    */
   public byte[] toByteArray() {
      byte[] buf = new byte[calculateByteSize()];
      int index = 0;
      for (byte[] chunk : _chunks) {
         if (chunk.length < OP_PUSHDATA1) {
            buf[index++] = (byte) (0xFF & chunk.length);
            System.arraycopy(chunk, 0, buf, index, chunk.length);
            index += chunk.length;
         } else if (chunk.length < 256) {
            buf[index++] = (byte) (0xFF & OP_PUSHDATA1);
            buf[index++] = (byte) (0xFF & chunk.length);
            System.arraycopy(chunk, 0, buf, index, chunk.length);
            index += chunk.length;
         } else if (chunk.length < 65536) {
            buf[index++] = (byte) (0xFF & OP_PUSHDATA2);
            buf[index++] = (byte) (0xFF & chunk.length);
            buf[index++] = (byte) (0xFF & (chunk.length >> 8));
            System.arraycopy(chunk, 0, buf, index, chunk.length);
            index += chunk.length;
         } else {
            throw new RuntimeException("Chunks larger than 65536 not implemented");
         }
      }
      return buf;
   }

   private int calculateByteSize() {
      int size = 0;
      for (byte[] chunk : _chunks) {
         if (chunk.length < OP_PUSHDATA1) {
            size += 1 + chunk.length;
         } else if (chunk.length < 256) {
            size += 1 + 1 + chunk.length;
         } else if (chunk.length < 65536) {
            size += 1 + 1 + 1 + chunk.length;
         } else {
            throw new RuntimeException("Chunks larger than 65536 not implemented");
         }
      }
      return size;
   }

}
