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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link JniException}. */
class JniExceptionTest {

  @Test
  void testConstructorWithMessage() {
    final String message = "Test error message";
    final JniException exception = new JniException(message);

    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getCause()).isNull();
    assertThat(exception.getNativeErrorCode()).isNull();
    assertFalse(exception.hasNativeErrorCode());
  }

  @Test
  void testConstructorWithMessageAndCause() {
    final String message = "Test error message";
    final RuntimeException cause = new RuntimeException("Root cause");
    final JniException exception = new JniException(message, cause);

    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getCause()).isEqualTo(cause);
    assertThat(exception.getNativeErrorCode()).isNull();
    assertFalse(exception.hasNativeErrorCode());
  }

  @Test
  void testConstructorWithMessageAndNativeErrorCode() {
    final String message = "Test error message";
    final int errorCode = 42;
    final JniException exception = new JniException(message, errorCode);

    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getCause()).isNull();
    assertThat(exception.getNativeErrorCode()).isEqualTo(errorCode);
    assertTrue(exception.hasNativeErrorCode());
  }

  @Test
  void testConstructorWithAllParameters() {
    final String message = "Test error message";
    final RuntimeException cause = new RuntimeException("Root cause");
    final int errorCode = 123;
    final JniException exception = new JniException(message, cause, errorCode);

    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getCause()).isEqualTo(cause);
    assertThat(exception.getNativeErrorCode()).isEqualTo(errorCode);
    assertTrue(exception.hasNativeErrorCode());
  }

  @Test
  void testToStringWithoutNativeErrorCode() {
    final String message = "Test error message";
    final JniException exception = new JniException(message);
    final String toString = exception.toString();

    assertThat(toString).contains(message);
    assertThat(toString).contains("JniException");
    assertThat(toString).doesNotContain("native error code");
  }

  @Test
  void testToStringWithNativeErrorCode() {
    final String message = "Test error message";
    final int errorCode = 42;
    final JniException exception = new JniException(message, errorCode);
    final String toString = exception.toString();

    assertThat(toString).contains(message);
    assertThat(toString).contains("JniException");
    assertThat(toString).contains("native error code: 42");
  }

  @Test
  void testInheritanceFromWasmException() {
    final JniException exception = new JniException("test");
    assertThat(exception).isInstanceOf(ai.tegmentum.wasmtime4j.exception.WasmException.class);
  }

  @Test
  void testSerialVersionUid() {
    // Ensure serialVersionUID is defined for serialization compatibility
    assertThat(JniException.class.getDeclaredFields())
        .anyMatch(field -> field.getName().equals("serialVersionUID"));
  }
}
