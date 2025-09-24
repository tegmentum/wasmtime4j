package ai.tegmentum.wasmtime4j.panama.performance;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.ref.WeakReference;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Panama-optimized memory pool for frequently allocated native objects and memory segments.
 *
 * <p>This class manages pools of reusable objects optimized for Panama Foreign Function API to
 * minimize allocation overhead and garbage collection pressure. It's particularly useful for native
 * parameter arrays, memory segments, arenas, and other frequently allocated objects in the Panama
 * layer.
 *
 * <p>Panama-specific features:
 *
 * <ul>
 *   <li>Memory segment pooling with automatic size-based categorization
 *   <li>Arena-aware resource management and lifecycle tracking
 *   <li>Zero-copy optimizations for compatible memory segments
 *   <li>Native alignment-aware allocation strategies
 *   <li>Performance monitoring and pool statistics optimized for Panama metrics
 *   <li>Automatic cleanup of unused pools with arena integration
 * </ul>
 *
 * <p>Usage Example:
 *
 * <pre>{@code
 * // Get pool for memory segments
 * PanaNativeObjectPool<MemorySegment> segmentPool = PanaNativeObjectPool.getPool(
 *     MemorySegment.class, (arena) -> arena.allocate(1024), 16);
 *
 * // Borrow and return segments
 * try (Arena arena = Arena.ofConfined()) {
 *   MemorySegment buffer = segmentPool.borrow(arena);
 *   try {
 *     // Use buffer...
 *   } finally {
 *     segmentPool.returnObject(buffer);
 *   }
 * }
 * }</pre>
 *
 * @param <T> the type of objects in this pool
 * @since 1.0.0
 */
public final class PanaNativeObjectPool<T> {

  private static final Logger LOGGER = Logger.getLogger(PanaNativeObjectPool.class.getName());

  /** Global registry of object pools by type. */
  private static final ConcurrentHashMap<Class<?>, WeakReference<PanaNativeObjectPool<?>>> POOLS =
      new ConcurrentHashMap<>();

  /** Default maximum pool size. */
  public static final int DEFAULT_MAX_POOL_SIZE = 32;

  /** Default minimum pool size. */
  public static final int DEFAULT_MIN_POOL_SIZE = 4;

  /** Pool of available objects. */
  private final BlockingQueue<T> availableObjects;

  /** Factory for creating new objects with Arena support. */
  private final ArenaObjectFactory<T> factory;

  /** Object type for this pool. */
  private final Class<T> objectType;

  /** Maximum number of objects in pool. */
  private final int maxPoolSize;

  /** Minimum number of objects in pool. */
  private final int minPoolSize;

  /** Current pool size. */
  private final AtomicInteger currentSize = new AtomicInteger(0);

  /** Number of objects currently borrowed. */
  private final AtomicInteger borrowedCount = new AtomicInteger(0);

  /** Total number of borrow operations. */
  private final AtomicLong totalBorrows = new AtomicLong(0);

  /** Total number of return operations. */
  private final AtomicLong totalReturns = new AtomicLong(0);

  /** Total number of objects created. */
  private final AtomicLong totalCreated = new AtomicLong(0);

  /** Pool creation timestamp. */
  private final long creationTime = System.currentTimeMillis();

  /** Whether this pool is closed. */
  private volatile boolean closed = false;

  /** Panama-specific performance optimization statistics. */
  private final AtomicLong totalBorrowTimeNs = new AtomicLong(0);

  private final AtomicLong totalReturnTimeNs = new AtomicLong(0);
  private final AtomicLong poolMisses = new AtomicLong(0);
  private final AtomicLong poolContention = new AtomicLong(0);

  /** Arena-specific tracking. */
  private final AtomicLong arenaAllocations = new AtomicLong(0);

  private final AtomicLong zeroCopyOperations = new AtomicLong(0);
  private final AtomicLong memorySegmentOperations = new AtomicLong(0);

  /** Pool size optimization. */
  private final AtomicInteger optimalSize = new AtomicInteger(minPoolSize);

  private volatile long lastOptimizationTime = System.currentTimeMillis();
  private static final long OPTIMIZATION_INTERVAL_MS = 10_000; // 10 seconds

  /** Pool prewarming. */
  private final AtomicLong prewarmCount = new AtomicLong(0);

