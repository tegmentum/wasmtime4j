package ai.tegmentum.wasmtime4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Statistics for streaming WebAssembly compilation operations.
 *
 * <p>StreamingStatistics provides real-time information about the progress and performance of
 * streaming compilation, including throughput metrics, phase information, and resource usage.
 *
 * @since 1.0.0
 */
public final class StreamingStatistics {

  private final long totalBytesReceived;
  private final long totalBytesProcessed;
  private final CompilationPhase currentPhase;
  private final double completionPercentage;
  private final Instant startTime;
  private final Optional<Instant> endTime;
  private final Duration elapsedTime;
  private final double throughputBytesPerSecond;
  private final long memoryUsage;
  private final int activeThreads;
  private final List<CompilationPhaseStats> phaseStatistics;
  private final Optional<String> currentOperation;
  private final long functionsCompiled;
  private final long totalFunctions;
  private final boolean cachingEnabled;
  private final long cacheHits;
  private final long cacheMisses;

  private StreamingStatistics(final Builder builder) {
    this.totalBytesReceived = builder.totalBytesReceived;
    this.totalBytesProcessed = builder.totalBytesProcessed;
    this.currentPhase = builder.currentPhase;
    this.completionPercentage = builder.completionPercentage;
    this.startTime = builder.startTime;
    this.endTime = builder.endTime;
    this.elapsedTime = builder.elapsedTime;
    this.throughputBytesPerSecond = builder.throughputBytesPerSecond;
    this.memoryUsage = builder.memoryUsage;
    this.activeThreads = builder.activeThreads;
    this.phaseStatistics = List.copyOf(builder.phaseStatistics);
    this.currentOperation = builder.currentOperation;
    this.functionsCompiled = builder.functionsCompiled;
    this.totalFunctions = builder.totalFunctions;
    this.cachingEnabled = builder.cachingEnabled;
    this.cacheHits = builder.cacheHits;
    this.cacheMisses = builder.cacheMisses;
  }

  /**
   * Gets the total number of bytes received from the input stream.
   *
   * @return total bytes received
   */
  public long getTotalBytesReceived() {
    return totalBytesReceived;
  }

  /**
   * Gets the total number of bytes processed by the compiler.
   *
   * <p>This may be less than bytes received if processing is still in progress.
   *
   * @return total bytes processed
   */
  public long getTotalBytesProcessed() {
    return totalBytesProcessed;
  }

  /**
   * Gets the current compilation phase.
   *
   * @return current compilation phase
   */
  public CompilationPhase getCurrentPhase() {
    return currentPhase;
  }

  /**
   * Gets the estimated completion percentage.
   *
   * <p>This is an estimate based on bytes processed and compilation phases completed.
   *
   * @return completion percentage (0.0 to 100.0)
   */
  public double getCompletionPercentage() {
    return completionPercentage;
  }

  /**
   * Gets the time when compilation started.
   *
   * @return compilation start time
   */
  public Instant getStartTime() {
    return startTime;
  }

  /**
   * Gets the time when compilation ended (if completed).
   *
   * @return compilation end time, or empty if still in progress
   */
  public Optional<Instant> getEndTime() {
    return endTime;
  }

  /**
   * Gets the elapsed time since compilation started.
   *
   * @return elapsed duration
   */
  public Duration getElapsedTime() {
    return elapsedTime;
  }

  /**
   * Gets the current throughput in bytes per second.
   *
   * @return throughput in bytes per second
   */
  public double getThroughputBytesPerSecond() {
    return throughputBytesPerSecond;
  }

  /**
   * Gets the current memory usage in bytes.
   *
   * @return memory usage in bytes
   */
  public long getMemoryUsage() {
    return memoryUsage;
  }

  /**
   * Gets the number of active compilation threads.
   *
   * @return number of active threads
   */
  public int getActiveThreads() {
    return activeThreads;
  }

