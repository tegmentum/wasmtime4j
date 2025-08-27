package ai.tegmentum.wasmtime4j;

/**
 * Optimization levels for WebAssembly compilation.
 *
 * <p>These levels control the trade-off between compilation time and runtime performance. Higher
 * optimization levels may take longer to compile but produce faster code.
 *
 * @since 1.0.0
 */
public enum OptimizationLevel {
  /** No optimization - fastest compilation, slowest execution. */
  NONE,

  /** Optimize for runtime speed - slower compilation, fastest execution. */
  SPEED,

  /** Optimize for code size - moderate compilation time, smaller code size. */
  SIZE,

  /** Optimize for both speed and size - balanced approach. */
  SPEED_AND_SIZE
}