  private volatile boolean prewarmingEnabled = true;

  /**
   * Factory interface for creating pooled objects with Arena support.
   *
   * @param <T> the type of objects to create
   */
  @FunctionalInterface
  public interface ArenaObjectFactory<T> {
    /**
     * Creates a new object instance using the provided arena.
     *
     * @param arena the arena to use for native memory allocation
     * @return new object instance
     */
    T create(Arena arena);
  }

  /**
   * Creates a new Panama-optimized object pool.
   *
   * @param objectType the class of objects in this pool
   * @param factory factory for creating new objects with Arena support
   * @param maxPoolSize maximum number of objects in pool
   * @param minPoolSize minimum number of objects in pool
   * @throws IllegalArgumentException if parameters are invalid
   */
  private PanaNativeObjectPool(
      final Class<T> objectType,
      final ArenaObjectFactory<T> factory,
      final int maxPoolSize,
      final int minPoolSize) {
    if (objectType == null) {
      throw new IllegalArgumentException("objectType cannot be null");
    }
    if (factory == null) {
      throw new IllegalArgumentException("factory cannot be null");
    }
    if (maxPoolSize <= 0) {
      throw new IllegalArgumentException("maxPoolSize must be positive: " + maxPoolSize);
    }
    if (minPoolSize < 0) {
      throw new IllegalArgumentException("minPoolSize cannot be negative: " + minPoolSize);
    }
    if (minPoolSize > maxPoolSize) {
      throw new IllegalArgumentException(
          "minPoolSize cannot exceed maxPoolSize: " + minPoolSize + " > " + maxPoolSize);
    }

    this.objectType = objectType;
    this.factory = factory;
    this.maxPoolSize = maxPoolSize;
    this.minPoolSize = minPoolSize;
    this.availableObjects = new LinkedBlockingQueue<>(maxPoolSize);

    // Pre-populate pool with minimum objects using a temporary arena
    try (Arena initArena = Arena.ofConfined()) {
      for (int i = 0; i < minPoolSize; i++) {
        try {
          final T obj = factory.create(initArena);
          if (obj != null) {
            availableObjects.offer(obj);
            currentSize.incrementAndGet();
            totalCreated.incrementAndGet();
          }
        } catch (final Exception e) {
          LOGGER.warning(
              "Failed to pre-populate Panama pool with "
                  + objectType.getSimpleName()
                  + ": "
                  + e.getMessage());
          break;
        }
      }
    }

    LOGGER.fine(
        "Created Panama "
            + objectType.getSimpleName()
            + " pool with "
            + currentSize.get()
            + "/"
            + maxPoolSize
            + " objects");
  }

  /**
   * Gets or creates a Panama object pool for the specified type.
   *
   * @param <T> the object type
   * @param objectType the class of objects in the pool
   * @param factory factory for creating new objects with Arena support
   * @param maxPoolSize maximum number of objects in pool
   * @return the object pool
   * @throws IllegalArgumentException if parameters are invalid
   */
  @SuppressWarnings("unchecked")
  public static <T> PanaNativeObjectPool<T> getPool(
      final Class<T> objectType, final ArenaObjectFactory<T> factory, final int maxPoolSize) {
    return getPool(objectType, factory, maxPoolSize, DEFAULT_MIN_POOL_SIZE);
  }

  /**
   * Gets or creates a Panama object pool for the specified type.
   *
   * @param <T> the object type
   * @param objectType the class of objects in the pool
   * @param factory factory for creating new objects with Arena support
   * @param maxPoolSize maximum number of objects in pool
   * @param minPoolSize minimum number of objects in pool
   * @return the object pool
   * @throws IllegalArgumentException if parameters are invalid
   */
  @SuppressWarnings("unchecked")
  public static <T> PanaNativeObjectPool<T> getPool(
      final Class<T> objectType,
      final ArenaObjectFactory<T> factory,
      final int maxPoolSize,
      final int minPoolSize) {
    if (objectType == null) {
      throw new IllegalArgumentException("objectType cannot be null");
    }

    // Check if pool already exists
    final WeakReference<PanaNativeObjectPool<?>> poolRef = POOLS.get(objectType);
    if (poolRef != null) {
      final PanaNativeObjectPool<?> existingPool = poolRef.get();
      if (existingPool != null && !existingPool.isClosed()) {
        return (PanaNativeObjectPool<T>) existingPool;
      }
      // Remove stale reference
      POOLS.remove(objectType, poolRef);
    }

    // Create new pool
    final PanaNativeObjectPool<T> newPool =
        new PanaNativeObjectPool<>(objectType, factory, maxPoolSize, minPoolSize);

    // Register in global pool registry
    POOLS.put(objectType, new WeakReference<>(newPool));

    return newPool;
  }