  /**
   * Gets detailed statistics for each compilation phase.
   *
   * @return list of phase statistics
   */
  public List<CompilationPhaseStats> getPhaseStatistics() {
    return phaseStatistics;
  }

  /**
   * Gets the current operation being performed (if available).
   *
   * @return current operation description
   */
  public Optional<String> getCurrentOperation() {
    return currentOperation;
  }

  /**
   * Gets the number of functions successfully compiled.
   *
   * @return number of functions compiled
   */
  public long getFunctionsCompiled() {
    return functionsCompiled;
  }

  /**
   * Gets the total number of functions in the module.
   *
   * <p>This may be 0 if the function count is not yet known.
   *
   * @return total number of functions
   */
  public long getTotalFunctions() {
    return totalFunctions;
  }

  /**
   * Checks if caching is enabled for this compilation.
   *
   * @return true if caching is enabled
   */
  public boolean isCachingEnabled() {
    return cachingEnabled;
  }

  /**
   * Gets the number of cache hits during compilation.
   *
   * @return number of cache hits
   */
  public long getCacheHits() {
    return cacheHits;
  }

  /**
   * Gets the number of cache misses during compilation.
   *
   * @return number of cache misses
   */
  public long getCacheMisses() {
    return cacheMisses;
  }

  /**
   * Calculates the cache hit ratio.
   *
   * @return cache hit ratio (0.0 to 1.0), or 0.0 if no cache operations
   */
  public double getCacheHitRatio() {
    final long totalCacheOperations = cacheHits + cacheMisses;
    return totalCacheOperations > 0 ? (double) cacheHits / totalCacheOperations : 0.0;
  }

  /**
   * Checks if compilation is complete.
   *
   * @return true if compilation is complete
   */
  public boolean isComplete() {
    return endTime.isPresent();
  }

  /**
   * Creates a new builder for StreamingStatistics.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final StreamingStatistics that = (StreamingStatistics) obj;
    return totalBytesReceived == that.totalBytesReceived
        && totalBytesProcessed == that.totalBytesProcessed
        && Double.compare(that.completionPercentage, completionPercentage) == 0
        && Double.compare(that.throughputBytesPerSecond, throughputBytesPerSecond) == 0
        && memoryUsage == that.memoryUsage
        && activeThreads == that.activeThreads
        && functionsCompiled == that.functionsCompiled
        && totalFunctions == that.totalFunctions
        && cachingEnabled == that.cachingEnabled
        && cacheHits == that.cacheHits
        && cacheMisses == that.cacheMisses
        && currentPhase == that.currentPhase
        && Objects.equals(startTime, that.startTime)
        && Objects.equals(endTime, that.endTime)
        && Objects.equals(elapsedTime, that.elapsedTime)
        && Objects.equals(phaseStatistics, that.phaseStatistics)
        && Objects.equals(currentOperation, that.currentOperation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        totalBytesReceived,
        totalBytesProcessed,
        currentPhase,
        completionPercentage,
        startTime,
        endTime,
        elapsedTime,
        throughputBytesPerSecond,
        memoryUsage,
        activeThreads,
        phaseStatistics,
        currentOperation,
        functionsCompiled,
        totalFunctions,
        cachingEnabled,
        cacheHits,
        cacheMisses);
  }

  @Override
  public String toString() {
    return "StreamingStatistics{"
        + "totalBytesReceived="
        + totalBytesReceived
        + ", totalBytesProcessed="
        + totalBytesProcessed
        + ", currentPhase="
        + currentPhase
        + ", completionPercentage="
        + completionPercentage
        + ", startTime="
        + startTime
        + ", endTime="
        + endTime
        + ", elapsedTime="
        + elapsedTime
        + ", throughputBytesPerSecond="
        + throughputBytesPerSecond
        + ", memoryUsage="
        + memoryUsage
        + ", activeThreads="
        + activeThreads
        + ", functionsCompiled="
        + functionsCompiled
        + ", totalFunctions="
        + totalFunctions
        + ", cachingEnabled="
        + cachingEnabled
        + ", cacheHits="
        + cacheHits
        + ", cacheMisses="
        + cacheMisses
        + '}';
  }

  /** Builder for StreamingStatistics. */
  public static final class Builder {
    private long totalBytesReceived = 0;
    private long totalBytesProcessed = 0;
    private CompilationPhase currentPhase = CompilationPhase.PARSING;
    private double completionPercentage = 0.0;
    private Instant startTime = Instant.now();
    private Optional<Instant> endTime = Optional.empty();
    private Duration elapsedTime = Duration.ZERO;
    private double throughputBytesPerSecond = 0.0;
    private long memoryUsage = 0;
    private int activeThreads = 0;
    private List<CompilationPhaseStats> phaseStatistics = List.of();
    private Optional<String> currentOperation = Optional.empty();
    private long functionsCompiled = 0;
    private long totalFunctions = 0;
    private boolean cachingEnabled = false;
    private long cacheHits = 0;
    private long cacheMisses = 0;

