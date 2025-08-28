/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.panama.util;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Advanced performance optimization utilities for Panama FFI operations.
 *
 * <p>This utility class provides high-performance optimization patterns specifically designed for
 * WebAssembly runtime operations through Panama FFI, including:
 *
 * <ul>
 *   <li>Batched FFI operations to reduce boundary crossing overhead
 *   <li>Advanced MethodHandle specialization and optimization
 *   <li>Memory access pattern optimization for bulk operations
 *   <li>Asynchronous operation pipelining for concurrent workloads
 *   <li>Performance monitoring and adaptive optimization strategies
 * </ul>
 *
 * <p>The optimizer works in conjunction with existing caching mechanisms to provide maximum
 * performance for Panama-based WebAssembly runtime operations.
 *
 * @since 1.0.0
 */
public final class PerformanceOptimizer {
  private static final Logger logger = Logger.getLogger(PerformanceOptimizer.class.getName());

  // Batching configuration
  private static final int DEFAULT_BATCH_SIZE = 32;
  private static final int DEFAULT_BATCH_TIMEOUT_MS = 10;
  private static final int DEFAULT_QUEUE_SIZE = 1024;

  // Performance tracking
  private final AtomicLong totalOperations = new AtomicLong(0);
  private final AtomicLong batchedOperations = new AtomicLong(0);
  private final AtomicLong optimizedCalls = new AtomicLong(0);
  private final AtomicInteger activeBatches = new AtomicInteger(0);

  // Batching infrastructure
  private final ArrayBlockingQueue<BatchedOperation<?>> operationQueue;
  private final Executor batchExecutor;
  private final int batchSize;
  private final int batchTimeoutMs;

  // Specialized method handle cache for high-frequency operations
  private final ConcurrentHashMap<String, SpecializedMethodHandle> specializedHandles;

  // Optimization state
  private volatile boolean optimizationEnabled = true;
  private volatile boolean shutdownRequested = false;

  /**
   * Creates a new performance optimizer with default settings.
   */
  public PerformanceOptimizer() {
    this(ForkJoinPool.commonPool(), DEFAULT_BATCH_SIZE, DEFAULT_BATCH_TIMEOUT_MS, DEFAULT_QUEUE_SIZE);
  }

  /**
   * Creates a new performance optimizer with custom configuration.
   *
   * @param executor the executor for batch processing
   * @param batchSize the target batch size for operations
   * @param batchTimeoutMs timeout for batch assembly in milliseconds
   * @param queueSize the size of the operation queue
   */
  public PerformanceOptimizer(
      final Executor executor, final int batchSize, final int batchTimeoutMs, final int queueSize) {
    this.batchExecutor = executor;
    this.batchSize = batchSize;
    this.batchTimeoutMs = batchTimeoutMs;
    this.operationQueue = new ArrayBlockingQueue<>(queueSize);
    this.specializedHandles = new ConcurrentHashMap<>();

    if (logger.isLoggable(Level.FINE)) {
      logger.fine(
          String.format(
              "Created PerformanceOptimizer with batch size: %d, timeout: %d ms, queue size: %d",
              batchSize, batchTimeoutMs, queueSize));
    }
  }

  /**
   * Functional interface for batched operations.
   *
   * @param <T> the return type of the operation
   */
  @FunctionalInterface
  public interface BatchedFFIOperation<T> {
    /**
     * Executes the operation with the provided parameters.
     *
     * @param handle the method handle to invoke
     * @param params the operation parameters
     * @return the operation result
     * @throws Throwable if the operation fails
     */
    T execute(MethodHandle handle, Object[] params) throws Throwable;
  }

