package ai.tegmentum.wasmtime4j.execution;

/**
 * Execution policy interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ExecutionPolicy {

  /**
   * Gets the policy name.
   *
   * @return policy name
   */
  String getPolicyName();

  /**
   * Checks if the operation is allowed.
   *
   * @param operation operation to check
   * @return true if allowed
   */
  boolean isAllowed(String operation);

  /**
   * Gets the maximum execution time.
   *
   * @return max execution time in milliseconds
   */
  long getMaxExecutionTime();

  /**
   * Gets the maximum memory usage.
   *
   * @return max memory usage in bytes
   */
  long getMaxMemoryUsage();

  /**
   * Gets the maximum CPU usage percentage.
   *
   * @return max CPU usage (0.0-1.0)
   */
  double getMaxCpuUsage();

  /**
   * Gets restricted operations.
   *
   * @return set of restricted operations
   */
  java.util.Set<String> getRestrictedOperations();

  /**
   * Gets allowed operations.
   *
   * @return set of allowed operations
   */
  java.util.Set<String> getAllowedOperations();

  /**
   * Gets the policy enforcement level.
   *
   * @return enforcement level
   */
  EnforcementLevel getEnforcementLevel();

  /**
   * Gets policy violations handling.
   *
   * @return violation handling
   */
  ViolationHandling getViolationHandling();

  /** Enforcement level enumeration. */
  enum EnforcementLevel {
    /** Log violations only. */
    ADVISORY,
    /** Warn on violations. */
    WARNING,
    /** Block violating operations. */
    BLOCKING,
    /** Terminate on violations. */
    STRICT
  }

  /** Violation handling enumeration. */
  enum ViolationHandling {
    /** Ignore violations. */
    IGNORE,
    /** Log violations. */
    LOG,
    /** Throw exceptions. */
    EXCEPTION,
    /** Terminate execution. */
    TERMINATE
  }
}
