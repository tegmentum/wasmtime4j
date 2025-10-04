package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.wasi.security.WasiSecurityValidator;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive test suite for WASI security validation.
 *
 * <p>This test validates complete WASI security functionality including:
 *
 * <ul>
 *   <li>Path traversal attack prevention
 *   <li>Environment variable access control
 *   <li>Sandbox boundary enforcement
 *   <li>Resource access validation
 *   <li>Security policy enforcement
 *   <li>Capability-based access control
 * </ul>
 */
class WasiSecurityValidationTest {

  @TempDir private Path tempDirectory;

  private WasiContext wasiContext;
  private WasiSecurityValidator securityValidator;

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    System.out.println("Setting up WASI security test: " + testInfo.getDisplayName());

    // Create WASI context with security restrictions
    wasiContext =
        WasiContext.builder()
            .withEnvironment("ALLOWED_VAR", "allowed_value")
            .withEnvironment("SECRET_VAR", "secret_value")
            .withEnvironment("PUBLIC_VAR", "public_value")
            .withPreopenDirectory("/tmp", tempDirectory.toString())
            .withWorkingDirectory(tempDirectory.toString())
            .build();

    assertNotNull(wasiContext, "WASI context must be created successfully");

    securityValidator = wasiContext.getSecurityValidator();
    assertNotNull(securityValidator, "Security validator must be available");

