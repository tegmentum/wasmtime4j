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

  @Nested
  @DisplayName("Error Category Boolean Return Mutation Tests")
  class ErrorCategoryBooleanReturnMutationTests {

    @Test
    @DisplayName("isImportError should return false for all non-import types")
    void isImportErrorShouldReturnFalseForAllNonImportTypes() {
      final InstantiationErrorType[] nonImportTypes = {
        InstantiationErrorType.START_FUNCTION_FAILED,
        InstantiationErrorType.DATA_SEGMENT_INIT_FAILED,
        InstantiationErrorType.ELEMENT_SEGMENT_INIT_FAILED,
        InstantiationErrorType.MEMORY_ALLOCATION_FAILED,
        InstantiationErrorType.TABLE_ALLOCATION_FAILED,
        InstantiationErrorType.GLOBAL_INIT_FAILED,
        InstantiationErrorType.RESOURCE_LIMIT_EXCEEDED,
        InstantiationErrorType.TIMEOUT,
        InstantiationErrorType.MULTIPLE_MEMORIES_UNSUPPORTED,
        InstantiationErrorType.MULTIPLE_TABLES_UNSUPPORTED,
        InstantiationErrorType.LINKER_ERROR,
        InstantiationErrorType.STORE_INCOMPATIBLE,
        InstantiationErrorType.UNKNOWN
      };

      for (InstantiationErrorType type : nonImportTypes) {
        final ModuleInstantiationException exception =
            new ModuleInstantiationException(type, "Error");
        assertFalse(
            exception.isImportError(),
            type.name() + " should NOT be an import error");
      }
    }

    @Test
    @DisplayName("Should have exactly 7 import error types")
    void shouldHaveExactly7ImportErrorTypes() {
      int count = 0;
      for (InstantiationErrorType type : InstantiationErrorType.values()) {
        final ModuleInstantiationException exception =
            new ModuleInstantiationException(type, "Error");
        if (exception.isImportError()) {
          count++;
        }
      }
      assertEquals(
          7,
          count,
          "Should have exactly 7 import error types: MISSING_IMPORT, IMPORT_TYPE_MISMATCH, "
              + "FUNCTION_SIGNATURE_MISMATCH, MEMORY_IMPORT_INCOMPATIBLE, TABLE_IMPORT_INCOMPATIBLE, "
              + "GLOBAL_IMPORT_MISMATCH, IMPORT_RESOLUTION_FAILED");
    }

    @Test
    @DisplayName("isResourceError should return false for all non-resource types")
    void isResourceErrorShouldReturnFalseForAllNonResourceTypes() {
      for (InstantiationErrorType type : InstantiationErrorType.values()) {
        if (type != InstantiationErrorType.MEMORY_ALLOCATION_FAILED
            && type != InstantiationErrorType.TABLE_ALLOCATION_FAILED
            && type != InstantiationErrorType.RESOURCE_LIMIT_EXCEEDED
            && type != InstantiationErrorType.TIMEOUT) {
          final ModuleInstantiationException exception =
              new ModuleInstantiationException(type, "Error");
          assertFalse(
              exception.isResourceError(),
              type.name() + " should NOT be a resource error");
        }
      }
    }

    @Test
    @DisplayName("Should have exactly 4 resource error types")
    void shouldHaveExactly4ResourceErrorTypes() {
      int count = 0;
      for (InstantiationErrorType type : InstantiationErrorType.values()) {
        final ModuleInstantiationException exception =
            new ModuleInstantiationException(type, "Error");
        if (exception.isResourceError()) {
          count++;
        }
      }
      assertEquals(
          4,
          count,
          "Should have exactly 4 resource error types: MEMORY_ALLOCATION_FAILED, "
              + "TABLE_ALLOCATION_FAILED, RESOURCE_LIMIT_EXCEEDED, TIMEOUT");
    }

    @Test
    @DisplayName("isInitializationError should return false for all non-initialization types")
    void isInitializationErrorShouldReturnFalseForAllNonInitTypes() {
      for (InstantiationErrorType type : InstantiationErrorType.values()) {
        if (type != InstantiationErrorType.START_FUNCTION_FAILED
            && type != InstantiationErrorType.DATA_SEGMENT_INIT_FAILED
            && type != InstantiationErrorType.ELEMENT_SEGMENT_INIT_FAILED
            && type != InstantiationErrorType.GLOBAL_INIT_FAILED) {
          final ModuleInstantiationException exception =
              new ModuleInstantiationException(type, "Error");
          assertFalse(
              exception.isInitializationError(),
              type.name() + " should NOT be an initialization error");
        }
      }
    }

    @Test
    @DisplayName("Should have exactly 4 initialization error types")
    void shouldHaveExactly4InitializationErrorTypes() {
      int count = 0;
      for (InstantiationErrorType type : InstantiationErrorType.values()) {
        final ModuleInstantiationException exception =
            new ModuleInstantiationException(type, "Error");
        if (exception.isInitializationError()) {
          count++;
        }
      }
      assertEquals(
          4,
          count,
          "Should have exactly 4 initialization error types: START_FUNCTION_FAILED, "
              + "DATA_SEGMENT_INIT_FAILED, ELEMENT_SEGMENT_INIT_FAILED, GLOBAL_INIT_FAILED");
    }

    @Test
    @DisplayName("isConfigurationError should return false for all non-configuration types")
    void isConfigurationErrorShouldReturnFalseForAllNonConfigTypes() {
      for (InstantiationErrorType type : InstantiationErrorType.values()) {
        if (type != InstantiationErrorType.LINKER_ERROR
            && type != InstantiationErrorType.STORE_INCOMPATIBLE
            && type != InstantiationErrorType.MULTIPLE_MEMORIES_UNSUPPORTED
            && type != InstantiationErrorType.MULTIPLE_TABLES_UNSUPPORTED) {
          final ModuleInstantiationException exception =
              new ModuleInstantiationException(type, "Error");
          assertFalse(
              exception.isConfigurationError(),
              type.name() + " should NOT be a configuration error");
        }
      }
    }

    @Test
    @DisplayName("Should have exactly 4 configuration error types")
    void shouldHaveExactly4ConfigurationErrorTypes() {
      int count = 0;
      for (InstantiationErrorType type : InstantiationErrorType.values()) {
        final ModuleInstantiationException exception =
            new ModuleInstantiationException(type, "Error");
        if (exception.isConfigurationError()) {
          count++;
        }
      }
      assertEquals(
          4,
          count,
          "Should have exactly 4 configuration error types: LINKER_ERROR, STORE_INCOMPATIBLE, "
              + "MULTIPLE_MEMORIES_UNSUPPORTED, MULTIPLE_TABLES_UNSUPPORTED");
    }
  }

  @Nested
  @DisplayName("formatMessage Edge Case Mutation Tests")
  class FormatMessageEdgeCaseMutationTests {

    @Test
    @DisplayName("Message should not include phase when phase is UNKNOWN")
    void messageShouldNotIncludePhaseWhenUnknown() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(
              InstantiationErrorType.MISSING_IMPORT,
              "Error message",
              InstantiationPhase.UNKNOWN,
              null,
              null,
              null);

      assertFalse(
          exception.getMessage().contains("(phase:"),
          "Message should not contain phase when UNKNOWN");
    }

    @Test
    @DisplayName("Message should include import name only when moduleName is null/empty")
    void messageShouldIncludeImportNameOnlyWhenModuleNameIsNull() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(
              InstantiationErrorType.MISSING_IMPORT,
              "Error message",
              InstantiationPhase.UNKNOWN,
              "my_func",
              null,
              null);

      assertTrue(
          exception.getMessage().contains("(import: my_func)"),
          "Message should contain import name without module prefix");
      assertFalse(
          exception.getMessage().contains(".my_func"),
          "Message should not have module.import format");
    }

    @Test
    @DisplayName("Message should include module name only when importName is null/empty")
    void messageShouldIncludeModuleNameOnlyWhenImportNameIsNull() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(
              InstantiationErrorType.MISSING_IMPORT,
              "Error message",
              InstantiationPhase.UNKNOWN,
              null,
              "my_module",
              null);

      assertTrue(
          exception.getMessage().contains("(module: my_module)"),
          "Message should contain module name alone");
    }

    @Test
    @DisplayName("Message should include module.import format when both are provided")
    void messageShouldIncludeModuleImportFormatWhenBothProvided() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(
              InstantiationErrorType.MISSING_IMPORT,
              "Error message",
              InstantiationPhase.UNKNOWN,
              "my_func",
              "my_module",
              null);

      assertTrue(
          exception.getMessage().contains("(import: my_module.my_func)"),
          "Message should contain module.import format");
    }

    @Test
    @DisplayName("Message should not include import when both importName and moduleName are empty")
    void messageShouldNotIncludeImportWhenBothEmpty() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(
              InstantiationErrorType.UNKNOWN,
              "Error message",
              InstantiationPhase.UNKNOWN,
              "",
              "",
              null);

      assertFalse(
          exception.getMessage().contains("(import:"),
          "Message should not contain import marker when empty");
      assertFalse(
          exception.getMessage().contains("(module:"),
          "Message should not contain module marker when empty");
    }

    @Test
    @DisplayName("Message with all fields should contain all sections in order")
    void messageWithAllFieldsShouldContainAllSectionsInOrder() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(
              InstantiationErrorType.IMPORT_RESOLUTION_FAILED,
              "Test error",
              InstantiationPhase.IMPORT_RESOLUTION,
              "test_func",
              "test_module",
              null);

      final String message = exception.getMessage();
      final int errorTypeIndex = message.indexOf("[IMPORT_RESOLUTION_FAILED]");
      final int baseMessageIndex = message.indexOf("Test error");
      final int phaseIndex = message.indexOf("(phase:");
      final int importIndex = message.indexOf("(import:");

      assertTrue(errorTypeIndex < baseMessageIndex, "Error type should come before message");
      assertTrue(baseMessageIndex < phaseIndex, "Message should come before phase");
      assertTrue(phaseIndex < importIndex, "Phase should come before import");
    }

    @Test
    @DisplayName("Message should handle importName empty but moduleName provided")
    void messageShouldHandleEmptyImportNameWithModuleName() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(
              InstantiationErrorType.UNKNOWN,
              "Error message",
              InstantiationPhase.UNKNOWN,
              "",
              "my_module",
              null);

      assertTrue(
          exception.getMessage().contains("(module: my_module)"),
          "Message should contain module when import is empty");
      assertFalse(
          exception.getMessage().contains("(import:"),
          "Message should not contain import marker when import is empty");
    }
  }

  @Nested
  @DisplayName("generateRecoverySuggestion Mutation Tests")
  class GenerateRecoverySuggestionMutationTests {

    @Test
    @DisplayName("All error types should have distinct recovery suggestions")
    void allErrorTypesShouldHaveDistinctRecoverySuggestions() {
      final java.util.Set<String> suggestions = new java.util.HashSet<>();
      for (InstantiationErrorType type : InstantiationErrorType.values()) {
        final ModuleInstantiationException exception =
            new ModuleInstantiationException(type, "Error");
        final String suggestion = exception.getRecoverySuggestion();
        assertNotNull(suggestion, type.name() + " should have a recovery suggestion");
        assertFalse(suggestion.isEmpty(), type.name() + " should have non-empty suggestion");
        // UNKNOWN and default share the same suggestion
        if (type != InstantiationErrorType.UNKNOWN) {
          assertTrue(
              suggestions.add(suggestion),
              type.name() + " should have distinct suggestion but got duplicate: " + suggestion);
        }
      }
    }

    @Test
    @DisplayName("MISSING_IMPORT should suggest providing imports through linker")
    void missingImportShouldSuggestLinker() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(InstantiationErrorType.MISSING_IMPORT, "Error");
      final String suggestion = exception.getRecoverySuggestion().toLowerCase();
      assertTrue(
          suggestion.contains("import") || suggestion.contains("linker"),
          "MISSING_IMPORT recovery should mention import or linker");
    }

    @Test
    @DisplayName("TIMEOUT should suggest increasing timeout")
    void timeoutShouldSuggestIncreasingTimeout() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(InstantiationErrorType.TIMEOUT, "Error");
      final String suggestion = exception.getRecoverySuggestion().toLowerCase();
      assertTrue(
          suggestion.contains("timeout") || suggestion.contains("optimize"),
          "TIMEOUT recovery should mention timeout or optimize");
    }

    @Test
    @DisplayName("Each specific error type should have contextual suggestion")
    void eachErrorTypeShouldHaveContextualSuggestion() {
      final java.util.Map<InstantiationErrorType, String[]> expectedKeywords =
          new java.util.HashMap<>();
      expectedKeywords.put(
          InstantiationErrorType.MISSING_IMPORT, new String[] {"import", "linker"});
      expectedKeywords.put(
          InstantiationErrorType.IMPORT_TYPE_MISMATCH, new String[] {"import", "type"});
      expectedKeywords.put(
          InstantiationErrorType.FUNCTION_SIGNATURE_MISMATCH, new String[] {"function", "signature"});
      expectedKeywords.put(
          InstantiationErrorType.MEMORY_IMPORT_INCOMPATIBLE, new String[] {"memory", "limit"});
      expectedKeywords.put(
          InstantiationErrorType.TABLE_IMPORT_INCOMPATIBLE, new String[] {"table", "size"});
      expectedKeywords.put(
          InstantiationErrorType.GLOBAL_IMPORT_MISMATCH, new String[] {"global", "mutability"});
      expectedKeywords.put(
          InstantiationErrorType.START_FUNCTION_FAILED, new String[] {"start", "function"});
      expectedKeywords.put(
          InstantiationErrorType.DATA_SEGMENT_INIT_FAILED, new String[] {"data", "segment"});
      expectedKeywords.put(
          InstantiationErrorType.ELEMENT_SEGMENT_INIT_FAILED, new String[] {"element", "segment"});
      expectedKeywords.put(
          InstantiationErrorType.MEMORY_ALLOCATION_FAILED, new String[] {"memory", "limit"});
      expectedKeywords.put(
          InstantiationErrorType.TABLE_ALLOCATION_FAILED, new String[] {"table", "limit"});
      expectedKeywords.put(
          InstantiationErrorType.GLOBAL_INIT_FAILED, new String[] {"global", "initializer"});
      expectedKeywords.put(
          InstantiationErrorType.RESOURCE_LIMIT_EXCEEDED, new String[] {"resource", "limit"});
      expectedKeywords.put(
          InstantiationErrorType.TIMEOUT, new String[] {"timeout", "optimize"});
      expectedKeywords.put(
          InstantiationErrorType.MULTIPLE_MEMORIES_UNSUPPORTED, new String[] {"memory", "multi"});
      expectedKeywords.put(
          InstantiationErrorType.MULTIPLE_TABLES_UNSUPPORTED, new String[] {"table", "multi"});
      expectedKeywords.put(
          InstantiationErrorType.IMPORT_RESOLUTION_FAILED, new String[] {"import", "resolution"});
      expectedKeywords.put(
          InstantiationErrorType.LINKER_ERROR, new String[] {"linker", "configuration"});
      expectedKeywords.put(
          InstantiationErrorType.STORE_INCOMPATIBLE, new String[] {"store", "configuration"});
      expectedKeywords.put(
          InstantiationErrorType.UNKNOWN, new String[] {"import", "configuration"});

      for (java.util.Map.Entry<InstantiationErrorType, String[]> entry :
          expectedKeywords.entrySet()) {
        final InstantiationErrorType type = entry.getKey();
        final String[] keywords = entry.getValue();
        final ModuleInstantiationException exception =
            new ModuleInstantiationException(type, "Error");
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
    @DisplayName("getImportName should return exact name passed to constructor")
    void getImportNameShouldReturnExactName() {
      final String[] testNames = {"func1", "my_import", "complex_name_123", "", null};
      for (String name : testNames) {
        final ModuleInstantiationException exception =
            new ModuleInstantiationException(
                InstantiationErrorType.UNKNOWN,
                "Error",
                InstantiationPhase.UNKNOWN,
                name,
                null,
                null);
        assertEquals(
            name,
            exception.getImportName(),
            "getImportName should return: " + name);
      }
    }

    @Test
    @DisplayName("getModuleName should return exact name passed to constructor")
    void getModuleNameShouldReturnExactName() {
      final String[] testNames = {"module1", "wasi_snapshot_preview1", "", null};
      for (String name : testNames) {
        final ModuleInstantiationException exception =
            new ModuleInstantiationException(
                InstantiationErrorType.UNKNOWN,
                "Error",
                InstantiationPhase.UNKNOWN,
                null,
                name,
                null);
        assertEquals(
            name,
            exception.getModuleName(),
            "getModuleName should return: " + name);
      }
    }

    @Test
    @DisplayName("getPhase should return UNKNOWN when null is passed")
    void getPhaseShouldReturnUnknownWhenNullPassed() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(
              InstantiationErrorType.UNKNOWN, "Error", null, null, null, null);
      assertEquals(
          InstantiationPhase.UNKNOWN,
          exception.getPhase(),
          "getPhase should return UNKNOWN when null was passed");
    }

    @Test
    @DisplayName("getErrorType should return UNKNOWN when null is passed")
    void getErrorTypeShouldReturnUnknownWhenNullPassed() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(
              null, "Error", InstantiationPhase.UNKNOWN, null, null, null);
      assertEquals(
          InstantiationErrorType.UNKNOWN,
          exception.getErrorType(),
          "getErrorType should return UNKNOWN when null was passed");
    }

    @Test
    @DisplayName("All phases should return correct value from getPhase")
    void allPhasesShouldReturnCorrectValueFromGetPhase() {
      for (InstantiationPhase phase : InstantiationPhase.values()) {
        final ModuleInstantiationException exception =
            new ModuleInstantiationException(
                InstantiationErrorType.UNKNOWN,
                "Error",
                phase,
                null,
                null,
                null);
        assertEquals(
            phase,
            exception.getPhase(),
            "getPhase should return: " + phase);
      }
    }
  }
}
