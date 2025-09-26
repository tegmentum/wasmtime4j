package ai.tegmentum.wasmtime4j.experimental;

/**
 * Tier-up compilation strategies for experimental JIT optimization.
 *
 * <p>Tier-up strategies control how the runtime transitions from interpreted
 * or baseline compiled code to optimized compiled code.
 *
 * @since 1.0.0
 */
public enum TierUpStrategy {

    /**
     * No tier-up compilation - use baseline compilation only.
     * Fast startup time but lower steady-state performance.
     */
    NONE("none", "No tier-up compilation"),

    /**
     * Conservative tier-up strategy.
     * Tier up only after significant execution time to avoid unnecessary compilation.
     */
    CONSERVATIVE("conservative", "Conservative tier-up strategy"),

    /**
     * Adaptive tier-up strategy.
     * Uses runtime profiling to determine optimal tier-up timing.
     */
    ADAPTIVE("adaptive", "Adaptive tier-up strategy"),

    /**
     * Aggressive tier-up strategy.
     * Tier up quickly to maximize performance for compute-intensive workloads.
     */
    AGGRESSIVE("aggressive", "Aggressive tier-up strategy"),

    /**
     * Speculative tier-up strategy.
     * Uses speculation and prediction to tier up before hotspots are detected.
     */
    SPECULATIVE("speculative", "Speculative tier-up strategy");

    private final String key;
    private final String description;

    TierUpStrategy(final String key, final String description) {
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