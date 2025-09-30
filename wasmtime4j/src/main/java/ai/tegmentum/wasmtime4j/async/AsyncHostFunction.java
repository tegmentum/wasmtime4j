package ai.tegmentum.wasmtime4j.async;

import ai.tegmentum.wasmtime4j.Function;
import ai.tegmentum.wasmtime4j.WasmValue;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Asynchronous host function interface for non-blocking native operations.
 *
 * <p>This interface allows host functions to perform asynchronous operations such as I/O, network
 * requests, or other blocking operations without blocking the WebAssembly execution thread.
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface AsyncHostFunction {

  /**
   * Executes the host function asynchronously.
   *
   * @param caller the WebAssembly function that called this host function
   * @param args the arguments passed to the host function
   * @return CompletableFuture containing the function result
   */
  CompletableFuture<WasmValue[]> call(Function<?> caller, WasmValue[] args);

  /**
   * Gets the name of this host function. Default implementation returns the class simple name.
   *
   * @return function name
   */
  default String getName() {
    return getClass().getSimpleName();
  }

  /**
   * Gets the module name this function should be imported from. Default implementation returns
   * "host".
   *
   * @return module name
   */
  default String getModuleName() {
    return "host";
  }

  /**
   * Gets the expected parameter types for this host function. Default implementation returns empty
   * array (no parameters).
   *
   * @return array of parameter types
   */
  default Class<?>[] getParameterTypes() {
    return new Class<?>[0];
  }

  /**
   * Gets the expected return types for this host function. Default implementation returns empty
   * array (no return values).
   *
   * @return array of return types
   */
  default Class<?>[] getReturnTypes() {
    return new Class<?>[0];
  }

  /**
   * Gets the maximum execution time for this host function. Default implementation returns 30
   * seconds.
   *
   * @return maximum execution time
   */
  default Duration getMaxExecutionTime() {
    return Duration.ofSeconds(30);
  }

  /**
   * Checks if this host function supports cancellation. Default implementation returns true.
   *
   * @return true if cancellation is supported
   */
  default boolean supportsCancellation() {
    return true;
  }

  /**
   * Gets the preferred executor for this host function. Default implementation returns null (use
   * default executor).
   *
   * @return preferred executor, or null for default
   */
  default Executor getPreferredExecutor() {
    return null;
  }

  /**
   * Checks if this host function can be called concurrently. Default implementation returns true.
   *
   * @return true if concurrent calls are safe
   */
  default boolean isConcurrencySafe() {
    return true;
  }

  /**
   * Gets the priority of this host function for scheduling. Higher values indicate higher priority.
   * Default implementation returns 0 (normal priority).
   *
   * @return function priority
   */
  default int getPriority() {
    return 0;
  }

  /**
   * Called when the host function is registered with an instance. Default implementation does
   * nothing.
   *
   * @param instance the instance this function is registered with
   */
  default void onRegister(Object instance) {
    // Default: no action
  }

  /**
   * Called when the host function is unregistered from an instance. Default implementation does
   * nothing.
   *
   * @param instance the instance this function is unregistered from
   */
  default void onUnregister(Object instance) {
    // Default: no action
  }

  /**
   * Creates a simple async host function that returns immediately.
   *
   * @param name function name
   * @param result result to return
   * @return simple async host function
   */
  static AsyncHostFunction simple(final String name, final WasmValue... result) {
    return new SimpleAsyncHostFunction(name, result);
  }

  /**
   * Creates an async host function that executes a blocking operation.
   *
   * @param name function name
   * @param operation blocking operation to execute
   * @return async host function wrapper
   */
  static AsyncHostFunction blocking(final String name, final BlockingOperation operation) {
    return new BlockingAsyncHostFunction(name, operation);
  }

  /**
   * Creates an async host function with custom executor.
   *
   * @param name function name
   * @param executor custom executor
   * @param operation operation to execute
   * @return async host function with custom executor
   */
  static AsyncHostFunction withExecutor(
      final String name, final Executor executor, final AsyncOperation operation) {
    return new ExecutorAsyncHostFunction(name, executor, operation);
  }

  /** Interface for blocking operations that should be executed asynchronously. */
  @FunctionalInterface
  interface BlockingOperation {
    WasmValue[] execute(Function<?> caller, WasmValue[] args) throws Exception;
  }

  /** Interface for async operations. */
  @FunctionalInterface
  interface AsyncOperation {
    CompletableFuture<WasmValue[]> execute(Function<?> caller, WasmValue[] args);
  }

  /** Simple implementation that returns a fixed result. */
  class SimpleAsyncHostFunction implements AsyncHostFunction {
    private final String name;
    private final WasmValue[] result;

    public SimpleAsyncHostFunction(final String name, final WasmValue[] result) {
      this.name = name;
      this.result = result.clone();
    }

    @Override
    public CompletableFuture<WasmValue[]> call(final Function<?> caller, final WasmValue[] args) {
      return CompletableFuture.completedFuture(result.clone());
    }

    @Override
    public String getName() {
      return name;
    }
  }

  /** Implementation that wraps a blocking operation. */
  class BlockingAsyncHostFunction implements AsyncHostFunction {
    private final String name;
    private final BlockingOperation operation;

    public BlockingAsyncHostFunction(final String name, final BlockingOperation operation) {
      this.name = name;
      this.operation = operation;
    }

    @Override
    public CompletableFuture<WasmValue[]> call(final Function<?> caller, final WasmValue[] args) {
      return CompletableFuture.supplyAsync(
          () -> {
            try {
              return operation.execute(caller, args);
            } catch (Exception e) {
              throw new RuntimeException("Host function execution failed", e);
            }
          });
    }

    @Override
    public String getName() {
      return name;
    }
  }

  /** Implementation with custom executor. */
  class ExecutorAsyncHostFunction implements AsyncHostFunction {
    private final String name;
    private final Executor executor;
    private final AsyncOperation operation;

    /**
     * Creates a new ExecutorAsyncHostFunction.
     *
     * @param name the function name
     * @param executor the executor for async operations
     * @param operation the async operation to execute
     */
    public ExecutorAsyncHostFunction(
        final String name, final Executor executor, final AsyncOperation operation) {
      this.name = name;
      this.executor = executor;
      this.operation = operation;
    }

    @Override
    public CompletableFuture<WasmValue[]> call(final Function<?> caller, final WasmValue[] args) {
      final CompletableFuture<WasmValue[]> future = new CompletableFuture<>();
      executor.execute(
          () -> {
            try {
              operation
                  .execute(caller, args)
                  .whenComplete(
                      (result, throwable) -> {
                        if (throwable != null) {
                          future.completeExceptionally(throwable);
                        } else {
                          future.complete(result);
                        }
                      });
            } catch (Exception e) {
              future.completeExceptionally(e);
            }
          });
      return future;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Executor getPreferredExecutor() {
      return executor;
    }
  }
}
