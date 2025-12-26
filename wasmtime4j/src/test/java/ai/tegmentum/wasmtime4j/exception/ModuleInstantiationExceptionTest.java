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

import ai.tegmentum.wasmtime4j.exception.ModuleInstantiationException.InstantiationErrorType;
import ai.tegmentum.wasmtime4j.exception.ModuleInstantiationException.InstantiationPhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ModuleInstantiationException} class.
 *
 * <p>This test class verifies the construction and behavior of module instantiation exceptions,
 * including error types, phases, and recovery suggestions.
 */
@DisplayName("ModuleInstantiationException Tests")
class ModuleInstantiationExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("ModuleInstantiationException should extend InstantiationException")
    void shouldExtendInstantiationException() {
      assertTrue(
          InstantiationException.class.isAssignableFrom(ModuleInstantiationException.class),
          "ModuleInstantiationException should extend InstantiationException");
    }

    @Test
    @DisplayName("ModuleInstantiationException should be serializable")
    void shouldBeSerializable() {
      assertTrue(
          java.io.Serializable.class.isAssignableFrom(ModuleInstantiationException.class),
          "ModuleInstantiationException should be serializable");
    }
  }

  @Nested
  @DisplayName("InstantiationErrorType Enum Tests")
  class InstantiationErrorTypeEnumTests {

    @Test
    @DisplayName("Should have MISSING_IMPORT value")
    void shouldHaveMissingImportValue() {
      assertNotNull(
          InstantiationErrorType.valueOf("MISSING_IMPORT"), "Should have MISSING_IMPORT value");
    }

    @Test
    @DisplayName("Should have IMPORT_TYPE_MISMATCH value")
    void shouldHaveImportTypeMismatchValue() {
      assertNotNull(
          InstantiationErrorType.valueOf("IMPORT_TYPE_MISMATCH"),
          "Should have IMPORT_TYPE_MISMATCH value");
    }

    @Test
    @DisplayName("Should have FUNCTION_SIGNATURE_MISMATCH value")
    void shouldHaveFunctionSignatureMismatchValue() {
      assertNotNull(
          InstantiationErrorType.valueOf("FUNCTION_SIGNATURE_MISMATCH"),
          "Should have FUNCTION_SIGNATURE_MISMATCH value");
    }

    @Test
    @DisplayName("Should have MEMORY_ALLOCATION_FAILED value")
    void shouldHaveMemoryAllocationFailedValue() {
      assertNotNull(
          InstantiationErrorType.valueOf("MEMORY_ALLOCATION_FAILED"),
          "Should have MEMORY_ALLOCATION_FAILED value");
    }

    @Test
    @DisplayName("Should have START_FUNCTION_FAILED value")
    void shouldHaveStartFunctionFailedValue() {
      assertNotNull(
          InstantiationErrorType.valueOf("START_FUNCTION_FAILED"),
          "Should have START_FUNCTION_FAILED value");
    }

    @Test
    @DisplayName("Should have UNKNOWN value")
    void shouldHaveUnknownValue() {
      assertNotNull(InstantiationErrorType.valueOf("UNKNOWN"), "Should have UNKNOWN value");
    }

    @Test
    @DisplayName("Each error type should have description")
    void eachErrorTypeShouldHaveDescription() {
      for (final InstantiationErrorType type : InstantiationErrorType.values()) {
        assertNotNull(type.getDescription(), type.name() + " should have description");
        assertFalse(
            type.getDescription().isEmpty(), type.name() + " should have non-empty description");
      }
    }

    @Test
    @DisplayName("Should have 20 error types")
    void shouldHave20ErrorTypes() {
      assertEquals(20, InstantiationErrorType.values().length, "Should have 20 error types");
    }
  }

  @Nested
  @DisplayName("InstantiationPhase Enum Tests")
  class InstantiationPhaseEnumTests {

    @Test
    @DisplayName("Should have IMPORT_RESOLUTION value")
    void shouldHaveImportResolutionValue() {
      assertNotNull(
          InstantiationPhase.valueOf("IMPORT_RESOLUTION"), "Should have IMPORT_RESOLUTION value");
    }

    @Test
    @DisplayName("Should have MEMORY_ALLOCATION value")
    void shouldHaveMemoryAllocationValue() {
      assertNotNull(
          InstantiationPhase.valueOf("MEMORY_ALLOCATION"), "Should have MEMORY_ALLOCATION value");
    }

    @Test
    @DisplayName("Should have START_FUNCTION_EXEC value")
    void shouldHaveStartFunctionExecValue() {
      assertNotNull(
          InstantiationPhase.valueOf("START_FUNCTION_EXEC"),
          "Should have START_FUNCTION_EXEC value");
    }

    @Test
    @DisplayName("Should have UNKNOWN value")
    void shouldHaveUnknownPhaseValue() {
      assertNotNull(InstantiationPhase.valueOf("UNKNOWN"), "Should have UNKNOWN value");
    }

    @Test
    @DisplayName("Each phase should have description")
    void eachPhaseShouldHaveDescription() {
      for (final InstantiationPhase phase : InstantiationPhase.values()) {
        assertNotNull(phase.getDescription(), phase.name() + " should have description");
        assertFalse(
            phase.getDescription().isEmpty(), phase.name() + " should have non-empty description");
      }
    }

    @Test
    @DisplayName("Should have 9 phases")
    void shouldHave9Phases() {
      assertEquals(9, InstantiationPhase.values().length, "Should have 9 phases");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with error type and message should set fields")
    void constructorWithErrorTypeAndMessage() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(InstantiationErrorType.MISSING_IMPORT, "Import missing");

      assertEquals(
          InstantiationErrorType.MISSING_IMPORT,
          exception.getErrorType(),
          "Error type should be MISSING_IMPORT");
      assertTrue(
          exception.getMessage().contains("Import missing"), "Message should contain error text");
      assertEquals(
          InstantiationPhase.UNKNOWN, exception.getPhase(), "Phase should default to UNKNOWN");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("Constructor with error type, message, and cause should set all")
    void constructorWithErrorTypeMessageAndCause() {
      final Throwable cause = new RuntimeException("Root cause");
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(
              InstantiationErrorType.TIMEOUT, "Timeout exceeded", cause);

      assertEquals(
          InstantiationErrorType.TIMEOUT, exception.getErrorType(), "Error type should be TIMEOUT");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }

    @Test
    @DisplayName("Full constructor should set all fields")
    void fullConstructorShouldSetAllFields() {
      final Throwable cause = new RuntimeException("Root cause");
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(
              InstantiationErrorType.IMPORT_RESOLUTION_FAILED,
              "Import failed",
              InstantiationPhase.IMPORT_RESOLUTION,
              "my_func",
              "my_module",
              cause);

      assertEquals(
          InstantiationErrorType.IMPORT_RESOLUTION_FAILED,
          exception.getErrorType(),
          "Error type should match");
      assertEquals(
          InstantiationPhase.IMPORT_RESOLUTION,
          exception.getPhase(),
          "Phase should be IMPORT_RESOLUTION");
      assertEquals("my_func", exception.getImportName(), "Import name should be 'my_func'");
      assertEquals("my_module", exception.getModuleName(), "Module name should be 'my_module'");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }

    @Test
    @DisplayName("Constructor should handle null error type")
    void constructorShouldHandleNullErrorType() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(
              null, "Error message", InstantiationPhase.UNKNOWN, null, null, null);

      assertEquals(
          InstantiationErrorType.UNKNOWN,
          exception.getErrorType(),
          "Null error type should default to UNKNOWN");
    }

    @Test
    @DisplayName("Constructor should handle null phase")
    void constructorShouldHandleNullPhase() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(
              InstantiationErrorType.MISSING_IMPORT, "Error message", null, null, null, null);

      assertEquals(
          InstantiationPhase.UNKNOWN, exception.getPhase(), "Null phase should default to UNKNOWN");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("getErrorType should return error type")
    void getErrorTypeShouldReturnErrorType() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(
              InstantiationErrorType.MEMORY_ALLOCATION_FAILED, "Memory failed");

      assertEquals(
          InstantiationErrorType.MEMORY_ALLOCATION_FAILED,
          exception.getErrorType(),
          "getErrorType should return MEMORY_ALLOCATION_FAILED");
    }

    @Test
    @DisplayName("getPhase should return instantiation phase")
    void getPhaseShouldReturnPhase() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(
              InstantiationErrorType.DATA_SEGMENT_INIT_FAILED,
              "Data init failed",
              InstantiationPhase.DATA_SEGMENT_INIT,
              null,
              null,
              null);

      assertEquals(
          InstantiationPhase.DATA_SEGMENT_INIT,
          exception.getPhase(),
          "getPhase should return DATA_SEGMENT_INIT");
    }

    @Test
    @DisplayName("getImportName should return import name")
    void getImportNameShouldReturnImportName() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(
              InstantiationErrorType.MISSING_IMPORT,
              "Error",
              InstantiationPhase.IMPORT_RESOLUTION,
              "test_import",
              null,
              null);

      assertEquals(
          "test_import", exception.getImportName(), "getImportName should return 'test_import'");
    }

    @Test
    @DisplayName("getModuleName should return module name")
    void getModuleNameShouldReturnModuleName() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(
              InstantiationErrorType.MISSING_IMPORT,
              "Error",
              InstantiationPhase.IMPORT_RESOLUTION,
              "func",
              "test_module",
              null);

      assertEquals(
          "test_module", exception.getModuleName(), "getModuleName should return 'test_module'");
    }

    @Test
    @DisplayName("getRecoverySuggestion should return non-null suggestion")
    void getRecoverySuggestionShouldReturnNonNull() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(InstantiationErrorType.MISSING_IMPORT, "Error");

      assertNotNull(exception.getRecoverySuggestion(), "Recovery suggestion should not be null");
      assertFalse(
          exception.getRecoverySuggestion().isEmpty(), "Recovery suggestion should not be empty");
    }
  }

  @Nested
  @DisplayName("Error Category Check Tests")
  class ErrorCategoryCheckTests {

    @Test
    @DisplayName("isImportError should return true for import errors")
    void isImportErrorShouldReturnTrueForImportErrors() {
      final ModuleInstantiationException missingImport =
          new ModuleInstantiationException(InstantiationErrorType.MISSING_IMPORT, "Error");
      final ModuleInstantiationException typeMismatch =
          new ModuleInstantiationException(InstantiationErrorType.IMPORT_TYPE_MISMATCH, "Error");
      final ModuleInstantiationException sigMismatch =
          new ModuleInstantiationException(
              InstantiationErrorType.FUNCTION_SIGNATURE_MISMATCH, "Error");

      assertTrue(missingImport.isImportError(), "MISSING_IMPORT should be import error");
      assertTrue(typeMismatch.isImportError(), "IMPORT_TYPE_MISMATCH should be import error");
      assertTrue(sigMismatch.isImportError(), "FUNCTION_SIGNATURE_MISMATCH should be import error");
    }

    @Test
    @DisplayName("isImportError should return false for non-import errors")
    void isImportErrorShouldReturnFalseForNonImportErrors() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(InstantiationErrorType.TIMEOUT, "Error");

      assertFalse(exception.isImportError(), "TIMEOUT should not be import error");
    }

    @Test
    @DisplayName("isResourceError should return true for resource errors")
    void isResourceErrorShouldReturnTrueForResourceErrors() {
      final ModuleInstantiationException memAlloc =
          new ModuleInstantiationException(
              InstantiationErrorType.MEMORY_ALLOCATION_FAILED, "Error");
      final ModuleInstantiationException tableAlloc =
          new ModuleInstantiationException(InstantiationErrorType.TABLE_ALLOCATION_FAILED, "Error");
      final ModuleInstantiationException resourceLimit =
          new ModuleInstantiationException(InstantiationErrorType.RESOURCE_LIMIT_EXCEEDED, "Error");
      final ModuleInstantiationException timeout =
          new ModuleInstantiationException(InstantiationErrorType.TIMEOUT, "Error");

      assertTrue(memAlloc.isResourceError(), "MEMORY_ALLOCATION_FAILED should be resource error");
      assertTrue(tableAlloc.isResourceError(), "TABLE_ALLOCATION_FAILED should be resource error");
      assertTrue(
          resourceLimit.isResourceError(), "RESOURCE_LIMIT_EXCEEDED should be resource error");
      assertTrue(timeout.isResourceError(), "TIMEOUT should be resource error");
    }

    @Test
    @DisplayName("isInitializationError should return true for initialization errors")
    void isInitializationErrorShouldReturnTrueForInitErrors() {
      final ModuleInstantiationException startFunc =
          new ModuleInstantiationException(InstantiationErrorType.START_FUNCTION_FAILED, "Error");
      final ModuleInstantiationException dataSegment =
          new ModuleInstantiationException(
              InstantiationErrorType.DATA_SEGMENT_INIT_FAILED, "Error");
      final ModuleInstantiationException elemSegment =
          new ModuleInstantiationException(
              InstantiationErrorType.ELEMENT_SEGMENT_INIT_FAILED, "Error");
      final ModuleInstantiationException globalInit =
          new ModuleInstantiationException(InstantiationErrorType.GLOBAL_INIT_FAILED, "Error");

      assertTrue(
          startFunc.isInitializationError(),
          "START_FUNCTION_FAILED should be initialization error");
      assertTrue(
          dataSegment.isInitializationError(),
          "DATA_SEGMENT_INIT_FAILED should be initialization error");
      assertTrue(
          elemSegment.isInitializationError(),
          "ELEMENT_SEGMENT_INIT_FAILED should be initialization error");
      assertTrue(
          globalInit.isInitializationError(), "GLOBAL_INIT_FAILED should be initialization error");
    }

    @Test
    @DisplayName("isConfigurationError should return true for configuration errors")
    void isConfigurationErrorShouldReturnTrueForConfigErrors() {
      final ModuleInstantiationException linker =
          new ModuleInstantiationException(InstantiationErrorType.LINKER_ERROR, "Error");
      final ModuleInstantiationException storeIncompat =
          new ModuleInstantiationException(InstantiationErrorType.STORE_INCOMPATIBLE, "Error");
      final ModuleInstantiationException multiMem =
          new ModuleInstantiationException(
              InstantiationErrorType.MULTIPLE_MEMORIES_UNSUPPORTED, "Error");
      final ModuleInstantiationException multiTable =
          new ModuleInstantiationException(
              InstantiationErrorType.MULTIPLE_TABLES_UNSUPPORTED, "Error");

      assertTrue(linker.isConfigurationError(), "LINKER_ERROR should be configuration error");
      assertTrue(
          storeIncompat.isConfigurationError(), "STORE_INCOMPATIBLE should be configuration error");
      assertTrue(
          multiMem.isConfigurationError(),
          "MULTIPLE_MEMORIES_UNSUPPORTED should be configuration error");
      assertTrue(
          multiTable.isConfigurationError(),
          "MULTIPLE_TABLES_UNSUPPORTED should be configuration error");
    }
  }

  @Nested
  @DisplayName("Recovery Suggestion Tests")
  class RecoverySuggestionTests {

    @Test
    @DisplayName("Each error type should have unique recovery suggestion")
    void eachErrorTypeShouldHaveUniqueSuggestion() {
      for (final InstantiationErrorType type : InstantiationErrorType.values()) {
        final ModuleInstantiationException exception =
            new ModuleInstantiationException(type, "Test error");
        assertNotNull(
            exception.getRecoverySuggestion(), type.name() + " should have recovery suggestion");
      }
    }
  }

  @Nested
  @DisplayName("Message Formatting Tests")
  class MessageFormattingTests {

    @Test
    @DisplayName("Message should include error type")
    void messageShouldIncludeErrorType() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(InstantiationErrorType.MISSING_IMPORT, "Error message");

      assertTrue(
          exception.getMessage().contains("MISSING_IMPORT"), "Message should contain error type");
    }

    @Test
    @DisplayName("Message should include phase when not UNKNOWN")
    void messageShouldIncludePhaseWhenNotUnknown() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(
              InstantiationErrorType.MISSING_IMPORT,
              "Error message",
              InstantiationPhase.IMPORT_RESOLUTION,
              null,
              null,
              null);

      assertTrue(
          exception.getMessage().contains("Import Resolution"),
          "Message should contain phase description");
    }

    @Test
    @DisplayName("Message should include import info when provided")
    void messageShouldIncludeImportInfo() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(
              InstantiationErrorType.MISSING_IMPORT,
              "Error message",
              InstantiationPhase.UNKNOWN,
              "my_func",
              "my_module",
              null);

      assertTrue(
          exception.getMessage().contains("my_module"), "Message should contain module name");
      assertTrue(exception.getMessage().contains("my_func"), "Message should contain import name");
    }
  }
}
