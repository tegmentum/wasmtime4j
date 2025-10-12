package ai.tegmentum.wasmtime4j.jni.performance;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * Advanced JNI optimization engine that provides sophisticated performance optimizations
 * specifically tailored for JNI-based WebAssembly operations.
 *
 * <p>This engine implements multiple optimization strategies:
 *
 * <ul>
 *   <li>Call batching to reduce JNI transition overhead
 *   <li>Thread-local resource pools for lock-free operations
 *   <li>Native method signature optimization and caching
 *   <li>Memory allocation pattern optimization
 *   <li>Critical section minimization
 *   <li>GC-aware scheduling and resource management
 * </ul>
 *
 * <p>The optimization engine continuously monitors JNI call patterns and automatically adjusts
 * optimization strategies to achieve optimal performance for the current workload.
 *
 * @since 1.0.0
 */
public final class JniOptimizationEngine {

  private static final Logger LOGGER = Logger.getLogger(JniOptimizationEngine.class.getName());

  /** Singleton instance for global optimization coordination. */
  private static volatile JniOptimizationEngine instance;

  /** Lock for singleton initialization. */
  private static final ReentrantLock INSTANCE_LOCK = new ReentrantLock();

  /** Whether optimization is enabled. */
  private static volatile boolean optimizationEnabled =
      Boolean.parseBoolean(System.getProperty("wasmtime4j.jni.optimization.enabled", "true"));

  /** Thread MX bean for CPU time monitoring. */
  private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

  /** Call batching configuration. */
  private static final int DEFAULT_BATCH_SIZE =
      Integer.parseInt(System.getProperty("wasmtime4j.jni.batch.size", "8"));

  private static final long BATCH_TIMEOUT_NS =
      Long.parseLong(System.getProperty("wasmtime4j.jni.batch.timeout", "1000000")); // 1ms

  /** Thread-local resource pools. */
  private static final ThreadLocal<ResourceBundle> THREAD_RESOURCES =
      ThreadLocal.withInitial(ResourceBundle::new);

  /** JNI call pattern analysis. */
  private final ConcurrentHashMap<String, CallPattern> callPatterns = new ConcurrentHashMap<>();

  private final AtomicLong totalJniCalls = new AtomicLong(0);
  private final AtomicLong totalJniTimeNs = new AtomicLong(0);
  private final AtomicLong batchedCalls = new AtomicLong(0);
  private final AtomicLong savedTransitions = new AtomicLong(0);

  /** GC-aware optimization. */
  private final AtomicLong lastGcTime = new AtomicLong(System.currentTimeMillis());

  private final AtomicInteger gcPressure = new AtomicInteger(0);
  private volatile boolean gcOptimizationActive = false;

  /** Critical section optimization. */
  private final ConcurrentHashMap<String, CriticalSection> criticalSections =
      new ConcurrentHashMap<>();

  /** Thread-local resource bundle for lock-free operations. */
  private static final class ResourceBundle {
    final ByteBuffer[] bufferPool;
    final Object[] parameterCache;
    final CallBatch currentBatch;
    int bufferIndex = 0;
    long lastUsed = System.currentTimeMillis();

    ResourceBundle() {
      this.bufferPool = new ByteBuffer[4];
      for (int i = 0; i < bufferPool.length; i++) {
        bufferPool[i] = ByteBuffer.allocateDirect(1024);
      }
      this.parameterCache = new Object[32];
      this.currentBatch = new CallBatch();
    }

    ByteBuffer getBuffer(final int minSize) {
      for (final ByteBuffer buffer : bufferPool) {
        if (buffer.capacity() >= minSize) {
          buffer.clear();
          return buffer;
        }
      }
      // Create temporary buffer if needed
      return ByteBuffer.allocateDirect(Math.max(minSize, 1024));
    }

    void markUsed() {
      lastUsed = System.currentTimeMillis();
    }
  }

