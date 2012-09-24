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
public class BccScriptMultisigOutput extends BccScriptOutput {

   public BccScriptMultisigOutput(String multisigAddress) throws BccScriptException {
      byte[] addressBytes = AddressUtil.StringAddressToByteAddress(multisigAddress);
      if (addressBytes == null) {
         throw new BccScriptException("Invalid address");
      }
      addOpcode(OP_HASH160);
      addChunk(addressBytes);
      addOpcode(OP_EQUAL);
   }

   protected BccScriptMultisigOutput(List<byte[]> chunks) {
      super(chunks);
   }

   protected static boolean isMultisigScript(List<byte[]> chunks) {
      if (chunks.size() != 3) {
         return false;
      }
      if (!BccScript.isOP(chunks.get(0), OP_HASH160)) {
         return false;
      }
      if (chunks.get(1).length != 20) {
         return false;
      }
      if (!BccScript.isOP(chunks.get(2), OP_EQUAL)) {
         return false;
      }
      return true;
   }

   /**
    * Get the raw multisig address that this output is for.
    * 
    * @return The raw multisig address that this output is for.
    */
   public byte[] getMultisigAddress() {
      return _chunks.get(1);
   }

}
