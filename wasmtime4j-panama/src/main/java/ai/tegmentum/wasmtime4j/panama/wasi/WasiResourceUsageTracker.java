package ai.tegmentum.wasmtime4j.panama.wasi;

import ai.tegmentum.wasmtime4j.panama.wasi.permission.WasiResourceLimits;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;

/**
 * Comprehensive resource usage tracking and statistics collection for WASI operations using Panama
 * FFI.
 *
 * <p>This class provides detailed tracking of resource consumption across WASI contexts with
 * real-time monitoring and limit enforcement. It tracks:
 *
 * <ul>
 *   <li>Memory usage and allocation patterns
 *   <li>File system operations and I/O statistics
 *   <li>Network operations and connection counts
 *   <li>CPU time and execution duration
 *   <li>Resource limit enforcement and violations
 *   <li>Performance metrics and trends
 * </ul>
 *
 * <p>The tracker is designed to be thread-safe and low-overhead to minimize impact on WASI
 * operation performance while providing comprehensive monitoring capabilities for Panama-based
 * implementations.
 *
 * @since 1.0.0
 */
public final class WasiResourceUsageTracker {

  private static final Logger LOGGER = Logger.getLogger(WasiResourceUsageTracker.class.getName());

  /** Context-specific resource usage tracking. */
  private final Map<String, ContextResourceUsage> contextUsage = new ConcurrentHashMap<>();

  /** Global resource limits configuration. */
  private final WasiResourceLimits resourceLimits;

  /** Whether to enable detailed tracking (may impact performance). */
  private final boolean detailedTrackingEnabled;

  /** Global statistics across all contexts. */
  private final GlobalStatistics globalStats = new GlobalStatistics();

  /**
   * Creates a new resource usage tracker with the specified limits.
   *
   * @param resourceLimits the resource limits to enforce
   * @param detailedTrackingEnabled whether to enable detailed tracking
   */
  public WasiResourceUsageTracker(
      final WasiResourceLimits resourceLimits, final boolean detailedTrackingEnabled) {
    if (resourceLimits == null) {
      throw new IllegalArgumentException("Resource limits cannot be null");
    }

    this.resourceLimits = resourceLimits;
    this.detailedTrackingEnabled = detailedTrackingEnabled;

    LOGGER.info(
        String.format(
            "Created Panama WASI resource usage tracker with detailed tracking: %s",
            detailedTrackingEnabled));
  }

  /**
   * Creates a new resource usage tracker with default settings.
   *
   * @param resourceLimits the resource limits to enforce
   */
  public WasiResourceUsageTracker(final WasiResourceLimits resourceLimits) {
    this(resourceLimits, false);
  }

  /**
   * Registers a new WASI context for resource tracking.
   *
   * @param contextId the unique context identifier
   */
  public void registerContext(final String contextId) {
    if (contextId == null || contextId.isEmpty()) {
      throw new IllegalArgumentException("Context ID cannot be null or empty");
    }

    final ContextResourceUsage usage = new ContextResourceUsage(contextId, Instant.now());
    contextUsage.put(contextId, usage);

    globalStats.contextsRegistered.increment();

    LOGGER.fine(String.format("Registered Panama WASI context for tracking: %s", contextId));
  }

  /**
   * Unregisters a WASI context from resource tracking.
   *
   * @param contextId the unique context identifier
   */
  public void unregisterContext(final String contextId) {
    if (contextId == null || contextId.isEmpty()) {
      throw new IllegalArgumentException("Context ID cannot be null or empty");
    }

    final ContextResourceUsage usage = contextUsage.remove(contextId);
    if (usage != null) {
      globalStats.contextsUnregistered.increment();
      LOGGER.fine(String.format("Unregistered Panama WASI context from tracking: %s", contextId));
    }
  }

