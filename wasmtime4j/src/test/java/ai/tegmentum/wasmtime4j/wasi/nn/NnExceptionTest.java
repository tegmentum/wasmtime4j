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

package ai.tegmentum.wasmtime4j.wasi.nn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Tests for {@link NnException} class.
 *
 * <p>NnException is thrown when WASI-NN operations fail.
 */
@DisplayName("NnException Tests")
class NnExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(NnException.class.getModifiers()), "NnException should be public");
    }

    @Test
    @DisplayName("should extend WasmException")
    void shouldExtendWasmException() {
      assertTrue(
          WasmException.class.isAssignableFrom(NnException.class),
          "NnException should extend WasmException");
    }

    @Test
    @DisplayName("should have serialVersionUID field")
    void shouldHaveSerialVersionUID() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          NnException.class.getDeclaredField("serialVersionUID");
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
      final String message = "NN operation failed";
      final NnException exception = new NnException(message);

      assertEquals(message, exception.getMessage(), "Message should match the provided message");
      assertNull(exception.getCause(), "Cause should be null when not provided");
      assertEquals(
          NnErrorCode.UNKNOWN,
          exception.getErrorCode(),
          "Error code should default to UNKNOWN when not provided");
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final String message = "Inference failed";
      final RuntimeException cause = new RuntimeException("Backend error");
      final NnException exception = new NnException(message, cause);

      assertEquals(message, exception.getMessage(), "Message should match the provided message");
      assertSame(cause, exception.getCause(), "Cause should match the provided cause");
      assertEquals(
          NnErrorCode.UNKNOWN,
          exception.getErrorCode(),
          "Error code should default to UNKNOWN when not provided");
    }

    @Test
    @DisplayName("should create exception with error code and message")
    void shouldCreateExceptionWithErrorCodeAndMessage() {
      final NnErrorCode errorCode = NnErrorCode.TIMEOUT;
      final String message = "Model loading timed out";
      final NnException exception = new NnException(errorCode, message);

      assertTrue(
          exception.getMessage().contains(message),
          "Message should contain the provided message text");
      assertNull(exception.getCause(), "Cause should be null when not provided");
      assertEquals(
          errorCode,
          exception.getErrorCode(),
          "Error code should match the provided error code");
    }

    @Test
    @DisplayName("should create exception with error code, message, and cause")
    void shouldCreateExceptionWithErrorCodeMessageAndCause() {
      final NnErrorCode errorCode = NnErrorCode.RUNTIME_ERROR;
      final String message = "Inference engine crashed";
      final RuntimeException cause = new RuntimeException("Segfault in native code");
      final NnException exception = new NnException(errorCode, message, cause);

      assertTrue(
          exception.getMessage().contains(message),
          "Message should contain the provided message text");
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
      final NnException exception = new NnException(NnErrorCode.NOT_FOUND, "Model not found");

      assertEquals(
          NnErrorCode.NOT_FOUND,
          exception.getErrorCode(),
          "getErrorCode should return the error code set at construction");
    }

    @Test
    @DisplayName("should default to UNKNOWN error code")
    void shouldDefaultToUnknownErrorCode() {
      final NnException exception = new NnException("Unknown error");

      assertEquals(
          NnErrorCode.UNKNOWN,
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
        throw new NnException("NN error");
      } catch (final WasmException e) {
        caught = true;
        assertTrue(e instanceof NnException, "Should be instance of NnException");
      }
      assertTrue(caught, "Exception should be caught as WasmException");
    }

    @Test
    @DisplayName("should be catchable as Exception")
    void shouldBeCatchableAsException() {
      boolean caught = false;
      try {
        throw new NnException("NN error");
      } catch (final Exception e) {
        caught = true;
        assertTrue(e instanceof NnException, "Should be instance of NnException");
      }
      assertTrue(caught, "Exception should be caught as Exception");
    }

    @Test
    @DisplayName("should preserve stack trace")
    void shouldPreserveStackTrace() {
      final NnException exception = new NnException("Test");
      final StackTraceElement[] stackTrace = exception.getStackTrace();

      assertNotNull(stackTrace, "Stack trace should not be null");
      assertTrue(stackTrace.length > 0, "Stack trace should not be empty");
    }
  }

  @Nested
  @DisplayName("Error Scenario Tests")
  class ErrorScenarioTests {

    @Test
    @DisplayName("should handle invalid argument error message")
    void shouldHandleInvalidArgumentErrorMessage() {
      final NnException exception =
          new NnException(
              NnErrorCode.INVALID_ARGUMENT, "Tensor dimensions do not match model input shape");

      assertTrue(
          exception.getMessage().contains("Tensor"),
          "Message should contain 'Tensor' keyword");
      assertTrue(
          exception.getMessage().contains("dimensions"),
          "Message should contain 'dimensions' keyword");
    }

    @Test
    @DisplayName("should handle model not found error message")
    void shouldHandleModelNotFoundErrorMessage() {
      final NnException exception =
          new NnException(NnErrorCode.NOT_FOUND, "Model 'resnet50' not found in registry");

      assertTrue(
          exception.getMessage().contains("Model"),
          "Message should contain 'Model' keyword");
      assertTrue(
          exception.getMessage().contains("not found"),
          "Message should contain 'not found' keyword");
    }

    @Test
    @DisplayName("should handle security violation error message")
    void shouldHandleSecurityViolationErrorMessage() {
      final NnException exception =
          new NnException(
              NnErrorCode.SECURITY, "Access denied to GPU device for untrusted module");

      assertTrue(
          exception.getMessage().contains("Access denied"),
          "Message should contain 'Access denied' keyword");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("invalidArgument should create exception with INVALID_ARGUMENT error code")
    void invalidArgumentShouldCreateCorrectException() {
      final NnException exception = NnException.invalidArgument("Bad tensor shape");

      assertEquals(
          NnErrorCode.INVALID_ARGUMENT,
          exception.getErrorCode(),
          "Error code should be INVALID_ARGUMENT");
      assertTrue(
          exception.getMessage().contains("Bad tensor shape"),
          "Message should contain the provided message");
    }

    @Test
    @DisplayName("invalidEncoding should create exception with INVALID_ENCODING error code")
    void invalidEncodingShouldCreateCorrectException() {
      final NnException exception = NnException.invalidEncoding("Unsupported ONNX version");

      assertEquals(
          NnErrorCode.INVALID_ENCODING,
          exception.getErrorCode(),
          "Error code should be INVALID_ENCODING");
      assertTrue(
          exception.getMessage().contains("Unsupported ONNX version"),
          "Message should contain the provided message");
    }

    @Test
    @DisplayName("timeout should create exception with TIMEOUT error code")
    void timeoutShouldCreateCorrectException() {
      final NnException exception = NnException.timeout("Inference exceeded 30s limit");

      assertEquals(
          NnErrorCode.TIMEOUT,
          exception.getErrorCode(),
          "Error code should be TIMEOUT");
      assertTrue(
          exception.getMessage().contains("Inference exceeded 30s limit"),
          "Message should contain the provided message");
    }

    @Test
    @DisplayName("runtimeError should create exception with RUNTIME_ERROR error code")
    void runtimeErrorShouldCreateCorrectException() {
      final NnException exception = NnException.runtimeError("GPU out of memory");

      assertEquals(
          NnErrorCode.RUNTIME_ERROR,
          exception.getErrorCode(),
          "Error code should be RUNTIME_ERROR");
      assertTrue(
          exception.getMessage().contains("GPU out of memory"),
          "Message should contain the provided message");
    }

    @Test
    @DisplayName("runtimeError with cause should create exception with RUNTIME_ERROR and cause")
    void runtimeErrorWithCauseShouldCreateCorrectException() {
      final RuntimeException cause = new RuntimeException("CUDA error");
      final NnException exception = NnException.runtimeError("GPU failure", cause);

      assertEquals(
          NnErrorCode.RUNTIME_ERROR,
          exception.getErrorCode(),
          "Error code should be RUNTIME_ERROR");
      assertTrue(
          exception.getMessage().contains("GPU failure"),
          "Message should contain the provided message");
      assertSame(cause, exception.getCause(), "Cause should match the provided cause");
    }

    @Test
    @DisplayName("unsupportedOperation should create exception with UNSUPPORTED_OPERATION error code")
    void unsupportedOperationShouldCreateCorrectException() {
      final NnException exception =
          NnException.unsupportedOperation("Quantization not supported on this backend");

      assertEquals(
          NnErrorCode.UNSUPPORTED_OPERATION,
          exception.getErrorCode(),
          "Error code should be UNSUPPORTED_OPERATION");
      assertTrue(
          exception.getMessage().contains("Quantization"),
          "Message should contain the provided message");
    }

    @Test
    @DisplayName("tooLarge should create exception with TOO_LARGE error code")
    void tooLargeShouldCreateCorrectException() {
      final NnException exception = NnException.tooLarge("Model exceeds 4GB memory limit");

      assertEquals(
          NnErrorCode.TOO_LARGE,
          exception.getErrorCode(),
          "Error code should be TOO_LARGE");
      assertTrue(
          exception.getMessage().contains("Model exceeds"),
          "Message should contain the provided message");
    }

    @Test
    @DisplayName("notFound should create exception with NOT_FOUND error code")
    void notFoundShouldCreateCorrectException() {
      final NnException exception = NnException.notFound("Model registry entry missing");

      assertEquals(
          NnErrorCode.NOT_FOUND,
          exception.getErrorCode(),
          "Error code should be NOT_FOUND");
      assertTrue(
          exception.getMessage().contains("Model registry"),
          "Message should contain the provided message");
    }

    @Test
    @DisplayName("security should create exception with SECURITY error code")
    void securityShouldCreateCorrectException() {
      final NnException exception = NnException.security("Untrusted model source");

      assertEquals(
          NnErrorCode.SECURITY,
          exception.getErrorCode(),
          "Error code should be SECURITY");
      assertTrue(
          exception.getMessage().contains("Untrusted model"),
          "Message should contain the provided message");
    }

    @Test
    @DisplayName("fromNativeError should create exception using NnErrorCode.fromNativeCode")
    void fromNativeErrorShouldCreateCorrectException() {
      final NnException exception = NnException.fromNativeError(0, "Native error occurred");

      assertEquals(
          NnErrorCode.fromNativeCode(0),
          exception.getErrorCode(),
          "Error code should match NnErrorCode.fromNativeCode(0)");
      assertTrue(
          exception.getMessage().contains("Native error occurred"),
          "Message should contain the provided message");
    }
  }

  @Nested
  @DisplayName("Boolean Checker Tests")
  class BooleanCheckerTests {

    @Test
    @DisplayName("isInvalidArgument should return true only for INVALID_ARGUMENT")
    void isInvalidArgumentShouldReturnTrueOnlyForInvalidArgument() {
      final NnException exception =
          new NnException(NnErrorCode.INVALID_ARGUMENT, "Bad argument");

      assertTrue(exception.isInvalidArgument(), "isInvalidArgument should return true");
      assertFalse(exception.isInvalidEncoding(), "isInvalidEncoding should return false");
      assertFalse(exception.isTimeout(), "isTimeout should return false");
      assertFalse(exception.isRuntimeError(), "isRuntimeError should return false");
      assertFalse(exception.isUnsupportedOperation(), "isUnsupportedOperation should return false");
      assertFalse(exception.isTooLarge(), "isTooLarge should return false");
      assertFalse(exception.isNotFound(), "isNotFound should return false");
      assertFalse(exception.isSecurity(), "isSecurity should return false");
    }

    @Test
    @DisplayName("isInvalidEncoding should return true only for INVALID_ENCODING")
    void isInvalidEncodingShouldReturnTrueOnlyForInvalidEncoding() {
      final NnException exception =
          new NnException(NnErrorCode.INVALID_ENCODING, "Bad encoding");

      assertFalse(exception.isInvalidArgument(), "isInvalidArgument should return false");
      assertTrue(exception.isInvalidEncoding(), "isInvalidEncoding should return true");
      assertFalse(exception.isTimeout(), "isTimeout should return false");
      assertFalse(exception.isRuntimeError(), "isRuntimeError should return false");
      assertFalse(exception.isUnsupportedOperation(), "isUnsupportedOperation should return false");
      assertFalse(exception.isTooLarge(), "isTooLarge should return false");
      assertFalse(exception.isNotFound(), "isNotFound should return false");
      assertFalse(exception.isSecurity(), "isSecurity should return false");
    }

    @Test
    @DisplayName("isTimeout should return true only for TIMEOUT")
    void isTimeoutShouldReturnTrueOnlyForTimeout() {
      final NnException exception = new NnException(NnErrorCode.TIMEOUT, "Timed out");

      assertFalse(exception.isInvalidArgument(), "isInvalidArgument should return false");
      assertFalse(exception.isInvalidEncoding(), "isInvalidEncoding should return false");
      assertTrue(exception.isTimeout(), "isTimeout should return true");
      assertFalse(exception.isRuntimeError(), "isRuntimeError should return false");
      assertFalse(exception.isUnsupportedOperation(), "isUnsupportedOperation should return false");
      assertFalse(exception.isTooLarge(), "isTooLarge should return false");
      assertFalse(exception.isNotFound(), "isNotFound should return false");
      assertFalse(exception.isSecurity(), "isSecurity should return false");
    }

    @Test
    @DisplayName("isRuntimeError should return true only for RUNTIME_ERROR")
    void isRuntimeErrorShouldReturnTrueOnlyForRuntimeError() {
      final NnException exception =
          new NnException(NnErrorCode.RUNTIME_ERROR, "Runtime failure");

      assertFalse(exception.isInvalidArgument(), "isInvalidArgument should return false");
      assertFalse(exception.isInvalidEncoding(), "isInvalidEncoding should return false");
      assertFalse(exception.isTimeout(), "isTimeout should return false");
      assertTrue(exception.isRuntimeError(), "isRuntimeError should return true");
      assertFalse(exception.isUnsupportedOperation(), "isUnsupportedOperation should return false");
      assertFalse(exception.isTooLarge(), "isTooLarge should return false");
      assertFalse(exception.isNotFound(), "isNotFound should return false");
      assertFalse(exception.isSecurity(), "isSecurity should return false");
    }

    @Test
    @DisplayName("isUnsupportedOperation should return true only for UNSUPPORTED_OPERATION")
    void isUnsupportedOperationShouldReturnTrueOnlyForUnsupportedOperation() {
      final NnException exception =
          new NnException(NnErrorCode.UNSUPPORTED_OPERATION, "Not supported");

      assertFalse(exception.isInvalidArgument(), "isInvalidArgument should return false");
      assertFalse(exception.isInvalidEncoding(), "isInvalidEncoding should return false");
      assertFalse(exception.isTimeout(), "isTimeout should return false");
      assertFalse(exception.isRuntimeError(), "isRuntimeError should return false");
      assertTrue(exception.isUnsupportedOperation(), "isUnsupportedOperation should return true");
      assertFalse(exception.isTooLarge(), "isTooLarge should return false");
      assertFalse(exception.isNotFound(), "isNotFound should return false");
      assertFalse(exception.isSecurity(), "isSecurity should return false");
    }

    @Test
    @DisplayName("isTooLarge should return true only for TOO_LARGE")
    void isTooLargeShouldReturnTrueOnlyForTooLarge() {
      final NnException exception = new NnException(NnErrorCode.TOO_LARGE, "Too large");

      assertFalse(exception.isInvalidArgument(), "isInvalidArgument should return false");
      assertFalse(exception.isInvalidEncoding(), "isInvalidEncoding should return false");
      assertFalse(exception.isTimeout(), "isTimeout should return false");
      assertFalse(exception.isRuntimeError(), "isRuntimeError should return false");
      assertFalse(exception.isUnsupportedOperation(), "isUnsupportedOperation should return false");
      assertTrue(exception.isTooLarge(), "isTooLarge should return true");
      assertFalse(exception.isNotFound(), "isNotFound should return false");
      assertFalse(exception.isSecurity(), "isSecurity should return false");
    }

    @Test
    @DisplayName("isNotFound should return true only for NOT_FOUND")
    void isNotFoundShouldReturnTrueOnlyForNotFound() {
      final NnException exception = new NnException(NnErrorCode.NOT_FOUND, "Not found");

      assertFalse(exception.isInvalidArgument(), "isInvalidArgument should return false");
      assertFalse(exception.isInvalidEncoding(), "isInvalidEncoding should return false");
      assertFalse(exception.isTimeout(), "isTimeout should return false");
      assertFalse(exception.isRuntimeError(), "isRuntimeError should return false");
      assertFalse(exception.isUnsupportedOperation(), "isUnsupportedOperation should return false");
      assertFalse(exception.isTooLarge(), "isTooLarge should return false");
      assertTrue(exception.isNotFound(), "isNotFound should return true");
      assertFalse(exception.isSecurity(), "isSecurity should return false");
    }

    @Test
    @DisplayName("isSecurity should return true only for SECURITY")
    void isSecurityShouldReturnTrueOnlyForSecurity() {
      final NnException exception = new NnException(NnErrorCode.SECURITY, "Security error");

      assertFalse(exception.isInvalidArgument(), "isInvalidArgument should return false");
      assertFalse(exception.isInvalidEncoding(), "isInvalidEncoding should return false");
      assertFalse(exception.isTimeout(), "isTimeout should return false");
      assertFalse(exception.isRuntimeError(), "isRuntimeError should return false");
      assertFalse(exception.isUnsupportedOperation(), "isUnsupportedOperation should return false");
      assertFalse(exception.isTooLarge(), "isTooLarge should return false");
      assertFalse(exception.isNotFound(), "isNotFound should return false");
      assertTrue(exception.isSecurity(), "isSecurity should return true");
    }
  }

  @Nested
  @DisplayName("Message Formatting Tests")
  class MessageFormattingTests {

    @Test
    @DisplayName("should prefix message with WASI name for TIMEOUT error code")
    void shouldPrefixMessageWithWasiNameForTimeout() {
      final NnException exception = new NnException(NnErrorCode.TIMEOUT, "Operation timed out");

      assertTrue(
          exception.getMessage().contains("[timeout]"),
          "Message should contain '[timeout]' prefix for TIMEOUT error code");
      assertTrue(
          exception.getMessage().contains("Operation timed out"),
          "Message should contain the original message text");
    }

    @Test
    @DisplayName("should prefix message with WASI name for INVALID_ARGUMENT error code")
    void shouldPrefixMessageWithWasiNameForInvalidArgument() {
      final NnException exception =
          new NnException(NnErrorCode.INVALID_ARGUMENT, "Bad input");

      assertTrue(
          exception.getMessage().contains("[invalid-argument]"),
          "Message should contain '[invalid-argument]' prefix for INVALID_ARGUMENT error code");
      assertTrue(
          exception.getMessage().contains("Bad input"),
          "Message should contain the original message text");
    }

    @Test
    @DisplayName("should prefix message with WASI name for RUNTIME_ERROR error code")
    void shouldPrefixMessageWithWasiNameForRuntimeError() {
      final NnException exception =
          new NnException(NnErrorCode.RUNTIME_ERROR, "Execution failed");

      assertTrue(
          exception.getMessage().contains("[runtime-error]"),
          "Message should contain '[runtime-error]' prefix for RUNTIME_ERROR error code");
      assertTrue(
          exception.getMessage().contains("Execution failed"),
          "Message should contain the original message text");
    }

    @Test
    @DisplayName("should prefix message with WASI name for NOT_FOUND error code")
    void shouldPrefixMessageWithWasiNameForNotFound() {
      final NnException exception =
          new NnException(NnErrorCode.NOT_FOUND, "Resource missing");

      assertTrue(
          exception.getMessage().contains("[not-found]"),
          "Message should contain '[not-found]' prefix for NOT_FOUND error code");
      assertTrue(
          exception.getMessage().contains("Resource missing"),
          "Message should contain the original message text");
    }

    @Test
    @DisplayName("should not prefix message for UNKNOWN error code")
    void shouldNotPrefixMessageForUnknownErrorCode() {
      final NnException exception = new NnException("Plain message");

      assertEquals(
          "Plain message",
          exception.getMessage(),
          "Message should not be prefixed for UNKNOWN error code");
    }
  }
}
