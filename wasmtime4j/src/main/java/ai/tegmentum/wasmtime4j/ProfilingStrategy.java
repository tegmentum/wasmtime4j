package ai.tegmentum.wasmtime4j;

/**
 * Profiling strategies for WebAssembly execution.
 *
 * <p>These strategies control how profiling information is collected during WebAssembly
 * execution, which can be useful for performance analysis and optimization.
 *
 * @since 1.0.0
 */
public enum ProfilingStrategy {
  /** No profiling - minimal overhead. */
  NONE,

  /** JIT function profiling - tracks JIT compilation events. */
  JIT_DUMP,

  /** Performance event profiling - tracks runtime performance events. */
  PERF_MAP,

  /** VTune profiling - integration with Intel VTune profiler. */
  VTUNE
}