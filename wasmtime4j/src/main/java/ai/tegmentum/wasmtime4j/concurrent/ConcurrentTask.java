package ai.tegmentum.wasmtime4j.concurrent;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * A task that can be executed concurrently with access to store data.
 *
 * <p>This functional interface represents a task that runs in the context of a store's concurrent
 * execution environment. The task receives an {@link Accessor} that provides thread-safe access to
 * the store's user data.
 *
 * @param <T> the type of user data in the store
 * @param <R> the type of the task result
 * @since 1.0.0
 */
@FunctionalInterface
public interface ConcurrentTask<T, R> {

  /**
   * Executes the concurrent task with access to store data.
   *
   * @param accessor the accessor providing thread-safe access to store data
   * @return the task result
   * @throws WasmException if the task fails
   * @since 1.0.0
   */
  R execute(final Accessor<T> accessor) throws WasmException;
}
