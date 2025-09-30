package ai.tegmentum.wasmtime4j;

/**
 * Debug information for WebAssembly component engines.
 *
 * @since 1.0.0
 */
public interface ComponentEngineDebugInfo {

  /**
   * Gets the debug level.
   *
   * @return the debug level
   */
  String getDebugLevel();

  /**
   * Checks if debugging is enabled.
   *
   * @return true if debugging is enabled
   */
  boolean isDebugEnabled();

  /**
   * Gets debug statistics.
   *
   * @return debug statistics as a string
   */
  String getDebugStatistics();
}
