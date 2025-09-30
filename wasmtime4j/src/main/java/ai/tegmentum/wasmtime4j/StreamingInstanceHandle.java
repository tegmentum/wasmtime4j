package ai.tegmentum.wasmtime4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Handle for manual control over streaming WebAssembly instantiation.
 *
 * <p>StreamingInstanceHandle provides fine-grained control over the instantiation process, allowing
 * applications to prioritize specific functions, control memory allocation, and monitor progress in
 * real-time.
 *
 * @since 1.0.0
 */
public interface StreamingInstanceHandle extends AutoCloseable {

  /**
   * Requests compilation of a specific function with high priority.
   *
   * <p>This method schedules the specified function for immediate compilation, bypassing normal
   * priority ordering.
   *
   * @param functionName the name of the function to prioritize
   * @return a CompletableFuture that completes when the function is compiled
   * @throws IllegalArgumentException if functionName is null
   * @throws IllegalStateException if the handle is closed
   */
  CompletableFuture<Void> prioritizeFunction(String functionName);

  /**
   * Requests compilation of multiple functions with high priority.
   *
   * @param functionNames the names of the functions to prioritize
   * @return a CompletableFuture that completes when all functions are compiled
   * @throws IllegalArgumentException if functionNames is null or contains null elements
   * @throws IllegalStateException if the handle is closed
   */
  CompletableFuture<Void> prioritizeFunctions(List<String> functionNames);

  /**
   * Allocates the WebAssembly linear memory for the instance.
   *
   * <p>This method can be called early in the instantiation process to ensure memory is available
   * when functions are compiled.
   *
   * @param initialPages initial number of memory pages to allocate
   * @param maximumPages maximum number of memory pages (0 for unlimited)
   * @return a CompletableFuture that completes when memory is allocated
   * @throws IllegalArgumentException if initialPages or maximumPages is negative
   * @throws IllegalStateException if the handle is closed or memory is already allocated
   */
  CompletableFuture<Memory> allocateMemory(long initialPages, long maximumPages);

  /**
   * Allocates WebAssembly tables for the instance.
   *
   * <p>This method pre-allocates tables defined by the module, making them available for function
   * compilation.
   *
   * @return a CompletableFuture that completes when all tables are allocated
   * @throws IllegalStateException if the handle is closed or tables are already allocated
   */
  CompletableFuture<List<Table>> allocateTables();

  /**
   * Allocates WebAssembly globals for the instance.
   *
   * <p>This method pre-allocates globals defined by the module, making them available for function
   * compilation.
   *
   * @return a CompletableFuture that completes when all globals are allocated
   * @throws IllegalStateException if the handle is closed or globals are already allocated
   */
  CompletableFuture<List<Global>> allocateGlobals();

  /**
   * Waits for a minimum set of functions to be ready for execution.
   *
   * <p>This method returns a partial instance that can execute the specified functions, even if
   * other functions are still being compiled.
   *
   * @param requiredFunctions list of function names that must be ready
   * @return a CompletableFuture that completes with a partial instance
   * @throws IllegalArgumentException if requiredFunctions is null
   * @throws IllegalStateException if the handle is closed
   */
  CompletableFuture<Instance> waitForFunctions(List<String> requiredFunctions);

  /**
   * Waits for the start function to be ready and executes it.
   *
   * <p>This method compiles and executes the module's start function (if present) before other
   * functions are fully ready.
   *
   * @return a CompletableFuture that completes when the start function has executed
   * @throws IllegalStateException if the handle is closed
   */
  CompletableFuture<Void> executeStartFunction();

  /**
   * Waits for all functions to be compiled and returns the complete instance.
   *
   * <p>This method blocks until the entire instantiation process is complete and returns a fully
   * functional instance.
   *
   * @return a CompletableFuture that completes with the fully instantiated instance
   * @throws IllegalStateException if the handle is closed
   */
  CompletableFuture<Instance> complete();

  /**
   * Gets the current instantiation statistics.
   *
   * @return current instantiation statistics
   */
  InstantiationStatistics getStatistics();

  /**
   * Gets the list of functions that are currently ready for execution.
   *
   * @return list of ready function names
   */
  List<String> getReadyFunctions();

  /**
   * Gets the list of functions that are still being compiled.
   *
   * @return list of pending function names
   */
  List<String> getPendingFunctions();

  /**
   * Checks if a specific function is ready for execution.
   *
   * @param functionName the name of the function to check
   * @return true if the function is ready
   * @throws IllegalArgumentException if functionName is null
   */
  boolean isFunctionReady(String functionName);

  /**
   * Checks if the instance memory has been allocated.
   *
   * @return true if memory is allocated
   */
  boolean isMemoryAllocated();

  /**
   * Checks if all instance tables have been allocated.
   *
   * @return true if tables are allocated
   */
  boolean areTablesAllocated();

  /**
   * Checks if all instance globals have been allocated.
   *
   * @return true if globals are allocated
   */
  boolean areGlobalsAllocated();

  /**
   * Cancels the streaming instantiation and releases resources.
   *
   * @param mayInterruptIfRunning whether to interrupt instantiation threads
   * @return true if cancellation was successful
   */
  boolean cancel(boolean mayInterruptIfRunning);

  /**
   * Checks if the instantiation has been completed or cancelled.
   *
   * @return true if instantiation is done
   */
  boolean isDone();

  /**
   * Closes the instance handle and releases associated resources.
   *
   * <p>If instantiation is not complete, this method will attempt to cancel it gracefully.
   */
  @Override
  void close();
}
