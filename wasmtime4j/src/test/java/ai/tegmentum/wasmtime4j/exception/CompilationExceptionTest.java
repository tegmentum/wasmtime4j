package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link CompilationException} class.
 *
 * <p>Tests verify:
 *
 * <ul>
 *   <li>Constructor behavior with message and cause
 *   <li>Inheritance hierarchy (extends WasmException)
 *   <li>Exception message and cause retrieval
 *   <li>Serialization/deserialization
 *   <li>Stack trace preservation
 * </ul>
 */
@DisplayName("CompilationException Tests")
class CompilationExceptionTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create exception with message only")
    void shouldCreateExceptionWithMessageOnly() {
      String message = "Invalid WebAssembly bytecode";
      CompilationException exception = new CompilationException(message);

      assertEquals(message, exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should create exception with null message")
    void shouldCreateExceptionWithNullMessage() {
      CompilationException exception = new CompilationException(null);

      assertNull(exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should create exception with empty message")
    void shouldCreateExceptionWithEmptyMessage() {
      CompilationException exception = new CompilationException("");

      assertEquals("", exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      String message = "Compilation failed";
      Throwable cause = new IllegalArgumentException("Invalid opcode");
      CompilationException exception = new CompilationException(message, cause);

      assertEquals(message, exception.getMessage());
      assertSame(cause, exception.getCause());
    }

    @Test
    @DisplayName("should create exception with message and null cause")
    void shouldCreateExceptionWithMessageAndNullCause() {
      String message = "Compilation error";
      CompilationException exception = new CompilationException(message, null);

      assertEquals(message, exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should create exception with null message and cause")
    void shouldCreateExceptionWithNullMessageAndCause() {
      Throwable cause = new RuntimeException("Root cause");
      CompilationException exception = new CompilationException(null, cause);

      assertNull(exception.getMessage());
      assertSame(cause, exception.getCause());
    }

    @Test
    @DisplayName("should preserve nested exception chain")
    void shouldPreserveNestedExceptionChain() {
      Throwable root = new IOException("I/O error");
      Throwable middle = new IllegalStateException("Invalid state", root);
      CompilationException exception = new CompilationException("Compilation failed", middle);

      assertSame(middle, exception.getCause());
      assertSame(root, exception.getCause().getCause());
    }
  }

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend WasmException")
    void shouldExtendWasmException() {
      CompilationException exception = new CompilationException("test");

      assertTrue(
          exception instanceof WasmException, "CompilationException should extend WasmException");
    }

    @Test
    @DisplayName("should be assignable to WasmException")
    void shouldBeAssignableToWasmException() {
      assertTrue(
          WasmException.class.isAssignableFrom(CompilationException.class),
          "CompilationException should be assignable to WasmException");
    }

    @Test
    @DisplayName("should be a checked exception")
    void shouldBeCheckedException() {
      assertTrue(
          Exception.class.isAssignableFrom(CompilationException.class),
          "CompilationException should extend Exception");

      // Verify it is NOT an unchecked exception (java.lang.RuntimeException)
      assertFalse(
          java.lang.RuntimeException.class.isAssignableFrom(CompilationException.class),
          "CompilationException should NOT extend java.lang.RuntimeException");
    }

    @Test
    @DisplayName("should be catchable as WasmException")
    void shouldBeCatchableAsWasmException() {
      boolean caught = false;
      try {
        throw new CompilationException("test");
      } catch (WasmException e) {
        caught = true;
        assertTrue(e instanceof CompilationException);
      }
      assertTrue(caught, "Should be catchable as WasmException");
    }

    @Test
    @DisplayName("should be catchable as Exception")
    void shouldBeCatchableAsException() {
      boolean caught = false;
      try {
        throw new CompilationException("test");
      } catch (Exception e) {
        caught = true;
        assertTrue(e instanceof CompilationException);
      }
      assertTrue(caught, "Should be catchable as Exception");
    }
  }

  @Nested
  @DisplayName("Message Formatting Tests")
  class MessageFormattingTests {

    @Test
    @DisplayName("should preserve special characters in message")
    void shouldPreserveSpecialCharactersInMessage() {
      String message = "Error at line 42: unexpected token '<EOF>'";
      CompilationException exception = new CompilationException(message);

      assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("should preserve unicode in message")
    void shouldPreserveUnicodeInMessage() {
      String message = "Compilation error: 无效的操作码";
      CompilationException exception = new CompilationException(message);

      assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("should preserve newlines in message")
    void shouldPreserveNewlinesInMessage() {
      String message = "Multiple errors:\n  - Error 1\n  - Error 2";
      CompilationException exception = new CompilationException(message);

      assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("should handle very long message")
    void shouldHandleVeryLongMessage() {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 10000; i++) {
        sb.append("Error at position ").append(i).append("; ");
      }
      String message = sb.toString();
      CompilationException exception = new CompilationException(message);

      assertEquals(message, exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Stack Trace Tests")
  class StackTraceTests {

    @Test
    @DisplayName("should have non-empty stack trace")
    void shouldHaveNonEmptyStackTrace() {
      CompilationException exception = new CompilationException("test");

      assertNotNull(exception.getStackTrace());
      assertTrue(exception.getStackTrace().length > 0, "Stack trace should not be empty");
    }

    @Test
    @DisplayName("should include test method in stack trace")
    void shouldIncludeTestMethodInStackTrace() {
      CompilationException exception = new CompilationException("test");

      boolean foundTestMethod = false;
      for (StackTraceElement element : exception.getStackTrace()) {
        if (element.getMethodName().equals("shouldIncludeTestMethodInStackTrace")) {
          foundTestMethod = true;
          break;
        }
      }
      assertTrue(foundTestMethod, "Stack trace should include test method");
    }

    @Test
    @DisplayName("should preserve stack trace after modification")
    void shouldPreserveStackTraceAfterModification() {
      CompilationException exception = new CompilationException("test");
      StackTraceElement[] original = exception.getStackTrace().clone();

      exception.setStackTrace(new StackTraceElement[0]);
      assertEquals(0, exception.getStackTrace().length);

      exception.setStackTrace(original);
      assertEquals(original.length, exception.getStackTrace().length);
    }
  }

  @Nested
  @DisplayName("Serialization Tests")
  class SerializationTests {

    @Test
    @DisplayName("should serialize and deserialize with message only")
    void shouldSerializeAndDeserializeWithMessageOnly() throws Exception {
      String message = "Compilation failed";
      CompilationException original = new CompilationException(message);

      CompilationException deserialized = serializeAndDeserialize(original);

      assertEquals(original.getMessage(), deserialized.getMessage());
      assertNull(deserialized.getCause());
    }

    @Test
    @DisplayName("should serialize and deserialize with message and cause")
    void shouldSerializeAndDeserializeWithMessageAndCause() throws Exception {
      String message = "Compilation failed";
      Throwable cause = new IllegalArgumentException("Invalid bytecode");
      CompilationException original = new CompilationException(message, cause);

      CompilationException deserialized = serializeAndDeserialize(original);

      assertEquals(original.getMessage(), deserialized.getMessage());
      assertNotNull(deserialized.getCause());
      assertEquals(cause.getMessage(), deserialized.getCause().getMessage());
    }

    @Test
    @DisplayName("should preserve stack trace through serialization")
    void shouldPreserveStackTraceThroughSerialization() throws Exception {
      CompilationException original = new CompilationException("test");

      CompilationException deserialized = serializeAndDeserialize(original);

      assertEquals(original.getStackTrace().length, deserialized.getStackTrace().length);
      for (int i = 0; i < original.getStackTrace().length; i++) {
        assertEquals(
            original.getStackTrace()[i].toString(), deserialized.getStackTrace()[i].toString());
      }
    }

    private CompilationException serializeAndDeserialize(CompilationException exception)
        throws IOException, ClassNotFoundException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        oos.writeObject(exception);
      }

      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      try (ObjectInputStream ois = new ObjectInputStream(bais)) {
        return (CompilationException) ois.readObject();
      }
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should include class name in toString")
    void shouldIncludeClassNameInToString() {
      CompilationException exception = new CompilationException("test error");

      String str = exception.toString();
      assertTrue(str.contains("CompilationException"), "toString should include class name");
    }

    @Test
    @DisplayName("should include message in toString")
    void shouldIncludeMessageInToString() {
      String message = "Specific compilation error";
      CompilationException exception = new CompilationException(message);

      String str = exception.toString();
      assertTrue(str.contains(message), "toString should include message");
    }
  }

  @Nested
  @DisplayName("Compilation Error Context Tests")
  class CompilationErrorContextTests {

    @Test
    @DisplayName("should handle typical validation error message")
    void shouldHandleTypicalValidationErrorMessage() {
      String message = "type mismatch: expected i32, got f64";
      CompilationException exception = new CompilationException(message);

      assertEquals(message, exception.getMessage());
      assertTrue(exception instanceof WasmException);
    }

    @Test
    @DisplayName("should handle memory configuration error message")
    void shouldHandleMemoryConfigurationErrorMessage() {
      String message = "memory initial size exceeds maximum";
      CompilationException exception = new CompilationException(message);

      assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("should handle unsupported feature error message")
    void shouldHandleUnsupportedFeatureErrorMessage() {
      String message = "unsupported feature: multi-memory proposal not enabled";
      CompilationException exception = new CompilationException(message);

      assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("should handle invalid bytecode error message")
    void shouldHandleInvalidBytecodeErrorMessage() {
      String message = "invalid magic number: expected 0x0061736d";
      CompilationException exception = new CompilationException(message);

      assertEquals(message, exception.getMessage());
    }
  }
}
