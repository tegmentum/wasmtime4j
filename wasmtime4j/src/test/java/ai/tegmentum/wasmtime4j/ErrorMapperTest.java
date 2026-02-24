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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.ErrorMapper;
import ai.tegmentum.wasmtime4j.exception.I32ExitException;
import ai.tegmentum.wasmtime4j.exception.InstantiationException;
import ai.tegmentum.wasmtime4j.exception.LinkingException;
import ai.tegmentum.wasmtime4j.exception.ResourceException;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasiException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.exception.WasmRuntimeException;
import ai.tegmentum.wasmtime4j.exception.WasmSecurityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ErrorMapper}.
 *
 * <p>Validates that native error codes are correctly mapped to the appropriate Java exception types.
 * Focuses on the WASI_EXIT code path and the parseI32Exit logic that extracts exit codes from
 * native error messages.
 */
@DisplayName("ErrorMapper")
class ErrorMapperTest {

  @Nested
  @DisplayName("WASI_EXIT mapping")
  class WasiExitMapping {

    @Test
    @DisplayName("WASI_EXIT code (-27) produces I32ExitException")
    void wasiExitCodeProducesI32ExitException() {
      final WasmException result = ErrorMapper.mapErrorCode(-27, "exit_code:0");

      assertInstanceOf(
          I32ExitException.class,
          result,
          "Error code -27 should produce I32ExitException, got: " + result.getClass().getName());
    }

    @Test
    @DisplayName("exit code 0 is parsed correctly")
    void exitCodeZeroParsed() {
      final WasmException result = ErrorMapper.mapErrorCode(-27, "exit_code:0");
      final I32ExitException exitEx = (I32ExitException) result;

      assertEquals(0, exitEx.getExitCode(), "Should parse exit code 0");
      assertTrue(exitEx.isSuccess(), "Exit code 0 should be success");
    }

    @Test
    @DisplayName("exit code 1 is parsed correctly")
    void exitCodeOneParsed() {
      final WasmException result = ErrorMapper.mapErrorCode(-27, "exit_code:1");
      final I32ExitException exitEx = (I32ExitException) result;

      assertEquals(1, exitEx.getExitCode(), "Should parse exit code 1");
      assertFalse(exitEx.isSuccess(), "Exit code 1 should not be success");
    }

    @Test
    @DisplayName("exit code 42 is parsed correctly")
    void exitCode42Parsed() {
      final WasmException result = ErrorMapper.mapErrorCode(-27, "exit_code:42");
      final I32ExitException exitEx = (I32ExitException) result;

      assertEquals(42, exitEx.getExitCode(), "Should parse exit code 42");
    }

    @Test
    @DisplayName("negative exit code is parsed correctly")
    void negativeExitCodeParsed() {
      final WasmException result = ErrorMapper.mapErrorCode(-27, "exit_code:-1");
      final I32ExitException exitEx = (I32ExitException) result;

      assertEquals(-1, exitEx.getExitCode(), "Should parse negative exit code");
    }

    @Test
    @DisplayName("exit code with whitespace is parsed correctly")
    void exitCodeWithWhitespaceParsed() {
      final WasmException result = ErrorMapper.mapErrorCode(-27, "exit_code: 5 ");
      final I32ExitException exitEx = (I32ExitException) result;

      assertEquals(5, exitEx.getExitCode(), "Should parse exit code with whitespace");
    }

    @Test
    @DisplayName("exit code 127 is parsed correctly")
    void exitCode127Parsed() {
      // Native layer sends "exit_code:127" as the error message for WASI_EXIT
      final WasmException result = ErrorMapper.mapErrorCode(-27, "exit_code:127");
      final I32ExitException exitEx = (I32ExitException) result;

      assertEquals(127, exitEx.getExitCode(), "Should parse exit code 127");
    }

    @Test
    @DisplayName("unparseable message defaults to exit code 1")
    void unparseableMessageDefaultsToOne() {
      final WasmException result = ErrorMapper.mapErrorCode(-27, "no exit code here");
      final I32ExitException exitEx = (I32ExitException) result;

      assertEquals(1, exitEx.getExitCode(), "Unparseable message should default to exit code 1");
    }

