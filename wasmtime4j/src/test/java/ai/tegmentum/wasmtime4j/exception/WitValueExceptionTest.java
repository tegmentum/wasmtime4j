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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WitValueException.ErrorCode;
import ai.tegmentum.wasmtime4j.wit.WitType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WitValueException} class.
 *
 * <p>This test class verifies the construction and behavior of WIT value exceptions, including
 * error codes and type information.
 */
@DisplayName("WitValueException Tests")
class WitValueExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WitValueException should extend WasmException")
    void shouldExtendWasmException() {
      assertTrue(
          WasmException.class.isAssignableFrom(WitValueException.class),
          "WitValueException should extend WasmException");
    }

    @Test
    @DisplayName("WitValueException should be serializable")
    void shouldBeSerializable() {
      assertTrue(
          java.io.Serializable.class.isAssignableFrom(WitValueException.class),
          "WitValueException should be serializable");
    }
  }

  @Nested
  @DisplayName("ErrorCode Enum Tests")
  class ErrorCodeEnumTests {

    @Test
    @DisplayName("Should have TYPE_MISMATCH value")
    void shouldHaveTypeMismatchValue() {
      assertNotNull(ErrorCode.valueOf("TYPE_MISMATCH"), "Should have TYPE_MISMATCH value");
    }

    @Test
    @DisplayName("Should have RANGE_ERROR value")
    void shouldHaveRangeErrorValue() {
      assertNotNull(ErrorCode.valueOf("RANGE_ERROR"), "Should have RANGE_ERROR value");
    }

    @Test
    @DisplayName("Should have INVALID_FORMAT value")
    void shouldHaveInvalidFormatValue() {
      assertNotNull(ErrorCode.valueOf("INVALID_FORMAT"), "Should have INVALID_FORMAT value");
    }

    @Test
    @DisplayName("Should have MARSHALLING_ERROR value")
    void shouldHaveMarshallingErrorValue() {
      assertNotNull(ErrorCode.valueOf("MARSHALLING_ERROR"), "Should have MARSHALLING_ERROR value");
    }

    @Test
    @DisplayName("Should have VALIDATION_ERROR value")
    void shouldHaveValidationErrorValue() {
      assertNotNull(ErrorCode.valueOf("VALIDATION_ERROR"), "Should have VALIDATION_ERROR value");
    }

    @Test
    @DisplayName("Should have NULL_VALUE value")
    void shouldHaveNullValueValue() {
      assertNotNull(ErrorCode.valueOf("NULL_VALUE"), "Should have NULL_VALUE value");
    }

    @Test
    @DisplayName("Should have UNSUPPORTED_OPERATION value")
    void shouldHaveUnsupportedOperationValue() {
      assertNotNull(
          ErrorCode.valueOf("UNSUPPORTED_OPERATION"), "Should have UNSUPPORTED_OPERATION value");
    }

    @Test
    @DisplayName("Should have 7 error codes")
    void shouldHave7ErrorCodes() {
      assertEquals(7, ErrorCode.values().length, "Should have 7 error codes");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with message and code should set fields")
    void constructorWithMessageAndCodeShouldSetFields() {
      final WitValueException exception =
          new WitValueException("Value error", ErrorCode.TYPE_MISMATCH);

      assertTrue(
          exception.getMessage().contains("Value error"), "Message should contain error text");
      assertEquals(
          ErrorCode.TYPE_MISMATCH, exception.getCode(), "Error code should be TYPE_MISMATCH");
      assertFalse(exception.getExpectedType().isPresent(), "Expected type should be empty");
      assertFalse(exception.getActualValue().isPresent(), "Actual value should be empty");
    }

    @Test
    @DisplayName("Constructor with message, code, and cause should set all")
    void constructorWithMessageCodeAndCauseShouldSetAll() {
      final Throwable cause = new RuntimeException("Root cause");
      final WitValueException exception =
          new WitValueException("Error message", ErrorCode.RANGE_ERROR, cause);

      assertTrue(
          exception.getMessage().contains("Error message"), "Message should contain error text");
      assertEquals(ErrorCode.RANGE_ERROR, exception.getCode(), "Error code should be RANGE_ERROR");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }

    @Test
    @DisplayName("Full constructor should set all fields")
    void fullConstructorShouldSetAllFields() {
      final WitType expectedType = WitType.createS32();
      final WitValueException exception =
          new WitValueException("Type error", ErrorCode.TYPE_MISMATCH, expectedType, "not an int");

      assertEquals(ErrorCode.TYPE_MISMATCH, exception.getCode(), "Error code should match");
      assertTrue(exception.getExpectedType().isPresent(), "Expected type should be present");
      assertTrue(exception.getActualValue().isPresent(), "Actual value should be present");
      assertEquals("not an int", exception.getActualValue().get(), "Actual value should match");
    }

    @Test
    @DisplayName("Constructor should handle null expected type")
    void constructorShouldHandleNullExpectedType() {
      final WitValueException exception =
          new WitValueException("Error", ErrorCode.VALIDATION_ERROR, null, "value");

      assertFalse(
          exception.getExpectedType().isPresent(), "Expected type should be empty for null");
    }

    @Test
    @DisplayName("Constructor should handle null actual value")
    void constructorShouldHandleNullActualValue() {
      final WitValueException exception =
          new WitValueException("Error", ErrorCode.NULL_VALUE, WitType.createS32(), null);

      assertFalse(exception.getActualValue().isPresent(), "Actual value should be empty for null");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("getCode should return error code")
    void getCodeShouldReturnErrorCode() {
      final WitValueException exception =
          new WitValueException("Error", ErrorCode.MARSHALLING_ERROR);

      assertEquals(
          ErrorCode.MARSHALLING_ERROR,
          exception.getCode(),
          "getCode should return MARSHALLING_ERROR");
    }

    @Test
    @DisplayName("getExpectedType should return expected type when set")
    void getExpectedTypeShouldReturnExpectedTypeWhenSet() {
      final WitType type = WitType.createU64();
      final WitValueException exception =
          new WitValueException("Error", ErrorCode.TYPE_MISMATCH, type, 42);

      assertTrue(exception.getExpectedType().isPresent(), "Expected type should be present");
      assertSame(type, exception.getExpectedType().get(), "Expected type should match");
    }

    @Test
    @DisplayName("getActualValue should return actual value when set")
    void getActualValueShouldReturnActualValueWhenSet() {
      final WitValueException exception =
          new WitValueException("Error", ErrorCode.RANGE_ERROR, WitType.createS32(), 999999);

      assertTrue(exception.getActualValue().isPresent(), "Actual value should be present");
      assertEquals(999999, exception.getActualValue().get(), "Actual value should match");
    }
  }

  @Nested
  @DisplayName("getMessage Tests")
  class GetMessageTests {

    @Test
    @DisplayName("getMessage should include base message")
    void getMessageShouldIncludeBaseMessage() {
      final WitValueException exception =
          new WitValueException("Base error", ErrorCode.VALIDATION_ERROR);

      assertTrue(
          exception.getMessage().contains("Base error"), "getMessage should include base message");
    }

    @Test
    @DisplayName("getMessage should include error code")
    void getMessageShouldIncludeErrorCode() {
      final WitValueException exception = new WitValueException("Error", ErrorCode.INVALID_FORMAT);

      assertTrue(
          exception.getMessage().contains("INVALID_FORMAT"),
          "getMessage should include error code");
    }

    @Test
    @DisplayName("getMessage should include expected type when set")
    void getMessageShouldIncludeExpectedTypeWhenSet() {
      final WitType type = WitType.createFloat32();
      final WitValueException exception =
          new WitValueException("Error", ErrorCode.TYPE_MISMATCH, type, "bad");

      assertTrue(
          exception.getMessage().contains("Expected type"),
          "getMessage should include expected type label");
    }

    @Test
    @DisplayName("getMessage should include actual value when set")
    void getMessageShouldIncludeActualValueWhenSet() {
      final WitValueException exception =
          new WitValueException("Error", ErrorCode.RANGE_ERROR, WitType.createS8(), 256);

      assertTrue(
          exception.getMessage().contains("Actual value"),
          "getMessage should include actual value label");
      assertTrue(exception.getMessage().contains("256"), "getMessage should include actual value");
    }
  }

  @Nested
  @DisplayName("Usage Tests")
  class UsageTests {

    @Test
    @DisplayName("Should be throwable")
    void shouldBeThrowable() {
      final WitValueException exception = new WitValueException("Test", ErrorCode.TYPE_MISMATCH);

      assertTrue(exception instanceof Throwable, "WitValueException should be throwable");
    }

    @Test
    @DisplayName("Should be catchable as WasmException")
    void shouldBeCatchableAsWasmException() {
      try {
        throw new WitValueException("Test error", ErrorCode.RANGE_ERROR);
      } catch (WasmException e) {
        assertTrue(e.getMessage().contains("Test error"), "Should be catchable as WasmException");
      }
    }
  }
}
