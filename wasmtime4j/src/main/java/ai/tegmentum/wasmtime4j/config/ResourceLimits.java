package ai.tegmentum.wasmtime4j.config;

/**
 * Resource limits configuration interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ResourceLimits {

  /**
   * Gets the maximum memory limit in bytes.
   *
   * @return the memory limit in bytes
   */
  long getMemoryLimitBytes();

  /**
   * Gets the maximum execution time in milliseconds.
   *
   * @return the time limit in milliseconds
   */
  long getExecutionTimeLimitMs();

  /**
   * Gets the maximum stack depth.
   *
   * @return the maximum stack depth
   */
  int getMaxStackDepth();

  /**
   * Checks if resource limits are enabled.
   *
   * @return true if resource limits are enabled
   */
  boolean isEnabled();
}
