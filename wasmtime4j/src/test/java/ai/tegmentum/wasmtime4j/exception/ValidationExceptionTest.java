package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ValidationException} class.
 *
 * <p>This test class verifies validation exception construction and inheritance.
 */
@DisplayName("ValidationException Tests")
class ValidationExceptionTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with message only should set message")
    void constructorWithMessageOnly() {
      String message = "Invalid module: type mismatch at instruction 42";
      ValidationException exception = new ValidationException(message);

      assertEquals(message, exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Constructor with null message should accept null")
    void constructorWithNullMessage() {
      ValidationException exception = new ValidationException((String) null);

      assertNull(exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Constructor with empty message should accept empty string")
    void constructorWithEmptyMessage() {
      ValidationException exception = new ValidationException("");

      assertEquals("", exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Constructor with message and cause should set both")
    void constructorWithMessageAndCause() {
      String message = "Invalid module bytecode";
      Throwable cause = new RuntimeException("Parsing error");
      ValidationException exception = new ValidationException(message, cause);

      assertEquals(message, exception.getMessage());
      assertSame(cause, exception.getCause());
    }

    @Test
    @DisplayName("Constructor with null message and valid cause should accept both")
    void constructorWithNullMessageAndValidCause() {
      Throwable cause = new RuntimeException("Root cause");
      ValidationException exception = new ValidationException(null, cause);

      assertNull(exception.getMessage());
      assertSame(cause, exception.getCause());
    }

    @Test
    @DisplayName("Constructor with valid message and null cause should accept both")
    void constructorWithValidMessageAndNullCause() {
      String message = "Validation failed";
      ValidationException exception = new ValidationException(message, null);

      assertEquals(message, exception.getMessage());
      assertNull(exception.getCause());
    }
  }

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("ValidationException should extend WasmException")
    void shouldExtendWasmException() {
      ValidationException exception = new ValidationException("Test");
      assertTrue(exception instanceof WasmException);
    }

    @Test
    @DisplayName("ValidationException should be a checked exception")
    void shouldBeCheckedException() {
      ValidationException exception = new ValidationException("Test");
      assertTrue(exception instanceof Exception);
      assertFalse(java.lang.RuntimeException.class.isAssignableFrom(exception.getClass()));
    }
  }

  @Nested
  @DisplayName("Validation Error Message Tests")
  class ValidationErrorMessageTests {

    @Test
    @DisplayName("Should preserve type mismatch error message")
    void shouldPreserveTypeMismatchErrorMessage() {
      String message = "type mismatch: expected i32 but found f64 at instruction offset 0x1234";
      ValidationException exception = new ValidationException(message);

      assertEquals(message, exception.getMessage());
      assertTrue(exception.getMessage().contains("type mismatch"));
      assertTrue(exception.getMessage().contains("i32"));
      assertTrue(exception.getMessage().contains("f64"));
    }

    @Test
    @DisplayName("Should preserve invalid opcode error message")
    void shouldPreserveInvalidOpcodeErrorMessage() {
      String message = "unknown opcode 0xfc23 at offset 256";
      ValidationException exception = new ValidationException(message);

      assertEquals(message, exception.getMessage());
      assertTrue(exception.getMessage().contains("opcode"));
    }

    @Test
    @DisplayName("Should preserve function signature error message")
    void shouldPreserveFunctionSignatureErrorMessage() {
      String message =
          "function signature mismatch: expected [i32, i32] -> [i32] but found [i64] -> [i32]";
      ValidationException exception = new ValidationException(message);

      assertEquals(message, exception.getMessage());
      assertTrue(exception.getMessage().contains("signature"));
    }

    @Test
    @DisplayName("Should preserve memory limit error message")
    void shouldPreserveMemoryLimitErrorMessage() {
      String message = "memory minimum 256 pages exceeds maximum of 128 pages";
      ValidationException exception = new ValidationException(message);

      assertEquals(message, exception.getMessage());
      assertTrue(exception.getMessage().contains("memory"));
      assertTrue(exception.getMessage().contains("pages"));
    }

    @Test
    @DisplayName("Should preserve import/export error message")
    void shouldPreserveImportExportErrorMessage() {
      String message = "import \"env\"::\"memory\" has incompatible type";
      ValidationException exception = new ValidationException(message);

      assertEquals(message, exception.getMessage());
      assertTrue(exception.getMessage().contains("import"));
    }

    @Test
    @DisplayName("Should preserve stack underflow error message")
    void shouldPreserveStackUnderflowErrorMessage() {
      String message = "stack underflow: expected 2 values but stack has 1";
      ValidationException exception = new ValidationException(message);

      assertEquals(message, exception.getMessage());
      assertTrue(exception.getMessage().contains("stack"));
    }

    @Test
    @DisplayName("Should preserve multiline error messages")
    void shouldPreserveMultilineErrorMessages() {
      String message =
          "Multiple validation errors:\n"
              + "- type mismatch at offset 0x10\n"
              + "- unknown local 5 at offset 0x20\n"
              + "- stack underflow at offset 0x30";
      ValidationException exception = new ValidationException(message);

      assertEquals(message, exception.getMessage());
      assertTrue(exception.getMessage().contains("\n"));
    }
  }

  @Nested
  @DisplayName("Exception Chaining Tests")
  class ExceptionChainingTests {

    @Test
    @DisplayName("Should support deep exception chaining")
    void shouldSupportDeepExceptionChaining() {
      Exception rootCause = new Exception("Binary parsing failed");
      Exception middleCause = new Exception("Section decode error", rootCause);
      ValidationException topException =
          new ValidationException("Module validation failed", middleCause);

      assertSame(middleCause, topException.getCause());
      assertSame(rootCause, topException.getCause().getCause());
    }

    @Test
    @DisplayName("Should support chaining with WasmException")
    void shouldSupportChainingWithWasmException() {
      WasmException wasmCause = new WasmException("Underlying WASM error");
      ValidationException exception = new ValidationException("Validation failed", wasmCause);

      assertSame(wasmCause, exception.getCause());
      assertTrue(exception.getCause() instanceof WasmException);
    }
  }

  @Nested
  @DisplayName("Stack Trace Tests")
  class StackTraceTests {

    @Test
    @DisplayName("Should have stack trace")
    void shouldHaveStackTrace() {
      ValidationException exception = new ValidationException("Test");

      StackTraceElement[] stackTrace = exception.getStackTrace();
      assertTrue(stackTrace != null && stackTrace.length > 0);
    }
  }
}
