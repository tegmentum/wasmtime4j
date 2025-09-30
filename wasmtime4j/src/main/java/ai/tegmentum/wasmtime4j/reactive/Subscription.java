package ai.tegmentum.wasmtime4j.reactive;

/**
 * Java 8 compatible subscription interface for controlling reactive streams flow.
 *
 * @since 1.0.0
 */
public interface Subscription {

  /**
   * Request items from the publisher.
   *
   * @param n the number of items to request (must be positive)
   */
  void request(long n);

  /** Cancel this subscription. */
  void cancel();
}