  /**
   * Borrows an object from the pool using the provided arena for new allocations.
   *
   * @param arena the arena to use if a new object needs to be created
   * @return an object from the pool or a newly created object
   * @throws IllegalStateException if the pool is closed
   * @throws IllegalArgumentException if arena is null
   */
  public T borrow(final Arena arena) {
    if (closed) {
      throw new IllegalStateException("Pool is closed");
    }
    if (arena == null) {
      throw new IllegalArgumentException("Arena cannot be null");
    }

    final long startTime = System.nanoTime();
    totalBorrows.incrementAndGet();
    borrowedCount.incrementAndGet();

    try {
      // Try to get object from pool first
      T obj = availableObjects.poll();
      if (obj != null) {
        // Pool hit - optimal case
        optimizePoolSizeIfNeeded();

        // Track arena operation if it's a MemorySegment
        if (obj instanceof MemorySegment) {
          memorySegmentOperations.incrementAndGet();
          PanamaPerformanceMonitor.recordMemorySegmentAllocation(arena, (MemorySegment) obj);
        }

        return obj;
      }

      // Pool miss - track for optimization
      poolMisses.incrementAndGet();

      // Check for contention
      if (borrowedCount.get() > maxPoolSize * 0.8) {
        poolContention.incrementAndGet();
      }

      // Pool is empty, create new object using provided arena
      try {
        obj = factory.create(arena);
        if (obj != null) {
          totalCreated.incrementAndGet();
          arenaAllocations.incrementAndGet();

          // Track arena operation
          if (obj instanceof MemorySegment) {
            memorySegmentOperations.incrementAndGet();
            PanamaPerformanceMonitor.recordMemorySegmentAllocation(arena, (MemorySegment) obj);
          }

          prewarmPoolIfNeeded(arena);
          return obj;
        }
      } catch (final Exception e) {
        LOGGER.warning(
            "Failed to create new Panama "
                + objectType.getSimpleName()
                + " object: "
                + e.getMessage());
      }

      // Fallback - return null (caller should handle)
      borrowedCount.decrementAndGet();
      return null;

    } finally {
      totalBorrowTimeNs.addAndGet(System.nanoTime() - startTime);
    }
  }

  /**
   * Returns an object to the pool.
   *
   * <p>The returned object should be in a clean, reusable state. If the pool is full, the object
   * will be discarded to prevent unbounded growth.
   *
   * @param obj the object to return
   * @throws IllegalArgumentException if obj is null
   */
  public void returnObject(final T obj) {
    if (obj == null) {
      throw new IllegalArgumentException("Cannot return null object");
    }
    if (closed) {
      // Pool is closed, don't return object
      borrowedCount.decrementAndGet();
      return;
    }

    final long startTime = System.nanoTime();
    totalReturns.incrementAndGet();
    borrowedCount.decrementAndGet();

    try {
      // Try to return object to pool
      if (currentSize.get() < maxPoolSize && availableObjects.offer(obj)) {
        // Successfully returned to pool

        // Track zero-copy operation if applicable
        if (obj instanceof MemorySegment) {
          zeroCopyOperations.incrementAndGet();
          PanamaPerformanceMonitor.recordZeroCopyOperation();
        }

        return;
      }

      // Pool is full, object will be garbage collected
      // This is intentional to prevent unbounded memory growth
    } finally {
      totalReturnTimeNs.addAndGet(System.nanoTime() - startTime);
    }
  }

  /**
   * Gets the current number of available objects in the pool.
   *
   * @return number of available objects
   */
  public int getAvailableCount() {
    return availableObjects.size();
  }

  /**
   * Gets the current number of borrowed objects.
   *
   * @return number of borrowed objects
   */
  public int getBorrowedCount() {
    return borrowedCount.get();
  }

