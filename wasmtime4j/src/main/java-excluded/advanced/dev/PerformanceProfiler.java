package ai.tegmentum.wasmtime4j.dev;

import ai.tegmentum.wasmtime4j.Instance;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Development-time performance profiler for WebAssembly execution analysis. Provides detailed
 * profiling information, bottleneck identification, and performance recommendations.
 */
public final class PerformanceProfiler {

  private final Instance instance;
  private final Map<String, FunctionProfile> functionProfiles;
  private final Map<String, AtomicLong> callCounts;
  private final Map<String, AtomicLong> totalExecutionTimes;
  private final long profilingStartTime;
  private volatile boolean isEnabled;
  private final long profileHandle;

  /**
   * Creates a performance profiler for the given instance.
   *
   * @param instance The WebAssembly instance to profile
   * @throws IllegalArgumentException if instance is null
   */
  public PerformanceProfiler(final Instance instance) {
    if (instance == null) {
      throw new IllegalArgumentException("Instance cannot be null");
    }
    this.instance = instance;
    this.functionProfiles = new ConcurrentHashMap<>();
    this.callCounts = new ConcurrentHashMap<>();
    this.totalExecutionTimes = new ConcurrentHashMap<>();
    this.profilingStartTime = System.nanoTime();
    this.isEnabled = false;
    this.profileHandle = initializeProfiler(instance);
  }

  /** Starts profiling execution. */
  public void startProfiling() {
    isEnabled = true;
    startNativeProfiling(profileHandle);
  }

  /** Stops profiling execution. */
  public void stopProfiling() {
    isEnabled = false;
    stopNativeProfiling(profileHandle);
  }

  /** Clears all profiling data. */
  public void reset() {
    functionProfiles.clear();
    callCounts.clear();
    totalExecutionTimes.clear();
    resetNativeProfiling(profileHandle);
  }

  /**
   * Generates comprehensive profiling report.
   *
   * @return Complete profiling report with analysis
   */
  public ProfilingReport generateReport() {
    final ProfilingData data = collectProfilingData();
    final List<FunctionProfile> profiles = analyzeFunctionProfiles(data);
    final List<Hotspot> hotspots = identifyHotspots(profiles);
    final List<Bottleneck> bottlenecks = identifyBottlenecks(profiles);
    final MemoryProfile memoryProfile = analyzeMemoryUsage();
    final CallGraphAnalysis callGraph = analyzeCallGraph();
    final List<PerformanceRecommendation> recommendations =
        generateRecommendations(profiles, hotspots, bottlenecks, memoryProfile);

    return new ProfilingReport(
        profiles,
        hotspots,
        bottlenecks,
        memoryProfile,
        callGraph,
        recommendations,
        System.nanoTime() - profilingStartTime);
  }

  /**
   * Profiles a specific function execution.
   *
   * @param functionName The function name to profile
   * @param execution The execution to profile
   * @param <T> The return type
   * @return The execution result
   */
  public <T> T profileFunction(final String functionName, final ProfiledExecution<T> execution) {
    if (!isEnabled) {
      return execution.execute();
    }

    final long startTime = System.nanoTime();
    final long startMemory = getCurrentMemoryUsage();

    try {
      final T result = execution.execute();
      recordSuccessfulExecution(functionName, startTime, startMemory);
      return result;
    } catch (final Exception e) {
      recordFailedExecution(functionName, startTime, e);
      throw e;
    }
  }

  /**
   * Gets real-time performance metrics.
   *
   * @return Current performance metrics
   */
  public PerformanceMetrics getCurrentMetrics() {
    final long totalTime = System.nanoTime() - profilingStartTime;
    final long totalCalls = callCounts.values().stream().mapToLong(AtomicLong::get).sum();
    final long totalExecutionTime =
        totalExecutionTimes.values().stream().mapToLong(AtomicLong::get).sum();

    final double averageExecutionTime =
        totalCalls > 0 ? (double) totalExecutionTime / totalCalls : 0.0;

    final MemoryProfile currentMemory = analyzeMemoryUsage();

    return new PerformanceMetrics(
        totalTime,
        totalCalls,
        totalExecutionTime,
        averageExecutionTime,
        functionProfiles.size(),
        currentMemory.getCurrentUsage(),
        getCurrentCpuUsage());
  }

