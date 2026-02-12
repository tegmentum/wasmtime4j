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
  @DisplayName("InstantiationException Tests")
  class InstantiationExceptionTests {

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final InstantiationException exception = new InstantiationException(TEST_MESSAGE);

      assertEquals(TEST_MESSAGE, exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final Throwable cause = new RuntimeException(TEST_CAUSE_MESSAGE);
      final InstantiationException exception = new InstantiationException(TEST_MESSAGE, cause);

      assertEquals(TEST_MESSAGE, exception.getMessage());
      assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("should extend WasmException")
    void shouldExtendWasmException() {
      final InstantiationException exception = new InstantiationException(TEST_MESSAGE);
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
    @DisplayName("should extend project RuntimeException (which extends WasmException)")
    void shouldExtendRuntimeException() {
      final TrapException exception =
          new TrapException(TrapException.TrapType.UNKNOWN, TEST_MESSAGE);
      // TrapException extends ai.tegmentum.wasmtime4j.exception.RuntimeException
      // which in turn extends WasmException
      assertTrue(
          exception instanceof ai.tegmentum.wasmtime4j.exception.RuntimeException,
          "TrapException should extend project's RuntimeException");
      assertTrue(
          exception instanceof WasmException,
          "TrapException should extend WasmException hierarchy");
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
  @DisplayName("RuntimeException Tests")
  class WasmRuntimeExceptionTests {

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final ai.tegmentum.wasmtime4j.exception.RuntimeException exception =
          new ai.tegmentum.wasmtime4j.exception.RuntimeException(TEST_MESSAGE);

      assertTrue(
          exception.getMessage().contains(TEST_MESSAGE),
          "Exception message should contain: " + TEST_MESSAGE);
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final Throwable cause = new java.lang.RuntimeException(TEST_CAUSE_MESSAGE);
      final ai.tegmentum.wasmtime4j.exception.RuntimeException exception =
          new ai.tegmentum.wasmtime4j.exception.RuntimeException(TEST_MESSAGE, cause);

      assertTrue(
          exception.getMessage().contains(TEST_MESSAGE),
          "Exception message should contain: " + TEST_MESSAGE);
      assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("should extend WasmException")
    void shouldExtendWasmException() {
      final ai.tegmentum.wasmtime4j.exception.RuntimeException exception =
          new ai.tegmentum.wasmtime4j.exception.RuntimeException(TEST_MESSAGE);
      assertTrue(exception instanceof WasmException);
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
  @DisplayName("ResourceException Tests")
  class ResourceExceptionTests {

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final ResourceException exception = new ResourceException(TEST_MESSAGE);

      assertEquals(TEST_MESSAGE, exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final Throwable cause = new java.lang.RuntimeException(TEST_CAUSE_MESSAGE);
      final ResourceException exception = new ResourceException(TEST_MESSAGE, cause);

      assertEquals(TEST_MESSAGE, exception.getMessage());
      assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("should extend WasmException")
    void shouldExtendWasmException() {
      final ResourceException exception = new ResourceException(TEST_MESSAGE);
      assertTrue(exception instanceof WasmException);
    }
  }

  @Nested
  @DisplayName("SecurityException Tests")
  class SecurityExceptionTests {

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final SecurityException exception = new SecurityException(TEST_MESSAGE);

      assertEquals(TEST_MESSAGE, exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final Throwable cause = new java.lang.RuntimeException(TEST_CAUSE_MESSAGE);
      final SecurityException exception = new SecurityException(TEST_MESSAGE, cause);

      assertEquals(TEST_MESSAGE, exception.getMessage());
      assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("should extend WasmException")
    void shouldExtendWasmException() {
      final SecurityException exception = new SecurityException(TEST_MESSAGE);
      assertTrue(exception instanceof WasmException);
    }
  }

  @Nested
  @DisplayName("MarshalingException Tests")
  class MarshalingExceptionTests {

    @Test
    @DisplayName("should create exception with full details")
    void shouldCreateExceptionWithFullDetails() {
      final MarshalingException exception =
          new MarshalingException(
              TEST_MESSAGE,
              MarshalingException.OperationType.SERIALIZATION,
              "TestObject",
              1024,
              "Use simpler objects");

      assertTrue(exception.getMessage().contains(TEST_MESSAGE));
      assertEquals(MarshalingException.OperationType.SERIALIZATION, exception.getOperationType());
      assertEquals("TestObject", exception.getObjectTypeName());
      assertEquals(1024L, exception.getEstimatedSize());
      assertEquals("Use simpler objects", exception.getRecoveryHint());
    }

    @Test
    @DisplayName("should create exception with cause")
    void shouldCreateExceptionWithCause() {
      final Throwable cause = new java.lang.RuntimeException(TEST_CAUSE_MESSAGE);
      final MarshalingException exception =
          new MarshalingException(
              TEST_MESSAGE,
              cause,
              MarshalingException.OperationType.DESERIALIZATION,
              "TestObject",
              -1,
              "Check format");

      assertTrue(exception.getMessage().contains(TEST_MESSAGE));
      assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("should extend WasmException")
    void shouldExtendWasmException() {
      final MarshalingException exception =
          new MarshalingException(
              TEST_MESSAGE, MarshalingException.OperationType.TYPE_CONVERSION, "String", -1, null);
      assertTrue(exception instanceof WasmException);
    }

    @Test
    @DisplayName("should create serialization failure using factory method")
    void shouldCreateSerializationFailureUsingFactoryMethod() {
      final Throwable cause = new java.lang.RuntimeException(TEST_CAUSE_MESSAGE);
      final MarshalingException exception =
          MarshalingException.serializationFailure("TestObject", cause);

      assertEquals(MarshalingException.OperationType.SERIALIZATION, exception.getOperationType());
      assertEquals("TestObject", exception.getObjectTypeName());
      assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("should create type conversion failure using factory method")
    void shouldCreateTypeConversionFailureUsingFactoryMethod() {
      final MarshalingException exception =
          MarshalingException.typeConversionFailure("Integer", "String");

      assertEquals(MarshalingException.OperationType.TYPE_CONVERSION, exception.getOperationType());
      assertTrue(exception.getObjectTypeName().contains("Integer"));
      assertTrue(exception.getObjectTypeName().contains("String"));
    }

    @Test
    @DisplayName("should create circular reference exception using factory method")
    void shouldCreateCircularReferenceExceptionUsingFactoryMethod() {
      final MarshalingException exception =
          MarshalingException.circularReferenceDetected("RecursiveObject");

      assertEquals(
          MarshalingException.OperationType.CIRCULAR_REFERENCE_DETECTION,
          exception.getOperationType());
      assertEquals("RecursiveObject", exception.getObjectTypeName());
    }

    @Test
    @DisplayName("should correctly determine recoverability")
    void shouldCorrectlyDetermineRecoverability() {
      final MarshalingException memoryException =
          new MarshalingException(
              TEST_MESSAGE,
              MarshalingException.OperationType.MEMORY_ALLOCATION,
              "Object",
              1024,
              null);
      final MarshalingException typeException =
          new MarshalingException(
              TEST_MESSAGE, MarshalingException.OperationType.TYPE_CONVERSION, "Object", -1, null);

      assertTrue(memoryException.isRecoverable());
      assertFalse(typeException.isRecoverable());
    }

    @Test
    @DisplayName("should provide retry strategies for recoverable exceptions")
    void shouldProvideRetryStrategiesForRecoverableExceptions() {
      final MarshalingException recoverableException =
          new MarshalingException(
              TEST_MESSAGE,
              MarshalingException.OperationType.MEMORY_ALLOCATION,
              "Object",
              1024,
              null);
      final MarshalingException nonRecoverableException =
          new MarshalingException(
              TEST_MESSAGE, MarshalingException.OperationType.TYPE_CONVERSION, "Object", -1, null);

      assertTrue(recoverableException.getRetryStrategies().length > 0);
      assertEquals(0, nonRecoverableException.getRetryStrategies().length);
    }
  }

  @Nested
  @DisplayName("ModuleCompilationException Tests")
  class ModuleCompilationExceptionTests {

    @Test
    @DisplayName("should create exception with error type and message")
    void shouldCreateExceptionWithErrorTypeAndMessage() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(
              ModuleCompilationException.CompilationErrorType.OUT_OF_MEMORY, TEST_MESSAGE);

      assertTrue(exception.getMessage().contains(TEST_MESSAGE));
      assertEquals(
          ModuleCompilationException.CompilationErrorType.OUT_OF_MEMORY, exception.getErrorType());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should create exception with error type, message, and cause")
    void shouldCreateExceptionWithErrorTypeMessageAndCause() {
      final Throwable cause = new java.lang.RuntimeException(TEST_CAUSE_MESSAGE);
      final ModuleCompilationException exception =
          new ModuleCompilationException(
              ModuleCompilationException.CompilationErrorType.UNSUPPORTED_INSTRUCTION,
              TEST_MESSAGE,
              cause);

      assertTrue(exception.getMessage().contains(TEST_MESSAGE));
      assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("should extend CompilationException")
    void shouldExtendCompilationException() {
      final ModuleCompilationException exception =
          new ModuleCompilationException(
              ModuleCompilationException.CompilationErrorType.UNKNOWN, TEST_MESSAGE);
      assertTrue(exception instanceof CompilationException);
    }
  }

  @Nested
  @DisplayName("ModuleValidationException Tests")
  class ModuleValidationExceptionTests {

    @Test
    @DisplayName("should create exception with error type and message")
    void shouldCreateExceptionWithErrorTypeAndMessage() {
      final ModuleValidationException exception =
          new ModuleValidationException(
              ModuleValidationException.ValidationErrorType.INVALID_MAGIC_NUMBER, TEST_MESSAGE);

      assertTrue(exception.getMessage().contains(TEST_MESSAGE));
      assertEquals(
          ModuleValidationException.ValidationErrorType.INVALID_MAGIC_NUMBER,
          exception.getErrorType());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should extend ValidationException")
    void shouldExtendValidationException() {
      final ModuleValidationException exception =
          new ModuleValidationException(
              ModuleValidationException.ValidationErrorType.UNKNOWN, TEST_MESSAGE);
      assertTrue(exception instanceof ValidationException);
    }
  }

  @Nested
  @DisplayName("ModuleInstantiationException Tests")
  class ModuleInstantiationExceptionTests {

    @Test
    @DisplayName("should create exception with error type and message")
    void shouldCreateExceptionWithErrorTypeAndMessage() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(
              ModuleInstantiationException.InstantiationErrorType.MISSING_IMPORT, TEST_MESSAGE);

      assertTrue(exception.getMessage().contains(TEST_MESSAGE));
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.MISSING_IMPORT,
          exception.getErrorType());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should extend InstantiationException")
    void shouldExtendInstantiationException() {
      final ModuleInstantiationException exception =
          new ModuleInstantiationException(
              ModuleInstantiationException.InstantiationErrorType.UNKNOWN, TEST_MESSAGE);
      assertTrue(exception instanceof InstantiationException);
    }
  }

  @Nested
  @DisplayName("ComponentLinkingException Tests")
  class ComponentLinkingExceptionTests {

    @Test
    @DisplayName("should create exception with full details")
    void shouldCreateExceptionWithFullDetails() {
      final java.util.List<String> components = java.util.Arrays.asList("comp1", "comp2");
      final java.util.Map<String, String> issues = new java.util.HashMap<>();
      issues.put("comp1:comp2", "interface mismatch");
      final java.util.Set<String> missing = new java.util.HashSet<>();
      missing.add("dep1");
      final java.util.Set<String> circular = new java.util.HashSet<>();
      final java.util.List<String> resolutions =
          java.util.Arrays.asList("Update comp1", "Install dep1");

      final ComponentLinkingException exception =
          new ComponentLinkingException(
              TEST_MESSAGE,
              ComponentLinkingException.LinkingFailureType.MISSING_DEPENDENCIES,
              components,
              issues,
              missing,
              circular,
              resolutions);

      assertEquals(TEST_MESSAGE, exception.getMessage());
      assertEquals(
          ComponentLinkingException.LinkingFailureType.MISSING_DEPENDENCIES,
          exception.getFailureType());
      assertEquals(2, exception.getInvolvedComponents().size());
      assertEquals(1, exception.getMissingDependencies().size());
      assertTrue(exception.getMissingDependencies().contains("dep1"));
    }

    @Test
    @DisplayName("should create exception with null optional fields")
    void shouldCreateExceptionWithNullOptionalFields() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              TEST_MESSAGE,
              ComponentLinkingException.LinkingFailureType.UNKNOWN,
              null,
              null,
              null,
              null,
              null);

      assertEquals(TEST_MESSAGE, exception.getMessage());
      assertNull(exception.getInvolvedComponents());
      assertNull(exception.getCompatibilityIssues());
    }

    @Test
    @DisplayName("should extend WasmException")
    void shouldExtendWasmException() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              TEST_MESSAGE,
              ComponentLinkingException.LinkingFailureType.UNKNOWN,
              null,
              null,
              null,
              null,
              null);
      assertTrue(exception instanceof WasmException);
    }

    @Test
    @DisplayName("should correctly determine recoverability")
    void shouldCorrectlyDetermineRecoverability() {
      final ComponentLinkingException recoverableException =
          new ComponentLinkingException(
              TEST_MESSAGE,
              ComponentLinkingException.LinkingFailureType.MISSING_DEPENDENCIES,
              null,
              null,
              null,
              null,
              null);
      final ComponentLinkingException nonRecoverableException =
          new ComponentLinkingException(
              TEST_MESSAGE,
              ComponentLinkingException.LinkingFailureType.NATIVE_LINKING_ERROR,
              null,
              null,
              null,
              null,
              null);

      assertTrue(recoverableException.isRecoverable());
      assertFalse(nonRecoverableException.isRecoverable());
    }

    @Test
    @DisplayName("should generate detailed error report")
    void shouldGenerateDetailedErrorReport() {
      final java.util.List<String> components = java.util.Arrays.asList("comp1", "comp2");
      final java.util.Set<String> missing = new java.util.HashSet<>();
      missing.add("dep1");

      final ComponentLinkingException exception =
          new ComponentLinkingException(
              TEST_MESSAGE,
              ComponentLinkingException.LinkingFailureType.MISSING_DEPENDENCIES,
              components,
              null,
              missing,
              null,
              java.util.Arrays.asList("Install dep1"));

      final String report = exception.getDetailedErrorReport();
      assertNotNull(report);
      assertTrue(report.contains("Component Linking Failure Report"));
      assertTrue(report.contains("MISSING_DEPENDENCIES"));
      assertTrue(report.contains("comp1"));
      assertTrue(report.contains("dep1"));
    }
  }

  @Nested
  @DisplayName("WasiFileSystemException Tests")
  class WasiFileSystemExceptionTests {

    @Test
    @DisplayName("should create exception with error type and message")
    void shouldCreateExceptionWithErrorTypeAndMessage() {
      final WasiFileSystemException exception =
          new WasiFileSystemException(
              WasiFileSystemException.FileSystemErrorType.NOT_FOUND, TEST_MESSAGE);

      assertTrue(exception.getMessage().contains(TEST_MESSAGE));
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.NOT_FOUND,
          exception.getFileSystemErrorType());
    }

    @Test
    @DisplayName("should create exception with error type, message, and cause")
    void shouldCreateExceptionWithErrorTypeMessageAndCause() {
      final Throwable cause = new java.lang.RuntimeException(TEST_CAUSE_MESSAGE);
      final WasiFileSystemException exception =
          new WasiFileSystemException(
              WasiFileSystemException.FileSystemErrorType.PERMISSION_DENIED, TEST_MESSAGE, cause);

      assertTrue(exception.getMessage().contains(TEST_MESSAGE));
      assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("should extend WasiException")
    void shouldExtendWasiException() {
      final WasiFileSystemException exception =
          new WasiFileSystemException(
              WasiFileSystemException.FileSystemErrorType.UNKNOWN, TEST_MESSAGE);
      assertTrue(exception instanceof WasiException);
    }

    @Test
    @DisplayName("should correctly identify permission errors")
    void shouldCorrectlyIdentifyPermissionErrors() {
      final WasiFileSystemException permException =
          new WasiFileSystemException(
              WasiFileSystemException.FileSystemErrorType.PERMISSION_DENIED, TEST_MESSAGE);
      final WasiFileSystemException roException =
          new WasiFileSystemException(
              WasiFileSystemException.FileSystemErrorType.READ_ONLY, TEST_MESSAGE);
      final WasiFileSystemException notFoundException =
          new WasiFileSystemException(
              WasiFileSystemException.FileSystemErrorType.NOT_FOUND, TEST_MESSAGE);

      assertTrue(permException.isPermissionError());
      assertTrue(roException.isPermissionError());
      assertFalse(notFoundException.isPermissionError());
    }

    @Test
    @DisplayName("should correctly identify existence errors")
    void shouldCorrectlyIdentifyExistenceErrors() {
      final WasiFileSystemException notFoundException =
          new WasiFileSystemException(
              WasiFileSystemException.FileSystemErrorType.NOT_FOUND, TEST_MESSAGE);
      final WasiFileSystemException existsException =
          new WasiFileSystemException(
              WasiFileSystemException.FileSystemErrorType.ALREADY_EXISTS, TEST_MESSAGE);
      final WasiFileSystemException permException =
          new WasiFileSystemException(
              WasiFileSystemException.FileSystemErrorType.PERMISSION_DENIED, TEST_MESSAGE);

      assertTrue(notFoundException.isExistenceError());
      assertTrue(existsException.isExistenceError());
      assertFalse(permException.isExistenceError());
    }

    @Test
    @DisplayName("should correctly identify file type errors")
    void shouldCorrectlyIdentifyFileTypeErrors() {
      final WasiFileSystemException isDirException =
          new WasiFileSystemException(
              WasiFileSystemException.FileSystemErrorType.IS_DIRECTORY, TEST_MESSAGE);
      final WasiFileSystemException notDirException =
          new WasiFileSystemException(
              WasiFileSystemException.FileSystemErrorType.NOT_DIRECTORY, TEST_MESSAGE);
      final WasiFileSystemException notFoundException =
          new WasiFileSystemException(
              WasiFileSystemException.FileSystemErrorType.NOT_FOUND, TEST_MESSAGE);

      assertTrue(isDirException.isFileTypeError());
      assertTrue(notDirException.isFileTypeError());
      assertFalse(notFoundException.isFileTypeError());
    }

    @Test
    @DisplayName("should correctly identify transient errors")
    void shouldCorrectlyIdentifyTransientErrors() {
      final WasiFileSystemException ioException =
          new WasiFileSystemException(
              WasiFileSystemException.FileSystemErrorType.IO_ERROR, TEST_MESSAGE);
      final WasiFileSystemException wouldBlockException =
          new WasiFileSystemException(
              WasiFileSystemException.FileSystemErrorType.WOULD_BLOCK, TEST_MESSAGE);
      final WasiFileSystemException permException =
          new WasiFileSystemException(
              WasiFileSystemException.FileSystemErrorType.PERMISSION_DENIED, TEST_MESSAGE);

      assertTrue(ioException.isTransientError());
      assertTrue(wouldBlockException.isTransientError());
      assertFalse(permException.isTransientError());
    }

    @Test
    @DisplayName("should get errno code from error type")
    void shouldGetErrnoCodeFromErrorType() {
      final WasiFileSystemException exception =
          new WasiFileSystemException(
              WasiFileSystemException.FileSystemErrorType.NOT_FOUND, TEST_MESSAGE);

      assertNotNull(exception.getErrnoCode());
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.NOT_FOUND.getErrnoCode(),
          exception.getErrnoCode().intValue());
    }

    @Test
    @DisplayName("all file system error types should have descriptions")
    void allFileSystemErrorTypesShouldHaveDescriptions() {
      for (final WasiFileSystemException.FileSystemErrorType errorType :
          WasiFileSystemException.FileSystemErrorType.values()) {
        assertNotNull(errorType.getDescription(), "Missing description for " + errorType);
        assertFalse(errorType.getDescription().isEmpty(), "Empty description for " + errorType);
      }
    }

    @Test
    @DisplayName("should convert errno to error type")
    void shouldConvertErrnoToErrorType() {
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.NOT_FOUND,
          WasiFileSystemException.FileSystemErrorType.fromErrno(44));
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.PERMISSION_DENIED,
          WasiFileSystemException.FileSystemErrorType.fromErrno(63));
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.UNKNOWN,
          WasiFileSystemException.FileSystemErrorType.fromErrno(999));
    }
  }

  @Nested
  @DisplayName("WasiResourceException Tests")
  class WasiResourceExceptionTests {

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final WasiResourceException exception = new WasiResourceException(TEST_MESSAGE);

      // WasiResourceException adds operation info to message
      assertTrue(
          exception.getMessage().contains(TEST_MESSAGE),
          "Exception message should contain: " + TEST_MESSAGE);
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should extend WasiException")
    void shouldExtendWasiException() {
      final WasiResourceException exception = new WasiResourceException(TEST_MESSAGE);
      assertTrue(exception instanceof WasiException);
    }
  }

  @Nested
  @DisplayName("WasiConfigurationException Tests")
  class WasiConfigurationExceptionTests {

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final WasiConfigurationException exception = new WasiConfigurationException(TEST_MESSAGE);

      // WasiConfigurationException adds operation info to message
      assertTrue(
          exception.getMessage().contains(TEST_MESSAGE),
          "Exception message should contain: " + TEST_MESSAGE);
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should extend WasiException")
    void shouldExtendWasiException() {
      final WasiConfigurationException exception = new WasiConfigurationException(TEST_MESSAGE);
      assertTrue(exception instanceof WasiException);
    }
  }

  @Nested
  @DisplayName("WitValueException Tests")
  class WitValueExceptionTests {

    @Test
    @DisplayName("should create exception with message and error code")
    void shouldCreateExceptionWithMessageAndErrorCode() {
      final WitValueException exception =
          new WitValueException(TEST_MESSAGE, WitValueException.ErrorCode.TYPE_MISMATCH);

      assertTrue(exception.getMessage().contains(TEST_MESSAGE));
      assertEquals(WitValueException.ErrorCode.TYPE_MISMATCH, exception.getCode());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should create exception with message, error code, and cause")
    void shouldCreateExceptionWithMessageErrorCodeAndCause() {
      final Throwable cause = new java.lang.RuntimeException(TEST_CAUSE_MESSAGE);
      final WitValueException exception =
          new WitValueException(TEST_MESSAGE, WitValueException.ErrorCode.RANGE_ERROR, cause);

      assertTrue(exception.getMessage().contains(TEST_MESSAGE));
      assertEquals(WitValueException.ErrorCode.RANGE_ERROR, exception.getCode());
      assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("should extend WasmException")
    void shouldExtendWasmException() {
      final WitValueException exception =
          new WitValueException(TEST_MESSAGE, WitValueException.ErrorCode.INVALID_FORMAT);
      assertTrue(exception instanceof WasmException);
    }

    @Test
    @DisplayName("should have all expected error codes")
    void shouldHaveAllExpectedErrorCodes() {
      final WitValueException.ErrorCode[] codes = WitValueException.ErrorCode.values();
      assertTrue(codes.length >= 6, "Expected at least 6 error codes");

      boolean hasTypeMismatch = false;
      boolean hasRangeError = false;
      boolean hasNullValue = false;

      for (final WitValueException.ErrorCode code : codes) {
        if (code == WitValueException.ErrorCode.TYPE_MISMATCH) {
          hasTypeMismatch = true;
        }
        if (code == WitValueException.ErrorCode.RANGE_ERROR) {
          hasRangeError = true;
        }
        if (code == WitValueException.ErrorCode.NULL_VALUE) {
          hasNullValue = true;
        }
      }

      assertTrue(hasTypeMismatch, "Missing TYPE_MISMATCH error code");
      assertTrue(hasRangeError, "Missing RANGE_ERROR error code");
      assertTrue(hasNullValue, "Missing NULL_VALUE error code");
    }
  }

  @Nested
  @DisplayName("WitRangeException Tests")
  class WitRangeExceptionTests {

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final WitRangeException exception = new WitRangeException(TEST_MESSAGE);

      // WitRangeException adds error code to message
      assertTrue(
          exception.getMessage().contains(TEST_MESSAGE),
          "Exception message should contain: " + TEST_MESSAGE);
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should extend WitValueException")
    void shouldExtendWitValueException() {
      final WitRangeException exception = new WitRangeException(TEST_MESSAGE);
      assertTrue(exception instanceof WitValueException);
      assertTrue(exception instanceof WasmException);
    }
  }

  @Nested
  @DisplayName("WitValidationException Tests")
  class WitValidationExceptionTests {

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final WitValidationException exception = new WitValidationException(TEST_MESSAGE);

      // WitValidationException adds error code to message
      assertTrue(
          exception.getMessage().contains(TEST_MESSAGE),
          "Exception message should contain: " + TEST_MESSAGE);
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should extend WitValueException")
    void shouldExtendWitValueException() {
      final WitValidationException exception = new WitValidationException(TEST_MESSAGE);
      assertTrue(exception instanceof WitValueException);
      assertTrue(exception instanceof WasmException);
    }
  }

  @Nested
  @DisplayName("WitMarshallingException Tests")
  class WitMarshallingExceptionTests {

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final WitMarshallingException exception = new WitMarshallingException(TEST_MESSAGE);

      // WitMarshallingException adds error code to message
      assertTrue(
          exception.getMessage().contains(TEST_MESSAGE),
          "Exception message should contain: " + TEST_MESSAGE);
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should extend WitValueException")
    void shouldExtendWitValueException() {
      final WitMarshallingException exception = new WitMarshallingException(TEST_MESSAGE);
      assertTrue(exception instanceof WitValueException);
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
