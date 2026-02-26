package ai.tegmentum.wasmtime4j.component;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * A task that executes within a concurrent accessor scope.
 *
 * <p>This functional interface represents work to be done within a {@code run_concurrent} scope.
 * The task receives an {@link Accessor} that can be used to make concurrent component function
 * calls.
 *
 * @param <T> the result type of the task
 * @since 1.1.0
 */
@FunctionalInterface
public interface AccessorTask<T> {

  /**
   * Executes the task within the concurrent scope.
   *
   * @param accessor the accessor for making concurrent calls
   * @return the task result
   * @throws WasmException if execution fails
   */
  T execute(Accessor accessor) throws WasmException;
}
