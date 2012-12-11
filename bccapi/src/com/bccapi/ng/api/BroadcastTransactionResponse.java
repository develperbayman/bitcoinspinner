package com.bccapi.ng.api;

import com.bccapi.bitlib.util.ByteReader;
import com.bccapi.bitlib.util.ByteReader.InsufficientBytesException;
import com.bccapi.bitlib.util.ByteWriter;
import com.bccapi.bitlib.util.Sha256Hash;

public class BroadcastTransactionResponse extends ApiObject {

   /**
    * The hash of the broadcasted transaction
    */
   public Sha256Hash hash;

   public BroadcastTransactionResponse(Sha256Hash hash) {
      this.hash = hash;
   }

   protected BroadcastTransactionResponse(ByteReader reader) throws InsufficientBytesException {
      hash = reader.getSha256Hash();
      // Payload may contain more, but we ignore it for forwards
      // compatibility
   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      writer.putSha256Hash(hash);
      return writer;
   }

   @Override
   protected byte getType() {
      return ApiObject.BROADCAST_TRANSACTION_RESPONSE_TYPE;
   }
}
