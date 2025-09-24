/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.monitoring;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Runtime diagnostics collector providing detailed insights into WebAssembly runtime operations,
 * instance lifecycle, memory usage patterns, and thread concurrency.
 *
 * <p>This collector provides:
 *
 * <ul>
 *   <li>Instance lifecycle monitoring and tracking
 *   <li>Memory usage pattern analysis
 *   <li>Thread and concurrency monitoring
 *   <li>Native library performance metrics
 *   <li>Resource allocation tracking
 *   <li>Performance bottleneck detection
 * </ul>
 *
 * @since 1.0.0
 */
public final class DiagnosticCollector {

  private static final Logger LOGGER = Logger.getLogger(DiagnosticCollector.class.getName());

  /** Diagnostic severity levels. */
  public enum DiagnosticSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
  }

  /** Diagnostic event types. */
  public enum DiagnosticEventType {
    INSTANCE_LIFECYCLE,
    MEMORY_ALLOCATION,
    THREAD_ACTIVITY,
    PERFORMANCE_BOTTLENECK,
    RESOURCE_EXHAUSTION,
    ERROR_CONDITION
  }

  /** Diagnostic event data. */
  public static final class DiagnosticEvent {
    private final DiagnosticEventType type;
    private final DiagnosticSeverity severity;
    private final String message;
    private final Instant timestamp;
    private final Map<String, Object> metadata;
    private final String threadName;
    private final long threadId;

    public DiagnosticEvent(
        final DiagnosticEventType type,
        final DiagnosticSeverity severity,
        final String message,
        final Map<String, Object> metadata) {
      this.type = type;
      this.severity = severity;
      this.message = message;
      this.timestamp = Instant.now();
      this.metadata = Map.copyOf(metadata != null ? metadata : Map.of());
      final Thread currentThread = Thread.currentThread();
      this.threadName = currentThread.getName();
      this.threadId = currentThread.getId();
    }

    public DiagnosticEventType getType() {
      return type;
    }

    public DiagnosticSeverity getSeverity() {
      return severity;
    }

    public String getMessage() {
      return message;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public Map<String, Object> getMetadata() {
      return metadata;
    }

    public String getThreadName() {
      return threadName;
    }

    public long getThreadId() {
      return threadId;
    }

    @Override
    public String toString() {
      return String.format(
          "[%s] %s (%s) at %s on thread %s[%d]: %s %s",
          severity, type, message, timestamp, threadName, threadId, message, metadata);
    }
  }

  /** Instance lifecycle tracking. */
  public static final class InstanceLifecycleInfo {
    private final String instanceId;
    private final String moduleName;
    private final Instant createdAt;
    private volatile Instant lastUsed;
    private volatile Instant disposedAt;
    private final AtomicLong usageCount = new AtomicLong(0);
    private final AtomicLong memoryAllocated = new AtomicLong(0);
    private volatile boolean disposed = false;

    public InstanceLifecycleInfo(final String instanceId, final String moduleName) {
      this.instanceId = instanceId;
      this.moduleName = moduleName;
      this.createdAt = Instant.now();
      this.lastUsed = Instant.now();
    }

    public void recordUsage() {
      usageCount.incrementAndGet();
      lastUsed = Instant.now();
    }

    public void recordMemoryAllocation(final long bytes) {
      memoryAllocated.addAndGet(bytes);
    }

    public void markDisposed() {
      disposed = true;
      disposedAt = Instant.now();
    }

    public String getInstanceId() {
      return instanceId;
    }

    public String getModuleName() {
      return moduleName;
    }

    public Instant getCreatedAt() {
      return createdAt;
    }

    public Instant getLastUsed() {
      return lastUsed;
    }

    public Instant getDisposedAt() {
      return disposedAt;
    }

    public long getUsageCount() {
      return usageCount.get();
    }

    public long getMemoryAllocated() {
      return memoryAllocated.get();
    }

    public boolean isDisposed() {
      return disposed;
    }

    public Duration getLifetime() {
      final Instant endTime = disposed ? disposedAt : Instant.now();
      return Duration.between(createdAt, endTime);
    }

    public Duration getIdleTime() {
      return Duration.between(lastUsed, Instant.now());
    }
  }

  /** Memory allocation tracking. */
  public static final class MemoryAllocationInfo {
    private final String allocationType;
    private final long size;
    private final String location;
    private final Instant timestamp;
    private final String threadName;
    private final Map<String, Object> stackTrace;

    public MemoryAllocationInfo(
        final String allocationType,
        final long size,
        final String location,
        final Map<String, Object> stackTrace) {
      this.allocationType = allocationType;
      this.size = size;
      this.location = location;
      this.timestamp = Instant.now();
      this.threadName = Thread.currentThread().getName();
      this.stackTrace = Map.copyOf(stackTrace != null ? stackTrace : Map.of());
    }

    public String getAllocationType() {
      return allocationType;
    }

    public long getSize() {
      return size;
    }

    public String getLocation() {
      return location;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public String getThreadName() {
      return threadName;
    }

    public Map<String, Object> getStackTrace() {
      return stackTrace;
    }
  }

  /** Thread activity monitoring. */
  public static final class ThreadActivityInfo {
    private final long threadId;
    private final String threadName;
    private final Thread.State state;
    private final long cpuTime;
    private final long userTime;
    private final long blockedTime;
    private final long waitedTime;
    private final Instant timestamp;

    public ThreadActivityInfo(
        final ThreadInfo threadInfo, final long cpuTime, final long userTime) {
      this.threadId = threadInfo.getThreadId();
      this.threadName = threadInfo.getThreadName();
      this.state = threadInfo.getThreadState();
      this.cpuTime = cpuTime;
      this.userTime = userTime;
      this.blockedTime = threadInfo.getBlockedTime();
      this.waitedTime = threadInfo.getWaitedTime();
      this.timestamp = Instant.now();
    }

    public long getThreadId() {
      return threadId;
    }

    public String getThreadName() {
      return threadName;
    }

    public Thread.State getState() {
      return state;
    }

    public long getCpuTime() {
      return cpuTime;
    }

    public long getUserTime() {
      return userTime;
    }

    public long getBlockedTime() {
      return blockedTime;
    }

    public long getWaitedTime() {
      return waitedTime;
    }

    public Instant getTimestamp() {
      return timestamp;
    }
  }

  /** Performance bottleneck detection. */
  public static final class PerformanceBottleneck {
    private final String operation;
    private final Duration duration;
    private final String location;
    private final DiagnosticSeverity severity;
    private final Instant detectedAt;
    private final Map<String, Object> context;

    public PerformanceBottleneck(
        final String operation,
        final Duration duration,
        final String location,
        final DiagnosticSeverity severity,
        final Map<String, Object> context) {
      this.operation = operation;
      this.duration = duration;
      this.location = location;
      this.severity = severity;
      this.detectedAt = Instant.now();
      this.context = Map.copyOf(context != null ? context : Map.of());
    }

    public String getOperation() {
      return operation;
    }

    public Duration getDuration() {
      return duration;
    }

    public String getLocation() {
      return location;
    }

    public DiagnosticSeverity getSeverity() {
      return severity;
    }

    public Instant getDetectedAt() {
      return detectedAt;
    }

    public Map<String, Object> getContext() {
      return context;
    }
  }

  /** Diagnostic data storage. */
  private final ConcurrentLinkedQueue<DiagnosticEvent> diagnosticEvents =
      new ConcurrentLinkedQueue<>();

  private final ConcurrentHashMap<String, InstanceLifecycleInfo> instanceLifecycles =
      new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<MemoryAllocationInfo> memoryAllocations =
      new ConcurrentLinkedQueue<>();
  private final ConcurrentHashMap<Long, ThreadActivityInfo> threadActivities =
      new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<PerformanceBottleneck> performanceBottlenecks =
      new ConcurrentLinkedQueue<>();

  /** JVM monitoring beans. */
  private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

  private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
  private final List<GarbageCollectorMXBean> gcBeans =
      ManagementFactory.getGarbageCollectorMXBeans();
  private final List<MemoryPoolMXBean> memoryPoolBeans = ManagementFactory.getMemoryPoolMXBeans();

  /** Configuration. */
  private volatile boolean enabled = true;

  private volatile int maxEvents = 10000;
  private volatile Duration eventRetention = Duration.ofHours(24);
  private volatile boolean threadMonitoringEnabled = true;
  private volatile boolean memoryTrackingEnabled = true;
  private volatile Duration monitoringInterval = Duration.ofSeconds(30);

  /** Background monitoring. */
  private final ScheduledExecutorService backgroundExecutor = Executors.newScheduledThreadPool(2);

  /** Statistics. */
  private final AtomicLong totalDiagnosticEvents = new AtomicLong(0);

  private final AtomicLong totalInstancesTracked = new AtomicLong(0);
  private final AtomicReference<Instant> lastDiagnosticRun = new AtomicReference<>(Instant.now());

  /** Creates a new diagnostic collector. */
  public DiagnosticCollector() {
    startBackgroundMonitoring();
    LOGGER.info("Diagnostic collector initialized");
  }

  /**
   * Records a diagnostic event.
   *
   * @param type event type
   * @param severity event severity
   * @param message event message
   * @param metadata optional metadata
   */
  public void recordEvent(
      final DiagnosticEventType type,
      final DiagnosticSeverity severity,
      final String message,
      final Map<String, Object> metadata) {
    if (!enabled) {
      return;
    }

    final DiagnosticEvent event = new DiagnosticEvent(type, severity, message, metadata);
    diagnosticEvents.offer(event);
    totalDiagnosticEvents.incrementAndGet();

    // Limit queue size
    while (diagnosticEvents.size() > maxEvents) {
      diagnosticEvents.poll();
    }

    LOGGER.info("Diagnostic event: " + event);
  }

  /**
   * Records an instance lifecycle event.
   *
   * @param instanceId instance identifier
   * @param moduleName module name
   * @param event lifecycle event (created, used, disposed)
   */
  public void recordInstanceLifecycle(
      final String instanceId, final String moduleName, final String event) {
    if (!enabled) {
      return;
    }

    switch (event.toLowerCase()) {
      case "created":
        final InstanceLifecycleInfo info = new InstanceLifecycleInfo(instanceId, moduleName);
        instanceLifecycles.put(instanceId, info);
        totalInstancesTracked.incrementAndGet();
        recordEvent(
            DiagnosticEventType.INSTANCE_LIFECYCLE,
            DiagnosticSeverity.INFO,
            "Instance created: " + instanceId,
            Map.of("instanceId", instanceId, "moduleName", moduleName));
        break;

      case "used":
        final InstanceLifecycleInfo useInfo = instanceLifecycles.get(instanceId);
        if (useInfo != null) {
          useInfo.recordUsage();
        }
        break;

      case "disposed":
        final InstanceLifecycleInfo disposeInfo = instanceLifecycles.get(instanceId);
        if (disposeInfo != null) {
          disposeInfo.markDisposed();
          recordEvent(
              DiagnosticEventType.INSTANCE_LIFECYCLE,
              DiagnosticSeverity.INFO,
              "Instance disposed: " + instanceId,
              Map.of(
                  "instanceId", instanceId,
                  "lifetime", disposeInfo.getLifetime().toString(),
                  "usageCount", disposeInfo.getUsageCount()));
        }
        break;
    }
  }

  /**
   * Records a memory allocation event.
   *
   * @param allocationType type of allocation (heap, native, etc.)
   * @param size allocation size in bytes
   * @param location allocation location
   */
  public void recordMemoryAllocation(
      final String allocationType, final long size, final String location) {
    if (!enabled || !memoryTrackingEnabled) {
      return;
    }

    final Map<String, Object> stackTrace = captureStackTrace();
    final MemoryAllocationInfo allocation =
        new MemoryAllocationInfo(allocationType, size, location, stackTrace);
    memoryAllocations.offer(allocation);

    // Limit queue size
    while (memoryAllocations.size() > maxEvents) {
      memoryAllocations.poll();
    }

    // Record large allocations as events
    if (size > 1024 * 1024) { // 1MB threshold
      recordEvent(
          DiagnosticEventType.MEMORY_ALLOCATION,
          size > 10 * 1024 * 1024 ? DiagnosticSeverity.WARNING : DiagnosticSeverity.INFO,
          String.format("Large memory allocation: %d bytes", size),
          Map.of("allocationType", allocationType, "size", size, "location", location));
    }
  }

  /**
   * Records a performance bottleneck.
   *
   * @param operation operation name
   * @param duration operation duration
   * @param location operation location
   * @param context additional context
   */
  public void recordPerformanceBottleneck(
      final String operation,
      final Duration duration,
      final String location,
      final Map<String, Object> context) {
    if (!enabled) {
      return;
    }

    DiagnosticSeverity severity = DiagnosticSeverity.INFO;
    if (duration.toMillis() > 1000) {
      severity = DiagnosticSeverity.WARNING;
    }
    if (duration.toMillis() > 5000) {
      severity = DiagnosticSeverity.ERROR;
    }
    if (duration.toMillis() > 10000) {
      severity = DiagnosticSeverity.CRITICAL;
    }

    final PerformanceBottleneck bottleneck =
        new PerformanceBottleneck(operation, duration, location, severity, context);
    performanceBottlenecks.offer(bottleneck);

    // Limit queue size
    while (performanceBottlenecks.size() > maxEvents) {
      performanceBottlenecks.poll();
    }

    recordEvent(
        DiagnosticEventType.PERFORMANCE_BOTTLENECK,
        severity,
        String.format("Performance bottleneck in %s: %dms", operation, duration.toMillis()),
        Map.of("operation", operation, "duration", duration.toMillis(), "location", location));
  }

  /** Starts background monitoring tasks. */
  private void startBackgroundMonitoring() {
    // Thread monitoring
    if (threadMonitoringEnabled) {
      backgroundExecutor.scheduleAtFixedRate(
          this::monitorThreadActivity,
          monitoringInterval.toSeconds(),
          monitoringInterval.toSeconds(),
          TimeUnit.SECONDS);
    }

    // Memory monitoring
    backgroundExecutor.scheduleAtFixedRate(
        this::monitorMemoryUsage,
        monitoringInterval.toSeconds(),
        monitoringInterval.toSeconds(),
        TimeUnit.SECONDS);

    // Cleanup task
    backgroundExecutor.scheduleAtFixedRate(
        this::performCleanup, eventRetention.toHours(), eventRetention.toHours(), TimeUnit.HOURS);
  }

  /** Monitors thread activity. */
  private void monitorThreadActivity() {
    try {
      final long[] threadIds = threadBean.getAllThreadIds();
      final ThreadInfo[] threadInfos = threadBean.getThreadInfo(threadIds);

      for (final ThreadInfo threadInfo : threadInfos) {
        if (threadInfo != null) {
          final long cpuTime =
              threadBean.isThreadCpuTimeSupported()
                  ? threadBean.getThreadCpuTime(threadInfo.getThreadId())
                  : -1;
          final long userTime =
              threadBean.isThreadCpuTimeSupported()
                  ? threadBean.getThreadUserTime(threadInfo.getThreadId())
                  : -1;

          final ThreadActivityInfo activity = new ThreadActivityInfo(threadInfo, cpuTime, userTime);
          threadActivities.put(threadInfo.getThreadId(), activity);

          // Detect problematic thread states
          if (threadInfo.getThreadState() == Thread.State.BLOCKED
              && threadInfo.getBlockedTime() > 30000) {
            recordEvent(
                DiagnosticEventType.THREAD_ACTIVITY,
                DiagnosticSeverity.WARNING,
                String.format(
                    "Thread %s blocked for %dms",
                    threadInfo.getThreadName(), threadInfo.getBlockedTime()),
                Map.of(
                    "threadId",
                    threadInfo.getThreadId(),
                    "threadName",
                    threadInfo.getThreadName()));
          }
        }
      }

      lastDiagnosticRun.set(Instant.now());

    } catch (final Exception e) {
      LOGGER.warning("Thread monitoring failed: " + e.getMessage());
    }
  }

  /** Monitors memory usage patterns. */
  private void monitorMemoryUsage() {
    try {
      final MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
      final double heapUtilization = (double) heapUsage.getUsed() / heapUsage.getMax();

      // Check for high memory usage
      if (heapUtilization > 0.9) {
        recordEvent(
            DiagnosticEventType.RESOURCE_EXHAUSTION,
            DiagnosticSeverity.CRITICAL,
            String.format("High heap memory usage: %.1f%%", heapUtilization * 100),
            Map.of("heapUsed", heapUsage.getUsed(), "heapMax", heapUsage.getMax()));
      } else if (heapUtilization > 0.8) {
        recordEvent(
            DiagnosticEventType.RESOURCE_EXHAUSTION,
            DiagnosticSeverity.WARNING,
            String.format("Elevated heap memory usage: %.1f%%", heapUtilization * 100),
            Map.of("heapUsed", heapUsage.getUsed(), "heapMax", heapUsage.getMax()));
      }

      // Check memory pools
      for (final MemoryPoolMXBean poolBean : memoryPoolBeans) {
        final MemoryUsage poolUsage = poolBean.getUsage();
        if (poolUsage != null && poolUsage.getMax() > 0) {
          final double poolUtilization = (double) poolUsage.getUsed() / poolUsage.getMax();
          if (poolUtilization > 0.9) {
            recordEvent(
                DiagnosticEventType.RESOURCE_EXHAUSTION,
                DiagnosticSeverity.WARNING,
                String.format(
                    "High memory pool usage in %s: %.1f%%",
                    poolBean.getName(), poolUtilization * 100),
                Map.of(
                    "poolName",
                    poolBean.getName(),
                    "poolUsed",
                    poolUsage.getUsed(),
                    "poolMax",
                    poolUsage.getMax()));
          }
        }
      }

      // Check for excessive GC activity
      long totalGcTime = 0;
      long totalGcCount = 0;
      for (final GarbageCollectorMXBean gcBean : gcBeans) {
        totalGcTime += gcBean.getCollectionTime();
        totalGcCount += gcBean.getCollectionCount();
      }

      // Simple heuristic: if GC time is > 10% of uptime, it's excessive
      final Duration uptime = Duration.between(lastDiagnosticRun.get(), Instant.now());
      final double gcTimeRatio = totalGcTime / (double) uptime.toMillis();
      if (gcTimeRatio > 0.1) {
        recordEvent(
            DiagnosticEventType.PERFORMANCE_BOTTLENECK,
            DiagnosticSeverity.WARNING,
            String.format("Excessive GC activity: %.1f%% of time", gcTimeRatio * 100),
            Map.of("gcTime", totalGcTime, "gcCount", totalGcCount));
      }

    } catch (final Exception e) {
      LOGGER.warning("Memory monitoring failed: " + e.getMessage());
    }
  }

  /** Performs cleanup of old diagnostic data. */
  private void performCleanup() {
    try {
      final Instant cutoff = Instant.now().minus(eventRetention);

      // Clean diagnostic events
      diagnosticEvents.removeIf(event -> event.getTimestamp().isBefore(cutoff));

      // Clean memory allocations
      memoryAllocations.removeIf(allocation -> allocation.getTimestamp().isBefore(cutoff));

      // Clean performance bottlenecks
      performanceBottlenecks.removeIf(bottleneck -> bottleneck.getDetectedAt().isBefore(cutoff));

      // Clean disposed instances
      instanceLifecycles
          .entrySet()
          .removeIf(
              entry -> {
                final InstanceLifecycleInfo info = entry.getValue();
                return info.isDisposed()
                    && info.getDisposedAt() != null
                    && info.getDisposedAt().isBefore(cutoff);
              });

      LOGGER.fine("Diagnostic data cleanup completed");

    } catch (final Exception e) {
      LOGGER.warning("Diagnostic cleanup failed: " + e.getMessage());
    }
  }

  /**
   * Captures current stack trace for diagnostic purposes.
   *
   * @return stack trace information
   */
  private Map<String, Object> captureStackTrace() {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final List<String> trace =
        Arrays.stream(stackTrace)
            .limit(10) // Limit to first 10 frames
            .map(StackTraceElement::toString)
            .collect(Collectors.toList());

    return Map.of("stackTrace", trace, "depth", stackTrace.length);
  }

  /**
   * Gets recent diagnostic events.
   *
   * @param limit maximum number of events
   * @return list of recent events
   */
  public List<DiagnosticEvent> getRecentEvents(final int limit) {
    return diagnosticEvents.stream()
        .sorted((e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp()))
        .limit(limit)
        .collect(Collectors.toList());
  }

  /**
   * Gets instance lifecycle information.
   *
   * @param instanceId instance identifier
   * @return lifecycle information or null if not found
   */
  public InstanceLifecycleInfo getInstanceLifecycle(final String instanceId) {
    return instanceLifecycles.get(instanceId);
  }

  /**
   * Gets all tracked instances.
   *
   * @return map of instance lifecycles
   */
  public Map<String, InstanceLifecycleInfo> getAllInstanceLifecycles() {
    return Map.copyOf(instanceLifecycles);
  }

  /**
   * Gets recent memory allocations.
   *
   * @param limit maximum number of allocations
   * @return list of recent allocations
   */
  public List<MemoryAllocationInfo> getRecentMemoryAllocations(final int limit) {
    return memoryAllocations.stream()
        .sorted((a1, a2) -> a2.getTimestamp().compareTo(a1.getTimestamp()))
        .limit(limit)
        .collect(Collectors.toList());
  }

  /**
   * Gets recent performance bottlenecks.
   *
   * @param limit maximum number of bottlenecks
   * @return list of recent bottlenecks
   */
  public List<PerformanceBottleneck> getRecentBottlenecks(final int limit) {
    return performanceBottlenecks.stream()
        .sorted((b1, b2) -> b2.getDetectedAt().compareTo(b1.getDetectedAt()))
        .limit(limit)
        .collect(Collectors.toList());
  }

  /**
   * Gets current thread activities.
   *
   * @return map of thread activities
   */
  public Map<Long, ThreadActivityInfo> getCurrentThreadActivities() {
    return Map.copyOf(threadActivities);
  }

  /**
   * Gets comprehensive diagnostic summary.
   *
   * @return formatted diagnostic summary
   */
  public String getDiagnosticSummary() {
    final StringBuilder sb = new StringBuilder("=== Diagnostic Summary ===\n");

    sb.append(String.format("Total diagnostic events: %,d\n", totalDiagnosticEvents.get()));
    sb.append(String.format("Total instances tracked: %,d\n", totalInstancesTracked.get()));
    sb.append(
        String.format(
            "Active instances: %d\n",
            instanceLifecycles.values().stream()
                .mapToInt(info -> info.isDisposed() ? 0 : 1)
                .sum()));
    sb.append(String.format("Recent events: %d\n", diagnosticEvents.size()));
    sb.append(String.format("Recent allocations: %d\n", memoryAllocations.size()));
    sb.append(String.format("Recent bottlenecks: %d\n", performanceBottlenecks.size()));
    sb.append(String.format("Active threads: %d\n", threadActivities.size()));
    sb.append(String.format("Last diagnostic run: %s\n", lastDiagnosticRun.get()));

    return sb.toString();
  }

  /**
   * Enables or disables diagnostic collection.
   *
   * @param enabled true to enable collection
   */
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    LOGGER.info("Diagnostic collection " + (enabled ? "enabled" : "disabled"));
  }

  /**
   * Sets the maximum number of events to retain.
   *
   * @param maxEvents maximum events
   */
  public void setMaxEvents(final int maxEvents) {
    this.maxEvents = maxEvents;
  }

  /**
   * Sets the event retention period.
   *
   * @param retention retention period
   */
  public void setEventRetention(final Duration retention) {
    this.eventRetention = retention;
  }

  /**
   * Enables or disables thread monitoring.
   *
   * @param enabled true to enable thread monitoring
   */
  public void setThreadMonitoringEnabled(final boolean enabled) {
    this.threadMonitoringEnabled = enabled;
  }

  /**
   * Enables or disables memory tracking.
   *
   * @param enabled true to enable memory tracking
   */
  public void setMemoryTrackingEnabled(final boolean enabled) {
    this.memoryTrackingEnabled = enabled;
  }

  /** Clears all diagnostic data. */
  public void clearDiagnosticData() {
    diagnosticEvents.clear();
    instanceLifecycles.clear();
    memoryAllocations.clear();
    threadActivities.clear();
    performanceBottlenecks.clear();
    LOGGER.info("All diagnostic data cleared");
  }

  /** Shuts down the diagnostic collector. */
  public void shutdown() {
    backgroundExecutor.shutdown();
    try {
      if (!backgroundExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
        backgroundExecutor.shutdownNow();
      }
    } catch (final InterruptedException e) {
      backgroundExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
    LOGGER.info("Diagnostic collector shutdown");
  }
}
