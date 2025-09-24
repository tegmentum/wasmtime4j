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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;

/**
 * Core metrics collection system providing comprehensive monitoring of WebAssembly runtime
 * operations, system resources, and performance indicators.
 *
 * <p>This collector provides:
 *
 * <ul>
 *   <li>Operation latency and throughput metrics
 *   <li>Resource usage tracking (memory, CPU, threads)
 *   <li>Error rates and types monitoring
 *   <li>WebAssembly module statistics
 *   <li>JVM health indicators
 *   <li>Custom application metrics
 * </ul>
 *
 * @since 1.0.0
 */
public final class MetricsCollector {

  private static final Logger LOGGER = Logger.getLogger(MetricsCollector.class.getName());

  /** Metric categories for organization. */
  public enum MetricCategory {
    OPERATION, // WebAssembly operation metrics
    RESOURCE, // System resource metrics
    ERROR, // Error and exception metrics
    PERFORMANCE, // Performance and latency metrics
    CUSTOM // Custom application metrics
  }

  /** Metric data types. */
  public enum MetricType {
    COUNTER, // Monotonically increasing counter
    GAUGE, // Current value that can fluctuate
    HISTOGRAM, // Distribution of values with percentiles
    TIMER, // Duration measurements with statistics
    RATE // Rate of events per time unit
  }

  /** Core metric data point. */
  public static final class MetricData {
    private final String name;
    private final MetricCategory category;
    private final MetricType type;
    private final double value;
    private final Instant timestamp;
    private final Map<String, String> labels;
    private final String unit;

    public MetricData(
        final String name,
        final MetricCategory category,
        final MetricType type,
        final double value,
        final String unit,
        final Map<String, String> labels) {
      this.name = name;
      this.category = category;
      this.type = type;
      this.value = value;
      this.unit = unit != null ? unit : "";
      this.timestamp = Instant.now();
      this.labels = Map.copyOf(labels != null ? labels : Map.of());
    }

    public String getName() {
      return name;
    }

    public MetricCategory getCategory() {
      return category;
    }

    public MetricType getType() {
      return type;
    }

    public double getValue() {
      return value;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public Map<String, String> getLabels() {
      return labels;
    }

    public String getUnit() {
      return unit;
    }

    @Override
    public String toString() {
      return String.format(
          "%s{%s}[%s] = %.3f %s at %s", name, labels, category, value, unit, timestamp);
    }
  }

  /** Histogram for tracking value distributions. */
  public static final class HistogramMetric {
    private final String name;
    private final LongAdder count = new LongAdder();
    private final LongAdder sum = new LongAdder();
    private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong max = new AtomicLong(Long.MIN_VALUE);
    private final ConcurrentHashMap<String, LongAdder> buckets = new ConcurrentHashMap<>();
    private final double[] percentileBounds;

    public HistogramMetric(final String name, final double[] percentileBounds) {
      this.name = name;
      this.percentileBounds = percentileBounds != null ? percentileBounds : new double[0];
      initializeBuckets();
    }

    private void initializeBuckets() {
      for (final double bound : percentileBounds) {
        buckets.put(String.valueOf(bound), new LongAdder());
      }
      buckets.put("+Inf", new LongAdder());
    }

    public synchronized void observe(final double value) {
      count.increment();
      sum.add((long) (value * 1000)); // Store as millis for precision

      // Update min/max
      final long longValue = (long) (value * 1000);
      min.updateAndGet(current -> Math.min(current, longValue));
      max.updateAndGet(current -> Math.max(current, longValue));

      // Update buckets
      for (final double bound : percentileBounds) {
        if (value <= bound) {
          buckets.get(String.valueOf(bound)).increment();
        }
      }
      buckets.get("+Inf").increment();
    }

    public long getCount() {
      return count.sum();
    }

    public double getSum() {
      return sum.sum() / 1000.0;
    }

    public double getMean() {
      final long totalCount = getCount();
      return totalCount > 0 ? getSum() / totalCount : 0.0;
    }

    public double getMin() {
      final long minValue = min.get();
      return minValue == Long.MAX_VALUE ? 0.0 : minValue / 1000.0;
    }

    public double getMax() {
      final long maxValue = max.get();
      return maxValue == Long.MIN_VALUE ? 0.0 : maxValue / 1000.0;
    }

