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
 * Tests for the {@link WitMarshallingException} class.
 *
 * <p>This test class verifies the construction and behavior of WIT marshalling exceptions,
 * including factory methods for common marshalling errors.
 */
@DisplayName("WitMarshallingException Tests")
class WitMarshallingExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WitMarshallingException should extend WitValueException")
    void shouldExtendWitValueException() {
      assertTrue(
          WitValueException.class.isAssignableFrom(WitMarshallingException.class),
          "WitMarshallingException should extend WitValueException");
    }

    @Test
    @DisplayName("WitMarshallingException should be serializable")
    void shouldBeSerializable() {
      assertTrue(
          java.io.Serializable.class.isAssignableFrom(WitMarshallingException.class),
          "WitMarshallingException should be serializable");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with message should set error code to MARSHALLING_ERROR")
    void constructorWithMessageShouldSetMarshallingErrorCode() {
      final WitMarshallingException exception = new WitMarshallingException("Marshalling failed");

      assertTrue(
          exception.getMessage().contains("Marshalling failed"),
          "Message should contain error text");
      assertEquals(
          ErrorCode.MARSHALLING_ERROR,
          exception.getCode(),
          "Error code should be MARSHALLING_ERROR");
    }

    @Test
    @DisplayName("Constructor with message and cause should set both")
    void constructorWithMessageAndCauseShouldSetBoth() {
      final Throwable cause = new RuntimeException("Root cause");
      final WitMarshallingException exception = new WitMarshallingException("Error message", cause);

      assertTrue(
          exception.getMessage().contains("Error message"), "Message should contain error text");
      assertSame(cause, exception.getCause(), "Cause should be set");
      assertEquals(
          ErrorCode.MARSHALLING_ERROR,
          exception.getCode(),
          "Error code should be MARSHALLING_ERROR");
    }

    @Test
    @DisplayName("Constructor with type information should set all fields")
    void constructorWithTypeInformationShouldSetAllFields() {
      final WitType type = WitType.list(WitType.createU8());
      final WitMarshallingException exception =
          new WitMarshallingException("Cannot marshal", type, new int[] {1, 2, 3});

      assertEquals(
          ErrorCode.MARSHALLING_ERROR,
          exception.getCode(),
          "Error code should be MARSHALLING_ERROR");
      assertTrue(exception.getExpectedType().isPresent(), "Expected type should be present");
      assertTrue(exception.getActualValue().isPresent(), "Actual value should be present");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("nullValue should create exception for null values")
    void nullValueShouldCreateExceptionForNullValues() {
      final WitType type = WitType.createS32();
      final WitMarshallingException exception = WitMarshallingException.nullValue(type);

      assertTrue(
          exception.getMessage().toLowerCase().contains("null"), "Message should mention null");
      assertTrue(
          exception.getMessage().toLowerCase().contains("optional")
              || exception.getMessage().toLowerCase().contains("non"),
          "Message should indicate non-optional requirement");
      assertTrue(exception.getExpectedType().isPresent(), "Expected type should be present");
    }

    @Test
    @DisplayName("typeMismatch should create exception for type mismatches")
    void typeMismatchShouldCreateExceptionForTypeMismatches() {
      final WitType type = WitType.createFloat64();
      final WitMarshallingException exception =
          WitMarshallingException.typeMismatch(type, "not a number");

      assertTrue(
          exception.getMessage().contains("String"), "Message should contain actual type name");
      assertTrue(
          exception.getMessage().toLowerCase().contains("marshal"),
          "Message should mention marshalling");
      assertTrue(exception.getExpectedType().isPresent(), "Expected type should be present");
      assertTrue(exception.getActualValue().isPresent(), "Actual value should be present");
    }

    @Test
    @DisplayName("typeMismatch should handle null actual value")
    void typeMismatchShouldHandleNullActualValue() {
      final WitType type = WitType.createBool();
      final WitMarshallingException exception = WitMarshallingException.typeMismatch(type, null);

      assertTrue(
          exception.getMessage().toLowerCase().contains("null"), "Message should mention null");
    }

    @Test
    @DisplayName("allocationFailure should create exception with cause")
    void allocationFailureShouldCreateExceptionWithCause() {
      final WitType type = WitType.createString();
      final Throwable cause = new OutOfMemoryError("No memory");
      final WitMarshallingException exception =
          WitMarshallingException.allocationFailure(type, cause);

      assertTrue(
          exception.getMessage().toLowerCase().contains("allocat"),
          "Message should mention allocation");
      assertTrue(
          exception.getMessage().toLowerCase().contains("memory"), "Message should mention memory");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }

    @Test
    @DisplayName("readFailure should create exception with cause")
    void readFailureShouldCreateExceptionWithCause() {
      final WitType type = WitType.createU64();
      final Throwable cause = new IllegalStateException("Read error");
      final WitMarshallingException exception = WitMarshallingException.readFailure(type, cause);

      assertTrue(
          exception.getMessage().toLowerCase().contains("read"), "Message should mention reading");
      assertTrue(
          exception.getMessage().toLowerCase().contains("memory")
              || exception.getMessage().toLowerCase().contains("native"),
          "Message should mention memory or native");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }
  }

  @Nested
  @DisplayName("Error Code Tests")
  class ErrorCodeTests {

    @Test
    @DisplayName("All factory methods should produce MARSHALLING_ERROR code")
    void allFactoryMethodsShouldProduceMarshallingErrorCode() {
      final WitType type = WitType.createS32();
      final WitMarshallingException e1 = WitMarshallingException.nullValue(type);
      final WitMarshallingException e2 = WitMarshallingException.typeMismatch(type, "str");
      final WitMarshallingException e3 =
          WitMarshallingException.allocationFailure(type, new Exception());
      final WitMarshallingException e4 = WitMarshallingException.readFailure(type, new Exception());

      assertEquals(
          ErrorCode.MARSHALLING_ERROR, e1.getCode(), "nullValue should use MARSHALLING_ERROR");
      assertEquals(
          ErrorCode.MARSHALLING_ERROR, e2.getCode(), "typeMismatch should use MARSHALLING_ERROR");
      assertEquals(
          ErrorCode.MARSHALLING_ERROR,
          e3.getCode(),
          "allocationFailure should use MARSHALLING_ERROR");
      assertEquals(
          ErrorCode.MARSHALLING_ERROR, e4.getCode(), "readFailure should use MARSHALLING_ERROR");
    }
  }

  @Nested
  @DisplayName("Usage Tests")
  class UsageTests {

    @Test
    @DisplayName("Should be throwable")
    void shouldBeThrowable() {
      final WitMarshallingException exception = new WitMarshallingException("Test");

      assertTrue(exception instanceof Throwable, "WitMarshallingException should be throwable");
    }

    @Test
    @DisplayName("Should be catchable as WitValueException")
    void shouldBeCatchableAsWitValueException() {
      try {
        throw new WitMarshallingException("Test error");
      } catch (WitValueException e) {
        assertTrue(
            e.getMessage().contains("Test error"), "Should be catchable as WitValueException");
      }
    }

    @Test
    @DisplayName("Should be catchable as WasmException")
    void shouldBeCatchableAsWasmException() {
      try {
        throw new WitMarshallingException("Test error");
      } catch (WasmException e) {
        assertNotNull(e, "Should be catchable as WasmException");
      }
    }
  }
}
