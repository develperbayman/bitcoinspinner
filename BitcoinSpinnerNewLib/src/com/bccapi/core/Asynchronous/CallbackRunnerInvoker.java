package com.bccapi.core.Asynchronous;

/**
 * Interface implemented by sub-classes of {@link AsynchronousAccount} to invoke runnable instances in the caller's
 * thread. This is core to how call-backs are implemented.
 */
public interface CallbackRunnerInvoker {
	/**
	 * Invoke the runnable in the caller's thread.
	 * 
	 * @param runnable
	 *            The runnable to run.
	 */
	public void invoke(Runnable runnable);
}
