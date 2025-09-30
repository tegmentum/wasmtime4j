package ai.tegmentum.wasmtime4j.experimental;

/**
 * Type validation strategies for type imports.
 *
 * @since 1.0.0
 */
public enum TypeValidationStrategy {
    /** Strict type validation with no flexibility. */
    STRICT("strict", "Strict type validation"),

    /** Relaxed type validation allowing some flexibility. */
    RELAXED("relaxed", "Relaxed type validation"),

    /** Dynamic type validation at runtime. */
    DYNAMIC("dynamic", "Dynamic type validation at runtime");

    private final String key;
    private final String description;

    TypeValidationStrategy(final String key, final String description) {
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