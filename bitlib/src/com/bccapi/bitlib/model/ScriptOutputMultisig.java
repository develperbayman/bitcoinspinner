package com.bccapi.bitlib.model;

public class ScriptOutputMultisig extends ScriptOutput {

   protected ScriptOutputMultisig(byte[][] chunks) {
      super(chunks);
   }

   protected static boolean isScriptOutputMultisig(byte[][] chunks) {
      if (chunks.length != 3) {
         return false;
      }
      if (!Script.isOP(chunks[0], OP_HASH160)) {
         return false;
      }
      if (chunks[1].length != 20) {
         return false;
      }
      if (!Script.isOP(chunks[2], OP_EQUAL)) {
         return false;
      }
      return true;
   }

   public ScriptOutputMultisig(byte[] addressBytes) {
      super(new byte[][] { { (byte) OP_HASH160 }, addressBytes, { (byte) OP_EQUAL } });
   }

   /**
    * Get the raw multisig address that this output is for.
    * 
    * @return The raw multisig address that this output is for.
    */
   public byte[] getMultisigAddressBytes() {
      return _chunks[1];
   }

   @Override
   public Address getAddress(NetworkParameters network) {
      byte[] addressBytes = getMultisigAddressBytes();
      return Address.fromMultisigBytes(addressBytes, network);
   }

}
