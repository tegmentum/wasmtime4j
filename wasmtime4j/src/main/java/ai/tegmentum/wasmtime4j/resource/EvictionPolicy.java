package ai.tegmentum.wasmtime4j.resource;

/**
 * Eviction policies for cache management.
 *
 * <p>EvictionPolicy defines the strategy used to remove entries from the cache
 * when space is needed for new entries or when cache limits are exceeded.
 *
 * @since 1.0.0
 */
public enum EvictionPolicy {

    /**
     * Least Recently Used (LRU) eviction policy.
     *
     * <p>Evicts the entries that were accessed least recently. This is the
     * most common eviction policy and works well for most use cases.
     */
    LRU("Least Recently Used", "Evicts entries that were accessed least recently"),

    /**
     * Least Frequently Used (LFU) eviction policy.
     *
     * <p>Evicts the entries that are accessed least frequently. This works
     * well when access patterns are predictable and some entries are consistently
     * more popular than others.
     */
    LFU("Least Frequently Used", "Evicts entries that are accessed least frequently"),

    /**
     * First In, First Out (FIFO) eviction policy.
     *
     * <p>Evicts the oldest entries first, regardless of access patterns.
     * This is useful when temporal ordering is more important than access frequency.
     */
    FIFO("First In, First Out", "Evicts the oldest entries first"),

    /**
     * Time-to-Live (TTL) based eviction policy.
     *
     * <p>Evicts entries based on their time-to-live settings. Entries with
     * shorter remaining TTL are evicted first.
     */
    TTL("Time To Live", "Evicts entries with shortest remaining TTL first"),

    /**
     * Size-based eviction policy.
     *
     * <p>Evicts the largest entries first to free up the most memory space.
     * This is useful when memory usage is the primary concern.
     */
    SIZE("Size Based", "Evicts the largest entries first"),

    /**
     * Priority-based eviction policy.
     *
     * <p>Evicts entries with the lowest priority first. This allows explicit
     * control over which entries are more important to retain.
     */
    PRIORITY("Priority Based", "Evicts entries with lowest priority first"),

    /**
     * Weight-based eviction policy.
     *
     * <p>Evicts entries with the highest eviction weight first. This provides
     * fine-grained control over eviction decisions.
     */
    WEIGHT("Weight Based", "Evicts entries with highest eviction weight first"),

    /**
     * Random eviction policy.
     *
     * <p>Evicts entries randomly. This can be useful for testing or when
     * no particular eviction strategy is preferred.
     */
    RANDOM("Random", "Evicts entries randomly"),

    /**
     * Adaptive eviction policy.
     *
     * <p>Dynamically chooses the best eviction strategy based on current
     * cache usage patterns and performance metrics.
     */
    ADAPTIVE("Adaptive", "Dynamically chooses the best eviction strategy"),

    /**
     * No eviction policy.
     *
     * <p>Never evicts entries automatically. Manual eviction is required
     * when the cache reaches capacity limits.
     */
    NONE("No Eviction", "Never evicts entries automatically");

    private final String displayName;
    private final String description;

    EvictionPolicy(final String displayName, final String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Gets the human-readable display name for this policy.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the description of this eviction policy.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this policy is time-based.
     *
     * @return true if the policy considers time factors
     */
    public boolean isTimeBased() {
        return this == LRU || this == TTL || this == FIFO;
    }

    /**
     * Checks if this policy is frequency-based.
     *
     * @return true if the policy considers access frequency
     */
    public boolean isFrequencyBased() {
        return this == LFU;
    }

    /**
     * Checks if this policy is size-based.
     *
     * @return true if the policy considers entry size
     */
    public boolean isSizeBased() {
        return this == SIZE;
    }

    /**
     * Checks if this policy is priority-based.
     *
     * @return true if the policy considers entry priority
     */
    public boolean isPriorityBased() {
        return this == PRIORITY || this == WEIGHT;
    }

    /**
     * Checks if this policy is deterministic.
     *
     * @return true if the policy produces predictable results
     */
    public boolean isDeterministic() {
        return this != RANDOM && this != ADAPTIVE;
    }

    /**
     * Checks if this policy requires access tracking.
     *
     * @return true if the policy needs to track access patterns
     */
    public boolean requiresAccessTracking() {
        return this == LRU || this == LFU || this == ADAPTIVE;
    }

    /**
     * Checks if this policy requires frequency tracking.
     *
     * @return true if the policy needs to track access frequency
     */
    public boolean requiresFrequencyTracking() {
        return this == LFU || this == ADAPTIVE;
    }

    /**
     * Gets the computational complexity of this eviction policy.
     *
     * @return the complexity description
     */
    public String getComplexity() {
        switch (this) {
            case LRU:
                return "O(1) for access, O(1) for eviction";
            case LFU:
                return "O(1) for access, O(log n) for eviction";
            case FIFO:
                return "O(1) for access, O(1) for eviction";
            case TTL:
                return "O(1) for access, O(log n) for eviction";
            case SIZE:
                return "O(1) for access, O(n) for eviction";
            case PRIORITY:
                return "O(1) for access, O(log n) for eviction";
            case WEIGHT:
                return "O(1) for access, O(log n) for eviction";
            case RANDOM:
                return "O(1) for access, O(1) for eviction";
            case ADAPTIVE:
                return "O(1) for access, O(log n) for eviction";
            case NONE:
            default:
                return "O(1) for access, no eviction";
        }
    }

    /**
     * Creates an eviction policy from a string representation.
     *
     * @param policyString the policy string (case-insensitive)
     * @return the corresponding eviction policy
     * @throws IllegalArgumentException if policyString is null or invalid
     */
    public static EvictionPolicy fromString(final String policyString) {
        if (policyString == null || policyString.trim().isEmpty()) {
            throw new IllegalArgumentException("Policy string cannot be null or empty");
        }

        final String normalized = policyString.trim().toUpperCase();
        try {
            return EvictionPolicy.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid eviction policy: " + policyString +
                    ". Valid values are: LRU, LFU, FIFO, TTL, SIZE, PRIORITY, WEIGHT, RANDOM, ADAPTIVE, NONE", e);
        }
    }

    /**
     * Gets the default eviction policy.
     *
     * @return the default eviction policy (LRU)
     */
    public static EvictionPolicy getDefault() {
        return LRU;
    }

    @Override
    public String toString() {
        return displayName;
    }
}