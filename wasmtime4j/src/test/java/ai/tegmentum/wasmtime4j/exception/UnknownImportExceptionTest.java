package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.LinkingException.LinkingErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link UnknownImportException} class.
 *
 * <p>UnknownImportException indicates a module's import cannot be resolved during instantiation.
 */
@DisplayName("UnknownImportException Tests")
class UnknownImportExceptionTest {

  @Test
  @DisplayName("should create with message and set IMPORT_NOT_FOUND error type")
  void shouldCreateWithMessageAndImportNotFoundType() {
    final UnknownImportException exception =
        new UnknownImportException("import 'env.memory' not found");
    assertTrue(
        exception.getMessage().contains("import 'env.memory' not found"),
        "Message should contain original text: " + exception.getMessage());
    assertEquals(LinkingErrorType.IMPORT_NOT_FOUND, exception.getErrorType());
  }

  @Test
  @DisplayName("should create with message and cause")
  void shouldCreateWithMessageAndCause() {
    final RuntimeException cause = new RuntimeException("resolution failed");
    final UnknownImportException exception = new UnknownImportException("missing import", cause);
    assertTrue(
        exception.getMessage().contains("missing import"),
        "Message should contain original text: " + exception.getMessage());
    assertEquals(cause, exception.getCause());
    assertEquals(LinkingErrorType.IMPORT_NOT_FOUND, exception.getErrorType());
  }

  @Test
  @DisplayName("should extend LinkingException")
  void shouldExtendLinkingException() {
    final UnknownImportException exception = new UnknownImportException("test");
    assertTrue(exception instanceof LinkingException, "Should be a LinkingException");
    assertTrue(exception instanceof WasmException, "Should also be a WasmException");
  }

  @Test
  @DisplayName("should be classified as missing item error")
  void shouldBeClassifiedAsMissingItemError() {
    final UnknownImportException exception = new UnknownImportException("test");
    assertTrue(exception.isMissingItemError(), "Should be classified as missing item error");
  }

  @Test
  @DisplayName("should have recovery suggestion")
  void shouldHaveRecoverySuggestion() {
    final UnknownImportException exception = new UnknownImportException("test import");
    assertNotNull(exception.getRecoverySuggestion(), "Should provide a recovery suggestion");
    assertTrue(
        exception.getRecoverySuggestion().contains("import"),
        "Recovery suggestion should mention imports: " + exception.getRecoverySuggestion());
  }
}
