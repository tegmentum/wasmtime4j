package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.exception.WasmRuntimeException;
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
      WasmException exception = JniExceptionMapper.mapNativeError(errorCode, "test message");

      assertNotNull(exception, "Error code " + errorCode + " should map to exception");
      assertTrue(
          exception.getMessage().contains("test message"),
          "Exception should contain original message");
    }
  }

  @ParameterizedTest
  @CsvSource({
    "-1, WebAssembly compilation failed",
    "-2, WebAssembly module validation failed",
    "-3, WebAssembly runtime error",
    "-4, Engine configuration error",
    "-5, Store error",
    "-6, Instance error",
    "-7, Memory access or allocation error",
    "-8, Function invocation error",
    "-9, Import or export resolution error",
    "-10, Type conversion or validation error",
    "-11, Resource management error",
    "-12, I/O operation error",
    "-13, Invalid parameter",
    "-14, Threading or concurrency error",
    "-15, WASI error",
    "-16, Security and permission violation error",
    "-17, Component model error",
    "-18, Interface definition or binding error"
  })
  void testErrorCodeMessageMapping(int errorCode, String expectedMessagePrefix) {
    WasmException exception = JniExceptionMapper.mapNativeError(errorCode, "test");
    assertTrue(
        exception.getMessage().contains(expectedMessagePrefix),
        "Error code "
            + errorCode
            + " should contain '"
            + expectedMessagePrefix
            + "' but got: "
            + exception.getMessage());
  }

  @Test
  void testResourceExceptionTypes() {
    // Memory error (-7) should return WasmRuntimeException with memory error message
    WasmException memoryException = JniExceptionMapper.mapNativeError(-7, "memory test");
    assertTrue(
        memoryException instanceof WasmRuntimeException,
        "Memory errors should produce WasmRuntimeException");
    assertTrue(
        memoryException.getMessage().contains("Memory access or allocation error"),
        "Memory errors should indicate memory access error");

    // Resource error (-11) should return ResourceException with resource error message
    WasmException resourceException = JniExceptionMapper.mapNativeError(-11, "resource test");
    assertTrue(
        resourceException.getMessage().contains("Resource management error"),
        "Resource errors should indicate resource error");

    // Compilation error (-1) should use CompilationException
    WasmException compilationException = JniExceptionMapper.mapNativeError(-1, "compilation test");
    assertTrue(
        compilationException instanceof CompilationException,
        "Compilation errors should produce CompilationException");
  }

  @Test
  void testNullMessageHandling() {
    // Test null message — null is replaced with "Unknown native error" before delegation
    WasmException nullException = JniExceptionMapper.mapNativeError(-1, null);
    assertNotNull(nullException.getMessage());
    assertFalse(nullException.getMessage().isEmpty());
    assertTrue(
        nullException instanceof CompilationException,
        "Should still produce CompilationException with null message");

    // Test empty message
    WasmException emptyException = JniExceptionMapper.mapNativeError(-1, "");
    assertNotNull(emptyException.getMessage());
    assertFalse(emptyException.getMessage().isEmpty());

    // Test whitespace-only message
    WasmException whitespaceException = JniExceptionMapper.mapNativeError(-1, "   ");
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
                    WasmException exception =
                        JniExceptionMapper.mapNativeError(
                            errorCode, "Thread " + threadId + " iteration " + j);
                    assertNotNull(exception);
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
    WasmException exception = JniExceptionMapper.mapNativeError(-1, longMessage);

    assertNotNull(exception);
    assertTrue(exception.getMessage().contains(longMessage));

    // Test message with special characters
    String specialMessage = "Error with special chars: (C) (R) (TM) and newlines\n\r\ttabs";
    WasmException specialException = JniExceptionMapper.mapNativeError(-1, specialMessage);

    assertNotNull(specialException);
    assertTrue(specialException.getMessage().contains(specialMessage));
  }

  @Test
  void testEdgeCaseErrorCodes() {
    // Test boundary values
    WasmException exception1 = JniExceptionMapper.mapNativeError(1, "positive error code");
    assertNotNull(exception1);
    assertTrue(exception1.getMessage().contains("Unknown native error"));

    WasmException exception2 = JniExceptionMapper.mapNativeError(Integer.MAX_VALUE, "max int");
    assertNotNull(exception2);
    assertTrue(exception2.getMessage().contains("Unknown native error"));

    WasmException exception3 = JniExceptionMapper.mapNativeError(Integer.MIN_VALUE, "min int");
    assertNotNull(exception3);
    assertTrue(exception3.getMessage().contains("Unknown native error"));

    // Test just outside valid range (-27 is beyond the -26 maximum)
    WasmException exception4 = JniExceptionMapper.mapNativeError(-27, "beyond range");
    assertNotNull(exception4);
    assertTrue(exception4.getMessage().contains("Unknown native error"));
  }
}
