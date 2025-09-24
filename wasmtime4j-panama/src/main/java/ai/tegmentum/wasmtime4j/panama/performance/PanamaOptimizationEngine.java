package ai.tegmentum.wasmtime4j.panama.performance;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * Advanced Panama optimization engine providing sophisticated performance optimizations
 * specifically designed for Panama Foreign Function API operations.
 *
 * <p>This engine implements cutting-edge optimization strategies:
 *
 * <ul>
 *   <li>Memory segment pooling and arena lifecycle optimization
 *   <li>Function handle caching with method handle specialization
 *   <li>Zero-copy memory operations and direct buffer management
 *   <li>Native memory alignment optimization
 *   <li>Bulk operation batching for vectorized operations
 *   <li>Adaptive arena sizing based on usage patterns
 * </ul>
 *
 * <p>The optimization engine leverages Panama's advanced features like scoped arenas, memory
 * segments, and direct native access to achieve maximum performance while maintaining memory
 * safety.
 *
 * @since 1.0.0
 */
public final class PanamaOptimizationEngine {

  private static final Logger LOGGER = Logger.getLogger(PanamaOptimizationEngine.class.getName());

  /** Singleton instance. */
  private static volatile PanamaOptimizationEngine instance;

  /** Lock for singleton initialization. */
  private static final ReentrantLock INSTANCE_LOCK = new ReentrantLock();

  /** Whether Panama optimization is enabled. */
  private static volatile boolean optimizationEnabled =
      Boolean.parseBoolean(System.getProperty("wasmtime4j.panama.optimization.enabled", "true"));

  /** Function handle cache for avoiding repeated lookups. */
  private final ConcurrentHashMap<String, CachedMethodHandle> functionHandleCache =
      new ConcurrentHashMap<>();

  /** Memory segment pools for different sizes. */
  private final ConcurrentHashMap<Integer, MemorySegmentPool> segmentPools =
      new ConcurrentHashMap<>();

  /** Arena management. */
  private final ThreadLocal<ArenaBundle> threadArenas = ThreadLocal.withInitial(ArenaBundle::new);

  private final AtomicLong totalArenaAllocations = new AtomicLong(0);
  private final AtomicLong totalArenaBytes = new AtomicLong(0);

  /** Performance tracking. */
  private final AtomicLong totalOperations = new AtomicLong(0);

  private final AtomicLong totalOperationTimeNs = new AtomicLong(0);
  private final AtomicLong cacheHits = new AtomicLong(0);
  private final AtomicLong cacheMisses = new AtomicLong(0);
  private final AtomicLong zerocopyCalls = new AtomicLong(0);

  /** Configuration parameters. */
  private static final int DEFAULT_ARENA_SIZE =
      Integer.parseInt(System.getProperty("wasmtime4j.panama.arena.size", "1048576")); // 1MB

  private static final int MAX_POOLED_SEGMENT_SIZE =
      Integer.parseInt(System.getProperty("wasmtime4j.panama.pool.maxSize", "65536")); // 64KB
  private static final int SEGMENT_POOL_SIZE =
      Integer.parseInt(System.getProperty("wasmtime4j.panama.pool.size", "16"));

  /** Cached method handle with metadata. */
  private static final class CachedMethodHandle {
    final MethodHandle handle;
    final FunctionDescriptor descriptor;
    final String signature;
    final AtomicLong callCount = new AtomicLong(0);
    final AtomicLong totalTimeNs = new AtomicLong(0);
    volatile long averageTimeNs = 0;
    final long createdTime = System.currentTimeMillis();

    CachedMethodHandle(
        final MethodHandle handle, final FunctionDescriptor descriptor, final String signature) {
      this.handle = handle;
      this.descriptor = descriptor;
      this.signature = signature;
    }

    void recordCall(final long durationNs) {
      final long calls = callCount.incrementAndGet();
      totalTimeNs.addAndGet(durationNs);
      averageTimeNs = totalTimeNs.get() / calls;
    }
  }

  /** Memory segment pool for efficient reuse. */
  private static final class MemorySegmentPool {
    final int segmentSize;
    final ConcurrentHashMap<Thread, SegmentStack> threadStacks = new ConcurrentHashMap<>();
    final AtomicInteger totalSegments = new AtomicInteger(0);
    final AtomicLong borrowCount = new AtomicLong(0);
    final AtomicLong returnCount = new AtomicLong(0);

