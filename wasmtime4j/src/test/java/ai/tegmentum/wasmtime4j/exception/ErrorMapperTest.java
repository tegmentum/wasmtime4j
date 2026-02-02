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
    @DisplayName("Should detect out of memory errors with correct error type")
    void shouldDetectOutOfMemoryErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "out of memory during compilation");

      assertInstanceOf(
          ModuleCompilationException.class, result, "Should return ModuleCompilationException");
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.OUT_OF_MEMORY,
          ex.getErrorType(),
          "Should detect OUT_OF_MEMORY error type");
    }

    @Test
    @DisplayName("Should detect memory keyword in error message")
    void shouldDetectMemoryKeyword() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "insufficient memory available");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.OUT_OF_MEMORY,
          ex.getErrorType(),
          "Should detect OUT_OF_MEMORY from 'memory' keyword");
    }

    @Test
    @DisplayName("Should detect timeout errors with correct error type")
    void shouldDetectTimeoutErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "compilation timeout exceeded");

      assertInstanceOf(
          ModuleCompilationException.class, result, "Should return ModuleCompilationException");
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.TIMEOUT,
          ex.getErrorType(),
          "Should detect TIMEOUT error type");
    }

    @Test
    @DisplayName("Should detect time keyword in error message")
    void shouldDetectTimeKeyword() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "compilation time limit reached");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.TIMEOUT,
          ex.getErrorType(),
          "Should detect TIMEOUT from 'time' keyword");
    }

    @Test
    @DisplayName("Should detect too complex errors with correct error type")
    void shouldDetectTooComplexErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "function is too complex");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.FUNCTION_TOO_COMPLEX,
          ex.getErrorType(),
          "Should detect FUNCTION_TOO_COMPLEX error type");
    }

    @Test
    @DisplayName("Should detect complex keyword in error message")
    void shouldDetectComplexKeyword() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "highly complex control flow");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.FUNCTION_TOO_COMPLEX,
          ex.getErrorType(),
          "Should detect FUNCTION_TOO_COMPLEX from 'complex' keyword");
    }

    @Test
    @DisplayName("Should detect unsupported instruction errors with correct error type")
    void shouldDetectUnsupportedInstructionErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "unsupported instruction found");

      assertInstanceOf(
          ModuleCompilationException.class, result, "Should return ModuleCompilationException");
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.UNSUPPORTED_INSTRUCTION,
          ex.getErrorType(),
          "Should detect UNSUPPORTED_INSTRUCTION error type");
    }

    @Test
    @DisplayName("Should detect not supported keyword in error message")
    void shouldDetectNotSupportedKeyword() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "feature is not supported");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.UNSUPPORTED_INSTRUCTION,
          ex.getErrorType(),
          "Should detect UNSUPPORTED_INSTRUCTION from 'not supported' keyword");
    }

    @Test
    @DisplayName("Should detect optimization failed errors with correct error type and phase")
    void shouldDetectOptimizationFailedErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "optimization failed");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.OPTIMIZATION_FAILED,
          ex.getErrorType(),
          "Should detect OPTIMIZATION_FAILED error type");
      assertEquals(
          ModuleCompilationException.CompilationPhase.OPTIMIZATION,
          ex.getPhase(),
          "Should set OPTIMIZATION phase");
    }

    @Test
    @DisplayName("Should detect optimize keyword in error message")
    void shouldDetectOptimizeKeyword() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "failed to optimize function");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.OPTIMIZATION_FAILED,
          ex.getErrorType(),
          "Should detect OPTIMIZATION_FAILED from 'optimize' keyword");
    }

    @Test
    @DisplayName("Should detect register allocation failed errors with correct error type and phase")
    void shouldDetectRegisterAllocationFailedErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "register allocation failed");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.REGISTER_ALLOCATION_FAILED,
          ex.getErrorType(),
          "Should detect REGISTER_ALLOCATION_FAILED error type");
      assertEquals(
          ModuleCompilationException.CompilationPhase.REGISTER_ALLOCATION,
          ex.getPhase(),
          "Should set REGISTER_ALLOCATION phase");
    }

    @Test
    @DisplayName("Should detect allocation keyword in error message")
    void shouldDetectAllocationKeyword() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "allocation spill detected");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.REGISTER_ALLOCATION_FAILED,
          ex.getErrorType(),
          "Should detect REGISTER_ALLOCATION_FAILED from 'allocation' keyword");
    }

    @Test
    @DisplayName("Should detect code generation failed errors with correct error type and phase")
    void shouldDetectCodeGenerationFailedErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "code generation failed");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.CODE_GENERATION_FAILED,
          ex.getErrorType(),
          "Should detect CODE_GENERATION_FAILED error type");
      assertEquals(
          ModuleCompilationException.CompilationPhase.CODE_GENERATION,
          ex.getPhase(),
          "Should set CODE_GENERATION phase");
    }

    @Test
    @DisplayName("Should detect codegen keyword in error message")
    void shouldDetectCodegenKeyword() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "codegen error occurred");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.CODE_GENERATION_FAILED,
          ex.getErrorType(),
          "Should detect CODE_GENERATION_FAILED from 'codegen' keyword");
    }

    @Test
    @DisplayName("Should default to UNKNOWN error type for unrecognized messages")
    void shouldDefaultToUnknownErrorType() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "some random error xyz");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.UNKNOWN,
          ex.getErrorType(),
          "Should default to UNKNOWN error type");
    }

    @Test
    @DisplayName("Should extract function name from error message")
    void shouldExtractFunctionName() {
      final WasmException result =
          ErrorMapper.mapError(
              ErrorMapper.COMPILATION_ERROR, "error in function:add_numbers is too complex");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals("add_numbers", ex.getFunctionName(), "Should extract function name");
    }
  }

  @Nested
  @DisplayName("Validation Error Mapping Tests")
  class ValidationErrorMappingTests {

    @Test
    @DisplayName("Should detect magic number errors with correct error type")
    void shouldDetectMagicNumberErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "invalid magic number");

      assertInstanceOf(
          ModuleValidationException.class, result, "Should return ModuleValidationException");
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.INVALID_MAGIC_NUMBER,
          ex.getErrorType(),
          "Should detect INVALID_MAGIC_NUMBER error type");
    }

    @Test
    @DisplayName("Should detect version keyword in error message")
    void shouldDetectVersionKeyword() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "unsupported wasm version");

      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.INVALID_MAGIC_NUMBER,
          ex.getErrorType(),
          "Should detect INVALID_MAGIC_NUMBER from 'version' keyword");
    }

    @Test
    @DisplayName("Should detect malformed module errors with correct error type")
    void shouldDetectMalformedModuleErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "malformed module structure");

      assertInstanceOf(
          ModuleValidationException.class, result, "Should return ModuleValidationException");
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.MALFORMED_MODULE,
          ex.getErrorType(),
          "Should detect MALFORMED_MODULE error type");
    }

    @Test
    @DisplayName("Should detect corrupt keyword in error message")
    void shouldDetectCorruptKeyword() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "corrupt binary format");

      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.MALFORMED_MODULE,
          ex.getErrorType(),
          "Should detect MALFORMED_MODULE from 'corrupt' keyword");
    }

    @Test
    @DisplayName("Should detect type mismatch errors with correct error type")
    void shouldDetectTypeMismatchErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "type mismatch in function");

      assertInstanceOf(
          ModuleValidationException.class, result, "Should return ModuleValidationException");
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.TYPE_MISMATCH,
          ex.getErrorType(),
          "Should detect TYPE_MISMATCH error type");
    }

    @Test
    @DisplayName("Should detect type keyword alone in error message")
    void shouldDetectTypeKeyword() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "invalid type definition");

      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.TYPE_MISMATCH,
          ex.getErrorType(),
          "Should detect TYPE_MISMATCH from 'type' keyword");
    }

    @Test
    @DisplayName("Should detect import errors with correct error type")
    void shouldDetectImportErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "invalid import specification");

      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.INVALID_IMPORT,
          ex.getErrorType(),
          "Should detect INVALID_IMPORT error type");
    }

    @Test
    @DisplayName("Should detect export errors with correct error type")
    void shouldDetectExportErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "duplicate export name");

      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.INVALID_EXPORT,
          ex.getErrorType(),
          "Should detect INVALID_EXPORT error type");
    }

    @Test
    @DisplayName("Should detect memory definition errors with correct error type")
    void shouldDetectMemoryDefinitionErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "invalid memory limits");

      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.INVALID_MEMORY_DEFINITION,
          ex.getErrorType(),
          "Should detect INVALID_MEMORY_DEFINITION error type");
    }

    @Test
    @DisplayName("Should detect table definition errors with correct error type")
    void shouldDetectTableDefinitionErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "invalid table definition");

      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.INVALID_TABLE_DEFINITION,
          ex.getErrorType(),
          "Should detect INVALID_TABLE_DEFINITION error type");
    }

    @Test
    @DisplayName("Should detect function body errors with correct error type")
    void shouldDetectFunctionBodyErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "invalid function body");

      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.INVALID_FUNCTION_BODY,
          ex.getErrorType(),
          "Should detect INVALID_FUNCTION_BODY error type");
    }

    @Test
    @DisplayName("Should detect unsupported feature errors with correct error type")
    void shouldDetectUnsupportedFeatureErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "unsupported feature used");

      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.UNSUPPORTED_FEATURE,
          ex.getErrorType(),
          "Should detect UNSUPPORTED_FEATURE error type");
    }

    @Test
    @DisplayName("Should detect feature keyword in error message")
    void shouldDetectFeatureKeyword() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "experimental feature enabled");

      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.UNSUPPORTED_FEATURE,
          ex.getErrorType(),
          "Should detect UNSUPPORTED_FEATURE from 'feature' keyword");
    }

    @Test
    @DisplayName("Should detect limit exceeded errors with correct error type")
    void shouldDetectLimitExceededErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "limit exceeded for locals");

      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.LIMIT_EXCEEDED,
          ex.getErrorType(),
          "Should detect LIMIT_EXCEEDED error type");
    }

    @Test
    @DisplayName("Should detect exceed keyword in error message")
    void shouldDetectExceedKeyword() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "maximum size exceeded");

      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.LIMIT_EXCEEDED,
          ex.getErrorType(),
          "Should detect LIMIT_EXCEEDED from 'exceed' keyword");
    }

    @Test
    @DisplayName("Should default to UNKNOWN error type for unrecognized messages")
    void shouldDefaultToUnknownErrorType() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "some random xyz error");

      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.UNKNOWN,
          ex.getErrorType(),
          "Should default to UNKNOWN error type");
    }

    @Test
    @DisplayName("Should extract section name from error message")
    void shouldExtractSectionName() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "error in section:code is invalid");

      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals("code", ex.getModuleSection(), "Should extract section name");
    }

    @Test
    @DisplayName("Should extract byte offset from error message")
    void shouldExtractByteOffset() {
      final WasmException result =
          ErrorMapper.mapError(
              ErrorMapper.VALIDATION_ERROR, "validation error at offset 12345: invalid");

      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(Integer.valueOf(12345), ex.getByteOffset(), "Should extract byte offset");
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

    @Test
    @DisplayName("Should detect table out of bounds trap")
    void shouldDetectTableOutOfBoundsTrap() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: table out of bounds access");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.TABLE_OUT_OF_BOUNDS,
          trap.getTrapType(),
          "Should be TABLE_OUT_OF_BOUNDS type");
    }

    @Test
    @DisplayName("Should detect array out of bounds trap")
    void shouldDetectArrayOutOfBoundsTrap() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: array out of bounds access");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.ARRAY_OUT_OF_BOUNDS,
          trap.getTrapType(),
          "Should be ARRAY_OUT_OF_BOUNDS type");
    }

    @Test
    @DisplayName("Should detect heap misaligned trap from misaligned keyword")
    void shouldDetectHeapMisalignedTrapFromMisaligned() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: misaligned memory access");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.HEAP_MISALIGNED,
          trap.getTrapType(),
          "Should be HEAP_MISALIGNED type");
    }

    @Test
    @DisplayName("Should detect heap misaligned trap from alignment keyword")
    void shouldDetectHeapMisalignedTrapFromAlignment() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: alignment error");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.HEAP_MISALIGNED,
          trap.getTrapType(),
          "Should be HEAP_MISALIGNED type");
    }

    @Test
    @DisplayName("Should detect indirect call to null trap")
    void shouldDetectIndirectCallToNullTrap() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: null call target");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.INDIRECT_CALL_TO_NULL,
          trap.getTrapType(),
          "Should be INDIRECT_CALL_TO_NULL type");
    }

    @Test
    @DisplayName("Should detect null reference trap")
    void shouldDetectNullReferenceTrap() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: null reference access");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.NULL_REFERENCE,
          trap.getTrapType(),
          "Should be NULL_REFERENCE type");
    }

    @Test
    @DisplayName("Should detect bad signature trap from signature keyword")
    void shouldDetectBadSignatureTrapFromSignature() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: function signature mismatch");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.BAD_SIGNATURE,
          trap.getTrapType(),
          "Should be BAD_SIGNATURE type");
    }

    @Test
    @DisplayName("Should detect bad signature trap from type mismatch keyword")
    void shouldDetectBadSignatureTrapFromTypeMismatch() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: type mismatch in call");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.BAD_SIGNATURE,
          trap.getTrapType(),
          "Should be BAD_SIGNATURE type");
    }

    @Test
    @DisplayName("Should detect integer overflow trap")
    void shouldDetectIntegerOverflowTrap() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: integer overflow occurred");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.INTEGER_OVERFLOW,
          trap.getTrapType(),
          "Should be INTEGER_OVERFLOW type");
    }

    @Test
    @DisplayName("Should detect division by zero trap with divide keyword")
    void shouldDetectDivisionByZeroTrapWithDivideKeyword() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: divide by zero error");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.INTEGER_DIVISION_BY_ZERO,
          trap.getTrapType(),
          "Should be INTEGER_DIVISION_BY_ZERO type");
    }

    @Test
    @DisplayName("Should detect bad conversion trap from conversion keyword")
    void shouldDetectBadConversionTrapFromConversion() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: invalid conversion to integer");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.BAD_CONVERSION_TO_INTEGER,
          trap.getTrapType(),
          "Should be BAD_CONVERSION_TO_INTEGER type");
    }

    @Test
    @DisplayName("Should detect bad conversion trap from float keyword")
    void shouldDetectBadConversionTrapFromFloat() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: float to int error");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.BAD_CONVERSION_TO_INTEGER,
          trap.getTrapType(),
          "Should be BAD_CONVERSION_TO_INTEGER type");
    }

    @Test
    @DisplayName("Should detect interrupt trap")
    void shouldDetectInterruptTrap() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: execution interrupt");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.INTERRUPT,
          trap.getTrapType(),
          "Should be INTERRUPT type");
    }

    @Test
    @DisplayName("Should detect out of fuel trap from fuel keyword")
    void shouldDetectOutOfFuelTrapFromFuel() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: fuel depleted");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.OUT_OF_FUEL,
          trap.getTrapType(),
          "Should be OUT_OF_FUEL type");
    }

    @Test
    @DisplayName("Should detect out of fuel trap from out of fuel keyword")
    void shouldDetectOutOfFuelTrapFromOutOfFuel() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: out of fuel error");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.OUT_OF_FUEL,
          trap.getTrapType(),
          "Should be OUT_OF_FUEL type");
    }

    @Test
    @DisplayName("Should default to unknown trap type for unrecognized trap message")
    void shouldDefaultToUnknownTrapType() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: some unknown error xyz");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.UNKNOWN,
          trap.getTrapType(),
          "Should be UNKNOWN type for unrecognized messages");
    }

    @Test
    @DisplayName("Should extract function name from trap message")
    void shouldExtractFunctionNameFromTrapMessage() {
      final WasmException result =
          ErrorMapper.mapError(
              ErrorMapper.RUNTIME_ERROR, "trap: stack overflow in function:my_recursive_func");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          "my_recursive_func", trap.getFunctionName(), "Should extract function name from trap");
    }

    @Test
    @DisplayName("Should extract instruction offset from trap message")
    void shouldExtractInstructionOffsetFromTrapMessage() {
      final WasmException result =
          ErrorMapper.mapError(
              ErrorMapper.RUNTIME_ERROR, "trap: memory out of bounds at offset 12345");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          Integer.valueOf(12345),
          trap.getInstructionOffset(),
          "Should extract instruction offset from trap");
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
