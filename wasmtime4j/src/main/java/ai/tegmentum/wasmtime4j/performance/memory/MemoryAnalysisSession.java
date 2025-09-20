package ai.tegmentum.wasmtime4j.performance.memory;

import ai.tegmentum.wasmtime4j.performance.GcImpactMetrics;
import ai.tegmentum.wasmtime4j.performance.memory.MemoryAnalyzer.MemorySnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Represents an active memory analysis session for tracking specific WebAssembly operations.
 *
 * <p>A memory analysis session provides detailed tracking of memory usage and garbage collection
 * impact during specific WebAssembly operations. Sessions can be used to:
 * <ul>
 *   <li>Measure memory allocation patterns for specific operations</li>
 *   <li>Track garbage collection impact during execution</li>
 *   <li>Identify memory leaks or excessive allocation</li>
 *   <li>Compare memory usage across different implementations</li>
 * </ul>
 *
 * <p>Sessions are typically created by {@link MemoryAnalyzer} and provide automatic
 * memory tracking during their lifecycle.
 *
 * @since 1.0.0
 */
public final class MemoryAnalysisSession {
  private static final Logger LOGGER = Logger.getLogger(MemoryAnalysisSession.class.getName());

  private final String sessionName;
  private final MemoryAnalyzer analyzer;
  private final Instant startTime;
  private final GcImpactMetrics.Snapshot initialGcState;
  private final MemoryAnalyzer.MemoryMetrics initialMemoryState;
  private final List<MemorySnapshot> sessionSnapshots;

  private volatile boolean completed;
  private volatile Instant endTime;
  private volatile MemoryAnalysisResult result;

  MemoryAnalysisSession(final String sessionName, final MemoryAnalyzer analyzer) {
    this.sessionName = Objects.requireNonNull(sessionName, "sessionName cannot be null");
    this.analyzer = Objects.requireNonNull(analyzer, "analyzer cannot be null");
    this.startTime = Instant.now();
    this.initialGcState = GcImpactMetrics.captureSnapshot();
    this.initialMemoryState = analyzer.getCurrentMemoryMetrics();
    this.sessionSnapshots = new CopyOnWriteArrayList<>();
    this.completed = false;
  }

  /**
   * Gets the session name.
   *
   * @return session name
   */
  public String getSessionName() {
    return sessionName;
  }

  /**
   * Gets the session start time.
   *
   * @return start time
   */
  public Instant getStartTime() {
    return startTime;
  }

  /**
   * Gets the elapsed time since session start.
   *
   * @return elapsed time
   */
  public Duration getElapsedTime() {
    final Instant endTimeToUse = completed ? endTime : Instant.now();
    return Duration.between(startTime, endTimeToUse);
  }

  /**
   * Checks if the session has been completed.
   *
   * @return true if session is completed
   */
  public boolean isCompleted() {
    return completed;
  }

  /**
   * Gets the initial memory state when the session started.
   *
   * @return initial memory metrics
   */
  public MemoryAnalyzer.MemoryMetrics getInitialMemoryState() {
    return initialMemoryState;
  }

  /**
   * Gets the current memory state (or final state if completed).
   *
   * @return current memory metrics
   */
  public MemoryAnalyzer.MemoryMetrics getCurrentMemoryState() {
    return analyzer.getCurrentMemoryMetrics();
  }

  /**
   * Gets all memory snapshots captured during this session.
   *
   * @return list of memory snapshots
   */
  public List<MemorySnapshot> getSessionSnapshots() {
    return new ArrayList<>(sessionSnapshots);
  }

  /**
   * Gets the number of snapshots captured during this session.
   *
   * @return snapshot count
   */
  public int getSnapshotCount() {
    return sessionSnapshots.size();
  }

  /**
   * Captures an immediate memory snapshot for this session.
   *
   * @return memory snapshot
   */
  public MemorySnapshot captureSnapshot() {
    if (completed) {
      throw new IllegalStateException("Session is already completed");
    }

    final MemoryAnalyzer.MemoryMetrics metrics = analyzer.getCurrentMemoryMetrics();
    final GcImpactMetrics.Snapshot gcSnapshot = GcImpactMetrics.captureSnapshot();
    final MemorySnapshot snapshot = new MemorySnapshot(metrics, gcSnapshot);

    sessionSnapshots.add(snapshot);
    LOGGER.fine("Captured snapshot for session: " + sessionName);

    return snapshot;
  }

  /**
   * Marks a significant operation within the session.
   *
   * @param operationName name of the operation
   * @return operation marker for tracking
   */
  public OperationMarker markOperation(final String operationName) {
    if (completed) {
      throw new IllegalStateException("Session is already completed");
    }

    return new OperationMarker(operationName, captureSnapshot());
  }

