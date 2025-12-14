package ai.tegmentum.wasmtime4j.gc;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * WebAssembly GC statistics.
 *
 * <p>Provides comprehensive metrics about garbage collection activity, memory usage, and
 * performance characteristics of the WebAssembly GC runtime.
 *
 * @since 1.0.0
 */
public final class GcStats {
  private final long totalAllocated;
  private final long totalCollected;
  private final long bytesAllocated;
  private final long bytesCollected;
  private final long minorCollections;
  private final long majorCollections;
  private final Duration totalGcTime;
  private final int currentHeapSize;
  private final int peakHeapSize;
  private final int maxHeapSize;
  private final Map<Integer, Long> objectsByGeneration;
  private final Instant captureTime;

  private GcStats(final Builder builder) {
    this.totalAllocated = builder.totalAllocated;
    this.totalCollected = builder.totalCollected;
    this.bytesAllocated = builder.bytesAllocated;
    this.bytesCollected = builder.bytesCollected;
    this.minorCollections = builder.minorCollections;
    this.majorCollections = builder.majorCollections;
    this.totalGcTime = builder.totalGcTime;
    this.currentHeapSize = builder.currentHeapSize;
    this.peakHeapSize = builder.peakHeapSize;
    this.maxHeapSize = builder.maxHeapSize;
    this.objectsByGeneration = Collections.unmodifiableMap(builder.objectsByGeneration);
    this.captureTime = builder.captureTime;
  }

  /**
   * Creates a new GC stats builder.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets the total number of objects allocated.
   *
   * @return total objects allocated
   */
  public long getTotalAllocated() {
    return totalAllocated;
  }

  /**
   * Gets the total number of objects collected.
   *
   * @return total objects collected
   */
  public long getTotalCollected() {
    return totalCollected;
  }

  /**
   * Gets the total bytes allocated.
   *
   * @return total bytes allocated
   */
  public long getBytesAllocated() {
    return bytesAllocated;
  }

  /**
   * Gets the total bytes collected.
   *
   * @return total bytes collected
   */
  public long getBytesCollected() {
    return bytesCollected;
  }

  /**
   * Gets the number of minor collections performed.
   *
   * @return minor collection count
   */
  public long getMinorCollections() {
    return minorCollections;
  }

  /**
   * Gets the number of major collections performed.
   *
   * @return major collection count
   */
  public long getMajorCollections() {
    return majorCollections;
  }

  /**
   * Gets the total number of collections performed.
   *
   * @return total collection count
   */
  public long getTotalCollections() {
    return minorCollections + majorCollections;
  }

  /**
   * Gets the total time spent in garbage collection.
   *
   * @return total GC time
   */
  public Duration getTotalGcTime() {
    return totalGcTime;
  }

  /**
   * Gets the current heap size in bytes.
   *
   * @return current heap size
   */
  public int getCurrentHeapSize() {
    return currentHeapSize;
  }

  /**
   * Gets the peak heap size in bytes.
   *
   * @return peak heap size
   */
  public int getPeakHeapSize() {
    return peakHeapSize;
  }

  /**
   * Gets the maximum configured heap size in bytes.
   *
   * @return maximum heap size
   */
  public int getMaxHeapSize() {
    return maxHeapSize;
  }

  /**
   * Gets the heap utilization as a percentage.
   *
   * @return heap utilization (0.0 to 100.0)
   */
  public double getHeapUtilization() {
    if (maxHeapSize == 0) {
      return 0.0;
    }
    return (currentHeapSize * 100.0) / maxHeapSize;
  }

  /**
   * Gets the collection efficiency as a percentage.
   *
   * @return collection efficiency (0.0 to 100.0)
   */
  public double getCollectionEfficiency() {
    if (bytesAllocated == 0) {
      return 100.0;
    }
    return (bytesCollected * 100.0) / bytesAllocated;
  }

  /**
   * Gets the allocation rate in bytes per second.
   *
   * @return allocation rate, or 0 if no time has elapsed
   */
  public double getAllocationRate() {
    final Duration elapsed = Duration.between(captureTime.minus(totalGcTime), captureTime);
    if (elapsed.isZero() || elapsed.isNegative()) {
      return 0.0;
    }
    return (double) bytesAllocated / elapsed.toNanos() * 1_000_000_000.0;
  }

  /**
   * Gets the GC overhead as a percentage of total time.
   *
   * @return GC overhead percentage
   */
  public double getGcOverhead() {
    final Duration elapsed = Duration.between(captureTime.minus(totalGcTime), captureTime);
    if (elapsed.isZero() || elapsed.isNegative()) {
      return 0.0;
    }
    return (totalGcTime.toNanos() * 100.0) / elapsed.toNanos();
  }

  /**
   * Gets the number of live objects.
   *
   * @return live object count
   */
  public long getLiveObjects() {
    return totalAllocated - totalCollected;
  }

  /**
   * Gets the objects by generation.
   *
   * @return unmodifiable map of generation to object count
   */
  public Map<Integer, Long> getObjectsByGeneration() {
    return objectsByGeneration;
  }

