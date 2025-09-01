package ai.tegmentum.wasmtime4j.webassembly;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

/**
 * Test runner that executes the same test against both JNI and Panama implementations. Provides
 * infrastructure for cross-runtime validation and comparison.
 */
public final class CrossRuntimeTestRunner {
  private static final Logger LOGGER = Logger.getLogger(CrossRuntimeTestRunner.class.getName());

  // Runtime management
  private static final ConcurrentMap<RuntimeType, WasmRuntime> runtimeCache =
      new ConcurrentHashMap<>();
  private static final Object RUNTIME_LOCK = new Object();

  // Test execution tracking
  private static final ConcurrentMap<String, CrossRuntimeTestResult> testResults =
      new ConcurrentHashMap<>();

  private CrossRuntimeTestRunner() {
    // Utility class - prevent instantiation
  }

  /**
   * Executes a test function against both JNI and Panama runtimes.
   *
   * @param testName the name of the test
   * @param testFunction the test function to execute
   * @return the cross-runtime test result
   */
  public static CrossRuntimeTestResult executeAcrossRuntimes(
      final String testName, final RuntimeTestFunction testFunction) {
    Objects.requireNonNull(testName, "testName cannot be null");
    Objects.requireNonNull(testFunction, "testFunction cannot be null");

    LOGGER.info("Executing cross-runtime test: " + testName);

    final CrossRuntimeTestResult.Builder resultBuilder =
        new CrossRuntimeTestResult.Builder(testName);

    // Test JNI runtime
    final RuntimeTestExecution jniExecution = executeWithRuntime(RuntimeType.JNI, testFunction);
    resultBuilder.jniResult(jniExecution);

    // Test Panama runtime if available
    RuntimeTestExecution panamaExecution = null;
    if (TestUtils.isPanamaAvailable()) {
      panamaExecution = executeWithRuntime(RuntimeType.PANAMA, testFunction);
      resultBuilder.panamaResult(panamaExecution);
    } else {
      LOGGER.info("Skipping Panama test - not available on Java " + TestUtils.getJavaVersion());
      resultBuilder.panamaResult(
          RuntimeTestExecution.skipped(RuntimeType.PANAMA, "Panama not available"));
    }

    final CrossRuntimeTestResult result = resultBuilder.build();
    testResults.put(testName, result);

    LOGGER.info("Cross-runtime test completed: " + testName + " - " + result.getSummary());
    return result;
  }

  /**
   * Executes a test function with a specific runtime.
   *
   * @param runtimeType the runtime type
   * @param testFunction the test function
   * @return the execution result
   */
  private static RuntimeTestExecution executeWithRuntime(
      final RuntimeType runtimeType, final RuntimeTestFunction testFunction) {
    final Instant startTime = Instant.now();

    try {
      final WasmRuntime runtime = getRuntimeInstance(runtimeType);
      final Object result = testFunction.execute(runtime);
      final Duration duration = Duration.between(startTime, Instant.now());

      return RuntimeTestExecution.successful(runtimeType, result, duration);

    } catch (final Exception e) {
      final Duration duration = Duration.between(startTime, Instant.now());
      LOGGER.warning("Test failed with " + runtimeType + ": " + e.getMessage());
      return RuntimeTestExecution.failed(runtimeType, e, duration);
    }
  }

  /**
   * Gets a cached runtime instance for the specified type.
   *
   * @param runtimeType the runtime type
   * @return the runtime instance
   * @throws RuntimeException if runtime creation fails
   */
  private static WasmRuntime getRuntimeInstance(final RuntimeType runtimeType) {
    WasmRuntime runtime = runtimeCache.get(runtimeType);
    if (runtime != null) {
      return runtime;
    }

    synchronized (RUNTIME_LOCK) {
      runtime = runtimeCache.get(runtimeType);
      if (runtime == null) {
        LOGGER.info("Creating runtime instance for " + runtimeType);

        // Override runtime selection for testing
        final String originalProperty = System.getProperty("wasmtime4j.runtime");
        try {
          System.setProperty("wasmtime4j.runtime", runtimeType.name().toLowerCase());
          runtime = WasmRuntimeFactory.create();
          runtimeCache.put(runtimeType, runtime);
        } catch (final ai.tegmentum.wasmtime4j.exception.WasmException e) {
          throw new RuntimeException("Failed to create runtime for " + runtimeType, e);
        } finally {
          // Restore original property
          if (originalProperty != null) {
            System.setProperty("wasmtime4j.runtime", originalProperty);
          } else {
            System.clearProperty("wasmtime4j.runtime");
          }
        }
      }
    }

    return runtime;
  }

