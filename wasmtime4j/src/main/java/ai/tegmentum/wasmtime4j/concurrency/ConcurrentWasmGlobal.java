package ai.tegmentum.wasmtime4j.concurrency;

import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CompletableFuture;

/**
 * Thread-safe WebAssembly global interface for concurrent access.
 *
 * <p>A ConcurrentWasmGlobal extends the standard WasmGlobal interface with explicit thread safety
 * guarantees for all global variable operations. Multiple threads can safely read and write
 * global values simultaneously with proper synchronization.
 *
 * <p>Key features:
 * <ul>
 *   <li>Thread-safe global value read and write operations
 *   <li>Atomic operations for concurrent access
 *   <li>Compare-and-swap operations for lock-free programming
 *   <li>Asynchronous global operations
 *   <li>Change notification for global value updates
 * </ul>
 *
 * @since 1.0.0
 */
public interface ConcurrentWasmGlobal extends WasmGlobal {

  /**
   * Thread-safe atomic read of the global value.
   *
   * <p>This method provides an atomic snapshot of the global value that is
   * consistent across all threads.
   *
   * @return the current value of the global
   * @throws WasmException if the read operation fails
   */
  Object getAtomic() throws WasmException;

  /**
   * Thread-safe atomic write of the global value.
   *
   * <p>This method atomically updates the global value and ensures the change
   * is immediately visible to all threads.
   *
   * @param value the new value to set
   * @throws WasmException if the write operation fails or value type is invalid
   * @throws IllegalArgumentException if value is null or wrong type
   */
  void setAtomic(final Object value) throws WasmException;

  /**
   * Atomically compares and sets the global value.
   *
   * <p>This method atomically compares the current value with the expected value
   * and sets it to the new value if they match. This is useful for lock-free programming.
   *
   * @param expectedValue the expected current value
   * @param newValue the new value to set
   * @return true if the global was successfully updated, false otherwise
   * @throws WasmException if the operation fails
   * @throws IllegalArgumentException if values are null or wrong type
   */
  boolean compareAndSet(final Object expectedValue, final Object newValue) throws WasmException;

  /**
   * Atomically gets the current value and sets a new value.
   *
   * <p>This method atomically exchanges the global value and returns the previous value.
   *
   * @param newValue the new value to set
   * @return the previous value of the global
   * @throws WasmException if the operation fails
   * @throws IllegalArgumentException if newValue is null or wrong type
   */
  Object getAndSet(final Object newValue) throws WasmException;

  /**
   * Asynchronously reads the global value.
   *
   * <p>This method returns immediately and provides the global value asynchronously.
   * Useful for non-blocking access patterns.
   *
   * @return a CompletableFuture that completes with the global value
   */
  CompletableFuture<Object> getAsync();

  /**
   * Asynchronously writes the global value.
   *
   * <p>This method returns immediately and updates the global value asynchronously.
   *
   * @param value the new value to set
   * @return a CompletableFuture that completes when the update is done
   * @throws IllegalArgumentException if value is null or wrong type
   */
  CompletableFuture<Void> setAsync(final Object value);

  /**
   * Atomically increments a numeric global value.
   *
   * <p>This method is only valid for numeric global types (i32, i64, f32, f64).
   * For integer types, it performs integer arithmetic. For floating-point types,
   * it performs floating-point arithmetic.
   *
   * @param delta the value to add (must be compatible with global type)
   * @return the new value after increment
   * @throws WasmException if the global is not numeric or operation fails
   * @throws IllegalArgumentException if delta is incompatible with global type
   */
  Object getAndAdd(final Object delta) throws WasmException;

  /**
   * Atomically decrements a numeric global value.
   *
   * <p>This is equivalent to calling getAndAdd with a negative delta.
   *
   * @param delta the value to subtract (must be compatible with global type)
   * @return the new value after decrement
   * @throws WasmException if the global is not numeric or operation fails
   * @throws IllegalArgumentException if delta is incompatible with global type
   */
  Object getAndSubtract(final Object delta) throws WasmException;

  /**
   * Atomically performs a bitwise AND operation on an integer global.
   *
   * <p>This method is only valid for integer global types (i32, i64).
   *
   * @param mask the mask to AND with the current value
   * @return the new value after the AND operation
   * @throws WasmException if the global is not an integer type or operation fails
   * @throws IllegalArgumentException if mask is incompatible with global type
   */
  Object getAndBitwiseAnd(final Object mask) throws WasmException;

  /**
   * Atomically performs a bitwise OR operation on an integer global.
   *
   * <p>This method is only valid for integer global types (i32, i64).
   *
   * @param mask the mask to OR with the current value
   * @return the new value after the OR operation
   * @throws WasmException if the global is not an integer type or operation fails
   * @throws IllegalArgumentException if mask is incompatible with global type
   */
  Object getAndBitwiseOr(final Object mask) throws WasmException;

  /**
   * Atomically performs a bitwise XOR operation on an integer global.
   *
   * <p>This method is only valid for integer global types (i32, i64).
   *
   * @param mask the mask to XOR with the current value
   * @return the new value after the XOR operation
   * @throws WasmException if the global is not an integer type or operation fails
   * @throws IllegalArgumentException if mask is incompatible with global type
   */
  Object getAndBitwiseXor(final Object mask) throws WasmException;

  /**
   * Registers a listener for global value changes.
   *
   * <p>The listener will be notified whenever the global value is modified.
   * This is useful for implementing reactive patterns or monitoring global state.
   *
   * @param listener the listener to register
   * @throws IllegalArgumentException if listener is null
   */
  void addChangeListener(final GlobalChangeListener listener);

  /**
   * Unregisters a previously registered change listener.
   *
   * @param listener the listener to remove
   * @throws IllegalArgumentException if listener is null
   */
  void removeChangeListener(final GlobalChangeListener listener);

  /**
   * Gets the number of times this global has been modified.
   *
   * <p>This is a cumulative counter that tracks all write operations.
   *
   * @return the total number of modifications
   */
  long getModificationCount();

  /**
   * Gets the number of threads currently accessing this global.
   *
   * <p>This includes both read and write operations currently in progress.
   *
   * @return the number of threads currently accessing the global
   */
  int getCurrentAccessorCount();

  /**
   * Gets statistics about concurrent access to this global.
   *
   * <p>This includes metrics about access patterns, contention, and performance.
   *
   * @return detailed concurrency statistics for this global
   */
  GlobalConcurrencyStatistics getConcurrencyStatistics();

  /**
   * Validates that the global is properly configured for concurrent access.
   *
   * @return true if the global is thread-safe and properly configured
   */
  boolean validateConcurrencyConfiguration();

  /**
   * Creates a thread-safe wrapper around this global for specific access patterns.
   *
   * <p>This can be used to create specialized views with different synchronization
   * strategies or access controls.
   *
   * @param accessPattern the expected access pattern
   * @return a specialized thread-safe wrapper
   */
  ConcurrentGlobalWrapper createWrapper(final GlobalAccessPattern accessPattern);
}