  /**
   * Gets the time when these statistics were captured.
   *
   * @return capture timestamp
   */
  public Instant getCaptureTime() {
    return captureTime;
  }

  /**
   * Checks if garbage collection has significant impact on performance.
   *
   * @return true if GC overhead exceeds 5%
   */
  public boolean hasSignificantGcImpact() {
    return getGcOverhead() > 5.0;
  }

  /**
   * Checks if heap utilization is high.
   *
   * @return true if heap utilization exceeds 80%
   */
  public boolean hasHighHeapUtilization() {
    return getHeapUtilization() > 80.0;
  }

  /**
   * Checks if allocation rate is high.
   *
   * @return true if allocation rate exceeds 100 MB/s
   */
  public boolean hasHighAllocationRate() {
    return getAllocationRate() > 100_000_000.0; // 100 MB/s
  }

  /**
   * Gets a human-readable summary of these statistics.
   *
   * @return summary string
   */
  public String getSummary() {
    return String.format(
        "GC Stats: %d/%d objects (%.1f%% collected), %.1f/%.1f MB heap (%.1f%% used), "
            + "%d collections (%.1fms total, %.1f%% overhead)",
        getLiveObjects(),
        totalAllocated,
        getCollectionEfficiency(),
        currentHeapSize / 1024.0 / 1024.0,
        maxHeapSize / 1024.0 / 1024.0,
        getHeapUtilization(),
        getTotalCollections(),
        totalGcTime.toNanos() / 1_000_000.0,
        getGcOverhead());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final GcStats gcStats = (GcStats) obj;
    return totalAllocated == gcStats.totalAllocated
        && totalCollected == gcStats.totalCollected
        && bytesAllocated == gcStats.bytesAllocated
        && bytesCollected == gcStats.bytesCollected
        && minorCollections == gcStats.minorCollections
        && majorCollections == gcStats.majorCollections
        && currentHeapSize == gcStats.currentHeapSize
        && peakHeapSize == gcStats.peakHeapSize
        && maxHeapSize == gcStats.maxHeapSize
        && Objects.equals(totalGcTime, gcStats.totalGcTime)
        && Objects.equals(objectsByGeneration, gcStats.objectsByGeneration)
        && Objects.equals(captureTime, gcStats.captureTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        totalAllocated,
        totalCollected,
        bytesAllocated,
        bytesCollected,
        minorCollections,
        majorCollections,
        totalGcTime,
        currentHeapSize,
        peakHeapSize,
        maxHeapSize,
        objectsByGeneration,
        captureTime);
  }

  @Override
  public String toString() {
    return getSummary();
  }

  /** Builder for creating GC statistics. */
  public static final class Builder {
    private long totalAllocated = 0;
    private long totalCollected = 0;
    private long bytesAllocated = 0;
    private long bytesCollected = 0;
    private long minorCollections = 0;
    private long majorCollections = 0;
    private Duration totalGcTime = Duration.ZERO;
    private int currentHeapSize = 0;
    private int peakHeapSize = 0;
    private int maxHeapSize = 0;
    private Map<Integer, Long> objectsByGeneration = new java.util.HashMap<>();
    private Instant captureTime = Instant.now();

    private Builder() {}

    public Builder totalAllocated(final long totalAllocated) {
      this.totalAllocated = totalAllocated;
      return this;
    }

    public Builder totalCollected(final long totalCollected) {
      this.totalCollected = totalCollected;
      return this;
    }

    public Builder bytesAllocated(final long bytesAllocated) {
      this.bytesAllocated = bytesAllocated;
      return this;
    }

    public Builder bytesCollected(final long bytesCollected) {
      this.bytesCollected = bytesCollected;
      return this;
    }

    public Builder minorCollections(final long minorCollections) {
      this.minorCollections = minorCollections;
      return this;
    }

    public Builder majorCollections(final long majorCollections) {
      this.majorCollections = majorCollections;
      return this;
    }

    public Builder totalGcTime(final Duration totalGcTime) {
      this.totalGcTime = Objects.requireNonNull(totalGcTime, "Total GC time cannot be null");
      return this;
    }

    public Builder currentHeapSize(final int currentHeapSize) {
      this.currentHeapSize = currentHeapSize;
      return this;
    }

    public Builder peakHeapSize(final int peakHeapSize) {
      this.peakHeapSize = peakHeapSize;
      return this;
    }

    public Builder maxHeapSize(final int maxHeapSize) {
      this.maxHeapSize = maxHeapSize;
      return this;
    }

    /**
     * Sets the objects by generation map.
     *
     * @param objectsByGeneration the objects by generation map
     * @return this builder instance
     */
    public Builder objectsByGeneration(final Map<Integer, Long> objectsByGeneration) {
      this.objectsByGeneration =
          new java.util.HashMap<>(
              Objects.requireNonNull(objectsByGeneration, "Objects by generation cannot be null"));
      return this;
    }

    public Builder captureTime(final Instant captureTime) {
      this.captureTime = Objects.requireNonNull(captureTime, "Capture time cannot be null");
      return this;
    }

    public GcStats build() {
      return new GcStats(this);
    }
  }
}
