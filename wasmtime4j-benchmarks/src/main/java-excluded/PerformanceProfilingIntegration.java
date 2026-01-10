package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.RuntimeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Comprehensive performance profiling integration for Wasmtime4j.
 *
 * <p>This framework provides detailed performance profiling with JVM metrics collection, garbage
 * collection analysis, memory usage tracking, and thread utilization monitoring. It integrates with
 * JMH profilers and provides comprehensive performance insights.
 *
 * <p>Key features: - JVM metrics collection (GC, memory, threads) - Performance profiling with
 * multiple profilers - Resource utilization monitoring - Memory allocation tracking - Garbage
 * collection impact analysis - Thread contention detection - Performance bottleneck identification
 * - Comprehensive metrics export for visualization
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {"-Xms4g", "-Xmx4g", "-XX:+UseG1GC"})
public class PerformanceProfilingIntegration extends BenchmarkBase {

  private static final Logger LOGGER =
      Logger.getLogger(PerformanceProfilingIntegration.class.getName());
  private static final ObjectMapper JSON_MAPPER = createJsonMapper();

  // Profiling configuration
  @Param({"JNI", "PANAMA"})
  private String runtimeTypeName;

  // Profiling state
  private ProfilingSession profilingSession;
  private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
  private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
  private final List<GarbageCollectorMXBean> gcMXBeans =
      ManagementFactory.getGarbageCollectorMXBeans();

  /** Comprehensive profiling session for collecting detailed performance metrics. */
  public static final class ProfilingSession {
    private final String sessionId;
    private final String benchmarkName;
    private final String runtimeType;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private final List<PerformanceSnapshot> snapshots;
    private final Map<String, Object> sessionMetadata;

    public ProfilingSession(
        final String sessionId, final String benchmarkName, final String runtimeType) {
      this.sessionId = validateNotNull(sessionId, "sessionId");
      this.benchmarkName = validateNotNull(benchmarkName, "benchmarkName");
      this.runtimeType = validateNotNull(runtimeType, "runtimeType");
      this.startTime = LocalDateTime.now();
      this.snapshots = new ArrayList<>();
      this.sessionMetadata = new HashMap<>();
    }

    public void addSnapshot(final PerformanceSnapshot snapshot) {
      snapshots.add(validateNotNull(snapshot, "snapshot"));
    }

    public void addMetadata(final String key, final Object value) {
      sessionMetadata.put(validateNotNull(key, "key"), value);
    }

    public void endSession() {
      this.endTime = LocalDateTime.now();
    }

    // Getters
    public String getSessionId() {
      return sessionId;
    }

    public String getBenchmarkName() {
      return benchmarkName;
    }

    public String getRuntimeType() {
      return runtimeType;
    }

    public LocalDateTime getStartTime() {
      return startTime;
    }

    public LocalDateTime getEndTime() {
      return endTime;
    }

    public List<PerformanceSnapshot> getSnapshots() {
      return new ArrayList<>(snapshots);
    }

    public Map<String, Object> getSessionMetadata() {
      return new HashMap<>(sessionMetadata);
    }

    public long getDurationMillis() {
      if (endTime == null) {
        return -1;
      }
      return java.time.Duration.between(startTime, endTime).toMillis();
    }

    private static <T> T validateNotNull(final T value, final String fieldName) {
      if (value == null) {
        throw new IllegalArgumentException(fieldName + " cannot be null");
      }
      return value;
    }
  }

  /** Performance snapshot containing detailed JVM and application metrics. */
  public static final class PerformanceSnapshot {
    private final LocalDateTime timestamp;
    private final MemoryMetrics memoryMetrics;
    private final GarbageCollectionMetrics gcMetrics;
    private final ThreadMetrics threadMetrics;
    private final SystemMetrics systemMetrics;
    private final Map<String, Object> customMetrics;

    public PerformanceSnapshot(
        final MemoryMetrics memoryMetrics,
        final GarbageCollectionMetrics gcMetrics,
        final ThreadMetrics threadMetrics,
        final SystemMetrics systemMetrics,
        final Map<String, Object> customMetrics) {
      this.timestamp = LocalDateTime.now();
      this.memoryMetrics = validateNotNull(memoryMetrics, "memoryMetrics");
      this.gcMetrics = validateNotNull(gcMetrics, "gcMetrics");
      this.threadMetrics = validateNotNull(threadMetrics, "threadMetrics");
      this.systemMetrics = validateNotNull(systemMetrics, "systemMetrics");
      this.customMetrics = customMetrics != null ? new HashMap<>(customMetrics) : new HashMap<>();
    }

