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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Advanced thread-safe concurrent access coordinator for Panama FFI operations.
 *
 * <p>This utility provides high-performance coordination patterns for concurrent access to Panama
 * FFI resources, including arena coordination, bulk operations, and optimistic concurrency control.
 * It builds upon the existing thread-safety patterns in the codebase to provide additional
 * coordination mechanisms specifically optimized for WebAssembly runtime operations.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Arena-coordinated bulk operations for reduced FFI boundary crossings
 *   <li>Optimistic concurrency control using StampedLock for high-throughput scenarios
 *   <li>Batched resource operations to minimize synchronization overhead
 *   <li>Concurrent operation tracking and metrics for performance optimization
 *   <li>Deadlock prevention through ordered resource acquisition
 * </ul>
 *
 * @since 1.0.0
 */
public final class ConcurrentAccessCoordinator {
  private static final Logger logger =
      Logger.getLogger(ConcurrentAccessCoordinator.class.getName());

  // Coordination locks for different resource types
  private final StampedLock arenaLock = new StampedLock();
  private final ReadWriteLock resourceLock = new ReentrantReadWriteLock();
  private final ConcurrentHashMap<String, StampedLock> resourceTypeLocks =
      new ConcurrentHashMap<>();

  // Bulk operation coordination
  private final ConcurrentLinkedQueue<BulkOperation> pendingOperations =
      new ConcurrentLinkedQueue<>();
  private final AtomicInteger activeBulkOperations = new AtomicInteger(0);
  private final AtomicLong totalOperations = new AtomicLong(0);

  // Async execution support
  private final Executor executor;

  // Configuration
  private final int maxConcurrentOperations;
  private final long operationTimeoutMs;
  private volatile boolean shutdownRequested = false;

  /** Creates a new concurrent access coordinator with default configuration. */
  public ConcurrentAccessCoordinator() {
    this(ForkJoinPool.commonPool(), 100, 30000L);
  }

  /**
   * Creates a new concurrent access coordinator with custom configuration.
   *
   * @param executor the executor for async operations
   * @param maxConcurrentOperations maximum concurrent operations allowed
   * @param operationTimeoutMs timeout for operations in milliseconds
   */
  public ConcurrentAccessCoordinator(
      final Executor executor, final int maxConcurrentOperations, final long operationTimeoutMs) {
    this.executor = executor;
    this.maxConcurrentOperations = maxConcurrentOperations;
    this.operationTimeoutMs = operationTimeoutMs;

    if (logger.isLoggable(Level.FINE)) {
      logger.fine(
          "Created ConcurrentAccessCoordinator with max operations: " + maxConcurrentOperations);
    }
  }

  /**
   * Functional interface for arena-coordinated operations.
   *
   * @param <T> the return type of the operation
   */
  @FunctionalInterface
  public interface ArenaOperation<T> {
    /**
     * Executes the operation with arena coordination.
     *
     * @param arena the coordinated arena
     * @return the operation result
     * @throws Exception if the operation fails
     */
    T execute(Arena arena) throws Exception;
  }

  /**
   * Functional interface for bulk operations on multiple resources.
   *
   * @param <T> the return type of the operation
   */
  @FunctionalInterface
  public interface BulkResourceOperation<T> {
    /**
     * Executes the bulk operation on multiple resources.
     *
     * @param resources the resources to operate on
     * @return the operation result
     * @throws Exception if the operation fails
     */
    T execute(MemorySegment[] resources) throws Exception;
  }

