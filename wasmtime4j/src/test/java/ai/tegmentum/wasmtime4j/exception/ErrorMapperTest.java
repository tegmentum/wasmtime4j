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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link ErrorMapper} — the shared error code to exception mapping used by both JNI and
 * Panama implementations.
 */
@DisplayName("ErrorMapper Tests")
class ErrorMapperTest {

  @Nested
  @DisplayName("Exception Type Mapping Tests")
  class ExceptionTypeMappingTests {

    @Test
    @DisplayName("Compilation error (-1) should map to CompilationException")
    void shouldMapCompilationError() {
      final WasmException ex = ErrorMapper.mapErrorCode(-1, "compile failed");
      assertTrue(
          ex instanceof CompilationException,
          "Error code -1 should produce CompilationException, got: " + ex.getClass().getName());
      assertTrue(
          ex.getMessage().contains("compile failed"),
          "Should include context. Got: " + ex.getMessage());
    }

    @Test
    @DisplayName("Validation error (-2) should map to ValidationException")
    void shouldMapValidationError() {
      final WasmException ex = ErrorMapper.mapErrorCode(-2, "invalid module");
      assertTrue(
          ex instanceof ValidationException,
          "Error code -2 should produce ValidationException, got: " + ex.getClass().getName());
    }

    @Test
    @DisplayName("Instance error (-6) should map to InstantiationException")
    void shouldMapInstanceError() {
      final WasmException ex = ErrorMapper.mapErrorCode(-6, "instance failure");
      assertTrue(
          ex instanceof InstantiationException,
          "Error code -6 should produce InstantiationException, got: " + ex.getClass().getName());
    }

    @Test
    @DisplayName("Import/Export error (-9) should map to LinkingException")
    void shouldMapImportExportError() {
      final WasmException ex = ErrorMapper.mapErrorCode(-9, "import missing");
      assertTrue(
          ex instanceof LinkingException,
          "Error code -9 should produce LinkingException, got: " + ex.getClass().getName());
    }

    @Test
    @DisplayName("Resource error (-11) should map to ResourceException")
    void shouldMapResourceError() {
      final WasmException ex = ErrorMapper.mapErrorCode(-11, "resource exhausted");
      assertTrue(
          ex instanceof ResourceException,
          "Error code -11 should produce ResourceException, got: " + ex.getClass().getName());
    }

    @Test
    @DisplayName("WASI error (-15) should map to WasiException")
    void shouldMapWasiError() {
      final WasmException ex = ErrorMapper.mapErrorCode(-15, "wasi failure");
      assertTrue(
          ex instanceof WasiException,
          "Error code -15 should produce WasiException, got: " + ex.getClass().getName());
    }

    @Test
    @DisplayName("Security error (-16) should map to WasmSecurityException")
    void shouldMapSecurityError() {
      final WasmException ex = ErrorMapper.mapErrorCode(-16, "access denied");
      assertTrue(
          ex instanceof WasmSecurityException,
          "Error code -16 should produce WasmSecurityException, got: " + ex.getClass().getName());
    }

    @Test
    @DisplayName("Security violation (-22) should map to WasmSecurityException")
    void shouldMapSecurityViolation() {
      final WasmException ex = ErrorMapper.mapErrorCode(-22, "violation");
      assertTrue(
          ex instanceof WasmSecurityException,
          "Error code -22 should produce WasmSecurityException, got: " + ex.getClass().getName());
    }

    @ParameterizedTest(name = "Error code {0} should map to WasmRuntimeException")
    @ValueSource(
        ints = {
          -3, -4, -5, -7, -8, -10, -12, -13, -14, -17, -18, -19, -20, -21, -23, -24, -25, -26
        })
    @DisplayName("Runtime-category errors should map to WasmRuntimeException")
    void shouldMapRuntimeCategoryErrors(final int errorCode) {
      final WasmException ex = ErrorMapper.mapErrorCode(errorCode, "runtime issue");
      assertTrue(
          ex instanceof WasmRuntimeException,
          "Error code "
              + errorCode
              + " should produce WasmRuntimeException, got: "
              + ex.getClass().getName());
    }
  }

  @Nested
  @DisplayName("Message Content Tests")
  class MessageContentTests {

