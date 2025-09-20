package ai.tegmentum.wasmtime4j.concurrency;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Comprehensive statistics about concurrency performance and patterns.
 *
 * <p>This interface provides detailed metrics about concurrent operations, thread usage,
 * performance characteristics, and resource contention patterns in WebAssembly engines
 * and their components.
 *
 * @since 1.0.0
 */
public interface ConcurrencyStatistics {

  /**
   * Gets the total number of concurrent operations executed.
   *
   * @return the total number of concurrent operations
   */
  long getTotalConcurrentOperations();

  /**
   * Gets the number of operations currently in progress.
   *
   * @return the number of active concurrent operations
   */
  int getActiveConcurrentOperations();

  /**
   * Gets the maximum number of concurrent operations ever active simultaneously.
   *
   * @return the peak concurrent operation count
   */
  int getPeakConcurrentOperations();

  /**
   * Gets the average number of concurrent operations over time.
   *
   * @return the average concurrent operation count
   */
  double getAverageConcurrentOperations();

  /**
   * Gets the total number of threads that have accessed the component.
   *
   * @return the total number of accessing threads
   */
  int getTotalAccessingThreads();

  /**
   * Gets the number of threads currently accessing the component.
   *
   * @return the number of currently active threads
   */
  int getCurrentAccessingThreads();

  /**
   * Gets the maximum number of threads ever active simultaneously.
   *
   * @return the peak thread count
   */
  int getPeakThreadCount();

  /**
   * Gets the total time spent waiting for locks.
   *
   * @return the cumulative lock wait time
   */
  Duration getTotalLockWaitTime();

  /**
   * Gets the average time spent waiting for locks per operation.
   *
   * @return the average lock wait time
   */
  Duration getAverageLockWaitTime();

  /**
   * Gets the maximum time spent waiting for a single lock.
   *
   * @return the maximum lock wait time
   */
  Duration getMaxLockWaitTime();

  /**
   * Gets the total number of lock contentions encountered.
   *
   * @return the total number of lock contentions
   */
  long getTotalLockContentions();

  /**
   * Gets the lock contention rate as a percentage of operations.
   *
   * @return the lock contention rate (0.0 to 100.0)
   */
  double getLockContentionRate();

  /**
   * Gets the total execution time for all concurrent operations.
   *
   * @return the cumulative execution time
   */
  Duration getTotalExecutionTime();

  /**
   * Gets the average execution time per operation.
   *
   * @return the average execution time
   */
  Duration getAverageExecutionTime();

  /**
   * Gets the minimum execution time for any operation.
   *
   * @return the minimum execution time
   */
  Duration getMinExecutionTime();

  /**
   * Gets the maximum execution time for any operation.
   *
   * @return the maximum execution time
   */
  Duration getMaxExecutionTime();

  /**
   * Gets the throughput in operations per second.
   *
   * @return the operations per second rate
   */
  double getOperationsPerSecond();

  /**
   * Gets the efficiency ratio of concurrent operations.
   *
   * <p>This measures how effectively concurrent operations are utilizing available resources.
   * A higher ratio indicates better parallel efficiency.
   *
   * @return the efficiency ratio (0.0 to 1.0)
   */
  double getConcurrencyEfficiency();

  /**
   * Gets the number of operations that timed out.
   *
   * @return the timeout count
   */
  long getTimeoutCount();

  /**
   * Gets the number of operations that failed due to errors.
   *
   * @return the error count
   */
  long getErrorCount();

  /**
   * Gets the number of operations that were cancelled.
   *
   * @return the cancellation count
   */
  long getCancellationCount();

  /**
   * Gets the success rate as a percentage of all operations.
   *
   * @return the success rate (0.0 to 100.0)
   */
  double getSuccessRate();

  /**
   * Gets detailed statistics by operation type.
   *
   * @return a map of operation type to specific statistics
   */
  Map<String, OperationStatistics> getOperationStatistics();

  /**
   * Gets detailed statistics by thread.
   *
   * @return a map of thread ID to thread-specific statistics
   */
  Map<Long, ThreadStatistics> getThreadStatistics();

  /**
   * Gets the timestamp when statistics collection started.
   *
   * @return the collection start time
   */
  Instant getCollectionStartTime();

  /**
   * Gets the duration for which statistics have been collected.
   *
   * @return the collection duration
   */
  Duration getCollectionDuration();

  /**
   * Checks if statistics collection is currently active.
   *
   * @return true if statistics are being collected
   */
  boolean isCollectionActive();

  /**
   * Gets the current memory usage for concurrent operations.
   *
   * @return memory usage statistics
   */
  MemoryUsageStatistics getMemoryUsage();

  /**
   * Gets CPU usage statistics for concurrent operations.
   *
   * @return CPU usage statistics
   */
  CpuUsageStatistics getCpuUsage();

  /**
   * Gets a summary report of the most important statistics.
   *
   * @return a human-readable summary of key metrics
   */
  String getSummaryReport();

  /**
   * Resets all statistics counters to zero.
   *
   * <p>This operation is atomic and thread-safe.
   */
  void reset();

  /**
   * Creates a snapshot of the current statistics.
   *
   * <p>The snapshot is immutable and represents the statistics at the time of creation.
   *
   * @return an immutable snapshot of current statistics
   */
  ConcurrencyStatisticsSnapshot createSnapshot();
}