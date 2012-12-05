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
 * This class handles Transaction input scripts for p2p payments
 */
public class BccScriptStandardInput extends BccScriptInput {

   public BccScriptStandardInput(byte[] signature, byte[] pubkey) {
      addChunk(signature);
      addChunk(pubkey);
   }

   protected BccScriptStandardInput(List<byte[]> chunks) {
      super(chunks);
   }

   protected static boolean isStandardInputScript(List<byte[]> chunks) {
      if (chunks.size() != 2) {
         return false;
      }
      // XXX going forward we may want to add more checks here
      return true;
   }

   /**
    * Get the signature contained in the script.
    * 
    * @return The signature contained in the script.
    */
   public byte[] getSignature() {
      return _chunks.get(0);
   }

   /**
    * Get the public key contained in the script.
    * 
    * @return The public key contained in the script.
    */
   public byte[] getPublicKey() {
      return _chunks.get(1);
   }

}