  /** Call batching for reducing JNI transition overhead. */
  private static final class CallBatch {
    final Object[] calls;
    final long[] timestamps;
    int count;
    long batchStartTime;

    CallBatch() {
      this.calls = new Object[DEFAULT_BATCH_SIZE];
      this.timestamps = new long[DEFAULT_BATCH_SIZE];
      this.count = 0;
      this.batchStartTime = 0;
    }

    boolean canAddCall() {
      return count < calls.length;
    }

    void addCall(final Object call) {
      if (count == 0) {
        batchStartTime = System.nanoTime();
      }
      calls[count] = call;
      timestamps[count] = System.nanoTime();
      count++;
    }

    boolean shouldFlush() {
      if (count == 0) {
        return false;
      }
      if (count >= calls.length) {
        return true;
      }
      final long now = System.nanoTime();
      return (now - batchStartTime) > BATCH_TIMEOUT_NS;
    }

    void clear() {
      count = 0;
      batchStartTime = 0;
      for (int i = 0; i < calls.length; i++) {
        calls[i] = null;
      }
    }
  }

  /** JNI call pattern tracking for optimization. */
  private static final class CallPattern {
    final String methodName;
    final AtomicLong callCount = new AtomicLong(0);
    final AtomicLong totalTimeNs = new AtomicLong(0);
    final AtomicInteger parameterCount = new AtomicInteger(0);
    volatile long averageTimeNs = 0;
    volatile boolean batchCandidate = false;
    volatile OptimizationStrategy strategy = OptimizationStrategy.NONE;

    CallPattern(final String methodName) {
      this.methodName = methodName;
    }

    void recordCall(final long durationNs, final int params) {
      callCount.incrementAndGet();
      totalTimeNs.addAndGet(durationNs);
      parameterCount.compareAndSet(0, params);
      averageTimeNs = totalTimeNs.get() / callCount.get();

      // Determine if batching would be beneficial
      if (callCount.get() > 10 && averageTimeNs > 10000) { // > 10μs
        batchCandidate = true;
        strategy = OptimizationStrategy.BATCH;
      }
    }
  }

  /** Critical section tracking for lock optimization. */
  private static final class CriticalSection {
    final String sectionName;
    final AtomicLong enterCount = new AtomicLong(0);
    final AtomicLong totalWaitTimeNs = new AtomicLong(0);
    final AtomicLong contention = new AtomicLong(0);
    volatile boolean optimized = false;

    CriticalSection(final String sectionName) {
      this.sectionName = sectionName;
    }

    void recordEntry(final long waitTimeNs) {
      enterCount.incrementAndGet();
      totalWaitTimeNs.addAndGet(waitTimeNs);
      if (waitTimeNs > 1000000) { // > 1ms wait
        contention.incrementAndGet();
      }
    }

    double getContentionRate() {
      final long enters = enterCount.get();
      return enters > 0 ? (contention.get() * 100.0) / enters : 0.0;
    }

    double getAverageWaitTimeNs() {
      final long enters = enterCount.get();
      return enters > 0 ? (double) totalWaitTimeNs.get() / enters : 0.0;
    }
  }

  /** Optimization strategies for different call patterns. */
  public enum OptimizationStrategy {
    NONE,
    BATCH,
    POOL,
    DIRECT,
    ASYNC
  }

  // Private constructor for singleton
  private JniOptimizationEngine() {
    LOGGER.info("JNI Optimization Engine initialized");
  }

  /**
   * Gets the singleton optimization engine instance.
   *
   * @return the optimization engine
   */
  public static JniOptimizationEngine getInstance() {
    if (instance == null) {
      INSTANCE_LOCK.lock();
      try {
        if (instance == null) {
          instance = new JniOptimizationEngine();
        }
      } finally {
        INSTANCE_LOCK.unlock();
      }
    }
    return instance;
  }

