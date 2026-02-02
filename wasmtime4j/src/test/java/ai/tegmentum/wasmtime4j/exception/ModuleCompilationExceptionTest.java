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

  @Nested
  @DisplayName("Error Category Boolean Return Mutation Tests")
  class ErrorCategoryBooleanReturnMutationTests {

    @Test
    @DisplayName("isResourceError should return false for all non-resource types")
    void isResourceErrorShouldReturnFalseForAllNonResourceTypes() {
      final CompilationErrorType[] nonResourceTypes = {
        CompilationErrorType.FUNCTION_TOO_COMPLEX,
        CompilationErrorType.UNSUPPORTED_INSTRUCTION,
        CompilationErrorType.CODE_GENERATION_FAILED,
        CompilationErrorType.OPTIMIZATION_FAILED,
        CompilationErrorType.REGISTER_ALLOCATION_FAILED,
        CompilationErrorType.CFG_CONSTRUCTION_FAILED,
        CompilationErrorType.UNSUPPORTED_TARGET,
        CompilationErrorType.COMPILER_INTERNAL_ERROR,
        CompilationErrorType.FEATURE_CONFIGURATION_ERROR,
        CompilationErrorType.UNKNOWN
      };

      for (CompilationErrorType type : nonResourceTypes) {
        final ModuleCompilationException exception =
            new ModuleCompilationException(type, "Error");
        assertFalse(
            exception.isResourceError(),
            type.name() + " should NOT be a resource error");
      }
    }

    @Test
    @DisplayName("Should have exactly 3 resource error types")
    void shouldHaveExactly3ResourceErrorTypes() {
      int count = 0;
      for (CompilationErrorType type : CompilationErrorType.values()) {
        final ModuleCompilationException exception =
            new ModuleCompilationException(type, "Error");
        if (exception.isResourceError()) {
          count++;
        }
      }
      assertEquals(
          3,
          count,
          "Should have exactly 3 resource error types: OUT_OF_MEMORY, TIMEOUT, RESOURCE_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("isComplexityError should return false for all non-complexity types")
    void isComplexityErrorShouldReturnFalseForAllNonComplexityTypes() {
      final CompilationErrorType[] nonComplexityTypes = {
        CompilationErrorType.OUT_OF_MEMORY,
        CompilationErrorType.TIMEOUT,
        CompilationErrorType.UNSUPPORTED_INSTRUCTION,
        CompilationErrorType.CODE_GENERATION_FAILED,
        CompilationErrorType.OPTIMIZATION_FAILED,
        CompilationErrorType.UNSUPPORTED_TARGET,
        CompilationErrorType.COMPILER_INTERNAL_ERROR,
        CompilationErrorType.RESOURCE_LIMIT_EXCEEDED,
        CompilationErrorType.FEATURE_CONFIGURATION_ERROR,
        CompilationErrorType.UNKNOWN
      };

      for (CompilationErrorType type : nonComplexityTypes) {
        final ModuleCompilationException exception =
            new ModuleCompilationException(type, "Error");
        assertFalse(
            exception.isComplexityError(),
            type.name() + " should NOT be a complexity error");
      }
    }

    @Test
    @DisplayName("Should have exactly 3 complexity error types")
    void shouldHaveExactly3ComplexityErrorTypes() {
      int count = 0;
      for (CompilationErrorType type : CompilationErrorType.values()) {
        final ModuleCompilationException exception =
            new ModuleCompilationException(type, "Error");
        if (exception.isComplexityError()) {
          count++;
        }
      }
      assertEquals(
          3,
          count,
          "Should have exactly 3 complexity error types: "
              + "FUNCTION_TOO_COMPLEX, CFG_CONSTRUCTION_FAILED, REGISTER_ALLOCATION_FAILED");
    }

    @Test
    @DisplayName("isFeatureError should return false for all non-feature types")
    void isFeatureErrorShouldReturnFalseForAllNonFeatureTypes() {
      for (CompilationErrorType type : CompilationErrorType.values()) {
        if (type != CompilationErrorType.UNSUPPORTED_INSTRUCTION
            && type != CompilationErrorType.UNSUPPORTED_TARGET
            && type != CompilationErrorType.FEATURE_CONFIGURATION_ERROR) {
          final ModuleCompilationException exception =
              new ModuleCompilationException(type, "Error");
          assertFalse(
              exception.isFeatureError(),
              type.name() + " should NOT be a feature error");
        }
      }
    }

    @Test
    @DisplayName("Should have exactly 3 feature error types")
    void shouldHaveExactly3FeatureErrorTypes() {
      int count = 0;
      for (CompilationErrorType type : CompilationErrorType.values()) {
        final ModuleCompilationException exception =
            new ModuleCompilationException(type, "Error");
        if (exception.isFeatureError()) {
          count++;
        }
      }
      assertEquals(
          3,
          count,
          "Should have exactly 3 feature error types: "
              + "UNSUPPORTED_INSTRUCTION, UNSUPPORTED_TARGET, FEATURE_CONFIGURATION_ERROR");
    }

    @Test
    @DisplayName("isInternalError should return false for all non-internal types")
    void isInternalErrorShouldReturnFalseForAllNonInternalTypes() {
      for (CompilationErrorType type : CompilationErrorType.values()) {
        if (type != CompilationErrorType.COMPILER_INTERNAL_ERROR
            && type != CompilationErrorType.CODE_GENERATION_FAILED
            && type != CompilationErrorType.OPTIMIZATION_FAILED) {
          final ModuleCompilationException exception =
              new ModuleCompilationException(type, "Error");
          assertFalse(
              exception.isInternalError(),
              type.name() + " should NOT be an internal error");
        }
      }
    }

    @Test
    @DisplayName("Should have exactly 3 internal error types")
    void shouldHaveExactly3InternalErrorTypes() {
      int count = 0;
      for (CompilationErrorType type : CompilationErrorType.values()) {
        final ModuleCompilationException exception =
            new ModuleCompilationException(type, "Error");
        if (exception.isInternalError()) {
          count++;
        }
      }
      assertEquals(
          3,
          count,
          "Should have exactly 3 internal error types: "
              + "COMPILER_INTERNAL_ERROR, CODE_GENERATION_FAILED, OPTIMIZATION_FAILED");
    }
  }

  @Nested
  @DisplayName("formatMessage Edge Case Mutation Tests")
  class FormatMessageEdgeCaseMutationTests {

    @Test
    @DisplayName("Message should not include phase when phase is UNKNOWN")
    void messageShouldNotIncludePhaseWhenUnknown() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(
              CompilationErrorType.OUT_OF_MEMORY,
              "Error message",
              CompilationPhase.UNKNOWN,
              null,
              null,
              null);

      assertFalse(
          exception.getMessage().contains("(phase:"),
          "Message should not contain phase when UNKNOWN");
    }

    @Test
    @DisplayName("Message should include phase when phase is not UNKNOWN")
    void messageShouldIncludePhaseWhenNotUnknown() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(
              CompilationErrorType.OPTIMIZATION_FAILED,
              "Error message",
              CompilationPhase.OPTIMIZATION,
              null,
              null,
              null);

      assertTrue(
          exception.getMessage().contains("(phase: Optimization)"),
          "Message should contain phase when not UNKNOWN");
    }

    @Test
    @DisplayName("Message should not include function when functionName is empty")
    void messageShouldNotIncludeFunctionWhenEmpty() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(
              CompilationErrorType.UNKNOWN,
              "Error message",
              CompilationPhase.UNKNOWN,
              "",
              null,
              null);

      assertFalse(
          exception.getMessage().contains("(function:"),
          "Message should not contain function when empty");
    }

    @Test
    @DisplayName("Message should include function index when functionName is null")
    void messageShouldIncludeFunctionIndexWhenNameIsNull() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(
              CompilationErrorType.FUNCTION_TOO_COMPLEX,
              "Error message",
              CompilationPhase.CODE_GENERATION,
              null,
              42,
              null);

      assertTrue(
          exception.getMessage().contains("(function index: 42)"),
          "Message should contain function index when name is null");
      assertFalse(
          exception.getMessage().contains("(function: "),
          "Message should not contain function name marker");
    }

    @Test
    @DisplayName("Message should prefer functionName over functionIndex")
    void messageShouldPreferFunctionNameOverIndex() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(
              CompilationErrorType.UNKNOWN,
              "Error message",
              CompilationPhase.UNKNOWN,
              "my_func",
              100,
              null);

      assertTrue(
          exception.getMessage().contains("(function: my_func)"),
          "Message should contain function name");
      assertFalse(
          exception.getMessage().contains("(function index:"),
          "Message should not contain function index when name is provided");
    }

    @Test
    @DisplayName("Message with all optional fields should contain all sections")
    void messageWithAllFieldsShouldContainAllSections() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(
              CompilationErrorType.REGISTER_ALLOCATION_FAILED,
              "Test error",
              CompilationPhase.REGISTER_ALLOCATION,
              "complex_func",
              99,
              null);

      final String message = exception.getMessage();
      assertTrue(
          message.contains("[REGISTER_ALLOCATION_FAILED]"),
          "Should contain error type");
      assertTrue(message.contains("Test error"), "Should contain base message");
      assertTrue(message.contains("(phase: Register Allocation)"), "Should contain phase");
      assertTrue(
          message.contains("(function: complex_func)"),
          "Should contain function name");
    }

    @Test
    @DisplayName("Message sections should appear in correct order")
    void messageSectionsShouldAppearInCorrectOrder() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(
              CompilationErrorType.CFG_CONSTRUCTION_FAILED,
              "Control flow error",
              CompilationPhase.CFG_CONSTRUCTION,
              "flow_func",
              null,
              null);

      final String message = exception.getMessage();
      final int errorTypeIndex = message.indexOf("[CFG_CONSTRUCTION_FAILED]");
      final int baseMessageIndex = message.indexOf("Control flow error");
      final int phaseIndex = message.indexOf("(phase:");
      final int functionIndex = message.indexOf("(function:");

      assertTrue(errorTypeIndex < baseMessageIndex, "Error type should come before message");
      assertTrue(baseMessageIndex < phaseIndex, "Message should come before phase");
      assertTrue(phaseIndex < functionIndex, "Phase should come before function");
    }
  }

  @Nested
  @DisplayName("generateRecoverySuggestion Mutation Tests")
  class GenerateRecoverySuggestionMutationTests {

    @Test
    @DisplayName("All error types should have distinct recovery suggestions")
    void allErrorTypesShouldHaveDistinctRecoverySuggestions() {
      final java.util.Set<String> suggestions = new java.util.HashSet<>();
      for (CompilationErrorType type : CompilationErrorType.values()) {
        final ModuleCompilationException exception =
            new ModuleCompilationException(type, "Error");
        final String suggestion = exception.getRecoverySuggestion();
        assertNotNull(suggestion, type.name() + " should have a recovery suggestion");
        assertFalse(suggestion.isEmpty(), type.name() + " should have non-empty suggestion");
        // UNKNOWN and default share the same suggestion
        if (type != CompilationErrorType.UNKNOWN) {
          assertTrue(
              suggestions.add(suggestion),
              type.name() + " should have distinct suggestion but got duplicate: " + suggestion);
        }
      }
    }

    @Test
    @DisplayName("OUT_OF_MEMORY should suggest increasing heap or splitting module")
    void outOfMemoryShouldSuggestHeapOrSplit() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(CompilationErrorType.OUT_OF_MEMORY, "Error");
      final String suggestion = exception.getRecoverySuggestion().toLowerCase();
      assertTrue(
          suggestion.contains("heap") || suggestion.contains("split"),
          "OUT_OF_MEMORY recovery should mention heap or split");
    }

    @Test
    @DisplayName("TIMEOUT should suggest increasing timeout or simplifying")
    void timeoutShouldSuggestTimeoutOrSimplify() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(CompilationErrorType.TIMEOUT, "Error");
      final String suggestion = exception.getRecoverySuggestion().toLowerCase();
      assertTrue(
          suggestion.contains("timeout") || suggestion.contains("simplify"),
          "TIMEOUT recovery should mention timeout or simplify");
    }

    @Test
    @DisplayName("FUNCTION_TOO_COMPLEX should suggest simplifying function")
    void functionTooComplexShouldSuggestSimplify() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(CompilationErrorType.FUNCTION_TOO_COMPLEX, "Error");
      final String suggestion = exception.getRecoverySuggestion().toLowerCase();
      assertTrue(
          suggestion.contains("simplify") || suggestion.contains("split"),
          "FUNCTION_TOO_COMPLEX recovery should mention simplify or split");
    }

    @Test
    @DisplayName("COMPILER_INTERNAL_ERROR should suggest reporting issue")
    void compilerInternalErrorShouldSuggestReport() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(CompilationErrorType.COMPILER_INTERNAL_ERROR, "Error");
      final String suggestion = exception.getRecoverySuggestion().toLowerCase();
      assertTrue(
          suggestion.contains("report") || suggestion.contains("maintainer"),
          "COMPILER_INTERNAL_ERROR recovery should mention reporting");
    }

    @Test
    @DisplayName("Each specific error type should have contextual suggestion")
    void eachErrorTypeShouldHaveContextualSuggestion() {
      final java.util.Map<CompilationErrorType, String[]> expectedKeywords =
          new java.util.HashMap<>();
      expectedKeywords.put(CompilationErrorType.OUT_OF_MEMORY, new String[] {"heap", "split"});
      expectedKeywords.put(CompilationErrorType.TIMEOUT, new String[] {"timeout", "simplify"});
      expectedKeywords.put(
          CompilationErrorType.FUNCTION_TOO_COMPLEX, new String[] {"simplify", "split", "smaller"});
      expectedKeywords.put(
          CompilationErrorType.UNSUPPORTED_INSTRUCTION, new String[] {"feature", "instruction"});
      expectedKeywords.put(
          CompilationErrorType.CODE_GENERATION_FAILED, new String[] {"optimization", "target"});
      expectedKeywords.put(
          CompilationErrorType.OPTIMIZATION_FAILED, new String[] {"optimization", "disable"});
      expectedKeywords.put(
          CompilationErrorType.REGISTER_ALLOCATION_FAILED, new String[] {"complexity", "optimization"});
      expectedKeywords.put(
          CompilationErrorType.CFG_CONSTRUCTION_FAILED, new String[] {"control flow", "simplify"});
      expectedKeywords.put(
          CompilationErrorType.UNSUPPORTED_TARGET, new String[] {"target", "architecture"});
      expectedKeywords.put(
          CompilationErrorType.COMPILER_INTERNAL_ERROR, new String[] {"report", "maintainer"});
      expectedKeywords.put(
          CompilationErrorType.RESOURCE_LIMIT_EXCEEDED, new String[] {"limit", "size"});
      expectedKeywords.put(
          CompilationErrorType.FEATURE_CONFIGURATION_ERROR, new String[] {"configuration", "feature"});
      expectedKeywords.put(
          CompilationErrorType.UNKNOWN, new String[] {"complexity", "configuration"});

      for (java.util.Map.Entry<CompilationErrorType, String[]> entry :
          expectedKeywords.entrySet()) {
        final CompilationErrorType type = entry.getKey();
        final String[] keywords = entry.getValue();
        final ModuleCompilationException exception =
            new ModuleCompilationException(type, "Error");
        final String suggestion = exception.getRecoverySuggestion().toLowerCase();

        boolean hasKeyword = false;
        for (String keyword : keywords) {
          if (suggestion.contains(keyword.toLowerCase())) {
            hasKeyword = true;
            break;
          }
        }
        assertTrue(
            hasKeyword,
            type.name()
                + " recovery suggestion should contain one of "
                + java.util.Arrays.toString(keywords)
                + " but was: "
                + suggestion);
      }
    }
  }

  @Nested
  @DisplayName("Getter Return Value Mutation Tests")
  class GetterReturnValueMutationTests {

    @Test
    @DisplayName("getFunctionName should return exact name passed to constructor")
    void getFunctionNameShouldReturnExactName() {
      final String[] testNames = {"func1", "my_function", "complex_name_123", "", null};
      for (String name : testNames) {
        final ModuleCompilationException exception =
            new ModuleCompilationException(
                CompilationErrorType.UNKNOWN,
                "Error",
                CompilationPhase.UNKNOWN,
                name,
                null,
                null);
        assertEquals(
            name,
            exception.getFunctionName(),
            "getFunctionName should return: " + name);
      }
    }

    @Test
    @DisplayName("getFunctionIndex should return exact index passed to constructor")
    void getFunctionIndexShouldReturnExactIndex() {
      final Integer[] testIndices = {null, 0, 1, 100, Integer.MAX_VALUE};
      for (Integer index : testIndices) {
        final ModuleCompilationException exception =
            new ModuleCompilationException(
                CompilationErrorType.UNKNOWN,
                "Error",
                CompilationPhase.UNKNOWN,
                null,
                index,
                null);
        assertEquals(
            index,
            exception.getFunctionIndex(),
            "getFunctionIndex should return: " + index);
      }
    }

    @Test
    @DisplayName("getPhase should return UNKNOWN when null is passed")
    void getPhaseShouldReturnUnknownWhenNullPassed() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(
              CompilationErrorType.UNKNOWN, "Error", null, null, null, null);
      assertEquals(
          CompilationPhase.UNKNOWN,
          exception.getPhase(),
          "getPhase should return UNKNOWN when null was passed");
    }

    @Test
    @DisplayName("getErrorType should return UNKNOWN when null is passed")
    void getErrorTypeShouldReturnUnknownWhenNullPassed() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(
              null, "Error", CompilationPhase.UNKNOWN, null, null, null);
      assertEquals(
          CompilationErrorType.UNKNOWN,
          exception.getErrorType(),
          "getErrorType should return UNKNOWN when null was passed");
    }
  }
}