    // Getters
    public LocalDateTime getTimestamp() {
      return timestamp;
    }

    public MemoryMetrics getMemoryMetrics() {
      return memoryMetrics;
    }

    public GarbageCollectionMetrics getGcMetrics() {
      return gcMetrics;
    }

    public ThreadMetrics getThreadMetrics() {
      return threadMetrics;
    }

    public SystemMetrics getSystemMetrics() {
      return systemMetrics;
    }

    public Map<String, Object> getCustomMetrics() {
      return new HashMap<>(customMetrics);
    }

    private static <T> T validateNotNull(final T value, final String fieldName) {
      if (value == null) {
        throw new IllegalArgumentException(fieldName + " cannot be null");
      }
      return value;
    }
  }

  /** Memory usage metrics. */
  public static final class MemoryMetrics {
    private final long heapUsed;
    private final long heapMax;
    private final long heapCommitted;
    private final long nonHeapUsed;
    private final long nonHeapMax;
    private final long nonHeapCommitted;
    private final double heapUtilization;

    public MemoryMetrics(final MemoryUsage heapMemory, final MemoryUsage nonHeapMemory) {
      this.heapUsed = heapMemory.getUsed();
      this.heapMax = heapMemory.getMax();
      this.heapCommitted = heapMemory.getCommitted();
      this.nonHeapUsed = nonHeapMemory.getUsed();
      this.nonHeapMax = nonHeapMemory.getMax();
      this.nonHeapCommitted = nonHeapMemory.getCommitted();
      this.heapUtilization = heapMax > 0 ? (double) heapUsed / heapMax : 0.0;
    }

    // Getters
    public long getHeapUsed() {
      return heapUsed;
    }

    public long getHeapMax() {
      return heapMax;
    }

    public long getHeapCommitted() {
      return heapCommitted;
    }

    public long getNonHeapUsed() {
      return nonHeapUsed;
    }

    public long getNonHeapMax() {
      return nonHeapMax;
    }

    public long getNonHeapCommitted() {
      return nonHeapCommitted;
    }

    public double getHeapUtilization() {
      return heapUtilization;
    }
  }

  /** Garbage collection metrics. */
  public static final class GarbageCollectionMetrics {
    private final Map<String, Long> gcCollectionCounts;
    private final Map<String, Long> gcCollectionTimes;
    private final long totalCollections;
    private final long totalCollectionTime;

    public GarbageCollectionMetrics(final List<GarbageCollectorMXBean> gcMXBeans) {
      this.gcCollectionCounts = new HashMap<>();
      this.gcCollectionTimes = new HashMap<>();

      long totalCollections = 0;
      long totalTime = 0;

      for (final GarbageCollectorMXBean gcBean : gcMXBeans) {
        final String name = gcBean.getName();
        final long collections = gcBean.getCollectionCount();
        final long time = gcBean.getCollectionTime();

        gcCollectionCounts.put(name, collections);
        gcCollectionTimes.put(name, time);

        if (collections > 0) {
          totalCollections += collections;
        }
        if (time > 0) {
          totalTime += time;
        }
      }

      this.totalCollections = totalCollections;
      this.totalCollectionTime = totalTime;
    }

    // Getters
    public Map<String, Long> getGcCollectionCounts() {
      return new HashMap<>(gcCollectionCounts);
    }

    public Map<String, Long> getGcCollectionTimes() {
      return new HashMap<>(gcCollectionTimes);
    }

    public long getTotalCollections() {
      return totalCollections;
    }

    public long getTotalCollectionTime() {
      return totalCollectionTime;
    }
  }

  /** Thread metrics. */
  public static final class ThreadMetrics {
    private final int threadCount;
    private final int peakThreadCount;
    private final int daemonThreadCount;
    private final long totalStartedThreadCount;

