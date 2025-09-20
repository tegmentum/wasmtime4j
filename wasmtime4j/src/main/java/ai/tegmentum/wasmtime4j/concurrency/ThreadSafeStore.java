package ai.tegmentum.wasmtime4j.concurrency;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Thread-safe WebAssembly store interface for concurrent execution contexts.
 *
 * <p>A ThreadSafeStore extends the standard Store interface with explicit thread safety guarantees
 * for all operations. Unlike the base Store interface which requires external synchronization,
 * ThreadSafeStore can be safely accessed from multiple threads without additional locking.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Thread-safe fuel and epoch management
 *   <li>Concurrent host function creation and management
 *   <li>Thread-safe data access with proper synchronization
 *   <li>Isolated execution contexts per thread when needed
 *   <li>Atomic operations for state modifications
 * </ul>
 *
 * <p>Implementation requirements:
 *
 * <ul>
 *   <li>All operations must be thread-safe without external synchronization
 *   <li>Fuel and epoch operations must be atomic
 *   <li>Host function creation must not interfere between threads
 *   <li>Store data access must be properly synchronized
 * </ul>
 *
 * @since 1.0.0
 */
public interface ThreadSafeStore extends Store {

  /**
   * Gets the thread-safe engine associated with this store.
   *
   * @return the ThreadSafeEngine that created this store
   */
  @Override
  ThreadSafeEngine getEngine();

  /**
   * Thread-safe access to custom data associated with this store.
   *
   * <p>This method provides thread-safe read access to the store's custom data. Multiple threads
   * can safely read the data concurrently.
   *
   * @return the custom data, or null if none was set
   */
  @Override
  Object getData();

  /**
   * Thread-safe modification of custom data associated with this store.
   *
   * <p>This method provides thread-safe write access to the store's custom data. The update is
   * atomic and visible to all threads immediately.
   *
   * @param data the custom data to associate
   */
  @Override
  void setData(final Object data);

  /**
   * Thread-safe atomic fuel amount setting.
   *
   * <p>This method atomically sets the fuel amount and is safe to call from multiple threads.
   * Concurrent fuel operations are properly synchronized.
   *
   * @param fuel the amount of fuel to set
   * @throws IllegalArgumentException if fuel is negative
   * @throws WasmException if the operation fails
   */
  @Override
  void setFuel(final long fuel) throws WasmException;

  /**
   * Thread-safe atomic fuel amount retrieval.
   *
   * <p>This method provides an atomic snapshot of the current fuel amount.
   *
   * @return the remaining fuel, or -1 if fuel consumption is disabled
   * @throws WasmException if the operation fails
   */
  @Override
  long getFuel() throws WasmException;

  /**
   * Thread-safe atomic fuel addition.
   *
   * <p>This method atomically adds fuel to the current amount and is safe to call from multiple
   * threads. The addition operation is atomic and consistent.
   *
   * @param fuel the amount of fuel to add
   * @throws IllegalArgumentException if fuel is negative
   * @throws WasmException if the operation fails
   */
  @Override
  void addFuel(final long fuel) throws WasmException;

  /**
   * Thread-safe atomic epoch deadline setting.
   *
   * <p>This method atomically sets the epoch deadline and is safe to call from multiple threads.
   *
   * @param ticks the number of epoch ticks before interruption
   * @throws WasmException if the operation fails
   */
  @Override
  void setEpochDeadline(final long ticks) throws WasmException;

  /**
   * Thread-safe host function creation.
   *
   * <p>This method creates host functions that are safe to use in concurrent environments. Multiple
   * threads can create host functions simultaneously without interference.
   *
   * @param name the name of the function (for debugging/logging purposes)
   * @param functionType the WebAssembly function type signature
   * @param implementation the Java implementation of the function
   * @return a thread-safe WasmFunction that can be used in import maps
   * @throws WasmException if host function creation fails
   * @throws IllegalArgumentException if any parameter is null
   */
  @Override
  ConcurrentWasmFunction createHostFunction(
      final String name, final FunctionType functionType, final HostFunction implementation)
      throws WasmException;

