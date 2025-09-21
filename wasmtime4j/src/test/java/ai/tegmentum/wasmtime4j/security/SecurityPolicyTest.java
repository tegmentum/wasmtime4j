package ai.tegmentum.wasmtime4j.security;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for security policy functionality.
 *
 * @since 1.0.0
 */
class SecurityPolicyTest {

  private SecurityPolicy restrictivePolicy;
  private SecurityPolicy permissivePolicy;
  private SecurityPolicy sandboxPolicy;

  @BeforeEach
  void setUp() {
    restrictivePolicy = SecurityPolicy.restrictive();
    permissivePolicy = SecurityPolicy.permissive();
    sandboxPolicy = SecurityPolicy.sandbox();
  }

  @Nested
  @DisplayName("Security Policy Creation Tests")
  class PolicyCreationTests {

    @Test
    @DisplayName("Should create restrictive policy with minimal capabilities")
    void shouldCreateRestrictivePolicyWithMinimalCapabilities() {
      final Set<Capability> capabilities = restrictivePolicy.getGrantedCapabilities();

      // Should have basic capabilities
      assertTrue(capabilities.contains(Capability.MODULE_LOAD_TRUSTED));
      assertTrue(capabilities.contains(Capability.INSTANCE_CREATE));
      assertTrue(capabilities.contains(Capability.INSTANCE_EXECUTE));
      assertTrue(capabilities.contains(Capability.MEMORY_READ));
      assertTrue(capabilities.contains(Capability.MEMORY_WRITE));

      // Should NOT have dangerous capabilities
      assertFalse(capabilities.contains(Capability.HOST_NATIVE_ACCESS));
      assertFalse(capabilities.contains(Capability.FILESYSTEM_DELETE));
      assertFalse(capabilities.contains(Capability.PROCESS_SPAWN));
      assertFalse(capabilities.contains(Capability.SECURITY_BYPASS));
      assertFalse(capabilities.contains(Capability.SYSTEM_ADMIN));
    }

    @Test
    @DisplayName("Should create permissive policy with most capabilities")
    void shouldCreatePermissivePolicyWithMostCapabilities() {
      final Set<Capability> capabilities = permissivePolicy.getGrantedCapabilities();

      // Should have most capabilities except dangerous ones
      assertTrue(capabilities.contains(Capability.MODULE_LOAD));
      assertTrue(capabilities.contains(Capability.FILESYSTEM_READ));
      assertTrue(capabilities.contains(Capability.NETWORK_CONNECT));

      // Should NOT have the most dangerous capabilities
      assertFalse(capabilities.contains(Capability.SECURITY_BYPASS));
      assertFalse(capabilities.contains(Capability.SYSTEM_ADMIN));
    }

    @Test
    @DisplayName("Should create sandbox policy with minimal capabilities and strict limits")
    void shouldCreateSandboxPolicyWithMinimalCapabilities() {
      final Set<Capability> capabilities = sandboxPolicy.getGrantedCapabilities();
      final ResourceLimits limits = sandboxPolicy.getResourceLimits();

      // Should have only most basic capabilities
      assertTrue(capabilities.contains(Capability.MODULE_LOAD_TRUSTED));
      assertTrue(capabilities.contains(Capability.INSTANCE_EXECUTE));

      // Should NOT have dangerous capabilities
      assertFalse(capabilities.contains(Capability.HOST_FUNCTION_CALL));
      assertFalse(capabilities.contains(Capability.NETWORK_CONNECT));
      assertFalse(capabilities.contains(Capability.FILESYSTEM_WRITE));

      // Should have strict resource limits
      assertEquals(16 * 1024 * 1024, limits.getMaxMemoryBytes()); // 16MB
      assertEquals(Duration.ofSeconds(5), limits.getMaxExecutionTime());
      assertEquals(1_000_000, limits.getMaxInstructions());
    }

