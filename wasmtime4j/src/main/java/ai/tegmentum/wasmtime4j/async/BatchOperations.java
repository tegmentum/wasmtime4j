package ai.tegmentum.wasmtime4j.async;

import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Provides batch operations for efficient processing of multiple WebAssembly operations.
 *
 * <p>BatchOperations enables high-throughput processing by grouping related operations together and
 * executing them efficiently with shared resources, progress tracking, and comprehensive error
 * handling. This is particularly useful for scenarios involving multiple module compilations,
 * function calls, or data processing tasks.
 *
 * <p>All batch operations support cancellation, progress monitoring, and detailed statistics
 * collection for performance analysis and monitoring.
 *
 * @since 1.0.0
 */
public interface BatchOperations {

  /**
   * Compiles multiple WebAssembly modules concurrently.
   *
   * <p>This method efficiently compiles multiple modules by sharing compilation resources and
   * optimizing the compilation pipeline for batch processing.
   *
   * @param sources the module sources to compile
   * @return a CompletableFuture that completes with the batch compilation results
   * @throws IllegalArgumentException if sources is null or empty
   */
  CompletableFuture<BatchCompilationResult> compileBatch(final Collection<ModuleSource> sources);

  /**
   * Compiles multiple WebAssembly modules with custom options.
   *
   * @param sources the module sources to compile
   * @param options batch compilation options
   * @return a CompletableFuture that completes with the batch compilation results
   * @throws IllegalArgumentException if sources or options is null
   */
  CompletableFuture<BatchCompilationResult> compileBatch(
      final Collection<ModuleSource> sources, final BatchOptions options);

  /**
   * Executes multiple function calls concurrently.
   *
   * <p>This method efficiently executes multiple function calls by optimizing execution scheduling
   * and resource usage across the batch.
   *
   * @param calls the function calls to execute
   * @return a CompletableFuture that completes with the batch execution results
   * @throws IllegalArgumentException if calls is null or empty
   */
  CompletableFuture<BatchExecutionResult> executeBatch(final Collection<FunctionCallRequest> calls);

  /**
   * Executes multiple function calls with custom options.
   *
   * @param calls the function calls to execute
   * @param options batch execution options
   * @return a CompletableFuture that completes with the batch execution results
   * @throws IllegalArgumentException if calls or options is null
   */
  CompletableFuture<BatchExecutionResult> executeBatch(
      final Collection<FunctionCallRequest> calls, final BatchOptions options);

  /**
   * Performs multiple memory operations concurrently.
   *
   * <p>This method efficiently performs bulk memory operations such as reads, writes, and transfers
   * by optimizing memory access patterns and reducing overhead.
   *
   * @param operations the memory operations to perform
   * @return a CompletableFuture that completes with the batch memory results
   * @throws IllegalArgumentException if operations is null or empty
   */
  CompletableFuture<BatchMemoryResult> memoryBatch(final Collection<MemoryOperation> operations);

  /**
   * Performs multiple memory operations with custom options.
   *
   * @param operations the memory operations to perform
   * @param options batch memory options
   * @return a CompletableFuture that completes with the batch memory results
   * @throws IllegalArgumentException if operations or options is null
   */
  CompletableFuture<BatchMemoryResult> memoryBatch(
      final Collection<MemoryOperation> operations, final BatchOptions options);

  /**
   * Creates a batch operation builder for complex batch configurations.
   *
   * @return a new batch operation builder
   */
  BatchOperationBuilder builder();

  /**
   * Gets statistics about batch operations performed.
   *
   * @return batch operation statistics
   */
  BatchOperationStatistics getStatistics();

  /** Represents a module source for batch compilation. */
  interface ModuleSource {
    /**
     * Gets the unique identifier for this module source.
     *
     * @return module source ID
     */
    String getId();

    /**
     * Gets the WebAssembly bytecode.
     *
     * @return WebAssembly bytecode
     */
    byte[] getWasmBytes();

    /**
     * Gets the priority for compilation (higher values = higher priority).
     *
     * @return compilation priority
     */
    int getPriority();

