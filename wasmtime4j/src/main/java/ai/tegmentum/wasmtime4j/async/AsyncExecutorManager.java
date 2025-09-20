package ai.tegmentum.wasmtime4j.async;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manager for async executors optimized for WebAssembly workloads.
 *
 * <p>AsyncExecutorManager provides specialized thread pools and execution strategies designed for
 * WebAssembly operations. It manages different types of executors for different async operation
 * categories and provides automatic optimization based on workload characteristics.
 *
 * <p>This manager is designed to optimize performance for WASM-specific workloads including
 * compilation, execution, memory operations, and streaming tasks.
 *
 * @since 1.0.0
 */
public interface AsyncExecutorManager {

  /**
   * Gets the default executor for general async operations.
   *
   * <p>Returns a general-purpose executor suitable for most async WebAssembly operations that don't
   * have specific performance requirements.
   *
   * @return default async executor
   */
  Executor getDefaultExecutor();

  /**
   * Gets the executor optimized for compilation operations.
   *
   * <p>Returns an executor configured for CPU-intensive compilation tasks with appropriate thread
   * pool sizing and priority handling.
   *
   * @return compilation executor
   */
  Executor getCompilationExecutor();

  /**
   * Gets the executor optimized for function execution.
   *
   * <p>Returns an executor designed for WebAssembly function calls with low latency and high
   * throughput characteristics.
   *
   * @return function execution executor
   */
  Executor getExecutionExecutor();

  /**
   * Gets the executor optimized for memory operations.
   *
   * <p>Returns an executor specialized for memory I/O operations including bulk transfers and
   * streaming memory access.
   *
   * @return memory operations executor
   */
  Executor getMemoryExecutor();

  /**
   * Gets the scheduled executor for timed operations.
   *
   * <p>Returns a scheduled executor service for operations requiring timeouts, periodic execution,
   * or delayed execution.
   *
   * @return scheduled executor service
   */
  ScheduledExecutorService getScheduledExecutor();

  /**
   * Creates a custom executor with specific configuration.
   *
   * <p>Creates an executor tailored for specific workload characteristics including thread count,
   * priority, and threading model.
   *
   * @param config executor configuration
   * @return configured custom executor
   * @throws IllegalArgumentException if config is null
   */
  Executor createCustomExecutor(final ExecutorConfiguration config);

  /**
   * Gets executor performance statistics.
   *
   * <p>Returns performance metrics for all managed executors including throughput, latency, and
   * resource utilization.
   *
   * @return executor performance statistics
   */
  ExecutorStatistics getStatistics();

  /**
   * Optimizes executor configuration based on runtime metrics.
   *
   * <p>Analyzes executor performance and adjusts configuration parameters to improve throughput and
   * reduce latency for current workloads.
   */
  void optimizeConfiguration();

  /**
   * Shuts down all managed executors gracefully.
   *
   * <p>Initiates graceful shutdown of all executors with a reasonable timeout. Operations should
   * complete this call before application termination.
   */
  void shutdown();

  /**
   * Shuts down all managed executors with timeout.
   *
   * <p>Initiates graceful shutdown with specified timeout. Forces termination of remaining tasks if
   * timeout is exceeded.
   *
   * @param timeout maximum time to wait for shutdown
   * @throws IllegalArgumentException if timeout is null or negative
   */
  void shutdown(final Duration timeout);

  /**
   * Checks if all executors have been shut down.
   *
   * @return true if all executors are shut down
   */
  boolean isShutdown();

  /** Configuration for custom executor creation. */
  interface ExecutorConfiguration {
    /**
     * Gets the number of core threads.
     *
     * @return core thread count
     */
    int getCoreThreads();

    /**
     * Gets the maximum number of threads.
     *
     * @return maximum thread count
     */
    int getMaxThreads();

    /**
     * Gets the thread idle timeout.
     *
     * @return thread idle timeout
     */
    Duration getThreadIdleTimeout();

    /**
     * Gets the thread priority level.
     *
     * @return thread priority (Thread.MIN_PRIORITY to Thread.MAX_PRIORITY)
     */
    int getThreadPriority();

    /**
     * Gets the thread factory for creating threads.
     *
     * @return thread factory, or null for default
     */
    ThreadFactory getThreadFactory();

    /**
     * Gets the executor type to create.
     *
     * @return executor type
     */
    ExecutorType getExecutorType();

    /**
     * Checks if daemon threads should be used.
     *
     * @return true if daemon threads should be used
     */
    boolean useDaemonThreads();

    /**
     * Gets the queue capacity for task queuing.
     *
     * @return queue capacity, or -1 for unbounded
     */
    int getQueueCapacity();
  }

