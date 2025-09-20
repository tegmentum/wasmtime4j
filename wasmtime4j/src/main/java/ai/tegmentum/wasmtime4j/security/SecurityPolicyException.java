package ai.tegmentum.wasmtime4j.security;

/**
 * Exception thrown when security policy operations fail.
 *
 * @since 1.0.0
 */
public class SecurityPolicyException extends SecurityException {

    /**
     * Creates a new security policy exception.
     *
     * @param message the exception message
     */
    public SecurityPolicyException(final String message) {
        super(message, SecurityEventType.SECURITY_POLICY_VIOLATION, SecuritySeverity.HIGH);
    }

    /**
     * Creates a new security policy exception with cause.
     *
     * @param message the exception message
     * @param cause the underlying cause
     */
    public SecurityPolicyException(final String message, final Throwable cause) {
        super(message, cause, SecurityEventType.SECURITY_POLICY_VIOLATION, SecuritySeverity.HIGH);
    }
}