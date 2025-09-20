package ai.tegmentum.wasmtime4j.security;

import java.time.Duration;

/**
 * Rate limiting configuration for security operations.
 *
 * @since 1.0.0
 */
public final class RateLimits {

    private final int maxOperationsPerSecond;
    private final int maxOperationsPerMinute;
    private final int maxOperationsPerHour;
    private final Duration cooldownPeriod;
    private final boolean burstAllowed;
    private final int burstSize;

    private RateLimits(final Builder builder) {
        this.maxOperationsPerSecond = builder.maxOperationsPerSecond;
        this.maxOperationsPerMinute = builder.maxOperationsPerMinute;
        this.maxOperationsPerHour = builder.maxOperationsPerHour;
        this.cooldownPeriod = builder.cooldownPeriod;
        this.burstAllowed = builder.burstAllowed;
        this.burstSize = builder.burstSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RateLimits defaultLimits() {
        return builder().build();
    }

    public static RateLimits unlimited() {
        return builder()
            .withMaxOperationsPerSecond(Integer.MAX_VALUE)
            .withMaxOperationsPerMinute(Integer.MAX_VALUE)
            .withMaxOperationsPerHour(Integer.MAX_VALUE)
            .withBurstAllowed(true)
            .withBurstSize(Integer.MAX_VALUE)
            .build();
    }

    public static RateLimits strict() {
        return builder()
            .withMaxOperationsPerSecond(10)
            .withMaxOperationsPerMinute(100)
            .withMaxOperationsPerHour(1000)
            .withBurstAllowed(false)
            .withCooldownPeriod(Duration.ofSeconds(30))
            .build();
    }

    public int getMaxOperationsPerSecond() { return maxOperationsPerSecond; }
    public int getMaxOperationsPerMinute() { return maxOperationsPerMinute; }
    public int getMaxOperationsPerHour() { return maxOperationsPerHour; }
    public Duration getCooldownPeriod() { return cooldownPeriod; }
    public boolean isBurstAllowed() { return burstAllowed; }
    public int getBurstSize() { return burstSize; }

    public static final class Builder {
        private int maxOperationsPerSecond = 100;
        private int maxOperationsPerMinute = 1000;
        private int maxOperationsPerHour = 10000;
        private Duration cooldownPeriod = Duration.ofSeconds(10);
        private boolean burstAllowed = true;
        private int burstSize = 50;

        public Builder withMaxOperationsPerSecond(final int maxOps) {
            this.maxOperationsPerSecond = maxOps;
            return this;
        }

        public Builder withMaxOperationsPerMinute(final int maxOps) {
            this.maxOperationsPerMinute = maxOps;
            return this;
        }

        public Builder withMaxOperationsPerHour(final int maxOps) {
            this.maxOperationsPerHour = maxOps;
            return this;
        }

        public Builder withCooldownPeriod(final Duration period) {
            this.cooldownPeriod = period;
            return this;
        }

        public Builder withBurstAllowed(final boolean allowed) {
            this.burstAllowed = allowed;
            return this;
        }

        public Builder withBurstSize(final int size) {
            this.burstSize = size;
            return this;
        }

        public RateLimits build() {
            return new RateLimits(this);
        }
    }
}