  /**
   * Analyzes performance trends over time.
   *
   * @param durationMs The duration to analyze in milliseconds
   * @return Performance trend analysis
   */
  public PerformanceTrends analyzeTrends(final long durationMs) {
    final List<PerformanceSample> samples = collectPerformanceSamples(durationMs);
    final TrendAnalysis executionTrend = analyzeExecutionTimeTrend(samples);
    final TrendAnalysis memoryTrend = analyzeMemoryTrend(samples);
    final TrendAnalysis throughputTrend = analyzeThroughputTrend(samples);

    return new PerformanceTrends(samples, executionTrend, memoryTrend, throughputTrend);
  }

  /**
   * Compares performance with baseline metrics.
   *
   * @param baseline The baseline metrics to compare against
   * @return Performance comparison result
   */
  public PerformanceComparison compareWithBaseline(final PerformanceBaseline baseline) {
    final PerformanceMetrics current = getCurrentMetrics();
    final Map<String, ComparisonResult> functionComparisons = new HashMap<>();

    for (final Map.Entry<String, FunctionProfile> entry : functionProfiles.entrySet()) {
      final String functionName = entry.getKey();
      final FunctionProfile currentProfile = entry.getValue();
      final FunctionProfile baselineProfile = baseline.getFunctionProfile(functionName);

      if (baselineProfile != null) {
        final ComparisonResult comparison =
            compareFunctionProfiles(currentProfile, baselineProfile);
        functionComparisons.put(functionName, comparison);
      }
    }

    return new PerformanceComparison(
        current,
        baseline.getMetrics(),
        functionComparisons,
        calculateOverallPerformanceChange(current, baseline.getMetrics()));
  }

  private native long initializeProfiler(Instance instance);

  private native void startNativeProfiling(long handle);

  private native void stopNativeProfiling(long handle);

  private native void resetNativeProfiling(long handle);

  private native ProfilingData collectProfilingData();

  private native List<FunctionProfile> analyzeFunctionProfiles(ProfilingData data);

  private native List<Hotspot> identifyHotspots(List<FunctionProfile> profiles);

  private native List<Bottleneck> identifyBottlenecks(List<FunctionProfile> profiles);

  private native MemoryProfile analyzeMemoryUsage();

  private native CallGraphAnalysis analyzeCallGraph();

  private native long getCurrentMemoryUsage();

  private native double getCurrentCpuUsage();

  private native List<PerformanceSample> collectPerformanceSamples(long durationMs);

  private native TrendAnalysis analyzeExecutionTimeTrend(List<PerformanceSample> samples);

  private native TrendAnalysis analyzeMemoryTrend(List<PerformanceSample> samples);

  private native TrendAnalysis analyzeThroughputTrend(List<PerformanceSample> samples);

  private void recordSuccessfulExecution(
      final String functionName, final long startTime, final long startMemory) {
    final long executionTime = System.nanoTime() - startTime;
    final long memoryUsed = getCurrentMemoryUsage() - startMemory;

    callCounts.computeIfAbsent(functionName, k -> new AtomicLong(0)).incrementAndGet();
    totalExecutionTimes
        .computeIfAbsent(functionName, k -> new AtomicLong(0))
        .addAndGet(executionTime);

    functionProfiles.compute(
        functionName,
        (name, existing) -> {
          if (existing == null) {
            return new FunctionProfile(
                name,
                executionTime,
                executionTime,
                executionTime,
                1,
                0,
                memoryUsed,
                memoryUsed,
                memoryUsed);
          } else {
            return existing.update(executionTime, memoryUsed, true);
          }
        });
  }

  private void recordFailedExecution(
      final String functionName, final long startTime, final Exception exception) {
    final long executionTime = System.nanoTime() - startTime;

    callCounts.computeIfAbsent(functionName, k -> new AtomicLong(0)).incrementAndGet();

    functionProfiles.compute(
        functionName,
        (name, existing) -> {
          if (existing == null) {
            return new FunctionProfile(
                name, executionTime, executionTime, executionTime, 1, 1, 0, 0, 0);
          } else {
            return existing.update(executionTime, 0, false);
          }
        });
  }

