package ai.tegmentum.wasmtime4j.security;

/**
 * Security event severity levels for categorizing the importance of security events.
 *
 * @since 1.0.0
 */
public enum SecuritySeverity {

    /**
     * Informational events that provide context but indicate no security concern.
     * Examples: successful authentication, normal operations.
     */
    INFO(0, "Informational"),

    /**
     * Low severity events that may be of interest but pose minimal risk.
     * Examples: failed login attempts, minor configuration changes.
     */
    LOW(1, "Low"),

    /**
     * Medium severity events that indicate potential security concerns.
     * Examples: authorization denials, resource limit warnings.
     */
    MEDIUM(2, "Medium"),

    /**
     * High severity events that indicate significant security issues.
     * Examples: policy violations, suspicious activity, access denials.
     */
    HIGH(3, "High"),

    /**
     * Critical severity events that indicate immediate security threats.
     * Examples: intrusion attempts, sandbox breaches, attack patterns.
     */
    CRITICAL(4, "Critical");

    private final int level;
    private final String displayName;

    SecuritySeverity(final int level, final String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    /**
     * Gets the numeric severity level.
     *
     * @return severity level (0-4, higher is more severe)
     */
    public int getLevel() {
        return level;
    }

    /**
     * Gets the display name for this severity level.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this severity level is at least as severe as the given level.
     *
     * @param other the severity level to compare against
     * @return true if this level is >= other level
     */
    public boolean isAtLeast(final SecuritySeverity other) {
        return this.level >= other.level;
    }

    /**
     * Checks if this severity level is more severe than the given level.
     *
     * @param other the severity level to compare against
     * @return true if this level > other level
     */
    public boolean isMoreSevereThan(final SecuritySeverity other) {
        return this.level > other.level;
    }

    /**
     * Checks if this severity level requires immediate attention.
     *
     * @return true for HIGH and CRITICAL levels
     */
    public boolean requiresImmediateAttention() {
        return this == HIGH || this == CRITICAL;
    }

    /**
     * Checks if this severity level should trigger alerts.
     *
     * @return true for MEDIUM, HIGH, and CRITICAL levels
     */
    public boolean shouldTriggerAlert() {
        return this.level >= MEDIUM.level;
    }

    /**
     * Gets the ANSI color code for console output.
     *
     * @return ANSI color code string
     */
    public String getColorCode() {
        switch (this) {
            case INFO:
                return "\u001B[36m"; // Cyan
            case LOW:
                return "\u001B[32m"; // Green
            case MEDIUM:
                return "\u001B[33m"; // Yellow
            case HIGH:
                return "\u001B[31m"; // Red
            case CRITICAL:
                return "\u001B[35m"; // Magenta
            default:
                return "\u001B[0m"; // Reset
        }
    }

    /**
     * Gets the ANSI reset code for console output.
     *
     * @return ANSI reset code string
     */
    public static String getResetCode() {
        return "\u001B[0m";
    }

    /**
     * Formats a message with severity-appropriate coloring.
     *
     * @param message the message to format
     * @return colored message string
     */
    public String format(final String message) {
        return getColorCode() + "[" + displayName.toUpperCase() + "] " + message + getResetCode();
    }
}