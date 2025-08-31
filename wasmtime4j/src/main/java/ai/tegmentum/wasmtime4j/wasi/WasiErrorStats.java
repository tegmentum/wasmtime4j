package ai.tegmentum.wasmtime4j.wasi;

import java.util.Map;

/**
 * Error statistics for WASI components and instances.
 *
 * @since 1.0.0
 */
public interface WasiErrorStats {

  /**
   * Gets the total number of errors encountered.
   *
   * @return total error count
   */
  long getTotalErrors();

  /**
   * Gets error counts broken down by error type.
   *
   * @return map of error types to counts
   */
  Map<String, Long> getErrorsByType();

  /**
   * Gets the number of fatal errors that terminated execution.
   *
   * @return fatal error count
   */
  long getFatalErrors();

  /**
   * Gets the number of recoverable errors that were handled.
   *
   * @return recoverable error count
   */
  long getRecoverableErrors();
}