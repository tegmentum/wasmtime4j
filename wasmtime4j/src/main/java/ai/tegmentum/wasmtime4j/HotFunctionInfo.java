package ai.tegmentum.wasmtime4j;

import java.util.Objects;
import java.util.Optional;

/**
 * Information about a hot function identified during analysis.
 *
 * <p>HotFunctionInfo provides details about a function that has been identified as frequently
 * called and should be prioritized during streaming compilation.
 *
 * @since 1.0.0
 */
public final class HotFunctionInfo {

  private final int functionIndex;
  private final Optional<String> functionName;
  private final double hotnessScore;
  private final long estimatedCallFrequency;
  private final CompilationPriority recommendedPriority;
  private final HotnessReason reason;
  private final long codeSize;
  private final Optional<String> sourceLocation;

  private HotFunctionInfo(final Builder builder) {
    this.functionIndex = builder.functionIndex;
    this.functionName = builder.functionName;
    this.hotnessScore = builder.hotnessScore;
    this.estimatedCallFrequency = builder.estimatedCallFrequency;
    this.recommendedPriority = builder.recommendedPriority;
    this.reason = builder.reason;
    this.codeSize = builder.codeSize;
    this.sourceLocation = builder.sourceLocation;
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
   * Gets the hotness score for this function.
   *
   * <p>Hotness scores range from 0.0 (coldest) to 1.0 (hottest).
   *
   * @return hotness score
   */
  public double getHotnessScore() {
    return hotnessScore;
  }

  /**
   * Gets the estimated call frequency for this function.
   *
   * @return estimated number of calls per execution
   */
  public long getEstimatedCallFrequency() {
    return estimatedCallFrequency;
  }

  /**
   * Gets the recommended compilation priority for this function.
   *
   * @return recommended compilation priority
   */
  public CompilationPriority getRecommendedPriority() {
    return recommendedPriority;
  }

  /**
   * Gets the reason this function was identified as hot.
   *
   * @return hotness reason
   */
  public HotnessReason getReason() {
    return reason;
  }

  /**
   * Gets the size of the function code in bytes.
   *
   * @return code size in bytes
   */
  public long getCodeSize() {
    return codeSize;
  }

  /**
   * Gets the source location information (if available).
   *
   * @return source location, or empty if not available
   */
  public Optional<String> getSourceLocation() {
    return sourceLocation;
  }

  /**
   * Creates a new builder for HotFunctionInfo.
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
    final HotFunctionInfo that = (HotFunctionInfo) obj;
    return functionIndex == that.functionIndex
        && Double.compare(that.hotnessScore, hotnessScore) == 0
        && estimatedCallFrequency == that.estimatedCallFrequency
        && codeSize == that.codeSize
        && Objects.equals(functionName, that.functionName)
        && recommendedPriority == that.recommendedPriority
        && reason == that.reason
        && Objects.equals(sourceLocation, that.sourceLocation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        functionIndex,
        functionName,
        hotnessScore,
        estimatedCallFrequency,
        recommendedPriority,
        reason,
        codeSize,
        sourceLocation);
  }

  @Override
  public String toString() {
    return "HotFunctionInfo{"
        + "functionIndex="
        + functionIndex
        + ", functionName="
        + functionName
        + ", hotnessScore="
        + hotnessScore
        + ", estimatedCallFrequency="
        + estimatedCallFrequency
        + ", recommendedPriority="
        + recommendedPriority
        + ", reason="
        + reason
        + ", codeSize="
        + codeSize
        + ", sourceLocation="
        + sourceLocation
        + '}';
  }

  /** Builder for HotFunctionInfo. */
  public static final class Builder {
    private final int functionIndex;
    private Optional<String> functionName = Optional.empty();
    private double hotnessScore = 0.0;
    private long estimatedCallFrequency = 0;
    private CompilationPriority recommendedPriority = CompilationPriority.HIGH;
    private HotnessReason reason = HotnessReason.STATIC_ANALYSIS;
    private long codeSize = 0;
    private Optional<String> sourceLocation = Optional.empty();

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
     * Sets the hotness score.
     *
     * @param hotnessScore the hotness score (must be between 0.0 and 1.0)
     * @return this builder
     * @throws IllegalArgumentException if hotnessScore is not between 0.0 and 1.0
     */
    public Builder hotnessScore(final double hotnessScore) {
      if (hotnessScore < 0.0 || hotnessScore > 1.0) {
        throw new IllegalArgumentException("Hotness score must be between 0.0 and 1.0");
      }
      this.hotnessScore = hotnessScore;
      return this;
    }

    /**
     * Sets the estimated call frequency.
     *
     * @param estimatedCallFrequency the estimated call frequency (must not be negative)
     * @return this builder
     * @throws IllegalArgumentException if estimatedCallFrequency is negative
     */
    public Builder estimatedCallFrequency(final long estimatedCallFrequency) {
      if (estimatedCallFrequency < 0) {
        throw new IllegalArgumentException("Estimated call frequency cannot be negative");
      }
      this.estimatedCallFrequency = estimatedCallFrequency;
      return this;
    }

    /**
     * Sets the recommended compilation priority.
     *
     * @param recommendedPriority the recommended priority (must not be null)
     * @return this builder
     * @throws IllegalArgumentException if recommendedPriority is null
     */
    public Builder recommendedPriority(final CompilationPriority recommendedPriority) {
      this.recommendedPriority =
          Objects.requireNonNull(recommendedPriority, "Recommended priority cannot be null");
      return this;
    }

    /**
     * Sets the hotness reason.
     *
     * @param reason the hotness reason (must not be null)
     * @return this builder
     * @throws IllegalArgumentException if reason is null
     */
    public Builder reason(final HotnessReason reason) {
      this.reason = Objects.requireNonNull(reason, "Hotness reason cannot be null");
      return this;
    }

    /**
     * Sets the code size.
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
     * Sets the source location information.
     *
     * @param sourceLocation the source location (can be null)
     * @return this builder
     */
    public Builder sourceLocation(final String sourceLocation) {
      this.sourceLocation = Optional.ofNullable(sourceLocation);
      return this;
    }

    /**
     * Builds the HotFunctionInfo instance.
     *
     * @return a new HotFunctionInfo
     */
    public HotFunctionInfo build() {
      return new HotFunctionInfo(this);
    }
  }
}
