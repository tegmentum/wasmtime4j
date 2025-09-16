package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Real-time progress reporter that implements the Observer pattern to provide live updates during
 * long-running comparison operations. Supports both console output and programmatic progress
 * tracking.
 *
 * @since 1.0.0
 */
public final class ProgressReporter implements ProgressListener {
  private final PrintStream output;
  private final VerbosityLevel verbosity;
  private final boolean useColors;
  private final SummaryFormatter formatter;

  // Progress tracking
  private final AtomicReference<String> currentOperation = new AtomicReference<>();
  private final AtomicInteger totalSteps = new AtomicInteger(0);
  private final AtomicInteger currentStep = new AtomicInteger(0);
  private final AtomicReference<Instant> operationStart = new AtomicReference<>();

  // Operation history for debugging
  private final ConcurrentLinkedQueue<OperationEvent> eventHistory = new ConcurrentLinkedQueue<>();
  private final ConcurrentHashMap<String, OperationStats> operationStats =
      new ConcurrentHashMap<>();

  // Output control
  private volatile boolean progressBarEnabled = true;
  private volatile boolean timestampsEnabled = false;
  private final Object outputLock = new Object();

  /**
   * Creates a new ProgressReporter with the specified configuration.
   *
   * @param output the output stream for progress updates
   * @param verbosity the verbosity level for controlling output detail
   * @param useColors whether to use ANSI color codes
   */
  public ProgressReporter(
      final PrintStream output, final VerbosityLevel verbosity, final boolean useColors) {
    this.output = Objects.requireNonNull(output, "output cannot be null");
    this.verbosity = Objects.requireNonNull(verbosity, "verbosity cannot be null");
    this.useColors = useColors && ConsoleColors.isColorSupported();
    this.formatter = new SummaryFormatter(verbosity, this.useColors);
  }

  /**
   * Creates a ProgressReporter that outputs to System.out.
   *
   * @param verbosity the verbosity level
   * @param useColors whether to use colors
   * @return new ProgressReporter instance
   */
  public static ProgressReporter forConsole(
      final VerbosityLevel verbosity, final boolean useColors) {
    return new ProgressReporter(System.out, verbosity, useColors);
  }

  @Override
  public void onOperationStarted(final String operationName, final int totalSteps) {
    final Instant startTime = Instant.now();
    currentOperation.set(operationName);
    this.totalSteps.set(totalSteps);
    currentStep.set(0);
    operationStart.set(startTime);

    recordEvent(OperationEventType.STARTED, operationName, null, null);

    if (verbosity.includes(VerbosityLevel.NORMAL)) {
      synchronized (outputLock) {
        final String message = String.format("Starting %s", operationName);
        if (totalSteps > 0) {
          output.printf("%s [0/%d]%n", colorizeMessage(message), totalSteps);
        } else {
          output.printf("%s...%n", colorizeMessage(message));
        }

        if (progressBarEnabled && totalSteps > 0 && verbosity.includes(VerbosityLevel.VERBOSE)) {
          output.print(createProgressBar(0, totalSteps));
          output.flush();
        }
      }
    }
  }

  @Override
  public void onProgress(final int currentStep, final String stepDescription) {
    this.currentStep.set(currentStep);
    recordEvent(OperationEventType.PROGRESS, stepDescription, currentStep, null);

    if (verbosity.includes(VerbosityLevel.VERBOSE)) {
      synchronized (outputLock) {
        final String operation = currentOperation.get();
        final int total = totalSteps.get();

        if (total > 0) {
          final String progressInfo = String.format("[%d/%d]", currentStep, total);
          final String status = stepDescription != null ? stepDescription : "In progress";
          final String line = formatter.formatProgressLine(operation, progressInfo, status);

          if (progressBarEnabled) {
            // Clear current line and show progress
            output.print("\r" + " ".repeat(80) + "\r");
            output.print(line);
            if (stepDescription != null) {
              output.printf(" - %s", stepDescription);
            }
            output.flush();
          } else {
            output.printf("%s - %s%n", line, stepDescription != null ? stepDescription : "");
          }
        } else {
          // Indeterminate progress
          if (stepDescription != null) {
            output.printf("  %s%n", colorizeMessage(stepDescription));
          }
        }
      }
    }
  }