  /**
   * Gets the maximum pool size.
   *
   * @return maximum pool size
   */
  public int getMaxPoolSize() {
    return maxPoolSize;
  }

  /**
   * Gets the total number of arena allocations.
   *
   * @return total arena allocations
   */
  public long getArenaAllocations() {
    return arenaAllocations.get();
  }

  /**
   * Gets the total number of zero-copy operations.
   *
   * @return total zero-copy operations
   */
  public long getZeroCopyOperations() {
    return zeroCopyOperations.get();
  }

  /**
   * Gets the total number of memory segment operations.
   *
   * @return total memory segment operations
   */
  public long getMemorySegmentOperations() {
    return memorySegmentOperations.get();
  }

  /**
   * Gets the pool hit rate (percentage of borrows satisfied from pool).
   *
   * @return hit rate as percentage (0.0 to 100.0)
   */
  public double getHitRate() {
    final long borrows = totalBorrows.get();
    if (borrows == 0) {
      return 100.0;
    }
    final long created = totalCreated.get();
    final long hits = Math.max(0, borrows - created);
    return (hits * 100.0) / borrows;
  }

  /**
   * Gets the object type for this pool.
   *
   * @return object type class
   */
  public Class<T> getObjectType() {
    return objectType;
  }

  /**
   * Checks if this pool is closed.
   *
   * @return true if closed
   */
  public boolean isClosed() {
    return closed;
  }

  /** Clears all objects from the pool. */
  public void clear() {
    availableObjects.clear();
    currentSize.set(0);
    LOGGER.fine("Cleared Panama " + objectType.getSimpleName() + " pool");
  }

  /** Closes this pool and releases all resources. */
  public void close() {
    if (closed) {
      return;
    }

    closed = true;
    clear();

    // Remove from global registry
    POOLS.remove(objectType);

    LOGGER.fine(
        "Closed Panama "
            + objectType.getSimpleName()
            + " pool (borrowed="
            + borrowedCount.get()
            + ", hit_rate="
            + String.format("%.1f", getHitRate())
            + "%, arena_allocs="
            + arenaAllocations.get()
            + ")");
  }

  /**
   * Gets Panama pool statistics as a formatted string.
   *
   * @return pool statistics including Panama-specific metrics
   */
  public String getStats() {
    return String.format(
        "Panama %s pool: available=%d/%d, borrowed=%d, total_borrows=%d, "
            + "total_returns=%d, total_created=%d, hit_rate=%.1f%%, "
            + "arena_allocs=%d, zero_copy_ops=%d, memseg_ops=%d, uptime=%dms",
        objectType.getSimpleName(),
        getAvailableCount(),
        maxPoolSize,
        getBorrowedCount(),
        totalBorrows.get(),
        totalReturns.get(),
        totalCreated.get(),
        getHitRate(),
        arenaAllocations.get(),
        zeroCopyOperations.get(),
        memorySegmentOperations.get(),
        System.currentTimeMillis() - creationTime);
  }

  @Override
  public String toString() {
    return "Panama"
        + objectType.getSimpleName()
        + "Pool{available="
        + getAvailableCount()
        + "/"
        + maxPoolSize
        + ", borrowed="
        + getBorrowedCount()
        + ", closed="
        + closed
        + ", arena_allocs="
        + arenaAllocations.get()
        + "}";
  }

  /**
   * Gets statistics for all registered Panama pools.
   *
   * @return statistics for all pools
   */
  public static String getAllPoolStats() {
    final StringBuilder sb = new StringBuilder("Panama NativeObjectPool Statistics:\n");
    for (final WeakReference<PanaNativeObjectPool<?>> poolRef : POOLS.values()) {
      final PanaNativeObjectPool<?> pool = poolRef.get();
      if (pool != null && !pool.isClosed()) {
        sb.append("  ").append(pool.getStats()).append("\n");
      }
    }
    return sb.toString();
  }

  /** Clears all pools and releases all resources. */
  public static void clearAllPools() {
    for (final WeakReference<PanaNativeObjectPool<?>> poolRef : POOLS.values()) {
      final PanaNativeObjectPool<?> pool = poolRef.get();
      if (pool != null) {
        pool.close();
      }
    }
    POOLS.clear();
    LOGGER.info("Cleared all Panama native object pools");
  }

