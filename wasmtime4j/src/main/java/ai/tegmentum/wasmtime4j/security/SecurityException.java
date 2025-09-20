package ai.tegmentum.wasmtime4j.security;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Base exception for security-related errors in the WebAssembly runtime.
 *
 * @since 1.0.0
 */
public class SecurityException extends WasmException {

    private final SecurityEventType eventType;
    private final SecuritySeverity severity;

    /**
     * Creates a new security exception.
     *
     * @param message the exception message
     */
    public SecurityException(final String message) {
        this(message, SecurityEventType.SECURITY_POLICY_VIOLATION, SecuritySeverity.HIGH);
    }

    /**
     * Creates a new security exception with cause.
     *
     * @param message the exception message
     * @param cause the underlying cause
     */
    public SecurityException(final String message, final Throwable cause) {
        this(message, cause, SecurityEventType.SECURITY_POLICY_VIOLATION, SecuritySeverity.HIGH);
    }

    /**
     * Creates a new security exception with event type and severity.
     *
     * @param message the exception message
     * @param eventType the associated security event type
     * @param severity the security severity level
     */
    public SecurityException(final String message, final SecurityEventType eventType,
                            final SecuritySeverity severity) {
        super(message);
        this.eventType = eventType;
        this.severity = severity;
    }

    /**
     * Creates a new security exception with cause, event type and severity.
     *
     * @param message the exception message
     * @param cause the underlying cause
     * @param eventType the associated security event type
     * @param severity the security severity level
     */
    public SecurityException(final String message, final Throwable cause,
                            final SecurityEventType eventType, final SecuritySeverity severity) {
        super(message, cause);
        this.eventType = eventType;
        this.severity = severity;
    }

    /**
     * Gets the associated security event type.
     *
     * @return security event type
     */
    public SecurityEventType getEventType() {
        return eventType;
    }

    /**
     * Gets the security severity level.
     *
     * @return security severity
     */
    public SecuritySeverity getSeverity() {
        return severity;
    }

    /**
     * Checks if this exception represents a critical security issue.
     *
     * @return true if severity is CRITICAL
     */
    public boolean isCritical() {
        return severity == SecuritySeverity.CRITICAL;
    }

    /**
     * Checks if this exception requires immediate attention.
     *
     * @return true if severity requires immediate attention
     */
    public boolean requiresImmediateAttention() {
        return severity.requiresImmediateAttention();
    }
}