    @Test
    @DisplayName("Should build custom policy with specific capabilities")
    void shouldBuildCustomPolicyWithSpecificCapabilities() {
      final SecurityPolicy customPolicy =
          SecurityPolicy.builder()
              .withCapability(Capability.FILESYSTEM_READ)
              .withCapability(Capability.NETWORK_CONNECT)
              .withoutCapability(Capability.FILESYSTEM_WRITE)
              .withMaxMemory(64 * 1024 * 1024) // 64MB
              .withMaxExecutionTime(Duration.ofSeconds(15))
              .withEnforcementLevel(EnforcementLevel.STRICT)
              .build();

      assertTrue(customPolicy.hasCapability(Capability.FILESYSTEM_READ));
      assertTrue(customPolicy.hasCapability(Capability.NETWORK_CONNECT));
      assertFalse(customPolicy.hasCapability(Capability.FILESYSTEM_WRITE));

      assertEquals(64 * 1024 * 1024, customPolicy.getResourceLimits().getMaxMemoryBytes());
      assertEquals(Duration.ofSeconds(15), customPolicy.getResourceLimits().getMaxExecutionTime());
      assertEquals(EnforcementLevel.STRICT, customPolicy.getEnforcementLevel());
    }
  }

  @Nested
  @DisplayName("Capability Validation Tests")
  class CapabilityValidationTests {

    @Test
    @DisplayName("Should validate capability inheritance correctly")
    void shouldValidateCapabilityInheritanceCorrectly() {
      final SecurityPolicy policyWithAdmin =
          SecurityPolicy.builder().withCapability(Capability.SYSTEM_ADMIN).build();

      // SYSTEM_ADMIN should imply most other capabilities
      assertTrue(policyWithAdmin.hasCapability(Capability.FILESYSTEM_READ));
      assertTrue(policyWithAdmin.hasCapability(Capability.MEMORY_READ));
      assertTrue(policyWithAdmin.hasCapability(Capability.NETWORK_CONNECT));
    }

    @Test
    @DisplayName("Should validate file system write implies create capability")
    void shouldValidateFileSystemWriteImpliesCreate() {
      final SecurityPolicy policyWithWrite =
          SecurityPolicy.builder().withCapability(Capability.FILESYSTEM_WRITE).build();

      assertTrue(policyWithWrite.hasCapability(Capability.FILESYSTEM_WRITE));
      assertTrue(policyWithWrite.hasCapability(Capability.FILESYSTEM_CREATE));
    }

    @Test
    @DisplayName("Should validate memory capabilities")
    void shouldValidateMemoryCapabilities() {
      final SecurityPolicy memoryPolicy =
          SecurityPolicy.builder().withCapability(Capability.RESOURCE_UNLIMITED_MEMORY).build();

      assertTrue(memoryPolicy.hasCapability(Capability.MEMORY_READ));
      assertTrue(memoryPolicy.hasCapability(Capability.MEMORY_WRITE));
      assertTrue(memoryPolicy.hasCapability(Capability.MEMORY_GROW));
    }
  }

  @Nested
  @DisplayName("Module Loading Validation Tests")
  class ModuleLoadingValidationTests {

    @Test
    @DisplayName("Should allow trusted module loading with trusted source")
    void shouldAllowTrustedModuleLoadingWithTrustedSource() {
      final SecurityPolicy trustedPolicy =
          SecurityPolicy.builder()
              .withCapability(Capability.MODULE_LOAD_TRUSTED)
              .withTrustedModuleSources(Set.of("trusted-source"))
              .build();

      assertTrue(trustedPolicy.isModuleLoadingAllowed("trusted-source", Optional.empty()));
      assertFalse(trustedPolicy.isModuleLoadingAllowed("untrusted-source", Optional.empty()));
    }

