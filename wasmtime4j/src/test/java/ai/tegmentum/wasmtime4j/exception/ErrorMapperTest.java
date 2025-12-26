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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ErrorMapper} utility class.
 *
 * <p>This test class verifies the error mapping functionality that converts native Wasmtime error
 * codes to appropriate Java exceptions.
 */
@DisplayName("ErrorMapper Tests")
class ErrorMapperTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("ErrorMapper should be a final utility class")
    void shouldBeFinalUtilityClass() {
      assertTrue(Modifier.isFinal(ErrorMapper.class.getModifiers()), "ErrorMapper should be final");
    }

    @Test
    @DisplayName("ErrorMapper should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = ErrorMapper.class.getDeclaredConstructor();

      assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");
    }

    @Test
    @DisplayName("Private constructor should throw UnsupportedOperationException")
    void constructorShouldThrowException() throws Exception {
      final Constructor<?> constructor = ErrorMapper.class.getDeclaredConstructor();
      constructor.setAccessible(true);

      final InvocationTargetException exception =
          assertThrows(InvocationTargetException.class, constructor::newInstance);
      assertInstanceOf(
          UnsupportedOperationException.class,
          exception.getCause(),
          "Should throw UnsupportedOperationException");
    }
  }

  @Nested
  @DisplayName("Error Code Constants Tests")
  class ErrorCodeConstantsTests {

    @Test
    @DisplayName("SUCCESS should be 0")
    void successShouldBeZero() {
      assertEquals(0, ErrorMapper.SUCCESS, "SUCCESS should be 0");
    }

    @Test
    @DisplayName("COMPILATION_ERROR should be -1")
    void compilationErrorShouldBeNegativeOne() {
      assertEquals(-1, ErrorMapper.COMPILATION_ERROR, "COMPILATION_ERROR should be -1");
    }

    @Test
    @DisplayName("VALIDATION_ERROR should be -2")
    void validationErrorShouldBeNegativeTwo() {
      assertEquals(-2, ErrorMapper.VALIDATION_ERROR, "VALIDATION_ERROR should be -2");
    }

    @Test
    @DisplayName("RUNTIME_ERROR should be -3")
    void runtimeErrorShouldBeNegativeThree() {
      assertEquals(-3, ErrorMapper.RUNTIME_ERROR, "RUNTIME_ERROR should be -3");
    }

    @Test
    @DisplayName("ENGINE_CONFIG_ERROR should be -4")
    void engineConfigErrorShouldBeNegativeFour() {
      assertEquals(-4, ErrorMapper.ENGINE_CONFIG_ERROR, "ENGINE_CONFIG_ERROR should be -4");
    }

    @Test
    @DisplayName("STORE_ERROR should be -5")
    void storeErrorShouldBeNegativeFive() {
      assertEquals(-5, ErrorMapper.STORE_ERROR, "STORE_ERROR should be -5");
    }

    @Test
    @DisplayName("INSTANCE_ERROR should be -6")
    void instanceErrorShouldBeNegativeSix() {
      assertEquals(-6, ErrorMapper.INSTANCE_ERROR, "INSTANCE_ERROR should be -6");
    }

    @Test
    @DisplayName("MEMORY_ERROR should be -7")
    void memoryErrorShouldBeNegativeSeven() {
      assertEquals(-7, ErrorMapper.MEMORY_ERROR, "MEMORY_ERROR should be -7");
    }

    @Test
    @DisplayName("FUNCTION_ERROR should be -8")
    void functionErrorShouldBeNegativeEight() {
      assertEquals(-8, ErrorMapper.FUNCTION_ERROR, "FUNCTION_ERROR should be -8");
    }

    @Test
    @DisplayName("IMPORT_EXPORT_ERROR should be -9")
    void importExportErrorShouldBeNegativeNine() {
      assertEquals(-9, ErrorMapper.IMPORT_EXPORT_ERROR, "IMPORT_EXPORT_ERROR should be -9");
    }

    @Test
    @DisplayName("TYPE_ERROR should be -10")
    void typeErrorShouldBeNegativeTen() {
      assertEquals(-10, ErrorMapper.TYPE_ERROR, "TYPE_ERROR should be -10");
    }

    @Test
    @DisplayName("RESOURCE_ERROR should be -11")
    void resourceErrorShouldBeNegativeEleven() {
      assertEquals(-11, ErrorMapper.RESOURCE_ERROR, "RESOURCE_ERROR should be -11");
    }

    @Test
    @DisplayName("IO_ERROR should be -12")
    void ioErrorShouldBeNegativeTwelve() {
      assertEquals(-12, ErrorMapper.IO_ERROR, "IO_ERROR should be -12");
    }

    @Test
    @DisplayName("INVALID_PARAMETER_ERROR should be -13")
    void invalidParameterErrorShouldBeNegativeThirteen() {
      assertEquals(
          -13, ErrorMapper.INVALID_PARAMETER_ERROR, "INVALID_PARAMETER_ERROR should be -13");
    }

    @Test
    @DisplayName("CONCURRENCY_ERROR should be -14")
    void concurrencyErrorShouldBeNegativeFourteen() {
      assertEquals(-14, ErrorMapper.CONCURRENCY_ERROR, "CONCURRENCY_ERROR should be -14");
    }

    @Test
    @DisplayName("WASI_ERROR should be -15")
    void wasiErrorShouldBeNegativeFifteen() {
      assertEquals(-15, ErrorMapper.WASI_ERROR, "WASI_ERROR should be -15");
    }

    @Test
    @DisplayName("COMPONENT_ERROR should be -16")
    void componentErrorShouldBeNegativeSixteen() {
      assertEquals(-16, ErrorMapper.COMPONENT_ERROR, "COMPONENT_ERROR should be -16");
    }

    @Test
    @DisplayName("INTERFACE_ERROR should be -17")
    void interfaceErrorShouldBeNegativeSeventeen() {
      assertEquals(-17, ErrorMapper.INTERFACE_ERROR, "INTERFACE_ERROR should be -17");
    }

    @Test
    @DisplayName("INTERNAL_ERROR should be -18")
    void internalErrorShouldBeNegativeEighteen() {
      assertEquals(-18, ErrorMapper.INTERNAL_ERROR, "INTERNAL_ERROR should be -18");
    }
  }

  @Nested
  @DisplayName("mapError Method Tests")
  class MapErrorMethodTests {

    @Test
    @DisplayName("mapError should return WasmException for SUCCESS code")
    void mapErrorShouldReturnWasmExceptionForSuccess() {
      final WasmException result = ErrorMapper.mapError(ErrorMapper.SUCCESS, "Success message");

      assertNotNull(result, "Result should not be null");
      assertInstanceOf(WasmException.class, result, "Result should be WasmException");
    }

    @Test
    @DisplayName("mapError should return CompilationException for COMPILATION_ERROR")
    void mapErrorShouldReturnCompilationExceptionForCompilationError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "Compilation failed");

      assertNotNull(result, "Result should not be null");
      assertInstanceOf(CompilationException.class, result, "Result should be CompilationException");
    }

    @Test
    @DisplayName("mapError should return ValidationException for VALIDATION_ERROR")
    void mapErrorShouldReturnValidationExceptionForValidationError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "Validation failed");

      assertNotNull(result, "Result should not be null");
      assertInstanceOf(ValidationException.class, result, "Result should be ValidationException");
    }

    @Test
    @DisplayName("mapError should return RuntimeException for RUNTIME_ERROR")
    void mapErrorShouldReturnRuntimeExceptionForRuntimeError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "Runtime error occurred");

      assertNotNull(result, "Result should not be null");
      assertInstanceOf(RuntimeException.class, result, "Result should be RuntimeException");
    }

    @Test
    @DisplayName("mapError should return TrapException for trap message")
    void mapErrorShouldReturnTrapExceptionForTrapMessage() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: stack overflow");

      assertNotNull(result, "Result should not be null");
      assertInstanceOf(
          TrapException.class, result, "Result should be TrapException for trap messages");
    }

    @Test
    @DisplayName("mapError should return InstantiationException for ENGINE_CONFIG_ERROR")
    void mapErrorShouldReturnInstantiationExceptionForEngineConfigError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "Invalid configuration");

      assertNotNull(result, "Result should not be null");
      assertInstanceOf(
          InstantiationException.class, result, "Result should be InstantiationException");
    }

    @Test
    @DisplayName("mapError should return LinkingException for IMPORT_EXPORT_ERROR")
    void mapErrorShouldReturnLinkingExceptionForImportExportError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "Import not found");

      assertNotNull(result, "Result should not be null");
      assertInstanceOf(LinkingException.class, result, "Result should be LinkingException");
    }

    @Test
    @DisplayName("mapError should return WasiException for WASI_ERROR")
    void mapErrorShouldReturnWasiExceptionForWasiError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "WASI error occurred");

      assertNotNull(result, "Result should not be null");
      assertInstanceOf(WasiException.class, result, "Result should be WasiException");
    }

    @Test
    @DisplayName("mapError should return WasiFileSystemException for file system errors")
    void mapErrorShouldReturnWasiFileSystemExceptionForFileErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "File not found: /path/to/file");

      assertNotNull(result, "Result should not be null");
      assertInstanceOf(
          WasiFileSystemException.class,
          result,
          "Result should be WasiFileSystemException for file errors");
    }

    @Test
    @DisplayName("mapError should preserve cause")
    void mapErrorShouldPreserveCause() {
      final Throwable cause = new RuntimeException("Root cause");
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "Error with cause", cause);

      assertNotNull(result, "Result should not be null");
      assertSame(cause, result.getCause(), "Cause should be preserved");
    }

    @Test
    @DisplayName("mapError should handle empty message")
    void mapErrorShouldHandleEmptyMessage() {
      final WasmException result = ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "");

      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("mapError should handle null message")
    void mapErrorShouldHandleNullMessage() {
      final WasmException result = ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, null);

      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("mapError should handle unknown error code")
    void mapErrorShouldHandleUnknownErrorCode() {
      final WasmException result = ErrorMapper.mapError(-999, "Unknown error");

      assertNotNull(result, "Result should not be null");
      assertInstanceOf(
          WasmException.class, result, "Result should be WasmException for unknown codes");
    }
  }

  @Nested
  @DisplayName("Compilation Error Mapping Tests")
  class CompilationErrorMappingTests {

    @Test
    @DisplayName("Should detect out of memory errors")
    void shouldDetectOutOfMemoryErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "out of memory during compilation");

      assertInstanceOf(
          ModuleCompilationException.class, result, "Should return ModuleCompilationException");
    }

    @Test
    @DisplayName("Should detect timeout errors")
    void shouldDetectTimeoutErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "compilation timeout exceeded");

      assertInstanceOf(
          ModuleCompilationException.class, result, "Should return ModuleCompilationException");
    }

    @Test
    @DisplayName("Should detect unsupported instruction errors")
    void shouldDetectUnsupportedInstructionErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "unsupported instruction found");

      assertInstanceOf(
          ModuleCompilationException.class, result, "Should return ModuleCompilationException");
    }
  }

  @Nested
  @DisplayName("Validation Error Mapping Tests")
  class ValidationErrorMappingTests {

    @Test
    @DisplayName("Should detect magic number errors")
    void shouldDetectMagicNumberErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "invalid magic number");

      assertInstanceOf(
          ModuleValidationException.class, result, "Should return ModuleValidationException");
    }

    @Test
    @DisplayName("Should detect type mismatch errors")
    void shouldDetectTypeMismatchErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "type mismatch in function");

      assertInstanceOf(
          ModuleValidationException.class, result, "Should return ModuleValidationException");
    }

    @Test
    @DisplayName("Should detect malformed module errors")
    void shouldDetectMalformedModuleErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "malformed module structure");

      assertInstanceOf(
          ModuleValidationException.class, result, "Should return ModuleValidationException");
    }
  }

  @Nested
  @DisplayName("Trap Error Mapping Tests")
  class TrapErrorMappingTests {

    @Test
    @DisplayName("Should detect stack overflow trap")
    void shouldDetectStackOverflowTrap() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: stack overflow");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.STACK_OVERFLOW,
          trap.getTrapType(),
          "Should be STACK_OVERFLOW type");
    }

    @Test
    @DisplayName("Should detect memory out of bounds trap")
    void shouldDetectMemoryOutOfBoundsTrap() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: memory out of bounds access");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.MEMORY_OUT_OF_BOUNDS,
          trap.getTrapType(),
          "Should be MEMORY_OUT_OF_BOUNDS type");
    }

    @Test
    @DisplayName("Should detect division by zero trap")
    void shouldDetectDivisionByZeroTrap() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: integer division by zero");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.INTEGER_DIVISION_BY_ZERO,
          trap.getTrapType(),
          "Should be INTEGER_DIVISION_BY_ZERO type");
    }

    @Test
    @DisplayName("Should detect unreachable trap")
    void shouldDetectUnreachableTrap() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: unreachable instruction executed");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.UNREACHABLE_CODE_REACHED,
          trap.getTrapType(),
          "Should be UNREACHABLE_CODE_REACHED type");
    }
  }

  @Nested
  @DisplayName("WASI Error Mapping Tests")
  class WasiErrorMappingTests {

    @Test
    @DisplayName("Should detect file not found errors")
    void shouldDetectFileNotFoundErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file not found: /path/to/file");

      assertInstanceOf(
          WasiFileSystemException.class, result, "Should return WasiFileSystemException");
    }

    @Test
    @DisplayName("Should detect permission denied errors")
    void shouldDetectPermissionDeniedErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "path permission denied");

      assertInstanceOf(
          WasiFileSystemException.class, result, "Should return WasiFileSystemException");
    }

    @Test
    @DisplayName("Should detect network errors")
    void shouldDetectNetworkErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "network connection failed");

      assertInstanceOf(WasiException.class, result, "Should return WasiException");
    }
  }

  @Nested
  @DisplayName("Linking Error Mapping Tests")
  class LinkingErrorMappingTests {

    @Test
    @DisplayName("Should detect import not found errors")
    void shouldDetectImportNotFoundErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "import not found: env.memory");

      assertInstanceOf(LinkingException.class, result, "Should return LinkingException");
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.IMPORT_NOT_FOUND,
          linking.getErrorType(),
          "Should be IMPORT_NOT_FOUND type");
    }

    @Test
    @DisplayName("Should detect function signature mismatch errors")
    void shouldDetectFunctionSignatureMismatchErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "function signature mismatch");

      assertInstanceOf(LinkingException.class, result, "Should return LinkingException");
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH,
          linking.getErrorType(),
          "Should be FUNCTION_SIGNATURE_MISMATCH type");
    }
  }
}