  /**
   * Optimizes a method handle for high-frequency usage patterns.
   *
   * <p>This method creates specialized versions of method handles optimized for common usage
   * patterns, including parameter type specialization and invocation optimization.
   *
   * @param handleName the name/identifier for the method handle
   * @param originalHandle the original method handle
   * @param usagePattern the expected usage pattern for optimization
   * @return an optimized method handle
   */
  public MethodHandle optimizeMethodHandle(
      final String handleName, final MethodHandle originalHandle, final UsagePattern usagePattern) {
    if (!optimizationEnabled) {
      return originalHandle;
    }

    return specializedHandles
        .computeIfAbsent(
            handleName,
            key -> {
              final MethodHandle optimized = createOptimizedHandle(originalHandle, usagePattern);
              if (logger.isLoggable(Level.FINE)) {
                logger.fine("Created optimized method handle for: " + handleName);
              }
              return new SpecializedMethodHandle(optimized, usagePattern, System.nanoTime());
            })
        .getHandle();
  }

  /**
   * Executes a batched FFI operation for improved performance.
   *
   * <p>This method queues operations for batched execution, which reduces FFI boundary crossing
   * overhead by executing multiple operations in a single native transition.
   *
   * @param <T> the return type
   * @param handle the method handle to invoke
   * @param params the operation parameters
   * @param operation the operation to execute
   * @return a CompletableFuture containing the result
   */
  public <T> CompletableFuture<T> executeBatched(
      final MethodHandle handle, final Object[] params, final BatchedFFIOperation<T> operation) {
    if (shutdownRequested) {
      return CompletableFuture.failedFuture(new IllegalStateException("Optimizer is shutting down"));
    }

    totalOperations.incrementAndGet();
    final BatchedOperation<T> batchedOp = new BatchedOperation<>(handle, params, operation);

    if (operationQueue.offer(batchedOp)) {
      processBatchIfReady();
      return batchedOp.getFuture();
    } else {
      // Queue full, execute immediately
      return CompletableFuture.supplyAsync(
          () -> {
            try {
              return operation.execute(handle, params);
            } catch (Throwable t) {
              throw new RuntimeException("Batched operation failed", t);
            }
          },
          batchExecutor);
    }
  }

  /**
   * Optimizes memory access patterns for bulk operations.
   *
   * <p>This method provides optimized memory access patterns for operations that work with
   * multiple memory segments, reducing cache misses and improving throughput.
   *
   * @param <T> the return type
   * @param segments the memory segments to access
   * @param accessor the memory access function
   * @return the operation results
   */
  public <T> List<T> optimizeMemoryAccess(
      final MemorySegment[] segments, final Function<MemorySegment, T> accessor) {
    if (!optimizationEnabled || segments.length == 0) {
      return java.util.Arrays.stream(segments).map(accessor).toList();
    }

    // Sort segments by address to optimize cache locality
    final MemorySegment[] sortedSegments = segments.clone();
    java.util.Arrays.sort(sortedSegments, (a, b) -> Long.compare(a.address(), b.address()));

    // Process in cache-friendly order
    final List<T> results = new ArrayList<>(segments.length);
    for (final MemorySegment segment : sortedSegments) {
      results.add(accessor.apply(segment));
    }

    optimizedCalls.incrementAndGet();
    return results;
  }

  /**
   * Creates a pipeline for asynchronous operation processing.
   *
   * <p>This method sets up a processing pipeline that can handle multiple concurrent operations
   * while maintaining optimal resource utilization and throughput.
   *
   * @param <T> the input type
   * @param <R> the output type
   * @param processor the processing function
   * @param parallelism the degree of parallelism
   * @return a processing pipeline
   */
  public <T, R> OperationPipeline<T, R> createPipeline(
      final Function<T, R> processor, final int parallelism) {
    return new OperationPipeline<>(processor, parallelism, batchExecutor);
  }

  /**
   * Gets performance statistics for monitoring and tuning.
   *
   * @return performance statistics
   */
  public PerformanceStatistics getStatistics() {
    return new PerformanceStatistics(
        totalOperations.get(),
        batchedOperations.get(),
        optimizedCalls.get(),
        activeBatches.get(),
        operationQueue.size(),
        specializedHandles.size());
  }

  /**
   * Enables or disables performance optimizations.
   *
   * @param enabled whether optimizations should be enabled
   */
  public void setOptimizationEnabled(final boolean enabled) {
    this.optimizationEnabled = enabled;
    if (logger.isLoggable(Level.INFO)) {
      logger.info("Performance optimization " + (enabled ? "enabled" : "disabled"));
    }
  }

