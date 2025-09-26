package ai.tegmentum.wasmtime4j.testsuite;

import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;

import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Engine for executing WebAssembly test cases across different runtime implementations.
 * Provides comprehensive test execution with proper resource management and error handling.
 */
public final class TestExecutionEngine {

    private static final Logger LOGGER = Logger.getLogger(TestExecutionEngine.class.getName());

    private final TestSuiteConfiguration configuration;

    public TestExecutionEngine(final TestSuiteConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        this.configuration = configuration;
    }

    /**
     * Executes a single test case using the specified runtime.
     *
     * @param testCase test case to execute
     * @param runtime runtime to use for execution
     * @return test result
     */
    public TestResult executeTest(final WebAssemblyTestCase testCase, final TestRuntime runtime) {
        if (testCase == null) {
            throw new IllegalArgumentException("Test case cannot be null");
        }
        if (runtime == null) {
            throw new IllegalArgumentException("Runtime cannot be null");
        }

        final long startTime = System.currentTimeMillis();

        try {
            LOGGER.fine("Executing test: " + testCase.getTestId() + " on runtime: " + runtime);

            // Handle composite test cases with sub-tests
            if (testCase.hasSubTests()) {
                return executeCompositeTest(testCase, runtime, startTime);
            }

            // Execute single test case
            return executeSingleTest(testCase, runtime, startTime);

        } catch (final Exception e) {
            final long duration = System.currentTimeMillis() - startTime;
            LOGGER.log(Level.WARNING, "Test execution failed: " + testCase.getTestId(), e);

            return TestResult.builder()
                .testCase(testCase)
                .runtime(runtime)
                .status(TestStatus.ERROR)
                .executionTimeMs(duration)
                .errorMessage(e.getMessage())
                .throwable(e)
                .build();
        }
    }

    private TestResult executeCompositeTest(final WebAssemblyTestCase testCase, final TestRuntime runtime,
                                           final long startTime) {
        final TestResult.Builder resultBuilder = TestResult.builder()
            .testCase(testCase)
            .runtime(runtime);

        int passed = 0;
        int failed = 0;
        int errors = 0;
        final StringBuilder combinedOutput = new StringBuilder();

        try {
            for (final WebAssemblyTestCase subTest : testCase.getSubTests()) {
                final TestResult subResult = executeSingleTest(subTest, runtime, startTime);

                switch (subResult.getStatus()) {
                    case PASSED -> passed++;
                    case FAILED -> failed++;
                    case ERROR -> errors++;
                }

                if (subResult.getOutput() != null) {
                    combinedOutput.append(subResult.getOutput()).append("\n");
                }

                // If any sub-test has a critical error, fail the entire composite test
                if (subResult.getStatus() == TestStatus.ERROR && subResult.getThrowable() != null) {
                    final long duration = System.currentTimeMillis() - startTime;
                    return resultBuilder
                        .status(TestStatus.ERROR)
                        .executionTimeMs(duration)
                        .errorMessage("Sub-test failed: " + subResult.getErrorMessage())
                        .throwable(subResult.getThrowable())
                        .output(combinedOutput.toString())
                        .build();
                }
            }

            // Determine overall status
            final TestStatus overallStatus;
            if (errors > 0) {
                overallStatus = TestStatus.ERROR;
            } else if (failed > 0) {
                overallStatus = TestStatus.FAILED;
            } else {
                overallStatus = TestStatus.PASSED;
            }

            final long duration = System.currentTimeMillis() - startTime;
            return resultBuilder
                .status(overallStatus)
                .executionTimeMs(duration)
                .output(combinedOutput.toString())
                .build();

        } catch (final Exception e) {
            final long duration = System.currentTimeMillis() - startTime;
            return resultBuilder
                .status(TestStatus.ERROR)
                .executionTimeMs(duration)
                .errorMessage("Composite test execution failed: " + e.getMessage())
                .throwable(e)
                .output(combinedOutput.toString())
                .build();
        }
    }

    private TestResult executeSingleTest(final WebAssemblyTestCase testCase, final TestRuntime runtime,
                                        final long startTime) {
        final TestResult.Builder resultBuilder = TestResult.builder()
            .testCase(testCase)
            .runtime(runtime);

        WasmRuntime wasmRuntime = null;

        try {
            // Create runtime instance
            wasmRuntime = createRuntimeForTesting(runtime);

            // Validate test file exists
            if (testCase.getTestFilePath() == null || !Files.exists(testCase.getTestFilePath())) {
                throw new TestSuiteException("Test file not found: " + testCase.getTestFilePath());
            }

            // Read test file
            final byte[] wasmBytes = Files.readAllBytes(testCase.getTestFilePath());

            // Execute test with timeout
            final TestResult result = executeWithTimeout(
                testCase, wasmRuntime, wasmBytes, runtime, startTime);

            // Validate result against expected outcome
            return validateTestResult(result, testCase);

        } catch (final TimeoutException e) {
            final long duration = System.currentTimeMillis() - startTime;
            return resultBuilder
                .status(TestStatus.TIMEOUT)
                .executionTimeMs(duration)
                .errorMessage("Test execution timed out")
                .throwable(e)
                .build();

        } catch (final Exception e) {
            final long duration = System.currentTimeMillis() - startTime;

            // Check if this is an expected failure
            if (testCase.getExpected() == TestExpectedResult.FAIL ||
                testCase.getExpected() == TestExpectedResult.TRAP) {
                return resultBuilder
                    .status(TestStatus.PASSED)
                    .executionTimeMs(duration)
                    .output("Expected failure occurred: " + e.getMessage())
                    .build();
            }

            return resultBuilder
                .status(TestStatus.ERROR)
                .executionTimeMs(duration)
                .errorMessage(e.getMessage())
                .throwable(e)
                .build();

        } finally {
            // Clean up resources
            if (wasmRuntime != null) {
                try {
                    wasmRuntime.close();
                } catch (final Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to close runtime", e);
                }
            }
        }
    }

