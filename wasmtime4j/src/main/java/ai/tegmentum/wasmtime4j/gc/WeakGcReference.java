package ai.tegmentum.wasmtime4j.gc;

import java.util.Optional;

/**
 * A weak reference to a WebAssembly GC object.
 *
 * <p>Weak references do not prevent the garbage collection of their referents and can be used to
 * implement caches or other memory-sensitive collections. When the referenced object is garbage
 * collected, the weak reference is automatically cleared.
 *
 * @since 1.0.0
 */
public interface WeakGcReference {

  /**
   * Gets the referenced object if it has not been garbage collected.
   *
   * @return the referenced object, or empty if it has been collected
   */
  Optional<GcObject> get();

  /**
   * Checks if the referenced object has been garbage collected.
   *
   * @return true if the object has been collected and this reference is cleared
   */
  boolean isCleared();

  /** Forces the weak reference to be cleared. The finalization callback will not be invoked. */
  void clear();

  /**
   * Gets the finalization callback associated with this weak reference.
   *
   * @return the finalization callback, or null if none
   */
  Runnable getFinalizationCallback();

  /**
   * Registers or updates the finalization callback.
   *
   * @param callback the new finalization callback
   */
  void setFinalizationCallback(Runnable callback);
}