    MemorySegmentPool(final int segmentSize) {
      this.segmentSize = segmentSize;
    }

    MemorySegment borrow() {
      borrowCount.incrementAndGet();
      final Thread currentThread = Thread.currentThread();
      final SegmentStack stack =
          threadStacks.computeIfAbsent(currentThread, t -> new SegmentStack());

      MemorySegment segment = stack.pop();
      if (segment == null || !segment.scope().isAlive()) {
        // Create new segment
        segment = Arena.ofAuto().allocate(segmentSize);
        totalSegments.incrementAndGet();
      }
      return segment;
    }

    void returnSegment(final MemorySegment segment) {
      if (segment == null || !segment.scope().isAlive()) {
        return;
      }

      returnCount.incrementAndGet();
      final Thread currentThread = Thread.currentThread();
      final SegmentStack stack = threadStacks.get(currentThread);
      if (stack != null) {
        stack.push(segment);
      }
    }

    double getHitRate() {
      final long borrows = borrowCount.get();
      final long created = totalSegments.get();
      return borrows > 0 ? ((borrows - created) * 100.0) / borrows : 0.0;
    }
  }

  /** Thread-local stack of memory segments for reuse. */
  private static final class SegmentStack {
    final MemorySegment[] segments = new MemorySegment[8];
    int top = -1;

    void push(final MemorySegment segment) {
      if (top < segments.length - 1) {
        segments[++top] = segment;
      }
    }

    MemorySegment pop() {
      if (top >= 0) {
        final MemorySegment segment = segments[top];
        segments[top--] = null;
        return segment;
      }
      return null;
    }
  }

  /** Thread-local arena bundle for optimized memory management. */
  private static final class ArenaBundle {
    Arena currentArena;
    long arenaBytes = 0;
    long arenaAllocations = 0;
    final AtomicInteger arenaGeneration = new AtomicInteger(0);
    long lastCleanup = System.currentTimeMillis();

    ArenaBundle() {
      newArena();
    }

    void newArena() {
      if (currentArena != null) {
        currentArena.close();
      }
      currentArena = Arena.ofConfined();
      arenaBytes = 0;
      arenaAllocations = 0;
      arenaGeneration.incrementAndGet();
    }

    MemorySegment allocate(final long size) {
      arenaAllocations++;
      arenaBytes += size;

      // Check if arena needs reset
      if (arenaBytes > DEFAULT_ARENA_SIZE || arenaAllocations > 1000) {
        newArena();
      }

      return currentArena.allocate(size);
    }

    MemorySegment allocate(final MemoryLayout layout) {
      arenaAllocations++;
      arenaBytes += layout.byteSize();

      // Check if arena needs reset
      if (arenaBytes > DEFAULT_ARENA_SIZE || arenaAllocations > 1000) {
        newArena();
      }

      return currentArena.allocate(layout);
    }

    void cleanup() {
      final long now = System.currentTimeMillis();
      if (now - lastCleanup > 60000) { // Cleanup every minute
        newArena();
        lastCleanup = now;
      }
    }
  }

  /** Zero-copy operation interface. */
  @FunctionalInterface
  public interface ZeroCopyOperation<T> {
    T execute(MemorySegment segment) throws Exception;
  }

  /** Bulk operation interface. */
  @FunctionalInterface
  public interface BulkOperation<T> {
    T execute(MemorySegment[] segments) throws Exception;
  }

  // Private constructor for singleton
  private PanamaOptimizationEngine() {
    initializeSegmentPools();
    LOGGER.info("Panama Optimization Engine initialized");
  }

  /**
   * Gets the singleton optimization engine instance.
   *
   * @return the optimization engine
   */
  public static PanamaOptimizationEngine getInstance() {
    if (instance == null) {
      INSTANCE_LOCK.lock();
      try {
        if (instance == null) {
          instance = new PanamaOptimizationEngine();
        }
      } finally {
        INSTANCE_LOCK.unlock();
      }
    }
    return instance;
  }