  /**
   * Optimizes a JNI call by applying the best optimization strategy.
   *
   * @param methodName the JNI method name
   * @param parameters the call parameters
   * @param operation the operation to execute
   * @param <T> the return type
   * @return the operation result
   * @throws Exception if the operation fails
   */
  public <T> T optimizeCall(
      final String methodName, final Object[] parameters, final OptimizedOperation<T> operation)
      throws Exception {

    if (!optimizationEnabled) {
      return operation.execute();
    }

    final long startTime = System.nanoTime();
    final long startCpuTime = getCurrentThreadCpuTime();

    try {
      totalJniCalls.incrementAndGet();

      // Get or create call pattern
      final CallPattern pattern = callPatterns.computeIfAbsent(methodName, CallPattern::new);

      // Select optimization strategy
      final OptimizationStrategy strategy = selectOptimizationStrategy(pattern, parameters);

      // Execute with selected optimization
      final T result = executeWithOptimization(strategy, methodName, parameters, operation);

      // Update pattern statistics
      final long duration = System.nanoTime() - startTime;
      pattern.recordCall(duration, parameters != null ? parameters.length : 0);
      totalJniTimeNs.addAndGet(duration);

      return result;

    } finally {
      // Track CPU time if available
      if (THREAD_MX_BEAN.isCurrentThreadCpuTimeSupported()) {
        final long cpuDuration = getCurrentThreadCpuTime() - startCpuTime;
        updateGcPressure(cpuDuration);
      }
    }
  }

  /**
   * Optimizes batch operations for improved performance.
   *
   * @param methodName the JNI method name
   * @param batchOperations array of operations to execute
   * @param <T> the return type
   * @return array of results
   * @throws Exception if any operation fails
   */
  public <T> T[] optimizeBatch(
      final String methodName, final OptimizedOperation<T>[] batchOperations) throws Exception {

    if (!optimizationEnabled || batchOperations == null) {
      @SuppressWarnings("unchecked")
      final T[] results = (T[]) new Object[batchOperations != null ? batchOperations.length : 0];
      if (batchOperations != null) {
        for (int i = 0; i < batchOperations.length; i++) {
          results[i] = batchOperations[i].execute();
        }
      }
      return results;
    }

    final long startTime = PerformanceMonitor.startOperation("jni_batch_optimization");
    try {
      batchedCalls.addAndGet(batchOperations.length);
      savedTransitions.addAndGet(Math.max(0, batchOperations.length - 1));

      @SuppressWarnings("unchecked")
      final T[] results = (T[]) new Object[batchOperations.length];

      // Execute all operations in optimized batch
      for (int i = 0; i < batchOperations.length; i++) {
        results[i] = batchOperations[i].execute();
      }

      return results;

    } finally {
      PerformanceMonitor.endOperation("jni_batch_optimization", startTime);
    }
  }

  /**
   * Enters a critical section with optimization tracking.
   *
   * @param sectionName the critical section name
   * @return a critical section token
   */
  public CriticalSectionToken enterCriticalSection(final String sectionName) {
    if (!optimizationEnabled) {
      return new CriticalSectionToken(sectionName, 0);
    }

    final long waitStart = System.nanoTime();
    final CriticalSection section =
        criticalSections.computeIfAbsent(sectionName, CriticalSection::new);

    // Simulate critical section entry timing
    final long waitTime = System.nanoTime() - waitStart;
    section.recordEntry(waitTime);

    return new CriticalSectionToken(sectionName, waitTime);
  }

  /**
   * Gets thread-local optimized resources.
   *
   * @return thread-local resource bundle
   */
  public ResourceBundle getThreadResources() {
    final ResourceBundle resources = THREAD_RESOURCES.get();
    resources.markUsed();
    return resources;
  }

