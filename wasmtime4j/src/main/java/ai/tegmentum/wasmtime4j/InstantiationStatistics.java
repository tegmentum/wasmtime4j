package ai.tegmentum.wasmtime4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Statistics for streaming WebAssembly instantiation operations.
 *
 * <p>InstantiationStatistics provides real-time information about the progress and performance of
 * streaming instantiation, including function compilation progress, memory allocation, and resource
 * usage.
 *
 * @since 1.0.0
 */
public final class InstantiationStatistics {

  private final InstantiationPhase currentPhase;
  private final double completionPercentage;
  private final Instant startTime;
  private final Optional<Instant> endTime;
  private final Duration elapsedTime;
  private final long functionsCompiled;
  private final long totalFunctions;
  private final long memoryAllocated;
  private final long tablesAllocated;
  private final long globalsAllocated;
  private final int activeThreads;
  private final List<FunctionCompilationStats> functionStatistics;
  private final Optional<String> currentOperation;
  private final boolean memoryPoolingUsed;
  private final long instancePoolHits;
  private final long instancePoolMisses;

  private InstantiationStatistics(final Builder builder) {
    this.currentPhase = builder.currentPhase;
    this.completionPercentage = builder.completionPercentage;
    this.startTime = builder.startTime;
    this.endTime = builder.endTime;
    this.elapsedTime = builder.elapsedTime;
    this.functionsCompiled = builder.functionsCompiled;
    this.totalFunctions = builder.totalFunctions;
    this.memoryAllocated = builder.memoryAllocated;
    this.tablesAllocated = builder.tablesAllocated;
    this.globalsAllocated = builder.globalsAllocated;
    this.activeThreads = builder.activeThreads;
    this.functionStatistics = List.copyOf(builder.functionStatistics);
    this.currentOperation = builder.currentOperation;
    this.memoryPoolingUsed = builder.memoryPoolingUsed;
    this.instancePoolHits = builder.instancePoolHits;
    this.instancePoolMisses = builder.instancePoolMisses;
  }

  /**
   * Gets the current instantiation phase.
   *
   * @return current instantiation phase
   */
  public InstantiationPhase getCurrentPhase() {
    return currentPhase;
  }

  /**
   * Gets the estimated completion percentage.
   *
   * @return completion percentage (0.0 to 100.0)
   */
  public double getCompletionPercentage() {
    return completionPercentage;
  }

  /**
   * Gets the time when instantiation started.
   *
   * @return instantiation start time
   */
  public Instant getStartTime() {
    return startTime;
  }

  /**
   * Gets the time when instantiation ended (if completed).
   *
   * @return instantiation end time, or empty if still in progress
   */
  public Optional<Instant> getEndTime() {
    return endTime;
  }