  private List<PerformanceRecommendation> generateRecommendations(
      final List<FunctionProfile> profiles,
      final List<Hotspot> hotspots,
      final List<Bottleneck> bottlenecks,
      final MemoryProfile memoryProfile) {

    final List<PerformanceRecommendation> recommendations = new ArrayList<>();

    // Analyze hotspots
    for (final Hotspot hotspot : hotspots) {
      if (hotspot.getExecutionTimePercentage() > 20.0) {
        recommendations.add(
            new PerformanceRecommendation(
                RecommendationType.HOTSPOT_OPTIMIZATION,
                "Optimize function: " + hotspot.getFunctionName(),
                "This function consumes "
                    + String.format("%.1f", hotspot.getExecutionTimePercentage())
                    + "% of total execution time",
                RecommendationPriority.HIGH));
      }
    }

    // Analyze memory usage
    if (memoryProfile.getPeakUsage() > memoryProfile.getAverageUsage() * 2) {
      recommendations.add(
          new PerformanceRecommendation(
              RecommendationType.MEMORY_OPTIMIZATION,
              "Optimize memory usage patterns",
              "Peak memory usage is significantly higher than average",
              RecommendationPriority.MEDIUM));
    }

    // Analyze function call patterns
    final FunctionProfile mostCalled =
        profiles.stream()
            .max((p1, p2) -> Long.compare(p1.getCallCount(), p2.getCallCount()))
            .orElse(null);

    if (mostCalled != null && mostCalled.getCallCount() > 1000) {
      recommendations.add(
          new PerformanceRecommendation(
              RecommendationType.INLINING,
              "Consider inlining frequently called function: " + mostCalled.getFunctionName(),
              "Function is called " + mostCalled.getCallCount() + " times",
              RecommendationPriority.MEDIUM));
    }

    return recommendations;
  }

  private ComparisonResult compareFunctionProfiles(
      final FunctionProfile current, final FunctionProfile baseline) {
    final double executionTimeChange =
        calculatePercentageChange(
            current.getAverageExecutionTime(), baseline.getAverageExecutionTime());
    final double memoryUsageChange =
        calculatePercentageChange(
            current.getAverageMemoryUsage(), baseline.getAverageMemoryUsage());
    final double callCountChange =
        calculatePercentageChange(current.getCallCount(), baseline.getCallCount());

    return new ComparisonResult(
        current.getFunctionName(),
        executionTimeChange,
        memoryUsageChange,
        callCountChange,
        determineOverallTrend(executionTimeChange, memoryUsageChange));
  }

  private double calculatePercentageChange(final double current, final double baseline) {
    if (baseline == 0) {
      return current > 0 ? 100.0 : 0.0;
    }
    return ((current - baseline) / baseline) * 100.0;
  }

  private PerformanceTrend determineOverallTrend(
      final double executionTimeChange, final double memoryUsageChange) {
    if (executionTimeChange < -5.0 && memoryUsageChange < -5.0) {
      return PerformanceTrend.IMPROVING;
    } else if (executionTimeChange > 5.0 || memoryUsageChange > 5.0) {
      return PerformanceTrend.DEGRADING;
    } else {
      return PerformanceTrend.STABLE;
    }
  }

  private double calculateOverallPerformanceChange(
      final PerformanceMetrics current, final PerformanceMetrics baseline) {
    final double executionTimeChange =
        calculatePercentageChange(
            current.getAverageExecutionTime(), baseline.getAverageExecutionTime());
    final double memoryChange =
        calculatePercentageChange(
            current.getCurrentMemoryUsage(), baseline.getCurrentMemoryUsage());

    return (executionTimeChange + memoryChange) / 2.0;
  }

  /** Functional interface for profiled execution. */
  @FunctionalInterface
  public interface ProfiledExecution<T> {
    T execute() throws Exception;
  }

