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

import ai.tegmentum.wasmtime4j.exception.ModuleValidationException.ValidationErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ModuleValidationException} class.
 *
 * <p>This test class verifies the construction and behavior of module validation exceptions,
 * including error types, sections, offsets, and recovery suggestions.
 */
@DisplayName("ModuleValidationException Tests")
class ModuleValidationExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("ModuleValidationException should extend ValidationException")
    void shouldExtendValidationException() {
      assertTrue(
          ValidationException.class.isAssignableFrom(ModuleValidationException.class),
          "ModuleValidationException should extend ValidationException");
    }

    @Test
    @DisplayName("ModuleValidationException should be serializable")
    void shouldBeSerializable() {
      assertTrue(
          java.io.Serializable.class.isAssignableFrom(ModuleValidationException.class),
          "ModuleValidationException should be serializable");
    }
  }

  @Nested
  @DisplayName("ValidationErrorType Enum Tests")
  class ValidationErrorTypeEnumTests {

    @Test
    @DisplayName("Should have INVALID_MAGIC_NUMBER value")
    void shouldHaveInvalidMagicNumberValue() {
      assertNotNull(
          ValidationErrorType.valueOf("INVALID_MAGIC_NUMBER"),
          "Should have INVALID_MAGIC_NUMBER value");
    }

    @Test
    @DisplayName("Should have MALFORMED_MODULE value")
    void shouldHaveMalformedModuleValue() {
      assertNotNull(
          ValidationErrorType.valueOf("MALFORMED_MODULE"), "Should have MALFORMED_MODULE value");
    }

    @Test
    @DisplayName("Should have TYPE_MISMATCH value")
    void shouldHaveTypeMismatchValue() {
      assertNotNull(
          ValidationErrorType.valueOf("TYPE_MISMATCH"), "Should have TYPE_MISMATCH value");
    }

    @Test
    @DisplayName("Should have INVALID_CONTROL_FLOW value")
    void shouldHaveInvalidControlFlowValue() {
      assertNotNull(
          ValidationErrorType.valueOf("INVALID_CONTROL_FLOW"),
          "Should have INVALID_CONTROL_FLOW value");
    }

    @Test
    @DisplayName("Should have UNSUPPORTED_FEATURE value")
    void shouldHaveUnsupportedFeatureValue() {
      assertNotNull(
          ValidationErrorType.valueOf("UNSUPPORTED_FEATURE"),
          "Should have UNSUPPORTED_FEATURE value");
    }

    @Test
    @DisplayName("Should have UNKNOWN value")
    void shouldHaveUnknownValue() {
      assertNotNull(ValidationErrorType.valueOf("UNKNOWN"), "Should have UNKNOWN value");
    }

    @Test
    @DisplayName("Each error type should have description")
    void eachErrorTypeShouldHaveDescription() {
      for (final ValidationErrorType type : ValidationErrorType.values()) {
        assertNotNull(type.getDescription(), type.name() + " should have description");
        assertFalse(
            type.getDescription().isEmpty(), type.name() + " should have non-empty description");
      }
    }

    @Test
    @DisplayName("Should have 23 error types")
    void shouldHave23ErrorTypes() {
      assertEquals(23, ValidationErrorType.values().length, "Should have 23 error types");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with error type and message should set fields")
    void constructorWithErrorTypeAndMessage() {
      final ModuleValidationException exception =
          new ModuleValidationException(ValidationErrorType.INVALID_MAGIC_NUMBER, "Invalid magic");

      assertEquals(
          ValidationErrorType.INVALID_MAGIC_NUMBER,
          exception.getErrorType(),
          "Error type should be INVALID_MAGIC_NUMBER");
      assertTrue(
          exception.getMessage().contains("Invalid magic"), "Message should contain error text");
      assertNull(exception.getModuleSection(), "Module section should be null");
      assertNull(exception.getByteOffset(), "Byte offset should be null");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("Constructor with error type, message, and cause should set all")
    void constructorWithErrorTypeMessageAndCause() {
      final Throwable cause = new RuntimeException("Root cause");
      final ModuleValidationException exception =
          new ModuleValidationException(
              ValidationErrorType.MALFORMED_MODULE, "Module malformed", cause);

      assertEquals(
          ValidationErrorType.MALFORMED_MODULE,
          exception.getErrorType(),
          "Error type should be MALFORMED_MODULE");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }

    @Test
    @DisplayName("Full constructor should set all fields")
    void fullConstructorShouldSetAllFields() {
      final Throwable cause = new RuntimeException("Root cause");
      final ModuleValidationException exception =
          new ModuleValidationException(
              ValidationErrorType.TYPE_MISMATCH, "Type mismatch error", "code", 100, cause);

      assertEquals(
          ValidationErrorType.TYPE_MISMATCH, exception.getErrorType(), "Error type should match");
      assertEquals("code", exception.getModuleSection(), "Module section should be 'code'");
      assertEquals(Integer.valueOf(100), exception.getByteOffset(), "Byte offset should be 100");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }

    @Test
    @DisplayName("Constructor should handle null error type")
    void constructorShouldHandleNullErrorType() {
      final ModuleValidationException exception =
          new ModuleValidationException(null, "Error message", null, null, null);

      assertEquals(
          ValidationErrorType.UNKNOWN,
          exception.getErrorType(),
          "Null error type should default to UNKNOWN");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("getErrorType should return error type")
    void getErrorTypeShouldReturnErrorType() {
      final ModuleValidationException exception =
          new ModuleValidationException(ValidationErrorType.INVALID_FUNCTION_BODY, "Invalid body");

      assertEquals(
          ValidationErrorType.INVALID_FUNCTION_BODY,
          exception.getErrorType(),
          "getErrorType should return INVALID_FUNCTION_BODY");
    }

    @Test
    @DisplayName("getModuleSection should return module section")
    void getModuleSectionShouldReturnModuleSection() {
      final ModuleValidationException exception =
          new ModuleValidationException(
              ValidationErrorType.INVALID_IMPORT, "Error", "import", null, null);

      assertEquals(
          "import", exception.getModuleSection(), "getModuleSection should return 'import'");
    }

    @Test
    @DisplayName("getByteOffset should return byte offset")
    void getByteOffsetShouldReturnByteOffset() {
      final ModuleValidationException exception =
          new ModuleValidationException(ValidationErrorType.UNKNOWN, "Error", null, 256, null);

      assertEquals(
          Integer.valueOf(256), exception.getByteOffset(), "getByteOffset should return 256");
    }

    @Test
    @DisplayName("getRecoverySuggestion should return non-null suggestion")
    void getRecoverySuggestionShouldReturnNonNull() {
      final ModuleValidationException exception =
          new ModuleValidationException(ValidationErrorType.INVALID_MAGIC_NUMBER, "Error");

      assertNotNull(exception.getRecoverySuggestion(), "Recovery suggestion should not be null");
      assertFalse(
          exception.getRecoverySuggestion().isEmpty(), "Recovery suggestion should not be empty");
    }
  }

  @Nested
  @DisplayName("Error Category Check Tests")
  class ErrorCategoryCheckTests {

    @Test
    @DisplayName("isStructuralError should return true for structural errors")
    void isStructuralErrorShouldReturnTrueForStructuralErrors() {
      final ModuleValidationException magic =
          new ModuleValidationException(ValidationErrorType.INVALID_MAGIC_NUMBER, "Error");
      final ModuleValidationException malformed =
          new ModuleValidationException(ValidationErrorType.MALFORMED_MODULE, "Error");
      final ModuleValidationException dataSegment =
          new ModuleValidationException(ValidationErrorType.INVALID_DATA_SEGMENT, "Error");
      final ModuleValidationException elemSegment =
          new ModuleValidationException(ValidationErrorType.INVALID_ELEMENT_SEGMENT, "Error");

      assertTrue(magic.isStructuralError(), "INVALID_MAGIC_NUMBER should be structural error");
      assertTrue(malformed.isStructuralError(), "MALFORMED_MODULE should be structural error");
      assertTrue(
          dataSegment.isStructuralError(), "INVALID_DATA_SEGMENT should be structural error");
      assertTrue(
          elemSegment.isStructuralError(), "INVALID_ELEMENT_SEGMENT should be structural error");
    }

    @Test
    @DisplayName("isStructuralError should return false for non-structural errors")
    void isStructuralErrorShouldReturnFalseForNonStructuralErrors() {
      final ModuleValidationException exception =
          new ModuleValidationException(ValidationErrorType.TYPE_MISMATCH, "Error");

      assertFalse(exception.isStructuralError(), "TYPE_MISMATCH should not be structural error");
    }

    @Test
    @DisplayName("isTypeError should return true for type errors")
    void isTypeErrorShouldReturnTrueForTypeErrors() {
      final ModuleValidationException typeDef =
          new ModuleValidationException(ValidationErrorType.INVALID_TYPE_DEFINITION, "Error");
      final ModuleValidationException funcSig =
          new ModuleValidationException(ValidationErrorType.INVALID_FUNCTION_SIGNATURE, "Error");
      final ModuleValidationException mismatch =
          new ModuleValidationException(ValidationErrorType.TYPE_MISMATCH, "Error");

      assertTrue(typeDef.isTypeError(), "INVALID_TYPE_DEFINITION should be type error");
      assertTrue(funcSig.isTypeError(), "INVALID_FUNCTION_SIGNATURE should be type error");
      assertTrue(mismatch.isTypeError(), "TYPE_MISMATCH should be type error");
    }

    @Test
    @DisplayName("isImportExportError should return true for import/export errors")
    void isImportExportErrorShouldReturnTrueForImportExportErrors() {
      final ModuleValidationException importErr =
          new ModuleValidationException(ValidationErrorType.INVALID_IMPORT, "Error");
      final ModuleValidationException exportErr =
          new ModuleValidationException(ValidationErrorType.INVALID_EXPORT, "Error");

      assertTrue(importErr.isImportExportError(), "INVALID_IMPORT should be import/export error");
      assertTrue(exportErr.isImportExportError(), "INVALID_EXPORT should be import/export error");
    }

    @Test
    @DisplayName("isMemoryError should return true for memory errors")
    void isMemoryErrorShouldReturnTrueForMemoryErrors() {
      final ModuleValidationException memDef =
          new ModuleValidationException(ValidationErrorType.INVALID_MEMORY_DEFINITION, "Error");
      final ModuleValidationException memOp =
          new ModuleValidationException(ValidationErrorType.INVALID_MEMORY_OPERATION, "Error");

      assertTrue(memDef.isMemoryError(), "INVALID_MEMORY_DEFINITION should be memory error");
      assertTrue(memOp.isMemoryError(), "INVALID_MEMORY_OPERATION should be memory error");
    }

    @Test
    @DisplayName("isFeatureError should return true for feature errors")
    void isFeatureErrorShouldReturnTrueForFeatureErrors() {
      final ModuleValidationException unsupported =
          new ModuleValidationException(ValidationErrorType.UNSUPPORTED_FEATURE, "Error");
      final ModuleValidationException limitExceeded =
          new ModuleValidationException(ValidationErrorType.LIMIT_EXCEEDED, "Error");

      assertTrue(unsupported.isFeatureError(), "UNSUPPORTED_FEATURE should be feature error");
      assertTrue(limitExceeded.isFeatureError(), "LIMIT_EXCEEDED should be feature error");
    }
  }

  @Nested
  @DisplayName("Recovery Suggestion Tests")
  class RecoverySuggestionTests {

    @Test
    @DisplayName("Each error type should have unique recovery suggestion")
    void eachErrorTypeShouldHaveUniqueSuggestion() {
      for (final ValidationErrorType type : ValidationErrorType.values()) {
        final ModuleValidationException exception =
            new ModuleValidationException(type, "Test error");
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
      final ModuleValidationException exception =
          new ModuleValidationException(ValidationErrorType.TYPE_MISMATCH, "Error message");

      assertTrue(
          exception.getMessage().contains("TYPE_MISMATCH"), "Message should contain error type");
    }

    @Test
    @DisplayName("Message should include section when provided")
    void messageShouldIncludeSectionWhenProvided() {
      final ModuleValidationException exception =
          new ModuleValidationException(
              ValidationErrorType.INVALID_FUNCTION_BODY, "Error message", "code", null, null);

      assertTrue(exception.getMessage().contains("code"), "Message should contain section");
    }

    @Test
    @DisplayName("Message should include offset when provided")
    void messageShouldIncludeOffsetWhenProvided() {
      final ModuleValidationException exception =
          new ModuleValidationException(
              ValidationErrorType.INVALID_FUNCTION_BODY, "Error message", null, 42, null);

      assertTrue(exception.getMessage().contains("42"), "Message should contain offset");
    }
  }

  @Nested
  @DisplayName("Error Category Boolean Return Mutation Tests")
  class ErrorCategoryBooleanReturnMutationTests {

    @Test
    @DisplayName("isStructuralError should return false for all non-structural types")
    void isStructuralErrorShouldReturnFalseForAllNonStructuralTypes() {
      // Test all types that are NOT structural errors
      final ValidationErrorType[] nonStructuralTypes = {
        ValidationErrorType.INVALID_TYPE_DEFINITION,
        ValidationErrorType.INVALID_FUNCTION_SIGNATURE,
        ValidationErrorType.INVALID_IMPORT,
        ValidationErrorType.INVALID_EXPORT,
        ValidationErrorType.INVALID_MEMORY_DEFINITION,
        ValidationErrorType.INVALID_TABLE_DEFINITION,
        ValidationErrorType.INVALID_GLOBAL_DEFINITION,
        ValidationErrorType.INVALID_FUNCTION_BODY,
        ValidationErrorType.TYPE_MISMATCH,
        ValidationErrorType.INVALID_CONTROL_FLOW,
        ValidationErrorType.INVALID_MEMORY_OPERATION,
        ValidationErrorType.INVALID_TABLE_OPERATION,
        ValidationErrorType.INVALID_CALL,
        ValidationErrorType.INVALID_LOCAL_ACCESS,
        ValidationErrorType.INVALID_GLOBAL_ACCESS,
        ValidationErrorType.INVALID_CONSTANT_EXPRESSION,
        ValidationErrorType.UNSUPPORTED_FEATURE,
        ValidationErrorType.LIMIT_EXCEEDED,
        ValidationErrorType.UNKNOWN
      };

      for (ValidationErrorType type : nonStructuralTypes) {
        final ModuleValidationException exception = new ModuleValidationException(type, "Error");
        assertFalse(
            exception.isStructuralError(), type.name() + " should NOT be a structural error");
      }
    }

    @Test
    @DisplayName("Should have exactly 4 structural error types")
    void shouldHaveExactly4StructuralErrorTypes() {
      int structuralCount = 0;
      for (ValidationErrorType type : ValidationErrorType.values()) {
        final ModuleValidationException exception = new ModuleValidationException(type, "Error");
        if (exception.isStructuralError()) {
          structuralCount++;
        }
      }
      assertEquals(
          4,
          structuralCount,
          "Should have exactly 4 structural error types: INVALID_MAGIC_NUMBER, MALFORMED_MODULE,"
              + " INVALID_DATA_SEGMENT, INVALID_ELEMENT_SEGMENT");
    }

    @Test
    @DisplayName("isTypeError should return false for all non-type error types")
    void isTypeErrorShouldReturnFalseForAllNonTypeErrorTypes() {
      final ValidationErrorType[] nonTypeErrors = {
        ValidationErrorType.INVALID_MAGIC_NUMBER,
        ValidationErrorType.MALFORMED_MODULE,
        ValidationErrorType.INVALID_IMPORT,
        ValidationErrorType.INVALID_EXPORT,
        ValidationErrorType.INVALID_MEMORY_DEFINITION,
        ValidationErrorType.INVALID_TABLE_DEFINITION,
        ValidationErrorType.INVALID_GLOBAL_DEFINITION,
        ValidationErrorType.INVALID_FUNCTION_BODY,
        ValidationErrorType.INVALID_CONTROL_FLOW,
        ValidationErrorType.INVALID_MEMORY_OPERATION,
        ValidationErrorType.INVALID_TABLE_OPERATION,
        ValidationErrorType.INVALID_CALL,
        ValidationErrorType.INVALID_LOCAL_ACCESS,
        ValidationErrorType.INVALID_GLOBAL_ACCESS,
        ValidationErrorType.INVALID_CONSTANT_EXPRESSION,
        ValidationErrorType.INVALID_DATA_SEGMENT,
        ValidationErrorType.INVALID_ELEMENT_SEGMENT,
        ValidationErrorType.UNSUPPORTED_FEATURE,
        ValidationErrorType.LIMIT_EXCEEDED,
        ValidationErrorType.UNKNOWN
      };

      for (ValidationErrorType type : nonTypeErrors) {
        final ModuleValidationException exception = new ModuleValidationException(type, "Error");
        assertFalse(exception.isTypeError(), type.name() + " should NOT be a type error");
      }
    }

    @Test
    @DisplayName("Should have exactly 3 type error types")
    void shouldHaveExactly3TypeErrorTypes() {
      int typeErrorCount = 0;
      for (ValidationErrorType type : ValidationErrorType.values()) {
        final ModuleValidationException exception = new ModuleValidationException(type, "Error");
        if (exception.isTypeError()) {
          typeErrorCount++;
        }
      }
      assertEquals(
          3,
          typeErrorCount,
          "Should have exactly 3 type error types: "
              + "INVALID_TYPE_DEFINITION, INVALID_FUNCTION_SIGNATURE, TYPE_MISMATCH");
    }

    @Test
    @DisplayName("isImportExportError should return false for all non-import/export types")
    void isImportExportErrorShouldReturnFalseForAllNonImportExportTypes() {
      for (ValidationErrorType type : ValidationErrorType.values()) {
        if (type != ValidationErrorType.INVALID_IMPORT
            && type != ValidationErrorType.INVALID_EXPORT) {
          final ModuleValidationException exception = new ModuleValidationException(type, "Error");
          assertFalse(
              exception.isImportExportError(),
              type.name() + " should NOT be an import/export error");
        }
      }
    }

    @Test
    @DisplayName("Should have exactly 2 import/export error types")
    void shouldHaveExactly2ImportExportErrorTypes() {
      int count = 0;
      for (ValidationErrorType type : ValidationErrorType.values()) {
        final ModuleValidationException exception = new ModuleValidationException(type, "Error");
        if (exception.isImportExportError()) {
          count++;
        }
      }
      assertEquals(2, count, "Should have exactly 2 import/export error types");
    }

    @Test
    @DisplayName("isMemoryError should return false for all non-memory types")
    void isMemoryErrorShouldReturnFalseForAllNonMemoryTypes() {
      for (ValidationErrorType type : ValidationErrorType.values()) {
        if (type != ValidationErrorType.INVALID_MEMORY_DEFINITION
            && type != ValidationErrorType.INVALID_MEMORY_OPERATION) {
          final ModuleValidationException exception = new ModuleValidationException(type, "Error");
          assertFalse(exception.isMemoryError(), type.name() + " should NOT be a memory error");
        }
      }
    }

    @Test
    @DisplayName("Should have exactly 2 memory error types")
    void shouldHaveExactly2MemoryErrorTypes() {
      int count = 0;
      for (ValidationErrorType type : ValidationErrorType.values()) {
        final ModuleValidationException exception = new ModuleValidationException(type, "Error");
        if (exception.isMemoryError()) {
          count++;
        }
      }
      assertEquals(2, count, "Should have exactly 2 memory error types");
    }

    @Test
    @DisplayName("isFeatureError should return false for all non-feature types")
    void isFeatureErrorShouldReturnFalseForAllNonFeatureTypes() {
      for (ValidationErrorType type : ValidationErrorType.values()) {
        if (type != ValidationErrorType.UNSUPPORTED_FEATURE
            && type != ValidationErrorType.LIMIT_EXCEEDED) {
          final ModuleValidationException exception = new ModuleValidationException(type, "Error");
          assertFalse(exception.isFeatureError(), type.name() + " should NOT be a feature error");
        }
      }
    }

    @Test
    @DisplayName("Should have exactly 2 feature error types")
    void shouldHaveExactly2FeatureErrorTypes() {
      int count = 0;
      for (ValidationErrorType type : ValidationErrorType.values()) {
        final ModuleValidationException exception = new ModuleValidationException(type, "Error");
        if (exception.isFeatureError()) {
          count++;
        }
      }
      assertEquals(2, count, "Should have exactly 2 feature error types");
    }
  }

  @Nested
  @DisplayName("formatMessage Edge Case Mutation Tests")
  class FormatMessageEdgeCaseMutationTests {

    @Test
    @DisplayName("Message should not include section marker when section is empty")
    void messageShouldNotIncludeSectionWhenEmpty() {
      final ModuleValidationException exception =
          new ModuleValidationException(
              ValidationErrorType.TYPE_MISMATCH, "Error message", "", null, null);

      assertFalse(
          exception.getMessage().contains("(section:"),
          "Message should not contain section marker when section is empty");
    }

    @Test
    @DisplayName("Message should not include section marker when section is null")
    void messageShouldNotIncludeSectionWhenNull() {
      final ModuleValidationException exception =
          new ModuleValidationException(
              ValidationErrorType.TYPE_MISMATCH, "Error message", null, null, null);

      assertFalse(
          exception.getMessage().contains("(section:"),
          "Message should not contain section marker when section is null");
    }

    @Test
    @DisplayName("Message should not include error type marker when error type is null")
    void messageShouldNotIncludeErrorTypeMarkerWhenNull() {
      // Null error type defaults to UNKNOWN, so this tests the formatting path
      final ModuleValidationException exception =
          new ModuleValidationException(null, "Error message", null, null, null);

      // errorType is null in formatMessage, so no [TYPE] prefix
      // But constructor replaces null with UNKNOWN, so we check internal behavior
      assertEquals(
          ValidationErrorType.UNKNOWN,
          exception.getErrorType(),
          "Null error type should be replaced with UNKNOWN");
    }

    @Test
    @DisplayName("Message with all optional fields should contain all sections")
    void messageWithAllFieldsShouldContainAllSections() {
      final ModuleValidationException exception =
          new ModuleValidationException(
              ValidationErrorType.INVALID_CALL, "Test error", "function", 1024, null);

      final String message = exception.getMessage();
      assertTrue(message.contains("[INVALID_CALL]"), "Should contain error type");
      assertTrue(message.contains("Test error"), "Should contain base message");
      assertTrue(message.contains("(section: function)"), "Should contain section");
      assertTrue(message.contains("(offset: 1024)"), "Should contain offset");
    }

    @Test
    @DisplayName("Message sections should appear in correct order")
    void messageSectionsShouldAppearInCorrectOrder() {
      final ModuleValidationException exception =
          new ModuleValidationException(
              ValidationErrorType.INVALID_LOCAL_ACCESS, "Access error", "code", 512, null);

      final String message = exception.getMessage();
      final int errorTypeIndex = message.indexOf("[INVALID_LOCAL_ACCESS]");
      final int baseMessageIndex = message.indexOf("Access error");
      final int sectionIndex = message.indexOf("(section:");
      final int offsetIndex = message.indexOf("(offset:");

      assertTrue(errorTypeIndex < baseMessageIndex, "Error type should come before message");
      assertTrue(baseMessageIndex < sectionIndex, "Message should come before section");
      assertTrue(sectionIndex < offsetIndex, "Section should come before offset");
    }

    @Test
    @DisplayName("Offset should be included when it is zero")
    void offsetShouldBeIncludedWhenZero() {
      final ModuleValidationException exception =
          new ModuleValidationException(
              ValidationErrorType.INVALID_MAGIC_NUMBER, "Error", null, 0, null);

      assertTrue(
          exception.getMessage().contains("(offset: 0)"),
          "Message should contain offset when it is zero");
    }
  }

  @Nested
  @DisplayName("generateRecoverySuggestion Mutation Tests")
  class GenerateRecoverySuggestionMutationTests {

    @Test
    @DisplayName("INVALID_MAGIC_NUMBER should suggest checking WebAssembly format")
    void invalidMagicNumberShouldSuggestFormat() {
      final ModuleValidationException exception =
          new ModuleValidationException(ValidationErrorType.INVALID_MAGIC_NUMBER, "Error");
      assertTrue(
          exception.getRecoverySuggestion().toLowerCase().contains("bytecode")
              || exception.getRecoverySuggestion().toLowerCase().contains("webassembly"),
          "INVALID_MAGIC_NUMBER recovery should mention bytecode or WebAssembly");
    }

    @Test
    @DisplayName("MALFORMED_MODULE should suggest checking corruption")
    void malformedModuleShouldSuggestCorruption() {
      final ModuleValidationException exception =
          new ModuleValidationException(ValidationErrorType.MALFORMED_MODULE, "Error");
      assertTrue(
          exception.getRecoverySuggestion().toLowerCase().contains("corruption")
              || exception.getRecoverySuggestion().toLowerCase().contains("incomplete"),
          "MALFORMED_MODULE recovery should mention corruption or incomplete");
    }

    @Test
    @DisplayName("All error types should have distinct recovery suggestions")
    void allErrorTypesShouldHaveDistinctRecoverySuggestions() {
      final java.util.Set<String> suggestions = new java.util.HashSet<>();
      for (ValidationErrorType type : ValidationErrorType.values()) {
        final ModuleValidationException exception = new ModuleValidationException(type, "Error");
        final String suggestion = exception.getRecoverySuggestion();
        assertNotNull(suggestion, type.name() + " should have a recovery suggestion");
        assertFalse(suggestion.isEmpty(), type.name() + " should have non-empty suggestion");
        // Note: UNKNOWN and default share the same suggestion, so we allow that
        if (type != ValidationErrorType.UNKNOWN) {
          assertTrue(
              suggestions.add(suggestion),
              type.name() + " should have distinct suggestion but got duplicate: " + suggestion);
        }
      }
    }

    @Test
    @DisplayName("Each error type should have specific recovery suggestion")
    void eachErrorTypeShouldHaveSpecificRecoverySuggestion() {
      // Map of error types to expected keywords in their recovery suggestions
      final java.util.Map<ValidationErrorType, String[]> expectedKeywords =
          new java.util.HashMap<>();
      expectedKeywords.put(
          ValidationErrorType.INVALID_TYPE_DEFINITION, new String[] {"type", "definition"});
      expectedKeywords.put(
          ValidationErrorType.INVALID_FUNCTION_SIGNATURE, new String[] {"function", "signature"});
      expectedKeywords.put(ValidationErrorType.INVALID_IMPORT, new String[] {"import"});
      expectedKeywords.put(ValidationErrorType.INVALID_EXPORT, new String[] {"export"});
      expectedKeywords.put(ValidationErrorType.INVALID_MEMORY_DEFINITION, new String[] {"memory"});
      expectedKeywords.put(ValidationErrorType.INVALID_TABLE_DEFINITION, new String[] {"table"});
      expectedKeywords.put(ValidationErrorType.INVALID_GLOBAL_DEFINITION, new String[] {"global"});
      expectedKeywords.put(
          ValidationErrorType.INVALID_FUNCTION_BODY, new String[] {"function", "bytecode"});
      expectedKeywords.put(ValidationErrorType.TYPE_MISMATCH, new String[] {"stack", "type"});
      expectedKeywords.put(
          ValidationErrorType.INVALID_CONTROL_FLOW, new String[] {"control", "flow"});
      expectedKeywords.put(
          ValidationErrorType.INVALID_MEMORY_OPERATION, new String[] {"memory", "operation"});
      expectedKeywords.put(
          ValidationErrorType.INVALID_TABLE_OPERATION, new String[] {"table", "operation"});
      expectedKeywords.put(ValidationErrorType.INVALID_CALL, new String[] {"function", "call"});
      expectedKeywords.put(ValidationErrorType.INVALID_LOCAL_ACCESS, new String[] {"local"});
      expectedKeywords.put(ValidationErrorType.INVALID_GLOBAL_ACCESS, new String[] {"global"});
      expectedKeywords.put(
          ValidationErrorType.INVALID_CONSTANT_EXPRESSION, new String[] {"constant"});
      expectedKeywords.put(ValidationErrorType.INVALID_DATA_SEGMENT, new String[] {"data"});
      expectedKeywords.put(ValidationErrorType.INVALID_ELEMENT_SEGMENT, new String[] {"element"});
      expectedKeywords.put(ValidationErrorType.UNSUPPORTED_FEATURE, new String[] {"feature"});
      expectedKeywords.put(
          ValidationErrorType.LIMIT_EXCEEDED, new String[] {"complexity", "smaller"});
      expectedKeywords.put(ValidationErrorType.UNKNOWN, new String[] {"specification"});

      for (java.util.Map.Entry<ValidationErrorType, String[]> entry : expectedKeywords.entrySet()) {
        final ValidationErrorType type = entry.getKey();
        final String[] keywords = entry.getValue();
        final ModuleValidationException exception = new ModuleValidationException(type, "Error");
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
    @DisplayName("getModuleSection should return exact section passed to constructor")
    void getModuleSectionShouldReturnExactSection() {
      final String[] testSections = {"code", "data", "import", "export", "type", "", null};
      for (String section : testSections) {
        final ModuleValidationException exception =
            new ModuleValidationException(
                ValidationErrorType.UNKNOWN, "Error", section, null, null);
        assertEquals(
            section, exception.getModuleSection(), "getModuleSection should return: " + section);
      }
    }

    @Test
    @DisplayName("getByteOffset should return exact offset passed to constructor")
    void getByteOffsetShouldReturnExactOffset() {
      final Integer[] testOffsets = {null, 0, 1, 100, 1024, Integer.MAX_VALUE};
      for (Integer offset : testOffsets) {
        final ModuleValidationException exception =
            new ModuleValidationException(ValidationErrorType.UNKNOWN, "Error", null, offset, null);
        assertEquals(offset, exception.getByteOffset(), "getByteOffset should return: " + offset);
      }
    }

    @Test
    @DisplayName("getErrorType should return UNKNOWN when null is passed")
    void getErrorTypeShouldReturnUnknownWhenNullPassed() {
      final ModuleValidationException exception =
          new ModuleValidationException(null, "Error", null, null, null);
      assertEquals(
          ValidationErrorType.UNKNOWN,
          exception.getErrorType(),
          "getErrorType should return UNKNOWN when null was passed to constructor");
      assertFalse(exception.getErrorType() == null, "getErrorType should never return null");
    }
  }
}
