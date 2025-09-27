package ai.tegmentum.wasmtime4j.toolchain;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of a WebAssembly optimization operation.
 *
 * <p>Contains the outcome of optimization including performance improvements,
 * size changes, and any optimization-specific metrics.
 *
 * @since 1.0.0
 */
public final class OptimizationResult {

  private final boolean successful;
  private final Optional<Path> outputFile;
  private final List<String> errors;
  private final List<String> warnings;
  private final Duration optimizationTime;
  private final Instant timestamp;
  private final OptimizationMetrics metrics;
  private final List<String> appliedPasses;

  private OptimizationResult(final boolean successful,
                             final Path outputFile,
                             final List<String> errors,
                             final List<String> warnings,
                             final Duration optimizationTime,
                             final Instant timestamp,
                             final OptimizationMetrics metrics,
                             final List<String> appliedPasses) {
    this.successful = successful;
    this.outputFile = Optional.ofNullable(outputFile);
    this.errors = Collections.unmodifiableList(Objects.requireNonNull(errors));
    this.warnings = Collections.unmodifiableList(Objects.requireNonNull(warnings));
    this.optimizationTime = Objects.requireNonNull(optimizationTime);
    this.timestamp = Objects.requireNonNull(timestamp);
    this.metrics = metrics;
    this.appliedPasses = Collections.unmodifiableList(Objects.requireNonNull(appliedPasses));
  }

  /**
   * Creates a successful optimization result.
   *
   * @param outputFile the optimized output file
   * @param optimizationTime the time taken for optimization
   * @param metrics optimization metrics
   * @param appliedPasses list of optimization passes that were applied
   * @return successful result
   */
  public static OptimizationResult success(final Path outputFile,
                                           final Duration optimizationTime,
                                           final OptimizationMetrics metrics,
                                           final List<String> appliedPasses) {
    return new OptimizationResult(
        true,
        outputFile,
        Collections.emptyList(),
        Collections.emptyList(),
        optimizationTime,
        Instant.now(),
        metrics,
        appliedPasses
    );
  }

  /**
   * Creates a successful optimization result with warnings.
   *
   * @param outputFile the optimized output file
   * @param optimizationTime the time taken for optimization
   * @param warnings any warnings generated
   * @param metrics optimization metrics
   * @param appliedPasses list of optimization passes that were applied
   * @return successful result with warnings
   */
  public static OptimizationResult success(final Path outputFile,
                                           final Duration optimizationTime,
                                           final List<String> warnings,
                                           final OptimizationMetrics metrics,
                                           final List<String> appliedPasses) {
    return new OptimizationResult(
        true,
        outputFile,
        Collections.emptyList(),
        warnings,
        optimizationTime,
        Instant.now(),
        metrics,
        appliedPasses
    );
  }

  /**
   * Creates a failed optimization result.
   *
   * @param errors the optimization errors
   * @param optimizationTime the time taken before failure
   * @return failed result
   */
  public static OptimizationResult failure(final List<String> errors,
                                           final Duration optimizationTime) {
    return new OptimizationResult(
        false,
        null,
        errors,
        Collections.emptyList(),
        optimizationTime,
        Instant.now(),
        null,
        Collections.emptyList()
    );
  }

  /**
   * Creates a failed optimization result with warnings.
   *
   * @param errors the optimization errors
   * @param warnings any warnings generated
   * @param optimizationTime the time taken before failure
   * @return failed result with warnings
   */
  public static OptimizationResult failure(final List<String> errors,
                                           final List<String> warnings,
                                           final Duration optimizationTime) {
    return new OptimizationResult(
        false,
        null,
        errors,
        warnings,
        optimizationTime,
        Instant.now(),
        null,
        Collections.emptyList()
    );
  }

  /**
   * Checks if the optimization was successful.
   *
   * @return true if successful, false otherwise
   */
  public boolean isSuccessful() {
    return successful;
  }

  /**
   * Gets the optimized output file.
   *
   * @return output file, or empty if optimization failed
   */
  public Optional<Path> getOutputFile() {
    return outputFile;
  }

  /**
   * Gets the optimization errors.
   *
   * @return list of error messages
   */
  public List<String> getErrors() {
    return errors;
  }

