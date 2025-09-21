package ai.tegmentum.wasmtime4j.panama.security;

import ai.tegmentum.wasmtime4j.security.*;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Panama implementation of SecurityPolicyBuilder.
 *
 * @since 1.0.0
 */
public final class PanamaSecurityPolicyBuilder implements SecurityPolicyBuilder {

  private boolean permissiveMode = false;
  private boolean restrictiveMode = false;
  private boolean sandboxMode = false;
  private final Set<Capability> capabilities = EnumSet.noneOf(Capability.class);
  private ResourceLimits resourceLimits = ResourceLimits.standard();
  private SecurityContext securityContext = SecurityContext.defaultContext();
  private boolean auditingEnabled = true;
  private Set<SecurityEventType> auditEventTypes = EnumSet.allOf(SecurityEventType.class);
  private boolean intrusionDetectionEnabled = false;
  private IntrusionDetectionConfig intrusionDetectionConfig;
  private EnforcementLevel enforcementLevel = EnforcementLevel.STRICT;
  private boolean moduleHashVerificationRequired = true;
  private Set<String> trustedModuleSources = new HashSet<>();
  private RateLimits rateLimits = RateLimits.defaultLimits();
  private Set<SecurityValidator> customValidators = new HashSet<>();
  private PolicyMetadata metadata = PolicyMetadata.defaultMetadata();

  public PanamaSecurityPolicyBuilder() {
    // Initialize with secure defaults
    setRestrictiveDefaults();
  }

  @Override
  public SecurityPolicyBuilder withPermissiveMode(final boolean permissive) {
    this.permissiveMode = permissive;
    if (permissive) {
      setPermissiveDefaults();
    }
    return this;
  }

  @Override
  public SecurityPolicyBuilder withRestrictiveMode(final boolean restrictive) {
    this.restrictiveMode = restrictive;
    if (restrictive) {
      setRestrictiveDefaults();
    }
    return this;
  }

  @Override
  public SecurityPolicyBuilder withSandboxMode(final boolean sandbox) {
    this.sandboxMode = sandbox;
    if (sandbox) {
      setSandboxDefaults();
    }
    return this;
  }

  @Override
  public SecurityPolicyBuilder withCapabilities(final Set<Capability> capabilities) {
    this.capabilities.clear();
    this.capabilities.addAll(capabilities);
    return this;
  }

  @Override
  public SecurityPolicyBuilder withCapability(final Capability capability) {
    this.capabilities.add(capability);
    return this;
  }

  @Override
  public SecurityPolicyBuilder withoutCapabilities(final Set<Capability> capabilities) {
    this.capabilities.removeAll(capabilities);
    return this;
  }

  @Override
  public SecurityPolicyBuilder withoutCapability(final Capability capability) {
    this.capabilities.remove(capability);
    return this;
  }

  @Override
  public SecurityPolicyBuilder withResourceLimits(final ResourceLimits limits) {
    this.resourceLimits = limits;
    return this;
  }

  @Override
  public SecurityPolicyBuilder withMaxMemory(final long maxMemoryBytes) {
    this.resourceLimits =
        ResourceLimits.builder()
            .withMaxMemory(maxMemoryBytes)
            .withMaxExecutionTime(resourceLimits.getMaxExecutionTime())
            .withMaxInstructions(resourceLimits.getMaxInstructions())
            .withMaxInstances(resourceLimits.getMaxInstances())
            .withMaxModules(resourceLimits.getMaxModules())
            .withMaxFileDescriptors(resourceLimits.getMaxFileDescriptors())
            .withMaxNetworkConnections(resourceLimits.getMaxNetworkConnections())
            .withMaxThreads(resourceLimits.getMaxThreads())
            .withMaxCallDepth(resourceLimits.getMaxCallDepth())
            .withPreemption(resourceLimits.isPreemptionEnabled())
            .build();
    return this;
  }

