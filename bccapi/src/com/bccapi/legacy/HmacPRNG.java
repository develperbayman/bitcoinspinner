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

package com.bccapi.legacy;

import java.security.NoSuchAlgorithmException;

import com.bccapi.bitlib.crypto.Hmac;
import com.bccapi.bitlib.crypto.RandomSource;
import com.bccapi.bitlib.util.BitUtils;

/**
 * A Pseudo Random Number Generator based on HMAC SHA-256 which is wrapping
 * {@link RandomSource}. This way we are certain that we use the same random
 * generator on all platforms, and can generate the same sequence of random
 * bytes from the same seed.
 */
public class HmacPRNG extends RandomSource {

   private int _nonce;
   private byte[] _key;
   private byte[] _randomBuffer;
   private int _index;

   /**
    * Constructor based on an input seed.
    * 
    * @param seed
    *           The seed to use.
    * @throws NoSuchAlgorithmException
    */
   public HmacPRNG(byte[] seed) throws NoSuchAlgorithmException {
      _key = seed;
      _nonce = 1;
      _randomBuffer = new byte[16];
      hmacIteration();
   }

   private void hmacIteration() {
      byte[] message = new byte[4];
      BitUtils.uint32ToByteArrayLE(_nonce++, message, 0);
      byte[] temp = Hmac.hmacSha256(_key, message);
      // Only use half of the output as random bytes
      System.arraycopy(temp, 0, _randomBuffer, 0, _randomBuffer.length);
      _index = 0;
   }

   @Override
   public synchronized void nextBytes(byte[] bytes) {
      for (int i = 0; i < bytes.length; i++) {
         bytes[i] = nextByte();
      }
   }

   private byte nextByte() {
      if (_index == _randomBuffer.length) {
         hmacIteration();
      }
      return _randomBuffer[_index++];
   }

}
