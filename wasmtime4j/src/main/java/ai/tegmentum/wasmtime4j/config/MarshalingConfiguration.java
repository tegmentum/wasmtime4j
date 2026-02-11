package ai.tegmentum.wasmtime4j.config;

import java.util.Objects;

/**
 * Configuration for complex parameter marshaling operations.
 *
 * <p>This class provides configuration options for customizing marshaling behavior, including
 * thresholds for strategy selection, memory management parameters, and performance optimization
 * settings.
 *
 * @since 1.0.0
 */
public final class MarshalingConfiguration {

  /** Default threshold for switching from value-based to hybrid marshaling (in bytes). */
  public static final int DEFAULT_VALUE_THRESHOLD = 512;

  /** Default threshold for switching from hybrid to memory-based marshaling (in bytes). */
  public static final int DEFAULT_HYBRID_THRESHOLD = 2048;

  /** Default maximum object graph depth. */
  public static final int DEFAULT_MAX_DEPTH = 100;

  /** Default memory alignment (in bytes). */
  public static final int DEFAULT_MEMORY_ALIGNMENT = 8;

  private final int valueMarshalingThreshold;
  private final int hybridMarshalingThreshold;
  private final int maxObjectGraphDepth;
  private final int memoryAlignment;
  private final boolean enableCircularReferenceDetection;
  private final boolean enableTypeValidation;
  private final boolean enablePerformanceOptimizations;

  private MarshalingConfiguration(final Builder builder) {
    this.valueMarshalingThreshold = builder.valueMarshalingThreshold;
    this.hybridMarshalingThreshold = builder.hybridMarshalingThreshold;
    this.maxObjectGraphDepth = builder.maxObjectGraphDepth;
    this.memoryAlignment = builder.memoryAlignment;
    this.enableCircularReferenceDetection = builder.enableCircularReferenceDetection;
    this.enableTypeValidation = builder.enableTypeValidation;
    this.enablePerformanceOptimizations = builder.enablePerformanceOptimizations;
  }

  /**
   * Gets the threshold for switching from value-based to hybrid marshaling.
   *
   * @return the threshold in bytes
   */
  public int getValueMarshalingThreshold() {
    return valueMarshalingThreshold;
  }

  /**
   * Gets the threshold for switching from hybrid to memory-based marshaling.
   *
   * @return the threshold in bytes
   */
  public int getHybridMarshalingThreshold() {
    return hybridMarshalingThreshold;
  }

  /**
   * Gets the maximum object graph depth for circular reference detection.
   *
   * @return the maximum depth
   */
  public int getMaxObjectGraphDepth() {
    return maxObjectGraphDepth;
  }

  /**
   * Gets the memory alignment requirement.
   *
   * @return the alignment in bytes
   */
  public int getMemoryAlignment() {
    return memoryAlignment;
  }

  /**
   * Checks if circular reference detection is enabled.
   *
   * @return true if enabled
   */
  public boolean isCircularReferenceDetectionEnabled() {
    return enableCircularReferenceDetection;
  }

  /**
   * Checks if type validation is enabled.
   *
   * @return true if enabled
   */
  public boolean isTypeValidationEnabled() {
    return enableTypeValidation;
  }

  /**
   * Checks if performance optimizations are enabled.
   *
   * @return true if enabled
   */
  public boolean arePerformanceOptimizationsEnabled() {
    return enablePerformanceOptimizations;
  }

  /**
   * Creates a configuration with default settings.
   *
   * @return a new default configuration
   */
  public static MarshalingConfiguration defaultConfiguration() {
    return builder().build();
  }

  /**
   * Creates a configuration optimized for performance.
   *
   * @return a new performance-optimized configuration
   */
  public static MarshalingConfiguration performanceOptimized() {
    return builder()
        .withValueMarshalingThreshold(1024)
        .withHybridMarshalingThreshold(4096)
        .withPerformanceOptimizations(true)
        .withCircularReferenceDetection(false) // Disable for performance
        .build();
  }

  /**
   * Creates a configuration optimized for safety.
   *
   * @return a new safety-optimized configuration
   */
  public static MarshalingConfiguration safetyOptimized() {
    return builder()
        .withValueMarshalingThreshold(256)
        .withHybridMarshalingThreshold(1024)
        .withMaxObjectGraphDepth(50)
        .withCircularReferenceDetection(true)
        .withTypeValidation(true)
        .build();
  }

  /**
   * Creates a new builder for marshaling configuration.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder class for creating MarshalingConfiguration instances. */
  public static final class Builder {
    private int valueMarshalingThreshold = DEFAULT_VALUE_THRESHOLD;
    private int hybridMarshalingThreshold = DEFAULT_HYBRID_THRESHOLD;
    private int maxObjectGraphDepth = DEFAULT_MAX_DEPTH;
    private int memoryAlignment = DEFAULT_MEMORY_ALIGNMENT;
    private boolean enableCircularReferenceDetection = true;
    private boolean enableTypeValidation = true;
    private boolean enablePerformanceOptimizations = false;

