package ai.tegmentum.wasmtime4j.reactive;

/**
 * Java 8 compatible subscriber interface for reactive streams.
 *
 * @param <T> the type of elements received
 * @since 1.0.0
 */
public interface Subscriber<T> {

  /**
   * Called when subscription is established.
   *
   * @param subscription the subscription for controlling flow
   */
  void onSubscribe(Subscription subscription);

  /**
   * Called when a new item is available.
   *
   * @param item the new item
   */
  void onNext(T item);

  /**
   * Called when an error occurs.
   *
   * @param throwable the error
   */
  void onError(Throwable throwable);

  /** Called when the stream is complete. */
  void onComplete();
}
