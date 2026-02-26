package ai.tegmentum.wasmtime4j.component;

/**
 * A direct source that provides synchronous access to values in addition to async reads.
 *
 * <p>DirectSource extends {@link Source} with the ability to provide values directly without going
 * through the asynchronous read path. This is useful for sources that already have values available
 * in memory.
 *
 * @since 1.1.0
 */
public interface DirectSource extends Source {

  /**
   * Directly reads available values into the buffer without suspending.
   *
   * <p>Returns the number of values read. If no values are immediately available, returns 0 without
   * blocking.
   *
   * @param buffer the buffer to read values into
   * @return the number of values read
   * @throws IllegalArgumentException if buffer is null
   */
  int readDirect(VecBuffer buffer);
}
