package ai.tegmentum.wasmtime4j;

/**
 * Statistics for WebAssembly component engines.
 *
 * @since 1.0.0
 */
public interface ComponentEngineStatistics {

  /**
   * Gets the total number of components loaded.
   *
   * @return the component count
   */
  long getComponentCount();

  /**
   * Gets the total number of instances created.
   *
   * @return the instance count
   */
  long getInstanceCount();

  /**
   * Gets the total memory usage in bytes.
   *
   * @return the memory usage in bytes
   */
  long getMemoryUsage();

  /**
   * Gets the uptime in milliseconds.
   *
   * @return the uptime in milliseconds
   */
  long getUptime();
}