    public ThreadMetrics(final ThreadMXBean threadMXBean) {
      this.threadCount = threadMXBean.getThreadCount();
      this.peakThreadCount = threadMXBean.getPeakThreadCount();
      this.daemonThreadCount = threadMXBean.getDaemonThreadCount();
      this.totalStartedThreadCount = threadMXBean.getTotalStartedThreadCount();
    }

    // Getters
    public int getThreadCount() {
      return threadCount;
    }

    public int getPeakThreadCount() {
      return peakThreadCount;
    }

    public int getDaemonThreadCount() {
      return daemonThreadCount;
    }

    public long getTotalStartedThreadCount() {
      return totalStartedThreadCount;
    }
  }

  /** System metrics. */
  public static final class SystemMetrics {
    private final int availableProcessors;
    private final double systemLoadAverage;
    private final long freeMemory;
    private final long totalMemory;
    private final long maxMemory;

    public SystemMetrics() {
      final Runtime runtime = Runtime.getRuntime();
      this.availableProcessors = runtime.availableProcessors();
      this.systemLoadAverage = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
      this.freeMemory = runtime.freeMemory();
      this.totalMemory = runtime.totalMemory();
      this.maxMemory = runtime.maxMemory();
    }

    // Getters
    public int getAvailableProcessors() {
      return availableProcessors;
    }

    public double getSystemLoadAverage() {
      return systemLoadAverage;
    }

    public long getFreeMemory() {
      return freeMemory;
    }

    public long getTotalMemory() {
      return totalMemory;
    }

    public long getMaxMemory() {
      return maxMemory;
    }
  }

  /** Performance analysis result. */
  public static final class PerformanceAnalysisResult {
    private final ProfilingSession session;
    private final PerformanceInsights insights;
    private final List<String> recommendations;
    private final Map<String, Double> keyMetrics;

    public PerformanceAnalysisResult(final ProfilingSession session) {
      this.session = validateNotNull(session, "session");
      this.insights = analyzePerformanceInsights(session);
      this.recommendations = generateRecommendations(session, insights);
      this.keyMetrics = extractKeyMetrics(session);
    }

    // Getters
    public ProfilingSession getSession() {
      return session;
    }

    public PerformanceInsights getInsights() {
      return insights;
    }

    public List<String> getRecommendations() {
      return new ArrayList<>(recommendations);
    }

    public Map<String, Double> getKeyMetrics() {
      return new HashMap<>(keyMetrics);
    }

    private PerformanceInsights analyzePerformanceInsights(final ProfilingSession session) {
      final List<PerformanceSnapshot> snapshots = session.getSnapshots();
      if (snapshots.isEmpty()) {
        return new PerformanceInsights(0.0, 0.0, 0.0, 0.0, "No performance data available");
      }

      // Calculate average metrics
      final double avgHeapUtilization =
          snapshots.stream()
              .mapToDouble(s -> s.getMemoryMetrics().getHeapUtilization())
              .average()
              .orElse(0.0);

      final double avgGcTime =
          snapshots.stream()
              .mapToDouble(s -> (double) s.getGcMetrics().getTotalCollectionTime())
              .average()
              .orElse(0.0);

      final double avgThreadCount =
          snapshots.stream()
              .mapToDouble(s -> s.getThreadMetrics().getThreadCount())
              .average()
              .orElse(0.0);

      final double avgSystemLoad =
          snapshots.stream()
              .mapToDouble(s -> s.getSystemMetrics().getSystemLoadAverage())
              .filter(load -> load >= 0) // Filter out unavailable values (-1)
              .average()
              .orElse(0.0);

      final String analysis =
          String.format(
              "Session analyzed %d snapshots over %d ms. Average heap utilization: %.1f%%, "
                  + "GC time: %.0f ms, thread count: %.0f, system load: %.2f",
              snapshots.size(),
              session.getDurationMillis(),
              avgHeapUtilization * 100,
              avgGcTime,
              avgThreadCount,
              avgSystemLoad);

      return new PerformanceInsights(
          avgHeapUtilization, avgGcTime, avgThreadCount, avgSystemLoad, analysis);
    }

