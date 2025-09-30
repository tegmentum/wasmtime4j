package ai.tegmentum.wasmtime4j.experimental;

/**
 * Import resolution mechanisms for type imports.
 *
 * @since 1.0.0
 */
public enum ImportResolutionMechanism {
    /** Static resolution at link time. */
    STATIC("static", "Static resolution at link time"),

    /** Dynamic resolution at runtime. */
    DYNAMIC("dynamic", "Dynamic resolution at runtime"),

    /** Lazy resolution when first accessed. */
    LAZY("lazy", "Lazy resolution when first accessed");

    private final String key;
    private final String description;

    ImportResolutionMechanism(final String key, final String description) {
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