  @Override
  public SecurityPolicyBuilder withMaxExecutionTime(final Duration maxExecutionTime) {
    this.resourceLimits =
        ResourceLimits.builder()
            .withMaxMemory(resourceLimits.getMaxMemoryBytes())
            .withMaxExecutionTime(maxExecutionTime)
            .withMaxInstructions(resourceLimits.getMaxInstructions())
            .withMaxInstances(resourceLimits.getMaxInstances())
            .withMaxModules(resourceLimits.getMaxModules())
            .withMaxFileDescriptors(resourceLimits.getMaxFileDescriptors())
            .withMaxNetworkConnections(resourceLimits.getMaxNetworkConnections())
            .withMaxThreads(resourceLimits.getMaxThreads())
            .withMaxCallDepth(resourceLimits.getMaxCallDepth())
            .withPreemption(resourceLimits.isPreemptionEnabled())
            .build();
    return this;
  }

  @Override
  public SecurityPolicyBuilder withMaxInstructions(final long maxInstructions) {
    this.resourceLimits =
        ResourceLimits.builder()
            .withMaxMemory(resourceLimits.getMaxMemoryBytes())
            .withMaxExecutionTime(resourceLimits.getMaxExecutionTime())
            .withMaxInstructions(maxInstructions)
            .withMaxInstances(resourceLimits.getMaxInstances())
            .withMaxModules(resourceLimits.getMaxModules())
            .withMaxFileDescriptors(resourceLimits.getMaxFileDescriptors())
            .withMaxNetworkConnections(resourceLimits.getMaxNetworkConnections())
            .withMaxThreads(resourceLimits.getMaxThreads())
            .withMaxCallDepth(resourceLimits.getMaxCallDepth())
            .withPreemption(resourceLimits.isPreemptionEnabled())
            .build();
    return this;
  }

  @Override
  public SecurityPolicyBuilder withHostFunctionAccess(final boolean allowed) {
    if (allowed) {
      capabilities.add(Capability.HOST_FUNCTION_CALL);
    } else {
      capabilities.remove(Capability.HOST_FUNCTION_CALL);
    }
    return this;
  }

  @Override
  public SecurityPolicyBuilder withNetworkAccess(final boolean allowed) {
    if (allowed) {
      capabilities.add(Capability.NETWORK_CONNECT);
    } else {
      capabilities.remove(Capability.NETWORK_CONNECT);
    }
    return this;
  }

  @Override
  public SecurityPolicyBuilder withFileSystemAccess(final boolean allowed) {
    if (allowed) {
      capabilities.add(Capability.FILESYSTEM_READ);
      capabilities.add(Capability.FILESYSTEM_WRITE);
    } else {
      capabilities.remove(Capability.FILESYSTEM_READ);
      capabilities.remove(Capability.FILESYSTEM_WRITE);
    }
    return this;
  }

  @Override
  public SecurityPolicyBuilder withSecurityContext(final SecurityContext context) {
    this.securityContext = context;
    return this;
  }

  @Override
  public SecurityPolicyBuilder withAuditingEnabled(final boolean enabled) {
    this.auditingEnabled = enabled;
    return this;
  }

  @Override
  public SecurityPolicyBuilder withAuditEventTypes(final Set<SecurityEventType> eventTypes) {
    this.auditEventTypes = EnumSet.copyOf(eventTypes);
    return this;
  }

  @Override
  public SecurityPolicyBuilder withIntrusionDetection(final boolean enabled) {
    this.intrusionDetectionEnabled = enabled;
    return this;
  }

  @Override
  public SecurityPolicyBuilder withIntrusionDetectionConfig(final IntrusionDetectionConfig config) {
    this.intrusionDetectionConfig = config;
    this.intrusionDetectionEnabled = (config != null);
    return this;
  }

  @Override
  public SecurityPolicyBuilder withEnforcementLevel(final EnforcementLevel level) {
    this.enforcementLevel = level;
    return this;
  }

  @Override
  public SecurityPolicyBuilder withModuleHashVerification(final boolean required) {
    this.moduleHashVerificationRequired = required;
    return this;
  }

  @Override
  public SecurityPolicyBuilder withTrustedModuleSources(final Set<String> sources) {
    this.trustedModuleSources.clear();
    this.trustedModuleSources.addAll(sources);
    return this;
  }

  @Override
  public SecurityPolicyBuilder withRateLimits(final RateLimits rateLimits) {
    this.rateLimits = rateLimits;
    return this;
  }

