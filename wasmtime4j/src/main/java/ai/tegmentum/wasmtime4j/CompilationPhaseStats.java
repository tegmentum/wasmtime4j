package ai.tegmentum.wasmtime4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Statistics for a specific compilation phase.
 *
 * <p>CompilationPhaseStats tracks the performance and resource usage of individual compilation
 * phases during streaming compilation.
 *
 * @since 1.0.0
 */
public final class CompilationPhaseStats {

  private final CompilationPhase phase;
  private final Optional<Instant> startTime;
  private final Optional<Instant> endTime;
  private final Duration duration;
  private final long bytesProcessed;
  private final long memoryUsed;
  private final int threadsUsed;
  private final boolean completed;
  private final Optional<String> errorMessage;

  private CompilationPhaseStats(final Builder builder) {
    this.phase = builder.phase;
    this.startTime = builder.startTime;
    this.endTime = builder.endTime;
    this.duration = builder.duration;
    this.bytesProcessed = builder.bytesProcessed;
    this.memoryUsed = builder.memoryUsed;
    this.threadsUsed = builder.threadsUsed;
    this.completed = builder.completed;
    this.errorMessage = builder.errorMessage;
  }

  /**
   * Gets the compilation phase these statistics represent.
   *
   * @return compilation phase
   */
  public CompilationPhase getPhase() {
    return phase;
  }

  /**
   * Gets the time when this phase started.
   *
   * @return start time, or empty if not yet started
   */
  public Optional<Instant> getStartTime() {
    return startTime;
  }

  /**
   * Gets the time when this phase ended.
   *
   * @return end time, or empty if not yet completed
   */
  public Optional<Instant> getEndTime() {
    return endTime;
  }

  /**
   * Gets the duration of this phase.
   *
   * @return phase duration
   */
  public Duration getDuration() {
    return duration;
  }

  /**
   * Gets the number of bytes processed during this phase.
   *
   * @return bytes processed
   */
  public long getBytesProcessed() {
    return bytesProcessed;
  }

  /**
   * Gets the peak memory usage during this phase.
   *
   * @return peak memory usage in bytes
   */
  public long getMemoryUsed() {
    return memoryUsed;
  }

  /**
   * Gets the number of threads used during this phase.
   *
   * @return number of threads used
   */
  public int getThreadsUsed() {
    return threadsUsed;
  }

  /**
   * Checks if this phase completed successfully.
   *
   * @return true if the phase completed
   */
  public boolean isCompleted() {
    return completed;
  }

  /**
   * Gets the error message if the phase failed.
   *
   * @return error message, or empty if no error occurred
   */
  public Optional<String> getErrorMessage() {
    return errorMessage;
  }

  /**
   * Calculates the throughput for this phase in bytes per second.
   *
   * @return throughput in bytes per second, or 0.0 if duration is zero
   */
  public double getThroughputBytesPerSecond() {
    if (duration.isZero() || bytesProcessed == 0) {
      return 0.0;
    }
    return (double) bytesProcessed / duration.toMillis() * 1000.0;
  }

  /**
   * Creates a new builder for CompilationPhaseStats.
   *
   * @param phase the compilation phase (must not be null)
   * @return a new builder instance
   * @throws IllegalArgumentException if phase is null
   */
  public static Builder builder(final CompilationPhase phase) {
    if (phase == null) {
      throw new IllegalArgumentException("Phase cannot be null");
    }
    return new Builder(phase);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final CompilationPhaseStats that = (CompilationPhaseStats) obj;
    return bytesProcessed == that.bytesProcessed
        && memoryUsed == that.memoryUsed
        && threadsUsed == that.threadsUsed
        && completed == that.completed
        && phase == that.phase
        && Objects.equals(startTime, that.startTime)
        && Objects.equals(endTime, that.endTime)
        && Objects.equals(duration, that.duration)
        && Objects.equals(errorMessage, that.errorMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        phase,
        startTime,
        endTime,
        duration,
        bytesProcessed,
        memoryUsed,
        threadsUsed,
        completed,
        errorMessage);
  }

