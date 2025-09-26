package ai.tegmentum.wasmtime4j.experimental;

/**
 * Strategies for WebAssembly stack switching operations.
 *
 * <p>Stack switching is an experimental WebAssembly proposal that enables
 * coroutines, fibers, and other advanced control flow mechanisms.
 *
 * @since 1.0.0
 */
public enum StackSwitchingStrategy {

    /**
     * Cooperative stack switching where switches only occur at designated yield points.
     * This strategy provides predictable behavior and lower overhead but requires
     * explicit cooperation from the WebAssembly code.
     */
    COOPERATIVE("cooperative", "Cooperative stack switching with explicit yield points"),

    /**
     * Preemptive stack switching where the runtime can switch stacks at any point.
     * This strategy provides more flexibility but has higher overhead and complexity.
     */
    PREEMPTIVE("preemptive", "Preemptive stack switching with runtime control"),

    /**
     * Hybrid stack switching that combines cooperative and preemptive approaches.
     * Uses cooperative switching when possible but falls back to preemptive when needed.
     */
    HYBRID("hybrid", "Hybrid cooperative/preemptive stack switching");

    private final String key;
    private final String description;

    StackSwitchingStrategy(final String key, final String description) {
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