    @Test
    @DisplayName("Should require hash verification when configured")
    void shouldRequireHashVerificationWhenConfigured() {
      final SecurityPolicy hashPolicy =
          SecurityPolicy.builder()
              .withCapability(Capability.MODULE_LOAD)
              .withModuleHashVerification(true)
              .build();

      assertFalse(hashPolicy.isModuleLoadingAllowed("any-source", Optional.empty()));
      assertTrue(hashPolicy.isModuleLoadingAllowed("any-source", Optional.of("sha256:abc123")));
    }

    @Test
    @DisplayName("Should reject module loading without capability")
    void shouldRejectModuleLoadingWithoutCapability() {
      final SecurityPolicy noLoadPolicy =
          SecurityPolicy.builder()
              .withoutCapability(Capability.MODULE_LOAD)
              .withoutCapability(Capability.MODULE_LOAD_TRUSTED)
              .build();

      assertFalse(noLoadPolicy.isModuleLoadingAllowed("any-source", Optional.empty()));
    }
  }

  @Nested
  @DisplayName("Host Function Validation Tests")
  class HostFunctionValidationTests {

    @Test
    @DisplayName("Should allow host function calls with capability")
    void shouldAllowHostFunctionCallsWithCapability() {
      final SecurityPolicy hostPolicy =
          SecurityPolicy.builder().withCapability(Capability.HOST_FUNCTION_CALL).build();

      assertTrue(hostPolicy.isHostFunctionExecutionAllowed("safe_function", new Object[] {}));
      assertTrue(hostPolicy.isHostFunctionExecutionAllowed("log", new Object[] {"message"}));
    }

    @Test
    @DisplayName("Should reject native functions without native access capability")
    void shouldRejectNativeFunctionsWithoutNativeAccessCapability() {
      final SecurityPolicy hostPolicy =
          SecurityPolicy.builder()
              .withCapability(Capability.HOST_FUNCTION_CALL)
              .withoutCapability(Capability.HOST_NATIVE_ACCESS)
              .build();

      assertFalse(hostPolicy.isHostFunctionExecutionAllowed("native_system_call", new Object[] {}));
      assertFalse(hostPolicy.isHostFunctionExecutionAllowed("file_operation", new Object[] {}));
    }

    @Test
    @DisplayName("Should allow native functions with native access capability")
    void shouldAllowNativeFunctionsWithNativeAccessCapability() {
      final SecurityPolicy nativePolicy =
          SecurityPolicy.builder()
              .withCapability(Capability.HOST_FUNCTION_CALL)
              .withCapability(Capability.HOST_NATIVE_ACCESS)
              .build();

      assertTrue(
          nativePolicy.isHostFunctionExecutionAllowed("native_system_call", new Object[] {}));
      assertTrue(nativePolicy.isHostFunctionExecutionAllowed("file_operation", new Object[] {}));
    }
  }

  @Nested
  @DisplayName("Memory Access Validation Tests")
  class MemoryAccessValidationTests {

    @Test
    @DisplayName("Should validate memory read operations")
    void shouldValidateMemoryReadOperations() {
      final SecurityPolicy readPolicy =
          SecurityPolicy.builder()
              .withCapability(Capability.MEMORY_READ)
              .withoutCapability(Capability.MEMORY_WRITE)
              .build();

      final MemoryAccess readAccess =
          new MemoryAccess(
              MemoryAccess.MemoryOperation.READ, 0, 1024, "test-module", "test-function");
      final MemoryAccess writeAccess =
          new MemoryAccess(
              MemoryAccess.MemoryOperation.WRITE, 0, 1024, "test-module", "test-function");

      assertTrue(readPolicy.isMemoryAccessAllowed(readAccess));
      assertFalse(readPolicy.isMemoryAccessAllowed(writeAccess));
    }