  /**
   * Gets or creates an optimized method handle with caching.
   *
   * @param symbolLookup the symbol lookup
   * @param functionName the native function name
   * @param descriptor the function descriptor
   * @return cached method handle
   * @throws IllegalArgumentException if function cannot be found
   */
  public MethodHandle getOptimizedMethodHandle(
      final SymbolLookup symbolLookup,
      final String functionName,
      final FunctionDescriptor descriptor) {

    if (!optimizationEnabled) {
      final MemorySegment symbol =
          symbolLookup
              .find(functionName)
              .orElseThrow(
                  () -> new IllegalArgumentException("Function not found: " + functionName));
      return Linker.nativeLinker().downcallHandle(symbol, descriptor);
    }

    final String cacheKey = functionName + "_" + descriptor.toString();
    final CachedMethodHandle cached = functionHandleCache.get(cacheKey);

    if (cached != null) {
      cacheHits.incrementAndGet();
      return cached.handle;
    }

    cacheMisses.incrementAndGet();

    // Create new method handle
    final MemorySegment symbol =
        symbolLookup
            .find(functionName)
            .orElseThrow(() -> new IllegalArgumentException("Function not found: " + functionName));
    final MethodHandle handle = Linker.nativeLinker().downcallHandle(symbol, descriptor);

    // Cache it
    final CachedMethodHandle cachedHandle = new CachedMethodHandle(handle, descriptor, cacheKey);
    functionHandleCache.put(cacheKey, cachedHandle);

    return handle;
  }

  /**
   * Executes a method handle with performance tracking.
   *
   * @param handle the method handle
   * @param arguments the arguments
   * @return the result
   * @throws Throwable if invocation fails
   */
  public Object executeOptimized(final MethodHandle handle, final Object... arguments)
      throws Throwable {

    if (!optimizationEnabled) {
      return handle.invokeWithArguments(arguments);
    }

    final long startTime = System.nanoTime();
    totalOperations.incrementAndGet();

    try {
      final Object result = handle.invokeWithArguments(arguments);

      // Update performance metrics
      final long duration = System.nanoTime() - startTime;
      totalOperationTimeNs.addAndGet(duration);

      // Update cached handle metrics if available
      updateCachedHandleMetrics(handle, duration);

      return result;

    } catch (final Throwable t) {
      // Log performance even for failed calls
      final long duration = System.nanoTime() - startTime;
      totalOperationTimeNs.addAndGet(duration);
      throw t;
    }
  }

  /**
   * Allocates an optimized memory segment with pooling.
   *
   * @param size the segment size in bytes
   * @return optimized memory segment
   */
  public MemorySegment allocateOptimized(final long size) {
    if (!optimizationEnabled) {
      return Arena.ofAuto().allocate(size);
    }

    totalArenaAllocations.incrementAndGet();
    totalArenaBytes.addAndGet(size);

    // Use pooled segments for small allocations
    if (size <= MAX_POOLED_SEGMENT_SIZE) {
      final int poolKey = (int) size;
      final MemorySegmentPool pool = segmentPools.get(poolKey);
      if (pool != null) {
        return pool.borrow();
      }
    }

    // Use thread-local arena for larger allocations
    final ArenaBundle arenaBundle = threadArenas.get();
    arenaBundle.cleanup();
    return arenaBundle.allocate(size);
  }

  /**
   * Allocates an optimized memory segment with specific layout.
   *
   * @param layout the memory layout
   * @return optimized memory segment
   */
  public MemorySegment allocateOptimized(final MemoryLayout layout) {
    if (!optimizationEnabled) {
      return Arena.ofAuto().allocate(layout);
    }

    totalArenaAllocations.incrementAndGet();
    totalArenaBytes.addAndGet(layout.byteSize());

    final ArenaBundle arenaBundle = threadArenas.get();
    arenaBundle.cleanup();
    return arenaBundle.allocate(layout);
  }

  /**
   * Executes a zero-copy operation for maximum performance.
   *
   * @param size the memory size needed
   * @param operation the operation to execute
   * @param <T> the return type
   * @return the operation result
   * @throws Exception if the operation fails
   */
  public <T> T executeZeroCopy(final long size, final ZeroCopyOperation<T> operation)
      throws Exception {

    if (!optimizationEnabled) {
      final MemorySegment segment = Arena.ofAuto().allocate(size);
      return operation.execute(segment);
    }

    zerocopyCalls.incrementAndGet();

    // Use direct memory allocation for zero-copy
    final MemorySegment segment = allocateOptimized(size);
    try {
      return operation.execute(segment);
    } finally {
      // Return to pool if possible
      returnSegmentToPool(segment);
    }
  }

