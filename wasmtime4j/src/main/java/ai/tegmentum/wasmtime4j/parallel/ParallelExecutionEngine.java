package ai.tegmentum.wasmtime4j.parallel;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.async.AsyncFunctionCall;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Parallel execution engine for WebAssembly operations with multi-threading support.
 *
 * <p>This engine provides advanced parallel execution capabilities, including concurrent
 * instance creation, parallel function execution, and load balancing across threads.
 *
 * @since 1.0.0
 */
public interface ParallelExecutionEngine extends Engine {

  /**
   * Creates multiple WebAssembly instances in parallel.
   *
   * @param module the compiled WebAssembly module
   * @param count number of instances to create
   * @return CompletableFuture containing the parallel instantiation result
   */
  CompletableFuture<ParallelInstantiationResult> createInstancesParallel(Module module, int count);

  /**
   * Creates instances in parallel with different modules.
   *
   * @param modules array of compiled WebAssembly modules
   * @return CompletableFuture containing the parallel instantiation result
   */
  CompletableFuture<ParallelInstantiationResult> createInstancesParallel(Module... modules);

  /**
   * Executes function calls in parallel across multiple instances.
   *
   * @param instances array of WebAssembly instances
   * @param functionCalls array of function calls to execute
   * @return CompletableFuture containing the parallel execution result
   */
  CompletableFuture<ParallelExecutionResult> executeParallel(
      Instance<?>[] instances, AsyncFunctionCall... functionCalls);

  /**
   * Executes the same function call on multiple instances in parallel.
   *
   * @param instances array of WebAssembly instances
   * @param functionCall function call to execute on all instances
   * @return CompletableFuture containing the parallel execution result
   */
  CompletableFuture<ParallelExecutionResult> executeBroadcast(
      Instance<?>[] instances, AsyncFunctionCall functionCall);

  /**
   * Executes function calls with load balancing across available instances.
   *
   * @param instancePool pool of available instances
   * @param functionCalls queue of function calls to execute
   * @return CompletableFuture containing the load-balanced execution result
   */
  CompletableFuture<LoadBalancedExecutionResult> executeLoadBalanced(
      InstancePool instancePool, List<AsyncFunctionCall> functionCalls);

  /**
   * Creates an instance pool for load-balanced execution.
   *
   * @param module the compiled WebAssembly module
   * @param poolConfig pool configuration
   * @return CompletableFuture containing the instance pool
   */
  CompletableFuture<InstancePool> createInstancePool(Module module, InstancePoolConfig poolConfig);

  /**
   * Gets the parallel execution configuration.
   *
   * @return parallel execution configuration
   */
  ParallelExecutionConfig getParallelConfig();

  /**
   * Sets the parallel execution configuration.
   *
   * @param config parallel execution configuration
   * @return this engine for method chaining
   */
  ParallelExecutionEngine setParallelConfig(ParallelExecutionConfig config);

  /**
   * Gets statistics about parallel execution operations.
   *
   * @return parallel execution statistics
   */
  ParallelExecutionStatistics getStatistics();

  /**
   * Sets a progress callback for parallel operations.
   *
   * @param callback progress callback
   * @return this engine for method chaining
   */
  ParallelExecutionEngine setProgressCallback(Consumer<ParallelExecutionProgress> callback);

  /**
   * Enables or disables automatic load balancing.
   *
   * @param enabled true to enable load balancing
   * @return this engine for method chaining
   */
  ParallelExecutionEngine setAutoLoadBalancing(boolean enabled);

  /**
   * Sets the load balancing strategy.
   *
   * @param strategy load balancing strategy
   * @return this engine for method chaining
   */
  ParallelExecutionEngine setLoadBalancingStrategy(LoadBalancingStrategy strategy);

  /**
   * Enables or disables fault tolerance for parallel execution.
   *
   * @param enabled true to enable fault tolerance
   * @return this engine for method chaining
   */
  ParallelExecutionEngine setFaultTolerance(boolean enabled);

  /**
   * Sets the maximum number of retry attempts for failed executions.
   *
   * @param maxRetries maximum retry attempts
   * @return this engine for method chaining
   */
  ParallelExecutionEngine setMaxRetries(int maxRetries);

  /**
   * Cancels all pending parallel operations.
   *
   * @return CompletableFuture that completes when all operations are cancelled
   */
  CompletableFuture<Void> cancelAllOperations();

  /**
   * Gets the current system load information.
   *
   * @return system load information
   */
  SystemLoadInfo getSystemLoadInfo();

  /**
   * Optimizes thread allocation based on current system load.
   *
   * @return CompletableFuture that completes when optimization is done
   */
  CompletableFuture<ThreadAllocationResult> optimizeThreadAllocation();

  /**
   * Creates a parallel execution engine with default configuration.
   *
   * @param baseEngine base engine to wrap
   * @return new parallel execution engine
   */
  static ParallelExecutionEngine create(final Engine baseEngine) {
    return create(baseEngine, ParallelExecutionConfig.defaultConfig());
  }

  /**
   * Creates a parallel execution engine with custom configuration.
   *
   * @param baseEngine base engine to wrap
   * @param config parallel execution configuration
   * @return new parallel execution engine
   */
  static ParallelExecutionEngine create(final Engine baseEngine, final ParallelExecutionConfig config) {
    // This will be implemented by the factory pattern
    throw new UnsupportedOperationException("Implementation provided by runtime factory");
  }
}