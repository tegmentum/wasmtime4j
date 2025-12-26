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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WitType;
import ai.tegmentum.wasmtime4j.exception.WitValueException.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WitValidationException} class.
 *
 * <p>This test class verifies the construction and behavior of WIT validation exceptions, including
 * factory methods for common validation errors.
 */
@DisplayName("WitValidationException Tests")
class WitValidationExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WitValidationException should extend WitValueException")
    void shouldExtendWitValueException() {
      assertTrue(
          WitValueException.class.isAssignableFrom(WitValidationException.class),
          "WitValidationException should extend WitValueException");
    }

    @Test
    @DisplayName("WitValidationException should be serializable")
    void shouldBeSerializable() {
      assertTrue(
          java.io.Serializable.class.isAssignableFrom(WitValidationException.class),
          "WitValidationException should be serializable");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with message should set error code to VALIDATION_ERROR")
    void constructorWithMessageShouldSetValidationErrorCode() {
      final WitValidationException exception = new WitValidationException("Validation failed");

      assertTrue(
          exception.getMessage().contains("Validation failed"),
          "Message should contain error text");
      assertEquals(
          ErrorCode.VALIDATION_ERROR, exception.getCode(), "Error code should be VALIDATION_ERROR");
    }

    @Test
    @DisplayName("Constructor with message and cause should set both")
    void constructorWithMessageAndCauseShouldSetBoth() {
      final Throwable cause = new RuntimeException("Root cause");
      final WitValidationException exception = new WitValidationException("Error message", cause);

      assertTrue(
          exception.getMessage().contains("Error message"), "Message should contain error text");
      assertSame(cause, exception.getCause(), "Cause should be set");
      assertEquals(
          ErrorCode.VALIDATION_ERROR, exception.getCode(), "Error code should be VALIDATION_ERROR");
    }

    @Test
    @DisplayName("Constructor with type information should set all fields")
    void constructorWithTypeInformationShouldSetAllFields() {
      final WitType type = WitType.createString();
      final WitValidationException exception =
          new WitValidationException("Invalid value", type, "bad value");

      assertEquals(
          ErrorCode.VALIDATION_ERROR, exception.getCode(), "Error code should be VALIDATION_ERROR");
      assertTrue(exception.getExpectedType().isPresent(), "Expected type should be present");
      assertTrue(exception.getActualValue().isPresent(), "Actual value should be present");
      assertEquals("bad value", exception.getActualValue().get(), "Actual value should match");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("invalidEncoding should create exception for invalid UTF-8")
    void invalidEncodingShouldCreateExceptionForInvalidUtf8() {
      // Using Character.toString to avoid Unicode escape in source (checkstyle)
      final String replacementChar = Character.toString((char) 0xFFFD);
      final WitValidationException exception =
          WitValidationException.invalidEncoding("invalid" + replacementChar + "string");

      assertTrue(
          exception.getMessage().toLowerCase().contains("encoding")
              || exception.getMessage().toLowerCase().contains("utf"),
          "Message should mention encoding or UTF-8");
      assertEquals(
          ErrorCode.VALIDATION_ERROR, exception.getCode(), "Error code should be VALIDATION_ERROR");
    }

    @Test
    @DisplayName("invalidChar should create exception for invalid codepoint")
    void invalidCharShouldCreateExceptionForInvalidCodepoint() {
      final WitValidationException exception = WitValidationException.invalidChar(0xD800);

      assertTrue(
          exception.getMessage().contains("D800") || exception.getMessage().contains("d800"),
          "Message should contain the codepoint");
      assertTrue(
          exception.getMessage().toLowerCase().contains("codepoint")
              || exception.getMessage().toLowerCase().contains("unicode")
              || exception.getMessage().toLowerCase().contains("char"),
          "Message should mention codepoint, unicode, or char");
    }

    @Test
    @DisplayName("constraintViolation should create exception for violated constraint")
    void constraintViolationShouldCreateExceptionForViolatedConstraint() {
      final WitValidationException exception =
          WitValidationException.constraintViolation("max_length=100", "a very long string...");

      assertTrue(
          exception.getMessage().contains("max_length=100"),
          "Message should contain the constraint");
      assertTrue(
          exception.getMessage().toLowerCase().contains("constraint")
              || exception.getMessage().toLowerCase().contains("violate"),
          "Message should mention constraint or violation");
    }

    @Test
    @DisplayName("invalidFormat should create exception for format errors")
    void invalidFormatShouldCreateExceptionForFormatErrors() {
      final WitValidationException exception =
          WitValidationException.invalidFormat("ISO-8601 date", "not-a-date");

      assertTrue(
          exception.getMessage().contains("ISO-8601"), "Message should contain expected format");
      assertTrue(
          exception.getMessage().contains("not-a-date"), "Message should contain the actual value");
      assertTrue(
          exception.getMessage().toLowerCase().contains("format"), "Message should mention format");
    }
  }

  @Nested
  @DisplayName("Error Code Tests")
  class ErrorCodeTests {

    @Test
    @DisplayName("All factory methods should produce VALIDATION_ERROR code")
    void allFactoryMethodsShouldProduceValidationErrorCode() {
      final WitValidationException e1 = WitValidationException.invalidEncoding("test");
      final WitValidationException e2 = WitValidationException.invalidChar(0xFFFF);
      final WitValidationException e3 =
          WitValidationException.constraintViolation("constraint", "value");
      final WitValidationException e4 = WitValidationException.invalidFormat("format", "value");

      assertEquals(
          ErrorCode.VALIDATION_ERROR, e1.getCode(), "invalidEncoding should use VALIDATION_ERROR");
      assertEquals(
          ErrorCode.VALIDATION_ERROR, e2.getCode(), "invalidChar should use VALIDATION_ERROR");
      assertEquals(
          ErrorCode.VALIDATION_ERROR,
          e3.getCode(),
          "constraintViolation should use VALIDATION_ERROR");
      assertEquals(
          ErrorCode.VALIDATION_ERROR, e4.getCode(), "invalidFormat should use VALIDATION_ERROR");
    }
  }

  @Nested
  @DisplayName("Usage Tests")
  class UsageTests {

    @Test
    @DisplayName("Should be throwable")
    void shouldBeThrowable() {
      final WitValidationException exception = new WitValidationException("Test");

      assertTrue(exception instanceof Throwable, "WitValidationException should be throwable");
    }

    @Test
    @DisplayName("Should be catchable as WitValueException")
    void shouldBeCatchableAsWitValueException() {
      try {
        throw new WitValidationException("Test error");
      } catch (WitValueException e) {
        assertTrue(
            e.getMessage().contains("Test error"), "Should be catchable as WitValueException");
      }
    }

    @Test
    @DisplayName("Should be catchable as WasmException")
    void shouldBeCatchableAsWasmException() {
      try {
        throw new WitValidationException("Test error");
      } catch (WasmException e) {
        assertNotNull(e, "Should be catchable as WasmException");
      }
    }

    @Test
    @DisplayName("Should preserve stack trace")
    void shouldPreserveStackTrace() {
      final WitValidationException exception = new WitValidationException("Test");

      assertTrue(exception.getStackTrace().length > 0, "Should have stack trace elements");
    }
  }
}
