package ai.tegmentum.wasmtime4j.execution;

/**
 * Execution status enumeration for WebAssembly components.
 *
 * @since 1.0.0
 */
public enum ExecutionStatus {
  /** Execution is pending. */
  PENDING,
  /** Execution is initializing. */
  INITIALIZING,
  /** Execution is running. */
  RUNNING,
  /** Execution is suspended. */
  SUSPENDED,
  /** Execution is paused. */
  PAUSED,
  /** Execution completed successfully. */
  COMPLETED,
  /** Execution failed with error. */
  FAILED,
  /** Execution was terminated. */
  TERMINATED,
  /** Execution was cancelled. */
  CANCELLED,
  /** Execution timed out. */
  TIMED_OUT,
  /** Execution is cleaning up. */
  CLEANING_UP,
  /** Execution is in error recovery. */
  ERROR_RECOVERY,
  /** Execution status is unknown. */
  UNKNOWN
}
