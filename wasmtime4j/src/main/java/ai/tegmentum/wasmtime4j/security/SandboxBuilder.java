package ai.tegmentum.wasmtime4j.security;

import java.time.Duration;

/**
 * Builder for creating sandbox instances.
 *
 * @since 1.0.0
 */
public interface SandboxBuilder {

    /**
     * Sets the security policy for the sandbox.
     *
     * @param policy the security policy
     * @return this builder for method chaining
     */
    SandboxBuilder withSecurityPolicy(final SecurityPolicy policy);

    /**
     * Sets the resource limits for the sandbox.
     *
     * @param limits the resource limits
     * @return this builder for method chaining
     */
    SandboxBuilder withResourceLimits(final ResourceLimits limits);

    /**
     * Enables or disables process isolation.
     *
     * @param enabled true to enable process isolation
     * @return this builder for method chaining
     */
    SandboxBuilder withProcessIsolation(final boolean enabled);

    /**
     * Enables or disables monitoring.
     *
     * @param enabled true to enable monitoring
     * @return this builder for method chaining
     */
    SandboxBuilder withMonitoring(final boolean enabled);

    /**
     * Enables or disables intrusion detection.
     *
     * @param enabled true to enable intrusion detection
     * @return this builder for method chaining
     */
    SandboxBuilder withIntrusionDetection(final boolean enabled);

    /**
     * Sets execution timeouts.
     *
     * @param timeout the execution timeout
     * @return this builder for method chaining
     */
    SandboxBuilder withTimeouts(final Duration timeout);

    /**
     * Creates the configured sandbox.
     *
     * @return a new sandbox instance
     * @throws SandboxException if sandbox creation fails
     */
    Sandbox build() throws SandboxException;
}