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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LinkingException} class.
 *
 * <p>LinkingException is thrown when WebAssembly module linking fails.
 */
@DisplayName("LinkingException Tests")
class LinkingExceptionTest {

  @Nested
  @DisplayName("LinkingErrorType Enum Tests")
  class LinkingErrorTypeEnumTests {

    @Test
    @DisplayName("should have all expected error types")
    void shouldHaveAllExpectedErrorTypes() {
      final LinkingException.LinkingErrorType[] values = LinkingException.LinkingErrorType.values();
      assertTrue(values.length >= 17, "Should have at least 17 linking error types");

      // Check key error types exist
      assertNotNull(
          LinkingException.LinkingErrorType.IMPORT_NOT_FOUND, "Should have IMPORT_NOT_FOUND");
      assertNotNull(
          LinkingException.LinkingErrorType.EXPORT_NOT_FOUND, "Should have EXPORT_NOT_FOUND");
      assertNotNull(
          LinkingException.LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH,
          "Should have FUNCTION_SIGNATURE_MISMATCH");
      assertNotNull(LinkingException.LinkingErrorType.UNKNOWN, "Should have UNKNOWN");
    }

    @Test
    @DisplayName("each error type should have description")
    void eachErrorTypeShouldHaveDescription() {
      for (final LinkingException.LinkingErrorType errorType :
          LinkingException.LinkingErrorType.values()) {
        assertNotNull(errorType.getDescription(), errorType.name() + " should have description");
        assertFalse(
            errorType.getDescription().isEmpty(),
            errorType.name() + " description should not be empty");
      }
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create exception with error type and message")
    void shouldCreateExceptionWithErrorTypeAndMessage() {
      final LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.IMPORT_NOT_FOUND, "Function 'add' not found");

      assertEquals(
          LinkingException.LinkingErrorType.IMPORT_NOT_FOUND,
          exception.getErrorType(),
          "Error type should be set");
      assertTrue(
          exception.getMessage().contains("IMPORT_NOT_FOUND"), "Message should contain error type");
      assertTrue(
          exception.getMessage().contains("Function 'add' not found"),
          "Message should contain detail");
    }

    @Test
    @DisplayName("should create exception with error type, message, and cause")
    void shouldCreateExceptionWithErrorTypeMessageAndCause() {
      final RuntimeException cause = new RuntimeException("Root cause");
      final LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH,
              "Signature mismatch",
              cause);

      assertEquals(
          LinkingException.LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH,
          exception.getErrorType(),
          "Error type should be set");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }

    @Test
    @DisplayName("should create exception with full details")
    void shouldCreateExceptionWithFullDetails() {
      final LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH,
              "Type mismatch",
              "env",
              "log",
              "(i32) -> void",
              "(i32, i32) -> void",
              null);

