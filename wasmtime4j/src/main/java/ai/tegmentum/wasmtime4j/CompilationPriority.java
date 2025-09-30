package ai.tegmentum.wasmtime4j;

/**
 * Compilation priority levels for streaming compilation.
 *
 * <p>CompilationPriority controls the resource allocation and scheduling priority for WebAssembly
 * streaming compilation operations.
 *
 * @since 1.0.0
 */
public enum CompilationPriority {
  /**
   * Low priority compilation.
   *
   * <p>Uses minimal system resources and runs in the background. Suitable for precompilation of
   * modules that may be used in the future.
   */
  LOW,

  /**
   * Normal priority compilation.
   *
   * <p>Balanced resource usage suitable for most applications. This is the default priority level.
   */
  NORMAL,

  /**
   * High priority compilation.
   *
   * <p>Uses more system resources and prioritizes compilation speed. Suitable for interactive
   * applications where fast compilation is critical.
   */
  HIGH,

  /**
   * Critical priority compilation.
   *
   * <p>Uses maximum available resources and preempts lower priority compilations. Use sparingly for
   * truly time-critical compilation scenarios.
   */
  CRITICAL
}
