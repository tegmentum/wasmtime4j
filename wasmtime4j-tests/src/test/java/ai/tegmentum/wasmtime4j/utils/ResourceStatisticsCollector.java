package ai.tegmentum.wasmtime4j.utils;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Comprehensive resource statistics collection utility for monitoring memory usage, garbage
 * collection behavior, and resource lifecycle patterns.
 *
 * <p>This utility provides detailed monitoring capabilities for resource management validation,
 * including memory tracking, GC analysis, and resource usage statistics.
 */
public final class ResourceStatisticsCollector {

  private static final Logger LOGGER =
      Logger.getLogger(ResourceStatisticsCollector.class.getName());

  private final MemoryMXBean memoryBean;
  private final List<GarbageCollectorMXBean> gcBeans;
  private final List<MemorySnapshot> memorySnapshots;
  private final List<GcSnapshot> gcSnapshots;
  private final ConcurrentMap<String, ResourceTypeStatistics> resourceStats;
  private final AtomicLong totalResourcesTracked = new AtomicLong(0);
  private final AtomicLong totalResourcesCleaned = new AtomicLong(0);
  private final AtomicLong totalResourceLeaks = new AtomicLong(0);

  private volatile boolean collecting = false;
  private volatile Instant collectionStartTime = null;
  private ScheduledFuture<?> scheduledCollection = null;

  /** Creates a new resource statistics collector. */
  public ResourceStatisticsCollector() {
    this.memoryBean = ManagementFactory.getMemoryMXBean();
    this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    this.memorySnapshots = Collections.synchronizedList(new ArrayList<>());
    this.gcSnapshots = Collections.synchronizedList(new ArrayList<>());
    this.resourceStats = new ConcurrentHashMap<>();
  }

  /**
   * Starts collecting resource statistics.
   *
   * @param testName the name of the test being monitored
   */
  public void startCollection(final String testName) {
    if (collecting) {
      LOGGER.warning("Statistics collection already active, stopping previous collection");
      stopCollection();
    }

    collecting = true;
    collectionStartTime = Instant.now();

    // Take initial snapshots
    captureMemorySnapshot();
    captureGcSnapshot();

    LOGGER.info("Started resource statistics collection for: " + testName);
  }

  /**
   * Starts periodic collection of resource statistics.
   *
   * @param testName the name of the test being monitored
   * @param intervalMs the interval in milliseconds between collections
   * @param executor the executor service to use for scheduling
   */
  public void startPeriodicCollection(
      final String testName, final long intervalMs, final ScheduledExecutorService executor) {
    startCollection(testName);

    scheduledCollection =
        executor.scheduleAtFixedRate(
            this::capturePeriodicSnapshots, intervalMs, intervalMs, TimeUnit.MILLISECONDS);

    LOGGER.info("Started periodic resource statistics collection (interval: " + intervalMs + "ms)");
  }

  /** Stops collecting resource statistics. */
  public void stopCollection() {
    if (!collecting) {
      return;
    }

    collecting = false;

    if (scheduledCollection != null && !scheduledCollection.isCancelled()) {
      scheduledCollection.cancel(false);
      scheduledCollection = null;
    }

    // Take final snapshots
    captureMemorySnapshot();
    captureGcSnapshot();

    final Duration collectionDuration =
        collectionStartTime != null
            ? Duration.between(collectionStartTime, Instant.now())
            : Duration.ZERO;

    LOGGER.info(
        "Stopped resource statistics collection (duration: "
            + collectionDuration.toMillis()
            + "ms)");
  }

  /**
   * Records the creation of a resource of the specified type.
   *
   * @param resourceType the type of resource created
   * @param count the number of resources created
   */
  public void recordResourceCreation(final String resourceType, final long count) {
    totalResourcesTracked.addAndGet(count);
    resourceStats
        .computeIfAbsent(resourceType, k -> new ResourceTypeStatistics(k))
        .recordCreation(count);
  }

