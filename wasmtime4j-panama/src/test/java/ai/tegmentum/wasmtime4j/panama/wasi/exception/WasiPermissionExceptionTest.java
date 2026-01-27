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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WasiPermissionException.
 *
 * <p>These tests exercise actual code execution to improve JaCoCo coverage.
 */
@DisplayName("WASI Permission Exception Integration Tests")
public class WasiPermissionExceptionTest {

  private static final Logger LOGGER =
      Logger.getLogger(WasiPermissionExceptionTest.class.getName());

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create exception with message only")
    void shouldCreateExceptionWithMessageOnly() {
      LOGGER.info("Testing message-only constructor");

      final WasiPermissionException ex =
          new WasiPermissionException("Access denied to /etc/passwd");

      assertNotNull(ex, "Exception should not be null");
      assertEquals("Access denied to /etc/passwd", ex.getMessage(), "Message should match");
      assertEquals("EACCES", ex.getWasiErrorCode(), "Error code should always be EACCES");
      assertNull(ex.getCause(), "Cause should be null");

      LOGGER.info("Created exception: " + ex);
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      LOGGER.info("Testing constructor with cause");

      final SecurityException rootCause = new SecurityException("Security manager denied access");
      final WasiPermissionException ex =
          new WasiPermissionException("Cannot access protected resource", rootCause);

      assertNotNull(ex, "Exception should not be null");
      assertEquals("Cannot access protected resource", ex.getMessage(), "Message should match");
      assertEquals("EACCES", ex.getWasiErrorCode(), "Error code should always be EACCES");
      assertSame(rootCause, ex.getCause(), "Cause should be the root cause");

      LOGGER.info("Created exception with cause: " + ex);
    }

    @Test
    @DisplayName("Should create exception with null message")
    void shouldCreateExceptionWithNullMessage() {
      LOGGER.info("Testing null message");

      final WasiPermissionException ex = new WasiPermissionException(null);

      assertNull(ex.getMessage(), "Message should be null");
      assertEquals(
          "EACCES", ex.getWasiErrorCode(), "Error code should be EACCES regardless of message");

      LOGGER.info("Created exception with null message");
    }

    @Test
    @DisplayName("Should create exception with empty message")
    void shouldCreateExceptionWithEmptyMessage() {
      LOGGER.info("Testing empty message");

      final WasiPermissionException ex = new WasiPermissionException("");

      assertEquals("", ex.getMessage(), "Message should be empty");
      assertEquals(
          "EACCES", ex.getWasiErrorCode(), "Error code should be EACCES regardless of message");

      LOGGER.info("Created exception with empty message");
    }

