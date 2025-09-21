package ai.tegmentum.wasmtime4j.performance;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.performance.events.PerformanceListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Default implementation of the PerformanceProfiler interface.
 *
 * <p>This implementation provides basic performance monitoring capabilities with minimal overhead.
 * It tracks basic metrics and provides configurable sampling intervals.
 *
 * @since 1.0.0
 */
final class DefaultPerformanceProfiler implements PerformanceProfiler {

  private static final Logger LOGGER = Logger.getLogger(DefaultPerformanceProfiler.class.getName());

  private final Engine engine;
  private final AtomicBoolean profiling = new AtomicBoolean(false);
  private final AtomicBoolean paused = new AtomicBoolean(false);
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final List<PerformanceListener> listeners = new CopyOnWriteArrayList<>();
  private final List<ProfileSnapshot> snapshots = new CopyOnWriteArrayList<>();
  private final AtomicInteger snapshotCounter = new AtomicInteger(0);

  private volatile ProfilerConfig config;
  private volatile ScheduledExecutorService executor;
  private volatile long startTime;

  /**
   * Creates a new default performance profiler.
   *
   * @param engine the engine to profile
   * @param config the profiler configuration
   */
  DefaultPerformanceProfiler(final Engine engine, final ProfilerConfig config) {
    this.engine = engine;
    this.config = config;
    LOGGER.fine("Created default performance profiler for engine: " + engine.getClass().getSimpleName());
  }