      assertEquals(
          LinkingException.LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH,
          exception.getErrorType(),
          "Error type should match");
      assertEquals("env", exception.getModuleName(), "Module name should be set");
      assertEquals("log", exception.getItemName(), "Item name should be set");
      assertEquals("(i32) -> void", exception.getExpectedType(), "Expected type should be set");
      assertEquals("(i32, i32) -> void", exception.getActualType(), "Actual type should be set");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for null or empty message")
    void shouldThrowForNullOrEmptyMessage() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new LinkingException(LinkingException.LinkingErrorType.UNKNOWN, null),
          "Should throw for null message");

      assertThrows(
          IllegalArgumentException.class,
          () -> new LinkingException(LinkingException.LinkingErrorType.UNKNOWN, ""),
          "Should throw for empty message");
    }

    @Test
    @DisplayName("should default to UNKNOWN when error type is null")
    void shouldDefaultToUnknownWhenErrorTypeIsNull() {
      final LinkingException exception =
          new LinkingException(null, "Unknown error", null, null, null, null, null);

      assertEquals(
          LinkingException.LinkingErrorType.UNKNOWN,
          exception.getErrorType(),
          "Should default to UNKNOWN error type");
    }
  }

  @Nested
  @DisplayName("Category Check Methods Tests")
  class CategoryCheckMethodsTests {

    @Test
    @DisplayName("isMissingItemError should return true for missing item errors")
    void isMissingItemErrorShouldReturnTrueForMissingItemErrors() {
      assertTrue(
          new LinkingException(LinkingException.LinkingErrorType.IMPORT_NOT_FOUND, "msg")
              .isMissingItemError(),
          "IMPORT_NOT_FOUND should be missing item error");
      assertTrue(
          new LinkingException(LinkingException.LinkingErrorType.EXPORT_NOT_FOUND, "msg")
              .isMissingItemError(),
          "EXPORT_NOT_FOUND should be missing item error");
      assertFalse(
          new LinkingException(LinkingException.LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH, "msg")
              .isMissingItemError(),
          "FUNCTION_SIGNATURE_MISMATCH should not be missing item error");
    }

    @Test
    @DisplayName("isTypeMismatchError should return true for type mismatch errors")
    void isTypeMismatchErrorShouldReturnTrueForTypeMismatchErrors() {
      assertTrue(
          new LinkingException(LinkingException.LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH, "msg")
              .isTypeMismatchError(),
          "FUNCTION_SIGNATURE_MISMATCH should be type mismatch error");
      assertTrue(
          new LinkingException(LinkingException.LinkingErrorType.GLOBAL_TYPE_MISMATCH, "msg")
              .isTypeMismatchError(),
          "GLOBAL_TYPE_MISMATCH should be type mismatch error");
      assertTrue(
          new LinkingException(LinkingException.LinkingErrorType.TABLE_TYPE_MISMATCH, "msg")
              .isTypeMismatchError(),
          "TABLE_TYPE_MISMATCH should be type mismatch error");
    }

    @Test
    @DisplayName("isHostFunctionError should return true for host function errors")
    void isHostFunctionErrorShouldReturnTrueForHostFunctionErrors() {
      assertTrue(
          new LinkingException(
                  LinkingException.LinkingErrorType.HOST_FUNCTION_BINDING_FAILED, "msg")
              .isHostFunctionError(),
          "HOST_FUNCTION_BINDING_FAILED should be host function error");
      assertTrue(
          new LinkingException(LinkingException.LinkingErrorType.WASI_IMPORT_FAILED, "msg")
              .isHostFunctionError(),
          "WASI_IMPORT_FAILED should be host function error");
    }

    @Test
    @DisplayName("isComponentError should return true for component errors")
    void isComponentErrorShouldReturnTrueForComponentErrors() {
      assertTrue(
          new LinkingException(LinkingException.LinkingErrorType.COMPONENT_LINKING_FAILED, "msg")
              .isComponentError(),
          "COMPONENT_LINKING_FAILED should be component error");
      assertTrue(
          new LinkingException(LinkingException.LinkingErrorType.INTERFACE_TYPE_MISMATCH, "msg")
              .isComponentError(),
          "INTERFACE_TYPE_MISMATCH should be component error");
    }

    @Test
    @DisplayName("isConfigurationError should return true for configuration errors")
    void isConfigurationErrorShouldReturnTrueForConfigurationErrors() {
      assertTrue(
          new LinkingException(LinkingException.LinkingErrorType.CIRCULAR_DEPENDENCY, "msg")
              .isConfigurationError(),
          "CIRCULAR_DEPENDENCY should be configuration error");
      assertTrue(
          new LinkingException(LinkingException.LinkingErrorType.NAMESPACE_CONFLICT, "msg")
              .isConfigurationError(),
          "NAMESPACE_CONFLICT should be configuration error");
      assertTrue(
          new LinkingException(LinkingException.LinkingErrorType.LINKER_CONFIGURATION_ERROR, "msg")
              .isConfigurationError(),
          "LINKER_CONFIGURATION_ERROR should be configuration error");
    }
  }

  @Nested
  @DisplayName("Recovery Suggestion Tests")
  class RecoverySuggestionTests {

    @Test
    @DisplayName("should provide recovery suggestion for each error type")
    void shouldProvideRecoverySuggestionForEachErrorType() {
      for (final LinkingException.LinkingErrorType errorType :
          LinkingException.LinkingErrorType.values()) {
        final LinkingException exception = new LinkingException(errorType, "test message");
        assertNotNull(
            exception.getRecoverySuggestion(),
            errorType.name() + " should have recovery suggestion");
        assertFalse(
            exception.getRecoverySuggestion().isEmpty(),
            errorType.name() + " recovery suggestion should not be empty");
      }
    }
  }

  @Nested
  @DisplayName("Accessor Methods Tests")
  class AccessorMethodsTests {

    @Test
    @DisplayName("getModuleName should return null when not set")
    void getModuleNameShouldReturnNullWhenNotSet() {
      final LinkingException exception =
          new LinkingException(LinkingException.LinkingErrorType.UNKNOWN, "test");
      assertNull(exception.getModuleName(), "Module name should be null when not set");
    }

    @Test
    @DisplayName("getItemName should return null when not set")
    void getItemNameShouldReturnNullWhenNotSet() {
      final LinkingException exception =
          new LinkingException(LinkingException.LinkingErrorType.UNKNOWN, "test");
      assertNull(exception.getItemName(), "Item name should be null when not set");
    }

    @Test
    @DisplayName("getExpectedType should return null when not set")
    void getExpectedTypeShouldReturnNullWhenNotSet() {
      final LinkingException exception =
          new LinkingException(LinkingException.LinkingErrorType.UNKNOWN, "test");
      assertNull(exception.getExpectedType(), "Expected type should be null when not set");
    }

    @Test
    @DisplayName("getActualType should return null when not set")
    void getActualTypeShouldReturnNullWhenNotSet() {
      final LinkingException exception =
          new LinkingException(LinkingException.LinkingErrorType.UNKNOWN, "test");
      assertNull(exception.getActualType(), "Actual type should be null when not set");
    }
  }

  @Nested
  @DisplayName("Error Category Boolean Return Mutation Tests")
  class ErrorCategoryBooleanReturnMutationTests {

    @Test
    @DisplayName("isMissingItemError should return false for all non-missing types")
    void isMissingItemErrorShouldReturnFalseForAllNonMissingTypes() {
      for (LinkingException.LinkingErrorType type : LinkingException.LinkingErrorType.values()) {
        if (type != LinkingException.LinkingErrorType.IMPORT_NOT_FOUND
            && type != LinkingException.LinkingErrorType.EXPORT_NOT_FOUND) {
          final LinkingException exception = new LinkingException(type, "Error");
          assertFalse(
              exception.isMissingItemError(), type.name() + " should NOT be a missing item error");
        }
      }
    }

    @Test
    @DisplayName("Should have exactly 2 missing item error types")
    void shouldHaveExactly2MissingItemErrorTypes() {
      int count = 0;
      for (LinkingException.LinkingErrorType type : LinkingException.LinkingErrorType.values()) {
        final LinkingException exception = new LinkingException(type, "Error");
        if (exception.isMissingItemError()) {
          count++;
        }
      }
      assertEquals(2, count, "Should have exactly 2 missing item error types");
    }

    @Test
    @DisplayName("isTypeMismatchError should return false for all non-type-mismatch types")
    void isTypeMismatchErrorShouldReturnFalseForAllNonTypeMismatchTypes() {
      final LinkingException.LinkingErrorType[] typeMismatchTypes = {
        LinkingException.LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH,
        LinkingException.LinkingErrorType.MEMORY_SIZE_MISMATCH,
        LinkingException.LinkingErrorType.MEMORY_LIMITS_INCOMPATIBLE,
        LinkingException.LinkingErrorType.TABLE_SIZE_MISMATCH,
        LinkingException.LinkingErrorType.TABLE_TYPE_MISMATCH,
        LinkingException.LinkingErrorType.GLOBAL_TYPE_MISMATCH,
        LinkingException.LinkingErrorType.GLOBAL_MUTABILITY_MISMATCH,
        LinkingException.LinkingErrorType.INTERFACE_TYPE_MISMATCH
      };
      java.util.Set<LinkingException.LinkingErrorType> typeMismatchSet =
          new java.util.HashSet<>(java.util.Arrays.asList(typeMismatchTypes));

      for (LinkingException.LinkingErrorType type : LinkingException.LinkingErrorType.values()) {
        if (!typeMismatchSet.contains(type)) {
          final LinkingException exception = new LinkingException(type, "Error");
          assertFalse(
              exception.isTypeMismatchError(),
              type.name() + " should NOT be a type mismatch error");
        }
      }
    }

    @Test
    @DisplayName("Should have exactly 8 type mismatch error types")
    void shouldHaveExactly8TypeMismatchErrorTypes() {
      int count = 0;
      for (LinkingException.LinkingErrorType type : LinkingException.LinkingErrorType.values()) {
        final LinkingException exception = new LinkingException(type, "Error");
        if (exception.isTypeMismatchError()) {
          count++;
        }
      }
      assertEquals(8, count, "Should have exactly 8 type mismatch error types");
    }

    @Test
    @DisplayName("isHostFunctionError should return false for all non-host-function types")
    void isHostFunctionErrorShouldReturnFalseForAllNonHostFunctionTypes() {
      for (LinkingException.LinkingErrorType type : LinkingException.LinkingErrorType.values()) {
        if (type != LinkingException.LinkingErrorType.HOST_FUNCTION_BINDING_FAILED
            && type != LinkingException.LinkingErrorType.WASI_IMPORT_FAILED) {
          final LinkingException exception = new LinkingException(type, "Error");
          assertFalse(
              exception.isHostFunctionError(),
              type.name() + " should NOT be a host function error");
        }
      }
    }

    @Test
    @DisplayName("Should have exactly 2 host function error types")
    void shouldHaveExactly2HostFunctionErrorTypes() {
      int count = 0;
      for (LinkingException.LinkingErrorType type : LinkingException.LinkingErrorType.values()) {
        final LinkingException exception = new LinkingException(type, "Error");
        if (exception.isHostFunctionError()) {
          count++;
        }
      }
      assertEquals(2, count, "Should have exactly 2 host function error types");
    }

    @Test
    @DisplayName("isComponentError should return false for all non-component types")
    void isComponentErrorShouldReturnFalseForAllNonComponentTypes() {
      final LinkingException.LinkingErrorType[] componentTypes = {
        LinkingException.LinkingErrorType.COMPONENT_LINKING_FAILED,
        LinkingException.LinkingErrorType.INTERFACE_TYPE_MISMATCH,
        LinkingException.LinkingErrorType.RESOURCE_TYPE_LINKING_FAILED,
        LinkingException.LinkingErrorType.CAPABILITY_NOT_SATISFIED
      };
      java.util.Set<LinkingException.LinkingErrorType> componentSet =
          new java.util.HashSet<>(java.util.Arrays.asList(componentTypes));

      for (LinkingException.LinkingErrorType type : LinkingException.LinkingErrorType.values()) {
        if (!componentSet.contains(type)) {
          final LinkingException exception = new LinkingException(type, "Error");
          assertFalse(
              exception.isComponentError(), type.name() + " should NOT be a component error");
        }
      }
    }

    @Test
    @DisplayName("Should have exactly 4 component error types")
    void shouldHaveExactly4ComponentErrorTypes() {
      int count = 0;
      for (LinkingException.LinkingErrorType type : LinkingException.LinkingErrorType.values()) {
        final LinkingException exception = new LinkingException(type, "Error");
        if (exception.isComponentError()) {
          count++;
        }
      }
      assertEquals(4, count, "Should have exactly 4 component error types");
    }

    @Test
    @DisplayName("isConfigurationError should return false for all non-configuration types")
    void isConfigurationErrorShouldReturnFalseForAllNonConfigurationTypes() {
      for (LinkingException.LinkingErrorType type : LinkingException.LinkingErrorType.values()) {
        if (type != LinkingException.LinkingErrorType.CIRCULAR_DEPENDENCY
            && type != LinkingException.LinkingErrorType.NAMESPACE_CONFLICT
            && type != LinkingException.LinkingErrorType.LINKER_CONFIGURATION_ERROR) {
          final LinkingException exception = new LinkingException(type, "Error");
          assertFalse(
              exception.isConfigurationError(),
              type.name() + " should NOT be a configuration error");
        }
      }
    }

    @Test
    @DisplayName("Should have exactly 3 configuration error types")
    void shouldHaveExactly3ConfigurationErrorTypes() {
      int count = 0;
      for (LinkingException.LinkingErrorType type : LinkingException.LinkingErrorType.values()) {
        final LinkingException exception = new LinkingException(type, "Error");
        if (exception.isConfigurationError()) {
          count++;
        }
      }
      assertEquals(3, count, "Should have exactly 3 configuration error types");
    }
  }

  @Nested
  @DisplayName("formatMessage Edge Case Mutation Tests")
  class FormatMessageEdgeCaseMutationTests {

    @Test
    @DisplayName("Message should include module.item format when both provided")
    void messageShouldIncludeModuleItemFormatWhenBothProvided() {
      final LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.IMPORT_NOT_FOUND,
              "Error message",
              "env",
              "log_func",
              null,
              null,
              null);

      assertTrue(
          exception.getMessage().contains("(env.log_func)"),
          "Message should contain module.item format");
    }

    @Test
    @DisplayName("Message should include item name only when module is null")
    void messageShouldIncludeItemNameOnlyWhenModuleIsNull() {
      final LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.IMPORT_NOT_FOUND,
              "Error message",
              null,
              "my_func",
              null,
              null,
              null);

      assertTrue(
          exception.getMessage().contains("(my_func)"), "Message should contain item name only");
      assertFalse(
          exception.getMessage().contains("(module:"),
          "Message should not have module format when only item provided");
    }

    @Test
    @DisplayName("Message should include module only when item is null")
    void messageShouldIncludeModuleOnlyWhenItemIsNull() {
      final LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.IMPORT_NOT_FOUND,
              "Error message",
              "env",
              null,
              null,
              null,
              null);

      assertTrue(
          exception.getMessage().contains("(module: env)"),
          "Message should contain module format only");
    }

    @Test
    @DisplayName("Message should include expected and actual types when both provided")
    void messageShouldIncludeExpectedAndActualTypesWhenBothProvided() {
      final LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH,
              "Error message",
              null,
              null,
              "(i32) -> i32",
              "(i64) -> i64",
              null);

      assertTrue(
          exception.getMessage().contains("(expected: (i32) -> i32, actual: (i64) -> i64)"),
          "Message should contain expected and actual types");
    }

    @Test
    @DisplayName("Message should include expected type only when actual is null")
    void messageShouldIncludeExpectedTypeOnlyWhenActualIsNull() {
      final LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH,
              "Error message",
              null,
              null,
              "(i32) -> i32",
              null,
              null);

      assertTrue(
          exception.getMessage().contains("(expected: (i32) -> i32)"),
          "Message should contain expected type only");
      assertFalse(
          exception.getMessage().contains("actual:"),
          "Message should not contain actual when null");
    }

    @Test
    @DisplayName("Message should not include types when both are empty")
    void messageShouldNotIncludeTypesWhenBothEmpty() {
      final LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.UNKNOWN, "Error message", null, null, "", "", null);

      assertFalse(
          exception.getMessage().contains("expected:"),
          "Message should not contain expected when empty");
      assertFalse(
          exception.getMessage().contains("actual:"),
          "Message should not contain actual when empty");
    }

    @Test
    @DisplayName("Message should not include module/item when both are empty")
    void messageShouldNotIncludeModuleItemWhenBothEmpty() {
      final LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.UNKNOWN, "Error message", "", "", null, null, null);

      // The message should just be "[UNKNOWN] Error message" without module/item markers
      assertFalse(
          exception.getMessage().contains("(module:"),
          "Message should not contain module marker when empty");
      // Check it doesn't have a standalone item name either
      assertEquals(
          "[UNKNOWN] Error message",
          exception.getMessage(),
          "Message should only contain error type and base message");
    }

    @Test
    @DisplayName("Message with all fields should contain all sections")
    void messageWithAllFieldsShouldContainAllSections() {
      final LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH,
              "Test error",
              "env",
              "log",
              "(i32)",
              "(i64)",
              null);

      final String message = exception.getMessage();
      assertTrue(message.contains("[FUNCTION_SIGNATURE_MISMATCH]"), "Should contain error type");
      assertTrue(message.contains("Test error"), "Should contain base message");
      assertTrue(message.contains("(env.log)"), "Should contain module.item");
      assertTrue(
          message.contains("(expected: (i32), actual: (i64))"), "Should contain type comparison");
    }
  }

  @Nested
  @DisplayName("generateRecoverySuggestion Mutation Tests")
  class GenerateRecoverySuggestionMutationTests {

    @Test
    @DisplayName("All error types should have distinct recovery suggestions")
    void allErrorTypesShouldHaveDistinctRecoverySuggestions() {
      final java.util.Set<String> suggestions = new java.util.HashSet<>();
      for (LinkingException.LinkingErrorType type : LinkingException.LinkingErrorType.values()) {
        final LinkingException exception = new LinkingException(type, "Error");
        final String suggestion = exception.getRecoverySuggestion();
        assertNotNull(suggestion, type.name() + " should have a recovery suggestion");
        assertFalse(suggestion.isEmpty(), type.name() + " should have non-empty suggestion");
        // UNKNOWN and default share the same suggestion
        if (type != LinkingException.LinkingErrorType.UNKNOWN) {
          assertTrue(
              suggestions.add(suggestion),
              type.name() + " should have distinct suggestion but got duplicate: " + suggestion);
        }
      }
    }

    @Test
    @DisplayName("Each specific error type should have contextual suggestion")
    void eachErrorTypeShouldHaveContextualSuggestion() {
      final java.util.Map<LinkingException.LinkingErrorType, String[]> expectedKeywords =
          new java.util.HashMap<>();
      expectedKeywords.put(
          LinkingException.LinkingErrorType.IMPORT_NOT_FOUND, new String[] {"import", "linker"});
      expectedKeywords.put(
          LinkingException.LinkingErrorType.EXPORT_NOT_FOUND, new String[] {"export", "module"});
      expectedKeywords.put(
          LinkingException.LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH,
          new String[] {"function", "type"});
      expectedKeywords.put(
          LinkingException.LinkingErrorType.MEMORY_SIZE_MISMATCH, new String[] {"memory", "size"});
      expectedKeywords.put(
          LinkingException.LinkingErrorType.MEMORY_LIMITS_INCOMPATIBLE,
          new String[] {"memory", "limit"});
      expectedKeywords.put(
          LinkingException.LinkingErrorType.TABLE_SIZE_MISMATCH, new String[] {"table", "size"});
      expectedKeywords.put(
          LinkingException.LinkingErrorType.TABLE_TYPE_MISMATCH, new String[] {"table", "type"});
      expectedKeywords.put(
          LinkingException.LinkingErrorType.GLOBAL_TYPE_MISMATCH, new String[] {"global", "type"});
      expectedKeywords.put(
          LinkingException.LinkingErrorType.GLOBAL_MUTABILITY_MISMATCH,
          new String[] {"global", "mutability"});
      expectedKeywords.put(
          LinkingException.LinkingErrorType.CIRCULAR_DEPENDENCY,
          new String[] {"circular", "dependency"});
      expectedKeywords.put(
          LinkingException.LinkingErrorType.NAMESPACE_CONFLICT,
          new String[] {"namespace", "unique"});
      expectedKeywords.put(
          LinkingException.LinkingErrorType.HOST_FUNCTION_BINDING_FAILED,
          new String[] {"host", "function"});
      expectedKeywords.put(
          LinkingException.LinkingErrorType.WASI_IMPORT_FAILED, new String[] {"wasi", "support"});
      expectedKeywords.put(
          LinkingException.LinkingErrorType.COMPONENT_LINKING_FAILED,
          new String[] {"component", "interface"});
      expectedKeywords.put(
          LinkingException.LinkingErrorType.INTERFACE_TYPE_MISMATCH, new String[] {"wit", "type"});
      expectedKeywords.put(
          LinkingException.LinkingErrorType.RESOURCE_TYPE_LINKING_FAILED,
          new String[] {"resource", "type"});
      expectedKeywords.put(
          LinkingException.LinkingErrorType.CAPABILITY_NOT_SATISFIED,
          new String[] {"capability", "required"});
      expectedKeywords.put(
          LinkingException.LinkingErrorType.LINKER_CONFIGURATION_ERROR,
          new String[] {"linker", "configuration"});
      expectedKeywords.put(
          LinkingException.LinkingErrorType.UNKNOWN,
          new String[] {"compatibility", "configuration"});

      for (java.util.Map.Entry<LinkingException.LinkingErrorType, String[]> entry :
          expectedKeywords.entrySet()) {
        final LinkingException.LinkingErrorType type = entry.getKey();
        final String[] keywords = entry.getValue();
        final LinkingException exception = new LinkingException(type, "Error");
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
    @DisplayName("getModuleName should return exact name passed to constructor")
    void getModuleNameShouldReturnExactName() {
      final String[] testNames = {"env", "wasi_snapshot_preview1", "custom_module", "", null};
      for (String name : testNames) {
        final LinkingException exception =
            new LinkingException(
                LinkingException.LinkingErrorType.UNKNOWN, "Error", name, null, null, null, null);
        assertEquals(name, exception.getModuleName(), "getModuleName should return: " + name);
      }
    }

    @Test
    @DisplayName("getItemName should return exact name passed to constructor")
    void getItemNameShouldReturnExactName() {
      final String[] testNames = {"log", "memory", "__heap_base", "", null};
      for (String name : testNames) {
        final LinkingException exception =
            new LinkingException(
                LinkingException.LinkingErrorType.UNKNOWN, "Error", null, name, null, null, null);
        assertEquals(name, exception.getItemName(), "getItemName should return: " + name);
      }
    }

    @Test
    @DisplayName("getExpectedType should return exact type passed to constructor")
    void getExpectedTypeShouldReturnExactType() {
      final String[] testTypes = {"(i32) -> void", "[i32; 10]", "funcref", "", null};
      for (String type : testTypes) {
        final LinkingException exception =
            new LinkingException(
                LinkingException.LinkingErrorType.UNKNOWN, "Error", null, null, type, null, null);
        assertEquals(type, exception.getExpectedType(), "getExpectedType should return: " + type);
      }
    }

    @Test
    @DisplayName("getActualType should return exact type passed to constructor")
    void getActualTypeShouldReturnExactType() {
      final String[] testTypes = {"(i64) -> i32", "[i64; 5]", "externref", "", null};
      for (String type : testTypes) {
        final LinkingException exception =
            new LinkingException(
                LinkingException.LinkingErrorType.UNKNOWN, "Error", null, null, null, type, null);
        assertEquals(type, exception.getActualType(), "getActualType should return: " + type);
      }
    }

    @Test
    @DisplayName("getErrorType should return UNKNOWN when null is passed")
    void getErrorTypeShouldReturnUnknownWhenNullPassed() {
      final LinkingException exception =
          new LinkingException(null, "Error", null, null, null, null, null);
      assertEquals(
          LinkingException.LinkingErrorType.UNKNOWN,
          exception.getErrorType(),
          "getErrorType should return UNKNOWN when null was passed");
    }
  }

  @Nested
  @DisplayName("formatMessage Equality Check Mutation Tests")
  class FormatMessageEqualityCheckMutationTests {

    @Test
    @DisplayName("null module with non-null non-empty item should show item only")
    void nullModuleWithNonNullItemShouldShowItemOnly() {
      final LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.UNKNOWN, "Error", null, "func", null, null, null);
      assertTrue(
          exception.getMessage().contains("(func)"),
          "Should show item only when module is null. Got: " + exception.getMessage());
      assertFalse(
          exception.getMessage().contains("module:"),
          "Should not show module format. Got: " + exception.getMessage());
    }

    @Test
    @DisplayName("empty module with non-null non-empty item should show item only")
    void emptyModuleWithNonNullItemShouldShowItemOnly() {
      final LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.UNKNOWN, "Error", "", "func", null, null, null);
      assertTrue(
          exception.getMessage().contains("(func)"),
          "Should show item only when module is empty. Got: " + exception.getMessage());
    }

    @Test
    @DisplayName("non-null module with null item should show module only")
    void nonNullModuleWithNullItemShouldShowModuleOnly() {
      final LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.UNKNOWN, "Error", "env", null, null, null, null);
      assertTrue(
          exception.getMessage().contains("(module: env)"),
          "Should show module only when item is null. Got: " + exception.getMessage());
    }

    @Test
    @DisplayName("non-null module with empty item should show module only")
    void nonNullModuleWithEmptyItemShouldShowModuleOnly() {
      final LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.UNKNOWN, "Error", "env", "", null, null, null);
      assertTrue(
          exception.getMessage().contains("(module: env)"),
          "Should show module only when item is empty. Got: " + exception.getMessage());
    }

    @Test
    @DisplayName("non-null expected with null actual should show expected only")
    void nonNullExpectedWithNullActualShouldShowExpectedOnly() {
      final LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.UNKNOWN, "Error", null, null, "(i32)", null, null);
      assertTrue(
          exception.getMessage().contains("(expected: (i32))"),
          "Should show expected only when actual is null. Got: " + exception.getMessage());
      assertFalse(exception.getMessage().contains("actual:"), "Should not show actual when null");
    }

    @Test
    @DisplayName("non-null expected with empty actual should show expected only")
    void nonNullExpectedWithEmptyActualShouldShowExpectedOnly() {
      final LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.UNKNOWN, "Error", null, null, "(i32)", "", null);
      assertTrue(
          exception.getMessage().contains("(expected: (i32))"),
          "Should show expected only when actual is empty. Got: " + exception.getMessage());
      assertFalse(exception.getMessage().contains("actual:"), "Should not show actual when empty");
    }

    @Test
    @DisplayName("null expected should not show any type info regardless of actual")
    void nullExpectedShouldNotShowTypeInfo() {
      final LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.UNKNOWN, "Error", null, null, null, "(i64)", null);
      assertFalse(
          exception.getMessage().contains("expected:"), "Should not show expected when null");
      assertFalse(
          exception.getMessage().contains("actual:"),
          "Should not show actual when expected is null");
    }

    @Test
    @DisplayName("empty expected should not show any type info regardless of actual")
    void emptyExpectedShouldNotShowTypeInfo() {
      final LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.UNKNOWN, "Error", null, null, "", "(i64)", null);
      assertFalse(
          exception.getMessage().contains("expected:"), "Should not show expected when empty");
      assertFalse(
          exception.getMessage().contains("actual:"),
          "Should not show actual when expected is empty");
    }

    @Test
    @DisplayName("null module and null item should not add module/item section")
    void nullModuleAndNullItemShouldNotAddSection() {
      final LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.UNKNOWN, "Error", null, null, null, null, null);
      assertEquals(
          "[UNKNOWN] Error", exception.getMessage(), "Message should not have module/item section");
    }
  }
}
