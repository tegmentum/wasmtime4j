package ai.tegmentum.wasmtime4j.wasi.extensions.threading;

/**
 * Enumeration of thread states in the WASI threading system.
 *
 * <p>Thread states represent the current execution status of a thread. These states correspond to
 * the standard Java thread states but are adapted for the WASI environment.
 *
 * @since 1.0.0
 */
public enum ThreadState {

  /**
   * Thread has been created but not yet started.
   *
   * <p>A thread in this state has been instantiated but the {@code start()} method has not been
   * called. The thread is not yet executing.
   */
  NEW,

  /**
   * Thread is executing or ready to execute.
   *
   * <p>A thread in this state is either currently running or is ready to run and waiting for the
   * thread scheduler to allocate CPU time.
   */
  RUNNABLE,

  /**
   * Thread is blocked waiting for a monitor lock.
   *
   * <p>A thread in this state is blocked waiting to enter a synchronized block or method, or
   * waiting to re-enter after calling wait().
   */
  BLOCKED,

  /**
   * Thread is waiting indefinitely for another thread to perform a specific action.
   *
   * <p>A thread enters this state as a result of calling:
   *
   * <ul>
   *   <li>{@code Object.wait()} with no timeout
   *   <li>{@code Thread.join()} with no timeout
   *   <li>{@code LockSupport.park()}
   * </ul>
   */
  WAITING,

  /**
   * Thread is waiting for another thread to perform an action for up to a specified time.
   *
   * <p>A thread enters this state as a result of calling:
   *
   * <ul>
   *   <li>{@code Thread.sleep()}
   *   <li>{@code Object.wait()} with timeout
   *   <li>{@code Thread.join()} with timeout
   *   <li>{@code LockSupport.parkNanos()}
   *   <li>{@code LockSupport.parkUntil()}
   * </ul>
   */
  TIMED_WAITING,

  /**
   * Thread has completed execution.
   *
   * <p>A thread in this state has finished executing its run() method and cannot be restarted.
   */
  TERMINATED;

  /**
   * Checks if this state represents an active thread.
   *
   * <p>Active threads are those that have been started but have not yet terminated. This includes
   * RUNNABLE, BLOCKED, WAITING, and TIMED_WAITING states.
   *
   * @return true if the thread is active, false otherwise
   */
  public boolean isActive() {
    return this != NEW && this != TERMINATED;
  }

  /**
   * Checks if this state represents a waiting thread.
   *
   * <p>Waiting threads are those that are blocked or waiting for some condition. This includes
   * BLOCKED, WAITING, and TIMED_WAITING states.
   *
   * @return true if the thread is waiting, false otherwise
   */
  public boolean isWaiting() {
    return this == BLOCKED || this == WAITING || this == TIMED_WAITING;
  }

  /**
   * Checks if this state represents a runnable thread.
   *
   * <p>A runnable thread is one that is either executing or ready to execute.
   *
   * @return true if the thread is runnable, false otherwise
   */
  public boolean isRunnable() {
    return this == RUNNABLE;
  }

  /**
   * Checks if this state represents a terminated thread.
   *
   * <p>A terminated thread has completed execution and cannot be restarted.
   *
   * @return true if the thread is terminated, false otherwise
   */
  public boolean isTerminated() {
    return this == TERMINATED;
  }

  /**
   * Gets a human-readable description of this thread state.
   *
   * @return a description of the thread state
   */
  public String getDescription() {
    switch (this) {
      case NEW:
        return "Thread created but not started";
      case RUNNABLE:
        return "Thread executing or ready to execute";
      case BLOCKED:
        return "Thread blocked waiting for monitor lock";
      case WAITING:
        return "Thread waiting indefinitely";
      case TIMED_WAITING:
        return "Thread waiting for specified time";
      case TERMINATED:
        return "Thread completed execution";
      default:
        return "Unknown thread state";
    }
  }

  /**
   * Converts a Java Thread.State to a WasiThreadState.
   *
   * @param javaState the Java thread state
   * @return the corresponding WASI thread state
   * @throws IllegalArgumentException if javaState is null
   */
  public static ThreadState fromJavaState(final Thread.State javaState) {
    if (javaState == null) {
      throw new IllegalArgumentException("Java thread state cannot be null");
    }

    switch (javaState) {
      case NEW:
        return NEW;
      case RUNNABLE:
        return RUNNABLE;
      case BLOCKED:
        return BLOCKED;
      case WAITING:
        return WAITING;
      case TIMED_WAITING:
        return TIMED_WAITING;
      case TERMINATED:
        return TERMINATED;
      default:
        throw new IllegalArgumentException("Unknown Java thread state: " + javaState);
    }
  }

  /**
   * Converts this WASI thread state to a Java Thread.State.
   *
   * @return the corresponding Java thread state
   */
  public Thread.State toJavaState() {
    switch (this) {
      case NEW:
        return Thread.State.NEW;
      case RUNNABLE:
        return Thread.State.RUNNABLE;
      case BLOCKED:
        return Thread.State.BLOCKED;
      case WAITING:
        return Thread.State.WAITING;
      case TIMED_WAITING:
        return Thread.State.TIMED_WAITING;
      case TERMINATED:
        return Thread.State.TERMINATED;
      default:
        throw new IllegalStateException("Unknown WASI thread state: " + this);
    }
  }
}
