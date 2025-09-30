package ai.tegmentum.wasmtime4j;

/**
 * Configuration for WebAssembly component engine optimization.
 *
 * @since 1.0.0
 */
public interface ComponentEngineOptimizationConfig {

  /**
   * Gets the optimization level.
   *
   * @return the optimization level
   */
  String getOptimizationLevel();

  /**
   * Checks if optimization is enabled.
   *
   * @return true if optimization is enabled
   */
  boolean isOptimizationEnabled();

  /**
   * Gets the optimization timeout in milliseconds.
   *
   * @return the timeout in milliseconds
   */
  long getOptimizationTimeout();
}
