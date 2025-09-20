package ai.tegmentum.wasmtime4j.async.reactive;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Represents an event in the WebAssembly module compilation process.
 *
 * <p>CompilationEvent provides detailed information about the various stages of compilation,
 * including progress updates, phase transitions, and error conditions. These events are designed to
 * work with reactive streams for real-time monitoring of compilation operations.
 *
 * <p>Events are immutable and contain all relevant information about the compilation state at the
 * time the event was created.
 *
 * @since 1.0.0
 */
public interface CompilationEvent {

  /**
   * Gets the unique identifier for the module being compiled.
   *
   * @return the module identifier
   */
  String getModuleId();

  /**
   * Gets the current compilation phase.
   *
   * @return the compilation phase
   */
  CompilationPhase getPhase();

  /**
   * Gets the compilation progress as a percentage.
   *
   * @return progress percentage (0.0 to 100.0)
   */
  double getProgress();

  /**
   * Gets the error that occurred during compilation, if any.
   *
   * @return the error, or empty if no error occurred
   */
  Optional<Exception> getError();

  /**
   * Gets the elapsed time since compilation started.
   *
   * @return elapsed duration
   */
  Duration getElapsed();

  /**
   * Gets the timestamp when this event was created.
   *
   * @return the event timestamp
   */
  Instant getTimestamp();

  /**
   * Gets the estimated remaining time for compilation.
   *
   * @return estimated remaining duration, or empty if unknown
   */
  Optional<Duration> getEstimatedTimeRemaining();

  /**
   * Gets the number of bytes processed so far.
   *
   * @return bytes processed
   */
  long getBytesProcessed();

  /**
   * Gets the total number of bytes to process.
   *
   * @return total bytes, or -1 if unknown
   */
  long getTotalBytes();

  /**
   * Gets the current compilation rate in bytes per second.
   *
   * @return compilation rate, or -1 if unknown
   */
  double getCompilationRateBytesPerSecond();

  /**
   * Gets additional details about the current compilation step.
   *
   * @return compilation step details, or empty if not available
   */
  Optional<String> getStepDetails();

  /**
   * Gets the optimization level being used for compilation.
   *
   * @return optimization level, or empty if not applicable
   */
  Optional<String> getOptimizationLevel();

  /**
   * Checks if this event represents a successful completion.
   *
   * @return true if compilation completed successfully
   */
  boolean isCompleted();

  /**
   * Checks if this event represents a failure.
   *
   * @return true if compilation failed
   */
  boolean isFailed();

  /**
   * Checks if this event represents a progress update.
   *
   * @return true if this is a progress update
   */
  boolean isProgressUpdate();

  /**
   * Checks if this event represents a phase transition.
   *
   * @return true if this is a phase transition
   */
  boolean isPhaseTransition();

  // Factory methods for creating events

  /**
   * Creates a phase transition event.
   *
   * @param moduleId the module identifier
   * @param phase the new compilation phase
   * @param elapsed elapsed time since compilation started
   * @return a new phase transition event
   */
  static CompilationEvent phaseTransition(
      final String moduleId, final CompilationPhase phase, final Duration elapsed) {
    return new CompilationEventImpl(
        moduleId,
        phase,
        0.0,
        null,
        elapsed,
        Instant.now(),
        null,
        0,
        -1,
        -1,
        null,
        null,
        false,
        false,
        false,
        true);
  }

  /**
   * Creates a progress update event.
   *
   * @param moduleId the module identifier
   * @param phase the current compilation phase
   * @param progress the progress percentage
   * @param elapsed elapsed time since compilation started
   * @param bytesProcessed bytes processed so far
   * @param totalBytes total bytes to process
   * @return a new progress update event
   */
  static CompilationEvent progressUpdate(
      final String moduleId,
      final CompilationPhase phase,
      final double progress,
      final Duration elapsed,
      final long bytesProcessed,
      final long totalBytes) {
    return new CompilationEventImpl(
        moduleId,
        phase,
        progress,
        null,
        elapsed,
        Instant.now(),
        null,
        bytesProcessed,
        totalBytes,
        -1,
        null,
        null,
        false,
        false,
        true,
        false);
  }

