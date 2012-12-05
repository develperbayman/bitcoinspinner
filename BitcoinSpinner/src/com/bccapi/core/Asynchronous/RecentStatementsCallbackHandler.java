package com.bccapi.core.Asynchronous;

import com.bccapi.api.AccountStatement;

/**
 * Implement this function to get a callback when {@link AsynchronousAccount.requestRecentStatements} has completed.
 */
public interface RecentStatementsCallbackHandler {
	/**
	 * This function is called when {@link AsynchronousAccount.requestRecentStatements} has completed.
	 * @param statements Recent transaction log or null if an error occurred.
	 * @param errorMessage Contains an error message if an error occurred.
	 */
	public void handleRecentStatementsCallback(AccountStatement statements, String errorMessage);
}