  /**
   * Executes an operation with optimistic arena coordination.
   *
   * <p>Uses StampedLock optimistic reading to minimize contention while ensuring consistency. Falls
   * back to pessimistic locking if optimistic validation fails.
   *
   * @param <T> the return type
   * @param arena the arena to coordinate access to
   * @param operation the operation to execute
   * @return the operation result
   * @throws RuntimeException if the operation fails or times out
   */
  public <T> T executeWithArenaCoordination(final Arena arena, final ArenaOperation<T> operation) {
    if (shutdownRequested) {
      throw new IllegalStateException("Coordinator is shutting down");
    }

    final long startTime = System.nanoTime();
    totalOperations.incrementAndGet();

    // Try optimistic read first
    long stamp = arenaLock.tryOptimisticRead();
    if (stamp != 0L) {
      try {
        final T result = operation.execute(arena);
        if (arenaLock.validate(stamp)) {
          // Optimistic read succeeded
          logOperationSuccess("optimistic", startTime);
          return result;
        }
      } catch (Exception e) {
        // Fall through to pessimistic approach
        if (logger.isLoggable(Level.FINE)) {
          logger.fine(
              "Optimistic arena operation failed, falling back to pessimistic: " + e.getMessage());
        }
      }
    }

    // Fall back to pessimistic read lock
    stamp = arenaLock.readLock();
    try {
      final T result = operation.execute(arena);
      logOperationSuccess("pessimistic", startTime);
      return result;
    } catch (Exception e) {
      throw new RuntimeException("Arena coordinated operation failed", e);
    } finally {
      arenaLock.unlockRead(stamp);
    }
  }

  /**
   * Executes a bulk operation on multiple resources with batching optimization.
   *
   * <p>This method coordinates access to multiple resources and executes the operation in a single
   * batch to minimize FFI boundary crossings and synchronization overhead.
   *
   * @param <T> the return type
   * @param resources the resources to operate on
   * @param operation the bulk operation to execute
   * @return the operation result
   * @throws RuntimeException if the operation fails or times out
   */
  public <T> T executeBulkOperation(
      final MemorySegment[] resources, final BulkResourceOperation<T> operation) {
    if (shutdownRequested) {
      throw new IllegalStateException("Coordinator is shutting down");
    }

    if (resources.length == 0) {
      throw new IllegalArgumentException("No resources provided for bulk operation");
    }

    final long startTime = System.nanoTime();
    final int currentOperations = activeBulkOperations.incrementAndGet();

    try {
      if (currentOperations > maxConcurrentOperations) {
        throw new RuntimeException("Too many concurrent bulk operations: " + currentOperations);
      }

      // Execute the bulk operation with resource coordination
      final T result = coordinateBulkResourceAccess(resources, operation);

      logOperationSuccess("bulk", startTime);
      return result;
    } finally {
      activeBulkOperations.decrementAndGet();
    }
  }

  /**
   * Executes an operation asynchronously with concurrency coordination.
   *
   * @param <T> the return type
   * @param operation the operation to execute
   * @return a CompletableFuture representing the operation
   */
  public <T> CompletableFuture<T> executeAsync(final Supplier<T> operation) {
    if (shutdownRequested) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Coordinator is shutting down"));
    }

