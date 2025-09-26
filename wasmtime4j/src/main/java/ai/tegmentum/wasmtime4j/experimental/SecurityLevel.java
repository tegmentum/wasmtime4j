package ai.tegmentum.wasmtime4j.experimental;

/**
 * Security levels for experimental WebAssembly sandboxing and protection features.
 *
 * <p>These security levels control the extent of experimental security measures
 * applied to WebAssembly execution environments.
 *
 * @since 1.0.0
 */
public enum SecurityLevel {

    /**
     * Standard security level with basic WebAssembly sandboxing.
     * Uses the default Wasmtime security model without experimental enhancements.
     */
    STANDARD("standard", "Standard WebAssembly security", 0),

    /**
     * Enhanced security level with additional experimental protections.
     * Includes memory protection, basic capability controls, and enhanced validation.
     */
    ENHANCED("enhanced", "Enhanced security with experimental protections", 1),

    /**
     * High security level with comprehensive experimental security features.
     * Includes advanced sandboxing, fine-grained resource controls, and cryptographic validation.
     */
    HIGH("high", "High security with comprehensive protections", 2),

    /**
     * Maximum security level with all experimental security features enabled.
     * Includes quantum-resistant cryptography, advanced isolation, and maximum restrictions.
     * May significantly impact performance.
     */
    MAXIMUM("maximum", "Maximum security with all experimental features", 3);

    private final String key;
    private final String description;
    private final int level;

    SecurityLevel(final String key, final String description, final int level) {
        this.key = key;
        this.description = description;
        this.level = level;
    }

    /**
     * Gets the unique key identifier for this security level.
     *
     * @return the security level key
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the human-readable description of this security level.
     *
     * @return the security level description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the numeric level value for comparison.
     *
     * @return the numeric security level (higher = more secure)
     */
    public int getLevel() {
        return level;
    }

    /**
     * Checks if this security level is at least as secure as the specified level.
     *
     * @param other the security level to compare against
     * @return true if this level is at least as secure as the other level
     */
    public boolean isAtLeast(final SecurityLevel other) {
        return this.level >= other.level;
    }

    /**
     * Checks if this security level is more secure than the specified level.
     *
     * @param other the security level to compare against
     * @return true if this level is more secure than the other level
     */
    public boolean isHigherThan(final SecurityLevel other) {
        return this.level > other.level;
    }

    @Override
    public String toString() {
        return key + " (" + description + ")";
    }
}