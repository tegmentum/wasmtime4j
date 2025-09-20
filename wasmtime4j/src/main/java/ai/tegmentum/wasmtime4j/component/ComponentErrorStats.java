package ai.tegmentum.wasmtime4j.component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Statistics for errors and exceptions in component execution.
 *
 * <p>ComponentErrorStats tracks error patterns, exception frequencies, and recovery metrics for
 * component instance execution. This information is valuable for debugging, monitoring, and
 * improving component reliability.
 *
 * @since 1.0.0
 */
public interface ComponentErrorStats {

  /**
   * Gets the total number of errors encountered.
   *
   * <p>Returns the cumulative count of all errors, exceptions, and traps that occurred during
   * component execution since creation or last reset.
   *
   * @return total error count
   */
  long getTotalErrorCount();

  /**
   * Gets the number of runtime errors.
   *
   * <p>Returns the count of runtime errors such as WebAssembly traps, out-of-bounds access, and
   * other execution-time failures.
   *
   * @return runtime error count
   */
  long getRuntimeErrorCount();

  /**
   * Gets the number of validation errors.
   *
   * <p>Returns the count of validation errors encountered during type checking, import
   * resolution, or other validation phases.
   *
   * @return validation error count
   */
  long getValidationErrorCount();

  /**
   * Gets the number of resource errors.
   *
   * <p>Returns the count of errors related to resource management, allocation failures, and
   * resource lifecycle violations.
   *
   * @return resource error count
   */
  long getResourceErrorCount();

  /**
   * Gets the number of interface errors.
   *
   * <p>Returns the count of errors that occurred during cross-component interface calls and
   * interface type mismatches.
   *
   * @return interface error count
   */
  long getInterfaceErrorCount();

  /**
   * Gets error statistics by error type.
   *
   * <p>Returns detailed error counts grouped by specific error types or exception classes.
   *
   * @return map of error types to their occurrence counts
   */
  Map<String, Long> getErrorCountsByType();

  /**
   * Gets error statistics by export.
   *
   * <p>Returns error counts broken down by which export function or interface the errors
   * occurred in.
   *
   * @return map of export names to their error counts
   */
  Map<String, Long> getErrorCountsByExport();

  /**
   * Gets the most recent errors.
   *
   * <p>Returns a list of the most recently encountered errors with their details and timestamps.
   * The list is ordered from most recent to oldest.
   *
   * @return list of recent error information
   */
  List<ComponentErrorInfo> getRecentErrors();

  /**
   * Gets the most frequent error types.
   *
   * <p>Returns a list of error types ordered by frequency of occurrence, with the most common
   * errors first.
   *
   * @return list of error types ordered by frequency
   */
  List<String> getMostFrequentErrorTypes();

  /**
   * Gets the number of recovered errors.
   *
   * <p>Returns the count of errors that were successfully handled and recovered from without
   * terminating the component instance.
   *
   * @return recovered error count
   */
  long getRecoveredErrorCount();

  /**
   * Gets the number of fatal errors.
   *
   * <p>Returns the count of errors that caused the component instance to become unusable or
   * required termination.
   *
   * @return fatal error count
   */
  long getFatalErrorCount();

  /**
   * Gets the error rate as a percentage.
   *
   * <p>Returns the percentage of operations that resulted in errors relative to total operations
   * performed.
   *
   * @return error rate percentage (0.0 to 100.0)
   */
  double getErrorRate();

  /**
   * Gets the timestamp of the first error.
   *
   * <p>Returns the time when the first error occurred in this component instance, or null if no
   * errors have occurred.
   *
   * @return first error timestamp, or null if no errors
   */
  Instant getFirstErrorTime();

  /**
   * Gets the timestamp of the most recent error.
   *
   * <p>Returns the time when the most recent error occurred, or null if no errors have occurred.
   *
   * @return last error timestamp, or null if no errors
   */
  Instant getLastErrorTime();

  /**
   * Gets the current error trend.
   *
   * <p>Returns information about whether errors are increasing, decreasing, or stable over recent
   * time periods.
   *
   * @return error trend analysis
   */
  ComponentErrorTrend getErrorTrend();

  /**
   * Resets all error statistics to zero.
   *
   * <p>Clears all accumulated error statistics and history while preserving the component
   * instance state. Timestamps are reset and counters are cleared.
   */
  void reset();
}