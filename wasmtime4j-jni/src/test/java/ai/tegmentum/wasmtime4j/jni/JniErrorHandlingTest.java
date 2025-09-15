package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Comprehensive tests for JNI-specific error handling.
 *
 * <p>These tests verify that JNI error codes are properly aligned with Rust, that proper exceptions
 * are thrown instead of returning 0, and that all error scenarios are handled defensively.
 */
class JniErrorHandlingTest {

  @Test
  void testAllErrorCodesMapped() {
    // Test that all Rust error codes (-1 to -18) are properly handled
    for (int errorCode = -1; errorCode >= -18; errorCode--) {
      JniException exception = JniExceptionMapper.mapNativeError(errorCode, "test message");

      assertNotNull(exception, "Error code " + errorCode + " should map to exception");
      assertEquals(errorCode, exception.getNativeErrorCode(), "Error code should be preserved");
      assertTrue(
          exception.getMessage().contains("test message"),
          "Exception should contain original message");
    }
  }

  @ParameterizedTest
  @CsvSource({
    "-1, Compilation failed",
    "-2, Validation failed",
    "-3, Runtime error",
    "-4, Engine configuration error",
    "-5, Store error",
    "-6, Instance error",
    "-7, Memory access error",
    "-8, Function invocation failed",
    "-9, Import/Export error",
    "-10, Type error",
    "-11, Resource error",
    "-12, I/O error",
    "-13, Invalid parameter",
    "-14, Concurrency error",
    "-15, WASI error",
    "-16, Component error",
    "-17, Interface error",
    "-18, Internal error"
  })
  void testErrorCodeMessageMapping(int errorCode, String expectedMessagePrefix) {
    JniException exception = JniExceptionMapper.mapNativeError(errorCode, "test");
    assertTrue(
        exception.getMessage().startsWith(expectedMessagePrefix),
        "Error code "
            + errorCode
            + " should start with '"
            + expectedMessagePrefix
            + "' but got: "
            + exception.getMessage());
  }

  @Test
  void testResourceExceptionTypes() {
    // Memory error (-7) should use JniResourceException
    JniException memoryException = JniExceptionMapper.mapNativeError(-7, "memory test");
    assertInstanceOf(
        JniResourceException.class,
        memoryException,
        "Memory errors should use JniResourceException");

    // Resource error (-11) should use JniResourceException
    JniException resourceException = JniExceptionMapper.mapNativeError(-11, "resource test");
    assertInstanceOf(
        JniResourceException.class,
        resourceException,
        "Resource errors should use JniResourceException");

    // Compilation error (-1) should use regular JniException
    JniException compilationException = JniExceptionMapper.mapNativeError(-1, "compilation test");
    assertEquals(
        JniException.class,
        compilationException.getClass(),
        "Compilation errors should use regular JniException");
  }

  @Test
  void testErrorCodeDescriptions() {
    for (int errorCode = -1; errorCode >= -18; errorCode--) {
      String description = JniExceptionMapper.getErrorCodeDescription(errorCode);

      assertNotNull(description, "Error code " + errorCode + " should have description");
      assertFalse(description.isEmpty(), "Description should not be empty");
      assertFalse(
          description.startsWith("Unknown error"),
          "Error code " + errorCode + " should not be unknown");
    }

    // Test success case
    assertEquals("No error", JniExceptionMapper.getErrorCodeDescription(0));

    // Test unknown error code
    String unknownDesc = JniExceptionMapper.getErrorCodeDescription(-999);
    assertTrue(
        unknownDesc.startsWith("Unknown error"),
        "Unknown error codes should be identified as such");
  }

  @Test
  void testNullMessageHandling() {
    // Test null message
    JniException nullException = JniExceptionMapper.mapNativeError(-1, null);
    assertNotNull(nullException.getMessage());
    assertFalse(nullException.getMessage().isEmpty());
    assertTrue(nullException.getMessage().contains("Unknown native error"));

    // Test empty message
    JniException emptyException = JniExceptionMapper.mapNativeError(-1, "");
    assertNotNull(emptyException.getMessage());
    assertFalse(emptyException.getMessage().isEmpty());

    // Test whitespace-only message
    JniException whitespaceException = JniExceptionMapper.mapNativeError(-1, "   ");
    assertNotNull(whitespaceException.getMessage());
    assertFalse(whitespaceException.getMessage().trim().isEmpty());
  }

  @Test
  void testNativeHandleValidation() {
    // Valid handle should not throw
    assertDoesNotThrow(
        () -> {
          JniExceptionMapper.validateNativeHandle(12345L, "test resource");
        });

    // Zero handle should throw
    JniResourceException exception =
        assertThrows(
            JniResourceException.class,
            () -> {
              JniExceptionMapper.validateNativeHandle(0L, "test resource");
            });
    assertTrue(exception.getMessage().contains("test resource"));
    assertTrue(exception.getMessage().contains("null"));

    // Null resource type should work
    assertThrows(
        JniResourceException.class,
        () -> {
          JniExceptionMapper.validateNativeHandle(0L, null);
        });
  }

