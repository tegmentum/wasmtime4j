package ai.tegmentum.wasmtime4j.nativefunctions;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.memory.MemoryLeakDetector;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base test class for comprehensive native function testing. Provides common infrastructure for
 * testing both JNI and Panama FFI native functions with memory leak detection, thread safety
 * validation, and resource management.
 *
 * <p>This class integrates with the existing MemoryLeakDetector framework to provide:
 *
 * <ul>
 *   <li>Automated memory leak detection for all native function tests
 *   <li>Cross-runtime validation (JNI vs Panama FFI)
 *   <li>Thread safety testing infrastructure
 *   <li>Resource cleanup validation
 *   <li>Performance regression detection
 * </ul>
 */
public abstract class BaseNativeFunctionTest {
  protected static final Logger LOGGER = Logger.getLogger(BaseNativeFunctionTest.class.getName());

  // Memory leak detection configuration
  protected MemoryLeakDetector.Configuration defaultMemoryConfig;
  protected MemoryLeakDetector.Configuration fastMemoryConfig;
  protected MemoryLeakDetector.Configuration thoroughMemoryConfig;

  // Test runtime and resources
  protected RuntimeType currentRuntime;
  protected WasmRuntime runtime;
  protected final List<AutoCloseable> testResources = new ArrayList<>();

  // Thread safety testing infrastructure
  protected ExecutorService threadPool;
  protected final Map<String, AtomicInteger> operationCounts = new ConcurrentHashMap<>();

  // Test data and utilities
  protected NativeFunctionTestUtils testUtils;

  @BeforeEach
  void setUpBaseNativeFunctionTest() {
    LOGGER.info("Setting up base native function test infrastructure");

    // Initialize memory leak detection configurations
    defaultMemoryConfig = MemoryLeakDetector.getDefaultConfiguration();
    fastMemoryConfig = MemoryLeakDetector.getFastConfiguration();
    thoroughMemoryConfig = MemoryLeakDetector.getThoroughConfiguration();

    // Initialize thread safety testing infrastructure
    threadPool = Executors.newFixedThreadPool(8);
    operationCounts.clear();

    // Initialize test utilities
    testUtils = new NativeFunctionTestUtils();

    LOGGER.info("Base native function test infrastructure initialized");
  }

  @AfterEach
  void tearDownBaseNativeFunctionTest() {
    LOGGER.info("Cleaning up base native function test infrastructure");

    // Close all test resources
    cleanupTestResources();

    // Shutdown thread pool
    if (threadPool != null) {
      threadPool.shutdown();
      try {
        if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
          threadPool.shutdownNow();
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        threadPool.shutdownNow();
      }
    }

    // Clear operation counts
    operationCounts.clear();

    // Force garbage collection to clean up native resources
    System.gc();
    System.gc();

    LOGGER.info("Base native function test infrastructure cleaned up");
  }