    private Builder() {}

    /**
     * Sets the threshold for switching from value-based to hybrid marshaling.
     *
     * @param threshold the threshold in bytes (must be positive)
     * @return this builder
     * @throws IllegalArgumentException if threshold is not positive
     */
    public Builder withValueMarshalingThreshold(final int threshold) {
      if (threshold <= 0) {
        throw new IllegalArgumentException("Value marshaling threshold must be positive");
      }
      this.valueMarshalingThreshold = threshold;
      return this;
    }

    /**
     * Sets the threshold for switching from hybrid to memory-based marshaling.
     *
     * @param threshold the threshold in bytes (must be positive)
     * @return this builder
     * @throws IllegalArgumentException if threshold is not positive
     */
    public Builder withHybridMarshalingThreshold(final int threshold) {
      if (threshold <= 0) {
        throw new IllegalArgumentException("Hybrid marshaling threshold must be positive");
      }
      this.hybridMarshalingThreshold = threshold;
      return this;
    }

    /**
     * Sets the maximum object graph depth.
     *
     * @param depth the maximum depth (must be positive)
     * @return this builder
     * @throws IllegalArgumentException if depth is not positive
     */
    public Builder withMaxObjectGraphDepth(final int depth) {
      if (depth <= 0) {
        throw new IllegalArgumentException("Max object graph depth must be positive");
      }
      this.maxObjectGraphDepth = depth;
      return this;
    }

    /**
     * Sets the memory alignment requirement.
     *
     * @param alignment the alignment in bytes (must be power of 2)
     * @return this builder
     * @throws IllegalArgumentException if alignment is not a power of 2
     */
    public Builder withMemoryAlignment(final int alignment) {
      if (alignment <= 0 || (alignment & (alignment - 1)) != 0) {
        throw new IllegalArgumentException("Memory alignment must be a positive power of 2");
      }
      this.memoryAlignment = alignment;
      return this;
    }

    /**
     * Sets whether to enable circular reference detection.
     *
     * @param enable true to enable circular reference detection
     * @return this builder
     */
    public Builder withCircularReferenceDetection(final boolean enable) {
      this.enableCircularReferenceDetection = enable;
      return this;
    }

    /**
     * Sets whether to enable type validation.
     *
     * @param enable true to enable type validation
     * @return this builder
     */
    public Builder withTypeValidation(final boolean enable) {
      this.enableTypeValidation = enable;
      return this;
    }

    /**
     * Sets whether to enable performance optimizations.
     *
     * @param enable true to enable performance optimizations
     * @return this builder
     */
    public Builder withPerformanceOptimizations(final boolean enable) {
      this.enablePerformanceOptimizations = enable;
      return this;
    }

    /**
     * Builds the MarshalingConfiguration instance.
     *
     * @return a new MarshalingConfiguration instance
     * @throws IllegalStateException if configuration is invalid
     */
    public MarshalingConfiguration build() {
      // Validate configuration consistency
      if (hybridMarshalingThreshold <= valueMarshalingThreshold) {
        throw new IllegalStateException(
            "Hybrid marshaling threshold ("
                + hybridMarshalingThreshold
                + ") must be greater than value marshaling threshold ("
                + valueMarshalingThreshold
                + ")");
      }

      return new MarshalingConfiguration(this);
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final MarshalingConfiguration other = (MarshalingConfiguration) obj;
    return valueMarshalingThreshold == other.valueMarshalingThreshold
        && hybridMarshalingThreshold == other.hybridMarshalingThreshold
        && maxObjectGraphDepth == other.maxObjectGraphDepth
        && memoryAlignment == other.memoryAlignment
        && enableCircularReferenceDetection == other.enableCircularReferenceDetection
        && enableTypeValidation == other.enableTypeValidation
        && enablePerformanceOptimizations == other.enablePerformanceOptimizations;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        valueMarshalingThreshold,
        hybridMarshalingThreshold,
        maxObjectGraphDepth,
        memoryAlignment,
        enableCircularReferenceDetection,
        enableTypeValidation,
        enablePerformanceOptimizations);
  }

  @Override
  public String toString() {
    return String.format(
        "MarshalingConfiguration{valueThreshold=%d, hybridThreshold=%d, maxDepth=%d, "
            + "alignment=%d, circularRefDetection=%s, typeValidation=%s, performanceOpt=%s}",
        valueMarshalingThreshold,
        hybridMarshalingThreshold,
        maxObjectGraphDepth,
        memoryAlignment,
        enableCircularReferenceDetection,
        enableTypeValidation,
        enablePerformanceOptimizations);
  }
}
