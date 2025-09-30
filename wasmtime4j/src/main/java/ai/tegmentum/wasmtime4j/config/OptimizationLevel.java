package ai.tegmentum.wasmtime4j.config;

/**
 * Optimization level enumeration for WebAssembly components.
 *
 * @since 1.0.0
 */
public enum OptimizationLevel {
  /** No optimization. */
  NONE,
  /** Basic optimization. */
  BASIC,
  /** Standard optimization. */
  STANDARD,
  /** Aggressive optimization. */
  AGGRESSIVE,
  /** Maximum optimization. */
  MAXIMUM
}
