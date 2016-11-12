package at.spot.thready;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Async<R, P> {

	static final private Map<Long, Queue<Object>> messageQueueMap = new ConcurrentHashMap<>();
	static final private Map<Long, AsyncCallback<?>> callbackMap = new ConcurrentHashMap<>();
	static final private Map<Long, List<RunnableThread>> runnableThreads = new ConcurrentHashMap<>();

	/**
	 * The first call of the {@link #run()} method creates a message queue for
	 * the current thread. The {@link AsyncRunnable} is then started in a new
	 * thread and runs until it is finished. It stores its return value in the
	 * message queue.<br/>
	 * The calling thread can use the {@link #await()} method to start the
	 * message handling loop.
	 */
	public static <RETURNVALUE, PARAMTERTYPE> void run(final AsyncRunnable<RETURNVALUE, PARAMTERTYPE> action,
			final AsyncCallback<RETURNVALUE> callback, final PARAMTERTYPE actonArgument) {

		final long threadId = Thread.currentThread().getId();

		getMessageQueue(threadId);
		callbackMap.put(threadId, callback);

		final RunnableThread runnableThread = new RunnableThread(threadId) {
			@Override
			public void run() {
				final RETURNVALUE o = action.doJob(actonArgument);

				final Queue<Object> queue = messageQueueMap.get(getCallbackThreadId());

				synchronized (queue) {
					queue.add(o);
					queue.notify();
				}

				runnableThreads.get(threadId).remove(this);
			}
		};

		getRunnableThreads(threadId).add(runnableThread);

		runnableThread.start();
	}

	/**
	 * Starts the message loop until the finish condition returns true. The
	 * {@link AsyncRunnable} threads may still continue to run and at the end
	 * they add their return value to the message queue. The original thread can
	 * start to process the queue at any time again, either with the default
	 * condition or another custom finish condition.
	 */
	public static synchronized void await(final FinishCondition finishCondition) throws AsyncQueueException {
		final long threadId = getCurrentThreadId();

		// gets the current thread message queue
		final Queue<Object> currentQueue = getMessageQueue(threadId);

		// synchronize on the queue so we can use the wait/notify mechanism
		synchronized (currentQueue) {
			// get the async callback for that thread
			final AsyncCallback callback = callbackMap.get(threadId);

			while (!finishCondition.allFinished()) {
				try {
					currentQueue.wait();
				} catch (final InterruptedException e) {
					throw new AsyncQueueException("Could not start watching the thread message queue", e);
				}

				final Object q = currentQueue.poll();

				if (q != null) {
					// execute callback in original thread Callback
					callback.callback(q);
				}
			}
		}
	}

	/**
	 * This starts the message handling loop and waits for all
	 * {@link AsyncRunnable}s to finish. Then it continues.
	 */
	public static synchronized void await() throws AsyncQueueException {
		final long threadId = getCurrentThreadId();

		await(() -> {
			return getRunnableThreads(threadId).size() == 0;
		});
	}

	protected static long getCurrentThreadId() {
		return Thread.currentThread().getId();
	}

	protected static List<RunnableThread> getRunnableThreads(final long threadId) {
		List<RunnableThread> threads = runnableThreads.get(threadId);

		if (threads == null) {
			threads = new ArrayList<>();
			runnableThreads.put(threadId, threads);
		}

		return threads;
	}

	protected static Queue<Object> getMessageQueue(final long threadId) {
		Queue<Object> queue = messageQueueMap.get(threadId);

		if (queue == null) {
			queue = new ConcurrentLinkedQueue<>();
			messageQueueMap.put(threadId, queue);
		}

		return queue;
	}

	/**
	 * Internal thread runnable that adds the {@link AsyncRunnable}'s result
	 * into the original thread's message queue.
	 *
	 */
	private static abstract class RunnableThread extends Thread {
		private final Long callbackThreadId;

		public RunnableThread(final Long callbackThreadId) {
			this.callbackThreadId = callbackThreadId;
		}

		protected Long getCallbackThreadId() {
			return this.callbackThreadId;
		}

		@Override
		public abstract void run();
	}
}
