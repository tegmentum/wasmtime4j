package ai.tegmentum.wasmtime4j;

/**
 * Configuration for WebAssembly component loading.
 *
 * @since 1.0.0
 */
public interface ComponentLoadConfig {

  /**
   * Gets the component source path.
   *
   * @return the source path
   */
  String getSourcePath();

  /**
   * Gets the load timeout in milliseconds.
   *
   * @return the timeout in milliseconds
   */
  long getLoadTimeout();

  /**
   * Checks if validation is enabled during loading.
   *
   * @return true if validation is enabled
   */
  boolean isValidationEnabled();

  /**
   * Gets the load strategy.
   *
   * @return the load strategy
   */
  String getLoadStrategy();
}
