package ai.tegmentum.wasmtime4j.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.ref.WeakReference;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Advanced memory monitoring utility for detailed memory usage analysis and leak detection.
 *
 * <p>This monitor provides comprehensive memory tracking capabilities including: - Detailed heap
 * and non-heap memory monitoring - Memory pool-specific analysis - Leak detection and alerting -
 * Memory pressure detection - Historical trend analysis
 */
public final class AdvancedMemoryMonitor {

  private static final Logger LOGGER = Logger.getLogger(AdvancedMemoryMonitor.class.getName());

  // Memory thresholds for alerting
  private static final double MEMORY_PRESSURE_THRESHOLD = 0.85; // 85%
  private static final double MEMORY_CRITICAL_THRESHOLD = 0.95; // 95%
  private static final long LEAK_DETECTION_THRESHOLD_MB = 50; // 50MB growth per minute

  private final MemoryMXBean memoryBean;
  private final List<MemoryPoolMXBean> memoryPoolBeans;
  private final List<DetailedMemorySnapshot> snapshots;
  private final ConcurrentMap<String, MemoryPoolTracker> poolTrackers;
  private final List<WeakReference<Object>> leakDetectionReferences;
  private final AtomicLong totalAllocatedObjects = new AtomicLong(0);
  private final AtomicLong suspectedLeaks = new AtomicLong(0);
  private final AtomicBoolean monitoring = new AtomicBoolean(false);

  private volatile Instant monitoringStartTime = null;
  private volatile String currentTestName = "unknown";
  private ScheduledFuture<?> scheduledMonitoring = null;
  private long baselineHeapUsage = 0;
  private long peakHeapUsage = 0;

  /** Creates a new advanced memory monitor. */
  public AdvancedMemoryMonitor() {
    this.memoryBean = ManagementFactory.getMemoryMXBean();
    this.memoryPoolBeans = ManagementFactory.getMemoryPoolMXBeans();
    this.snapshots = Collections.synchronizedList(new ArrayList<>());
    this.poolTrackers = new ConcurrentHashMap<>();
    this.leakDetectionReferences = Collections.synchronizedList(new ArrayList<>());

    initializePoolTrackers();
  }

  /**
   * Starts memory monitoring for the specified test.
   *
   * @param testName the name of the test being monitored
   */
  public void startMonitoring(final String testName) {
    if (monitoring.compareAndSet(false, true)) {
      this.currentTestName = testName;
      this.monitoringStartTime = Instant.now();

      // Establish baseline
      establishBaseline();

      // Take initial snapshot
      captureDetailedSnapshot();

      LOGGER.info("Started advanced memory monitoring for: " + testName);
    } else {
      LOGGER.warning("Memory monitoring already active, ignoring start request for: " + testName);
    }
  }

  /**
   * Starts periodic memory monitoring.
   *
   * @param testName the name of the test being monitored
   * @param intervalMs monitoring interval in milliseconds
   * @param executor executor service for scheduling
   */
  public void startPeriodicMonitoring(
      final String testName, final long intervalMs, final ScheduledExecutorService executor) {
    startMonitoring(testName);

    scheduledMonitoring =
        executor.scheduleAtFixedRate(
            this::performPeriodicMonitoring, intervalMs, intervalMs, TimeUnit.MILLISECONDS);

    LOGGER.info("Started periodic memory monitoring (interval: " + intervalMs + "ms)");
  }

  /** Stops memory monitoring. */
  public void stopMonitoring() {
    if (monitoring.compareAndSet(true, false)) {
      if (scheduledMonitoring != null && !scheduledMonitoring.isCancelled()) {
        scheduledMonitoring.cancel(false);
        scheduledMonitoring = null;
      }

      // Take final snapshot
      captureDetailedSnapshot();

      // Perform final leak detection
      performLeakDetection();

      final Duration monitoringDuration =
          monitoringStartTime != null
              ? Duration.between(monitoringStartTime, Instant.now())
              : Duration.ZERO;

      LOGGER.info(
          "Stopped memory monitoring for: "
              + currentTestName
              + " (duration: "
              + monitoringDuration.toMillis()
              + "ms)");

      // Generate final report
      final MemoryMonitoringReport report = generateReport();
      if (report.hasMemoryLeaks() || report.hasMemoryPressure()) {
        LOGGER.warning("Memory issues detected: " + report.getSummary());
      }
    }
  }

