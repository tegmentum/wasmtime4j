package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.LinkingException.LinkingErrorType;
import ai.tegmentum.wasmtime4j.exception.TrapException.TrapType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link InstantiationException} class.
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
@DisplayName("InstantiationException Tests")
class InstantiationExceptionTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create exception with message only")
    void shouldCreateExceptionWithMessageOnly() {
      String message = "Failed to instantiate module";
      InstantiationException exception = new InstantiationException(message);

      assertEquals(message, exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should create exception with null message")
    void shouldCreateExceptionWithNullMessage() {
      InstantiationException exception = new InstantiationException(null);

      assertNull(exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should create exception with empty message")
    void shouldCreateExceptionWithEmptyMessage() {
      InstantiationException exception = new InstantiationException("");

      assertEquals("", exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      String message = "Instantiation failed";
      Throwable cause = new IllegalArgumentException("Missing import");
      InstantiationException exception = new InstantiationException(message, cause);

      assertEquals(message, exception.getMessage());
      assertSame(cause, exception.getCause());
    }

    @Test
    @DisplayName("should create exception with message and null cause")
    void shouldCreateExceptionWithMessageAndNullCause() {
      String message = "Instantiation error";
      InstantiationException exception = new InstantiationException(message, null);

      assertEquals(message, exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should create exception with null message and cause")
    void shouldCreateExceptionWithNullMessageAndCause() {
      Throwable cause = new RuntimeException("Root cause");
      InstantiationException exception = new InstantiationException(null, cause);

      assertNull(exception.getMessage());
      assertSame(cause, exception.getCause());
    }

    @Test
    @DisplayName("should preserve nested exception chain")
    void shouldPreserveNestedExceptionChain() {
      Throwable root = new IOException("I/O error");
      Throwable middle = new IllegalStateException("Invalid state", root);
      InstantiationException exception = new InstantiationException("Instantiation failed", middle);

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
      InstantiationException exception = new InstantiationException("test");

      assertTrue(
          exception instanceof WasmException, "InstantiationException should extend WasmException");
    }

    @Test
    @DisplayName("should be assignable to WasmException")
    void shouldBeAssignableToWasmException() {
      assertTrue(
          WasmException.class.isAssignableFrom(InstantiationException.class),
          "InstantiationException should be assignable to WasmException");
    }

    @Test
    @DisplayName("should be a checked exception")
    void shouldBeCheckedException() {
      assertTrue(
          Exception.class.isAssignableFrom(InstantiationException.class),
          "InstantiationException should extend Exception");

      // Verify it is NOT an unchecked exception (java.lang.RuntimeException)
      assertFalse(
          java.lang.RuntimeException.class.isAssignableFrom(InstantiationException.class),
          "InstantiationException should NOT extend java.lang.RuntimeException");
    }

    @Test
    @DisplayName("should be catchable as WasmException")
    void shouldBeCatchableAsWasmException() {
      boolean caught = false;
      try {
        throw new InstantiationException("test");
      } catch (WasmException e) {
        caught = true;
        assertTrue(e instanceof InstantiationException);
      }
      assertTrue(caught, "Should be catchable as WasmException");
    }

    @Test
    @DisplayName("should be catchable as Exception")
    void shouldBeCatchableAsException() {
      boolean caught = false;
      try {
        throw new InstantiationException("test");
      } catch (Exception e) {
        caught = true;
        assertTrue(e instanceof InstantiationException);
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
      String message = "Import not found: module='env', name='memory'";
      InstantiationException exception = new InstantiationException(message);

      assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("should preserve unicode in message")
    void shouldPreserveUnicodeInMessage() {
      String message = "Instantiation error: 缺少导入";
      InstantiationException exception = new InstantiationException(message);

      assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("should preserve newlines in message")
    void shouldPreserveNewlinesInMessage() {
      String message = "Missing imports:\n  - env::memory\n  - env::table";
      InstantiationException exception = new InstantiationException(message);

      assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("should handle very long message")
    void shouldHandleVeryLongMessage() {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 10000; i++) {
        sb.append("Missing import ").append(i).append("; ");
      }
      String message = sb.toString();
      InstantiationException exception = new InstantiationException(message);

      assertEquals(message, exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Stack Trace Tests")
  class StackTraceTests {

    @Test
    @DisplayName("should have non-empty stack trace")
    void shouldHaveNonEmptyStackTrace() {
      InstantiationException exception = new InstantiationException("test");

      assertNotNull(exception.getStackTrace());
      assertTrue(exception.getStackTrace().length > 0, "Stack trace should not be empty");
    }

    @Test
    @DisplayName("should include test method in stack trace")
    void shouldIncludeTestMethodInStackTrace() {
      InstantiationException exception = new InstantiationException("test");

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
      InstantiationException exception = new InstantiationException("test");
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
      String message = "Instantiation failed";
      InstantiationException original = new InstantiationException(message);

      InstantiationException deserialized = serializeAndDeserialize(original);

      assertEquals(original.getMessage(), deserialized.getMessage());
      assertNull(deserialized.getCause());
    }

    @Test
    @DisplayName("should serialize and deserialize with message and cause")
    void shouldSerializeAndDeserializeWithMessageAndCause() throws Exception {
      String message = "Instantiation failed";
      Throwable cause = new IllegalArgumentException("Invalid import");
      InstantiationException original = new InstantiationException(message, cause);

      InstantiationException deserialized = serializeAndDeserialize(original);

      assertEquals(original.getMessage(), deserialized.getMessage());
      assertNotNull(deserialized.getCause());
      assertEquals(cause.getMessage(), deserialized.getCause().getMessage());
    }

    @Test
    @DisplayName("should preserve stack trace through serialization")
    void shouldPreserveStackTraceThroughSerialization() throws Exception {
      InstantiationException original = new InstantiationException("test");

      InstantiationException deserialized = serializeAndDeserialize(original);

      assertEquals(original.getStackTrace().length, deserialized.getStackTrace().length);
      for (int i = 0; i < original.getStackTrace().length; i++) {
        assertEquals(
            original.getStackTrace()[i].toString(), deserialized.getStackTrace()[i].toString());
      }
    }

    private InstantiationException serializeAndDeserialize(InstantiationException exception)
        throws IOException, ClassNotFoundException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        oos.writeObject(exception);
      }

      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      try (ObjectInputStream ois = new ObjectInputStream(bais)) {
        return (InstantiationException) ois.readObject();
      }
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should include class name in toString")
    void shouldIncludeClassNameInToString() {
      InstantiationException exception = new InstantiationException("test error");

      String str = exception.toString();
      assertTrue(str.contains("InstantiationException"), "toString should include class name");
    }

    @Test
    @DisplayName("should include message in toString")
    void shouldIncludeMessageInToString() {
      String message = "Specific instantiation error";
      InstantiationException exception = new InstantiationException(message);

      String str = exception.toString();
      assertTrue(str.contains(message), "toString should include message");
    }
  }

  @Nested
  @DisplayName("Instantiation Error Context Tests")
  class InstantiationErrorContextTests {

    @Test
    @DisplayName("should handle typical missing import error message")
    void shouldHandleTypicalMissingImportErrorMessage() {
      String message = "unknown import: `env::memory` has not been defined";
      InstantiationException exception = new InstantiationException(message);

      assertEquals(message, exception.getMessage());
      assertTrue(exception instanceof WasmException);
    }

    @Test
    @DisplayName("should handle type mismatch error message")
    void shouldHandleTypeMismatchErrorMessage() {
      String message = "incompatible import type for `env::table`";
      InstantiationException exception = new InstantiationException(message);

      assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("should handle memory limits error message")
    void shouldHandleMemoryLimitsErrorMessage() {
      String message = "memory minimum size mismatch: expected 1, got 2 pages";
      InstantiationException exception = new InstantiationException(message);

      assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("should handle global mutability error message")
    void shouldHandleGlobalMutabilityErrorMessage() {
      String message = "global mutability mismatch: expected mutable, got immutable";
      InstantiationException exception = new InstantiationException(message);

      assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("should handle start function trap error")
    void shouldHandleStartFunctionTrapError() {
      String message = "start function trapped: unreachable executed";
      TrapException trapCause =
          new TrapException(TrapType.UNREACHABLE_CODE_REACHED, "unreachable executed");
      InstantiationException exception = new InstantiationException(message, trapCause);

      assertEquals(message, exception.getMessage());
      assertSame(trapCause, exception.getCause());
    }

    @Test
    @DisplayName("should handle linking failure as cause")
    void shouldHandleLinkingFailureAsCause() {
      String message = "Instantiation failed due to linking error";
      LinkingException linkCause =
          new LinkingException(LinkingErrorType.IMPORT_NOT_FOUND, "Missing import");
      InstantiationException exception = new InstantiationException(message, linkCause);

      assertEquals(message, exception.getMessage());
      assertSame(linkCause, exception.getCause());
    }
  }

  @Nested
  @DisplayName("Name Shadow Warning Tests")
  class NameShadowWarningTests {

    @Test
    @DisplayName("should not shadow java.lang.InstantiationException")
    void shouldNotShadowJavaLangInstantiationException() {
      // Verify our InstantiationException is different from java.lang.InstantiationException
      InstantiationException wasmException = new InstantiationException("test");

      // java.lang.InstantiationException is a checked exception extending Exception
      // Our InstantiationException extends WasmException which extends Exception

      // Verify our class is NOT assignable to java.lang.InstantiationException
      // (they are completely unrelated types in the hierarchy)
      assertFalse(
          java.lang.InstantiationException.class.isAssignableFrom(wasmException.getClass()),
          "Wasm InstantiationException should not be assignable to"
              + " java.lang.InstantiationException");

      // Verify different package
      assertFalse(
          wasmException.getClass().getName().equals("java.lang.InstantiationException"),
          "Should be in different package");

      assertEquals(
          "ai.tegmentum.wasmtime4j.exception.InstantiationException",
          wasmException.getClass().getName());
    }

    @Test
    @DisplayName("should be distinguishable when both are caught")
    void shouldBeDistinguishableWhenBothAreCaught() {
      // Create both exception types
      InstantiationException wasmException = new InstantiationException("wasm error");
      java.lang.InstantiationException javaException =
          new java.lang.InstantiationException("java error");

      // Verify they can be caught separately
      boolean caughtWasm = false;
      boolean caughtJava = false;

      try {
        throw wasmException;
      } catch (WasmException e) {
        caughtWasm = true;
      }

      try {
        throw javaException;
      } catch (java.lang.InstantiationException e) {
        caughtJava = true;
      }

      assertTrue(caughtWasm, "Should catch wasm InstantiationException as WasmException");
      assertTrue(caughtJava, "Should catch java.lang.InstantiationException");
    }
  }
}
