package ai.tegmentum.wasmtime4j.webassembly;

import ai.tegmentum.wasmtime4j.AdvancedWasmFeature;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration class for advanced WebAssembly feature testing, providing fine-grained control over
 * SIMD, Threading, and Exception handling test execution parameters.
 *
 * <p>This configuration enables targeted testing of advanced features with appropriate timeouts,
 * feature selection, and performance validation settings.
 */
public final class AdvancedFeatureTestConfig {
  /** Default timeout for advanced feature tests (longer than basic tests). */
  public static final Duration DEFAULT_ADVANCED_TIMEOUT = Duration.ofMinutes(2);

  /** Default timeout for SIMD performance tests. */
  public static final Duration DEFAULT_SIMD_TIMEOUT = Duration.ofSeconds(45);

  /** Default timeout for threading tests with synchronization. */
  public static final Duration DEFAULT_THREADING_TIMEOUT = Duration.ofMinutes(3);

  /** Default timeout for exception handling tests. */
  public static final Duration DEFAULT_EXCEPTION_TIMEOUT = Duration.ofSeconds(30);

  private final Set<AdvancedWasmFeature> enabledFeatures;
  private final boolean crossRuntimeValidation;
  private final boolean performanceBenchmarking;
  private final Duration testTimeout;
  private final int maxRetryAttempts;
  private final boolean skipKnownFailures;
  private final boolean verboseLogging;

  private AdvancedFeatureTestConfig(final Builder builder) {
    this.enabledFeatures = Collections.unmodifiableSet(EnumSet.copyOf(builder.enabledFeatures));
    this.crossRuntimeValidation = builder.crossRuntimeValidation;
    this.performanceBenchmarking = builder.performanceBenchmarking;
    this.testTimeout = builder.testTimeout;
    this.maxRetryAttempts = builder.maxRetryAttempts;
    this.skipKnownFailures = builder.skipKnownFailures;
    this.verboseLogging = builder.verboseLogging;
  }

  /**
   * Gets the set of enabled advanced features for testing.
   *
   * @return immutable set of enabled features
   */
  public Set<AdvancedWasmFeature> getEnabledFeatures() {
    return enabledFeatures;
  }

  /**
   * Checks if cross-runtime validation is enabled (JNI vs Panama consistency).
   *
   * @return true if cross-runtime validation is enabled
   */
  public boolean isCrossRuntimeValidationEnabled() {
    return crossRuntimeValidation;
  }

  /**
   * Checks if performance benchmarking is enabled for advanced features.
   *
   * @return true if performance benchmarking is enabled
   */
  public boolean isPerformanceBenchmarkingEnabled() {
    return performanceBenchmarking;
  }

  /**
   * Gets the test timeout duration.
   *
   * @return the test timeout duration
   */
  public Duration getTestTimeout() {
    return testTimeout;
  }

  /**
   * Gets the maximum number of retry attempts for flaky tests.
   *
   * @return the maximum retry attempts
   */
  public int getMaxRetryAttempts() {
    return maxRetryAttempts;
  }

  /**
   * Checks if known failing tests should be skipped.
   *
   * @return true if known failures should be skipped
   */
  public boolean shouldSkipKnownFailures() {
    return skipKnownFailures;
  }

  /**
   * Checks if verbose logging is enabled for detailed test output.
   *
   * @return true if verbose logging is enabled
   */
  public boolean isVerboseLoggingEnabled() {
    return verboseLogging;
  }

  /**
   * Checks if a specific advanced feature is enabled for testing.
   *
   * @param feature the feature to check
   * @return true if the feature is enabled
   */
  public boolean isFeatureEnabled(final AdvancedWasmFeature feature) {
    return enabledFeatures.contains(feature);
  }

