package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown when the pooling allocator's concurrency limit is exceeded.
 *
 * <p>This exception is thrown when attempting to create more concurrent instances than the pooling
 * allocator allows. The concurrency limit is configured via {@code instancePoolSize} in the engine
 * configuration.
 *
 * @since 1.1.0
 */
public class PoolConcurrencyLimitException extends ResourceException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new pool concurrency limit exception with the specified message.
   *
   * @param message the error message describing the limit exceeded
   */
  public PoolConcurrencyLimitException(final String message) {
    super(message);
  }

  /**
   * Creates a new pool concurrency limit exception with the specified message and cause.
   *
   * @param message the error message describing the limit exceeded
   * @param cause the underlying cause
   */
  public PoolConcurrencyLimitException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
