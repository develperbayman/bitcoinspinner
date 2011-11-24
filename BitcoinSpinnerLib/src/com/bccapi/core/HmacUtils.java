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

public class HmacUtils {
   private static final int SHA256_BLOCK_SIZE = 64;
   public static byte[] hmacSha256(byte[] key, byte[] message) {

      // Ensure 32 byte key length
      if (key.length > SHA256_BLOCK_SIZE) {
         key = HashUtils.sha256(key);
      }
      if (key.length < SHA256_BLOCK_SIZE) {
         // Zero pad
         byte[] temp = new byte[SHA256_BLOCK_SIZE];
         System.arraycopy(key, 0, temp, 0, key.length);
         key = temp;
      }

      // Prepare o key pad
      byte[] o_key_pad = new byte[SHA256_BLOCK_SIZE];
      for (int i = 0; i < SHA256_BLOCK_SIZE; i++) {
         o_key_pad[i] = (byte) (0x5c ^ key[i]);
      }

      // Prepare i key pad
      byte[] i_key_pad = new byte[SHA256_BLOCK_SIZE];
      for (int i = 0; i < SHA256_BLOCK_SIZE; i++) {
         i_key_pad[i] = (byte) (0x36 ^ key[i]);
      }

      return HashUtils.sha256(o_key_pad, HashUtils.sha256(i_key_pad, message));
   }

}
