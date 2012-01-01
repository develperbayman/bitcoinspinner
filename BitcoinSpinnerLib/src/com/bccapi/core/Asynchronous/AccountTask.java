package com.bccapi.core.Asynchronous;

/**
 * Task object returned when calling asynchronous functions with {@link AsynchronousAccount}.
 */
public interface AccountTask {

	/**
	 * Call this method to cancel call-backs. Has no effect if the call-back has already occurred.
	 */
	void cancel();
}
