package ai.tegmentum.wasmtime4j.experimental;

/**
 * Page size strategies for custom page sizes.
 *
 * @since 1.0.0
 */
public enum PageSizeStrategy {
    /** Use system default page size. */
    SYSTEM("system", "Use system default page size"),

    /** Use optimal page size based on workload analysis. */
    OPTIMAL("optimal", "Use optimal page size based on analysis"),

    /** Use custom specified page size. */
    CUSTOM("custom", "Use custom specified page size");

    private final String key;
    private final String description;

    PageSizeStrategy(final String key, final String description) {
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