  /** Optimizes pool size based on usage patterns. */
  private void optimizePoolSizeIfNeeded() {
    final long currentTime = System.currentTimeMillis();
    if (currentTime - lastOptimizationTime < OPTIMIZATION_INTERVAL_MS) {
      return;
    }

    lastOptimizationTime = currentTime;

    final long borrows = totalBorrows.get();
    final long misses = poolMisses.get();
    final double missRate = borrows > 0 ? (misses * 100.0) / borrows : 0.0;

    // Adjust optimal size based on miss rate
    final int currentOptimalSize = optimalSize.get();
    if (missRate > 20.0 && currentOptimalSize < maxPoolSize) {
      // High miss rate - increase pool size
      final int newOptimalSize = Math.min(maxPoolSize, currentOptimalSize + 2);
      optimalSize.set(newOptimalSize);
      LOGGER.fine(
          String.format(
              "Increased optimal Panama pool size for %s to %d (miss rate: %.1f%%)",
              objectType.getSimpleName(), newOptimalSize, missRate));
    } else if (missRate < 5.0 && currentOptimalSize > minPoolSize) {
      // Low miss rate - decrease pool size
      final int newOptimalSize = Math.max(minPoolSize, currentOptimalSize - 1);
      optimalSize.set(newOptimalSize);
      LOGGER.fine(
          String.format(
              "Decreased optimal Panama pool size for %s to %d (miss rate: %.1f%%)",
              objectType.getSimpleName(), newOptimalSize, missRate));
    }
  }

  /** Prewarns the pool if needed based on usage patterns. */
  private void prewarmPoolIfNeeded(final Arena arena) {
    if (!prewarmingEnabled || closed || arena == null) {
      return;
    }

    final int available = getAvailableCount();
    final int optimal = optimalSize.get();

    if (available < optimal / 2) {
      // Pool is below half capacity - prewarm
      prewarmToOptimalSize(arena);
    }
  }

  /** Prewarns the pool to optimal size using the provided arena. */
  private void prewarmToOptimalSize(final Arena arena) {
    if (!prewarmingEnabled || closed || arena == null) {
      return;
    }

    final int available = getAvailableCount();
    final int optimal = optimalSize.get();
    final int toCreate = Math.min(optimal - available, maxPoolSize - currentSize.get());

    for (int i = 0; i < toCreate; i++) {
      try {
        final T obj = factory.create(arena);
        if (obj != null && availableObjects.offer(obj)) {
          currentSize.incrementAndGet();
          totalCreated.incrementAndGet();
          prewarmCount.incrementAndGet();
          arenaAllocations.incrementAndGet();
        } else {
          break; // Pool full or creation failed
        }
      } catch (final Exception e) {
        LOGGER.warning(
            "Failed to prewarm Panama " + objectType.getSimpleName() + " pool: " + e.getMessage());
        break;
      }
    }

    if (toCreate > 0) {
      LOGGER.fine(
          String.format(
              "Prewarmed Panama %s pool with %d objects (now %d/%d)",
              objectType.getSimpleName(), toCreate, getAvailableCount(), optimal));
    }
  }

  /**
   * Gets Panama-specific performance statistics for this pool.
   *
   * @return Panama performance statistics
   */
  public String getPanamaPerformanceStats() {
    return String.format(
        "Panama %s performance: avg_borrow=%.0fns, avg_return=%.0fns, miss_rate=%.1f%%, "
            + "contention=%.1f%%, arena_allocs=%d, zero_copy=%d, memseg_ops=%d, prewarmed=%d",
        objectType.getSimpleName(),
        totalBorrows.get() > 0 ? (double) totalBorrowTimeNs.get() / totalBorrows.get() : 0.0,
        totalReturns.get() > 0 ? (double) totalReturnTimeNs.get() / totalReturns.get() : 0.0,
        totalBorrows.get() > 0 ? (poolMisses.get() * 100.0) / totalBorrows.get() : 0.0,
        totalBorrows.get() > 0 ? (poolContention.get() * 100.0) / totalBorrows.get() : 0.0,
        arenaAllocations.get(),
        zeroCopyOperations.get(),
        memorySegmentOperations.get(),
        prewarmCount.get());
  }
}