  /**
   * Records memory allocation for a context.
   *
   * @param contextId the context identifier
   * @param bytes the number of bytes allocated
   * @throws IllegalStateException if memory limit would be exceeded
   */
  public void recordMemoryAllocation(final String contextId, final long bytes) {
    if (contextId == null || contextId.isEmpty()) {
      throw new IllegalArgumentException("Context ID cannot be null or empty");
    }
    if (bytes < 0) {
      throw new IllegalArgumentException("Bytes cannot be negative");
    }

    final ContextResourceUsage usage = getContextUsage(contextId);

    // Check memory limits before allocation
    usage.memoryUsed.addAndGet(bytes);
    final long newMemoryUsage = usage.memoryUsed.get();
    if (resourceLimits.isMemoryLimited() && newMemoryUsage > resourceLimits.getMaxMemoryBytes()) {
      usage.memoryUsed.addAndGet(-bytes); // Rollback
      globalStats.memoryLimitViolations.increment();
      throw new IllegalStateException(
          String.format(
              "Memory limit exceeded for Panama context %s: %d bytes (limit: %d)",
              contextId, newMemoryUsage, resourceLimits.getMaxMemoryBytes()));
    }

    usage.totalMemoryAllocated.add(bytes);
    globalStats.totalMemoryAllocated.add(bytes);

    if (detailedTrackingEnabled) {
      usage.memoryAllocations.increment();
      globalStats.totalMemoryAllocations.increment();
    }

    LOGGER.finest(
        String.format("Recorded Panama memory allocation: %s, %d bytes", contextId, bytes));
  }

  /**
   * Records memory deallocation for a context.
   *
   * @param contextId the context identifier
   * @param bytes the number of bytes deallocated
   */
  public void recordMemoryDeallocation(final String contextId, final long bytes) {
    if (contextId == null || contextId.isEmpty()) {
      throw new IllegalArgumentException("Context ID cannot be null or empty");
    }
    if (bytes < 0) {
      throw new IllegalArgumentException("Bytes cannot be negative");
    }

    final ContextResourceUsage usage = getContextUsage(contextId);

    usage.memoryUsed.addAndGet(-bytes);
    usage.totalMemoryDeallocated.add(bytes);
    globalStats.totalMemoryDeallocated.add(bytes);

    if (detailedTrackingEnabled) {
      usage.memoryDeallocations.increment();
      globalStats.totalMemoryDeallocations.increment();
    }

    LOGGER.finest(
        String.format("Recorded Panama memory deallocation: %s, %d bytes", contextId, bytes));
  }

  /**
   * Records a file system operation for a context.
   *
   * @param contextId the context identifier
   * @param operation the file operation type
   * @param bytes the number of bytes involved (for read/write operations)
   * @param durationNanos the operation duration in nanoseconds
   */
  public void recordFileSystemOperation(
      final String contextId,
      final WasiFileOperation operation,
      final long bytes,
      final long durationNanos) {

    if (contextId == null || contextId.isEmpty()) {
      throw new IllegalArgumentException("Context ID cannot be null or empty");
    }
    if (operation == null) {
      throw new IllegalArgumentException("Operation cannot be null");
    }
    if (bytes < 0) {
      throw new IllegalArgumentException("Bytes cannot be negative");
    }
    if (durationNanos < 0) {
      throw new IllegalArgumentException("Duration cannot be negative");
    }

    final ContextResourceUsage usage = getContextUsage(contextId);

    // Check operation-specific limits
    checkFileSystemOperationLimits(usage, operation, bytes);

    // Record operation statistics
    switch (operation) {
      case READ:
        usage.fileReadOperations.increment();
        usage.totalBytesRead.add(bytes);
        globalStats.totalFileReadOperations.increment();
        globalStats.totalBytesRead.add(bytes);
        break;
      case WRITE:
        usage.fileWriteOperations.increment();
        usage.totalBytesWritten.add(bytes);
        globalStats.totalFileWriteOperations.increment();
        globalStats.totalBytesWritten.add(bytes);
        break;
      case OPEN:
        usage.fileOpenOperations.increment();
        globalStats.totalFileOpenOperations.increment();
        break;
      case CLOSE:
        usage.fileCloseOperations.increment();
        globalStats.totalFileCloseOperations.increment();
        break;
      default:
        usage.otherFileOperations.increment();
        globalStats.totalOtherFileOperations.increment();
        break;
    }

    if (detailedTrackingEnabled) {
      usage.totalFileSystemDuration.add(durationNanos);
      globalStats.totalFileSystemDuration.add(durationNanos);
    }

    LOGGER.finest(
        String.format(
            "Recorded Panama file system operation: %s, %s, %d bytes, %d ns",
            contextId, operation, bytes, durationNanos));
  }

