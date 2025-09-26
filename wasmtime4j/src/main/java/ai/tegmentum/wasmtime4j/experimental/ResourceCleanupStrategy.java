package ai.tegmentum.wasmtime4j.experimental;

/**
 * Resource cleanup strategies for resource types.
 *
 * @since 1.0.0
 */
public enum ResourceCleanupStrategy {
    /** Manual resource cleanup. */
    MANUAL("manual", "Manual resource cleanup"),

    /** Automatic resource cleanup. */
    AUTOMATIC("automatic", "Automatic resource cleanup"),

    /** Hybrid manual/automatic cleanup. */
    HYBRID("hybrid", "Hybrid manual/automatic cleanup");

    private final String key;
    private final String description;

    ResourceCleanupStrategy(final String key, final String description) {
        this.key = key;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return key + " (" + description + ")";
    }
}