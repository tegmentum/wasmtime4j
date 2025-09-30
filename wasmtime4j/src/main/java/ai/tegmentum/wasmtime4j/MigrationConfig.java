package ai.tegmentum.wasmtime4j;

/**
 * Migration configuration interface for WebAssembly interface evolution.
 *
 * @since 1.0.0
 */
public interface MigrationConfig {
  /**
   * Gets migration strategy.
   *
   * @return migration strategy
   */
  MigrationStrategy getStrategy();

  /** Migration strategy enumeration. */
  enum MigrationStrategy {
    IMMEDIATE,
    PHASED,
    ROLLBACK_ON_FAILURE
  }
}
