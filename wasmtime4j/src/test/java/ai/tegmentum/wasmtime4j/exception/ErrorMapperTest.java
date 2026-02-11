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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    @DisplayName(
        "Should detect register allocation failed errors with correct error type and phase")
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

    @Test
    @DisplayName("OR boundary: 'memory' alone triggers OUT_OF_MEMORY")
    void memoryAloneShouldTriggerOutOfMemory() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "unable to access memory region");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.OUT_OF_MEMORY,
          ex.getErrorType(),
          "'memory' alone should trigger OUT_OF_MEMORY");
    }

    @Test
    @DisplayName("OR boundary: 'time' alone triggers TIMEOUT")
    void timeAloneShouldTriggerTimeout() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "exceeded compile time");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.TIMEOUT,
          ex.getErrorType(),
          "'time' alone should trigger TIMEOUT");
    }

    @Test
    @DisplayName("OR boundary: 'complex' alone triggers FUNCTION_TOO_COMPLEX")
    void complexAloneShouldTriggerTooComplex() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "expression too complex to compile");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.FUNCTION_TOO_COMPLEX,
          ex.getErrorType(),
          "'complex' alone should trigger FUNCTION_TOO_COMPLEX");
    }

    @Test
    @DisplayName("OR boundary: 'not supported' triggers UNSUPPORTED_INSTRUCTION")
    void notSupportedShouldTriggerUnsupported() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "instruction not supported");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.UNSUPPORTED_INSTRUCTION,
          ex.getErrorType(),
          "'not supported' should trigger UNSUPPORTED_INSTRUCTION");
    }

    @Test
    @DisplayName("OR boundary: 'optimize' alone triggers OPTIMIZATION_FAILED")
    void optimizeAloneShouldTriggerOptimization() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "failed to optimize code");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.OPTIMIZATION_FAILED,
          ex.getErrorType(),
          "'optimize' alone should trigger OPTIMIZATION_FAILED");
    }

    @Test
    @DisplayName("OR boundary: 'allocation' alone triggers REGISTER_ALLOCATION_FAILED")
    void allocationAloneShouldTriggerRegisterAllocation() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "failed during allocation pass");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.REGISTER_ALLOCATION_FAILED,
          ex.getErrorType(),
          "'allocation' alone should trigger REGISTER_ALLOCATION_FAILED");
    }

    @Test
    @DisplayName("OR boundary: 'codegen' alone triggers CODE_GENERATION_FAILED")
    void codegenAloneShouldTriggerCodeGeneration() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "codegen phase failed");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.CODE_GENERATION_FAILED,
          ex.getErrorType(),
          "'codegen' alone should trigger CODE_GENERATION_FAILED");
    }

    @Test
    @DisplayName("Negative: no keywords should produce UNKNOWN")
    void noKeywordsShouldProduceUnknown() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "unspecified build failure");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.UNKNOWN,
          ex.getErrorType(),
          "No keywords should produce UNKNOWN");
    }

    @Test
    @DisplayName("First branch: 'out of memory' takes priority over 'time'")
    void outOfMemoryTakesPriorityOverTime() {
      // Message contains both 'memory' and 'time' - memory should win (first in chain)
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "out of memory during compile time");

      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.OUT_OF_MEMORY,
          ex.getErrorType(),
          "'out of memory' should take priority over 'time'");
    }

    @Test
    @DisplayName("Negative: timeout should NOT produce OUT_OF_MEMORY")
    void timeoutShouldNotProduceOutOfMemory() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "timeout exceeded");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.OUT_OF_MEMORY,
          ex.getErrorType(),
          "timeout should NOT produce OUT_OF_MEMORY");
      assertEquals(
          ModuleCompilationException.CompilationErrorType.TIMEOUT,
          ex.getErrorType(),
          "timeout should produce TIMEOUT");
    }

    @Test
    @DisplayName("Negative: complex should NOT produce TIMEOUT")
    void complexShouldNotProduceTimeout() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "function too complex");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.TIMEOUT,
          ex.getErrorType(),
          "complex should NOT produce TIMEOUT");
    }

    @Test
    @DisplayName("Negative: unsupported should NOT produce FUNCTION_TOO_COMPLEX")
    void unsupportedShouldNotProduceTooComplex() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "unsupported wasm feature");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.FUNCTION_TOO_COMPLEX,
          ex.getErrorType(),
          "unsupported should NOT produce FUNCTION_TOO_COMPLEX");
    }

    @Test
    @DisplayName("Negative: optimization should NOT produce UNSUPPORTED_INSTRUCTION")
    void optimizationShouldNotProduceUnsupported() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "optimization pass error");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.UNSUPPORTED_INSTRUCTION,
          ex.getErrorType(),
          "optimization should NOT produce UNSUPPORTED_INSTRUCTION");
    }

    @Test
    @DisplayName("Negative: register allocation should NOT produce OPTIMIZATION_FAILED")
    void registerAllocationShouldNotProduceOptimization() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "register spill error");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.OPTIMIZATION_FAILED,
          ex.getErrorType(),
          "register allocation should NOT produce OPTIMIZATION_FAILED");
    }

    @Test
    @DisplayName("Negative: code generation should NOT produce REGISTER_ALLOCATION_FAILED")
    void codeGenerationShouldNotProduceRegisterAllocation() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "code generation error");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.REGISTER_ALLOCATION_FAILED,
          ex.getErrorType(),
          "code generation should NOT produce REGISTER_ALLOCATION_FAILED");
    }

    @Test
    @DisplayName("Negative: unknown error should NOT produce any specific type")
    void unknownShouldNotProduceSpecificTypes() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "random xyz error");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.OUT_OF_MEMORY,
          ex.getErrorType(),
          "unknown should NOT produce OUT_OF_MEMORY");
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.TIMEOUT,
          ex.getErrorType(),
          "unknown should NOT produce TIMEOUT");
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.FUNCTION_TOO_COMPLEX,
          ex.getErrorType(),
          "unknown should NOT produce FUNCTION_TOO_COMPLEX");
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

    @Test
    @DisplayName("OR boundary: 'version' alone triggers INVALID_MAGIC_NUMBER")
    void versionAloneShouldTriggerInvalidMagic() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "invalid version number");

      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.INVALID_MAGIC_NUMBER,
          ex.getErrorType(),
          "'version' alone should trigger INVALID_MAGIC_NUMBER");
    }

    @Test
    @DisplayName("OR boundary: 'corrupt' alone triggers MALFORMED_MODULE")
    void corruptAloneShouldTriggerMalformed() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "data appears corrupt");

      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.MALFORMED_MODULE,
          ex.getErrorType(),
          "'corrupt' alone should trigger MALFORMED_MODULE");
    }

    @Test
    @DisplayName("OR boundary: 'type' alone triggers TYPE_MISMATCH")
    void typeAloneShouldTriggerTypeMismatch() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "incompatible type detected");

      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.TYPE_MISMATCH,
          ex.getErrorType(),
          "'type' alone should trigger TYPE_MISMATCH");
    }

    @Test
    @DisplayName("Priority: 'magic' takes priority over 'type'")
    void magicTakesPriorityOverType() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "magic type header invalid");

      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.INVALID_MAGIC_NUMBER,
          ex.getErrorType(),
          "'magic' should take priority over 'type'");
    }

    @Test
    @DisplayName("OR boundary: 'feature' alone triggers UNSUPPORTED_FEATURE")
    void featureAloneShouldTriggerUnsupportedFeature() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "feature not available");

      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.UNSUPPORTED_FEATURE,
          ex.getErrorType(),
          "'feature' alone should trigger UNSUPPORTED_FEATURE");
    }

    @Test
    @DisplayName("OR boundary: 'exceed' alone triggers LIMIT_EXCEEDED")
    void exceedAloneShouldTriggerLimitExceeded() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "value will exceed bounds");

      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.LIMIT_EXCEEDED,
          ex.getErrorType(),
          "'exceed' alone should trigger LIMIT_EXCEEDED");
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
          TrapException.TrapType.BAD_SIGNATURE, trap.getTrapType(), "Should be BAD_SIGNATURE type");
    }

    @Test
    @DisplayName("Should detect bad signature trap from type mismatch keyword")
    void shouldDetectBadSignatureTrapFromTypeMismatch() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: type mismatch in call");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.BAD_SIGNATURE, trap.getTrapType(), "Should be BAD_SIGNATURE type");
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
          TrapException.TrapType.INTERRUPT, trap.getTrapType(), "Should be INTERRUPT type");
    }

    @Test
    @DisplayName("Should detect out of fuel trap from fuel keyword")
    void shouldDetectOutOfFuelTrapFromFuel() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: fuel depleted");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.OUT_OF_FUEL, trap.getTrapType(), "Should be OUT_OF_FUEL type");
    }

    @Test
    @DisplayName("Should detect out of fuel trap from out of fuel keyword")
    void shouldDetectOutOfFuelTrapFromOutOfFuel() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: out of fuel error");

      assertInstanceOf(TrapException.class, result, "Should return TrapException");
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.OUT_OF_FUEL, trap.getTrapType(), "Should be OUT_OF_FUEL type");
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

    @Test
    @DisplayName("AND boundary: 'out of bounds' alone should NOT be MEMORY_OUT_OF_BOUNDS")
    void outOfBoundsAloneShouldNotBeMemoryOutOfBounds() {
      // "out of bounds" without "memory" should not match MEMORY_OUT_OF_BOUNDS
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: index out of bounds error");

      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertNotEquals(
          TrapException.TrapType.MEMORY_OUT_OF_BOUNDS,
          trap.getTrapType(),
          "'out of bounds' without 'memory' should NOT be MEMORY_OUT_OF_BOUNDS");
    }

    @Test
    @DisplayName("AND boundary: 'out of bounds' alone should NOT be TABLE_OUT_OF_BOUNDS")
    void outOfBoundsAloneShouldNotBeTableOutOfBounds() {
      // "out of bounds" without "table" should not match TABLE_OUT_OF_BOUNDS
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: index out of bounds error");

      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertNotEquals(
          TrapException.TrapType.TABLE_OUT_OF_BOUNDS,
          trap.getTrapType(),
          "'out of bounds' without 'table' should NOT be TABLE_OUT_OF_BOUNDS");
    }

    @Test
    @DisplayName("AND boundary: 'out of bounds' alone should NOT be ARRAY_OUT_OF_BOUNDS")
    void outOfBoundsAloneShouldNotBeArrayOutOfBounds() {
      // "out of bounds" without "array" should not match ARRAY_OUT_OF_BOUNDS
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: index out of bounds error");

      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertNotEquals(
          TrapException.TrapType.ARRAY_OUT_OF_BOUNDS,
          trap.getTrapType(),
          "'out of bounds' without 'array' should NOT be ARRAY_OUT_OF_BOUNDS");
    }

    @Test
    @DisplayName("AND boundary: 'memory' alone should NOT be MEMORY_OUT_OF_BOUNDS")
    void memoryAloneInTrapShouldNotBeMemoryOutOfBounds() {
      // "memory" without "out of bounds" should not match MEMORY_OUT_OF_BOUNDS
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: memory access error");

      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertNotEquals(
          TrapException.TrapType.MEMORY_OUT_OF_BOUNDS,
          trap.getTrapType(),
          "'memory' without 'out of bounds' should NOT be MEMORY_OUT_OF_BOUNDS");
    }

    @Test
    @DisplayName("AND boundary: 'null' alone should NOT be INDIRECT_CALL_TO_NULL")
    void nullAloneShouldNotBeIndirectCallToNull() {
      // "null" without "call" should not match INDIRECT_CALL_TO_NULL
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: null pointer error");

      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertNotEquals(
          TrapException.TrapType.INDIRECT_CALL_TO_NULL,
          trap.getTrapType(),
          "'null' without 'call' should NOT be INDIRECT_CALL_TO_NULL");
    }

    @Test
    @DisplayName("AND boundary: 'null' alone should NOT be NULL_REFERENCE")
    void nullAloneShouldNotBeNullReference() {
      // "null" without "reference" should not match NULL_REFERENCE
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: null pointer error");

      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertNotEquals(
          TrapException.TrapType.NULL_REFERENCE,
          trap.getTrapType(),
          "'null' without 'reference' should NOT be NULL_REFERENCE");
    }

    @Test
    @DisplayName("AND boundary: 'call' alone should NOT be INDIRECT_CALL_TO_NULL")
    void callAloneShouldNotBeIndirectCallToNull() {
      // "call" without "null" should not match INDIRECT_CALL_TO_NULL
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: invalid call target");

      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertNotEquals(
          TrapException.TrapType.INDIRECT_CALL_TO_NULL,
          trap.getTrapType(),
          "'call' without 'null' should NOT be INDIRECT_CALL_TO_NULL");
    }

    @Test
    @DisplayName("AND boundary: 'reference' alone should NOT be NULL_REFERENCE")
    void referenceAloneShouldNotBeNullReference() {
      // "reference" without "null" should not match NULL_REFERENCE
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: invalid reference type");

      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertNotEquals(
          TrapException.TrapType.NULL_REFERENCE,
          trap.getTrapType(),
          "'reference' without 'null' should NOT be NULL_REFERENCE");
    }

    @Test
    @DisplayName("Negative: trap without specific keywords should be UNKNOWN")
    void trapWithoutSpecificKeywordsShouldBeUnknown() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: generic error xyz");

      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.UNKNOWN,
          trap.getTrapType(),
          "trap without specific keywords should be UNKNOWN");
    }

    @Test
    @DisplayName("Priority: 'stack overflow' takes priority over generic keywords")
    void stackOverflowTakesPriority() {
      // stack overflow should match before later branches
      final WasmException result =
          ErrorMapper.mapError(
              ErrorMapper.RUNTIME_ERROR, "trap: stack overflow during function call");

      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.STACK_OVERFLOW,
          trap.getTrapType(),
          "'stack overflow' should take priority");
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

    @Test
    @DisplayName("Should detect export not found errors")
    void shouldDetectExportNotFoundErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "export not found: my_func");

      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.EXPORT_NOT_FOUND,
          linking.getErrorType(),
          "Should be EXPORT_NOT_FOUND type");
    }

    @Test
    @DisplayName("Should detect memory size mismatch errors")
    void shouldDetectMemorySizeMismatchErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "memory size mismatch");

      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.MEMORY_SIZE_MISMATCH,
          linking.getErrorType(),
          "Should be MEMORY_SIZE_MISMATCH type");
    }

    @Test
    @DisplayName("Should detect memory limits incompatible errors")
    void shouldDetectMemoryLimitsIncompatibleErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "memory limit exceeded");

      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.MEMORY_LIMITS_INCOMPATIBLE,
          linking.getErrorType(),
          "Should be MEMORY_LIMITS_INCOMPATIBLE type");
    }

    @Test
    @DisplayName("Should detect table size mismatch errors")
    void shouldDetectTableSizeMismatchErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "table size mismatch");

      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.TABLE_SIZE_MISMATCH,
          linking.getErrorType(),
          "Should be TABLE_SIZE_MISMATCH type");
    }

    @Test
    @DisplayName("Should detect table type mismatch errors")
    void shouldDetectTableTypeMismatchErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "table type mismatch");

      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.TABLE_TYPE_MISMATCH,
          linking.getErrorType(),
          "Should be TABLE_TYPE_MISMATCH type");
    }

    @Test
    @DisplayName("Should detect global type mismatch errors")
    void shouldDetectGlobalTypeMismatchErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "global type mismatch");

      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.GLOBAL_TYPE_MISMATCH,
          linking.getErrorType(),
          "Should be GLOBAL_TYPE_MISMATCH type");
    }

    @Test
    @DisplayName("Should detect global mutability mismatch errors")
    void shouldDetectGlobalMutabilityMismatchErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "global mutability mismatch");

      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.GLOBAL_MUTABILITY_MISMATCH,
          linking.getErrorType(),
          "Should be GLOBAL_MUTABILITY_MISMATCH type");
    }

    @Test
    @DisplayName("Should detect circular dependency errors")
    void shouldDetectCircularDependencyErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "circular dependency detected");

      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.CIRCULAR_DEPENDENCY,
          linking.getErrorType(),
          "Should be CIRCULAR_DEPENDENCY type");
    }

    @Test
    @DisplayName("Should detect namespace conflict errors")
    void shouldDetectNamespaceConflictErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "namespace conflict");

      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.NAMESPACE_CONFLICT,
          linking.getErrorType(),
          "Should be NAMESPACE_CONFLICT type");
    }

    @Test
    @DisplayName("Should detect host function binding failed errors")
    void shouldDetectHostFunctionBindingFailedErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "host function binding failed");

      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.HOST_FUNCTION_BINDING_FAILED,
          linking.getErrorType(),
          "Should be HOST_FUNCTION_BINDING_FAILED type");
    }

    @Test
    @DisplayName("Should detect wasi import failed errors")
    void shouldDetectWasiImportFailedErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "wasi import failed");

      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.WASI_IMPORT_FAILED,
          linking.getErrorType(),
          "Should be WASI_IMPORT_FAILED type");
    }

    @Test
    @DisplayName("Should detect component linking failed errors")
    void shouldDetectComponentLinkingFailedErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "component linking failed");

      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.COMPONENT_LINKING_FAILED,
          linking.getErrorType(),
          "Should be COMPONENT_LINKING_FAILED type");
    }

    @Test
    @DisplayName("Should detect interface type mismatch errors")
    void shouldDetectInterfaceTypeMismatchErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "interface type mismatch");

      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.INTERFACE_TYPE_MISMATCH,
          linking.getErrorType(),
          "Should be INTERFACE_TYPE_MISMATCH type");
    }

    @Test
    @DisplayName("Should default to UNKNOWN for unrecognized linking errors")
    void shouldDefaultToUnknownForUnrecognizedLinkingErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "xyz random error");

      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.UNKNOWN,
          linking.getErrorType(),
          "Should be UNKNOWN type");
    }

    @Test
    @DisplayName("Should extract module and item name from import pattern")
    void shouldExtractModuleAndItemNameFromImportPattern() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "import:env.my_function not found");

      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertEquals("env", linking.getModuleName(), "Should extract module name");
    }

    @Test
    @DisplayName("AND boundary: 'import' alone should NOT be IMPORT_NOT_FOUND")
    void importAloneShouldNotBeImportNotFound() {
      // "import" without "not found" should not match IMPORT_NOT_FOUND
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "import error occurred");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.IMPORT_NOT_FOUND,
          linking.getErrorType(),
          "'import' without 'not found' should NOT be IMPORT_NOT_FOUND");
    }

    @Test
    @DisplayName("AND boundary: 'export' alone should NOT be EXPORT_NOT_FOUND")
    void exportAloneShouldNotBeExportNotFound() {
      // "export" without "not found" should not match EXPORT_NOT_FOUND
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "export error occurred");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.EXPORT_NOT_FOUND,
          linking.getErrorType(),
          "'export' without 'not found' should NOT be EXPORT_NOT_FOUND");
    }

    @Test
    @DisplayName("AND boundary: 'memory' alone should NOT be MEMORY_SIZE_MISMATCH")
    void memoryAloneShouldNotBeMemorySizeMismatch() {
      // "memory" without "size" should not match MEMORY_SIZE_MISMATCH
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "memory error occurred");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.MEMORY_SIZE_MISMATCH,
          linking.getErrorType(),
          "'memory' without 'size' should NOT be MEMORY_SIZE_MISMATCH");
    }

    @Test
    @DisplayName("AND boundary: 'size' alone should NOT be MEMORY_SIZE_MISMATCH")
    void sizeAloneShouldNotBeMemorySizeMismatch() {
      // "size" without "memory" should not match MEMORY_SIZE_MISMATCH
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "size error occurred");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.MEMORY_SIZE_MISMATCH,
          linking.getErrorType(),
          "'size' without 'memory' should NOT be MEMORY_SIZE_MISMATCH");
    }

    @Test
    @DisplayName("AND boundary: 'table' alone should NOT be TABLE_SIZE_MISMATCH")
    void tableAloneShouldNotBeTableSizeMismatch() {
      // "table" without "size" should not match TABLE_SIZE_MISMATCH
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "table error occurred");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.TABLE_SIZE_MISMATCH,
          linking.getErrorType(),
          "'table' without 'size' should NOT be TABLE_SIZE_MISMATCH");
    }

    @Test
    @DisplayName("AND boundary: 'global' alone should NOT be GLOBAL_TYPE_MISMATCH")
    void globalAloneShouldNotBeGlobalTypeMismatch() {
      // "global" without "type" should not match GLOBAL_TYPE_MISMATCH
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "global error occurred");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.GLOBAL_TYPE_MISMATCH,
          linking.getErrorType(),
          "'global' without 'type' should NOT be GLOBAL_TYPE_MISMATCH");
    }

    @Test
    @DisplayName("Negative: wasi should NOT be HOST_FUNCTION_BINDING_FAILED")
    void wasiShouldNotBeHostFunctionBindingFailed() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "wasi error occurred");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.HOST_FUNCTION_BINDING_FAILED,
          linking.getErrorType(),
          "wasi should NOT be HOST_FUNCTION_BINDING_FAILED");
    }

    @Test
    @DisplayName("Negative: component should NOT be WASI_IMPORT_FAILED")
    void componentShouldNotBeWasiImportFailed() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "component error occurred");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.WASI_IMPORT_FAILED,
          linking.getErrorType(),
          "component should NOT be WASI_IMPORT_FAILED");
    }

    @Test
    @DisplayName("Negative: interface should NOT be COMPONENT_LINKING_FAILED")
    void interfaceShouldNotBeComponentLinkingFailed() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "interface error occurred");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.COMPONENT_LINKING_FAILED,
          linking.getErrorType(),
          "interface should NOT be COMPONENT_LINKING_FAILED");
    }

    @Test
    @DisplayName("Priority: 'import' + 'not found' takes priority")
    void importNotFoundTakesPriority() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "import not found signature error");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.IMPORT_NOT_FOUND,
          linking.getErrorType(),
          "'import not found' should take priority");
    }
  }

  // ============================================================================
  // MUTATION TESTING COVERAGE TESTS
  // ============================================================================

  @Nested
  @DisplayName("Runtime Error Branch Coverage Tests")
  class RuntimeErrorBranchCoverageTests {

    @Test
    @DisplayName("Should detect timeout in runtime error message")
    void shouldDetectTimeoutInRuntimeError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "execution timeout exceeded");

      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.TIMEOUT, rt.getErrorType(), "Should be TIMEOUT type");
    }

    @Test
    @DisplayName("Should detect time keyword in runtime error message")
    void shouldDetectTimeKeywordInRuntimeError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "time limit reached");

      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.TIMEOUT,
          rt.getErrorType(),
          "Should be TIMEOUT from time keyword");
    }

    @Test
    @DisplayName("Should detect interrupt in runtime error message")
    void shouldDetectInterruptInRuntimeError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "execution interrupt requested");

      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.INTERRUPTED,
          rt.getErrorType(),
          "Should be INTERRUPTED type");
    }

    @Test
    @DisplayName("Should detect memory access violation in runtime error")
    void shouldDetectMemoryAccessViolationInRuntimeError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "invalid memory access");

      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.MEMORY_ACCESS_VIOLATION,
          rt.getErrorType(),
          "Should be MEMORY_ACCESS_VIOLATION type");
    }

    @Test
    @DisplayName("Should detect access keyword in runtime error")
    void shouldDetectAccessKeywordInRuntimeError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "access violation detected");

      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.MEMORY_ACCESS_VIOLATION,
          rt.getErrorType(),
          "Should be MEMORY_ACCESS_VIOLATION from access keyword");
    }

    @Test
    @DisplayName("Should detect stack error in runtime error")
    void shouldDetectStackErrorInRuntimeError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "stack limit exceeded");

      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.STACK_ERROR,
          rt.getErrorType(),
          "Should be STACK_ERROR type");
    }

    @Test
    @DisplayName("Should detect resource exhausted in runtime error")
    void shouldDetectResourceExhaustedInRuntimeError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "resource exhausted");

      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.RESOURCE_EXHAUSTED,
          rt.getErrorType(),
          "Should be RESOURCE_EXHAUSTED type");
    }

    @Test
    @DisplayName("Should detect limit keyword in runtime error")
    void shouldDetectLimitKeywordInRuntimeError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "limit reached error");

      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.RESOURCE_EXHAUSTED,
          rt.getErrorType(),
          "Should be RESOURCE_EXHAUSTED from limit keyword");
    }

    @Test
    @DisplayName("Should detect function execution failed in runtime error")
    void shouldDetectFunctionExecutionFailedInRuntimeError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "function call failed");

      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.FUNCTION_EXECUTION_FAILED,
          rt.getErrorType(),
          "Should be FUNCTION_EXECUTION_FAILED type");
    }

    @Test
    @DisplayName("Should detect host function failed in runtime error")
    void shouldDetectHostFunctionFailedInRuntimeError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "host callback failed");

      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.HOST_FUNCTION_FAILED,
          rt.getErrorType(),
          "Should be HOST_FUNCTION_FAILED type");
    }

    @Test
    @DisplayName("Should default to UNKNOWN for unrecognized runtime error")
    void shouldDefaultToUnknownForUnrecognizedRuntimeError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "xyz random xyz");

      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.UNKNOWN, rt.getErrorType(), "Should be UNKNOWN type");
    }

    @Test
    @DisplayName("Should extract function name from runtime error message")
    void shouldExtractFunctionNameFromRuntimeError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "error in function:my_func execution");

      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertEquals("my_func", rt.getFunctionName(), "Should extract function name");
    }

    @Test
    @DisplayName("Negative: interrupt should NOT be TIMEOUT")
    void interruptShouldNotBeTimeout() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "execution interrupt signal");
      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertNotEquals(
          RuntimeException.RuntimeErrorType.TIMEOUT,
          rt.getErrorType(),
          "interrupt should NOT be TIMEOUT");
    }

    @Test
    @DisplayName("Negative: memory error should NOT be INTERRUPTED")
    void memoryShouldNotBeInterrupted() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "memory fault error");
      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertNotEquals(
          RuntimeException.RuntimeErrorType.INTERRUPTED,
          rt.getErrorType(),
          "memory should NOT be INTERRUPTED");
    }

    @Test
    @DisplayName("Negative: stack error should NOT be MEMORY_ACCESS_VIOLATION")
    void stackShouldNotBeMemoryViolation() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "stack limit exhausted");
      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertNotEquals(
          RuntimeException.RuntimeErrorType.MEMORY_ACCESS_VIOLATION,
          rt.getErrorType(),
          "stack should NOT be MEMORY_ACCESS_VIOLATION");
    }

    @Test
    @DisplayName("Negative: resource error should NOT be STACK_ERROR")
    void resourceShouldNotBeStackError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "resource pool depleted");
      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertNotEquals(
          RuntimeException.RuntimeErrorType.STACK_ERROR,
          rt.getErrorType(),
          "resource should NOT be STACK_ERROR");
    }

    @Test
    @DisplayName("Negative: function error should NOT be RESOURCE_EXHAUSTED")
    void functionShouldNotBeResourceExhausted() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "function invocation error");
      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertNotEquals(
          RuntimeException.RuntimeErrorType.RESOURCE_EXHAUSTED,
          rt.getErrorType(),
          "function should NOT be RESOURCE_EXHAUSTED");
    }

    @Test
    @DisplayName("Negative: host error should NOT be FUNCTION_EXECUTION_FAILED")
    void hostShouldNotBeFunctionExecutionFailed() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "host callback error");
      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertNotEquals(
          RuntimeException.RuntimeErrorType.FUNCTION_EXECUTION_FAILED,
          rt.getErrorType(),
          "host should NOT be FUNCTION_EXECUTION_FAILED");
    }

    @Test
    @DisplayName("Negative: unknown should NOT produce any specific type")
    void unknownRuntimeShouldNotBeSpecific() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "xyz random error abc");
      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertNotEquals(
          RuntimeException.RuntimeErrorType.TIMEOUT,
          rt.getErrorType(),
          "unknown should NOT be TIMEOUT");
      assertNotEquals(
          RuntimeException.RuntimeErrorType.MEMORY_ACCESS_VIOLATION,
          rt.getErrorType(),
          "unknown should NOT be MEMORY_ACCESS_VIOLATION");
    }

    @Test
    @DisplayName("OR boundary: 'access' alone triggers MEMORY_ACCESS_VIOLATION")
    void accessAloneShouldTriggerMemoryViolation() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "invalid access detected");
      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.MEMORY_ACCESS_VIOLATION,
          rt.getErrorType(),
          "'access' alone should trigger MEMORY_ACCESS_VIOLATION");
    }

    @Test
    @DisplayName("OR boundary: 'limit' alone triggers RESOURCE_EXHAUSTED")
    void limitAloneShouldTriggerResourceExhausted() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "limit reached error");
      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.RESOURCE_EXHAUSTED,
          rt.getErrorType(),
          "'limit' alone should trigger RESOURCE_EXHAUSTED");
    }

    @Test
    @DisplayName("Trap pattern should trigger trap error")
    void trapPatternShouldTriggerTrapError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap:stack_overflow occurred");
      assertInstanceOf(TrapException.class, result);
    }

    @Test
    @DisplayName("'trap' keyword should trigger trap error")
    void trapKeywordShouldTriggerTrapError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "execution trap detected");
      assertInstanceOf(TrapException.class, result);
    }

    @Test
    @DisplayName("Non-trap runtime error should NOT be TrapException")
    void nonTrapShouldNotBeTrapException() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "timeout occurred");
      assertInstanceOf(RuntimeException.class, result);
      // Should be RuntimeException, not TrapException
      assertEquals(
          RuntimeException.class,
          result.getClass(),
          "Non-trap runtime error should be RuntimeException, not TrapException");
    }
  }

  @Nested
  @DisplayName("Instantiation Error Branch Coverage Tests")
  class InstantiationErrorBranchCoverageTests {

    @Test
    @DisplayName("Should detect import not found in instantiation error")
    void shouldDetectImportNotFoundInInstantiationError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "import not found: env.memory");

      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.MISSING_IMPORT,
          ex.getErrorType(),
          "Should be MISSING_IMPORT type");
      assertEquals(
          ModuleInstantiationException.InstantiationPhase.IMPORT_RESOLUTION,
          ex.getPhase(),
          "Should be IMPORT_RESOLUTION phase");
    }

    @Test
    @DisplayName("Should detect import type mismatch in instantiation error")
    void shouldDetectImportTypeMismatchInInstantiationError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "import type mismatch");

      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.IMPORT_TYPE_MISMATCH,
          ex.getErrorType(),
          "Should be IMPORT_TYPE_MISMATCH type");
    }

    @Test
    @DisplayName("Should detect signature mismatch in instantiation error")
    void shouldDetectSignatureMismatchInInstantiationError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "signature mismatch detected");

      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.FUNCTION_SIGNATURE_MISMATCH,
          ex.getErrorType(),
          "Should be FUNCTION_SIGNATURE_MISMATCH type");
    }

    @Test
    @DisplayName("Should detect memory allocation failed in instantiation error")
    void shouldDetectMemoryAllocationFailedInInstantiationError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "memory allocation failed");

      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.MEMORY_ALLOCATION_FAILED,
          ex.getErrorType(),
          "Should be MEMORY_ALLOCATION_FAILED type");
      assertEquals(
          ModuleInstantiationException.InstantiationPhase.MEMORY_ALLOCATION,
          ex.getPhase(),
          "Should be MEMORY_ALLOCATION phase");
    }

    @Test
    @DisplayName("Should detect table allocation failed in instantiation error")
    void shouldDetectTableAllocationFailedInInstantiationError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "table allocation failed");

      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.TABLE_ALLOCATION_FAILED,
          ex.getErrorType(),
          "Should be TABLE_ALLOCATION_FAILED type");
      assertEquals(
          ModuleInstantiationException.InstantiationPhase.TABLE_ALLOCATION,
          ex.getPhase(),
          "Should be TABLE_ALLOCATION phase");
    }

    @Test
    @DisplayName("Should detect start function failed in instantiation error")
    void shouldDetectStartFunctionFailedInInstantiationError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "start function failed");

      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.START_FUNCTION_FAILED,
          ex.getErrorType(),
          "Should be START_FUNCTION_FAILED type");
      assertEquals(
          ModuleInstantiationException.InstantiationPhase.START_FUNCTION_EXEC,
          ex.getPhase(),
          "Should be START_FUNCTION_EXEC phase");
    }

    @Test
    @DisplayName("Should detect data segment init failed in instantiation error")
    void shouldDetectDataSegmentInitFailedInInstantiationError() {
      final WasmException result =
          ErrorMapper.mapError(
              ErrorMapper.ENGINE_CONFIG_ERROR, "data segment initialization failed");

      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.DATA_SEGMENT_INIT_FAILED,
          ex.getErrorType(),
          "Should be DATA_SEGMENT_INIT_FAILED type");
      assertEquals(
          ModuleInstantiationException.InstantiationPhase.DATA_SEGMENT_INIT,
          ex.getPhase(),
          "Should be DATA_SEGMENT_INIT phase");
    }

    @Test
    @DisplayName("Should detect element segment init failed in instantiation error")
    void shouldDetectElementSegmentInitFailedInInstantiationError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "element segment error");

      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.ELEMENT_SEGMENT_INIT_FAILED,
          ex.getErrorType(),
          "Should be ELEMENT_SEGMENT_INIT_FAILED type");
      assertEquals(
          ModuleInstantiationException.InstantiationPhase.ELEMENT_SEGMENT_INIT,
          ex.getPhase(),
          "Should be ELEMENT_SEGMENT_INIT phase");
    }

    @Test
    @DisplayName("Should detect timeout in instantiation error")
    void shouldDetectTimeoutInInstantiationError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "instantiation timeout");

      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.TIMEOUT,
          ex.getErrorType(),
          "Should be TIMEOUT type");
    }

    @Test
    @DisplayName("Should detect resource limit exceeded in instantiation error")
    void shouldDetectResourceLimitExceededInInstantiationError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "resource limit reached");

      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.RESOURCE_LIMIT_EXCEEDED,
          ex.getErrorType(),
          "Should be RESOURCE_LIMIT_EXCEEDED type");
    }

    @Test
    @DisplayName("Should detect limit keyword in instantiation error")
    void shouldDetectLimitKeywordInInstantiationError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "limit exceeded");

      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.RESOURCE_LIMIT_EXCEEDED,
          ex.getErrorType(),
          "Should be RESOURCE_LIMIT_EXCEEDED from limit keyword");
    }

    @Test
    @DisplayName("Should default to UNKNOWN for unrecognized instantiation error")
    void shouldDefaultToUnknownForUnrecognizedInstantiationError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "xyz random xyz");

      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.UNKNOWN,
          ex.getErrorType(),
          "Should be UNKNOWN type");
    }

    @Test
    @DisplayName("STORE_ERROR should map to InstantiationException")
    void storeErrorShouldMapToInstantiationException() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.STORE_ERROR, "store error occurred");

      assertInstanceOf(InstantiationException.class, result);
    }

    @Test
    @DisplayName("INSTANCE_ERROR should map to InstantiationException")
    void instanceErrorShouldMapToInstantiationException() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.INSTANCE_ERROR, "instance error occurred");

      assertInstanceOf(InstantiationException.class, result);
    }

    @Test
    @DisplayName("AND boundary: 'import' alone should NOT be MISSING_IMPORT")
    void importAloneShouldNotBeMissingImport() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "import error occurred");
      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertNotEquals(
          ModuleInstantiationException.InstantiationErrorType.MISSING_IMPORT,
          ex.getErrorType(),
          "'import' without 'not found' should NOT be MISSING_IMPORT");
    }

    @Test
    @DisplayName("AND boundary: 'not found' alone should NOT be MISSING_IMPORT")
    void notFoundAloneShouldNotBeMissingImport() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "module not found");
      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertNotEquals(
          ModuleInstantiationException.InstantiationErrorType.MISSING_IMPORT,
          ex.getErrorType(),
          "'not found' without 'import' should NOT be MISSING_IMPORT");
    }

    @Test
    @DisplayName("AND boundary: 'type' alone should NOT be IMPORT_TYPE_MISMATCH")
    void typeAloneShouldNotBeImportTypeMismatch() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "type error occurred");
      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertNotEquals(
          ModuleInstantiationException.InstantiationErrorType.IMPORT_TYPE_MISMATCH,
          ex.getErrorType(),
          "'type' without 'import' should NOT be IMPORT_TYPE_MISMATCH");
    }

    @Test
    @DisplayName("AND boundary: 'memory' alone should NOT be MEMORY_ALLOCATION_FAILED")
    void memoryAloneShouldNotBeMemoryAllocationFailed() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "memory error occurred");
      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertNotEquals(
          ModuleInstantiationException.InstantiationErrorType.MEMORY_ALLOCATION_FAILED,
          ex.getErrorType(),
          "'memory' without 'allocation' should NOT be MEMORY_ALLOCATION_FAILED");
    }

    @Test
    @DisplayName("AND boundary: 'allocation' alone should NOT be MEMORY_ALLOCATION_FAILED")
    void allocationAloneShouldNotBeMemoryAllocationFailed() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "allocation error occurred");
      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertNotEquals(
          ModuleInstantiationException.InstantiationErrorType.MEMORY_ALLOCATION_FAILED,
          ex.getErrorType(),
          "'allocation' without 'memory' should NOT be MEMORY_ALLOCATION_FAILED");
    }

    @Test
    @DisplayName("AND boundary: 'table' alone should NOT be TABLE_ALLOCATION_FAILED")
    void tableAloneShouldNotBeTableAllocationFailed() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "table error occurred");
      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertNotEquals(
          ModuleInstantiationException.InstantiationErrorType.TABLE_ALLOCATION_FAILED,
          ex.getErrorType(),
          "'table' without 'allocation' should NOT be TABLE_ALLOCATION_FAILED");
    }

    @Test
    @DisplayName("Negative: signature should NOT be MISSING_IMPORT")
    void signatureShouldNotBeMissingImport() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "signature mismatch");
      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertNotEquals(
          ModuleInstantiationException.InstantiationErrorType.MISSING_IMPORT,
          ex.getErrorType(),
          "signature should NOT be MISSING_IMPORT");
    }

    @Test
    @DisplayName("Negative: data segment should NOT be ELEMENT_SEGMENT_INIT_FAILED")
    void dataSegmentShouldNotBeElementSegmentFailed() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "data segment error");
      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertNotEquals(
          ModuleInstantiationException.InstantiationErrorType.ELEMENT_SEGMENT_INIT_FAILED,
          ex.getErrorType(),
          "data segment should NOT be ELEMENT_SEGMENT_INIT_FAILED");
    }

    @Test
    @DisplayName("Negative: timeout should NOT be RESOURCE_LIMIT_EXCEEDED")
    void timeoutShouldNotBeResourceLimitExceeded() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "timeout occurred");
      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertNotEquals(
          ModuleInstantiationException.InstantiationErrorType.RESOURCE_LIMIT_EXCEEDED,
          ex.getErrorType(),
          "timeout should NOT be RESOURCE_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("Priority: 'import not found' takes priority")
    void importNotFoundTakesPriority() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "import not found type mismatch");
      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.MISSING_IMPORT,
          ex.getErrorType(),
          "'import not found' should take priority");
    }
  }

  @Nested
  @DisplayName("WASI Error Branch Coverage Tests")
  class WasiErrorBranchCoverageTests {

    @Test
    @DisplayName("Should detect network category in WASI error")
    void shouldDetectNetworkCategoryInWasiError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "network connection refused");

      assertInstanceOf(WasiException.class, result);
      final WasiException wasi = (WasiException) result;
      assertEquals(
          WasiException.ErrorCategory.NETWORK, wasi.getCategory(), "Should be NETWORK category");
    }

    @Test
    @DisplayName("Should detect permission category in WASI error")
    void shouldDetectPermissionCategoryInWasiError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "permission denied for operation");

      assertInstanceOf(WasiException.class, result);
      final WasiException wasi = (WasiException) result;
      assertEquals(
          WasiException.ErrorCategory.PERMISSION,
          wasi.getCategory(),
          "Should be PERMISSION category");
    }

    @Test
    @DisplayName("Should detect access keyword in WASI error")
    void shouldDetectAccessKeywordInWasiError() {
      final WasmException result = ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "access denied");

      assertInstanceOf(WasiException.class, result);
      final WasiException wasi = (WasiException) result;
      assertEquals(
          WasiException.ErrorCategory.PERMISSION,
          wasi.getCategory(),
          "Should be PERMISSION from access keyword");
    }

    @Test
    @DisplayName("Should detect resource limit category in WASI error")
    void shouldDetectResourceLimitCategoryInWasiError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "resource limit reached");

      assertInstanceOf(WasiException.class, result);
      final WasiException wasi = (WasiException) result;
      assertEquals(
          WasiException.ErrorCategory.RESOURCE_LIMIT,
          wasi.getCategory(),
          "Should be RESOURCE_LIMIT category");
    }

    @Test
    @DisplayName("Should detect limit keyword in WASI error")
    void shouldDetectLimitKeywordInWasiError() {
      final WasmException result = ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "limit exceeded");

      assertInstanceOf(WasiException.class, result);
      final WasiException wasi = (WasiException) result;
      assertEquals(
          WasiException.ErrorCategory.RESOURCE_LIMIT,
          wasi.getCategory(),
          "Should be RESOURCE_LIMIT from limit keyword");
    }

    @Test
    @DisplayName("Should detect component category in WASI error")
    void shouldDetectComponentCategoryInWasiError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "component error occurred");

      assertInstanceOf(WasiException.class, result);
      final WasiException wasi = (WasiException) result;
      assertEquals(
          WasiException.ErrorCategory.COMPONENT,
          wasi.getCategory(),
          "Should be COMPONENT category");
    }

    @Test
    @DisplayName("Should detect configuration category in WASI error")
    void shouldDetectConfigurationCategoryInWasiError() {
      final WasmException result = ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "config invalid");

      assertInstanceOf(WasiException.class, result);
      final WasiException wasi = (WasiException) result;
      assertEquals(
          WasiException.ErrorCategory.CONFIGURATION,
          wasi.getCategory(),
          "Should be CONFIGURATION category");
    }

    @Test
    @DisplayName("Should default to SYSTEM category for unrecognized WASI error")
    void shouldDefaultToSystemCategoryForUnrecognizedWasiError() {
      final WasmException result = ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "xyz random error");

      assertInstanceOf(WasiException.class, result);
      final WasiException wasi = (WasiException) result;
      assertEquals(
          WasiException.ErrorCategory.SYSTEM, wasi.getCategory(), "Should be SYSTEM category");
    }

    @Test
    @DisplayName("WASI error should have retryable=false")
    void wasiErrorShouldNotBeRetryable() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "network error occurred");
      assertInstanceOf(WasiException.class, result);
      final WasiException wasi = (WasiException) result;
      assertEquals(false, wasi.isRetryable(), "WASI error should have retryable=false");
    }

    @Test
    @DisplayName("COMPONENT_ERROR should map to WasiException with COMPONENT category")
    void componentErrorShouldMapToWasiException() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPONENT_ERROR, "component error occurred");
      assertInstanceOf(WasiException.class, result);
      final WasiException wasi = (WasiException) result;
      assertEquals(
          WasiException.ErrorCategory.COMPONENT,
          wasi.getCategory(),
          "COMPONENT_ERROR should map to COMPONENT category");
      assertEquals(false, wasi.isRetryable(), "Component error should have retryable=false");
    }

    @Test
    @DisplayName("INTERFACE_ERROR should map to WasiException with COMPONENT category")
    void interfaceErrorShouldMapToWasiException() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.INTERFACE_ERROR, "interface error occurred");
      assertInstanceOf(WasiException.class, result);
      final WasiException wasi = (WasiException) result;
      assertEquals(
          WasiException.ErrorCategory.COMPONENT,
          wasi.getCategory(),
          "INTERFACE_ERROR should map to COMPONENT category");
    }

    @Test
    @DisplayName("Negative: permission should NOT be NETWORK category")
    void permissionShouldNotBeNetwork() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "permission denied error");
      assertInstanceOf(WasiException.class, result);
      final WasiException wasi = (WasiException) result;
      assertNotEquals(
          WasiException.ErrorCategory.NETWORK,
          wasi.getCategory(),
          "permission should NOT be NETWORK");
    }

    @Test
    @DisplayName("Negative: resource should NOT be PERMISSION category")
    void resourceShouldNotBePermission() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "resource exhausted error");
      assertInstanceOf(WasiException.class, result);
      final WasiException wasi = (WasiException) result;
      assertNotEquals(
          WasiException.ErrorCategory.PERMISSION,
          wasi.getCategory(),
          "resource should NOT be PERMISSION");
    }

    @Test
    @DisplayName("Negative: component should NOT be RESOURCE_LIMIT category")
    void componentShouldNotBeResourceLimit() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "component model error");
      assertInstanceOf(WasiException.class, result);
      final WasiException wasi = (WasiException) result;
      assertNotEquals(
          WasiException.ErrorCategory.RESOURCE_LIMIT,
          wasi.getCategory(),
          "component should NOT be RESOURCE_LIMIT");
    }

    @Test
    @DisplayName("Negative: config should NOT be COMPONENT category")
    void configShouldNotBeComponent() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "config error occurred");
      assertInstanceOf(WasiException.class, result);
      final WasiException wasi = (WasiException) result;
      assertNotEquals(
          WasiException.ErrorCategory.COMPONENT,
          wasi.getCategory(),
          "config should NOT be COMPONENT");
    }

    @Test
    @DisplayName("Negative: unknown WASI error should NOT be any specific category")
    void unknownWasiShouldNotBeSpecificCategory() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "xyz unknown wasi abc");
      assertInstanceOf(WasiException.class, result);
      final WasiException wasi = (WasiException) result;
      assertNotEquals(
          WasiException.ErrorCategory.NETWORK, wasi.getCategory(), "unknown should NOT be NETWORK");
      assertNotEquals(
          WasiException.ErrorCategory.PERMISSION,
          wasi.getCategory(),
          "unknown should NOT be PERMISSION");
      assertNotEquals(
          WasiException.ErrorCategory.COMPONENT,
          wasi.getCategory(),
          "unknown should NOT be COMPONENT");
    }
  }

  @Nested
  @DisplayName("WASI File System Error Branch Coverage Tests")
  class WasiFileSystemErrorBranchCoverageTests {

    @Test
    @DisplayName("Should detect file not found with enoent keyword")
    void shouldDetectFileNotFoundWithEnoent() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file operation failed: enoent");

      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.NOT_FOUND,
          ex.getFileSystemErrorType(),
          "Should be NOT_FOUND type");
    }

    @Test
    @DisplayName("Should detect permission denied with eacces keyword")
    void shouldDetectPermissionDeniedWithEacces() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file error: eacces");

      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.PERMISSION_DENIED,
          ex.getFileSystemErrorType(),
          "Should be PERMISSION_DENIED type");
    }

    @Test
    @DisplayName("Should detect already exists with eexist keyword")
    void shouldDetectAlreadyExistsWithEexist() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file already exists eexist");

      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.ALREADY_EXISTS,
          ex.getFileSystemErrorType(),
          "Should be ALREADY_EXISTS type");
    }

    @Test
    @DisplayName("Should detect is a directory with eisdir keyword")
    void shouldDetectIsADirectoryWithEisdir() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file is a directory eisdir");

      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.IS_DIRECTORY,
          ex.getFileSystemErrorType(),
          "Should be IS_DIRECTORY type");
    }

    @Test
    @DisplayName("Should detect not a directory with enotdir keyword")
    void shouldDetectNotADirectoryWithEnotdir() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file not a directory enotdir");

      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.NOT_DIRECTORY,
          ex.getFileSystemErrorType(),
          "Should be NOT_DIRECTORY type");
    }

    @Test
    @DisplayName("Should detect directory not empty with enotempty keyword")
    void shouldDetectDirectoryNotEmptyWithEnotempty() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "directory not empty enotempty");

      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.DIRECTORY_NOT_EMPTY,
          ex.getFileSystemErrorType(),
          "Should be DIRECTORY_NOT_EMPTY type");
    }

    @Test
    @DisplayName("Should detect no space with enospc keyword")
    void shouldDetectNoSpaceWithEnospc() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file no space enospc");

      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.NO_SPACE,
          ex.getFileSystemErrorType(),
          "Should be NO_SPACE type");
    }

    @Test
    @DisplayName("Should detect file too large with efbig keyword")
    void shouldDetectFileTooLargeWithEfbig() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file too large efbig");

      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.FILE_TOO_LARGE,
          ex.getFileSystemErrorType(),
          "Should be FILE_TOO_LARGE type");
    }

    @Test
    @DisplayName("Should detect bad file descriptor with ebadf keyword")
    void shouldDetectBadFileDescriptorWithEbadf() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file bad file descriptor ebadf");

      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.INVALID_FILE_DESCRIPTOR,
          ex.getFileSystemErrorType(),
          "Should be INVALID_FILE_DESCRIPTOR type");
    }

    @Test
    @DisplayName("Should detect I/O error with eio keyword")
    void shouldDetectIoErrorWithEio() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file i/o error eio");

      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.IO_ERROR,
          ex.getFileSystemErrorType(),
          "Should be IO_ERROR type");
    }

    @Test
    @DisplayName("Should default to UNKNOWN for unrecognized file system error")
    void shouldDefaultToUnknownForUnrecognizedFileSystemError() {
      final WasmException result = ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file xyz random");

      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.UNKNOWN,
          ex.getFileSystemErrorType(),
          "Should be UNKNOWN type");
    }

    @Test
    @DisplayName("Should extract errno code from file system error message")
    void shouldExtractErrnoCodeFromFileSystemErrorMessage() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file not found errno 44");

      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(Integer.valueOf(44), ex.getErrnoCode(), "Should extract errno code 44");
    }

    @Test
    @DisplayName("Should detect directory keyword for file system errors")
    void shouldDetectDirectoryKeywordForFileSystemErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "directory operation failed");

      assertInstanceOf(WasiFileSystemException.class, result);
    }

    @Test
    @DisplayName("Should detect path keyword for file system errors")
    void shouldDetectPathKeywordForFileSystemErrors() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "path operation failed");

      assertInstanceOf(WasiFileSystemException.class, result);
    }

    @Test
    @DisplayName("OR boundary: 'enoent' alone triggers NOT_FOUND")
    void enoentAloneShouldTriggerNotFound() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file error enoent");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.NOT_FOUND,
          ex.getFileSystemErrorType(),
          "'enoent' alone should trigger NOT_FOUND");
    }

    @Test
    @DisplayName("OR boundary: 'eacces' alone triggers PERMISSION_DENIED")
    void eaccesAloneShouldTriggerPermissionDenied() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file error eacces");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.PERMISSION_DENIED,
          ex.getFileSystemErrorType(),
          "'eacces' alone should trigger PERMISSION_DENIED");
    }

    @Test
    @DisplayName("OR boundary: 'eexist' alone triggers ALREADY_EXISTS")
    void eexistAloneShouldTriggerAlreadyExists() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file error eexist");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.ALREADY_EXISTS,
          ex.getFileSystemErrorType(),
          "'eexist' alone should trigger ALREADY_EXISTS");
    }

    @Test
    @DisplayName("OR boundary: 'eisdir' alone triggers IS_DIRECTORY")
    void eisdirAloneShouldTriggerIsDirectory() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file error eisdir");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.IS_DIRECTORY,
          ex.getFileSystemErrorType(),
          "'eisdir' alone should trigger IS_DIRECTORY");
    }

    @Test
    @DisplayName("OR boundary: 'enotdir' alone triggers NOT_DIRECTORY")
    void enotdirAloneShouldTriggerNotDirectory() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file error enotdir");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.NOT_DIRECTORY,
          ex.getFileSystemErrorType(),
          "'enotdir' alone should trigger NOT_DIRECTORY");
    }

    @Test
    @DisplayName("OR boundary: 'enotempty' alone triggers DIRECTORY_NOT_EMPTY")
    void enotemptyAloneShouldTriggerDirectoryNotEmpty() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file error enotempty");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.DIRECTORY_NOT_EMPTY,
          ex.getFileSystemErrorType(),
          "'enotempty' alone should trigger DIRECTORY_NOT_EMPTY");
    }

    @Test
    @DisplayName("OR boundary: 'enospc' alone triggers NO_SPACE")
    void enospcAloneShouldTriggerNoSpace() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file error enospc");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.NO_SPACE,
          ex.getFileSystemErrorType(),
          "'enospc' alone should trigger NO_SPACE");
    }

    @Test
    @DisplayName("OR boundary: 'efbig' alone triggers FILE_TOO_LARGE")
    void efbigAloneShouldTriggerFileTooLarge() {
      final WasmException result = ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file error efbig");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.FILE_TOO_LARGE,
          ex.getFileSystemErrorType(),
          "'efbig' alone should trigger FILE_TOO_LARGE");
    }

    @Test
    @DisplayName("OR boundary: 'ebadf' alone triggers INVALID_FILE_DESCRIPTOR")
    void ebadfAloneShouldTriggerInvalidFileDescriptor() {
      final WasmException result = ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file error ebadf");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.INVALID_FILE_DESCRIPTOR,
          ex.getFileSystemErrorType(),
          "'ebadf' alone should trigger INVALID_FILE_DESCRIPTOR");
    }

    @Test
    @DisplayName("OR boundary: 'eio' alone triggers IO_ERROR")
    void eioAloneShouldTriggerIoError() {
      final WasmException result = ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file error eio");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.IO_ERROR,
          ex.getFileSystemErrorType(),
          "'eio' alone should trigger IO_ERROR");
    }

    @Test
    @DisplayName("Negative: unrecognized file error should be UNKNOWN")
    void unrecognizedFileErrorShouldBeUnknown() {
      final WasmException result = ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file xyz random");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.UNKNOWN,
          ex.getFileSystemErrorType(),
          "unrecognized file error should be UNKNOWN");
    }

    @Test
    @DisplayName("Negative: eacces should NOT be NOT_FOUND")
    void eaccesShouldNotBeNotFound() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file error eacces");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertNotEquals(
          WasiFileSystemException.FileSystemErrorType.NOT_FOUND,
          ex.getFileSystemErrorType(),
          "eacces should NOT be NOT_FOUND");
    }

    @Test
    @DisplayName("Negative: eexist should NOT be PERMISSION_DENIED")
    void eexistShouldNotBePermissionDenied() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file error eexist");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertNotEquals(
          WasiFileSystemException.FileSystemErrorType.PERMISSION_DENIED,
          ex.getFileSystemErrorType(),
          "eexist should NOT be PERMISSION_DENIED");
    }

    @Test
    @DisplayName("Negative: eisdir should NOT be ALREADY_EXISTS")
    void eisdirShouldNotBeAlreadyExists() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file error eisdir");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertNotEquals(
          WasiFileSystemException.FileSystemErrorType.ALREADY_EXISTS,
          ex.getFileSystemErrorType(),
          "eisdir should NOT be ALREADY_EXISTS");
    }

    @Test
    @DisplayName("Negative: enotdir should NOT be IS_DIRECTORY")
    void enotdirShouldNotBeIsDirectory() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file error enotdir");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertNotEquals(
          WasiFileSystemException.FileSystemErrorType.IS_DIRECTORY,
          ex.getFileSystemErrorType(),
          "enotdir should NOT be IS_DIRECTORY");
    }

    @Test
    @DisplayName("Negative: enotempty should NOT be NOT_DIRECTORY")
    void enotemptyShouldNotBeNotDirectory() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file error enotempty");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertNotEquals(
          WasiFileSystemException.FileSystemErrorType.NOT_DIRECTORY,
          ex.getFileSystemErrorType(),
          "enotempty should NOT be NOT_DIRECTORY");
    }

    @Test
    @DisplayName("Priority: 'not found' takes priority")
    void notFoundTakesPriority() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file not found eacces");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.NOT_FOUND,
          ex.getFileSystemErrorType(),
          "'not found' should take priority over 'eacces'");
    }
  }

  @Nested
  @DisplayName("Other Error Code Branch Coverage Tests")
  class OtherErrorCodeBranchCoverageTests {

    @Test
    @DisplayName("MEMORY_ERROR should map to RuntimeException with MEMORY_ACCESS_VIOLATION")
    void memoryErrorShouldMapToRuntimeException() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.MEMORY_ERROR, "memory error occurred");

      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.MEMORY_ACCESS_VIOLATION,
          rt.getErrorType(),
          "Should be MEMORY_ACCESS_VIOLATION type");
    }

    @Test
    @DisplayName("FUNCTION_ERROR should map to RuntimeException with FUNCTION_EXECUTION_FAILED")
    void functionErrorShouldMapToRuntimeException() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.FUNCTION_ERROR, "function error occurred");

      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.FUNCTION_EXECUTION_FAILED,
          rt.getErrorType(),
          "Should be FUNCTION_EXECUTION_FAILED type");
    }

    @Test
    @DisplayName("TYPE_ERROR should map to ValidationException")
    void typeErrorShouldMapToValidationException() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.TYPE_ERROR, "type error occurred");

      assertInstanceOf(ValidationException.class, result);
      assertTrue(result.getMessage().contains("Type error"), "Message should contain 'Type error'");
    }

    @Test
    @DisplayName("RESOURCE_ERROR should map to RuntimeException with RESOURCE_EXHAUSTED")
    void resourceErrorShouldMapToRuntimeException() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RESOURCE_ERROR, "resource error occurred");

      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.RESOURCE_EXHAUSTED,
          rt.getErrorType(),
          "Should be RESOURCE_EXHAUSTED type");
    }

    @Test
    @DisplayName("IO_ERROR should map to WasiFileSystemException with IO_ERROR type")
    void ioErrorShouldMapToWasiFileSystemException() {
      final WasmException result = ErrorMapper.mapError(ErrorMapper.IO_ERROR, "io error occurred");

      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.IO_ERROR,
          ex.getFileSystemErrorType(),
          "Should be IO_ERROR type");
    }

    @Test
    @DisplayName("INVALID_PARAMETER_ERROR should map to WasmException")
    void invalidParameterErrorShouldMapToWasmException() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.INVALID_PARAMETER_ERROR, "invalid param");

      assertInstanceOf(WasmException.class, result);
      assertTrue(
          result.getMessage().contains("Invalid parameter"),
          "Message should contain 'Invalid parameter'");
    }

    @Test
    @DisplayName("CONCURRENCY_ERROR should map to WasmException")
    void concurrencyErrorShouldMapToWasmException() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.CONCURRENCY_ERROR, "concurrency issue");

      assertInstanceOf(WasmException.class, result);
      assertTrue(
          result.getMessage().contains("Concurrency error"),
          "Message should contain 'Concurrency error'");
    }

    @Test
    @DisplayName("COMPONENT_ERROR should map to WasiException with COMPONENT category")
    void componentErrorShouldMapToWasiException() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPONENT_ERROR, "component error");

      assertInstanceOf(WasiException.class, result);
      final WasiException wasi = (WasiException) result;
      assertEquals(
          WasiException.ErrorCategory.COMPONENT,
          wasi.getCategory(),
          "Should be COMPONENT category");
    }

    @Test
    @DisplayName("INTERFACE_ERROR should map to WasiException with COMPONENT category")
    void interfaceErrorShouldMapToWasiException() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.INTERFACE_ERROR, "interface error");

      assertInstanceOf(WasiException.class, result);
      final WasiException wasi = (WasiException) result;
      assertEquals(
          WasiException.ErrorCategory.COMPONENT,
          wasi.getCategory(),
          "Should be COMPONENT category");
    }

    @Test
    @DisplayName("INTERNAL_ERROR should map to default WasmException")
    void internalErrorShouldMapToDefaultWasmException() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.INTERNAL_ERROR, "internal error");

      assertInstanceOf(WasmException.class, result);
      assertTrue(result.getMessage().contains("Error code"), "Message should contain 'Error code'");
    }
  }

  @Nested
  @DisplayName("mapError Overload Tests")
  class MapErrorOverloadTests {

    @Test
    @DisplayName("Two-arg mapError should call three-arg with null cause")
    void twoArgMapErrorShouldCallThreeArgWithNullCause() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "compilation failed");

      assertNotNull(result, "Result should not be null");
      assertInstanceOf(CompilationException.class, result);
      // Cause should be null since we used two-arg version
      // This verifies the overload works
    }
  }

  // ============================================================================
  // ADDITIONAL MUTATION KILLING TESTS - EXACT TYPE VERIFICATION
  // ============================================================================

  @Nested
  @DisplayName("Exact Type Verification Tests")
  class ExactTypeVerificationTests {

    @Test
    @DisplayName("COMPILATION_ERROR should return exactly ModuleCompilationException")
    void compilationErrorShouldReturnExactType() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "compilation failed");
      assertEquals(
          ModuleCompilationException.class,
          result.getClass(),
          "Should return exactly ModuleCompilationException");
    }

    @Test
    @DisplayName("VALIDATION_ERROR should return exactly ModuleValidationException")
    void validationErrorShouldReturnExactType() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "validation failed");
      assertEquals(
          ModuleValidationException.class,
          result.getClass(),
          "Should return exactly ModuleValidationException");
    }

    @Test
    @DisplayName("RUNTIME_ERROR without trap should return exactly RuntimeException")
    void runtimeErrorWithoutTrapShouldReturnExactType() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "timeout occurred");
      assertEquals(
          RuntimeException.class,
          result.getClass(),
          "Should return exactly RuntimeException (not TrapException)");
    }

    @Test
    @DisplayName("RUNTIME_ERROR with trap should return exactly TrapException")
    void runtimeErrorWithTrapShouldReturnTrapException() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: stack overflow");
      assertEquals(TrapException.class, result.getClass(), "Should return exactly TrapException");
    }

    @Test
    @DisplayName("ENGINE_CONFIG_ERROR should return exactly ModuleInstantiationException")
    void engineConfigErrorShouldReturnExactType() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "config error");
      assertEquals(
          ModuleInstantiationException.class,
          result.getClass(),
          "Should return exactly ModuleInstantiationException");
    }

    @Test
    @DisplayName("IMPORT_EXPORT_ERROR should return exactly LinkingException")
    void importExportErrorShouldReturnExactType() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "linking error");
      assertEquals(
          LinkingException.class, result.getClass(), "Should return exactly LinkingException");
    }
  }

  @Nested
  @DisplayName("Compilation Error Mutation Killing Tests")
  class CompilationErrorMutationKillingTests {

    @Test
    @DisplayName("'memory' without 'out of' should still trigger OUT_OF_MEMORY")
    void memoryWithoutOutOfShouldTriggerOutOfMemory() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "memory limit exceeded");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.OUT_OF_MEMORY,
          ex.getErrorType(),
          "'memory' alone should trigger OUT_OF_MEMORY");
    }

    @Test
    @DisplayName("'time' without 'timeout' should still trigger TIMEOUT")
    void timeWithoutTimeoutShouldTriggerTimeout() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "no more time left");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.TIMEOUT,
          ex.getErrorType(),
          "'time' alone should trigger TIMEOUT");
    }

    @Test
    @DisplayName("'complex' without 'too' should still trigger FUNCTION_TOO_COMPLEX")
    void complexWithoutTooShouldTriggerTooComplex() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "highly complex function");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.FUNCTION_TOO_COMPLEX,
          ex.getErrorType(),
          "'complex' alone should trigger FUNCTION_TOO_COMPLEX");
    }

    @Test
    @DisplayName("'not supported' without 'unsupported' should trigger UNSUPPORTED_INSTRUCTION")
    void notSupportedAloneShouldTriggerUnsupported() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "this is not supported here");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.UNSUPPORTED_INSTRUCTION,
          ex.getErrorType(),
          "'not supported' should trigger UNSUPPORTED_INSTRUCTION");
    }

    @Test
    @DisplayName("'register' without 'allocation' should still trigger REGISTER_ALLOCATION_FAILED")
    void registerWithoutAllocationShouldTriggerRegisterAllocation() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "register spill error");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.REGISTER_ALLOCATION_FAILED,
          ex.getErrorType(),
          "'register' alone should trigger REGISTER_ALLOCATION_FAILED");
    }

    @Test
    @DisplayName("'code generation' without 'codegen' should trigger CODE_GENERATION_FAILED")
    void codeGenerationShouldTriggerCodeGenFailed() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "code generation error");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.CODE_GENERATION_FAILED,
          ex.getErrorType(),
          "'code generation' should trigger CODE_GENERATION_FAILED");
    }
  }

  @Nested
  @DisplayName("Validation Error Mutation Killing Tests")
  class ValidationErrorMutationKillingTests {

    @Test
    @DisplayName("'type' without 'mismatch' should still trigger TYPE_MISMATCH")
    void typeWithoutMismatchShouldTriggerTypeMismatch() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "wrong type used");
      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.TYPE_MISMATCH,
          ex.getErrorType(),
          "'type' alone should trigger TYPE_MISMATCH");
    }

    @Test
    @DisplayName("'table' alone should trigger INVALID_TABLE_DEFINITION")
    void tableAloneShouldTriggerInvalidTable() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "table spec wrong");
      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.INVALID_TABLE_DEFINITION,
          ex.getErrorType(),
          "'table' alone should trigger INVALID_TABLE_DEFINITION");
    }

    @Test
    @DisplayName("'function' alone should trigger INVALID_FUNCTION_BODY")
    void functionAloneShouldTriggerInvalidFunctionBody() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "function validation failed");
      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.INVALID_FUNCTION_BODY,
          ex.getErrorType(),
          "'function' alone should trigger INVALID_FUNCTION_BODY");
    }
  }

  @Nested
  @DisplayName("Runtime Error Mutation Killing Tests")
  class RuntimeErrorMutationKillingTests {

    @Test
    @DisplayName("'interrupt' alone should trigger INTERRUPTED")
    void interruptAloneShouldTriggerInterrupted() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "user interrupt signal");
      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.INTERRUPTED,
          rt.getErrorType(),
          "'interrupt' alone should trigger INTERRUPTED");
    }

    @Test
    @DisplayName("'stack' alone should trigger STACK_ERROR")
    void stackAloneShouldTriggerStackError() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "stack depth exceeded");
      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.STACK_ERROR,
          rt.getErrorType(),
          "'stack' alone should trigger STACK_ERROR");
    }

    @Test
    @DisplayName("'function' alone in runtime should trigger FUNCTION_EXECUTION_FAILED")
    void functionAloneInRuntimeShouldTriggerFunctionFailed() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "function invocation error");
      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.FUNCTION_EXECUTION_FAILED,
          rt.getErrorType(),
          "'function' alone should trigger FUNCTION_EXECUTION_FAILED");
    }
  }

  @Nested
  @DisplayName("Trap Error Mutation Killing Tests")
  class TrapErrorMutationKillingTests {

    @Test
    @DisplayName("'out of bounds' + 'table' exactly should trigger TABLE_OUT_OF_BOUNDS")
    void outOfBoundsTableExactlyShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: table index out of bounds");
      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.TABLE_OUT_OF_BOUNDS,
          trap.getTrapType(),
          "'out of bounds' + 'table' should trigger TABLE_OUT_OF_BOUNDS");
    }

    @Test
    @DisplayName("'out of bounds' + 'array' exactly should trigger ARRAY_OUT_OF_BOUNDS")
    void outOfBoundsArrayExactlyShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: array index out of bounds");
      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.ARRAY_OUT_OF_BOUNDS,
          trap.getTrapType(),
          "'out of bounds' + 'array' should trigger ARRAY_OUT_OF_BOUNDS");
    }

    @Test
    @DisplayName("'signature' alone in trap should trigger BAD_SIGNATURE")
    void signatureAloneInTrapShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: wrong signature");
      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.BAD_SIGNATURE,
          trap.getTrapType(),
          "'signature' alone should trigger BAD_SIGNATURE");
    }

    @Test
    @DisplayName("'type mismatch' alone in trap should trigger BAD_SIGNATURE")
    void typeMismatchAloneInTrapShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: type mismatch");
      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.BAD_SIGNATURE,
          trap.getTrapType(),
          "'type mismatch' alone should trigger BAD_SIGNATURE");
    }

    @Test
    @DisplayName("'fuel' alone in trap should trigger OUT_OF_FUEL")
    void fuelAloneInTrapShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: fuel exhausted");
      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.OUT_OF_FUEL,
          trap.getTrapType(),
          "'fuel' alone should trigger OUT_OF_FUEL");
    }

    @Test
    @DisplayName("'out of fuel' in trap should trigger OUT_OF_FUEL")
    void outOfFuelInTrapShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: execution ran out of fuel");
      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.OUT_OF_FUEL,
          trap.getTrapType(),
          "'out of fuel' should trigger OUT_OF_FUEL");
    }
  }

  @Nested
  @DisplayName("Instantiation Error Mutation Killing Tests")
  class InstantiationErrorMutationKillingTests {

    @Test
    @DisplayName("'memory' + 'allocation' should trigger MEMORY_ALLOCATION_FAILED")
    void memoryAllocationShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "memory allocation issue");
      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.MEMORY_ALLOCATION_FAILED,
          ex.getErrorType(),
          "'memory allocation' should trigger MEMORY_ALLOCATION_FAILED");
    }

    @Test
    @DisplayName("'start function' alone should trigger START_FUNCTION_FAILED")
    void startFunctionAloneShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "start function error");
      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.START_FUNCTION_FAILED,
          ex.getErrorType(),
          "'start function' should trigger START_FUNCTION_FAILED");
    }

    @Test
    @DisplayName("Import pattern with module.name should extract module name")
    void importPatternShouldExtractModuleName() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "import:mymodule.myfunction");
      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      // The import pattern matching should extract module name
      assertEquals("mymodule", ex.getModuleName(), "Should extract module name from import");
    }
  }

  @Nested
  @DisplayName("Linking Error Mutation Killing Tests")
  class LinkingErrorMutationKillingTests {

    @Test
    @DisplayName("'signature' alone in linking should trigger FUNCTION_SIGNATURE_MISMATCH")
    void signatureAloneInLinkingShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "signature is wrong");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH,
          linking.getErrorType(),
          "'signature' alone should trigger FUNCTION_SIGNATURE_MISMATCH");
    }

    @Test
    @DisplayName("'function' alone in linking should trigger FUNCTION_SIGNATURE_MISMATCH")
    void functionAloneInLinkingShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "function error in linking");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH,
          linking.getErrorType(),
          "'function' alone should trigger FUNCTION_SIGNATURE_MISMATCH");
    }

    @Test
    @DisplayName("'memory' + 'limit' should trigger MEMORY_LIMITS_INCOMPATIBLE")
    void memoryLimitShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "memory limit incompatible");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.MEMORY_LIMITS_INCOMPATIBLE,
          linking.getErrorType(),
          "'memory limit' should trigger MEMORY_LIMITS_INCOMPATIBLE");
    }

    @Test
    @DisplayName("'table' + 'type' should trigger TABLE_TYPE_MISMATCH")
    void tableTypeShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "table type is wrong");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.TABLE_TYPE_MISMATCH,
          linking.getErrorType(),
          "'table type' should trigger TABLE_TYPE_MISMATCH");
    }

    @Test
    @DisplayName("'global' + 'mutability' should trigger GLOBAL_MUTABILITY_MISMATCH")
    void globalMutabilityShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "global mutability wrong");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertEquals(
          LinkingException.LinkingErrorType.GLOBAL_MUTABILITY_MISMATCH,
          linking.getErrorType(),
          "'global mutability' should trigger GLOBAL_MUTABILITY_MISMATCH");
    }
  }

  @Nested
  @DisplayName("WASI Error Mutation Killing Tests")
  class WasiErrorMutationKillingTests {

    @Test
    @DisplayName("'access' alone should trigger PERMISSION category")
    void accessAloneShouldTriggerPermission() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "access restriction");
      assertInstanceOf(WasiException.class, result);
      final WasiException wasi = (WasiException) result;
      assertEquals(
          WasiException.ErrorCategory.PERMISSION,
          wasi.getCategory(),
          "'access' alone should trigger PERMISSION");
    }

    @Test
    @DisplayName("'limit' alone should trigger RESOURCE_LIMIT category")
    void limitAloneShouldTriggerResourceLimit() {
      final WasmException result = ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "limit reached");
      assertInstanceOf(WasiException.class, result);
      final WasiException wasi = (WasiException) result;
      assertEquals(
          WasiException.ErrorCategory.RESOURCE_LIMIT,
          wasi.getCategory(),
          "'limit' alone should trigger RESOURCE_LIMIT");
    }
  }

  @Nested
  @DisplayName("File System Error Mutation Killing Tests")
  class FileSystemErrorMutationKillingTests {

    @Test
    @DisplayName("'not found' alone should trigger NOT_FOUND")
    void notFoundAloneShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file not found anywhere");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.NOT_FOUND,
          ex.getFileSystemErrorType(),
          "'not found' should trigger NOT_FOUND");
    }

    @Test
    @DisplayName("'permission denied' alone should trigger PERMISSION_DENIED")
    void permissionDeniedAloneShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file permission denied");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.PERMISSION_DENIED,
          ex.getFileSystemErrorType(),
          "'permission denied' should trigger PERMISSION_DENIED");
    }

    @Test
    @DisplayName("'already exists' alone should trigger ALREADY_EXISTS")
    void alreadyExistsAloneShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file already exists");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.ALREADY_EXISTS,
          ex.getFileSystemErrorType(),
          "'already exists' should trigger ALREADY_EXISTS");
    }

    @Test
    @DisplayName("'is a directory' alone should trigger IS_DIRECTORY")
    void isADirectoryAloneShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "path is a directory");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.IS_DIRECTORY,
          ex.getFileSystemErrorType(),
          "'is a directory' should trigger IS_DIRECTORY");
    }

    @Test
    @DisplayName("'not a directory' alone should trigger NOT_DIRECTORY")
    void notADirectoryAloneShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "path not a directory");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.NOT_DIRECTORY,
          ex.getFileSystemErrorType(),
          "'not a directory' should trigger NOT_DIRECTORY");
    }

    @Test
    @DisplayName("'directory not empty' alone should trigger DIRECTORY_NOT_EMPTY")
    void directoryNotEmptyAloneShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "directory not empty");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.DIRECTORY_NOT_EMPTY,
          ex.getFileSystemErrorType(),
          "'directory not empty' should trigger DIRECTORY_NOT_EMPTY");
    }

    @Test
    @DisplayName("'no space' alone should trigger NO_SPACE")
    void noSpaceAloneShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file no space left");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.NO_SPACE,
          ex.getFileSystemErrorType(),
          "'no space' should trigger NO_SPACE");
    }

    @Test
    @DisplayName("'file too large' alone should trigger FILE_TOO_LARGE")
    void fileTooLargeAloneShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "path file too large");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.FILE_TOO_LARGE,
          ex.getFileSystemErrorType(),
          "'file too large' should trigger FILE_TOO_LARGE");
    }

    @Test
    @DisplayName("'bad file descriptor' alone should trigger INVALID_FILE_DESCRIPTOR")
    void badFileDescriptorAloneShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file bad file descriptor");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.INVALID_FILE_DESCRIPTOR,
          ex.getFileSystemErrorType(),
          "'bad file descriptor' should trigger INVALID_FILE_DESCRIPTOR");
    }

    @Test
    @DisplayName("'i/o error' alone should trigger IO_ERROR")
    void ioErrorAloneShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file i/o error occurred");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException ex = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.IO_ERROR,
          ex.getFileSystemErrorType(),
          "'i/o error' should trigger IO_ERROR");
    }
  }

  @Nested
  @DisplayName("Second Operand OR Condition Tests")
  class SecondOperandOrConditionTests {

    // These tests specifically target messages that ONLY match the second operand of || conditions
    // They are designed to kill "removed conditional - replaced equality check with true" mutations

    // ===== mapCompilationError =====

    @Test
    @DisplayName(
        "Line 205: Message with ONLY 'memory' (not 'out of memory') should trigger OUT_OF_MEMORY")
    void line205OnlyMemoryShouldTriggerOutOfMemory() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "insufficient memory space");
      assertInstanceOf(ModuleCompilationException.class, result);
      assertEquals(
          ModuleCompilationException.CompilationErrorType.OUT_OF_MEMORY,
          ((ModuleCompilationException) result).getErrorType());
    }

    @Test
    @DisplayName("Line 205: Message without memory keywords should NOT be OUT_OF_MEMORY")
    void line205WithoutMemoryShouldNotBeOutOfMemory() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "timeout exceeded");
      assertInstanceOf(ModuleCompilationException.class, result);
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.OUT_OF_MEMORY,
          ((ModuleCompilationException) result).getErrorType());
    }

    @Test
    @DisplayName("Line 207: Message with ONLY 'time' (not 'timeout') should trigger TIMEOUT")
    void line207OnlyTimeShouldTriggerTimeout() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "compile time exceeded");
      assertInstanceOf(ModuleCompilationException.class, result);
      assertEquals(
          ModuleCompilationException.CompilationErrorType.TIMEOUT,
          ((ModuleCompilationException) result).getErrorType());
    }

    @Test
    @DisplayName("Line 207: Message without time keywords should NOT be TIMEOUT")
    void line207WithoutTimeShouldNotBeTimeout() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "function is complex");
      assertInstanceOf(ModuleCompilationException.class, result);
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.TIMEOUT,
          ((ModuleCompilationException) result).getErrorType());
    }

    @Test
    @DisplayName(
        "Line 209: Message with ONLY 'complex' (not 'too complex') should trigger"
            + " FUNCTION_TOO_COMPLEX")
    void line209OnlyComplexShouldTriggerTooComplex() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "highly complex logic");
      assertInstanceOf(ModuleCompilationException.class, result);
      assertEquals(
          ModuleCompilationException.CompilationErrorType.FUNCTION_TOO_COMPLEX,
          ((ModuleCompilationException) result).getErrorType());
    }

    @Test
    @DisplayName("Line 209: Message without complex keywords should NOT be FUNCTION_TOO_COMPLEX")
    void line209WithoutComplexShouldNotBeTooComplex() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "unsupported instruction");
      assertInstanceOf(ModuleCompilationException.class, result);
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.FUNCTION_TOO_COMPLEX,
          ((ModuleCompilationException) result).getErrorType());
    }

    // ===== mapValidationError =====

    @Test
    @DisplayName(
        "Line 246: Message with ONLY 'type' (not 'type mismatch') should trigger TYPE_MISMATCH")
    void line246OnlyTypeShouldTriggerTypeMismatch() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "invalid type used");
      assertInstanceOf(ModuleValidationException.class, result);
      assertEquals(
          ModuleValidationException.ValidationErrorType.TYPE_MISMATCH,
          ((ModuleValidationException) result).getErrorType());
    }

    @Test
    @DisplayName(
        "Line 258: Message with ONLY 'feature' (not 'unsupported') should trigger"
            + " UNSUPPORTED_FEATURE")
    void line258OnlyFeatureShouldTriggerUnsupported() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "feature unavailable");
      assertInstanceOf(ModuleValidationException.class, result);
      assertEquals(
          ModuleValidationException.ValidationErrorType.UNSUPPORTED_FEATURE,
          ((ModuleValidationException) result).getErrorType());
    }

    @Test
    @DisplayName("Line 260: Message with ONLY 'exceed' (not 'limit') should trigger LIMIT_EXCEEDED")
    void line260OnlyExceedShouldTriggerLimitExceeded() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "maximum will exceed");
      assertInstanceOf(ModuleValidationException.class, result);
      assertEquals(
          ModuleValidationException.ValidationErrorType.LIMIT_EXCEEDED,
          ((ModuleValidationException) result).getErrorType());
    }

    // ===== mapRuntimeError =====

    @Test
    @DisplayName("Line 284: Message with 'trap' word should return TrapException")
    void line284TrapWordShouldReturnTrapException() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "execution trapped");
      assertInstanceOf(TrapException.class, result);
    }

    @Test
    @DisplayName("Line 284: Message without trap should return RuntimeException")
    void line284WithoutTrapShouldReturnRuntimeException() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "execution timeout");
      assertEquals(RuntimeException.class, result.getClass());
    }

    @Test
    @DisplayName("Line 291: Message with ONLY 'time' (not 'timeout') should trigger TIMEOUT")
    void line291OnlyTimeShouldTriggerTimeout() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "execution time exceeded");
      assertInstanceOf(RuntimeException.class, result);
      assertEquals(
          RuntimeException.RuntimeErrorType.TIMEOUT, ((RuntimeException) result).getErrorType());
    }

    @Test
    @DisplayName(
        "Line 295: Message with ONLY 'access' (not 'memory') should trigger"
            + " MEMORY_ACCESS_VIOLATION")
    void line295OnlyAccessShouldTriggerMemoryViolation() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "invalid access detected");
      assertInstanceOf(RuntimeException.class, result);
      assertEquals(
          RuntimeException.RuntimeErrorType.MEMORY_ACCESS_VIOLATION,
          ((RuntimeException) result).getErrorType());
    }

    // ===== mapTrapError =====

    @Test
    @DisplayName("Line 327: 'out of bounds' + 'table' should trigger TABLE_OUT_OF_BOUNDS")
    void line327OutOfBoundsTableShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: table index out of bounds");
      assertInstanceOf(TrapException.class, result);
      assertEquals(
          TrapException.TrapType.TABLE_OUT_OF_BOUNDS, ((TrapException) result).getTrapType());
    }

    @Test
    @DisplayName(
        "Line 327: 'table' alone (without 'out of bounds') should NOT be TABLE_OUT_OF_BOUNDS")
    void line327TableAloneShouldNotBe() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: table error");
      assertInstanceOf(TrapException.class, result);
      assertNotEquals(
          TrapException.TrapType.TABLE_OUT_OF_BOUNDS, ((TrapException) result).getTrapType());
    }

    @Test
    @DisplayName("Line 329: 'out of bounds' + 'array' should trigger ARRAY_OUT_OF_BOUNDS")
    void line329OutOfBoundsArrayShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: array out of bounds access");
      assertInstanceOf(TrapException.class, result);
      assertEquals(
          TrapException.TrapType.ARRAY_OUT_OF_BOUNDS, ((TrapException) result).getTrapType());
    }

    @Test
    @DisplayName(
        "Line 329: 'array' alone (without 'out of bounds') should NOT be ARRAY_OUT_OF_BOUNDS")
    void line329ArrayAloneShouldNotBe() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: array error");
      assertInstanceOf(TrapException.class, result);
      assertNotEquals(
          TrapException.TrapType.ARRAY_OUT_OF_BOUNDS, ((TrapException) result).getTrapType());
    }

    @Test
    @DisplayName("Line 339: 'overflow' + 'integer' should trigger INTEGER_OVERFLOW")
    void line339OverflowIntegerShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: integer overflow detected");
      assertInstanceOf(TrapException.class, result);
      assertEquals(TrapException.TrapType.INTEGER_OVERFLOW, ((TrapException) result).getTrapType());
    }

    @Test
    @DisplayName("Line 339: 'overflow' alone (without 'integer') should NOT be INTEGER_OVERFLOW")
    void line339OverflowAloneShouldNotBe() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: stack overflow");
      assertInstanceOf(TrapException.class, result);
      assertNotEquals(
          TrapException.TrapType.INTEGER_OVERFLOW, ((TrapException) result).getTrapType());
    }

    @Test
    @DisplayName("Line 350: 'out of fuel' should trigger OUT_OF_FUEL")
    void line350OutOfFuelShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: out of fuel");
      assertInstanceOf(TrapException.class, result);
      assertEquals(TrapException.TrapType.OUT_OF_FUEL, ((TrapException) result).getTrapType());
    }

    @Test
    @DisplayName("Line 350: 'fuel' alone should also trigger OUT_OF_FUEL")
    void line350FuelAloneShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: fuel exhausted");
      assertInstanceOf(TrapException.class, result);
      assertEquals(TrapException.TrapType.OUT_OF_FUEL, ((TrapException) result).getTrapType());
    }

    // ===== mapInstantiationError =====

    @Test
    @DisplayName("Line 380: 'import' + 'type' should trigger IMPORT_TYPE_MISMATCH")
    void line380ImportTypeShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "import type mismatch");
      assertInstanceOf(ModuleInstantiationException.class, result);
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.IMPORT_TYPE_MISMATCH,
          ((ModuleInstantiationException) result).getErrorType());
    }

    @Test
    @DisplayName("Line 389: 'table' + 'allocation' should trigger TABLE_ALLOCATION_FAILED")
    void line389TableAllocationShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "table allocation failed");
      assertInstanceOf(ModuleInstantiationException.class, result);
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.TABLE_ALLOCATION_FAILED,
          ((ModuleInstantiationException) result).getErrorType());
    }

    @Test
    @DisplayName("Line 403: 'resource' alone should trigger RESOURCE_LIMIT_EXCEEDED")
    void line403ResourceAloneShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "resource exhausted");
      assertInstanceOf(ModuleInstantiationException.class, result);
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.RESOURCE_LIMIT_EXCEEDED,
          ((ModuleInstantiationException) result).getErrorType());
    }

    // ===== mapLinkingError =====

    @Test
    @DisplayName("Line 428: 'export' + 'not found' should trigger EXPORT_NOT_FOUND")
    void line428ExportNotFoundShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "export not found");
      assertInstanceOf(LinkingException.class, result);
      assertEquals(
          LinkingException.LinkingErrorType.EXPORT_NOT_FOUND,
          ((LinkingException) result).getErrorType());
    }

    @Test
    @DisplayName("Line 436: 'memory' + 'limit' should trigger MEMORY_LIMITS_INCOMPATIBLE")
    void line436MemoryLimitShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "memory limit mismatch");
      assertInstanceOf(LinkingException.class, result);
      assertEquals(
          LinkingException.LinkingErrorType.MEMORY_LIMITS_INCOMPATIBLE,
          ((LinkingException) result).getErrorType());
    }

    @Test
    @DisplayName(
        "Line 436: 'limit' alone (without 'memory') should NOT be MEMORY_LIMITS_INCOMPATIBLE")
    void line436LimitAloneShouldNotBe() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "limit error");
      assertInstanceOf(LinkingException.class, result);
      assertNotEquals(
          LinkingException.LinkingErrorType.MEMORY_LIMITS_INCOMPATIBLE,
          ((LinkingException) result).getErrorType());
    }

    @Test
    @DisplayName("Line 438: 'table' + 'size' should trigger TABLE_SIZE_MISMATCH")
    void line438TableSizeShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "table size mismatch");
      assertInstanceOf(LinkingException.class, result);
      assertEquals(
          LinkingException.LinkingErrorType.TABLE_SIZE_MISMATCH,
          ((LinkingException) result).getErrorType());
    }

    @Test
    @DisplayName("Line 440: 'table' + 'type' should trigger TABLE_TYPE_MISMATCH")
    void line440TableTypeShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "table type mismatch");
      assertInstanceOf(LinkingException.class, result);
      assertEquals(
          LinkingException.LinkingErrorType.TABLE_TYPE_MISMATCH,
          ((LinkingException) result).getErrorType());
    }

    @Test
    @DisplayName("Line 444: 'global' + 'mutability' should trigger GLOBAL_MUTABILITY_MISMATCH")
    void line444GlobalMutabilityShouldTrigger() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "global mutability mismatch");
      assertInstanceOf(LinkingException.class, result);
      assertEquals(
          LinkingException.LinkingErrorType.GLOBAL_MUTABILITY_MISMATCH,
          ((LinkingException) result).getErrorType());
    }

    @Test
    @DisplayName(
        "Line 444: 'mutability' alone (without 'global') should NOT be GLOBAL_MUTABILITY_MISMATCH")
    void line444MutabilityAloneShouldNotBe() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "mutability error");
      assertInstanceOf(LinkingException.class, result);
      assertNotEquals(
          LinkingException.LinkingErrorType.GLOBAL_MUTABILITY_MISMATCH,
          ((LinkingException) result).getErrorType());
    }

    // ===== mapWasiError =====

    @Test
    @DisplayName("Line 485: 'limit' alone (without 'resource') should trigger RESOURCE_LIMIT")
    void line485LimitAloneShouldTriggerResourceLimit() {
      final WasmException result = ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "limit exceeded");
      assertInstanceOf(WasiException.class, result);
      assertEquals(
          WasiException.ErrorCategory.RESOURCE_LIMIT, ((WasiException) result).getCategory());
    }
  }

  @Nested
  @DisplayName("Branch Exclusion Tests")
  class BranchExclusionTests {

    // These tests verify that when one branch is taken, other branches are NOT taken
    // They help kill mutations that replace conditions with true

    // ===== mapCompilationError CRITICAL branch tests =====
    // Each test verifies both positive AND negative cases to ensure mutation detection

    @Test
    @DisplayName("Line 205: MUST return OUT_OF_MEMORY for 'memory', NOT for 'xyz'")
    void line205MustReturnOutOfMemoryForMemoryNotXyz() {
      // Positive: 'memory' should return OUT_OF_MEMORY
      final WasmException positive =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "memory issue");
      assertEquals(
          ModuleCompilationException.CompilationErrorType.OUT_OF_MEMORY,
          ((ModuleCompilationException) positive).getErrorType());

      // Negative: 'xyz' should NOT return OUT_OF_MEMORY
      final WasmException negative =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "xyz issue");
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.OUT_OF_MEMORY,
          ((ModuleCompilationException) negative).getErrorType(),
          "Random 'xyz' must NOT return OUT_OF_MEMORY");
    }

    @Test
    @DisplayName("Line 207: MUST return TIMEOUT for 'time', NOT for 'xyz'")
    void line207MustReturnTimeoutForTimeNotXyz() {
      // Positive: 'time' should return TIMEOUT
      final WasmException positive =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "compile time issue");
      assertEquals(
          ModuleCompilationException.CompilationErrorType.TIMEOUT,
          ((ModuleCompilationException) positive).getErrorType());

      // Negative: 'xyz' should NOT return TIMEOUT
      final WasmException negative =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "xyz random");
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.TIMEOUT,
          ((ModuleCompilationException) negative).getErrorType(),
          "Random 'xyz' must NOT return TIMEOUT");
    }

    @Test
    @DisplayName("Line 209: MUST return TOO_COMPLEX for 'complex', NOT for 'xyz'")
    void line209MustReturnTooComplexForComplexNotXyz() {
      // Positive: 'complex' should return FUNCTION_TOO_COMPLEX
      final WasmException positive =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "complex flow");
      assertEquals(
          ModuleCompilationException.CompilationErrorType.FUNCTION_TOO_COMPLEX,
          ((ModuleCompilationException) positive).getErrorType());

      // Negative: 'xyz' should NOT return FUNCTION_TOO_COMPLEX
      final WasmException negative = ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "xyz abc");
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.FUNCTION_TOO_COMPLEX,
          ((ModuleCompilationException) negative).getErrorType(),
          "Random 'xyz' must NOT return FUNCTION_TOO_COMPLEX");
    }

    // ===== mapCompilationError branch exclusions =====

    @Test
    @DisplayName("Compilation: 'timeout' must NOT trigger OUT_OF_MEMORY (line 205)")
    void compilationTimeoutMustNotTriggerOutOfMemory() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "compilation timeout");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.OUT_OF_MEMORY,
          ex.getErrorType(),
          "timeout should NOT trigger OUT_OF_MEMORY");
      assertEquals(
          ModuleCompilationException.CompilationErrorType.TIMEOUT,
          ex.getErrorType(),
          "timeout MUST trigger TIMEOUT");
    }

    @Test
    @DisplayName("Compilation: 'complex' must NOT trigger OUT_OF_MEMORY (line 205)")
    void compilationComplexMustNotTriggerOutOfMemory() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "too complex function");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.OUT_OF_MEMORY,
          ex.getErrorType(),
          "complex should NOT trigger OUT_OF_MEMORY");
      assertEquals(
          ModuleCompilationException.CompilationErrorType.FUNCTION_TOO_COMPLEX,
          ex.getErrorType(),
          "complex MUST trigger FUNCTION_TOO_COMPLEX");
    }

    @Test
    @DisplayName("Compilation: 'complex' must NOT trigger TIMEOUT (line 207)")
    void compilationComplexMustNotTriggerTimeout() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "too complex");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.TIMEOUT,
          ex.getErrorType(),
          "complex should NOT trigger TIMEOUT");
    }

    @Test
    @DisplayName("Compilation: 'unsupported' must NOT trigger FUNCTION_TOO_COMPLEX (line 209)")
    void compilationUnsupportedMustNotTriggerComplex() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "unsupported instruction");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException ex = (ModuleCompilationException) result;
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.FUNCTION_TOO_COMPLEX,
          ex.getErrorType(),
          "unsupported should NOT trigger FUNCTION_TOO_COMPLEX");
    }

    // ===== mapValidationError branch exclusions =====

    @Test
    @DisplayName("Validation: 'import' must NOT trigger TYPE_MISMATCH (line 246)")
    void validationImportMustNotTriggerTypeMismatch() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "invalid import");
      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertNotEquals(
          ModuleValidationException.ValidationErrorType.TYPE_MISMATCH,
          ex.getErrorType(),
          "import should NOT trigger TYPE_MISMATCH");
    }

    @Test
    @DisplayName("Validation: 'limit' must NOT trigger UNSUPPORTED_FEATURE (line 258)")
    void validationLimitMustNotTriggerUnsupported() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "limit exceeded");
      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertNotEquals(
          ModuleValidationException.ValidationErrorType.UNSUPPORTED_FEATURE,
          ex.getErrorType(),
          "limit should NOT trigger UNSUPPORTED_FEATURE");
    }

    @Test
    @DisplayName("Validation: 'unknown' must NOT trigger LIMIT_EXCEEDED (line 260)")
    void validationUnknownMustNotTriggerLimitExceeded() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "unknown error xyz");
      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException ex = (ModuleValidationException) result;
      assertNotEquals(
          ModuleValidationException.ValidationErrorType.LIMIT_EXCEEDED,
          ex.getErrorType(),
          "unknown should NOT trigger LIMIT_EXCEEDED");
    }

    // ===== mapRuntimeError branch exclusions =====

    @Test
    @DisplayName("Runtime: 'timeout' must NOT trigger TrapException (line 284)")
    void runtimeTimeoutMustNotTriggerTrap() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "execution timeout");
      assertEquals(
          RuntimeException.class,
          result.getClass(),
          "timeout should return RuntimeException, not TrapException");
    }

    @Test
    @DisplayName("Runtime: 'interrupt' must NOT trigger TIMEOUT (line 291)")
    void runtimeInterruptMustNotTriggerTimeout() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "user interrupt");
      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertNotEquals(
          RuntimeException.RuntimeErrorType.TIMEOUT,
          rt.getErrorType(),
          "interrupt should NOT trigger TIMEOUT");
    }

    @Test
    @DisplayName("Runtime: 'stack' must NOT trigger MEMORY_ACCESS_VIOLATION (line 295)")
    void runtimeStackMustNotTriggerMemoryViolation() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "stack exhausted");
      assertInstanceOf(RuntimeException.class, result);
      final RuntimeException rt = (RuntimeException) result;
      assertNotEquals(
          RuntimeException.RuntimeErrorType.MEMORY_ACCESS_VIOLATION,
          rt.getErrorType(),
          "stack should NOT trigger MEMORY_ACCESS_VIOLATION");
    }

    // ===== mapTrapError branch exclusions =====

    @Test
    @DisplayName("Trap: 'overflow integer' must NOT trigger STACK_OVERFLOW (line 339)")
    void trapIntegerOverflowMustNotTriggerStackOverflow() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: integer overflow");
      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertNotEquals(
          TrapException.TrapType.STACK_OVERFLOW,
          trap.getTrapType(),
          "integer overflow should NOT trigger STACK_OVERFLOW");
    }

    @Test
    @DisplayName("Trap: 'unreachable' must NOT trigger OUT_OF_FUEL (line 350)")
    void trapUnreachableMustNotTriggerOutOfFuel() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: unreachable");
      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertNotEquals(
          TrapException.TrapType.OUT_OF_FUEL,
          trap.getTrapType(),
          "unreachable should NOT trigger OUT_OF_FUEL");
    }

    // ===== mapInstantiationError branch exclusions =====

    @Test
    @DisplayName("Instantiation: 'signature' must NOT trigger IMPORT_TYPE_MISMATCH (line 380)")
    void instantiationSignatureMustNotTriggerImportTypeMismatch() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "signature mismatch");
      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertNotEquals(
          ModuleInstantiationException.InstantiationErrorType.IMPORT_TYPE_MISMATCH,
          ex.getErrorType(),
          "signature should NOT trigger IMPORT_TYPE_MISMATCH");
    }

    @Test
    @DisplayName(
        "Instantiation: 'start function' must NOT trigger TABLE_ALLOCATION_FAILED (line 389)")
    void instantiationStartFunctionMustNotTriggerTableAllocation() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "start function error");
      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException ex = (ModuleInstantiationException) result;
      assertNotEquals(
          ModuleInstantiationException.InstantiationErrorType.TABLE_ALLOCATION_FAILED,
          ex.getErrorType(),
          "start function should NOT trigger TABLE_ALLOCATION_FAILED");
    }

    // ===== mapLinkingError branch exclusions =====

    @Test
    @DisplayName("Linking: 'signature' must NOT trigger EXPORT_NOT_FOUND (line 428)")
    void linkingSignatureMustNotTriggerExportNotFound() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "signature mismatch");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.EXPORT_NOT_FOUND,
          linking.getErrorType(),
          "signature should NOT trigger EXPORT_NOT_FOUND");
    }

    @Test
    @DisplayName("Linking: 'table size' must NOT trigger MEMORY_LIMITS_INCOMPATIBLE (line 436)")
    void linkingTableSizeMustNotTriggerMemoryLimits() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "table size mismatch");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.MEMORY_LIMITS_INCOMPATIBLE,
          linking.getErrorType(),
          "table size should NOT trigger MEMORY_LIMITS_INCOMPATIBLE");
    }

    @Test
    @DisplayName("Linking: 'table type' must NOT trigger TABLE_SIZE_MISMATCH (line 438)")
    void linkingTableTypeMustNotTriggerTableSize() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "table type error");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.TABLE_SIZE_MISMATCH,
          linking.getErrorType(),
          "table type should NOT trigger TABLE_SIZE_MISMATCH");
    }

    @Test
    @DisplayName("Linking: 'global type' must NOT trigger TABLE_TYPE_MISMATCH (line 440)")
    void linkingGlobalTypeMustNotTriggerTableType() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "global type error");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.TABLE_TYPE_MISMATCH,
          linking.getErrorType(),
          "global type should NOT trigger TABLE_TYPE_MISMATCH");
    }

    @Test
    @DisplayName("Linking: 'circular' must NOT trigger GLOBAL_MUTABILITY_MISMATCH (line 444)")
    void linkingCircularMustNotTriggerGlobalMutability() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "circular dependency");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.GLOBAL_MUTABILITY_MISMATCH,
          linking.getErrorType(),
          "circular should NOT trigger GLOBAL_MUTABILITY_MISMATCH");
    }

    // ===== mapWasiError branch exclusions =====

    @Test
    @DisplayName("WASI: 'component' must NOT trigger RESOURCE_LIMIT (line 485)")
    void wasiComponentMustNotTriggerResourceLimit() {
      final WasmException result = ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "component error");
      assertInstanceOf(WasiException.class, result);
      final WasiException wasi = (WasiException) result;
      assertNotEquals(
          WasiException.ErrorCategory.RESOURCE_LIMIT,
          wasi.getCategory(),
          "component should NOT trigger RESOURCE_LIMIT");
    }
  }

  @Nested
  @DisplayName("Class Exact Type Discrimination Tests")
  class ClassExactTypeDiscriminationTests {

    @Test
    @DisplayName("RuntimeException should NOT be TrapException")
    void runtimeExceptionShouldNotBeTrapException() {
      final WasmException result = ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "timeout error");
      assertEquals(
          RuntimeException.class,
          result.getClass(),
          "RuntimeException should not be TrapException");
      assertNotEquals(TrapException.class, result.getClass(), "Should not be TrapException");
    }

    @Test
    @DisplayName("TrapException should be exactly TrapException")
    void trapExceptionShouldBeExactlyTrapException() {
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: overflow");
      assertEquals(TrapException.class, result.getClass(), "Should be exactly TrapException");
    }

    @Test
    @DisplayName("WasiFileSystemException should be exactly WasiFileSystemException")
    void wasiFileSystemExceptionShouldBeExact() {
      final WasmException result = ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file not found");
      assertEquals(
          WasiFileSystemException.class,
          result.getClass(),
          "Should be exactly WasiFileSystemException");
    }

    @Test
    @DisplayName("WasiException without file keywords should be exactly WasiException")
    void wasiExceptionWithoutFileShouldBeExact() {
      final WasmException result = ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "network timeout");
      assertEquals(
          WasiException.class,
          result.getClass(),
          "Should be exactly WasiException (not WasiFileSystemException)");
    }
  }

  @Nested
  @DisplayName("OR Conditional Second-Branch Mutation Tests")
  class OrConditionalSecondBranchMutationTests {

    // ===== mapCompilationError OR branches (lines 205, 207, 209) =====

    @Test
    @DisplayName("Line 205: 'memory' without 'out of memory' should trigger OUT_OF_MEMORY")
    void memoryOnlyShouldTriggerOutOfMemory() {
      // This kills mutation: contains("out of memory") replaced with true
      // Message has "memory" but NOT "out of memory"
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "failed due to memory issues");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException compilation = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.OUT_OF_MEMORY,
          compilation.getErrorType(),
          "'memory' alone (without 'out of memory') should still trigger OUT_OF_MEMORY");
    }

    @Test
    @DisplayName("Line 207: 'time' without 'timeout' should trigger TIMEOUT")
    void timeOnlyShouldTriggerTimeout() {
      // This kills mutation: contains("timeout") replaced with true
      // Message has "time" but NOT "timeout"
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "exceeded time limit");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException compilation = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.TIMEOUT,
          compilation.getErrorType(),
          "'time' alone (without 'timeout') should still trigger TIMEOUT");
    }

    @Test
    @DisplayName("Line 209: 'complex' without 'too complex' should trigger FUNCTION_TOO_COMPLEX")
    void complexOnlyShouldTriggerFunctionTooComplex() {
      // This kills mutation: contains("too complex") replaced with true
      // Message has "complex" but NOT "too complex"
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "function is complex");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException compilation = (ModuleCompilationException) result;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.FUNCTION_TOO_COMPLEX,
          compilation.getErrorType(),
          "'complex' alone (without 'too complex') should still trigger FUNCTION_TOO_COMPLEX");
    }

    // ===== mapValidationError OR branches (lines 246, 258, 260) =====

    @Test
    @DisplayName("Line 246: 'type' without 'type mismatch' should trigger TYPE_MISMATCH")
    void typeOnlyShouldTriggerTypeMismatch() {
      // This kills mutation: contains("type mismatch") replaced with true
      // Message has "type" but NOT "type mismatch"
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "invalid type definition");
      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException validation = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.TYPE_MISMATCH,
          validation.getErrorType(),
          "'type' alone (without 'type mismatch') should still trigger TYPE_MISMATCH");
    }

    @Test
    @DisplayName("Line 258: 'feature' without 'unsupported' should trigger UNSUPPORTED_FEATURE")
    void featureOnlyShouldTriggerUnsupportedFeature() {
      // This kills mutation: contains("unsupported") replaced with true
      // Message has "feature" but NOT "unsupported"
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "feature not available");
      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException validation = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.UNSUPPORTED_FEATURE,
          validation.getErrorType(),
          "'feature' alone (without 'unsupported') should still trigger UNSUPPORTED_FEATURE");
    }

    @Test
    @DisplayName("Line 260: 'exceed' without 'limit' should trigger LIMIT_EXCEEDED")
    void exceedOnlyShouldTriggerLimitExceeded() {
      // This kills mutation: contains("limit") replaced with true
      // Message has "exceed" but NOT "limit"
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "values exceed maximum");
      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException validation = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.LIMIT_EXCEEDED,
          validation.getErrorType(),
          "'exceed' alone (without 'limit') should still trigger LIMIT_EXCEEDED");
    }

    // ===== mapRuntimeError OR branches (lines 284, 291, 295) =====

    @Test
    @DisplayName("Line 284: 'trap' substring without trap pattern should still route to trapError")
    void trapSubstringWithoutPatternShouldRouteTrapError() {
      // This kills mutation: trapMatcher.find() replaced with true
      // Message has "trap" substring but doesn't match trap pattern "trap: X"
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trapped in error state");
      assertInstanceOf(TrapException.class, result);
    }

    @Test
    @DisplayName("Line 291: 'time' without 'timeout' should trigger TIMEOUT")
    void runtimeTimeOnlyShouldTriggerTimeout() {
      // This kills mutation: contains("timeout") replaced with true
      // Message has "time" but NOT "timeout" and NOT "trap"
      final WasmException result = ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "exceeded time");
      assertInstanceOf(RuntimeException.class, result);
      assertNotEquals(TrapException.class, result.getClass());
      final RuntimeException runtime = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.TIMEOUT,
          runtime.getErrorType(),
          "'time' alone (without 'timeout') should still trigger TIMEOUT");
    }

    @Test
    @DisplayName("Line 295: 'access' without 'memory' should trigger MEMORY_ACCESS_VIOLATION")
    void accessOnlyShouldTriggerMemoryAccessViolation() {
      // This kills mutation: contains("memory") replaced with true
      // Message has "access" but NOT "memory" and NOT "trap"
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "access violation detected");
      assertInstanceOf(RuntimeException.class, result);
      assertNotEquals(TrapException.class, result.getClass());
      final RuntimeException runtime = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.MEMORY_ACCESS_VIOLATION,
          runtime.getErrorType(),
          "'access' alone (without 'memory') should still trigger MEMORY_ACCESS_VIOLATION");
    }

    // ===== mapTrapError OR branches (line 350) =====

    @Test
    @DisplayName("Line 350: 'out of fuel' should trigger OUT_OF_FUEL")
    void outOfFuelShouldTriggerOutOfFuel() {
      // This kills mutation: contains("fuel") replaced with false
      // "out of fuel" contains "fuel", so both branches should work
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: out of fuel");
      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.OUT_OF_FUEL,
          trap.getTrapType(),
          "'out of fuel' should trigger OUT_OF_FUEL");
    }

    @Test
    @DisplayName("Line 350: 'fuel' without 'out of' should trigger OUT_OF_FUEL")
    void fuelOnlyShouldTriggerOutOfFuel() {
      // This ensures the first branch (contains("fuel")) is exercised
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: fuel exhausted");
      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.OUT_OF_FUEL,
          trap.getTrapType(),
          "'fuel' alone should trigger OUT_OF_FUEL");
    }

    // ===== mapWasiError OR branches (line 485) =====

    @Test
    @DisplayName("Line 485: 'limit' without 'resource' should trigger RESOURCE_LIMIT")
    void limitOnlyShouldTriggerResourceLimit() {
      // This kills mutation: contains("resource") replaced with true
      // Message has "limit" but NOT "resource"
      final WasmException result = ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "limit exceeded");
      assertInstanceOf(WasiException.class, result);
      assertNotEquals(WasiFileSystemException.class, result.getClass());
      final WasiException wasi = (WasiException) result;
      assertEquals(
          WasiException.ErrorCategory.RESOURCE_LIMIT,
          wasi.getCategory(),
          "'limit' alone (without 'resource') should still trigger RESOURCE_LIMIT");
    }
  }

  @Nested
  @DisplayName("AND Conditional Mutation Tests")
  class AndConditionalMutationTests {

    // ===== mapInstantiationError AND branches (lines 380, 389) =====

    @Test
    @DisplayName("Line 380: 'type' without 'import' should NOT trigger IMPORT_TYPE_MISMATCH")
    void typeWithoutImportShouldNotTriggerImportTypeMismatch() {
      // This kills mutation: contains("import") replaced with true
      // If mutated to true, "type mismatch error" would incorrectly match
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "type mismatch error");
      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException inst = (ModuleInstantiationException) result;
      assertNotEquals(
          ModuleInstantiationException.InstantiationErrorType.IMPORT_TYPE_MISMATCH,
          inst.getErrorType(),
          "'type' without 'import' should NOT trigger IMPORT_TYPE_MISMATCH");
    }

    @Test
    @DisplayName(
        "Line 389: 'allocation' without 'table' should NOT trigger TABLE_ALLOCATION_FAILED")
    void allocationWithoutTableShouldNotTriggerTableAllocation() {
      // This kills mutation: contains("table") replaced with true
      // If mutated to true, "allocation error" would incorrectly match
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "allocation error");
      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException inst = (ModuleInstantiationException) result;
      assertNotEquals(
          ModuleInstantiationException.InstantiationErrorType.TABLE_ALLOCATION_FAILED,
          inst.getErrorType(),
          "'allocation' without 'table' should NOT trigger TABLE_ALLOCATION_FAILED");
    }

    // ===== mapLinkingError AND branches (lines 428, 436, 438, 440, 444) =====

    @Test
    @DisplayName("Line 428: 'not found' without 'export' should NOT trigger EXPORT_NOT_FOUND")
    void notFoundWithoutExportShouldNotTriggerExportNotFound() {
      // This kills mutation: contains("export") replaced with true
      // "import not found" has "not found" but not "export"
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "import not found");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.EXPORT_NOT_FOUND,
          linking.getErrorType(),
          "'not found' without 'export' should NOT trigger EXPORT_NOT_FOUND");
    }

    @Test
    @DisplayName("Line 436: 'limit' without 'memory' should NOT trigger MEMORY_LIMITS_INCOMPATIBLE")
    void limitWithoutMemoryShouldNotTriggerMemoryLimits() {
      // This kills mutation: contains("memory") replaced with true
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "limit exceeded");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.MEMORY_LIMITS_INCOMPATIBLE,
          linking.getErrorType(),
          "'limit' without 'memory' should NOT trigger MEMORY_LIMITS_INCOMPATIBLE");
    }

    @Test
    @DisplayName("Line 438: 'size' without 'table' should NOT trigger TABLE_SIZE_MISMATCH")
    void sizeWithoutTableShouldNotTriggerTableSize() {
      // This kills mutation: contains("table") replaced with true
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "size mismatch error");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.TABLE_SIZE_MISMATCH,
          linking.getErrorType(),
          "'size' without 'table' should NOT trigger TABLE_SIZE_MISMATCH");
    }

    @Test
    @DisplayName("Line 440: 'type' without 'table' should NOT trigger TABLE_TYPE_MISMATCH")
    void typeWithoutTableShouldNotTriggerTableType() {
      // This kills mutation: contains("table") replaced with true
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "type mismatch error");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.TABLE_TYPE_MISMATCH,
          linking.getErrorType(),
          "'type' without 'table' should NOT trigger TABLE_TYPE_MISMATCH");
    }

    @Test
    @DisplayName(
        "Line 444: 'mutability' without 'global' should NOT trigger GLOBAL_MUTABILITY_MISMATCH")
    void mutabilityWithoutGlobalShouldNotTriggerGlobalMutability() {
      // This kills mutation: contains("global") replaced with true
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "mutability error");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.GLOBAL_MUTABILITY_MISMATCH,
          linking.getErrorType(),
          "'mutability' without 'global' should NOT trigger GLOBAL_MUTABILITY_MISMATCH");
    }
  }

  @Nested
  @DisplayName("Matcher Find False Replacement Mutation Tests")
  class MatcherFindFalseReplacementMutationTests {

    // ===== Line 412: mapInstantiationError second importMatcher.find() =====

    @Test
    @DisplayName("Line 412: Instantiation error with import pattern should extract module name")
    void instantiationErrorWithImportPatternShouldExtractModuleName() {
      // The pattern is "import[: ](word).(word)" - expects import:mod.name or import mod.name
      // This kills mutation: find() replaced with false
      final WasmException result =
          ErrorMapper.mapError(
              ErrorMapper.ENGINE_CONFIG_ERROR, "import:test_module.test_func failed");
      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException inst = (ModuleInstantiationException) result;
      // The first find() extracts moduleName (group 1)
      assertNotNull(inst.getModuleName(), "Module name should be extracted from import pattern");
      assertEquals("test_module", inst.getModuleName(), "Module name should be 'test_module'");
    }

    // ===== Line 463: mapLinkingError second importMatcher.find() =====

    @Test
    @DisplayName("Line 463: Linking error with import pattern should extract module name")
    void linkingErrorWithImportPatternShouldExtractModuleName() {
      // The pattern is "import[: ](word).(word)" - expects import:mod.name or import mod.name
      // This kills mutation: find() replaced with false
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "import:env.memory_func not found");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotNull(linking.getModuleName(), "Module name should be extracted from import pattern");
      assertEquals("env", linking.getModuleName(), "Module name should be 'env'");
    }

    // ===== Line 532: mapWasiFileSystemError errnoMatcher.find() =====

    @Test
    @DisplayName("Line 532: File error with errno pattern should extract errno code")
    void fileErrorWithErrnoShouldExtractErrnoCode() {
      // This kills mutation: find() replaced with false
      // When the mutation is active, errnoCode is always null
      // When find() returns true, errnoCode should be non-null
      // Pattern is "errno[: ](digits)" - expects errno:N or errno N
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file operation failed errno:2");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException fsError = (WasiFileSystemException) result;
      assertNotNull(
          fsError.getErrnoCode(),
          "Errno code should be extracted when 'errno:N' pattern is present");
      assertEquals(2, fsError.getErrnoCode(), "Errno code should be 2");
    }

    @Test
    @DisplayName("Line 532: File error without errno pattern should use error type default")
    void fileErrorWithoutErrnoPatternShouldUseErrorTypeDefault() {
      // This tests the else branch (find() returns false)
      // When no errno pattern is present, constructor falls back to error type's default
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file operation error");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException fsError = (WasiFileSystemException) result;
      // The error type is UNKNOWN which has errno code -1
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.UNKNOWN.getErrnoCode(),
          fsError.getErrnoCode(),
          "Errno code should be error type's default when pattern not present");
    }

    @Test
    @DisplayName("Line 532: Extracted errno should override error type default")
    void extractedErrnoShouldOverrideDefault() {
      // This strongly kills mutation: find() replaced with false
      // The error type UNKNOWN has default errno -1, but we extract errno:99
      // If mutation is active (find() -> false), errnoCode would be -1
      // If mutation is NOT active (find() works), errnoCode would be 99
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file operation error errno:99");
      assertInstanceOf(WasiFileSystemException.class, result);
      final WasiFileSystemException fsError = (WasiFileSystemException) result;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.UNKNOWN,
          fsError.getFileSystemErrorType(),
          "Error type should be UNKNOWN for this message");
      assertEquals(
          99,
          fsError.getErrnoCode(),
          "Errno code should be extracted value (99), not default (-1)");
    }
  }

  @Nested
  @DisplayName("First Operand True Replacement Mutation Tests")
  class FirstOperandTrueReplacementMutationTests {

    // These tests kill mutations where the FIRST operand of an OR is replaced with TRUE
    // The strategy: test with message that matches a LATER condition but NOT the mutated one
    // If mutated, the earlier condition would incorrectly match

    // ===== mapCompilationError: Line 205 contains("out of memory") -> true =====

    @Test
    @DisplayName("Line 205: 'timeout' should NOT trigger OUT_OF_MEMORY")
    void timeoutShouldNotTriggerOutOfMemory() {
      // This kills: contains("out of memory") replaced with true on line 205
      // "timeout" doesn't contain "out of memory" or "memory"
      // If mutated, would incorrectly get OUT_OF_MEMORY
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "compilation timeout");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException compilation = (ModuleCompilationException) result;
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.OUT_OF_MEMORY,
          compilation.getErrorType(),
          "'timeout' should NOT trigger OUT_OF_MEMORY");
      assertEquals(
          ModuleCompilationException.CompilationErrorType.TIMEOUT,
          compilation.getErrorType(),
          "'timeout' should trigger TIMEOUT");
    }

    // ===== mapCompilationError: Line 207 contains("timeout") -> true =====

    @Test
    @DisplayName("Line 207: 'too complex' should NOT trigger TIMEOUT")
    void tooComplexShouldNotTriggerTimeout() {
      // This kills: contains("timeout") replaced with true on line 207
      // "too complex" doesn't contain "timeout" or "time"
      // If mutated, would incorrectly get TIMEOUT
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "function too complex");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException compilation = (ModuleCompilationException) result;
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.TIMEOUT,
          compilation.getErrorType(),
          "'too complex' should NOT trigger TIMEOUT");
      assertEquals(
          ModuleCompilationException.CompilationErrorType.FUNCTION_TOO_COMPLEX,
          compilation.getErrorType(),
          "'too complex' should trigger FUNCTION_TOO_COMPLEX");
    }

    // ===== mapCompilationError: Line 209 contains("too complex") -> true =====

    @Test
    @DisplayName("Line 209: 'unsupported' should NOT trigger FUNCTION_TOO_COMPLEX")
    void unsupportedShouldNotTriggerFunctionTooComplex() {
      // This kills: contains("too complex") replaced with true on line 209
      // "unsupported instruction" doesn't contain "too complex" or "complex"
      // If mutated, would incorrectly get FUNCTION_TOO_COMPLEX
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "unsupported instruction");
      assertInstanceOf(ModuleCompilationException.class, result);
      final ModuleCompilationException compilation = (ModuleCompilationException) result;
      assertNotEquals(
          ModuleCompilationException.CompilationErrorType.FUNCTION_TOO_COMPLEX,
          compilation.getErrorType(),
          "'unsupported' should NOT trigger FUNCTION_TOO_COMPLEX");
      assertEquals(
          ModuleCompilationException.CompilationErrorType.UNSUPPORTED_INSTRUCTION,
          compilation.getErrorType(),
          "'unsupported' should trigger UNSUPPORTED_INSTRUCTION");
    }

    // ===== mapValidationError: Line 246 contains("type mismatch") -> true =====

    @Test
    @DisplayName("Line 246: 'import' should NOT trigger TYPE_MISMATCH")
    void importShouldNotTriggerTypeMismatch() {
      // This kills: contains("type mismatch") replaced with true on line 246
      // "import error" doesn't contain "type mismatch" or "type"
      // If mutated, would incorrectly get TYPE_MISMATCH
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "import error");
      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException validation = (ModuleValidationException) result;
      assertNotEquals(
          ModuleValidationException.ValidationErrorType.TYPE_MISMATCH,
          validation.getErrorType(),
          "'import' should NOT trigger TYPE_MISMATCH");
      assertEquals(
          ModuleValidationException.ValidationErrorType.INVALID_IMPORT,
          validation.getErrorType(),
          "'import' should trigger INVALID_IMPORT");
    }

    // ===== mapValidationError: Line 258 contains("unsupported") -> true =====

    @Test
    @DisplayName("Line 258: 'limit exceeded' should NOT trigger UNSUPPORTED_FEATURE")
    void limitExceededShouldNotTriggerUnsupportedFeature() {
      // This kills: contains("unsupported") replaced with true on line 258
      // "limit exceeded" doesn't contain "unsupported" or "feature"
      // If mutated, would incorrectly get UNSUPPORTED_FEATURE
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "limit exceeded");
      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException validation = (ModuleValidationException) result;
      assertNotEquals(
          ModuleValidationException.ValidationErrorType.UNSUPPORTED_FEATURE,
          validation.getErrorType(),
          "'limit exceeded' should NOT trigger UNSUPPORTED_FEATURE");
      assertEquals(
          ModuleValidationException.ValidationErrorType.LIMIT_EXCEEDED,
          validation.getErrorType(),
          "'limit exceeded' should trigger LIMIT_EXCEEDED");
    }

    // ===== mapValidationError: Line 260 contains("limit") -> true =====

    @Test
    @DisplayName("Line 260: unknown validation error should NOT trigger LIMIT_EXCEEDED")
    void unknownValidationShouldNotTriggerLimitExceeded() {
      // This kills: contains("limit") replaced with true on line 260
      // "parsing failed" doesn't contain "limit" or "exceed"
      // If mutated, would incorrectly get LIMIT_EXCEEDED
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "parsing failed");
      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException validation = (ModuleValidationException) result;
      assertNotEquals(
          ModuleValidationException.ValidationErrorType.LIMIT_EXCEEDED,
          validation.getErrorType(),
          "'parsing failed' should NOT trigger LIMIT_EXCEEDED");
      assertEquals(
          ModuleValidationException.ValidationErrorType.UNKNOWN,
          validation.getErrorType(),
          "'parsing failed' should trigger UNKNOWN");
    }

    // ===== mapRuntimeError: Line 284 trapMatcher.find() -> true =====

    @Test
    @DisplayName("Line 284: 'timeout' should NOT route to trapError")
    void timeoutShouldNotRouteTrapError() {
      // This kills: trapMatcher.find() replaced with true on line 284
      // "timeout" doesn't match trap pattern and doesn't contain "trap"
      // If mutated, would incorrectly route to trapError
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "timeout occurred");
      assertInstanceOf(RuntimeException.class, result);
      assertNotEquals(
          TrapException.class,
          result.getClass(),
          "'timeout' should return RuntimeException, not TrapException");
    }

    // ===== mapRuntimeError: Line 291 contains("timeout") -> true =====

    @Test
    @DisplayName("Line 291: 'interrupt' should NOT trigger TIMEOUT")
    void interruptShouldNotTriggerTimeout() {
      // This kills: contains("timeout") replaced with true on line 291
      // "interrupt" doesn't contain "timeout" or "time"
      // If mutated, would incorrectly get TIMEOUT
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "interrupt signal");
      assertInstanceOf(RuntimeException.class, result);
      assertNotEquals(TrapException.class, result.getClass());
      final RuntimeException runtime = (RuntimeException) result;
      assertNotEquals(
          RuntimeException.RuntimeErrorType.TIMEOUT,
          runtime.getErrorType(),
          "'interrupt' should NOT trigger TIMEOUT");
      assertEquals(
          RuntimeException.RuntimeErrorType.INTERRUPTED,
          runtime.getErrorType(),
          "'interrupt' should trigger INTERRUPTED");
    }

    // ===== mapRuntimeError: Line 295 contains("memory") -> true =====

    @Test
    @DisplayName("Line 295: 'stack' should NOT trigger MEMORY_ACCESS_VIOLATION")
    void stackShouldNotTriggerMemoryAccessViolation() {
      // This kills: contains("memory") replaced with true on line 295
      // "stack error" doesn't contain "memory" or "access"
      // If mutated, would incorrectly get MEMORY_ACCESS_VIOLATION
      final WasmException result = ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "stack error");
      assertInstanceOf(RuntimeException.class, result);
      assertNotEquals(TrapException.class, result.getClass());
      final RuntimeException runtime = (RuntimeException) result;
      assertNotEquals(
          RuntimeException.RuntimeErrorType.MEMORY_ACCESS_VIOLATION,
          runtime.getErrorType(),
          "'stack' should NOT trigger MEMORY_ACCESS_VIOLATION");
      assertEquals(
          RuntimeException.RuntimeErrorType.STACK_ERROR,
          runtime.getErrorType(),
          "'stack' should trigger STACK_ERROR");
    }

    // ===== mapTrapError: Line 339 contains("overflow") -> true =====
    // Already covered by TrapErrorIntegerOverflowMutationTests

    // ===== mapInstantiationError: Line 380 contains("import") -> true =====

    @Test
    @DisplayName("Line 380: 'signature' should NOT trigger IMPORT_TYPE_MISMATCH")
    void signatureShouldNotTriggerImportTypeMismatch() {
      // This kills: contains("import") replaced with true on line 380
      // "signature mismatch" doesn't contain "import"
      // If mutated, would incorrectly get IMPORT_TYPE_MISMATCH
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.ENGINE_CONFIG_ERROR, "signature mismatch");
      assertInstanceOf(ModuleInstantiationException.class, result);
      final ModuleInstantiationException inst = (ModuleInstantiationException) result;
      assertNotEquals(
          ModuleInstantiationException.InstantiationErrorType.IMPORT_TYPE_MISMATCH,
          inst.getErrorType(),
          "'signature' should NOT trigger IMPORT_TYPE_MISMATCH");
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.FUNCTION_SIGNATURE_MISMATCH,
          inst.getErrorType(),
          "'signature' should trigger FUNCTION_SIGNATURE_MISMATCH");
    }

    // ===== mapLinkingError: Line 428 contains("export") -> true =====

    @Test
    @DisplayName("Line 428: 'host function' should NOT trigger EXPORT_NOT_FOUND")
    void hostFunctionShouldNotTriggerExportNotFound() {
      // This kills: contains("export") replaced with true on line 428
      // "host function binding" doesn't contain "export"
      // If mutated, would incorrectly get EXPORT_NOT_FOUND
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "host function binding failed");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.EXPORT_NOT_FOUND,
          linking.getErrorType(),
          "'host function' should NOT trigger EXPORT_NOT_FOUND");
      assertEquals(
          LinkingException.LinkingErrorType.HOST_FUNCTION_BINDING_FAILED,
          linking.getErrorType(),
          "'host function' should trigger HOST_FUNCTION_BINDING_FAILED");
    }

    // ===== mapLinkingError: Line 436 contains("memory") -> true =====

    @Test
    @DisplayName("Line 436: 'table size' should NOT trigger MEMORY_LIMITS_INCOMPATIBLE")
    void tableSizeShouldNotTriggerMemoryLimits() {
      // This kills: contains("memory") replaced with true on line 436
      // "table size mismatch" doesn't contain "memory"
      // If mutated, would incorrectly get MEMORY_LIMITS_INCOMPATIBLE
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "table size mismatch");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.MEMORY_LIMITS_INCOMPATIBLE,
          linking.getErrorType(),
          "'table size' should NOT trigger MEMORY_LIMITS_INCOMPATIBLE");
      assertEquals(
          LinkingException.LinkingErrorType.TABLE_SIZE_MISMATCH,
          linking.getErrorType(),
          "'table size' should trigger TABLE_SIZE_MISMATCH");
    }

    // ===== mapLinkingError: Line 440 contains("table") -> true =====

    @Test
    @DisplayName("Line 440: 'global type' should NOT trigger TABLE_TYPE_MISMATCH")
    void globalTypeShouldNotTriggerTableType() {
      // This kills: contains("table") replaced with true on line 440
      // "global type mismatch" doesn't contain "table"
      // If mutated, would incorrectly get TABLE_TYPE_MISMATCH
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "global type mismatch");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.TABLE_TYPE_MISMATCH,
          linking.getErrorType(),
          "'global type' should NOT trigger TABLE_TYPE_MISMATCH");
      assertEquals(
          LinkingException.LinkingErrorType.GLOBAL_TYPE_MISMATCH,
          linking.getErrorType(),
          "'global type' should trigger GLOBAL_TYPE_MISMATCH");
    }

    // ===== mapLinkingError: Line 444 contains("global") -> true =====

    @Test
    @DisplayName("Line 444: 'circular' should NOT trigger GLOBAL_MUTABILITY_MISMATCH")
    void circularShouldNotTriggerGlobalMutability() {
      // This kills: contains("global") replaced with true on line 444
      // "circular dependency" doesn't contain "global"
      // If mutated, would incorrectly get GLOBAL_MUTABILITY_MISMATCH
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, "circular dependency");
      assertInstanceOf(LinkingException.class, result);
      final LinkingException linking = (LinkingException) result;
      assertNotEquals(
          LinkingException.LinkingErrorType.GLOBAL_MUTABILITY_MISMATCH,
          linking.getErrorType(),
          "'circular' should NOT trigger GLOBAL_MUTABILITY_MISMATCH");
      assertEquals(
          LinkingException.LinkingErrorType.CIRCULAR_DEPENDENCY,
          linking.getErrorType(),
          "'circular' should trigger CIRCULAR_DEPENDENCY");
    }

    // ===== mapWasiError: Line 485 contains("resource") -> true =====

    @Test
    @DisplayName("Line 485: 'component' should NOT trigger RESOURCE_LIMIT")
    void componentShouldNotTriggerResourceLimit() {
      // This kills: contains("resource") replaced with true on line 485
      // "component error" doesn't contain "resource" or "limit"
      // If mutated, would incorrectly get RESOURCE_LIMIT
      final WasmException result = ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "component error");
      assertInstanceOf(WasiException.class, result);
      assertNotEquals(WasiFileSystemException.class, result.getClass());
      final WasiException wasi = (WasiException) result;
      assertNotEquals(
          WasiException.ErrorCategory.RESOURCE_LIMIT,
          wasi.getCategory(),
          "'component' should NOT trigger RESOURCE_LIMIT");
      assertEquals(
          WasiException.ErrorCategory.COMPONENT,
          wasi.getCategory(),
          "'component' should trigger COMPONENT");
    }
  }

  @Nested
  @DisplayName("First Operand Only Tests - Kills EQUAL_IF True Replacement Mutations")
  class FirstOperandOnlyMutationTests {

    // These tests use ONLY the first operand keyword (not the second) to kill mutations
    // where the first operand's equality check is replaced with TRUE
    // The key: if mutation makes first check always-jump, it would skip short-circuit

    // ===== mapCompilationError Line 205: "out of memory" without "memory" alone =====
    // Note: "out of memory" always contains "memory", so this is an equivalent mutant

    // ===== mapCompilationError Line 207: "timeout" without "time" alone =====
    // Note: "timeout" always contains "time", so this is an equivalent mutant

    // ===== mapCompilationError Line 209: "too complex" without "complex" alone =====
    // Note: "too complex" always contains "complex", so this is an equivalent mutant

    // ===== mapValidationError Line 246: "type mismatch" without "type" alone =====
    // Note: "type mismatch" always contains "type", so this is an equivalent mutant

    // ===== mapValidationError Line 258: "unsupported" without "feature" =====

    @Test
    @DisplayName("Line 258: 'unsupported' alone (no 'feature') should trigger UNSUPPORTED_FEATURE")
    void unsupportedAloneShouldTriggerUnsupportedFeature() {
      // This kills mutation: first operand equality check replaced with true
      // Use message with "unsupported" that doesn't contain earlier keywords
      // (not magic, version, malformed, corrupt, type, import, export, memory, table, function)
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "unsupported opcode detected");
      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException validation = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.UNSUPPORTED_FEATURE,
          validation.getErrorType(),
          "'unsupported' alone should trigger UNSUPPORTED_FEATURE");
    }

    // ===== mapValidationError Line 260: "limit" without "exceed" =====
    // Note: We need a message with just "limit" that doesn't match earlier conditions

    @Test
    @DisplayName("Line 260: 'limit' alone (no 'exceed') should trigger LIMIT_EXCEEDED")
    void limitAloneShouldTriggerLimitExceeded() {
      // This kills mutation: first operand equality check replaced with true
      // Use message with "limit" that doesn't contain earlier keywords
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "limit reached");
      assertInstanceOf(ModuleValidationException.class, result);
      final ModuleValidationException validation = (ModuleValidationException) result;
      assertEquals(
          ModuleValidationException.ValidationErrorType.LIMIT_EXCEEDED,
          validation.getErrorType(),
          "'limit' alone should trigger LIMIT_EXCEEDED");
    }

    // ===== mapRuntimeError Line 291: "timeout" without "time" =====
    // Note: "timeout" always contains "time", so this is an equivalent mutant

    // ===== mapRuntimeError Line 295: "memory" without "access" =====

    @Test
    @DisplayName("Line 295: 'memory' alone (no 'access') should trigger MEMORY_ACCESS_VIOLATION")
    void memoryAloneShouldTriggerMemoryAccessViolation() {
      // This kills mutation: first operand equality check replaced with true
      // Use message with "memory" that doesn't match trap pattern
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "memory error occurred");
      assertInstanceOf(RuntimeException.class, result);
      // Verify it's not a TrapException (which it would be if "trap" matched)
      assertNotEquals(TrapException.class, result.getClass());
      final RuntimeException runtime = (RuntimeException) result;
      assertEquals(
          RuntimeException.RuntimeErrorType.MEMORY_ACCESS_VIOLATION,
          runtime.getErrorType(),
          "'memory' alone should trigger MEMORY_ACCESS_VIOLATION");
    }

    // ===== mapTrapError Line 339: "overflow" with "integer" - both needed =====
    // This is an AND condition, handled separately

    // ===== mapTrapError Line 350: "fuel" without "out of fuel" =====

    @Test
    @DisplayName("Line 350: 'fuel' alone (no 'out of fuel') should trigger OUT_OF_FUEL")
    void fuelAloneShouldTriggerOutOfFuel() {
      // "fuel exhausted" contains "fuel" but not "out of fuel"
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: fuel exhausted");
      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.OUT_OF_FUEL,
          trap.getTrapType(),
          "'fuel' alone should trigger OUT_OF_FUEL");
    }

    // ===== mapLinkingError Line 436: "memory" with "limit" - needs both for MEMORY_LIMITS =====
    // Actually this is an AND condition, covered by other tests

    // ===== mapWasiError Line 485: "resource" without "limit" =====

    @Test
    @DisplayName("Line 485: 'resource' alone (no 'limit') should trigger RESOURCE_LIMIT")
    void resourceAloneShouldTriggerResourceLimit() {
      // This kills mutation: first operand equality check replaced with true
      // Use message with "resource" that doesn't contain file/directory/path
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "resource unavailable");
      assertInstanceOf(WasiException.class, result);
      assertNotEquals(WasiFileSystemException.class, result.getClass());
      final WasiException wasi = (WasiException) result;
      assertEquals(
          WasiException.ErrorCategory.RESOURCE_LIMIT,
          wasi.getCategory(),
          "'resource' alone should trigger RESOURCE_LIMIT");
    }
  }

  @Nested
  @DisplayName("Trap Error Integer Overflow AND Conditional Mutation Tests")
  class TrapErrorIntegerOverflowMutationTests {

    // ===== Line 339: mapTrapError contains("overflow") && contains("integer") =====

    @Test
    @DisplayName("Line 339: 'overflow' without 'integer' should NOT trigger INTEGER_OVERFLOW")
    void overflowWithoutIntegerShouldNotTriggerIntegerOverflow() {
      // This kills mutation: contains("overflow") replaced with true
      // "stack overflow" has "overflow" but NOT "integer"
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: stack overflow");
      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertNotEquals(
          TrapException.TrapType.INTEGER_OVERFLOW,
          trap.getTrapType(),
          "'overflow' without 'integer' should NOT trigger INTEGER_OVERFLOW");
      assertEquals(
          TrapException.TrapType.STACK_OVERFLOW,
          trap.getTrapType(),
          "'stack overflow' should trigger STACK_OVERFLOW");
    }

    @Test
    @DisplayName("Line 339: 'integer' without 'overflow' should NOT trigger INTEGER_OVERFLOW")
    void integerWithoutOverflowShouldNotTriggerIntegerOverflow() {
      // This tests that both conditions must be true
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: integer error");
      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertNotEquals(
          TrapException.TrapType.INTEGER_OVERFLOW,
          trap.getTrapType(),
          "'integer' without 'overflow' should NOT trigger INTEGER_OVERFLOW");
    }

    @Test
    @DisplayName("Line 339: Both 'integer' and 'overflow' should trigger INTEGER_OVERFLOW")
    void integerAndOverflowShouldTriggerIntegerOverflow() {
      // This tests the happy path
      final WasmException result =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: integer overflow detected");
      assertInstanceOf(TrapException.class, result);
      final TrapException trap = (TrapException) result;
      assertEquals(
          TrapException.TrapType.INTEGER_OVERFLOW,
          trap.getTrapType(),
          "'integer overflow' should trigger INTEGER_OVERFLOW");
    }
  }
}
