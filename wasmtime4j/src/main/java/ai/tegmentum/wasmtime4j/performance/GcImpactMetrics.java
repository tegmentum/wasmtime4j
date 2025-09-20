package ai.tegmentum.wasmtime4j.performance;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Captures and analyzes the impact of garbage collection on WebAssembly performance.
 *
 * <p>This class provides comprehensive metrics about garbage collection activity including:
 *
 * <ul>
 *   <li>GC frequency and duration measurements
 *   <li>Memory allocation and collection patterns
 *   <li>GC overhead percentage calculations
 *   <li>Performance impact assessment
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // Capture GC state before operation
 * GcImpactMetrics.Snapshot beforeGc = GcImpactMetrics.captureSnapshot();
 *
 * // ... perform WebAssembly operations ...
 *
 * // Capture GC state after operation
 * GcImpactMetrics.Snapshot afterGc = GcImpactMetrics.captureSnapshot();
 *
 * // Calculate impact
 * GcImpactMetrics impact = GcImpactMetrics.calculate(beforeGc, afterGc, operationDuration);
 *
 * System.out.println("GC overhead: " + impact.getGcOverheadPercentage() + "%");
 * }</pre>
 *
 * @since 1.0.0
 */
public final class GcImpactMetrics {
  private final Duration totalGcTime;
  private final long totalGcCollections;
  private final long memoryAllocated;
  private final long memoryFreed;
  private final Duration operationDuration;
  private final double gcOverheadPercentage;
  private final double allocationRate;
  private final double collectionEfficiency;
  private final Instant captureTime;
  private final List<GcCollectorMetrics> collectorMetrics;

  private GcImpactMetrics(
      final Duration totalGcTime,
      final long totalGcCollections,
      final long memoryAllocated,
      final long memoryFreed,
      final Duration operationDuration,
      final List<GcCollectorMetrics> collectorMetrics) {
    this.totalGcTime = Objects.requireNonNull(totalGcTime, "totalGcTime cannot be null");
    this.totalGcCollections = totalGcCollections;
    this.memoryAllocated = memoryAllocated;
    this.memoryFreed = memoryFreed;
    this.operationDuration =
        Objects.requireNonNull(operationDuration, "operationDuration cannot be null");
    this.collectorMetrics = List.copyOf(collectorMetrics);
    this.captureTime = Instant.now();

    // Calculate derived metrics
    this.gcOverheadPercentage = calculateGcOverheadPercentage();
    this.allocationRate = calculateAllocationRate();
    this.collectionEfficiency = calculateCollectionEfficiency();
  }

  /**
   * Captures a snapshot of current garbage collection state.
   *
   * @return snapshot of current GC state
   */
  public static Snapshot captureSnapshot() {
    final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    final Instant timestamp = Instant.now();

    long totalCollections = 0;
    long totalGcTimeMs = 0;
    final var collectorSnapshots = new java.util.ArrayList<GcCollectorSnapshot>();

    for (final GarbageCollectorMXBean gcBean : gcBeans) {
      final long collections = gcBean.getCollectionCount();
      final long gcTime = gcBean.getCollectionTime(); // milliseconds

      if (collections > 0) {
        totalCollections += collections;
        totalGcTimeMs += gcTime;

        collectorSnapshots.add(
            new GcCollectorSnapshot(
                gcBean.getName(), collections, gcTime, gcBean.getMemoryPoolNames()));
      }
    }

    final MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    final MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

    return new Snapshot(
        timestamp,
        Duration.ofMillis(totalGcTimeMs),
        totalCollections,
        heapUsage.getUsed(),
        nonHeapUsage.getUsed(),
        heapUsage.getMax(),
        collectorSnapshots);
  }

  /**
   * Calculates GC impact metrics between two snapshots.
   *
   * @param before snapshot before operation
   * @param after snapshot after operation
   * @param operationDuration duration of the operation
   * @return GC impact metrics
   * @throws IllegalArgumentException if parameters are invalid
   */
  public static GcImpactMetrics calculate(
      final Snapshot before, final Snapshot after, final Duration operationDuration) {
    Objects.requireNonNull(before, "before snapshot cannot be null");
    Objects.requireNonNull(after, "after snapshot cannot be null");
    Objects.requireNonNull(operationDuration, "operationDuration cannot be null");

    if (after.getTimestamp().isBefore(before.getTimestamp())) {
      throw new IllegalArgumentException("after snapshot must be captured after before snapshot");
    }

    final Duration totalGcTime = after.getTotalGcTime().minus(before.getTotalGcTime());
    final long totalCollections = after.getTotalCollections() - before.getTotalCollections();
    final long memoryChange = after.getHeapMemoryUsed() - before.getHeapMemoryUsed();

    // Calculate allocation and collection based on memory changes and GC activity
    final long memoryAllocated = Math.max(0, memoryChange + calculateMemoryFreed(before, after));
    final long memoryFreed = calculateMemoryFreed(before, after);

    final var collectorMetrics = calculateCollectorMetrics(before, after);

    return new GcImpactMetrics(
        totalGcTime,
        totalCollections,
        memoryAllocated,
        memoryFreed,
        operationDuration,
        collectorMetrics);
  }

