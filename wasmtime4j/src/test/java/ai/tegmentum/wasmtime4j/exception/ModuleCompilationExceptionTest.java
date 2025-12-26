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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.ModuleCompilationException.CompilationErrorType;
import ai.tegmentum.wasmtime4j.exception.ModuleCompilationException.CompilationPhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ModuleCompilationException} class.
 *
 * <p>This test class verifies the construction and behavior of module compilation exceptions,
 * including error types, phases, and recovery suggestions.
 */
@DisplayName("ModuleCompilationException Tests")
class ModuleCompilationExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("ModuleCompilationException should extend CompilationException")
    void shouldExtendCompilationException() {
      assertTrue(
          CompilationException.class.isAssignableFrom(ModuleCompilationException.class),
          "ModuleCompilationException should extend CompilationException");
    }

    @Test
    @DisplayName("ModuleCompilationException should be serializable")
    void shouldBeSerializable() {
      assertTrue(
          java.io.Serializable.class.isAssignableFrom(ModuleCompilationException.class),
          "ModuleCompilationException should be serializable");
    }
  }

  @Nested
  @DisplayName("CompilationErrorType Enum Tests")
  class CompilationErrorTypeEnumTests {

    @Test
    @DisplayName("Should have OUT_OF_MEMORY value")
    void shouldHaveOutOfMemoryValue() {
      assertNotNull(
          CompilationErrorType.valueOf("OUT_OF_MEMORY"), "Should have OUT_OF_MEMORY value");
    }

    @Test
    @DisplayName("Should have TIMEOUT value")
    void shouldHaveTimeoutValue() {
      assertNotNull(CompilationErrorType.valueOf("TIMEOUT"), "Should have TIMEOUT value");
    }

    @Test
    @DisplayName("Should have FUNCTION_TOO_COMPLEX value")
    void shouldHaveFunctionTooComplexValue() {
      assertNotNull(
          CompilationErrorType.valueOf("FUNCTION_TOO_COMPLEX"),
          "Should have FUNCTION_TOO_COMPLEX value");
    }

    @Test
    @DisplayName("Should have UNSUPPORTED_INSTRUCTION value")
    void shouldHaveUnsupportedInstructionValue() {
      assertNotNull(
          CompilationErrorType.valueOf("UNSUPPORTED_INSTRUCTION"),
          "Should have UNSUPPORTED_INSTRUCTION value");
    }

    @Test
    @DisplayName("Should have CODE_GENERATION_FAILED value")
    void shouldHaveCodeGenerationFailedValue() {
      assertNotNull(
          CompilationErrorType.valueOf("CODE_GENERATION_FAILED"),
          "Should have CODE_GENERATION_FAILED value");
    }

    @Test
    @DisplayName("Should have OPTIMIZATION_FAILED value")
    void shouldHaveOptimizationFailedValue() {
      assertNotNull(
          CompilationErrorType.valueOf("OPTIMIZATION_FAILED"),
          "Should have OPTIMIZATION_FAILED value");
    }

    @Test
    @DisplayName("Should have UNKNOWN value")
    void shouldHaveUnknownValue() {
      assertNotNull(CompilationErrorType.valueOf("UNKNOWN"), "Should have UNKNOWN value");
    }

    @Test
    @DisplayName("Each error type should have description")
    void eachErrorTypeShouldHaveDescription() {
      for (final CompilationErrorType type : CompilationErrorType.values()) {
        assertNotNull(type.getDescription(), type.name() + " should have description");
        assertFalse(
            type.getDescription().isEmpty(), type.name() + " should have non-empty description");
      }
    }

    @Test
    @DisplayName("Should have 13 error types")
    void shouldHave13ErrorTypes() {
      assertEquals(13, CompilationErrorType.values().length, "Should have 13 error types");
    }
  }

  @Nested
  @DisplayName("CompilationPhase Enum Tests")
  class CompilationPhaseEnumTests {

    @Test
    @DisplayName("Should have INITIALIZATION value")
    void shouldHaveInitializationValue() {
      assertNotNull(CompilationPhase.valueOf("INITIALIZATION"), "Should have INITIALIZATION value");
    }

    @Test
    @DisplayName("Should have CFG_CONSTRUCTION value")
    void shouldHaveCfgConstructionValue() {
      assertNotNull(
          CompilationPhase.valueOf("CFG_CONSTRUCTION"), "Should have CFG_CONSTRUCTION value");
    }

    @Test
    @DisplayName("Should have OPTIMIZATION value")
    void shouldHaveOptimizationValue() {
      assertNotNull(CompilationPhase.valueOf("OPTIMIZATION"), "Should have OPTIMIZATION value");
    }

    @Test
    @DisplayName("Should have REGISTER_ALLOCATION value")
    void shouldHaveRegisterAllocationValue() {
      assertNotNull(
          CompilationPhase.valueOf("REGISTER_ALLOCATION"), "Should have REGISTER_ALLOCATION value");
    }

    @Test
    @DisplayName("Should have CODE_GENERATION value")
    void shouldHaveCodeGenerationValue() {
      assertNotNull(
          CompilationPhase.valueOf("CODE_GENERATION"), "Should have CODE_GENERATION value");
    }

    @Test
    @DisplayName("Should have UNKNOWN value")
    void shouldHaveUnknownPhaseValue() {
      assertNotNull(CompilationPhase.valueOf("UNKNOWN"), "Should have UNKNOWN value");
    }

    @Test
    @DisplayName("Each phase should have description")
    void eachPhaseShouldHaveDescription() {
      for (final CompilationPhase phase : CompilationPhase.values()) {
        assertNotNull(phase.getDescription(), phase.name() + " should have description");
        assertFalse(
            phase.getDescription().isEmpty(), phase.name() + " should have non-empty description");
      }
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with error type and message should set fields")
    void constructorWithErrorTypeAndMessage() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(CompilationErrorType.OUT_OF_MEMORY, "Ran out of memory");

      assertEquals(
          CompilationErrorType.OUT_OF_MEMORY,
          exception.getErrorType(),
          "Error type should be OUT_OF_MEMORY");
      assertTrue(
          exception.getMessage().contains("Ran out of memory"),
          "Message should contain error text");
      assertEquals(
          CompilationPhase.UNKNOWN, exception.getPhase(), "Phase should default to UNKNOWN");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("Constructor with error type, message, and cause should set all")
    void constructorWithErrorTypeMessageAndCause() {
      final Throwable cause = new RuntimeException("Root cause");
      final ModuleCompilationException exception =
          new ModuleCompilationException(CompilationErrorType.TIMEOUT, "Timeout exceeded", cause);

      assertEquals(
          CompilationErrorType.TIMEOUT, exception.getErrorType(), "Error type should be TIMEOUT");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }

    @Test
    @DisplayName("Full constructor should set all fields")
    void fullConstructorShouldSetAllFields() {
      final Throwable cause = new RuntimeException("Root cause");
      final ModuleCompilationException exception =
          new ModuleCompilationException(
              CompilationErrorType.FUNCTION_TOO_COMPLEX,
              "Function too complex",
              CompilationPhase.OPTIMIZATION,
              "my_function",
              42,
              cause);

      assertEquals(
          CompilationErrorType.FUNCTION_TOO_COMPLEX,
          exception.getErrorType(),
          "Error type should match");
      assertEquals(
          CompilationPhase.OPTIMIZATION, exception.getPhase(), "Phase should be OPTIMIZATION");
      assertEquals(
          "my_function", exception.getFunctionName(), "Function name should be 'my_function'");
      assertEquals(
          Integer.valueOf(42), exception.getFunctionIndex(), "Function index should be 42");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }

    @Test
    @DisplayName("Constructor should handle null error type")
    void constructorShouldHandleNullErrorType() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(
              null, "Error message", CompilationPhase.UNKNOWN, null, null, null);

      assertEquals(
          CompilationErrorType.UNKNOWN,
          exception.getErrorType(),
          "Null error type should default to UNKNOWN");
    }

    @Test
    @DisplayName("Constructor should handle null phase")
    void constructorShouldHandleNullPhase() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(
              CompilationErrorType.OUT_OF_MEMORY, "Error message", null, null, null, null);

      assertEquals(
          CompilationPhase.UNKNOWN, exception.getPhase(), "Null phase should default to UNKNOWN");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("getErrorType should return error type")
    void getErrorTypeShouldReturnErrorType() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(
              CompilationErrorType.CODE_GENERATION_FAILED, "Code gen failed");

      assertEquals(
          CompilationErrorType.CODE_GENERATION_FAILED,
          exception.getErrorType(),
          "getErrorType should return CODE_GENERATION_FAILED");
    }

    @Test
    @DisplayName("getPhase should return compilation phase")
    void getPhaseShouldReturnPhase() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(
              CompilationErrorType.OPTIMIZATION_FAILED,
              "Optimization failed",
              CompilationPhase.OPTIMIZATION,
              null,
              null,
              null);

      assertEquals(
          CompilationPhase.OPTIMIZATION,
          exception.getPhase(),
          "getPhase should return OPTIMIZATION");
    }

    @Test
    @DisplayName("getFunctionName should return function name")
    void getFunctionNameShouldReturnFunctionName() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(
              CompilationErrorType.UNKNOWN,
              "Error",
              CompilationPhase.UNKNOWN,
              "test_func",
              null,
              null);

      assertEquals(
          "test_func", exception.getFunctionName(), "getFunctionName should return 'test_func'");
    }

    @Test
    @DisplayName("getFunctionIndex should return function index")
    void getFunctionIndexShouldReturnFunctionIndex() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(
              CompilationErrorType.UNKNOWN, "Error", CompilationPhase.UNKNOWN, null, 100, null);

      assertEquals(
          Integer.valueOf(100), exception.getFunctionIndex(), "getFunctionIndex should return 100");
    }

    @Test
    @DisplayName("getRecoverySuggestion should return non-null suggestion")
    void getRecoverySuggestionShouldReturnNonNull() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(CompilationErrorType.OUT_OF_MEMORY, "Error");

      assertNotNull(exception.getRecoverySuggestion(), "Recovery suggestion should not be null");
      assertFalse(
          exception.getRecoverySuggestion().isEmpty(), "Recovery suggestion should not be empty");
    }
  }

  @Nested
  @DisplayName("Error Category Check Tests")
  class ErrorCategoryCheckTests {

    @Test
    @DisplayName("isResourceError should return true for resource errors")
    void isResourceErrorShouldReturnTrueForResourceErrors() {
      final ModuleCompilationException outOfMemory =
          new ModuleCompilationException(CompilationErrorType.OUT_OF_MEMORY, "Error");
      final ModuleCompilationException timeout =
          new ModuleCompilationException(CompilationErrorType.TIMEOUT, "Error");
      final ModuleCompilationException resourceLimit =
          new ModuleCompilationException(CompilationErrorType.RESOURCE_LIMIT_EXCEEDED, "Error");

      assertTrue(outOfMemory.isResourceError(), "OUT_OF_MEMORY should be resource error");
      assertTrue(timeout.isResourceError(), "TIMEOUT should be resource error");
      assertTrue(
          resourceLimit.isResourceError(), "RESOURCE_LIMIT_EXCEEDED should be resource error");
    }

    @Test
    @DisplayName("isResourceError should return false for non-resource errors")
    void isResourceErrorShouldReturnFalseForNonResourceErrors() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(CompilationErrorType.UNSUPPORTED_INSTRUCTION, "Error");

      assertFalse(
          exception.isResourceError(), "UNSUPPORTED_INSTRUCTION should not be resource error");
    }

    @Test
    @DisplayName("isComplexityError should return true for complexity errors")
    void isComplexityErrorShouldReturnTrueForComplexityErrors() {
      final ModuleCompilationException tooComplex =
          new ModuleCompilationException(CompilationErrorType.FUNCTION_TOO_COMPLEX, "Error");
      final ModuleCompilationException cfgFailed =
          new ModuleCompilationException(CompilationErrorType.CFG_CONSTRUCTION_FAILED, "Error");
      final ModuleCompilationException regAlloc =
          new ModuleCompilationException(CompilationErrorType.REGISTER_ALLOCATION_FAILED, "Error");

      assertTrue(tooComplex.isComplexityError(), "FUNCTION_TOO_COMPLEX should be complexity error");
      assertTrue(
          cfgFailed.isComplexityError(), "CFG_CONSTRUCTION_FAILED should be complexity error");
      assertTrue(
          regAlloc.isComplexityError(), "REGISTER_ALLOCATION_FAILED should be complexity error");
    }

    @Test
    @DisplayName("isFeatureError should return true for feature errors")
    void isFeatureErrorShouldReturnTrueForFeatureErrors() {
      final ModuleCompilationException unsupported =
          new ModuleCompilationException(CompilationErrorType.UNSUPPORTED_INSTRUCTION, "Error");
      final ModuleCompilationException unsupportedTarget =
          new ModuleCompilationException(CompilationErrorType.UNSUPPORTED_TARGET, "Error");
      final ModuleCompilationException featureConfig =
          new ModuleCompilationException(CompilationErrorType.FEATURE_CONFIGURATION_ERROR, "Error");

      assertTrue(unsupported.isFeatureError(), "UNSUPPORTED_INSTRUCTION should be feature error");
      assertTrue(unsupportedTarget.isFeatureError(), "UNSUPPORTED_TARGET should be feature error");
      assertTrue(
          featureConfig.isFeatureError(), "FEATURE_CONFIGURATION_ERROR should be feature error");
    }

    @Test
    @DisplayName("isInternalError should return true for internal errors")
    void isInternalErrorShouldReturnTrueForInternalErrors() {
      final ModuleCompilationException internal =
          new ModuleCompilationException(CompilationErrorType.COMPILER_INTERNAL_ERROR, "Error");
      final ModuleCompilationException codeGen =
          new ModuleCompilationException(CompilationErrorType.CODE_GENERATION_FAILED, "Error");
      final ModuleCompilationException optFailed =
          new ModuleCompilationException(CompilationErrorType.OPTIMIZATION_FAILED, "Error");

      assertTrue(internal.isInternalError(), "COMPILER_INTERNAL_ERROR should be internal error");
      assertTrue(codeGen.isInternalError(), "CODE_GENERATION_FAILED should be internal error");
      assertTrue(optFailed.isInternalError(), "OPTIMIZATION_FAILED should be internal error");
    }
  }

  @Nested
  @DisplayName("Recovery Suggestion Tests")
  class RecoverySuggestionTests {

    @Test
    @DisplayName("Each error type should have unique recovery suggestion")
    void eachErrorTypeShouldHaveUniqueSuggestion() {
      for (final CompilationErrorType type : CompilationErrorType.values()) {
        final ModuleCompilationException exception =
            new ModuleCompilationException(type, "Test error");
        assertNotNull(
            exception.getRecoverySuggestion(), type.name() + " should have recovery suggestion");
      }
    }
  }
}