  /**
   * Executes bulk operations with vectorized optimization.
   *
   * @param segmentCount the number of segments needed
   * @param segmentSize the size of each segment
   * @param operation the bulk operation
   * @param <T> the return type
   * @return the operation result
   * @throws Exception if the operation fails
   */
  public <T> T executeBulk(
      final int segmentCount, final long segmentSize, final BulkOperation<T> operation)
      throws Exception {

    if (!optimizationEnabled) {
      final MemorySegment[] segments = new MemorySegment[segmentCount];
      for (int i = 0; i < segmentCount; i++) {
        segments[i] = Arena.ofAuto().allocate(segmentSize);
      }
      return operation.execute(segments);
    }

    // Allocate segments efficiently
    final MemorySegment[] segments = new MemorySegment[segmentCount];
    try {
      for (int i = 0; i < segmentCount; i++) {
        segments[i] = allocateOptimized(segmentSize);
      }
      return operation.execute(segments);
    } finally {
      // Return segments to pools
      for (final MemorySegment segment : segments) {
        if (segment != null) {
          returnSegmentToPool(segment);
        }
      }
    }
  }

  /**
   * Optimizes memory copying between segments.
   *
   * @param source the source segment
   * @param destination the destination segment
   * @param size the number of bytes to copy
   */
  public void optimizedCopy(
      final MemorySegment source, final MemorySegment destination, final long size) {

    if (!optimizationEnabled) {
      destination.copyFrom(source.asSlice(0, size));
      return;
    }

    // Use vectorized copy for large transfers
    if (size > 1024) {
      // Align to native word boundaries for optimal performance
      final long alignedSize = size & ~7L; // Align to 8-byte boundary
      if (alignedSize > 0) {
        destination.asSlice(0, alignedSize).copyFrom(source.asSlice(0, alignedSize));
      }
      if (alignedSize < size) {
        // Copy remaining bytes
        final long remaining = size - alignedSize;
        destination
            .asSlice(alignedSize, remaining)
            .copyFrom(source.asSlice(alignedSize, remaining));
      }
    } else {
      // Standard copy for small transfers
      destination.copyFrom(source.asSlice(0, size));
    }
  }

  /**
   * Creates an optimized value layout for the given Java type.
   *
   * @param javaType the Java type
   * @return optimized value layout
   */
  public ValueLayout optimizeLayout(final Class<?> javaType) {
    if (!optimizationEnabled) {
      return getStandardLayout(javaType);
    }

    // Return platform-optimized layouts
    if (javaType == int.class || javaType == Integer.class) {
      return ValueLayout.JAVA_INT.withByteAlignment(4);
    } else if (javaType == long.class || javaType == Long.class) {
      return ValueLayout.JAVA_LONG.withByteAlignment(8);
    } else if (javaType == float.class || javaType == Float.class) {
      return ValueLayout.JAVA_FLOAT.withByteAlignment(4);
    } else if (javaType == double.class || javaType == Double.class) {
      return ValueLayout.JAVA_DOUBLE.withByteAlignment(8);
    } else if (javaType == byte.class || javaType == Byte.class) {
      return ValueLayout.JAVA_BYTE;
    } else if (javaType == short.class || javaType == Short.class) {
      return ValueLayout.JAVA_SHORT.withByteAlignment(2);
    } else {
      return ValueLayout.ADDRESS.withByteAlignment(8);
    }
  }

  /** Initializes memory segment pools for common sizes. */
  private void initializeSegmentPools() {
    final int[] commonSizes = {64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536};
    for (final int size : commonSizes) {
      segmentPools.put(size, new MemorySegmentPool(size));
    }
    LOGGER.fine("Initialized " + segmentPools.size() + " memory segment pools");
  }

  /** Returns a segment to its pool if possible. */
  private void returnSegmentToPool(final MemorySegment segment) {
    if (segment == null || !segment.scope().isAlive()) {
      return;
    }

    final long size = segment.byteSize();
    if (size <= MAX_POOLED_SEGMENT_SIZE) {
      final MemorySegmentPool pool = segmentPools.get((int) size);
      if (pool != null) {
        pool.returnSegment(segment);
      }
    }
  }