  /**
   * Checks if SIMD features are enabled (any SIMD-related feature).
   *
   * @return true if SIMD testing is enabled
   */
  public boolean isSimdEnabled() {
    return enabledFeatures.contains(AdvancedWasmFeature.SIMD_ARITHMETIC)
        || enabledFeatures.contains(AdvancedWasmFeature.SIMD_MEMORY)
        || enabledFeatures.contains(AdvancedWasmFeature.SIMD_MANIPULATION)
        || enabledFeatures.contains(AdvancedWasmFeature.SIMD_PERFORMANCE);
  }

  /**
   * Checks if Threading features are enabled (any threading-related feature).
   *
   * @return true if threading testing is enabled
   */
  public boolean isThreadingEnabled() {
    return enabledFeatures.contains(AdvancedWasmFeature.ATOMIC_OPERATIONS)
        || enabledFeatures.contains(AdvancedWasmFeature.ATOMIC_CAS)
        || enabledFeatures.contains(AdvancedWasmFeature.SHARED_MEMORY)
        || enabledFeatures.contains(AdvancedWasmFeature.MEMORY_ORDERING)
        || enabledFeatures.contains(AdvancedWasmFeature.THREAD_SAFETY)
        || enabledFeatures.contains(AdvancedWasmFeature.ATOMIC_PERFORMANCE);
  }

  /**
   * Checks if Exception handling features are enabled (any exception-related feature).
   *
   * @return true if exception testing is enabled
   */
  public boolean isExceptionHandlingEnabled() {
    return enabledFeatures.contains(AdvancedWasmFeature.EXCEPTIONS)
        || enabledFeatures.contains(AdvancedWasmFeature.CROSS_MODULE_EXCEPTIONS)
        || enabledFeatures.contains(AdvancedWasmFeature.NESTED_EXCEPTIONS)
        || enabledFeatures.contains(AdvancedWasmFeature.EXCEPTION_TYPES)
        || enabledFeatures.contains(AdvancedWasmFeature.EXCEPTION_PERFORMANCE);
  }

  /**
   * Creates a default configuration for all advanced features.
   *
   * @return default advanced feature test configuration
   */
  public static AdvancedFeatureTestConfig defaultConfig() {
    return builder().enableAllFeatures().build();
  }

  /**
   * Creates a SIMD-only configuration for focused SIMD testing.
   *
   * @return SIMD-focused test configuration
   */
  public static AdvancedFeatureTestConfig simdOnlyConfig() {
    return builder()
        .enableFeatures(
            AdvancedWasmFeature.SIMD_ARITHMETIC,
            AdvancedWasmFeature.SIMD_MEMORY,
            AdvancedWasmFeature.SIMD_MANIPULATION,
            AdvancedWasmFeature.SIMD_PERFORMANCE)
        .timeout(DEFAULT_SIMD_TIMEOUT)
        .build();
  }

  /**
   * Creates a Threading-only configuration for focused threading testing.
   *
   * @return Threading-focused test configuration
   */
  public static AdvancedFeatureTestConfig threadingOnlyConfig() {
    return builder()
        .enableFeatures(
            AdvancedWasmFeature.ATOMIC_OPERATIONS,
            AdvancedWasmFeature.ATOMIC_CAS,
            AdvancedWasmFeature.SHARED_MEMORY,
            AdvancedWasmFeature.MEMORY_ORDERING,
            AdvancedWasmFeature.THREAD_SAFETY,
            AdvancedWasmFeature.ATOMIC_PERFORMANCE)
        .timeout(DEFAULT_THREADING_TIMEOUT)
        .build();
  }

  /**
   * Creates an Exception-only configuration for focused exception testing.
   *
   * @return Exception-focused test configuration
   */
  public static AdvancedFeatureTestConfig exceptionOnlyConfig() {
    return builder()
        .enableFeatures(
            AdvancedWasmFeature.EXCEPTIONS,
            AdvancedWasmFeature.CROSS_MODULE_EXCEPTIONS,
            AdvancedWasmFeature.NESTED_EXCEPTIONS,
            AdvancedWasmFeature.EXCEPTION_TYPES,
            AdvancedWasmFeature.EXCEPTION_PERFORMANCE)
        .timeout(DEFAULT_EXCEPTION_TIMEOUT)
        .build();
  }

