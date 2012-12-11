package com.bccapi.ng.api;

import java.util.ArrayList;
import java.util.List;

import com.bccapi.bitlib.model.Address;
import com.bccapi.bitlib.util.ByteReader;
import com.bccapi.bitlib.util.ByteReader.InsufficientBytesException;
import com.bccapi.bitlib.util.ByteWriter;
import com.bccapi.bitlib.util.StringUtils;

public class QueryTransactionInventoryRequest extends ApiObject {
   // The maximum value for limit. No more than this number of transactions can
   // be retrieved
   public static final int MAXIMUM = 100;

   public List<Address> addresses;
   public int limit;

   public QueryTransactionInventoryRequest(List<Address> addresses, int limit) {
      this.addresses = addresses;
      this.limit = limit;
   }

   public QueryTransactionInventoryRequest(Address address, int limit) {
      this.addresses = new ArrayList<Address>(1);
      this.addresses.add(address);
      this.limit = limit;
   }

   protected QueryTransactionInventoryRequest(ByteReader reader) throws InsufficientBytesException {
      int num = reader.getIntLE();
      addresses = new ArrayList<Address>(num);
      for (int i = 0; i < num; i++) {
         byte[] addressBytes = reader.getBytes(21);
         addresses.add(new Address(addressBytes));
      }
      limit = reader.getIntLE();
      // Payload may contain more, but we ignore it for forwards
      // compatibility
   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      writer.putIntLE(addresses.size());
      for (Address address : addresses) {
         writer.putBytes(address.getAllAddressBytes());
      }
      writer.putIntLE(limit);
      return writer;
   }

   @Override
   protected byte getType() {
      return ApiObject.TRANSACTION_INVENTORY_REQUEST_TYPE;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append('[').append(StringUtils.join(addresses.toArray(), ", ")).append(']');
      sb.append(", ").append(limit);
      return sb.toString();
   }

}
