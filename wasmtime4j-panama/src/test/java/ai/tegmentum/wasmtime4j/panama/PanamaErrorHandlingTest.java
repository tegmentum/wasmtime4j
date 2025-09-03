package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Comprehensive tests for Panama FFI-specific error handling.
 *
 * <p>These tests verify that Panama error codes are properly aligned with Rust, that error pointer
 * interpretation is defensive, and that proper exceptions are thrown for all error scenarios.
 */
@DisplayName("Panama Error Handling Tests")
class PanamaErrorHandlingTest {

  @Test
  @DisplayName("All 18 error codes have proper descriptions")
  void testAllErrorCodeDescriptions() {
    // Test that all Rust error codes (-1 to -18) are properly handled
    for (int errorCode = -1; errorCode >= -18; errorCode--) {
      String description = PanamaErrorHandler.getErrorDescription(errorCode);

      assertNotNull(description, "Error code " + errorCode + " should have description");
      assertFalse(description.isEmpty(), "Description should not be empty");
      assertFalse(
          description.startsWith("Unknown Error"),
          "Error code " + errorCode + " should not be unknown, got: " + description);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "-1, Compilation Error",
    "-2, Validation Error",
    "-3, Runtime Error",
    "-4, Engine Configuration Error",
    "-5, Store Error",
    "-6, Instance Error",
    "-7, Memory Error",
    "-8, Function Error",
    "-9, Import/Export Error",
    "-10, Type Error",
    "-11, Resource Error",
    "-12, I/O Error",
    "-13, Invalid Parameter",
    "-14, Concurrency Error",
    "-15, WASI Error",
    "-16, Component Error",
    "-17, Interface Error",
    "-18, Internal Error"
  })
  @DisplayName("Error code descriptions match expected values")
  void testErrorCodeDescriptions(int errorCode, String expectedDescription) {
    String actualDescription = PanamaErrorHandler.getErrorDescription(errorCode);
    assertEquals(
        expectedDescription,
        actualDescription,
        "Error code " + errorCode + " description mismatch");
  }

  @Test
  @DisplayName("Success error code handling")
  void testSuccessErrorCode() {
    // Success (0) should not throw
    assertDoesNotThrow(
        () -> {
          PanamaErrorHandler.checkErrorCode(0, "test operation");
        });

    assertEquals("Success", PanamaErrorHandler.getErrorDescription(0));
  }

  @Test
  @DisplayName("Error code exception throwing")
  void testErrorCodeExceptionThrowing() {
    // Compilation error should throw CompilationException
    assertThrows(
        CompilationException.class,
        () -> {
          PanamaErrorHandler.checkErrorCode(-1, "compilation test");
        });

    // Validation error should throw ValidationException
    assertThrows(
        ValidationException.class,
        () -> {
          PanamaErrorHandler.checkErrorCode(-2, "validation test");
        });

    // Runtime error should throw RuntimeException
    assertThrows(
        ai.tegmentum.wasmtime4j.exception.RuntimeException.class,
        () -> {
          PanamaErrorHandler.checkErrorCode(-3, "runtime test");
        });

    // All other errors should throw RuntimeException
    for (int errorCode = -4; errorCode >= -18; errorCode--) {
      final int finalErrorCode = errorCode;
      assertThrows(
          ai.tegmentum.wasmtime4j.exception.RuntimeException.class,
          () -> {
            PanamaErrorHandler.checkErrorCode(finalErrorCode, "test for code " + finalErrorCode);
          });
    }
  }

  @Test
  @DisplayName("Custom error messages")
  void testCustomErrorMessages() {
    try {
      PanamaErrorHandler.checkErrorCode(-1, "Custom operation failed");
      fail("Should have thrown exception");
    } catch (final CompilationException e) {
      assertTrue(e.getMessage().contains("Custom operation failed"));
    } catch (final WasmException e) {
      fail("Unexpected exception type: " + e.getClass().getName());
    }

    try {
      PanamaErrorHandler.checkErrorCode(-1, "Format test %d %s", 42, "value");
      fail("Should have thrown exception");
    } catch (final CompilationException e) {
      assertTrue(e.getMessage().contains("Format test 42 value"));
    } catch (final WasmException e) {
      fail("Unexpected exception type: " + e.getClass().getName());
    }
  }

  @Test
  @DisplayName("Null operation handling")
  void testNullOperationHandling() {
    try {
      PanamaErrorHandler.checkErrorCode(-1, (String) null);
      fail("Should have thrown exception");
    } catch (final CompilationException e) {
      // Should use default message
      assertNotNull(e.getMessage());
      assertFalse(e.getMessage().isEmpty());
    } catch (final WasmException e) {
      fail("Unexpected exception type: " + e.getClass().getName());
    }

    try {
      PanamaErrorHandler.checkErrorCode(-1, "");
      fail("Should have thrown exception");
    } catch (final CompilationException e) {
      // Should handle empty operation
      assertNotNull(e.getMessage());
    } catch (final WasmException e) {
      fail("Unexpected exception type: " + e.getClass().getName());
    }
  }