    private Builder() {}

    public Builder totalBytesReceived(final long totalBytesReceived) {
      this.totalBytesReceived = totalBytesReceived;
      return this;
    }

    public Builder totalBytesProcessed(final long totalBytesProcessed) {
      this.totalBytesProcessed = totalBytesProcessed;
      return this;
    }

    public Builder currentPhase(final CompilationPhase currentPhase) {
      this.currentPhase = Objects.requireNonNull(currentPhase, "Current phase cannot be null");
      return this;
    }

    public Builder completionPercentage(final double completionPercentage) {
      this.completionPercentage = completionPercentage;
      return this;
    }

    public Builder startTime(final Instant startTime) {
      this.startTime = Objects.requireNonNull(startTime, "Start time cannot be null");
      return this;
    }

    public Builder endTime(final Instant endTime) {
      this.endTime = Optional.ofNullable(endTime);
      return this;
    }

    public Builder elapsedTime(final Duration elapsedTime) {
      this.elapsedTime = Objects.requireNonNull(elapsedTime, "Elapsed time cannot be null");
      return this;
    }

    public Builder throughputBytesPerSecond(final double throughputBytesPerSecond) {
      this.throughputBytesPerSecond = throughputBytesPerSecond;
      return this;
    }

    public Builder memoryUsage(final long memoryUsage) {
      this.memoryUsage = memoryUsage;
      return this;
    }

    public Builder activeThreads(final int activeThreads) {
      this.activeThreads = activeThreads;
      return this;
    }

    /**
     * Sets the compilation phase statistics.
     *
     * @param phaseStatistics the list of compilation phase statistics
     * @return this builder
     */
    public Builder phaseStatistics(final List<CompilationPhaseStats> phaseStatistics) {
      this.phaseStatistics =
          Objects.requireNonNull(phaseStatistics, "Phase statistics cannot be null");
      return this;
    }

    public Builder currentOperation(final String currentOperation) {
      this.currentOperation = Optional.ofNullable(currentOperation);
      return this;
    }

    public Builder functionsCompiled(final long functionsCompiled) {
      this.functionsCompiled = functionsCompiled;
      return this;
    }

    public Builder totalFunctions(final long totalFunctions) {
      this.totalFunctions = totalFunctions;
      return this;
    }

    public Builder cachingEnabled(final boolean cachingEnabled) {
      this.cachingEnabled = cachingEnabled;
      return this;
    }

    public Builder cacheHits(final long cacheHits) {
      this.cacheHits = cacheHits;
      return this;
    }

    public Builder cacheMisses(final long cacheMisses) {
      this.cacheMisses = cacheMisses;
      return this;
    }

    public StreamingStatistics build() {
      return new StreamingStatistics(this);
    }
  }
}
