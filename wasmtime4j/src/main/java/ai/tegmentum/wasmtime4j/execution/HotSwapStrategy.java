package ai.tegmentum.wasmtime4j.execution;

/**
 * Hot swap strategy interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface HotSwapStrategy {

  /**
   * Gets the strategy name.
   *
   * @return strategy name
   */
  String getName();

  /**
   * Gets the strategy type.
   *
   * @return strategy type
   */
  StrategyType getType();

  /** Strategy type enumeration. */
  enum StrategyType {
    /** Rolling update strategy. */
    ROLLING_UPDATE,
    /** Blue-green deployment strategy. */
    BLUE_GREEN,
    /** Canary deployment strategy. */
    CANARY,
    /** Instant replacement strategy. */
    INSTANT_REPLACEMENT,
    /** Gradual migration strategy. */
    GRADUAL_MIGRATION
  }
}
