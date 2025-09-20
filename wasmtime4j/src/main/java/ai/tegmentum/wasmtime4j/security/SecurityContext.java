package ai.tegmentum.wasmtime4j.security;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Security context containing authentication and authorization information.
 *
 * @since 1.0.0
 */
public final class SecurityContext {

    private final String principalId;
    private final String sessionId;
    private final Instant authenticatedAt;
    private final Map<String, String> attributes;
    private final SecurityLevel securityLevel;

    private SecurityContext(final Builder builder) {
        this.principalId = builder.principalId;
        this.sessionId = builder.sessionId;
        this.authenticatedAt = builder.authenticatedAt;
        this.attributes = Map.copyOf(builder.attributes);
        this.securityLevel = builder.securityLevel;
    }

    /**
     * Creates a default security context for anonymous access.
     *
     * @return default security context
     */
    public static SecurityContext defaultContext() {
        return builder()
            .withPrincipalId("anonymous")
            .withSecurityLevel(SecurityLevel.LOW)
            .build();
    }

    /**
     * Creates a system security context for privileged operations.
     *
     * @return system security context
     */
    public static SecurityContext systemContext() {
        return builder()
            .withPrincipalId("system")
            .withSecurityLevel(SecurityLevel.SYSTEM)
            .build();
    }

    /**
     * Creates a new security context builder.
     *
     * @return security context builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getPrincipalId() { return principalId; }
    public Optional<String> getSessionId() { return Optional.ofNullable(sessionId); }
    public Optional<Instant> getAuthenticatedAt() { return Optional.ofNullable(authenticatedAt); }
    public Map<String, String> getAttributes() { return attributes; }
    public SecurityLevel getSecurityLevel() { return securityLevel; }

    public Optional<String> getAttribute(final String key) {
        return Optional.ofNullable(attributes.get(key));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final SecurityContext that = (SecurityContext) o;
        return Objects.equals(principalId, that.principalId) &&
               Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(principalId, sessionId);
    }

    /**
     * Security levels for principals.
     */
    public enum SecurityLevel {
        SYSTEM, HIGH, MEDIUM, LOW
    }

    /**
     * Builder for SecurityContext.
     */
    public static final class Builder {
        private String principalId;
        private String sessionId;
        private Instant authenticatedAt;
        private Map<String, String> attributes = Map.of();
        private SecurityLevel securityLevel = SecurityLevel.LOW;

        public Builder withPrincipalId(final String principalId) {
            this.principalId = principalId;
            return this;
        }

        public Builder withSessionId(final String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder withAuthenticatedAt(final Instant authenticatedAt) {
            this.authenticatedAt = authenticatedAt;
            return this;
        }

        public Builder withAttributes(final Map<String, String> attributes) {
            this.attributes = attributes != null ? attributes : Map.of();
            return this;
        }

        public Builder withSecurityLevel(final SecurityLevel securityLevel) {
            this.securityLevel = securityLevel;
            return this;
        }

        public SecurityContext build() {
            Objects.requireNonNull(principalId, "Principal ID cannot be null");
            Objects.requireNonNull(securityLevel, "Security level cannot be null");
            return new SecurityContext(this);
        }
    }
}