  /**
   * Records CPU time usage for a context.
   *
   * @param contextId the context identifier
   * @param cpuTimeNanos the CPU time in nanoseconds
   */
  public void recordCpuTime(final String contextId, final long cpuTimeNanos) {
    if (contextId == null || contextId.isEmpty()) {
      throw new IllegalArgumentException("Context ID cannot be null or empty");
    }
    if (cpuTimeNanos < 0) {
      throw new IllegalArgumentException("CPU time cannot be negative");
    }

    final ContextResourceUsage usage = getContextUsage(contextId);

    // Check CPU time limits
    usage.totalCpuTime.add(cpuTimeNanos);
    final long newCpuTime = usage.totalCpuTime.sum();
    if (resourceLimits.getMaxCpuTime() != null) {
      final long maxCpuTimeNanos = resourceLimits.getMaxCpuTime().toNanos();
      if (newCpuTime > maxCpuTimeNanos) {
        globalStats.cpuLimitViolations.increment();
        throw new IllegalStateException(
            String.format(
                "CPU time limit exceeded for Panama context %s: %d ns (limit: %d ns)",
                contextId, newCpuTime, maxCpuTimeNanos));
      }
    }

    globalStats.totalCpuTime.add(cpuTimeNanos);

    LOGGER.finest(String.format("Recorded Panama CPU time: %s, %d ns", contextId, cpuTimeNanos));
  }

  /**
   * Records execution time for a context.
   *
   * @param contextId the context identifier
   * @param executionTimeNanos the execution time in nanoseconds
   */
  public void recordExecutionTime(final String contextId, final long executionTimeNanos) {
    if (contextId == null || contextId.isEmpty()) {
      throw new IllegalArgumentException("Context ID cannot be null or empty");
    }
    if (executionTimeNanos < 0) {
      throw new IllegalArgumentException("Execution time cannot be negative");
    }

    final ContextResourceUsage usage = getContextUsage(contextId);

    usage.totalExecutionTime.add(executionTimeNanos);
    globalStats.totalExecutionTime.add(executionTimeNanos);

    LOGGER.finest(
        String.format("Recorded Panama execution time: %s, %d ns", contextId, executionTimeNanos));
  }

  /**
   * Gets resource usage statistics for a specific context.
   *
   * @param contextId the context identifier
   * @return the context resource usage statistics
   */
  public ContextResourceUsageSnapshot getContextUsageSnapshot(final String contextId) {
    if (contextId == null || contextId.isEmpty()) {
      throw new IllegalArgumentException("Context ID cannot be null or empty");
    }

    final ContextResourceUsage usage = contextUsage.get(contextId);
    if (usage == null) {
      throw new IllegalArgumentException("Unknown context ID: " + contextId);
    }

    return new ContextResourceUsageSnapshot(usage);
  }

  /**
   * Gets global resource usage statistics across all contexts.
   *
   * @return the global resource usage statistics
   */
  public GlobalResourceUsageSnapshot getGlobalUsage() {
    return new GlobalResourceUsageSnapshot(globalStats);
  }

  /**
   * Gets the resource limits configuration.
   *
   * @return the resource limits
   */
  public WasiResourceLimits getResourceLimits() {
    return resourceLimits;
  }

  /**
   * Checks if detailed tracking is enabled.
   *
   * @return true if detailed tracking is enabled, false otherwise
   */
  public boolean isDetailedTrackingEnabled() {
    return detailedTrackingEnabled;
  }

  /**
   * Gets the current number of tracked contexts.
   *
   * @return the number of tracked contexts
   */
  public int getTrackedContextCount() {
    return contextUsage.size();
  }

  /** Gets context usage, throwing if not found. */
  private ContextResourceUsage getContextUsage(final String contextId) {
    final ContextResourceUsage usage = contextUsage.get(contextId);
    if (usage == null) {
      throw new IllegalArgumentException("Unknown context ID: " + contextId);
    }
    return usage;
  }

