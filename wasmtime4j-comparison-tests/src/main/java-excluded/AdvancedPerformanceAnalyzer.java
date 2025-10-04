package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.performance.GcImpactMetrics;
import ai.tegmentum.wasmtime4j.performance.ProfileSnapshot;
import ai.tegmentum.wasmtime4j.performance.insights.PerformanceInsights;
import ai.tegmentum.wasmtime4j.performance.insights.PerformanceInsightsEngine;
import ai.tegmentum.wasmtime4j.performance.memory.MemoryAnalysisResult;
import ai.tegmentum.wasmtime4j.performance.memory.MemoryAnalyzer;
import ai.tegmentum.wasmtime4j.performance.profiling.JvmProfilerIntegration;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Advanced performance analyzer that integrates profiling capabilities with the existing
 * PerformanceAnalyzer to provide comprehensive Wasmtime performance analysis.
 *
 * <p>This analyzer extends the base {@link PerformanceAnalyzer} functionality with:
 *
 * <ul>
 *   <li>JVM profiling integration (async-profiler, JFR)
 *   <li>Real-time memory analysis and GC impact measurement
 *   <li>Micro-benchmark generation and execution
 *   <li>Intelligent performance insights and recommendations
 *   <li>Cross-runtime performance comparison with profiling data
 * </ul>
 *
 * <p>The analyzer maintains compatibility with existing code while providing enhanced analysis
 * capabilities for production performance monitoring and optimization.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * AdvancedPerformanceAnalyzer analyzer = AdvancedPerformanceAnalyzer.builder()
 *     .enableJvmProfiling(true)
 *     .enableMemoryAnalysis(true)
 *     .enableInsightsGeneration(true)
 *     .build();
 *
 * // Start comprehensive analysis session
 * AdvancedAnalysisSession session = analyzer.startAdvancedSession("wasmtime-test");
 *
 * // Run test with standard PerformanceAnalyzer compatibility
 * List<TestExecutionResult> results = runTests();
 * PerformanceComparisonResult comparison = analyzer.analyze(results);
 *
 * // Get enhanced analysis with profiling data
 * AdvancedAnalysisResult advanced = session.complete();
 * PerformanceInsights insights = advanced.getInsights();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class AdvancedPerformanceAnalyzer extends PerformanceAnalyzer {
  private static final Logger LOGGER =
      Logger.getLogger(AdvancedPerformanceAnalyzer.class.getName());

  private final boolean jvmProfilingEnabled;
  private final boolean memoryAnalysisEnabled;
  private final boolean insightsGenerationEnabled;
  private final boolean microbenchmarkEnabled;

  private final JvmProfilerIntegration profilerIntegration;
  private final MemoryAnalyzer memoryAnalyzer;
  private final PerformanceInsightsEngine insightsEngine;
  private final Map<String, AdvancedAnalysisSession> activeSessions;

  private AdvancedPerformanceAnalyzer(final Builder builder) {
    super(); // Initialize base PerformanceAnalyzer

    this.jvmProfilingEnabled = builder.jvmProfilingEnabled;
    this.memoryAnalysisEnabled = builder.memoryAnalysisEnabled;
    this.insightsGenerationEnabled = builder.insightsGenerationEnabled;
    this.microbenchmarkEnabled = builder.microbenchmarkEnabled;

    this.profilerIntegration = jvmProfilingEnabled ? JvmProfilerIntegration.create() : null;
    this.memoryAnalyzer = memoryAnalysisEnabled ? MemoryAnalyzer.create() : null;
    this.insightsEngine = insightsGenerationEnabled ? PerformanceInsightsEngine.create() : null;
    this.activeSessions = new ConcurrentHashMap<>();

    LOGGER.info(
        "Advanced Performance Analyzer initialized with features: "
            + "JVM Profiling="
            + jvmProfilingEnabled
            + ", Memory Analysis="
            + memoryAnalysisEnabled
            + ", Insights="
            + insightsGenerationEnabled
            + ", Microbenchmarks="
            + microbenchmarkEnabled);
  }

  /**
   * Creates a builder for advanced performance analyzer configuration.
   *
   * @return builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates an advanced performance analyzer with default configuration.
   *
   * @return analyzer instance
   */
  public static AdvancedPerformanceAnalyzer create() {
    return builder().build();
  }

  /**
   * Analyzes test results with enhanced profiling capabilities.
   *
   * <p>This method extends the base {@link PerformanceAnalyzer#analyze(List)} method by adding
   * profiling data collection and analysis.
   *
   * @param results test execution results
   * @return enhanced performance comparison result
   */
  @Override
  public PerformanceComparisonResult analyze(final List<TestExecutionResult> results) {
    // Perform base analysis
    final PerformanceComparisonResult baseResult = super.analyze(results);

    // Enhance with profiling data if available
    enhanceWithProfilingData(baseResult, results);

    return baseResult;
  }

  /**
   * Starts an advanced analysis session with comprehensive profiling.
   *
   * @param sessionName unique name for the session
   * @return advanced analysis session
   * @throws IllegalArgumentException if session name already exists
   */
  public AdvancedAnalysisSession startAdvancedSession(final String sessionName) {
    Objects.requireNonNull(sessionName, "sessionName cannot be null");

    if (activeSessions.containsKey(sessionName)) {
      throw new IllegalArgumentException("Session already exists: " + sessionName);
    }

    final AdvancedAnalysisSession session = new AdvancedAnalysisSession(sessionName, this);
    activeSessions.put(sessionName, session);

    LOGGER.info("Started advanced analysis session: " + sessionName);
    return session;
  }

  /**
   * Generates performance insights from collected analysis data.
   *
   * @return performance insights or null if insights generation is disabled
   */
  public PerformanceInsights generateInsights() {
    if (!insightsGenerationEnabled || insightsEngine == null) {
      LOGGER.warning("Insights generation is not enabled");
      return null;
    }

    return insightsEngine.generateInsights();
  }

  /**
   * Gets the status of all advanced features.
   *
   * @return feature status
   */
  public AdvancedFeatureStatus getFeatureStatus() {
    final Map<String, Boolean> features = new HashMap<>();
    features.put("jvmProfiling", jvmProfilingEnabled);
    features.put("memoryAnalysis", memoryAnalysisEnabled);
    features.put("insightsGeneration", insightsGenerationEnabled);
    features.put("microbenchmarks", microbenchmarkEnabled);

    final Map<String, String> toolStatus = new HashMap<>();
    if (profilerIntegration != null) {
      toolStatus.put("availableProfilers", profilerIntegration.getAvailableTools().toString());
      toolStatus.put(
          "activeProfileSessions", String.valueOf(profilerIntegration.getActiveSessionCount()));
    }

    if (memoryAnalyzer != null) {
      toolStatus.put("memoryMonitoring", String.valueOf(memoryAnalyzer.isMonitoring()));
      toolStatus.put(
          "activeMemorySessions", String.valueOf(memoryAnalyzer.getActiveSessions().size()));
    }

    return new AdvancedFeatureStatus(features, toolStatus);
  }

  /** Closes the advanced analyzer and releases all resources. */
  public void close() {
    // Complete all active sessions
    for (final AdvancedAnalysisSession session : new ArrayList<>(activeSessions.values())) {
      try {
        session.complete();
      } catch (final Exception e) {
        LOGGER.warning(
            "Failed to complete session: " + session.getSessionName() + " - " + e.getMessage());
      }
    }

    // Close profiling tools
    if (profilerIntegration != null) {
      profilerIntegration.stopAllSessions();
    }

    if (memoryAnalyzer != null) {
      memoryAnalyzer.close();
    }

    LOGGER.info("Advanced Performance Analyzer closed");
  }

  JvmProfilerIntegration getProfilerIntegration() {
    return profilerIntegration;
  }

  MemoryAnalyzer getMemoryAnalyzer() {
    return memoryAnalyzer;
  }

  PerformanceInsightsEngine getInsightsEngine() {
    return insightsEngine;
  }

  void removeSession(final String sessionName) {
    activeSessions.remove(sessionName);
  }

  private void enhanceWithProfilingData(
      final PerformanceComparisonResult result, final List<TestExecutionResult> results) {
    if (insightsEngine == null) {
      return;
    }

    // Add memory analysis data
    if (memoryAnalysisEnabled && !results.isEmpty()) {
      // This would integrate memory analysis data
      // For now, we'll capture current memory state
      final GcImpactMetrics gcMetrics = GcImpactMetrics.measureForcedGc();
      insightsEngine.addGcMetrics(gcMetrics);
    }

    // Add runtime characteristics
    final Map<String, Object> characteristics = extractRuntimeCharacteristics(result);
    for (final Map.Entry<String, Object> entry : characteristics.entrySet()) {
      if (entry.getValue() instanceof Map) {
        insightsEngine.addRuntimeCharacteristics(
            entry.getKey(), (Map<String, Object>) entry.getValue());
      }
    }
  }

  private Map<String, Object> extractRuntimeCharacteristics(
      final PerformanceComparisonResult result) {
    final Map<String, Object> characteristics = new HashMap<>();

    for (final Map.Entry<String, PerformanceMetrics> entry :
        result.getMetricsByRuntime().entrySet()) {
      final String runtime = entry.getKey();
      final PerformanceMetrics metrics = entry.getValue();

      final Map<String, Object> runtimeData = new HashMap<>();
      runtimeData.put("meanExecutionTime", metrics.getMeanExecutionTimeMs());
      runtimeData.put("successRate", metrics.getSuccessRate());
      runtimeData.put("sampleSize", metrics.getSampleSize());
      runtimeData.put("standardDeviation", metrics.getStandardDeviation());

      // Add Wasmtime-specific metrics if available
      if (metrics.getMeanCompilationTimeMs() > 0) {
        runtimeData.put("compilationTime", metrics.getMeanCompilationTimeMs());
        runtimeData.put("functionCallOverhead", metrics.getMeanFunctionCallOverheadNanos());
        runtimeData.put("memoryOperationTime", metrics.getMeanMemoryOperationTimeNanos());
        runtimeData.put("performanceScore", metrics.getWasmtimePerformanceScore());
      }

      characteristics.put(runtime, runtimeData);
    }

    return characteristics;
  }

  /** Advanced analysis session that provides comprehensive profiling capabilities. */
  public static final class AdvancedAnalysisSession {
    private final String sessionName;
    private final AdvancedPerformanceAnalyzer analyzer;
    private final Instant startTime;

    private JvmProfilerIntegration.ProfileSession profilingSession;
    private MemoryAnalyzer.MemoryAnalysisSession memorySession;
    private final List<ProfileSnapshot> snapshots;
    private volatile boolean completed;

    AdvancedAnalysisSession(final String sessionName, final AdvancedPerformanceAnalyzer analyzer) {
      this.sessionName = sessionName;
      this.analyzer = analyzer;
      this.startTime = Instant.now();
      this.snapshots = new ArrayList<>();
      this.completed = false;

      // Start profiling if enabled
      if (analyzer.jvmProfilingEnabled && analyzer.profilerIntegration != null) {
        try {
          this.profilingSession =
              analyzer.profilerIntegration.startProfiling(
                  JvmProfilerIntegration.ProfilingMode.CPU_SAMPLING);
        } catch (final Exception e) {
          LOGGER.warning("Failed to start JVM profiling: " + e.getMessage());
        }
      }

      // Start memory analysis if enabled
      if (analyzer.memoryAnalysisEnabled && analyzer.memoryAnalyzer != null) {
        try {
          this.memorySession = analyzer.memoryAnalyzer.startSession(sessionName);
        } catch (final Exception e) {
          LOGGER.warning("Failed to start memory analysis: " + e.getMessage());
        }
      }
    }

    public String getSessionName() {
      return sessionName;
    }

    public Instant getStartTime() {
      return startTime;
    }

    public Duration getElapsedTime() {
      return Duration.between(startTime, Instant.now());
    }

    public boolean isCompleted() {
      return completed;
    }

    /**
     * Captures a performance snapshot during the session.
     *
     * @return profile snapshot
     */
    public ProfileSnapshot captureSnapshot() {
      if (completed) {
        throw new IllegalStateException("Session is already completed");
      }

      // This would create a real ProfileSnapshot implementation
      // For now, we'll create a placeholder
      final ProfileSnapshot snapshot = createSnapshotPlaceholder();
      snapshots.add(snapshot);

      return snapshot;
    }

    /**
     * Runs standard performance analysis within this advanced session.
     *
     * @param results test execution results
     * @return performance comparison result
     */
    public PerformanceComparisonResult runAnalysis(final List<TestExecutionResult> results) {
      if (completed) {
        throw new IllegalStateException("Session is already completed");
      }

      return analyzer.analyze(results);
    }

    /**
     * Completes the session and returns comprehensive analysis results.
     *
     * @return advanced analysis result
     */
    public AdvancedAnalysisResult complete() {
      if (completed) {
        throw new IllegalStateException("Session is already completed");
      }

      this.completed = true;
      final Instant endTime = Instant.now();

      // Stop profiling sessions
      JvmProfilerIntegration.ProfilingResult profilingResult = null;
      if (profilingSession != null) {
        try {
          profilingResult = profilingSession.stop();
        } catch (final Exception e) {
          LOGGER.warning("Failed to stop profiling session: " + e.getMessage());
        }
      }

      MemoryAnalysisResult memoryResult = null;
      if (memorySession != null) {
        try {
          memoryResult = memorySession.complete();
        } catch (final Exception e) {
          LOGGER.warning("Failed to complete memory session: " + e.getMessage());
        }
      }

      // Add results to insights engine
      if (analyzer.insightsEngine != null) {
        if (memoryResult != null) {
          analyzer.insightsEngine.addMemoryAnalysis(memoryResult);
        }

        for (final ProfileSnapshot snapshot : snapshots) {
          analyzer.insightsEngine.addProfileSnapshot(snapshot);
        }
      }

      // Generate insights
      PerformanceInsights insights = null;
      if (analyzer.insightsGenerationEnabled) {
        insights = analyzer.generateInsights();
      }

      // Remove from active sessions
      analyzer.removeSession(sessionName);

      LOGGER.info(
          "Completed advanced analysis session: "
              + sessionName
              + " (duration: "
              + Duration.between(startTime, endTime)
              + ")");

      return new AdvancedAnalysisResult(
          sessionName,
          startTime,
          endTime,
          profilingResult,
          memoryResult,
          List.copyOf(snapshots),
          insights);
    }

    private ProfileSnapshot createSnapshotPlaceholder() {
      // This would create a real ProfileSnapshot implementation
      // For now, return null as a placeholder
      return null;
    }
  }

  /** Comprehensive result from an advanced analysis session. */
  public static final class AdvancedAnalysisResult {
    private final String sessionName;
    private final Instant startTime;
    private final Instant endTime;
    private final JvmProfilerIntegration.ProfilingResult profilingResult;
    private final MemoryAnalysisResult memoryResult;
    private final List<ProfileSnapshot> snapshots;
    private final PerformanceInsights insights;

    AdvancedAnalysisResult(
        final String sessionName,
        final Instant startTime,
        final Instant endTime,
        final JvmProfilerIntegration.ProfilingResult profilingResult,
        final MemoryAnalysisResult memoryResult,
        final List<ProfileSnapshot> snapshots,
        final PerformanceInsights insights) {
      this.sessionName = sessionName;
      this.startTime = startTime;
      this.endTime = endTime;
      this.profilingResult = profilingResult;
      this.memoryResult = memoryResult;
      this.snapshots = List.copyOf(snapshots);
      this.insights = insights;
    }

    public String getSessionName() {
      return sessionName;
    }

    public Instant getStartTime() {
      return startTime;
    }

    public Instant getEndTime() {
      return endTime;
    }

    public Duration getSessionDuration() {
      return Duration.between(startTime, endTime);
    }

    public JvmProfilerIntegration.ProfilingResult getProfilingResult() {
      return profilingResult;
    }

    public MemoryAnalysisResult getMemoryResult() {
      return memoryResult;
    }

    public List<ProfileSnapshot> getSnapshots() {
      return snapshots;
    }

    public PerformanceInsights getInsights() {
      return insights;
    }

    public boolean hasProfilingData() {
      return profilingResult != null;
    }

    public boolean hasMemoryData() {
      return memoryResult != null;
    }

    public boolean hasInsights() {
      return insights != null;
    }
  }

  /** Status of advanced features. */
  public static final class AdvancedFeatureStatus {
    private final Map<String, Boolean> features;
    private final Map<String, String> toolStatus;

    AdvancedFeatureStatus(
        final Map<String, Boolean> features, final Map<String, String> toolStatus) {
      this.features = Map.copyOf(features);
      this.toolStatus = Map.copyOf(toolStatus);
    }

    public Map<String, Boolean> getFeatures() {
      return features;
    }

    public Map<String, String> getToolStatus() {
      return toolStatus;
    }

    public boolean isFeatureEnabled(final String feature) {
      return features.getOrDefault(feature, false);
    }
  }

  /** Builder for AdvancedPerformanceAnalyzer. */
  public static final class Builder {
    private boolean jvmProfilingEnabled = false;
    private boolean memoryAnalysisEnabled = true;
    private boolean insightsGenerationEnabled = true;
    private boolean microbenchmarkEnabled = false;

    public Builder enableJvmProfiling(final boolean enabled) {
      this.jvmProfilingEnabled = enabled;
      return this;
    }

    public Builder enableMemoryAnalysis(final boolean enabled) {
      this.memoryAnalysisEnabled = enabled;
      return this;
    }

    public Builder enableInsightsGeneration(final boolean enabled) {
      this.insightsGenerationEnabled = enabled;
      return this;
    }

    public Builder enableMicrobenchmarks(final boolean enabled) {
      this.microbenchmarkEnabled = enabled;
      return this;
    }

    public AdvancedPerformanceAnalyzer build() {
      return new AdvancedPerformanceAnalyzer(this);
    }
  }
}
