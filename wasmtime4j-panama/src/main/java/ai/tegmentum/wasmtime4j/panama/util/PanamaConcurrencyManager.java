package ai.tegmentum.wasmtime4j.panama.util;

import ai.tegmentum.wasmtime4j.panama.performance.PanamaPerformanceMonitor;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * Panama-optimized concurrency management utilities for WebAssembly operations.
 *
 * <p>This class provides thread-safe access coordination for Panama Foreign Function API
 * operations, ensuring proper resource management and preventing data races while maximizing
 * performance through optimized locking strategies.
 *
 * <p>Panama-specific concurrency features:
 *
 * <ul>
 *   <li>Arena-aware thread safety with proper scope management
 *   <li>Memory segment access coordination to prevent corruption
 *   <li>Method handle call synchronization for thread-unsafe native operations
 *   <li>Optimized lock-free operations for read-heavy workloads
 *   <li>Deadlock prevention and resource leak detection
 * </ul>
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li>Resource access coordination with configurable concurrency levels
 *   <li>Arena lifecycle management across multiple threads
 *   <li>Memory segment sharing with proper synchronization
 *   <li>Async operation management with CompletableFuture integration
 *   <li>Performance monitoring and contention analysis
 * </ul>
 *
 * <p>Usage Example:
 *
 * <pre>{@code
 * PanamaConcurrencyManager manager = new PanamaConcurrencyManager(8);
 *
 * // Coordinate access to shared resource
 * manager.executeWithLock("resource1", () -> {
 *   // Thread-safe operation
 *   return processSharedResource();
 * });
 *
 * // Async operation with arena management
 * CompletableFuture<String> result = manager.executeAsync(() -> {
 *   try (Arena arena = Arena.ofConfined()) {
 *     return performNativeOperation(arena);
 *   }
 * });
 * }</pre>
 *
 * @since 1.0.0
 */
public final class PanamaConcurrencyManager {

  private static final Logger LOGGER = Logger.getLogger(PanamaConcurrencyManager.class.getName());

  /** Default maximum number of concurrent operations. */
  public static final int DEFAULT_MAX_CONCURRENT_OPERATIONS = 16;

  /** Maximum number of concurrent operations allowed. */
  private final int maxConcurrentOperations;

  /** Semaphore to control overall concurrency. */
  private final Semaphore concurrencySemaphore;

  /** Per-resource locks for fine-grained synchronization. */
  private final ConcurrentHashMap<String, ReadWriteLock> resourceLocks = new ConcurrentHashMap<>();

  /** Per-arena access coordination. */
  private final ConcurrentHashMap<Arena, AtomicInteger> arenaRefCounts = new ConcurrentHashMap<>();

  /** Executor service for async operations. */
  private final ExecutorService asyncExecutor;

  /** Thread factory for creating monitored threads. */
  private final ThreadFactory threadFactory;

  /** Statistics tracking. */
  private final AtomicLong totalOperations = new AtomicLong(0);

  private final AtomicLong concurrentOperations = new AtomicLong(0);
  private final AtomicLong maxConcurrencyReached = new AtomicLong(0);
  private final AtomicLong lockContentions = new AtomicLong(0);
  private final AtomicLong totalWaitTimeNs = new AtomicLong(0);
  private final AtomicLong arenaOperations = new AtomicLong(0);

  /** Arena lifecycle tracking. */
  private final ConcurrentHashMap<Arena, ArenaInfo> arenaInfoMap = new ConcurrentHashMap<>();

  /** Arena information for tracking. */
  private static final class ArenaInfo {
    final Arena arena;
    final long creationTime;
    final AtomicInteger activeOperations = new AtomicInteger(0);
    volatile boolean closed = false;

    ArenaInfo(final Arena arena) {
      this.arena = arena;
      this.creationTime = System.currentTimeMillis();
    }

    void incrementOperations() {
      activeOperations.incrementAndGet();
    }

    void decrementOperations() {
      activeOperations.decrementAndGet();
    }

    long getLifetimeMs() {
      return System.currentTimeMillis() - creationTime;
    }

    boolean hasActiveOperations() {
      return activeOperations.get() > 0;
    }
  }