  /** Complete profiling report. */
  public static final class ProfilingReport {
    private final List<FunctionProfile> functionProfiles;
    private final List<Hotspot> hotspots;
    private final List<Bottleneck> bottlenecks;
    private final MemoryProfile memoryProfile;
    private final CallGraphAnalysis callGraph;
    private final List<PerformanceRecommendation> recommendations;
    private final long totalProfilingTime;

    public ProfilingReport(
        final List<FunctionProfile> functionProfiles,
        final List<Hotspot> hotspots,
        final List<Bottleneck> bottlenecks,
        final MemoryProfile memoryProfile,
        final CallGraphAnalysis callGraph,
        final List<PerformanceRecommendation> recommendations,
        final long totalProfilingTime) {
      this.functionProfiles = Collections.unmodifiableList(new ArrayList<>(functionProfiles));
      this.hotspots = Collections.unmodifiableList(new ArrayList<>(hotspots));
      this.bottlenecks = Collections.unmodifiableList(new ArrayList<>(bottlenecks));
      this.memoryProfile = memoryProfile;
      this.callGraph = callGraph;
      this.recommendations = Collections.unmodifiableList(new ArrayList<>(recommendations));
      this.totalProfilingTime = totalProfilingTime;
    }

    public List<FunctionProfile> getFunctionProfiles() {
      return functionProfiles;
    }

    public List<Hotspot> getHotspots() {
      return hotspots;
    }

    public List<Bottleneck> getBottlenecks() {
      return bottlenecks;
    }

    public MemoryProfile getMemoryProfile() {
      return memoryProfile;
    }

    public CallGraphAnalysis getCallGraph() {
      return callGraph;
    }

    public List<PerformanceRecommendation> getRecommendations() {
      return recommendations;
    }

    public long getTotalProfilingTime() {
      return totalProfilingTime;
    }
  }

  /** Function performance profile. */
  public static final class FunctionProfile {
    private final String functionName;
    private final long minExecutionTime;
    private final long maxExecutionTime;
    private final long averageExecutionTime;
    private final long callCount;
    private final long errorCount;
    private final long minMemoryUsage;
    private final long maxMemoryUsage;
    private final long averageMemoryUsage;

    public FunctionProfile(
        final String functionName,
        final long minExecutionTime,
        final long maxExecutionTime,
        final long averageExecutionTime,
        final long callCount,
        final long errorCount,
        final long minMemoryUsage,
        final long maxMemoryUsage,
        final long averageMemoryUsage) {
      this.functionName = functionName;
      this.minExecutionTime = minExecutionTime;
      this.maxExecutionTime = maxExecutionTime;
      this.averageExecutionTime = averageExecutionTime;
      this.callCount = callCount;
      this.errorCount = errorCount;
      this.minMemoryUsage = minMemoryUsage;
      this.maxMemoryUsage = maxMemoryUsage;
      this.averageMemoryUsage = averageMemoryUsage;
    }

    public FunctionProfile update(
        final long executionTime, final long memoryUsage, final boolean success) {
      final long newCallCount = callCount + 1;
      final long newErrorCount = success ? errorCount : errorCount + 1;
      final long newMinExecutionTime = Math.min(minExecutionTime, executionTime);
      final long newMaxExecutionTime = Math.max(maxExecutionTime, executionTime);
      final long newAverageExecutionTime =
          (averageExecutionTime * callCount + executionTime) / newCallCount;
      final long newMinMemoryUsage = Math.min(minMemoryUsage, memoryUsage);
      final long newMaxMemoryUsage = Math.max(maxMemoryUsage, memoryUsage);
      final long newAverageMemoryUsage =
          (averageMemoryUsage * callCount + memoryUsage) / newCallCount;

      return new FunctionProfile(
          functionName,
          newMinExecutionTime,
          newMaxExecutionTime,
          newAverageExecutionTime,
          newCallCount,
          newErrorCount,
          newMinMemoryUsage,
          newMaxMemoryUsage,
          newAverageMemoryUsage);
    }

    public String getFunctionName() {
      return functionName;
    }

    public long getMinExecutionTime() {
      return minExecutionTime;
    }

    public long getMaxExecutionTime() {
      return maxExecutionTime;
    }

