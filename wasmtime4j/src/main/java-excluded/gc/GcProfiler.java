package ai.tegmentum.wasmtime4j.gc;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Performance profiler for WebAssembly GC operations.
 *
 * <p>Provides detailed performance metrics and analysis for garbage collection operations,
 * object allocation, field access, and other GC-related activities.
 *
 * @since 1.0.0
 */
public interface GcProfiler {

  /**
   * Starts profiling GC operations.
   */
  void start();

  /**
   * Stops profiling and returns the results.
   *
   * @return profiling results
   */
  GcProfilingResults stop();

  /**
   * Checks if profiling is currently active.
   *
   * @return true if profiling is active
   */
  boolean isActive();

  /**
   * Gets the current profiling duration.
   *
   * @return profiling duration
   */
  Duration getProfilingDuration();

  /**
   * Records a custom profiling event.
   *
   * @param eventName the event name
   * @param duration the event duration
   * @param metadata additional event metadata
   */
  void recordEvent(String eventName, Duration duration, Map<String, Object> metadata);

  /**
   * Results of GC profiling.
   */
  interface GcProfilingResults {

    /**
     * Gets the total profiling duration.
     *
     * @return profiling duration
     */
    Duration getTotalDuration();

    /**
     * Gets the number of profiling samples collected.
     *
     * @return sample count
     */
    long getSampleCount();

    /**
     * Gets allocation performance statistics.
     *
     * @return allocation statistics
     */
    AllocationStatistics getAllocationStatistics();

    /**
     * Gets field access performance statistics.
     *
     * @return field access statistics
     */
    FieldAccessStatistics getFieldAccessStatistics();

    /**
     * Gets array access performance statistics.
     *
     * @return array access statistics
     */
    ArrayAccessStatistics getArrayAccessStatistics();

    /**
     * Gets reference operation performance statistics.
     *
     * @return reference operation statistics
     */
    ReferenceOperationStatistics getReferenceOperationStatistics();

    /**
     * Gets garbage collection performance statistics.
     *
     * @return GC statistics
     */
    GcPerformanceStatistics getGcPerformanceStatistics();

    /**
     * Gets type operation performance statistics.
     *
     * @return type operation statistics
     */
    TypeOperationStatistics getTypeOperationStatistics();

    /**
     * Gets performance hotspots and bottlenecks.
     *
     * @return list of performance hotspots
     */
    List<PerformanceHotspot> getHotspots();

    /**
     * Gets performance comparison with baseline measurements.
     *
     * @return performance comparison results
     */
    PerformanceComparison getBaselineComparison();

    /**
     * Gets detailed timeline of profiling events.
     *
     * @return profiling timeline
     */
    ProfilingTimeline getTimeline();
  }

  /**
   * Allocation performance statistics.
   */
  interface AllocationStatistics {
    /** Gets total allocation count. */
    long getTotalAllocations();

    /** Gets average allocation time. */
    Duration getAverageAllocationTime();

    /** Gets allocation time percentiles. */
    Map<Double, Duration> getAllocationTimePercentiles();

    /** Gets allocations by type. */
    Map<String, Long> getAllocationsByType();

    /** Gets allocation throughput (allocations per second). */
    double getAllocationThroughput();

    /** Gets allocation memory throughput (bytes per second). */
    double getMemoryThroughput();
  }

  /**
   * Field access performance statistics.
   */
  interface FieldAccessStatistics {
    /** Gets total field read count. */
    long getTotalFieldReads();

    /** Gets total field write count. */
    long getTotalFieldWrites();

    /** Gets average field read time. */
    Duration getAverageReadTime();

    /** Gets average field write time. */
    Duration getAverageWriteTime();

    /** Gets field access time percentiles. */
    Map<Double, Duration> getAccessTimePercentiles();

    /** Gets field access patterns. */
    Map<String, FieldAccessPattern> getAccessPatterns();
  }

  /**
   * Array access performance statistics.
   */
  interface ArrayAccessStatistics {
    /** Gets total array element reads. */
    long getTotalElementReads();

    /** Gets total array element writes. */
    long getTotalElementWrites();

    /** Gets average element read time. */
    Duration getAverageReadTime();

    /** Gets average element write time. */
    Duration getAverageWriteTime();

    /** Gets array access time percentiles. */
    Map<Double, Duration> getAccessTimePercentiles();

    /** Gets array operation throughput. */
    double getOperationThroughput();
  }

  /**
   * Reference operation performance statistics.
   */
  interface ReferenceOperationStatistics {
    /** Gets total ref.cast operations. */
    long getTotalCastOperations();

    /** Gets total ref.test operations. */
    long getTotalTestOperations();

    /** Gets average cast time. */
    Duration getAverageCastTime();

    /** Gets average test time. */
    Duration getAverageTestTime();

    /** Gets cast success rate. */
    double getCastSuccessRate();

    /** Gets type checking overhead. */
    Duration getTypeCheckingOverhead();
  }

