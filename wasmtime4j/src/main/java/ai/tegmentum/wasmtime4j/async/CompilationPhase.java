package ai.tegmentum.wasmtime4j.async;

import java.time.Duration;
import java.time.Instant;

/**
 * Represents a phase in WebAssembly module compilation.
 *
 * <p>Each compilation phase has a name, duration, and optional metrics
 * that provide insight into the compilation process.
 *
 * @since 1.0.0
 */
public final class CompilationPhase {

  /** Phase names for common compilation stages. */
  public static final String PARSE = "parse";
  public static final String VALIDATE = "validate";
  public static final String OPTIMIZE = "optimize";
  public static final String CODEGEN = "codegen";
  public static final String LINK = "link";
  public static final String FINALIZE = "finalize";

  private final String name;
  private final Duration duration;
  private final Instant startTime;
  private final Instant endTime;
  private final long bytesProcessed;
  private final String details;

  /**
   * Creates a new compilation phase.
   *
   * @param name the phase name
   * @param duration time taken for this phase
   * @param startTime when the phase started
   * @param endTime when the phase completed
   * @param bytesProcessed number of bytes processed in this phase
   * @param details optional phase-specific details
   */
  public CompilationPhase(
      final String name,
      final Duration duration,
      final Instant startTime,
      final Instant endTime,
      final long bytesProcessed,
      final String details) {
    this.name = name;
    this.duration = duration;
    this.startTime = startTime;
    this.endTime = endTime;
    this.bytesProcessed = bytesProcessed;
    this.details = details;
  }

  /**
   * Creates a new compilation phase with minimal information.
   *
   * @param name the phase name
   * @param duration time taken for this phase
   * @return a new compilation phase
   */
  public static CompilationPhase of(final String name, final Duration duration) {
    final Instant now = Instant.now();
    return new CompilationPhase(
        name, duration, now.minus(duration), now, 0L, "");
  }

  /**
   * Gets the phase name.
   *
   * @return the phase name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the phase duration.
   *
   * @return the time taken for this phase
   */
  public Duration getDuration() {
    return duration;
  }

  /**
   * Gets the phase start time.
   *
   * @return when the phase started
   */
  public Instant getStartTime() {
    return startTime;
  }

  /**
   * Gets the phase end time.
   *
   * @return when the phase completed
   */
  public Instant getEndTime() {
    return endTime;
  }

  /**
   * Gets the number of bytes processed in this phase.
   *
   * @return bytes processed count
   */
  public long getBytesProcessed() {
    return bytesProcessed;
  }

  /**
   * Gets optional phase-specific details.
   *
   * @return details string, may be empty
   */
  public String getDetails() {
    return details;
  }

  /**
   * Calculates the throughput for this phase in bytes per second.
   *
   * @return throughput in bytes/second
   */
  public double getThroughput() {
    final double seconds = duration.toNanos() / 1_000_000_000.0;
    return seconds > 0 ? bytesProcessed / seconds : 0.0;
  }

  @Override
  public String toString() {
    return String.format(
        "CompilationPhase{name='%s', duration=%s, throughput=%.2f KB/s}",
        name, duration, getThroughput() / 1024.0);
  }
}