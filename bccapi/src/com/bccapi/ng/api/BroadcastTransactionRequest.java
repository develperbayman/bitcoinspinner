package com.bccapi.ng.api;

import com.bccapi.bitlib.model.Transaction;
import com.bccapi.bitlib.model.Transaction.TransactionParsingException;
import com.bccapi.bitlib.util.ByteReader;
import com.bccapi.bitlib.util.ByteReader.InsufficientBytesException;
import com.bccapi.bitlib.util.ByteWriter;

public class BroadcastTransactionRequest extends ApiObject {

   public Transaction transaction;

   public BroadcastTransactionRequest(Transaction transaction) {
      this.transaction = transaction;
   }

   protected BroadcastTransactionRequest(ByteReader reader) throws InsufficientBytesException, ApiException {
      try {
         transaction = Transaction.fromByteReader(reader);
      } catch (TransactionParsingException e) {
         throw new ApiException(BitcoinClientApi.ERROR_CODE_PARSER_ERROR, "Unable to parse API object");
      }
      // Payload may contain more, but we ignore it for forwards
      // compatibility
   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      transaction.toByteWriter(writer);
      return writer;
   }

   @Override
   protected byte getType() {
      return ApiObject.BROADCAST_TRANSACTION_REQUEST_TYPE;
   }

}