    return CompletableFuture.supplyAsync(operation, executor)
        .orTimeout(operationTimeoutMs, TimeUnit.MILLISECONDS)
        .whenComplete(
            (result, throwable) -> {
              if (throwable != null) {
                logger.log(Level.WARNING, "Async operation failed", throwable);
              }
            });
  }

  /**
   * Batches multiple operations for coordinated execution.
   *
   * <p>This method collects operations and executes them in coordinated batches to reduce
   * synchronization overhead and improve throughput for high-volume scenarios.
   *
   * @param <T> the return type
   * @param operations the operations to batch
   * @return a CompletableFuture containing all results
   */
  @SafeVarargs
  public final <T> CompletableFuture<T[]> executeBatch(final Supplier<T>... operations) {
    if (shutdownRequested) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Coordinator is shutting down"));
    }

    if (operations.length == 0) {
      return CompletableFuture.completedFuture((T[]) new Object[0]);
    }

    final CompletableFuture<T>[] futures = new CompletableFuture[operations.length];
    for (int i = 0; i < operations.length; i++) {
      futures[i] = executeAsync(operations[i]);
    }

    return CompletableFuture.allOf(futures)
        .thenApply(
            ignored -> {
              @SuppressWarnings("unchecked")
              final T[] results = (T[]) new Object[futures.length];
              for (int i = 0; i < futures.length; i++) {
                results[i] = futures[i].join();
              }
              return results;
            });
  }

  /**
   * Gets a resource-type-specific lock for fine-grained coordination.
   *
   * <p>This allows coordination of access to specific resource types (e.g., "memory", "table")
   * without blocking access to other resource types.
   *
   * @param resourceType the type of resource
   * @return a StampedLock for the resource type
   */
  public StampedLock getResourceTypeLock(final String resourceType) {
    return resourceTypeLocks.computeIfAbsent(resourceType, k -> new StampedLock());
  }

  /**
   * Gets coordination statistics for monitoring and optimization.
   *
   * @return coordination statistics
   */
  public CoordinationStatistics getStatistics() {
    return new CoordinationStatistics(
        totalOperations.get(),
        activeBulkOperations.get(),
        pendingOperations.size(),
        resourceTypeLocks.size());
  }

  /**
   * Initiates shutdown of the coordinator.
   *
   * <p>This method prevents new operations from starting and allows existing operations to
   * complete. It does not block waiting for completion.
   */
  public void shutdown() {
    shutdownRequested = true;
    logger.info("Concurrent access coordinator shutdown requested");
  }

  /**
   * Checks if the coordinator is shutting down.
   *
   * @return true if shutdown has been requested
   */
  public boolean isShutdownRequested() {
    return shutdownRequested;
  }

  /**
   * Coordinates access to multiple resources for bulk operations.
   *
   * @param <T> the return type
   * @param resources the resources to coordinate
   * @param operation the operation to execute
   * @return the operation result
   * @throws Exception if coordination or execution fails
   */
  private <T> T coordinateBulkResourceAccess(
      final MemorySegment[] resources, final BulkResourceOperation<T> operation) throws Exception {
    // Sort resources by address to prevent deadlocks
    final MemorySegment[] sortedResources = resources.clone();
    java.util.Arrays.sort(sortedResources, (a, b) -> Long.compare(a.address(), b.address()));

    // Execute with coordinated resource access
    final long stamp;
    try {
      stamp = resourceLock.readLock().tryLock(operationTimeoutMs, TimeUnit.MILLISECONDS) ? 1L : 0L;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Thread interrupted while waiting for resource lock", e);
    }
    if (stamp == 0L) {
      throw new RuntimeException("Failed to acquire resource lock within timeout");
    }

    try {
      return operation.execute(sortedResources);
    } finally {
      resourceLock.readLock().unlock();
    }
  }

  /**
   * Logs successful operation completion with timing information.
   *
   * @param operationType the type of operation
   * @param startTime the operation start time in nanoseconds
   */
  private void logOperationSuccess(final String operationType, final long startTime) {
    if (logger.isLoggable(Level.FINE)) {
      final long duration = System.nanoTime() - startTime;
      final double durationMs = duration / 1_000_000.0;
      logger.fine(String.format("Completed %s operation in %.2f ms", operationType, durationMs));
    }
  }

  /** Represents a bulk operation to be coordinated. */
  private static final class BulkOperation {
    private final Runnable operation;
    private final long submissionTime;

    BulkOperation(final Runnable operation) {
      this.operation = operation;
      this.submissionTime = System.nanoTime();
    }

    void execute() {
      operation.run();
    }

    long getAge() {
      return System.nanoTime() - submissionTime;
    }
  }

  /** Statistics about coordination operations. */
  public static final class CoordinationStatistics {
    private final long totalOperations;
    private final int activeBulkOperations;
    private final int pendingOperations;
    private final int resourceTypeLocks;

    CoordinationStatistics(
        final long totalOperations,
        final int activeBulkOperations,
        final int pendingOperations,
        final int resourceTypeLocks) {
      this.totalOperations = totalOperations;
      this.activeBulkOperations = activeBulkOperations;
      this.pendingOperations = pendingOperations;
      this.resourceTypeLocks = resourceTypeLocks;
    }

    public long getTotalOperations() {
      return totalOperations;
    }

    public int getActiveBulkOperations() {
      return activeBulkOperations;
    }

    public int getPendingOperations() {
      return pendingOperations;
    }

    public int getResourceTypeLocks() {
      return resourceTypeLocks;
    }

    @Override
    public String toString() {
      return String.format(
          "CoordinationStatistics{totalOperations=%d, activeBulkOperations=%d, "
              + "pendingOperations=%d, resourceTypeLocks=%d}",
          totalOperations, activeBulkOperations, pendingOperations, resourceTypeLocks);
    }
  }
}
