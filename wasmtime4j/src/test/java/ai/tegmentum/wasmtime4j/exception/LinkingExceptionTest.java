package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.LinkingException.LinkingErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests for the {@link LinkingException} class.
 *
 * <p>This test class verifies linking exception construction, error types, and classification.
 */
@DisplayName("LinkingException Tests")
class LinkingExceptionTest {

  @Nested
  @DisplayName("LinkingErrorType Enum Tests")
  class LinkingErrorTypeTests {

    @ParameterizedTest
    @EnumSource(LinkingErrorType.class)
    @DisplayName("All error types should have non-null descriptions")
    void allErrorTypesShouldHaveDescriptions(final LinkingErrorType errorType) {
      assertNotNull(errorType.getDescription());
      assertFalse(errorType.getDescription().isEmpty());
    }

    @Test
    @DisplayName("IMPORT_NOT_FOUND should have correct description")
    void importNotFoundDescription() {
      assertEquals("Import not found", LinkingErrorType.IMPORT_NOT_FOUND.getDescription());
    }

    @Test
    @DisplayName("FUNCTION_SIGNATURE_MISMATCH should have correct description")
    void functionSignatureMismatchDescription() {
      assertEquals(
          "Function signature mismatch",
          LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH.getDescription());
    }

    @Test
    @DisplayName("All linking error types should be accessible")
    void allLinkingErrorTypesAccessible() {
      LinkingErrorType[] allTypes = LinkingErrorType.values();
      assertEquals(19, allTypes.length);

      // Verify expected types exist
      assertNotNull(LinkingErrorType.valueOf("IMPORT_NOT_FOUND"));
      assertNotNull(LinkingErrorType.valueOf("EXPORT_NOT_FOUND"));
      assertNotNull(LinkingErrorType.valueOf("FUNCTION_SIGNATURE_MISMATCH"));
      assertNotNull(LinkingErrorType.valueOf("MEMORY_SIZE_MISMATCH"));
      assertNotNull(LinkingErrorType.valueOf("MEMORY_LIMITS_INCOMPATIBLE"));
      assertNotNull(LinkingErrorType.valueOf("TABLE_SIZE_MISMATCH"));
      assertNotNull(LinkingErrorType.valueOf("TABLE_TYPE_MISMATCH"));
      assertNotNull(LinkingErrorType.valueOf("GLOBAL_TYPE_MISMATCH"));
      assertNotNull(LinkingErrorType.valueOf("GLOBAL_MUTABILITY_MISMATCH"));
      assertNotNull(LinkingErrorType.valueOf("CIRCULAR_DEPENDENCY"));
      assertNotNull(LinkingErrorType.valueOf("NAMESPACE_CONFLICT"));
      assertNotNull(LinkingErrorType.valueOf("HOST_FUNCTION_BINDING_FAILED"));
      assertNotNull(LinkingErrorType.valueOf("WASI_IMPORT_FAILED"));
      assertNotNull(LinkingErrorType.valueOf("COMPONENT_LINKING_FAILED"));
      assertNotNull(LinkingErrorType.valueOf("INTERFACE_TYPE_MISMATCH"));
      assertNotNull(LinkingErrorType.valueOf("RESOURCE_TYPE_LINKING_FAILED"));
      assertNotNull(LinkingErrorType.valueOf("CAPABILITY_NOT_SATISFIED"));
      assertNotNull(LinkingErrorType.valueOf("LINKER_CONFIGURATION_ERROR"));
      assertNotNull(LinkingErrorType.valueOf("UNKNOWN"));
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with error type and message should set both")
    void constructorWithErrorTypeAndMessage() {
      LinkingException exception =
          new LinkingException(LinkingErrorType.IMPORT_NOT_FOUND, "Import env::memory not found");

      assertEquals(LinkingErrorType.IMPORT_NOT_FOUND, exception.getErrorType());
      assertTrue(exception.getMessage().contains("Import env::memory not found"));
      assertTrue(exception.getMessage().contains("IMPORT_NOT_FOUND"));
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Constructor with null error type should default to UNKNOWN")
    void constructorWithNullErrorType() {
      LinkingException exception = new LinkingException(null, "Test message");

      assertEquals(LinkingErrorType.UNKNOWN, exception.getErrorType());
    }

    @Test
    @DisplayName("Constructor with empty message should throw IllegalArgumentException")
    void constructorWithEmptyMessage() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            new LinkingException(LinkingErrorType.IMPORT_NOT_FOUND, "");
          });
    }

