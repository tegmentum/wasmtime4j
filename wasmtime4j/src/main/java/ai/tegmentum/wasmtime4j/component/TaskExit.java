package ai.tegmentum.wasmtime4j.component;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Represents the completion of a concurrent guest task in the component model.
 *
 * <p>A {@code TaskExit} is returned from concurrent component function calls and represents a
 * handle to the task's completion. Calling {@link #block()} waits for all transitive subtasks to
 * complete, corresponding to the Wasmtime {@code TaskExit::block} method.
 *
 * <p>In the current implementation, concurrent calls via {@link
 * ai.tegmentum.wasmtime4j.Store#runConcurrent} block internally, so the returned {@code TaskExit}
 * will typically be already completed. This type exists for API completeness with the Wasmtime
 * {@code TaskExit} type and to support future non-blocking concurrent call patterns.
 *
 * @since 1.1.0
 */
public interface TaskExit {

  /**
   * Blocks until all transitive subtasks of this task have completed.
   *
   * <p>This corresponds to {@code TaskExit::block} in the Wasmtime Rust API. If the task has
   * already completed, this method returns immediately.
   *
   * @throws WasmException if the task failed or waiting was interrupted
   */
  void block() throws WasmException;

  /**
   * Returns whether this task has already completed.
   *
   * @return true if the task has completed and {@link #block()} will return immediately
   */
  boolean isCompleted();

  /**
   * Returns a {@code TaskExit} that is already completed.
   *
   * <p>This is useful when a concurrent call completes synchronously and no further waiting is
   * needed.
   *
   * @return a completed {@code TaskExit}
   */
  static TaskExit completed() {
    return CompletedTaskExit.INSTANCE;
  }
}