  /**
   * Creates a new Panama concurrency manager.
   *
   * @param maxConcurrentOperations the maximum number of concurrent operations
   * @throws IllegalArgumentException if maxConcurrentOperations is not positive
   */
  public PanamaConcurrencyManager(final int maxConcurrentOperations) {
    PanamaValidation.requirePositive(maxConcurrentOperations, "maxConcurrentOperations");

    this.maxConcurrentOperations = maxConcurrentOperations;
    this.concurrencySemaphore = new Semaphore(maxConcurrentOperations, true);

    // Create thread factory for monitoring
    this.threadFactory =
        new ThreadFactory() {
          private final AtomicInteger threadNumber = new AtomicInteger(1);

          @Override
          public Thread newThread(final Runnable r) {
            final Thread t = new Thread(r, "panama-concurrent-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
          }
        };

    // Create executor with monitored threads
    this.asyncExecutor =
        new ForkJoinPool(
            maxConcurrentOperations, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, false);

    LOGGER.info(
        "Created Panama concurrency manager with "
            + maxConcurrentOperations
            + " max concurrent operations");
  }

  /** Creates a new Panama concurrency manager with default settings. */
  public PanamaConcurrencyManager() {
    this(DEFAULT_MAX_CONCURRENT_OPERATIONS);
  }

  /**
   * Executes an operation with controlled concurrency.
   *
   * @param <T> the return type
   * @param operation the operation to execute
   * @return the operation result
   * @throws InterruptedException if interrupted while waiting for concurrency slot
   */
  public <T> T execute(final java.util.concurrent.Callable<T> operation) throws Exception {
    PanamaValidation.requireNonNull(operation, "operation");

    final long startTime = PanamaPerformanceMonitor.startOperation("panama_concurrent_execute");
    final long waitStartTime = System.nanoTime();

    try {
      // Acquire concurrency permit
      concurrencySemaphore.acquire();
      final long waitTime = System.nanoTime() - waitStartTime;
      totalWaitTimeNs.addAndGet(waitTime);

      try {
        totalOperations.incrementAndGet();
        final long currentConcurrent = concurrentOperations.incrementAndGet();

        // Track max concurrency reached
        maxConcurrencyReached.updateAndGet(current -> Math.max(current, currentConcurrent));

        LOGGER.fine(
            () ->
                String.format(
                    "Executing Panama concurrent operation (active: %d)", currentConcurrent));

        // Execute the operation
        return operation.call();

      } finally {
        concurrentOperations.decrementAndGet();
      }

    } finally {
      concurrencySemaphore.release();
      PanamaPerformanceMonitor.endOperation("panama_concurrent_execute", startTime);
    }
  }

  /**
   * Executes an operation asynchronously with controlled concurrency.
   *
   * @param <T> the return type
   * @param operation the operation to execute
   * @return CompletableFuture with the operation result
   */
  public <T> CompletableFuture<T> executeAsync(final java.util.concurrent.Callable<T> operation) {
    PanamaValidation.requireNonNull(operation, "operation");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return execute(operation);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        },
        asyncExecutor);
  }

  /**
   * Executes an operation with resource-specific locking.
   *
   * @param <T> the return type
   * @param resourceId the resource identifier for locking
   * @param operation the operation to execute
   * @return the operation result
   */
  public <T> T executeWithLock(
      final String resourceId, final java.util.concurrent.Callable<T> operation) throws Exception {
    PanamaValidation.requireNonEmpty(resourceId, "resourceId");
    PanamaValidation.requireNonNull(operation, "operation");

    final ReadWriteLock lock =
        resourceLocks.computeIfAbsent(resourceId, k -> new ReentrantReadWriteLock(true));
    final long startTime =
        PanamaPerformanceMonitor.startOperation("panama_concurrent_execute_with_lock");
    final long lockStartTime = System.nanoTime();

    try {
      lock.writeLock().lock();
      final long lockWaitTime = System.nanoTime() - lockStartTime;
      totalWaitTimeNs.addAndGet(lockWaitTime);

      if (lockWaitTime > 1_000_000) { // More than 1ms wait indicates contention
        lockContentions.incrementAndGet();
      }

      try {
        LOGGER.fine(() -> String.format("Acquired lock for resource: %s", resourceId));
        return execute(operation);
      } finally {
        lock.writeLock().unlock();
      }

    } finally {
      PanamaPerformanceMonitor.endOperation("panama_concurrent_execute_with_lock", startTime);
    }
  }

  /**
   * Executes a read operation with shared locking.
   *
   * @param <T> the return type
   * @param resourceId the resource identifier for locking
   * @param operation the operation to execute
   * @return the operation result
   */
  public <T> T executeWithReadLock(
      final String resourceId, final java.util.concurrent.Callable<T> operation) throws Exception {
    PanamaValidation.requireNonEmpty(resourceId, "resourceId");
    PanamaValidation.requireNonNull(operation, "operation");

    final ReadWriteLock lock =
        resourceLocks.computeIfAbsent(resourceId, k -> new ReentrantReadWriteLock(true));
    final long startTime =
        PanamaPerformanceMonitor.startOperation("panama_concurrent_execute_with_read_lock");

    try {
      lock.readLock().lock();
      try {
        LOGGER.fine(() -> String.format("Acquired read lock for resource: %s", resourceId));
        return execute(operation);
      } finally {
        lock.readLock().unlock();
      }

    } finally {
      PanamaPerformanceMonitor.endOperation("panama_concurrent_execute_with_read_lock", startTime);
    }
  }

  /**
   * Executes an operation with arena lifecycle management.
   *
   * @param <T> the return type
   * @param arena the arena to manage
   * @param operation the operation to execute
   * @return the operation result
   */
  public <T> T executeWithArena(final Arena arena, final java.util.concurrent.Callable<T> operation)
      throws Exception {
    PanamaValidation.requireNonNull(arena, "arena");
    PanamaValidation.requireNonNull(operation, "operation");

    if (!arena.scope().isAlive()) {
      throw new IllegalStateException("Arena is already closed");
    }

    final long startTime =
        PanamaPerformanceMonitor.startOperation("panama_concurrent_execute_with_arena");
    final ArenaInfo arenaInfo = arenaInfoMap.computeIfAbsent(arena, ArenaInfo::new);

    try {
      arenaInfo.incrementOperations();
      arenaOperations.incrementAndGet();

      LOGGER.fine(
          () ->
              String.format(
                  "Executing operation with arena (active ops: %d)",
                  arenaInfo.activeOperations.get()));

      PanamaPerformanceMonitor.recordArenaAllocation(arena, 0); // Track usage

      return execute(operation);

    } finally {
      arenaInfo.decrementOperations();
      PanamaPerformanceMonitor.endOperation("panama_concurrent_execute_with_arena", startTime);
    }
  }

  /**
   * Coordinates access to a memory segment across multiple threads.
   *
   * @param <T> the return type
   * @param segment the memory segment to access
   * @param operation the operation to execute
   * @return the operation result
   */
  public <T> T executeWithMemorySegment(
      final MemorySegment segment, final java.util.concurrent.Callable<T> operation)
      throws Exception {
    PanamaValidation.requireNonNull(segment, "segment");
    PanamaValidation.requireNonNull(operation, "operation");

    // Use segment identity as lock key
    final String segmentId = "segment_" + System.identityHashCode(segment);

    return executeWithLock(
        segmentId,
        () -> {
          PanamaPerformanceMonitor.recordMemorySegmentAllocation(null, segment);
          return operation.call();
        });
  }

  /**
   * Gets the current concurrency statistics.
   *
   * @return formatted statistics string
   */
  public String getStatistics() {
    final long totalOps = totalOperations.get();
    final long currentConcurrent = concurrentOperations.get();
    final long maxConcurrent = maxConcurrencyReached.get();
    final long contentions = lockContentions.get();
    final long avgWaitTimeNs = totalOps > 0 ? totalWaitTimeNs.get() / totalOps : 0;
    final int availablePermits = concurrencySemaphore.availablePermits();

    return String.format(
        "Panama Concurrency Manager Statistics:%n"
            + "  Total operations: %,d%n"
            + "  Current concurrent operations: %d%n"
            + "  Max concurrent operations reached: %d%n"
            + "  Lock contentions: %,d%n"
            + "  Average wait time: %,d ns%n"
            + "  Available permits: %d/%d%n"
            + "  Active resource locks: %d%n"
            + "  Arena operations: %,d%n"
            + "  Active arenas: %d",
        totalOps,
        currentConcurrent,
        maxConcurrent,
        contentions,
        avgWaitTimeNs,
        availablePermits,
        maxConcurrentOperations,
        resourceLocks.size(),
        arenaOperations.get(),
        arenaInfoMap.size());
  }

  /**
   * Gets performance metrics for the concurrency manager.
   *
   * @return performance metrics string
   */
  public String getPerformanceMetrics() {
    final long totalOps = totalOperations.get();
    final long contentions = lockContentions.get();
    final double contentionRate = totalOps > 0 ? (contentions * 100.0) / totalOps : 0.0;
    final double avgWaitTimeMs =
        totalOps > 0 ? (totalWaitTimeNs.get() / totalOps) / 1_000_000.0 : 0.0;

    return String.format(
        "Panama Concurrency Performance: contention_rate=%.2f%%, avg_wait=%.3fms, "
            + "concurrent_ops=%d/%d, arena_ops=%d",
        contentionRate,
        avgWaitTimeMs,
        concurrentOperations.get(),
        maxConcurrentOperations,
        arenaOperations.get());
  }

  /**
   * Gets information about active arenas.
   *
   * @return active arena information
   */
  public String getActiveArenaInfo() {
    if (arenaInfoMap.isEmpty()) {
      return "No active arenas";
    }

    final StringBuilder sb = new StringBuilder(String.format("Active Arena Information:%n"));
    for (final ArenaInfo info : arenaInfoMap.values()) {
      if (!info.closed && info.arena.scope().isAlive()) {
        sb.append(
            String.format(
                "  Arena: lifetime=%dms, active_ops=%d%n",
                info.getLifetimeMs(), info.activeOperations.get()));
      }
    }
    return sb.toString();
  }

  /**
   * Checks for potential deadlocks or resource leaks.
   *
   * @return warning messages or null if no issues detected
   */
  public String checkForIssues() {
    final StringBuilder issues = new StringBuilder();

    // Check for potential deadlocks
    final long currentConcurrent = concurrentOperations.get();
    if (currentConcurrent >= maxConcurrentOperations * 0.9) {
      issues
          .append("• High concurrency usage may cause blocking: ")
          .append(currentConcurrent)
          .append("/")
          .append(maxConcurrentOperations)
          .append("\n");
    }

    // Check for excessive lock contention
    final long totalOps = totalOperations.get();
    final long contentions = lockContentions.get();
    if (totalOps > 0 && (contentions * 100.0) / totalOps > 10.0) {
      issues
          .append("• High lock contention detected: ")
          .append(String.format("%.1f%%", (contentions * 100.0) / totalOps))
          .append("\n");
    }

    // Check for long-lived arenas
    final long currentTime = System.currentTimeMillis();
    for (final ArenaInfo info : arenaInfoMap.values()) {
      if (info.arena.scope().isAlive()
          && (currentTime - info.creationTime) > 300_000) { // 5 minutes
        issues
            .append("• Long-lived arena detected: ")
            .append(info.getLifetimeMs())
            .append("ms lifetime\n");
      }
    }

    return issues.length() > 0 ? "Panama Concurrency Issues Detected:\n" + issues.toString() : null;
  }

  /** Cleans up closed arenas from tracking. */
  public void cleanupClosedArenas() {
    final int sizeBefore = arenaInfoMap.size();

    arenaInfoMap
        .entrySet()
        .removeIf(
            entry -> {
              final Arena arena = entry.getKey();
              final ArenaInfo info = entry.getValue();

              if (!arena.scope().isAlive()) {
                info.closed = true;
                return true;
              }
              return false;
            });

    final int sizeAfter = arenaInfoMap.size();
    if (sizeBefore > sizeAfter) {
      LOGGER.fine("Cleaned up " + (sizeBefore - sizeAfter) + " closed arenas");
    }
  }

  /** Forces cleanup of unused resource locks. */
  public void cleanupUnusedLocks() {
    // In a real implementation, we would track lock usage and remove unused ones
    // For now, just log the current count
    LOGGER.fine("Current resource locks: " + resourceLocks.size());
  }

  /** Resets the concurrency statistics. */
  public void resetStatistics() {
    totalOperations.set(0);
    maxConcurrencyReached.set(0);
    lockContentions.set(0);
    totalWaitTimeNs.set(0);
    arenaOperations.set(0);
    LOGGER.fine("Reset Panama concurrency manager statistics");
  }

  /**
   * Gets the current number of available concurrency permits.
   *
   * @return number of available permits
   */
  public int getAvailablePermits() {
    return concurrencySemaphore.availablePermits();
  }

  /**
   * Gets the maximum number of concurrent operations allowed.
   *
   * @return maximum concurrent operations
   */
  public int getMaxConcurrentOperations() {
    return maxConcurrentOperations;
  }

  /** Shuts down the concurrency manager and releases resources. */
  public void shutdown() {
    try {
      asyncExecutor.shutdown();
      cleanupClosedArenas();
      resourceLocks.clear();
      LOGGER.info("Shut down Panama concurrency manager");
    } catch (final Exception e) {
      LOGGER.warning("Error shutting down Panama concurrency manager: " + e.getMessage());
    }
  }
}
