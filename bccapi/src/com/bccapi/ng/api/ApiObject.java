package com.bccapi.ng.api;

import com.bccapi.bitlib.util.ByteReader;
import com.bccapi.bitlib.util.ByteReader.InsufficientBytesException;
import com.bccapi.bitlib.util.ByteWriter;

public abstract class ApiObject {

   protected static final byte ERROR_TYPE = (byte) 0x01;
   protected static final byte BALANCE_TYPE = (byte) 0x02;
   protected static final byte TRANSACTION_SUMMARY_TYPE = (byte) 0x03;
   protected static final byte BALANCE_REQUEST_TYPE = (byte) 0x04;
   protected static final byte BALANCE_RESPONSE_TYPE = (byte) 0x5;
   protected static final byte UNSPENT_OUTPUTS_REQUEST_TYPE = (byte) 0x06;
   protected static final byte UNSPENT_OUTPUTS_RESPONSE_TYPE = (byte) 0x07;
   protected static final byte TRANSACTION_INVENTORY_REQUEST_TYPE = (byte) 0x08;
   protected static final byte TRANSACTION_INVENTORY_RESPONSE_TYPE = (byte) 0x09;
   protected static final byte TRANSACTION_SUMMARY_REQUEST_TYPE = (byte) 0x10;
   protected static final byte TRANSACTION_SUMMARY_RESPONSE_TYPE = (byte) 0x11;
   protected static final byte BROADCAST_TRANSACTION_REQUEST_TYPE = (byte) 0x12;
   protected static final byte BROADCAST_TRANSACTION_RESPONSE_TYPE = (byte) 0x13;

   public final ByteWriter serialize(ByteWriter writer) {
      byte[] payload = toByteWriter(new ByteWriter(1024)).toBytes();
      writer.put(getType());
      writer.putIntLE(payload.length);
      writer.putBytes(payload);
      return writer;
   }

   private static ApiObject deserialize(ByteReader reader) throws ApiException {
      try {
         byte type = reader.get();
         int length = reader.getIntLE();
         byte[] payload = reader.getBytes(length);
         ByteReader payloadReader = new ByteReader(payload);
         if (type == ERROR_TYPE) {
            return new ApiError(payloadReader);
         } else if (type == BALANCE_TYPE) {
            return new Balance(payloadReader);
         } else if (type == TRANSACTION_SUMMARY_TYPE) {
            return new TransactionSummary(payloadReader);
         } else if (type == BALANCE_REQUEST_TYPE) {
            return new QueryBalanceRequest(payloadReader);
         } else if (type == BALANCE_RESPONSE_TYPE) {
            return new QueryBalanceResponse(payloadReader);
         } else if (type == UNSPENT_OUTPUTS_REQUEST_TYPE) {
            return new QueryUnspentOutputsRequest(payloadReader);
         } else if (type == UNSPENT_OUTPUTS_RESPONSE_TYPE) {
            return new QueryUnspentOutputsResponse(payloadReader);
         } else if (type == TRANSACTION_INVENTORY_REQUEST_TYPE) {
            return new QueryTransactionInventoryRequest(payloadReader);
         } else if (type == TRANSACTION_INVENTORY_RESPONSE_TYPE) {
            return new QueryTransactionInventoryResponse(payloadReader);
         } else if (type == TRANSACTION_SUMMARY_REQUEST_TYPE) {
            return new QueryTransactionSummaryRequest(payloadReader);
         } else if (type == TRANSACTION_SUMMARY_RESPONSE_TYPE) {
            return new QueryTransactionSummaryResponse(payloadReader);
         } else if (type == BROADCAST_TRANSACTION_REQUEST_TYPE) {
            return new BroadcastTransactionRequest(payloadReader);
         } else if (type == BROADCAST_TRANSACTION_RESPONSE_TYPE) {
            return new BroadcastTransactionResponse(payloadReader);
         } else {
            throw new ApiException(BitcoinClientApi.ERROR_CODE_UNKNOWN_TYPE, "Error deserializing server response");
         }
      } catch (InsufficientBytesException e) {
         throw new ApiException(BitcoinClientApi.ERROR_CODE_PARSER_ERROR, "Unable to parse API object");
      }
   }

   @SuppressWarnings("unchecked")
   public static <T> T deserialize(Class<T> klass, ByteReader reader) throws ApiException {
      ApiObject obj = deserialize(reader);
      if (obj.getClass().equals(klass)) {
         return (T) obj;
      } else if (obj.getClass().equals(ApiError.class)) {
         throw new ApiException((ApiError) obj);
      }
      throw new ApiException(BitcoinClientApi.ERROR_CODE_UNKNOWN_TYPE, "Error deserializing server response");
   }

   protected abstract ByteWriter toByteWriter(ByteWriter writer);

   protected abstract byte getType();

}
