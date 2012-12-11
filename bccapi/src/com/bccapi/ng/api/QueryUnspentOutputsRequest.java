package com.bccapi.ng.api;

import java.util.ArrayList;
import java.util.List;

import com.bccapi.bitlib.model.Address;
import com.bccapi.bitlib.util.ByteReader;
import com.bccapi.bitlib.util.ByteReader.InsufficientBytesException;
import com.bccapi.bitlib.util.ByteWriter;

public class QueryUnspentOutputsRequest extends ApiObject {

   public List<Address> addresses;

   public QueryUnspentOutputsRequest(List<Address> addresses) {
      this.addresses = addresses;
   }

   public QueryUnspentOutputsRequest(Address address) {
      this.addresses = new ArrayList<Address>(1);
      this.addresses.add(address);
   }

   protected QueryUnspentOutputsRequest(ByteReader reader) throws InsufficientBytesException {
      int num = reader.getIntLE();
      addresses = new ArrayList<Address>(num);
      for (int i = 0; i < num; i++) {
         byte[] addressBytes = reader.getBytes(21);
         Address address = new Address(addressBytes);
         addresses.add(address);
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
      return ApiObject.UNSPENT_OUTPUTS_REQUEST_TYPE;
   }

}