  @Override
  public void startProfiling() {
    if (closed.get()) {
      throw new IllegalStateException("Profiler has been closed");
    }
    if (profiling.get()) {
      throw new IllegalStateException("Profiling is already active");
    }

    startTime = System.currentTimeMillis();
    profiling.set(true);
    paused.set(false);

    // Start background sampling if configured
    if (config.isRealTimeEnabled()) {
      executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "PerformanceProfiler-Sampler");
        t.setDaemon(true);
        return t;
      });

      long intervalMs = config.getSamplingInterval().toMillis();
      executor.scheduleAtFixedRate(this::sampleMetrics, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    LOGGER.fine("Started performance profiling");
  }

  @Override
  public void stopProfiling() {
    if (!profiling.get()) {
      throw new IllegalStateException("Profiling is not active");
    }

    profiling.set(false);
    paused.set(false);

    if (executor != null) {
      executor.shutdown();
      try {
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
          executor.shutdownNow();
        }
      } catch (InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      }
      executor = null;
    }

    LOGGER.fine("Stopped performance profiling");
  }

  @Override
  public void pauseProfiling() {
    if (!profiling.get()) {
      throw new IllegalStateException("Profiling is not active");
    }
    if (paused.get()) {
      throw new IllegalStateException("Profiling is already paused");
    }

    paused.set(true);
    LOGGER.fine("Paused performance profiling");
  }

  @Override
  public void resumeProfiling() {
    if (!profiling.get()) {
      throw new IllegalStateException("Profiling is not active");
    }
    if (!paused.get()) {
      throw new IllegalStateException("Profiling is not paused");
    }

    paused.set(false);
    LOGGER.fine("Resumed performance profiling");
  }

  @Override
  public boolean isProfiling() {
    return profiling.get() && !paused.get();
  }

  @Override
  public boolean isPaused() {
    return paused.get();
  }

  @Override
  public ProfileSnapshot captureSnapshot() {
    if (closed.get()) {
      throw new IllegalStateException("Profiler has been closed");
    }

    long timestamp = System.currentTimeMillis();
    PerformanceMetrics metrics = getCurrentMetrics();
    ProfileSnapshot snapshot = new DefaultProfileSnapshot(
        snapshotCounter.incrementAndGet(),
        timestamp,
        metrics,
        config
    );

    // Maintain snapshot limit
    snapshots.add(snapshot);
    while (snapshots.size() > config.getMaxSnapshots()) {
      snapshots.remove(0);
    }

    LOGGER.fine("Captured performance snapshot #" + snapshot.getId());
    return snapshot;
  }

  @Override
  public List<ProfileSnapshot> getSnapshots() {
    return new ArrayList<>(snapshots);
  }

  @Override
  public void clearSnapshots() {
    snapshots.clear();
    snapshotCounter.set(0);
    LOGGER.fine("Cleared all performance snapshots");
  }

  @Override
  public void addPerformanceListener(final PerformanceListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }
    listeners.add(listener);
    LOGGER.fine("Added performance listener: " + listener.getClass().getSimpleName());
  }

  @Override
  public boolean removePerformanceListener(final PerformanceListener listener) {
    boolean removed = listeners.remove(listener);
    if (removed) {
      LOGGER.fine("Removed performance listener: " + listener.getClass().getSimpleName());
    }
    return removed;
  }

  @Override
  public ProfilerConfig getConfig() {
    return config;
  }

  @Override
  public void updateConfig(final ProfilerConfig newConfig) {
    if (newConfig == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }

    boolean wasRealTime = config.isRealTimeEnabled();
    this.config = newConfig;

    // Restart sampling if real-time setting changed
    if (profiling.get() && wasRealTime != newConfig.isRealTimeEnabled()) {
      stopProfiling();
      startProfiling();
    }

    LOGGER.fine("Updated profiler configuration");
  }

  @Override
  public PerformanceMetrics getCurrentMetrics() {
    long currentTime = System.currentTimeMillis();
    long uptime = profiling.get() ? currentTime - startTime : 0;

    // Create basic metrics - in a real implementation, these would come from
    // the native layer or JVM monitoring
    return new DefaultPerformanceMetrics(
        currentTime,
        uptime,
        getMemoryUsage(),
        getCpuUsage(),
        0.0, // Network I/O not implemented
        0.0, // Disk I/O not implemented
        0, // Function calls not tracked yet
        Collections.emptyMap() // Function profiles not implemented
    );
  }

  @Override
  public GcImpactMetrics measureGcImpact() {
    // Capture before metrics
    PerformanceMetrics beforeMetrics = getCurrentMetrics();
    long beforeGcTime = getGcTime();

    // Force garbage collection
    System.gc();
    System.runFinalization();

    // Small delay to allow GC to complete
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Capture after metrics
    PerformanceMetrics afterMetrics = getCurrentMetrics();
    long afterGcTime = getGcTime();

    long gcTime = afterGcTime - beforeGcTime;
    double memoryFreed = beforeMetrics.getMemoryUsage() - afterMetrics.getMemoryUsage();

    return new DefaultGcImpactMetrics(gcTime, memoryFreed, beforeMetrics, afterMetrics);
  }

  @Override
  public double getProfilerOverhead() {
    if (!profiling.get()) {
      return 0.0;
    }
    return config.getEstimatedOverhead();
  }

  @Override
  public String exportData(final ExportFormat format) {
    if (format == null) {
      throw new IllegalArgumentException("Format cannot be null");
    }

    switch (format) {
      case JSON:
        return exportAsJson();
      case CSV:
        return exportAsCsv();
      default:
        throw new IllegalArgumentException("Unsupported export format: " + format);
    }
  }

  @Override
  public void close() {
    if (closed.getAndSet(true)) {
      return; // Already closed
    }

    if (profiling.get()) {
      try {
        stopProfiling();
      } catch (Exception e) {
        LOGGER.warning("Error stopping profiling during close: " + e.getMessage());
      }
    }

    listeners.clear();
    snapshots.clear();
    LOGGER.fine("Closed performance profiler");
  }

  private void sampleMetrics() {
    if (!profiling.get() || paused.get()) {
      return;
    }

    try {
      captureSnapshot();
    } catch (Exception e) {
      LOGGER.warning("Error sampling metrics: " + e.getMessage());
    }
  }

  private double getMemoryUsage() {
    Runtime runtime = Runtime.getRuntime();
    long totalMemory = runtime.totalMemory();
    long freeMemory = runtime.freeMemory();
    long usedMemory = totalMemory - freeMemory;
    return (double) usedMemory / (1024 * 1024); // Convert to MB
  }

  private double getCpuUsage() {
    // This is a simplified CPU usage estimation
    // In a real implementation, this would use JMX or native monitoring
    return Math.random() * 0.1; // Return a small random value for now
  }

  private long getGcTime() {
    // Simplified GC time tracking
    // In a real implementation, this would use GarbageCollectorMXBean
    return System.currentTimeMillis();
  }

  private String exportAsJson() {
    StringBuilder json = new StringBuilder();
    json.append("{\"snapshots\":[");

    for (int i = 0; i < snapshots.size(); i++) {
      if (i > 0) json.append(",");
      ProfileSnapshot snapshot = snapshots.get(i);
      json.append("{\"id\":").append(snapshot.getId())
          .append(",\"timestamp\":").append(snapshot.getTimestamp())
          .append(",\"memory\":").append(snapshot.getMetrics().getMemoryUsage())
          .append(",\"cpu\":").append(snapshot.getMetrics().getCpuUsage())
          .append("}");
    }

    json.append("]}");
    return json.toString();
  }

  private String exportAsCsv() {
    StringBuilder csv = new StringBuilder();
    csv.append("id,timestamp,memory,cpu\n");

    for (ProfileSnapshot snapshot : snapshots) {
      csv.append(snapshot.getId()).append(",")
         .append(snapshot.getTimestamp()).append(",")
         .append(snapshot.getMetrics().getMemoryUsage()).append(",")
         .append(snapshot.getMetrics().getCpuUsage()).append("\n");
    }

    return csv.toString();
  }
}