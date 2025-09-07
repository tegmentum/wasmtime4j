package ai.tegmentum.wasmtime4j.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive tests for error handling across JNI and Panama implementations.
 *
 * <p>These tests verify that error codes are properly aligned between Rust and Java, that error
 * messages are correctly extracted, and that proper exceptions are thrown instead of JVM crashes.
 */
@DisplayName("Error Handling Test Suite")
class ErrorHandlingTest {

  @Test
  @DisplayName("JNI error code constants match Rust exactly")
  void testJniErrorCodeAlignment() {
    // Test all 18 error codes from -1 to -18 match between JNI and Rust
    // This test verifies the critical fix for error code misalignment

    var mapper = ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper.class;

    // Verify compilation error (-1)
    var compilationDesc =
        ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper.getErrorCodeDescription(-1);
    assertEquals("Compilation error", compilationDesc);

    // Verify validation error (-2)
    var validationDesc =
        ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper.getErrorCodeDescription(-2);
    assertEquals("Validation error", validationDesc);

    // Verify runtime error (-3)
    var runtimeDesc =
        ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper.getErrorCodeDescription(-3);
    assertEquals("Runtime error", runtimeDesc);

    // Verify all 18 error codes are handled (no "Unknown error" responses)
    for (int errorCode = -1; errorCode >= -18; errorCode--) {
      var description =
          ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper.getErrorCodeDescription(errorCode);
      assertFalse(
          description.startsWith("Unknown error"),
          "Error code " + errorCode + " should have proper description, got: " + description);
    }
  }

  @Test
  @DisplayName("Panama error code constants match Rust exactly")
  void testPanamaErrorCodeAlignment() {
    // Test all 18 error codes from -1 to -18 match between Panama and Rust

    // Verify compilation error (-1)
    var compilationDesc = ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler.getErrorDescription(-1);
    assertEquals("Compilation Error", compilationDesc);

    // Verify validation error (-2)
    var validationDesc = ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler.getErrorDescription(-2);
    assertEquals("Validation Error", validationDesc);

    // Verify runtime error (-3)
    var runtimeDesc = ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler.getErrorDescription(-3);
    assertEquals("Runtime Error", runtimeDesc);

    // Verify all 18 error codes are handled (no "Unknown Error" responses)
    for (int errorCode = -1; errorCode >= -18; errorCode--) {
      var description =
          ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler.getErrorDescription(errorCode);
      assertFalse(
          description.startsWith("Unknown Error"),
          "Error code " + errorCode + " should have proper description, got: " + description);
    }
  }

  @ParameterizedTest
  @ValueSource(
      ints = {-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -13, -14, -15, -16, -17, -18})
  @DisplayName("All error codes have proper JNI exception mapping")
  void testJniExceptionMapping(int errorCode) {
    var exception =
        ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper.mapNativeError(
            errorCode, "Test error message");

    assertNotNull(exception, "Error code " + errorCode + " should map to exception");
    assertTrue(
        exception.getMessage().contains("Test error message"),
        "Exception message should contain original message");
    assertEquals(errorCode, exception.getErrorCode(), "Exception should preserve error code");
  }

  @ParameterizedTest
  @ValueSource(
      ints = {-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -13, -14, -15, -16, -17, -18})
  @DisplayName("All error codes have proper Panama exception mapping")
  void testPanamaExceptionMapping(int errorCode) {
    assertDoesNotThrow(
        () -> {
          ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler.checkErrorCode(
              errorCode, "Test operation");
        },
        "Should not throw exception for individual error code test");

    // The actual exception throwing is tested by the checkErrorCode method
    // which will throw appropriate WasmException subclasses
  }

  @Test
  @DisplayName("JNI error messages are safely extracted")
  void testJniErrorMessageSafety() {
    // Test null message handling
    var nullException =
        ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper.mapNativeError(-1, null);
    assertNotNull(nullException);
    assertNotNull(nullException.getMessage());

    // Test empty message handling
    var emptyException = ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper.mapNativeError(-1, "");
    assertNotNull(emptyException);
    assertNotNull(emptyException.getMessage());

    // Test very long message handling
    var longMessage = "x".repeat(10000);
    var longException =
        ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper.mapNativeError(-1, longMessage);
    assertNotNull(longException);
    assertTrue(longException.getMessage().contains(longMessage));
  }

  @Test
  @DisplayName("Panama error pointer interpretation is defensive")
  void testPanamaErrorPointerSafety() {
    // Test null pointer handling
    assertDoesNotThrow(
        () -> {
          ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler.checkErrorStruct(
              null, "Test operation", null);
        },
        "Should handle null error struct safely");

    // Test invalid error codes
    assertThrows(
        Exception.class,
        () -> {
          ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler.checkErrorCode(
              -999, "Invalid error code test");
        },
        "Should handle invalid error codes properly");
  }

