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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmExceptionHandlingException.ExceptionPayload;
import ai.tegmentum.wasmtime4j.exception.WasmExceptionHandlingException.ExceptionTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasmExceptionHandlingException} class.
 *
 * <p>This test class verifies the construction and behavior of WebAssembly exception handling
 * exceptions, including the inner ExceptionTag and ExceptionPayload classes.
 */
@DisplayName("WasmExceptionHandlingException Tests")
class WasmExceptionHandlingExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasmExceptionHandlingException should extend WasmtimeException")
    void shouldExtendWasmtimeException() {
      assertTrue(
          WasmtimeException.class.isAssignableFrom(WasmExceptionHandlingException.class),
          "WasmExceptionHandlingException should extend WasmtimeException");
    }

    @Test
    @DisplayName("WasmExceptionHandlingException should be serializable")
    void shouldBeSerializable() {
      assertTrue(
          java.io.Serializable.class.isAssignableFrom(WasmExceptionHandlingException.class),
          "WasmExceptionHandlingException should be serializable");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with message should set message")
    void constructorWithMessageShouldSetMessage() {
      final WasmExceptionHandlingException exception =
          new WasmExceptionHandlingException("Exception handling error");

      assertEquals(
          "Exception handling error",
          exception.getMessage(),
          "Message should be 'Exception handling error'");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("Constructor with message and cause should set both")
    void constructorWithMessageAndCauseShouldSetBoth() {
      final Throwable cause = new Exception("Root cause");
      final WasmExceptionHandlingException exception =
          new WasmExceptionHandlingException("Error message", cause);

      assertEquals("Error message", exception.getMessage(), "Message should be 'Error message'");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }

    @Test
    @DisplayName("Constructor with cause only should set cause and message")
    void constructorWithCauseOnlyShouldSetCause() {
      final Throwable cause = new Exception("Root cause");
      final WasmExceptionHandlingException exception = new WasmExceptionHandlingException(cause);

      assertSame(cause, exception.getCause(), "Cause should be set");
      assertEquals("Root cause", exception.getMessage(), "Message should come from cause");
    }
  }

  @Nested
  @DisplayName("ExceptionTag Inner Class Tests")
  class ExceptionTagTests {

    @Test
    @DisplayName("ExceptionTag should store tag name")
    void exceptionTagShouldStoreTagName() {
      final ExceptionTag tag = new ExceptionTag("my_exception", 1, "(i32) -> ()");

      assertEquals("my_exception", tag.getTagName(), "Tag name should be 'my_exception'");
    }

    @Test
    @DisplayName("ExceptionTag should store tag ID")
    void exceptionTagShouldStoreTagId() {
      final ExceptionTag tag = new ExceptionTag("my_exception", 42, "(i32) -> ()");

      assertEquals(42, tag.getTagId(), "Tag ID should be 42");
    }

    @Test
    @DisplayName("ExceptionTag should store signature")
    void exceptionTagShouldStoreSignature() {
      final ExceptionTag tag = new ExceptionTag("my_exception", 1, "(i32, i64) -> ()");

      assertEquals(
          "(i32, i64) -> ()", tag.getSignature(), "Signature should be '(i32, i64) -> ()'");
    }

    @Test
    @DisplayName("ExceptionTag should handle null values")
    void exceptionTagShouldHandleNullValues() {
      final ExceptionTag tag = new ExceptionTag(null, 0, null);

      assertNull(tag.getTagName(), "Tag name should be null");
      assertEquals(0, tag.getTagId(), "Tag ID should be 0");
      assertNull(tag.getSignature(), "Signature should be null");
    }

    @Test
    @DisplayName("ExceptionTag should handle negative tag ID")
    void exceptionTagShouldHandleNegativeTagId() {
      final ExceptionTag tag = new ExceptionTag("tag", -1, "sig");

      assertEquals(-1, tag.getTagId(), "Tag ID should be -1");
    }
  }

  @Nested
  @DisplayName("ExceptionPayload Inner Class Tests")
  class ExceptionPayloadTests {

    @Test
    @DisplayName("ExceptionPayload should store payload data")
    void exceptionPayloadShouldStorePayloadData() {
      final byte[] data = {0x01, 0x02, 0x03, 0x04};
      final ExceptionPayload payload = new ExceptionPayload(data, "i32");

      assertArrayEquals(data, payload.getPayloadData(), "Payload data should match");
    }

    @Test
    @DisplayName("ExceptionPayload should return defensive copy of data")
    void exceptionPayloadShouldReturnDefensiveCopy() {
      final byte[] data = {0x01, 0x02, 0x03, 0x04};
      final ExceptionPayload payload = new ExceptionPayload(data, "i32");

      final byte[] retrieved = payload.getPayloadData();
      assertNotSame(data, retrieved, "Should return a copy, not the original");

      // Modify original to verify defensive copy was made
      data[0] = 0x00;
      assertEquals(
          0x01, payload.getPayloadData()[0], "Original modification should not affect payload");
    }

    @Test
    @DisplayName("ExceptionPayload should store payload type")
    void exceptionPayloadShouldStorePayloadType() {
      final ExceptionPayload payload = new ExceptionPayload(new byte[4], "f64");

      assertEquals("f64", payload.getPayloadType(), "Payload type should be 'f64'");
    }

    @Test
    @DisplayName("ExceptionPayload should return correct size")
    void exceptionPayloadShouldReturnCorrectSize() {
      final byte[] data = {0x01, 0x02, 0x03, 0x04, 0x05};
      final ExceptionPayload payload = new ExceptionPayload(data, "bytes");

      assertEquals(5, payload.getPayloadSize(), "Payload size should be 5");
    }

    @Test
    @DisplayName("ExceptionPayload should handle null data")
    void exceptionPayloadShouldHandleNullData() {
      final ExceptionPayload payload = new ExceptionPayload(null, "void");

      assertNotNull(payload.getPayloadData(), "Payload data should not be null");
      assertEquals(0, payload.getPayloadSize(), "Payload size should be 0 for null input");
    }

    @Test
    @DisplayName("ExceptionPayload should handle empty data")
    void exceptionPayloadShouldHandleEmptyData() {
      final ExceptionPayload payload = new ExceptionPayload(new byte[0], "empty");

      assertEquals(0, payload.getPayloadSize(), "Payload size should be 0");
      assertNotNull(payload.getPayloadData(), "Payload data should not be null");
    }

    @Test
    @DisplayName("ExceptionPayload should handle null type")
    void exceptionPayloadShouldHandleNullType() {
      final ExceptionPayload payload = new ExceptionPayload(new byte[4], null);

      assertNull(payload.getPayloadType(), "Payload type should be null");
    }
  }

  @Nested
  @DisplayName("Usage Tests")
  class UsageTests {

    @Test
    @DisplayName("Should be throwable")
    void shouldBeThrowable() {
      final WasmExceptionHandlingException exception = new WasmExceptionHandlingException("Test");

      assertTrue(
          exception instanceof Throwable, "WasmExceptionHandlingException should be throwable");
    }

    @Test
    @DisplayName("Should be catchable as WasmtimeException")
    void shouldBeCatchableAsWasmtimeException() {
      try {
        throw new WasmExceptionHandlingException("Test error");
      } catch (WasmtimeException e) {
        assertEquals("Test error", e.getMessage(), "Should be catchable as WasmtimeException");
      }
    }

    @Test
    @DisplayName("Should preserve stack trace")
    void shouldPreserveStackTrace() {
      final WasmExceptionHandlingException exception = new WasmExceptionHandlingException("Test");

      assertTrue(exception.getStackTrace().length > 0, "Should have stack trace elements");
    }
  }
}