  /**
   * Forces garbage collection and measures the impact.
   *
   * @return GC impact metrics from forced collection
   */
  public static GcImpactMetrics measureForcedGc() {
    final Snapshot before = captureSnapshot();
    final Instant gcStart = Instant.now();

    System.gc();

    // Wait briefly for GC to complete
    try {
      Thread.sleep(50);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    final Instant gcEnd = Instant.now();
    final Snapshot after = captureSnapshot();

    return calculate(before, after, Duration.between(gcStart, gcEnd));
  }

  // Getters
  public Duration getTotalGcTime() {
    return totalGcTime;
  }

  public long getTotalGcCollections() {
    return totalGcCollections;
  }

  public long getMemoryAllocated() {
    return memoryAllocated;
  }

  public long getMemoryFreed() {
    return memoryFreed;
  }

  public Duration getOperationDuration() {
    return operationDuration;
  }

  public double getGcOverheadPercentage() {
    return gcOverheadPercentage;
  }

  public double getAllocationRate() {
    return allocationRate;
  }

  public double getCollectionEfficiency() {
    return collectionEfficiency;
  }

  public Instant getCaptureTime() {
    return captureTime;
  }

  public List<GcCollectorMetrics> getCollectorMetrics() {
    return collectorMetrics;
  }

  /**
   * Checks if garbage collection had significant impact on performance.
   *
   * @return true if GC overhead exceeds 5% of operation time
   */
  public boolean hasSignificantImpact() {
    return gcOverheadPercentage > 5.0;
  }

  /**
   * Checks if memory allocation rate is high.
   *
   * @return true if allocation rate exceeds 100MB/s
   */
  public boolean hasHighAllocationRate() {
    return allocationRate > 100.0; // MB/s
  }

  /**
   * Gets a summary of GC impact for reporting.
   *
   * @return human-readable summary
   */
  public String getSummary() {
    return String.format(
        "GC Impact: %.1f%% overhead, %d collections, %.1f MB allocated, %.1f MB/s rate",
        gcOverheadPercentage,
        totalGcCollections,
        memoryAllocated / (1024.0 * 1024.0),
        allocationRate);
  }

  private double calculateGcOverheadPercentage() {
    if (operationDuration.isZero() || operationDuration.isNegative()) {
      return 0.0;
    }
    return (totalGcTime.toNanos() / (double) operationDuration.toNanos()) * 100.0;
  }

  private double calculateAllocationRate() {
    if (operationDuration.isZero() || operationDuration.isNegative()) {
      return 0.0;
    }
    final double seconds = operationDuration.toNanos() / 1_000_000_000.0;
    final double megabytes = memoryAllocated / (1024.0 * 1024.0);
    return megabytes / seconds;
  }

  private double calculateCollectionEfficiency() {
    if (memoryAllocated == 0) {
      return 100.0;
    }
    return (memoryFreed / (double) memoryAllocated) * 100.0;
  }

  private static long calculateMemoryFreed(final Snapshot before, final Snapshot after) {
    // Estimate memory freed based on GC activity and memory usage changes
    // This is an approximation since we can't directly measure freed memory
    final long memoryChange = after.getHeapMemoryUsed() - before.getHeapMemoryUsed();
    final long collections = after.getTotalCollections() - before.getTotalCollections();

    if (collections > 0 && memoryChange < 0) {
      return Math.abs(memoryChange);
    }

    // Estimate based on typical collection patterns
    return collections * 1024 * 1024; // Rough estimate: 1MB per collection
  }

  private static List<GcCollectorMetrics> calculateCollectorMetrics(
      final Snapshot before, final Snapshot after) {
    final var metrics = new java.util.ArrayList<GcCollectorMetrics>();

    for (final GcCollectorSnapshot beforeCollector : before.getCollectorSnapshots()) {
      final String collectorName = beforeCollector.getName();

      // Find matching collector in after snapshot
      final var afterCollector =
          after.getCollectorSnapshots().stream()
              .filter(c -> c.getName().equals(collectorName))
              .findFirst();

      if (afterCollector.isPresent()) {
        final GcCollectorSnapshot afterSnap = afterCollector.get();
        final long collections =
            afterSnap.getCollectionCount() - beforeCollector.getCollectionCount();
        final long gcTime = afterSnap.getCollectionTime() - beforeCollector.getCollectionTime();

        if (collections > 0 || gcTime > 0) {
          metrics.add(
              new GcCollectorMetrics(
                  collectorName,
                  collections,
                  Duration.ofMillis(gcTime),
                  beforeCollector.getMemoryPoolNames()));
        }
      }
    }

    return metrics;
  }

  @Override
  public String toString() {
    return String.format(
        "GcImpactMetrics{gcTime=%s, collections=%d, allocated=%d bytes, "
            + "freed=%d bytes, overhead=%.2f%%, rate=%.2f MB/s}",
        totalGcTime,
        totalGcCollections,
        memoryAllocated,
        memoryFreed,
        gcOverheadPercentage,
        allocationRate);
  }

  /** Snapshot of garbage collection state at a specific point in time. */
  public static final class Snapshot {
    private final Instant timestamp;
    private final Duration totalGcTime;
    private final long totalCollections;
    private final long heapMemoryUsed;
    private final long nonHeapMemoryUsed;
    private final long maxHeapMemory;
    private final List<GcCollectorSnapshot> collectorSnapshots;

    public Snapshot(
        final Instant timestamp,
        final Duration totalGcTime,
        final long totalCollections,
        final long heapMemoryUsed,
        final long nonHeapMemoryUsed,
        final long maxHeapMemory,
        final List<GcCollectorSnapshot> collectorSnapshots) {
      this.timestamp = timestamp;
      this.totalGcTime = totalGcTime;
      this.totalCollections = totalCollections;
      this.heapMemoryUsed = heapMemoryUsed;
      this.nonHeapMemoryUsed = nonHeapMemoryUsed;
      this.maxHeapMemory = maxHeapMemory;
      this.collectorSnapshots = List.copyOf(collectorSnapshots);
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public Duration getTotalGcTime() {
      return totalGcTime;
    }

    public long getTotalCollections() {
      return totalCollections;
    }

    public long getHeapMemoryUsed() {
      return heapMemoryUsed;
    }

    public long getNonHeapMemoryUsed() {
      return nonHeapMemoryUsed;
    }

    public long getMaxHeapMemory() {
      return maxHeapMemory;
    }

    public List<GcCollectorSnapshot> getCollectorSnapshots() {
      return collectorSnapshots;
    }
  }

  /** Snapshot of a specific garbage collector's state. */
  public static final class GcCollectorSnapshot {
    private final String name;
    private final long collectionCount;
    private final long collectionTime; // milliseconds
    private final String[] memoryPoolNames;

    public GcCollectorSnapshot(
        final String name,
        final long collectionCount,
        final long collectionTime,
        final String[] memoryPoolNames) {
      this.name = name;
      this.collectionCount = collectionCount;
      this.collectionTime = collectionTime;
      this.memoryPoolNames = memoryPoolNames.clone();
    }

    public String getName() {
      return name;
    }

    public long getCollectionCount() {
      return collectionCount;
    }

    public long getCollectionTime() {
      return collectionTime;
    }

    public String[] getMemoryPoolNames() {
      return memoryPoolNames.clone();
    }
  }

  /** Metrics for a specific garbage collector. */
  public static final class GcCollectorMetrics {
    private final String collectorName;
    private final long collections;
    private final Duration totalTime;
    private final String[] memoryPoolNames;

    public GcCollectorMetrics(
        final String collectorName,
        final long collections,
        final Duration totalTime,
        final String[] memoryPoolNames) {
      this.collectorName = collectorName;
      this.collections = collections;
      this.totalTime = totalTime;
      this.memoryPoolNames = memoryPoolNames.clone();
    }

    public String getCollectorName() {
      return collectorName;
    }

    public long getCollections() {
      return collections;
    }

    public Duration getTotalTime() {
      return totalTime;
    }

    public String[] getMemoryPoolNames() {
      return memoryPoolNames.clone();
    }

    public double getAverageCollectionTime() {
      return collections > 0 ? totalTime.toNanos() / (double) collections / 1_000_000.0 : 0.0; // ms
    }
  }
}
