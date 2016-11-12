package at.spot.thready;

public class AsyncQueueException extends Exception {

	private static final long serialVersionUID = 1L;

	public AsyncQueueException(String message) {
		super(message);
	}

	public AsyncQueueException(String message, Throwable rootCause) {
		super(message, rootCause);
	}

}
