package com.bccapi.ng.api;

import java.util.ArrayList;
import java.util.List;

import com.bccapi.bitlib.model.Address;
import com.bccapi.bitlib.util.ByteReader;
import com.bccapi.bitlib.util.ByteReader.InsufficientBytesException;
import com.bccapi.bitlib.util.ByteWriter;
import com.bccapi.bitlib.util.StringUtils;

public class QueryBalanceRequest extends ApiObject {

   public List<Address> addresses;

   public QueryBalanceRequest(List<Address> addresses) {
      this.addresses = addresses;
   }

   public QueryBalanceRequest(Address address) {
      this.addresses = new ArrayList<Address>(1);
      this.addresses.add(address);
   }

   protected QueryBalanceRequest(ByteReader reader) throws InsufficientBytesException {
      int num = reader.getIntLE();
      addresses = new ArrayList<Address>(num);
      for (int i = 0; i < num; i++) {
         byte[] addressBytes = reader.getBytes(21);
         addresses.add(new Address(addressBytes));
      }
      // Payload may contain more, but we ignore it for forwards
      // compatibility
   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      writer.putIntLE(addresses.size());
      for (Address address : addresses) {
         writer.putBytes(address.getAllAddressBytes());
      }
      return writer;
   }

   @Override
   protected byte getType() {
      return ApiObject.BALANCE_REQUEST_TYPE;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append('[');
      sb.append(StringUtils.join(addresses.toArray(), ", "));
      sb.append(']');
      return sb.toString();
   }

}