    /**
     * Gets optional metadata associated with this module.
     *
     * @return module metadata, or null if none
     */
    Object getMetadata();
  }

  /** Represents a function call request for batch execution. */
  interface FunctionCallRequest {
    /**
     * Gets the unique identifier for this call request.
     *
     * @return call request ID
     */
    String getId();

    /**
     * Gets the module containing the function to call.
     *
     * @return the module
     */
    Module getModule();

    /**
     * Gets the name of the function to call.
     *
     * @return function name
     */
    String getFunctionName();

    /**
     * Gets the parameters for the function call.
     *
     * @return function parameters
     */
    WasmValue[] getParameters();

    /**
     * Gets the priority for execution (higher values = higher priority).
     *
     * @return execution priority
     */
    int getPriority();

    /**
     * Gets optional metadata associated with this call.
     *
     * @return call metadata, or null if none
     */
    Object getMetadata();
  }

  /** Represents a memory operation for batch processing. */
  interface MemoryOperation {
    /**
     * Gets the unique identifier for this memory operation.
     *
     * @return operation ID
     */
    String getId();

    /**
     * Gets the type of memory operation.
     *
     * @return operation type
     */
    MemoryOperationType getType();

    /**
     * Gets the memory offset for the operation.
     *
     * @return memory offset
     */
    int getOffset();

    /**
     * Gets the length of the operation in bytes.
     *
     * @return operation length
     */
    int getLength();

    /**
     * Gets the data for write operations.
     *
     * @return operation data, or null for read operations
     */
    byte[] getData();

    /**
     * Gets the priority for this operation (higher values = higher priority).
     *
     * @return operation priority
     */
    int getPriority();

    /**
     * Gets optional metadata associated with this operation.
     *
     * @return operation metadata, or null if none
     */
    Object getMetadata();
  }

  /** Types of memory operations for batch processing. */
  enum MemoryOperationType {
    READ,
    WRITE,
    COPY,
    FILL,
    COMPARE
  }

  /** Configuration options for batch operations. */
  interface BatchOptions {
    /**
     * Gets the maximum number of concurrent operations.
     *
     * @return maximum concurrency level
     */
    int getMaxConcurrency();

    /**
     * Gets the timeout for the entire batch operation.
     *
     * @return batch timeout, or null for no timeout
     */
    Duration getBatchTimeout();

    /**
     * Gets the timeout for individual operations within the batch.
     *
     * @return individual operation timeout, or null for no timeout
     */
    Duration getOperationTimeout();

    /**
     * Gets the custom executor for batch operations.
     *
     * @return custom executor, or null to use default
     */
    Executor getExecutor();

    /**
     * Gets the cancellation token for the batch operation.
     *
     * @return cancellation token, or null if not cancellable
     */
    CancellationToken getCancellationToken();

    /**
     * Checks if progress tracking is enabled.
     *
     * @return true if progress tracking is enabled
     */
    boolean isProgressTrackingEnabled();

    /**
     * Gets the progress callback for tracking batch progress.
     *
     * @return progress callback, or null if not tracking progress
     */
    Consumer<BatchProgress> getProgressCallback();

    /**
     * Checks if operations should fail fast on first error.
     *
     * @return true if fail-fast is enabled
     */
    boolean isFailFastEnabled();

    /**
     * Gets the maximum number of retries for failed operations.
     *
     * @return maximum retry count
     */
    int getMaxRetries();

    /**
     * Gets the delay between retry attempts.
     *
     * @return retry delay
     */
    Duration getRetryDelay();

    /**
     * Checks if operations should be sorted by priority before execution.
     *
     * @return true if priority sorting is enabled
     */
    boolean isPrioritySortingEnabled();
  }

  /** Progress information for batch operations. */
  interface BatchProgress {
    /**
     * Gets the total number of operations in the batch.
     *
     * @return total operation count
     */
    int getTotalOperations();

    /**
     * Gets the number of completed operations.
     *
     * @return completed operation count
     */
    int getCompletedOperations();

    /**
     * Gets the number of failed operations.
     *
     * @return failed operation count
     */
    int getFailedOperations();