    @Test
    @DisplayName("Should create exception with null cause")
    void shouldCreateExceptionWithNullCause() {
      LOGGER.info("Testing null cause");

      final WasiPermissionException ex = new WasiPermissionException("Permission error", null);

      assertEquals("Permission error", ex.getMessage(), "Message should match");
      assertEquals("EACCES", ex.getWasiErrorCode(), "Error code should be EACCES");
      assertNull(ex.getCause(), "Cause should be null");

      LOGGER.info("Created exception with null cause");
    }
  }

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("Should extend WasiFileSystemException")
    void shouldExtendWasiFileSystemException() {
      LOGGER.info("Testing inheritance");

      final WasiPermissionException ex = new WasiPermissionException("Test permission error");

      assertTrue(ex instanceof WasiFileSystemException, "Should extend WasiFileSystemException");
      assertTrue(ex instanceof Exception, "Should be an Exception");
      assertTrue(ex instanceof Throwable, "Should be a Throwable");

      LOGGER.info("Inheritance verified");
    }

    @Test
    @DisplayName("Should be catchable as WasiFileSystemException")
    void shouldBeCatchableAsWasiFileSystemException() {
      LOGGER.info("Testing polymorphic catching");

      try {
        throw new WasiPermissionException("Caught as parent");
      } catch (final WasiFileSystemException caught) {
        assertTrue(caught instanceof WasiPermissionException, "Should be WasiPermissionException");
        assertEquals("EACCES", caught.getWasiErrorCode(), "Error code should be EACCES");
        LOGGER.info("Successfully caught as WasiFileSystemException: " + caught);
      }
    }

    @Test
    @DisplayName("Should be catchable as Exception")
    void shouldBeCatchableAsException() {
      LOGGER.info("Testing catchable as Exception");

      try {
        throw new WasiPermissionException("Exception catch test");
      } catch (final Exception caught) {
        assertTrue(caught instanceof WasiPermissionException, "Should be WasiPermissionException");
        LOGGER.info("Successfully caught as Exception: " + caught);
      }
    }
  }

  @Nested
  @DisplayName("Error Code Tests")
  class ErrorCodeTests {

    @Test
    @DisplayName("Should always return EACCES error code")
    void shouldAlwaysReturnEaccesErrorCode() {
      LOGGER.info("Testing EACCES error code consistency");

      final WasiPermissionException ex1 = new WasiPermissionException("Error 1");
      final WasiPermissionException ex2 = new WasiPermissionException("Error 2", new Exception());
      final WasiPermissionException ex3 = new WasiPermissionException(null);

      assertEquals("EACCES", ex1.getWasiErrorCode(), "ex1 should have EACCES");
      assertEquals("EACCES", ex2.getWasiErrorCode(), "ex2 should have EACCES");
      assertEquals("EACCES", ex3.getWasiErrorCode(), "ex3 should have EACCES");

      LOGGER.info("All instances have EACCES error code");
    }

    @Test
    @DisplayName("Should preserve EACCES even through cause chain")
    void shouldPreserveEaccesThroughCauseChain() {
      LOGGER.info("Testing EACCES preservation in cause chain");

      final Throwable level1 = new RuntimeException("Level 1");
      final Throwable level2 = new RuntimeException("Level 2", level1);
      final WasiPermissionException ex = new WasiPermissionException("Top level", level2);

      assertEquals("EACCES", ex.getWasiErrorCode(), "Error code should remain EACCES");
      assertSame(level2, ex.getCause(), "Direct cause should be level2");

      LOGGER.info("EACCES preserved through cause chain");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("Should produce formatted string")
    void shouldProduceFormattedString() {
      LOGGER.info("Testing toString");

      final WasiPermissionException ex = new WasiPermissionException("Read access denied");

      final String str = ex.toString();

      assertNotNull(str, "toString should not be null");
      assertTrue(str.contains("WasiPermissionException"), "Should contain class name: " + str);
      assertTrue(str.contains("Read access denied"), "Should contain message: " + str);

      LOGGER.info("toString: " + str);
    }

    @Test
    @DisplayName("Should handle null message in toString")
    void shouldHandleNullMessageInToString() {
      LOGGER.info("Testing toString with null message");

      final WasiPermissionException ex = new WasiPermissionException(null);

      final String str = ex.toString();

      assertNotNull(str, "toString should not be null even with null message");
      assertTrue(str.contains("WasiPermissionException"), "Should contain class name: " + str);

      LOGGER.info("toString with null message: " + str);
    }
  }

  @Nested
  @DisplayName("Common Permission Scenarios")
  class CommonPermissionScenarios {

    @Test
    @DisplayName("Should represent file read permission denied")
    void shouldRepresentFileReadPermissionDenied() {
      LOGGER.info("Testing file read permission scenario");

      final WasiPermissionException ex =
          new WasiPermissionException("Cannot read file: /etc/shadow");

      assertEquals("EACCES", ex.getWasiErrorCode(), "Should be EACCES");
      assertTrue(ex.getMessage().contains("/etc/shadow"), "Message should contain file path");

      LOGGER.info("File read permission scenario: " + ex);
    }

    @Test
    @DisplayName("Should represent file write permission denied")
    void shouldRepresentFileWritePermissionDenied() {
      LOGGER.info("Testing file write permission scenario");

      final WasiPermissionException ex =
          new WasiPermissionException("Cannot write to file: /usr/bin/test");

      assertEquals("EACCES", ex.getWasiErrorCode(), "Should be EACCES");
      assertTrue(ex.getMessage().contains("write"), "Message should mention write operation");

      LOGGER.info("File write permission scenario: " + ex);
    }

    @Test
    @DisplayName("Should represent directory access permission denied")
    void shouldRepresentDirectoryAccessPermissionDenied() {
      LOGGER.info("Testing directory access permission scenario");

      final WasiPermissionException ex =
          new WasiPermissionException("Cannot access directory: /root");

      assertEquals("EACCES", ex.getWasiErrorCode(), "Should be EACCES");
      assertTrue(ex.getMessage().contains("directory"), "Message should mention directory");

      LOGGER.info("Directory access permission scenario: " + ex);
    }

    @Test
    @DisplayName("Should represent execute permission denied")
    void shouldRepresentExecutePermissionDenied() {
      LOGGER.info("Testing execute permission scenario");

      final WasiPermissionException ex =
          new WasiPermissionException("Cannot execute: /usr/local/bin/script.sh");

      assertEquals("EACCES", ex.getWasiErrorCode(), "Should be EACCES");
      assertTrue(ex.getMessage().contains("execute"), "Message should mention execute");

      LOGGER.info("Execute permission scenario: " + ex);
    }
  }

  @Nested
  @DisplayName("Multiple Exception Tests")
  class MultipleExceptionTests {

    @Test
    @DisplayName("Should create multiple independent exceptions")
    void shouldCreateMultipleIndependentExceptions() {
      LOGGER.info("Testing multiple independent exceptions");

      final WasiPermissionException ex1 = new WasiPermissionException("First permission error");
      final WasiPermissionException ex2 = new WasiPermissionException("Second permission error");
      final WasiPermissionException ex3 = new WasiPermissionException("Third permission error");

      // Verify independence
      assertEquals("First permission error", ex1.getMessage());
      assertEquals("Second permission error", ex2.getMessage());
      assertEquals("Third permission error", ex3.getMessage());

      // All should have EACCES
      assertEquals("EACCES", ex1.getWasiErrorCode());
      assertEquals("EACCES", ex2.getWasiErrorCode());
      assertEquals("EACCES", ex3.getWasiErrorCode());

      LOGGER.info("Created 3 independent exceptions");
    }
  }
}
