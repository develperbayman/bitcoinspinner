package com.bccapi.core.Asynchronous;

import com.bccapi.api.Tx;

/**
 * Implement this function to get a callback when {@link AsynchronousAccount.requestTransactionSubmission} has
 * completed.
 */
public interface TransactionSubmissionCallbackHandler {
	/**
	 * This function is called when {@link AsynchronousAccount.requestTransactionSubmission} has completed.
	 * 
	 * @param transaction
	 *            The transaction sent or null if an error occurred.
	 * @param errorMessage
	 *            Contains an error message if an error occurred.
	 */
	public void handleTransactionSubmission(Tx transaction, String errorMessage);
}
