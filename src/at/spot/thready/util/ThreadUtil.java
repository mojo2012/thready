package at.spot.thready.util;

public class ThreadUtil {
	/**
	 * Suspends the current thread for the given amount of seconds. Exceptions
	 * are swallowed.
	 */
	public static void sleep(final int seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (final InterruptedException e) {
		}
	}
}
