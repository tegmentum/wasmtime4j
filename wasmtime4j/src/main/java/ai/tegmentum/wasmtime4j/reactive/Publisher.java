package ai.tegmentum.wasmtime4j.reactive;

/**
 * Java 8 compatible publisher interface for reactive streams.
 *
 * <p>This interface provides a minimal reactive streams implementation that is compatible with Java
 * 8, avoiding the use of java.util.concurrent.Flow which was introduced in Java 9.
 *
 * @param <T> the type of elements published
 * @since 1.0.0
 */
public interface Publisher<T> {

  /**
   * Request subscription to this publisher.
   *
   * @param subscriber the subscriber to subscribe
   */
  void subscribe(Subscriber<? super T> subscriber);
}
