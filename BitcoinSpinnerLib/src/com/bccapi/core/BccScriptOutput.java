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

/**
 * This class handles Transaction output scripts for p2p payments
 */
public class BccScriptOutput extends BccScript {

   private byte[] _address;

   public BccScriptOutput(PublicECKey key) {
      byte[] address = key.getPublicKeyHash();
      addOpcode(OP_DUP);
      addOpcode(OP_HASH160);
      addChunk(address);
      addOpcode(OP_EQUALVERIFY);
      addOpcode(OP_CHECKSIG);
      _address = address;
   }

   public BccScriptOutput(byte[] script) throws BccScriptException {
      super(script);
      if (_chunks.size() != 5) {
         throw new BccScriptException("ScriptSig needs two chunks");
      }
      if (_chunks.get(0).length != 1 && (0xFF & (int) _chunks.get(0)[0]) != OP_DUP) {
         throw new BccScriptException();
      }
      if (_chunks.get(1).length != 1 && (0xFF & (int) _chunks.get(1)[0]) != OP_HASH160) {
         throw new BccScriptException();
      }
      _address = _chunks.get(2);
      if (_chunks.get(3).length != 1 && (0xFF & (int) _chunks.get(3)[0]) != OP_EQUALVERIFY) {
         throw new BccScriptException();
      }
      if (_chunks.get(4).length != 1 && (0xFF & (int) _chunks.get(4)[0]) != OP_CHECKSIG) {
         throw new BccScriptException();
      }
   }

   /**
    * Get the address that this output is for.
    * 
    * @return The address that this output is for.
    */
   public byte[] getAddress() {
      return _address;
   }

}
