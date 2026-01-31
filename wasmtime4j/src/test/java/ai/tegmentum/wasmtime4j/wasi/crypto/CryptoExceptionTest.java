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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
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
    @DisplayName("should be public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(CryptoException.class.getModifiers()),
          "CryptoException should be public");
    }

    @Test
    @DisplayName("should extend WasmException")
    void shouldExtendWasmException() {
      assertTrue(
          WasmException.class.isAssignableFrom(CryptoException.class),
          "CryptoException should extend WasmException");
    }

    @Test
    @DisplayName("should have serialVersionUID field")
    void shouldHaveSerialVersionUID() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          CryptoException.class.getDeclaredField("serialVersionUID");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "serialVersionUID should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "serialVersionUID should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "serialVersionUID should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create exception with message only")
    void shouldCreateExceptionWithMessageOnly() {
      final String message = "Crypto operation failed";
      final CryptoException exception = new CryptoException(message);

      assertEquals(message, exception.getMessage(), "Message should match the provided message");
      assertNull(exception.getCause(), "Cause should be null when not provided");
      assertEquals(
          CryptoErrorCode.UNKNOWN,
          exception.getErrorCode(),
          "Error code should default to UNKNOWN when not provided");
    }

    @Test
    @DisplayName("should create exception with message and error code")
    void shouldCreateExceptionWithMessageAndErrorCode() {
      final String message = "Invalid key length";
      final CryptoErrorCode errorCode = CryptoErrorCode.INVALID_KEY;
      final CryptoException exception = new CryptoException(message, errorCode);

      assertEquals(message, exception.getMessage(), "Message should match the provided message");
      assertNull(exception.getCause(), "Cause should be null when not provided");
      assertEquals(
          errorCode,
          exception.getErrorCode(),
          "Error code should match the provided error code");
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final String message = "Encryption failed";
      final RuntimeException cause = new RuntimeException("Low-level failure");
      final CryptoException exception = new CryptoException(message, cause);

      assertEquals(message, exception.getMessage(), "Message should match the provided message");
      assertSame(cause, exception.getCause(), "Cause should match the provided cause");
      assertEquals(
          CryptoErrorCode.UNKNOWN,
          exception.getErrorCode(),
          "Error code should default to UNKNOWN when not provided");
    }

    @Test
    @DisplayName("should create exception with message, error code, and cause")
    void shouldCreateExceptionWithMessageErrorCodeAndCause() {
      final String message = "Decryption failed";
      final CryptoErrorCode errorCode = CryptoErrorCode.DECRYPTION_FAILED;
      final RuntimeException cause = new RuntimeException("Bad padding");
      final CryptoException exception = new CryptoException(message, errorCode, cause);

      assertEquals(message, exception.getMessage(), "Message should match the provided message");
      assertEquals(
          errorCode,
          exception.getErrorCode(),
          "Error code should match the provided error code");
      assertSame(cause, exception.getCause(), "Cause should match the provided cause");
    }
  }

  @Nested
  @DisplayName("Error Code Tests")
  class ErrorCodeTests {

    @Test
    @DisplayName("should return correct error code via getErrorCode")
    void shouldReturnCorrectErrorCode() {
      final CryptoException exception =
          new CryptoException("Timeout", CryptoErrorCode.TIMEOUT);

      assertEquals(
          CryptoErrorCode.TIMEOUT,
          exception.getErrorCode(),
          "getErrorCode should return the error code set at construction");
    }

    @Test
    @DisplayName("should default to UNKNOWN error code")
    void shouldDefaultToUnknownErrorCode() {
      final CryptoException exception = new CryptoException("Unknown error");

      assertEquals(
          CryptoErrorCode.UNKNOWN,
          exception.getErrorCode(),
          "Error code should default to UNKNOWN for message-only constructor");
    }
  }

  @Nested
  @DisplayName("Exception Behavior Tests")
  class ExceptionBehaviorTests {

    @Test
    @DisplayName("should be catchable as WasmException")
    void shouldBeCatchableAsWasmException() {
      boolean caught = false;
      try {
        throw new CryptoException("Crypto error");
      } catch (final WasmException e) {
        caught = true;
        assertTrue(e instanceof CryptoException, "Should be instance of CryptoException");
      }
      assertTrue(caught, "Exception should be caught as WasmException");
    }

    @Test
    @DisplayName("should be catchable as Exception")
    void shouldBeCatchableAsException() {
      boolean caught = false;
      try {
        throw new CryptoException("Crypto error");
      } catch (final Exception e) {
        caught = true;
        assertTrue(e instanceof CryptoException, "Should be instance of CryptoException");
      }
      assertTrue(caught, "Exception should be caught as Exception");
    }

    @Test
    @DisplayName("should preserve stack trace")
    void shouldPreserveStackTrace() {
      final CryptoException exception = new CryptoException("Test");
      final StackTraceElement[] stackTrace = exception.getStackTrace();

      assertNotNull(stackTrace, "Stack trace should not be null");
      assertTrue(stackTrace.length > 0, "Stack trace should not be empty");
    }
  }

  @Nested
  @DisplayName("Error Scenario Tests")
  class ErrorScenarioTests {

    @Test
    @DisplayName("should handle unsupported algorithm error message")
    void shouldHandleUnsupportedAlgorithmErrorMessage() {
      final CryptoException exception =
          new CryptoException(
              "Algorithm AES-512 is not supported", CryptoErrorCode.UNSUPPORTED_ALGORITHM);

      assertTrue(
          exception.getMessage().contains("Algorithm"),
          "Message should contain 'Algorithm' keyword");
      assertTrue(
          exception.getMessage().contains("not supported"),
          "Message should contain 'not supported' keyword");
    }

    @Test
    @DisplayName("should handle invalid signature error message")
    void shouldHandleInvalidSignatureErrorMessage() {
      final CryptoException exception =
          new CryptoException(
              "Signature verification failed for document hash",
              CryptoErrorCode.VERIFICATION_FAILED);

      assertTrue(
          exception.getMessage().contains("Signature"),
          "Message should contain 'Signature' keyword");
      assertTrue(
          exception.getMessage().contains("verification"),
          "Message should contain 'verification' keyword");
    }

    @Test
    @DisplayName("should handle key generation failure error message")
    void shouldHandleKeyGenerationFailureErrorMessage() {
      final CryptoException exception =
          new CryptoException(
              "RSA key generation failed due to insufficient entropy",
              CryptoErrorCode.KEY_GENERATION_FAILED);

      assertTrue(
          exception.getMessage().contains("key generation"),
          "Message should contain 'key generation' keyword");
      assertTrue(
          exception.getMessage().contains("entropy"),
          "Message should contain 'entropy' keyword");
    }

    @Test
    @DisplayName("should handle authentication failure error message")
    void shouldHandleAuthenticationFailureErrorMessage() {
      final CryptoException exception =
          new CryptoException(
              "AEAD authentication tag mismatch", CryptoErrorCode.AUTHENTICATION_FAILED);

      assertTrue(
          exception.getMessage().contains("authentication"),
          "Message should contain 'authentication' keyword");
    }
  }
}