  /**
   * Completes the session and returns the analysis result.
   *
   * @return memory analysis result
   */
  public MemoryAnalysisResult complete() {
    if (completed) {
      return result;
    }

    this.endTime = Instant.now();
    this.completed = true;

    // Capture final state
    final MemoryAnalyzer.MemoryMetrics finalMemoryState = analyzer.getCurrentMemoryMetrics();
    final GcImpactMetrics.Snapshot finalGcState = GcImpactMetrics.captureSnapshot();

    // Calculate GC impact during session
    final Duration sessionDuration = Duration.between(startTime, endTime);
    final GcImpactMetrics gcImpact = GcImpactMetrics.calculate(
        initialGcState, finalGcState, sessionDuration);

    // Analyze session data
    this.result = analyzeSessionData(finalMemoryState, gcImpact);

    // Remove from active sessions
    analyzer.removeSession(sessionName);

    LOGGER.info("Completed memory analysis session: " + sessionName +
                " (duration: " + sessionDuration + ")");

    return result;
  }

  /**
   * Forces completion of the session (typically called during cleanup).
   */
  void forceComplete() {
    if (!completed) {
      complete();
    }
  }

  void addSnapshot(final MemorySnapshot snapshot) {
    if (!completed) {
      sessionSnapshots.add(snapshot);
    }
  }

  private MemoryAnalysisResult analyzeSessionData(final MemoryAnalyzer.MemoryMetrics finalState,
                                                 final GcImpactMetrics gcImpact) {
    // Calculate memory deltas
    final long heapDelta = finalState.getHeapUsed() - initialMemoryState.getHeapUsed();
    final long nonHeapDelta = finalState.getNonHeapUsed() - initialMemoryState.getNonHeapUsed();

    // Analyze allocation patterns
    final AllocationAnalysis allocationAnalysis = analyzeAllocations();

    // Detect potential issues
    final List<String> issues = detectIssues(heapDelta, nonHeapDelta, gcImpact);

    // Generate recommendations
    final List<String> recommendations = generateRecommendations(
        heapDelta, nonHeapDelta, gcImpact, allocationAnalysis);

    return new MemoryAnalysisResult(
        sessionName,
        startTime,
        endTime,
        initialMemoryState,
        finalState,
        heapDelta,
        nonHeapDelta,
        gcImpact,
        allocationAnalysis,
        sessionSnapshots.size(),
        issues,
        recommendations
    );
  }

  private AllocationAnalysis analyzeAllocations() {
    if (sessionSnapshots.size() < 2) {
      return AllocationAnalysis.insufficient();
    }

    // Analyze allocation rate during session
    final List<Long> allocations = new ArrayList<>();
    long totalAllocation = 0;
    long peakAllocation = 0;

    for (int i = 1; i < sessionSnapshots.size(); i++) {
      final long current = sessionSnapshots.get(i).getMemoryMetrics().getHeapUsed();
      final long previous = sessionSnapshots.get(i - 1).getMemoryMetrics().getHeapUsed();

      if (current > previous) {
        final long allocation = current - previous;
        allocations.add(allocation);
        totalAllocation += allocation;
        peakAllocation = Math.max(peakAllocation, allocation);
      }
    }

    final double averageAllocation = allocations.isEmpty() ? 0 :
        allocations.stream().mapToLong(Long::longValue).average().orElse(0);

    final Duration sessionDuration = getElapsedTime();
    final double allocationRate = sessionDuration.toMillis() > 0 ?
        (totalAllocation / 1024.0 / 1024.0) / (sessionDuration.toMillis() / 1000.0) : 0; // MB/s

    return new AllocationAnalysis(
        totalAllocation,
        peakAllocation,
        averageAllocation,
        allocationRate,
        allocations.size()
    );
  }

  private List<String> detectIssues(final long heapDelta, final long nonHeapDelta,
                                   final GcImpactMetrics gcImpact) {
    final List<String> issues = new ArrayList<>();

    // Check for excessive memory growth
    if (heapDelta > 50 * 1024 * 1024) { // 50MB
      issues.add("Significant heap memory increase: " + formatBytes(heapDelta));
    }

    if (nonHeapDelta > 10 * 1024 * 1024) { // 10MB
      issues.add("Significant non-heap memory increase: " + formatBytes(nonHeapDelta));
    }

    // Check for GC impact
    if (gcImpact.hasSignificantImpact()) {
      issues.add("High garbage collection overhead: " +
                 String.format("%.1f%%", gcImpact.getGcOverheadPercentage()));
    }

    if (gcImpact.hasHighAllocationRate()) {
      issues.add("High allocation rate detected: " +
                 String.format("%.1f MB/s", gcImpact.getAllocationRate()));
    }

    return issues;
  }