    @Test
    @DisplayName("Should always allow bounds checking")
    void shouldAlwaysAllowBoundsChecking() {
      final SecurityPolicy minimalPolicy =
          SecurityPolicy.builder()
              .withoutCapability(Capability.MEMORY_READ)
              .withoutCapability(Capability.MEMORY_WRITE)
              .build();

      final MemoryAccess boundsCheck =
          new MemoryAccess(
              MemoryAccess.MemoryOperation.BOUNDS_CHECK, 0, 1024, "test-module", "test-function");

      assertTrue(minimalPolicy.isMemoryAccessAllowed(boundsCheck));
    }
  }

  @Nested
  @DisplayName("Resource Limits Tests")
  class ResourceLimitsTests {

    @Test
    @DisplayName("Should enforce memory limits")
    void shouldEnforceMemoryLimits() {
      final ResourceLimits limits =
          ResourceLimits.builder()
              .withMaxMemory(64 * 1024 * 1024) // 64MB
              .build();

      assertTrue(limits.isMemoryWithinLimits(32 * 1024 * 1024)); // 32MB - within limit
      assertFalse(limits.isMemoryWithinLimits(128 * 1024 * 1024)); // 128MB - exceeds limit
    }

    @Test
    @DisplayName("Should enforce execution time limits")
    void shouldEnforceExecutionTimeLimits() {
      final ResourceLimits limits =
          ResourceLimits.builder().withMaxExecutionTime(Duration.ofSeconds(30)).build();

      assertTrue(limits.isExecutionTimeWithinLimits(Duration.ofSeconds(15)));
      assertFalse(limits.isExecutionTimeWithinLimits(Duration.ofMinutes(2)));
    }

    @Test
    @DisplayName("Should enforce instruction count limits")
    void shouldEnforceInstructionCountLimits() {
      final ResourceLimits limits = ResourceLimits.builder().withMaxInstructions(1_000_000).build();

      assertTrue(limits.isInstructionCountWithinLimits(500_000));
      assertFalse(limits.isInstructionCountWithinLimits(2_000_000));
    }
  }

  @Nested
  @DisplayName("Policy Validation Tests")
  class PolicyValidationTests {

    @Test
    @DisplayName("Should validate consistent policy configuration")
    void shouldValidateConsistentPolicyConfiguration() {
      assertDoesNotThrow(
          () -> {
            final SecurityPolicy validPolicy =
                SecurityPolicy.builder()
                    .withCapability(Capability.MEMORY_READ)
                    .withResourceLimits(ResourceLimits.standard())
                    .build();
            validPolicy.validate();
          });
    }

    @Test
    @DisplayName("Should reject inconsistent capability combinations")
    void shouldRejectInconsistentCapabilityCombinations() {
      assertThrows(
          SecurityPolicyException.class,
          () -> {
            SecurityPolicy.builder()
                .withCapability(Capability.SECURITY_BYPASS)
                .withoutCapability(Capability.SYSTEM_ADMIN)
                .build();
          });
    }

    @Test
    @DisplayName("Should reject dangerous capabilities in sandbox mode")
    void shouldRejectDangerousCapabilitiesInSandboxMode() {
      assertThrows(
          SecurityPolicyException.class,
          () -> {
            SecurityPolicy.builder()
                .withSandboxMode(true)
                .withCapability(Capability.HOST_NATIVE_ACCESS)
                .build();
          });
    }

    @Test
    @DisplayName("Should reject conflicting modes")
    void shouldRejectConflictingModes() {
      assertThrows(
          SecurityPolicyException.class,
          () -> {
            SecurityPolicy.builder().withPermissiveMode(true).withSandboxMode(true).build();
          });
    }
  }

  @Nested
  @DisplayName("Auditing Configuration Tests")
  class AuditingConfigurationTests {

