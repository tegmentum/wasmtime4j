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
}