  /**
   * Records the cleanup of a resource of the specified type.
   *
   * @param resourceType the type of resource cleaned up
   * @param count the number of resources cleaned up
   */
  public void recordResourceCleanup(final String resourceType, final long count) {
    totalResourcesCleaned.addAndGet(count);
    resourceStats
        .computeIfAbsent(resourceType, k -> new ResourceTypeStatistics(k))
        .recordCleanup(count);
  }

  /**
   * Records a detected resource leak.
   *
   * @param resourceType the type of resource that leaked
   * @param count the number of leaked resources
   */
  public void recordResourceLeak(final String resourceType, final long count) {
    totalResourceLeaks.addAndGet(count);
    resourceStats
        .computeIfAbsent(resourceType, k -> new ResourceTypeStatistics(k))
        .recordLeak(count);
  }

  /**
   * Records a memory pressure event.
   *
   * @param pressureLevel the level of memory pressure (0.0 to 1.0)
   * @param triggeringAction the action that triggered the pressure
   */
  public void recordMemoryPressure(final double pressureLevel, final String triggeringAction) {
    // Capture snapshot during memory pressure
    captureMemorySnapshot();
    LOGGER.info(
        "Memory pressure recorded: " + (pressureLevel * 100) + "% (" + triggeringAction + ")");
  }

  /** Captures a memory usage snapshot. */
  public void captureMemorySnapshot() {
    if (!collecting) {
      return;
    }

    final MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    final MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

    final MemorySnapshot snapshot =
        new MemorySnapshot(
            System.currentTimeMillis(),
            heapUsage.getUsed(),
            heapUsage.getCommitted(),
            heapUsage.getMax(),
            nonHeapUsage.getUsed(),
            nonHeapUsage.getCommitted(),
            nonHeapUsage.getMax());

    memorySnapshots.add(snapshot);
  }

  /** Captures a garbage collection snapshot. */
  public void captureGcSnapshot() {
    if (!collecting) {
      return;
    }

    long totalCollections = 0;
    long totalCollectionTime = 0;

    for (final GarbageCollectorMXBean gcBean : gcBeans) {
      totalCollections += gcBean.getCollectionCount();
      totalCollectionTime += gcBean.getCollectionTime();
    }

    final GcSnapshot snapshot =
        new GcSnapshot(System.currentTimeMillis(), totalCollections, totalCollectionTime);

    gcSnapshots.add(snapshot);
  }

  /** Captures both memory and GC snapshots periodically. */
  private void capturePeriodicSnapshots() {
    try {
      captureMemorySnapshot();
      captureGcSnapshot();
    } catch (final Exception e) {
      LOGGER.warning("Error capturing periodic snapshots: " + e.getMessage());
    }
  }

  /**
   * Generates a comprehensive statistics report.
   *
   * @return detailed resource and memory statistics report
   */
  public ResourceStatisticsReport generateReport() {
    final Duration collectionDuration =
        collectionStartTime != null
            ? Duration.between(collectionStartTime, Instant.now())
            : Duration.ZERO;

    return new ResourceStatisticsReport(
        collectionDuration,
        totalResourcesTracked.get(),
        totalResourcesCleaned.get(),
        totalResourceLeaks.get(),
        new ArrayList<>(memorySnapshots),
        new ArrayList<>(gcSnapshots),
        new ConcurrentHashMap<>(resourceStats));
  }

  /**
   * Gets current memory usage statistics.
   *
   * @return current memory usage information
   */
  public MemoryUsageStatistics getCurrentMemoryUsage() {
    final MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    final MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

    return new MemoryUsageStatistics(
        heapUsage.getUsed(),
        heapUsage.getCommitted(),
        heapUsage.getMax(),
        nonHeapUsage.getUsed(),
        nonHeapUsage.getCommitted(),
        nonHeapUsage.getMax());
  }

