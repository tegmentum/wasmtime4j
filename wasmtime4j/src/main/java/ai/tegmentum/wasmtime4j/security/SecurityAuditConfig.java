package ai.tegmentum.wasmtime4j.security;

import java.time.Duration;
import java.util.Set;

/**
 * Configuration for security audit logging.
 *
 * @since 1.0.0
 */
public final class SecurityAuditConfig {

    private final boolean enabled;
    private final Set<SecurityEventType> eventTypes;
    private final Duration retentionPeriod;
    private final int maxLogSize;
    private final String logFormat;

    private SecurityAuditConfig(final Builder builder) {
        this.enabled = builder.enabled;
        this.eventTypes = Set.copyOf(builder.eventTypes);
        this.retentionPeriod = builder.retentionPeriod;
        this.maxLogSize = builder.maxLogSize;
        this.logFormat = builder.logFormat;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SecurityAuditConfig defaultConfig() {
        return builder().build();
    }

    public boolean isEnabled() { return enabled; }
    public Set<SecurityEventType> getEventTypes() { return eventTypes; }
    public Duration getRetentionPeriod() { return retentionPeriod; }
    public int getMaxLogSize() { return maxLogSize; }
    public String getLogFormat() { return logFormat; }

    public static final class Builder {
        private boolean enabled = true;
        private Set<SecurityEventType> eventTypes = Set.of(SecurityEventType.values());
        private Duration retentionPeriod = Duration.ofDays(30);
        private int maxLogSize = 100 * 1024 * 1024; // 100MB
        private String logFormat = "JSON";

        public Builder withEnabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder withEventTypes(final Set<SecurityEventType> eventTypes) {
            this.eventTypes = eventTypes;
            return this;
        }

        public Builder withRetentionPeriod(final Duration retentionPeriod) {
            this.retentionPeriod = retentionPeriod;
            return this;
        }

        public Builder withMaxLogSize(final int maxLogSize) {
            this.maxLogSize = maxLogSize;
            return this;
        }

        public Builder withLogFormat(final String logFormat) {
            this.logFormat = logFormat;
            return this;
        }

        public SecurityAuditConfig build() {
            return new SecurityAuditConfig(this);
        }
    }
}