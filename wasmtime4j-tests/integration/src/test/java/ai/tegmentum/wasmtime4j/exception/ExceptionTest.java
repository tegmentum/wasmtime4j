/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for exception package.
 *
 * <p>This test class validates the exception hierarchy and specific exception types.
 */
@DisplayName("Exception Integration Tests")
public class ExceptionIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(ExceptionIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting Exception Integration Tests");
  }

  @Nested
  @DisplayName("WasmException Tests")
  class WasmExceptionTests {

    @Test
    @DisplayName("Should create WasmException with message")
    void shouldCreateWasmExceptionWithMessage() {
      LOGGER.info("Testing WasmException with message");

      WasmException exception = new WasmException("Test error");

      assertNotNull(exception, "Exception should not be null");
      assertEquals("Test error", exception.getMessage(), "Message should match");
      assertNull(exception.getCause(), "Cause should be null");

      LOGGER.info("WasmException with message verified");
    }

    @Test
    @DisplayName("Should create WasmException with message and cause")
    void shouldCreateWasmExceptionWithMessageAndCause() {
      LOGGER.info("Testing WasmException with message and cause");

      java.lang.RuntimeException cause = new java.lang.RuntimeException("Original error");
      WasmException exception = new WasmException("Wrapped error", cause);

      assertNotNull(exception, "Exception should not be null");
      assertEquals("Wrapped error", exception.getMessage(), "Message should match");
      assertEquals(cause, exception.getCause(), "Cause should match");

      LOGGER.info("WasmException with message and cause verified");
    }

    @Test
    @DisplayName("Should create WasmException with cause only")
    void shouldCreateWasmExceptionWithCauseOnly() {
      LOGGER.info("Testing WasmException with cause only");

      java.lang.RuntimeException cause = new java.lang.RuntimeException("Original error");
      WasmException exception = new WasmException(cause);

      assertNotNull(exception, "Exception should not be null");
      assertEquals(cause, exception.getCause(), "Cause should match");

      LOGGER.info("WasmException with cause only verified");
    }

    @Test
    @DisplayName("Should be throwable")
    void shouldBeThrowable() {
      LOGGER.info("Testing WasmException is throwable");

      assertThrows(
          WasmException.class,
          () -> {
            throw new WasmException("Test throw");
          },
          "WasmException should be throwable");

      LOGGER.info("WasmException throwable verified");
    }

    @Test
    @DisplayName("Should extend Exception")
    void shouldExtendException() {
      LOGGER.info("Testing WasmException extends Exception");

      assertTrue(
          Exception.class.isAssignableFrom(WasmException.class),
          "WasmException should extend Exception");

      LOGGER.info("WasmException inheritance verified");
    }
  }

  @Nested
  @DisplayName("TrapException Tests")
  class TrapExceptionTests {

    @Test
    @DisplayName("Should have all expected TrapType values")
    void shouldHaveAllExpectedTrapTypeValues() {
      LOGGER.info("Testing TrapException.TrapType enum values");

      TrapException.TrapType[] types = TrapException.TrapType.values();

      assertTrue(types.length >= 14, "Should have at least 14 trap types");

      assertNotNull(TrapException.TrapType.STACK_OVERFLOW, "STACK_OVERFLOW should exist");
      assertNotNull(
          TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, "MEMORY_OUT_OF_BOUNDS should exist");
      assertNotNull(TrapException.TrapType.HEAP_MISALIGNED, "HEAP_MISALIGNED should exist");
      assertNotNull(TrapException.TrapType.TABLE_OUT_OF_BOUNDS, "TABLE_OUT_OF_BOUNDS should exist");
      assertNotNull(
          TrapException.TrapType.INDIRECT_CALL_TO_NULL, "INDIRECT_CALL_TO_NULL should exist");
      assertNotNull(TrapException.TrapType.BAD_SIGNATURE, "BAD_SIGNATURE should exist");
      assertNotNull(TrapException.TrapType.INTEGER_OVERFLOW, "INTEGER_OVERFLOW should exist");
      assertNotNull(
          TrapException.TrapType.INTEGER_DIVISION_BY_ZERO, "INTEGER_DIVISION_BY_ZERO should exist");
      assertNotNull(
          TrapException.TrapType.BAD_CONVERSION_TO_INTEGER,
          "BAD_CONVERSION_TO_INTEGER should exist");
      assertNotNull(
          TrapException.TrapType.UNREACHABLE_CODE_REACHED, "UNREACHABLE_CODE_REACHED should exist");
      assertNotNull(TrapException.TrapType.INTERRUPT, "INTERRUPT should exist");
      assertNotNull(TrapException.TrapType.OUT_OF_FUEL, "OUT_OF_FUEL should exist");
      assertNotNull(TrapException.TrapType.NULL_REFERENCE, "NULL_REFERENCE should exist");
      assertNotNull(TrapException.TrapType.ARRAY_OUT_OF_BOUNDS, "ARRAY_OUT_OF_BOUNDS should exist");
      assertNotNull(TrapException.TrapType.UNKNOWN, "UNKNOWN should exist");

      LOGGER.info("TrapType enum verified: " + types.length + " types");
    }

    @Test
    @DisplayName("Should create TrapException with type and message")
    void shouldCreateTrapExceptionWithTypeAndMessage() {
      LOGGER.info("Testing TrapException creation");

      TrapException exception =
          new TrapException(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, "Memory access error");

      assertNotNull(exception, "Exception should not be null");
      assertEquals(
          TrapException.TrapType.MEMORY_OUT_OF_BOUNDS,
          exception.getTrapType(),
          "Trap type should match");
      assertTrue(
          exception.getMessage().contains("Memory access error"), "Message should contain error");

      LOGGER.info("TrapException creation verified");
    }

    @Test
    @DisplayName("Should provide trap type description")
    void shouldProvideTrapTypeDescription() {
      LOGGER.info("Testing TrapType descriptions");

      for (TrapException.TrapType type : TrapException.TrapType.values()) {
        String description = type.getDescription();
        assertNotNull(description, "Description should not be null for " + type);
        assertFalse(description.isEmpty(), "Description should not be empty for " + type);
        LOGGER.info(type.name() + ": " + description);
      }

      LOGGER.info("TrapType descriptions verified");
    }

    @Test
    @DisplayName("Should identify memory errors correctly")
    void shouldIdentifyMemoryErrorsCorrectly() {
      LOGGER.info("Testing isMemoryError() method");

      TrapException memoryError =
          new TrapException(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, "Memory error");
      assertTrue(memoryError.isMemoryError(), "MEMORY_OUT_OF_BOUNDS should be memory error");

      TrapException stackError =
          new TrapException(TrapException.TrapType.STACK_OVERFLOW, "Stack error");
      assertTrue(stackError.isMemoryError(), "STACK_OVERFLOW should be memory error");

      TrapException heapError =
          new TrapException(TrapException.TrapType.HEAP_MISALIGNED, "Heap error");
      assertTrue(heapError.isMemoryError(), "HEAP_MISALIGNED should be memory error");

      TrapException intError =
          new TrapException(TrapException.TrapType.INTEGER_OVERFLOW, "Int error");
      assertFalse(intError.isMemoryError(), "INTEGER_OVERFLOW should not be memory error");

      LOGGER.info("isMemoryError() method verified");
    }

    @Test
    @DisplayName("Should identify arithmetic errors correctly")
    void shouldIdentifyArithmeticErrorsCorrectly() {
      LOGGER.info("Testing isArithmeticError() method");

      TrapException overflowError =
          new TrapException(TrapException.TrapType.INTEGER_OVERFLOW, "Overflow");
      assertTrue(overflowError.isArithmeticError(), "INTEGER_OVERFLOW should be arithmetic error");

      TrapException divByZeroError =
          new TrapException(TrapException.TrapType.INTEGER_DIVISION_BY_ZERO, "Div by zero");
      assertTrue(
          divByZeroError.isArithmeticError(),
          "INTEGER_DIVISION_BY_ZERO should be arithmetic error");

      TrapException conversionError =
          new TrapException(TrapException.TrapType.BAD_CONVERSION_TO_INTEGER, "Bad conversion");
      assertTrue(
          conversionError.isArithmeticError(),
          "BAD_CONVERSION_TO_INTEGER should be arithmetic error");

      TrapException memoryError =
          new TrapException(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, "Memory");
      assertFalse(
          memoryError.isArithmeticError(), "MEMORY_OUT_OF_BOUNDS should not be arithmetic error");

      LOGGER.info("isArithmeticError() method verified");
    }

    @Test
    @DisplayName("Should identify control flow errors correctly")
    void shouldIdentifyControlFlowErrorsCorrectly() {
      LOGGER.info("Testing isControlFlowError() method");

      TrapException indirectCallError =
          new TrapException(TrapException.TrapType.INDIRECT_CALL_TO_NULL, "Indirect call");
      assertTrue(
          indirectCallError.isControlFlowError(),
          "INDIRECT_CALL_TO_NULL should be control flow error");

      TrapException signatureError =
          new TrapException(TrapException.TrapType.BAD_SIGNATURE, "Bad signature");
      assertTrue(signatureError.isControlFlowError(), "BAD_SIGNATURE should be control flow error");

      TrapException unreachableError =
          new TrapException(TrapException.TrapType.UNREACHABLE_CODE_REACHED, "Unreachable");
      assertTrue(
          unreachableError.isControlFlowError(),
          "UNREACHABLE_CODE_REACHED should be control flow error");

      TrapException nullRefError =
          new TrapException(TrapException.TrapType.NULL_REFERENCE, "Null ref");
      assertTrue(nullRefError.isControlFlowError(), "NULL_REFERENCE should be control flow error");

      TrapException memoryError =
          new TrapException(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, "Memory");
      assertFalse(
          memoryError.isControlFlowError(),
          "MEMORY_OUT_OF_BOUNDS should not be control flow error");

      LOGGER.info("isControlFlowError() method verified");
    }

    @Test
    @DisplayName("Should identify resource exhaustion errors correctly")
    void shouldIdentifyResourceExhaustionErrorsCorrectly() {
      LOGGER.info("Testing isResourceExhaustionError() method");

      TrapException stackError = new TrapException(TrapException.TrapType.STACK_OVERFLOW, "Stack");
      assertTrue(
          stackError.isResourceExhaustionError(),
          "STACK_OVERFLOW should be resource exhaustion error");

      TrapException fuelError = new TrapException(TrapException.TrapType.OUT_OF_FUEL, "Fuel");
      assertTrue(
          fuelError.isResourceExhaustionError(), "OUT_OF_FUEL should be resource exhaustion error");

      TrapException interruptError =
          new TrapException(TrapException.TrapType.INTERRUPT, "Interrupt");
      assertTrue(
          interruptError.isResourceExhaustionError(),
          "INTERRUPT should be resource exhaustion error");

      TrapException memoryError =
          new TrapException(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, "Memory");
      assertFalse(
          memoryError.isResourceExhaustionError(),
          "MEMORY_OUT_OF_BOUNDS should not be resource exhaustion error");

      LOGGER.info("isResourceExhaustionError() method verified");
    }

    @Test
    @DisplayName("Should identify bounds errors correctly")
    void shouldIdentifyBoundsErrorsCorrectly() {
      LOGGER.info("Testing isBoundsError() method");

      TrapException memoryError =
          new TrapException(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, "Memory");
      assertTrue(memoryError.isBoundsError(), "MEMORY_OUT_OF_BOUNDS should be bounds error");

      TrapException tableError =
          new TrapException(TrapException.TrapType.TABLE_OUT_OF_BOUNDS, "Table");
      assertTrue(tableError.isBoundsError(), "TABLE_OUT_OF_BOUNDS should be bounds error");

      TrapException arrayError =
          new TrapException(TrapException.TrapType.ARRAY_OUT_OF_BOUNDS, "Array");
      assertTrue(arrayError.isBoundsError(), "ARRAY_OUT_OF_BOUNDS should be bounds error");

      TrapException stackError = new TrapException(TrapException.TrapType.STACK_OVERFLOW, "Stack");
      assertFalse(stackError.isBoundsError(), "STACK_OVERFLOW should not be bounds error");

      LOGGER.info("isBoundsError() method verified");
    }

    @Test
    @DisplayName("Should provide recovery suggestion")
    void shouldProvideRecoverySuggestion() {
      LOGGER.info("Testing getRecoverySuggestion() method");

      for (TrapException.TrapType type : TrapException.TrapType.values()) {
        TrapException exception = new TrapException(type, "Test");
        String suggestion = exception.getRecoverySuggestion();
        assertNotNull(suggestion, "Recovery suggestion should not be null for " + type);
        assertFalse(suggestion.isEmpty(), "Recovery suggestion should not be empty for " + type);
        LOGGER.info(type.name() + " suggestion: " + suggestion);
      }

      LOGGER.info("Recovery suggestions verified");
    }

    @Test
    @DisplayName("Should extend WasmException")
    void shouldExtendWasmException() {
      LOGGER.info("Testing TrapException extends WasmException");

      Class<?> trapClass = TrapException.class;
      Class<?> superClass = trapClass.getSuperclass();
      LOGGER.info("TrapException class: " + trapClass.getName());
      LOGGER.info("TrapException superclass: " + superClass.getName());
      LOGGER.info(
          "WasmException.isAssignableFrom(TrapException): "
              + WasmException.class.isAssignableFrom(trapClass));

      assertTrue(
          WasmException.class.isAssignableFrom(trapClass),
          "TrapException should extend WasmException, but superclass is " + superClass.getName());

      LOGGER.info("TrapException inheritance verified");
    }
  }

  @Nested
  @DisplayName("Exception Hierarchy Tests")
  class ExceptionHierarchyTests {

    @Test
    @DisplayName("Should verify CompilationException extends WasmException")
    void shouldVerifyCompilationExceptionExtendsWasmException() {
      LOGGER.info("Testing CompilationException hierarchy");

      assertTrue(
          WasmException.class.isAssignableFrom(CompilationException.class),
          "CompilationException should extend WasmException");

      LOGGER.info("CompilationException hierarchy verified");
    }

    @Test
    @DisplayName("Should verify ValidationException extends WasmException")
    void shouldVerifyValidationExceptionExtendsWasmException() {
      LOGGER.info("Testing ValidationException hierarchy");

      assertTrue(
          WasmException.class.isAssignableFrom(ValidationException.class),
          "ValidationException should extend WasmException");

      LOGGER.info("ValidationException hierarchy verified");
    }

    @Test
    @DisplayName("Should verify LinkingException extends WasmException")
    void shouldVerifyLinkingExceptionExtendsWasmException() {
      LOGGER.info("Testing LinkingException hierarchy");

      assertTrue(
          WasmException.class.isAssignableFrom(LinkingException.class),
          "LinkingException should extend WasmException");

      LOGGER.info("LinkingException hierarchy verified");
    }

    @Test
    @DisplayName("Should verify WasiException extends WasmException")
    void shouldVerifyWasiExceptionExtendsWasmException() {
      LOGGER.info("Testing WasiException hierarchy");

      assertTrue(
          WasmException.class.isAssignableFrom(WasiException.class),
          "WasiException should extend WasmException");

      LOGGER.info("WasiException hierarchy verified");
    }
  }

  @Nested
  @DisplayName("Specific Exception Tests")
  class SpecificExceptionTests {

    @Test
    @DisplayName("Should create CompilationException")
    void shouldCreateCompilationException() {
      LOGGER.info("Testing CompilationException creation");

      CompilationException exception = new CompilationException("Compilation failed");

      assertNotNull(exception, "Exception should not be null");
      assertEquals("Compilation failed", exception.getMessage(), "Message should match");

      LOGGER.info("CompilationException creation verified");
    }

    @Test
    @DisplayName("Should create ValidationException")
    void shouldCreateValidationException() {
      LOGGER.info("Testing ValidationException creation");

      ValidationException exception = new ValidationException("Validation failed");

      assertNotNull(exception, "Exception should not be null");
      assertEquals("Validation failed", exception.getMessage(), "Message should match");

      LOGGER.info("ValidationException creation verified");
    }

    @Test
    @DisplayName("Should create LinkingException")
    void shouldCreateLinkingException() {
      LOGGER.info("Testing LinkingException creation");

      LinkingException exception =
          new LinkingException(
              LinkingException.LinkingErrorType.IMPORT_NOT_FOUND, "Linking failed");

      assertNotNull(exception, "Exception should not be null");
      assertTrue(
          exception.getMessage().contains("Linking failed"), "Message should contain error text");
      assertEquals(
          LinkingException.LinkingErrorType.IMPORT_NOT_FOUND,
          exception.getErrorType(),
          "Error type should match");

      LOGGER.info("LinkingException creation verified");
    }
  }
}
