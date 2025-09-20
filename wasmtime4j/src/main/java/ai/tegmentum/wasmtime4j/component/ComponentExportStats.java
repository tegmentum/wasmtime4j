package ai.tegmentum.wasmtime4j.component;

import java.time.Duration;
import java.time.Instant;

/**
 * Statistics for individual component exports.
 *
 * <p>ComponentExportStats tracks detailed usage and performance metrics for specific exported
 * functions, interfaces, or resources from a component instance. This information is useful for
 * profiling, optimization, and monitoring individual export usage patterns.
 *
 * @since 1.0.0
 */
public interface ComponentExportStats {

  /**
   * Gets the name of the export these statistics apply to.
   *
   * <p>Returns the export name as defined in the component interface.
   *
   * @return the export name
   */
  String getExportName();

  /**
   * Gets the kind of export these statistics apply to.
   *
   * <p>Returns the type of export (function, interface, resource, etc.) that these statistics
   * track.
   *
   * @return the export kind
   */
  ComponentExportKind getExportKind();

  /**
   * Gets the total number of calls made to this export.
   *
   * <p>Returns the cumulative count of all invocations of this export since the component
   * instance was created or statistics were last reset.
   *
   * @return total call count
   */
  long getTotalCalls();

  /**
   * Gets the total execution time spent in this export.
   *
   * <p>Returns the cumulative time spent executing code within this export across all
   * invocations.
   *
   * @return total execution time
   */
  Duration getTotalExecutionTime();

  /**
   * Gets the average execution time per call.
   *
   * <p>Returns the mean execution time across all calls to this export.
   *
   * @return average execution time per call
   */
  Duration getAverageExecutionTime();

  /**
   * Gets the minimum execution time recorded.
   *
   * <p>Returns the shortest execution time observed for any single call to this export.
   *
   * @return minimum execution time
   */
  Duration getMinExecutionTime();

  /**
   * Gets the maximum execution time recorded.
   *
   * <p>Returns the longest execution time observed for any single call to this export.
   *
   * @return maximum execution time
   */
  Duration getMaxExecutionTime();

  /**
   * Gets the number of errors that occurred during export calls.
   *
   * <p>Returns the count of exceptions, traps, or other error conditions that occurred when
   * calling this export.
   *
   * @return error count
   */
  long getErrorCount();

  /**
   * Gets the error rate as a percentage.
   *
   * <p>Returns the percentage of calls that resulted in errors (errors / total calls * 100).
   *
   * @return error rate percentage (0.0 to 100.0)
   */
  double getErrorRate();

  /**
   * Gets the timestamp of the first call to this export.
   *
   * <p>Returns the time when this export was first invoked, or null if it has never been called.
   *
   * @return first call timestamp, or null if never called
   */
  Instant getFirstCallTime();

  /**
   * Gets the timestamp of the most recent call to this export.
   *
   * <p>Returns the time when this export was last invoked, or null if it has never been called.
   *
   * @return last call timestamp, or null if never called
   */
  Instant getLastCallTime();

  /**
   * Gets the number of concurrent calls currently executing.
   *
   * <p>Returns the count of calls to this export that are currently in progress across all
   * threads.
   *
   * @return current concurrent call count
   */
  int getConcurrentCalls();

  /**
   * Gets the maximum concurrent calls ever recorded.
   *
   * <p>Returns the highest number of concurrent calls to this export that were executing
   * simultaneously at any point in time.
   *
   * @return maximum concurrent calls
   */
  int getMaxConcurrentCalls();

  /**
   * Gets the total memory allocated by this export.
   *
   * <p>Returns the cumulative amount of memory allocated during execution of this export across
   * all calls.
   *
   * @return total memory allocated in bytes
   */
  long getTotalMemoryAllocated();

  /**
   * Gets the average memory allocated per call.
   *
   * <p>Returns the mean memory allocation per invocation of this export.
   *
   * @return average memory allocated per call in bytes
   */
  long getAverageMemoryPerCall();

  /**
   * Gets the current throughput in calls per second.
   *
   * <p>Returns the recent throughput calculated over a sliding time window.
   *
   * @return current throughput in calls per second
   */
  double getCurrentThroughput();

  /**
   * Resets all statistics for this export to zero.
   *
   * <p>Clears all accumulated statistics while preserving the export name and kind. Timestamps
   * are reset to null and counters are reset to zero.
   */
  void reset();
}