  /**
   * Validates that both runtimes produce the same result for a test.
   *
   * @param testName the test name
   * @param testFunction the test function
   * @param resultComparator function to compare results
   * @return validation result
   */
  public static CrossRuntimeValidationResult validateConsistency(
      final String testName,
      final RuntimeTestFunction testFunction,
      final Function<RuntimeTestComparison, Boolean> resultComparator) {
    final CrossRuntimeTestResult testResult = executeAcrossRuntimes(testName, testFunction);

    final CrossRuntimeValidationResult.Builder validationBuilder =
        new CrossRuntimeValidationResult.Builder(testName);

    if (testResult.bothSuccessful()) {
      final RuntimeTestComparison comparison =
          new RuntimeTestComparison(testResult.getJniExecution(), testResult.getPanamaExecution());

      final boolean consistent = resultComparator.apply(comparison);
      validationBuilder.consistent(consistent);

      if (consistent) {
        validationBuilder.addInfo("Both runtimes produced consistent results");
      } else {
        validationBuilder.addError("Runtimes produced inconsistent results");
        validationBuilder.addError("JNI result: " + testResult.getJniExecution().getResult());
        validationBuilder.addError("Panama result: " + testResult.getPanamaExecution().getResult());
      }
    } else {
      validationBuilder.consistent(false);
      validationBuilder.addError("Not both runtimes executed successfully");

      if (!testResult.getJniExecution().isSuccessful()) {
        validationBuilder.addError("JNI failed: " + testResult.getJniExecution().getException());
      }

      if (testResult.getPanamaExecution() != null && !testResult.getPanamaExecution().isSuccessful()) {
        validationBuilder.addError("Panama failed: " + testResult.getPanamaExecution().getException());
      }
    }

    return validationBuilder.build();
  }

  /**
   * Gets all test results for reporting.
   *
   * @return map of test results
   */
  public static ConcurrentMap<String, CrossRuntimeTestResult> getAllTestResults() {
    return new ConcurrentHashMap<>(testResults);
  }

  /** Clears all cached runtimes and test results. */
  public static void clearCache() {
    synchronized (RUNTIME_LOCK) {
      // Close runtime instances
      runtimeCache
          .values()
          .forEach(
              runtime -> {
                try {
                  runtime.close();
                } catch (final Exception e) {
                  LOGGER.warning("Failed to close runtime: " + e.getMessage());
                }
          });

      runtimeCache.clear();
      testResults.clear();
      LOGGER.info("Cleared all cached runtimes and test results");
    }
  }

  /** Functional interface for runtime tests. */
  @FunctionalInterface
  public interface RuntimeTestFunction {
    /**
     * Executes the test with the given runtime.
     *
     * @param runtime the WebAssembly runtime
     * @return the test result
     * @throws Exception if the test fails
     */
    Object execute(WasmRuntime runtime) throws Exception;
  }

  /** JUnit 5 ArgumentsProvider for parameterized tests across runtimes. */
  public static final class RuntimeArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
      final List<Arguments> arguments = new ArrayList<>();

      // Always include JNI
      arguments.add(Arguments.of(RuntimeType.JNI));

      // Include Panama if available
      if (TestUtils.isPanamaAvailable()) {
        arguments.add(Arguments.of(RuntimeType.PANAMA));
      }

      return arguments.stream();
    }
  }

  /** JUnit 5 ArgumentsProvider for test cases with runtime combinations. */
  public static final class TestCaseRuntimeProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(final ExtensionContext context)
        throws IOException {
      final List<Arguments> arguments = new ArrayList<>();

      // Load test cases
      final WasmTestDataManager dataManager = WasmTestDataManager.getInstance();
      final List<WasmTestCase> customTests =
          dataManager.loadTestSuite(WasmTestSuiteLoader.TestSuiteType.CUSTOM_TESTS);

      // Create arguments for each test case and runtime combination
      for (final WasmTestCase testCase : customTests) {
        // JNI runtime
        arguments.add(Arguments.of(testCase, RuntimeType.JNI));

        // Panama runtime if available
        if (TestUtils.isPanamaAvailable()) {
          arguments.add(Arguments.of(testCase, RuntimeType.PANAMA));
        }
      }

      return arguments.stream();
    }
  }

  /**
   * Creates a test execution summary for reporting.
   *
   * @return execution summary
   */
  public static CrossRuntimeExecutionSummary createExecutionSummary() {
    final CrossRuntimeExecutionSummary.Builder summaryBuilder =
        new CrossRuntimeExecutionSummary.Builder();

    for (final CrossRuntimeTestResult result : testResults.values()) {
      summaryBuilder.addTestResult(result);
    }

    return summaryBuilder.build();
  }
}
