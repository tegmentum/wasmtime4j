package ai.tegmentum.wasmtime4j.testsuite;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the result of executing a WebAssembly test case.
 */
public final class TestResult {

    private final WebAssemblyTestCase testCase;
    private final TestRuntime runtime;
    private final TestStatus status;
    private final long executionTimeMs;
    private final String output;
    private final String errorMessage;
    private final Throwable throwable;
    private final Instant timestamp;
    private final Map<String, Object> metrics;

    private TestResult(final Builder builder) {
        this.testCase = builder.testCase;
        this.runtime = builder.runtime;
        this.status = builder.status;
        this.executionTimeMs = builder.executionTimeMs;
        this.output = builder.output;
        this.errorMessage = builder.errorMessage;
        this.throwable = builder.throwable;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.metrics = Map.copyOf(builder.metrics);
    }

    // Getters
    public WebAssemblyTestCase getTestCase() { return testCase; }
    public TestRuntime getRuntime() { return runtime; }
    public TestStatus getStatus() { return status; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public String getOutput() { return output; }
    public String getErrorMessage() { return errorMessage; }
    public Throwable getThrowable() { return throwable; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, Object> getMetrics() { return metrics; }

    /**
     * Checks if the test was successful.
     *
     * @return true if test passed
     */
    public boolean isSuccess() {
        return status == TestStatus.PASSED;
    }

    /**
     * Checks if the test failed.
     *
     * @return true if test failed or had an error
     */
    public boolean isFailure() {
        return status == TestStatus.FAILED || status == TestStatus.ERROR;
    }

    /**
     * Gets a metric value by key.
     *
     * @param key metric key
     * @param <T> expected value type
     * @return metric value or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetric(final String key) {
        return (T) metrics.get(key);
    }

    /**
     * Creates a new builder for TestResult.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new builder initialized with values from an existing result.
     *
     * @param result existing result to copy from
     * @return new builder instance
     */
    public static Builder builder(final TestResult result) {
        if (result == null) {
            throw new IllegalArgumentException("Result cannot be null");
        }

        return new Builder()
            .testCase(result.testCase)
            .runtime(result.runtime)
            .status(result.status)
            .executionTimeMs(result.executionTimeMs)
            .output(result.output)
            .errorMessage(result.errorMessage)
            .throwable(result.throwable)
            .timestamp(result.timestamp)
            .metrics(result.metrics);
    }

    /**
     * Creates a failure result.
     *
     * @param testCase test case
     * @param runtime runtime
     * @param errorMessage error message
     * @param throwable throwable
     * @return failure result
     */
    public static TestResult failure(final WebAssemblyTestCase testCase, final TestRuntime runtime,
                                   final String errorMessage, final Throwable throwable) {
        return builder()
            .testCase(testCase)
            .runtime(runtime)
            .status(TestStatus.FAILED)
            .errorMessage(errorMessage)
            .throwable(throwable)
            .build();
    }

    /**
     * Creates an error result.
     *
     * @param testCase test case
     * @param runtime runtime
     * @param errorMessage error message
     * @param throwable throwable
     * @return error result
     */
    public static TestResult error(final WebAssemblyTestCase testCase, final TestRuntime runtime,
                                 final String errorMessage, final Throwable throwable) {
        return builder()
            .testCase(testCase)
            .runtime(runtime)
            .status(TestStatus.ERROR)
            .errorMessage(errorMessage)
            .throwable(throwable)
            .build();
    }

    /**
     * Creates a success result.
     *
     * @param testCase test case
     * @param runtime runtime
     * @param executionTimeMs execution time in milliseconds
     * @return success result
     */
    public static TestResult success(final WebAssemblyTestCase testCase, final TestRuntime runtime,
                                   final long executionTimeMs) {
        return builder()
            .testCase(testCase)
            .runtime(runtime)
            .status(TestStatus.PASSED)
            .executionTimeMs(executionTimeMs)
            .build();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final TestResult that = (TestResult) o;
        return Objects.equals(testCase, that.testCase) &&
               Objects.equals(runtime, that.runtime) &&
               Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testCase, runtime, timestamp);
    }

    @Override
    public String toString() {
        return "TestResult{" +
                "testCase=" + (testCase != null ? testCase.getTestId() : "null") +
                ", runtime=" + runtime +
                ", status=" + status +
                ", executionTimeMs=" + executionTimeMs +
                ", hasError=" + (errorMessage != null) +
                '}';
    }

    /**
     * Builder for TestResult.
     */
    public static final class Builder {
        private WebAssemblyTestCase testCase;
        private TestRuntime runtime;
        private TestStatus status;
        private long executionTimeMs;
        private String output;
        private String errorMessage;
        private Throwable throwable;
        private Instant timestamp;
        private Map<String, Object> metrics = Map.of();

        public Builder testCase(final WebAssemblyTestCase testCase) {
            this.testCase = testCase;
            return this;
        }

        public Builder runtime(final TestRuntime runtime) {
            this.runtime = runtime;
            return this;
        }

        public Builder status(final TestStatus status) {
            this.status = status;
            return this;
        }

        public Builder executionTimeMs(final long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }

        public Builder output(final String output) {
            this.output = output;
            return this;
        }

        public Builder errorMessage(final String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder throwable(final Throwable throwable) {
            this.throwable = throwable;
            return this;
        }

        public Builder timestamp(final Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder metrics(final Map<String, Object> metrics) {
            this.metrics = metrics != null ? Map.copyOf(metrics) : Map.of();
            return this;
        }

        public TestResult build() {
            if (testCase == null) {
                throw new IllegalStateException("Test case must be set");
            }
            if (runtime == null) {
                throw new IllegalStateException("Runtime must be set");
            }
            if (status == null) {
                throw new IllegalStateException("Status must be set");
            }

            return new TestResult(this);
        }
    }
}