    public Map<String, Long> getBuckets() {
      final Map<String, Long> result = new ConcurrentHashMap<>();
      for (final Map.Entry<String, LongAdder> entry : buckets.entrySet()) {
        result.put(entry.getKey(), entry.getValue().sum());
      }
      return result;
    }

    public String getName() {
      return name;
    }
  }

  /** Timer for measuring operation durations. */
  public static final class TimerMetric {
    private final HistogramMetric histogram;
    private final AtomicReference<Instant> lastUpdate = new AtomicReference<>(Instant.now());

    public TimerMetric(final String name) {
      // Standard duration buckets in milliseconds
      final double[] buckets = {
        0.1, 0.5, 1.0, 2.5, 5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0
      };
      this.histogram = new HistogramMetric(name, buckets);
    }

    public void record(final Duration duration) {
      final double durationMs = duration.toNanos() / 1_000_000.0;
      histogram.observe(durationMs);
      lastUpdate.set(Instant.now());
    }

    public void record(final long durationNanos) {
      record(Duration.ofNanos(durationNanos));
    }

    public HistogramMetric getHistogram() {
      return histogram;
    }

    public Instant getLastUpdate() {
      return lastUpdate.get();
    }

    public TimerSample start() {
      return new TimerSample(this);
    }
  }

  /** Timer sample for measuring operation duration. */
  public static final class TimerSample {
    private final TimerMetric timer;
    private final long startNanos;

    TimerSample(final TimerMetric timer) {
      this.timer = timer;
      this.startNanos = System.nanoTime();
    }

    public void stop() {
      final long durationNanos = System.nanoTime() - startNanos;
      timer.record(durationNanos);
    }
  }

  /** Rate metric for tracking events per time unit. */
  public static final class RateMetric {
    private final String name;
    private final LongAdder totalEvents = new LongAdder();
    private final AtomicReference<Instant> startTime = new AtomicReference<>(Instant.now());
    private final AtomicReference<Instant> lastEvent = new AtomicReference<>(Instant.now());

    public RateMetric(final String name) {
      this.name = name;
    }

    public void mark() {
      mark(1);
    }

    public void mark(final long events) {
      totalEvents.add(events);
      lastEvent.set(Instant.now());
    }

    public long getTotalEvents() {
      return totalEvents.sum();
    }

    public double getEventsPerSecond() {
      final long events = getTotalEvents();
      final Duration duration = Duration.between(startTime.get(), Instant.now());
      final double seconds = duration.toNanos() / 1_000_000_000.0;
      return seconds > 0 ? events / seconds : 0.0;
    }

    public Instant getLastEvent() {
      return lastEvent.get();
    }

    public String getName() {
      return name;
    }
  }

  /** Metrics storage. */
  private final ConcurrentHashMap<String, MetricData> gauges = new ConcurrentHashMap<>();

  private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, HistogramMetric> histograms = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, TimerMetric> timers = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, RateMetric> rates = new ConcurrentHashMap<>();

  /** System metrics. */
  private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

  private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

  /** Collection configuration. */
  private volatile boolean enabled = true;

  private volatile boolean systemMetricsEnabled = true;
  private volatile Duration systemMetricsInterval = Duration.ofSeconds(30);

  /** Background collection. */
  private final ScheduledExecutorService backgroundExecutor = Executors.newScheduledThreadPool(2);

  /** Collector statistics. */
  private final AtomicLong totalMetricsCollected = new AtomicLong(0);

  private final Instant collectorStartTime = Instant.now();

  /** Creates a new metrics collector. */
  public MetricsCollector() {
    startSystemMetricsCollection();
    LOGGER.info("Metrics collector initialized");
  }

  /**
   * Records a counter metric.
   *
   * @param name metric name
   * @param value counter value to add
   * @param labels optional labels
   */
  public void counter(final String name, final long value, final Map<String, String> labels) {
    if (!enabled) {
      return;
    }

    final String key = createKey(name, labels);
    counters.computeIfAbsent(key, k -> new AtomicLong(0)).addAndGet(value);
    totalMetricsCollected.incrementAndGet();
  }

  /**
   * Records a counter metric with value 1.
   *
   * @param name metric name
   * @param labels optional labels
   */
  public void counter(final String name, final Map<String, String> labels) {
    counter(name, 1, labels);
  }

  /**
   * Records a counter metric with value 1 and no labels.
   *
   * @param name metric name
   */
  public void counter(final String name) {
    counter(name, 1, null);
  }

