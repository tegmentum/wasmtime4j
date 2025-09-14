package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Comprehensive resource leak detection and monitoring system for WASI contexts and resources.
 *
 * <p>This class provides advanced resource leak detection capabilities with real-time monitoring
 * and alerting. It tracks:
 *
 * <ul>
 *   <li>WASI context lifecycle and resource usage
 *   <li>File handle allocations and cleanup
 *   <li>Memory segment allocations and deallocation
 *   <li>Native resource tracking across both JNI and Panama implementations
 *   <li>Resource usage statistics and trend analysis
 *   <li>Automated leak detection with configurable thresholds
 * </ul>
 *
 * <p>The leak detector uses phantom references and weak references to track resource lifecycle
 * without preventing garbage collection. It provides both proactive monitoring and reactive cleanup
 * to ensure system stability.
 *
 * @since 1.0.0
 */
public final class WasiResourceLeakDetector implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(WasiResourceLeakDetector.class.getName());

  /** Default monitoring interval in seconds. */
  private static final int DEFAULT_MONITORING_INTERVAL_SECONDS = 30;

  /** Default resource leak threshold. */
  private static final int DEFAULT_LEAK_THRESHOLD = 1000;

  /** Default resource age threshold for leak detection in minutes. */
  private static final int DEFAULT_RESOURCE_AGE_THRESHOLD_MINUTES = 30;

  /** Tracked WASI contexts by ID. */
  private final Map<String, TrackedWasiContext> trackedContexts = new ConcurrentHashMap<>();

  /** Tracked file handles by descriptor. */
  private final Map<Integer, TrackedFileHandle> trackedFileHandles = new ConcurrentHashMap<>();

  /** Tracked memory segments by address. */
  private final Map<Long, TrackedMemorySegment> trackedMemorySegments = new ConcurrentHashMap<>();

  /** Phantom references for automatic leak detection. */
  private final Map<PhantomReference<?>, String> phantomReferences = new ConcurrentHashMap<>();

  /** Reference queue for garbage collection notifications. */
  private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();

  /** Scheduled executor for monitoring tasks. */
  private final ScheduledExecutorService monitoringExecutor;

  /** Monitoring task future. */
  private final ScheduledFuture<?> monitoringTask;

  /** Resource leak threshold. */
  private final int leakThreshold;

  /** Resource age threshold for leak detection. */
  private final Duration resourceAgeThreshold;

  /** Whether this detector has been shut down. */
  private volatile boolean shutdown = false;

  /** Resource allocation statistics. */
  private final ResourceStatistics statistics = new ResourceStatistics();

  /** Creates a new resource leak detector with default settings. */
  public WasiResourceLeakDetector() {
    this(
        DEFAULT_LEAK_THRESHOLD,
        Duration.ofMinutes(DEFAULT_RESOURCE_AGE_THRESHOLD_MINUTES),
        DEFAULT_MONITORING_INTERVAL_SECONDS);
  }

  /**
   * Creates a new resource leak detector with the specified settings.
   *
   * @param leakThreshold the resource count threshold for leak detection
   * @param resourceAgeThreshold the age threshold for considering resources as leaked
   * @param monitoringIntervalSeconds the monitoring interval in seconds
   */
  public WasiResourceLeakDetector(
      final int leakThreshold,
      final Duration resourceAgeThreshold,
      final int monitoringIntervalSeconds) {

    JniValidation.requirePositive(leakThreshold, "leakThreshold");
    JniValidation.requireNonNull(resourceAgeThreshold, "resourceAgeThreshold");
    JniValidation.requirePositive(monitoringIntervalSeconds, "monitoringIntervalSeconds");

    this.leakThreshold = leakThreshold;
    this.resourceAgeThreshold = resourceAgeThreshold;

    this.monitoringExecutor =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              final Thread thread = new Thread(r, "WasiResourceLeakDetector-Monitor");
              thread.setDaemon(true);
              return thread;
            });

    // Start monitoring task
    this.monitoringTask =
        monitoringExecutor.scheduleAtFixedRate(
            this::performMonitoring,
            monitoringIntervalSeconds,
            monitoringIntervalSeconds,
            TimeUnit.SECONDS);

    LOGGER.info(
        String.format(
            "Created WASI resource leak detector: threshold=%d, age=%s, interval=%ds",
            leakThreshold, resourceAgeThreshold, monitoringIntervalSeconds));
  }

  /**
   * Tracks a WASI context for resource leak detection.
   *
   * @param contextId the unique context identifier
   * @param context the WASI context to track
   */
  public void trackWasiContext(final String contextId, final WasiContext context) {
    JniValidation.requireNonEmpty(contextId, "contextId");
    JniValidation.requireNonNull(context, "context");

    if (shutdown) {
      return;
    }

    final TrackedWasiContext tracked =
        new TrackedWasiContext(contextId, new WeakReference<>(context), Instant.now());

    trackedContexts.put(contextId, tracked);

    // Create phantom reference for automatic cleanup
    final PhantomReference<WasiContext> phantomRef = new PhantomReference<>(context, referenceQueue);
    phantomReferences.put(phantomRef, "WasiContext:" + contextId);

    statistics.contextsCreated.incrementAndGet();

    LOGGER.fine(String.format("Started tracking WASI context: %s", contextId));
  }

  /**
   * Untracks a WASI context.
   *
   * @param contextId the unique context identifier
   */
  public void untrackWasiContext(final String contextId) {
    JniValidation.requireNonEmpty(contextId, "contextId");

    final TrackedWasiContext tracked = trackedContexts.remove(contextId);
    if (tracked != null) {
      statistics.contextsDestroyed.incrementAndGet();
      LOGGER.fine(String.format("Stopped tracking WASI context: %s", contextId));
    }
  }

  /**
   * Tracks a file handle for resource leak detection.
   *
   * @param fileDescriptor the file descriptor
   * @param handle the file handle to track
   */
  public void trackFileHandle(final int fileDescriptor, final WasiFileHandle handle) {
    JniValidation.requireNonNull(handle, "handle");

    if (shutdown) {
      return;
    }

    final TrackedFileHandle tracked =
        new TrackedFileHandle(fileDescriptor, new WeakReference<>(handle), Instant.now());

    trackedFileHandles.put(fileDescriptor, tracked);

    // Create phantom reference for automatic cleanup
    final PhantomReference<WasiFileHandle> phantomRef =
        new PhantomReference<>(handle, referenceQueue);
    phantomReferences.put(phantomRef, "FileHandle:" + fileDescriptor);

    statistics.fileHandlesCreated.incrementAndGet();

    LOGGER.fine(String.format("Started tracking file handle: %d", fileDescriptor));
  }

  /**
   * Untracks a file handle.
   *
   * @param fileDescriptor the file descriptor
   */
  public void untrackFileHandle(final int fileDescriptor) {
    final TrackedFileHandle tracked = trackedFileHandles.remove(fileDescriptor);
    if (tracked != null) {
      statistics.fileHandlesDestroyed.incrementAndGet();
      LOGGER.fine(String.format("Stopped tracking file handle: %d", fileDescriptor));
    }
  }

  /**
   * Tracks a memory segment for resource leak detection.
   *
   * @param address the memory segment address
   * @param segment the memory segment to track (can be any object representing memory)
   */
  public void trackMemorySegment(final long address, final Object segment) {
    JniValidation.requireNonNull(segment, "segment");

    if (shutdown) {
      return;
    }

    final TrackedMemorySegment tracked =
        new TrackedMemorySegment(address, new WeakReference<>(segment), Instant.now());

    trackedMemorySegments.put(address, tracked);

    // Create phantom reference for automatic cleanup
    final PhantomReference<Object> phantomRef = new PhantomReference<>(segment, referenceQueue);
    phantomReferences.put(phantomRef, "MemorySegment:" + address);

    statistics.memorySegmentsCreated.incrementAndGet();

    LOGGER.fine(String.format("Started tracking memory segment: 0x%x", address));
  }

  /**
   * Untracks a memory segment.
   *
   * @param address the memory segment address
   */
  public void untrackMemorySegment(final long address) {
    final TrackedMemorySegment tracked = trackedMemorySegments.remove(address);
    if (tracked != null) {
      statistics.memorySegmentsDestroyed.incrementAndGet();
      LOGGER.fine(String.format("Stopped tracking memory segment: 0x%x", address));
    }
  }

  /**
   * Gets the current resource statistics.
   *
   * @return the resource statistics
   */
  public ResourceStatistics getStatistics() {
    return new ResourceStatistics(statistics); // Defensive copy
  }

  /**
   * Gets the current number of tracked WASI contexts.
   *
   * @return the number of tracked contexts
   */
  public int getTrackedContextCount() {
    return trackedContexts.size();
  }

  /**
   * Gets the current number of tracked file handles.
   *
   * @return the number of tracked file handles
   */
  public int getTrackedFileHandleCount() {
    return trackedFileHandles.size();
  }

  /**
   * Gets the current number of tracked memory segments.
   *
   * @return the number of tracked memory segments
   */
  public int getTrackedMemorySegmentCount() {
    return trackedMemorySegments.size();
  }

  /**
   * Performs manual leak detection and cleanup.
   *
   * @return the leak detection results
   */
  public LeakDetectionResults performLeakDetection() {
    if (shutdown) {
      return new LeakDetectionResults(0, 0, 0, 0);
    }

    final Instant now = Instant.now();
    int leakedContexts = 0;
    int leakedFileHandles = 0;
    int leakedMemorySegments = 0;
    int cleanedUpResources = 0;

    // Check WASI contexts for leaks
    for (final Map.Entry<String, TrackedWasiContext> entry : trackedContexts.entrySet()) {
      final TrackedWasiContext tracked = entry.getValue();
      if (tracked.reference.get() == null) {
        // Context has been garbage collected but not untracked
        trackedContexts.remove(entry.getKey());
        cleanedUpResources++;
      } else if (Duration.between(tracked.creationTime, now).compareTo(resourceAgeThreshold) > 0) {
        // Context is too old and might be leaked
        leakedContexts++;
        LOGGER.warning(
            String.format(
                "Potential WASI context leak detected: %s (age: %s)",
                entry.getKey(), Duration.between(tracked.creationTime, now)));
      }
    }

    // Check file handles for leaks
    for (final Map.Entry<Integer, TrackedFileHandle> entry : trackedFileHandles.entrySet()) {
      final TrackedFileHandle tracked = entry.getValue();
      if (tracked.reference.get() == null) {
        // Handle has been garbage collected but not untracked
        trackedFileHandles.remove(entry.getKey());
        cleanedUpResources++;
      } else if (Duration.between(tracked.creationTime, now).compareTo(resourceAgeThreshold) > 0) {
        // Handle is too old and might be leaked
        leakedFileHandles++;
        LOGGER.warning(
            String.format(
                "Potential file handle leak detected: %d (age: %s)",
                entry.getKey(), Duration.between(tracked.creationTime, now)));
      }
    }

    // Check memory segments for leaks
    for (final Map.Entry<Long, TrackedMemorySegment> entry : trackedMemorySegments.entrySet()) {
      final TrackedMemorySegment tracked = entry.getValue();
      if (tracked.reference.get() == null) {
        // Segment has been garbage collected but not untracked
        trackedMemorySegments.remove(entry.getKey());
        cleanedUpResources++;
      } else if (Duration.between(tracked.creationTime, now).compareTo(resourceAgeThreshold) > 0) {
        // Segment is too old and might be leaked
        leakedMemorySegments++;
        LOGGER.warning(
            String.format(
                "Potential memory segment leak detected: 0x%x (age: %s)",
                entry.getKey(), Duration.between(tracked.creationTime, now)));
      }
    }

    // Process phantom references for automatic cleanup
    cleanedUpResources += processPhantomReferences();

    final LeakDetectionResults results =
        new LeakDetectionResults(
            leakedContexts, leakedFileHandles, leakedMemorySegments, cleanedUpResources);

    LOGGER.fine(String.format("Leak detection completed: %s", results));

    return results;
  }

  /** Closes the resource leak detector and releases all resources. */
  @Override
  public void close() {
    if (shutdown) {
      return;
    }

    LOGGER.info("Shutting down WASI resource leak detector");
    shutdown = true;

    // Cancel monitoring task
    if (monitoringTask != null) {
      monitoringTask.cancel(false);
    }

    // Shutdown monitoring executor
    monitoringExecutor.shutdown();
    try {
      if (!monitoringExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        monitoringExecutor.shutdownNow();
      }
    } catch (final InterruptedException e) {
      monitoringExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    // Clear tracking data
    trackedContexts.clear();
    trackedFileHandles.clear();
    trackedMemorySegments.clear();
    phantomReferences.clear();

    LOGGER.info("WASI resource leak detector shutdown completed");
  }

  /** Performs periodic monitoring and leak detection. */
  private void performMonitoring() {
    if (shutdown) {
      return;
    }

    try {
      final LeakDetectionResults results = performLeakDetection();

      // Check if leak threshold is exceeded
      final int totalLeaked =
          results.leakedContexts + results.leakedFileHandles + results.leakedMemorySegments;

      if (totalLeaked > leakThreshold) {
        LOGGER.log(
            Level.SEVERE,
            String.format(
                "Resource leak threshold exceeded: %d leaked resources (threshold: %d)",
                totalLeaked, leakThreshold));
      }

      // Update statistics
      statistics.lastMonitoringTime = Instant.now();
      statistics.totalLeakDetectionRuns.incrementAndGet();
      statistics.totalResourcesCleanedUp.addAndGet(results.cleanedUpResources);

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Error during resource leak monitoring", e);
    }
  }

  /** Processes phantom references for automatic cleanup. */
  @SuppressWarnings("unchecked")
  private int processPhantomReferences() {
    int cleanedUp = 0;
    PhantomReference<?> phantomRef;

    while ((phantomRef = (PhantomReference<?>) referenceQueue.poll()) != null) {
      final String resourceId = phantomReferences.remove(phantomRef);
      if (resourceId != null) {
        LOGGER.fine(String.format("Automatically cleaning up resource: %s", resourceId));

        if (resourceId.startsWith("WasiContext:")) {
          final String contextId = resourceId.substring("WasiContext:".length());
          trackedContexts.remove(contextId);
        } else if (resourceId.startsWith("FileHandle:")) {
          final int fd = Integer.parseInt(resourceId.substring("FileHandle:".length()));
          trackedFileHandles.remove(fd);
        } else if (resourceId.startsWith("MemorySegment:")) {
          final long address = Long.parseLong(resourceId.substring("MemorySegment:".length()));
          trackedMemorySegments.remove(address);
        }

        cleanedUp++;
      }
      phantomRef.clear();
    }

    return cleanedUp;
  }

  /** Tracked WASI context information. */
  private static final class TrackedWasiContext {
    final String contextId;
    final WeakReference<WasiContext> reference;
    final Instant creationTime;

    TrackedWasiContext(
        final String contextId, final WeakReference<WasiContext> reference, final Instant creationTime) {
      this.contextId = contextId;
      this.reference = reference;
      this.creationTime = creationTime;
    }
  }

  /** Tracked file handle information. */
  private static final class TrackedFileHandle {
    final int fileDescriptor;
    final WeakReference<WasiFileHandle> reference;
    final Instant creationTime;

    TrackedFileHandle(
        final int fileDescriptor,
        final WeakReference<WasiFileHandle> reference,
        final Instant creationTime) {
      this.fileDescriptor = fileDescriptor;
      this.reference = reference;
      this.creationTime = creationTime;
    }
  }

  /** Tracked memory segment information. */
  private static final class TrackedMemorySegment {
    final long address;
    final WeakReference<Object> reference;
    final Instant creationTime;

    TrackedMemorySegment(
        final long address, final WeakReference<Object> reference, final Instant creationTime) {
      this.address = address;
      this.reference = reference;
      this.creationTime = creationTime;
    }
  }

  /** Resource allocation and leak detection statistics. */
  public static final class ResourceStatistics {
    private final AtomicLong contextsCreated = new AtomicLong(0);
    private final AtomicLong contextsDestroyed = new AtomicLong(0);
    private final AtomicLong fileHandlesCreated = new AtomicLong(0);
    private final AtomicLong fileHandlesDestroyed = new AtomicLong(0);
    private final AtomicLong memorySegmentsCreated = new AtomicLong(0);
    private final AtomicLong memorySegmentsDestroyed = new AtomicLong(0);
    private final AtomicLong totalLeakDetectionRuns = new AtomicLong(0);
    private final AtomicLong totalResourcesCleanedUp = new AtomicLong(0);
    private volatile Instant lastMonitoringTime;

    /** Creates new empty statistics. */
    ResourceStatistics() {
      this.lastMonitoringTime = Instant.now();
    }

    /** Creates a defensive copy of statistics. */
    ResourceStatistics(final ResourceStatistics source) {
      this.contextsCreated.set(source.contextsCreated.get());
      this.contextsDestroyed.set(source.contextsDestroyed.get());
      this.fileHandlesCreated.set(source.fileHandlesCreated.get());
      this.fileHandlesDestroyed.set(source.fileHandlesDestroyed.get());
      this.memorySegmentsCreated.set(source.memorySegmentsCreated.get());
      this.memorySegmentsDestroyed.set(source.memorySegmentsDestroyed.get());
      this.totalLeakDetectionRuns.set(source.totalLeakDetectionRuns.get());
      this.totalResourcesCleanedUp.set(source.totalResourcesCleanedUp.get());
      this.lastMonitoringTime = source.lastMonitoringTime;
    }

    public long getContextsCreated() {
      return contextsCreated.get();
    }

    public long getContextsDestroyed() {
      return contextsDestroyed.get();
    }

    public long getFileHandlesCreated() {
      return fileHandlesCreated.get();
    }

    public long getFileHandlesDestroyed() {
      return fileHandlesDestroyed.get();
    }

    public long getMemorySegmentsCreated() {
      return memorySegmentsCreated.get();
    }

    public long getMemorySegmentsDestroyed() {
      return memorySegmentsDestroyed.get();
    }

    public long getTotalLeakDetectionRuns() {
      return totalLeakDetectionRuns.get();
    }

    public long getTotalResourcesCleanedUp() {
      return totalResourcesCleanedUp.get();
    }

    public Instant getLastMonitoringTime() {
      return lastMonitoringTime;
    }

    public long getActiveContexts() {
      return contextsCreated.get() - contextsDestroyed.get();
    }

    public long getActiveFileHandles() {
      return fileHandlesCreated.get() - fileHandlesDestroyed.get();
    }

    public long getActiveMemorySegments() {
      return memorySegmentsCreated.get() - memorySegmentsDestroyed.get();
    }

    @Override
    public String toString() {
      return String.format(
          "ResourceStatistics{contexts=%d/%d, handles=%d/%d, segments=%d/%d, runs=%d, cleaned=%d}",
          getActiveContexts(),
          contextsCreated.get(),
          getActiveFileHandles(),
          fileHandlesCreated.get(),
          getActiveMemorySegments(),
          memorySegmentsCreated.get(),
          totalLeakDetectionRuns.get(),
          totalResourcesCleanedUp.get());
    }
  }

  /** Results of leak detection operation. */
  public static final class LeakDetectionResults {
    private final int leakedContexts;
    private final int leakedFileHandles;
    private final int leakedMemorySegments;
    private final int cleanedUpResources;

    LeakDetectionResults(
        final int leakedContexts,
        final int leakedFileHandles,
        final int leakedMemorySegments,
        final int cleanedUpResources) {
      this.leakedContexts = leakedContexts;
      this.leakedFileHandles = leakedFileHandles;
      this.leakedMemorySegments = leakedMemorySegments;
      this.cleanedUpResources = cleanedUpResources;
    }

    public int getLeakedContexts() {
      return leakedContexts;
    }

    public int getLeakedFileHandles() {
      return leakedFileHandles;
    }

    public int getLeakedMemorySegments() {
      return leakedMemorySegments;
    }

    public int getCleanedUpResources() {
      return cleanedUpResources;
    }

    public int getTotalLeaked() {
      return leakedContexts + leakedFileHandles + leakedMemorySegments;
    }

    public boolean hasLeaks() {
      return getTotalLeaked() > 0;
    }

    @Override
    public String toString() {
      return String.format(
          "LeakDetectionResults{contexts=%d, handles=%d, segments=%d, cleaned=%d}",
          leakedContexts, leakedFileHandles, leakedMemorySegments, cleanedUpResources);
    }
  }
}