  /**
   * Creates a new builder for advanced feature test configuration.
   *
   * @return new configuration builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder class for creating AdvancedFeatureTestConfig instances. */
  public static final class Builder {
    private final Set<AdvancedWasmFeature> enabledFeatures =
        EnumSet.noneOf(AdvancedWasmFeature.class);
    private boolean crossRuntimeValidation = true;
    private boolean performanceBenchmarking = false;
    private Duration testTimeout = DEFAULT_ADVANCED_TIMEOUT;
    private int maxRetryAttempts = 3;
    private boolean skipKnownFailures = false;
    private boolean verboseLogging = false;

    private Builder() {
      // Package-private constructor
    }

    /**
     * Enables all advanced features for comprehensive testing.
     *
     * @return this builder
     */
    public Builder enableAllFeatures() {
      this.enabledFeatures.addAll(Arrays.asList(AdvancedWasmFeature.values()));
      return this;
    }

    /**
     * Enables specific advanced features.
     *
     * @param features the features to enable
     * @return this builder
     */
    public Builder enableFeatures(final AdvancedWasmFeature... features) {
      Objects.requireNonNull(features, "features cannot be null");
      this.enabledFeatures.addAll(Arrays.asList(features));
      return this;
    }

    /**
     * Enables cross-runtime validation.
     *
     * @param enabled true to enable cross-runtime validation
     * @return this builder
     */
    public Builder crossRuntimeValidation(final boolean enabled) {
      this.crossRuntimeValidation = enabled;
      return this;
    }

    /**
     * Enables performance benchmarking.
     *
     * @param enabled true to enable performance benchmarking
     * @return this builder
     */
    public Builder performanceBenchmarking(final boolean enabled) {
      this.performanceBenchmarking = enabled;
      return this;
    }

    /**
     * Sets the test timeout duration.
     *
     * @param timeout the test timeout duration
     * @return this builder
     */
    public Builder timeout(final Duration timeout) {
      this.testTimeout = Objects.requireNonNull(timeout, "timeout cannot be null");
      return this;
    }

    /**
     * Sets the maximum retry attempts for flaky tests.
     *
     * @param attempts the maximum retry attempts
     * @return this builder
     */
    public Builder maxRetryAttempts(final int attempts) {
      if (attempts < 0) {
        throw new IllegalArgumentException("maxRetryAttempts cannot be negative");
      }
      this.maxRetryAttempts = attempts;
      return this;
    }

    /**
     * Configures whether to skip known failing tests.
     *
     * @param skip true to skip known failures
     * @return this builder
     */
    public Builder skipKnownFailures(final boolean skip) {
      this.skipKnownFailures = skip;
      return this;
    }

    /**
     * Enables verbose logging for detailed test output.
     *
     * @param enabled true to enable verbose logging
     * @return this builder
     */
    public Builder verboseLogging(final boolean enabled) {
      this.verboseLogging = enabled;
      return this;
    }

    /**
     * Builds the advanced feature test configuration.
     *
     * @return the configured AdvancedFeatureTestConfig
     */
    public AdvancedFeatureTestConfig build() {
      if (enabledFeatures.isEmpty()) {
        throw new IllegalStateException("At least one advanced feature must be enabled");
      }
      return new AdvancedFeatureTestConfig(this);
    }
  }

  @Override
  public String toString() {
    return "AdvancedFeatureTestConfig{"
        + "enabledFeatures="
        + enabledFeatures.size()
        + ", crossRuntimeValidation="
        + crossRuntimeValidation
        + ", performanceBenchmarking="
        + performanceBenchmarking
        + ", testTimeout="
        + testTimeout.toSeconds()
        + "s"
        + ", maxRetryAttempts="
        + maxRetryAttempts
        + ", skipKnownFailures="
        + skipKnownFailures
        + ", verboseLogging="
        + verboseLogging
        + '}';
  }
}