  /** Checks file system operation limits. */
  private void checkFileSystemOperationLimits(
      final ContextResourceUsage usage, final WasiFileOperation operation, final long bytes) {

    // Check rate limits for disk operations
    if (resourceLimits.isDiskIoLimited()) {
      // Note: In a real implementation, this would use a rate limiter
      // For now, we just record violations if they occur too frequently
      final long currentTime = System.currentTimeMillis();
      final long timeDiff = currentTime - usage.lastOperationTime.get();

      if (timeDiff < 1000) { // Within 1 second
        switch (operation) {
          case READ:
            if (usage.recentReadOperations.incrementAndGet()
                > resourceLimits.getMaxDiskReadsPerSecond()) {
              globalStats.diskReadLimitViolations.increment();
              throw new IllegalStateException(
                  String.format(
                      "Disk read rate limit exceeded for Panama context %s", usage.contextId));
            }
            break;
          case WRITE:
            if (usage.recentWriteOperations.incrementAndGet()
                > resourceLimits.getMaxDiskWritesPerSecond()) {
              globalStats.diskWriteLimitViolations.increment();
              throw new IllegalStateException(
                  String.format(
                      "Disk write rate limit exceeded for Panama context %s", usage.contextId));
            }
            break;
          default:
            // No specific limit for other operations
            break;
        }
      } else {
        // Reset counters after 1 second
        usage.recentReadOperations.set(0);
        usage.recentWriteOperations.set(0);
      }

      usage.lastOperationTime.set(currentTime);
    }
  }

  /** Internal context resource usage tracking. */
  private static final class ContextResourceUsage {
    final String contextId;
    final Instant creationTime;

    // Memory tracking
    final AtomicLong memoryUsed = new AtomicLong(0);
    final LongAdder totalMemoryAllocated = new LongAdder();
    final LongAdder totalMemoryDeallocated = new LongAdder();
    final LongAdder memoryAllocations = new LongAdder();
    final LongAdder memoryDeallocations = new LongAdder();

    // File system tracking
    final LongAdder fileReadOperations = new LongAdder();
    final LongAdder fileWriteOperations = new LongAdder();
    final LongAdder fileOpenOperations = new LongAdder();
    final LongAdder fileCloseOperations = new LongAdder();
    final LongAdder otherFileOperations = new LongAdder();
    final LongAdder totalBytesRead = new LongAdder();
    final LongAdder totalBytesWritten = new LongAdder();
    final LongAdder totalFileSystemDuration = new LongAdder();

    // Rate limiting tracking
    final AtomicLong recentReadOperations = new AtomicLong(0);
    final AtomicLong recentWriteOperations = new AtomicLong(0);
    final AtomicLong lastOperationTime = new AtomicLong(System.currentTimeMillis());

    // Execution tracking
    final LongAdder totalCpuTime = new LongAdder();
    final LongAdder totalExecutionTime = new LongAdder();

    ContextResourceUsage(final String contextId, final Instant creationTime) {
      this.contextId = contextId;
      this.creationTime = creationTime;
    }
  }

  /** Global statistics across all contexts. */
  private static final class GlobalStatistics {
    final LongAdder contextsRegistered = new LongAdder();
    final LongAdder contextsUnregistered = new LongAdder();

    final LongAdder totalMemoryAllocated = new LongAdder();
    final LongAdder totalMemoryDeallocated = new LongAdder();
    final LongAdder totalMemoryAllocations = new LongAdder();
    final LongAdder totalMemoryDeallocations = new LongAdder();

    final LongAdder totalFileReadOperations = new LongAdder();
    final LongAdder totalFileWriteOperations = new LongAdder();
    final LongAdder totalFileOpenOperations = new LongAdder();
    final LongAdder totalFileCloseOperations = new LongAdder();
    final LongAdder totalOtherFileOperations = new LongAdder();
    final LongAdder totalBytesRead = new LongAdder();
    final LongAdder totalBytesWritten = new LongAdder();
    final LongAdder totalFileSystemDuration = new LongAdder();

    final LongAdder totalCpuTime = new LongAdder();
    final LongAdder totalExecutionTime = new LongAdder();

    final LongAdder memoryLimitViolations = new LongAdder();
    final LongAdder cpuLimitViolations = new LongAdder();
    final LongAdder diskReadLimitViolations = new LongAdder();
    final LongAdder diskWriteLimitViolations = new LongAdder();
  }

  /** Immutable snapshot of context resource usage. */
  public static final class ContextResourceUsageSnapshot {
    private final String contextId;
    private final Instant creationTime;
    private final long memoryUsed;
    private final long totalMemoryAllocated;
    private final long totalMemoryDeallocated;
    private final long memoryAllocations;
    private final long memoryDeallocations;
    private final long fileReadOperations;
    private final long fileWriteOperations;
    private final long fileOpenOperations;
    private final long fileCloseOperations;
    private final long otherFileOperations;
    private final long totalBytesRead;
    private final long totalBytesWritten;
    private final long totalFileSystemDuration;
    private final long totalCpuTime;
    private final long totalExecutionTime;

