package ai.tegmentum.wasmtime4j.toolchain;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of a WebAssembly linking operation.
 *
 * <p>Contains the outcome of linking multiple object files into a single
 * WebAssembly module, including success status, output file, and any
 * linking-specific information.
 *
 * @since 1.0.0
 */
public final class LinkingResult {

  private final boolean successful;
  private final Optional<Path> linkedModule;
  private final List<String> linkingErrors;
  private final List<String> linkingWarnings;
  private final Duration linkingTime;
  private final Instant timestamp;
  private final LinkingMetrics metrics;
  private final Optional<String> linkerOutput;

  private LinkingResult(final boolean successful,
                        final Path linkedModule,
                        final List<String> linkingErrors,
                        final List<String> linkingWarnings,
                        final Duration linkingTime,
                        final Instant timestamp,
                        final LinkingMetrics metrics,
                        final String linkerOutput) {
    this.successful = successful;
    this.linkedModule = Optional.ofNullable(linkedModule);
    this.linkingErrors = List.copyOf(linkingErrors);
    this.linkingWarnings = List.copyOf(linkingWarnings);
    this.linkingTime = Objects.requireNonNull(linkingTime);
    this.timestamp = Objects.requireNonNull(timestamp);
    this.metrics = metrics;
    this.linkerOutput = Optional.ofNullable(linkerOutput);
  }

  /**
   * Creates a successful linking result.
   *
   * @param linkedModule the linked output module
   * @param linkingTime the time taken for linking
   * @param warnings any warnings generated
   * @return successful result
   */
  public static LinkingResult success(final Path linkedModule,
                                      final Duration linkingTime,
                                      final List<String> warnings) {
    return new LinkingResult(true, linkedModule, List.of(), warnings,
                             linkingTime, Instant.now(), null, null);
  }

  /**
   * Creates a successful linking result with metrics.
   *
   * @param linkedModule the linked output module
   * @param linkingTime the time taken for linking
   * @param warnings any warnings generated
   * @param metrics linking metrics
   * @return successful result with metrics
   */
  public static LinkingResult success(final Path linkedModule,
                                      final Duration linkingTime,
                                      final List<String> warnings,
                                      final LinkingMetrics metrics) {
    return new LinkingResult(true, linkedModule, List.of(), warnings,
                             linkingTime, Instant.now(), metrics, null);
  }

  /**
   * Creates a failed linking result.
   *
   * @param linkingErrors the linking errors
   * @param linkingTime the time taken before failure
   * @return failed result
   */
  public static LinkingResult failure(final List<String> linkingErrors,
                                      final Duration linkingTime) {
    return new LinkingResult(false, null, linkingErrors, List.of(),
                             linkingTime, Instant.now(), null, null);
  }

  /**
   * Creates a failed linking result with full details.
   *
   * @param linkingErrors the linking errors
   * @param warnings any warnings generated
   * @param linkingTime the time taken before failure
   * @param linkerOutput the full linker output
   * @return failed result with full details
   */
  public static LinkingResult failure(final List<String> linkingErrors,
                                      final List<String> warnings,
                                      final Duration linkingTime,
                                      final String linkerOutput) {
    return new LinkingResult(false, null, linkingErrors, warnings,
                             linkingTime, Instant.now(), null, linkerOutput);
  }

  /**
   * Checks if the linking was successful.
   *
   * @return true if successful, false otherwise
   */
  public boolean isSuccessful() {
    return successful;
  }

  /**
   * Gets the linked module path.
   *
   * @return linked module, or empty if linking failed
   */
  public Optional<Path> getLinkedModule() {
    return linkedModule;
  }

  /**
   * Gets the linking errors.
   *
   * @return list of error messages
   */
  public List<String> getLinkingErrors() {
    return linkingErrors;
  }

  /**
   * Gets the linking warnings.
   *
   * @return list of warning messages
   */
  public List<String> getLinkingWarnings() {
    return linkingWarnings;
  }

  /**
   * Gets the linking time.
   *
   * @return duration of linking
   */
  public Duration getLinkingTime() {
    return linkingTime;
  }

  /**
   * Gets the linking timestamp.
   *
   * @return when linking completed
   */
  public Instant getTimestamp() {
    return timestamp;
  }

  /**
   * Gets the linking metrics.
   *
   * @return linking metrics, or empty if not available
   */
  public Optional<LinkingMetrics> getMetrics() {
    return Optional.ofNullable(metrics);
  }

  /**
   * Gets the full linker output.
   *
   * @return linker output, or empty if not captured
   */
  public Optional<String> getLinkerOutput() {
    return linkerOutput;
  }

  /**
   * Checks if there were any errors.
   *
   * @return true if errors occurred
   */
  public boolean hasErrors() {
    return !linkingErrors.isEmpty();
  }

  /**
   * Checks if there were any warnings.
   *
   * @return true if warnings occurred
   */
  public boolean hasWarnings() {
    return !linkingWarnings.isEmpty();
  }

  /**
   * Gets a summary of the linking result.
   *
   * @return formatted summary string
   */
  public String getSummary() {
    if (successful) {
      final String warningText = linkingWarnings.isEmpty() ? "" : String.format(" (%d warnings)", linkingWarnings.size());
      return String.format("Linking successful in %s%s", linkingTime, warningText);
    } else {
      return String.format("Linking failed with %d error(s) after %s", linkingErrors.size(), linkingTime);
    }
  }

  @Override
  public String toString() {
    return String.format("LinkingResult{successful=%s, errors=%d, warnings=%d, time=%s}",
        successful, linkingErrors.size(), linkingWarnings.size(), linkingTime);
  }

  /**
   * Linking metrics data.
   */
  public static final class LinkingMetrics {
    private final int inputObjectsCount;
    private final long totalInputSizeBytes;
    private final long outputSizeBytes;
    private final int resolvedSymbols;
    private final int unresolvedSymbols;
    private final long peakMemoryUsageBytes;

    public LinkingMetrics(final int inputObjectsCount,
                          final long totalInputSizeBytes,
                          final long outputSizeBytes,
                          final int resolvedSymbols,
                          final int unresolvedSymbols,
                          final long peakMemoryUsageBytes) {
      this.inputObjectsCount = inputObjectsCount;
      this.totalInputSizeBytes = totalInputSizeBytes;
      this.outputSizeBytes = outputSizeBytes;
      this.resolvedSymbols = resolvedSymbols;
      this.unresolvedSymbols = unresolvedSymbols;
      this.peakMemoryUsageBytes = peakMemoryUsageBytes;
    }

    public int getInputObjectsCount() { return inputObjectsCount; }
    public long getTotalInputSizeBytes() { return totalInputSizeBytes; }
    public long getOutputSizeBytes() { return outputSizeBytes; }
    public int getResolvedSymbols() { return resolvedSymbols; }
    public int getUnresolvedSymbols() { return unresolvedSymbols; }
    public long getPeakMemoryUsageBytes() { return peakMemoryUsageBytes; }

    /**
     * Gets the size reduction ratio.
     *
     * @return ratio of output size to input size
     */
    public double getSizeReductionRatio() {
      if (totalInputSizeBytes == 0) return 0.0;
      return (double) outputSizeBytes / totalInputSizeBytes;
    }

    @Override
    public String toString() {
      return String.format("LinkingMetrics{objects=%d, inputSize=%d bytes, outputSize=%d bytes, symbols=%d/%d}",
          inputObjectsCount, totalInputSizeBytes, outputSizeBytes, resolvedSymbols, unresolvedSymbols);
    }
  }
}