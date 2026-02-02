/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.SecurityException.SecurityContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link SecurityException} class.
 *
 * <p>This test class verifies the construction and behavior of security exceptions, including
 * security contexts and violation checks.
 */
@DisplayName("SecurityException Tests")
class SecurityExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("SecurityException should extend WasmException")
    void shouldExtendWasmException() {
      assertTrue(
          WasmException.class.isAssignableFrom(SecurityException.class),
          "SecurityException should extend WasmException");
    }

    @Test
    @DisplayName("SecurityException should be serializable")
    void shouldBeSerializable() {
      assertTrue(
          java.io.Serializable.class.isAssignableFrom(SecurityException.class),
          "SecurityException should be serializable");
    }

    @Test
    @DisplayName("SecurityException should NOT extend java.lang.SecurityException")
    void shouldNotExtendJavaSecurityException() {
      assertFalse(
          java.lang.SecurityException.class.isAssignableFrom(SecurityException.class),
          "SecurityException should NOT extend java.lang.SecurityException");
    }
  }

  @Nested
  @DisplayName("SecurityContext Enum Tests")
  class SecurityContextEnumTests {

    @Test
    @DisplayName("Should have HOST_FUNCTION value")
    void shouldHaveHostFunctionValue() {
      assertNotNull(SecurityContext.valueOf("HOST_FUNCTION"), "Should have HOST_FUNCTION value");
    }

    @Test
    @DisplayName("Should have WASI_CAPABILITY value")
    void shouldHaveWasiCapabilityValue() {
      assertNotNull(
          SecurityContext.valueOf("WASI_CAPABILITY"), "Should have WASI_CAPABILITY value");
    }

    @Test
    @DisplayName("Should have MODULE_IMPORT_EXPORT value")
    void shouldHaveModuleImportExportValue() {
      assertNotNull(
          SecurityContext.valueOf("MODULE_IMPORT_EXPORT"),
          "Should have MODULE_IMPORT_EXPORT value");
    }

    @Test
    @DisplayName("Should have MEMORY_ACCESS value")
    void shouldHaveMemoryAccessValue() {
      assertNotNull(SecurityContext.valueOf("MEMORY_ACCESS"), "Should have MEMORY_ACCESS value");
    }

    @Test
    @DisplayName("Should have FILE_SYSTEM_ACCESS value")
    void shouldHaveFileSystemAccessValue() {
      assertNotNull(
          SecurityContext.valueOf("FILE_SYSTEM_ACCESS"), "Should have FILE_SYSTEM_ACCESS value");
    }

    @Test
    @DisplayName("Should have NETWORK_ACCESS value")
    void shouldHaveNetworkAccessValue() {
      assertNotNull(SecurityContext.valueOf("NETWORK_ACCESS"), "Should have NETWORK_ACCESS value");
    }

    @Test
    @DisplayName("Should have SYSTEM_RESOURCE value")
    void shouldHaveSystemResourceValue() {
      assertNotNull(
          SecurityContext.valueOf("SYSTEM_RESOURCE"), "Should have SYSTEM_RESOURCE value");
    }

    @Test
    @DisplayName("Should have SANDBOX_POLICY value")
    void shouldHaveSandboxPolicyValue() {
      assertNotNull(SecurityContext.valueOf("SANDBOX_POLICY"), "Should have SANDBOX_POLICY value");
    }

    @Test
    @DisplayName("Should have 8 security contexts")
    void shouldHave8SecurityContexts() {
      assertEquals(8, SecurityContext.values().length, "Should have 8 security contexts");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with message should set defaults")
    void constructorWithMessageShouldSetDefaults() {
      final SecurityException exception = new SecurityException("Security violation");

      assertEquals(
          "Security violation", exception.getMessage(), "Message should be 'Security violation'");
      assertNull(exception.getViolatedPolicy(), "Violated policy should be null");
      assertNull(exception.getAttemptedAction(), "Attempted action should be null");
      assertNull(exception.getDeniedResource(), "Denied resource should be null");
      assertEquals(
          SecurityContext.SANDBOX_POLICY,
          exception.getSecurityContext(),
          "Security context should default to SANDBOX_POLICY");
    }

    @Test
    @DisplayName("Constructor with message and cause should set both")
    void constructorWithMessageAndCauseShouldSetBoth() {
      final Throwable cause = new Exception("Root cause");
      final SecurityException exception = new SecurityException("Security error", cause);

      assertEquals("Security error", exception.getMessage(), "Message should be 'Security error'");
      assertSame(cause, exception.getCause(), "Cause should be set");
      assertEquals(
          SecurityContext.SANDBOX_POLICY,
          exception.getSecurityContext(),
          "Security context should default to SANDBOX_POLICY");
    }

    @Test
    @DisplayName("Constructor with security details should set fields")
    void constructorWithSecurityDetailsShouldSetFields() {
      final SecurityException exception =
          new SecurityException(
              "Permission denied",
              "no-file-read",
              "read /etc/passwd",
              SecurityContext.FILE_SYSTEM_ACCESS);

      assertEquals("Permission denied", exception.getMessage(), "Message should match");
      assertEquals("no-file-read", exception.getViolatedPolicy(), "Violated policy should match");
      assertEquals(
          "read /etc/passwd", exception.getAttemptedAction(), "Attempted action should match");
      assertNull(exception.getDeniedResource(), "Denied resource should be null");
      assertEquals(
          SecurityContext.FILE_SYSTEM_ACCESS,
          exception.getSecurityContext(),
          "Security context should be FILE_SYSTEM_ACCESS");
    }

    @Test
    @DisplayName("Full constructor without cause should set all fields")
    void fullConstructorWithoutCauseShouldSetAllFields() {
      final SecurityException exception =
          new SecurityException(
              "Network blocked",
              "no-network",
              "connect",
              "example.com:443",
              SecurityContext.NETWORK_ACCESS);

      assertEquals("no-network", exception.getViolatedPolicy(), "Violated policy should match");
      assertEquals("connect", exception.getAttemptedAction(), "Attempted action should match");
      assertEquals(
          "example.com:443", exception.getDeniedResource(), "Denied resource should match");
      assertEquals(
          SecurityContext.NETWORK_ACCESS,
          exception.getSecurityContext(),
          "Security context should be NETWORK_ACCESS");
    }

    @Test
    @DisplayName("Full constructor with cause should set all fields")
    void fullConstructorWithCauseShouldSetAllFields() {
      final Throwable cause = new Exception("Underlying error");
      final SecurityException exception =
          new SecurityException(
              "Host function blocked",
              "whitelist-policy",
              "call_dangerous_func",
              "native_function",
              SecurityContext.HOST_FUNCTION,
              cause);

      assertEquals(
          SecurityContext.HOST_FUNCTION,
          exception.getSecurityContext(),
          "Security context should be HOST_FUNCTION");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }
  }

  @Nested
  @DisplayName("Violation Check Tests")
  class ViolationCheckTests {

    @Test
    @DisplayName("isHostFunctionViolation should return true for HOST_FUNCTION")
    void isHostFunctionViolationShouldReturnTrueForHostFunction() {
      final SecurityException exception =
          new SecurityException("Error", "policy", "action", SecurityContext.HOST_FUNCTION);

      assertTrue(
          exception.isHostFunctionViolation(),
          "isHostFunctionViolation should return true for HOST_FUNCTION");
      assertFalse(
          exception.isWasiCapabilityViolation(), "isWasiCapabilityViolation should return false");
    }

    @Test
    @DisplayName("isWasiCapabilityViolation should return true for WASI_CAPABILITY")
    void isWasiCapabilityViolationShouldReturnTrueForWasiCapability() {
      final SecurityException exception =
          new SecurityException("Error", "policy", "action", SecurityContext.WASI_CAPABILITY);

      assertTrue(
          exception.isWasiCapabilityViolation(),
          "isWasiCapabilityViolation should return true for WASI_CAPABILITY");
    }

    @Test
    @DisplayName("isFileSystemViolation should return true for FILE_SYSTEM_ACCESS")
    void isFileSystemViolationShouldReturnTrueForFileSystemAccess() {
      final SecurityException exception =
          new SecurityException("Error", "policy", "action", SecurityContext.FILE_SYSTEM_ACCESS);

      assertTrue(
          exception.isFileSystemViolation(),
          "isFileSystemViolation should return true for FILE_SYSTEM_ACCESS");
    }

    @Test
    @DisplayName("isNetworkViolation should return true for NETWORK_ACCESS")
    void isNetworkViolationShouldReturnTrueForNetworkAccess() {
      final SecurityException exception =
          new SecurityException("Error", "policy", "action", SecurityContext.NETWORK_ACCESS);

      assertTrue(
          exception.isNetworkViolation(),
          "isNetworkViolation should return true for NETWORK_ACCESS");
    }

    @Test
    @DisplayName("isMemoryViolation should return true for MEMORY_ACCESS")
    void isMemoryViolationShouldReturnTrueForMemoryAccess() {
      final SecurityException exception =
          new SecurityException("Error", "policy", "action", SecurityContext.MEMORY_ACCESS);

      assertTrue(
          exception.isMemoryViolation(), "isMemoryViolation should return true for MEMORY_ACCESS");
    }

    @Test
    @DisplayName("isSandboxViolation should return true for SANDBOX_POLICY")
    void isSandboxViolationShouldReturnTrueForSandboxPolicy() {
      final SecurityException exception = new SecurityException("Error");

      assertTrue(
          exception.isSandboxViolation(),
          "isSandboxViolation should return true for SANDBOX_POLICY (default)");
    }
  }

  @Nested
  @DisplayName("getSecurityViolationDescription Tests")
  class GetSecurityViolationDescriptionTests {

    @Test
    @DisplayName("Description should include context")
    void descriptionShouldIncludeContext() {
      final SecurityException exception =
          new SecurityException("Error", "policy", "action", SecurityContext.FILE_SYSTEM_ACCESS);

      final String description = exception.getSecurityViolationDescription();

      assertTrue(description.contains("file system access"), "Description should include context");
    }

    @Test
    @DisplayName("Description should include policy when set")
    void descriptionShouldIncludePolicyWhenSet() {
      final SecurityException exception =
          new SecurityException(
              "Error", "no-read-policy", "action", SecurityContext.SANDBOX_POLICY);

      final String description = exception.getSecurityViolationDescription();

      assertTrue(description.contains("no-read-policy"), "Description should include policy");
    }

    @Test
    @DisplayName("Description should include action when set")
    void descriptionShouldIncludeActionWhenSet() {
      final SecurityException exception =
          new SecurityException(
              "Error", "policy", "read-secret-file", SecurityContext.SANDBOX_POLICY);

      final String description = exception.getSecurityViolationDescription();

      assertTrue(description.contains("read-secret-file"), "Description should include action");
    }

    @Test
    @DisplayName("Description should include resource when set")
    void descriptionShouldIncludeResourceWhenSet() {
      final SecurityException exception =
          new SecurityException(
              "Error", "policy", "action", "/etc/passwd", SecurityContext.FILE_SYSTEM_ACCESS);

      final String description = exception.getSecurityViolationDescription();

      assertTrue(description.contains("/etc/passwd"), "Description should include resource");
    }
  }

  @Nested
  @DisplayName("Usage Tests")
  class UsageTests {

    @Test
    @DisplayName("Should be throwable")
    void shouldBeThrowable() {
      final SecurityException exception = new SecurityException("Test");

      assertTrue(exception instanceof Throwable, "SecurityException should be throwable");
    }

    @Test
    @DisplayName("Should be catchable as WasmException")
    void shouldBeCatchableAsWasmException() {
      try {
        throw new SecurityException("Test error");
      } catch (WasmException e) {
        assertEquals("Test error", e.getMessage(), "Should be catchable as WasmException");
      }
    }
  }

  // ============================================================================
  // MUTATION TESTING COVERAGE TESTS
  // ============================================================================
  // The following tests are specifically designed to kill PIT mutations that
  // survive basic functionality tests. They test:
  // 1. Boolean return value mutations for violation check methods
  // 2. Exact count verification for security context categories
  // 3. getSecurityViolationDescription edge cases (null fields)
  // 4. Getter return value exactness
  // ============================================================================

  @Nested
  @DisplayName("Violation Check Boolean Return Mutation Tests")
  class ViolationCheckBooleanReturnMutationTests {

    // -------------------------------------------------------------------------
    // isHostFunctionViolation() - Tests for false returns on non-HOST_FUNCTION contexts
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isHostFunctionViolation returns false for WASI_CAPABILITY")
    void isHostFunctionViolationReturnsFalseForWasiCapability() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.WASI_CAPABILITY);
      assertFalse(ex.isHostFunctionViolation(), "WASI_CAPABILITY should NOT be host function");
    }

    @Test
    @DisplayName("isHostFunctionViolation returns false for MODULE_IMPORT_EXPORT")
    void isHostFunctionViolationReturnsFalseForModuleImportExport() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.MODULE_IMPORT_EXPORT);
      assertFalse(ex.isHostFunctionViolation(), "MODULE_IMPORT_EXPORT should NOT be host function");
    }

    @Test
    @DisplayName("isHostFunctionViolation returns false for MEMORY_ACCESS")
    void isHostFunctionViolationReturnsFalseForMemoryAccess() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.MEMORY_ACCESS);
      assertFalse(ex.isHostFunctionViolation(), "MEMORY_ACCESS should NOT be host function");
    }

    @Test
    @DisplayName("isHostFunctionViolation returns false for FILE_SYSTEM_ACCESS")
    void isHostFunctionViolationReturnsFalseForFileSystemAccess() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.FILE_SYSTEM_ACCESS);
      assertFalse(ex.isHostFunctionViolation(), "FILE_SYSTEM_ACCESS should NOT be host function");
    }

    @Test
    @DisplayName("isHostFunctionViolation returns false for NETWORK_ACCESS")
    void isHostFunctionViolationReturnsFalseForNetworkAccess() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.NETWORK_ACCESS);
      assertFalse(ex.isHostFunctionViolation(), "NETWORK_ACCESS should NOT be host function");
    }

    @Test
    @DisplayName("isHostFunctionViolation returns false for SYSTEM_RESOURCE")
    void isHostFunctionViolationReturnsFalseForSystemResource() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.SYSTEM_RESOURCE);
      assertFalse(ex.isHostFunctionViolation(), "SYSTEM_RESOURCE should NOT be host function");
    }

    @Test
    @DisplayName("isHostFunctionViolation returns false for SANDBOX_POLICY")
    void isHostFunctionViolationReturnsFalseForSandboxPolicy() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.SANDBOX_POLICY);
      assertFalse(ex.isHostFunctionViolation(), "SANDBOX_POLICY should NOT be host function");
    }

    // -------------------------------------------------------------------------
    // isWasiCapabilityViolation() - Tests for false returns
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isWasiCapabilityViolation returns false for HOST_FUNCTION")
    void isWasiCapabilityViolationReturnsFalseForHostFunction() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.HOST_FUNCTION);
      assertFalse(ex.isWasiCapabilityViolation(), "HOST_FUNCTION should NOT be WASI capability");
    }

    @Test
    @DisplayName("isWasiCapabilityViolation returns false for MODULE_IMPORT_EXPORT")
    void isWasiCapabilityViolationReturnsFalseForModuleImportExport() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.MODULE_IMPORT_EXPORT);
      assertFalse(
          ex.isWasiCapabilityViolation(), "MODULE_IMPORT_EXPORT should NOT be WASI capability");
    }

    @Test
    @DisplayName("isWasiCapabilityViolation returns false for MEMORY_ACCESS")
    void isWasiCapabilityViolationReturnsFalseForMemoryAccess() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.MEMORY_ACCESS);
      assertFalse(ex.isWasiCapabilityViolation(), "MEMORY_ACCESS should NOT be WASI capability");
    }

    @Test
    @DisplayName("isWasiCapabilityViolation returns false for FILE_SYSTEM_ACCESS")
    void isWasiCapabilityViolationReturnsFalseForFileSystemAccess() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.FILE_SYSTEM_ACCESS);
      assertFalse(
          ex.isWasiCapabilityViolation(), "FILE_SYSTEM_ACCESS should NOT be WASI capability");
    }

    @Test
    @DisplayName("isWasiCapabilityViolation returns false for NETWORK_ACCESS")
    void isWasiCapabilityViolationReturnsFalseForNetworkAccess() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.NETWORK_ACCESS);
      assertFalse(ex.isWasiCapabilityViolation(), "NETWORK_ACCESS should NOT be WASI capability");
    }

    @Test
    @DisplayName("isWasiCapabilityViolation returns false for SYSTEM_RESOURCE")
    void isWasiCapabilityViolationReturnsFalseForSystemResource() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.SYSTEM_RESOURCE);
      assertFalse(ex.isWasiCapabilityViolation(), "SYSTEM_RESOURCE should NOT be WASI capability");
    }

    @Test
    @DisplayName("isWasiCapabilityViolation returns false for SANDBOX_POLICY")
    void isWasiCapabilityViolationReturnsFalseForSandboxPolicy() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.SANDBOX_POLICY);
      assertFalse(ex.isWasiCapabilityViolation(), "SANDBOX_POLICY should NOT be WASI capability");
    }

    // -------------------------------------------------------------------------
    // isFileSystemViolation() - Tests for false returns
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isFileSystemViolation returns false for HOST_FUNCTION")
    void isFileSystemViolationReturnsFalseForHostFunction() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.HOST_FUNCTION);
      assertFalse(ex.isFileSystemViolation(), "HOST_FUNCTION should NOT be file system");
    }

    @Test
    @DisplayName("isFileSystemViolation returns false for WASI_CAPABILITY")
    void isFileSystemViolationReturnsFalseForWasiCapability() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.WASI_CAPABILITY);
      assertFalse(ex.isFileSystemViolation(), "WASI_CAPABILITY should NOT be file system");
    }

    @Test
    @DisplayName("isFileSystemViolation returns false for MODULE_IMPORT_EXPORT")
    void isFileSystemViolationReturnsFalseForModuleImportExport() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.MODULE_IMPORT_EXPORT);
      assertFalse(ex.isFileSystemViolation(), "MODULE_IMPORT_EXPORT should NOT be file system");
    }

    @Test
    @DisplayName("isFileSystemViolation returns false for MEMORY_ACCESS")
    void isFileSystemViolationReturnsFalseForMemoryAccess() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.MEMORY_ACCESS);
      assertFalse(ex.isFileSystemViolation(), "MEMORY_ACCESS should NOT be file system");
    }

    @Test
    @DisplayName("isFileSystemViolation returns false for NETWORK_ACCESS")
    void isFileSystemViolationReturnsFalseForNetworkAccess() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.NETWORK_ACCESS);
      assertFalse(ex.isFileSystemViolation(), "NETWORK_ACCESS should NOT be file system");
    }

    @Test
    @DisplayName("isFileSystemViolation returns false for SYSTEM_RESOURCE")
    void isFileSystemViolationReturnsFalseForSystemResource() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.SYSTEM_RESOURCE);
      assertFalse(ex.isFileSystemViolation(), "SYSTEM_RESOURCE should NOT be file system");
    }

    @Test
    @DisplayName("isFileSystemViolation returns false for SANDBOX_POLICY")
    void isFileSystemViolationReturnsFalseForSandboxPolicy() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.SANDBOX_POLICY);
      assertFalse(ex.isFileSystemViolation(), "SANDBOX_POLICY should NOT be file system");
    }

    // -------------------------------------------------------------------------
    // isNetworkViolation() - Tests for false returns
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isNetworkViolation returns false for HOST_FUNCTION")
    void isNetworkViolationReturnsFalseForHostFunction() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.HOST_FUNCTION);
      assertFalse(ex.isNetworkViolation(), "HOST_FUNCTION should NOT be network");
    }

    @Test
    @DisplayName("isNetworkViolation returns false for WASI_CAPABILITY")
    void isNetworkViolationReturnsFalseForWasiCapability() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.WASI_CAPABILITY);
      assertFalse(ex.isNetworkViolation(), "WASI_CAPABILITY should NOT be network");
    }

    @Test
    @DisplayName("isNetworkViolation returns false for MODULE_IMPORT_EXPORT")
    void isNetworkViolationReturnsFalseForModuleImportExport() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.MODULE_IMPORT_EXPORT);
      assertFalse(ex.isNetworkViolation(), "MODULE_IMPORT_EXPORT should NOT be network");
    }

    @Test
    @DisplayName("isNetworkViolation returns false for MEMORY_ACCESS")
    void isNetworkViolationReturnsFalseForMemoryAccess() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.MEMORY_ACCESS);
      assertFalse(ex.isNetworkViolation(), "MEMORY_ACCESS should NOT be network");
    }

    @Test
    @DisplayName("isNetworkViolation returns false for FILE_SYSTEM_ACCESS")
    void isNetworkViolationReturnsFalseForFileSystemAccess() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.FILE_SYSTEM_ACCESS);
      assertFalse(ex.isNetworkViolation(), "FILE_SYSTEM_ACCESS should NOT be network");
    }

    @Test
    @DisplayName("isNetworkViolation returns false for SYSTEM_RESOURCE")
    void isNetworkViolationReturnsFalseForSystemResource() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.SYSTEM_RESOURCE);
      assertFalse(ex.isNetworkViolation(), "SYSTEM_RESOURCE should NOT be network");
    }

    @Test
    @DisplayName("isNetworkViolation returns false for SANDBOX_POLICY")
    void isNetworkViolationReturnsFalseForSandboxPolicy() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.SANDBOX_POLICY);
      assertFalse(ex.isNetworkViolation(), "SANDBOX_POLICY should NOT be network");
    }

    // -------------------------------------------------------------------------
    // isMemoryViolation() - Tests for false returns
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isMemoryViolation returns false for HOST_FUNCTION")
    void isMemoryViolationReturnsFalseForHostFunction() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.HOST_FUNCTION);
      assertFalse(ex.isMemoryViolation(), "HOST_FUNCTION should NOT be memory");
    }

    @Test
    @DisplayName("isMemoryViolation returns false for WASI_CAPABILITY")
    void isMemoryViolationReturnsFalseForWasiCapability() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.WASI_CAPABILITY);
      assertFalse(ex.isMemoryViolation(), "WASI_CAPABILITY should NOT be memory");
    }

    @Test
    @DisplayName("isMemoryViolation returns false for MODULE_IMPORT_EXPORT")
    void isMemoryViolationReturnsFalseForModuleImportExport() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.MODULE_IMPORT_EXPORT);
      assertFalse(ex.isMemoryViolation(), "MODULE_IMPORT_EXPORT should NOT be memory");
    }

    @Test
    @DisplayName("isMemoryViolation returns false for FILE_SYSTEM_ACCESS")
    void isMemoryViolationReturnsFalseForFileSystemAccess() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.FILE_SYSTEM_ACCESS);
      assertFalse(ex.isMemoryViolation(), "FILE_SYSTEM_ACCESS should NOT be memory");
    }

    @Test
    @DisplayName("isMemoryViolation returns false for NETWORK_ACCESS")
    void isMemoryViolationReturnsFalseForNetworkAccess() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.NETWORK_ACCESS);
      assertFalse(ex.isMemoryViolation(), "NETWORK_ACCESS should NOT be memory");
    }

    @Test
    @DisplayName("isMemoryViolation returns false for SYSTEM_RESOURCE")
    void isMemoryViolationReturnsFalseForSystemResource() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.SYSTEM_RESOURCE);
      assertFalse(ex.isMemoryViolation(), "SYSTEM_RESOURCE should NOT be memory");
    }

    @Test
    @DisplayName("isMemoryViolation returns false for SANDBOX_POLICY")
    void isMemoryViolationReturnsFalseForSandboxPolicy() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.SANDBOX_POLICY);
      assertFalse(ex.isMemoryViolation(), "SANDBOX_POLICY should NOT be memory");
    }

    // -------------------------------------------------------------------------
    // isSandboxViolation() - Tests for false returns
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isSandboxViolation returns false for HOST_FUNCTION")
    void isSandboxViolationReturnsFalseForHostFunction() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.HOST_FUNCTION);
      assertFalse(ex.isSandboxViolation(), "HOST_FUNCTION should NOT be sandbox");
    }

    @Test
    @DisplayName("isSandboxViolation returns false for WASI_CAPABILITY")
    void isSandboxViolationReturnsFalseForWasiCapability() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.WASI_CAPABILITY);
      assertFalse(ex.isSandboxViolation(), "WASI_CAPABILITY should NOT be sandbox");
    }

    @Test
    @DisplayName("isSandboxViolation returns false for MODULE_IMPORT_EXPORT")
    void isSandboxViolationReturnsFalseForModuleImportExport() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.MODULE_IMPORT_EXPORT);
      assertFalse(ex.isSandboxViolation(), "MODULE_IMPORT_EXPORT should NOT be sandbox");
    }

    @Test
    @DisplayName("isSandboxViolation returns false for MEMORY_ACCESS")
    void isSandboxViolationReturnsFalseForMemoryAccess() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.MEMORY_ACCESS);
      assertFalse(ex.isSandboxViolation(), "MEMORY_ACCESS should NOT be sandbox");
    }

    @Test
    @DisplayName("isSandboxViolation returns false for FILE_SYSTEM_ACCESS")
    void isSandboxViolationReturnsFalseForFileSystemAccess() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.FILE_SYSTEM_ACCESS);
      assertFalse(ex.isSandboxViolation(), "FILE_SYSTEM_ACCESS should NOT be sandbox");
    }

    @Test
    @DisplayName("isSandboxViolation returns false for NETWORK_ACCESS")
    void isSandboxViolationReturnsFalseForNetworkAccess() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.NETWORK_ACCESS);
      assertFalse(ex.isSandboxViolation(), "NETWORK_ACCESS should NOT be sandbox");
    }

    @Test
    @DisplayName("isSandboxViolation returns false for SYSTEM_RESOURCE")
    void isSandboxViolationReturnsFalseForSystemResource() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", SecurityContext.SYSTEM_RESOURCE);
      assertFalse(ex.isSandboxViolation(), "SYSTEM_RESOURCE should NOT be sandbox");
    }
  }

  @Nested
  @DisplayName("Violation Category Count Verification Tests")
  class ViolationCategoryCountVerificationTests {

    @Test
    @DisplayName("Should have exactly 1 host function context")
    void shouldHaveExactlyOneHostFunctionContext() {
      int count = 0;
      for (final SecurityContext ctx : SecurityContext.values()) {
        final SecurityException ex = new SecurityException("Error", "p", "a", ctx);
        if (ex.isHostFunctionViolation()) {
          count++;
        }
      }
      assertEquals(1, count, "Should have exactly 1 host function context");
    }

    @Test
    @DisplayName("Should have exactly 1 WASI capability context")
    void shouldHaveExactlyOneWasiCapabilityContext() {
      int count = 0;
      for (final SecurityContext ctx : SecurityContext.values()) {
        final SecurityException ex = new SecurityException("Error", "p", "a", ctx);
        if (ex.isWasiCapabilityViolation()) {
          count++;
        }
      }
      assertEquals(1, count, "Should have exactly 1 WASI capability context");
    }

    @Test
    @DisplayName("Should have exactly 1 file system context")
    void shouldHaveExactlyOneFileSystemContext() {
      int count = 0;
      for (final SecurityContext ctx : SecurityContext.values()) {
        final SecurityException ex = new SecurityException("Error", "p", "a", ctx);
        if (ex.isFileSystemViolation()) {
          count++;
        }
      }
      assertEquals(1, count, "Should have exactly 1 file system context");
    }

    @Test
    @DisplayName("Should have exactly 1 network context")
    void shouldHaveExactlyOneNetworkContext() {
      int count = 0;
      for (final SecurityContext ctx : SecurityContext.values()) {
        final SecurityException ex = new SecurityException("Error", "p", "a", ctx);
        if (ex.isNetworkViolation()) {
          count++;
        }
      }
      assertEquals(1, count, "Should have exactly 1 network context");
    }

    @Test
    @DisplayName("Should have exactly 1 memory context")
    void shouldHaveExactlyOneMemoryContext() {
      int count = 0;
      for (final SecurityContext ctx : SecurityContext.values()) {
        final SecurityException ex = new SecurityException("Error", "p", "a", ctx);
        if (ex.isMemoryViolation()) {
          count++;
        }
      }
      assertEquals(1, count, "Should have exactly 1 memory context");
    }

    @Test
    @DisplayName("Should have exactly 1 sandbox context")
    void shouldHaveExactlyOneSandboxContext() {
      int count = 0;
      for (final SecurityContext ctx : SecurityContext.values()) {
        final SecurityException ex = new SecurityException("Error", "p", "a", ctx);
        if (ex.isSandboxViolation()) {
          count++;
        }
      }
      assertEquals(1, count, "Should have exactly 1 sandbox context");
    }
  }

  @Nested
  @DisplayName("GetSecurityViolationDescription Edge Case Mutation Tests")
  class GetSecurityViolationDescriptionEdgeCaseMutationTests {

    @Test
    @DisplayName("Description with null policy should not include policy section")
    void descriptionWithNullPolicyShouldNotIncludePolicySection() {
      final SecurityException ex =
          new SecurityException("Error", null, "action", SecurityContext.HOST_FUNCTION);
      final String desc = ex.getSecurityViolationDescription();
      assertFalse(desc.contains("[policy:"), "Should NOT include policy section when null");
      assertTrue(desc.contains("[action:"), "Should include action section");
    }

    @Test
    @DisplayName("Description with null action should not include action section")
    void descriptionWithNullActionShouldNotIncludeActionSection() {
      final SecurityException ex =
          new SecurityException("Error", "policy", null, SecurityContext.HOST_FUNCTION);
      final String desc = ex.getSecurityViolationDescription();
      assertTrue(desc.contains("[policy:"), "Should include policy section");
      assertFalse(desc.contains("[action:"), "Should NOT include action section when null");
    }

    @Test
    @DisplayName("Description with null resource should not include resource section")
    void descriptionWithNullResourceShouldNotIncludeResourceSection() {
      final SecurityException ex =
          new SecurityException("Error", "policy", "action", null, SecurityContext.HOST_FUNCTION);
      final String desc = ex.getSecurityViolationDescription();
      assertFalse(desc.contains("[resource:"), "Should NOT include resource section when null");
    }

    @Test
    @DisplayName("Description with all null optional fields should only have context")
    void descriptionWithAllNullOptionalFieldsShouldOnlyHaveContext() {
      final SecurityException ex = new SecurityException("Error");
      final String desc = ex.getSecurityViolationDescription();
      assertTrue(desc.contains("[context:"), "Should include context section");
      assertFalse(desc.contains("[policy:"), "Should NOT include policy section");
      assertFalse(desc.contains("[action:"), "Should NOT include action section");
      assertFalse(desc.contains("[resource:"), "Should NOT include resource section");
    }

    @Test
    @DisplayName("Description with all fields should include all sections")
    void descriptionWithAllFieldsShouldIncludeAllSections() {
      final SecurityException ex =
          new SecurityException(
              "Error", "test-policy", "test-action", "test-resource", SecurityContext.HOST_FUNCTION);
      final String desc = ex.getSecurityViolationDescription();
      assertTrue(desc.contains("[context:"), "Should include context section");
      assertTrue(desc.contains("[policy: test-policy]"), "Should include policy section");
      assertTrue(desc.contains("[action: test-action]"), "Should include action section");
      assertTrue(desc.contains("[resource: test-resource]"), "Should include resource section");
    }

    @Test
    @DisplayName("Context name should be lowercase with spaces")
    void contextNameShouldBeLowercaseWithSpaces() {
      final SecurityException ex =
          new SecurityException("Error", "p", "a", SecurityContext.FILE_SYSTEM_ACCESS);
      final String desc = ex.getSecurityViolationDescription();
      assertTrue(
          desc.contains("file system access"),
          "FILE_SYSTEM_ACCESS should be 'file system access': " + desc);
    }

    @Test
    @DisplayName("Each context has unique description prefix")
    void eachContextHasUniqueDescriptionPrefix() {
      final java.util.Set<String> contextPrefixes = new java.util.HashSet<>();
      for (final SecurityContext ctx : SecurityContext.values()) {
        final SecurityException ex = new SecurityException("Error", null, null, ctx);
        final String desc = ex.getSecurityViolationDescription();
        // Extract just the context part
        final int start = desc.indexOf("[context: ") + 10;
        final int end = desc.indexOf("]", start);
        final String contextPart = desc.substring(start, end);
        assertFalse(
            contextPrefixes.contains(contextPart),
            "Context '" + contextPart + "' should be unique for " + ctx.name());
        contextPrefixes.add(contextPart);
      }
      assertEquals(
          SecurityContext.values().length,
          contextPrefixes.size(),
          "All contexts should have unique prefixes");
    }
  }

  @Nested
  @DisplayName("Getter Return Value Mutation Tests")
  class GetterReturnValueMutationTests {

    @Test
    @DisplayName("getViolatedPolicy returns exact value set in constructor")
    void getViolatedPolicyReturnsExactValue() {
      final String policy = "unique-policy-12345";
      final SecurityException ex =
          new SecurityException("Error", policy, "action", SecurityContext.HOST_FUNCTION);
      assertSame(policy, ex.getViolatedPolicy(), "Should return exact same policy instance");
    }

    @Test
    @DisplayName("getAttemptedAction returns exact value set in constructor")
    void getAttemptedActionReturnsExactValue() {
      final String action = "unique-action-12345";
      final SecurityException ex =
          new SecurityException("Error", "policy", action, SecurityContext.HOST_FUNCTION);
      assertSame(action, ex.getAttemptedAction(), "Should return exact same action instance");
    }

    @Test
    @DisplayName("getDeniedResource returns exact value set in constructor")
    void getDeniedResourceReturnsExactValue() {
      final String resource = "unique-resource-12345";
      final SecurityException ex =
          new SecurityException(
              "Error", "policy", "action", resource, SecurityContext.HOST_FUNCTION);
      assertSame(resource, ex.getDeniedResource(), "Should return exact same resource instance");
    }

    @Test
    @DisplayName("getSecurityContext returns exact context set in constructor")
    void getSecurityContextReturnsExactValue() {
      for (final SecurityContext ctx : SecurityContext.values()) {
        final SecurityException ex = new SecurityException("Error", "p", "a", ctx);
        assertSame(ctx, ex.getSecurityContext(), "Should return same context for " + ctx.name());
      }
    }

    @Test
    @DisplayName("getViolatedPolicy returns null when not provided")
    void getViolatedPolicyReturnsNullWhenNotProvided() {
      final SecurityException ex = new SecurityException("Error");
      assertNull(ex.getViolatedPolicy(), "Should return null when not provided");
    }

    @Test
    @DisplayName("getAttemptedAction returns null when not provided")
    void getAttemptedActionReturnsNullWhenNotProvided() {
      final SecurityException ex = new SecurityException("Error");
      assertNull(ex.getAttemptedAction(), "Should return null when not provided");
    }

    @Test
    @DisplayName("getDeniedResource returns null when not provided")
    void getDeniedResourceReturnsNullWhenNotProvided() {
      final SecurityException ex = new SecurityException("Error");
      assertNull(ex.getDeniedResource(), "Should return null when not provided");
    }

    @Test
    @DisplayName("getCause returns exact cause set in constructor")
    void getCauseReturnsExactValue() {
      final Throwable cause = new java.lang.RuntimeException("cause");
      final SecurityException ex =
          new SecurityException(
              "Error", "policy", "action", "resource", SecurityContext.HOST_FUNCTION, cause);
      assertSame(cause, ex.getCause(), "Should return exact same cause instance");
    }
  }

  @Nested
  @DisplayName("Default Security Context Mutation Tests")
  class DefaultSecurityContextMutationTests {

    @Test
    @DisplayName("Simple constructor defaults to SANDBOX_POLICY")
    void simpleConstructorDefaultsToSandboxPolicy() {
      final SecurityException ex = new SecurityException("Error");
      assertEquals(
          SecurityContext.SANDBOX_POLICY,
          ex.getSecurityContext(),
          "Simple constructor should default to SANDBOX_POLICY");
    }

    @Test
    @DisplayName("Constructor with cause defaults to SANDBOX_POLICY")
    void constructorWithCauseDefaultsToSandboxPolicy() {
      final SecurityException ex =
          new SecurityException("Error", new java.lang.RuntimeException("cause"));
      assertEquals(
          SecurityContext.SANDBOX_POLICY,
          ex.getSecurityContext(),
          "Constructor with cause should default to SANDBOX_POLICY");
    }
  }
}
