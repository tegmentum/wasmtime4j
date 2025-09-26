package ai.tegmentum.wasmtime4j.testsuite;

/**
 * Complexity levels for WebAssembly test cases.
 * Used for test scheduling, timeout determination, and resource allocation.
 */
public enum TestComplexity {
    /**
     * Simple test case with minimal resource requirements.
     */
    SIMPLE("simple", "Simple", "Basic test with minimal complexity", 1000, 1),

    /**
     * Moderate test case with average resource requirements.
     */
    MODERATE("moderate", "Moderate", "Test with moderate complexity", 5000, 2),

    /**
     * Complex test case with high resource requirements.
     */
    COMPLEX("complex", "Complex", "Test with high complexity", 30000, 4);

    private final String id;
    private final String displayName;
    private final String description;
    private final long defaultTimeoutMs;
    private final int resourceMultiplier;

    TestComplexity(final String id, final String displayName, final String description,
                   final long defaultTimeoutMs, final int resourceMultiplier) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.defaultTimeoutMs = defaultTimeoutMs;
        this.resourceMultiplier = resourceMultiplier;
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

    public long getDefaultTimeoutMs() {
        return defaultTimeoutMs;
    }

    public int getResourceMultiplier() {
        return resourceMultiplier;
    }

    /**
     * Gets TestComplexity by ID.
     *
     * @param id complexity ID
     * @return TestComplexity or SIMPLE if not found
     */
    public static TestComplexity fromId(final String id) {
        if (id == null) {
            return SIMPLE;
        }
        for (final TestComplexity complexity : values()) {
            if (complexity.id.equalsIgnoreCase(id)) {
                return complexity;
            }
        }
        return SIMPLE;
    }
}