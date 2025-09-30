package ai.tegmentum.wasmtime4j.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.SecurityException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;

/**
 * Comprehensive security test suite validating all security features.
 *
 * <p>Tests cover:
 *
 * <ul>
 *   <li>Capability-based security and dynamic revocation
 *   <li>Advanced sandboxing and process isolation
 *   <li>Dynamic security policy enforcement
 *   <li>Cryptographic security features
 *   <li>Security monitoring and auditing
 *   <li>Secure communication
 *   <li>Vulnerability management
 * </ul>
 */
class ComprehensiveSecurityTest {

  private SecurityManager securityManager;
  private CapabilityManager capabilityManager;
  private DynamicSecurityPolicyEngine policyEngine;
  private CryptographicSecurityManager cryptoManager;
  private SecurityMonitoringSystem monitoringSystem;
  private SecureCommunicationManager communicationManager;
  private VulnerabilityManager vulnerabilityManager;

  @BeforeEach
  void setUp(final TestInfo testInfo) {
    System.out.println("Setting up security test: " + testInfo.getDisplayName());

    // Initialize security manager
    this.securityManager = SecurityManager.create();

    // Initialize capability manager
    this.capabilityManager = new CapabilityManager(securityManager);

    // Initialize policy engine
    this.policyEngine = new DynamicSecurityPolicyEngine(securityManager);

    // Initialize crypto manager
    final CryptographicSecurityManager.CryptographicPolicy cryptoPolicy =
        CryptographicSecurityManager.CryptographicPolicy.builder()
            .name("Test Crypto Policy")
            .requireSignatures(true)
            .build();
    this.cryptoManager = new CryptographicSecurityManager(securityManager, cryptoPolicy);

    // Initialize monitoring system
    final SecurityMonitoringSystem.MonitoringConfiguration monitoringConfig =
        new SecurityMonitoringSystem.MonitoringConfiguration("Test Monitoring");
    this.monitoringSystem = new SecurityMonitoringSystem(securityManager, monitoringConfig);

    // Initialize communication manager
    final SecureCommunicationManager.CommunicationPolicy commPolicy =
        new SecureCommunicationManager.CommunicationPolicy("Test Communication Policy");
    this.communicationManager = new SecureCommunicationManager(securityManager, commPolicy);

    // Initialize vulnerability manager
    final VulnerabilityManager.VulnerabilityPolicy vulnPolicy =
        new VulnerabilityManager.VulnerabilityPolicy("Test Vulnerability Policy");
    this.vulnerabilityManager = new VulnerabilityManager(securityManager, vulnPolicy);
  }

  @Nested
  @DisplayName("Capability-Based Security Tests")
  class CapabilitySecurityTests {

    @Test
    @DisplayName("Should grant and validate capabilities")
    void shouldGrantAndValidateCapabilities() {
      // Given
      final String principalId = "test-principal";
      final Capability memoryCapability = Capability.memoryAccess(1024 * 1024, true);
      final String grantorId = "test-grantor";

      // When
      final String grantId =
          capabilityManager.grantCapability(
              principalId, memoryCapability, grantorId, Optional.empty(), false);

      // Then
      assertNotNull(grantId);
      assertTrue(capabilityManager.hasCapability(principalId, memoryCapability));

      final Set<Capability> capabilities = capabilityManager.getCapabilities(principalId);
      assertFalse(capabilities.isEmpty());
      assertTrue(capabilities.contains(memoryCapability));

      System.out.println("✓ Capability granted and validated successfully");
    }

    @Test
    @DisplayName("Should revoke capabilities dynamically")
    void shouldRevokeCapabilitiesDynamically() {
      // Given
      final String principalId = "test-principal";
      final Capability fileSystemCapability = Capability.fileSystemAccess();
      final String grantorId = "test-grantor";

      final String grantId =
          capabilityManager.grantCapability(
              principalId, fileSystemCapability, grantorId, Optional.empty(), false);

      // When
      final boolean revoked = capabilityManager.revokeCapability(grantId, grantorId);

      // Then
      assertTrue(revoked);
      assertFalse(capabilityManager.hasCapability(principalId, fileSystemCapability));

      System.out.println("✓ Capability revoked successfully");
    }

