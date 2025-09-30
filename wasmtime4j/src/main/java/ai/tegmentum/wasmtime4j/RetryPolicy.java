package ai.tegmentum.wasmtime4j;

/**
 * Retry policy for failed network requests during streaming compilation.
 *
 * <p>RetryPolicy defines different strategies for retrying failed HTTP requests during network
 * streaming operations.
 *
 * @since 1.0.0
 */
public enum RetryPolicy {
  /**
   * No retry - fail immediately on the first error.
   *
   * <p>Use this policy for time-critical applications where retry delays are not acceptable.
   */
  NO_RETRY,

  /**
   * Linear backoff retry with fixed intervals.
   *
   * <p>Retries with fixed delays between attempts (e.g., 1s, 2s, 3s, 4s, ...).
   */
  LINEAR_BACKOFF,

  /**
   * Exponential backoff retry with increasing intervals.
   *
   * <p>Retries with exponentially increasing delays (e.g., 1s, 2s, 4s, 8s, ...). This is the
   * recommended policy for most applications.
   */
  EXPONENTIAL_BACKOFF,

  /**
   * Exponential backoff with jitter to avoid thundering herd.
   *
   * <p>Similar to exponential backoff but adds random jitter to prevent multiple clients from
   * retrying simultaneously.
   */
  EXPONENTIAL_BACKOFF_WITH_JITTER,

  /**
   * Custom retry policy with user-defined parameters.
   *
   * <p>Allows applications to define custom retry behavior through configuration parameters.
   */
  CUSTOM
}