    public long getAverageExecutionTime() {
      return averageExecutionTime;
    }

    public long getCallCount() {
      return callCount;
    }

    public long getErrorCount() {
      return errorCount;
    }

    public long getMinMemoryUsage() {
      return minMemoryUsage;
    }

    public long getMaxMemoryUsage() {
      return maxMemoryUsage;
    }

    public long getAverageMemoryUsage() {
      return averageMemoryUsage;
    }

    public double getErrorRate() {
      return callCount > 0 ? (double) errorCount / callCount : 0.0;
    }
  }

  /** Performance hotspot identification. */
  public static final class Hotspot {
    private final String functionName;
    private final long totalExecutionTime;
    private final double executionTimePercentage;
    private final long callCount;

    public Hotspot(
        final String functionName,
        final long totalExecutionTime,
        final double executionTimePercentage,
        final long callCount) {
      this.functionName = functionName;
      this.totalExecutionTime = totalExecutionTime;
      this.executionTimePercentage = executionTimePercentage;
      this.callCount = callCount;
    }

    public String getFunctionName() {
      return functionName;
    }

    public long getTotalExecutionTime() {
      return totalExecutionTime;
    }

    public double getExecutionTimePercentage() {
      return executionTimePercentage;
    }

    public long getCallCount() {
      return callCount;
    }
  }

  /** Performance bottleneck identification. */
  public static final class Bottleneck {
    private final String description;
    private final BottleneckType type;
    private final double severity;
    private final String recommendation;

    public Bottleneck(
        final String description,
        final BottleneckType type,
        final double severity,
        final String recommendation) {
      this.description = description;
      this.type = type;
      this.severity = severity;
      this.recommendation = recommendation;
    }

    public String getDescription() {
      return description;
    }

    public BottleneckType getType() {
      return type;
    }

    public double getSeverity() {
      return severity;
    }

    public String getRecommendation() {
      return recommendation;
    }
  }

  /** Memory usage profile. */
  public static final class MemoryProfile {
    private final long currentUsage;
    private final long peakUsage;
    private final long averageUsage;
    private final long allocationCount;
    private final long deallocationCount;

    public MemoryProfile(
        final long currentUsage,
        final long peakUsage,
        final long averageUsage,
        final long allocationCount,
        final long deallocationCount) {
      this.currentUsage = currentUsage;
      this.peakUsage = peakUsage;
      this.averageUsage = averageUsage;
      this.allocationCount = allocationCount;
      this.deallocationCount = deallocationCount;
    }

    public long getCurrentUsage() {
      return currentUsage;
    }

    public long getPeakUsage() {
      return peakUsage;
    }

    public long getAverageUsage() {
      return averageUsage;
    }

    public long getAllocationCount() {
      return allocationCount;
    }

    public long getDeallocationCount() {
      return deallocationCount;
    }
  }

  /** Call graph analysis. */
  public static final class CallGraphAnalysis {
    private final Map<String, List<String>> callRelationships;
    private final Map<String, Integer> callDepths;
    private final List<String> recursiveFunctions;

    public CallGraphAnalysis(
        final Map<String, List<String>> callRelationships,
        final Map<String, Integer> callDepths,
        final List<String> recursiveFunctions) {
      this.callRelationships = Collections.unmodifiableMap(new HashMap<>(callRelationships));
      this.callDepths = Collections.unmodifiableMap(new HashMap<>(callDepths));
      this.recursiveFunctions = Collections.unmodifiableList(new ArrayList<>(recursiveFunctions));
    }

    public Map<String, List<String>> getCallRelationships() {
      return callRelationships;
    }

    public Map<String, Integer> getCallDepths() {
      return callDepths;
    }

    public List<String> getRecursiveFunctions() {
      return recursiveFunctions;
    }
  }

  /** Performance recommendation. */
  public static final class PerformanceRecommendation {
    private final RecommendationType type;
    private final String title;
    private final String description;
    private final RecommendationPriority priority;

    public PerformanceRecommendation(
        final RecommendationType type,
        final String title,
        final String description,
        final RecommendationPriority priority) {
      this.type = type;
      this.title = title;
      this.description = description;
      this.priority = priority;
    }

