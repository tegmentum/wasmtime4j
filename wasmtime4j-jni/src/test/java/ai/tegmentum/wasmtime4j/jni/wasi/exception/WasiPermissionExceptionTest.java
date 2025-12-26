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

package ai.tegmentum.wasmtime4j.jni.wasi.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiPermissionException.PermissionViolationType;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasiPermissionException} class.
 *
 * <p>This test class verifies WasiPermissionException constructors and factory methods.
 */
@DisplayName("WasiPermissionException Tests")
class WasiPermissionExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiPermissionException should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(WasiPermissionException.class.getModifiers()),
          "WasiPermissionException should be final");
    }

    @Test
    @DisplayName("WasiPermissionException should extend WasiException")
    void shouldExtendWasiException() {
      assertTrue(
          WasiException.class.isAssignableFrom(WasiPermissionException.class),
          "WasiPermissionException should extend WasiException");
    }
  }

  @Nested
  @DisplayName("PermissionViolationType Tests")
  class PermissionViolationTypeTests {

    @Test
    @DisplayName("All violation types should have descriptions")
    void allViolationTypesShouldHaveDescriptions() {
      for (PermissionViolationType type : PermissionViolationType.values()) {
        assertNotNull(type.getDescription(), "Description should not be null: " + type.name());
        assertFalse(
            type.getDescription().isEmpty(), "Description should not be empty: " + type.name());
      }
    }

    @Test
    @DisplayName("toString should return description")
    void toStringShouldReturnDescription() {
      assertEquals(
          "File system access denied",
          PermissionViolationType.FILE_SYSTEM_ACCESS.toString(),
          "toString should return description");
    }

    @Test
    @DisplayName("Should have expected violation types")
    void shouldHaveExpectedViolationTypes() {
      assertNotNull(PermissionViolationType.FILE_SYSTEM_ACCESS);
      assertNotNull(PermissionViolationType.SANDBOX_ESCAPE);
      assertNotNull(PermissionViolationType.PATH_TRAVERSAL);
      assertNotNull(PermissionViolationType.ENVIRONMENT_ACCESS);
      assertNotNull(PermissionViolationType.DANGEROUS_OPERATION);
      assertNotNull(PermissionViolationType.RESOURCE_LIMIT_EXCEEDED);
      assertNotNull(PermissionViolationType.CAPABILITY_NOT_GRANTED);
      assertNotNull(PermissionViolationType.SECURITY_POLICY_VIOLATION);
      assertNotNull(PermissionViolationType.UNKNOWN);
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create exception with message, violation type, operation, and resource")
    void shouldCreateExceptionWithFullParameters() {
      final WasiPermissionException exception =
          new WasiPermissionException(
              "Access denied", PermissionViolationType.FILE_SYSTEM_ACCESS, "open", "/secret/file");

      assertEquals(
          PermissionViolationType.FILE_SYSTEM_ACCESS,
          exception.getViolationType(),
          "Violation type should match");
      assertEquals(
          "/secret/file", exception.getAttemptedResource(), "Attempted resource should match");
      assertNull(exception.getViolatedPolicy(), "Violated policy should be null");
      assertEquals(WasiErrorCode.EPERM, exception.getErrorCode(), "Error code should be EPERM");
    }

    @Test
    @DisplayName("Should create exception with violated policy")
    void shouldCreateExceptionWithViolatedPolicy() {
      final WasiPermissionException exception =
          new WasiPermissionException(
              "Policy violation",
              PermissionViolationType.SECURITY_POLICY_VIOLATION,
              "execute",
              "/bin/shell",
              "no-shell-execution");

      assertEquals(
          "no-shell-execution", exception.getViolatedPolicy(), "Violated policy should match");
    }

    @Test
    @DisplayName("Should create exception with access denied code")
    void shouldCreateExceptionWithAccessDeniedCode() {
      final WasiPermissionException exception =
          new WasiPermissionException(
              "Access denied",
              PermissionViolationType.FILE_SYSTEM_ACCESS,
              "read",
              "/protected",
              true);

      assertEquals(
          WasiErrorCode.EACCES,
          exception.getErrorCode(),
          "Error code should be EACCES when useAccessDeniedCode is true");
    }

    @Test
    @DisplayName("Should create exception with EPERM code when useAccessDeniedCode is false")
    void shouldCreateExceptionWithEpermCodeWhenFalse() {
      final WasiPermissionException exception =
          new WasiPermissionException(
              "Operation denied",
              PermissionViolationType.DANGEROUS_OPERATION,
              "exec",
              "/malware",
              false);

      assertEquals(
          WasiErrorCode.EPERM,
          exception.getErrorCode(),
          "Error code should be EPERM when useAccessDeniedCode is false");
    }

    @Test
    @DisplayName("Should create exception with message only")
    void shouldCreateExceptionWithMessageOnly() {
      final WasiPermissionException exception =
          new WasiPermissionException("Simple permission error");

      assertNotNull(exception.getMessage(), "Message should not be null");
      assertEquals(
          PermissionViolationType.UNKNOWN,
          exception.getViolationType(),
          "Violation type should be UNKNOWN");
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final RuntimeException cause = new RuntimeException("Original error");
      final WasiPermissionException exception =
          new WasiPermissionException("Permission failed", cause);

      assertNotNull(exception.getCause(), "Cause should be preserved");
    }
  }

  @Nested
  @DisplayName("Violation Category Tests")
  class ViolationCategoryTests {

    @Test
    @DisplayName("isFileSystemViolation should return true for FS violations")
    void isFileSystemViolationShouldReturnTrueForFsViolations() {
      assertTrue(
          new WasiPermissionException("FS", PermissionViolationType.FILE_SYSTEM_ACCESS, "op", "res")
              .isFileSystemViolation(),
          "Should detect FILE_SYSTEM_ACCESS");

      assertTrue(
          new WasiPermissionException(
                  "Sandbox", PermissionViolationType.SANDBOX_ESCAPE, "op", "res")
              .isFileSystemViolation(),
          "Should detect SANDBOX_ESCAPE");

      assertTrue(
          new WasiPermissionException("Path", PermissionViolationType.PATH_TRAVERSAL, "op", "res")
              .isFileSystemViolation(),
          "Should detect PATH_TRAVERSAL");
    }

    @Test
    @DisplayName("isFileSystemViolation should return false for non-FS violations")
    void isFileSystemViolationShouldReturnFalseForNonFsViolations() {
      assertFalse(
          new WasiPermissionException(
                  "Env", PermissionViolationType.ENVIRONMENT_ACCESS, "op", "res")
              .isFileSystemViolation(),
          "ENVIRONMENT_ACCESS is not FS violation");
    }

    @Test
    @DisplayName("isDangerousOperationViolation should detect dangerous operations")
    void isDangerousOperationViolationShouldDetectDangerousOperations() {
      assertTrue(
          new WasiPermissionException(
                  "Danger", PermissionViolationType.DANGEROUS_OPERATION, "op", "res")
              .isDangerousOperationViolation(),
          "Should detect DANGEROUS_OPERATION");

      assertFalse(
          new WasiPermissionException("FS", PermissionViolationType.FILE_SYSTEM_ACCESS, "op", "res")
              .isDangerousOperationViolation(),
          "FS is not dangerous operation");
    }

    @Test
    @DisplayName("isResourceLimitViolation should detect resource limit violations")
    void isResourceLimitViolationShouldDetectResourceLimitViolations() {
      assertTrue(
          new WasiPermissionException(
                  "Limit", PermissionViolationType.RESOURCE_LIMIT_EXCEEDED, "op", "res")
              .isResourceLimitViolation(),
          "Should detect RESOURCE_LIMIT_EXCEEDED");

      assertFalse(
          new WasiPermissionException("FS", PermissionViolationType.FILE_SYSTEM_ACCESS, "op", "res")
              .isResourceLimitViolation(),
          "FS is not resource limit violation");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("fileSystemAccessDenied should create correct exception")
    void fileSystemAccessDeniedShouldCreateCorrectException() {
      final WasiPermissionException exception =
          WasiPermissionException.fileSystemAccessDenied("read", "/protected/file");

      assertEquals(PermissionViolationType.FILE_SYSTEM_ACCESS, exception.getViolationType());
      assertEquals(WasiErrorCode.EACCES, exception.getErrorCode());
      assertTrue(exception.getMessage().contains("/protected/file"));
    }

    @Test
    @DisplayName("sandboxEscape should create correct exception")
    void sandboxEscapeShouldCreateCorrectException() {
      final WasiPermissionException exception =
          WasiPermissionException.sandboxEscape("stat", "../../etc/passwd");

      assertEquals(PermissionViolationType.SANDBOX_ESCAPE, exception.getViolationType());
      assertEquals(WasiErrorCode.EPERM, exception.getErrorCode());
      assertTrue(exception.getMessage().contains("Sandbox escape"));
    }

    @Test
    @DisplayName("pathTraversal should create correct exception")
    void pathTraversalShouldCreateCorrectException() {
      final WasiPermissionException exception =
          WasiPermissionException.pathTraversal("open", "../../../etc/shadow");

      assertEquals(PermissionViolationType.PATH_TRAVERSAL, exception.getViolationType());
      assertTrue(exception.getMessage().contains("Path traversal"));
    }

    @Test
    @DisplayName("environmentAccessDenied should create correct exception")
    void environmentAccessDeniedShouldCreateCorrectException() {
      final WasiPermissionException exception =
          WasiPermissionException.environmentAccessDenied("getenv", "AWS_SECRET_KEY");

      assertEquals(PermissionViolationType.ENVIRONMENT_ACCESS, exception.getViolationType());
      assertEquals(WasiErrorCode.EACCES, exception.getErrorCode());
      assertTrue(exception.getMessage().contains("AWS_SECRET_KEY"));
    }

    @Test
    @DisplayName("dangerousOperation should create correct exception")
    void dangerousOperationShouldCreateCorrectException() {
      final WasiPermissionException exception =
          WasiPermissionException.dangerousOperation("exec", "/bin/rm");

      assertEquals(PermissionViolationType.DANGEROUS_OPERATION, exception.getViolationType());
      assertEquals(WasiErrorCode.EPERM, exception.getErrorCode());
      assertTrue(exception.getMessage().contains("Dangerous operation"));
    }

    @Test
    @DisplayName("resourceLimitExceeded should create correct exception")
    void resourceLimitExceededShouldCreateCorrectException() {
      final WasiPermissionException exception =
          WasiPermissionException.resourceLimitExceeded("alloc", "memory", "1GB");

      assertEquals(PermissionViolationType.RESOURCE_LIMIT_EXCEEDED, exception.getViolationType());
      assertTrue(exception.getMessage().contains("memory"));
      assertTrue(exception.getMessage().contains("1GB"));
    }

    @Test
    @DisplayName("capabilityNotGranted should create correct exception")
    void capabilityNotGrantedShouldCreateCorrectException() {
      final WasiPermissionException exception =
          WasiPermissionException.capabilityNotGranted("socket", "network");

      assertEquals(PermissionViolationType.CAPABILITY_NOT_GRANTED, exception.getViolationType());
      assertTrue(exception.getMessage().contains("network"));
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should be throwable and catchable")
    void shouldBeThrowableAndCatchable() {
      try {
        throw WasiPermissionException.sandboxEscape("stat", "/etc/passwd");
      } catch (WasiPermissionException e) {
        assertEquals(
            PermissionViolationType.SANDBOX_ESCAPE,
            e.getViolationType(),
            "Should catch with correct violation type");
      }
    }

    @Test
    @DisplayName("Should be catchable as WasiException")
    void shouldBeCatchableAsWasiException() {
      try {
        throw WasiPermissionException.fileSystemAccessDenied("write", "/secret");
      } catch (WasiException e) {
        assertTrue(
            e instanceof WasiPermissionException, "Should be instance of WasiPermissionException");
        assertTrue(e.isPermissionError(), "Should be permission error");
      }
    }

    @Test
    @DisplayName("Exception chain should work correctly")
    void exceptionChainShouldWorkCorrectly() {
      final WasiPermissionException exception =
          WasiPermissionException.dangerousOperation("exec", "/bin/malware");

      // Check that it's properly categorized
      assertTrue(exception.isDangerousOperationViolation());
      assertFalse(exception.isFileSystemViolation());
      assertFalse(exception.isResourceLimitViolation());

      // Check error code
      assertEquals(WasiErrorCode.EPERM, exception.getErrorCode());
    }

    @Test
    @DisplayName("All factory methods should produce valid exceptions")
    void allFactoryMethodsShouldProduceValidExceptions() {
      final WasiPermissionException[] exceptions = {
        WasiPermissionException.fileSystemAccessDenied("op", "res"),
        WasiPermissionException.sandboxEscape("op", "res"),
        WasiPermissionException.pathTraversal("op", "res"),
        WasiPermissionException.environmentAccessDenied("op", "var"),
        WasiPermissionException.dangerousOperation("op", "res"),
        WasiPermissionException.resourceLimitExceeded("op", "type", "limit"),
        WasiPermissionException.capabilityNotGranted("op", "cap")
      };

      for (WasiPermissionException e : exceptions) {
        assertNotNull(e.getMessage(), "Message should not be null");
        assertNotNull(e.getViolationType(), "Violation type should not be null");
        assertNotNull(e.getErrorCode(), "Error code should not be null");
        assertTrue(e.isPermissionError(), "Should be permission error");
      }
    }
  }
}