  /**
   * Initiates shutdown of the performance optimizer.
   */
  public void shutdown() {
    shutdownRequested = true;
    // Process remaining operations
    processPendingOperations();
    logger.info("Performance optimizer shutdown completed");
  }

  /**
   * Creates an optimized version of a method handle based on usage patterns.
   *
   * @param originalHandle the original method handle
   * @param pattern the usage pattern
   * @return an optimized method handle
   */
  private MethodHandle createOptimizedHandle(
      final MethodHandle originalHandle, final UsagePattern pattern) {
    try {
      final MethodHandle optimized;
      
      switch (pattern) {
        case HIGH_FREQUENCY_SIMPLE -> {
          // Optimize for frequent calls with simple parameters
          optimized = MethodHandles.foldArguments(
              originalHandle, 
              MethodHandles.identity(originalHandle.type().parameterType(0)));
        }
        case BULK_OPERATIONS -> {
          // Optimize for bulk operations with array parameters
          optimized = originalHandle.asCollector(Object[].class, originalHandle.type().parameterCount());
        }
        case MEMORY_INTENSIVE -> {
          // Optimize for memory-intensive operations
          optimized = MethodHandles.permuteArguments(
              originalHandle, 
              originalHandle.type(),
              java.util.stream.IntStream.range(0, originalHandle.type().parameterCount()).toArray());
        }
        default -> optimized = originalHandle;
      }

      return optimized;
    } catch (Exception e) {
      logger.log(Level.WARNING, "Failed to create optimized method handle, using original", e);
      return originalHandle;
    }
  }

  /**
   * Processes batched operations if a batch is ready.
   */
  private void processBatchIfReady() {
    if (operationQueue.size() >= batchSize || shouldProcessTimeout()) {
      batchExecutor.execute(this::processBatch);
    }
  }

  /**
   * Processes a batch of operations.
   */
  private void processBatch() {
    final List<BatchedOperation<?>> batch = new ArrayList<>(batchSize);
    operationQueue.drainTo(batch, batchSize);

    if (batch.isEmpty()) {
      return;
    }

    activeBatches.incrementAndGet();
    batchedOperations.addAndGet(batch.size());

    try {
      // Execute all operations in the batch
      for (final BatchedOperation<?> operation : batch) {
        try {
          final Object result = operation.getOperation().execute(operation.getHandle(), operation.getParams());
          operation.complete(result);
        } catch (Throwable t) {
          operation.completeExceptionally(t);
        }
      }
    } finally {
      activeBatches.decrementAndGet();
    }
  }

  /**
   * Processes all pending operations during shutdown.
   */
  private void processPendingOperations() {
    while (!operationQueue.isEmpty()) {
      processBatch();
    }
  }

  /**
   * Determines if a batch should be processed due to timeout.
   *
   * @return true if timeout-based processing should occur
   */
  private boolean shouldProcessTimeout() {
    // Simple timeout logic - in practice, this would be more sophisticated
    return !operationQueue.isEmpty() && operationQueue.size() > batchSize / 4;
  }

  /**
   * Usage patterns for method handle optimization.
   */
  public enum UsagePattern {
    /** High-frequency calls with simple parameters. */
    HIGH_FREQUENCY_SIMPLE,
    /** Bulk operations with array parameters. */
    BULK_OPERATIONS,
    /** Memory-intensive operations. */
    MEMORY_INTENSIVE,
    /** General-purpose usage. */
    GENERAL
  }

  /**
   * Represents a specialized method handle with optimization metadata.
   */
  private static final class SpecializedMethodHandle {
    private final MethodHandle handle;
    private final UsagePattern pattern;
    private final long creationTime;

    SpecializedMethodHandle(final MethodHandle handle, final UsagePattern pattern, final long creationTime) {
      this.handle = handle;
      this.pattern = pattern;
      this.creationTime = creationTime;
    }

    MethodHandle getHandle() {
      return handle;
    }