  @Override
  public void onOperationCompleted(final String operationName, final String message) {
    final Instant endTime = Instant.now();
    final Instant startTime = operationStart.get();
    final Duration duration =
        startTime != null ? Duration.between(startTime, endTime) : Duration.ZERO;

    recordEvent(OperationEventType.COMPLETED, operationName, null, message);
    updateOperationStats(operationName, duration, true);

    if (verbosity.includes(VerbosityLevel.NORMAL)) {
      synchronized (outputLock) {
        if (progressBarEnabled && totalSteps.get() > 0) {
          // Clear progress bar line
          output.print("\r" + " ".repeat(80) + "\r");
        }

        final String completionMessage = message != null ? message : "completed";
        final String durationText = formatDuration(duration);

        if (useColors) {
          output.printf(
              "%s %s (%s)%n",
              ConsoleColors.success("✓"), operationName, ConsoleColors.dim(durationText));
        } else {
          output.printf("✓ %s (%s)%n", operationName, durationText);
        }

        if (message != null && verbosity.includes(VerbosityLevel.VERBOSE)) {
          output.printf("  %s%n", message);
        }
      }
    }

    // Reset state
    currentOperation.set(null);
    totalSteps.set(0);
    currentStep.set(0);
    operationStart.set(null);
  }

  @Override
  public void onOperationFailed(final String operationName, final Throwable error) {
    final Instant endTime = Instant.now();
    final Instant startTime = operationStart.get();
    final Duration duration =
        startTime != null ? Duration.between(startTime, endTime) : Duration.ZERO;

    recordEvent(OperationEventType.FAILED, operationName, null, error.getMessage());
    updateOperationStats(operationName, duration, false);

    if (verbosity.includes(VerbosityLevel.QUIET)) { // Always show failures
      synchronized (outputLock) {
        if (progressBarEnabled && totalSteps.get() > 0) {
          // Clear progress bar line
          output.print("\r" + " ".repeat(80) + "\r");
        }

        if (useColors) {
          output.printf(
              "%s %s: %s%n",
              ConsoleColors.error("✗"), operationName, ConsoleColors.error(error.getMessage()));
        } else {
          output.printf("✗ %s: %s%n", operationName, error.getMessage());
        }

        if (verbosity.includes(VerbosityLevel.DEBUG)) {
          output.printf("  Stack trace:%n");
          error.printStackTrace(output);
        }
      }
    }

    // Reset state
    currentOperation.set(null);
    totalSteps.set(0);
    currentStep.set(0);
    operationStart.set(null);
  }

  @Override
  public void onStatusUpdate(final String message, final String details) {
    recordEvent(OperationEventType.STATUS, message, null, details);

    if (verbosity.includes(VerbosityLevel.VERBOSE)) {
      synchronized (outputLock) {
        final String timestamp =
            timestampsEnabled
                ? String.format("[%s] ", Instant.now().toString().substring(11, 19))
                : "";

        output.printf("%s%s%n", timestamp, colorizeMessage(message));

        if (details != null && verbosity.includes(VerbosityLevel.DEBUG)) {
          output.printf("  %s%n", details);
        }
      }
    }
  }

  /**
   * Gets the current progress as a percentage (0-100).
   *
   * @return progress percentage, or -1 if indeterminate
   */
  public int getProgressPercentage() {
    final int total = totalSteps.get();
    final int current = currentStep.get();

    if (total <= 0) {
      return -1; // Indeterminate
    }

    return Math.min(100, (current * 100) / total);
  }

  /**
   * Gets the name of the currently running operation.
   *
   * @return current operation name, or null if none
   */
  public String getCurrentOperation() {
    return currentOperation.get();
  }

  /**
   * Gets the estimated time remaining for the current operation.
   *
   * @return estimated time remaining, or null if unknown
   */
  public Duration getEstimatedTimeRemaining() {
    final Instant start = operationStart.get();
    final int total = totalSteps.get();
    final int current = currentStep.get();

    if (start == null || total <= 0 || current <= 0) {
      return null;
    }

    final Duration elapsed = Duration.between(start, Instant.now());
    final double progressRatio = (double) current / total;
    final long totalEstimatedMillis = (long) (elapsed.toMillis() / progressRatio);
    final long remainingMillis = totalEstimatedMillis - elapsed.toMillis();

    return Duration.ofMillis(Math.max(0, remainingMillis));
  }

  /**
   * Enables or disables progress bar display.
   *
   * @param enabled whether to show progress bars
   */
  public void setProgressBarEnabled(final boolean enabled) {
    this.progressBarEnabled = enabled;
  }

  /**
   * Enables or disables timestamps in status messages.
   *
   * @param enabled whether to show timestamps
   */
  public void setTimestampsEnabled(final boolean enabled) {
    this.timestampsEnabled = enabled;
  }

  /**
   * Gets statistics for all completed operations.
   *
   * @return operation statistics
   */
  public ConcurrentHashMap<String, OperationStats> getOperationStats() {
    return new ConcurrentHashMap<>(operationStats);
  }

  private void recordEvent(
      final OperationEventType type,
      final String operation,
      final Integer step,
      final String details) {
    eventHistory.offer(new OperationEvent(type, operation, step, details, Instant.now()));

    // Keep only last 1000 events to prevent memory leaks
    while (eventHistory.size() > 1000) {
      eventHistory.poll();
    }
  }