    private List<String> generateRecommendations(
        final ProfilingSession session, final PerformanceInsights insights) {
      final List<String> recommendations = new ArrayList<>();

      if (insights.getAvgHeapUtilization() > 0.8) {
        recommendations.add(
            "High heap utilization detected (>80%). Consider increasing heap size or optimizing"
                + " memory usage.");
      }

      if (insights.getAvgGcTime() > 100) {
        recommendations.add(
            "High GC overhead detected (>100ms average). Consider tuning GC parameters or reducing"
                + " allocation pressure.");
      }

      if (insights.getAvgThreadCount() > Runtime.getRuntime().availableProcessors() * 2) {
        recommendations.add(
            "High thread count relative to available processors. Consider thread pool"
                + " optimization.");
      }

      if (insights.getAvgSystemLoad() > Runtime.getRuntime().availableProcessors()) {
        recommendations.add(
            "System load average exceeds available processors. Consider reducing concurrent"
                + " operations.");
      }

      if (session.getDurationMillis() > 30000) { // 30 seconds
        recommendations.add(
            "Long benchmark duration detected. Consider shorter measurement periods for more"
                + " responsive feedback.");
      }

      if (recommendations.isEmpty()) {
        recommendations.add(
            "Performance metrics are within acceptable ranges. No specific optimizations needed.");
      }

      return recommendations;
    }

    private Map<String, Double> extractKeyMetrics(final ProfilingSession session) {
      final Map<String, Double> metrics = new HashMap<>();
      final List<PerformanceSnapshot> snapshots = session.getSnapshots();

      if (snapshots.isEmpty()) {
        return metrics;
      }

      // Aggregate key metrics
      metrics.put(
          "avgHeapUtilization",
          snapshots.stream()
              .mapToDouble(s -> s.getMemoryMetrics().getHeapUtilization())
              .average()
              .orElse(0.0));

      metrics.put(
          "maxHeapUsed",
          (double)
              snapshots.stream()
                  .mapToLong(s -> s.getMemoryMetrics().getHeapUsed())
                  .max()
                  .orElse(0L));

      metrics.put(
          "totalGcCollections",
          (double)
              snapshots.stream()
                  .mapToLong(s -> s.getGcMetrics().getTotalCollections())
                  .max()
                  .orElse(0L));

      metrics.put(
          "totalGcTime",
          (double)
              snapshots.stream()
                  .mapToLong(s -> s.getGcMetrics().getTotalCollectionTime())
                  .max()
                  .orElse(0L));

      metrics.put(
          "peakThreadCount",
          (double)
              snapshots.stream()
                  .mapToInt(s -> s.getThreadMetrics().getPeakThreadCount())
                  .max()
                  .orElse(0));

      metrics.put("sessionDuration", (double) session.getDurationMillis());

      return metrics;
    }

    private static <T> T validateNotNull(final T value, final String fieldName) {
      if (value == null) {
        throw new IllegalArgumentException(fieldName + " cannot be null");
      }
      return value;
    }
  }

  /** Performance insights container. */
  public static final class PerformanceInsights {
    private final double avgHeapUtilization;
    private final double avgGcTime;
    private final double avgThreadCount;
    private final double avgSystemLoad;
    private final String analysis;

    public PerformanceInsights(
        final double avgHeapUtilization,
        final double avgGcTime,
        final double avgThreadCount,
        final double avgSystemLoad,
        final String analysis) {
      this.avgHeapUtilization = avgHeapUtilization;
      this.avgGcTime = avgGcTime;
      this.avgThreadCount = avgThreadCount;
      this.avgSystemLoad = avgSystemLoad;
      this.analysis = analysis;
    }

    // Getters
    public double getAvgHeapUtilization() {
      return avgHeapUtilization;
    }

    public double getAvgGcTime() {
      return avgGcTime;
    }

    public double getAvgThreadCount() {
      return avgThreadCount;
    }

    public double getAvgSystemLoad() {
      return avgSystemLoad;
    }

    public String getAnalysis() {
      return analysis;
    }
  }

  @Setup(org.openjdk.jmh.annotations.Level.Trial)
  public void setupProfiling() {
    final String sessionId = "session_" + System.currentTimeMillis();
    profilingSession = new ProfilingSession(sessionId, "ProfilingBenchmark", runtimeTypeName);

    // Add session metadata
    profilingSession.addMetadata("javaVersion", System.getProperty("java.version"));
    profilingSession.addMetadata("jvmName", System.getProperty("java.vm.name"));
    profilingSession.addMetadata("osName", System.getProperty("os.name"));
    profilingSession.addMetadata("osArch", System.getProperty("os.arch"));

    LOGGER.info("Performance profiling session started: " + sessionId);
  }

