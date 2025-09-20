package ai.tegmentum.wasmtime4j.security;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Enterprise-grade security policy interface for WebAssembly runtime operations.
 *
 * <p>Provides comprehensive capability-based security model with fine-grained access control,
 * resource restrictions, and security auditing capabilities. Implements defense-in-depth
 * approach to WebAssembly security.
 *
 * <p>Security policies are enforced at multiple layers:
 * <ul>
 *   <li>Module compilation and loading</li>
 *   <li>Instance creation and execution</li>
 *   <li>Host function calls and system interactions</li>
 *   <li>Memory and resource access</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface SecurityPolicy {

    /**
     * Creates a new security policy builder with default restrictive settings.
     *
     * @return a new SecurityPolicyBuilder instance
     */
    static SecurityPolicyBuilder builder() {
        // Use runtime selection pattern to find appropriate implementation
        try {
            // Try Panama implementation first
            final Class<?> builderClass =
                Class.forName("ai.tegmentum.wasmtime4j.panama.security.PanamaSecurityPolicyBuilder");
            return (SecurityPolicyBuilder) builderClass.getDeclaredConstructor().newInstance();
        } catch (final ClassNotFoundException e) {
            // Panama not available, try JNI implementation
            try {
                final Class<?> builderClass =
                    Class.forName("ai.tegmentum.wasmtime4j.jni.security.JniSecurityPolicyBuilder");
                return (SecurityPolicyBuilder) builderClass.getDeclaredConstructor().newInstance();
            } catch (final ClassNotFoundException e2) {
                throw new UnsupportedOperationException(
                    "No SecurityPolicyBuilder implementation available. " +
                    "Ensure wasmtime4j-panama or wasmtime4j-jni is on the classpath.");
            } catch (final Exception e2) {
                throw new RuntimeException("Failed to create security policy builder", e2);
            }
        } catch (final Exception e) {
            throw new RuntimeException("Failed to create security policy builder", e);
        }
    }

    /**
     * Creates a permissive security policy suitable for development and testing.
     *
     * <p><strong>WARNING:</strong> This policy grants broad permissions and should
     * never be used in production environments.
     *
     * @return permissive security policy
     */
    static SecurityPolicy permissive() {
        return builder()
            .withPermissiveMode(true)
            .withAuditingEnabled(false)
            .build();
    }

    /**
     * Creates a restrictive security policy suitable for production environments.
     *
     * <p>This policy enforces strict capability-based access control with minimal
     * permissions and comprehensive auditing.
     *
     * @return restrictive security policy
     */
    static SecurityPolicy restrictive() {
        return builder()
            .withRestrictiveMode(true)
            .withAuditingEnabled(true)
            .withIntrusionDetection(true)
            .build();
    }

    /**
     * Creates a sandbox-ready security policy for untrusted code execution.
     *
     * <p>This policy provides maximum isolation with strict resource limits,
     * no host access, and comprehensive monitoring.
     *
     * @return sandbox security policy
     */
    static SecurityPolicy sandbox() {
        return builder()
            .withSandboxMode(true)
            .withHostFunctionAccess(false)
            .withNetworkAccess(false)
            .withFileSystemAccess(false)
            .withResourceLimits(ResourceLimits.minimal())
            .build();
    }

    /**
     * Checks if a specific capability is granted by this policy.
     *
     * @param capability the capability to check
     * @return true if the capability is granted, false otherwise
     */
    boolean hasCapability(final Capability capability);

    /**
     * Gets all capabilities granted by this security policy.
     *
     * @return immutable set of granted capabilities
     */
    Set<Capability> getGrantedCapabilities();

    /**
     * Checks if module loading is allowed for the specified module source.
     *
     * @param moduleSource the source of the WebAssembly module
     * @param moduleHash optional cryptographic hash for verification
     * @return true if module loading is allowed, false otherwise
     */
    boolean isModuleLoadingAllowed(final String moduleSource, final Optional<String> moduleHash);

    /**
     * Checks if instance creation is allowed with the specified configuration.
     *
     * @param instanceConfig the instance configuration parameters
     * @return true if instance creation is allowed, false otherwise
     */
    boolean isInstanceCreationAllowed(final InstanceConfig instanceConfig);

    /**
     * Checks if host function execution is allowed.
     *
     * @param functionName the name of the host function
     * @param parameters the function parameters
     * @return true if execution is allowed, false otherwise
     */
    boolean isHostFunctionExecutionAllowed(final String functionName, final Object[] parameters);

    /**
     * Checks if memory access is allowed for the specified operation.
     *
     * @param memoryAccess the memory access details
     * @return true if access is allowed, false otherwise
     */
    boolean isMemoryAccessAllowed(final MemoryAccess memoryAccess);

    /**
     * Gets the resource limits enforced by this policy.
     *
     * @return resource limits configuration
     */
    ResourceLimits getResourceLimits();

    /**
     * Gets the security context associated with this policy.
     *
     * @return security context
     */
    SecurityContext getSecurityContext();

    /**
     * Checks if audit logging is enabled for the specified event type.
     *
     * @param eventType the type of security event
     * @return true if auditing is enabled, false otherwise
     */
    boolean isAuditingEnabled(final SecurityEventType eventType);

    /**
     * Gets the intrusion detection configuration.
     *
     * @return intrusion detection settings, or empty if disabled
     */
    Optional<IntrusionDetectionConfig> getIntrusionDetection();

    /**
     * Validates this security policy for completeness and consistency.
     *
     * <p>This method performs comprehensive validation including:
     * <ul>
     *   <li>Capability consistency checks</li>
     *   <li>Resource limit validation</li>
     *   <li>Security context verification</li>
     *   <li>Policy rule conflict detection</li>
     * </ul>
     *
     * @throws SecurityPolicyException if the policy is invalid or inconsistent
     */
    void validate() throws SecurityPolicyException;

    /**
     * Creates a derived policy with additional restrictions.
     *
     * <p>The derived policy will have all the restrictions of this policy
     * plus any additional restrictions specified in the modifier.
     *
     * @param modifier the security policy modifier
     * @return a new, more restrictive security policy
     */
    SecurityPolicy withAdditionalRestrictions(final SecurityPolicyModifier modifier);

    /**
     * Gets the policy enforcement level.
     *
     * @return enforcement level (STRICT, PERMISSIVE, AUDIT_ONLY)
     */
    EnforcementLevel getEnforcementLevel();

    /**
     * Gets the policy version and metadata.
     *
     * @return policy metadata
     */
    PolicyMetadata getMetadata();
}