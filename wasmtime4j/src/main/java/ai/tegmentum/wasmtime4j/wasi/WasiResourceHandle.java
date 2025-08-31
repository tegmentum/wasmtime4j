package ai.tegmentum.wasmtime4j.wasi;

/**
 * Handle representing a WASI resource that can be passed between components.
 *
 * @since 1.0.0
 */
public interface WasiResourceHandle {

  /**
   * Gets the resource identifier.
   *
   * @return the resource ID
   */
  long getResourceId();

  /**
   * Gets the resource type.
   *
   * @return the resource type name
   */
  String getResourceType();

  /**
   * Gets the owning component instance.
   *
   * @return the component instance that owns this resource
   */
  WasiInstance getOwner();

  /**
   * Checks if this handle is still valid.
   *
   * @return true if valid, false otherwise
   */
  boolean isValid();
}