  @TearDown(org.openjdk.jmh.annotations.Level.Trial)
  public void teardownProfiling() {
    if (profilingSession != null) {
      profilingSession.endSession();
      LOGGER.info("Performance profiling session ended: " + profilingSession.getSessionId());

      try {
        final PerformanceAnalysisResult result = new PerformanceAnalysisResult(profilingSession);
        saveProfilingResults(result);
      } catch (final Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to save profiling results", e);
      }
    }
  }

  @Setup(org.openjdk.jmh.annotations.Level.Invocation)
  public void captureSnapshot() {
    if (profilingSession != null) {
      final MemoryMetrics memoryMetrics =
          new MemoryMetrics(
              memoryMXBean.getHeapMemoryUsage(), memoryMXBean.getNonHeapMemoryUsage());

      final GarbageCollectionMetrics gcMetrics = new GarbageCollectionMetrics(gcMXBeans);
      final ThreadMetrics threadMetrics = new ThreadMetrics(threadMXBean);
      final SystemMetrics systemMetrics = new SystemMetrics();

      final PerformanceSnapshot snapshot =
          new PerformanceSnapshot(memoryMetrics, gcMetrics, threadMetrics, systemMetrics, null);

      profilingSession.addSnapshot(snapshot);
    }
  }

  /** Profiled benchmark for JNI runtime. */
  @Benchmark
  public int profiledBenchmarkJni() throws Exception {
    if (!"JNI".equals(runtimeTypeName)) {
      return preventOptimization(0);
    }

    final var runtime = createRuntime(RuntimeType.JNI);
    final var engine = createEngine(runtime);
    final var store = createStore(engine);
    final var module = compileModule(engine, COMPLEX_WASM_MODULE);
    final var instance = instantiateModule(store, module);

    // Perform memory-intensive operations
    return performMemoryIntensiveWork();
  }

  /** Profiled benchmark for Panama runtime. */
  @Benchmark
  public int profiledBenchmarkPanama() throws Exception {
    if (!"PANAMA".equals(runtimeTypeName) || getJavaVersion() < 23) {
      return preventOptimization(0);
    }

    final var runtime = createRuntime(RuntimeType.PANAMA);
    final var engine = createEngine(runtime);
    final var store = createStore(engine);
    final var module = compileModule(engine, COMPLEX_WASM_MODULE);
    final var instance = instantiateModule(store, module);

    // Perform memory-intensive operations
    return performMemoryIntensiveWork();
  }

  /** Performs memory-intensive work to generate profiling data. */
  private int performMemoryIntensiveWork() {
    int result = 0;
    final List<byte[]> allocations = new ArrayList<>();

    // Create memory pressure
    for (int i = 0; i < 100; i++) {
      final byte[] buffer = new byte[1024 * 16]; // 16KB allocations
      for (int j = 0; j < buffer.length; j++) {
        buffer[j] = (byte) (i + j);
      }
      allocations.add(buffer);
      result += buffer.length;
    }

    // Force some GC activity
    if (allocations.size() > 50) {
      allocations.subList(0, 25).clear();
    }

    return preventOptimization(result);
  }

  /** Creates a configured JSON mapper for profiling data serialization. */
  private static ObjectMapper createJsonMapper() {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }

