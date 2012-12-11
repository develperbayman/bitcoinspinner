package com.bccapi.ng.api;

import java.util.LinkedList;
import java.util.List;

import com.bccapi.bitlib.util.ByteReader;
import com.bccapi.bitlib.util.ByteReader.InsufficientBytesException;
import com.bccapi.bitlib.util.ByteWriter;
import com.bccapi.bitlib.util.Sha256Hash;

public class QueryTransactionSummaryRequest extends ApiObject {
   // The maximum number of transaction IDs to query. No more than this number
   // of transactions can
   // be queried.
   public static final int MAXIMUM_TRANSACTIONS = 100;

   public List<Sha256Hash> transactionHashes;

   public QueryTransactionSummaryRequest(List<Sha256Hash> transactionHashes) {
      this.transactionHashes = transactionHashes;
   }

   protected QueryTransactionSummaryRequest(ByteReader reader) throws InsufficientBytesException {
      transactionHashes = new LinkedList<Sha256Hash>();
      int num = reader.getIntLE();
      for (int i = 0; i < num; i++) {
         Sha256Hash hash = reader.getSha256Hash();
         transactionHashes.add(hash);
      }
      // Payload may contain more, but we ignore it for forwards
      // compatibility
   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      writer.putIntLE(transactionHashes.size());
      for (Sha256Hash hash : transactionHashes) {
         writer.putSha256Hash(hash);
      }
      return writer;
   }

   @Override
   protected byte getType() {
      return ApiObject.TRANSACTION_SUMMARY_REQUEST_TYPE;
   }

}
