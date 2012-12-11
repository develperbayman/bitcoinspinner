package com.bccapi.ng.api;

import com.bccapi.bitlib.model.Address;
import com.bccapi.bitlib.util.ByteReader;
import com.bccapi.bitlib.util.ByteReader.InsufficientBytesException;
import com.bccapi.bitlib.util.ByteWriter;
import com.bccapi.bitlib.util.Sha256Hash;

public class TransactionSummary extends ApiObject implements Comparable<TransactionSummary> {

   public static class Item {
      public Address address;
      public long value;

      public Item(Address address, long value) {
         this.address = address;
         this.value = value;
      }
   }

   public Sha256Hash hash;
   public int height;
   public int time;
   public Item[] inputs;
   public Item[] outputs;

   public TransactionSummary(Sha256Hash hash, int height, int time, Item[] inputs, Item[] outputs) {
      this.hash = hash;
      this.height = height;
      this.time = time;
      this.inputs = inputs;
      this.outputs = outputs;
   }

   protected TransactionSummary(ByteReader reader) throws InsufficientBytesException {
      hash = reader.getSha256Hash();
      height = reader.getIntLE();
      time = reader.getIntLE();
      inputs = readItems(reader);
      outputs = readItems(reader);
      // Payload may contain more, but we ignore it for forwards
      // compatibility
   }

   private Item[] readItems(ByteReader reader) throws InsufficientBytesException {
      int num = reader.getShortLE();
      Item[] items = new Item[num];
      for (int i = 0; i < num; i++) {
         items[i] = new Item(new Address(reader.getBytes(21)), reader.getLongLE());
      }
      return items;
   }

   private void writeItems(Item[] items, ByteWriter writer) {
      writer.putShortLE((short) items.length);
      for (Item item : items) {
         writer.putBytes(item.address.getAllAddressBytes());
         writer.putLongLE(item.value);
      }
   }

   /**
    * Calculate the number of confirmations on this transaction from the current
    * block height.
    */
   public int calculateConfirmatons(int currentHeight) {
      if (height == -1) {
         return 0;
      } else {
         return currentHeight - height + 1;
      }
   }

   @Override
   public int hashCode() {
      return hash.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (!(obj instanceof TransactionSummary)) {
         return false;
      }
      TransactionSummary other = (TransactionSummary) obj;
      return other.hash.equals(this.hash);
   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      writer.putSha256Hash(hash);
      writer.putIntLE(height);
      writer.putIntLE(time);
      writeItems(inputs, writer);
      writeItems(outputs, writer);
      return writer;
   }

   @Override
   protected byte getType() {
      return ApiObject.TRANSACTION_SUMMARY_TYPE;
   }

   @Override
   public int compareTo(TransactionSummary other) {
      // Make pending transaction have maximum height
      int myHeight = height == -1 ? Integer.MAX_VALUE : height;
      int otherHeight = other.height == -1 ? Integer.MAX_VALUE : other.height;

      if (myHeight < otherHeight) {
         return 1;
      } else if (myHeight > otherHeight) {
         return -1;
      } else {
         // sort by time
         if (time < other.time) {
            return 1;
         } else if (time > other.time) {
            return -1;
         }
         return 0;
      }
   }

}
