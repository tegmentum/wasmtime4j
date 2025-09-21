package ai.tegmentum.wasmtime4j.jni.security;

import ai.tegmentum.wasmtime4j.security.*;
import java.util.Optional;
import java.util.Set;

/**
 * JNI implementation of SecurityPolicy.
 *
 * @since 1.0.0
 */
public final class JniSecurityPolicy implements SecurityPolicy {

  private final Set<Capability> grantedCapabilities;
  private final ResourceLimits resourceLimits;
  private final SecurityContext securityContext;
  private final boolean auditingEnabled;
  private final Set<SecurityEventType> auditEventTypes;
  private final boolean intrusionDetectionEnabled;
  private final IntrusionDetectionConfig intrusionDetectionConfig;
  private final EnforcementLevel enforcementLevel;
  private final boolean moduleHashVerificationRequired;
  private final Set<String> trustedModuleSources;
  private final RateLimits rateLimits;
  private final Set<SecurityValidator> customValidators;
  private final PolicyMetadata metadata;

  public JniSecurityPolicy(
      final Set<Capability> grantedCapabilities,
      final ResourceLimits resourceLimits,
      final SecurityContext securityContext,
      final boolean auditingEnabled,
      final Set<SecurityEventType> auditEventTypes,
      final boolean intrusionDetectionEnabled,
      final IntrusionDetectionConfig intrusionDetectionConfig,
      final EnforcementLevel enforcementLevel,
      final boolean moduleHashVerificationRequired,
      final Set<String> trustedModuleSources,
      final RateLimits rateLimits,
      final Set<SecurityValidator> customValidators,
      final PolicyMetadata metadata) {
    this.grantedCapabilities = Set.copyOf(grantedCapabilities);
    this.resourceLimits = resourceLimits;
    this.securityContext = securityContext;
    this.auditingEnabled = auditingEnabled;
    this.auditEventTypes = Set.copyOf(auditEventTypes);
    this.intrusionDetectionEnabled = intrusionDetectionEnabled;
    this.intrusionDetectionConfig = intrusionDetectionConfig;
    this.enforcementLevel = enforcementLevel;
    this.moduleHashVerificationRequired = moduleHashVerificationRequired;
    this.trustedModuleSources = Set.copyOf(trustedModuleSources);
    this.rateLimits = rateLimits;
    this.customValidators = Set.copyOf(customValidators);
    this.metadata = metadata;
  }

