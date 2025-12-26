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

import ai.tegmentum.wasmtime4j.exception.DebugException.DebugErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link DebugException} class.
 *
 * <p>This test class verifies the construction and behavior of debugging exceptions, including
 * error types and debug context.
 */
@DisplayName("DebugException Tests")
class DebugExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("DebugException should extend WasmException")
    void shouldExtendWasmException() {
      assertTrue(
          WasmException.class.isAssignableFrom(DebugException.class),
          "DebugException should extend WasmException");
    }

    @Test
    @DisplayName("DebugException should be serializable")
    void shouldBeSerializable() {
      assertTrue(
          java.io.Serializable.class.isAssignableFrom(DebugException.class),
          "DebugException should be serializable");
    }
  }

  @Nested
  @DisplayName("DebugErrorType Enum Tests")
  class DebugErrorTypeEnumTests {

    @Test
    @DisplayName("Should have UNKNOWN value")
    void shouldHaveUnknownValue() {
      assertNotNull(DebugErrorType.valueOf("UNKNOWN"), "Should have UNKNOWN value");
    }

    @Test
    @DisplayName("Should have BREAKPOINT_ERROR value")
    void shouldHaveBreakpointErrorValue() {
      assertNotNull(
          DebugErrorType.valueOf("BREAKPOINT_ERROR"), "Should have BREAKPOINT_ERROR value");
    }

    @Test
    @DisplayName("Should have STEP_ERROR value")
    void shouldHaveStepErrorValue() {
      assertNotNull(DebugErrorType.valueOf("STEP_ERROR"), "Should have STEP_ERROR value");
    }

    @Test
    @DisplayName("Should have VARIABLE_INSPECTION_ERROR value")
    void shouldHaveVariableInspectionErrorValue() {
      assertNotNull(
          DebugErrorType.valueOf("VARIABLE_INSPECTION_ERROR"),
          "Should have VARIABLE_INSPECTION_ERROR value");
    }

    @Test
    @DisplayName("Should have STACK_FRAME_ERROR value")
    void shouldHaveStackFrameErrorValue() {
      assertNotNull(
          DebugErrorType.valueOf("STACK_FRAME_ERROR"), "Should have STACK_FRAME_ERROR value");
    }

    @Test
    @DisplayName("Should have SESSION_ERROR value")
    void shouldHaveSessionErrorValue() {
      assertNotNull(DebugErrorType.valueOf("SESSION_ERROR"), "Should have SESSION_ERROR value");
    }

    @Test
    @DisplayName("Should have SOURCE_MAPPING_ERROR value")
    void shouldHaveSourceMappingErrorValue() {
      assertNotNull(
          DebugErrorType.valueOf("SOURCE_MAPPING_ERROR"), "Should have SOURCE_MAPPING_ERROR value");
    }

    @Test
    @DisplayName("Should have SYMBOL_ERROR value")
    void shouldHaveSymbolErrorValue() {
      assertNotNull(DebugErrorType.valueOf("SYMBOL_ERROR"), "Should have SYMBOL_ERROR value");
    }

    @Test
    @DisplayName("Should have WATCH_ERROR value")
    void shouldHaveWatchErrorValue() {
      assertNotNull(DebugErrorType.valueOf("WATCH_ERROR"), "Should have WATCH_ERROR value");
    }

    @Test
    @DisplayName("Should have CONNECTION_ERROR value")
    void shouldHaveConnectionErrorValue() {
      assertNotNull(
          DebugErrorType.valueOf("CONNECTION_ERROR"), "Should have CONNECTION_ERROR value");
    }

    @Test
    @DisplayName("Should have INVALID_STATE value")
    void shouldHaveInvalidStateValue() {
      assertNotNull(DebugErrorType.valueOf("INVALID_STATE"), "Should have INVALID_STATE value");
    }

    @Test
    @DisplayName("Should have PROTOCOL_ERROR value")
    void shouldHaveProtocolErrorValue() {
      assertNotNull(DebugErrorType.valueOf("PROTOCOL_ERROR"), "Should have PROTOCOL_ERROR value");
    }

    @Test
    @DisplayName("Should have NOT_SUPPORTED value")
    void shouldHaveNotSupportedValue() {
      assertNotNull(DebugErrorType.valueOf("NOT_SUPPORTED"), "Should have NOT_SUPPORTED value");
    }

    @Test
    @DisplayName("Should have TIMEOUT value")
    void shouldHaveTimeoutValue() {
      assertNotNull(DebugErrorType.valueOf("TIMEOUT"), "Should have TIMEOUT value");
    }

    @Test
    @DisplayName("Should have 14 error types")
    void shouldHave14ErrorTypes() {
      assertEquals(14, DebugErrorType.values().length, "Should have 14 error types");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with message should set defaults")
    void constructorWithMessageShouldSetDefaults() {
      final DebugException exception = new DebugException("Debug error");

      assertEquals("Debug error", exception.getMessage(), "Message should be 'Debug error'");
      assertEquals(
          DebugErrorType.UNKNOWN, exception.getErrorType(), "Error type should default to UNKNOWN");
      assertNull(exception.getDebugContext(), "Debug context should be null");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("Constructor with message and cause should set both")
    void constructorWithMessageAndCauseShouldSetBoth() {
      final Throwable cause = new Exception("Root cause");
      final DebugException exception = new DebugException("Debug error", cause);

      assertEquals("Debug error", exception.getMessage(), "Message should be 'Debug error'");
      assertSame(cause, exception.getCause(), "Cause should be set");
      assertEquals(
          DebugErrorType.UNKNOWN, exception.getErrorType(), "Error type should default to UNKNOWN");
    }

    @Test
    @DisplayName("Constructor with message and error type should set both")
    void constructorWithMessageAndErrorTypeShouldSetBoth() {
      final DebugException exception =
          new DebugException("Breakpoint error", DebugErrorType.BREAKPOINT_ERROR);

      assertEquals(
          "Breakpoint error", exception.getMessage(), "Message should be 'Breakpoint error'");
      assertEquals(
          DebugErrorType.BREAKPOINT_ERROR,
          exception.getErrorType(),
          "Error type should be BREAKPOINT_ERROR");
    }

    @Test
    @DisplayName("Full constructor should set all fields")
    void fullConstructorShouldSetAllFields() {
      final Throwable cause = new Exception("Root cause");
      final DebugException exception =
          new DebugException(
              "Step error", cause, DebugErrorType.STEP_ERROR, "function: my_func, line: 42");

      assertEquals("Step error", exception.getMessage(), "Message should match");
      assertEquals(
          DebugErrorType.STEP_ERROR, exception.getErrorType(), "Error type should be STEP_ERROR");
      assertEquals(
          "function: my_func, line: 42", exception.getDebugContext(), "Debug context should match");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }

    @Test
    @DisplayName("Constructor should handle null error type")
    void constructorShouldHandleNullErrorType() {
      final DebugException exception = new DebugException("Error message", null, null, null);

      assertEquals(
          DebugErrorType.UNKNOWN,
          exception.getErrorType(),
          "Null error type should default to UNKNOWN");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("getErrorType should return error type")
    void getErrorTypeShouldReturnErrorType() {
      final DebugException exception = new DebugException("Error", DebugErrorType.TIMEOUT);

      assertEquals(
          DebugErrorType.TIMEOUT, exception.getErrorType(), "getErrorType should return TIMEOUT");
    }

    @Test
    @DisplayName("getDebugContext should return debug context")
    void getDebugContextShouldReturnDebugContext() {
      final DebugException exception =
          new DebugException("Error", null, DebugErrorType.UNKNOWN, "context info");

      assertEquals(
          "context info",
          exception.getDebugContext(),
          "getDebugContext should return 'context info'");
    }

    @Test
    @DisplayName("hasDebugContext should return true when context present")
    void hasDebugContextShouldReturnTrueWhenContextPresent() {
      final DebugException exception =
          new DebugException("Error", null, DebugErrorType.UNKNOWN, "some context");

      assertTrue(exception.hasDebugContext(), "hasDebugContext should return true");
    }

    @Test
    @DisplayName("hasDebugContext should return false when context null")
    void hasDebugContextShouldReturnFalseWhenContextNull() {
      final DebugException exception = new DebugException("Error");

      assertFalse(exception.hasDebugContext(), "hasDebugContext should return false for null");
    }

    @Test
    @DisplayName("hasDebugContext should return false when context empty")
    void hasDebugContextShouldReturnFalseWhenContextEmpty() {
      final DebugException exception =
          new DebugException("Error", null, DebugErrorType.UNKNOWN, "");

      assertFalse(exception.hasDebugContext(), "hasDebugContext should return false for empty");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include error type")
    void toStringShouldIncludeErrorType() {
      final DebugException exception = new DebugException("Error", DebugErrorType.BREAKPOINT_ERROR);

      final String result = exception.toString();

      assertTrue(result.contains("BREAKPOINT_ERROR"), "toString should contain error type");
    }

    @Test
    @DisplayName("toString should include message")
    void toStringShouldIncludeMessage() {
      final DebugException exception = new DebugException("My debug error", DebugErrorType.UNKNOWN);

      final String result = exception.toString();

      assertTrue(result.contains("My debug error"), "toString should contain message");
    }

    @Test
    @DisplayName("toString should include context when present")
    void toStringShouldIncludeContextWhenPresent() {
      final DebugException exception =
          new DebugException("Error", null, DebugErrorType.UNKNOWN, "context data");

      final String result = exception.toString();

      assertTrue(result.contains("context data"), "toString should contain context");
    }

    @Test
    @DisplayName("toString should include class name")
    void toStringShouldIncludeClassName() {
      final DebugException exception = new DebugException("Error");

      final String result = exception.toString();

      assertTrue(result.contains("DebugException"), "toString should contain class name");
    }
  }

  @Nested
  @DisplayName("Usage Tests")
  class UsageTests {

    @Test
    @DisplayName("Should be throwable")
    void shouldBeThrowable() {
      final DebugException exception = new DebugException("Test");

      assertTrue(exception instanceof Throwable, "DebugException should be throwable");
    }

    @Test
    @DisplayName("Should be catchable as WasmException")
    void shouldBeCatchableAsWasmException() {
      try {
        throw new DebugException("Test error");
      } catch (WasmException e) {
        assertEquals("Test error", e.getMessage(), "Should be catchable as WasmException");
      }
    }
  }
}