  /** Saves comprehensive profiling results to file. */
  private void saveProfilingResults(final PerformanceAnalysisResult result) throws IOException {
    final Path resultsDir = Paths.get("profiling-results");
    Files.createDirectories(resultsDir);

    final String filename =
        String.format(
            "profiling-results_%s_%s.json",
            result.getSession().getSessionId(),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

    final Path resultFile = resultsDir.resolve(filename);

    final ObjectNode root = JSON_MAPPER.createObjectNode();
    root.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

    // Session information
    final ObjectNode sessionNode = JSON_MAPPER.createObjectNode();
    sessionNode.put("sessionId", result.getSession().getSessionId());
    sessionNode.put("benchmarkName", result.getSession().getBenchmarkName());
    sessionNode.put("runtimeType", result.getSession().getRuntimeType());
    sessionNode.put("duration", result.getSession().getDurationMillis());
    root.set("session", sessionNode);

    // Performance insights
    final ObjectNode insightsNode = JSON_MAPPER.createObjectNode();
    insightsNode.put("avgHeapUtilization", result.getInsights().getAvgHeapUtilization());
    insightsNode.put("avgGcTime", result.getInsights().getAvgGcTime());
    insightsNode.put("avgThreadCount", result.getInsights().getAvgThreadCount());
    insightsNode.put("avgSystemLoad", result.getInsights().getAvgSystemLoad());
    insightsNode.put("analysis", result.getInsights().getAnalysis());
    root.set("insights", insightsNode);

    // Recommendations
    final ArrayNode recommendationsArray = JSON_MAPPER.createArrayNode();
    for (final String recommendation : result.getRecommendations()) {
      recommendationsArray.add(recommendation);
    }
    root.set("recommendations", recommendationsArray);

    // Key metrics
    final ObjectNode metricsNode = JSON_MAPPER.createObjectNode();
    for (final Map.Entry<String, Double> entry : result.getKeyMetrics().entrySet()) {
      metricsNode.put(entry.getKey(), entry.getValue());
    }
    root.set("keyMetrics", metricsNode);

    Files.write(
        resultFile,
        JSON_MAPPER.writeValueAsBytes(root),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);

    LOGGER.info("Profiling results saved to: " + resultFile);
  }

  /** Main method for running performance profiling benchmarks. */
  public static void main(final String[] args) throws Exception {
    LOGGER.info("Starting comprehensive performance profiling");

    final Options options =
        new OptionsBuilder()
            .include(PerformanceProfilingIntegration.class.getSimpleName())
            .addProfiler(GCProfiler.class)
            .param("runtimeTypeName", "JNI", "PANAMA")
            .forks(2)
            .warmupIterations(5)
            .measurementIterations(10)
            .build();

    final var results = new Runner(options).run();

    LOGGER.info("Performance profiling completed with " + results.size() + " benchmark results");

    // Generate summary report
    generateProfilingSummaryReport(results);
  }

  /** Generates a summary report for all profiling sessions. */
  private static void generateProfilingSummaryReport(final Iterable<RunResult> results) {
    final StringBuilder report = new StringBuilder();
    report.append("PERFORMANCE PROFILING SUMMARY REPORT\n");
    report.append("===================================\n\n");

    report
        .append("Analysis Date: ")
        .append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .append("\n");
    report
        .append("JVM: ")
        .append(System.getProperty("java.vm.name"))
        .append(" ")
        .append(System.getProperty("java.version"))
        .append("\n");
    report
        .append("OS: ")
        .append(System.getProperty("os.name"))
        .append(" ")
        .append(System.getProperty("os.arch"))
        .append("\n\n");

    int benchmarkCount = 0;
    for (final RunResult result : results) {
      benchmarkCount++;
      report.append("Benchmark: ").append(result.getParams().getBenchmark()).append("\n");
      report
          .append("Score: ")
          .append(String.format("%.2f", result.getPrimaryResult().getScore()))
          .append(" ")
          .append(result.getPrimaryResult().getScoreUnit())
          .append("\n");
      report
          .append("Error: ±")
          .append(String.format("%.2f", result.getPrimaryResult().getScoreError()))
          .append("\n");

      // Add GC profiler results if available
      if (result.getSecondaryResults().containsKey("gc.alloc.rate")) {
        final double allocRate = result.getSecondaryResults().get("gc.alloc.rate").getScore();
        report
            .append("Allocation Rate: ")
            .append(String.format("%.2f", allocRate))
            .append(" MB/sec\n");
      }

      if (result.getSecondaryResults().containsKey("gc.time")) {
        final double gcTime = result.getSecondaryResults().get("gc.time").getScore();
        report.append("GC Time: ").append(String.format("%.2f", gcTime)).append(" ms\n");
      }

      report.append("\n");
    }

    report.append("Total Benchmarks Analyzed: ").append(benchmarkCount).append("\n");

    System.out.println(report.toString());
    LOGGER.info("Profiling summary report generated");
  }
}
