package ai.tegmentum.wasmtime4j.utils;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Utility class for cross-runtime validation testing. Provides automated testing capabilities to
 * ensure JNI and Panama implementations behave identically.
 */
public final class CrossRuntimeValidator {
  private static final Logger LOGGER = Logger.getLogger(CrossRuntimeValidator.class.getName());

  private CrossRuntimeValidator() {
    // Utility class - prevent instantiation
  }

  /** Test result containing execution data for comparison. */
  public static final class TestResult {
    private final RuntimeType runtimeType;
    private final Object result;
    private final Duration executionTime;
    private final Exception exception;

    private TestResult(
        final RuntimeType runtimeType,
        final Object result,
        final Duration executionTime,
        final Exception exception) {
      this.runtimeType = runtimeType;
      this.result = result;
      this.executionTime = executionTime;
      this.exception = exception;
    }

    public RuntimeType getRuntimeType() {
      return runtimeType;
    }

    public Object getResult() {
      return result;
    }

    public Duration getExecutionTime() {
      return executionTime;
    }

    public Exception getException() {
      return exception;
    }

    public boolean isSuccess() {
      return exception == null;
    }

    public boolean hasException() {
      return exception != null;
    }
  }

  /** Comparison result containing validation outcomes. */
  public static final class ComparisonResult {
    private final List<TestResult> results;
    private final boolean identicalResults;
    private final boolean identicalExceptions;
    private final String differenceDescription;

    private ComparisonResult(
        final List<TestResult> results,
        final boolean identicalResults,
        final boolean identicalExceptions,
        final String differenceDescription) {
      this.results = new ArrayList<>(results);
      this.identicalResults = identicalResults;
      this.identicalExceptions = identicalExceptions;
      this.differenceDescription = differenceDescription;
    }

    public List<TestResult> getResults() {
      return new ArrayList<>(results);
    }

    public boolean areResultsIdentical() {
      return identicalResults;
    }

    public boolean areExceptionsIdentical() {
      return identicalExceptions;
    }

    public String getDifferenceDescription() {
      return differenceDescription;
    }

    public boolean isValid() {
      return identicalResults && identicalExceptions;
    }
  }

  /** Functional interface for runtime-specific test operations. */
  @FunctionalInterface
  public interface RuntimeOperation<T> {
    T execute(WasmRuntime runtime) throws Exception;
  }

  /**
   * Validates that both JNI and Panama runtimes produce identical results.
   *
   * @param operation the operation to test with both runtimes
   * @param <T> the result type
   * @return comparison result with validation outcome
   */
  public static <T> ComparisonResult validateCrossRuntime(final RuntimeOperation<T> operation) {
    return validateCrossRuntime(operation, Duration.ofMinutes(1));
  }

  /**
   * Validates that both JNI and Panama runtimes produce identical results with timeout.
   *
   * @param operation the operation to test with both runtimes
   * @param timeout maximum execution time per runtime
   * @param <T> the result type
   * @return comparison result with validation outcome
   */
  public static <T> ComparisonResult validateCrossRuntime(
      final RuntimeOperation<T> operation, final Duration timeout) {
    final List<TestResult> results = new ArrayList<>();

    // Execute with JNI runtime
    final TestResult jniResult = executeWithRuntime(RuntimeType.JNI, operation, timeout);
    results.add(jniResult);

    // Execute with Panama runtime if available
    TestResult panamaResult = null;
    if (TestUtils.isPanamaAvailable()) {
      panamaResult = executeWithRuntime(RuntimeType.PANAMA, operation, timeout);
      results.add(panamaResult);
    } else {
      LOGGER.warning("Panama runtime not available, skipping cross-runtime validation");
      return new ComparisonResult(results, true, true, "Panama not available");
    }

    // Compare results
    return compareResults(jniResult, panamaResult);
  }