    @Test
    @DisplayName("null context defaults to exit code 1")
    void nullContextDefaultsToOne() {
      final WasmException result = ErrorMapper.mapErrorCode(-27);
      final I32ExitException exitEx = (I32ExitException) result;

      // With null context, the message from WasmErrorCode.WASI_EXIT is "WASI proc_exit called"
      // The parseI32Exit won't find a colon with a number, so defaults to 1
      assertEquals(1, exitEx.getExitCode(), "Null context should default to exit code 1");
    }

    @Test
    @DisplayName("non-numeric after colon defaults to exit code 1")
    void nonNumericAfterColonDefaultsToOne() {
      final WasmException result = ErrorMapper.mapErrorCode(-27, "code:abc");
      final I32ExitException exitEx = (I32ExitException) result;

      assertEquals(1, exitEx.getExitCode(), "Non-numeric after colon should default to 1");
    }
  }

  @Nested
  @DisplayName("Standard error code mapping")
  class StandardMapping {

    @Test
    @DisplayName("COMPILATION_ERROR (-1) maps to CompilationException")
    void compilationError() {
      final WasmException result = ErrorMapper.mapErrorCode(-1, "test");
      assertInstanceOf(CompilationException.class, result);
    }

    @Test
    @DisplayName("VALIDATION_ERROR (-2) maps to ValidationException")
    void validationError() {
      final WasmException result = ErrorMapper.mapErrorCode(-2, "test");
      assertInstanceOf(ValidationException.class, result);
    }

    @Test
    @DisplayName("RUNTIME_ERROR (-3) maps to WasmRuntimeException")
    void runtimeError() {
      final WasmException result = ErrorMapper.mapErrorCode(-3, "test");
      assertInstanceOf(WasmRuntimeException.class, result);
    }

    @Test
    @DisplayName("INSTANCE_ERROR (-6) maps to InstantiationException")
    void instanceError() {
      final WasmException result = ErrorMapper.mapErrorCode(-6, "test");
      assertInstanceOf(InstantiationException.class, result);
    }

    @Test
    @DisplayName("IMPORT_EXPORT_ERROR (-9) maps to LinkingException")
    void importExportError() {
      final WasmException result = ErrorMapper.mapErrorCode(-9, "test");
      assertInstanceOf(LinkingException.class, result);
    }

    @Test
    @DisplayName("RESOURCE_ERROR (-11) maps to ResourceException")
    void resourceError() {
      final WasmException result = ErrorMapper.mapErrorCode(-11, "test");
      assertInstanceOf(ResourceException.class, result);
    }

    @Test
    @DisplayName("WASI_ERROR (-15) maps to WasiException")
    void wasiError() {
      final WasmException result = ErrorMapper.mapErrorCode(-15, "test");
      assertInstanceOf(WasiException.class, result);
    }

    @Test
    @DisplayName("SECURITY_ERROR (-16) maps to WasmSecurityException")
    void securityError() {
      final WasmException result = ErrorMapper.mapErrorCode(-16, "test");
      assertInstanceOf(WasmSecurityException.class, result);
    }

    @Test
    @DisplayName("SECURITY_VIOLATION (-22) maps to WasmSecurityException")
    void securityViolation() {
      final WasmException result = ErrorMapper.mapErrorCode(-22, "test");
      assertInstanceOf(WasmSecurityException.class, result);
    }

    @Test
    @DisplayName("Unknown error code maps to WasmException")
    void unknownCode() {
      final WasmException result = ErrorMapper.mapErrorCode(-999, "test");
      assertInstanceOf(WasmException.class, result);
      assertNotNull(result.getMessage());
      assertTrue(result.getMessage().contains("-999"), "Should contain the unknown code");
    }

    @Test
    @DisplayName("SUCCESS (0) returns WasmException")
    void successCode() {
      final WasmException result = ErrorMapper.mapErrorCode(0, "test");
      assertInstanceOf(WasmException.class, result);
    }
  }
}