  /**
   * Tracks an object for leak detection.
   *
   * @param object the object to track
   * @param description description of the object for debugging
   */
  public void trackObjectForLeakDetection(final Object object, final String description) {
    if (monitoring.get()) {
      leakDetectionReferences.add(new WeakReference<>(object));
      totalAllocatedObjects.incrementAndGet();
    }
  }

  /**
   * Forces a memory snapshot capture.
   *
   * @return the captured snapshot
   */
  public DetailedMemorySnapshot forceSnapshot() {
    return captureDetailedSnapshot();
  }

  /**
   * Checks current memory pressure level.
   *
   * @return memory pressure information
   */
  public MemoryPressureInfo checkMemoryPressure() {
    final MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    final double heapUtilization = (double) heapUsage.getUsed() / heapUsage.getCommitted();
    final double maxUtilization =
        heapUsage.getMax() > 0 ? (double) heapUsage.getUsed() / heapUsage.getMax() : 0.0;

    final MemoryPressureLevel level;
    if (maxUtilization > MEMORY_CRITICAL_THRESHOLD || heapUtilization > MEMORY_CRITICAL_THRESHOLD) {
      level = MemoryPressureLevel.CRITICAL;
    } else if (maxUtilization > MEMORY_PRESSURE_THRESHOLD
        || heapUtilization > MEMORY_PRESSURE_THRESHOLD) {
      level = MemoryPressureLevel.HIGH;
    } else if (heapUtilization > 0.6) {
      level = MemoryPressureLevel.MODERATE;
    } else {
      level = MemoryPressureLevel.LOW;
    }

    return new MemoryPressureInfo(level, heapUtilization, maxUtilization);
  }