  /**
   * Records a gauge metric.
   *
   * @param name metric name
   * @param value current value
   * @param unit measurement unit
   * @param labels optional labels
   */
  public void gauge(
      final String name, final double value, final String unit, final Map<String, String> labels) {
    if (!enabled) {
      return;
    }

    final String key = createKey(name, labels);
    final MetricData metric =
        new MetricData(name, MetricCategory.RESOURCE, MetricType.GAUGE, value, unit, labels);
    gauges.put(key, metric);
    totalMetricsCollected.incrementAndGet();
  }

  /**
   * Records a gauge metric without unit.
   *
   * @param name metric name
   * @param value current value
   * @param labels optional labels
   */
  public void gauge(final String name, final double value, final Map<String, String> labels) {
    gauge(name, value, null, labels);
  }

  /**
   * Records a gauge metric with no labels.
   *
   * @param name metric name
   * @param value current value
   */
  public void gauge(final String name, final double value) {
    gauge(name, value, null, null);
  }

  /**
   * Gets or creates a histogram metric.
   *
   * @param name metric name
   * @param percentileBounds percentile bounds for buckets
   * @return histogram metric
   */
  public HistogramMetric histogram(final String name, final double[] percentileBounds) {
    return histograms.computeIfAbsent(name, k -> new HistogramMetric(name, percentileBounds));
  }

  /**
   * Gets or creates a histogram metric with default buckets.
   *
   * @param name metric name
   * @return histogram metric
   */
  public HistogramMetric histogram(final String name) {
    final double[] defaultBuckets = {0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0};
    return histogram(name, defaultBuckets);
  }

  /**
   * Gets or creates a timer metric.
   *
   * @param name metric name
   * @return timer metric
   */
  public TimerMetric timer(final String name) {
    return timers.computeIfAbsent(name, TimerMetric::new);
  }

  /**
   * Gets or creates a rate metric.
   *
   * @param name metric name
   * @return rate metric
   */
  public RateMetric rate(final String name) {
    return rates.computeIfAbsent(name, RateMetric::new);
  }

  /**
   * Records a timing measurement.
   *
   * @param name metric name
   * @param duration operation duration
   * @param labels optional labels
   */
  public void timing(final String name, final Duration duration, final Map<String, String> labels) {
    if (!enabled) {
      return;
    }

    final String key = createKey(name, labels);
    timer(key).record(duration);
    totalMetricsCollected.incrementAndGet();
  }

  /**
   * Records a timing measurement in nanoseconds.
   *
   * @param name metric name
   * @param durationNanos duration in nanoseconds
   * @param labels optional labels
   */
  public void timing(
      final String name, final long durationNanos, final Map<String, String> labels) {
    timing(name, Duration.ofNanos(durationNanos), labels);
  }

  /**
   * Records a timing measurement with no labels.
   *
   * @param name metric name
   * @param duration operation duration
   */
  public void timing(final String name, final Duration duration) {
    timing(name, duration, null);
  }

  /** Starts collection of system metrics. */
  private void startSystemMetricsCollection() {
    backgroundExecutor.scheduleAtFixedRate(
        this::collectSystemMetrics,
        systemMetricsInterval.toSeconds(),
        systemMetricsInterval.toSeconds(),
        TimeUnit.SECONDS);
  }

  /** Collects system and JVM metrics. */
  private void collectSystemMetrics() {
    if (!systemMetricsEnabled) {
      return;
    }

    try {
      // JVM memory metrics
      final MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
      final MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

      gauge("jvm.memory.heap.used", heapUsage.getUsed(), "bytes", null);
      gauge("jvm.memory.heap.committed", heapUsage.getCommitted(), "bytes", null);
      gauge("jvm.memory.heap.max", heapUsage.getMax(), "bytes", null);
      gauge(
          "jvm.memory.heap.usage",
          (double) heapUsage.getUsed() / heapUsage.getMax(),
          "ratio",
          null);

      gauge("jvm.memory.nonheap.used", nonHeapUsage.getUsed(), "bytes", null);
      gauge("jvm.memory.nonheap.committed", nonHeapUsage.getCommitted(), "bytes", null);

      // Thread metrics
      gauge("jvm.threads.current", threadBean.getThreadCount(), "threads", null);
      gauge("jvm.threads.daemon", threadBean.getDaemonThreadCount(), "threads", null);
      gauge("jvm.threads.peak", threadBean.getPeakThreadCount(), "threads", null);
      gauge("jvm.threads.total_started", threadBean.getTotalStartedThreadCount(), "threads", null);

      // Runtime metrics
      final Runtime runtime = Runtime.getRuntime();
      gauge("jvm.runtime.processors", runtime.availableProcessors(), "processors", null);

      // Collector uptime
      final Duration uptime = Duration.between(collectorStartTime, Instant.now());
      gauge("metrics.collector.uptime", uptime.toSeconds(), "seconds", null);
      gauge("metrics.collector.total_collected", totalMetricsCollected.get(), "metrics", null);

    } catch (final Exception e) {
      LOGGER.warning("Failed to collect system metrics: " + e.getMessage());
    }
  }