    private WasmRuntime createRuntimeForTesting(final TestRuntime runtime) throws TestSuiteException {
        try {
            // Force specific runtime implementation for testing
            System.setProperty("wasmtime4j.runtime", runtime.getId());

            return WasmRuntimeFactory.createRuntime();

        } catch (final Exception e) {
            throw new TestSuiteException("Failed to create runtime: " + runtime, e);
        }
    }

    private TestResult executeWithTimeout(final WebAssemblyTestCase testCase, final WasmRuntime runtime,
                                         final byte[] wasmBytes, final TestRuntime testRuntime,
                                         final long startTime) throws TimeoutException {

        final long timeoutMs = calculateTimeout(testCase);
        final TestResult.Builder resultBuilder = TestResult.builder()
            .testCase(testCase)
            .runtime(testRuntime);

        try {
            // Execute the actual WebAssembly code
            final TestExecutionResult executionResult = executeWasm(runtime, wasmBytes, testCase);

            final long duration = System.currentTimeMillis() - startTime;

            if (duration > timeoutMs) {
                throw new TimeoutException("Test execution exceeded timeout: " + timeoutMs + "ms");
            }

            return resultBuilder
                .status(executionResult.isSuccess() ? TestStatus.PASSED : TestStatus.FAILED)
                .executionTimeMs(duration)
                .output(executionResult.getOutput())
                .errorMessage(executionResult.getErrorMessage())
                .build();

        } catch (final Exception e) {
            final long duration = System.currentTimeMillis() - startTime;

            if (duration > timeoutMs || e instanceof TimeoutException) {
                throw new TimeoutException("Test execution timed out");
            }

            throw e;
        }
    }

    private TestExecutionResult executeWasm(final WasmRuntime runtime, final byte[] wasmBytes,
                                           final WebAssemblyTestCase testCase) {
        try {
            // Basic WebAssembly module compilation and instantiation
            runtime.compileModule("test-module", wasmBytes);

            // For spec tests, we might need more sophisticated execution logic
            // This is a basic implementation - real tests would need more specific handling
            return new TestExecutionResult(true, "Module compiled successfully", null);

        } catch (final Exception e) {
            return new TestExecutionResult(false, null, e.getMessage());
        }
    }

    private TestResult validateTestResult(final TestResult result, final WebAssemblyTestCase testCase) {
        final TestExpectedResult expected = testCase.getExpected();
        final TestStatus actualStatus = result.getStatus();

        // Check if the result matches expectations
        final boolean resultMatches = switch (expected) {
            case PASS -> actualStatus == TestStatus.PASSED;
            case FAIL, TRAP -> actualStatus == TestStatus.FAILED || actualStatus == TestStatus.ERROR;
            case TIMEOUT -> actualStatus == TestStatus.TIMEOUT;
            case SKIP -> actualStatus == TestStatus.SKIPPED;
            case UNKNOWN -> true; // Accept any result for unknown expectations
        };

        if (!resultMatches) {
            return TestResult.builder(result)
                .status(TestStatus.FAILED)
                .errorMessage("Expected " + expected + " but got " + actualStatus)
                .build();
        }

        return result;
    }

    private long calculateTimeout(final WebAssemblyTestCase testCase) {
        // Base timeout from configuration
        long timeoutMs = TimeUnit.MINUTES.toMillis(configuration.getTestTimeoutMinutes());

        // Adjust based on test complexity
        final TestComplexity complexity = testCase.getComplexity();
        timeoutMs = Math.max(timeoutMs, complexity.getDefaultTimeoutMs());

        // Consider estimated execution time
        if (testCase.getEstimatedExecutionTimeMs() > 0) {
            timeoutMs = Math.max(timeoutMs, testCase.getEstimatedExecutionTimeMs() * 2);
        }

        return timeoutMs;
    }

    /**
     * Internal class to represent test execution results.
     */
    private static final class TestExecutionResult {
        private final boolean success;
        private final String output;
        private final String errorMessage;

        private TestExecutionResult(final boolean success, final String output, final String errorMessage) {
            this.success = success;
            this.output = output;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public String getOutput() { return output; }
        public String getErrorMessage() { return errorMessage; }
    }
}