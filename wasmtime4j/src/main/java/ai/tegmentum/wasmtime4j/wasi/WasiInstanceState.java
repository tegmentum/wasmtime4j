package ai.tegmentum.wasmtime4j.wasi;

/**
 * Enumeration of WASI component instance states.
 *
 * <p>Instance states track the lifecycle of component instances from creation through execution to
 * termination. State transitions are managed by the runtime and reflect the current execution
 * status.
 *
 * @since 1.0.0
 */
public enum WasiInstanceState {

  /**
   * Instance has been created but not yet started execution.
   *
   * <p>In this state, the instance has been successfully instantiated with its configuration but
   * no function calls have been made yet. Resources are allocated and the instance is ready for
   * execution.
   */
  CREATED,

  /**
   * Instance is currently executing a function call.
   *
   * <p>The instance is actively running component code. This may involve multiple nested function
   * calls and resource operations.
   */
  RUNNING,

  /**
   * Instance execution has been suspended.
   *
   * <p>The instance has been temporarily paused and retains all its state. Suspended instances
   * can be resumed to continue execution from where they left off.
   */
  SUSPENDED,

  /**
   * Instance is waiting for an asynchronous operation to complete.
   *
   * <p>The instance has initiated an async operation (such as I/O) and is waiting for completion.
   * This is distinct from suspension as it represents active waiting rather than manual pause.
   */
  WAITING,

  /**
   * Instance execution completed successfully.
   *
   * <p>All function calls have completed and the instance is no longer executing. The instance
   * remains valid and can accept new function calls.
   */
  COMPLETED,

  /**
   * Instance has been explicitly terminated.
   *
   * <p>Execution was forcibly stopped, possibly due to timeout, resource limits, or explicit
   * termination. The instance may not have completed its intended operations.
   */
  TERMINATED,

  /**
   * Instance encountered a fatal error.
   *
   * <p>An unrecoverable error occurred during execution, such as a trap, out-of-memory condition,
   * or security violation. The instance is no longer usable.
   */
  ERROR,

  /**
   * Instance has been closed and resources released.
   *
   * <p>The instance has been explicitly closed and all associated resources have been cleaned up.
   * The instance is no longer valid for any operations.
   */
  CLOSED;

  /**
   * Checks if this state represents an active execution state.
   *
   * <p>Active states are those where the instance is currently executing or capable of executing.
   *
   * @return true if the state is active (CREATED, RUNNING, SUSPENDED, WAITING, COMPLETED)
   */
  public boolean isActive() {
    switch (this) {
      case CREATED:
      case RUNNING:
      case SUSPENDED:
      case WAITING:
      case COMPLETED:
        return true;
      case TERMINATED:
      case ERROR:
      case CLOSED:
        return false;
      default:
        return false;
    }
  }

  /**
   * Checks if this state represents a terminal state.
   *
   * <p>Terminal states are those where the instance cannot transition to an active state.
   *
   * @return true if the state is terminal (TERMINATED, ERROR, CLOSED)
   */
  public boolean isTerminal() {
    switch (this) {
      case TERMINATED:
      case ERROR:
      case CLOSED:
        return true;
      case CREATED:
      case RUNNING:
      case SUSPENDED:
      case WAITING:
      case COMPLETED:
        return false;
      default:
        return false;
    }
  }

  /**
   * Checks if this state allows function calls.
   *
   * <p>Callable states are those where new function calls can be initiated.
   *
   * @return true if function calls are allowed (CREATED, COMPLETED)
   */
  public boolean isCallable() {
    switch (this) {
      case CREATED:
      case COMPLETED:
        return true;
      case RUNNING:
      case SUSPENDED:
      case WAITING:
      case TERMINATED:
      case ERROR:
      case CLOSED:
        return false;
      default:
        return false;
    }
  }

  /**
   * Checks if this state can be suspended.
   *
   * <p>Suspendable states are those where the instance can be paused.
   *
   * @return true if suspension is allowed (RUNNING, WAITING)
   */
  public boolean isSuspendable() {
    switch (this) {
      case RUNNING:
      case WAITING:
        return true;
      case CREATED:
      case SUSPENDED:
      case COMPLETED:
      case TERMINATED:
      case ERROR:
      case CLOSED:
        return false;
      default:
        return false;
    }
  }

  /**
   * Checks if this state can be resumed.
   *
   * <p>Resumable states are those where execution can be continued.
   *
   * @return true if resumption is allowed (SUSPENDED)
   */
  public boolean isResumable() {
    return this == SUSPENDED;
  }

  /**
   * Gets a human-readable description of this state.
   *
   * @return descriptive text for this state
   */
  public String getDescription() {
    switch (this) {
      case CREATED:
        return "Instance created and ready for execution";
      case RUNNING:
        return "Instance is currently executing";
      case SUSPENDED:
        return "Instance execution has been suspended";
      case WAITING:
        return "Instance is waiting for asynchronous operation";
      case COMPLETED:
        return "Instance execution completed successfully";
      case TERMINATED:
        return "Instance has been terminated";
      case ERROR:
        return "Instance encountered a fatal error";
      case CLOSED:
        return "Instance has been closed and resources released";
      default:
        return "Unknown state";
    }
  }
}