  /**
   * Gets the elapsed time since instantiation started.
   *
   * @return elapsed duration
   */
  public Duration getElapsedTime() {
    return elapsedTime;
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
   * @return total number of functions
   */
  public long getTotalFunctions() {
    return totalFunctions;
  }

  /**
   * Gets the amount of memory allocated in bytes.
   *
   * @return memory allocated in bytes
   */
  public long getMemoryAllocated() {
    return memoryAllocated;
  }

  /**
   * Gets the number of tables allocated.
   *
   * @return number of tables allocated
   */
  public long getTablesAllocated() {
    return tablesAllocated;
  }

  /**
   * Gets the number of globals allocated.
   *
   * @return number of globals allocated
   */
  public long getGlobalsAllocated() {
    return globalsAllocated;
  }

  /**
   * Gets the number of active instantiation threads.
   *
   * @return number of active threads
   */
  public int getActiveThreads() {
    return activeThreads;
  }

  /**
   * Gets detailed statistics for function compilation.
   *
   * @return list of function compilation statistics
   */
  public List<FunctionCompilationStats> getFunctionStatistics() {
    return functionStatistics;
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
   * Checks if memory pooling was used for this instantiation.
   *
   * @return true if memory pooling was used
   */
  public boolean isMemoryPoolingUsed() {
    return memoryPoolingUsed;
  }

  /**
   * Gets the number of instance pool hits during instantiation.
   *
   * @return number of instance pool hits
   */
  public long getInstancePoolHits() {
    return instancePoolHits;
  }

  /**
   * Gets the number of instance pool misses during instantiation.
   *
   * @return number of instance pool misses
   */
  public long getInstancePoolMisses() {
    return instancePoolMisses;
  }

  /**
   * Calculates the function compilation progress ratio.
   *
   * @return function compilation progress (0.0 to 1.0)
   */
  public double getFunctionCompilationProgress() {
    return totalFunctions > 0 ? (double) functionsCompiled / totalFunctions : 0.0;
  }

  /**
   * Calculates the instance pool hit ratio.
   *
   * @return pool hit ratio (0.0 to 1.0), or 0.0 if no pool operations
   */
  public double getInstancePoolHitRatio() {
    final long totalPoolOperations = instancePoolHits + instancePoolMisses;
    return totalPoolOperations > 0 ? (double) instancePoolHits / totalPoolOperations : 0.0;
  }

  /**
   * Checks if instantiation is complete.
   *
   * @return true if instantiation is complete
   */
  public boolean isComplete() {
    return endTime.isPresent();
  }

  /**
   * Creates a new builder for InstantiationStatistics.
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
    final InstantiationStatistics that = (InstantiationStatistics) obj;
    return Double.compare(that.completionPercentage, completionPercentage) == 0
        && functionsCompiled == that.functionsCompiled
        && totalFunctions == that.totalFunctions
        && memoryAllocated == that.memoryAllocated
        && tablesAllocated == that.tablesAllocated
        && globalsAllocated == that.globalsAllocated
        && activeThreads == that.activeThreads
        && memoryPoolingUsed == that.memoryPoolingUsed
        && instancePoolHits == that.instancePoolHits
        && instancePoolMisses == that.instancePoolMisses
        && currentPhase == that.currentPhase
        && Objects.equals(startTime, that.startTime)
        && Objects.equals(endTime, that.endTime)
        && Objects.equals(elapsedTime, that.elapsedTime)
        && Objects.equals(functionStatistics, that.functionStatistics)
        && Objects.equals(currentOperation, that.currentOperation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        currentPhase,
        completionPercentage,
        startTime,
        endTime,
        elapsedTime,
        functionsCompiled,
        totalFunctions,
        memoryAllocated,
        tablesAllocated,
        globalsAllocated,
        activeThreads,
        functionStatistics,
        currentOperation,
        memoryPoolingUsed,
        instancePoolHits,
        instancePoolMisses);
  }

  @Override
  public String toString() {
    return "InstantiationStatistics{"
        + "currentPhase="
        + currentPhase
        + ", completionPercentage="
        + completionPercentage
        + ", startTime="
        + startTime
        + ", endTime="
        + endTime
        + ", elapsedTime="
        + elapsedTime
        + ", functionsCompiled="
        + functionsCompiled
        + ", totalFunctions="
        + totalFunctions
        + ", memoryAllocated="
        + memoryAllocated
        + ", tablesAllocated="
        + tablesAllocated
        + ", globalsAllocated="
        + globalsAllocated
        + ", activeThreads="
        + activeThreads
        + ", memoryPoolingUsed="
        + memoryPoolingUsed
        + ", instancePoolHits="
        + instancePoolHits
        + ", instancePoolMisses="
        + instancePoolMisses
        + '}';
  }

  /** Builder for InstantiationStatistics. */
  public static final class Builder {
    private InstantiationPhase currentPhase = InstantiationPhase.PREPARATION;
    private double completionPercentage = 0.0;
    private Instant startTime = Instant.now();
    private Optional<Instant> endTime = Optional.empty();
    private Duration elapsedTime = Duration.ZERO;
    private long functionsCompiled = 0;
    private long totalFunctions = 0;
    private long memoryAllocated = 0;
    private long tablesAllocated = 0;
    private long globalsAllocated = 0;
    private int activeThreads = 0;
    private List<FunctionCompilationStats> functionStatistics = List.of();
    private Optional<String> currentOperation = Optional.empty();
    private boolean memoryPoolingUsed = false;
    private long instancePoolHits = 0;
    private long instancePoolMisses = 0;

    private Builder() {}

    public Builder currentPhase(final InstantiationPhase currentPhase) {
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

    public Builder functionsCompiled(final long functionsCompiled) {
      this.functionsCompiled = functionsCompiled;
      return this;
    }

    public Builder totalFunctions(final long totalFunctions) {
      this.totalFunctions = totalFunctions;
      return this;
    }

    public Builder memoryAllocated(final long memoryAllocated) {
      this.memoryAllocated = memoryAllocated;
      return this;
    }

    public Builder tablesAllocated(final long tablesAllocated) {
      this.tablesAllocated = tablesAllocated;
      return this;
    }

    public Builder globalsAllocated(final long globalsAllocated) {
      this.globalsAllocated = globalsAllocated;
      return this;
    }

    public Builder activeThreads(final int activeThreads) {
      this.activeThreads = activeThreads;
      return this;
    }

    /**
     * Sets the function compilation statistics.
     *
     * @param functionStatistics the list of function compilation statistics
     * @return this builder
     */
    public Builder functionStatistics(final List<FunctionCompilationStats> functionStatistics) {
      this.functionStatistics =
          Objects.requireNonNull(functionStatistics, "Function statistics cannot be null");
      return this;
    }

    public Builder currentOperation(final String currentOperation) {
      this.currentOperation = Optional.ofNullable(currentOperation);
      return this;
    }

    public Builder memoryPoolingUsed(final boolean memoryPoolingUsed) {
      this.memoryPoolingUsed = memoryPoolingUsed;
      return this;
    }

    public Builder instancePoolHits(final long instancePoolHits) {
      this.instancePoolHits = instancePoolHits;
      return this;
    }

    public Builder instancePoolMisses(final long instancePoolMisses) {
      this.instancePoolMisses = instancePoolMisses;
      return this;
    }

    public InstantiationStatistics build() {
      return new InstantiationStatistics(this);
    }
  }
}