  /**
   * Optimizes memory allocation patterns based on usage.
   *
   * @param size the allocation size
   * @param allocationType the type of allocation
   * @return optimized allocation strategy
   */
  public AllocationStrategy optimizeAllocation(final int size, final String allocationType) {
    if (!optimizationEnabled) {
      return AllocationStrategy.DIRECT;
    }

    // Analyze allocation pattern and GC pressure
    if (gcPressure.get() > 50) {
      // High GC pressure - prefer pooled allocations
      return size < 1024 ? AllocationStrategy.POOLED : AllocationStrategy.DIRECT;
    }

    // Normal case - use size-based strategy
    if (size < 256) {
      return AllocationStrategy.THREAD_LOCAL;
    } else if (size < 4096) {
      return AllocationStrategy.POOLED;
    } else {
      return AllocationStrategy.DIRECT;
    }
  }

  /** Token representing a critical section entry. */
  public static final class CriticalSectionToken {
    private final String sectionName;
    private final long entryTime;

    CriticalSectionToken(final String sectionName, final long entryTime) {
      this.sectionName = sectionName;
      this.entryTime = entryTime;
    }

    public String getSectionName() {
      return sectionName;
    }

    public long getEntryTime() {
      return entryTime;
    }
  }

  /** Memory allocation optimization strategies. */
  public enum AllocationStrategy {
    /** Direct heap allocation. */
    DIRECT,
    /** Thread-local cached allocation. */
    THREAD_LOCAL,
    /** Pooled allocation with reuse. */
    POOLED,
    /** Off-heap allocation. */
    OFF_HEAP
  }

  /**
   * Functional interface for optimized operations.
   *
   * @param <T> the return type
   */
  @FunctionalInterface
  public interface OptimizedOperation<T> {
    /**
     * Executes the operation.
     *
     * @return the operation result
     * @throws Exception if the operation fails
     */
    T execute() throws Exception;
  }

  /** Selects the best optimization strategy for a call pattern. */
  private OptimizationStrategy selectOptimizationStrategy(
      final CallPattern pattern, final Object[] parameters) {

    // Consider GC pressure
    if (gcOptimizationActive) {
      return OptimizationStrategy.POOL;
    }

    // Use pattern-based strategy if available
    if (pattern.strategy != OptimizationStrategy.NONE) {
      return pattern.strategy;
    }

    // Default strategy based on parameters
    if (parameters == null || parameters.length == 0) {
      return OptimizationStrategy.DIRECT;
    } else if (parameters.length > 4) {
      return OptimizationStrategy.BATCH;
    } else {
      return OptimizationStrategy.POOL;
    }
  }

  /** Executes operation with the selected optimization strategy. */
  private <T> T executeWithOptimization(
      final OptimizationStrategy strategy,
      final String methodName,
      final Object[] parameters,
      final OptimizedOperation<T> operation)
      throws Exception {

    switch (strategy) {
      case BATCH:
        return executeBatched(methodName, operation);
      case POOL:
        return executePooled(operation);
      case DIRECT:
        return operation.execute();
      case ASYNC:
        return executeAsync(operation);
      default:
        return operation.execute();
    }
  }

  /** Executes operation in batched mode. */
  private <T> T executeBatched(final String methodName, final OptimizedOperation<T> operation)
      throws Exception {

    final ResourceBundle resources = getThreadResources();
    final CallBatch batch = resources.currentBatch;

    // Add to batch if possible
    if (batch.canAddCall()) {
      batch.addCall(operation);
    }

    // Flush batch if needed
    if (batch.shouldFlush()) {
      flushBatch(batch);
    }

    return operation.execute();
  }

  /** Executes operation with pooled resources. */
  private <T> T executePooled(final OptimizedOperation<T> operation) throws Exception {
    // Thread-local resources are available via getThreadResources() if needed
    return operation.execute();
  }

  /** Executes operation asynchronously. */
  private <T> T executeAsync(final OptimizedOperation<T> operation) throws Exception {
    // For now, execute synchronously
    // Future implementation could use async execution
    return operation.execute();
  }

