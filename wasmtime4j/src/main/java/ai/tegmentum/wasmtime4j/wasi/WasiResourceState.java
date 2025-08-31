package ai.tegmentum.wasmtime4j.wasi;

/**
 * Enumeration of WASI resource states.
 *
 * <p>Resource states track the lifecycle and current condition of WASI resources. States are
 * resource-type-specific but follow common patterns for creation, usage, and cleanup.
 *
 * @since 1.0.0
 */
public enum WasiResourceState {

  /**
   * Resource has been created but not yet initialized or opened.
   */
  CREATED,

  /**
   * Resource is open and ready for operations.
   */
  OPEN,

  /**
   * Resource is currently being used in an operation.
   */
  ACTIVE,

  /**
   * Resource is temporarily unavailable but can be reactivated.
   */
  SUSPENDED,

  /**
   * Resource has encountered an error and may not be usable.
   */
  ERROR,

  /**
   * Resource has been closed and is no longer usable.
   */
  CLOSED;

  /**
   * Checks if this state represents a usable resource.
   *
   * @return true if the resource can be used for operations
   */
  public boolean isUsable() {
    switch (this) {
      case OPEN:
      case ACTIVE:
        return true;
      case CREATED:
      case SUSPENDED:
      case ERROR:
      case CLOSED:
        return false;
      default:
        return false;
    }
  }

  /**
   * Checks if this state is terminal (resource cannot be reused).
   *
   * @return true if the state is terminal
   */
  public boolean isTerminal() {
    return this == CLOSED || this == ERROR;
  }
}