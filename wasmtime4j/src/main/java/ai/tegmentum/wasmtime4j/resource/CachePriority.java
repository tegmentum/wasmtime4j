package ai.tegmentum.wasmtime4j.resource;

/**
 * Priority levels for cache entries to influence eviction decisions.
 *
 * <p>CachePriority provides a way to specify the relative importance of
 * cached resources, affecting their likelihood of being evicted when
 * cache space is limited.
 *
 * @since 1.0.0
 */
public enum CachePriority {

    /**
     * Critical priority - these entries should be evicted last.
     *
     * <p>Used for essential resources that are expensive to recreate
     * and are frequently accessed.
     */
    CRITICAL("Critical", 5, 0.1),

    /**
     * High priority - these entries are important and should be retained.
     *
     * <p>Used for frequently accessed resources that are moderately
     * expensive to recreate.
     */
    HIGH("High", 4, 0.3),

    /**
     * Normal priority - default priority for most entries.
     *
     * <p>Used for regular resources with average access patterns
     * and recreation costs.
     */
    NORMAL("Normal", 3, 1.0),

    /**
     * Low priority - these entries can be evicted more readily.
     *
     * <p>Used for infrequently accessed resources or resources
     * that are cheap to recreate.
     */
    LOW("Low", 2, 2.0),

    /**
     * Minimal priority - these entries are evicted first.
     *
     * <p>Used for temporary or test resources that can be
     * recreated easily.
     */
    MINIMAL("Minimal", 1, 3.0);

    private final String displayName;
    private final int numericValue;
    private final double evictionWeight;

    CachePriority(final String displayName, final int numericValue, final double evictionWeight) {
        this.displayName = displayName;
        this.numericValue = numericValue;
        this.evictionWeight = evictionWeight;
    }

    /**
     * Gets the human-readable display name for this priority.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the numeric value for this priority (higher = more important).
     *
     * @return the numeric value (1-5)
     */
    public int getNumericValue() {
        return numericValue;
    }

    /**
     * Gets the default eviction weight for this priority.
     *
     * <p>Lower weights make entries less likely to be evicted.
     * Higher weights make entries more likely to be evicted.
     *
     * @return the eviction weight
     */
    public double getEvictionWeight() {
        return evictionWeight;
    }

    /**
     * Checks if this priority is higher than another priority.
     *
     * @param other the other priority to compare
     * @return true if this priority is higher
     * @throws IllegalArgumentException if other is null
     */
    public boolean isHigherThan(final CachePriority other) {
        if (other == null) {
            throw new IllegalArgumentException("Other priority cannot be null");
        }
        return this.numericValue > other.numericValue;
    }

    /**
     * Checks if this priority is lower than another priority.
     *
     * @param other the other priority to compare
     * @return true if this priority is lower
     * @throws IllegalArgumentException if other is null
     */
    public boolean isLowerThan(final CachePriority other) {
        if (other == null) {
            throw new IllegalArgumentException("Other priority cannot be null");
        }
        return this.numericValue < other.numericValue;
    }

    /**
     * Gets the priority that is one level higher than this priority.
     *
     * @return the higher priority, or this priority if already at maximum
     */
    public CachePriority getHigher() {
        switch (this) {
            case MINIMAL:
                return LOW;
            case LOW:
                return NORMAL;
            case NORMAL:
                return HIGH;
            case HIGH:
                return CRITICAL;
            case CRITICAL:
            default:
                return CRITICAL;
        }
    }

    /**
     * Gets the priority that is one level lower than this priority.
     *
     * @return the lower priority, or this priority if already at minimum
     */
    public CachePriority getLower() {
        switch (this) {
            case CRITICAL:
                return HIGH;
            case HIGH:
                return NORMAL;
            case NORMAL:
                return LOW;
            case LOW:
                return MINIMAL;
            case MINIMAL:
            default:
                return MINIMAL;
        }
    }

    /**
     * Creates a priority from a numeric value.
     *
     * @param numericValue the numeric value (1-5)
     * @return the corresponding priority
     * @throws IllegalArgumentException if numericValue is not in range 1-5
     */
    public static CachePriority fromNumericValue(final int numericValue) {
        switch (numericValue) {
            case 1:
                return MINIMAL;
            case 2:
                return LOW;
            case 3:
                return NORMAL;
            case 4:
                return HIGH;
            case 5:
                return CRITICAL;
            default:
                throw new IllegalArgumentException(
                        "Numeric value must be between 1 and 5, got: " + numericValue);
        }
    }

    /**
     * Creates a priority from a string representation.
     *
     * @param priorityString the priority string (case-insensitive)
     * @return the corresponding priority
     * @throws IllegalArgumentException if priorityString is null or invalid
     */
    public static CachePriority fromString(final String priorityString) {
        if (priorityString == null || priorityString.trim().isEmpty()) {
            throw new IllegalArgumentException("Priority string cannot be null or empty");
        }

        final String normalized = priorityString.trim().toUpperCase();
        try {
            return CachePriority.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid priority string: " + priorityString +
                    ". Valid values are: CRITICAL, HIGH, NORMAL, LOW, MINIMAL", e);
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}