  private void updateOperationStats(
      final String operationName, final Duration duration, final boolean success) {
    operationStats.compute(
        operationName,
        (name, existing) -> {
          if (existing == null) {
            return new OperationStats(1, success ? 1 : 0, duration, duration, duration);
          } else {
            final int newCount = existing.getCount() + 1;
            final int newSuccessCount = existing.getSuccessCount() + (success ? 1 : 0);
            final Duration newMin =
                duration.compareTo(existing.getMinDuration()) < 0
                    ? duration
                    : existing.getMinDuration();
            final Duration newMax =
                duration.compareTo(existing.getMaxDuration()) > 0
                    ? duration
                    : existing.getMaxDuration();
            final Duration newTotal = existing.getTotalDuration().plus(duration);

            return new OperationStats(newCount, newSuccessCount, newMin, newMax, newTotal);
          }
        });
  }

  private String createProgressBar(final int current, final int total) {
    if (total <= 0) {
      return "";
    }

    final int barWidth = 40;
    final int filled = (current * barWidth) / total;
    final int percentage = (current * 100) / total;

    final StringBuilder bar = new StringBuilder();
    bar.append("[");
    bar.append("=".repeat(filled));
    bar.append(" ".repeat(barWidth - filled));
    bar.append("] ");
    bar.append(String.format("%3d%%", percentage));

    return bar.toString();
  }

  private String colorizeMessage(final String message) {
    if (!useColors || message == null) {
      return message;
    }

    // Simple heuristic coloring based on message content
    final String lower = message.toLowerCase();
    if (lower.contains("error") || lower.contains("failed")) {
      return ConsoleColors.error(message);
    } else if (lower.contains("warning") || lower.contains("warn")) {
      return ConsoleColors.warning(message);
    } else if (lower.contains("success") || lower.contains("completed")) {
      return ConsoleColors.success(message);
    } else {
      return ConsoleColors.info(message);
    }
  }

  private String formatDuration(final Duration duration) {
    final long millis = duration.toMillis();
    if (millis < 1000) {
      return millis + "ms";
    } else if (millis < 60000) {
      return String.format("%.1fs", millis / 1000.0);
    } else {
      final long seconds = duration.getSeconds();
      final long minutes = seconds / 60;
      final long remainingSeconds = seconds % 60;
      return String.format("%dm %ds", minutes, remainingSeconds);
    }
  }

  /** Event types for operation tracking. */
  private enum OperationEventType {
    STARTED,
    PROGRESS,
    COMPLETED,
    FAILED,
    STATUS
  }

  /** Single operation event for history tracking. */
  private static final class OperationEvent {
    private final OperationEventType type;
    private final String operation;
    private final Integer step;
    private final String details;
    private final Instant timestamp;

    OperationEvent(
        final OperationEventType type,
        final String operation,
        final Integer step,
        final String details,
        final Instant timestamp) {
      this.type = type;
      this.operation = operation;
      this.step = step;
      this.details = details;
      this.timestamp = timestamp;
    }

    public OperationEventType getType() {
      return type;
    }

    public String getOperation() {
      return operation;
    }

    public Integer getStep() {
      return step;
    }

    public String getDetails() {
      return details;
    }

    public Instant getTimestamp() {
      return timestamp;
    }
  }

  /** Statistics for a specific operation type. */
  public static final class OperationStats {
    private final int count;
    private final int successCount;
    private final Duration minDuration;
    private final Duration maxDuration;
    private final Duration totalDuration;

    OperationStats(
        final int count,
        final int successCount,
        final Duration minDuration,
        final Duration maxDuration,
        final Duration totalDuration) {
      this.count = count;
      this.successCount = successCount;
      this.minDuration = minDuration;
      this.maxDuration = maxDuration;
      this.totalDuration = totalDuration;
    }

    public int getCount() {
      return count;
    }

    public int getSuccessCount() {
      return successCount;
    }

    public int getFailureCount() {
      return count - successCount;
    }

    public double getSuccessRate() {
      return count > 0 ? (double) successCount / count : 0.0;
    }

    public Duration getMinDuration() {
      return minDuration;
    }

    public Duration getMaxDuration() {
      return maxDuration;
    }

    public Duration getAverageDuration() {
      return count > 0 ? totalDuration.dividedBy(count) : Duration.ZERO;
    }

    public Duration getTotalDuration() {
      return totalDuration;
    }

    @Override
    public String toString() {
      return String.format(
          "OperationStats{count=%d, success=%.1f%%, avg=%s}",
          count, getSuccessRate() * 100, getAverageDuration());
    }
  }
}
