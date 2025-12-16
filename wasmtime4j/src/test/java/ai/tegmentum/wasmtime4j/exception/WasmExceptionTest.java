package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasmException} base exception class.
 *
 * <p>This test class verifies the construction and behavior of the base WebAssembly exception
 * class.
 */
@DisplayName("WasmException Tests")
class WasmExceptionTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with message only should set message")
    void constructorWithMessageOnly() {
      String message = "Test error message";
      WasmException exception = new WasmException(message);

      assertEquals(message, exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Constructor with null message should accept null")
    void constructorWithNullMessage() {
      WasmException exception = new WasmException((String) null);

      assertNull(exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Constructor with empty message should accept empty string")
    void constructorWithEmptyMessage() {
      WasmException exception = new WasmException("");

      assertEquals("", exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Constructor with message and cause should set both")
    void constructorWithMessageAndCause() {
      String message = "Test error message";
      Throwable cause = new RuntimeException("Root cause");
      WasmException exception = new WasmException(message, cause);

      assertEquals(message, exception.getMessage());
      assertSame(cause, exception.getCause());
    }

    @Test
    @DisplayName("Constructor with null message and valid cause should accept both")
    void constructorWithNullMessageAndValidCause() {
      Throwable cause = new RuntimeException("Root cause");
      WasmException exception = new WasmException(null, cause);

      assertNull(exception.getMessage());
      assertSame(cause, exception.getCause());
    }

    @Test
    @DisplayName("Constructor with valid message and null cause should accept both")
    void constructorWithValidMessageAndNullCause() {
      String message = "Test error message";
      WasmException exception = new WasmException(message, null);

      assertEquals(message, exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Constructor with cause only should set cause")
    void constructorWithCauseOnly() {
      Throwable cause = new RuntimeException("Root cause");
      WasmException exception = new WasmException(cause);

      assertNotNull(exception.getMessage());
      assertTrue(exception.getMessage().contains("Root cause"));
      assertSame(cause, exception.getCause());
    }

    @Test
    @DisplayName("Constructor with null cause only should accept null")
    void constructorWithNullCauseOnly() {
      WasmException exception = new WasmException((Throwable) null);

      assertNull(exception.getMessage());
      assertNull(exception.getCause());
    }
  }

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("WasmException should be a checked exception")
    void shouldBeCheckedException() {
      WasmException exception = new WasmException("Test");

      assertTrue(exception instanceof Exception);
      assertFalse(java.lang.RuntimeException.class.isAssignableFrom(exception.getClass()));
    }

    @Test
    @DisplayName("WasmException should be throwable")
    void shouldBeThrowable() {
      WasmException exception = new WasmException("Test");

      assertTrue(exception instanceof Throwable);
    }
  }

  @Nested
  @DisplayName("Exception Chaining Tests")
  class ExceptionChainingTests {

    @Test
    @DisplayName("Should support deep exception chaining")
    void shouldSupportDeepExceptionChaining() {
      Exception rootCause = new Exception("Root");
      Exception middleCause = new Exception("Middle", rootCause);
      WasmException topException = new WasmException("Top", middleCause);

      assertSame(middleCause, topException.getCause());
      assertSame(rootCause, topException.getCause().getCause());
    }

    @Test
    @DisplayName("Should support chaining with different exception types")
    void shouldSupportChainingWithDifferentExceptionTypes() {
      IllegalArgumentException argException = new IllegalArgumentException("Bad arg");
      NullPointerException npeException = new NullPointerException("Null value");
      npeException.initCause(argException);
      WasmException wasmException = new WasmException("Wasm error", npeException);

      assertEquals("Wasm error", wasmException.getMessage());
      assertSame(npeException, wasmException.getCause());
      assertSame(argException, wasmException.getCause().getCause());
    }
  }

  @Nested
  @DisplayName("Stack Trace Tests")
  class StackTraceTests {

    @Test
    @DisplayName("Should have stack trace")
    void shouldHaveStackTrace() {
      WasmException exception = new WasmException("Test");

      StackTraceElement[] stackTrace = exception.getStackTrace();
      assertNotNull(stackTrace);
      assertTrue(stackTrace.length > 0);
    }

    @Test
    @DisplayName("Stack trace should include test method")
    void stackTraceShouldIncludeTestMethod() {
      WasmException exception = new WasmException("Test");

      StackTraceElement[] stackTrace = exception.getStackTrace();
      boolean foundTestMethod = false;
      for (StackTraceElement element : stackTrace) {
        if (element.getMethodName().contains("stackTraceShouldIncludeTestMethod")) {
          foundTestMethod = true;
          break;
        }
      }
      assertTrue(foundTestMethod, "Stack trace should include the test method");
    }
  }

  @Nested
  @DisplayName("Serialization Tests")
  class SerializationTests {

    @Test
    @DisplayName("Should have serialVersionUID")
    void shouldHaveSerialVersionUID() {
      // WasmException implements Serializable via Exception
      WasmException exception = new WasmException("Test");
      assertTrue(exception instanceof java.io.Serializable);
    }
  }

  @Nested
  @DisplayName("Message Formatting Tests")
  class MessageFormattingTests {

    @Test
    @DisplayName("Should preserve special characters in message")
    void shouldPreserveSpecialCharactersInMessage() {
      String message = "Error at offset 0x1234: invalid opcode\n\tat function $foo";
      WasmException exception = new WasmException(message);

      assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("Should preserve unicode characters in message")
    void shouldPreserveUnicodeCharactersInMessage() {
      String message = "Error: 日本語テスト 🔥";
      WasmException exception = new WasmException(message);

      assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("Should preserve very long messages")
    void shouldPreserveVeryLongMessages() {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 10000; i++) {
        sb.append("x");
      }
      String longMessage = sb.toString();
      WasmException exception = new WasmException(longMessage);

      assertEquals(longMessage, exception.getMessage());
      assertEquals(10000, exception.getMessage().length());
    }
  }
}
