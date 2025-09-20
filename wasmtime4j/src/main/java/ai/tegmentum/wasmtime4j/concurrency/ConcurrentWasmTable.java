package ai.tegmentum.wasmtime4j.concurrency;

import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CompletableFuture;

/**
 * Thread-safe WebAssembly table interface for concurrent access.
 *
 * <p>A ConcurrentWasmTable extends the standard WasmTable interface with explicit thread safety
 * guarantees for all table operations. Multiple threads can safely read and write table elements
 * simultaneously with proper synchronization.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Thread-safe table element read and write operations
 *   <li>Atomic operations for concurrent access
 *   <li>Compare-and-swap operations for table elements
 *   <li>Asynchronous bulk table operations
 *   <li>Concurrent table growth with proper coordination
 * </ul>
 *
 * @since 1.0.0
 */
public interface ConcurrentWasmTable extends WasmTable {

  /**
   * Thread-safe atomic read of a table element.
   *
   * @param index the table index to read from
   * @return the element at the specified index
   * @throws WasmException if the index is out of bounds or read fails
   */
  Object getAtomic(final int index) throws WasmException;

  /**
   * Thread-safe atomic write of a table element.
   *
   * @param index the table index to write to
   * @param element the element to store
   * @throws WasmException if the index is out of bounds, element type is invalid, or write fails
   */
  void setAtomic(final int index, final Object element) throws WasmException;

  /**
   * Atomically compares and sets a table element.
   *
   * @param index the table index
   * @param expectedElement the expected current element
   * @param newElement the new element to set
   * @return true if the element was successfully updated, false otherwise
   * @throws WasmException if the index is out of bounds or operation fails
   */
  boolean compareAndSet(final int index, final Object expectedElement, final Object newElement)
      throws WasmException;

  /**
   * Atomically gets the current element and sets a new element.
   *
   * @param index the table index
   * @param newElement the new element to set
   * @return the previous element at the index
   * @throws WasmException if the index is out of bounds or operation fails
   */
  Object getAndSet(final int index, final Object newElement) throws WasmException;

  /**
   * Asynchronously reads a table element.
   *
   * @param index the table index to read from
   * @return a CompletableFuture that completes with the element
   */
  CompletableFuture<Object> getAsync(final int index);

  /**
   * Asynchronously writes a table element.
   *
   * @param index the table index to write to
   * @param element the element to store
   * @return a CompletableFuture that completes when the write is done
   */
  CompletableFuture<Void> setAsync(final int index, final Object element);

  /**
   * Asynchronously reads multiple table elements.
   *
   * @param startIndex the starting index
   * @param count the number of elements to read
   * @return a CompletableFuture that completes with an array of elements
   */
  CompletableFuture<Object[]> getBulkAsync(final int startIndex, final int count);

  /**
   * Asynchronously writes multiple table elements.
   *
   * @param startIndex the starting index
   * @param elements the elements to store
   * @return a CompletableFuture that completes when the write is done
   */
  CompletableFuture<Void> setBulkAsync(final int startIndex, final Object[] elements);

  /**
   * Performs a bulk copy operation within the table atomically.
   *
   * @param sourceIndex the source starting index
   * @param destIndex the destination starting index
   * @param count the number of elements to copy
   * @return a CompletableFuture that completes when the copy is done
   */
  CompletableFuture<Void> copyAsync(final int sourceIndex, final int destIndex, final int count);

  /**
   * Performs a bulk fill operation atomically.
   *
   * @param startIndex the starting index
   * @param element the element to fill with
   * @param count the number of elements to fill
   * @return a CompletableFuture that completes when the fill is done
   */
  CompletableFuture<Void> fillAsync(final int startIndex, final Object element, final int count);

  /**
   * Thread-safe atomic table growth operation.
   *
   * <p>This method atomically grows the table size and initializes new elements. All concurrent
   * operations are properly coordinated during the growth.
   *
   * @param newSize the new table size
   * @param initElement the element to initialize new slots with
   * @return a CompletableFuture that completes when growth is done
   * @throws WasmException if the new size is invalid or growth fails
   */
  CompletableFuture<Void> growAsync(final int newSize, final Object initElement)
      throws WasmException;

  /**
   * Executes a table operation with a read lock held.
   *
   * @param <T> the return type of the operation
   * @param operation the operation to execute under read lock
   * @return the result of the operation
   * @throws WasmException if the operation fails
   */
  <T> T executeWithReadLock(final java.util.function.Supplier<T> operation) throws WasmException;

  /**
   * Executes a table operation with a write lock held.
   *
   * @param <T> the return type of the operation
   * @param operation the operation to execute under write lock
   * @return the result of the operation
   * @throws WasmException if the operation fails
   */
  <T> T executeWithWriteLock(final java.util.function.Supplier<T> operation) throws WasmException;

  /**
   * Creates a thread-safe view of a table region.
   *
   * @param startIndex the starting index of the region
   * @param count the number of elements in the region
   * @return a thread-safe TableRegion view
   * @throws WasmException if the region is out of bounds
   */
  TableRegion createThreadSafeRegion(final int startIndex, final int count) throws WasmException;

  /**
   * Searches for an element in the table concurrently.
   *
   * @param element the element to search for
   * @param startIndex the starting index for the search
   * @return a CompletableFuture that completes with the index of the element, or -1 if not found
   */
  CompletableFuture<Integer> findElementAsync(final Object element, final int startIndex);

  /**
   * Counts occurrences of an element in the table concurrently.
   *
   * @param element the element to count
   * @return a CompletableFuture that completes with the count of occurrences
   */
  CompletableFuture<Integer> countElementAsync(final Object element);

  /**
   * Gets the number of threads currently accessing this table.
   *
   * @return the number of threads currently reading or writing the table
   */
  int getCurrentAccessorCount();

  /**
   * Gets the total number of operations performed on this table.
   *
   * @return the total number of read and write operations
   */
  long getTotalOperationCount();

  /**
   * Gets statistics about concurrent access to this table.
   *
   * @return detailed concurrency statistics for this table
   */
  TableConcurrencyStatistics getConcurrencyStatistics();

  /**
   * Validates that the table is properly configured for concurrent access.
   *
   * @return true if the table is thread-safe and properly configured
   */
  boolean validateConcurrencyConfiguration();

  /**
   * Creates a snapshot of the current table state for debugging.
   *
   * @return a read-only snapshot of the table contents
   */
  TableSnapshot createSnapshot();

  /**
   * Registers a listener for table modification events.
   *
   * @param listener the listener to register
   */
  void addModificationListener(final TableModificationListener listener);

  /**
   * Unregisters a previously registered modification listener.
   *
   * @param listener the listener to remove
   */
  void removeModificationListener(final TableModificationListener listener);
}