  @Test
  void testNativeResultValidation() {
    // True result should not throw
    assertDoesNotThrow(
        () -> {
          JniExceptionMapper.validateNativeResult(true, "test operation");
        });

    // False result should throw
    JniException exception =
        assertThrows(
            JniException.class,
            () -> {
              JniExceptionMapper.validateNativeResult(false, "test operation");
            });
    assertTrue(exception.getMessage().contains("test operation"));

    // Null operation should work
    assertThrows(
        JniException.class,
        () -> {
          JniExceptionMapper.validateNativeResult(false, null);
        });
  }

  @Test
  void testSafeErrorMessageExtraction() {
    // Valid message
    String result1 = JniExceptionMapper.getSafeErrorMessage("valid message", "default");
    assertEquals("valid message", result1);

    // Null message with default
    String result2 = JniExceptionMapper.getSafeErrorMessage(null, "default message");
    assertEquals("default message", result2);

    // Empty message with default
    String result3 = JniExceptionMapper.getSafeErrorMessage("", "default message");
    assertEquals("default message", result3);

    // Whitespace message with default
    String result4 = JniExceptionMapper.getSafeErrorMessage("   ", "default message");
    assertEquals("default message", result4);

    // Null message and null default
    String result5 = JniExceptionMapper.getSafeErrorMessage(null, null);
    assertEquals("Unknown error", result5);
  }

  @Test
  void testNativeExceptionWrapping() {
    RuntimeException cause = new RuntimeException("Original error");

    JniException wrapped = JniExceptionMapper.wrapNativeException("test operation", cause);

    assertNotNull(wrapped);
    assertTrue(wrapped.getMessage().contains("test operation"));
    assertEquals(cause, wrapped.getCause());

    // Test null operation
    JniException wrapped2 = JniExceptionMapper.wrapNativeException(null, cause);
    assertNotNull(wrapped2);
    assertTrue(wrapped2.getMessage().contains("Native operation failed"));
  }

  @Test
  void testCleanupExceptionCreation() {
    // Test with cause
    RuntimeException cause = new RuntimeException("cleanup failed");
    JniResourceException cleanupEx = JniExceptionMapper.createCleanupException("engine", cause);

    assertNotNull(cleanupEx);
    assertTrue(cleanupEx.getMessage().contains("engine"));
    assertEquals(cause, cleanupEx.getCause());

    // Test without cause
    JniResourceException cleanupEx2 = JniExceptionMapper.createCleanupException("module", null);
    assertNotNull(cleanupEx2);
    assertTrue(cleanupEx2.getMessage().contains("module"));
    assertNull(cleanupEx2.getCause());

    // Test null resource type
    JniResourceException cleanupEx3 = JniExceptionMapper.createCleanupException(null, null);
    assertNotNull(cleanupEx3);
    assertTrue(cleanupEx3.getMessage().contains("resource"));
  }

  @Test
  void testInvalidStateExceptionCreation() {
    JniException stateEx = JniExceptionMapper.createInvalidStateException("engine", "disposed");

    assertNotNull(stateEx);
    assertTrue(stateEx.getMessage().contains("engine"));
    assertTrue(stateEx.getMessage().contains("disposed"));

    // Test null parameters
    JniException stateEx2 = JniExceptionMapper.createInvalidStateException(null, null);
    assertNotNull(stateEx2);
    assertTrue(stateEx2.getMessage().contains("Resource"));
    assertTrue(stateEx2.getMessage().contains("invalid state"));
  }

  @Test
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
                    JniException exception =
                        JniExceptionMapper.mapNativeError(
                            errorCode, "Thread " + threadId + " iteration " + j);
                    assertNotNull(exception);
                    assertEquals(errorCode, exception.getNativeErrorCode());
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
      assertNull(exceptions[i], "Thread " + i + " should not have failed");
    }
  }

  @Test
  void testLargeMessageHandling() {
    // Test very long error message
    StringBuilder sb = new StringBuilder(10000);
    for (int i = 0; i < 10000; i++) {
      sb.append("x");
    }
    String longMessage = sb.toString();
    JniException exception = JniExceptionMapper.mapNativeError(-1, longMessage);

    assertNotNull(exception);
    assertTrue(exception.getMessage().contains(longMessage));

    // Test message with special characters
    String specialMessage = "Error with special chars: (C) (R) (TM) and newlines\n\r\ttabs";
    JniException specialException = JniExceptionMapper.mapNativeError(-1, specialMessage);

    assertNotNull(specialException);
    assertTrue(specialException.getMessage().contains(specialMessage));
  }

  @Test
  void testEdgeCaseErrorCodes() {
    // Test boundary values
    JniException exception1 = JniExceptionMapper.mapNativeError(1, "positive error code");
    assertNotNull(exception1);
    assertTrue(exception1.getMessage().contains("Unknown native error"));

    JniException exception2 = JniExceptionMapper.mapNativeError(Integer.MAX_VALUE, "max int");
    assertNotNull(exception2);
    assertTrue(exception2.getMessage().contains("Unknown native error"));

    JniException exception3 = JniExceptionMapper.mapNativeError(Integer.MIN_VALUE, "min int");
    assertNotNull(exception3);
    assertTrue(exception3.getMessage().contains("Unknown native error"));

    // Test just outside valid range
    JniException exception4 = JniExceptionMapper.mapNativeError(-19, "beyond range");
    assertNotNull(exception4);
    assertTrue(exception4.getMessage().contains("Unknown native error"));
  }
}
