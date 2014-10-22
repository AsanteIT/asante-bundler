package ar.com.asanteit.bundler;

/**
 * A runtime wrapper for any {@link Exception} that could happen at runtime.
 *
 * @author mschonaker
 */
public class BundlerSQLException extends BundlerException {

	private static final long serialVersionUID = -8181697803715554686L;

	BundlerSQLException(Throwable cause) {
		super(cause);
	}

}