    /**
     * Gets the number of cancelled operations.
     *
     * @return cancelled operation count
     */
    int getCancelledOperations();

    /**
     * Gets the overall progress as a percentage (0.0 to 1.0).
     *
     * @return progress percentage
     */
    double getProgressPercentage();

    /**
     * Gets the estimated time remaining.
     *
     * @return estimated remaining time, or null if not available
     */
    Duration getEstimatedTimeRemaining();

    /**
     * Gets the time elapsed since batch start.
     *
     * @return elapsed time
     */
    Duration getElapsedTime();

    /**
     * Gets the current throughput in operations per second.
     *
     * @return current throughput
     */
    double getCurrentThroughput();

    /**
     * Gets the average throughput in operations per second.
     *
     * @return average throughput
     */
    double getAverageThroughput();
  }

  /** Result of a batch compilation operation. */
  interface BatchCompilationResult {
    /**
     * Gets the list of successful compilation results.
     *
     * @return successful compilations
     */
    List<CompilationSuccess> getSuccesses();

    /**
     * Gets the list of failed compilation results.
     *
     * @return failed compilations
     */
    List<CompilationFailure> getFailures();

    /**
     * Gets the overall batch statistics.
     *
     * @return batch statistics
     */
    BatchStatistics getStatistics();

    /**
     * Checks if the entire batch was successful.
     *
     * @return true if all compilations succeeded
     */
    boolean isCompleteSuccess();

    /**
     * Gets the total number of operations in the batch.
     *
     * @return total operation count
     */
    int getTotalOperations();

    /**
     * Gets the number of successful operations.
     *
     * @return success count
     */
    int getSuccessCount();

    /**
     * Gets the number of failed operations.
     *
     * @return failure count
     */
    int getFailureCount();
  }

  /** Result of a batch execution operation. */
  interface BatchExecutionResult {
    /**
     * Gets the list of successful execution results.
     *
     * @return successful executions
     */
    List<ExecutionSuccess> getSuccesses();

    /**
     * Gets the list of failed execution results.
     *
     * @return failed executions
     */
    List<ExecutionFailure> getFailures();

    /**
     * Gets the overall batch statistics.
     *
     * @return batch statistics
     */
    BatchStatistics getStatistics();

    /**
     * Checks if the entire batch was successful.
     *
     * @return true if all executions succeeded
     */
    boolean isCompleteSuccess();

    /**
     * Gets the total number of operations in the batch.
     *
     * @return total operation count
     */
    int getTotalOperations();

    /**
     * Gets the number of successful operations.
     *
     * @return success count
     */
    int getSuccessCount();

    /**
     * Gets the number of failed operations.
     *
     * @return failure count
     */
    int getFailureCount();
  }

  /** Result of a batch memory operation. */
  interface BatchMemoryResult {
    /**
     * Gets the list of successful memory operation results.
     *
     * @return successful operations
     */
    List<MemoryOperationSuccess> getSuccesses();

    /**
     * Gets the list of failed memory operation results.
     *
     * @return failed operations
     */
    List<MemoryOperationFailure> getFailures();

    /**
     * Gets the overall batch statistics.
     *
     * @return batch statistics
     */
    BatchStatistics getStatistics();

    /**
     * Checks if the entire batch was successful.
     *
     * @return true if all operations succeeded
     */
    boolean isCompleteSuccess();

    /**
     * Gets the total number of operations in the batch.
     *
     * @return total operation count
     */
    int getTotalOperations();

    /**
     * Gets the number of successful operations.
     *
     * @return success count
     */
    int getSuccessCount();

    /**
     * Gets the number of failed operations.
     *
     * @return failure count
     */
    int getFailureCount();
  }

  /** Represents a successful compilation result. */
  interface CompilationSuccess {
    /**
     * Gets the module source ID.
     *
     * @return source ID
     */
    String getSourceId();

    /**
     * Gets the compiled module.
     *
     * @return compiled module
     */
    Module getModule();

    /**
     * Gets the compilation time.
     *
     * @return compilation duration
     */
    Duration getCompilationTime();