    private ContextResourceUsageSnapshot(final ContextResourceUsage usage) {
      this.contextId = usage.contextId;
      this.creationTime = usage.creationTime;
      this.memoryUsed = usage.memoryUsed.get();
      this.totalMemoryAllocated = usage.totalMemoryAllocated.sum();
      this.totalMemoryDeallocated = usage.totalMemoryDeallocated.sum();
      this.memoryAllocations = usage.memoryAllocations.sum();
      this.memoryDeallocations = usage.memoryDeallocations.sum();
      this.fileReadOperations = usage.fileReadOperations.sum();
      this.fileWriteOperations = usage.fileWriteOperations.sum();
      this.fileOpenOperations = usage.fileOpenOperations.sum();
      this.fileCloseOperations = usage.fileCloseOperations.sum();
      this.otherFileOperations = usage.otherFileOperations.sum();
      this.totalBytesRead = usage.totalBytesRead.sum();
      this.totalBytesWritten = usage.totalBytesWritten.sum();
      this.totalFileSystemDuration = usage.totalFileSystemDuration.sum();
      this.totalCpuTime = usage.totalCpuTime.sum();
      this.totalExecutionTime = usage.totalExecutionTime.sum();
    }

    public String getContextId() {
      return contextId;
    }

    public Instant getCreationTime() {
      return creationTime;
    }

    public Duration getUptime() {
      return Duration.between(creationTime, Instant.now());
    }

    public long getMemoryUsed() {
      return memoryUsed;
    }

    public long getTotalMemoryAllocated() {
      return totalMemoryAllocated;
    }

    public long getTotalMemoryDeallocated() {
      return totalMemoryDeallocated;
    }

    public long getMemoryAllocations() {
      return memoryAllocations;
    }

    public long getMemoryDeallocations() {
      return memoryDeallocations;
    }

    public long getFileReadOperations() {
      return fileReadOperations;
    }

    public long getFileWriteOperations() {
      return fileWriteOperations;
    }

    public long getFileOpenOperations() {
      return fileOpenOperations;
    }

    public long getFileCloseOperations() {
      return fileCloseOperations;
    }

    public long getOtherFileOperations() {
      return otherFileOperations;
    }

    /**
     * Gets the total number of file operations.
     *
     * @return the total file operations count
     */
    public long getTotalFileOperations() {
      return fileReadOperations
          + fileWriteOperations
          + fileOpenOperations
          + fileCloseOperations
          + otherFileOperations;
    }

    public long getTotalBytesRead() {
      return totalBytesRead;
    }

    public long getTotalBytesWritten() {
      return totalBytesWritten;
    }

    public long getTotalFileSystemDuration() {
      return totalFileSystemDuration;
    }

    public long getTotalCpuTime() {
      return totalCpuTime;
    }

    public long getTotalExecutionTime() {
      return totalExecutionTime;
    }

    @Override
    public String toString() {
      return String.format(
          "PanamaContextUsage{id=%s, uptime=%s, memory=%d, fileOps=%d, ioBytes=%d/%d}",
          contextId,
          getUptime(),
          memoryUsed,
          getTotalFileOperations(),
          totalBytesRead,
          totalBytesWritten);
    }
  }

  /** Immutable snapshot of global resource usage. */
  public static final class GlobalResourceUsageSnapshot {
    private final long contextsRegistered;
    private final long contextsUnregistered;
    private final long totalMemoryAllocated;
    private final long totalMemoryDeallocated;
    private final long totalMemoryAllocations;
    private final long totalMemoryDeallocations;
    private final long totalFileReadOperations;
    private final long totalFileWriteOperations;
    private final long totalFileOpenOperations;
    private final long totalFileCloseOperations;
    private final long totalOtherFileOperations;
    private final long totalBytesRead;
    private final long totalBytesWritten;
    private final long totalFileSystemDuration;
    private final long totalCpuTime;
    private final long totalExecutionTime;
    private final long memoryLimitViolations;
    private final long cpuLimitViolations;
    private final long diskReadLimitViolations;
    private final long diskWriteLimitViolations;