  /**
   * Atomically compares and sets the fuel amount.
   *
   * <p>This method atomically compares the current fuel amount with the expected value and sets it
   * to the new value if they match. This is useful for lock-free fuel management.
   *
   * @param expectedFuel the expected current fuel amount
   * @param newFuel the new fuel amount to set
   * @return true if the fuel was successfully updated, false otherwise
   * @throws WasmException if the operation fails
   */
  boolean compareAndSetFuel(final long expectedFuel, final long newFuel) throws WasmException;

  /**
   * Atomically increments the fuel amount by the specified delta.
   *
   * <p>This method atomically adds the delta to the current fuel amount and returns the new value.
   * The operation is atomic and thread-safe.
   *
   * @param delta the amount to add to the fuel (can be negative)
   * @return the new fuel amount after the increment
   * @throws WasmException if the operation fails
   */
  long getAndAddFuel(final long delta) throws WasmException;

  /**
   * Gets the current epoch tick count.
   *
   * <p>This method provides a thread-safe way to read the current epoch tick count.
   *
   * @return the current epoch tick count
   * @throws WasmException if the operation fails
   */
  long getCurrentEpochTicks() throws WasmException;

  /**
   * Increments the epoch by one tick atomically.
   *
   * <p>This method atomically increments the epoch and is safe to call from multiple threads. It's
   * useful for coordinated epoch management across threads.
   *
   * @return the new epoch tick count after increment
   * @throws WasmException if the operation fails
   */
  long incrementEpoch() throws WasmException;

  /**
   * Gets the read-write lock used for store synchronization.
   *
   * <p>This exposes the internal synchronization mechanism for advanced use cases where custom
   * synchronization patterns are needed. Use with caution.
   *
   * @return the ReadWriteLock used for synchronization
   */
  ReadWriteLock getSynchronizationLock();

  /**
   * Executes a read operation with the store's read lock held.
   *
   * <p>This method acquires the read lock, executes the operation, and releases the lock. Multiple
   * read operations can execute concurrently.
   *
   * @param <T> the return type of the operation
   * @param operation the operation to execute under read lock
   * @return the result of the operation
   * @throws WasmException if the operation fails
   * @throws IllegalArgumentException if operation is null
   */
  <T> T executeWithReadLock(final java.util.function.Supplier<T> operation) throws WasmException;

  /**
   * Executes a write operation with the store's write lock held.
   *
   * <p>This method acquires the write lock, executes the operation, and releases the lock. Write
   * operations are exclusive and block other read and write operations.
   *
   * @param <T> the return type of the operation
   * @param operation the operation to execute under write lock
   * @return the result of the operation
   * @throws WasmException if the operation fails
   * @throws IllegalArgumentException if operation is null
   */
  <T> T executeWithWriteLock(final java.util.function.Supplier<T> operation) throws WasmException;

  /**
   * Checks if the store is currently thread-safe and properly configured.
   *
   * <p>This method validates that the store's internal synchronization mechanisms are properly
   * initialized and functioning correctly.
   *
   * @return true if the store is thread-safe and properly configured
   */
  boolean isThreadSafe();

  /**
   * Gets statistics about concurrent access to this store.
   *
   * <p>This includes metrics about lock contention, operation counts, and performance under
   * concurrent load.
   *
   * @return detailed concurrency statistics for this store
   */
  StoreConcurrencyStatistics getConcurrencyStatistics();

  /**
   * Gets the number of threads currently accessing this store.
   *
   * <p>This count includes both read and write operations currently in progress.
   *
   * @return the number of threads currently accessing the store
   */
  int getCurrentAccessorCount();

  /**
   * Gets the total number of concurrent operations performed on this store.
   *
   * <p>This is a cumulative counter that tracks all read and write operations.
   *
   * @return the total number of concurrent operations
   */
  long getTotalConcurrentOperations();

  /**
   * Waits for all current operations to complete.
   *
   * <p>This method blocks until no threads are currently accessing the store. It's useful for
   * graceful shutdown or synchronization points.
   *
   * @param timeoutMillis maximum time to wait in milliseconds
   * @return true if all operations completed within the timeout, false otherwise
   * @throws InterruptedException if the wait is interrupted
   */
  boolean awaitQuiescence(final long timeoutMillis) throws InterruptedException;
}