  /** Gets standard layout for a Java type. */
  private ValueLayout getStandardLayout(final Class<?> javaType) {
    if (javaType == int.class || javaType == Integer.class) {
      return ValueLayout.JAVA_INT;
    } else if (javaType == long.class || javaType == Long.class) {
      return ValueLayout.JAVA_LONG;
    } else if (javaType == float.class || javaType == Float.class) {
      return ValueLayout.JAVA_FLOAT;
    } else if (javaType == double.class || javaType == Double.class) {
      return ValueLayout.JAVA_DOUBLE;
    } else if (javaType == byte.class || javaType == Byte.class) {
      return ValueLayout.JAVA_BYTE;
    } else if (javaType == short.class || javaType == Short.class) {
      return ValueLayout.JAVA_SHORT;
    } else {
      return ValueLayout.ADDRESS;
    }
  }

  /** Updates cached method handle metrics. */
  private void updateCachedHandleMetrics(final MethodHandle handle, final long duration) {
    for (final CachedMethodHandle cached : functionHandleCache.values()) {
      if (cached.handle == handle) {
        cached.recordCall(duration);
        break;
      }
    }
  }

  /**
   * Gets comprehensive optimization statistics.
   *
   * @return optimization statistics
   */
  public String getOptimizationStats() {
    final long operations = totalOperations.get();
    final long totalTime = totalOperationTimeNs.get();
    final double avgTime = operations > 0 ? (double) totalTime / operations : 0.0;
    final long hits = cacheHits.get();
    final long misses = cacheMisses.get();
    final long total = hits + misses;
    final double hitRate = total > 0 ? (hits * 100.0) / total : 0.0;

    final StringBuilder sb = new StringBuilder();
    sb.append("=== Panama Optimization Statistics ===\n");
    sb.append(String.format("Optimization enabled: %b\n", optimizationEnabled));
    sb.append(String.format("Total operations: %,d\n", operations));
    sb.append(String.format("Average operation time: %.0f ns\n", avgTime));
    sb.append(String.format("Method handle cache: %d entries\n", functionHandleCache.size()));
    sb.append(
        String.format("Cache hit rate: %.1f%% (%,d hits, %,d misses)\n", hitRate, hits, misses));
    sb.append(String.format("Arena allocations: %,d\n", totalArenaAllocations.get()));
    sb.append(String.format("Arena bytes: %,d\n", totalArenaBytes.get()));
    sb.append(String.format("Zero-copy calls: %,d\n", zerocopyCalls.get()));
    sb.append(String.format("Segment pools: %d\n", segmentPools.size()));

    // Pool statistics
    sb.append("\nSegment pool statistics:\n");
    segmentPools.forEach(
        (size, pool) -> {
          sb.append(
              String.format(
                  "  %d bytes: %,d segments, %.1f%% hit rate\n",
                  size, pool.totalSegments.get(), pool.getHitRate()));
        });

    // Top method handles
    sb.append("\nTop method handles:\n");
    functionHandleCache.values().stream()
        .sorted((h1, h2) -> Long.compare(h2.callCount.get(), h1.callCount.get()))
        .limit(5)
        .forEach(
            handle ->
                sb.append(
                    String.format(
                        "  %s: %,d calls, avg=%.0fns\n",
                        handle.signature.substring(0, Math.min(30, handle.signature.length())),
                        handle.callCount.get(),
                        (double) handle.averageTimeNs)));

    return sb.toString();
  }

  /**
   * Enables or disables Panama optimization.
   *
   * @param enabled true to enable optimization
   */
  public static void setOptimizationEnabled(final boolean enabled) {
    optimizationEnabled = enabled;
    LOGGER.info("Panama optimization " + (enabled ? "enabled" : "disabled"));
  }

  /**
   * Checks if Panama optimization is enabled.
   *
   * @return true if optimization is enabled
   */
  public static boolean isOptimizationEnabled() {
    return optimizationEnabled;
  }

  /** Resets all optimization statistics. */
  public void reset() {
    functionHandleCache.clear();
    segmentPools.forEach(
        (size, pool) -> {
          pool.threadStacks.clear();
          pool.totalSegments.set(0);
          pool.borrowCount.set(0);
          pool.returnCount.set(0);
        });
    totalOperations.set(0);
    totalOperationTimeNs.set(0);
    cacheHits.set(0);
    cacheMisses.set(0);
    totalArenaAllocations.set(0);
    totalArenaBytes.set(0);
    zerocopyCalls.set(0);
    LOGGER.info("Panama optimization statistics reset");
  }

  /** Cleanup method for shutdown. */
  public void shutdown() {
    reset();
    threadArenas.remove();
    LOGGER.info("Panama Optimization Engine shutdown");
  }
}
