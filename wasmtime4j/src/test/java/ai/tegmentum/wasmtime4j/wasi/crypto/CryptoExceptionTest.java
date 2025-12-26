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

package ai.tegmentum.wasmtime4j.wasi.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CryptoException} class.
 *
 * <p>CryptoException is thrown when cryptographic operations fail in WASI-crypto.
 */
@DisplayName("CryptoException Tests")
class CryptoExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should extend WasmException")
    void shouldExtendWasmException() {
      assertTrue(
          WasmException.class.isAssignableFrom(CryptoException.class),
          "CryptoException should extend WasmException");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(CryptoException.class.getModifiers()),
          "CryptoException should be public");
    }

    @Test
    @DisplayName("should have constructor with message only")
    void shouldHaveConstructorWithMessageOnly() throws NoSuchMethodException {
      final Constructor<?> constructor = CryptoException.class.getConstructor(String.class);
      assertNotNull(constructor, "Constructor with message should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with message and error code")
    void shouldHaveConstructorWithMessageAndErrorCode() throws NoSuchMethodException {
      final Constructor<?> constructor =
          CryptoException.class.getConstructor(String.class, CryptoErrorCode.class);
      assertNotNull(constructor, "Constructor with message and error code should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with message and cause")
    void shouldHaveConstructorWithMessageAndCause() throws NoSuchMethodException {
      final Constructor<?> constructor =
          CryptoException.class.getConstructor(String.class, Throwable.class);
      assertNotNull(constructor, "Constructor with message and cause should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with message, error code, and cause")
    void shouldHaveConstructorWithMessageErrorCodeAndCause() throws NoSuchMethodException {
      final Constructor<?> constructor =
          CryptoException.class.getConstructor(
              String.class, CryptoErrorCode.class, Throwable.class);
      assertNotNull(constructor, "Constructor with all parameters should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getErrorCode method")
    void shouldHaveGetErrorCodeMethod() throws NoSuchMethodException {
      final Method method = CryptoException.class.getMethod("getErrorCode");
      assertNotNull(method, "getErrorCode method should exist");
      assertEquals(
          CryptoErrorCode.class,
          method.getReturnType(),
          "getErrorCode should return CryptoErrorCode");
    }
  }

  @Nested
  @DisplayName("Instance Creation Tests")
  class InstanceCreationTests {

    @Test
    @DisplayName("should create instance with message only")
    void shouldCreateInstanceWithMessageOnly() {
      final CryptoException exception = new CryptoException("Test error");

      assertEquals("Test error", exception.getMessage(), "Message should match");
      assertEquals(
          CryptoErrorCode.UNKNOWN,
          exception.getErrorCode(),
          "Default error code should be UNKNOWN");
    }

    @Test
    @DisplayName("should create instance with message and error code")
    void shouldCreateInstanceWithMessageAndErrorCode() {
      final CryptoException exception =
          new CryptoException("Invalid key", CryptoErrorCode.INVALID_KEY);

      assertEquals("Invalid key", exception.getMessage(), "Message should match");
      assertEquals(
          CryptoErrorCode.INVALID_KEY, exception.getErrorCode(), "Error code should match");
    }

    @Test
    @DisplayName("should create instance with message and cause")
    void shouldCreateInstanceWithMessageAndCause() {
      final RuntimeException cause = new RuntimeException("Underlying error");
      final CryptoException exception = new CryptoException("Crypto failed", cause);

      assertEquals("Crypto failed", exception.getMessage(), "Message should match");
      assertEquals(cause, exception.getCause(), "Cause should match");
      assertEquals(
          CryptoErrorCode.UNKNOWN,
          exception.getErrorCode(),
          "Default error code should be UNKNOWN");
    }

    @Test
    @DisplayName("should create instance with all parameters")
    void shouldCreateInstanceWithAllParameters() {
      final RuntimeException cause = new RuntimeException("Underlying error");
      final CryptoException exception =
          new CryptoException("Decryption failed", CryptoErrorCode.DECRYPTION_FAILED, cause);

      assertEquals("Decryption failed", exception.getMessage(), "Message should match");
      assertEquals(
          CryptoErrorCode.DECRYPTION_FAILED, exception.getErrorCode(), "Error code should match");
      assertEquals(cause, exception.getCause(), "Cause should match");
    }

    @Test
    @DisplayName("should be throwable")
    void shouldBeThrowable() {
      final CryptoException exception = new CryptoException("Test error");

      assertTrue(exception instanceof Throwable, "CryptoException should be throwable");
    }
  }
}
