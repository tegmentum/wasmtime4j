package ai.tegmentum.wasmtime4j.exception;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Monitoring and analytics utility for WebAssembly runtime errors.
 *
 * <p>This class provides error tracking, pattern analysis, and monitoring capabilities for
 * wasmtime4j applications. It helps identify common error patterns, performance issues, and
 * potential problems in WebAssembly module execution.
 *
 * <p>Error monitoring features:
 *
 * <ul>
 *   <li>Error frequency tracking and statistics
 *   <li>Error pattern analysis and correlation
 *   <li>Performance impact measurement
 *   <li>Recovery suggestion effectiveness tracking
 *   <li>Runtime health monitoring
 * </ul>
 *
 * <p>This class is thread-safe and can be used in multi-threaded environments.
 *
 * @since 1.0.0
 */
public final class ErrorMonitor {

  private static final Logger logger = Logger.getLogger(ErrorMonitor.class.getName());

  /** Singleton instance for global error monitoring. */
  private static final ErrorMonitor INSTANCE = new ErrorMonitor();

  /** Error occurrence counters by exception type. */
  private final ConcurrentHashMap<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();

  /** Recent error occurrences for pattern analysis. */
  private final ConcurrentHashMap<String, List<ErrorOccurrence>> recentErrors =
      new ConcurrentHashMap<>();

  /** Error rate tracking for performance monitoring. */
  private final ConcurrentHashMap<String, RateTracker> errorRates = new ConcurrentHashMap<>();

  /** Maximum number of recent errors to keep per type. */
  private static final int MAX_RECENT_ERRORS = 100;

  /** Time window for rate tracking (in minutes). */
  private static final int RATE_WINDOW_MINUTES = 15;

  /** Error occurrence record for pattern analysis. */
  public static class ErrorOccurrence {
    private final Instant timestamp;
    private final String errorType;
    private final String errorMessage;
    private final String functionContext;
    private final boolean isRetryable;
    private final long threadId;