    @Test
    @DisplayName("Constructor with null message should throw IllegalArgumentException")
    void constructorWithNullMessage() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            new LinkingException(LinkingErrorType.IMPORT_NOT_FOUND, null);
          });
    }

    @Test
    @DisplayName("Constructor with error type, message, and cause should set all")
    void constructorWithErrorTypeMessageAndCause() {
      Throwable cause = new RuntimeException("Root cause");
      LinkingException exception =
          new LinkingException(
              LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH, "Signature mismatch", cause);

      assertEquals(LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH, exception.getErrorType());
      assertTrue(exception.getMessage().contains("Signature mismatch"));
      assertSame(cause, exception.getCause());
    }

    @Test
    @DisplayName("Full constructor should set all fields")
    void fullConstructor() {
      Throwable cause = new RuntimeException("Root cause");
      LinkingException exception =
          new LinkingException(
              LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH,
              "Function type mismatch",
              "my_module",
              "add_numbers",
              "[i32, i32] -> [i32]",
              "[i64, i64] -> [i64]",
              cause);

      assertEquals(LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH, exception.getErrorType());
      assertEquals("my_module", exception.getModuleName());
      assertEquals("add_numbers", exception.getItemName());
      assertEquals("[i32, i32] -> [i32]", exception.getExpectedType());
      assertEquals("[i64, i64] -> [i64]", exception.getActualType());
      assertSame(cause, exception.getCause());
    }

    @Test
    @DisplayName("Constructor with null optional fields should accept them")
    void constructorWithNullOptionalFields() {
      LinkingException exception =
          new LinkingException(
              LinkingErrorType.IMPORT_NOT_FOUND, "Import not found", null, null, null, null, null);

      assertEquals(LinkingErrorType.IMPORT_NOT_FOUND, exception.getErrorType());
      assertNull(exception.getModuleName());
      assertNull(exception.getItemName());
      assertNull(exception.getExpectedType());
      assertNull(exception.getActualType());
      assertNull(exception.getCause());
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getErrorType should return correct error type")
    void getErrorType() {
      LinkingException exception =
          new LinkingException(LinkingErrorType.MEMORY_SIZE_MISMATCH, "Memory error");
      assertEquals(LinkingErrorType.MEMORY_SIZE_MISMATCH, exception.getErrorType());
    }

    @Test
    @DisplayName("getModuleName should return module name")
    void getModuleName() {
      LinkingException exception =
          new LinkingException(
              LinkingErrorType.EXPORT_NOT_FOUND,
              "Export not found",
              "test_module",
              null,
              null,
              null,
              null);
      assertEquals("test_module", exception.getModuleName());
    }

    @Test
    @DisplayName("getItemName should return item name")
    void getItemName() {
      LinkingException exception =
          new LinkingException(
              LinkingErrorType.IMPORT_NOT_FOUND,
              "Import not found",
              null,
              "my_function",
              null,
              null,
              null);
      assertEquals("my_function", exception.getItemName());
    }

    @Test
    @DisplayName("getExpectedType should return expected type")
    void getExpectedType() {
      LinkingException exception =
          new LinkingException(
              LinkingErrorType.GLOBAL_TYPE_MISMATCH,
              "Type mismatch",
              null,
              null,
              "i32",
              "i64",
              null);
      assertEquals("i32", exception.getExpectedType());
    }

    @Test
    @DisplayName("getActualType should return actual type")
    void getActualType() {
      LinkingException exception =
          new LinkingException(
              LinkingErrorType.GLOBAL_TYPE_MISMATCH,
              "Type mismatch",
              null,
              null,
              "i32",
              "i64",
              null);
      assertEquals("i64", exception.getActualType());
    }

    @Test
    @DisplayName("getRecoverySuggestion should return non-null suggestion")
    void getRecoverySuggestion() {
      LinkingException exception =
          new LinkingException(LinkingErrorType.IMPORT_NOT_FOUND, "Import missing");
      assertNotNull(exception.getRecoverySuggestion());
      assertFalse(exception.getRecoverySuggestion().isEmpty());
    }
  }

  @Nested
  @DisplayName("Recovery Suggestion Tests")
  class RecoverySuggestionTests {

    @ParameterizedTest
    @EnumSource(LinkingErrorType.class)
    @DisplayName("All error types should have recovery suggestions")
    void allErrorTypesShouldHaveRecoverySuggestions(final LinkingErrorType errorType) {
      LinkingException exception = new LinkingException(errorType, "Test");
      assertNotNull(exception.getRecoverySuggestion());
      assertFalse(exception.getRecoverySuggestion().isEmpty());
    }

    @Test
    @DisplayName("IMPORT_NOT_FOUND should suggest providing imports")
    void importNotFoundRecoverySuggestion() {
      LinkingException exception =
          new LinkingException(LinkingErrorType.IMPORT_NOT_FOUND, "Import missing");
      assertTrue(
          exception.getRecoverySuggestion().toLowerCase().contains("import")
              || exception.getRecoverySuggestion().toLowerCase().contains("linker"));
    }

    @Test
    @DisplayName("WASI_IMPORT_FAILED should suggest WASI configuration")
    void wasiImportFailedRecoverySuggestion() {
      LinkingException exception =
          new LinkingException(LinkingErrorType.WASI_IMPORT_FAILED, "WASI error");
      assertTrue(exception.getRecoverySuggestion().toLowerCase().contains("wasi"));
    }
  }

  @Nested
  @DisplayName("Classification Method Tests")
  class ClassificationMethodTests {

    @Test
    @DisplayName("isMissingItemError should return true for missing imports/exports")
    void isMissingItemError() {
      assertTrue(
          new LinkingException(LinkingErrorType.IMPORT_NOT_FOUND, "Test").isMissingItemError());
      assertTrue(
          new LinkingException(LinkingErrorType.EXPORT_NOT_FOUND, "Test").isMissingItemError());

      assertFalse(
          new LinkingException(LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH, "Test")
              .isMissingItemError());
      assertFalse(
          new LinkingException(LinkingErrorType.CIRCULAR_DEPENDENCY, "Test").isMissingItemError());
    }

    @Test
    @DisplayName("isTypeMismatchError should return true for type mismatch errors")
    void isTypeMismatchError() {
      assertTrue(
          new LinkingException(LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH, "Test")
              .isTypeMismatchError());
      assertTrue(
          new LinkingException(LinkingErrorType.MEMORY_SIZE_MISMATCH, "Test")
              .isTypeMismatchError());
      assertTrue(
          new LinkingException(LinkingErrorType.MEMORY_LIMITS_INCOMPATIBLE, "Test")
              .isTypeMismatchError());
      assertTrue(
          new LinkingException(LinkingErrorType.TABLE_SIZE_MISMATCH, "Test").isTypeMismatchError());
      assertTrue(
          new LinkingException(LinkingErrorType.TABLE_TYPE_MISMATCH, "Test").isTypeMismatchError());
      assertTrue(
          new LinkingException(LinkingErrorType.GLOBAL_TYPE_MISMATCH, "Test")
              .isTypeMismatchError());
      assertTrue(
          new LinkingException(LinkingErrorType.GLOBAL_MUTABILITY_MISMATCH, "Test")
              .isTypeMismatchError());
      assertTrue(
          new LinkingException(LinkingErrorType.INTERFACE_TYPE_MISMATCH, "Test")
              .isTypeMismatchError());

      assertFalse(
          new LinkingException(LinkingErrorType.IMPORT_NOT_FOUND, "Test").isTypeMismatchError());
      assertFalse(
          new LinkingException(LinkingErrorType.CIRCULAR_DEPENDENCY, "Test").isTypeMismatchError());
    }

    @Test
    @DisplayName("isHostFunctionError should return true for host function errors")
    void isHostFunctionError() {
      assertTrue(
          new LinkingException(LinkingErrorType.HOST_FUNCTION_BINDING_FAILED, "Test")
              .isHostFunctionError());
      assertTrue(
          new LinkingException(LinkingErrorType.WASI_IMPORT_FAILED, "Test").isHostFunctionError());

      assertFalse(
          new LinkingException(LinkingErrorType.IMPORT_NOT_FOUND, "Test").isHostFunctionError());
      assertFalse(
          new LinkingException(LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH, "Test")
              .isHostFunctionError());
    }

    @Test
    @DisplayName("isComponentError should return true for component-related errors")
    void isComponentError() {
      assertTrue(
          new LinkingException(LinkingErrorType.COMPONENT_LINKING_FAILED, "Test")
              .isComponentError());
      assertTrue(
          new LinkingException(LinkingErrorType.INTERFACE_TYPE_MISMATCH, "Test")
              .isComponentError());
      assertTrue(
          new LinkingException(LinkingErrorType.RESOURCE_TYPE_LINKING_FAILED, "Test")
              .isComponentError());
      assertTrue(
          new LinkingException(LinkingErrorType.CAPABILITY_NOT_SATISFIED, "Test")
              .isComponentError());

      assertFalse(
          new LinkingException(LinkingErrorType.IMPORT_NOT_FOUND, "Test").isComponentError());
      assertFalse(
          new LinkingException(LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH, "Test")
              .isComponentError());
    }

    @Test
    @DisplayName("isConfigurationError should return true for configuration errors")
    void isConfigurationError() {
      assertTrue(
          new LinkingException(LinkingErrorType.CIRCULAR_DEPENDENCY, "Test")
              .isConfigurationError());
      assertTrue(
          new LinkingException(LinkingErrorType.NAMESPACE_CONFLICT, "Test").isConfigurationError());
      assertTrue(
          new LinkingException(LinkingErrorType.LINKER_CONFIGURATION_ERROR, "Test")
              .isConfigurationError());

      assertFalse(
          new LinkingException(LinkingErrorType.IMPORT_NOT_FOUND, "Test").isConfigurationError());
      assertFalse(
          new LinkingException(LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH, "Test")
              .isConfigurationError());
    }
  }

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("LinkingException should extend WasmException")
    void shouldExtendWasmException() {
      LinkingException exception = new LinkingException(LinkingErrorType.UNKNOWN, "Test");
      assertTrue(exception instanceof WasmException);
    }

    @Test
    @DisplayName("LinkingException should be a checked exception")
    void shouldBeCheckedException() {
      LinkingException exception = new LinkingException(LinkingErrorType.UNKNOWN, "Test");
      assertTrue(exception instanceof Exception);
      assertFalse(java.lang.RuntimeException.class.isAssignableFrom(exception.getClass()));
    }
  }

  @Nested
  @DisplayName("Message Formatting Tests")
  class MessageFormattingTests {

    @Test
    @DisplayName("Message should include error type name")
    void messageShouldIncludeErrorTypeName() {
      LinkingException exception =
          new LinkingException(LinkingErrorType.IMPORT_NOT_FOUND, "Missing import");
      assertTrue(exception.getMessage().contains("IMPORT_NOT_FOUND"));
    }

    @Test
    @DisplayName("Message should include module and item name when provided")
    void messageShouldIncludeModuleAndItemName() {
      LinkingException exception =
          new LinkingException(
              LinkingErrorType.IMPORT_NOT_FOUND,
              "Import missing",
              "env",
              "memory",
              null,
              null,
              null);
      assertTrue(exception.getMessage().contains("env"));
      assertTrue(exception.getMessage().contains("memory"));
    }

    @Test
    @DisplayName("Message should include expected and actual types when provided")
    void messageShouldIncludeExpectedAndActualTypes() {
      LinkingException exception =
          new LinkingException(
              LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH,
              "Signature error",
              null,
              null,
              "[i32] -> [i32]",
              "[i64] -> [i64]",
              null);
      assertTrue(exception.getMessage().contains("[i32] -> [i32]"));
      assertTrue(exception.getMessage().contains("[i64] -> [i64]"));
    }

    @Test
    @DisplayName("Message should include only expected type when actual is null")
    void messageShouldIncludeOnlyExpectedType() {
      LinkingException exception =
          new LinkingException(
              LinkingErrorType.IMPORT_NOT_FOUND,
              "Import missing",
              null,
              "my_func",
              "[i32] -> [i32]",
              null,
              null);
      assertTrue(exception.getMessage().contains("[i32] -> [i32]"));
    }

    @Test
    @DisplayName("Message should include only item name when module is null")
    void messageShouldIncludeOnlyItemName() {
      LinkingException exception =
          new LinkingException(
              LinkingErrorType.EXPORT_NOT_FOUND,
              "Export missing",
              null,
              "exported_function",
              null,
              null,
              null);
      assertTrue(exception.getMessage().contains("exported_function"));
    }

    @Test
    @DisplayName("Message should include only module name when item is null")
    void messageShouldIncludeOnlyModuleName() {
      LinkingException exception =
          new LinkingException(
              LinkingErrorType.CIRCULAR_DEPENDENCY,
              "Circular dependency detected",
              "module_a",
              null,
              null,
              null,
              null);
      assertTrue(exception.getMessage().contains("module_a"));
    }
  }
}