    public RecommendationType getType() {
      return type;
    }

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }

    public RecommendationPriority getPriority() {
      return priority;
    }
  }

  /** Current performance metrics. */
  public static final class PerformanceMetrics {
    private final long totalTime;
    private final long totalCalls;
    private final long totalExecutionTime;
    private final double averageExecutionTime;
    private final int uniqueFunctionCount;
    private final long currentMemoryUsage;
    private final double currentCpuUsage;

    public PerformanceMetrics(
        final long totalTime,
        final long totalCalls,
        final long totalExecutionTime,
        final double averageExecutionTime,
        final int uniqueFunctionCount,
        final long currentMemoryUsage,
        final double currentCpuUsage) {
      this.totalTime = totalTime;
      this.totalCalls = totalCalls;
      this.totalExecutionTime = totalExecutionTime;
      this.averageExecutionTime = averageExecutionTime;
      this.uniqueFunctionCount = uniqueFunctionCount;
      this.currentMemoryUsage = currentMemoryUsage;
      this.currentCpuUsage = currentCpuUsage;
    }

    public long getTotalTime() {
      return totalTime;
    }

    public long getTotalCalls() {
      return totalCalls;
    }

    public long getTotalExecutionTime() {
      return totalExecutionTime;
    }

    public double getAverageExecutionTime() {
      return averageExecutionTime;
    }

    public int getUniqueFunctionCount() {
      return uniqueFunctionCount;
    }

    public long getCurrentMemoryUsage() {
      return currentMemoryUsage;
    }

    public double getCurrentCpuUsage() {
      return currentCpuUsage;
    }
  }

  /** Performance trend analysis. */
  public static final class PerformanceTrends {
    private final List<PerformanceSample> samples;
    private final TrendAnalysis executionTimeTrend;
    private final TrendAnalysis memoryTrend;
    private final TrendAnalysis throughputTrend;

    public PerformanceTrends(
        final List<PerformanceSample> samples,
        final TrendAnalysis executionTimeTrend,
        final TrendAnalysis memoryTrend,
        final TrendAnalysis throughputTrend) {
      this.samples = Collections.unmodifiableList(new ArrayList<>(samples));
      this.executionTimeTrend = executionTimeTrend;
      this.memoryTrend = memoryTrend;
      this.throughputTrend = throughputTrend;
    }

    public List<PerformanceSample> getSamples() {
      return samples;
    }

    public TrendAnalysis getExecutionTimeTrend() {
      return executionTimeTrend;
    }

    public TrendAnalysis getMemoryTrend() {
      return memoryTrend;
    }

    public TrendAnalysis getThroughputTrend() {
      return throughputTrend;
    }
  }

  /** Performance sample data point. */
  public static final class PerformanceSample {
    private final long timestamp;
    private final double executionTime;
    private final long memoryUsage;
    private final long callCount;

    public PerformanceSample(
        final long timestamp,
        final double executionTime,
        final long memoryUsage,
        final long callCount) {
      this.timestamp = timestamp;
      this.executionTime = executionTime;
      this.memoryUsage = memoryUsage;
      this.callCount = callCount;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public double getExecutionTime() {
      return executionTime;
    }

    public long getMemoryUsage() {
      return memoryUsage;
    }

    public long getCallCount() {
      return callCount;
    }
  }

  /** Trend analysis result. */
  public static final class TrendAnalysis {
    private final PerformanceTrend trend;
    private final double slope;
    private final double correlation;
    private final String description;

    public TrendAnalysis(
        final PerformanceTrend trend,
        final double slope,
        final double correlation,
        final String description) {
      this.trend = trend;
      this.slope = slope;
      this.correlation = correlation;
      this.description = description;
    }

    public PerformanceTrend getTrend() {
      return trend;
    }

    public double getSlope() {
      return slope;
    }

    public double getCorrelation() {
      return correlation;
    }

    public String getDescription() {
      return description;
    }
  }

  /** Performance comparison result. */
  public static final class PerformanceComparison {
    private final PerformanceMetrics current;
    private final PerformanceMetrics baseline;
    private final Map<String, ComparisonResult> functionComparisons;
    private final double overallPerformanceChange;

    public PerformanceComparison(
        final PerformanceMetrics current,
        final PerformanceMetrics baseline,
        final Map<String, ComparisonResult> functionComparisons,
        final double overallPerformanceChange) {
      this.current = current;
      this.baseline = baseline;
      this.functionComparisons = Collections.unmodifiableMap(new HashMap<>(functionComparisons));
      this.overallPerformanceChange = overallPerformanceChange;
    }

    public PerformanceMetrics getCurrent() {
      return current;
    }

    public PerformanceMetrics getBaseline() {
      return baseline;
    }

    public Map<String, ComparisonResult> getFunctionComparisons() {
      return functionComparisons;
    }

    public double getOverallPerformanceChange() {
      return overallPerformanceChange;
    }
  }

  /** Function comparison result. */
  public static final class ComparisonResult {
    private final String functionName;
    private final double executionTimeChange;
    private final double memoryUsageChange;
    private final double callCountChange;
    private final PerformanceTrend overallTrend;

    public ComparisonResult(
        final String functionName,
        final double executionTimeChange,
        final double memoryUsageChange,
        final double callCountChange,
        final PerformanceTrend overallTrend) {
      this.functionName = functionName;
      this.executionTimeChange = executionTimeChange;
      this.memoryUsageChange = memoryUsageChange;
      this.callCountChange = callCountChange;
      this.overallTrend = overallTrend;
    }

    public String getFunctionName() {
      return functionName;
    }

    public double getExecutionTimeChange() {
      return executionTimeChange;
    }

    public double getMemoryUsageChange() {
      return memoryUsageChange;
    }

    public double getCallCountChange() {
      return callCountChange;
    }

    public PerformanceTrend getOverallTrend() {
      return overallTrend;
    }
  }

  /** Performance baseline for comparison. */
  public static final class PerformanceBaseline {
    private final PerformanceMetrics metrics;
    private final Map<String, FunctionProfile> functionProfiles;

    public PerformanceBaseline(
        final PerformanceMetrics metrics, final Map<String, FunctionProfile> functionProfiles) {
      this.metrics = metrics;
      this.functionProfiles = Collections.unmodifiableMap(new HashMap<>(functionProfiles));
    }

    public PerformanceMetrics getMetrics() {
      return metrics;
    }

    public Map<String, FunctionProfile> getFunctionProfiles() {
      return functionProfiles;
    }

    public FunctionProfile getFunctionProfile(final String functionName) {
      return functionProfiles.get(functionName);
    }
  }

  /** Profiling data from native layer. */
  public static final class ProfilingData {
    private final Map<String, Long> functionExecutionTimes;
    private final Map<String, Long> functionCallCounts;
    private final Map<String, Long> memoryAllocations;

    public ProfilingData(
        final Map<String, Long> functionExecutionTimes,
        final Map<String, Long> functionCallCounts,
        final Map<String, Long> memoryAllocations) {
      this.functionExecutionTimes =
          Collections.unmodifiableMap(new HashMap<>(functionExecutionTimes));
      this.functionCallCounts = Collections.unmodifiableMap(new HashMap<>(functionCallCounts));
      this.memoryAllocations = Collections.unmodifiableMap(new HashMap<>(memoryAllocations));
    }

    public Map<String, Long> getFunctionExecutionTimes() {
      return functionExecutionTimes;
    }

    public Map<String, Long> getFunctionCallCounts() {
      return functionCallCounts;
    }

    public Map<String, Long> getMemoryAllocations() {
      return memoryAllocations;
    }
  }

  public enum RecommendationType {
    HOTSPOT_OPTIMIZATION,
    MEMORY_OPTIMIZATION,
    INLINING,
    CACHING,
    ALGORITHM_IMPROVEMENT
  }

  public enum RecommendationPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }

  public enum BottleneckType {
    CPU_BOUND,
    MEMORY_BOUND,
    IO_BOUND,
    SYNCHRONIZATION
  }

  public enum PerformanceTrend {
    IMPROVING,
    STABLE,
    DEGRADING
  }
}