    @ParameterizedTest(name = "Error code {0} should produce message containing \"{1}\"")
    @CsvSource({
      "0, No error occurred",
      "-1, WebAssembly compilation failed",
      "-2, WebAssembly module validation failed",
      "-3, WebAssembly runtime error",
      "-4, Engine configuration error",
      "-5, Store error",
      "-6, Instance error",
      "-7, Memory access or allocation error",
      "-8, Function invocation error",
      "-9, Import or export resolution error",
      "-10, Type conversion or validation error",
      "-11, Resource management error",
      "-12, I/O operation error",
      "-13, Invalid parameter",
      "-14, Threading or concurrency error",
      "-15, WASI error",
      "-16, Security and permission violation error",
      "-17, Component model error",
      "-18, Interface definition or binding error",
      "-19, Network operation error",
      "-20, Process execution error",
      "-21, Internal system error",
      "-22, Security violation error",
      "-23, Invalid data format error",
      "-24, I/O operation error",
      "-25, Unsupported operation",
      "-26, Operation would block"
    })
    @DisplayName("All 27 error codes should produce correct messages")
    void shouldMapAllErrorCodes(final int errorCode, final String expectedMessagePart) {
      final WasmException ex = ErrorMapper.mapErrorCode(errorCode, "test detail");
      assertNotNull(ex, "Exception should not be null for error code " + errorCode);
      assertTrue(
          ex.getMessage().contains(expectedMessagePart),
          "Error code "
              + errorCode
              + " should produce message containing '"
              + expectedMessagePart
              + "' but got: "
              + ex.getMessage());
    }

    @Test
    @DisplayName("Should include context in exception message")
    void shouldIncludeContextInMessage() {
      final WasmException ex = ErrorMapper.mapErrorCode(-1, "Failed to compile WAT");
      assertTrue(
          ex.getMessage().contains("Failed to compile WAT"),
          "Should include context. Got: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should include error message detail in all mapped exceptions")
    void shouldIncludeErrorMessageInAllMappedExceptions() {
      final String testMessage = "specific error detail";
      for (int code = -1; code >= -26; code--) {
        final WasmException ex = ErrorMapper.mapErrorCode(code, testMessage);
        assertTrue(
            ex.getMessage().contains(testMessage),
            "Error code " + code + " should include the message. Got: " + ex.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should work with null context")
    void shouldWorkWithNullContext() {
      final WasmException ex = ErrorMapper.mapErrorCode(-1);
      assertNotNull(ex, "Exception should not be null");
      assertTrue(
          ex instanceof CompilationException,
          "Should still produce correct type with null context");
    }

    @Test
    @DisplayName("Should handle unknown error code gracefully")
    void shouldHandleUnknownErrorCode() {
      final WasmException ex = ErrorMapper.mapErrorCode(-999, "something broke");
      assertNotNull(ex, "Exception should not be null");
      assertTrue(
          ex.getMessage().contains("-999"),
          "Should include the error code in message. Got: " + ex.getMessage());
      assertTrue(
          ex.getMessage().contains("Unknown native error"),
          "Should indicate unknown. Got: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should handle positive error code")
    void shouldHandlePositiveErrorCode() {
      final WasmException ex = ErrorMapper.mapErrorCode(1, "positive code");
      assertNotNull(ex, "Exception should not be null");
      assertTrue(ex.getMessage().contains("Unknown native error"));
    }

    @Test
    @DisplayName("Should handle Integer.MAX_VALUE error code")
    void shouldHandleMaxIntErrorCode() {
      final WasmException ex = ErrorMapper.mapErrorCode(Integer.MAX_VALUE, "max int");
      assertNotNull(ex, "Exception should not be null");
      assertTrue(ex.getMessage().contains("Unknown native error"));
    }

    @Test
    @DisplayName("Should handle Integer.MIN_VALUE error code")
    void shouldHandleMinIntErrorCode() {
      final WasmException ex = ErrorMapper.mapErrorCode(Integer.MIN_VALUE, "min int");
      assertNotNull(ex, "Exception should not be null");
      assertTrue(ex.getMessage().contains("Unknown native error"));
    }

    @Test
    @DisplayName("All WasmErrorCode values should be handled")
    void allWasmErrorCodesShouldBeHandled() {
      for (final WasmErrorCode errorCode : WasmErrorCode.values()) {
        final WasmException ex = ErrorMapper.mapErrorCode(errorCode.getCode(), "coverage test");
        assertNotNull(
            ex,
            "WasmErrorCode."
                + errorCode.name()
                + " (code "
                + errorCode.getCode()
                + ") should produce a non-null exception");
      }
    }
  }
}
