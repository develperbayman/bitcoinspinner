package com.bccapi.core.Asynchronous;

import com.bccapi.api.SendCoinForm;

/**
 * Implement this function to get a callback when {@link AsynchronousAccount.requestSendCoinForm} has completed.
 */
public interface GetSendCoinFormCallbackHandler {
	/**
	 * This function is called when {@link AsynchronousAccount.requestSendCoinForm} has completed.
	 * @param form The {@link SendCoinForm}obtained or null if an error occurred.
	 * @param errorMessage Contains an error message if an error occurred.
	 */
	public void handleGetSendCoinFormCallback(SendCoinForm form, String errorMessage);
}
