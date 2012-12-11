package com.bccapi.ng.api;

import com.bccapi.bitlib.util.ByteReader;
import com.bccapi.bitlib.util.ByteReader.InsufficientBytesException;
import com.bccapi.bitlib.util.ByteWriter;

public class ApiError extends ApiObject {

   public int errorCode;

   public String errorMessage;

   public ApiError(int errorCode, String errorMessage) {
      this.errorCode = errorCode;
      this.errorMessage = errorMessage;
   }

   protected ApiError(ByteReader reader) throws InsufficientBytesException {
      errorCode = reader.getIntLE();
      errorMessage = reader.getString();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Code: ").append(errorCode);
      sb.append(" Message: ").append(errorMessage);
      return sb.toString();
   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      writer.putIntLE(errorCode);
      writer.putString(errorMessage);
      return writer;
   }

   @Override
   public byte getType() {
      return ApiObject.ERROR_TYPE;
   }

}
