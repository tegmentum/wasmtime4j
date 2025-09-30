package ai.tegmentum.wasmtime4j;

/**
 * Represents the lifecycle state of a WebAssembly instance.
 *
 * <p>Instance state tracking provides visibility into the instance lifecycle for proper resource
 * management and debugging.
 *
 * @since 1.0.0
 */
public enum InstanceState {
  /** Instance is being created but not yet ready for use. */
  CREATING,

  /** Instance has been successfully created and is ready for use. */
  CREATED,

  /** Instance is currently executing WebAssembly code. */
  RUNNING,

  /** Instance execution has been suspended or paused. */
  SUSPENDED,

  /** Instance is in an error state due to execution failure. */
  ERROR,

  /** Instance has been explicitly disposed and resources cleaned up. */
  DISPOSED,

  /** Instance is being destroyed (cleanup in progress). */
  DESTROYING
}