  /**
   * Creates a completion event.
   *
   * @param moduleId the module identifier
   * @param elapsed elapsed time for the entire compilation
   * @return a new completion event
   */
  static CompilationEvent completed(final String moduleId, final Duration elapsed) {
    return new CompilationEventImpl(
        moduleId,
        CompilationPhase.COMPLETED,
        100.0,
        null,
        elapsed,
        Instant.now(),
        null,
        -1,
        -1,
        -1,
        null,
        null,
        true,
        false,
        false,
        false);
  }

  /**
   * Creates a failure event.
   *
   * @param moduleId the module identifier
   * @param error the error that caused the failure
   * @param elapsed elapsed time when the failure occurred
   * @return a new failure event
   */
  static CompilationEvent failed(
      final String moduleId, final Exception error, final Duration elapsed) {
    return new CompilationEventImpl(
        moduleId,
        CompilationPhase.FAILED,
        0.0,
        error,
        elapsed,
        Instant.now(),
        null,
        -1,
        -1,
        -1,
        null,
        null,
        false,
        true,
        false,
        false);
  }

  /** Default implementation of CompilationEvent. */
  final class CompilationEventImpl implements CompilationEvent {
    private final String moduleId;
    private final CompilationPhase phase;
    private final double progress;
    private final Exception error;
    private final Duration elapsed;
    private final Instant timestamp;
    private final Duration estimatedTimeRemaining;
    private final long bytesProcessed;
    private final long totalBytes;
    private final double compilationRate;
    private final String stepDetails;
    private final String optimizationLevel;
    private final boolean completed;
    private final boolean failed;
    private final boolean progressUpdate;
    private final boolean phaseTransition;

    CompilationEventImpl(
        final String moduleId,
        final CompilationPhase phase,
        final double progress,
        final Exception error,
        final Duration elapsed,
        final Instant timestamp,
        final Duration estimatedTimeRemaining,
        final long bytesProcessed,
        final long totalBytes,
        final double compilationRate,
        final String stepDetails,
        final String optimizationLevel,
        final boolean completed,
        final boolean failed,
        final boolean progressUpdate,
        final boolean phaseTransition) {
      this.moduleId = moduleId;
      this.phase = phase;
      this.progress = progress;
      this.error = error;
      this.elapsed = elapsed;
      this.timestamp = timestamp;
      this.estimatedTimeRemaining = estimatedTimeRemaining;
      this.bytesProcessed = bytesProcessed;
      this.totalBytes = totalBytes;
      this.compilationRate = compilationRate;
      this.stepDetails = stepDetails;
      this.optimizationLevel = optimizationLevel;
      this.completed = completed;
      this.failed = failed;
      this.progressUpdate = progressUpdate;
      this.phaseTransition = phaseTransition;
    }

    @Override
    public String getModuleId() {
      return moduleId;
    }

    @Override
    public CompilationPhase getPhase() {
      return phase;
    }

    @Override
    public double getProgress() {
      return progress;
    }

    @Override
    public Optional<Exception> getError() {
      return Optional.ofNullable(error);
    }

    @Override
    public Duration getElapsed() {
      return elapsed;
    }

    @Override
    public Instant getTimestamp() {
      return timestamp;
    }

    @Override
    public Optional<Duration> getEstimatedTimeRemaining() {
      return Optional.ofNullable(estimatedTimeRemaining);
    }

    @Override
    public long getBytesProcessed() {
      return bytesProcessed;
    }

    @Override
    public long getTotalBytes() {
      return totalBytes;
    }

    @Override
    public double getCompilationRateBytesPerSecond() {
      return compilationRate;
    }

    @Override
    public Optional<String> getStepDetails() {
      return Optional.ofNullable(stepDetails);
    }

    @Override
    public Optional<String> getOptimizationLevel() {
      return Optional.ofNullable(optimizationLevel);
    }

    @Override
    public boolean isCompleted() {
      return completed;
    }

    @Override
    public boolean isFailed() {
      return failed;
    }

    @Override
    public boolean isProgressUpdate() {
      return progressUpdate;
    }

    @Override
    public boolean isPhaseTransition() {
      return phaseTransition;
    }

    @Override
    public String toString() {
      return String.format(
          "CompilationEvent{moduleId='%s', phase=%s, progress=%.1f%%, elapsed=%s, completed=%s,"
              + " failed=%s}",
          moduleId, phase, progress, elapsed, completed, failed);
    }
  }
}
