package com.bccapi.ng.api;

import java.util.LinkedList;
import java.util.List;

import com.bccapi.bitlib.util.ByteReader;
import com.bccapi.bitlib.util.ByteReader.InsufficientBytesException;
import com.bccapi.bitlib.util.ByteWriter;

public class QueryTransactionSummaryResponse extends ApiObject {

   /**
    * Transaction summaries
    */
   public List<TransactionSummary> transactions;
   public int chainHeight;

   public QueryTransactionSummaryResponse(List<TransactionSummary> transactions, int chainHeight) {
      this.transactions = transactions;
      this.chainHeight = chainHeight;
   }

   protected QueryTransactionSummaryResponse(ByteReader reader) throws InsufficientBytesException, ApiException {
      int size = reader.getIntLE();
      transactions = new LinkedList<TransactionSummary>();
      for (int i = 0; i < size; i++) {
         transactions.add(ApiObject.deserialize(TransactionSummary.class, reader));
      }
      chainHeight = reader.getIntLE();
      // Payload may contain more, but we ignore it for forwards
      // compatibility
   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      writer.putIntLE(transactions.size());
      for (TransactionSummary item : transactions) {
         item.serialize(writer);
      }
      writer.putIntLE(chainHeight);
      return writer;
   }

   @Override
   protected byte getType() {
      return ApiObject.TRANSACTION_SUMMARY_RESPONSE_TYPE;
   }

}
