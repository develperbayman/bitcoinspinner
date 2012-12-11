package com.bccapi.bitlib.model;

import com.bccapi.bitlib.util.HashUtils;

public class ScriptOutputPubkey extends ScriptOutput {

   protected ScriptOutputPubkey(byte[][] chunks) {
      super(chunks);
   }

   protected static boolean isScriptOutputPubkey(byte[][] chunks) {
      if (chunks.length != 2) {
         return false;
      }
      if (!Script.isOP(chunks[1], OP_CHECKSIG)) {
         return false;
      }
      return true;
   }

   /**
    * Get the public key bytes that this output is for.
    * 
    * @return The public key bytes that this output is for.
    */
   public byte[] getPublicKeyBytes() {
      return _chunks[0];
   }

   @Override
   public Address getAddress(NetworkParameters network) {
      byte[] addressBytes = HashUtils.addressHash(getPublicKeyBytes());
      return Address.fromStandardBytes(addressBytes, network);
   }

}
