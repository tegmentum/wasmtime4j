package ai.tegmentum.wasmtime4j.component;

/**
 * A {@link TaskExit} implementation representing an already-completed task.
 *
 * <p>This is used when concurrent calls complete synchronously and no further waiting is needed.
 *
 * @since 1.1.0
 */
final class CompletedTaskExit implements TaskExit {

  /** Singleton instance since completed tasks are stateless. */
  static final CompletedTaskExit INSTANCE = new CompletedTaskExit();

  private CompletedTaskExit() {}

  @Override
  public void block() {
    // Already completed; nothing to wait for.
  }

  @Override
  public boolean isCompleted() {
    return true;
  }

  @Override
  public String toString() {
    return "TaskExit[completed]";
  }
}
