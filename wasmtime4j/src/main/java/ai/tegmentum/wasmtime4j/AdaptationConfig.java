package ai.tegmentum.wasmtime4j;

/**
 * Adaptation configuration interface for WebAssembly interface evolution.
 *
 * @since 1.0.0
 */
public interface AdaptationConfig {
  /**
   * Gets adaptation strategy.
   *
   * @return adaptation strategy
   */
  AdaptationStrategy getStrategy();

  /** Adaptation strategy enumeration. */
  enum AdaptationStrategy {
    AUTOMATIC,
    MANUAL,
    HYBRID
  }
}