  /**
   * Gets the optimization warnings.
   *
   * @return list of warning messages
   */
  public List<String> getWarnings() {
    return warnings;
  }

  /**
   * Gets the optimization time.
   *
   * @return duration of optimization
   */
  public Duration getOptimizationTime() {
    return optimizationTime;
  }

  /**
   * Gets the optimization timestamp.
   *
   * @return when optimization completed
   */
  public Instant getTimestamp() {
    return timestamp;
  }

  /**
   * Gets the optimization metrics.
   *
   * @return optimization metrics, or empty if not available
   */
  public Optional<OptimizationMetrics> getMetrics() {
    return Optional.ofNullable(metrics);
  }

  /**
   * Gets the list of optimization passes that were applied.
   *
   * @return list of applied optimization pass names
   */
  public List<String> getAppliedPasses() {
    return appliedPasses;
  }

  /**
   * Checks if there were any errors.
   *
   * @return true if errors occurred
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  /**
   * Checks if there were any warnings.
   *
   * @return true if warnings occurred
   */
  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  /**
   * Gets a summary of the optimization result.
   *
   * @return formatted summary string
   */
  public String getSummary() {
    if (successful) {
      final StringBuilder sb = new StringBuilder();
      sb.append("Optimization successful in ").append(optimizationTime);

      if (metrics != null) {
        final double sizeReduction = metrics.getSizeReductionPercentage();
        if (sizeReduction > 0) {
          sb.append(String.format(" (%.1f%% size reduction)", sizeReduction));
        }
      }

      if (!warnings.isEmpty()) {
        sb.append(String.format(" (%d warnings)", warnings.size()));
      }

      return sb.toString();
    } else {
      return String.format("Optimization failed with %d error(s) after %s", errors.size(), optimizationTime);
    }
  }

  @Override
  public String toString() {
    return String.format("OptimizationResult{successful=%s, errors=%d, warnings=%d, time=%s, passes=%d}",
        successful, errors.size(), warnings.size(), optimizationTime, appliedPasses.size());
  }

  /**
   * Optimization metrics data.
   */
  public static final class OptimizationMetrics {
    private final long originalSizeBytes;
    private final long optimizedSizeBytes;
    private final double estimatedPerformanceGain;
    private final int passesApplied;
    private final long peakMemoryUsageBytes;

    public OptimizationMetrics(final long originalSizeBytes,
                               final long optimizedSizeBytes,
                               final double estimatedPerformanceGain,
                               final int passesApplied,
                               final long peakMemoryUsageBytes) {
      this.originalSizeBytes = originalSizeBytes;
      this.optimizedSizeBytes = optimizedSizeBytes;
      this.estimatedPerformanceGain = estimatedPerformanceGain;
      this.passesApplied = passesApplied;
      this.peakMemoryUsageBytes = peakMemoryUsageBytes;
    }

    public long getOriginalSizeBytes() { return originalSizeBytes; }
    public long getOptimizedSizeBytes() { return optimizedSizeBytes; }
    public double getEstimatedPerformanceGain() { return estimatedPerformanceGain; }
    public int getPassesApplied() { return passesApplied; }
    public long getPeakMemoryUsageBytes() { return peakMemoryUsageBytes; }

    /**
     * Gets the size reduction as a percentage.
     *
     * @return percentage reduction in size (0-100)
     */
    public double getSizeReductionPercentage() {
      if (originalSizeBytes == 0) return 0.0;
      return ((double) (originalSizeBytes - optimizedSizeBytes) / originalSizeBytes) * 100.0;
    }

    /**
     * Gets the absolute size reduction in bytes.
     *
     * @return bytes saved by optimization
     */
    public long getSizeReductionBytes() {
      return originalSizeBytes - optimizedSizeBytes;
    }

    @Override
    public String toString() {
      return String.format("OptimizationMetrics{original=%d bytes, optimized=%d bytes, reduction=%.1f%%, performance=%.2fx, passes=%d}",
          originalSizeBytes, optimizedSizeBytes, getSizeReductionPercentage(), estimatedPerformanceGain, passesApplied);
    }
  }
}