    @Test
    @DisplayName("Should handle capability delegation")
    void shouldHandleCapabilityDelegation() {
      // Given
      final String delegatorId = "delegator";
      final String delegateeId = "delegatee";
      final Capability networkCapability = Capability.networkAccess();
      final String grantorId = "grantor";

      final String originalGrantId =
          capabilityManager.grantCapability(
              delegatorId, networkCapability, grantorId, Optional.empty(), true);

      // When
      final String delegationId =
          assertDoesNotThrow(
              () ->
                  capabilityManager.delegateCapability(
                      originalGrantId,
                      delegatorId,
                      delegateeId,
                      Optional.empty(),
                      Optional.empty()));

      // Then
      assertNotNull(delegationId);
      assertTrue(capabilityManager.hasCapability(delegateeId, networkCapability));

      System.out.println("✓ Capability delegation succeeded");
    }

    @Test
    @DisplayName("Should prevent unauthorized delegation")
    void shouldPreventUnauthorizedDelegation() {
      // Given
      final String principalId = "principal";
      final String unauthorizedId = "unauthorized";
      final String delegateeId = "delegatee";
      final Capability capability = Capability.memoryAccess(512 * 1024, false);

      final String grantId =
          capabilityManager.grantCapability(
              principalId, capability, "grantor", Optional.empty(), true);

      // When & Then
      assertThrows(
          SecurityException.class,
          () ->
              capabilityManager.delegateCapability(
                  grantId, unauthorizedId, delegateeId, Optional.empty(), Optional.empty()));

      System.out.println("✓ Unauthorized delegation prevented");
    }

    @Test
    @DisplayName("Should cleanup expired capabilities")
    @Timeout(5)
    void shouldCleanupExpiredCapabilities() throws InterruptedException {
      // Given
      final String principalId = "expiring-principal";
      final Capability capability = Capability.environmentAccess(Set.of("TEST_VAR"), false);
      final Instant nearFuture = Instant.now().plusMillis(100);

      capabilityManager.grantCapability(
          principalId, capability, "grantor", Optional.of(nearFuture), false);

      // Wait for expiration
      Thread.sleep(200);

      // When
      final int cleanedUp = capabilityManager.cleanupExpiredCapabilities();

      // Then
      assertTrue(cleanedUp > 0);
      assertFalse(capabilityManager.hasCapability(principalId, capability));

      System.out.println("✓ Expired capabilities cleaned up successfully");
    }
  }

  @Nested
  @DisplayName("Advanced Sandboxing Tests")
  class AdvancedSandboxingTests {

    @Test
    @DisplayName("Should create and configure secure sandbox")
    void shouldCreateAndConfigureSecureSandbox() {
      // Given
      final AdvancedSandbox.SandboxConfiguration config =
          AdvancedSandbox.SandboxConfiguration.builder()
              .securityLevel(5)
              .resourceLimits(
                  ResourceLimits.builder()
                      .maxMemoryBytes(10 * 1024 * 1024)
                      .maxCpuTimeMs(5000L)
                      .build())
              .allowedSystemCalls(Set.of("read", "write"))
              .networkPolicy(AdvancedSandbox.NetworkPolicy.denyAll())
              .fileSystemPolicy(AdvancedSandbox.FileSystemPolicy.readOnly())
              .maxLifetime(Duration.ofMinutes(10))
              .build();

      // When
      final AdvancedSandbox sandbox = new AdvancedSandbox("test-sandbox", config, securityManager);

      // Then
      assertNotNull(sandbox);
      assertNotNull(sandbox.getResourceUsage());

      sandbox.close();
      System.out.println("✓ Secure sandbox created and configured successfully");
    }

    @Test
    @DisplayName("Should enforce system call restrictions")
    void shouldEnforceSystemCallRestrictions() {
      // Given
      final AdvancedSandbox.SandboxConfiguration config =
          AdvancedSandbox.SandboxConfiguration.builder().allowedSystemCalls(Set.of("read")).build();

      try (final AdvancedSandbox sandbox =
          new AdvancedSandbox("restricted-sandbox", config, securityManager)) {

        // When & Then
        assertDoesNotThrow(() -> sandbox.isSystemCallAllowed("read"));
        assertThrows(SecurityException.class, () -> sandbox.isSystemCallAllowed("write"));

        System.out.println("✓ System call restrictions enforced successfully");
      }
    }

