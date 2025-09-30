package ai.tegmentum.wasmtime4j.config;

/**
 * Compilation strategy enumeration for WebAssembly components.
 *
 * @since 1.0.0
 */
public enum CompilationStrategy {
  /** Auto-detect best compilation strategy. */
  AUTO,
  /** Optimize for compilation speed. */
  SPEED,
  /** Optimize for runtime performance. */
  PERFORMANCE,
  /** Optimize for memory usage. */
  SIZE,
  /** Default strategy. */
  DEFAULT
}
