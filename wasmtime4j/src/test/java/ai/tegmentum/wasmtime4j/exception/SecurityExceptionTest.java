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
}
