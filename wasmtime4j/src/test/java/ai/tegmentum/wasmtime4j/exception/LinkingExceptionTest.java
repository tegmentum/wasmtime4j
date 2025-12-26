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

import java.lang.reflect.Modifier;
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
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(LinkingException.class.getModifiers()),
          "LinkingException should be public");
    }

    @Test
    @DisplayName("should extend WasmException")
    void shouldExtendWasmException() {
      assertTrue(
          WasmException.class.isAssignableFrom(LinkingException.class),
          "LinkingException should extend WasmException");
    }

    @Test
    @DisplayName("should have LinkingErrorType nested enum")
    void shouldHaveLinkingErrorTypeNestedEnum() {
      final Class<?>[] declaredClasses = LinkingException.class.getDeclaredClasses();
      boolean hasLinkingErrorType = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("LinkingErrorType") && clazz.isEnum()) {
          hasLinkingErrorType = true;
          break;
        }
      }
      assertTrue(hasLinkingErrorType, "LinkingException should have LinkingErrorType nested enum");
    }
  }

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
}
