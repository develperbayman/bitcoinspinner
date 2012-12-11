package com.bccapi.ng.api;

import com.bccapi.bitlib.util.ByteReader;
import com.bccapi.bitlib.util.ByteReader.InsufficientBytesException;
import com.bccapi.bitlib.util.ByteWriter;

public class QueryBalanceResponse extends ApiObject {

   /**
    * Current balance.
    */
   public Balance balance;

   public QueryBalanceResponse(Balance balance) {
      this.balance = balance;
   }

   protected QueryBalanceResponse(ByteReader reader) throws InsufficientBytesException, ApiException {
      balance = ApiObject.deserialize(Balance.class, reader);
      // Payload may contain more, but we ignore it for forwards
      // compatibility
   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      balance.serialize(writer);
      return writer;
   }

   @Override
   protected byte getType() {
      return ApiObject.BALANCE_RESPONSE_TYPE;
   }
}
