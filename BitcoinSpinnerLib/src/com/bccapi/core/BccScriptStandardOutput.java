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

import java.util.List;

/**
 * This class handles Transaction output scripts for p2p payments
 */
public class BccScriptStandardOutput extends BccScriptOutput {

   public BccScriptStandardOutput(PublicECKey key) {
      byte[] address = key.getPublicKeyHash();
      addOpcode(OP_DUP);
      addOpcode(OP_HASH160);
      addChunk(address);
      addOpcode(OP_EQUALVERIFY);
      addOpcode(OP_CHECKSIG);
   }

   protected BccScriptStandardOutput(List<byte[]> chunks) {
      super(chunks);
   }

   protected static boolean isStandardOutputScript(List<byte[]> chunks) {
      if (chunks.size() != 5) {
         return false;
      }
      if (!BccScript.isOP(chunks.get(0), OP_DUP)) {
         return false;
      }
      if (!BccScript.isOP(chunks.get(1), OP_HASH160)) {
         return false;
      }
      if (chunks.get(2).length != 20) {
         return false;
      }
      if (!BccScript.isOP(chunks.get(3), OP_EQUALVERIFY)) {
         return false;
      }
      if (!BccScript.isOP(chunks.get(4), OP_CHECKSIG)) {
         return false;
      }
      return true;
   }

   /**
    * Get the address that this output is for.
    * 
    * @return The address that this output is for.
    */
   public byte[] getAddress() {
      return _chunks.get(2);
   }

}
