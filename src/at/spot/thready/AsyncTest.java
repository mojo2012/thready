package at.spot.thready;

import java.util.Random;

import at.spot.thready.util.ThreadUtil;

public class AsyncTest {
	public static void main(final String... args) throws AsyncQueueException {

		System.out.println("Starting threads from thread " + Thread.currentThread().getId());

		// submit two different runnable threads and wait for their return
		// runnable and callback can be written as lambdas
		// Async.run(runnable, callback, "test");
		// Async.run(runnable, callback, "3");
		//
		// Async.run(runnable2, callback, 777l);
		// Async.run(runnable2, callback, 333l);

		// the thread waits till all async runnables are finished.
		Async.await();

		System.out.println("All runnables finished");

		final long currentThreadId = Thread.currentThread().getId();

		final Thread testThread = new Thread(new Runnable() {
			@Override
			public void run() {
				// ThreadUtil.sleep(3);
				Async.handleMessage("Generic thread test", currentThreadId, callback);
			}
		});

		testThread.start();

		// fake some other work in this thread
		ThreadUtil.sleep(3);

		Async.loop();

		System.out.println("All external threads finished");
	}

	/**
	 * This is the worker thread's runnable. We fake some work here.
	 */
	static final AsyncRunnable<String, String> runnable = new AsyncRunnable<String, String>() {
		@Override
		public String doJob(final String actonArgument) {
			// fake some async work here
			final Random r = new Random(Thread.currentThread().getId() * 100);
			final int randomNum = r.nextInt(10 - 1 + 1) + 1;

			System.out.println(
					"run from thread " + Thread.currentThread().getId() + ", waiting " + randomNum + " seconds ...");

			ThreadUtil.sleep(randomNum);

			// return the work
			return actonArgument + " done";
		}
	};

	/**
	 * This is the worker thread's runnable. We fake some work here.
	 */
	static final AsyncRunnable<String, Long> runnable2 = new AsyncRunnable<String, Long>() {
		@Override
		public String doJob(final Long actonArgument) {
			// fake some async work here
			final Random r = new Random(Thread.currentThread().getId() * 100);
			final int randomNum = r.nextInt(10 - 1 + 1) + 1;

			System.out.println(
					"run from thread " + Thread.currentThread().getId() + ", waiting " + randomNum + " seconds ...");

			ThreadUtil.sleep(randomNum);

			// return the work
			return actonArgument.toString() + " is an awesome number";
		}
	};

	/**
	 * This is the callback. It is being called in the main thread after the
	 * worker runnable has finished its job.
	 */
	static final AsyncCallback<String> callback = new AsyncCallback<String>() {
		@Override
		public void callback(final String returnValue) {
			// print out the async work return value
			System.out.println("main from thread " + Thread.currentThread().getId() + ": " + returnValue.toString());
		}
	};
}
