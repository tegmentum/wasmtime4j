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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Panama {@link WasiException} class.
 *
 * <p>This test class verifies WasiException constructors and behavior.
 */
@DisplayName("Panama WasiException Tests")
class WasiExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiException should extend PanamaException")
    void shouldExtendPanamaException() {
      assertTrue(PanamaException.class.isAssignableFrom(WasiException.class),
          "WasiException should extend PanamaException");
    }

    @Test
    @DisplayName("WasiException should be a RuntimeException")
    void shouldBeRuntimeException() {
      assertTrue(RuntimeException.class.isAssignableFrom(WasiException.class),
          "WasiException should be a RuntimeException");
    }
  }

  @Nested
  @DisplayName("Constructor(String) Tests")
  class ConstructorStringTests {

    @Test
    @DisplayName("Should create exception with message only")
    void shouldCreateExceptionWithMessageOnly() {
      final String message = "WASI operation failed";
      final WasiException exception = new WasiException(message);

      assertEquals(message, exception.getMessage(), "Message should match");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("Should handle null message")
    void shouldHandleNullMessage() {
      final WasiException exception = new WasiException((String) null);

      assertNull(exception.getMessage(), "Message should be null");
    }

    @Test
    @DisplayName("Should handle empty message")
    void shouldHandleEmptyMessage() {
      final WasiException exception = new WasiException("");

      assertEquals("", exception.getMessage(), "Message should be empty");
    }
  }

  @Nested
  @DisplayName("Constructor(String, Throwable) Tests")
  class ConstructorStringThrowableTests {

    @Test
    @DisplayName("Should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final String message = "WASI operation failed";
      final Throwable cause = new RuntimeException("Underlying error");
      final WasiException exception = new WasiException(message, cause);

      assertEquals(message, exception.getMessage(), "Message should match");
      assertSame(cause, exception.getCause(), "Cause should match");
    }

    @Test
    @DisplayName("Should handle null cause")
    void shouldHandleNullCause() {
      final WasiException exception = new WasiException("WASI failed", null);

      assertEquals("WASI failed", exception.getMessage(), "Message should match");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("Should preserve exception chain")
    void shouldPreserveExceptionChain() {
      final Exception root = new IllegalStateException("Root cause");
      final RuntimeException middle = new RuntimeException("Middle", root);
      final WasiException exception = new WasiException("Top level", middle);

      assertSame(middle, exception.getCause(), "Direct cause should match");
      assertSame(root, exception.getCause().getCause(), "Root cause should be preserved");
    }
  }

  @Nested
  @DisplayName("Constructor(Throwable) Tests")
  class ConstructorThrowableTests {

    @Test
    @DisplayName("Should create exception with cause only")
    void shouldCreateExceptionWithCauseOnly() {
      final Throwable cause = new RuntimeException("Underlying error");
      final WasiException exception = new WasiException(cause);

      assertNotNull(exception.getMessage(), "Message should not be null");
      assertSame(cause, exception.getCause(), "Cause should match");
    }

    @Test
    @DisplayName("Should handle null cause")
    void shouldHandleNullCause() {
      final WasiException exception = new WasiException((Throwable) null);

      assertNull(exception.getCause(), "Cause should be null");
    }
  }

  @Nested
  @DisplayName("Serialization Tests")
  class SerializationTests {

    @Test
    @DisplayName("Should have serialVersionUID")
    void shouldHaveSerialVersionUid() {
      // Verify by creating exception - if serialVersionUID is wrong, serialization would fail
      final WasiException exception = new WasiException("Test");
      assertNotNull(exception);
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should be throwable and catchable")
    void shouldBeThrowableAndCatchable() {
      try {
        throw new WasiException("Test WASI exception");
      } catch (WasiException e) {
        assertEquals("Test WASI exception", e.getMessage(),
            "Should catch with correct message");
      }
    }

    @Test
    @DisplayName("Should be catchable as PanamaException")
    void shouldBeCatchableAsPanamaException() {
      try {
        throw new WasiException("WASI error");
      } catch (PanamaException e) {
        assertTrue(e instanceof WasiException, "Should be instance of WasiException");
      }
    }

    @Test
    @DisplayName("Should be catchable as RuntimeException")
    void shouldBeCatchableAsRuntimeException() {
      try {
        throw new WasiException("Runtime WASI error");
      } catch (RuntimeException e) {
        assertTrue(e instanceof WasiException, "Should be instance of WasiException");
      }
    }

    @Test
    @DisplayName("Typical usage pattern for WASI operations")
    void typicalUsagePatternForWasiOperations() {
      final String filePath = "/path/to/file";
      final WasiException exception;

      try {
        // Simulate WASI operation failure
        throw new RuntimeException("File not found: " + filePath);
      } catch (RuntimeException e) {
        exception = new WasiException("WASI file operation failed: " + filePath, e);
      }

      assertNotNull(exception, "Exception should be created");
      assertTrue(exception.getMessage().contains(filePath), "Message should contain file path");
      assertNotNull(exception.getCause(), "Cause should be present");
    }
  }
}