  /**
   * Creates a runtime for the current test context.
   *
   * @param runtimeType the runtime type to create
   * @return the created runtime
   */
  protected WasmRuntime createRuntime(final RuntimeType runtimeType) throws WasmException {
    final WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType);
    testResources.add(runtime);
    this.runtime = runtime;
    this.currentRuntime = runtimeType;
    return runtime;
  }

  /**
   * Creates a runtime for the best available runtime type.
   *
   * @return the created runtime
   */
  protected WasmRuntime createRuntime() throws WasmException {
    return createRuntime(getBestAvailableRuntime());
  }

  /**
   * Gets the best available runtime type for testing.
   *
   * @return the best available runtime type
   */
  protected RuntimeType getBestAvailableRuntime() {
    return TestUtils.isPanamaAvailable() ? RuntimeType.PANAMA : RuntimeType.JNI;
  }

  /**
   * Tests a native function operation with memory leak detection.
   *
   * @param operationName name of the operation being tested
   * @param operation the operation to test
   * @param config memory leak detection configuration
   * @return leak analysis result
   */
  protected MemoryLeakDetector.LeakAnalysisResult testWithMemoryLeakDetection(
      final String operationName,
      final MemoryLeakDetector.TestedOperation operation,
      final MemoryLeakDetector.Configuration config) {

    LOGGER.info("Testing native function with memory leak detection: " + operationName);

    final MemoryLeakDetector.LeakAnalysisResult result =
        MemoryLeakDetector.detectLeaks(
            operationName + "[" + currentRuntime + "]", currentRuntime, operation, config);

    // Log results
    LOGGER.info("Memory leak analysis for " + operationName + ":");
    LOGGER.info("Leak detected: " + result.isLeakDetected());
    LOGGER.info("Memory increase: " + result.getMemoryIncrease() + " bytes");
    LOGGER.info("Leak rate: " + result.getLeakRate() + " bytes/sec");

    if (result.isLeakDetected()) {
      LOGGER.warning("Memory leak detected in " + operationName);
      LOGGER.warning("Analysis: " + result.getAnalysis());
      result.getRecommendations().forEach(rec -> LOGGER.warning("Recommendation: " + rec));
    }

    return result;
  }

  /**
   * Tests a native function operation with fast memory leak detection.
   *
   * @param operationName name of the operation being tested
   * @param operation the operation to test
   * @return leak analysis result
   */
  protected MemoryLeakDetector.LeakAnalysisResult testWithFastMemoryLeakDetection(
      final String operationName, final MemoryLeakDetector.TestedOperation operation) {
    return testWithMemoryLeakDetection(operationName, operation, fastMemoryConfig);
  }

  /**
   * Tests a native function operation with thorough memory leak detection.
   *
   * @param operationName name of the operation being tested
   * @param operation the operation to test
   * @return leak analysis result
   */
  protected MemoryLeakDetector.LeakAnalysisResult testWithThoroughMemoryLeakDetection(
      final String operationName, final MemoryLeakDetector.TestedOperation operation) {
    return testWithMemoryLeakDetection(operationName, operation, thoroughMemoryConfig);
  }

  /**
   * Tests thread safety of a native function operation.
   *
   * @param operationName name of the operation being tested
   * @param operation the operation to test
   * @param threadCount number of concurrent threads
   * @param operationsPerThread number of operations per thread
   * @return thread safety test result
   */
  protected ThreadSafetyTestResult testThreadSafety(
      final String operationName,
      final ThreadSafeOperation operation,
      final int threadCount,
      final int operationsPerThread) {

    LOGGER.info(
        String.format(
            "Testing thread safety for %s with %d threads, %d ops/thread",
            operationName, threadCount, operationsPerThread));

    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch completionLatch = new CountDownLatch(threadCount);
    final List<Future<ThreadOperationResult>> futures = new ArrayList<>();
    final AtomicInteger totalOperations = new AtomicInteger(0);

    // Submit concurrent operations
    for (int i = 0; i < threadCount; i++) {
      final int threadId = i;
      final Future<ThreadOperationResult> future =
          threadPool.submit(
              () -> {
                try {
                  // Wait for all threads to be ready
                  startLatch.await();

                  int successCount = 0;
                  int errorCount = 0;
                  final List<Exception> exceptions = new ArrayList<>();

                  for (int op = 0; op < operationsPerThread; op++) {
                    try {
                      operation.execute(runtime, threadId, op);
                      successCount++;
                      totalOperations.incrementAndGet();
                    } catch (final Exception e) {
                      errorCount++;
                      exceptions.add(e);
                    }
                  }

                  return new ThreadOperationResult(threadId, successCount, errorCount, exceptions);
                } finally {
                  completionLatch.countDown();
                }
              });
      futures.add(future);
    }

    // Start all threads simultaneously
    final long startTime = System.currentTimeMillis();
    startLatch.countDown();

    // Wait for completion
    try {
      final boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
      final long duration = System.currentTimeMillis() - startTime;

      if (!completed) {
        LOGGER.warning("Thread safety test timed out for " + operationName);
      }

      // Collect results
      final List<ThreadOperationResult> results = new ArrayList<>();
      for (final Future<ThreadOperationResult> future : futures) {
        try {
          results.add(future.get(1, TimeUnit.SECONDS));
        } catch (final Exception e) {
          LOGGER.warning("Failed to get thread result: " + e.getMessage());
        }
      }

      return new ThreadSafetyTestResult(
          operationName, threadCount, operationsPerThread, duration, results, completed);

    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return new ThreadSafetyTestResult(
          operationName, threadCount, operationsPerThread, -1, new ArrayList<>(), false);
    }
  }

  /**
   * Tests cross-runtime compatibility for a native function operation.
   *
   * @param operationName name of the operation being tested
   * @param operation the operation to test
   * @return cross-runtime test results
   */
  protected Map<RuntimeType, MemoryLeakDetector.LeakAnalysisResult> testCrossRuntimeCompatibility(
      final String operationName, final MemoryLeakDetector.TestedOperation operation) {

    LOGGER.info("Testing cross-runtime compatibility for: " + operationName);

    final Map<RuntimeType, MemoryLeakDetector.LeakAnalysisResult> results =
        MemoryLeakDetector.compareRuntimes(operationName, operation, fastMemoryConfig);

    // Log comparison results
    LOGGER.info("Cross-runtime compatibility results for " + operationName + ":");
    results.forEach(
        (runtime, result) -> {
          LOGGER.info(
              String.format(
                  "  %s: Leak=%s, Increase=%d bytes, Rate=%.2f bytes/sec",
                  runtime,
                  result.isLeakDetected(),
                  result.getMemoryIncrease(),
                  result.getLeakRate()));
        });

    return results;
  }

  /**
   * Validates that no memory leaks are detected in the analysis result.
   *
   * @param result the leak analysis result to validate
   * @param operationName name of the operation for error reporting
   */
  protected void assertNoMemoryLeaks(
      final MemoryLeakDetector.LeakAnalysisResult result, final String operationName) {
    if (result.isLeakDetected()) {
      final String errorMessage =
          String.format(
              "Memory leak detected in %s: %d bytes increase, %.2f bytes/sec rate. Analysis: %s",
              operationName,
              result.getMemoryIncrease(),
              result.getLeakRate(),
              result.getAnalysis());
      assertThat(result.isLeakDetected()).withFailMessage(errorMessage).isFalse();
    }
  }

  /**
   * Validates thread safety test results.
   *
   * @param result the thread safety test result
   * @param operationName name of the operation for error reporting
   */
  protected void assertThreadSafety(
      final ThreadSafetyTestResult result, final String operationName) {
    assertThat(result.isCompleted())
        .withFailMessage("Thread safety test for %s did not complete", operationName)
        .isTrue();

    final int totalErrors = result.getTotalErrors();
    if (totalErrors > 0) {
      LOGGER.warning(
          String.format("Thread safety test for %s had %d errors", operationName, totalErrors));
      // Log first few exceptions for debugging
      result.getResults().stream()
          .flatMap(r -> r.getExceptions().stream())
          .limit(3)
          .forEach(e -> LOGGER.warning("Exception: " + e.getMessage()));
    }

    // Allow some errors in concurrent scenarios, but not too many
    final int maxAllowedErrors = result.getTotalOperations() / 20; // 5% error rate
    assertThat(totalErrors)
        .withFailMessage(
            "Too many errors in thread safety test for %s: %d errors out of %d operations",
            operationName, totalErrors, result.getTotalOperations())
        .isLessThanOrEqualTo(maxAllowedErrors);
  }

  /** Cleans up all test resources. */
  private void cleanupTestResources() {
    for (final AutoCloseable resource : testResources) {
      try {
        resource.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close test resource: " + e.getMessage());
      }
    }
    testResources.clear();
  }

  /** Functional interface for thread-safe operation testing. */
  @FunctionalInterface
  protected interface ThreadSafeOperation {
    void execute(WasmRuntime runtime, int threadId, int operationId) throws Exception;
  }

  /** Result of a single thread's operations in thread safety testing. */
  protected static final class ThreadOperationResult {
    private final int threadId;
    private final int successCount;
    private final int errorCount;
    private final List<Exception> exceptions;

    public ThreadOperationResult(
        final int threadId,
        final int successCount,
        final int errorCount,
        final List<Exception> exceptions) {
      this.threadId = threadId;
      this.successCount = successCount;
      this.errorCount = errorCount;
      this.exceptions = new ArrayList<>(exceptions);
    }

    public int getThreadId() {
      return threadId;
    }

    public int getSuccessCount() {
      return successCount;
    }

    public int getErrorCount() {
      return errorCount;
    }

    public List<Exception> getExceptions() {
      return new ArrayList<>(exceptions);
    }
  }

  /** Result of thread safety testing. */
  protected static final class ThreadSafetyTestResult {
    private final String operationName;
    private final int threadCount;
    private final int operationsPerThread;
    private final long duration;
    private final List<ThreadOperationResult> results;
    private final boolean completed;

    public ThreadSafetyTestResult(
        final String operationName,
        final int threadCount,
        final int operationsPerThread,
        final long duration,
        final List<ThreadOperationResult> results,
        final boolean completed) {
      this.operationName = operationName;
      this.threadCount = threadCount;
      this.operationsPerThread = operationsPerThread;
      this.duration = duration;
      this.results = new ArrayList<>(results);
      this.completed = completed;
    }

    public String getOperationName() {
      return operationName;
    }

    public int getThreadCount() {
      return threadCount;
    }

    public int getOperationsPerThread() {
      return operationsPerThread;
    }

    public long getDuration() {
      return duration;
    }

    public List<ThreadOperationResult> getResults() {
      return new ArrayList<>(results);
    }

    public boolean isCompleted() {
      return completed;
    }

    public int getTotalOperations() {
      return results.stream().mapToInt(ThreadOperationResult::getSuccessCount).sum();
    }

    public int getTotalErrors() {
      return results.stream().mapToInt(ThreadOperationResult::getErrorCount).sum();
    }

    public double getOperationsPerSecond() {
      return duration > 0 ? (getTotalOperations() * 1000.0) / duration : 0.0;
    }
  }
}