  private List<String> generateRecommendations(final long heapDelta, final long nonHeapDelta,
                                              final GcImpactMetrics gcImpact,
                                              final AllocationAnalysis allocationAnalysis) {
    final List<String> recommendations = new ArrayList<>();

    // Memory growth recommendations
    if (heapDelta > 0) {
      if (heapDelta > 100 * 1024 * 1024) { // 100MB
        recommendations.add("Consider optimizing memory usage - large heap growth detected");
      } else if (heapDelta > 20 * 1024 * 1024) { // 20MB
        recommendations.add("Monitor memory usage patterns for potential optimizations");
      }
    }

    // GC recommendations
    if (gcImpact.getGcOverheadPercentage() > 5.0) {
      recommendations.add("Consider increasing heap size to reduce GC pressure");
    }

    if (gcImpact.getTotalGcCollections() > 10) {
      recommendations.add("High GC frequency - consider object pooling or reducing allocations");
    }

    // Allocation rate recommendations
    if (allocationAnalysis.getAllocationRate() > 50.0) { // 50 MB/s
      recommendations.add("High allocation rate - consider reusing objects where possible");
    }

    if (recommendations.isEmpty()) {
      recommendations.add("Memory usage appears optimal for this operation");
    }

    return recommendations;
  }

  private String formatBytes(final long bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    } else if (bytes < 1024 * 1024) {
      return String.format("%.1f KB", bytes / 1024.0);
    } else {
      return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }
  }

  /**
   * Represents a marked operation within a memory analysis session.
   */
  public static final class OperationMarker {
    private final String operationName;
    private final MemorySnapshot startSnapshot;
    private final Instant startTime;

    OperationMarker(final String operationName, final MemorySnapshot startSnapshot) {
      this.operationName = operationName;
      this.startSnapshot = startSnapshot;
      this.startTime = Instant.now();
    }

    public String getOperationName() { return operationName; }
    public MemorySnapshot getStartSnapshot() { return startSnapshot; }
    public Instant getStartTime() { return startTime; }

    /**
     * Completes the operation marker and returns analysis of the operation.
     *
     * @param session the session this marker belongs to
     * @return operation analysis result
     */
    public OperationAnalysis complete(final MemoryAnalysisSession session) {
      final MemorySnapshot endSnapshot = session.captureSnapshot();
      final Duration operationDuration = Duration.between(startTime, Instant.now());

      final long heapDelta = endSnapshot.getMemoryMetrics().getHeapUsed() -
                           startSnapshot.getMemoryMetrics().getHeapUsed();

      return new OperationAnalysis(
          operationName,
          startTime,
          operationDuration,
          heapDelta,
          startSnapshot,
          endSnapshot
      );
    }
  }

  /**
   * Analysis result for a specific operation within a session.
   */
  public static final class OperationAnalysis {
    private final String operationName;
    private final Instant startTime;
    private final Duration duration;
    private final long memoryDelta;
    private final MemorySnapshot startSnapshot;
    private final MemorySnapshot endSnapshot;

    OperationAnalysis(final String operationName, final Instant startTime,
                     final Duration duration, final long memoryDelta,
                     final MemorySnapshot startSnapshot, final MemorySnapshot endSnapshot) {
      this.operationName = operationName;
      this.startTime = startTime;
      this.duration = duration;
      this.memoryDelta = memoryDelta;
      this.startSnapshot = startSnapshot;
      this.endSnapshot = endSnapshot;
    }

    public String getOperationName() { return operationName; }
    public Instant getStartTime() { return startTime; }
    public Duration getDuration() { return duration; }
    public long getMemoryDelta() { return memoryDelta; }
    public MemorySnapshot getStartSnapshot() { return startSnapshot; }
    public MemorySnapshot getEndSnapshot() { return endSnapshot; }
  }

  /**
   * Analysis of allocation patterns during a session.
   */
  public static final class AllocationAnalysis {
    private final long totalAllocation;
    private final long peakAllocation;
    private final double averageAllocation;
    private final double allocationRate; // MB/s
    private final int allocationEvents;

    AllocationAnalysis(final long totalAllocation, final long peakAllocation,
                      final double averageAllocation, final double allocationRate,
                      final int allocationEvents) {
      this.totalAllocation = totalAllocation;
      this.peakAllocation = peakAllocation;
      this.averageAllocation = averageAllocation;
      this.allocationRate = allocationRate;
      this.allocationEvents = allocationEvents;
    }

    public static AllocationAnalysis insufficient() {
      return new AllocationAnalysis(0, 0, 0, 0, 0);
    }

    public long getTotalAllocation() { return totalAllocation; }
    public long getPeakAllocation() { return peakAllocation; }
    public double getAverageAllocation() { return averageAllocation; }
    public double getAllocationRate() { return allocationRate; }
    public int getAllocationEvents() { return allocationEvents; }
  }
}