  @Override
  public boolean hasCapability(final Capability capability) {
    // Check direct capability
    if (grantedCapabilities.contains(capability)) {
      return true;
    }

    // Check implied capabilities
    for (final Capability granted : grantedCapabilities) {
      if (granted.implies(capability)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public Set<Capability> getGrantedCapabilities() {
    return grantedCapabilities;
  }

  @Override
  public boolean isModuleLoadingAllowed(
      final String moduleSource, final Optional<String> moduleHash) {
    // Check if module loading capability is granted
    if (!hasCapability(Capability.MODULE_LOAD) && !hasCapability(Capability.MODULE_LOAD_TRUSTED)) {
      return false;
    }

    // If only trusted module loading is allowed, check source
    if (!hasCapability(Capability.MODULE_LOAD) && hasCapability(Capability.MODULE_LOAD_TRUSTED)) {
      if (!trustedModuleSources.contains(moduleSource)) {
        return false;
      }
    }

    // Check hash verification requirement
    if (moduleHashVerificationRequired && moduleHash.isEmpty()) {
      return false;
    }

    return true;
  }

  @Override
  public boolean isInstanceCreationAllowed(final InstanceConfig instanceConfig) {
    return hasCapability(Capability.INSTANCE_CREATE);
  }

  @Override
  public boolean isHostFunctionExecutionAllowed(
      final String functionName, final Object[] parameters) {
    if (!hasCapability(Capability.HOST_FUNCTION_CALL)) {
      return false;
    }

    // Check for native access requirements
    if (isNativeFunction(functionName) && !hasCapability(Capability.HOST_NATIVE_ACCESS)) {
      return false;
    }

    return true;
  }

  @Override
  public boolean isMemoryAccessAllowed(final MemoryAccess memoryAccess) {
    switch (memoryAccess.getOperation()) {
      case READ:
        return hasCapability(Capability.MEMORY_READ);
      case WRITE:
        return hasCapability(Capability.MEMORY_WRITE);
      case GROW:
        return hasCapability(Capability.MEMORY_GROW);
      case BOUNDS_CHECK:
        return true; // Always allowed for safety
      default:
        return false;
    }
  }

  @Override
  public ResourceLimits getResourceLimits() {
    return resourceLimits;
  }

  @Override
  public SecurityContext getSecurityContext() {
    return securityContext;
  }

  @Override
  public boolean isAuditingEnabled(final SecurityEventType eventType) {
    return auditingEnabled && auditEventTypes.contains(eventType);
  }

  @Override
  public Optional<IntrusionDetectionConfig> getIntrusionDetection() {
    return intrusionDetectionEnabled
        ? Optional.ofNullable(intrusionDetectionConfig)
        : Optional.empty();
  }

  @Override
  public void validate() throws SecurityPolicyException {
    if (grantedCapabilities == null) {
      throw new SecurityPolicyException("Granted capabilities cannot be null");
    }

    if (resourceLimits == null) {
      throw new SecurityPolicyException("Resource limits cannot be null");
    }

    if (securityContext == null) {
      throw new SecurityPolicyException("Security context cannot be null");
    }

    if (enforcementLevel == null) {
      throw new SecurityPolicyException("Enforcement level cannot be null");
    }

    try {
      resourceLimits.validate();
    } catch (final IllegalArgumentException e) {
      throw new SecurityPolicyException("Invalid resource limits: " + e.getMessage(), e);
    }

    // Validate capability consistency
    if (grantedCapabilities.contains(Capability.SECURITY_BYPASS)
        && !grantedCapabilities.contains(Capability.SYSTEM_ADMIN)) {
      throw new SecurityPolicyException(
          "SECURITY_BYPASS capability requires SYSTEM_ADMIN capability");
    }

    // Run custom validators
    for (final SecurityValidator validator : customValidators) {
      final SecurityEvent testEvent =
          SecurityEvent.of(SecurityEventType.SECURITY_POLICY_CHANGED, "Policy validation check");
      final SecurityValidator.ValidationResult result = validator.validate(testEvent);
      if (!result.isValid()) {
        throw new SecurityPolicyException("Custom validator failed: " + result.getMessage());
      }
    }
  }

  @Override
  public SecurityPolicy withAdditionalRestrictions(final SecurityPolicyModifier modifier) {
    // Create a new policy with additional restrictions
    final Set<Capability> newCapabilities = Set.copyOf(grantedCapabilities);
    newCapabilities.removeAll(modifier.getCapabilitiesToRevoke());

    final ResourceLimits newLimits = resourceLimits.restrictTo(modifier.getAdditionalLimits());

    return new JniSecurityPolicy(
        newCapabilities,
        newLimits,
        securityContext,
        auditingEnabled,
        auditEventTypes,
        intrusionDetectionEnabled,
        intrusionDetectionConfig,
        modifier.getEnforcementLevel(),
        moduleHashVerificationRequired,
        trustedModuleSources,
        rateLimits,
        customValidators,
        metadata);
  }

  @Override
  public EnforcementLevel getEnforcementLevel() {
    return enforcementLevel;
  }

  @Override
  public PolicyMetadata getMetadata() {
    return metadata;
  }

  private boolean isNativeFunction(final String functionName) {
    // Simple heuristic to detect native functions
    return functionName.startsWith("native_")
        || functionName.contains("system")
        || functionName.contains("file")
        || functionName.contains("process");
  }
}
