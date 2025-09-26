package ai.tegmentum.wasmtime4j.testsuite;

/**
 * Expected results for WebAssembly test case execution.
 */
public enum TestExpectedResult {
    /**
     * Test is expected to pass successfully.
     */
    PASS("pass", "Pass", "Test should execute successfully"),

    /**
     * Test is expected to fail with an error.
     */
    FAIL("fail", "Fail", "Test should fail with an error"),

    /**
     * Test is expected to trap during execution.
     */
    TRAP("trap", "Trap", "Test should trap during execution"),

    /**
     * Test is expected to timeout.
     */
    TIMEOUT("timeout", "Timeout", "Test should timeout"),

    /**
     * Test is expected to be skipped.
     */
    SKIP("skip", "Skip", "Test should be skipped"),

    /**
     * Test result is unknown or undetermined.
     */
    UNKNOWN("unknown", "Unknown", "Test result is unknown");

    private final String id;
    private final String displayName;
    private final String description;

    TestExpectedResult(final String id, final String displayName, final String description) {
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
     * Gets TestExpectedResult by ID.
     *
     * @param id result ID
     * @return TestExpectedResult or UNKNOWN if not found
     */
    public static TestExpectedResult fromId(final String id) {
        if (id == null) {
            return UNKNOWN;
        }
        for (final TestExpectedResult result : values()) {
            if (result.id.equalsIgnoreCase(id)) {
                return result;
            }
        }
        return UNKNOWN;
    }
}