  @Override
  public String toString() {
    return "CompilationPhaseStats{"
        + "phase="
        + phase
        + ", startTime="
        + startTime
        + ", endTime="
        + endTime
        + ", duration="
        + duration
        + ", bytesProcessed="
        + bytesProcessed
        + ", memoryUsed="
        + memoryUsed
        + ", threadsUsed="
        + threadsUsed
        + ", completed="
        + completed
        + ", errorMessage="
        + errorMessage
        + '}';
  }

  /** Builder for CompilationPhaseStats. */
  public static final class Builder {
    private final CompilationPhase phase;
    private Optional<Instant> startTime = Optional.empty();
    private Optional<Instant> endTime = Optional.empty();
    private Duration duration = Duration.ZERO;
    private long bytesProcessed = 0;
    private long memoryUsed = 0;
    private int threadsUsed = 0;
    private boolean completed = false;
    private Optional<String> errorMessage = Optional.empty();

    private Builder(final CompilationPhase phase) {
      this.phase = phase;
    }

    /**
     * Sets the start time for this phase.
     *
     * @param startTime the start time (can be null)
     * @return this builder
     */
    public Builder startTime(final Instant startTime) {
      this.startTime = Optional.ofNullable(startTime);
      return this;
    }

    /**
     * Sets the end time for this phase.
     *
     * @param endTime the end time (can be null)
     * @return this builder
     */
    public Builder endTime(final Instant endTime) {
      this.endTime = Optional.ofNullable(endTime);
      return this;
    }

    /**
     * Sets the duration for this phase.
     *
     * @param duration the duration (must not be null or negative)
     * @return this builder
     * @throws IllegalArgumentException if duration is null or negative
     */
    public Builder duration(final Duration duration) {
      if (duration == null) {
        throw new IllegalArgumentException("Duration cannot be null");
      }
      if (duration.isNegative()) {
        throw new IllegalArgumentException("Duration cannot be negative");
      }
      this.duration = duration;
      return this;
    }

    /**
     * Sets the number of bytes processed during this phase.
     *
     * @param bytesProcessed the number of bytes processed (must not be negative)
     * @return this builder
     * @throws IllegalArgumentException if bytesProcessed is negative
     */
    public Builder bytesProcessed(final long bytesProcessed) {
      if (bytesProcessed < 0) {
        throw new IllegalArgumentException("Bytes processed cannot be negative");
      }
      this.bytesProcessed = bytesProcessed;
      return this;
    }

    /**
     * Sets the peak memory usage during this phase.
     *
     * @param memoryUsed the peak memory usage in bytes (must not be negative)
     * @return this builder
     * @throws IllegalArgumentException if memoryUsed is negative
     */
    public Builder memoryUsed(final long memoryUsed) {
      if (memoryUsed < 0) {
        throw new IllegalArgumentException("Memory used cannot be negative");
      }
      this.memoryUsed = memoryUsed;
      return this;
    }

    /**
     * Sets the number of threads used during this phase.
     *
     * @param threadsUsed the number of threads used (must not be negative)
     * @return this builder
     * @throws IllegalArgumentException if threadsUsed is negative
     */
    public Builder threadsUsed(final int threadsUsed) {
      if (threadsUsed < 0) {
        throw new IllegalArgumentException("Threads used cannot be negative");
      }
      this.threadsUsed = threadsUsed;
      return this;
    }

    /**
     * Sets whether this phase completed successfully.
     *
     * @param completed whether the phase completed
     * @return this builder
     */
    public Builder completed(final boolean completed) {
      this.completed = completed;
      return this;
    }

    /**
     * Sets an error message if the phase failed.
     *
     * @param errorMessage the error message (can be null)
     * @return this builder
     */
    public Builder errorMessage(final String errorMessage) {
      this.errorMessage = Optional.ofNullable(errorMessage);
      return this;
    }

    /**
     * Builds the CompilationPhaseStats instance.
     *
     * @return a new CompilationPhaseStats
     */
    public CompilationPhaseStats build() {
      return new CompilationPhaseStats(this);
    }
  }
}