    /**
     * Gets the metadata associated with this result.
     *
     * @return result metadata, or null if none
     */
    Object getMetadata();
  }

  /** Represents a failed compilation result. */
  interface CompilationFailure {
    /**
     * Gets the module source ID.
     *
     * @return source ID
     */
    String getSourceId();

    /**
     * Gets the compilation error.
     *
     * @return compilation exception
     */
    WasmException getError();

    /**
     * Gets the time spent before failure.
     *
     * @return time before failure
     */
    Duration getTimeBeforeFailure();

    /**
     * Gets the metadata associated with this result.
     *
     * @return result metadata, or null if none
     */
    Object getMetadata();
  }

  /** Represents a successful execution result. */
  interface ExecutionSuccess {
    /**
     * Gets the call request ID.
     *
     * @return request ID
     */
    String getRequestId();

    /**
     * Gets the execution results.
     *
     * @return function call results
     */
    WasmValue[] getResults();

    /**
     * Gets the execution time.
     *
     * @return execution duration
     */
    Duration getExecutionTime();

    /**
     * Gets the metadata associated with this result.
     *
     * @return result metadata, or null if none
     */
    Object getMetadata();
  }

  /** Represents a failed execution result. */
  interface ExecutionFailure {
    /**
     * Gets the call request ID.
     *
     * @return request ID
     */
    String getRequestId();

    /**
     * Gets the execution error.
     *
     * @return execution exception
     */
    WasmException getError();

    /**
     * Gets the time spent before failure.
     *
     * @return time before failure
     */
    Duration getTimeBeforeFailure();

    /**
     * Gets the metadata associated with this result.
     *
     * @return result metadata, or null if none
     */
    Object getMetadata();
  }

  /** Represents a successful memory operation result. */
  interface MemoryOperationSuccess {
    /**
     * Gets the operation ID.
     *
     * @return operation ID
     */
    String getOperationId();

    /**
     * Gets the operation type.
     *
     * @return operation type
     */
    MemoryOperationType getType();

    /**
     * Gets the result data for read operations.
     *
     * @return result data, or null for non-read operations
     */
    byte[] getResultData();

    /**
     * Gets the operation execution time.
     *
     * @return execution duration
     */
    Duration getExecutionTime();

    /**
     * Gets the metadata associated with this result.
     *
     * @return result metadata, or null if none
     */
    Object getMetadata();
  }

  /** Represents a failed memory operation result. */
  interface MemoryOperationFailure {
    /**
     * Gets the operation ID.
     *
     * @return operation ID
     */
    String getOperationId();

    /**
     * Gets the operation type.
     *
     * @return operation type
     */
    MemoryOperationType getType();

    /**
     * Gets the operation error.
     *
     * @return operation exception
     */
    WasmException getError();

    /**
     * Gets the time spent before failure.
     *
     * @return time before failure
     */
    Duration getTimeBeforeFailure();

    /**
     * Gets the metadata associated with this result.
     *
     * @return result metadata, or null if none
     */
    Object getMetadata();
  }

  /** Overall statistics for a batch operation. */
  interface BatchStatistics {
    /**
     * Gets the batch start time.
     *
     * @return start timestamp
     */
    Instant getStartTime();

    /**
     * Gets the batch end time.
     *
     * @return end timestamp
     */
    Instant getEndTime();

    /**
     * Gets the total batch execution time.
     *
     * @return total duration
     */
    Duration getTotalTime();

    /**
     * Gets the average operation execution time.
     *
     * @return average duration per operation
     */
    Duration getAverageOperationTime();

    /**
     * Gets the minimum operation execution time.
     *
     * @return minimum duration
     */
    Duration getMinOperationTime();

    /**
     * Gets the maximum operation execution time.
     *
     * @return maximum duration
     */
    Duration getMaxOperationTime();

    /**
     * Gets the overall throughput in operations per second.
     *
     * @return operations per second
     */
    double getThroughput();

    /**
     * Gets the success rate as a percentage (0.0 to 1.0).
     *
     * @return success rate
     */
    double getSuccessRate();

