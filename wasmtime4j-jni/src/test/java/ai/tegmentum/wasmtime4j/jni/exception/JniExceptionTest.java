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
package ai.tegmentum.wasmtime4j.jni.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link JniException}. */
class JniExceptionTest {

  @Test
  void testConstructorWithMessage() {
    final String message = "Test error message";
    final JniException exception = new JniException(message);

    assertEquals(message, exception.getMessage());
    assertNull(exception.getCause());
    assertNull(exception.getNativeErrorCode());
    assertFalse(exception.hasNativeErrorCode());
  }

  @Test
  void testConstructorWithMessageAndCause() {
    final String message = "Test error message";
    final RuntimeException cause = new RuntimeException("Root cause");
    final JniException exception = new JniException(message, cause);

    assertEquals(message, exception.getMessage());
    assertEquals(cause, exception.getCause());
    assertNull(exception.getNativeErrorCode());
    assertFalse(exception.hasNativeErrorCode());
  }

  @Test
  void testConstructorWithMessageAndNativeErrorCode() {
    final String message = "Test error message";
    final int errorCode = 42;
    final JniException exception = new JniException(message, errorCode);

    assertEquals(message, exception.getMessage());
    assertNull(exception.getCause());
    assertEquals(errorCode, exception.getNativeErrorCode());
    assertTrue(exception.hasNativeErrorCode());
  }

  @Test
  void testConstructorWithAllParameters() {
    final String message = "Test error message";
    final RuntimeException cause = new RuntimeException("Root cause");
    final int errorCode = 123;
    final JniException exception = new JniException(message, cause, errorCode);

    assertEquals(message, exception.getMessage());
    assertEquals(cause, exception.getCause());
    assertEquals(errorCode, exception.getNativeErrorCode());
    assertTrue(exception.hasNativeErrorCode());
  }

  @Test
  void testToStringWithoutNativeErrorCode() {
    final String message = "Test error message";
    final JniException exception = new JniException(message);
    final String toString = exception.toString();

    assertTrue(toString.contains(message), "Expected string to contain: " + message);
    assertTrue(toString.contains("JniException"), "Expected string to contain: JniException");
    assertFalse(
        toString.contains("native error code"),
        "Expected string not to contain: native error code");
  }

  @Test
  void testToStringWithNativeErrorCode() {
    final String message = "Test error message";
    final int errorCode = 42;
    final JniException exception = new JniException(message, errorCode);
    final String toString = exception.toString();

    assertTrue(toString.contains(message), "Expected string to contain: " + message);
    assertTrue(toString.contains("JniException"), "Expected string to contain: JniException");
    assertTrue(
        toString.contains("native error code: 42"),
        "Expected string to contain: native error code: 42");
  }

  @Test
  void testInheritanceFromWasmException() {
    final JniException exception = new JniException("test");
    assertInstanceOf(ai.tegmentum.wasmtime4j.exception.WasmException.class, exception);
  }

  @Test
  void testSerialVersionUid() {
    // Ensure serialVersionUID is defined for serialization compatibility
    assertTrue(
        Arrays.stream(JniException.class.getDeclaredFields())
            .anyMatch(field -> field.getName().equals("serialVersionUID")),
        "Expected serialVersionUID field to be defined");
  }
}
