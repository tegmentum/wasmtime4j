package ai.tegmentum.wasmtime4j.concurrent;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Provides concurrent access to store data from multiple threads.
 *
 * <p>The Accessor type enables safe concurrent access to Store data when running multiple
 * WebAssembly guests concurrently. It allows read-only access to user data while ensuring proper
 * synchronization.
 *
 * <p>This corresponds to wasmtime's {@code Accessor<T>} type which provides concurrent access to a
 * Store's user data from within host functions running on multiple threads.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * store.runConcurrent(accessor -> {
 *     // Access store data safely from multiple threads
 *     MyData data = accessor.getData(MyData.class);
 *     // ... process data ...
 *     return result;
 * });
 * }</pre>
 *
 * @param <T> the type of user data stored in the associated Store
 * @since 1.0.0
 */
public interface Accessor<T> {

  /**
   * Gets the user data associated with the store.
   *
   * <p>This provides read-only access to the store's user data. The data is accessed in a
   * thread-safe manner.
   *
   * @return the user data, or null if no data was set
   * @since 1.0.0
   */
  T getData();

  /**
   * Gets the user data with a specific type cast.
   *
   * <p>This is a convenience method that casts the user data to the specified type.
   *
   * @param <U> the expected type of the user data
   * @param clazz the class to cast to
   * @return the user data cast to the specified type
   * @throws ClassCastException if the data cannot be cast to the specified type
   * @since 1.0.0
   */
  @SuppressWarnings("unchecked")
  default <U> U getData(final Class<U> clazz) {
    Object data = getData();
    if (data == null) {
      return null;
    }
    return clazz.cast(data);
  }

  /**
   * Checks if the accessor is still valid for use.
   *
   * <p>An accessor becomes invalid when the associated store is closed or when the concurrent
   * operation completes.
   *
   * @return true if the accessor is valid, false otherwise
   * @since 1.0.0
   */
  boolean isValid();

  /**
   * Gets the identifier of the associated store.
   *
   * <p>This can be used to correlate accessors with their parent stores.
   *
   * @return the store identifier
   * @since 1.0.0
   */
  long getStoreId();

  /**
   * Signals that the concurrent operation has completed successfully.
   *
   * <p>This method is typically called internally when the concurrent operation finishes. After
   * this call, the accessor becomes invalid.
   *
   * @throws WasmException if signaling completion fails
   * @since 1.0.0
   */
  void complete() throws WasmException;

  /**
   * Signals that the concurrent operation has failed.
   *
   * <p>This method is called when an error occurs during concurrent execution. After this call, the
   * accessor becomes invalid.
   *
   * @param error the error that caused the failure
   * @throws WasmException if signaling failure fails
   * @since 1.0.0
   */
  void fail(final Throwable error) throws WasmException;
}
