package ar.com.asanteit.bundler;

/**
 * Root of the hierarchy of exceptions thrown by {@link Bundler}.
 *
 * @author mschonaker
 */
public abstract class BundlerException extends RuntimeException {

	private static final long serialVersionUID = 6358465035725238374L;

	BundlerException() {
		super();
	}

	BundlerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	BundlerException(String message, Throwable cause) {
		super(message, cause);
	}

	BundlerException(String message) {
		super(message);
	}

	BundlerException(Throwable cause) {
		super(cause);
	}

}
