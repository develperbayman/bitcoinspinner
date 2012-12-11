package com.bccapi.ng.api;

import java.util.LinkedList;
import java.util.List;

import com.bccapi.bitlib.util.ByteReader;
import com.bccapi.bitlib.util.ByteReader.InsufficientBytesException;
import com.bccapi.bitlib.util.ByteWriter;
import com.bccapi.bitlib.util.Sha256Hash;

public class QueryTransactionInventoryResponse extends ApiObject {

   public static class Item {
      public Sha256Hash hash;
      public int height;

      public Item(Sha256Hash hash, int height) {
         this.hash = hash;
         this.height = height;
      }
   }

   /**
    * Transaction inventory
    */
   public List<Item> transactions;

   /**
    * Current height of the block chain.
    */
   public int chainHeight;

   public QueryTransactionInventoryResponse(List<Item> transactions, int chainHeight) {
      this.transactions = transactions;
      this.chainHeight = chainHeight;
   }

   protected QueryTransactionInventoryResponse(ByteReader reader) throws InsufficientBytesException {
      int size = reader.getIntLE();
      transactions = new LinkedList<Item>();
      for (int i = 0; i < size; i++) {
         Sha256Hash txHash = reader.getSha256Hash();
         int height = reader.getIntLE();
         transactions.add(new Item(txHash, height));
      }
      chainHeight = reader.getIntLE();
      // Payload may contain more, but we ignore it for forwards
      // compatibility
   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      writer.putIntLE(transactions.size());
      for (Item item : transactions) {
         writer.putSha256Hash(item.hash);
         writer.putIntLE(item.height);
      }
      writer.putIntLE(chainHeight);
      return writer;
   }

   @Override
   protected byte getType() {
      return ApiObject.TRANSACTION_INVENTORY_RESPONSE_TYPE;
   }

}
