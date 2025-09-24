package ai.tegmentum.wasmtime4j;

/**
 * Instantiation priority levels for streaming instantiation.
 *
 * <p>InstantiationPriority controls the resource allocation and scheduling priority for WebAssembly
 * streaming instantiation operations.
 *
 * @since 1.0.0
 */
public enum InstantiationPriority {
  /**
   * Low priority instantiation.
   *
   * <p>Uses minimal system resources and runs in the background. Suitable for pre-instantiation
   * of modules that may be used in the future.
   */
  LOW,

  /**
   * Normal priority instantiation.
   *
   * <p>Balanced resource usage suitable for most applications. This is the default priority level.
   */
  NORMAL,

  /**
   * High priority instantiation.
   *
   * <p>Uses more system resources and prioritizes instantiation speed. Suitable for interactive
   * applications where fast instantiation is critical.
   */
  HIGH,

  /**
   * Critical priority instantiation.
   *
   * <p>Uses maximum available resources and preempts lower priority instantiations. Use sparingly
   * for truly time-critical instantiation scenarios.
   */
  CRITICAL
}