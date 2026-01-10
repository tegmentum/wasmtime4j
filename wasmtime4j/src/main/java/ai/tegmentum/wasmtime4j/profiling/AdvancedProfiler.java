/*
 * Copyright 2025 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.profiling;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Advanced profiler for WebAssembly runtime performance analysis.
 *
 * <p>This profiler provides comprehensive profiling capabilities including:
 *
 * <ul>
 *   <li>Function execution profiling with timing and memory tracking
 *   <li>Memory allocation and deallocation tracking
 *   <li>Flame graph generation for visual performance analysis
 *   <li>Stack trace collection for debugging
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ProfilerConfiguration config = ProfilerConfiguration.builder()
 *     .samplingInterval(Duration.ofMicroseconds(100))
 *     .enableMemoryProfiling(true)
 *     .enableFlameGraphs(true)
 *     .build();
 *
 * try (AdvancedProfiler profiler = new AdvancedProfiler(config)) {
 *     ProfilingSession session = profiler.startProfiling(Duration.ofMinutes(5));
 *     // ... execute WebAssembly code ...
 *     ProfilingStatistics stats = session.getStatistics();
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public final class AdvancedProfiler implements AutoCloseable {

  private final ProfilerConfiguration configuration;
  private final AtomicLong allocationIdGenerator;
  private final Map<Long, AllocationRecord> allocations;
  private final List<FunctionRecord> functionRecords;
  private final AtomicLong sampleCount;
  private volatile boolean closed;

  /**
   * Creates a new advanced profiler with the specified configuration.
   *
   * @param configuration the profiler configuration
   * @throws IllegalArgumentException if configuration is null
   */
  public AdvancedProfiler(final ProfilerConfiguration configuration) {
    this.configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
    this.allocationIdGenerator = new AtomicLong(0);
    this.allocations = new ConcurrentHashMap<>();
    this.functionRecords = new ArrayList<>();
    this.sampleCount = new AtomicLong(0);
    this.closed = false;
  }

  /**
   * Starts a new profiling session with the specified timeout.
   *
   * @param timeout the maximum duration for the profiling session
   * @return a new profiling session
   * @throws IllegalArgumentException if timeout is null or negative
   * @throws IllegalStateException if the profiler is closed
   */
  public ProfilingSession startProfiling(final Duration timeout) {
    if (timeout == null || timeout.isNegative()) {
      throw new IllegalArgumentException("Timeout must be a positive duration");
    }
    if (closed) {
      throw new IllegalStateException("Profiler is closed");
    }
    return new ProfilingSession(this, timeout);
  }

  /**
   * Profiles an operation and returns its result.
   *
   * @param name the operation name for identification
   * @param operation the operation to profile
   * @param runtimeType the runtime type (e.g., "JNI", "PANAMA")
   * @param <T> the return type of the operation
   * @return the result of the operation
   */
  public <T> T profileOperation(
      final String name, final Supplier<T> operation, final String runtimeType) {
    Objects.requireNonNull(name, "Name cannot be null");
    Objects.requireNonNull(operation, "Operation cannot be null");

    if (closed) {
      return operation.get();
    }

    final Instant start = Instant.now();
    final long startMemory = Runtime.getRuntime().freeMemory();

    try {
      return operation.get();
    } finally {
      final Duration elapsed = Duration.between(start, Instant.now());
      final long memoryUsed = startMemory - Runtime.getRuntime().freeMemory();
      recordFunctionExecution(name, elapsed, memoryUsed, runtimeType);
    }
  }

  /**
   * Records a function execution with timing and memory information.
   *
   * @param name the function name
   * @param executionTime the execution duration
   * @param memoryUsed the approximate memory used in bytes
   * @param runtimeType the runtime type
   */
  public void recordFunctionExecution(
      final String name,
      final Duration executionTime,
      final long memoryUsed,
      final String runtimeType) {
    if (closed) {
      return;
    }

    synchronized (functionRecords) {
      functionRecords.add(
          new FunctionRecord(
              name,
              executionTime,
              memoryUsed,
              runtimeType != null ? runtimeType : "UNKNOWN",
              Instant.now()));
    }
    sampleCount.incrementAndGet();
  }

  /**
   * Records a memory allocation.
   *
   * @param size the allocation size in bytes
   * @param label a label for the allocation
   * @return a unique allocation ID for tracking
   */
  public long recordMemoryAllocation(final long size, final String label) {
    if (closed) {
      return -1;
    }

    final long id = allocationIdGenerator.incrementAndGet();
    if (configuration.isEnableMemoryProfiling()) {
      allocations.put(id, new AllocationRecord(id, size, label, Instant.now()));
    }
    return id;
  }

  /**
   * Records a memory deallocation.
   *
   * @param allocationId the allocation ID from {@link #recordMemoryAllocation}
   */
  public void recordMemoryDeallocation(final long allocationId) {
    if (closed) {
      return;
    }

    if (configuration.isEnableMemoryProfiling()) {
      final AllocationRecord record = allocations.remove(allocationId);
      if (record != null) {
        record.markDeallocated();
      }
    }
  }

  /**
   * Gets the current profiler configuration.
   *
   * @return the configuration
   */
  public ProfilerConfiguration getConfiguration() {
    return configuration;
  }

  /**
   * Checks if the profiler is closed.
   *
   * @return true if closed
   */
  public boolean isClosed() {
    return closed;
  }

  @Override
  public void close() {
    closed = true;
    allocations.clear();
    synchronized (functionRecords) {
      functionRecords.clear();
    }
  }

  // Internal helper to get function records snapshot
  List<FunctionRecord> getFunctionRecordsSnapshot() {
    synchronized (functionRecords) {
      return new ArrayList<>(functionRecords);
    }
  }

  // Internal helper to get allocation count
  long getAllocationCount() {
    return allocationIdGenerator.get();
  }

  // Internal helper to get sample count
  long getSampleCount() {
    return sampleCount.get();
  }

  /** Configuration for the advanced profiler. */
  public static final class ProfilerConfiguration {

    private final Duration samplingInterval;
    private final int maxSamples;
    private final boolean enableMemoryProfiling;
    private final boolean enableJfrIntegration;
    private final boolean enableFlameGraphs;
    private final boolean enableStackTraceCollection;

    private ProfilerConfiguration(final Builder builder) {
      this.samplingInterval = builder.samplingInterval;
      this.maxSamples = builder.maxSamples;
      this.enableMemoryProfiling = builder.enableMemoryProfiling;
      this.enableJfrIntegration = builder.enableJfrIntegration;
      this.enableFlameGraphs = builder.enableFlameGraphs;
      this.enableStackTraceCollection = builder.enableStackTraceCollection;
    }

    /**
     * Creates a new configuration builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
      return new Builder();
    }

    /**
     * Gets the sampling interval.
     *
     * @return the sampling interval
     */
    public Duration getSamplingInterval() {
      return samplingInterval;
    }

    /**
     * Gets the maximum number of samples to collect.
     *
     * @return the maximum number of samples to collect
     */
    public int getMaxSamples() {
      return maxSamples;
    }

    /**
     * Checks if memory profiling is enabled.
     *
     * @return true if memory profiling is enabled
     */
    public boolean isEnableMemoryProfiling() {
      return enableMemoryProfiling;
    }

    /**
     * Checks if JFR integration is enabled.
     *
     * @return true if JFR integration is enabled
     */
    public boolean isEnableJfrIntegration() {
      return enableJfrIntegration;
    }

    /**
     * Checks if flame graph generation is enabled.
     *
     * @return true if flame graph generation is enabled
     */
    public boolean isEnableFlameGraphs() {
      return enableFlameGraphs;
    }

    /**
     * Checks if stack trace collection is enabled.
     *
     * @return true if stack trace collection is enabled
     */
    public boolean isEnableStackTraceCollection() {
      return enableStackTraceCollection;
    }

    /** Builder for profiler configuration. */
    public static final class Builder {

      private Duration samplingInterval = Duration.ofMillis(10);
      private int maxSamples = 10000;
      private boolean enableMemoryProfiling = false;
      private boolean enableJfrIntegration = false;
      private boolean enableFlameGraphs = false;
      private boolean enableStackTraceCollection = false;

      private Builder() {}

      /**
       * Sets the sampling interval.
       *
       * @param interval the sampling interval
       * @return this builder
       */
      public Builder samplingInterval(final Duration interval) {
        this.samplingInterval = Objects.requireNonNull(interval, "Interval cannot be null");
        return this;
      }

      /**
       * Sets the maximum number of samples.
       *
       * @param maxSamples the maximum samples
       * @return this builder
       */
      public Builder maxSamples(final int maxSamples) {
        if (maxSamples <= 0) {
          throw new IllegalArgumentException("Max samples must be positive");
        }
        this.maxSamples = maxSamples;
        return this;
      }

      /**
       * Enables or disables memory profiling.
       *
       * @param enable true to enable
       * @return this builder
       */
      public Builder enableMemoryProfiling(final boolean enable) {
        this.enableMemoryProfiling = enable;
        return this;
      }

      /**
       * Enables or disables JFR integration.
       *
       * @param enable true to enable
       * @return this builder
       */
      public Builder enableJfrIntegration(final boolean enable) {
        this.enableJfrIntegration = enable;
        return this;
      }

      /**
       * Enables or disables flame graph generation.
       *
       * @param enable true to enable
       * @return this builder
       */
      public Builder enableFlameGraphs(final boolean enable) {
        this.enableFlameGraphs = enable;
        return this;
      }

      /**
       * Enables or disables stack trace collection.
       *
       * @param enable true to enable
       * @return this builder
       */
      public Builder enableStackTraceCollection(final boolean enable) {
        this.enableStackTraceCollection = enable;
        return this;
      }

      /**
       * Builds the configuration.
       *
       * @return the configuration
       */
      public ProfilerConfiguration build() {
        return new ProfilerConfiguration(this);
      }
    }
  }

  /** An active profiling session. */
  public static final class ProfilingSession implements AutoCloseable {

    private final AdvancedProfiler profiler;
    private final Duration timeout;
    private final Instant startTime;
    private volatile boolean closed;

    private ProfilingSession(final AdvancedProfiler profiler, final Duration timeout) {
      this.profiler = profiler;
      this.timeout = timeout;
      this.startTime = Instant.now();
      this.closed = false;
    }

    /**
     * Gets the current profiling statistics.
     *
     * @return the statistics
     */
    public ProfilingStatistics getStatistics() {
      final List<FunctionRecord> records = profiler.getFunctionRecordsSnapshot();
      final long totalCalls = records.size();
      final Duration totalTime =
          records.stream()
              .map(FunctionRecord::getExecutionTime)
              .reduce(Duration.ZERO, Duration::plus);
      final long totalMemory = records.stream().mapToLong(FunctionRecord::getMemoryUsed).sum();

      final Map<String, Long> functionCalls = new HashMap<>();
      for (final FunctionRecord record : records) {
        functionCalls.merge(record.getName(), 1L, Long::sum);
      }

      return new ProfilingStatistics(totalCalls, totalTime, totalMemory, functionCalls);
    }

    /**
     * Generates a flame graph from the profiling data.
     *
     * @return the root flame frame
     */
    public FlameGraphGenerator.FlameFrame generateFlameGraph() {
      final List<FunctionRecord> records = profiler.getFunctionRecordsSnapshot();
      final Duration totalTime =
          records.stream()
              .map(FunctionRecord::getExecutionTime)
              .reduce(Duration.ZERO, Duration::plus);

      final List<FlameGraphGenerator.FlameFrame> children = new ArrayList<>();
      final Map<String, Duration> functionTimes = new HashMap<>();

      for (final FunctionRecord record : records) {
        functionTimes.merge(record.getName(), record.getExecutionTime(), Duration::plus);
      }

      for (final Map.Entry<String, Duration> entry : functionTimes.entrySet()) {
        children.add(
            new FlameGraphGenerator.FlameFrame(
                entry.getKey(), entry.getValue(), new ArrayList<>()));
      }

      return new FlameGraphGenerator.FlameFrame("root", totalTime, children);
    }

    /**
     * Gets the session start time.
     *
     * @return the start time
     */
    public Instant getStartTime() {
      return startTime;
    }

    /**
     * Gets the session duration so far.
     *
     * @return the elapsed duration
     */
    public Duration getElapsedTime() {
      return Duration.between(startTime, Instant.now());
    }

    /**
     * Checks if the session has timed out.
     *
     * @return true if timed out
     */
    public boolean isTimedOut() {
      return getElapsedTime().compareTo(timeout) >= 0;
    }

    /**
     * Checks if the session is closed.
     *
     * @return true if closed
     */
    public boolean isClosed() {
      return closed;
    }

    @Override
    public void close() {
      closed = true;
    }
  }

  /** Statistics collected during profiling. */
  public static final class ProfilingStatistics {

    private final long totalCalls;
    private final Duration totalTime;
    private final long totalMemory;
    private final Map<String, Long> functionCalls;

    ProfilingStatistics(
        final long totalCalls,
        final Duration totalTime,
        final long totalMemory,
        final Map<String, Long> functionCalls) {
      this.totalCalls = totalCalls;
      this.totalTime = totalTime;
      this.totalMemory = totalMemory;
      this.functionCalls = new HashMap<>(functionCalls);
    }

    /**
     * Gets the total number of function calls.
     *
     * @return the total number of function calls
     */
    public long getTotalCalls() {
      return totalCalls;
    }

    /**
     * Gets the total execution time.
     *
     * @return the total execution time
     */
    public Duration getTotalTime() {
      return totalTime;
    }

    /**
     * Gets the total memory used in bytes.
     *
     * @return the total memory used in bytes
     */
    public long getTotalMemory() {
      return totalMemory;
    }

    /**
     * Gets a map of function names to call counts.
     *
     * @return a map of function names to call counts
     */
    public Map<String, Long> getFunctionCalls() {
      return new HashMap<>(functionCalls);
    }
  }

  /** Internal record for function executions. */
  static final class FunctionRecord {

    private final String name;
    private final Duration executionTime;
    private final long memoryUsed;
    private final String runtimeType;
    private final Instant timestamp;

    FunctionRecord(
        final String name,
        final Duration executionTime,
        final long memoryUsed,
        final String runtimeType,
        final Instant timestamp) {
      this.name = name;
      this.executionTime = executionTime;
      this.memoryUsed = memoryUsed;
      this.runtimeType = runtimeType;
      this.timestamp = timestamp;
    }

    String getName() {
      return name;
    }

    Duration getExecutionTime() {
      return executionTime;
    }

    long getMemoryUsed() {
      return memoryUsed;
    }

    String getRuntimeType() {
      return runtimeType;
    }

    Instant getTimestamp() {
      return timestamp;
    }
  }

  /** Internal record for memory allocations. */
  static final class AllocationRecord {

    private final long id;
    private final long size;
    private final String label;
    private final Instant allocatedAt;
    private volatile Instant deallocatedAt;

    AllocationRecord(
        final long id, final long size, final String label, final Instant allocatedAt) {
      this.id = id;
      this.size = size;
      this.label = label;
      this.allocatedAt = allocatedAt;
    }

    void markDeallocated() {
      this.deallocatedAt = Instant.now();
    }

    long getId() {
      return id;
    }

    long getSize() {
      return size;
    }

    String getLabel() {
      return label;
    }

    Instant getAllocatedAt() {
      return allocatedAt;
    }

    Instant getDeallocatedAt() {
      return deallocatedAt;
    }

    boolean isDeallocated() {
      return deallocatedAt != null;
    }
  }
}
