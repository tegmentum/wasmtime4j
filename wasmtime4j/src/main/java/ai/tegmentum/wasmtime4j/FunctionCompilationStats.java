package ai.tegmentum.wasmtime4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Statistics for individual function compilation during streaming instantiation.
 *
 * <p>FunctionCompilationStats tracks the performance and resource usage of individual function
 * compilation operations during streaming instantiation.
 *
 * @since 1.0.0
 */
public final class FunctionCompilationStats {

  private final int functionIndex;
  private final Optional<String> functionName;
  private final Optional<Instant> startTime;
  private final Optional<Instant> endTime;
  private final Duration duration;
  private final long codeSize;
  private final long compiledSize;
  private final CompilationPriority priority;
  private final boolean lazilyCompiled;
  private final boolean completed;
  private final Optional<String> errorMessage;

  private FunctionCompilationStats(final Builder builder) {
    this.functionIndex = builder.functionIndex;
    this.functionName = builder.functionName;
    this.startTime = builder.startTime;
    this.endTime = builder.endTime;
    this.duration = builder.duration;
    this.codeSize = builder.codeSize;
    this.compiledSize = builder.compiledSize;
    this.priority = builder.priority;
    this.lazilyCompiled = builder.lazilyCompiled;
    this.completed = builder.completed;
    this.errorMessage = builder.errorMessage;
  }

  /**
   * Gets the function index in the module.
   *
   * @return function index
   */
  public int getFunctionIndex() {
    return functionIndex;
  }

  /**
   * Gets the function name (if available).
   *
   * @return function name, or empty if not available
   */
  public Optional<String> getFunctionName() {
    return functionName;
  }

  /**
   * Gets the time when compilation started.
   *
   * @return start time, or empty if not yet started
   */
  public Optional<Instant> getStartTime() {
    return startTime;
  }

  /**
   * Gets the time when compilation ended.
   *
   * @return end time, or empty if not yet completed
   */
  public Optional<Instant> getEndTime() {
    return endTime;
  }

  /**
   * Gets the duration of compilation.
   *
   * @return compilation duration
   */
  public Duration getDuration() {
    return duration;
  }

  /**
   * Gets the size of the WebAssembly function code in bytes.
   *
   * @return code size in bytes
   */
  public long getCodeSize() {
    return codeSize;
  }

  /**
   * Gets the size of the compiled native code in bytes.
   *
   * @return compiled size in bytes
   */
  public long getCompiledSize() {
    return compiledSize;
  }

  /**
   * Gets the compilation priority for this function.
   *
   * @return compilation priority
   */
  public CompilationPriority getPriority() {
    return priority;
  }

  /**
   * Checks if this function was compiled lazily.
   *
   * <p>Lazily compiled functions are compiled on first invocation rather than during
   * instantiation.
   *
   * @return true if function was compiled lazily
   */
  public boolean isLazilyCompiled() {
    return lazilyCompiled;
  }

  /**
   * Checks if compilation completed successfully.
   *
   * @return true if compilation completed
   */
  public boolean isCompleted() {
    return completed;
  }

  /**
   * Gets the error message if compilation failed.
   *
   * @return error message, or empty if no error occurred
   */
  public Optional<String> getErrorMessage() {
    return errorMessage;
  }

  /**
   * Calculates the compilation throughput in bytes per second.
   *
   * @return throughput in bytes per second, or 0.0 if duration is zero
   */
  public double getCompilationThroughput() {
    if (duration.isZero() || codeSize == 0) {
      return 0.0;
    }
    return (double) codeSize / duration.toMillis() * 1000.0;
  }

  /**
   * Calculates the code expansion ratio (compiled size / original size).
   *
   * @return code expansion ratio, or 0.0 if code size is zero
   */
  public double getCodeExpansionRatio() {
    return codeSize > 0 ? (double) compiledSize / codeSize : 0.0;
  }

  /**
   * Creates a new builder for FunctionCompilationStats.
   *
   * @param functionIndex the function index (must not be negative)
   * @return a new builder instance
   * @throws IllegalArgumentException if functionIndex is negative
   */
  public static Builder builder(final int functionIndex) {
    if (functionIndex < 0) {
      throw new IllegalArgumentException("Function index cannot be negative");
    }
    return new Builder(functionIndex);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final FunctionCompilationStats that = (FunctionCompilationStats) obj;
    return functionIndex == that.functionIndex
        && codeSize == that.codeSize
        && compiledSize == that.compiledSize
        && lazilyCompiled == that.lazilyCompiled
        && completed == that.completed
        && Objects.equals(functionName, that.functionName)
        && Objects.equals(startTime, that.startTime)
        && Objects.equals(endTime, that.endTime)
        && Objects.equals(duration, that.duration)
        && priority == that.priority
        && Objects.equals(errorMessage, that.errorMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        functionIndex,
        functionName,
        startTime,
        endTime,
        duration,
        codeSize,
        compiledSize,
        priority,
        lazilyCompiled,
        completed,
        errorMessage);
  }

