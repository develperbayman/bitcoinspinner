package com.bccapi.bitlib.model;

import java.io.UnsupportedEncodingException;

import com.bccapi.bitlib.util.HashUtils;

public class ScriptOutputMsg extends ScriptOutput {

   protected ScriptOutputMsg(byte[][] chunks) {
      super(chunks);
   }

   protected static boolean isScriptOutputMsg(byte[][] chunks) {
      if (chunks.length != 4) {
         return false;
      }
      if (!Script.isOP(chunks[1], OP_DROP)) {
         return false;
      }
      if (!Script.isOP(chunks[3], OP_CHECKSIG)) {
         return false;
      }
      return true;
   }

   /**
    * Get the bytes for the message contained in this output.
    * 
    * @return The message bytes of this output.
    */
   public byte[] getMessageBytes() {
      return _chunks[0];
   }

   public String getMessage() {
      try {
         return new String(getMessageBytes(), "US-ASCII");
      } catch (UnsupportedEncodingException e) {
         return "";
      }
   }

   /**
    * Get the public key bytes that this output is for.
    * 
    * @return The public key bytes that this output is for.
    */
   public byte[] getPublicKeyBytes() {
      return _chunks[2];
   }

   @Override
   public Address getAddress(NetworkParameters network) {
      byte[] addressBytes = HashUtils.addressHash(getPublicKeyBytes());
      return Address.fromStandardBytes(addressBytes, network);
   }

}