  @Test
  @DisplayName("Error struct null pointer handling")
  void testErrorStructNullHandling() {
    // Null error struct should not throw (indicates no error)
    assertDoesNotThrow(
        () -> {
          PanamaErrorHandler.checkErrorStruct(null, "test operation", null);
        });
  }

  @Test
  @DisplayName("Safe error checking")
  void testSafeErrorChecking() {
    // Valid error code should throw normally
    assertThrows(
        CompilationException.class,
        () -> {
          PanamaErrorHandler.safeCheckError(-1, "test operation", "fallback message");
        });

    // Should handle unexpected exceptions gracefully
    assertDoesNotThrow(
        () -> {
          PanamaErrorHandler.safeCheckError(0, "success operation", "fallback message");
        });
  }

  @Test
  @DisplayName("Recoverable error identification")
  void testRecoverableErrorIdentification() {
    // Memory errors should be recoverable
    assertTrue(PanamaErrorHandler.isRecoverableError(-7));

    // Invalid parameter errors should be recoverable
    assertTrue(PanamaErrorHandler.isRecoverableError(-13));

    // Resource errors should be recoverable
    assertTrue(PanamaErrorHandler.isRecoverableError(-11));

    // Compilation errors should not be recoverable
    assertFalse(PanamaErrorHandler.isRecoverableError(-1));

    // Validation errors should not be recoverable
    assertFalse(PanamaErrorHandler.isRecoverableError(-2));

    // Internal errors should not be recoverable
    assertFalse(PanamaErrorHandler.isRecoverableError(-18));

    // Success should not be recoverable (not an error)
    assertFalse(PanamaErrorHandler.isRecoverableError(0));

    // Unknown errors should not be recoverable
    assertFalse(PanamaErrorHandler.isRecoverableError(-999));
  }