    /**
     * Gets the total memory usage during the batch operation.
     *
     * @return memory usage in bytes
     */
    long getTotalMemoryUsage();

    /**
     * Gets the peak memory usage during the batch operation.
     *
     * @return peak memory usage in bytes
     */
    long getPeakMemoryUsage();
  }

  /** Builder for creating complex batch operations. */
  interface BatchOperationBuilder {
    /**
     * Adds module sources for compilation.
     *
     * @param sources module sources to add
     * @return this builder
     */
    BatchOperationBuilder addCompilations(final Collection<ModuleSource> sources);

    /**
     * Adds function calls for execution.
     *
     * @param calls function calls to add
     * @return this builder
     */
    BatchOperationBuilder addExecutions(final Collection<FunctionCallRequest> calls);

    /**
     * Adds memory operations.
     *
     * @param operations memory operations to add
     * @return this builder
     */
    BatchOperationBuilder addMemoryOperations(final Collection<MemoryOperation> operations);

    /**
     * Sets batch options.
     *
     * @param options batch options
     * @return this builder
     */
    BatchOperationBuilder withOptions(final BatchOptions options);

    /**
     * Sets maximum concurrency.
     *
     * @param maxConcurrency maximum concurrent operations
     * @return this builder
     */
    BatchOperationBuilder withMaxConcurrency(final int maxConcurrency);

    /**
     * Sets batch timeout.
     *
     * @param timeout batch timeout
     * @return this builder
     */
    BatchOperationBuilder withTimeout(final Duration timeout);

    /**
     * Sets progress callback.
     *
     * @param callback progress callback
     * @return this builder
     */
    BatchOperationBuilder withProgressCallback(final Consumer<BatchProgress> callback);

    /**
     * Sets cancellation token.
     *
     * @param token cancellation token
     * @return this builder
     */
    BatchOperationBuilder withCancellation(final CancellationToken token);

    /**
     * Executes the batch operation.
     *
     * @return a CompletableFuture that completes with the mixed batch results
     */
    CompletableFuture<MixedBatchResult> execute();
  }

  /** Result of a mixed batch operation containing multiple operation types. */
  interface MixedBatchResult {
    /**
     * Gets the compilation results.
     *
     * @return compilation results, or null if no compilations were performed
     */
    BatchCompilationResult getCompilationResults();

    /**
     * Gets the execution results.
     *
     * @return execution results, or null if no executions were performed
     */
    BatchExecutionResult getExecutionResults();

    /**
     * Gets the memory operation results.
     *
     * @return memory results, or null if no memory operations were performed
     */
    BatchMemoryResult getMemoryResults();

    /**
     * Gets the overall batch statistics.
     *
     * @return overall statistics
     */
    BatchStatistics getOverallStatistics();

    /**
     * Checks if the entire mixed batch was successful.
     *
     * @return true if all operations succeeded
     */
    boolean isCompleteSuccess();
  }

  /** Statistics for batch operations. */
  interface BatchOperationStatistics {
    /**
     * Gets the total number of batch operations performed.
     *
     * @return total batch count
     */
    long getTotalBatchOperations();

    /**
     * Gets the total number of individual operations across all batches.
     *
     * @return total operation count
     */
    long getTotalIndividualOperations();

    /**
     * Gets the average batch size.
     *
     * @return average operations per batch
     */
    double getAverageBatchSize();

    /**
     * Gets the average batch execution time.
     *
     * @return average batch duration
     */
    Duration getAverageBatchTime();

    /**
     * Gets the overall success rate across all batches.
     *
     * @return overall success rate (0.0 to 1.0)
     */
    double getOverallSuccessRate();

    /**
     * Gets the overall throughput across all batches.
     *
     * @return overall throughput in operations per second
     */
    double getOverallThroughput();

    /**
     * Gets the peak concurrent operations across all batches.
     *
     * @return peak concurrency level
     */
    int getPeakConcurrency();

    /**
     * Gets the total memory usage across all batches.
     *
     * @return total memory usage in bytes
     */
    long getTotalMemoryUsage();
  }
}
