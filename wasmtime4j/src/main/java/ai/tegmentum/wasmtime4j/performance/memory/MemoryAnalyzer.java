package ai.tegmentum.wasmtime4j.performance.memory;

import ai.tegmentum.wasmtime4j.performance.GcImpactMetrics;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Comprehensive memory usage and garbage collection impact analyzer for WebAssembly operations.
 *
 * <p>This analyzer provides detailed insights into memory allocation patterns, garbage collection
 * behavior, and their impact on WebAssembly performance. It combines real-time monitoring with
 * historical analysis to identify optimization opportunities.
 *
 * <p>Key features include:
 *
 * <ul>
 *   <li>Real-time memory usage tracking
 *   <li>Garbage collection impact measurement
 *   <li>Memory leak detection and analysis
 *   <li>Allocation pattern identification
 *   <li>GC tuning recommendations
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * MemoryAnalyzer analyzer = MemoryAnalyzer.create();
 *
 * // Start monitoring
 * analyzer.startMonitoring(Duration.ofSeconds(1));
 *
 * // Mark start of WebAssembly operation
 * MemoryAnalysisSession session = analyzer.startSession("wasmtime-execution");
 *
 * // ... perform WebAssembly operations ...
 *
 * // Analyze memory impact
 * MemoryAnalysisResult result = session.complete();
 * System.out.println("Memory impact: " + result.getSummary());
 *
 * // Get recommendations
 * List<String> recommendations = result.getOptimizationRecommendations();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class MemoryAnalyzer {
  private static final Logger LOGGER = Logger.getLogger(MemoryAnalyzer.class.getName());

  private static final int DEFAULT_HISTORY_SIZE = 1000;
  private static final Duration DEFAULT_SAMPLING_INTERVAL = Duration.ofSeconds(1);

  private final MemoryMXBean memoryBean;
  private final List<MemoryPoolMXBean> memoryPools;
  private final ScheduledExecutorService scheduler;
  private final Map<String, MemoryAnalysisSession> activeSessions;
  private final CircularBuffer<MemorySnapshot> memoryHistory;
  private final boolean autoGcDetection;

  private volatile boolean monitoring;
  private volatile Duration samplingInterval;

  private MemoryAnalyzer(final Builder builder) {
    this.memoryBean = ManagementFactory.getMemoryMXBean();
    this.memoryPools = ManagementFactory.getMemoryPoolMXBeans();
    this.scheduler =
        Executors.newScheduledThreadPool(
            1,
            r -> {
              final Thread t = new Thread(r, "MemoryAnalyzer-Sampler");
              t.setDaemon(true);
              return t;
            });
    this.activeSessions = new ConcurrentHashMap<>();
    this.memoryHistory = new CircularBuffer<>(builder.historySize);
    this.autoGcDetection = builder.autoGcDetection;
    this.monitoring = false;
    this.samplingInterval = builder.samplingInterval;
  }

  /**
   * Creates a new memory analyzer with default configuration.
   *
   * @return memory analyzer instance
   */
  public static MemoryAnalyzer create() {
    return builder().build();
  }

  /**
   * Creates a builder for customizing memory analyzer configuration.
   *
   * @return builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Starts memory monitoring with the configured sampling interval. */
  public void startMonitoring() {
    startMonitoring(samplingInterval);
  }

  /**
   * Starts memory monitoring with a specific sampling interval.
   *
   * @param interval sampling interval
   */
  public void startMonitoring(final Duration interval) {
    Objects.requireNonNull(interval, "interval cannot be null");

    if (monitoring) {
      LOGGER.warning("Memory monitoring is already active");
      return;
    }

    this.samplingInterval = interval;
    this.monitoring = true;

    scheduler.scheduleAtFixedRate(
        this::captureMemorySnapshot, 0, interval.toMillis(), TimeUnit.MILLISECONDS);

    LOGGER.info("Started memory monitoring with interval: " + interval);
  }

  /** Stops memory monitoring. */
  public void stopMonitoring() {
    if (!monitoring) {
      return;
    }

    monitoring = false;
    scheduler.shutdown();

    LOGGER.info("Stopped memory monitoring");
  }

  /**
   * Checks if memory monitoring is currently active.
   *
   * @return true if monitoring is active
   */
  public boolean isMonitoring() {
    return monitoring;
  }

  /**
   * Starts a new memory analysis session for tracking specific operations.
   *
   * @param sessionName name for the session
   * @return analysis session
   */
  public MemoryAnalysisSession startSession(final String sessionName) {
    Objects.requireNonNull(sessionName, "sessionName cannot be null");

    if (activeSessions.containsKey(sessionName)) {
      throw new IllegalArgumentException("Session already exists: " + sessionName);
    }

    final MemoryAnalysisSession session = new MemoryAnalysisSession(sessionName, this);
    activeSessions.put(sessionName, session);

    LOGGER.info("Started memory analysis session: " + sessionName);
    return session;
  }

  /**
   * Gets the current memory usage metrics.
   *
   * @return current memory metrics
   */
  public MemoryMetrics getCurrentMemoryMetrics() {
    final MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    final MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

    final Map<String, Long> poolUsages = new HashMap<>();
    for (final MemoryPoolMXBean pool : memoryPools) {
      final MemoryUsage usage = pool.getUsage();
      if (usage != null) {
        poolUsages.put(pool.getName(), usage.getUsed());
      }
    }

    return new MemoryMetrics(
        heapUsage.getUsed(),
        heapUsage.getMax(),
        nonHeapUsage.getUsed(),
        nonHeapUsage.getMax(),
        poolUsages,
        Instant.now());
  }

  /**
   * Gets historical memory usage data.
   *
   * @return list of memory snapshots
   */
  public List<MemorySnapshot> getMemoryHistory() {
    return memoryHistory.getAll();
  }

  /**
   * Analyzes memory usage patterns over time.
   *
   * @return memory pattern analysis
   */
  public MemoryPatternAnalysis analyzeMemoryPatterns() {
    final List<MemorySnapshot> history = getMemoryHistory();
    if (history.size() < 10) {
      return MemoryPatternAnalysis.insufficient(history.size());
    }

    return analyzePatterns(history);
  }

  /**
   * Detects potential memory leaks based on usage patterns.
   *
   * @return memory leak analysis
   */
  public MemoryLeakAnalysis detectMemoryLeaks() {
    final List<MemorySnapshot> history = getMemoryHistory();
    if (history.size() < 50) {
      return MemoryLeakAnalysis.insufficient(history.size());
    }

    return analyzeForLeaks(history);
  }

  /**
   * Provides recommendations for garbage collection tuning.
   *
   * @return GC tuning recommendations
   */
  public GcTuningRecommendations getGcTuningRecommendations() {
    final List<MemorySnapshot> history = getMemoryHistory();
    final GcImpactMetrics.Snapshot currentGc = GcImpactMetrics.captureSnapshot();

    return analyzeGcTuning(history, currentGc);
  }

  /**
   * Gets active memory analysis sessions.
   *
   * @return map of active sessions
   */
  public Map<String, MemoryAnalysisSession> getActiveSessions() {
    return new HashMap<>(activeSessions);
  }

  /** Closes the memory analyzer and releases resources. */
  public void close() {
    stopMonitoring();

    // Complete any active sessions
    for (final MemoryAnalysisSession session : new ArrayList<>(activeSessions.values())) {
      try {
        session.complete();
      } catch (final Exception e) {
        LOGGER.warning(
            "Failed to complete session: " + session.getSessionName() + " - " + e.getMessage());
      }
    }

    try {
      scheduler.awaitTermination(5, TimeUnit.SECONDS);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  void removeSession(final String sessionName) {
    activeSessions.remove(sessionName);
  }

  private void captureMemorySnapshot() {
    try {
      final MemoryMetrics metrics = getCurrentMemoryMetrics();
      final GcImpactMetrics.Snapshot gcSnapshot = GcImpactMetrics.captureSnapshot();

      final MemorySnapshot snapshot = new MemorySnapshot(metrics, gcSnapshot);
      memoryHistory.add(snapshot);

      // Notify active sessions
      for (final MemoryAnalysisSession session : activeSessions.values()) {
        session.addSnapshot(snapshot);
      }

    } catch (final Exception e) {
      LOGGER.warning("Failed to capture memory snapshot: " + e.getMessage());
    }
  }

  private MemoryPatternAnalysis analyzePatterns(final List<MemorySnapshot> history) {
    // Analyze memory usage trends
    final List<Long> heapUsages =
        history.stream().map(s -> s.getMemoryMetrics().getHeapUsed()).toList();

    final TrendAnalysis heapTrend = analyzeTrend(heapUsages);
    final long peakHeapUsage = heapUsages.stream().mapToLong(Long::longValue).max().orElse(0);
    final double averageHeapUsage =
        heapUsages.stream().mapToLong(Long::longValue).average().orElse(0);

    // Analyze allocation patterns
    final AllocationPattern allocationPattern = analyzeAllocationPattern(history);

    // Analyze GC patterns
    final GcPattern gcPattern = analyzeGcPattern(history);

    return new MemoryPatternAnalysis(
        heapTrend, peakHeapUsage, averageHeapUsage, allocationPattern, gcPattern, Instant.now());
  }

  private MemoryLeakAnalysis analyzeForLeaks(final List<MemorySnapshot> history) {
    // Simple leak detection based on steadily increasing memory usage
    final List<Long> heapUsages =
        history.stream().map(s -> s.getMemoryMetrics().getHeapUsed()).toList();

    // Calculate slope of memory usage over time
    final double slope = calculateSlope(heapUsages);
    final boolean potentialLeak = slope > 0.1; // Threshold for leak detection

    final List<String> suspiciousPatterns = new ArrayList<>();
    if (potentialLeak) {
      suspiciousPatterns.add("Steadily increasing heap usage detected");
    }

    // Check for non-heap memory growth
    final List<Long> nonHeapUsages =
        history.stream().map(s -> s.getMemoryMetrics().getNonHeapUsed()).toList();

    final double nonHeapSlope = calculateSlope(nonHeapUsages);
    if (nonHeapSlope > 0.05) {
      suspiciousPatterns.add("Non-heap memory growth detected");
    }

    return new MemoryLeakAnalysis(
        potentialLeak, slope, suspiciousPatterns, history.size(), Instant.now());
  }

  private GcTuningRecommendations analyzeGcTuning(
      final List<MemorySnapshot> history, final GcImpactMetrics.Snapshot currentGc) {
    final List<String> recommendations = new ArrayList<>();

    // Analyze GC frequency
    final long totalGcCollections = currentGc.getTotalCollections();
    final Duration totalGcTime = currentGc.getTotalGcTime();

    if (totalGcTime.toMillis() > 0) {
      final double gcOverhead =
          (totalGcTime.toNanos() / (double) Duration.ofMinutes(1).toNanos()) * 100;

      if (gcOverhead > 10.0) {
        recommendations.add(
            "High GC overhead detected ("
                + String.format("%.1f", gcOverhead)
                + "%). Consider increasing heap size.");
      }

      if (totalGcCollections > 1000) {
        recommendations.add(
            "Frequent GC collections detected ("
                + totalGcCollections
                + "). Consider GC algorithm tuning.");
      }
    }

    // Analyze heap utilization
    if (!history.isEmpty()) {
      final MemorySnapshot latest = history.get(history.size() - 1);
      final double heapUtilization =
          (double) latest.getMemoryMetrics().getHeapUsed()
              / latest.getMemoryMetrics().getMaxHeap()
              * 100;

      if (heapUtilization > 90) {
        recommendations.add(
            "High heap utilization ("
                + String.format("%.1f", heapUtilization)
                + "%). Consider increasing heap size.");
      } else if (heapUtilization < 30) {
        recommendations.add(
            "Low heap utilization ("
                + String.format("%.1f", heapUtilization)
                + "%). Consider reducing heap size.");
      }
    }

    if (recommendations.isEmpty()) {
      recommendations.add("GC configuration appears optimal for current workload.");
    }

    return new GcTuningRecommendations(recommendations, currentGc, Instant.now());
  }

  private TrendAnalysis analyzeTrend(final List<Long> values) {
    if (values.size() < 2) {
      return TrendAnalysis.STABLE;
    }

    final double slope = calculateSlope(values);

    if (slope > 0.1) {
      return TrendAnalysis.INCREASING;
    } else if (slope < -0.1) {
      return TrendAnalysis.DECREASING;
    } else {
      return TrendAnalysis.STABLE;
    }
  }

  private double calculateSlope(final List<Long> values) {
    if (values.size() < 2) {
      return 0.0;
    }

    // Simple linear regression slope calculation
    final double n = values.size();
    double sumX = 0;
    double sumY = 0;
    double sumXY = 0;
    double sumXX = 0;

    for (int i = 0; i < values.size(); i++) {
      final double x = i;
      final double y = values.get(i);
      sumX += x;
      sumY += y;
      sumXY += x * y;
      sumXX += x * x;
    }

    final double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
    return slope / 1024 / 1024; // Normalize to MB per sample
  }

  private AllocationPattern analyzeAllocationPattern(final List<MemorySnapshot> history) {
    // Simplified allocation pattern analysis
    final List<Long> allocations = new ArrayList<>();
    for (int i = 1; i < history.size(); i++) {
      final long current = history.get(i).getMemoryMetrics().getHeapUsed();
      final long previous = history.get(i - 1).getMemoryMetrics().getHeapUsed();
      if (current > previous) {
        allocations.add(current - previous);
      }
    }

    if (allocations.isEmpty()) {
      return AllocationPattern.STABLE;
    }

    final double averageAllocation =
        allocations.stream().mapToLong(Long::longValue).average().orElse(0);
    final long maxAllocation = allocations.stream().mapToLong(Long::longValue).max().orElse(0);

    if (maxAllocation > averageAllocation * 10) {
      return AllocationPattern.SPIKY;
    } else if (averageAllocation > 10 * 1024 * 1024) {
      return AllocationPattern.HIGH_RATE;
    } else {
      return AllocationPattern.NORMAL;
    }
  }

  private GcPattern analyzeGcPattern(final List<MemorySnapshot> history) {
    // Simplified GC pattern analysis
    if (history.size() < 10) {
      return GcPattern.INSUFFICIENT_DATA;
    }

    final List<Long> gcCounts =
        history.stream().map(s -> s.getGcSnapshot().getTotalCollections()).toList();

    final long totalGcIncrease = gcCounts.get(gcCounts.size() - 1) - gcCounts.get(0);
    final double gcRate = (double) totalGcIncrease / history.size();

    if (gcRate > 1.0) {
      return GcPattern.FREQUENT;
    } else if (gcRate > 0.1) {
      return GcPattern.NORMAL;
    } else {
      return GcPattern.INFREQUENT;
    }
  }

  /** Builder for MemoryAnalyzer. */
  public static final class Builder {
    private Duration samplingInterval = DEFAULT_SAMPLING_INTERVAL;
    private int historySize = DEFAULT_HISTORY_SIZE;
    private boolean autoGcDetection = true;

    public Builder samplingInterval(final Duration interval) {
      this.samplingInterval = interval;
      return this;
    }

    public Builder historySize(final int size) {
      this.historySize = size;
      return this;
    }

    public Builder autoGcDetection(final boolean enable) {
      this.autoGcDetection = enable;
      return this;
    }

    public MemoryAnalyzer build() {
      return new MemoryAnalyzer(this);
    }
  }

  /** Represents current memory metrics. */
  public static final class MemoryMetrics {
    private final long heapUsed;
    private final long maxHeap;
    private final long nonHeapUsed;
    private final long maxNonHeap;
    private final Map<String, Long> poolUsages;
    private final Instant timestamp;

    public MemoryMetrics(
        final long heapUsed,
        final long maxHeap,
        final long nonHeapUsed,
        final long maxNonHeap,
        final Map<String, Long> poolUsages,
        final Instant timestamp) {
      this.heapUsed = heapUsed;
      this.maxHeap = maxHeap;
      this.nonHeapUsed = nonHeapUsed;
      this.maxNonHeap = maxNonHeap;
      this.poolUsages = Map.copyOf(poolUsages);
      this.timestamp = timestamp;
    }

    public long getHeapUsed() {
      return heapUsed;
    }

    public long getMaxHeap() {
      return maxHeap;
    }

    public long getNonHeapUsed() {
      return nonHeapUsed;
    }

    public long getMaxNonHeap() {
      return maxNonHeap;
    }

    public Map<String, Long> getPoolUsages() {
      return poolUsages;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public double getHeapUtilization() {
      return maxHeap > 0 ? (double) heapUsed / maxHeap * 100 : 0;
    }
  }

  /** Memory snapshot combining memory metrics and GC state. */
  public static final class MemorySnapshot {
    private final MemoryMetrics memoryMetrics;
    private final GcImpactMetrics.Snapshot gcSnapshot;

    public MemorySnapshot(
        final MemoryMetrics memoryMetrics, final GcImpactMetrics.Snapshot gcSnapshot) {
      this.memoryMetrics = memoryMetrics;
      this.gcSnapshot = gcSnapshot;
    }

    public MemoryMetrics getMemoryMetrics() {
      return memoryMetrics;
    }

    public GcImpactMetrics.Snapshot getGcSnapshot() {
      return gcSnapshot;
    }
  }

  /** Circular buffer for memory history. */
  static final class CircularBuffer<T> {
    private final Object[] buffer;
    private final int capacity;
    private int head = 0;
    private int size = 0;

    CircularBuffer(final int capacity) {
      this.capacity = capacity;
      this.buffer = new Object[capacity];
    }

    synchronized void add(final T item) {
      buffer[head] = item;
      head = (head + 1) % capacity;
      if (size < capacity) {
        size++;
      }
    }

    @SuppressWarnings("unchecked")
    synchronized List<T> getAll() {
      final List<T> result = new ArrayList<>(size);
      final int start = size < capacity ? 0 : head;

      for (int i = 0; i < size; i++) {
        final int index = (start + i) % capacity;
        result.add((T) buffer[index]);
      }

      return result;
    }
  }

  // Enums and analysis result classes
  public enum TrendAnalysis {
    INCREASING,
    DECREASING,
    STABLE
  }

  public enum AllocationPattern {
    NORMAL,
    HIGH_RATE,
    SPIKY,
    STABLE
  }

  public enum GcPattern {
    FREQUENT,
    NORMAL,
    INFREQUENT,
    INSUFFICIENT_DATA
  }

  /** Memory pattern analysis result. */
  public static final class MemoryPatternAnalysis {
    private final TrendAnalysis heapTrend;
    private final long peakHeapUsage;
    private final double averageHeapUsage;
    private final AllocationPattern allocationPattern;
    private final GcPattern gcPattern;
    private final Instant analysisTime;

    public MemoryPatternAnalysis(
        final TrendAnalysis heapTrend,
        final long peakHeapUsage,
        final double averageHeapUsage,
        final AllocationPattern allocationPattern,
        final GcPattern gcPattern,
        final Instant analysisTime) {
      this.heapTrend = heapTrend;
      this.peakHeapUsage = peakHeapUsage;
      this.averageHeapUsage = averageHeapUsage;
      this.allocationPattern = allocationPattern;
      this.gcPattern = gcPattern;
      this.analysisTime = analysisTime;
    }

    public static MemoryPatternAnalysis insufficient(final int sampleCount) {
      return new MemoryPatternAnalysis(
          TrendAnalysis.STABLE,
          0,
          0,
          AllocationPattern.STABLE,
          GcPattern.INSUFFICIENT_DATA,
          Instant.now());
    }

    public TrendAnalysis getHeapTrend() {
      return heapTrend;
    }

    public long getPeakHeapUsage() {
      return peakHeapUsage;
    }

    public double getAverageHeapUsage() {
      return averageHeapUsage;
    }

    public AllocationPattern getAllocationPattern() {
      return allocationPattern;
    }

    public GcPattern getGcPattern() {
      return gcPattern;
    }

    public Instant getAnalysisTime() {
      return analysisTime;
    }
  }

  /** Memory leak analysis result. */
  public static final class MemoryLeakAnalysis {
    private final boolean potentialLeak;
    private final double growthRate;
    private final List<String> suspiciousPatterns;
    private final int sampleCount;
    private final Instant analysisTime;

    public MemoryLeakAnalysis(
        final boolean potentialLeak,
        final double growthRate,
        final List<String> suspiciousPatterns,
        final int sampleCount,
        final Instant analysisTime) {
      this.potentialLeak = potentialLeak;
      this.growthRate = growthRate;
      this.suspiciousPatterns = List.copyOf(suspiciousPatterns);
      this.sampleCount = sampleCount;
      this.analysisTime = analysisTime;
    }

    public static MemoryLeakAnalysis insufficient(final int sampleCount) {
      return new MemoryLeakAnalysis(
          false, 0, List.of("Insufficient data for analysis"), sampleCount, Instant.now());
    }

    public boolean isPotentialLeak() {
      return potentialLeak;
    }

    public double getGrowthRate() {
      return growthRate;
    }

    public List<String> getSuspiciousPatterns() {
      return suspiciousPatterns;
    }

    public int getSampleCount() {
      return sampleCount;
    }

    public Instant getAnalysisTime() {
      return analysisTime;
    }
  }

  /** GC tuning recommendations. */
  public static final class GcTuningRecommendations {
    private final List<String> recommendations;
    private final GcImpactMetrics.Snapshot gcSnapshot;
    private final Instant analysisTime;

    public GcTuningRecommendations(
        final List<String> recommendations,
        final GcImpactMetrics.Snapshot gcSnapshot,
        final Instant analysisTime) {
      this.recommendations = List.copyOf(recommendations);
      this.gcSnapshot = gcSnapshot;
      this.analysisTime = analysisTime;
    }

    public List<String> getRecommendations() {
      return recommendations;
    }

    public GcImpactMetrics.Snapshot getGcSnapshot() {
      return gcSnapshot;
    }

    public Instant getAnalysisTime() {
      return analysisTime;
    }
  }
}