  /**
   * GC performance statistics.
   */
  interface GcPerformanceStatistics {
    /** Gets total GC collections. */
    long getTotalCollections();

    /** Gets total GC pause time. */
    Duration getTotalPauseTime();

    /** Gets average GC pause time. */
    Duration getAveragePauseTime();

    /** Gets maximum GC pause time. */
    Duration getMaxPauseTime();

    /** Gets GC throughput (bytes collected per second). */
    double getGcThroughput();

    /** Gets collection efficiency (bytes collected per pause time). */
    double getCollectionEfficiency();
  }

  /**
   * Type operation performance statistics.
   */
  interface TypeOperationStatistics {
    /** Gets total type registrations. */
    long getTotalTypeRegistrations();

    /** Gets average type registration time. */
    Duration getAverageRegistrationTime();

    /** Gets type lookup performance. */
    Duration getAverageLookupTime();

    /** Gets type validation overhead. */
    Duration getValidationOverhead();
  }

  /**
   * A performance hotspot identified during profiling.
   */
  interface PerformanceHotspot {
    /** Gets the hotspot name. */
    String getName();

    /** Gets the hotspot type. */
    HotspotType getType();

    /** Gets the time spent in this hotspot. */
    Duration getTotalTime();

    /** Gets the percentage of total profiling time. */
    double getTimePercentage();

    /** Gets the number of occurrences. */
    long getOccurrenceCount();

    /** Gets the average time per occurrence. */
    Duration getAverageTime();

    /** Gets hotspot-specific details. */
    Map<String, Object> getDetails();
  }

  /**
   * Types of performance hotspots.
   */
  enum HotspotType {
    /** Allocation hotspot. */
    ALLOCATION,
    /** Field access hotspot. */
    FIELD_ACCESS,
    /** Array access hotspot. */
    ARRAY_ACCESS,
    /** Type checking hotspot. */
    TYPE_CHECKING,
    /** Garbage collection hotspot. */
    GARBAGE_COLLECTION,
    /** Reference operation hotspot. */
    REFERENCE_OPERATION
  }

  /**
   * Performance comparison with baseline measurements.
   */
  interface PerformanceComparison {
    /** Gets the baseline duration. */
    Duration getBaselineDuration();

    /** Gets the current duration. */
    Duration getCurrentDuration();

    /** Gets the performance change ratio. */
    double getChangeRatio();

    /** Gets whether performance improved. */
    boolean isImproved();

    /** Gets detailed comparison by operation type. */
    Map<String, OperationComparison> getOperationComparisons();

    /** Gets regression analysis. */
    RegressionAnalysis getRegressionAnalysis();
  }

  /**
   * Comparison for a specific operation type.
   */
  interface OperationComparison {
    /** Gets the operation name. */
    String getOperationName();

    /** Gets the baseline average time. */
    Duration getBaselineAverage();

    /** Gets the current average time. */
    Duration getCurrentAverage();

    /** Gets the change percentage. */
    double getChangePercentage();

    /** Gets statistical significance. */
    boolean isSignificant();
  }

  /**
   * Performance regression analysis.
   */
  interface RegressionAnalysis {
    /** Gets whether a performance regression was detected. */
    boolean hasRegression();

    /** Gets the regression severity. */
    RegressionSeverity getSeverity();

    /** Gets the operations that regressed. */
    List<String> getRegressedOperations();

    /** Gets recommendations for addressing regressions. */
    List<String> getRecommendations();
  }

  /**
   * Performance regression severity levels.
   */
  enum RegressionSeverity {
    /** Minor performance regression. */
    MINOR,
    /** Moderate performance regression. */
    MODERATE,
    /** Major performance regression. */
    MAJOR,
    /** Critical performance regression. */
    CRITICAL
  }

  /**
   * Timeline of profiling events.
   */
  interface ProfilingTimeline {
    /** Gets all profiling events in chronological order. */
    List<ProfilingEvent> getEvents();

    /** Gets events within a specific time range. */
    List<ProfilingEvent> getEvents(Instant start, Instant end);

    /** Gets the timeline sampling interval. */
    Duration getSamplingInterval();
  }

  /**
   * A single profiling event.
   */
  interface ProfilingEvent {
    /** Gets the event timestamp. */
    Instant getTimestamp();

    /** Gets the event type. */
    String getEventType();

    /** Gets the event duration. */
    Duration getDuration();

    /** Gets the thread ID. */
    long getThreadId();

    /** Gets event metadata. */
    Map<String, Object> getMetadata();
  }

  /**
   * Field access pattern information.
   */
  interface FieldAccessPattern {
    /** Gets the field name or index. */
    String getFieldIdentifier();

    /** Gets the access frequency. */
    long getAccessCount();

    /** Gets the read/write ratio. */
    double getReadWriteRatio();

    /** Gets temporal locality information. */
    double getTemporalLocality();

    /** Gets spatial locality information. */
    double getSpatialLocality();
  }
}