    private GlobalResourceUsageSnapshot(final GlobalStatistics stats) {
      this.contextsRegistered = stats.contextsRegistered.sum();
      this.contextsUnregistered = stats.contextsUnregistered.sum();
      this.totalMemoryAllocated = stats.totalMemoryAllocated.sum();
      this.totalMemoryDeallocated = stats.totalMemoryDeallocated.sum();
      this.totalMemoryAllocations = stats.totalMemoryAllocations.sum();
      this.totalMemoryDeallocations = stats.totalMemoryDeallocations.sum();
      this.totalFileReadOperations = stats.totalFileReadOperations.sum();
      this.totalFileWriteOperations = stats.totalFileWriteOperations.sum();
      this.totalFileOpenOperations = stats.totalFileOpenOperations.sum();
      this.totalFileCloseOperations = stats.totalFileCloseOperations.sum();
      this.totalOtherFileOperations = stats.totalOtherFileOperations.sum();
      this.totalBytesRead = stats.totalBytesRead.sum();
      this.totalBytesWritten = stats.totalBytesWritten.sum();
      this.totalFileSystemDuration = stats.totalFileSystemDuration.sum();
      this.totalCpuTime = stats.totalCpuTime.sum();
      this.totalExecutionTime = stats.totalExecutionTime.sum();
      this.memoryLimitViolations = stats.memoryLimitViolations.sum();
      this.cpuLimitViolations = stats.cpuLimitViolations.sum();
      this.diskReadLimitViolations = stats.diskReadLimitViolations.sum();
      this.diskWriteLimitViolations = stats.diskWriteLimitViolations.sum();
    }

    public long getContextsRegistered() {
      return contextsRegistered;
    }

    public long getContextsUnregistered() {
      return contextsUnregistered;
    }

    public long getActiveContexts() {
      return contextsRegistered - contextsUnregistered;
    }

    public long getTotalMemoryAllocated() {
      return totalMemoryAllocated;
    }

    public long getTotalMemoryDeallocated() {
      return totalMemoryDeallocated;
    }

    public long getCurrentMemoryUsage() {
      return totalMemoryAllocated - totalMemoryDeallocated;
    }

    public long getTotalMemoryAllocations() {
      return totalMemoryAllocations;
    }

    public long getTotalMemoryDeallocations() {
      return totalMemoryDeallocations;
    }

    /**
     * Gets the total number of file operations across all contexts.
     *
     * @return the total file operations count
     */
    public long getTotalFileOperations() {
      return totalFileReadOperations
          + totalFileWriteOperations
          + totalFileOpenOperations
          + totalFileCloseOperations
          + totalOtherFileOperations;
    }

    public long getTotalFileReadOperations() {
      return totalFileReadOperations;
    }

    public long getTotalFileWriteOperations() {
      return totalFileWriteOperations;
    }

    public long getTotalFileOpenOperations() {
      return totalFileOpenOperations;
    }

    public long getTotalFileCloseOperations() {
      return totalFileCloseOperations;
    }

    public long getTotalOtherFileOperations() {
      return totalOtherFileOperations;
    }

    public long getTotalBytesRead() {
      return totalBytesRead;
    }

    public long getTotalBytesWritten() {
      return totalBytesWritten;
    }

    public long getTotalFileSystemDuration() {
      return totalFileSystemDuration;
    }

    public long getTotalCpuTime() {
      return totalCpuTime;
    }

    public long getTotalExecutionTime() {
      return totalExecutionTime;
    }

    public long getMemoryLimitViolations() {
      return memoryLimitViolations;
    }

    public long getCpuLimitViolations() {
      return cpuLimitViolations;
    }

    public long getDiskReadLimitViolations() {
      return diskReadLimitViolations;
    }

    public long getDiskWriteLimitViolations() {
      return diskWriteLimitViolations;
    }

    /**
     * Gets the total number of limit violations.
     *
     * @return the total limit violations count
     */
    public long getTotalLimitViolations() {
      return memoryLimitViolations
          + cpuLimitViolations
          + diskReadLimitViolations
          + diskWriteLimitViolations;
    }

    @Override
    public String toString() {
      return String.format(
          "PanamaGlobalUsage{contexts=%d/%d, memory=%d/%d, fileOps=%d, violations=%d}",
          getActiveContexts(),
          contextsRegistered,
          getCurrentMemoryUsage(),
          totalMemoryAllocated,
          getTotalFileOperations(),
          getTotalLimitViolations());
    }
  }
}
