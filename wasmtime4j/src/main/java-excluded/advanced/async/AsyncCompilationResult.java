package ai.tegmentum.wasmtime4j.async;

import ai.tegmentum.wasmtime4j.Module;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Result of an asynchronous WebAssembly module compilation operation.
 *
 * <p>This class provides detailed information about the compilation process,
 * including timing data, compilation phases, and the resulting module.
 *
 * @since 1.0.0
 */
public final class AsyncCompilationResult {

  private final Module module;
  private final Duration compilationTime;
  private final Instant startTime;
  private final Instant endTime;
  private final List<CompilationPhase> phases;
  private final Optional<String> warnings;
  private final CompilationStatistics statistics;

  /**
   * Creates a new async compilation result.
   *
   * @param module the compiled WebAssembly module
   * @param compilationTime total time taken for compilation
   * @param startTime when compilation started
   * @param endTime when compilation completed
   * @param phases list of compilation phases executed
   * @param warnings optional warnings from compilation
   * @param statistics detailed compilation statistics
   */
  public AsyncCompilationResult(
      final Module module,
      final Duration compilationTime,
      final Instant startTime,
      final Instant endTime,
      final List<CompilationPhase> phases,
      final Optional<String> warnings,
      final CompilationStatistics statistics) {
    this.module = module;
    this.compilationTime = compilationTime;
    this.startTime = startTime;
    this.endTime = endTime;
    this.phases = List.copyOf(phases);
    this.warnings = warnings;
    this.statistics = statistics;
  }

  /**
   * Gets the compiled WebAssembly module.
   *
   * @return the compiled module
   */
  public Module getModule() {
    return module;
  }

  /**
   * Gets the total compilation time.
   *
   * @return the compilation duration
   */
  public Duration getCompilationTime() {
    return compilationTime;
  }

  /**
   * Gets the compilation start time.
   *
   * @return the start timestamp
   */
  public Instant getStartTime() {
    return startTime;
  }

  /**
   * Gets the compilation end time.
   *
   * @return the end timestamp
   */
  public Instant getEndTime() {
    return endTime;
  }

  /**
   * Gets the compilation phases executed.
   *
   * @return immutable list of compilation phases
   */
  public List<CompilationPhase> getPhases() {
    return phases;
  }

  /**
   * Gets any warnings from compilation.
   *
   * @return optional warnings string
   */
  public Optional<String> getWarnings() {
    return warnings;
  }

  /**
   * Gets detailed compilation statistics.
   *
   * @return the compilation statistics
   */
  public CompilationStatistics getStatistics() {
    return statistics;
  }

  /**
   * Checks if compilation produced any warnings.
   *
   * @return true if warnings were generated
   */
  public boolean hasWarnings() {
    return warnings.isPresent();
  }

  /**
   * Gets the compilation throughput in bytes per second.
   *
   * @return compilation throughput
   */
  public double getThroughput() {
    final long totalBytes = statistics.getBytesProcessed();
    final double seconds = compilationTime.toNanos() / 1_000_000_000.0;
    return seconds > 0 ? totalBytes / seconds : 0.0;
  }

  @Override
  public String toString() {
    return String.format(
        "AsyncCompilationResult{time=%s, phases=%d, throughput=%.2f MB/s, hasWarnings=%s}",
        compilationTime, phases.size(), getThroughput() / (1024.0 * 1024.0), hasWarnings());
  }
}