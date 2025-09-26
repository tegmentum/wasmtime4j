package ai.tegmentum.wasmtime4j.async;

import java.time.Duration;
import java.time.Instant;

/**
 * Represents a phase in WebAssembly instance instantiation.
 *
 * <p>Each instantiation phase has a name, duration, resource usage metrics,
 * and optional details that provide insight into the instantiation process.
 *
 * @since 1.0.0
 */
public final class InstantiationPhase {

  private final String name;
  private final Duration duration;
  private final Instant startTime;
  private final Instant endTime;
  private final long memoryAllocated;
  private final int functionsProcessed;
  private final String details;
  private final boolean successful;

  /**
   * Creates a new instantiation phase.
   *
   * @param name the phase name
   * @param duration time taken for this phase
   * @param startTime when the phase started
   * @param endTime when the phase completed
   * @param memoryAllocated memory allocated during this phase
   * @param functionsProcessed functions processed in this phase
   * @param details optional phase-specific details
   * @param successful whether the phase completed successfully
   */
  public InstantiationPhase(
      final String name,
      final Duration duration,
      final Instant startTime,
      final Instant endTime,
      final long memoryAllocated,
      final int functionsProcessed,
      final String details,
      final boolean successful) {
    this.name = name;
    this.duration = duration;
    this.startTime = startTime;
    this.endTime = endTime;
    this.memoryAllocated = memoryAllocated;
    this.functionsProcessed = functionsProcessed;
    this.details = details;
    this.successful = successful;
  }

  /**
   * Creates a successful instantiation phase with minimal information.
   *
   * @param name the phase name
   * @param duration time taken for this phase
   * @return a new successful instantiation phase
   */
  public static InstantiationPhase successful(final String name, final Duration duration) {
    final Instant now = Instant.now();
    return new InstantiationPhase(
        name, duration, now.minus(duration), now, 0L, 0, "", true);
  }

  /**
   * Creates a failed instantiation phase.
   *
   * @param name the phase name
   * @param duration time taken before failure
   * @param errorDetails details about the failure
   * @return a new failed instantiation phase
   */
  public static InstantiationPhase failed(
      final String name, final Duration duration, final String errorDetails) {
    final Instant now = Instant.now();
    return new InstantiationPhase(
        name, duration, now.minus(duration), now, 0L, 0, errorDetails, false);
  }

  /**
   * Creates an instantiation phase with resource metrics.
   *
   * @param name the phase name
   * @param duration time taken for this phase
   * @param memoryAllocated memory allocated during this phase
   * @param functionsProcessed functions processed in this phase
   * @return a new instantiation phase with metrics
   */
  public static InstantiationPhase withMetrics(
      final String name,
      final Duration duration,
      final long memoryAllocated,
      final int functionsProcessed) {
    final Instant now = Instant.now();
    return new InstantiationPhase(
        name, duration, now.minus(duration), now, memoryAllocated, functionsProcessed, "", true);
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
   * Gets the amount of memory allocated during this phase.
   *
   * @return memory allocated in bytes
   */
  public long getMemoryAllocated() {
    return memoryAllocated;
  }

  /**
   * Gets the number of functions processed in this phase.
   *
   * @return functions processed count
   */
  public int getFunctionsProcessed() {
    return functionsProcessed;
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
   * Checks if the phase completed successfully.
   *
   * @return true if the phase was successful
   */
  public boolean isSuccessful() {
    return successful;
  }

  /**
   * Calculates the memory allocation rate for this phase in bytes per second.
   *
   * @return memory allocation rate
   */
  public double getMemoryAllocationRate() {
    final double seconds = duration.toNanos() / 1_000_000_000.0;
    return seconds > 0 ? memoryAllocated / seconds : 0.0;
  }

  /**
   * Calculates the function processing rate for this phase in functions per second.
   *
   * @return function processing rate
   */
  public double getFunctionProcessingRate() {
    final double seconds = duration.toNanos() / 1_000_000_000.0;
    return seconds > 0 ? functionsProcessed / seconds : 0.0;
  }

  /**
   * Checks if this phase involved memory allocation.
   *
   * @return true if memory was allocated
   */
  public boolean hasMemoryAllocation() {
    return memoryAllocated > 0;
  }

  /**
   * Checks if this phase involved function processing.
   *
   * @return true if functions were processed
   */
  public boolean hasFunctionProcessing() {
    return functionsProcessed > 0;
  }

  /**
   * Checks if this phase has additional details.
   *
   * @return true if details are available
   */
  public boolean hasDetails() {
    return !details.isEmpty();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("InstantiationPhase{name='").append(name).append("'");
    sb.append(", duration=").append(duration);
    sb.append(", successful=").append(successful);

    if (memoryAllocated > 0) {
      sb.append(", memory=").append(formatBytes(memoryAllocated));
    }
    if (functionsProcessed > 0) {
      sb.append(", functions=").append(functionsProcessed);
    }
    if (!details.isEmpty()) {
      sb.append(", details='").append(details).append("'");
    }

    sb.append("}");
    return sb.toString();
  }

  private String formatBytes(final long bytes) {
    if (bytes < 1024) return bytes + "B";
    if (bytes < 1024 * 1024) return (bytes / 1024) + "KB";
    return (bytes / (1024 * 1024)) + "MB";
  }
}