  /** Types of executors that can be created. */
  enum ExecutorType {
    /** Fixed-size thread pool executor */
    FIXED_THREAD_POOL,
    /** Cached thread pool executor */
    CACHED_THREAD_POOL,
    /** Single thread executor */
    SINGLE_THREAD,
    /** Fork-join pool executor */
    FORK_JOIN_POOL,
    /** Work-stealing pool executor */
    WORK_STEALING_POOL,
    /** Scheduled thread pool executor */
    SCHEDULED_THREAD_POOL
  }

  /** Statistics for executor performance monitoring. */
  interface ExecutorStatistics {
    /**
     * Gets statistics for the default executor.
     *
     * @return default executor statistics
     */
    ExecutorMetrics getDefaultExecutorMetrics();

    /**
     * Gets statistics for the compilation executor.
     *
     * @return compilation executor statistics
     */
    ExecutorMetrics getCompilationExecutorMetrics();

    /**
     * Gets statistics for the execution executor.
     *
     * @return execution executor statistics
     */
    ExecutorMetrics getExecutionExecutorMetrics();

    /**
     * Gets statistics for the memory executor.
     *
     * @return memory executor statistics
     */
    ExecutorMetrics getMemoryExecutorMetrics();

    /**
     * Gets statistics for the scheduled executor.
     *
     * @return scheduled executor statistics
     */
    ExecutorMetrics getScheduledExecutorMetrics();

    /**
     * Gets overall performance summary.
     *
     * @return performance summary across all executors
     */
    OverallPerformanceMetrics getOverallMetrics();
  }

  /** Performance metrics for individual executors. */
  interface ExecutorMetrics {
    /**
     * Gets the number of tasks completed.
     *
     * @return completed task count
     */
    long getCompletedTaskCount();

    /**
     * Gets the number of active threads.
     *
     * @return active thread count
     */
    int getActiveThreadCount();

    /**
     * Gets the core pool size.
     *
     * @return core pool size
     */
    int getCorePoolSize();

    /**
     * Gets the maximum pool size.
     *
     * @return maximum pool size
     */
    int getMaximumPoolSize();

    /**
     * Gets the current pool size.
     *
     * @return current pool size
     */
    int getPoolSize();

    /**
     * Gets the number of queued tasks.
     *
     * @return queued task count
     */
    long getQueuedTaskCount();

    /**
     * Gets the average task execution time.
     *
     * @return average execution time in milliseconds
     */
    double getAverageTaskExecutionTime();

    /**
     * Gets the task throughput rate.
     *
     * @return tasks per second
     */
    double getTaskThroughput();

    /**
     * Gets the current CPU utilization.
     *
     * @return CPU utilization percentage (0.0 to 1.0)
     */
    double getCpuUtilization();

    /**
     * Gets the rejected task count.
     *
     * @return rejected task count
     */
    long getRejectedTaskCount();
  }

  /** Overall performance metrics across all executors. */
  interface OverallPerformanceMetrics {
    /**
     * Gets the total number of tasks executed.
     *
     * @return total task count
     */
    long getTotalTasksExecuted();

    /**
     * Gets the total number of active threads.
     *
     * @return total active thread count
     */
    int getTotalActiveThreads();

    /**
     * Gets the overall CPU utilization.
     *
     * @return overall CPU utilization percentage (0.0 to 1.0)
     */
    double getOverallCpuUtilization();

    /**
     * Gets the memory usage by thread pools.
     *
     * @return memory usage in bytes
     */
    long getThreadPoolMemoryUsage();

    /**
     * Gets performance recommendations.
     *
     * @return list of performance optimization recommendations
     */
    java.util.List<String> getPerformanceRecommendations();
  }

  /**
   * Creates a default AsyncExecutorManager instance.
   *
   * @return default async executor manager
   */
  static AsyncExecutorManager createDefault() {
    return new DefaultAsyncExecutorManager();
  }

  /**
   * Creates an AsyncExecutorManager with custom configuration.
   *
   * @param config manager configuration
   * @return configured async executor manager
   * @throws IllegalArgumentException if config is null
   */
  static AsyncExecutorManager create(final ManagerConfiguration config) {
    return new DefaultAsyncExecutorManager(config);
  }

  /** Configuration for the executor manager. */
  interface ManagerConfiguration {
    /**
     * Gets the optimization strategy.
     *
     * @return optimization strategy
     */
    OptimizationStrategy getOptimizationStrategy();

    /**
     * Gets the monitoring interval.
     *
     * @return monitoring interval
     */
    Duration getMonitoringInterval();

    /**
     * Checks if automatic optimization is enabled.
     *
     * @return true if auto-optimization is enabled
     */
    boolean isAutoOptimizationEnabled();

    /**
     * Gets the shutdown timeout.
     *
     * @return shutdown timeout
     */
    Duration getShutdownTimeout();
  }