  /**
   * Gets resource leak statistics by type.
   *
   * @return map of resource types to their leak statistics
   */
  public ConcurrentMap<String, ResourceLeakStatistics> getResourceLeakStatistics() {
    final ConcurrentMap<String, ResourceLeakStatistics> leakStats = new ConcurrentHashMap<>();

    for (final ResourceTypeStatistics typeStats : resourceStats.values()) {
      if (typeStats.getTotalLeaks() > 0) {
        leakStats.put(
            typeStats.getResourceType(),
            new ResourceLeakStatistics(
                typeStats.getResourceType(),
                typeStats.getTotalCreated(),
                typeStats.getTotalCleaned(),
                typeStats.getTotalLeaks(),
                typeStats.getLeakRate()));
      }
    }

    return leakStats;
  }

  /**
   * Analyzes memory usage patterns from collected snapshots.
   *
   * @return memory pattern analysis results
   */
  public MemoryPatternAnalysis analyzeMemoryPatterns() {
    if (memorySnapshots.size() < 2) {
      return new MemoryPatternAnalysis("Insufficient data for pattern analysis");
    }

    final List<MemorySnapshot> snapshots = new ArrayList<>(memorySnapshots);
    snapshots.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));

    final MemorySnapshot first = snapshots.get(0);
    final MemorySnapshot last = snapshots.get(snapshots.size() - 1);

    final long durationMs = last.timestamp - first.timestamp;
    final long heapGrowth = last.heapUsed - first.heapUsed;
    final long nonHeapGrowth = last.nonHeapUsed - first.nonHeapUsed;

    // Calculate growth rates (bytes per second)
    final double heapGrowthRate =
        durationMs > 0 ? (double) heapGrowth / (durationMs / 1000.0) : 0.0;
    final double nonHeapGrowthRate =
        durationMs > 0 ? (double) nonHeapGrowth / (durationMs / 1000.0) : 0.0;

    // Detect memory usage patterns
    final MemoryUsagePattern pattern = detectMemoryUsagePattern(snapshots);

    // Calculate memory efficiency
    final double avgHeapUsed = snapshots.stream().mapToLong(s -> s.heapUsed).average().orElse(0.0);
    final double avgHeapCommitted =
        snapshots.stream().mapToLong(s -> s.heapCommitted).average().orElse(1.0);
    final double memoryEfficiency = (avgHeapUsed / avgHeapCommitted) * 100.0;

    return new MemoryPatternAnalysis(
        durationMs,
        heapGrowth,
        nonHeapGrowth,
        heapGrowthRate,
        nonHeapGrowthRate,
        pattern,
        memoryEfficiency,
        snapshots.size());
  }

  /**
   * Analyzes garbage collection behavior from collected snapshots.
   *
   * @return GC behavior analysis results
   */
  public GarbageCollectionAnalysis analyzeGarbageCollection() {
    if (gcSnapshots.size() < 2) {
      return new GarbageCollectionAnalysis("Insufficient GC data for analysis");
    }

    final List<GcSnapshot> snapshots = new ArrayList<>(gcSnapshots);
    snapshots.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));

    final GcSnapshot first = snapshots.get(0);
    final GcSnapshot last = snapshots.get(snapshots.size() - 1);

    final long totalCollections = last.totalCollections - first.totalCollections;
    final long totalCollectionTime = last.totalCollectionTime - first.totalCollectionTime;
    final long durationMs = last.timestamp - first.timestamp;

    final double collectionFrequency =
        durationMs > 0 ? (double) totalCollections / (durationMs / 1000.0) : 0.0;
    final double collectionOverhead =
        durationMs > 0 ? (double) totalCollectionTime / durationMs * 100.0 : 0.0;
    final double avgCollectionTime =
        totalCollections > 0 ? (double) totalCollectionTime / totalCollections : 0.0;

    return new GarbageCollectionAnalysis(
        totalCollections,
        totalCollectionTime,
        collectionFrequency,
        collectionOverhead,
        avgCollectionTime);
  }

  /** Resets all collected statistics. */
  public void reset() {
    memorySnapshots.clear();
    gcSnapshots.clear();
    resourceStats.clear();
    totalResourcesTracked.set(0);
    totalResourcesCleaned.set(0);
    totalResourceLeaks.set(0);
    collectionStartTime = null;

    LOGGER.info("Resource statistics collector reset");
  }

  /** Detects memory usage patterns from snapshots. */
  private MemoryUsagePattern detectMemoryUsagePattern(final List<MemorySnapshot> snapshots) {
    if (snapshots.size() < 3) {
      return MemoryUsagePattern.INSUFFICIENT_DATA;
    }

    // Analyze heap usage trend
    final long firstThirdEnd = snapshots.size() / 3;
    final long lastThirdStart = 2 * snapshots.size() / 3;

    final double firstThirdAvg =
        snapshots.subList(0, (int) firstThirdEnd).stream()
            .mapToLong(s -> s.heapUsed)
            .average()
            .orElse(0.0);

    final double lastThirdAvg =
        snapshots.subList((int) lastThirdStart, snapshots.size()).stream()
            .mapToLong(s -> s.heapUsed)
            .average()
            .orElse(0.0);

    final double changePercent =
        firstThirdAvg > 0 ? (lastThirdAvg - firstThirdAvg) / firstThirdAvg * 100.0 : 0.0;

    if (changePercent > 20.0) {
      return MemoryUsagePattern.INCREASING;
    } else if (changePercent < -20.0) {
      return MemoryUsagePattern.DECREASING;
    } else if (hasSignificantFluctuations(snapshots)) {
      return MemoryUsagePattern.FLUCTUATING;
    } else {
      return MemoryUsagePattern.STABLE;
    }
  }

  /** Checks for significant fluctuations in memory usage. */
  private boolean hasSignificantFluctuations(final List<MemorySnapshot> snapshots) {
    if (snapshots.size() < 5) {
      return false;
    }

    final double avg = snapshots.stream().mapToLong(s -> s.heapUsed).average().orElse(0.0);
    final double variance =
        snapshots.stream().mapToDouble(s -> Math.pow(s.heapUsed - avg, 2)).average().orElse(0.0);

    final double stdDev = Math.sqrt(variance);
    final double coefficientOfVariation = avg > 0 ? stdDev / avg : 0.0;

    return coefficientOfVariation > 0.2; // > 20% coefficient of variation
  }

  /** Memory snapshot at a point in time. */
  public static class MemorySnapshot {
    public final long timestamp;
    public final long heapUsed;
    public final long heapCommitted;
    public final long heapMax;
    public final long nonHeapUsed;
    public final long nonHeapCommitted;
    public final long nonHeapMax;

    public MemorySnapshot(
        final long timestamp,
        final long heapUsed,
        final long heapCommitted,
        final long heapMax,
        final long nonHeapUsed,
        final long nonHeapCommitted,
        final long nonHeapMax) {
      this.timestamp = timestamp;
      this.heapUsed = heapUsed;
      this.heapCommitted = heapCommitted;
      this.heapMax = heapMax;
      this.nonHeapUsed = nonHeapUsed;
      this.nonHeapCommitted = nonHeapCommitted;
      this.nonHeapMax = nonHeapMax;
    }
  }

  /** Garbage collection snapshot at a point in time. */
  public static class GcSnapshot {
    public final long timestamp;
    public final long totalCollections;
    public final long totalCollectionTime;

    public GcSnapshot(
        final long timestamp, final long totalCollections, final long totalCollectionTime) {
      this.timestamp = timestamp;
      this.totalCollections = totalCollections;
      this.totalCollectionTime = totalCollectionTime;
    }
  }

  /** Statistics for a specific resource type. */
  private static class ResourceTypeStatistics {
    private final String resourceType;
    private final AtomicLong totalCreated = new AtomicLong(0);
    private final AtomicLong totalCleaned = new AtomicLong(0);
    private final AtomicLong totalLeaks = new AtomicLong(0);

    public ResourceTypeStatistics(final String resourceType) {
      this.resourceType = resourceType;
    }

    public void recordCreation(final long count) {
      totalCreated.addAndGet(count);
    }

    public void recordCleanup(final long count) {
      totalCleaned.addAndGet(count);
    }

    public void recordLeak(final long count) {
      totalLeaks.addAndGet(count);
    }

    public String getResourceType() {
      return resourceType;
    }

    public long getTotalCreated() {
      return totalCreated.get();
    }

    public long getTotalCleaned() {
      return totalCleaned.get();
    }

    public long getTotalLeaks() {
      return totalLeaks.get();
    }

    public double getLeakRate() {
      final long created = getTotalCreated();
      return created > 0 ? (double) getTotalLeaks() / created * 100.0 : 0.0;
    }
  }

  /** Memory usage patterns. */
  public enum MemoryUsagePattern {
    INCREASING,
    DECREASING,
    STABLE,
    FLUCTUATING,
    INSUFFICIENT_DATA
  }

  /** Current memory usage statistics. */
  public static class MemoryUsageStatistics {
    public final long heapUsed;
    public final long heapCommitted;
    public final long heapMax;
    public final long nonHeapUsed;
    public final long nonHeapCommitted;
    public final long nonHeapMax;

    public MemoryUsageStatistics(
        final long heapUsed,
        final long heapCommitted,
        final long heapMax,
        final long nonHeapUsed,
        final long nonHeapCommitted,
        final long nonHeapMax) {
      this.heapUsed = heapUsed;
      this.heapCommitted = heapCommitted;
      this.heapMax = heapMax;
      this.nonHeapUsed = nonHeapUsed;
      this.nonHeapCommitted = nonHeapCommitted;
      this.nonHeapMax = nonHeapMax;
    }

    public double getHeapUtilization() {
      return heapCommitted > 0 ? (double) heapUsed / heapCommitted * 100.0 : 0.0;
    }

    public double getNonHeapUtilization() {
      return nonHeapCommitted > 0 ? (double) nonHeapUsed / nonHeapCommitted * 100.0 : 0.0;
    }
  }

  /** Resource leak statistics for a specific resource type. */
  public static class ResourceLeakStatistics {
    public final String resourceType;
    public final long totalCreated;
    public final long totalCleaned;
    public final long totalLeaks;
    public final double leakRate;

    public ResourceLeakStatistics(
        final String resourceType,
        final long totalCreated,
        final long totalCleaned,
        final long totalLeaks,
        final double leakRate) {
      this.resourceType = resourceType;
      this.totalCreated = totalCreated;
      this.totalCleaned = totalCleaned;
      this.totalLeaks = totalLeaks;
      this.leakRate = leakRate;
    }
  }

  /** Memory pattern analysis results. */
  public static class MemoryPatternAnalysis {
    public final long durationMs;
    public final long heapGrowth;
    public final long nonHeapGrowth;
    public final double heapGrowthRate;
    public final double nonHeapGrowthRate;
    public final MemoryUsagePattern pattern;
    public final double memoryEfficiency;
    public final int snapshotCount;
    public final String errorMessage;

    public MemoryPatternAnalysis(
        final long durationMs,
        final long heapGrowth,
        final long nonHeapGrowth,
        final double heapGrowthRate,
        final double nonHeapGrowthRate,
        final MemoryUsagePattern pattern,
        final double memoryEfficiency,
        final int snapshotCount) {
      this.durationMs = durationMs;
      this.heapGrowth = heapGrowth;
      this.nonHeapGrowth = nonHeapGrowth;
      this.heapGrowthRate = heapGrowthRate;
      this.nonHeapGrowthRate = nonHeapGrowthRate;
      this.pattern = pattern;
      this.memoryEfficiency = memoryEfficiency;
      this.snapshotCount = snapshotCount;
      this.errorMessage = null;
    }

    public MemoryPatternAnalysis(final String errorMessage) {
      this.durationMs = 0;
      this.heapGrowth = 0;
      this.nonHeapGrowth = 0;
      this.heapGrowthRate = 0.0;
      this.nonHeapGrowthRate = 0.0;
      this.pattern = MemoryUsagePattern.INSUFFICIENT_DATA;
      this.memoryEfficiency = 0.0;
      this.snapshotCount = 0;
      this.errorMessage = errorMessage;
    }

    public boolean hasError() {
      return errorMessage != null;
    }

    public boolean indicatesMemoryLeak() {
      return pattern == MemoryUsagePattern.INCREASING && heapGrowthRate > 1024; // > 1KB/sec growth
    }
  }

  /** Garbage collection analysis results. */
  public static class GarbageCollectionAnalysis {
    public final long totalCollections;
    public final long totalCollectionTime;
    public final double collectionFrequency;
    public final double collectionOverhead;
    public final double avgCollectionTime;
    public final String errorMessage;

    public GarbageCollectionAnalysis(
        final long totalCollections,
        final long totalCollectionTime,
        final double collectionFrequency,
        final double collectionOverhead,
        final double avgCollectionTime) {
      this.totalCollections = totalCollections;
      this.totalCollectionTime = totalCollectionTime;
      this.collectionFrequency = collectionFrequency;
      this.collectionOverhead = collectionOverhead;
      this.avgCollectionTime = avgCollectionTime;
      this.errorMessage = null;
    }

    public GarbageCollectionAnalysis(final String errorMessage) {
      this.totalCollections = 0;
      this.totalCollectionTime = 0;
      this.collectionFrequency = 0.0;
      this.collectionOverhead = 0.0;
      this.avgCollectionTime = 0.0;
      this.errorMessage = errorMessage;
    }

    public boolean hasError() {
      return errorMessage != null;
    }

    public boolean indicatesExcessiveGc() {
      return collectionOverhead > 10.0; // > 10% time spent in GC
    }
  }

  /** Comprehensive resource statistics report. */
  public static class ResourceStatisticsReport {
    public final Duration collectionDuration;
    public final long totalResourcesTracked;
    public final long totalResourcesCleaned;
    public final long totalResourceLeaks;
    public final List<MemorySnapshot> memorySnapshots;
    public final List<GcSnapshot> gcSnapshots;
    public final ConcurrentMap<String, ResourceTypeStatistics> resourceStats;

    public ResourceStatisticsReport(
        final Duration collectionDuration,
        final long totalResourcesTracked,
        final long totalResourcesCleaned,
        final long totalResourceLeaks,
        final List<MemorySnapshot> memorySnapshots,
        final List<GcSnapshot> gcSnapshots,
        final ConcurrentMap<String, ResourceTypeStatistics> resourceStats) {
      this.collectionDuration = collectionDuration;
      this.totalResourcesTracked = totalResourcesTracked;
      this.totalResourcesCleaned = totalResourcesCleaned;
      this.totalResourceLeaks = totalResourceLeaks;
      this.memorySnapshots = memorySnapshots;
      this.gcSnapshots = gcSnapshots;
      this.resourceStats = resourceStats;
    }

    public double getOverallLeakRate() {
      return totalResourcesTracked > 0
          ? (double) totalResourceLeaks / totalResourcesTracked * 100.0
          : 0.0;
    }

    public double getCleanupEffectiveness() {
      return totalResourcesTracked > 0
          ? (double) totalResourcesCleaned / totalResourcesTracked * 100.0
          : 0.0;
    }

    public String generateSummary() {
      return String.format(
          "Resource Statistics Summary:\n"
              + "  Collection Duration: %d seconds\n"
              + "  Total Resources Tracked: %d\n"
              + "  Total Resources Cleaned: %d\n"
              + "  Total Resource Leaks: %d\n"
              + "  Overall Leak Rate: %.2f%%\n"
              + "  Cleanup Effectiveness: %.2f%%\n"
              + "  Memory Snapshots: %d\n"
              + "  GC Snapshots: %d\n"
              + "  Resource Types Monitored: %d",
          collectionDuration.getSeconds(),
          totalResourcesTracked,
          totalResourcesCleaned,
          totalResourceLeaks,
          getOverallLeakRate(),
          getCleanupEffectiveness(),
          memorySnapshots.size(),
          gcSnapshots.size(),
          resourceStats.size());
    }
  }
}
