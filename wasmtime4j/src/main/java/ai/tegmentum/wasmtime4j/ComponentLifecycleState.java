package ai.tegmentum.wasmtime4j;

/**
 * Represents the lifecycle state of a WebAssembly component.
 *
 * <p>Components progress through various lifecycle states from creation through disposal, with
 * different capabilities available in each state.
 *
 * @since 1.0.0
 */
public enum ComponentLifecycleState {
  /** Component is being created and initialized. */
  CREATING,

  /** Component is loaded and ready for instantiation. */
  READY,

  /** Component is active and can be instantiated. */
  ACTIVE,

  /** Component is suspended and temporarily unavailable. */
  SUSPENDED,

  /** Component is being updated or modified. */
  UPDATING,

  /** Component is deprecated but still functional. */
  DEPRECATED,

  /** Component is in an error state. */
  ERROR,

  /** Component is being destroyed. */
  DESTROYING,

  /** Component has been destroyed and is no longer usable. */
  DESTROYED
}
