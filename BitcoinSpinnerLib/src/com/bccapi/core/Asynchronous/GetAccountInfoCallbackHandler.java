package com.bccapi.core.Asynchronous;

import com.bccapi.api.AccountInfo;

/**
 * Implement this function to get a callback when {@link AsynchronousAccount.requestAccountInfo} has completed.
 */
public interface GetAccountInfoCallbackHandler {
	/**
	 * This function is called when {@link AsynchronousAccount.requestAccountInfo} has completed.
	 * @param info The current {@link AccountInfo} or null if an error occurred.
	 * @param errorMessage Contains an error message if an error occurred.
	 */
	public void handleGetAccountInfoCallback(AccountInfo info, String errorMessage);
}