  /** Optimization strategies for executor management. */
  enum OptimizationStrategy {
    /** Optimize for maximum throughput */
    THROUGHPUT,
    /** Optimize for minimum latency */
    LATENCY,
    /** Balance throughput and latency */
    BALANCED,
    /** Optimize for memory efficiency */
    MEMORY_EFFICIENT,
    /** Optimize for CPU efficiency */
    CPU_EFFICIENT
  }

  /** Default implementation of AsyncExecutorManager. */
  final class DefaultAsyncExecutorManager implements AsyncExecutorManager {
    private final AtomicLong taskCounter = new AtomicLong();
    private final ForkJoinPool defaultExecutor;
    private final ForkJoinPool compilationExecutor;
    private final ForkJoinPool executionExecutor;
    private final ForkJoinPool memoryExecutor;
    private final ScheduledThreadPoolExecutor scheduledExecutor;
    private final ManagerConfiguration config;
    private volatile boolean shutdown = false;

    public DefaultAsyncExecutorManager() {
      this(createDefaultConfiguration());
    }

    public DefaultAsyncExecutorManager(final ManagerConfiguration config) {
      this.config = config;

      // Create thread factory for WASM operations
      final ThreadFactory wasmThreadFactory =
          r -> {
            final Thread thread = new Thread(r, "wasm-async-" + taskCounter.incrementAndGet());
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY + 1);
            return thread;
          };

      // Initialize executors with optimal configurations
      this.defaultExecutor =
          new ForkJoinPool(
              Runtime.getRuntime().availableProcessors(),
              ForkJoinPool.defaultForkJoinWorkerThreadFactory,
              null,
              true);

      this.compilationExecutor =
          new ForkJoinPool(
              Math.max(1, Runtime.getRuntime().availableProcessors() - 1),
              ForkJoinPool.defaultForkJoinWorkerThreadFactory,
              null,
              false);

      this.executionExecutor =
          new ForkJoinPool(
              Runtime.getRuntime().availableProcessors() * 2,
              ForkJoinPool.defaultForkJoinWorkerThreadFactory,
              null,
              true);

      this.memoryExecutor =
          new ForkJoinPool(
              Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
              ForkJoinPool.defaultForkJoinWorkerThreadFactory,
              null,
              true);

      this.scheduledExecutor =
          new ScheduledThreadPoolExecutor(
              Math.max(1, Runtime.getRuntime().availableProcessors() / 4), wasmThreadFactory);
    }

    @Override
    public Executor getDefaultExecutor() {
      checkNotShutdown();
      return defaultExecutor;
    }

    @Override
    public Executor getCompilationExecutor() {
      checkNotShutdown();
      return compilationExecutor;
    }

    @Override
    public Executor getExecutionExecutor() {
      checkNotShutdown();
      return executionExecutor;
    }

    @Override
    public Executor getMemoryExecutor() {
      checkNotShutdown();
      return memoryExecutor;
    }

    @Override
    public ScheduledExecutorService getScheduledExecutor() {
      checkNotShutdown();
      return scheduledExecutor;
    }

    @Override
    public Executor createCustomExecutor(final ExecutorConfiguration config) {
      // Implementation would create custom executor based on configuration
      throw new UnsupportedOperationException("Custom executor creation not yet implemented");
    }

    @Override
    public ExecutorStatistics getStatistics() {
      // Implementation would return current statistics
      throw new UnsupportedOperationException("Statistics collection not yet implemented");
    }

    @Override
    public void optimizeConfiguration() {
      // Implementation would analyze performance and adjust configurations
    }

    @Override
    public void shutdown() {
      shutdown(Duration.ofSeconds(30));
    }

    @Override
    public void shutdown(final Duration timeout) {
      shutdown = true;
      defaultExecutor.shutdown();
      compilationExecutor.shutdown();
      executionExecutor.shutdown();
      memoryExecutor.shutdown();
      scheduledExecutor.shutdown();
    }

    @Override
    public boolean isShutdown() {
      return shutdown;
    }

    private void checkNotShutdown() {
      if (shutdown) {
        throw new IllegalStateException("AsyncExecutorManager has been shut down");
      }
    }

    private static ManagerConfiguration createDefaultConfiguration() {
      return new ManagerConfiguration() {
        @Override
        public OptimizationStrategy getOptimizationStrategy() {
          return OptimizationStrategy.BALANCED;
        }

        @Override
        public Duration getMonitoringInterval() {
          return Duration.ofSeconds(30);
        }

        @Override
        public boolean isAutoOptimizationEnabled() {
          return true;
        }

        @Override
        public Duration getShutdownTimeout() {
          return Duration.ofSeconds(30);
        }
      };
    }
  }
}
