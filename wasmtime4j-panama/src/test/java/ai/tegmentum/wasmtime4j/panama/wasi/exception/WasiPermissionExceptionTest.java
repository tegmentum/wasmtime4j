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

package ai.tegmentum.wasmtime4j.panama.wasi.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.exception.PanamaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Panama {@link WasiPermissionException} class.
 *
 * <p>This test class verifies WasiPermissionException constructors and behavior.
 */
@DisplayName("Panama WasiPermissionException Tests")
class WasiPermissionExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiPermissionException should extend WasiFileSystemException")
    void shouldExtendWasiFileSystemException() {
      assertTrue(WasiFileSystemException.class.isAssignableFrom(WasiPermissionException.class),
          "WasiPermissionException should extend WasiFileSystemException");
    }

    @Test
    @DisplayName("WasiPermissionException should extend PanamaException")
    void shouldExtendPanamaException() {
      assertTrue(PanamaException.class.isAssignableFrom(WasiPermissionException.class),
          "WasiPermissionException should extend PanamaException");
    }

    @Test
    @DisplayName("WasiPermissionException should be a RuntimeException")
    void shouldBeRuntimeException() {
      assertTrue(RuntimeException.class.isAssignableFrom(WasiPermissionException.class),
          "WasiPermissionException should be a RuntimeException");
    }
  }

  @Nested
  @DisplayName("Constructor(String) Tests")
  class ConstructorStringTests {

    @Test
    @DisplayName("Should create exception with message and EACCES error code")
    void shouldCreateExceptionWithMessageAndEaccesErrorCode() {
      final WasiPermissionException exception = new WasiPermissionException(
          "Access denied to resource");

      assertEquals("Access denied to resource", exception.getMessage(), "Message should match");
      assertEquals("EACCES", exception.getWasiErrorCode(), "Error code should be EACCES");
    }

    @Test
    @DisplayName("Should always use EACCES error code")
    void shouldAlwaysUseEaccesErrorCode() {
      final String[] messages = {
          "Permission denied",
          "Access denied",
          "Insufficient permissions",
          "Cannot write to read-only file"
      };

      for (String message : messages) {
        final WasiPermissionException exception = new WasiPermissionException(message);
        assertEquals("EACCES", exception.getWasiErrorCode(),
            "Error code should always be EACCES for: " + message);
      }
    }
  }

  @Nested
  @DisplayName("Constructor(String, Throwable) Tests")
  class ConstructorWithCauseTests {

    @Test
    @DisplayName("Should create exception with message, EACCES error code, and cause")
    void shouldCreateExceptionWithMessageEaccesErrorCodeAndCause() {
      final Throwable cause = new SecurityException("Underlying security error");
      final WasiPermissionException exception = new WasiPermissionException(
          "Permission denied", cause);

      assertEquals("Permission denied", exception.getMessage(), "Message should match");
      assertEquals("EACCES", exception.getWasiErrorCode(), "Error code should be EACCES");
      assertSame(cause, exception.getCause(), "Cause should match");
    }

    @Test
    @DisplayName("Should handle null cause")
    void shouldHandleNullCause() {
      final WasiPermissionException exception = new WasiPermissionException(
          "Permission denied", null);

      assertEquals("EACCES", exception.getWasiErrorCode(), "Error code should be EACCES");
      assertEquals(null, exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("Should preserve exception chain")
    void shouldPreserveExceptionChain() {
      final Exception root = new SecurityException("Root cause");
      final RuntimeException middle = new RuntimeException("Middle", root);
      final WasiPermissionException exception = new WasiPermissionException(
          "Top level", middle);

      assertSame(middle, exception.getCause(), "Direct cause should match");
      assertSame(root, exception.getCause().getCause(), "Root cause should be preserved");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include class name and message")
    void toStringShouldIncludeClassNameAndMessage() {
      final WasiPermissionException exception = new WasiPermissionException(
          "Access denied to /protected/file");
      final String str = exception.toString();

      assertTrue(str.contains("WasiPermissionException"), "Should contain class name");
      assertTrue(str.contains("Access denied to /protected/file"), "Should contain message");
    }

    @Test
    @DisplayName("toString should format correctly")
    void toStringShouldFormatCorrectly() {
      final WasiPermissionException exception = new WasiPermissionException(
          "Permission denied");
      final String expected = "WasiPermissionException: Permission denied";

      assertEquals(expected, exception.toString(), "toString should format correctly");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should be throwable and catchable")
    void shouldBeThrowableAndCatchable() {
      try {
        throw new WasiPermissionException("Access denied");
      } catch (WasiPermissionException e) {
        assertEquals("EACCES", e.getWasiErrorCode(), "Should catch with EACCES error code");
        assertEquals("Access denied", e.getMessage(), "Should catch with correct message");
      }
    }

    @Test
    @DisplayName("Should be catchable as WasiFileSystemException")
    void shouldBeCatchableAsWasiFileSystemException() {
      try {
        throw new WasiPermissionException("Permission denied");
      } catch (WasiFileSystemException e) {
        assertTrue(e instanceof WasiPermissionException,
            "Should be instance of WasiPermissionException");
        assertEquals("EACCES", e.getWasiErrorCode(), "Should have EACCES error code");
      }
    }

    @Test
    @DisplayName("Should be catchable as PanamaException")
    void shouldBeCatchableAsPanamaException() {
      try {
        throw new WasiPermissionException("Permission denied");
      } catch (PanamaException e) {
        assertTrue(e instanceof WasiPermissionException,
            "Should be instance of WasiPermissionException");
      }
    }

    @Test
    @DisplayName("Should be catchable as RuntimeException")
    void shouldBeCatchableAsRuntimeException() {
      try {
        throw new WasiPermissionException("Permission denied");
      } catch (RuntimeException e) {
        assertTrue(e instanceof WasiPermissionException,
            "Should be instance of WasiPermissionException");
      }
    }

    @Test
    @DisplayName("Typical usage pattern for permission checks")
    void typicalUsagePatternForPermissionChecks() {
      final String resourcePath = "/protected/secret.txt";
      final WasiPermissionException exception;

      try {
        // Simulate permission check failure
        throw new SecurityException("No access to: " + resourcePath);
      } catch (SecurityException e) {
        exception = new WasiPermissionException(
            "Permission denied for: " + resourcePath, e);
      }

      assertNotNull(exception, "Exception should be created");
      assertEquals("EACCES", exception.getWasiErrorCode(), "Should have EACCES error code");
      assertTrue(exception.getMessage().contains(resourcePath),
          "Message should contain resource path");
      assertNotNull(exception.getCause(), "Cause should be present");
      assertTrue(exception.getCause() instanceof SecurityException,
          "Cause should be SecurityException");
    }

    @Test
    @DisplayName("Should distinguish from parent WasiFileSystemException")
    void shouldDistinguishFromParentWasiFileSystemException() {
      final WasiPermissionException permException = new WasiPermissionException(
          "Permission error");
      final WasiFileSystemException fsException = new WasiFileSystemException(
          "File system error", "ENOENT");

      // Both are WasiFileSystemException
      assertTrue(permException instanceof WasiFileSystemException);
      assertTrue(fsException instanceof WasiFileSystemException);

      // Only permException is WasiPermissionException
      assertTrue(permException instanceof WasiPermissionException);
      assertTrue(!(fsException instanceof WasiPermissionException));

      // Different error codes
      assertEquals("EACCES", permException.getWasiErrorCode());
      assertEquals("ENOENT", fsException.getWasiErrorCode());
    }
  }
}