  /**
   * Executes an operation with a specific runtime and measures performance.
   *
   * @param runtimeType the runtime type to use
   * @param operation the operation to execute
   * @param timeout maximum execution time
   * @param <T> the result type
   * @return test result with execution data
   */
  private static <T> TestResult executeWithRuntime(
      final RuntimeType runtimeType, final RuntimeOperation<T> operation, final Duration timeout) {
    LOGGER.info("Executing operation with " + runtimeType + " runtime");

    final CompletableFuture<TestResult> future =
        CompletableFuture.supplyAsync(
            () -> {
              final Instant startTime = Instant.now();
              try (final WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
                final T result = operation.execute(runtime);
                final Duration executionTime = Duration.between(startTime, Instant.now());
                LOGGER.info(
                    runtimeType + " execution completed in " + executionTime.toMillis() + "ms");
                return new TestResult(runtimeType, result, executionTime, null);
              } catch (final Exception e) {
                final Duration executionTime = Duration.between(startTime, Instant.now());
                LOGGER.warning(runtimeType + " execution failed: " + e.getMessage());
                return new TestResult(runtimeType, null, executionTime, e);
              }
            });

    try {
      return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (final TimeoutException e) {
      future.cancel(true);
      final String message =
          runtimeType + " operation timed out after " + timeout.toMillis() + "ms";
      LOGGER.severe(message);
      return new TestResult(runtimeType, null, timeout, new TimeoutException(message));
    } catch (final InterruptedException | ExecutionException e) {
      final String message = runtimeType + " operation failed: " + e.getMessage();
      LOGGER.severe(message);
      return new TestResult(runtimeType, null, Duration.ZERO, new RuntimeException(message, e));
    }
  }

  /**
   * Compares two test results for equality.
   *
   * @param jniResult the JNI test result
   * @param panamaResult the Panama test result
   * @return comparison result
   */
  private static ComparisonResult compareResults(
      final TestResult jniResult, final TestResult panamaResult) {
    final List<TestResult> results = List.of(jniResult, panamaResult);

    // Compare exceptions first
    final boolean identicalExceptions =
        compareExceptions(jniResult.getException(), panamaResult.getException());

    // If both failed with exceptions, check if exceptions are equivalent
    if (jniResult.hasException() && panamaResult.hasException()) {
      final String differenceDescription =
          identicalExceptions
              ? "Both runtimes failed with equivalent exceptions"
              : describeExceptionDifferences(jniResult.getException(), panamaResult.getException());

      return new ComparisonResult(
          results, identicalExceptions, identicalExceptions, differenceDescription);
    }

    // If one succeeded and one failed, that's a difference
    if (jniResult.isSuccess() != panamaResult.isSuccess()) {
      final String differenceDescription =
          String.format(
              "Result mismatch: %s %s, %s %s",
              RuntimeType.JNI,
              jniResult.isSuccess() ? "succeeded" : "failed",
              RuntimeType.PANAMA,
              panamaResult.isSuccess() ? "succeeded" : "failed");

      return new ComparisonResult(results, false, false, differenceDescription);
    }

    // Both succeeded, compare results
    final boolean identicalResults =
        compareObjects(jniResult.getResult(), panamaResult.getResult());
    final String differenceDescription =
        identicalResults
            ? "Results are identical"
            : describeResultDifferences(jniResult.getResult(), panamaResult.getResult());

    return new ComparisonResult(results, identicalResults, true, differenceDescription);
  }

  /**
   * Compares two exceptions for equivalence.
   *
   * @param e1 first exception (may be null)
   * @param e2 second exception (may be null)
   * @return true if exceptions are equivalent
   */
  private static boolean compareExceptions(final Exception e1, final Exception e2) {
    if (e1 == null && e2 == null) {
      return true;
    }
    if (e1 == null || e2 == null) {
      return false;
    }

    // Compare exception types and messages
    return e1.getClass().equals(e2.getClass()) && Objects.equals(e1.getMessage(), e2.getMessage());
  }

  /**
   * Compares two objects for equality, handling various types appropriately.
   *
   * @param obj1 first object
   * @param obj2 second object
   * @return true if objects are considered equal
   */
  private static boolean compareObjects(final Object obj1, final Object obj2) {
    if (obj1 == null && obj2 == null) {
      return true;
    }
    if (obj1 == null || obj2 == null) {
      return false;
    }

    // Handle byte arrays specially
    if (obj1 instanceof byte[] && obj2 instanceof byte[]) {
      return java.util.Arrays.equals((byte[]) obj1, (byte[]) obj2);
    }

    // Handle arrays generally
    if (obj1.getClass().isArray() && obj2.getClass().isArray()) {
      return java.util.Arrays.deepEquals(new Object[] {obj1}, new Object[] {obj2});
    }

    // Standard equality check
    return Objects.equals(obj1, obj2);
  }

  /**
   * Describes differences between two exceptions.
   *
   * @param e1 first exception
   * @param e2 second exception
   * @return description of differences
   */
  private static String describeExceptionDifferences(final Exception e1, final Exception e2) {
    if (e1 == null) {
      return "JNI succeeded, Panama failed with: " + e2.getClass().getSimpleName();
    }
    if (e2 == null) {
      return "Panama succeeded, JNI failed with: " + e1.getClass().getSimpleName();
    }

    final StringBuilder diff = new StringBuilder();
    diff.append("Exception differences: ");

    if (!e1.getClass().equals(e2.getClass())) {
      diff.append("types differ (")
          .append(e1.getClass().getSimpleName())
          .append(" vs ")
          .append(e2.getClass().getSimpleName())
          .append(") ");
    }

    if (!Objects.equals(e1.getMessage(), e2.getMessage())) {
      diff.append("messages differ ('")
          .append(e1.getMessage())
          .append("' vs '")
          .append(e2.getMessage())
          .append("')");
    }

    return diff.toString();
  }

  /**
   * Describes differences between two result objects.
   *
   * @param result1 first result
   * @param result2 second result
   * @return description of differences
   */
  private static String describeResultDifferences(final Object result1, final Object result2) {
    if (result1 == null) {
      return "JNI returned null, Panama returned: " + result2;
    }
    if (result2 == null) {
      return "Panama returned null, JNI returned: " + result1;
    }

    if (!result1.getClass().equals(result2.getClass())) {
      return String.format(
          "Type mismatch: %s vs %s",
          result1.getClass().getSimpleName(), result2.getClass().getSimpleName());
    }

    if (result1 instanceof byte[] && result2 instanceof byte[]) {
      final byte[] bytes1 = (byte[]) result1;
      final byte[] bytes2 = (byte[]) result2;
      return String.format("Byte array length mismatch: %d vs %d", bytes1.length, bytes2.length);
    }

    return String.format("Value mismatch: '%s' vs '%s'", result1, result2);
  }

  /**
   * Validates multiple operations in parallel for performance comparison.
   *
   * @param operations list of operations to validate
   * @return list of comparison results
   */
  public static List<ComparisonResult> validateMultiple(
      final List<RuntimeOperation<Object>> operations) {
    return validateMultiple(operations, Duration.ofMinutes(1));
  }

  /**
   * Validates multiple operations in parallel with timeout.
   *
   * @param operations list of operations to validate
   * @param timeout maximum execution time per operation per runtime
   * @return list of comparison results
   */
  public static List<ComparisonResult> validateMultiple(
      final List<RuntimeOperation<Object>> operations, final Duration timeout) {
    LOGGER.info("Validating " + operations.size() + " operations across runtimes");

    final List<CompletableFuture<ComparisonResult>> futures = new ArrayList<>();

    for (final RuntimeOperation<Object> operation : operations) {
      final CompletableFuture<ComparisonResult> future =
          CompletableFuture.supplyAsync(() -> validateCrossRuntime(operation, timeout));
      futures.add(future);
    }

    // Wait for all validations to complete
    final List<ComparisonResult> results = new ArrayList<>();
    for (final CompletableFuture<ComparisonResult> future : futures) {
      try {
        results.add(future.get());
      } catch (final InterruptedException | ExecutionException e) {
        LOGGER.severe("Validation failed: " + e.getMessage());
        // Create a failed result
        final List<TestResult> failedResults =
            List.of(
                new TestResult(RuntimeType.JNI, null, Duration.ZERO, new RuntimeException(e)),
                new TestResult(RuntimeType.PANAMA, null, Duration.ZERO, new RuntimeException(e)));
        results.add(
            new ComparisonResult(
                failedResults, false, false, "Validation execution failed: " + e.getMessage()));
      }
    }

    LOGGER.info("Completed validation of " + results.size() + " operations");
    return results;
  }

  /**
   * Analyzes performance differences between runtimes.
   *
   * @param results comparison results to analyze
   * @return performance analysis report
   */
  public static String analyzePerformance(final List<ComparisonResult> results) {
    final StringBuilder report = new StringBuilder();
    report.append("Cross-Runtime Performance Analysis\n");
    report.append("================================\n\n");

    long totalJniTime = 0;
    long totalPanamaTime = 0;
    int validComparisons = 0;

    for (int i = 0; i < results.size(); i++) {
      final ComparisonResult result = results.get(i);
      final List<TestResult> testResults = result.getResults();

      if (testResults.size() == 2) {
        final TestResult jniResult = testResults.get(0);
        final TestResult panamaResult = testResults.get(1);

        if (jniResult.isSuccess() && panamaResult.isSuccess()) {
          final long jniMs = jniResult.getExecutionTime().toMillis();
          final long panamaMs = panamaResult.getExecutionTime().toMillis();

          totalJniTime += jniMs;
          totalPanamaTime += panamaMs;
          validComparisons++;

          report.append(
              String.format("Operation %d: JNI=%dms, Panama=%dms", i + 1, jniMs, panamaMs));

          if (jniMs != panamaMs) {
            final double ratio = (double) panamaMs / jniMs;
            if (ratio > 1.1) {
              report.append(String.format(" (Panama %.1fx slower)", ratio));
            } else if (ratio < 0.9) {
              report.append(String.format(" (Panama %.1fx faster)", 1.0 / ratio));
            }
          }
          report.append("\n");
        } else {
          report.append(
              String.format(
                  "Operation %d: Failed (%s)\n", i + 1, result.getDifferenceDescription()));
        }
      }
    }

    if (validComparisons > 0) {
      report.append(
          String.format(
              "\nSummary: JNI total=%dms, Panama total=%dms\n", totalJniTime, totalPanamaTime));

      final double avgRatio = (double) totalPanamaTime / totalJniTime;
      if (avgRatio > 1.1) {
        report.append(String.format("Overall: Panama %.1fx slower than JNI\n", avgRatio));
      } else if (avgRatio < 0.9) {
        report.append(String.format("Overall: Panama %.1fx faster than JNI\n", 1.0 / avgRatio));
      } else {
        report.append("Overall: Performance is similar\n");
      }
    }

    return report.toString();
  }
}