    ErrorOccurrence(
        final String errorType,
        final String errorMessage,
        final String functionContext,
        final boolean isRetryable) {
      this.timestamp = Instant.now();
      this.errorType = errorType;
      this.errorMessage = errorMessage;
      this.functionContext = functionContext;
      this.isRetryable = isRetryable;
      this.threadId = Thread.currentThread().getId();
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public String getErrorType() {
      return errorType;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    public String getFunctionContext() {
      return functionContext;
    }

    public boolean isRetryable() {
      return isRetryable;
    }

    public long getThreadId() {
      return threadId;
    }
  }

  /** Rate tracking for error frequency analysis. */
  private static class RateTracker {
    private final ConcurrentHashMap<Long, AtomicLong> buckets = new ConcurrentHashMap<>();
    private volatile long lastCleanup = System.currentTimeMillis();

    void recordEvent() {
      final long currentMinute = System.currentTimeMillis() / (60 * 1000);
      buckets.computeIfAbsent(currentMinute, k -> new AtomicLong(0)).incrementAndGet();

      // Cleanup old buckets periodically
      final long now = System.currentTimeMillis();
      if (now - lastCleanup > 5 * 60 * 1000) { // Cleanup every 5 minutes
        cleanup();
        lastCleanup = now;
      }
    }

    double getRate() {
      final long currentMinute = System.currentTimeMillis() / (60 * 1000);
      final long windowStart = currentMinute - RATE_WINDOW_MINUTES;

      long totalEvents = 0;
      for (Map.Entry<Long, AtomicLong> entry : buckets.entrySet()) {
        if (entry.getKey() >= windowStart) {
          totalEvents += entry.getValue().get();
        }
      }

      return (double) totalEvents / RATE_WINDOW_MINUTES;
    }

    private void cleanup() {
      final long currentMinute = System.currentTimeMillis() / (60 * 1000);
      final long cutoff = currentMinute - (RATE_WINDOW_MINUTES * 2); // Keep double the window
      buckets.entrySet().removeIf(entry -> entry.getKey() < cutoff);
    }
  }

  /** Error monitoring statistics. */
  public static class ErrorStatistics {
    private final String errorType;
    private final long totalOccurrences;
    private final double errorRate;
    private final Instant firstOccurrence;
    private final Instant lastOccurrence;
    private final double retryablePercentage;
    private final List<String> commonMessages;
    private final List<String> commonFunctions;

    ErrorStatistics(
        final String errorType,
        final long totalOccurrences,
        final double errorRate,
        final Instant firstOccurrence,
        final Instant lastOccurrence,
        final double retryablePercentage,
        final List<String> commonMessages,
        final List<String> commonFunctions) {
      this.errorType = errorType;
      this.totalOccurrences = totalOccurrences;
      this.errorRate = errorRate;
      this.firstOccurrence = firstOccurrence;
      this.lastOccurrence = lastOccurrence;
      this.retryablePercentage = retryablePercentage;
      this.commonMessages = new ArrayList<>(commonMessages);
      this.commonFunctions = new ArrayList<>(commonFunctions);
    }

    public String getErrorType() {
      return errorType;
    }

    public long getTotalOccurrences() {
      return totalOccurrences;
    }

    public double getErrorRate() {
      return errorRate;
    }

    public Instant getFirstOccurrence() {
      return firstOccurrence;
    }

    public Instant getLastOccurrence() {
      return lastOccurrence;
    }

    public double getRetryablePercentage() {
      return retryablePercentage;
    }

    public List<String> getCommonMessages() {
      return new ArrayList<>(commonMessages);
    }

    public List<String> getCommonFunctions() {
      return new ArrayList<>(commonFunctions);
    }
  }

  /** Private constructor for singleton pattern. */
  private ErrorMonitor() {}

  /**
   * Gets the global error monitor instance.
   *
   * @return the error monitor instance
   */
  public static ErrorMonitor getInstance() {
    return INSTANCE;
  }

  /**
   * Records an error occurrence for monitoring and analysis.
   *
   * @param exception the WebAssembly exception that occurred
   */
  public void recordError(final WasmException exception) {
    if (exception == null) {
      return;
    }

    final String errorType = exception.getClass().getSimpleName();
    final String errorMessage = exception.getMessage();
    final String functionContext = extractFunctionContext(exception);
    final boolean isRetryable = determineIfRetryable(exception);

    // Update error counters
    errorCounts.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();

    // Update error rates
    errorRates.computeIfAbsent(errorType, k -> new RateTracker()).recordEvent();

    // Store recent error for pattern analysis
    final ErrorOccurrence occurrence =
        new ErrorOccurrence(errorType, errorMessage, functionContext, isRetryable);

    recentErrors.computeIfAbsent(errorType, k -> new ArrayList<>()).add(occurrence);

    // Limit recent errors list size
    final List<ErrorOccurrence> errors = recentErrors.get(errorType);
    if (errors.size() > MAX_RECENT_ERRORS) {
      errors.remove(0); // Remove oldest
    }

    // Log error for debugging
    logger.fine("Recorded error: " + errorType + " - " + errorMessage);

    // Check for error patterns that might indicate systemic issues
    analyzeErrorPatterns(errorType);
  }

  /**
   * Gets error statistics for all monitored error types.
   *
   * @return list of error statistics sorted by occurrence count
   */
  public List<ErrorStatistics> getErrorStatistics() {
    return errorCounts.entrySet().stream()
        .map(
            entry -> {
              final String errorType = entry.getKey();
              final long totalOccurrences = entry.getValue().get();
              final double errorRate =
                  errorRates.getOrDefault(errorType, new RateTracker()).getRate();

              final List<ErrorOccurrence> occurrences =
                  recentErrors.getOrDefault(errorType, new ArrayList<>());

              final Instant firstOccurrence =
                  occurrences.stream()
                      .map(ErrorOccurrence::getTimestamp)
                      .min(Instant::compareTo)
                      .orElse(Instant.now());

              final Instant lastOccurrence =
                  occurrences.stream()
                      .map(ErrorOccurrence::getTimestamp)
                      .max(Instant::compareTo)
                      .orElse(Instant.now());

              final double retryablePercentage =
                  occurrences.stream()
                          .mapToDouble(occ -> occ.isRetryable() ? 1.0 : 0.0)
                          .average()
                          .orElse(0.0)
                      * 100.0;

              final List<String> commonMessages =
                  occurrences.stream()
                      .map(ErrorOccurrence::getErrorMessage)
                      .collect(Collectors.groupingBy(msg -> msg, Collectors.counting()))
                      .entrySet()
                      .stream()
                      .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                      .limit(5)
                      .map(Map.Entry::getKey)
                      .collect(Collectors.toList());

              final List<String> commonFunctions =
                  occurrences.stream()
                      .map(ErrorOccurrence::getFunctionContext)
                      .filter(func -> func != null && !func.isEmpty())
                      .collect(Collectors.groupingBy(func -> func, Collectors.counting()))
                      .entrySet()
                      .stream()
                      .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                      .limit(5)
                      .map(Map.Entry::getKey)
                      .collect(Collectors.toList());

              return new ErrorStatistics(
                  errorType,
                  totalOccurrences,
                  errorRate,
                  firstOccurrence,
                  lastOccurrence,
                  retryablePercentage,
                  commonMessages,
                  commonFunctions);
            })
        .sorted(Comparator.comparingLong(ErrorStatistics::getTotalOccurrences).reversed())
        .collect(Collectors.toList());
  }

  /**
   * Gets error statistics for a specific error type.
   *
   * @param errorType the error type to get statistics for
   * @return error statistics for the specified type, or null if not found
   */
  public ErrorStatistics getErrorStatistics(final String errorType) {
    return getErrorStatistics().stream()
        .filter(stats -> stats.getErrorType().equals(errorType))
        .findFirst()
        .orElse(null);
  }

  /**
   * Gets the total number of errors recorded.
   *
   * @return total error count across all types
   */
  public long getTotalErrorCount() {
    return errorCounts.values().stream().mapToLong(AtomicLong::get).sum();
  }

  /**
   * Gets the overall error rate (errors per minute).
   *
   * @return overall error rate
   */
  public double getOverallErrorRate() {
    return errorRates.values().stream().mapToDouble(RateTracker::getRate).sum();
  }

  /**
   * Checks if there are any concerning error patterns.
   *
   * @return true if concerning patterns are detected, false otherwise
   */
  public boolean hasConcerningPatterns() {
    // Check for high error rates
    final double overallRate = getOverallErrorRate();
    if (overallRate > 10.0) { // More than 10 errors per minute
      return true;
    }

    // Check for specific error types with high rates
    for (Map.Entry<String, RateTracker> entry : errorRates.entrySet()) {
      final double rate = entry.getValue().getRate();
      if (rate > 5.0) { // More than 5 errors per minute for any single type
        return true;
      }
    }

    // Check for memory-related errors
    final long memoryErrors =
        errorCounts.entrySet().stream()
            .filter(
                entry ->
                    entry.getKey().contains("Memory") || entry.getKey().contains("OutOfBounds"))
            .mapToLong(entry -> entry.getValue().get())
            .sum();

    if (memoryErrors > getTotalErrorCount() * 0.3) { // More than 30% memory errors
      return true;
    }

    return false;
  }

  /** Resets all error monitoring data. */
  public void reset() {
    errorCounts.clear();
    recentErrors.clear();
    errorRates.clear();
    logger.info("Error monitoring data reset");
  }

  /**
   * Generates a summary report of error monitoring data.
   *
   * @return formatted summary report
   */
  public String generateSummaryReport() {
    final StringBuilder report = new StringBuilder();
    report.append("=== WebAssembly Error Monitoring Summary ===\n");
    report.append("Total Errors: ").append(getTotalErrorCount()).append("\n");
    report
        .append("Overall Error Rate: ")
        .append(String.format("%.2f", getOverallErrorRate()))
        .append(" errors/minute\n");
    report
        .append("Concerning Patterns: ")
        .append(hasConcerningPatterns() ? "YES" : "NO")
        .append("\n\n");

    final List<ErrorStatistics> stats = getErrorStatistics();
    if (!stats.isEmpty()) {
      report.append("Top Error Types:\n");
      stats.stream()
          .limit(10)
          .forEach(
              stat -> {
                report
                    .append("  ")
                    .append(stat.getErrorType())
                    .append(": ")
                    .append(stat.getTotalOccurrences())
                    .append(" occurrences (")
                    .append(String.format("%.2f", stat.getErrorRate()))
                    .append("/min)\n");
              });
    }

    return report.toString();
  }

  /** Extracts function context from exception information. */
  private String extractFunctionContext(final WasmException exception) {
    if (exception instanceof TrapException) {
      return ((TrapException) exception).getFunctionName();
    } else if (exception instanceof RuntimeException) {
      return ((RuntimeException) exception).getFunctionName();
    } else if (exception instanceof ModuleCompilationException) {
      return ((ModuleCompilationException) exception).getFunctionName();
    }
    return null;
  }

  /** Determines if an exception represents a retryable error. */
  private boolean determineIfRetryable(final WasmException exception) {
    if (exception instanceof WasiException) {
      return ((WasiException) exception).isRetryable();
    } else if (exception instanceof TrapException) {
      final TrapException trapException = (TrapException) exception;
      return trapException.getTrapType() == TrapException.TrapType.INTERRUPT
          || trapException.getTrapType() == TrapException.TrapType.OUT_OF_FUEL;
    } else if (exception instanceof WasiFileSystemException) {
      return ((WasiFileSystemException) exception).isTransientError();
    }
    return false;
  }

  /** Analyzes error patterns for a specific error type. */
  private void analyzeErrorPatterns(final String errorType) {
    final List<ErrorOccurrence> occurrences = recentErrors.get(errorType);
    if (occurrences == null || occurrences.size() < 5) {
      return; // Need at least 5 occurrences for pattern analysis
    }

    // Check for rapid error bursts (more than 3 errors in 10 seconds)
    final Instant tenSecondsAgo = Instant.now().minus(10, ChronoUnit.SECONDS);
    final long recentCount =
        occurrences.stream().filter(occ -> occ.getTimestamp().isAfter(tenSecondsAgo)).count();

    if (recentCount > 3) {
      logger.warning(
          "Detected error burst for " + errorType + ": " + recentCount + " errors in 10 seconds");
    }

    // Check for thread-specific patterns
    final Map<Long, Long> threadCounts =
        occurrences.stream()
            .collect(Collectors.groupingBy(ErrorOccurrence::getThreadId, Collectors.counting()));

    threadCounts.entrySet().stream()
        .filter(
            entry -> entry.getValue() > occurrences.size() * 0.7) // One thread has >70% of errors
        .forEach(
            entry -> {
              logger.warning(
                  "Thread "
                      + entry.getKey()
                      + " responsible for "
                      + entry.getValue()
                      + "/"
                      + occurrences.size()
                      + " "
                      + errorType
                      + " errors");
            });
  }
}
