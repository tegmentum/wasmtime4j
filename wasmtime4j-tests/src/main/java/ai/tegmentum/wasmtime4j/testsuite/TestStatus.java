package ai.tegmentum.wasmtime4j.testsuite;

/**
 * Status values for WebAssembly test execution results.
 */
public enum TestStatus {
    /**
     * Test passed successfully.
     */
    PASSED("passed", "Passed", "Test executed successfully"),

    /**
     * Test failed as expected or unexpectedly.
     */
    FAILED("failed", "Failed", "Test failed during execution"),

    /**
     * Test encountered an error that prevented execution.
     */
    ERROR("error", "Error", "Test encountered an execution error"),

    /**
     * Test execution timed out.
     */
    TIMEOUT("timeout", "Timeout", "Test execution timed out"),

    /**
     * Test was skipped.
     */
    SKIPPED("skipped", "Skipped", "Test was skipped"),

    /**
     * Test is currently running.
     */
    RUNNING("running", "Running", "Test is currently executing");

    private final String id;
    private final String displayName;
    private final String description;

    TestStatus(final String id, final String displayName, final String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if this status represents a completed test.
     *
     * @return true if test is completed
     */
    public boolean isCompleted() {
        return this != RUNNING;
    }

    /**
     * Checks if this status represents a successful test.
     *
     * @return true if test was successful
     */
    public boolean isSuccess() {
        return this == PASSED;
    }

    /**
     * Checks if this status represents a failed test.
     *
     * @return true if test failed
     */
    public boolean isFailure() {
        return this == FAILED || this == ERROR || this == TIMEOUT;
    }

    /**
     * Gets TestStatus by ID.
     *
     * @param id status ID
     * @return TestStatus or ERROR if not found
     */
    public static TestStatus fromId(final String id) {
        if (id == null) {
            return ERROR;
        }
        for (final TestStatus status : values()) {
            if (status.id.equalsIgnoreCase(id)) {
                return status;
            }
        }
        return ERROR;
    }
}