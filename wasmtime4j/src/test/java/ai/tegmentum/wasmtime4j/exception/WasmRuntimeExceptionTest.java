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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasmRuntimeException} class.
 *
 * <p>This test class verifies the construction and behavior of WebAssembly runtime exceptions.
 */
@DisplayName("WasmRuntimeException Tests")
class WasmRuntimeExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasmRuntimeException should extend WasmException")
    void shouldExtendWasmException() {
      assertTrue(
          WasmException.class.isAssignableFrom(WasmRuntimeException.class),
          "WasmRuntimeException should extend WasmException");
    }

    @Test
    @DisplayName("WasmRuntimeException should be serializable")
    void shouldBeSerializable() {
      assertTrue(
          java.io.Serializable.class.isAssignableFrom(WasmRuntimeException.class),
          "WasmRuntimeException should be serializable");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with message should set message")
    void constructorWithMessageShouldSetMessage() {
      final WasmRuntimeException exception = new WasmRuntimeException("Runtime error");

      assertEquals("Runtime error", exception.getMessage(), "Message should be 'Runtime error'");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("Constructor with message and cause should set both")
    void constructorWithMessageAndCauseShouldSetBoth() {
      final Throwable cause = new Exception("Root cause");
      final WasmRuntimeException exception = new WasmRuntimeException("Runtime error", cause);

      assertEquals("Runtime error", exception.getMessage(), "Message should be 'Runtime error'");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }

    @Test
    @DisplayName("Constructor with cause only should set cause")
    void constructorWithCauseOnlyShouldSetCause() {
      final Throwable cause = new Exception("Root cause");
      final WasmRuntimeException exception = new WasmRuntimeException(cause);

      assertSame(cause, exception.getCause(), "Cause should be set");
      assertTrue(
          exception.getMessage().contains("Root cause"), "Message should contain cause message");
    }
  }

  @Nested
  @DisplayName("Usage Tests")
  class UsageTests {

    @Test
    @DisplayName("Should be throwable")
    void shouldBeThrowable() {
      final WasmRuntimeException exception = new WasmRuntimeException("Test");

      assertTrue(exception instanceof Throwable, "WasmRuntimeException should be throwable");
    }

    @Test
    @DisplayName("Should be catchable as WasmException")
    void shouldBeCatchableAsWasmException() {
      try {
        throw new WasmRuntimeException("Test error");
      } catch (WasmException e) {
        assertEquals("Test error", e.getMessage(), "Should be catchable as WasmException");
      }
    }

    @Test
    @DisplayName("Should preserve stack trace")
    void shouldPreserveStackTrace() {
      final WasmRuntimeException exception = new WasmRuntimeException("Test");

      assertTrue(exception.getStackTrace().length > 0, "Should have stack trace elements");
    }
  }
}
