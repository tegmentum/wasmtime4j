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

import ai.tegmentum.wasmtime4j.panama.exception.PanamaException;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WasiFileSystemException.
 *
 * <p>These tests exercise actual code execution to improve JaCoCo coverage.
 */
@DisplayName("WASI File System Exception Integration Tests")
public class WasiFileSystemExceptionIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(WasiFileSystemExceptionIntegrationTest.class.getName());

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create exception with message and error code")
    void shouldCreateExceptionWithMessageAndErrorCode() {
      LOGGER.info("Testing basic constructor");

      final WasiFileSystemException ex = new WasiFileSystemException("File not found", "ENOENT");

      assertNotNull(ex, "Exception should not be null");
      assertEquals("File not found", ex.getMessage(), "Message should match");
      assertEquals("ENOENT", ex.getWasiErrorCode(), "Error code should be ENOENT");
      assertNull(ex.getCause(), "Cause should be null");

      LOGGER.info("Created exception: " + ex);
    }

    @Test
    @DisplayName("Should create exception with message, error code, and cause")
    void shouldCreateExceptionWithMessageErrorCodeAndCause() {
      LOGGER.info("Testing constructor with cause");

      final IOException rootCause = new IOException("Underlying I/O error");
      final WasiFileSystemException ex =
          new WasiFileSystemException("Failed to read file", "EIO", rootCause);

      assertNotNull(ex, "Exception should not be null");
      assertEquals("Failed to read file", ex.getMessage(), "Message should match");
      assertEquals("EIO", ex.getWasiErrorCode(), "Error code should be EIO");
      assertSame(rootCause, ex.getCause(), "Cause should be the root cause");

      LOGGER.info("Created exception with cause: " + ex);
    }

    @Test
    @DisplayName("Should create exception with null message")
    void shouldCreateExceptionWithNullMessage() {
      LOGGER.info("Testing null message");

      final WasiFileSystemException ex = new WasiFileSystemException(null, "EINVAL");

      assertNull(ex.getMessage(), "Message should be null");
      assertEquals("EINVAL", ex.getWasiErrorCode(), "Error code should be EINVAL");

      LOGGER.info("Created exception with null message: " + ex);
    }

    @Test
    @DisplayName("Should create exception with empty message")
    void shouldCreateExceptionWithEmptyMessage() {
      LOGGER.info("Testing empty message");

      final WasiFileSystemException ex = new WasiFileSystemException("", "EACCES");

      assertEquals("", ex.getMessage(), "Message should be empty");
      assertEquals("EACCES", ex.getWasiErrorCode(), "Error code should be EACCES");

      LOGGER.info("Created exception with empty message");
    }

    @Test
    @DisplayName("Should create exception with null error code")
    void shouldCreateExceptionWithNullErrorCode() {
      LOGGER.info("Testing null error code");

      final WasiFileSystemException ex = new WasiFileSystemException("Unknown error", null);

      assertEquals("Unknown error", ex.getMessage(), "Message should match");
      assertNull(ex.getWasiErrorCode(), "Error code should be null");

      LOGGER.info("Created exception with null error code");
    }
  }

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("Should extend PanamaException")
    void shouldExtendPanamaException() {
      LOGGER.info("Testing inheritance");

      final WasiFileSystemException ex = new WasiFileSystemException("Test error", "ENOENT");

      assertTrue(ex instanceof PanamaException, "Should extend PanamaException");
      assertTrue(ex instanceof Exception, "Should be an Exception");
      assertTrue(ex instanceof Throwable, "Should be a Throwable");

      LOGGER.info("Inheritance verified");
    }

    @Test
    @DisplayName("Should be throwable")
    void shouldBeThrowable() {
      LOGGER.info("Testing throwability");

      try {
        throw new WasiFileSystemException("Test throw", "EAGAIN");
      } catch (final WasiFileSystemException caught) {
        assertEquals("Test throw", caught.getMessage(), "Caught exception message should match");
        assertEquals(
            "EAGAIN", caught.getWasiErrorCode(), "Caught exception error code should match");
        LOGGER.info("Successfully caught exception: " + caught);
      }
    }

    @Test
    @DisplayName("Should be catchable as Exception")
    void shouldBeCatchableAsException() {
      LOGGER.info("Testing catchable as Exception");

      try {
        throw new WasiFileSystemException("Exception catch test", "ETEST");
      } catch (final Exception caught) {
        assertTrue(caught instanceof WasiFileSystemException, "Should be WasiFileSystemException");
        LOGGER.info("Successfully caught as Exception: " + caught);
      }
    }
  }

  @Nested
  @DisplayName("Error Code Tests")
  class ErrorCodeTests {

    @Test
    @DisplayName("Should return correct error code for common WASI errors")
    void shouldReturnCorrectErrorCodeForCommonErrors() {
      LOGGER.info("Testing common error codes");

      final String[] errorCodes = {
        "ENOENT", "EACCES", "EEXIST", "EINVAL", "EIO",
        "ENOTDIR", "EISDIR", "ENOTEMPTY", "EBADF", "ENOMEM"
      };

      for (final String errorCode : errorCodes) {
        final WasiFileSystemException ex = new WasiFileSystemException("Error", errorCode);
        assertEquals(errorCode, ex.getWasiErrorCode(), "Error code should match: " + errorCode);
      }

      LOGGER.info("Tested " + errorCodes.length + " common error codes");
    }

    @Test
    @DisplayName("Should preserve error code through cause chain")
    void shouldPreserveErrorCodeThroughCauseChain() {
      LOGGER.info("Testing error code in cause chain");

      final Throwable level1 = new RuntimeException("Level 1");
      final Throwable level2 = new RuntimeException("Level 2", level1);
      final WasiFileSystemException ex = new WasiFileSystemException("Top level", "EPERM", level2);

      assertEquals("EPERM", ex.getWasiErrorCode(), "Error code should be preserved");
      assertSame(level2, ex.getCause(), "Direct cause should be level2");
      assertSame(level1, ex.getCause().getCause(), "Root cause should be level1");

      LOGGER.info("Error code preserved through cause chain");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("Should produce formatted string")
    void shouldProduceFormattedString() {
      LOGGER.info("Testing toString");

      final WasiFileSystemException ex = new WasiFileSystemException("Permission denied", "EACCES");

      final String str = ex.toString();

      assertNotNull(str, "toString should not be null");
      assertTrue(str.contains("WasiFileSystemException"), "Should contain class name: " + str);
      assertTrue(str.contains("EACCES"), "Should contain error code: " + str);
      assertTrue(str.contains("Permission denied"), "Should contain message: " + str);

      LOGGER.info("toString: " + str);
    }

    @Test
    @DisplayName("Should handle null message in toString")
    void shouldHandleNullMessageInToString() {
      LOGGER.info("Testing toString with null message");

      final WasiFileSystemException ex = new WasiFileSystemException(null, "ENULL");

      final String str = ex.toString();

      assertNotNull(str, "toString should not be null even with null message");
      assertTrue(str.contains("ENULL"), "Should contain error code: " + str);

      LOGGER.info("toString with null message: " + str);
    }

    @Test
    @DisplayName("Should handle null error code in toString")
    void shouldHandleNullErrorCodeInToString() {
      LOGGER.info("Testing toString with null error code");

      final WasiFileSystemException ex = new WasiFileSystemException("Some error", null);

      final String str = ex.toString();

      assertNotNull(str, "toString should not be null even with null error code");
      assertTrue(
          str.contains("null") || str.contains("Some error"),
          "Should contain message or null indicator: " + str);

      LOGGER.info("toString with null error code: " + str);
    }
  }

  @Nested
  @DisplayName("Stack Trace Tests")
  class StackTraceTests {

    @Test
    @DisplayName("Should capture stack trace")
    void shouldCaptureStackTrace() {
      LOGGER.info("Testing stack trace capture");

      final WasiFileSystemException ex = new WasiFileSystemException("Stack trace test", "ETRACE");

      final StackTraceElement[] trace = ex.getStackTrace();

      assertNotNull(trace, "Stack trace should not be null");
      assertTrue(trace.length > 0, "Stack trace should not be empty");
      assertTrue(
          trace[0].getMethodName().contains("shouldCaptureStackTrace")
              || trace[0].getClassName().contains("WasiFileSystemExceptionIntegrationTest"),
          "Stack trace should start in test method");

      LOGGER.info("Stack trace has " + trace.length + " elements");
    }

    @Test
    @DisplayName("Should allow modifying stack trace")
    void shouldAllowModifyingStackTrace() {
      LOGGER.info("Testing stack trace modification");

      final WasiFileSystemException ex = new WasiFileSystemException("Modify test", "EMOD");

      final StackTraceElement[] newTrace =
          new StackTraceElement[] {
            new StackTraceElement("TestClass", "testMethod", "TestClass.java", 42)
          };
      ex.setStackTrace(newTrace);

      final StackTraceElement[] result = ex.getStackTrace();
      assertEquals(1, result.length, "Stack trace should have 1 element");
      assertEquals("TestClass", result[0].getClassName(), "Class name should be TestClass");

      LOGGER.info("Stack trace modification verified");
    }
  }

  @Nested
  @DisplayName("Chained Exception Tests")
  class ChainedExceptionTests {

    @Test
    @DisplayName("Should support deep exception chaining")
    void shouldSupportDeepExceptionChaining() {
      LOGGER.info("Testing deep exception chaining");

      Throwable current = new IOException("Root I/O error");
      for (int i = 0; i < 5; i++) {
        current = new RuntimeException("Level " + i, current);
      }

      final WasiFileSystemException ex =
          new WasiFileSystemException("Top level WASI error", "ECHAIN", current);

      // Count chain depth
      int depth = 0;
      Throwable t = ex;
      while (t != null) {
        depth++;
        t = t.getCause();
      }

      assertEquals(7, depth, "Chain should have 7 elements (1 WASI + 5 Runtime + 1 IO)");

      LOGGER.info("Deep chaining verified with depth: " + depth);
    }

    @Test
    @DisplayName("Should handle self-referential cause gracefully")
    void shouldHandleSelfReferentialCauseGracefully() {
      LOGGER.info("Testing exception creation (cannot create true self-reference)");

      // Note: Java's Throwable.initCause() prevents self-referential causes
      // This test verifies we can create exceptions without issues
      final WasiFileSystemException ex1 = new WasiFileSystemException("Error 1", "E1");
      final WasiFileSystemException ex2 = new WasiFileSystemException("Error 2", "E2", ex1);

      assertSame(ex1, ex2.getCause(), "ex2's cause should be ex1");
      assertNull(ex1.getCause(), "ex1 should have no cause");

      LOGGER.info("Chained exceptions verified");
    }
  }

  /** Simple IOException for testing. */
  private static class IOException extends Exception {
    private static final long serialVersionUID = 1L;

    IOException(final String message) {
      super(message);
    }
  }
}