  /**
   * Analyzes memory usage trends.
   *
   * @return memory trend analysis
   */
  public MemoryTrendAnalysis analyzeMemoryTrends() {
    if (snapshots.size() < 3) {
      return new MemoryTrendAnalysis("Insufficient data for trend analysis");
    }

    final List<DetailedMemorySnapshot> sortedSnapshots = new ArrayList<>(snapshots);
    sortedSnapshots.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));

    return performTrendAnalysis(sortedSnapshots);
  }

  /**
   * Generates a comprehensive memory monitoring report.
   *
   * @return detailed memory monitoring report
   */
  public MemoryMonitoringReport generateReport() {
    final Duration monitoringDuration =
        monitoringStartTime != null
            ? Duration.between(monitoringStartTime, Instant.now())
            : Duration.ZERO;

    final MemoryTrendAnalysis trendAnalysis = analyzeMemoryTrends();
    final MemoryPressureInfo currentPressure = checkMemoryPressure();
    final List<MemoryPoolAnalysis> poolAnalyses = analyzeMemoryPools();

    return new MemoryMonitoringReport(
        currentTestName,
        monitoringDuration,
        baselineHeapUsage,
        peakHeapUsage,
        getCurrentHeapUsage(),
        suspectedLeaks.get(),
        snapshots.size(),
        trendAnalysis,
        currentPressure,
        poolAnalyses);
  }

  /** Resets all monitoring data. */
  public void reset() {
    snapshots.clear();
    leakDetectionReferences.clear();
    poolTrackers.clear();
    totalAllocatedObjects.set(0);
    suspectedLeaks.set(0);
    baselineHeapUsage = 0;
    peakHeapUsage = 0;
    monitoringStartTime = null;
    currentTestName = "unknown";

    initializePoolTrackers();
    LOGGER.info("Memory monitor reset");
  }

  /** Initializes memory pool trackers. */
  private void initializePoolTrackers() {
    for (final MemoryPoolMXBean poolBean : memoryPoolBeans) {
      poolTrackers.put(poolBean.getName(), new MemoryPoolTracker(poolBean));
    }
  }

  /** Establishes memory usage baseline. */
  private void establishBaseline() {
    // Force garbage collection to get clean baseline
    System.gc();
    try {
      Thread.sleep(200);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    baselineHeapUsage = getCurrentHeapUsage();
    peakHeapUsage = baselineHeapUsage;
    LOGGER.info("Established memory baseline: " + (baselineHeapUsage / 1024 / 1024) + " MB");
  }

  /** Captures a detailed memory snapshot. */
  private DetailedMemorySnapshot captureDetailedSnapshot() {
    final long timestamp = System.currentTimeMillis();
    final MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    final MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

    // Update peak usage
    peakHeapUsage = Math.max(peakHeapUsage, heapUsage.getUsed());

    // Capture memory pool states
    final ConcurrentMap<String, MemoryPoolState> poolStates = new ConcurrentHashMap<>();
    for (final MemoryPoolMXBean poolBean : memoryPoolBeans) {
      final MemoryUsage poolUsage = poolBean.getUsage();
      poolStates.put(
          poolBean.getName(),
          new MemoryPoolState(
              poolBean.getName(),
              poolUsage.getUsed(),
              poolUsage.getCommitted(),
              poolUsage.getMax()));
    }

    final DetailedMemorySnapshot snapshot =
        new DetailedMemorySnapshot(
            timestamp, heapUsage, nonHeapUsage, poolStates, totalAllocatedObjects.get());

    snapshots.add(snapshot);

    // Check for memory pressure
    final MemoryPressureInfo pressure = checkMemoryPressure();
    if (pressure.level == MemoryPressureLevel.HIGH
        || pressure.level == MemoryPressureLevel.CRITICAL) {
      LOGGER.warning(
          "Memory pressure detected: "
              + pressure.level
              + " (heap: "
              + String.format("%.1f%%", pressure.heapUtilization * 100)
              + ")");
    }

    return snapshot;
  }

  /** Performs periodic monitoring tasks. */
  private void performPeriodicMonitoring() {
    try {
      captureDetailedSnapshot();
      performLeakDetection();
      updatePoolTrackers();
    } catch (final Exception e) {
      LOGGER.warning("Error during periodic memory monitoring: " + e.getMessage());
    }
  }

  /** Performs leak detection using weak references. */
  private void performLeakDetection() {
    int collectedReferences = 0;
    final List<WeakReference<Object>> toRemove = new ArrayList<>();

    for (final WeakReference<Object> ref : leakDetectionReferences) {
      if (ref.get() == null) {
        collectedReferences++;
        toRemove.add(ref);
      }
    }

    leakDetectionReferences.removeAll(toRemove);

    final long activeReferences = leakDetectionReferences.size();
    final long totalAllocated = totalAllocatedObjects.get();

    if (totalAllocated > 0) {
      final double leakRatio = (double) activeReferences / totalAllocated;
      if (leakRatio > 0.1) { // More than 10% of objects not collected
        suspectedLeaks.addAndGet(activeReferences);
        LOGGER.warning(
            "Potential memory leaks detected: "
                + activeReferences
                + " objects not collected ("
                + String.format("%.1f%%", leakRatio * 100)
                + ")");
      }
    }

    if (collectedReferences > 0) {
      LOGGER.fine(
          "Collected "
              + collectedReferences
              + " references, "
              + activeReferences
              + " still active");
    }
  }

  /** Updates memory pool trackers. */
  private void updatePoolTrackers() {
    for (final MemoryPoolTracker tracker : poolTrackers.values()) {
      tracker.update();
    }
  }

  /** Performs trend analysis on memory snapshots. */
  private MemoryTrendAnalysis performTrendAnalysis(
      final List<DetailedMemorySnapshot> sortedSnapshots) {
    final DetailedMemorySnapshot first = sortedSnapshots.get(0);
    final DetailedMemorySnapshot last = sortedSnapshots.get(sortedSnapshots.size() - 1);

    final long durationMs = last.timestamp - first.timestamp;
    final long heapGrowth = last.heapUsage.getUsed() - first.heapUsage.getUsed();
    final long nonHeapGrowth = last.nonHeapUsage.getUsed() - first.nonHeapUsage.getUsed();

    // Calculate growth rates (bytes per minute)
    final double durationMinutes = durationMs / 60000.0;
    final double heapGrowthRate = durationMinutes > 0 ? heapGrowth / durationMinutes : 0.0;
    final double nonHeapGrowthRate = durationMinutes > 0 ? nonHeapGrowth / durationMinutes : 0.0;

    // Detect memory leak indicators
    final boolean potentialHeapLeak = heapGrowthRate > (LEAK_DETECTION_THRESHOLD_MB * 1024 * 1024);
    final boolean potentialNonHeapLeak =
        nonHeapGrowthRate > (LEAK_DETECTION_THRESHOLD_MB * 1024 * 1024 * 0.5);

    // Calculate memory stability (coefficient of variation)
    final double avgHeapUsed =
        sortedSnapshots.stream().mapToLong(s -> s.heapUsage.getUsed()).average().orElse(0.0);

    final double heapVariance =
        sortedSnapshots.stream()
            .mapToDouble(s -> Math.pow(s.heapUsage.getUsed() - avgHeapUsed, 2))
            .average()
            .orElse(0.0);

    final double heapStability =
        avgHeapUsed > 0 ? 1.0 - (Math.sqrt(heapVariance) / avgHeapUsed) : 1.0;

    return new MemoryTrendAnalysis(
        durationMs,
        heapGrowth,
        nonHeapGrowth,
        heapGrowthRate,
        nonHeapGrowthRate,
        potentialHeapLeak,
        potentialNonHeapLeak,
        heapStability,
        sortedSnapshots.size());
  }

  /** Analyzes memory pools. */
  private List<MemoryPoolAnalysis> analyzeMemoryPools() {
    final List<MemoryPoolAnalysis> analyses = new ArrayList<>();

    for (final MemoryPoolTracker tracker : poolTrackers.values()) {
      analyses.add(tracker.analyze());
    }

    return analyses;
  }

  /** Gets current heap usage. */
  private long getCurrentHeapUsage() {
    return memoryBean.getHeapMemoryUsage().getUsed();
  }

  /** Detailed memory snapshot with pool information. */
  public static class DetailedMemorySnapshot {
    public final long timestamp;
    public final MemoryUsage heapUsage;
    public final MemoryUsage nonHeapUsage;
    public final ConcurrentMap<String, MemoryPoolState> poolStates;
    public final long trackedObjectCount;

    public DetailedMemorySnapshot(
        final long timestamp,
        final MemoryUsage heapUsage,
        final MemoryUsage nonHeapUsage,
        final ConcurrentMap<String, MemoryPoolState> poolStates,
        final long trackedObjectCount) {
      this.timestamp = timestamp;
      this.heapUsage = heapUsage;
      this.nonHeapUsage = nonHeapUsage;
      this.poolStates = poolStates;
      this.trackedObjectCount = trackedObjectCount;
    }
  }

  /** State of a memory pool at a point in time. */
  public static class MemoryPoolState {
    public final String name;
    public final long used;
    public final long committed;
    public final long max;

    public MemoryPoolState(
        final String name, final long used, final long committed, final long max) {
      this.name = name;
      this.used = used;
      this.committed = committed;
      this.max = max;
    }

    public double getUtilization() {
      return committed > 0 ? (double) used / committed : 0.0;
    }
  }

  /** Memory pressure levels. */
  public enum MemoryPressureLevel {
    LOW,
    MODERATE,
    HIGH,
    CRITICAL
  }

  /** Memory pressure information. */
  public static class MemoryPressureInfo {
    public final MemoryPressureLevel level;
    public final double heapUtilization;
    public final double maxUtilization;

    public MemoryPressureInfo(
        final MemoryPressureLevel level,
        final double heapUtilization,
        final double maxUtilization) {
      this.level = level;
      this.heapUtilization = heapUtilization;
      this.maxUtilization = maxUtilization;
    }
  }

  /** Memory trend analysis results. */
  public static class MemoryTrendAnalysis {
    public final long durationMs;
    public final long heapGrowth;
    public final long nonHeapGrowth;
    public final double heapGrowthRateMBPerMin;
    public final double nonHeapGrowthRateMBPerMin;
    public final boolean potentialHeapLeak;
    public final boolean potentialNonHeapLeak;
    public final double heapStability;
    public final int snapshotCount;
    public final String errorMessage;

    public MemoryTrendAnalysis(
        final long durationMs,
        final long heapGrowth,
        final long nonHeapGrowth,
        final double heapGrowthRate,
        final double nonHeapGrowthRate,
        final boolean potentialHeapLeak,
        final boolean potentialNonHeapLeak,
        final double heapStability,
        final int snapshotCount) {
      this.durationMs = durationMs;
      this.heapGrowth = heapGrowth;
      this.nonHeapGrowth = nonHeapGrowth;
      this.heapGrowthRateMBPerMin = heapGrowthRate / (1024 * 1024);
      this.nonHeapGrowthRateMBPerMin = nonHeapGrowthRate / (1024 * 1024);
      this.potentialHeapLeak = potentialHeapLeak;
      this.potentialNonHeapLeak = potentialNonHeapLeak;
      this.heapStability = heapStability;
      this.snapshotCount = snapshotCount;
      this.errorMessage = null;
    }

    public MemoryTrendAnalysis(final String errorMessage) {
      this.durationMs = 0;
      this.heapGrowth = 0;
      this.nonHeapGrowth = 0;
      this.heapGrowthRateMBPerMin = 0.0;
      this.nonHeapGrowthRateMBPerMin = 0.0;
      this.potentialHeapLeak = false;
      this.potentialNonHeapLeak = false;
      this.heapStability = 1.0;
      this.snapshotCount = 0;
      this.errorMessage = errorMessage;
    }

    public boolean hasError() {
      return errorMessage != null;
    }

    public boolean indicatesMemoryLeak() {
      return potentialHeapLeak || potentialNonHeapLeak;
    }
  }

  /** Memory pool tracker for individual pool monitoring. */
  private static class MemoryPoolTracker {
    private final MemoryPoolMXBean poolBean;
    private final List<MemoryPoolState> history;
    private long peakUsage = 0;

    public MemoryPoolTracker(final MemoryPoolMXBean poolBean) {
      this.poolBean = poolBean;
      this.history = Collections.synchronizedList(new ArrayList<>());
    }

    public void update() {
      final MemoryUsage usage = poolBean.getUsage();
      if (usage != null) {
        peakUsage = Math.max(peakUsage, usage.getUsed());
        history.add(
            new MemoryPoolState(
                poolBean.getName(), usage.getUsed(), usage.getCommitted(), usage.getMax()));

        // Keep only recent history (last 100 entries)
        while (history.size() > 100) {
          history.remove(0);
        }
      }
    }

    public MemoryPoolAnalysis analyze() {
      if (history.isEmpty()) {
        return new MemoryPoolAnalysis(poolBean.getName(), "No data available");
      }

      final MemoryUsage currentUsage = poolBean.getUsage();
      if (currentUsage == null) {
        return new MemoryPoolAnalysis(poolBean.getName(), "Pool not available");
      }

      final double utilizationPercent =
          currentUsage.getCommitted() > 0
              ? (double) currentUsage.getUsed() / currentUsage.getCommitted() * 100.0
              : 0.0;

      final double maxUtilizationPercent =
          currentUsage.getMax() > 0
              ? (double) currentUsage.getUsed() / currentUsage.getMax() * 100.0
              : 0.0;

      final boolean highUtilization = utilizationPercent > 85.0 || maxUtilizationPercent > 85.0;

      return new MemoryPoolAnalysis(
          poolBean.getName(),
          currentUsage.getUsed(),
          currentUsage.getCommitted(),
          currentUsage.getMax(),
          peakUsage,
          utilizationPercent,
          maxUtilizationPercent,
          highUtilization);
    }
  }

  /** Analysis results for a memory pool. */
  public static class MemoryPoolAnalysis {
    public final String poolName;
    public final long currentUsed;
    public final long currentCommitted;
    public final long currentMax;
    public final long peakUsage;
    public final double utilizationPercent;
    public final double maxUtilizationPercent;
    public final boolean highUtilization;
    public final String errorMessage;

    public MemoryPoolAnalysis(
        final String poolName,
        final long currentUsed,
        final long currentCommitted,
        final long currentMax,
        final long peakUsage,
        final double utilizationPercent,
        final double maxUtilizationPercent,
        final boolean highUtilization) {
      this.poolName = poolName;
      this.currentUsed = currentUsed;
      this.currentCommitted = currentCommitted;
      this.currentMax = currentMax;
      this.peakUsage = peakUsage;
      this.utilizationPercent = utilizationPercent;
      this.maxUtilizationPercent = maxUtilizationPercent;
      this.highUtilization = highUtilization;
      this.errorMessage = null;
    }

    public MemoryPoolAnalysis(final String poolName, final String errorMessage) {
      this.poolName = poolName;
      this.currentUsed = 0;
      this.currentCommitted = 0;
      this.currentMax = 0;
      this.peakUsage = 0;
      this.utilizationPercent = 0.0;
      this.maxUtilizationPercent = 0.0;
      this.highUtilization = false;
      this.errorMessage = errorMessage;
    }

    public boolean hasError() {
      return errorMessage != null;
    }
  }

  /** Comprehensive memory monitoring report. */
  public static class MemoryMonitoringReport {
    public final String testName;
    public final Duration monitoringDuration;
    public final long baselineHeapUsage;
    public final long peakHeapUsage;
    public final long finalHeapUsage;
    public final long suspectedLeaks;
    public final int snapshotCount;
    public final MemoryTrendAnalysis trendAnalysis;
    public final MemoryPressureInfo finalPressure;
    public final List<MemoryPoolAnalysis> poolAnalyses;

    public MemoryMonitoringReport(
        final String testName,
        final Duration monitoringDuration,
        final long baselineHeapUsage,
        final long peakHeapUsage,
        final long finalHeapUsage,
        final long suspectedLeaks,
        final int snapshotCount,
        final MemoryTrendAnalysis trendAnalysis,
        final MemoryPressureInfo finalPressure,
        final List<MemoryPoolAnalysis> poolAnalyses) {
      this.testName = testName;
      this.monitoringDuration = monitoringDuration;
      this.baselineHeapUsage = baselineHeapUsage;
      this.peakHeapUsage = peakHeapUsage;
      this.finalHeapUsage = finalHeapUsage;
      this.suspectedLeaks = suspectedLeaks;
      this.snapshotCount = snapshotCount;
      this.trendAnalysis = trendAnalysis;
      this.finalPressure = finalPressure;
      this.poolAnalyses = poolAnalyses;
    }

    public boolean hasMemoryLeaks() {
      return suspectedLeaks > 0 || (trendAnalysis != null && trendAnalysis.indicatesMemoryLeak());
    }

    public boolean hasMemoryPressure() {
      return finalPressure != null
          && (finalPressure.level == MemoryPressureLevel.HIGH
              || finalPressure.level == MemoryPressureLevel.CRITICAL);
    }

    public long getHeapGrowth() {
      return finalHeapUsage - baselineHeapUsage;
    }

    public double getHeapGrowthMB() {
      return getHeapGrowth() / (1024.0 * 1024.0);
    }

    public String getSummary() {
      return String.format(
          "Memory Report [%s]: %.1fMB growth, %dMB peak, %d suspected leaks, %s pressure",
          testName,
          getHeapGrowthMB(),
          peakHeapUsage / (1024 * 1024),
          suspectedLeaks,
          finalPressure != null ? finalPressure.level : "UNKNOWN");
    }
  }
}