  @Test
  @DisplayName("Parameter validation methods")
  void testParameterValidation() {
    // Valid pointer (mock with non-null value)
    // Note: We can't easily test MemorySegment without native code,
    // so we test the other validation methods

    // Positive values
    assertEquals(10L, PanamaErrorHandler.requirePositive(10L, "test"));
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          PanamaErrorHandler.requirePositive(0L, "test");
        });
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          PanamaErrorHandler.requirePositive(-1L, "test");
        });

    // Non-negative values
    assertEquals(0L, PanamaErrorHandler.requireNonNegative(0L, "test"));
    assertEquals(10L, PanamaErrorHandler.requireNonNegative(10L, "test"));
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          PanamaErrorHandler.requireNonNegative(-1L, "test");
        });

    assertEquals(0, PanamaErrorHandler.requireNonNegative(0, "test"));
    assertEquals(10, PanamaErrorHandler.requireNonNegative(10, "test"));
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          PanamaErrorHandler.requireNonNegative(-1, "test");
        });

    // Valid index
    assertEquals(0, PanamaErrorHandler.requireValidIndex(0, 5, "test"));
    assertEquals(4, PanamaErrorHandler.requireValidIndex(4, 5, "test"));
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> {
          PanamaErrorHandler.requireValidIndex(-1, 5, "test");
        });
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> {
          PanamaErrorHandler.requireValidIndex(5, 5, "test");
        });

    // Non-empty strings
    assertEquals("hello", PanamaErrorHandler.requireNotEmpty("hello", "test"));
    assertEquals("hello", PanamaErrorHandler.requireNonEmpty("hello", "test"));

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          PanamaErrorHandler.requireNotEmpty("", "test");
        });
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          PanamaErrorHandler.requireNotEmpty(null, "test");
        });
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          PanamaErrorHandler.requireNotEmpty("   ", "test");
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          PanamaErrorHandler.requireNonEmpty("", "test");
        });
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          PanamaErrorHandler.requireNonEmpty(null, "test");
        });
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          PanamaErrorHandler.requireNonEmpty("   ", "test");
        });
  }

  @Test
  @DisplayName("Result validation")
  void testResultValidation() {
    // Valid result
    String result = PanamaErrorHandler.requireSuccess("hello", "test operation");
    assertEquals("hello", result);

    // Null result should throw
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          PanamaErrorHandler.requireSuccess(null, "test operation");
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          PanamaErrorHandler.requireSuccess(null, null);
        });
  }

  @Test
  @DisplayName("Detailed error message creation")
  void testDetailedErrorMessageCreation() {
    // Full context
    String message1 =
        PanamaErrorHandler.createDetailedErrorMessage("operation", "context", "cause");
    assertEquals("operation (context): cause", message1);

    // No context
    String message2 = PanamaErrorHandler.createDetailedErrorMessage("operation", null, "cause");
    assertEquals("operation: cause", message2);

    // No operation
    String message3 = PanamaErrorHandler.createDetailedErrorMessage(null, "context", "cause");
    assertEquals("context: cause", message3);

    // Only cause
    String message4 = PanamaErrorHandler.createDetailedErrorMessage(null, null, "cause");
    assertEquals("cause", message4);

    // All null
    String message5 = PanamaErrorHandler.createDetailedErrorMessage(null, null, null);
    assertEquals("Native operation failed", message5);

    // Empty strings
    String message6 = PanamaErrorHandler.createDetailedErrorMessage("", "", "");
    assertEquals("Native operation failed", message6);

    // Whitespace strings
    String message7 = PanamaErrorHandler.createDetailedErrorMessage("   ", "   ", "   ");
    assertEquals("Native operation failed", message7);
  }

  @Test
  @DisplayName("Exception mapping functionality")
  void testExceptionMapping() {
    // Test mapping RuntimeException
    RuntimeException runtime = new RuntimeException("runtime error");
    WasmException mapped1 = PanamaErrorHandler.mapToWasmException(runtime, "test context");
    assertTrue(mapped1.getMessage().contains("test context"));
    assertTrue(mapped1.getMessage().contains("runtime error"));

    // Test mapping with null context
    WasmException mapped2 = PanamaErrorHandler.mapToWasmException(runtime, null);
    assertNotNull(mapped2);

    // Test mapping WasmException (should return same)
    CompilationException compilation = new CompilationException("compilation error");
    WasmException mapped3 = PanamaErrorHandler.mapToWasmException(compilation, "test context");
    assertEquals(compilation, mapped3);

    // Test mapping Throwable
    Throwable throwable = new Throwable("generic throwable");
    WasmException mapped4 = PanamaErrorHandler.mapToWasmException(throwable, "throwable context");
    assertTrue(mapped4.getMessage().contains("throwable context"));
  }

  @Test
  @DisplayName("Edge case error codes")
  void testEdgeCaseErrorCodes() {
    // Test unknown negative error code
    String desc1 = PanamaErrorHandler.getErrorDescription(-999);
    assertTrue(desc1.startsWith("Unknown Error"));
    assertTrue(desc1.contains("-999"));

    // Test positive error code
    String desc2 = PanamaErrorHandler.getErrorDescription(123);
    assertTrue(desc2.startsWith("Unknown Error"));
    assertTrue(desc2.contains("123"));

    // Test extreme values
    String desc3 = PanamaErrorHandler.getErrorDescription(Integer.MAX_VALUE);
    assertTrue(desc3.startsWith("Unknown Error"));

    String desc4 = PanamaErrorHandler.getErrorDescription(Integer.MIN_VALUE);
    assertTrue(desc4.startsWith("Unknown Error"));

    // Test just outside valid range
    String desc5 = PanamaErrorHandler.getErrorDescription(-19);
    assertTrue(desc5.startsWith("Unknown Error"));
  }

  @Test
  @DisplayName("Thread safety of error handling")
  void testThreadSafety() throws InterruptedException {
    int numThreads = 10;
    Thread[] threads = new Thread[numThreads];
    Exception[] exceptions = new Exception[numThreads];

    for (int i = 0; i < numThreads; i++) {
      final int threadId = i;
      threads[i] =
          new Thread(
              () -> {
                try {
                  for (int j = 0; j < 100; j++) {
                    int errorCode = -1 - (j % 18);

                    // Test error description
                    String desc = PanamaErrorHandler.getErrorDescription(errorCode);
                    assertNotNull(desc);
                    assertFalse(desc.startsWith("Unknown Error"));

                    // Test recoverable check
                    PanamaErrorHandler.isRecoverableError(errorCode);

                    // Test validation methods
                    PanamaErrorHandler.requireNonNegative(j, "thread test");
                    String message =
                        PanamaErrorHandler.createDetailedErrorMessage(
                            "Thread " + threadId, "iteration " + j, "test");
                    assertNotNull(message);
                  }
                } catch (Exception e) {
                  exceptions[threadId] = e;
                }
              });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    for (int i = 0; i < numThreads; i++) {
      assertNull(
          exceptions[i],
          "Thread "
              + i
              + " should not have failed: "
              + (exceptions[i] != null ? exceptions[i].getMessage() : ""));
    }
  }

  @Test
  @DisplayName("Memory safety for large inputs")
  void testMemorySafetyLargeInputs() {
    // Test very large strings don't cause issues
    String longString = "x".repeat(100000);

    String result =
        PanamaErrorHandler.createDetailedErrorMessage(longString, longString, longString);
    assertNotNull(result);
    assertTrue(result.contains("x"));

    // Test validation with long parameter names
    assertDoesNotThrow(
        () -> {
          PanamaErrorHandler.requireNonNegative(5, longString);
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          PanamaErrorHandler.requireNonNegative(-1, longString);
        });
  }
}

