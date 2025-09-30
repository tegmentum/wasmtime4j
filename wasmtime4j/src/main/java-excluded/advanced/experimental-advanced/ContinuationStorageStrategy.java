package ai.tegmentum.wasmtime4j.experimental;

/**
 * Storage strategies for WebAssembly call/cc continuations.
 *
 * <p>Call/CC (call-with-current-continuation) is an experimental WebAssembly proposal
 * that enables first-class continuations for advanced control flow.
 *
 * @since 1.0.0
 */
public enum ContinuationStorageStrategy {

    /**
     * Store continuations on the execution stack.
     * This strategy provides fast access but limited storage capacity
     * and potential stack overflow risks with deep continuations.
     */
    STACK("stack", "Store continuations on execution stack"),

    /**
     * Store continuations in heap memory.
     * This strategy provides unlimited storage capacity but slower access
     * and requires garbage collection management.
     */
    HEAP("heap", "Store continuations in heap memory"),

    /**
     * Hybrid storage strategy that uses stack for small continuations
     * and heap for large ones. Provides a balance between performance
     * and capacity.
     */
    HYBRID("hybrid", "Hybrid stack/heap continuation storage");

    private final String key;
    private final String description;

    ContinuationStorageStrategy(final String key, final String description) {
        this.key = key;
        this.description = description;
    }

    /**
     * Gets the unique key identifier for this strategy.
     *
     * @return the strategy key
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the human-readable description of this strategy.
     *
     * @return the strategy description
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return key + " (" + description + ")";
    }
}