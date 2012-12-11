package com.bccapi.ng.async;

/**
 * Task object returned when calling asynchronous functions with {@link AsynchronousApi}.
 */
public interface AsyncTask {

	/**
	 * Call this method to cancel call-backs. Has no effect if the call-back has already occurred.
	 */
	void cancel();
}
