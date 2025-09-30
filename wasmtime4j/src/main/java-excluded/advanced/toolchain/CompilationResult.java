package ai.tegmentum.wasmtime4j.toolchain;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of a WebAssembly compilation operation.
 *
 * <p>Contains the outcome of compilation including success status,
 * output files, compilation metrics, and any errors or warnings.
 *
 * @since 1.0.0
 */
public final class CompilationResult {

  private final boolean successful;
  private final Optional<Path> outputFile;
  private final List<String> errors;
  private final List<String> warnings;
  private final Duration compilationTime;
  private final Instant timestamp;
  private final CompilationMetrics metrics;
  private final Optional<String> compilerOutput;

  private CompilationResult(final boolean successful,
                            final Path outputFile,
                            final List<String> errors,
                            final List<String> warnings,
                            final Duration compilationTime,
                            final Instant timestamp,
                            final CompilationMetrics metrics,
                            final String compilerOutput) {
    this.successful = successful;
    this.outputFile = Optional.ofNullable(outputFile);
    this.errors = Collections.unmodifiableList(Objects.requireNonNull(errors));
    this.warnings = Collections.unmodifiableList(Objects.requireNonNull(warnings));
    this.compilationTime = Objects.requireNonNull(compilationTime);
    this.timestamp = Objects.requireNonNull(timestamp);
    this.metrics = metrics;
    this.compilerOutput = Optional.ofNullable(compilerOutput);
  }

  /**
   * Creates a successful compilation result.
   *
   * @param outputFile the compiled output file
   * @param compilationTime the time taken for compilation
   * @param warnings any warnings generated
   * @return successful result
   */
  public static CompilationResult success(final Path outputFile,
                                          final Duration compilationTime,
                                          final List<String> warnings) {
    return new CompilationResult(
        true,
        outputFile,
        Collections.emptyList(),
        warnings,
        compilationTime,
        Instant.now(),
        null,
        null
    );
  }

  /**
   * Creates a successful compilation result with metrics.
   *
   * @param outputFile the compiled output file
   * @param compilationTime the time taken for compilation
   * @param warnings any warnings generated
   * @param metrics compilation metrics
   * @return successful result with metrics
   */
  public static CompilationResult success(final Path outputFile,
                                          final Duration compilationTime,
                                          final List<String> warnings,
                                          final CompilationMetrics metrics) {
    return new CompilationResult(
        true,
        outputFile,
        Collections.emptyList(),
        warnings,
        compilationTime,
        Instant.now(),
        metrics,
        null
    );
  }

  /**
   * Creates a failed compilation result.
   *
   * @param errors the compilation errors
   * @param compilationTime the time taken before failure
   * @return failed result
   */
  public static CompilationResult failure(final List<String> errors,
                                          final Duration compilationTime) {
    return new CompilationResult(
        false,
        null,
        errors,
        Collections.emptyList(),
        compilationTime,
        Instant.now(),
        null,
        null
    );
  }

  /**
   * Creates a failed compilation result with warnings.
   *
   * @param errors the compilation errors
   * @param warnings any warnings generated
   * @param compilationTime the time taken before failure
   * @return failed result
   */
  public static CompilationResult failure(final List<String> errors,
                                          final List<String> warnings,
                                          final Duration compilationTime) {
    return new CompilationResult(
        false,
        null,
        errors,
        warnings,
        compilationTime,
        Instant.now(),
        null,
        null
    );
  }

  /**
   * Creates a failed compilation result with full details.
   *
   * @param errors the compilation errors
   * @param warnings any warnings generated
   * @param compilationTime the time taken before failure
   * @param compilerOutput the full compiler output
   * @return failed result with full details
   */
  public static CompilationResult failure(final List<String> errors,
                                          final List<String> warnings,
                                          final Duration compilationTime,
                                          final String compilerOutput) {
    return new CompilationResult(
        false,
        null,
        errors,
        warnings,
        compilationTime,
        Instant.now(),
        null,
        compilerOutput
    );
  }

  /**
   * Checks if the compilation was successful.
   *
   * @return true if successful, false otherwise
   */
  public boolean isSuccessful() {
    return successful;
  }

  /**
   * Gets the output file path.
   *
   * @return output file, or empty if compilation failed
   */
  public Optional<Path> getOutputFile() {
    return outputFile;
  }

  /**
   * Gets the compilation errors.
   *
   * @return list of error messages
   */
  public List<String> getErrors() {
    return errors;
  }

  /**
   * Gets the compilation warnings.
   *
   * @return list of warning messages
   */
  public List<String> getWarnings() {
    return warnings;
  }

  /**
   * Gets the compilation time.
   *
   * @return duration of compilation
   */
  public Duration getCompilationTime() {
    return compilationTime;
  }

  /**
   * Gets the compilation timestamp.
   *
   * @return when compilation completed
   */
  public Instant getTimestamp() {
    return timestamp;
  }

  /**
   * Gets the compilation metrics.
   *
   * @return compilation metrics, or empty if not available
   */
  public Optional<CompilationMetrics> getMetrics() {
    return Optional.ofNullable(metrics);
  }

  /**
   * Gets the full compiler output.
   *
   * @return compiler output, or empty if not captured
   */
  public Optional<String> getCompilerOutput() {
    return compilerOutput;
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
   * Gets a summary of the compilation result.
   *
   * @return formatted summary string
   */
  public String getSummary() {
    if (successful) {
      final String warningText = warnings.isEmpty() ? "" : String.format(" (%d warnings)", warnings.size());
      return String.format("Compilation successful in %s%s", compilationTime, warningText);
    } else {
      return String.format("Compilation failed with %d error(s) after %s", errors.size(), compilationTime);
    }
  }

  @Override
  public String toString() {
    return String.format("CompilationResult{successful=%s, errors=%d, warnings=%d, time=%s}",
        successful, errors.size(), warnings.size(), compilationTime);
  }

  /**
   * Compilation metrics data.
   */
  public static final class CompilationMetrics {
    private final long sourceLineCount;
    private final long outputSizeBytes;
    private final long peakMemoryUsageBytes;
    private final int optimizationPasses;

    public CompilationMetrics(final long sourceLineCount,
                              final long outputSizeBytes,
                              final long peakMemoryUsageBytes,
                              final int optimizationPasses) {
      this.sourceLineCount = sourceLineCount;
      this.outputSizeBytes = outputSizeBytes;
      this.peakMemoryUsageBytes = peakMemoryUsageBytes;
      this.optimizationPasses = optimizationPasses;
    }

    public long getSourceLineCount() { return sourceLineCount; }
    public long getOutputSizeBytes() { return outputSizeBytes; }
    public long getPeakMemoryUsageBytes() { return peakMemoryUsageBytes; }
    public int getOptimizationPasses() { return optimizationPasses; }

    @Override
    public String toString() {
      return String.format("CompilationMetrics{lines=%d, size=%d bytes, memory=%d bytes, passes=%d}",
          sourceLineCount, outputSizeBytes, peakMemoryUsageBytes, optimizationPasses);
    }
  }
}