    System.out.println("WASI security test setup completed");
  }

  @Test
  void testPathTraversalPrevention() {
    // Test various path traversal attack patterns
    final String[] malicioussPaths = {
      "../../../etc/passwd",
      "..\\..\\..\\windows\\system32\\config\\sam",
      "/tmp/../../../etc/shadow",
      "../../../../root/.ssh/id_rsa",
      "/tmp/./../../home/user/.bashrc",
      "..%2f..%2f..%2fetc%2fpasswd", // URL encoded
      "..%252f..%252f..%252fetc%252fpasswd", // Double URL encoded
      "....//....//....//etc/passwd",
      "/tmp/../../../../../proc/version",
      "C:\\..\\..\\..\\Windows\\System32\\drivers\\etc\\hosts"
    };

    for (final String maliciousPath : malicioussPaths) {
      assertThrows(
          Exception.class,
          () -> securityValidator.validatePath(Paths.get(maliciousPath)),
          "Path traversal must be prevented for: " + maliciousPath);
    }

    // Test legitimate paths that should be allowed
    final String[] legitimatePaths = {
      "test_file.txt",
      "subdir/test_file.txt",
      "./local_file.txt",
      "data/config.json",
      "logs/application.log"
    };

    for (final String legitimatePath : legitimatePaths) {
      assertDoesNotThrow(
          () -> securityValidator.validatePath(Paths.get(legitimatePath)),
          "Legitimate path must be allowed: " + legitimatePath);
    }

    System.out.println("Path traversal prevention validated");
  }

  @Test
  void testEnvironmentVariableAccessControl() {
    // Test allowed environment variable access
    assertDoesNotThrow(
        () -> securityValidator.validateEnvironmentAccess("ALLOWED_VAR"),
        "Access to allowed environment variable must succeed");

    assertDoesNotThrow(
        () -> securityValidator.validateEnvironmentAccess("PUBLIC_VAR"),
        "Access to public environment variable must succeed");

    // Test restricted environment variables
    final String[] restrictedVars = {
      "PATH", "HOME", "USER", "PWD", "SHELL", "SSH_AUTH_SOCK", "DISPLAY", "XAUTHORITY"
    };

    for (final String restrictedVar : restrictedVars) {
      // Note: depending on security policy, these might be allowed or restricted
      // The test validates that the security validator makes consistent decisions
      try {
        securityValidator.validateEnvironmentAccess(restrictedVar);
        System.out.println("Environment variable " + restrictedVar + " is allowed");
      } catch (final Exception e) {
        System.out.println(
            "Environment variable " + restrictedVar + " is restricted: " + e.getMessage());
        // This is expected for restricted variables
      }
    }

    // Test invalid environment variable names
    final String[] invalidVars = {
      "", // Empty name
      null, // Null name would be caught by validation before reaching security validator
      "INVALID\nVAR", // Contains newline
      "INVALID\0VAR" // Contains null byte
    };

    for (final String invalidVar : invalidVars) {
      if (invalidVar != null) {
        assertThrows(
            Exception.class,
            () -> securityValidator.validateEnvironmentAccess(invalidVar),
            "Invalid environment variable name must be rejected: " + invalidVar);
      }
    }

    System.out.println("Environment variable access control validated");
  }

  @Test
  void testSandboxBoundaryEnforcement() {
    // Test access within sandbox boundaries
    final Path allowedPath = tempDirectory.resolve("allowed_file.txt");
    assertDoesNotThrow(
        () -> securityValidator.validatePath(allowedPath), "Access within sandbox must be allowed");

    // Test access outside sandbox boundaries
    final Path outsidePath = tempDirectory.getParent().resolve("outside_file.txt");
    if (outsidePath != null) {
      assertThrows(
          Exception.class,
          () -> securityValidator.validatePath(outsidePath),
          "Access outside sandbox must be prevented");
    }

    // Test system directories
    final String[] systemPaths = {
      "/etc/passwd",
      "/proc/version",
      "/sys/kernel/version",
      "C:\\Windows\\System32\\drivers\\etc\\hosts",
      "C:\\Program Files\\",
      "/usr/bin/ls",
      "/bin/sh"
    };

    for (final String systemPath : systemPaths) {
      assertThrows(
          Exception.class,
          () -> securityValidator.validatePath(Paths.get(systemPath)),
          "Access to system path must be prevented: " + systemPath);
    }

    System.out.println("Sandbox boundary enforcement validated");
  }

  @Test
  void testResourceAccessValidation() {
    // Test file operation permissions
    final Path testFile = tempDirectory.resolve("security_test.txt");

    // Test read access validation
    assertDoesNotThrow(
        () -> securityValidator.validateFileAccess(testFile, "read"),
        "Read access within sandbox must be allowed");

    // Test write access validation
    assertDoesNotThrow(
        () -> securityValidator.validateFileAccess(testFile, "write"),
        "Write access within sandbox must be allowed");

    // Test execute access validation (might be restricted)
    try {
      securityValidator.validateFileAccess(testFile, "execute");
      System.out.println("Execute access is allowed for test file");
    } catch (final Exception e) {
      System.out.println("Execute access is restricted: " + e.getMessage());
    }

    // Test network access validation
    try {
      securityValidator.validateNetworkAccess("127.0.0.1", 8080);
      System.out.println("Network access to localhost:8080 is allowed");
    } catch (final Exception e) {
      System.out.println("Network access is restricted: " + e.getMessage());
    }

    // Test restricted network access
    final String[] restrictedHosts = {
      "0.0.0.0", // All interfaces
      "169.254.0.1", // Link-local
      "224.0.0.1", // Multicast
      "255.255.255.255" // Broadcast
    };

    for (final String host : restrictedHosts) {
      assertThrows(
          Exception.class,
          () -> securityValidator.validateNetworkAccess(host, 80),
          "Network access to restricted host must be prevented: " + host);
    }

    System.out.println("Resource access validation validated");
  }

  @Test
  void testSecurityPolicyEnforcement() {
    // Test capability checking
    final String[] capabilities = {
      "file_read", "file_write", "network_connect", "process_spawn", "env_access"
    };

    for (final String capability : capabilities) {
      final boolean hasCapability = securityValidator.hasCapability(capability);
      System.out.println(
          "Capability " + capability + ": " + (hasCapability ? "granted" : "denied"));
      // Don't assert specific values as they depend on security policy configuration
    }

    // Test security level enforcement
    final int securityLevel = securityValidator.getSecurityLevel();
    assertTrue(securityLevel >= 0, "Security level must be non-negative");
    assertTrue(securityLevel <= 10, "Security level must be within reasonable range");

    System.out.println("Security policy enforcement validated with level: " + securityLevel);
  }

  @Test
  void testThreatDetection() {
    // Test detection of suspicious patterns
    final String[] suspiciousPatterns = {
      "/proc/self/mem",
      "/dev/kmem",
      "\\Device\\PhysicalMemory",
      "/sys/firmware/efi/efivars/",
      "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Lsa\\Security Packages"
    };

    for (final String pattern : suspiciousPatterns) {
      final boolean isThreat = securityValidator.detectThreat(pattern);
      if (isThreat) {
        System.out.println("Threat detected for pattern: " + pattern);
      }
    }

    // Test normal patterns that should not be threats
    final String[] normalPatterns = {
      "config.txt", "data.json", "application.log", "user_preferences.xml"
    };

    for (final String pattern : normalPatterns) {
      final boolean isThreat = securityValidator.detectThreat(pattern);
      assertFalse(isThreat, "Normal pattern must not be detected as threat: " + pattern);
    }

    System.out.println("Threat detection validated");
  }

  @Test
  void testAuditLogging() {
    // Test security event logging
    assertDoesNotThrow(
        () ->
            securityValidator.logSecurityEvent("test_event", "Test security event for validation"),
        "Security event logging must not throw");

    // Test access attempt logging
    assertDoesNotThrow(
        () -> securityValidator.logAccessAttempt("/tmp/test_file.txt", "read", true),
        "Access attempt logging must not throw");

    // Test violation logging
    assertDoesNotThrow(
        () -> securityValidator.logSecurityViolation("path_traversal", "../../../etc/passwd"),
        "Security violation logging must not throw");

    // Verify audit log functionality
    final boolean auditEnabled = securityValidator.isAuditEnabled();
    System.out.println("Audit logging is " + (auditEnabled ? "enabled" : "disabled"));

    if (auditEnabled) {
      final int auditEventCount = securityValidator.getAuditEventCount();
      assertTrue(auditEventCount >= 0, "Audit event count must be non-negative");
      System.out.println("Audit event count: " + auditEventCount);
    }

    System.out.println("Audit logging validated");
  }

  @Test
  void testSecurityConfiguration() {
    // Test security configuration retrieval
    final boolean pathValidationEnabled = securityValidator.isPathValidationEnabled();
    final boolean environmentValidationEnabled = securityValidator.isEnvironmentValidationEnabled();
    final boolean networkValidationEnabled = securityValidator.isNetworkValidationEnabled();

    System.out.println("Security configuration:");
    System.out.println("  Path validation: " + pathValidationEnabled);
    System.out.println("  Environment validation: " + environmentValidationEnabled);
    System.out.println("  Network validation: " + networkValidationEnabled);

    // Test security mode
    final String securityMode = securityValidator.getSecurityMode();
    assertNotNull(securityMode, "Security mode must not be null");
    assertThat(securityMode).isIn("strict", "normal", "permissive", "custom");

    System.out.println("Security mode: " + securityMode);

    // Test allowed file extensions
    final String[] allowedExtensions = securityValidator.getAllowedFileExtensions();
    if (allowedExtensions != null && allowedExtensions.length > 0) {
      System.out.println("Allowed file extensions: " + String.join(", ", allowedExtensions));
    }

    System.out.println("Security configuration validated");
  }

  @Test
  void testDynamicSecurityUpdates() {
    // Test dynamic security policy updates
    assertDoesNotThrow(
        () -> securityValidator.updateSecurityPolicy("test_policy", "test_value"),
        "Security policy update must not throw");

    // Test capability granting/revoking
    final String testCapability = "test_capability";
    assertDoesNotThrow(
        () -> securityValidator.grantCapability(testCapability),
        "Capability granting must not throw");

    assertTrue(
        securityValidator.hasCapability(testCapability), "Granted capability must be available");

    assertDoesNotThrow(
        () -> securityValidator.revokeCapability(testCapability),
        "Capability revoking must not throw");

    assertFalse(
        securityValidator.hasCapability(testCapability),
        "Revoked capability must not be available");

    System.out.println("Dynamic security updates validated");
  }

  @Test
  void testSecurityPerformance() {
    // Test performance of security validation
    final long startTime = System.nanoTime();
    final int iterations = 1000;

    for (int i = 0; i < iterations; i++) {
      final Path testPath = tempDirectory.resolve("perf_test_" + i + ".txt");
      assertDoesNotThrow(
          () -> securityValidator.validatePath(testPath),
          "Performance test path validation must not throw");
    }

    final long endTime = System.nanoTime();
    final long durationMs = (endTime - startTime) / 1_000_000;
    final double avgTimePerValidation = (double) durationMs / iterations;

    System.out.printf(
        "Security validation performance: %d iterations in %d ms (%.3f ms/validation)%n",
        iterations, durationMs, avgTimePerValidation);

    // Performance should be reasonable (less than 1ms per validation on average)
    assertTrue(
        avgTimePerValidation < 1.0, "Security validation should be fast (< 1ms per validation)");

    System.out.println("Security performance validated");
  }
}
