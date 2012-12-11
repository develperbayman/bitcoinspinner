package com.bccapi.ng.async;

import com.bccapi.ng.api.ApiError;

/**
 * Implement this function to get a callbacks when calls to
 * {@link AsynchronousApi} have completed.
 */
public interface AbstractCallbackHandler<T> {
   /**
    * This function is called when an {@link AsynchronousApi} function has
    * completed.
    * 
    * @param response
    *           The response received from the server or null if an error
    *           occurred.
    * @param error
    *           Is null unless an error occurred.
    */
   public void handleCallback(T response, ApiError error);
}