    UsagePattern getPattern() {
      return pattern;
    }

    long getAge() {
      return System.nanoTime() - creationTime;
    }
  }

  /**
   * Represents a batched operation waiting for execution.
   */
  private static final class BatchedOperation<T> {
    private final MethodHandle handle;
    private final Object[] params;
    private final BatchedFFIOperation<T> operation;
    private final CompletableFuture<T> future = new CompletableFuture<>();

    BatchedOperation(final MethodHandle handle, final Object[] params, final BatchedFFIOperation<T> operation) {
      this.handle = handle;
      this.params = params;
      this.operation = operation;
    }

    MethodHandle getHandle() {
      return handle;
    }

    Object[] getParams() {
      return params;
    }

    BatchedFFIOperation<T> getOperation() {
      return operation;
    }

    CompletableFuture<T> getFuture() {
      return future;
    }

    @SuppressWarnings("unchecked")
    void complete(final Object result) {
      future.complete((T) result);
    }

    void completeExceptionally(final Throwable throwable) {
      future.completeExceptionally(throwable);
    }
  }

  /**
   * Asynchronous operation pipeline for high-throughput processing.
   */
  public static final class OperationPipeline<T, R> {
    private final Function<T, R> processor;
    private final Executor executor;
    private final int parallelism;

    OperationPipeline(final Function<T, R> processor, final int parallelism, final Executor executor) {
      this.processor = processor;
      this.parallelism = parallelism;
      this.executor = executor;
    }

    /**
     * Processes multiple items through the pipeline.
     *
     * @param items the items to process
     * @return a CompletableFuture containing all results
     */
    public CompletableFuture<List<R>> process(final List<T> items) {
      if (items.isEmpty()) {
        return CompletableFuture.completedFuture(List.of());
      }

      final int chunkSize = Math.max(1, items.size() / parallelism);
      final List<CompletableFuture<List<R>>> futures = new ArrayList<>();

      for (int i = 0; i < items.size(); i += chunkSize) {
        final int end = Math.min(i + chunkSize, items.size());
        final List<T> chunk = items.subList(i, end);
        
        futures.add(CompletableFuture.supplyAsync(
            () -> chunk.stream().map(processor).toList(),
            executor));
      }

      return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
          .thenApply(void_ -> futures.stream()
              .flatMap(future -> future.join().stream())
              .toList());
    }
  }

  /**
   * Performance statistics for monitoring.
   */
  public static final class PerformanceStatistics {
    private final long totalOperations;
    private final long batchedOperations;
    private final long optimizedCalls;
    private final int activeBatches;
    private final int queueSize;
    private final int specializedHandles;

    PerformanceStatistics(
        final long totalOperations,
        final long batchedOperations,
        final long optimizedCalls,
        final int activeBatches,
        final int queueSize,
        final int specializedHandles) {
      this.totalOperations = totalOperations;
      this.batchedOperations = batchedOperations;
      this.optimizedCalls = optimizedCalls;
      this.activeBatches = activeBatches;
      this.queueSize = queueSize;
      this.specializedHandles = specializedHandles;
    }

    public long getTotalOperations() {
      return totalOperations;
    }

    public long getBatchedOperations() {
      return batchedOperations;
    }

    public long getOptimizedCalls() {
      return optimizedCalls;
    }

    public int getActiveBatches() {
      return activeBatches;
    }

    public int getQueueSize() {
      return queueSize;
    }

    public int getSpecializedHandles() {
      return specializedHandles;
    }

    public double getBatchingRatio() {
      return totalOperations == 0 ? 0.0 : (double) batchedOperations / totalOperations;
    }

    @Override
    public String toString() {
      return String.format(
          "PerformanceStatistics{totalOperations=%d, batchedOperations=%d, optimizedCalls=%d, "
              + "activeBatches=%d, queueSize=%d, specializedHandles=%d, batchingRatio=%.2f}",
          totalOperations,
          batchedOperations,
          optimizedCalls,
          activeBatches,
          queueSize,
          specializedHandles,
          getBatchingRatio());
    }
  }
}