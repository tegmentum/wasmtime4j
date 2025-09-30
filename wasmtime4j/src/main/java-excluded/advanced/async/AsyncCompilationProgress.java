package ai.tegmentum.wasmtime4j.async;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Progress information for asynchronous WebAssembly module compilation.
 *
 * <p>This class provides real-time updates about the compilation process,
 * including current phase, completion percentage, and estimated time remaining.
 *
 * @since 1.0.0
 */
public final class AsyncCompilationProgress {

  private final String currentPhase;
  private final double completionPercentage;
  private final Duration elapsedTime;
  private final Optional<Duration> estimatedTimeRemaining;
  private final List<CompilationPhase> completedPhases;
  private final long bytesProcessed;
  private final long totalBytes;
  private final String statusMessage;
  private final boolean cancellable;

  /**
   * Creates new compilation progress information.
   *
   * @param currentPhase name of the current compilation phase
   * @param completionPercentage completion percentage (0.0 - 1.0)
   * @param elapsedTime time elapsed since compilation started
   * @param estimatedTimeRemaining optional estimated time remaining
   * @param completedPhases list of completed compilation phases
   * @param bytesProcessed number of bytes processed so far
   * @param totalBytes total bytes to process
   * @param statusMessage current status message
   * @param cancellable whether compilation can be cancelled
   */
  public AsyncCompilationProgress(
      final String currentPhase,
      final double completionPercentage,
      final Duration elapsedTime,
      final Optional<Duration> estimatedTimeRemaining,
      final List<CompilationPhase> completedPhases,
      final long bytesProcessed,
      final long totalBytes,
      final String statusMessage,
      final boolean cancellable) {
    this.currentPhase = currentPhase;
    this.completionPercentage = Math.max(0.0, Math.min(1.0, completionPercentage));
    this.elapsedTime = elapsedTime;
    this.estimatedTimeRemaining = estimatedTimeRemaining;
    this.completedPhases = List.copyOf(completedPhases);
    this.bytesProcessed = bytesProcessed;
    this.totalBytes = totalBytes;
    this.statusMessage = statusMessage;
    this.cancellable = cancellable;
  }

  /**
   * Creates initial progress state.
   *
   * @param totalBytes total bytes to process
   * @return initial progress
   */
  public static AsyncCompilationProgress initial(final long totalBytes) {
    return new AsyncCompilationProgress(
        CompilationPhase.PARSE,
        0.0,
        Duration.ZERO,
        Optional.empty(),
        List.of(),
        0L,
        totalBytes,
        "Starting compilation",
        true);
  }

  /**
   * Creates completed progress state.
   *
   * @param phases all completed phases
   * @param elapsedTime total time taken
   * @param totalBytes total bytes processed
   * @return completed progress
   */
  public static AsyncCompilationProgress completed(
      final List<CompilationPhase> phases,
      final Duration elapsedTime,
      final long totalBytes) {
    return new AsyncCompilationProgress(
        "completed",
        1.0,
        elapsedTime,
        Optional.of(Duration.ZERO),
        phases,
        totalBytes,
        totalBytes,
        "Compilation completed successfully",
        false);
  }

  /**
   * Gets the current compilation phase name.
   *
   * @return current phase name
   */
  public String getCurrentPhase() {
    return currentPhase;
  }

  /**
   * Gets the completion percentage.
   *
   * @return completion percentage (0.0 - 1.0)
   */
  public double getCompletionPercentage() {
    return completionPercentage;
  }

  /**
   * Gets the time elapsed since compilation started.
   *
   * @return elapsed time
   */
  public Duration getElapsedTime() {
    return elapsedTime;
  }

  /**
   * Gets the estimated time remaining.
   *
   * @return optional estimated time remaining
   */
  public Optional<Duration> getEstimatedTimeRemaining() {
    return estimatedTimeRemaining;
  }

  /**
   * Gets the list of completed compilation phases.
   *
   * @return immutable list of completed phases
   */
  public List<CompilationPhase> getCompletedPhases() {
    return completedPhases;
  }

  /**
   * Gets the number of bytes processed so far.
   *
   * @return bytes processed
   */
  public long getBytesProcessed() {
    return bytesProcessed;
  }

  /**
   * Gets the total bytes to process.
   *
   * @return total bytes
   */
  public long getTotalBytes() {
    return totalBytes;
  }

  /**
   * Gets the current status message.
   *
   * @return status message
   */
  public String getStatusMessage() {
    return statusMessage;
  }

  /**
   * Checks if compilation can be cancelled.
   *
   * @return true if cancellation is supported
   */
  public boolean isCancellable() {
    return cancellable;
  }

  /**
   * Checks if compilation is completed.
   *
   * @return true if compilation is finished
   */
  public boolean isCompleted() {
    return completionPercentage >= 1.0;
  }

  /**
   * Calculates the current throughput in bytes per second.
   *
   * @return current throughput
   */
  public double getCurrentThroughput() {
    final double seconds = elapsedTime.toNanos() / 1_000_000_000.0;
    return seconds > 0 ? bytesProcessed / seconds : 0.0;
  }

  /**
   * Creates a copy with updated progress information.
   *
   * @param currentPhase new current phase
   * @param completionPercentage new completion percentage
   * @param bytesProcessed new bytes processed count
   * @param statusMessage new status message
   * @return updated progress
   */
  public AsyncCompilationProgress withUpdate(
      final String currentPhase,
      final double completionPercentage,
      final long bytesProcessed,
      final String statusMessage) {
    final Duration newElapsed = elapsedTime.plus(Duration.ofMillis(100)); // Simulate progression
    final Optional<Duration> eta = estimateTimeRemaining(completionPercentage, newElapsed);

    return new AsyncCompilationProgress(
        currentPhase,
        completionPercentage,
        newElapsed,
        eta,
        completedPhases,
        bytesProcessed,
        totalBytes,
        statusMessage,
        cancellable);
  }

  private Optional<Duration> estimateTimeRemaining(
      final double completion, final Duration elapsed) {
    if (completion <= 0.0) {
      return Optional.empty();
    }

    final double totalEstimate = elapsed.toNanos() / completion;
    final long remainingNanos = (long) (totalEstimate - elapsed.toNanos());

    return remainingNanos > 0
        ? Optional.of(Duration.ofNanos(remainingNanos))
        : Optional.of(Duration.ZERO);
  }

  @Override
  public String toString() {
    return String.format(
        "AsyncCompilationProgress{phase='%s', completion=%.1f%%, throughput=%.2f KB/s}",
        currentPhase, completionPercentage * 100, getCurrentThroughput() / 1024.0);
  }
}