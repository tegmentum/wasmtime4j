package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.ResourceTableException.ErrorKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ResourceTableException} class.
 *
 * <p>ResourceTableException indicates a component model resource table operation failure.
 */
@DisplayName("ResourceTableException Tests")
class ResourceTableExceptionTest {

  @Nested
  @DisplayName("ErrorKind Tests")
  class ErrorKindTests {

    @Test
    @DisplayName("should have all expected error kinds")
    void shouldHaveAllExpectedErrorKinds() {
      assertEquals(5, ErrorKind.values().length, "Should have exactly 5 error kinds");
      assertNotNull(ErrorKind.FULL);
      assertNotNull(ErrorKind.NOT_PRESENT);
      assertNotNull(ErrorKind.WRONG_TYPE);
      assertNotNull(ErrorKind.HAS_CHILDREN);
      assertNotNull(ErrorKind.HAS_PARENT);
    }

    @Test
    @DisplayName("each error kind should have a non-empty description")
    void eachErrorKindShouldHaveDescription() {
      for (final ErrorKind kind : ErrorKind.values()) {
        assertNotNull(kind.getDescription(), kind.name() + " should have a description");
        assertTrue(
            !kind.getDescription().isEmpty(), kind.name() + " description should not be empty");
      }
    }

    @Test
    @DisplayName("FULL should describe table full condition")
    void fullShouldDescribeTableFull() {
      assertTrue(
          ErrorKind.FULL.getDescription().toLowerCase().contains("full"),
          "FULL description should mention 'full': " + ErrorKind.FULL.getDescription());
    }

    @Test
    @DisplayName("NOT_PRESENT should describe missing resource")
    void notPresentShouldDescribeMissingResource() {
      assertTrue(
          ErrorKind.NOT_PRESENT.getDescription().toLowerCase().contains("not present"),
          "NOT_PRESENT description: " + ErrorKind.NOT_PRESENT.getDescription());
    }
  }

  @Nested
  @DisplayName("Construction Tests")
  class ConstructionTests {

    @Test
    @DisplayName("should create with error kind and message")
    void shouldCreateWithErrorKindAndMessage() {
      final ResourceTableException exception =
          new ResourceTableException(ErrorKind.FULL, "table capacity 128 exceeded");
      assertEquals(ErrorKind.FULL, exception.getErrorKind());
      assertEquals("table capacity 128 exceeded", exception.getMessage());
    }

    @Test
    @DisplayName("should create with error kind, message, and cause")
    void shouldCreateWithErrorKindMessageAndCause() {
      final RuntimeException cause = new RuntimeException("underlying error");
      final ResourceTableException exception =
          new ResourceTableException(ErrorKind.NOT_PRESENT, "handle 42 not found", cause);
      assertEquals(ErrorKind.NOT_PRESENT, exception.getErrorKind());
      assertEquals("handle 42 not found", exception.getMessage());
      assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("each error kind should work in constructor")
    void eachErrorKindShouldWorkInConstructor() {
      for (final ErrorKind kind : ErrorKind.values()) {
        final ResourceTableException exception =
            new ResourceTableException(kind, "test " + kind.name());
        assertEquals(kind, exception.getErrorKind());
        assertTrue(
            exception.getMessage().contains(kind.name()), "Message should contain kind name");
      }
    }
  }

  @Nested
  @DisplayName("Hierarchy Tests")
  class HierarchyTests {

    @Test
    @DisplayName("should extend ResourceException")
    void shouldExtendResourceException() {
      final ResourceTableException exception =
          new ResourceTableException(ErrorKind.WRONG_TYPE, "test");
      assertTrue(exception instanceof ResourceException, "Should be a ResourceException");
      assertTrue(exception instanceof WasmException, "Should also be a WasmException");
    }
  }
}