    @Test
    @DisplayName("Should configure auditing for specific event types")
    void shouldConfigureAuditingForSpecificEventTypes() {
      final SecurityPolicy auditPolicy =
          SecurityPolicy.builder()
              .withAuditingEnabled(true)
              .withAuditEventTypes(
                  Set.of(
                      SecurityEventType.SECURITY_POLICY_VIOLATION,
                      SecurityEventType.CAPABILITY_VIOLATION))
              .build();

      assertTrue(auditPolicy.isAuditingEnabled(SecurityEventType.SECURITY_POLICY_VIOLATION));
      assertTrue(auditPolicy.isAuditingEnabled(SecurityEventType.CAPABILITY_VIOLATION));
      assertFalse(auditPolicy.isAuditingEnabled(SecurityEventType.MODULE_LOADED));
    }

    @Test
    @DisplayName("Should disable all auditing when configured")
    void shouldDisableAllAuditingWhenConfigured() {
      final SecurityPolicy noAuditPolicy =
          SecurityPolicy.builder().withAuditingEnabled(false).build();

      assertFalse(noAuditPolicy.isAuditingEnabled(SecurityEventType.SECURITY_POLICY_VIOLATION));
      assertFalse(noAuditPolicy.isAuditingEnabled(SecurityEventType.MODULE_LOADED));
    }
  }

  @Nested
  @DisplayName("Derived Policy Tests")
  class DerivedPolicyTests {

    @Test
    @DisplayName("Should create derived policy with additional restrictions")
    void shouldCreateDerivedPolicyWithAdditionalRestrictions() {
      final SecurityPolicy basePolicy =
          SecurityPolicy.builder()
              .withCapability(Capability.FILESYSTEM_READ)
              .withCapability(Capability.FILESYSTEM_WRITE)
              .withCapability(Capability.NETWORK_CONNECT)
              .withResourceLimits(ResourceLimits.generous())
              .build();

      final SecurityPolicyModifier modifier =
          new SecurityPolicyModifier(
              Set.of(Capability.FILESYSTEM_WRITE, Capability.NETWORK_CONNECT),
              ResourceLimits.minimal(),
              EnforcementLevel.STRICT);

      final SecurityPolicy derivedPolicy = basePolicy.withAdditionalRestrictions(modifier);

      // Should have removed capabilities
      assertTrue(derivedPolicy.hasCapability(Capability.FILESYSTEM_READ));
      assertFalse(derivedPolicy.hasCapability(Capability.FILESYSTEM_WRITE));
      assertFalse(derivedPolicy.hasCapability(Capability.NETWORK_CONNECT));

      // Should have more restrictive resource limits
      assertTrue(
          derivedPolicy.getResourceLimits().getMaxMemoryBytes()
              <= basePolicy.getResourceLimits().getMaxMemoryBytes());
    }
  }

  @Nested
  @DisplayName("Security Context Tests")
  class SecurityContextTests {

    @Test
    @DisplayName("Should create security context with principal information")
    void shouldCreateSecurityContextWithPrincipalInformation() {
      final SecurityContext context =
          SecurityContext.builder()
              .withPrincipalId("test-user")
              .withSecurityLevel(SecurityContext.SecurityLevel.HIGH)
              .withAttributes(Map.of("role", "admin", "department", "security"))
              .build();

      assertEquals("test-user", context.getPrincipalId());
      assertEquals(SecurityContext.SecurityLevel.HIGH, context.getSecurityLevel());
      assertEquals(Optional.of("admin"), context.getAttribute("role"));
      assertEquals(Optional.of("security"), context.getAttribute("department"));
    }

    @Test
    @DisplayName("Should create default anonymous context")
    void shouldCreateDefaultAnonymousContext() {
      final SecurityContext context = SecurityContext.defaultContext();

      assertEquals("anonymous", context.getPrincipalId());
      assertEquals(SecurityContext.SecurityLevel.LOW, context.getSecurityLevel());
    }

    @Test
    @DisplayName("Should create system context for privileged operations")
    void shouldCreateSystemContextForPrivilegedOperations() {
      final SecurityContext context = SecurityContext.systemContext();

      assertEquals("system", context.getPrincipalId());
      assertEquals(SecurityContext.SecurityLevel.SYSTEM, context.getSecurityLevel());
    }
  }
}