  @Override
  public SecurityPolicyBuilder withCustomValidators(final Set<SecurityValidator> validators) {
    this.customValidators.clear();
    this.customValidators.addAll(validators);
    return this;
  }

  @Override
  public SecurityPolicyBuilder withMetadata(final PolicyMetadata metadata) {
    this.metadata = metadata;
    return this;
  }

  @Override
  public SecurityPolicy build() throws SecurityPolicyException {
    validate();
    return new PanamaSecurityPolicy(
        EnumSet.copyOf(capabilities),
        resourceLimits,
        securityContext,
        auditingEnabled,
        EnumSet.copyOf(auditEventTypes),
        intrusionDetectionEnabled,
        intrusionDetectionConfig,
        enforcementLevel,
        moduleHashVerificationRequired,
        Set.copyOf(trustedModuleSources),
        rateLimits,
        Set.copyOf(customValidators),
        metadata);
  }

  private void setPermissiveDefaults() {
    capabilities.addAll(EnumSet.allOf(Capability.class));
    capabilities.remove(Capability.SECURITY_BYPASS);
    capabilities.remove(Capability.SYSTEM_ADMIN);
    enforcementLevel = EnforcementLevel.PERMISSIVE;
    auditingEnabled = false;
    intrusionDetectionEnabled = false;
    moduleHashVerificationRequired = false;
  }

  private void setRestrictiveDefaults() {
    capabilities.clear();
    // Add only minimal safe capabilities
    capabilities.add(Capability.MODULE_LOAD_TRUSTED);
    capabilities.add(Capability.MODULE_COMPILE);
    capabilities.add(Capability.INSTANCE_CREATE);
    capabilities.add(Capability.INSTANCE_EXECUTE);
    capabilities.add(Capability.MEMORY_READ);
    capabilities.add(Capability.MEMORY_WRITE);
    enforcementLevel = EnforcementLevel.STRICT;
    auditingEnabled = true;
    intrusionDetectionEnabled = true;
    moduleHashVerificationRequired = true;
  }

  private void setSandboxDefaults() {
    capabilities.clear();
    // Sandbox mode allows only most basic operations
    capabilities.add(Capability.MODULE_LOAD_TRUSTED);
    capabilities.add(Capability.MODULE_COMPILE);
    capabilities.add(Capability.INSTANCE_CREATE);
    capabilities.add(Capability.INSTANCE_EXECUTE);
    capabilities.add(Capability.MEMORY_READ);
    capabilities.add(Capability.MEMORY_WRITE);
    enforcementLevel = EnforcementLevel.STRICT;
    auditingEnabled = true;
    intrusionDetectionEnabled = true;
    moduleHashVerificationRequired = true;
    resourceLimits = ResourceLimits.minimal();
  }

  private void validate() throws SecurityPolicyException {
    if (permissiveMode && sandboxMode) {
      throw new SecurityPolicyException("Cannot enable both permissive and sandbox modes");
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

    // Validate capability combinations
    if (capabilities.contains(Capability.SECURITY_BYPASS)
        && !capabilities.contains(Capability.SYSTEM_ADMIN)) {
      throw new SecurityPolicyException(
          "SECURITY_BYPASS capability requires SYSTEM_ADMIN capability");
    }

    // Validate that sandbox mode doesn't have dangerous capabilities
    if (sandboxMode) {
      final Set<Capability> dangerousCapabilities =
          EnumSet.of(
              Capability.HOST_NATIVE_ACCESS,
              Capability.FILESYSTEM_DELETE,
              Capability.PROCESS_SPAWN,
              Capability.NETWORK_RAW_SOCKET,
              Capability.SECURITY_POLICY_MODIFY,
              Capability.SYSTEM_ADMIN);

      for (final Capability cap : dangerousCapabilities) {
        if (capabilities.contains(cap)) {
          throw new SecurityPolicyException(
              "Sandbox mode cannot have dangerous capability: " + cap.getId());
        }
      }
    }

    try {
      resourceLimits.validate();
    } catch (final IllegalArgumentException e) {
      throw new SecurityPolicyException("Invalid resource limits: " + e.getMessage(), e);
    }
  }
}
