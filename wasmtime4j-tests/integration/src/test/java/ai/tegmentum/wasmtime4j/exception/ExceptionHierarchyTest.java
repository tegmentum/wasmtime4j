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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for the wasmtime4j exception hierarchy.
 *
 * <p>Tests all exception classes for proper construction, message handling, cause chaining, and
 * serialization.
 *
 * @since 1.0.0
 */
@DisplayName("Exception Hierarchy Tests")
public final class ExceptionHierarchyTest {

  private static final Logger LOGGER = Logger.getLogger(ExceptionHierarchyTest.class.getName());

  private static final String TEST_MESSAGE = "Test error message";
  private static final String TEST_CAUSE_MESSAGE = "Test cause message";

  @Nested
  @DisplayName("WasmException Tests")
  class WasmExceptionTests {

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final WasmException exception = new WasmException(TEST_MESSAGE);

      assertEquals(TEST_MESSAGE, exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final Throwable cause = new RuntimeException(TEST_CAUSE_MESSAGE);
      final WasmException exception = new WasmException(TEST_MESSAGE, cause);

      assertEquals(TEST_MESSAGE, exception.getMessage());
      assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("should create exception with cause only")
    void shouldCreateExceptionWithCauseOnly() {
      final Throwable cause = new RuntimeException(TEST_CAUSE_MESSAGE);
      final WasmException exception = new WasmException(cause);

      assertNotNull(exception.getMessage());
      assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("should be serializable")
    void shouldBeSerializable() throws Exception {
      final WasmException original = new WasmException(TEST_MESSAGE);
      final WasmException deserialized = serializeAndDeserialize(original);

      assertEquals(original.getMessage(), deserialized.getMessage());
    }

    @Test
    @DisplayName("should extend Exception")
    void shouldExtendException() {
      final WasmException exception = new WasmException(TEST_MESSAGE);
      assertTrue(exception instanceof Exception);
    }
  }

  @Nested
  @DisplayName("CompilationException Tests")
  class CompilationExceptionTests {

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final CompilationException exception = new CompilationException(TEST_MESSAGE);

      assertEquals(TEST_MESSAGE, exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final Throwable cause = new RuntimeException(TEST_CAUSE_MESSAGE);
      final CompilationException exception = new CompilationException(TEST_MESSAGE, cause);

      assertEquals(TEST_MESSAGE, exception.getMessage());
      assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("should extend WasmException")
    void shouldExtendWasmException() {
      final CompilationException exception = new CompilationException(TEST_MESSAGE);
      assertTrue(exception instanceof WasmException);
    }

    @Test
    @DisplayName("should be serializable")
    void shouldBeSerializable() throws Exception {
      final CompilationException original = new CompilationException(TEST_MESSAGE);
      final CompilationException deserialized = serializeAndDeserialize(original);

      assertEquals(original.getMessage(), deserialized.getMessage());
    }
  }

  @Nested
  @DisplayName("ValidationException Tests")
  class ValidationExceptionTests {

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final ValidationException exception = new ValidationException(TEST_MESSAGE);

      assertEquals(TEST_MESSAGE, exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final Throwable cause = new RuntimeException(TEST_CAUSE_MESSAGE);
      final ValidationException exception = new ValidationException(TEST_MESSAGE, cause);

      assertEquals(TEST_MESSAGE, exception.getMessage());
      assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("should extend WasmException")
    void shouldExtendWasmException() {
      final ValidationException exception = new ValidationException(TEST_MESSAGE);
      assertTrue(exception instanceof WasmException);
    }
  }

  @Nested
  @DisplayName("LinkingException Tests")
  class LinkingExceptionTests {

    @Test
    @DisplayName("should create exception with error type and message")
    void shouldCreateExceptionWithErrorTypeAndMessage() {
      final LinkingException exception =
          new LinkingException(LinkingException.LinkingErrorType.IMPORT_NOT_FOUND, TEST_MESSAGE);

      assertTrue(exception.getMessage().contains(TEST_MESSAGE));
      assertEquals(LinkingException.LinkingErrorType.IMPORT_NOT_FOUND, exception.getErrorType());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should create exception with error type, message, and cause")
    void shouldCreateExceptionWithErrorTypeMessageAndCause() {
      final Throwable cause = new RuntimeException(TEST_CAUSE_MESSAGE);
      final LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH, TEST_MESSAGE, cause);

      assertTrue(exception.getMessage().contains(TEST_MESSAGE));
      assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("should extend WasmException")
    void shouldExtendWasmException() {
      final LinkingException exception =
          new LinkingException(LinkingException.LinkingErrorType.UNKNOWN, TEST_MESSAGE);
      assertTrue(exception instanceof WasmException);
    }

    @Test
    @DisplayName("should correctly identify missing item errors")
    void shouldCorrectlyIdentifyMissingItemErrors() {
      final LinkingException importException =
          new LinkingException(LinkingException.LinkingErrorType.IMPORT_NOT_FOUND, TEST_MESSAGE);
      final LinkingException exportException =
          new LinkingException(LinkingException.LinkingErrorType.EXPORT_NOT_FOUND, TEST_MESSAGE);
      final LinkingException sigException =
          new LinkingException(
              LinkingException.LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH, TEST_MESSAGE);

      assertTrue(importException.isMissingItemError());
      assertTrue(exportException.isMissingItemError());
      assertFalse(sigException.isMissingItemError());
    }

    @Test
    @DisplayName("should correctly identify type mismatch errors")
    void shouldCorrectlyIdentifyTypeMismatchErrors() {
      final LinkingException sigException =
          new LinkingException(
              LinkingException.LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH, TEST_MESSAGE);
      final LinkingException memException =
          new LinkingException(
              LinkingException.LinkingErrorType.MEMORY_SIZE_MISMATCH, TEST_MESSAGE);
      final LinkingException importException =
          new LinkingException(LinkingException.LinkingErrorType.IMPORT_NOT_FOUND, TEST_MESSAGE);

      assertTrue(sigException.isTypeMismatchError());
      assertTrue(memException.isTypeMismatchError());
      assertFalse(importException.isTypeMismatchError());
    }

    @Test
    @DisplayName("should provide recovery suggestions for all error types")
    void shouldProvideRecoverySuggestionsForAllErrorTypes() {
      for (final LinkingException.LinkingErrorType errorType :
          LinkingException.LinkingErrorType.values()) {
        final LinkingException exception = new LinkingException(errorType, TEST_MESSAGE);
        assertNotNull(exception.getRecoverySuggestion(), "Missing suggestion for " + errorType);
        assertFalse(
            exception.getRecoverySuggestion().isEmpty(), "Empty suggestion for " + errorType);
      }
    }

    @Test
    @DisplayName("all linking error types should have descriptions")
    void allLinkingErrorTypesShouldHaveDescriptions() {
      for (final LinkingException.LinkingErrorType errorType :
          LinkingException.LinkingErrorType.values()) {
        assertNotNull(errorType.getDescription(), "Missing description for " + errorType);
        assertFalse(errorType.getDescription().isEmpty(), "Empty description for " + errorType);
      }
    }
  }

  @Nested
  @DisplayName("TrapException Tests")
  class TrapExceptionTests {

    @Test
    @DisplayName("should create exception with trap type and message")
    void shouldCreateExceptionWithTrapTypeAndMessage() {
      final TrapException exception =
          new TrapException(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, TEST_MESSAGE);

      assertTrue(exception.getMessage().contains(TEST_MESSAGE));
      assertEquals(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, exception.getTrapType());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should create exception with trap type, message, and cause")
    void shouldCreateExceptionWithTrapTypeMessageAndCause() {
      final Throwable cause = new RuntimeException(TEST_CAUSE_MESSAGE);
      final TrapException exception =
          new TrapException(TrapException.TrapType.STACK_OVERFLOW, TEST_MESSAGE, cause);

      assertTrue(exception.getMessage().contains(TEST_MESSAGE));
      assertEquals(TrapException.TrapType.STACK_OVERFLOW, exception.getTrapType());
      assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("should create exception with full trap details")
    void shouldCreateExceptionWithFullTrapDetails() {
      final TrapException exception =
          new TrapException(
              TrapException.TrapType.INTEGER_DIVISION_BY_ZERO,
              TEST_MESSAGE,
              "wasm backtrace here",
              "myFunction",
              42,
              null);

      assertEquals(TrapException.TrapType.INTEGER_DIVISION_BY_ZERO, exception.getTrapType());
      assertEquals("wasm backtrace here", exception.getWasmBacktrace());
      assertEquals("myFunction", exception.getFunctionName());
      assertEquals(Integer.valueOf(42), exception.getInstructionOffset());
      assertNotNull(exception.getRecoverySuggestion());
    }

    @Test
    @DisplayName("should extend RuntimeException")
    void shouldExtendRuntimeException() {
      final TrapException exception =
          new TrapException(TrapException.TrapType.UNKNOWN, TEST_MESSAGE);
      // TrapException extends RuntimeException (traps are unrecoverable)
      assertTrue(
          exception instanceof RuntimeException, "TrapException should extend RuntimeException");
    }

    @Test
    @DisplayName("should correctly identify memory errors")
    void shouldCorrectlyIdentifyMemoryErrors() {
      final TrapException memoryException =
          new TrapException(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, TEST_MESSAGE);
      final TrapException stackException =
          new TrapException(TrapException.TrapType.STACK_OVERFLOW, TEST_MESSAGE);
      final TrapException heapException =
          new TrapException(TrapException.TrapType.HEAP_MISALIGNED, TEST_MESSAGE);
      final TrapException divException =
          new TrapException(TrapException.TrapType.INTEGER_DIVISION_BY_ZERO, TEST_MESSAGE);

      assertTrue(memoryException.isMemoryError());
      assertTrue(stackException.isMemoryError());
      assertTrue(heapException.isMemoryError());
      assertFalse(divException.isMemoryError());
    }

    @Test
    @DisplayName("should correctly identify arithmetic errors")
    void shouldCorrectlyIdentifyArithmeticErrors() {
      final TrapException overflowException =
          new TrapException(TrapException.TrapType.INTEGER_OVERFLOW, TEST_MESSAGE);
      final TrapException divException =
          new TrapException(TrapException.TrapType.INTEGER_DIVISION_BY_ZERO, TEST_MESSAGE);
      final TrapException convException =
          new TrapException(TrapException.TrapType.BAD_CONVERSION_TO_INTEGER, TEST_MESSAGE);
      final TrapException memException =
          new TrapException(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, TEST_MESSAGE);

      assertTrue(overflowException.isArithmeticError());
      assertTrue(divException.isArithmeticError());
      assertTrue(convException.isArithmeticError());
      assertFalse(memException.isArithmeticError());
    }

    @Test
    @DisplayName("should correctly identify control flow errors")
    void shouldCorrectlyIdentifyControlFlowErrors() {
      final TrapException nullException =
          new TrapException(TrapException.TrapType.INDIRECT_CALL_TO_NULL, TEST_MESSAGE);
      final TrapException sigException =
          new TrapException(TrapException.TrapType.BAD_SIGNATURE, TEST_MESSAGE);
      final TrapException unreachableException =
          new TrapException(TrapException.TrapType.UNREACHABLE_CODE_REACHED, TEST_MESSAGE);
      final TrapException memException =
          new TrapException(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, TEST_MESSAGE);

      assertTrue(nullException.isControlFlowError());
      assertTrue(sigException.isControlFlowError());
      assertTrue(unreachableException.isControlFlowError());
      assertFalse(memException.isControlFlowError());
    }

    @Test
    @DisplayName("should correctly identify resource exhaustion errors")
    void shouldCorrectlyIdentifyResourceExhaustionErrors() {
      final TrapException stackException =
          new TrapException(TrapException.TrapType.STACK_OVERFLOW, TEST_MESSAGE);
      final TrapException fuelException =
          new TrapException(TrapException.TrapType.OUT_OF_FUEL, TEST_MESSAGE);
      final TrapException interruptException =
          new TrapException(TrapException.TrapType.INTERRUPT, TEST_MESSAGE);
      final TrapException divException =
          new TrapException(TrapException.TrapType.INTEGER_DIVISION_BY_ZERO, TEST_MESSAGE);

      assertTrue(stackException.isResourceExhaustionError());
      assertTrue(fuelException.isResourceExhaustionError());
      assertTrue(interruptException.isResourceExhaustionError());
      assertFalse(divException.isResourceExhaustionError());
    }

    @Test
    @DisplayName("should correctly identify bounds errors")
    void shouldCorrectlyIdentifyBoundsErrors() {
      final TrapException memException =
          new TrapException(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, TEST_MESSAGE);
      final TrapException tableException =
          new TrapException(TrapException.TrapType.TABLE_OUT_OF_BOUNDS, TEST_MESSAGE);
      final TrapException arrayException =
          new TrapException(TrapException.TrapType.ARRAY_OUT_OF_BOUNDS, TEST_MESSAGE);
      final TrapException divException =
          new TrapException(TrapException.TrapType.INTEGER_DIVISION_BY_ZERO, TEST_MESSAGE);

      assertTrue(memException.isBoundsError());
      assertTrue(tableException.isBoundsError());
      assertTrue(arrayException.isBoundsError());
      assertFalse(divException.isBoundsError());
    }

    @Test
    @DisplayName("should provide recovery suggestions for all trap types")
    void shouldProvideRecoverySuggestionsForAllTrapTypes() {
      for (final TrapException.TrapType trapType : TrapException.TrapType.values()) {
        final TrapException exception = new TrapException(trapType, TEST_MESSAGE);
        assertNotNull(exception.getRecoverySuggestion(), "Missing suggestion for " + trapType);
        assertFalse(
            exception.getRecoverySuggestion().isEmpty(), "Empty suggestion for " + trapType);
      }
    }

    @Test
    @DisplayName("all trap types should have descriptions")
    void allTrapTypesShouldHaveDescriptions() {
      for (final TrapException.TrapType trapType : TrapException.TrapType.values()) {
        assertNotNull(trapType.getDescription(), "Missing description for " + trapType);
        assertFalse(trapType.getDescription().isEmpty(), "Empty description for " + trapType);
      }
    }

    @Test
    @DisplayName("should handle null trap type gracefully")
    void shouldHandleNullTrapTypeGracefully() {
      final TrapException exception = new TrapException(null, TEST_MESSAGE);
      assertEquals(TrapException.TrapType.UNKNOWN, exception.getTrapType());
    }

    @Test
    @DisplayName("should reject null or empty message")
    void shouldRejectNullOrEmptyMessage() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new TrapException(TrapException.TrapType.UNKNOWN, null));
      assertThrows(
          IllegalArgumentException.class,
          () -> new TrapException(TrapException.TrapType.UNKNOWN, ""));
    }
  }

  @Nested
  @DisplayName("WasiException Tests")
  class WasiExceptionTests {

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final WasiException exception = new WasiException(TEST_MESSAGE);

      assertEquals(TEST_MESSAGE, exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final Throwable cause = new java.lang.RuntimeException(TEST_CAUSE_MESSAGE);
      final WasiException exception = new WasiException(TEST_MESSAGE, cause);

      assertEquals(TEST_MESSAGE, exception.getMessage());
      assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("should extend WasmException")
    void shouldExtendWasmException() {
      final WasiException exception = new WasiException(TEST_MESSAGE);
      assertTrue(exception instanceof WasmException);
    }
  }

  @Nested
  @DisplayName("Exception Chaining Tests")
  class ExceptionChainingTests {

    @Test
    @DisplayName("should preserve exception chain across multiple levels")
    void shouldPreserveExceptionChainAcrossMultipleLevels() {
      final java.lang.RuntimeException root = new java.lang.RuntimeException("Root cause");
      final WasiException middle = new WasiException("Middle layer", root);
      final WasmException top = new WasmException("Top level", middle);

      assertEquals("Top level", top.getMessage());
      assertEquals(middle, top.getCause());
      assertEquals(root, top.getCause().getCause());
    }

    @Test
    @DisplayName("should allow different exception types in chain")
    void shouldAllowDifferentExceptionTypesInChain() {
      final CompilationException compilation = new CompilationException("Compilation failed");
      final LinkingException linking =
          new LinkingException(
              LinkingException.LinkingErrorType.IMPORT_NOT_FOUND, "Linking failed", compilation);
      final WasmException wrapper = new WasmException("Operation failed", linking);

      assertTrue(wrapper.getCause() instanceof LinkingException);
      assertTrue(wrapper.getCause().getCause() instanceof CompilationException);
    }
  }

  @Nested
  @DisplayName("Serialization Tests")
  class SerializationTests {

    @Test
    @DisplayName("should serialize and deserialize CompilationException")
    void shouldSerializeAndDeserializeCompilationException() throws Exception {
      final CompilationException original = new CompilationException(TEST_MESSAGE);
      final CompilationException deserialized = serializeAndDeserialize(original);

      assertEquals(original.getMessage(), deserialized.getMessage());
    }

    @Test
    @DisplayName("should serialize and deserialize TrapException")
    void shouldSerializeAndDeserializeTrapException() throws Exception {
      final TrapException original =
          new TrapException(
              TrapException.TrapType.MEMORY_OUT_OF_BOUNDS,
              TEST_MESSAGE,
              "backtrace",
              "func",
              42,
              null);
      final TrapException deserialized = serializeAndDeserialize(original);

      assertEquals(original.getMessage(), deserialized.getMessage());
      assertEquals(original.getTrapType(), deserialized.getTrapType());
      assertEquals(original.getWasmBacktrace(), deserialized.getWasmBacktrace());
      assertEquals(original.getFunctionName(), deserialized.getFunctionName());
      assertEquals(original.getInstructionOffset(), deserialized.getInstructionOffset());
    }

    @Test
    @DisplayName("should serialize exception with cause")
    void shouldSerializeExceptionWithCause() throws Exception {
      final java.lang.RuntimeException cause = new java.lang.RuntimeException(TEST_CAUSE_MESSAGE);
      final WasmException original = new WasmException(TEST_MESSAGE, cause);
      final WasmException deserialized = serializeAndDeserialize(original);

      assertEquals(original.getMessage(), deserialized.getMessage());
      assertNotNull(deserialized.getCause());
      assertEquals(TEST_CAUSE_MESSAGE, deserialized.getCause().getMessage());
    }
  }

  /**
   * Helper method to serialize and deserialize an exception.
   *
   * @param exception the exception to serialize
   * @param <T> the exception type
   * @return the deserialized exception
   * @throws Exception if serialization fails
   */
  @SuppressWarnings("unchecked")
  private <T extends Exception> T serializeAndDeserialize(final T exception) throws Exception {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(exception);
    }

    final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    try (ObjectInputStream ois = new ObjectInputStream(bais)) {
      return (T) ois.readObject();
    }
  }
}