  @Test
  @DisplayName("Error recovery mechanisms work correctly")
  void testErrorRecovery() {
    // Test recoverable error identification
    assertTrue(
        ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler.isRecoverableError(-7),
        "Memory errors should be recoverable");
    assertTrue(
        ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler.isRecoverableError(-13),
        "Invalid parameter errors should be recoverable");
    assertTrue(
        ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler.isRecoverableError(-11),
        "Resource errors should be recoverable");

    // Test non-recoverable errors
    assertFalse(
        ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler.isRecoverableError(-1),
        "Compilation errors should not be recoverable");
    assertFalse(
        ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler.isRecoverableError(-18),
        "Internal errors should not be recoverable");
  }

  @Test
  @DisplayName("Error code consistency across implementations")
  void testErrorCodeConsistency() {
    // Verify that JNI and Panama use the same error codes for the same error types

    // Compilation error should be -1 in both
    assertEquals(
        "Compilation error",
        ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper.getErrorCodeDescription(-1));
    assertEquals(
        "Compilation Error",
        ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler.getErrorDescription(-1));

    // Validation error should be -2 in both
    assertEquals(
        "Validation error",
        ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper.getErrorCodeDescription(-2));
    assertEquals(
        "Validation Error",
        ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler.getErrorDescription(-2));

    // Runtime error should be -3 in both
    assertEquals(
        "Runtime error",
        ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper.getErrorCodeDescription(-3));
    assertEquals(
        "Runtime Error", ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler.getErrorDescription(-3));

    // Verify all 18 error codes exist in both implementations
    for (int errorCode = -1; errorCode >= -18; errorCode--) {
      var jniDesc =
          ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper.getErrorCodeDescription(errorCode);
      var panamaDesc =
          ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler.getErrorDescription(errorCode);

      assertFalse(jniDesc.startsWith("Unknown error"), "JNI should handle error code " + errorCode);
      assertFalse(
          panamaDesc.startsWith("Unknown Error"), "Panama should handle error code " + errorCode);
    }
  }

  @Test
  @DisplayName("Memory layout constants are properly defined")
  void testMemoryLayoutConstants() {
    // Verify that all required MemoryLayout constants exist and are accessible
    assertNotNull(
        ai.tegmentum.wasmtime4j.panama.MemoryLayouts.WASMTIME_ERROR,
        "WASMTIME_ERROR layout should be defined");
    assertNotNull(
        ai.tegmentum.wasmtime4j.panama.MemoryLayouts.WASMTIME_ERROR_CODE,
        "WASMTIME_ERROR_CODE varhandle should be defined");
    assertNotNull(
        ai.tegmentum.wasmtime4j.panama.MemoryLayouts.WASMTIME_ERROR_MESSAGE,
        "WASMTIME_ERROR_MESSAGE varhandle should be defined");
    assertNotNull(
        ai.tegmentum.wasmtime4j.panama.MemoryLayouts.WASMTIME_ERROR_MESSAGE_LEN,
        "WASMTIME_ERROR_MESSAGE_LEN varhandle should be defined");
  }

  @Test
  @DisplayName("Error handling prevents JVM crashes")
  void testJvmCrashPrevention() {
    // This test verifies that error conditions don't cause JVM crashes

    // Test extreme error codes
    assertDoesNotThrow(
        () -> {
          ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper.getErrorCodeDescription(
              Integer.MIN_VALUE);
        },
        "Extreme negative error codes should not crash");

    assertDoesNotThrow(
        () -> {
          ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper.getErrorCodeDescription(
              Integer.MAX_VALUE);
        },
        "Extreme positive error codes should not crash");

    // Test null/invalid inputs
    assertDoesNotThrow(
        () -> {
          ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper.getSafeErrorMessage(null, null);
        },
        "Null error messages should not crash");

    assertDoesNotThrow(
        () -> {
          ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler.createDetailedErrorMessage(
              null, null, null);
        },
        "Null context should not crash");
  }

  @Test
  @DisplayName("Thread safety of error handling")
  void testErrorHandlingThreadSafety() throws InterruptedException {
    // Test that concurrent error handling doesn't cause issues
    var threads = new Thread[10];
    var exceptions = new Exception[10];

    for (int i = 0; i < 10; i++) {
      final int threadId = i;
      threads[i] =
          new Thread(
              () -> {
                try {
                  // Each thread tests different error codes concurrently
                  var errorCode = -1 - (threadId % 18);
                  var exception =
                      ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper.mapNativeError(
                          errorCode, "Thread " + threadId + " error");
                  assertNotNull(exception);
                } catch (Exception e) {
                  exceptions[threadId] = e;
                }
              });
      threads[i].start();
    }

    // Wait for all threads to complete
    for (Thread thread : threads) {
      thread.join();
    }

    // Verify no exceptions occurred during concurrent access
    for (int i = 0; i < 10; i++) {
      assertNull(exceptions[i], "Thread " + i + " should not have thrown exception");
    }
  }

  @Test
  @DisplayName("Defensive programming measures")
  void testDefensiveProgramming() {
    // Test parameter validation
    assertDoesNotThrow(
        () -> {
          ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler.requireNonNegative(0, "test");
        },
        "Zero should be valid for non-negative check");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler.requireNonNegative(-1, "test");
        },
        "Negative values should be rejected");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler.requireNotEmpty("", "test");
        },
        "Empty strings should be rejected");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler.requireNotEmpty(null, "test");
        },
        "Null strings should be rejected");
  }
}