  @Override
  public String toString() {
    return "FunctionCompilationStats{"
        + "functionIndex="
        + functionIndex
        + ", functionName="
        + functionName
        + ", startTime="
        + startTime
        + ", endTime="
        + endTime
        + ", duration="
        + duration
        + ", codeSize="
        + codeSize
        + ", compiledSize="
        + compiledSize
        + ", priority="
        + priority
        + ", lazilyCompiled="
        + lazilyCompiled
        + ", completed="
        + completed
        + ", errorMessage="
        + errorMessage
        + '}';
  }

  /** Builder for FunctionCompilationStats. */
  public static final class Builder {
    private final int functionIndex;
    private Optional<String> functionName = Optional.empty();
    private Optional<Instant> startTime = Optional.empty();
    private Optional<Instant> endTime = Optional.empty();
    private Duration duration = Duration.ZERO;
    private long codeSize = 0;
    private long compiledSize = 0;
    private CompilationPriority priority = CompilationPriority.NORMAL;
    private boolean lazilyCompiled = false;
    private boolean completed = false;
    private Optional<String> errorMessage = Optional.empty();

    private Builder(final int functionIndex) {
      this.functionIndex = functionIndex;
    }

    /**
     * Sets the function name.
     *
     * @param functionName the function name (can be null)
     * @return this builder
     */
    public Builder functionName(final String functionName) {
      this.functionName = Optional.ofNullable(functionName);
      return this;
    }

    /**
     * Sets the compilation start time.
     *
     * @param startTime the start time (can be null)
     * @return this builder
     */
    public Builder startTime(final Instant startTime) {
      this.startTime = Optional.ofNullable(startTime);
      return this;
    }

    /**
     * Sets the compilation end time.
     *
     * @param endTime the end time (can be null)
     * @return this builder
     */
    public Builder endTime(final Instant endTime) {
      this.endTime = Optional.ofNullable(endTime);
      return this;
    }

    /**
     * Sets the compilation duration.
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
     * Sets the function code size.
     *
     * @param codeSize the code size in bytes (must not be negative)
     * @return this builder
     * @throws IllegalArgumentException if codeSize is negative
     */
    public Builder codeSize(final long codeSize) {
      if (codeSize < 0) {
        throw new IllegalArgumentException("Code size cannot be negative");
      }
      this.codeSize = codeSize;
      return this;
    }

    /**
     * Sets the compiled code size.
     *
     * @param compiledSize the compiled size in bytes (must not be negative)
     * @return this builder
     * @throws IllegalArgumentException if compiledSize is negative
     */
    public Builder compiledSize(final long compiledSize) {
      if (compiledSize < 0) {
        throw new IllegalArgumentException("Compiled size cannot be negative");
      }
      this.compiledSize = compiledSize;
      return this;
    }

    /**
     * Sets the compilation priority.
     *
     * @param priority the compilation priority (must not be null)
     * @return this builder
     * @throws IllegalArgumentException if priority is null
     */
    public Builder priority(final CompilationPriority priority) {
      if (priority == null) {
        throw new IllegalArgumentException("Priority cannot be null");
      }
      this.priority = priority;
      return this;
    }

    /**
     * Sets whether the function was compiled lazily.
     *
     * @param lazilyCompiled whether the function was compiled lazily
     * @return this builder
     */
    public Builder lazilyCompiled(final boolean lazilyCompiled) {
      this.lazilyCompiled = lazilyCompiled;
      return this;
    }

    /**
     * Sets whether compilation completed successfully.
     *
     * @param completed whether compilation completed
     * @return this builder
     */
    public Builder completed(final boolean completed) {
      this.completed = completed;
      return this;
    }

    /**
     * Sets an error message if compilation failed.
     *
     * @param errorMessage the error message (can be null)
     * @return this builder
     */
    public Builder errorMessage(final String errorMessage) {
      this.errorMessage = Optional.ofNullable(errorMessage);
      return this;
    }

    /**
     * Builds the FunctionCompilationStats instance.
     *
     * @return a new FunctionCompilationStats
     */
    public FunctionCompilationStats build() {
      return new FunctionCompilationStats(this);
    }
  }
}