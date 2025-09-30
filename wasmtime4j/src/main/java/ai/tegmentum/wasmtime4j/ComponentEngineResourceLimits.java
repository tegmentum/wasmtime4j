package ai.tegmentum.wasmtime4j;

/**
 * Resource limits for WebAssembly component engines.
 *
 * @since 1.0.0
 */
public interface ComponentEngineResourceLimits {

  /**
   * Gets the memory limit in bytes.
   *
   * @return the memory limit in bytes
   */
  long getMemoryLimit();

  /**
   * Gets the execution time limit in milliseconds.
   *
   * @return the time limit in milliseconds
   */
  long getTimeLimit();

  /**
   * Gets the maximum number of instances.
   *
   * @return the maximum instances
   */
  int getMaxInstances();

  /**
   * Checks if resource limiting is enabled.
   *
   * @return true if resource limiting is enabled
   */
  boolean isResourceLimitingEnabled();
}
