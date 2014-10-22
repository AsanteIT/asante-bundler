package ar.com.asanteit.bundler;

/**
 * Thrown when some validation fails.
 *
 * @author mschonaker
 */
public class BundlerValidationException extends BundlerException {

	private static final long serialVersionUID = 6403815195239035313L;

	BundlerValidationException(String message) {
		super(message);
	}
}
