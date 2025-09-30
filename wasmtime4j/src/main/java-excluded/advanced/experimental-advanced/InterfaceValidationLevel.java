package ai.tegmentum.wasmtime4j.experimental;

/**
 * Interface validation levels for interface types.
 *
 * @since 1.0.0
 */
public enum InterfaceValidationLevel {
    /** Minimal interface validation. */
    MINIMAL("minimal", "Minimal interface validation"),

    /** Standard interface validation. */
    STANDARD("standard", "Standard interface validation"),

    /** Strict interface validation. */
    STRICT("strict", "Strict interface validation");

    private final String key;
    private final String description;

    InterfaceValidationLevel(final String key, final String description) {
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