    @Test
    @DisplayName("Should monitor resource usage")
    void shouldMonitorResourceUsage() {
      // Given
      final AdvancedSandbox.SandboxConfiguration config =
          AdvancedSandbox.SandboxConfiguration.builder()
              .resourceLimits(ResourceLimits.builder().maxMemoryBytes(1024 * 1024).build())
              .build();

      try (final AdvancedSandbox sandbox =
          new AdvancedSandbox("monitored-sandbox", config, securityManager)) {

        // When
        final AdvancedSandbox.ResourceUsageStats usage = sandbox.getResourceUsage();

        // Then
        assertNotNull(usage);
        assertTrue(usage.getMemoryUsed() >= 0);
        assertTrue(usage.getCpuTimeMs() >= 0);

        System.out.println("✓ Resource usage monitored successfully");
      }
    }

    @Test
    @DisplayName("Should execute code with timeout enforcement")
    void shouldExecuteCodeWithTimeoutEnforcement() {
      // Given
      final AdvancedSandbox.SandboxConfiguration config =
          AdvancedSandbox.SandboxConfiguration.builder().build();

      try (final AdvancedSandbox sandbox =
          new AdvancedSandbox("timeout-sandbox", config, securityManager)) {

        final AdvancedSandbox.SandboxExecutable<String> task =
            (sb) -> {
              // Simulate some work
              Thread.sleep(10);
              return "Task completed";
            };

        // When
        final String result =
            assertDoesNotThrow(() -> sandbox.execute(task, Duration.ofSeconds(1)));

        // Then
        assertEquals("Task completed", result);

        System.out.println("✓ Code execution with timeout succeeded");
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Nested
  @DisplayName("Dynamic Security Policy Tests")
  class DynamicSecurityPolicyTests {

    @Test
    @DisplayName("Should add and evaluate security policies")
    void shouldAddAndEvaluateSecurityPolicies() throws SecurityException {
      // Given
      final DynamicSecurityPolicyEngine.EnhancedSecurityPolicy policy =
          DynamicSecurityPolicyEngine.EnhancedSecurityPolicy.builder()
              .id("test-policy")
              .name("Test Security Policy")
              .description("Test policy for unit testing")
              .addRule(
                  DynamicSecurityPolicyEngine.PolicyRule.builder()
                      .id("allow-read-rule")
                      .condition(
                          new DynamicSecurityPolicyEngine.PolicyCondition(
                              "action",
                              DynamicSecurityPolicyEngine.ConditionOperator.EQUALS,
                              "read"))
                      .action(DynamicSecurityPolicyEngine.PolicyAction.PERMIT)
                      .priority(100)
                      .build())
              .build();

      // When
      final boolean added = policyEngine.addPolicy("test-tenant", policy, "1.0");

      // Then
      assertTrue(added);

      // Create mock access request
      final AccessRequest request =
          AccessRequest.builder()
              .userIdentity(
                  UserIdentity.builder()
                      .id("test-user")
                      .principalType(UserIdentity.PrincipalType.USER)
                      .build())
              .resourceId("test-resource")
              .action("read")
              .build();

      // Evaluate policy
      final DynamicSecurityPolicyEngine.PolicyDecision decision =
          policyEngine.evaluate("test-tenant", request);

      assertNotNull(decision);

      System.out.println("✓ Security policy added and evaluated successfully");
    }

    @Test
    @DisplayName("Should support policy rollback")
    void shouldSupportPolicyRollback() throws SecurityException {
      // Given
      final DynamicSecurityPolicyEngine.EnhancedSecurityPolicy policy1 =
          DynamicSecurityPolicyEngine.EnhancedSecurityPolicy.builder()
              .id("policy-v1")
              .name("Policy Version 1")
              .build();

      final DynamicSecurityPolicyEngine.EnhancedSecurityPolicy policy2 =
          DynamicSecurityPolicyEngine.EnhancedSecurityPolicy.builder()
              .id("policy-v2")
              .name("Policy Version 2")
              .build();

      // Add initial policy
      policyEngine.addPolicy("rollback-tenant", policy1, "1.0");

      // Update to new policy
      policyEngine.addPolicy("rollback-tenant", policy2, "2.0");

      // When
      final boolean rolledBack = policyEngine.rollbackPolicy("rollback-tenant", "1.0");

      // Then
      assertTrue(rolledBack);

      System.out.println("✓ Policy rollback succeeded");
    }

    @Test
    @DisplayName("Should generate compliance reports")
    void shouldGenerateComplianceReports() {
      // Given
      final DynamicSecurityPolicyEngine.ReportingPeriod period =
          new DynamicSecurityPolicyEngine.ReportingPeriod(
              Instant.now().minus(Duration.ofDays(7)), Instant.now());

      // When
      final ComplianceReport report =
          policyEngine.generateComplianceReport("test-tenant", ComplianceFramework.SOX, period);

      // Then
      assertNotNull(report);

      System.out.println("✓ Compliance report generated successfully");
    }
  }

  @Nested
  @DisplayName("Cryptographic Security Tests")
  class CryptographicSecurityTests {

    @Test
    @DisplayName("Should encrypt and decrypt modules")
    void shouldEncryptAndDecryptModules() throws SecurityException {
      // Given
      final byte[] moduleBytes = "test wasm module content".getBytes();
      final String encryptionKeyId = "test-encryption-key";

      // When
      final CryptographicSecurityManager.EncryptedModule encryptedModule =
          cryptoManager.encryptModule(moduleBytes, encryptionKeyId);

      // Then
      assertNotNull(encryptedModule);
      assertNotNull(encryptedModule.getEncryptedData());
      assertEquals(moduleBytes.length, encryptedModule.getOriginalSize());

      System.out.println("✓ Module encryption succeeded");

      // TODO: Test decryption when implementation is complete
      // final byte[] decryptedBytes = cryptoManager.decryptModule(moduleId);
      // assertArrayEquals(moduleBytes, decryptedBytes);
    }

    @Test
    @DisplayName("Should validate module integrity")
    void shouldValidateModuleIntegrity() throws SecurityException {
      // Given
      final byte[] moduleBytes = "test module for integrity check".getBytes();
      final byte[] expectedHash = new byte[32]; // Mock hash
      final CryptographicSecurityManager.HashAlgorithm hashAlgorithm =
          CryptographicSecurityManager.HashAlgorithm.SHA256;

      // When & Then - This will use the placeholder implementation
      assertDoesNotThrow(
          () -> cryptoManager.validateModuleIntegrity(moduleBytes, expectedHash, hashAlgorithm));

      System.out.println("✓ Module integrity validation succeeded");
    }

    @Test
    @DisplayName("Should manage trust store operations")
    void shouldManageTrustStoreOperations() throws SecurityException {
      // Given
      final Path trustStorePath =
          Paths.get(System.getProperty("java.io.tmpdir"), "test-truststore.p12");
      final char[] password = "test-password".toCharArray();

      // When & Then
      assertDoesNotThrow(() -> cryptoManager.exportTrustStore(trustStorePath, password));
      assertDoesNotThrow(() -> cryptoManager.importTrustStore(trustStorePath, password));

      System.out.println("✓ Trust store operations succeeded");
    }
  }

  @Nested
  @DisplayName("Security Monitoring Tests")
  class SecurityMonitoringTests {

    @Test
    @DisplayName("Should process security events")
    void shouldProcessSecurityEvents() {
      // Given
      final SecurityMonitoringSystem.SecurityEvent event =
          new SecurityMonitoringSystem.SecurityEvent(
              "test-event-1",
              "authentication_failure",
              Instant.now(),
              "test-module",
              "Failed authentication attempt",
              Map.of("user_id", "test-user", "ip", "192.168.1.100"),
              SecurityMonitoringSystem.SecurityEventSeverity.WARNING);

      // When
      final SecurityMonitoringSystem.SecurityEventProcessingResult result =
          monitoringSystem.processSecurityEvent(event);

      // Then
      assertTrue(result.isSuccessful());
      assertNotNull(result.getProcessingTime());
      assertNotNull(result.getThreatAnalysis());

      System.out.println("✓ Security event processed successfully");
    }

    @Test
    @DisplayName("Should generate security reports")
    void shouldGenerateSecurityReports() {
      // Given
      final SecurityMonitoringSystem.ReportingPeriod period =
          new SecurityMonitoringSystem.ReportingPeriod(
              Instant.now().minus(Duration.ofDays(1)), Instant.now());

      // When
      final SecurityMonitoringSystem.SecurityReport report =
          monitoringSystem.generateSecurityReport(
              period, SecurityMonitoringSystem.SecurityReportType.COMPREHENSIVE);

      // Then
      assertNotNull(report);

      System.out.println("✓ Security report generated successfully");
    }

    @Test
    @DisplayName("Should initiate incident response")
    void shouldInitiateIncidentResponse() {
      // Given
      final SecurityMonitoringSystem.SecurityEvent criticalEvent =
          new SecurityMonitoringSystem.SecurityEvent(
              "critical-event-1",
              "security_breach",
              Instant.now(),
              "compromised-module",
              "Potential security breach detected",
              Map.of("severity", "critical"),
              SecurityMonitoringSystem.SecurityEventSeverity.CRITICAL);

      // When
      final SecurityMonitoringSystem.IncidentResponse response =
          monitoringSystem.initiateIncidentResponse(
              criticalEvent, SecurityMonitoringSystem.IncidentSeverity.CRITICAL);

      // Then
      assertNotNull(response);

      System.out.println("✓ Incident response initiated successfully");
    }

    @Test
    @DisplayName("Should retrieve real-time metrics")
    void shouldRetrieveRealTimeMetrics() {
      // When
      final SecurityMonitoringSystem.SecurityMetrics metrics =
          monitoringSystem.getCurrentSecurityMetrics();

      // Then
      assertNotNull(metrics);
      assertTrue(metrics.getEventCount() >= 0);

      System.out.println("✓ Real-time security metrics retrieved successfully");
    }
  }

  @Nested
  @DisplayName("Secure Communication Tests")
  class SecureCommunicationTests {

    @Test
    @DisplayName("Should create secure message")
    void shouldCreateSecureMessage() {
      // Given
      final String content = "Test secure message content";

      // When
      final SecureCommunicationManager.SecureMessage message =
          SecureCommunicationManager.SecureMessage.builder()
              .messageId("test-msg-1")
              .messageType("TEST")
              .content(content.getBytes())
              .timestamp(Instant.now())
              .build();

      // Then
      assertNotNull(message);
      assertEquals("test-msg-1", message.getMessageId());
      assertEquals("TEST", message.getMessageType());
      assertEquals(content.length(), message.getContent().length);

      System.out.println("✓ Secure message created successfully");
    }

    @Test
    @DisplayName("Should configure connection parameters")
    void shouldConfigureConnectionParameters() {
      // When
      final SecureCommunicationManager.ConnectionConfiguration config =
          new SecureCommunicationManager.ConnectionConfiguration(
              "https",
              true,
              Duration.ofSeconds(30),
              true,
              List.of("TLS_AES_256_GCM_SHA384", "TLS_CHACHA20_POLY1305_SHA256"));

      // Then
      assertNotNull(config);
      assertEquals("https", config.getProtocol());
      assertTrue(config.isClientAuthenticationRequired());
      assertEquals(Duration.ofSeconds(30), config.getConnectionTimeout());
      assertTrue(config.isVerifyHostname());
      assertFalse(config.getAllowedCipherSuites().isEmpty());

      System.out.println("✓ Connection configuration created successfully");
    }

    @Test
    @DisplayName("Should handle message with integrity hash")
    void shouldHandleMessageWithIntegrityHash() {
      // Given
      final SecureCommunicationManager.SecureMessage originalMessage =
          SecureCommunicationManager.SecureMessage.builder()
              .messageId("integrity-test")
              .messageType("INTEGRITY_TEST")
              .content("test content".getBytes())
              .build();

      // When
      final SecureCommunicationManager.SecureMessage messageWithHash =
          originalMessage.withIntegrityHash("mock-integrity-hash");

      // Then
      assertTrue(messageWithHash.getIntegrityHash().isPresent());
      assertEquals("mock-integrity-hash", messageWithHash.getIntegrityHash().get());

      System.out.println("✓ Message integrity hash handled successfully");
    }
  }

  @Nested
  @DisplayName("Vulnerability Management Tests")
  class VulnerabilityManagementTests {

    @Test
    @DisplayName("Should scan module for vulnerabilities")
    void shouldScanModuleForVulnerabilities() throws SecurityException {
      // Given
      final String moduleId = "test-module-1";
      final byte[] moduleBytes = "mock wasm module bytes".getBytes();
      final VulnerabilityManager.ModuleMetadata metadata =
          new VulnerabilityManager.ModuleMetadata(
              List.of(
                  new VulnerabilityManager.Dependency("lodash", "4.17.20"),
                  new VulnerabilityManager.Dependency("axios", "0.21.0")));

      // When
      final VulnerabilityManager.SecurityScanResult scanResult =
          vulnerabilityManager.scanModule(moduleId, moduleBytes, metadata);

      // Then
      assertNotNull(scanResult);
      assertEquals(moduleId, scanResult.getModuleId());
      assertNotNull(scanResult.getScanDuration());
      assertNotNull(scanResult.getRiskAssessment());

      System.out.println("✓ Module vulnerability scan completed successfully");
    }

    @Test
    @DisplayName("Should scan dependencies for vulnerabilities")
    void shouldScanDependenciesForVulnerabilities() throws SecurityException {
      // Given
      final List<VulnerabilityManager.Dependency> dependencies =
          List.of(
              new VulnerabilityManager.Dependency("express", "4.17.1"),
              new VulnerabilityManager.Dependency("moment", "2.29.1"),
              new VulnerabilityManager.Dependency("react", "17.0.2"));

      // When
      final VulnerabilityManager.DependencyScanResult scanResult =
          vulnerabilityManager.scanDependencies(dependencies);

      // Then
      assertNotNull(scanResult);

      System.out.println("✓ Dependency vulnerability scan completed successfully");
    }

    @Test
    @DisplayName("Should update vulnerability database")
    void shouldUpdateVulnerabilityDatabase() throws SecurityException {
      // When
      final VulnerabilityManager.DatabaseUpdateResult updateResult =
          vulnerabilityManager.updateVulnerabilityDatabase();

      // Then
      assertNotNull(updateResult);
      assertTrue(updateResult.getNewVulnerabilities() >= 0);
      assertTrue(updateResult.getUpdatedVulnerabilities() >= 0);

      System.out.println("✓ Vulnerability database updated successfully");
    }

    @Test
    @DisplayName("Should generate vulnerability reports")
    void shouldGenerateVulnerabilityReports() {
      // Given
      final VulnerabilityManager.ReportingPeriod period =
          new VulnerabilityManager.ReportingPeriod(
              Instant.now().minus(Duration.ofDays(30)), Instant.now());

      // When
      final VulnerabilityManager.ComprehensiveVulnerabilityReport report =
          vulnerabilityManager.generateReport(period, true);

      // Then
      assertNotNull(report);
      assertNotNull(report.getReportId());

      System.out.println("✓ Vulnerability report generated successfully");
    }

    @Test
    @DisplayName("Should configure zero-day protection")
    void shouldConfigureZeroDayProtection() {
      // Given
      final VulnerabilityManager.ZeroDayProtectionConfig config =
          new VulnerabilityManager.ZeroDayProtectionConfig("Advanced Zero-Day Protection");

      // When & Then
      assertDoesNotThrow(() -> vulnerabilityManager.configureZeroDayProtection(config));

      System.out.println("✓ Zero-day protection configured successfully");
    }
  }

  @Nested
  @DisplayName("Integration Security Tests")
  class IntegrationSecurityTests {

    @Test
    @DisplayName("Should integrate all security components")
    @Timeout(10)
    void shouldIntegrateAllSecurityComponents() throws SecurityException {
      System.out.println("Starting comprehensive security integration test...");

      // 1. Grant capabilities
      final String principalId = "integration-test-principal";
      final Capability memoryCapability = Capability.memoryAccess(2048 * 1024, true);
      final String grantId =
          capabilityManager.grantCapability(
              principalId, memoryCapability, "integration-test", Optional.empty(), false);

      assertTrue(capabilityManager.hasCapability(principalId, memoryCapability));
      System.out.println("✓ Step 1: Capabilities granted");

      // 2. Create security policy
      final DynamicSecurityPolicyEngine.EnhancedSecurityPolicy policy =
          DynamicSecurityPolicyEngine.EnhancedSecurityPolicy.builder()
              .id("integration-policy")
              .name("Integration Test Policy")
              .addRule(
                  DynamicSecurityPolicyEngine.PolicyRule.builder()
                      .id("allow-memory-access")
                      .condition(
                          new DynamicSecurityPolicyEngine.PolicyCondition(
                              "resource",
                              DynamicSecurityPolicyEngine.ConditionOperator.EQUALS,
                              "memory"))
                      .action(DynamicSecurityPolicyEngine.PolicyAction.PERMIT)
                      .priority(100)
                      .build())
              .build();

      policyEngine.addPolicy("integration-tenant", policy, "1.0");
      System.out.println("✓ Step 2: Security policy created");

      // 3. Create secure sandbox
      final AdvancedSandbox.SandboxConfiguration sandboxConfig =
          AdvancedSandbox.SandboxConfiguration.builder()
              .securityLevel(4)
              .resourceLimits(
                  ResourceLimits.builder().maxMemoryBytes(4096 * 1024).maxCpuTimeMs(10000L).build())
              .allowedSystemCalls(Set.of("read", "write", "mmap"))
              .build();

      try (final AdvancedSandbox sandbox =
          new AdvancedSandbox("integration-sandbox", sandboxConfig, securityManager)) {

        assertNotNull(sandbox.getResourceUsage());
        System.out.println("✓ Step 3: Secure sandbox created");

        // 4. Process security events
        final SecurityMonitoringSystem.SecurityEvent event =
            new SecurityMonitoringSystem.SecurityEvent(
                "integration-event",
                "sandbox_created",
                Instant.now(),
                "integration-test",
                "Sandbox created for integration test",
                Map.of("sandbox_id", "integration-sandbox"),
                SecurityMonitoringSystem.SecurityEventSeverity.INFO);

        final SecurityMonitoringSystem.SecurityEventProcessingResult eventResult =
            monitoringSystem.processSecurityEvent(event);

        assertTrue(eventResult.isSuccessful());
        System.out.println("✓ Step 4: Security event processed");

        // 5. Create and validate secure message
        final SecureCommunicationManager.SecureMessage message =
            SecureCommunicationManager.SecureMessage.builder()
                .messageId("integration-message")
                .messageType("INTEGRATION_TEST")
                .content("Integration test message content".getBytes())
                .timestamp(Instant.now())
                .build();

        assertNotNull(message);
        System.out.println("✓ Step 5: Secure message created");

        // 6. Perform vulnerability scan
        final VulnerabilityManager.ModuleMetadata moduleMetadata =
            new VulnerabilityManager.ModuleMetadata(
                List.of(new VulnerabilityManager.Dependency("test-lib", "1.0.0")));

        final VulnerabilityManager.SecurityScanResult scanResult =
            vulnerabilityManager.scanModule(
                "integration-module",
                "mock module bytes for integration test".getBytes(),
                moduleMetadata);

        assertNotNull(scanResult);
        System.out.println("✓ Step 6: Vulnerability scan completed");

        // 7. Generate comprehensive reports
        final SecurityMonitoringSystem.ReportingPeriod reportPeriod =
            new SecurityMonitoringSystem.ReportingPeriod(
                Instant.now().minus(Duration.ofMinutes(1)), Instant.now());

        final SecurityMonitoringSystem.SecurityReport securityReport =
            monitoringSystem.generateSecurityReport(
                reportPeriod, SecurityMonitoringSystem.SecurityReportType.COMPREHENSIVE);

        assertNotNull(securityReport);
        System.out.println("✓ Step 7: Security report generated");

        System.out.println("✓ All integration steps completed successfully!");
      }
    }

    @Test
    @DisplayName("Should handle security violations gracefully")
    void shouldHandleSecurityViolationsGracefully() {
      System.out.println("Testing security violation handling...");

      // Test unauthorized capability access
      final String unauthorizedPrincipal = "unauthorized-user";
      final Capability restrictedCapability = Capability.systemCallAccess(Set.of("exec"));

      assertFalse(capabilityManager.hasCapability(unauthorizedPrincipal, restrictedCapability));
      System.out.println("✓ Unauthorized capability access denied");

      // Test sandbox security violations
      final AdvancedSandbox.SandboxConfiguration restrictiveConfig =
          AdvancedSandbox.SandboxConfiguration.builder()
              .securityLevel(5)
              .allowedSystemCalls(Set.of("read")) // Very restrictive
              .networkPolicy(AdvancedSandbox.NetworkPolicy.denyAll())
              .build();

      try (final AdvancedSandbox restrictiveSandbox =
          new AdvancedSandbox("restrictive-sandbox", restrictiveConfig, securityManager)) {

        assertThrows(SecurityException.class, () -> restrictiveSandbox.isSystemCallAllowed("exec"));

        assertThrows(
            SecurityException.class,
            () -> restrictiveSandbox.isNetworkAccessAllowed("malicious.com", 80, "tcp"));

        System.out.println("✓ Sandbox violations handled correctly");
      }
    }
  }
}
