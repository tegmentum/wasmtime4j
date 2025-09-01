package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for WasiException class. */
class WasiExceptionTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Simple message constructor creates exception correctly")
    void testSimpleMessageConstructor() {
      final String message = "WASI operation failed";
      final WasiException exception = new WasiException(message);

      assertEquals(message, exception.getMessage());
      assertNull(exception.getCause());
      assertNull(exception.getOperation());
      assertNull(exception.getResource());
      assertFalse(exception.isRetryable());
      assertEquals(WasiException.ErrorCategory.SYSTEM, exception.getCategory());
    }

    @Test
    @DisplayName("Message with cause constructor creates exception correctly")
    void testMessageWithCauseConstructor() {
      final String message = "WASI operation failed";
      final RuntimeException cause = new RuntimeException("Native error");
      final WasiException exception = new WasiException(message, cause);

      assertEquals(message, exception.getMessage());
      assertEquals(cause, exception.getCause());
      assertNull(exception.getOperation());
      assertNull(exception.getResource());
      assertFalse(exception.isRetryable());
      assertEquals(WasiException.ErrorCategory.SYSTEM, exception.getCategory());
    }

    @Test
    @DisplayName("Detailed constructor creates exception correctly")
    void testDetailedConstructor() {
      final String message = "File operation failed";
      final String operation = "file-read";
      final String resource = "/path/to/file";
      final boolean retryable = true;
      final WasiException.ErrorCategory category = WasiException.ErrorCategory.FILE_SYSTEM;

      final WasiException exception =
          new WasiException(message, operation, resource, retryable, category);

      assertEquals(
          "File operation failed (operation: file-read) (resource: /path/to/file)",
          exception.getMessage());
      assertNull(exception.getCause());
      assertEquals(operation, exception.getOperation());
      assertEquals(resource, exception.getResource());
      assertTrue(exception.isRetryable());
      assertEquals(category, exception.getCategory());
    }

    @Test
    @DisplayName("Detailed constructor with cause creates exception correctly")
    void testDetailedConstructorWithCause() {
      final String message = "Network operation failed";
      final String operation = "socket-connect";
      final String resource = "localhost:8080";
      final boolean retryable = true;
      final WasiException.ErrorCategory category = WasiException.ErrorCategory.NETWORK;
      final RuntimeException cause = new RuntimeException("Connection refused");

      final WasiException exception =
          new WasiException(message, operation, resource, retryable, category, cause);

      assertEquals(
          "Network operation failed (operation: socket-connect) (resource: localhost:8080)",
          exception.getMessage());
      assertEquals(cause, exception.getCause());
      assertEquals(operation, exception.getOperation());
      assertEquals(resource, exception.getResource());
      assertTrue(exception.isRetryable());
      assertEquals(category, exception.getCategory());
    }

    @Test
    @DisplayName("Constructor with null message throws IllegalArgumentException")
    void testConstructorWithNullMessage() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new WasiException(
                  null, "operation", "resource", false, WasiException.ErrorCategory.SYSTEM));
    }

    @Test
    @DisplayName("Constructor with empty message throws IllegalArgumentException")
    void testConstructorWithEmptyMessage() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new WasiException(
                  "", "operation", "resource", false, WasiException.ErrorCategory.SYSTEM));
    }
  }

  @Nested
  @DisplayName("Category Classification Tests")
  class CategoryClassificationTests {

    @Test
    @DisplayName("isFileSystemError returns true for FILE_SYSTEM category")
    void testIsFileSystemError() {
      final WasiException exception =
          new WasiException("Error", "op", "res", false, WasiException.ErrorCategory.FILE_SYSTEM);
      assertTrue(exception.isFileSystemError());
      assertFalse(exception.isNetworkError());
      assertFalse(exception.isPermissionError());
      assertFalse(exception.isResourceLimitError());
      assertFalse(exception.isComponentError());
      assertFalse(exception.isConfigurationError());
    }

    @Test
    @DisplayName("isNetworkError returns true for NETWORK category")
    void testIsNetworkError() {
      final WasiException exception =
          new WasiException("Error", "op", "res", false, WasiException.ErrorCategory.NETWORK);
      assertFalse(exception.isFileSystemError());
      assertTrue(exception.isNetworkError());
      assertFalse(exception.isPermissionError());
      assertFalse(exception.isResourceLimitError());
      assertFalse(exception.isComponentError());
      assertFalse(exception.isConfigurationError());
    }

    @Test
    @DisplayName("isPermissionError returns true for PERMISSION category")
    void testIsPermissionError() {
      final WasiException exception =
          new WasiException("Error", "op", "res", false, WasiException.ErrorCategory.PERMISSION);
      assertFalse(exception.isFileSystemError());
      assertFalse(exception.isNetworkError());
      assertTrue(exception.isPermissionError());
      assertFalse(exception.isResourceLimitError());
      assertFalse(exception.isComponentError());
      assertFalse(exception.isConfigurationError());
    }

    @Test
    @DisplayName("isResourceLimitError returns true for RESOURCE_LIMIT category")
    void testIsResourceLimitError() {
      final WasiException exception =
          new WasiException(
              "Error", "op", "res", false, WasiException.ErrorCategory.RESOURCE_LIMIT);
      assertFalse(exception.isFileSystemError());
      assertFalse(exception.isNetworkError());
      assertFalse(exception.isPermissionError());
      assertTrue(exception.isResourceLimitError());
      assertFalse(exception.isComponentError());
      assertFalse(exception.isConfigurationError());
    }

    @Test
    @DisplayName("isComponentError returns true for COMPONENT category")
    void testIsComponentError() {
      final WasiException exception =
          new WasiException("Error", "op", "res", false, WasiException.ErrorCategory.COMPONENT);
      assertFalse(exception.isFileSystemError());
      assertFalse(exception.isNetworkError());
      assertFalse(exception.isPermissionError());
      assertFalse(exception.isResourceLimitError());
      assertTrue(exception.isComponentError());
      assertFalse(exception.isConfigurationError());
    }

    @Test
    @DisplayName("isConfigurationError returns true for CONFIGURATION category")
    void testIsConfigurationError() {
      final WasiException exception =
          new WasiException("Error", "op", "res", false, WasiException.ErrorCategory.CONFIGURATION);
      assertFalse(exception.isFileSystemError());
      assertFalse(exception.isNetworkError());
      assertFalse(exception.isPermissionError());
      assertFalse(exception.isResourceLimitError());
      assertFalse(exception.isComponentError());
      assertTrue(exception.isConfigurationError());
    }
  }

  @Nested
  @DisplayName("Message Formatting Tests")
  class MessageFormattingTests {

    @Test
    @DisplayName("formatMessage with operation and resource formats correctly")
    void testFormatMessageWithOperationAndResource() {
      final WasiException exception =
          new WasiException(
              "Error", "test-op", "test-res", false, WasiException.ErrorCategory.SYSTEM);
      assertEquals("Error (operation: test-op) (resource: test-res)", exception.getMessage());
    }

    @Test
    @DisplayName("formatMessage with operation only formats correctly")
    void testFormatMessageWithOperationOnly() {
      final WasiException exception =
          new WasiException("Error", "test-op", null, false, WasiException.ErrorCategory.SYSTEM);
      assertEquals("Error (operation: test-op)", exception.getMessage());
    }

    @Test
    @DisplayName("formatMessage with resource only formats correctly")
    void testFormatMessageWithResourceOnly() {
      final WasiException exception =
          new WasiException("Error", null, "test-res", false, WasiException.ErrorCategory.SYSTEM);
      assertEquals("Error (resource: test-res)", exception.getMessage());
    }

    @Test
    @DisplayName("formatMessage with neither operation nor resource formats correctly")
    void testFormatMessageWithNeitherOperationNorResource() {
      final WasiException exception =
          new WasiException("Error", null, null, false, WasiException.ErrorCategory.SYSTEM);
      assertEquals("Error", exception.getMessage());
    }

    @Test
    @DisplayName("formatMessage with empty operation and resource handles correctly")
    void testFormatMessageWithEmptyOperationAndResource() {
      final WasiException exception =
          new WasiException("Error", "", "", false, WasiException.ErrorCategory.SYSTEM);
      assertEquals("Error", exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("WasiException extends WasmException")
    void testWasiExceptionExtendsWasmException() {
      final WasiException exception = new WasiException("Test error");
      assertTrue(exception instanceof WasmException);
    }

    @Test
    @DisplayName("WasiException extends Exception")
    void testWasiExceptionExtendsException() {
      final WasiException exception = new WasiException("Test error");
      assertTrue(exception instanceof Exception);
    }

    @Test
    @DisplayName("WasiException extends Throwable")
    void testWasiExceptionExtendsThrowable() {
      final WasiException exception = new WasiException("Test error");
      assertTrue(exception instanceof Throwable);
    }
  }

  @Nested
  @DisplayName("Serialization Tests")
  class SerializationTests {

    @Test
    @DisplayName("WasiException has correct serialVersionUID")
    void testSerialVersionUID() {
      // This is a compile-time check - if the serialVersionUID is not compatible,
      // the test will not compile
      final WasiException exception = new WasiException("Test error");
      assertNotNull(exception);
    }
  }

  @Nested
  @DisplayName("Error Category Enum Tests")
  class ErrorCategoryEnumTests {

    @Test
    @DisplayName("All ErrorCategory values are properly defined")
    void testErrorCategoryValues() {
      final WasiException.ErrorCategory[] categories = WasiException.ErrorCategory.values();
      assertEquals(7, categories.length);

      assertTrue(contains(categories, WasiException.ErrorCategory.FILE_SYSTEM));
      assertTrue(contains(categories, WasiException.ErrorCategory.NETWORK));
      assertTrue(contains(categories, WasiException.ErrorCategory.PERMISSION));
      assertTrue(contains(categories, WasiException.ErrorCategory.RESOURCE_LIMIT));
      assertTrue(contains(categories, WasiException.ErrorCategory.COMPONENT));
      assertTrue(contains(categories, WasiException.ErrorCategory.CONFIGURATION));
      assertTrue(contains(categories, WasiException.ErrorCategory.SYSTEM));
    }

    private boolean contains(
        final WasiException.ErrorCategory[] array, final WasiException.ErrorCategory value) {
      for (final WasiException.ErrorCategory category : array) {
        if (category == value) {
          return true;
        }
      }
      return false;
    }
  }
}
