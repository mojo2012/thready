package at.spot.thready;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Async<R, P> {

	static final private Map<Long, Queue<Payload<?, ?>>> messageQueueMap = new ConcurrentHashMap<>();
	static final private Map<Long, List<AsyncRunnable<?, ?>>> runnableThreads = new ConcurrentHashMap<>();
	static final private Map<AsyncRunnable<?, ?>, AsyncCallback<?>> callbackMap = new ConcurrentHashMap<>();

	/**
	 * Prepares a message queue for the current thread, without starting any
	 * {@link AsyncRunnable}s.
	 */
	public static void prepare() {
		prepare(Thread.currentThread().getId());
	}

	protected static void prepare(final long threadId) {
		getMessageQueue(threadId);
	}

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

		// prepare the message loop
		prepare();

		final Payload<RETURNVALUE, PARAMTERTYPE> config = new Payload<>();
		config.runnable = action;
		config.targetThreadId = threadId;
		config.callback = callback;

		final RunnableThread<RETURNVALUE, PARAMTERTYPE> runnableThread = new RunnableThread<RETURNVALUE, PARAMTERTYPE>(
				config) {
			@Override
			public void run() {
				// execute action and store return value in the payload
				setReturnValue(action.doJob(actonArgument));

				final Queue<Payload<?, ?>> queue = messageQueueMap.get(getCallbackThreadId());

				// add the payload to the message queue
				synchronized (queue) {
					queue.add(getPayload());
					queue.notify();
				}
			}
		};

		getRunnableThreads(threadId).add(action);

		runnableThread.start();
	}

	/**
	 * Allows to inject messages into the message loop of thead with the given
	 * thread id. This thread needs to call the {@link #await()} method to
	 * process the messages.
	 */
	public static <RETURNVALUE, PARAMTYPE> void handleMessage(final RETURNVALUE value, final long threadId,
			final AsyncCallback<RETURNVALUE> callback) {

		final Payload<RETURNVALUE, PARAMTYPE> payload = new Payload<>();
		payload.callback = callback;
		payload.returnValue = value;
		payload.runnable = (n) -> {
			return value;
		};

		getRunnableThreads(threadId).add(payload.runnable);
		getMessageQueue(threadId).add(payload);
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
		final Queue<Payload<?, ?>> currentQueue = getMessageQueue(threadId);

		// synchronize on the queue so we can use the wait/notify mechanism
		synchronized (currentQueue) {
			// get the async callback for that thread

			while (!finishCondition.allFinished()) {
				try {
					if (currentQueue.peek() == null) {
						currentQueue.wait();
					}
				} catch (final InterruptedException e) {
					throw new AsyncQueueException("Could not start watching the thread message queue.", e);
				}

				final Payload payload = currentQueue.poll();

				if (payload != null) {
					runnableThreads.get(threadId).remove(payload.runnable);

					// execute callback in original thread Callback
					payload.callback.callback(payload.returnValue);
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

	protected static List<AsyncRunnable<?, ?>> getRunnableThreads(final long threadId) {
		List<AsyncRunnable<?, ?>> runnable = runnableThreads.get(threadId);

		if (runnable == null) {
			runnable = new ArrayList<>();
			runnableThreads.put(threadId, runnable);
		}

		return runnable;
	}

	protected AsyncCallback<?> getCallbackForRunnable(final AsyncRunnable<?, ?> runnable) {
		final AsyncCallback<?> callback = callbackMap.get(runnable);
		return callback;
	}

	protected static Queue<Payload<?, ?>> getMessageQueue(final long threadId) {
		Queue<Payload<?, ?>> queue = messageQueueMap.get(threadId);

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
	private static abstract class RunnableThread<RETURNTYPE, PARAMETERTYPE> extends Thread {
		private final Payload<RETURNTYPE, PARAMETERTYPE> runnablePackage;

		public RunnableThread(final Payload<RETURNTYPE, PARAMETERTYPE> runnablePackage) {
			this.runnablePackage = runnablePackage;
		}

		protected Long getCallbackThreadId() {
			return this.runnablePackage.targetThreadId;
		}

		protected void setReturnValue(final RETURNTYPE returnValue) {
			runnablePackage.returnValue = returnValue;
		}

		protected Payload<RETURNTYPE, PARAMETERTYPE> getPayload() {
			return this.runnablePackage;
		}

		@Override
		public abstract void run();
	}

	private static class Payload<RETURNTYPE, PARAMETERTYPE> {
		public AsyncRunnable<RETURNTYPE, PARAMETERTYPE> runnable;
		public AsyncCallback<RETURNTYPE> callback;
		public long targetThreadId;
		public RETURNTYPE returnValue;
	}
}
