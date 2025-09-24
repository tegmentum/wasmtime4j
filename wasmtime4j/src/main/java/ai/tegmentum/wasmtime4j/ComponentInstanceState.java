package ai.tegmentum.wasmtime4j;

/**
 * Represents the state of a WebAssembly component instance.
 *
 * <p>Component instances transition through various states during their lifecycle, from creation
 * through termination.
 *
 * @since 1.0.0
 */
public enum ComponentInstanceState {
  /** Instance is being created but not yet ready. */
  INITIALIZING,

  /** Instance is active and ready for use. */
  ACTIVE,

  /** Instance is suspended or paused. */
  SUSPENDED,

  /** Instance is in an error state. */
  ERROR,

  /** Instance is being terminated. */
  TERMINATING,

  /** Instance has been terminated and resources released. */
  TERMINATED
}
