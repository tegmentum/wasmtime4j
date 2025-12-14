package ai.tegmentum.wasmtime4j.concurrent;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.Callable;

/**
 * A task that can be spawned for concurrent execution.
 *
 * <p>This interface extends {@link Callable} to provide a task that can be spawned via {@link
 * ai.tegmentum.wasmtime4j.Store#spawn(SpawnableTask)}.
 *
 * @param <T> the type of the task result
 * @since 1.0.0
 */
@FunctionalInterface
public interface SpawnableTask<T> extends Callable<T> {

  /**
   * Executes the spawned task.
   *
   * @return the task result
   * @throws WasmException if the task fails
   * @since 1.0.0
   */
  T run() throws WasmException;

  /**
   * {@inheritDoc}
   *
   * <p>This default implementation delegates to {@link #run()}.
   */
  @Override
  default T call() throws Exception {
    return run();
  }
}