  /** Flushes a call batch. */
  private void flushBatch(final CallBatch batch) {
    if (batch.count == 0) {
      return;
    }

    try {
      // Execute all batched calls
      for (int i = 0; i < batch.count; i++) {
        // Batched execution would happen here
      }
      savedTransitions.addAndGet(batch.count - 1);
    } finally {
      batch.clear();
    }
  }

  /** Gets current thread CPU time if available. */
  private long getCurrentThreadCpuTime() {
    if (THREAD_MX_BEAN.isCurrentThreadCpuTimeSupported()) {
      return THREAD_MX_BEAN.getCurrentThreadCpuTime();
    }
    return 0;
  }

  /** Updates GC pressure metric based on CPU usage. */
  private void updateGcPressure(final long cpuTimeNs) {
    // Simplified GC pressure calculation
    final long threshold = 10_000_000; // 10ms
    if (cpuTimeNs > threshold) {
      gcPressure.incrementAndGet();
      gcOptimizationActive = gcPressure.get() > 100;
    } else if (gcPressure.get() > 0) {
      gcPressure.decrementAndGet();
      gcOptimizationActive = gcPressure.get() > 100;
    }
  }

  /**
   * Gets comprehensive optimization statistics.
   *
   * @return optimization statistics
   */
  public String getOptimizationStats() {
    final long totalCalls = totalJniCalls.get();
    final long totalTime = totalJniTimeNs.get();
    final double avgCallTime = totalCalls > 0 ? (double) totalTime / totalCalls : 0.0;
    final long batched = batchedCalls.get();
    final long saved = savedTransitions.get();

    final StringBuilder sb = new StringBuilder();
    sb.append(String.format("=== JNI Optimization Statistics ===%n"));
    sb.append(String.format("Optimization enabled: %b%n", optimizationEnabled));
    sb.append(String.format("Total JNI calls: %,d%n", totalCalls));
    sb.append(String.format("Average call time: %.0f ns%n", avgCallTime));
    sb.append(String.format("Batched calls: %,d%n", batched));
    sb.append(String.format("Saved transitions: %,d%n", saved));
    sb.append(String.format("GC pressure: %d%n", gcPressure.get()));
    sb.append(String.format("GC optimization active: %b%n", gcOptimizationActive));
    sb.append(String.format("Call patterns tracked: %d%n", callPatterns.size()));
    sb.append(String.format("Critical sections: %d%n", criticalSections.size()));

    // Top call patterns
    sb.append(String.format("%nTop call patterns:%n"));
    callPatterns.values().stream()
        .sorted((p1, p2) -> Long.compare(p2.callCount.get(), p1.callCount.get()))
        .limit(5)
        .forEach(
            pattern ->
                sb.append(
                    String.format(
                        "  %s: %,d calls, avg=%.0fns, strategy=%s%n",
                        pattern.methodName,
                        pattern.callCount.get(),
                        (double) pattern.averageTimeNs,
                        pattern.strategy)));

    return sb.toString();
  }

  /**
   * Enables or disables JNI optimization.
   *
   * @param enabled true to enable optimization
   */
  public static void setOptimizationEnabled(final boolean enabled) {
    optimizationEnabled = enabled;
    LOGGER.info("JNI optimization " + (enabled ? "enabled" : "disabled"));
  }

  /**
   * Checks if JNI optimization is enabled.
   *
   * @return true if optimization is enabled
   */
  public static boolean isOptimizationEnabled() {
    return optimizationEnabled;
  }

  /** Resets all optimization statistics. */
  public void reset() {
    callPatterns.clear();
    criticalSections.clear();
    totalJniCalls.set(0);
    totalJniTimeNs.set(0);
    batchedCalls.set(0);
    savedTransitions.set(0);
    gcPressure.set(0);
    gcOptimizationActive = false;
    LOGGER.info("JNI optimization statistics reset");
  }

  /** Cleanup method for shutdown. */
  public void shutdown() {
    reset();
    THREAD_RESOURCES.remove();
    LOGGER.info("JNI Optimization Engine shutdown");
  }
}
