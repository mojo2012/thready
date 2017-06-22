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

	/**
	 * Returns the thread id of the thread with the given thread name. If no
	 * thread is found, -1 is returned.
	 * 
	 * @param threadName
	 * @return
	 */
	public static long getThreadIdByName(final String threadName) {
		final Thread thread = getThreadByName(threadName);
		final long threadId = (thread != null ? thread.getId() : -1);

		return threadId;
	}

	/**
	 * Returns the thread with the given name. If no thread is found, null is
	 * returned.
	 * 
	 * @param threadName
	 * @return
	 */
	public static Thread getThreadByName(final String threadName) {
		for (final Thread t : Thread.getAllStackTraces().keySet()) {
			if (t.getName().equals(threadName)) {
				return t;
			}
		}

		return null;
	}

	/**
	 * Returns the thread with the given thread id. If no thread is found, null
	 * is returned.
	 * 
	 * @param threadId
	 * @return
	 */
	public static Thread getThreadByID(final long threadId) {
		for (final Thread t : Thread.getAllStackTraces().keySet()) {
			if (Long.compare(t.getId(), threadId) == 0) {
				return t;
			}
		}

		return null;
	}
}
