package com.bccapi.ng.api;

public class ApiException extends Exception {
   private static final long serialVersionUID = 1L;

   public int errorCode;

   public ApiException(ApiError apiError) {
      super(apiError.errorMessage);
      errorCode = apiError.errorCode;
   }

   public ApiException(int errorCode, String errorMessage) {
      super(errorMessage);
      this.errorCode = errorCode;
   }

}