  /**
   * Gets current counter value.
   *
   * @param name counter name
   * @param labels optional labels
   * @return current counter value
   */
  public long getCounterValue(final String name, final Map<String, String> labels) {
    final String key = createKey(name, labels);
    final AtomicLong counter = counters.get(key);
    return counter != null ? counter.get() : 0;
  }

  /**
   * Gets current counter value with no labels.
   *
   * @param name counter name
   * @return current counter value
   */
  public long getCounterValue(final String name) {
    return getCounterValue(name, null);
  }

  /**
   * Gets current gauge value.
   *
   * @param name gauge name
   * @param labels optional labels
   * @return current gauge value or null if not found
   */
  public MetricData getGaugeValue(final String name, final Map<String, String> labels) {
    final String key = createKey(name, labels);
    return gauges.get(key);
  }

  /**
   * Gets current gauge value with no labels.
   *
   * @param name gauge name
   * @return current gauge value or null if not found
   */
  public MetricData getGaugeValue(final String name) {
    return getGaugeValue(name, null);
  }

  /** Resets all metrics. */
  public void reset() {
    counters.clear();
    gauges.clear();
    histograms.clear();
    timers.clear();
    rates.clear();
    totalMetricsCollected.set(0);
    LOGGER.info("All metrics reset");
  }

  /**
   * Creates a unique key for metric storage.
   *
   * @param name metric name
   * @param labels optional labels
   * @return unique key
   */
  private String createKey(final String name, final Map<String, String> labels) {
    if (labels == null || labels.isEmpty()) {
      return name;
    }

    final StringBuilder sb = new StringBuilder(name);
    sb.append('{');
    labels.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry -> sb.append(entry.getKey()).append('=').append(entry.getValue()).append(','));
    if (sb.charAt(sb.length() - 1) == ',') {
      sb.setLength(sb.length() - 1);
    }
    sb.append('}');
    return sb.toString();
  }

  /**
   * Gets comprehensive metrics summary.
   *
   * @return formatted metrics summary
   */
  public String getMetricsSummary() {
    final StringBuilder sb = new StringBuilder("=== Metrics Summary ===\n");

    sb.append(String.format("Total metrics collected: %,d\n", totalMetricsCollected.get()));
    sb.append(String.format("Uptime: %s\n", Duration.between(collectorStartTime, Instant.now())));
    sb.append(String.format("Counters: %d\n", counters.size()));
    sb.append(String.format("Gauges: %d\n", gauges.size()));
    sb.append(String.format("Histograms: %d\n", histograms.size()));
    sb.append(String.format("Timers: %d\n", timers.size()));
    sb.append(String.format("Rates: %d\n", rates.size()));
    sb.append(String.format("System metrics enabled: %s\n", systemMetricsEnabled));

    return sb.toString();
  }

  /**
   * Enables or disables metrics collection.
   *
   * @param enabled true to enable collection
   */
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    LOGGER.info("Metrics collection " + (enabled ? "enabled" : "disabled"));
  }

  /**
   * Enables or disables system metrics collection.
   *
   * @param enabled true to enable system metrics
   */
  public void setSystemMetricsEnabled(final boolean enabled) {
    this.systemMetricsEnabled = enabled;
    LOGGER.info("System metrics collection " + (enabled ? "enabled" : "disabled"));
  }

  /**
   * Sets the system metrics collection interval.
   *
   * @param interval collection interval
   */
  public void setSystemMetricsInterval(final Duration interval) {
    this.systemMetricsInterval = interval;
    LOGGER.info("System metrics interval set to " + interval);
  }

  /**
   * Checks if metrics collection is enabled.
   *
   * @return true if enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /** Shuts down the metrics collector. */
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
    LOGGER.info("Metrics collector shutdown");
  }
}
