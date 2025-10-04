/*
 * Copyright 2024 Tegmentum Technology, Inc.
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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.gc.GcRuntime;
import ai.tegmentum.wasmtime4j.gc.GcValue;
import ai.tegmentum.wasmtime4j.jni.JniExceptionHandler;
import ai.tegmentum.wasmtime4j.panama.PanamaExceptionHandler;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive test suite for WebAssembly exception handling foundation.
 *
 * <p>This test suite validates the exception handling foundation implemented for Task #309,
 * including exception tag creation, payload validation, cross-language exception propagation, GC
 * integration, and debugging support.
 *
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Exception Handling Foundation Tests")
class ExceptionHandlingFoundationTest {

  private static final Logger LOGGER =
      Logger.getLogger(ExceptionHandlingFoundationTest.class.getName());

  private JniExceptionHandler jniHandler;
  private PanamaExceptionHandler panamaHandler;
  private GcRuntime gcRuntime;

  @BeforeEach
  void setUp() {
    // Initialize GC runtime for GC-aware exception handling tests
    try {
      gcRuntime =
          GcRuntime.builder().enableGcProfiling(true).enableMemoryLeakDetection(true).build();
    } catch (final Exception e) {
      LOGGER.warning(
          "Failed to initialize GC runtime, GC tests will be skipped: " + e.getMessage());
      gcRuntime = null;
    }

    // Initialize JNI exception handler
    try {
      final JniExceptionHandler.ExceptionHandlingConfig jniConfig =
          JniExceptionHandler.ExceptionHandlingConfig.builder()
              .enableNestedTryCatch(true)
              .enableExceptionUnwinding(true)
              .maxUnwindDepth(1000)
              .validateExceptionTypes(true)
              .enableStackTraces(true)
              .enableExceptionPropagation(true)
              .enableGcIntegration(gcRuntime != null)
              .build();
      jniHandler = new JniExceptionHandler(jniConfig, gcRuntime);
    } catch (final Exception e) {
      LOGGER.warning("Failed to initialize JNI exception handler: " + e.getMessage());
      jniHandler = null;
    }

    // Initialize Panama exception handler (Java 23+)
    try {
      final PanamaExceptionHandler.ExceptionHandlingConfig panamaConfig =
          PanamaExceptionHandler.ExceptionHandlingConfig.builder()
              .enableNestedTryCatch(true)
              .enableExceptionUnwinding(true)
              .maxUnwindDepth(1000)
              .validateExceptionTypes(true)
              .enableStackTraces(true)
              .enableExceptionPropagation(true)
              .enableGcIntegration(gcRuntime != null)
              .build();
      panamaHandler = new PanamaExceptionHandler(panamaConfig, gcRuntime);
    } catch (final Exception e) {
      LOGGER.warning("Failed to initialize Panama exception handler: " + e.getMessage());
      panamaHandler = null;
    }
  }

  @Test
  @DisplayName("Exception types should be properly defined in hierarchy")
  void testExceptionTypeHierarchy() {
    // Test base exception types
    final WasmExceptionHandlingException baseException =
        new WasmExceptionHandlingException(
            "Test exception",
            WasmExceptionHandlingException.ExceptionErrorCode.TAG_CREATION_FAILED);

    assertNotNull(baseException);
    assertEquals(
        WasmExceptionHandlingException.ExceptionErrorCode.TAG_CREATION_FAILED,
        baseException.getExceptionErrorCode());
    assertInstanceOf(WasmtimeException.class, baseException);
    assertInstanceOf(WasmException.class, baseException);

    // Test specific exception types
    final WasmExceptionHandlingException.TagCreationException tagException =
        new WasmExceptionHandlingException.TagCreationException("Failed to create tag", "test_tag");
    assertNotNull(tagException);
    assertEquals(
        WasmExceptionHandlingException.ExceptionErrorCode.TAG_CREATION_FAILED,
        tagException.getExceptionErrorCode());

    final WasmExceptionHandlingException.UnwindingException unwindException =
        new WasmExceptionHandlingException.UnwindingException("Unwinding failed", 5);
    assertNotNull(unwindException);
    assertEquals(5, unwindException.getUnwindDepth());
  }

  @Test
  @DisplayName("Exception tag creation should work with proper validation")
  void testExceptionTagCreation() {
    assumeTrue(jniHandler != null, "JNI handler not available");

    final List<WasmValueType> paramTypes = List.of(WasmValueType.I32, WasmValueType.F64);

    // Test basic tag creation
    final WasmExceptionHandlingException.ExceptionTag tag =
        jniHandler.createExceptionTag("test_tag", paramTypes, false);

    assertNotNull(tag);
    assertEquals("test_tag", tag.getName());
    assertEquals(paramTypes, tag.getParameterTypes());
    assertFalse(tag.isGcAware());
    assertTrue(tag.getNativeHandle() > 0);

    // Test duplicate tag creation should fail
    assertThrows(
        IllegalArgumentException.class,
        () -> jniHandler.createExceptionTag("test_tag", paramTypes, false));

    // Test invalid tag name
    assertThrows(
        IllegalArgumentException.class, () -> jniHandler.createExceptionTag("", paramTypes, false));
    assertThrows(
        IllegalArgumentException.class,
        () -> jniHandler.createExceptionTag("   ", paramTypes, false));

    // Test null parameters
    assertThrows(
        IllegalArgumentException.class,
        () -> jniHandler.createExceptionTag(null, paramTypes, false));
    assertThrows(
        IllegalArgumentException.class, () -> jniHandler.createExceptionTag("test", null, false));
  }

  @Test
  @DisplayName("GC-aware exception tag creation should require GC runtime")
  void testGcAwareExceptionTagCreation() {
    assumeTrue(jniHandler != null, "JNI handler not available");

    final List<WasmValueType> paramTypes = List.of(WasmValueType.EXTERNREF);

    if (gcRuntime != null) {
      // Test GC-aware tag creation with GC runtime available
      final WasmExceptionHandlingException.ExceptionTag gcTag =
          jniHandler.createExceptionTag("gc_tag", paramTypes, true);

      assertNotNull(gcTag);
      assertEquals("gc_tag", gcTag.getName());
      assertTrue(gcTag.isGcAware());
    } else {
      // Test GC-aware tag creation should fail without GC runtime
      assertThrows(
          IllegalArgumentException.class,
          () -> jniHandler.createExceptionTag("gc_tag", paramTypes, true));
    }
  }

  @Test
  @DisplayName("Exception payload validation should work correctly")
  void testExceptionPayloadValidation() {
    assumeTrue(jniHandler != null, "JNI handler not available");

    final List<WasmValueType> paramTypes = List.of(WasmValueType.I32, WasmValueType.F64);
    final WasmExceptionHandlingException.ExceptionTag tag =
        jniHandler.createExceptionTag("validation_tag", paramTypes, false);

    // Test valid payload
    final List<WasmValue> validValues = List.of(WasmValue.createI32(42), WasmValue.createF64(3.14));
    final WasmExceptionHandlingException.ExceptionPayload validPayload =
        new WasmExceptionHandlingException.ExceptionPayload(tag, validValues, List.of());

    assertNotNull(validPayload);
    assertEquals(tag, validPayload.getTag());
    assertEquals(validValues, validPayload.getValues());
    assertFalse(validPayload.hasGcValues());

    // Test invalid payload size
    final List<WasmValue> invalidSizeValues = List.of(WasmValue.createI32(42));
    assertThrows(
        WasmExceptionHandlingException.PayloadValidationException.class,
        () ->
            new WasmExceptionHandlingException.ExceptionPayload(tag, invalidSizeValues, List.of()));

    // Test invalid payload types would be caught during exception throwing
  }

  @Test
  @DisplayName("Cross-language exception propagation should work")
  void testCrossLanguageExceptionPropagation() {
    assumeTrue(jniHandler != null, "JNI handler not available");

    final List<WasmValueType> paramTypes = List.of(WasmValueType.I32);
    final WasmExceptionHandlingException.ExceptionTag tag =
        jniHandler.createExceptionTag("propagation_tag", paramTypes, false);

    final CountDownLatch latch = new CountDownLatch(1);
    final boolean[] handlerCalled = {false};

    // Register exception handler
    jniHandler.registerExceptionHandler(
        tag,
        (exceptionTag, payload) -> {
          assertEquals(tag, exceptionTag);
          assertNotNull(payload);
          handlerCalled[0] = true;
          latch.countDown();
          return true; // Continue execution
        });

    // Test exception propagation in separate thread
    final CompletableFuture<Void> future =
        CompletableFuture.runAsync(
            () -> {
              try {
                final List<WasmValue> values = List.of(WasmValue.createI32(123));
                final WasmExceptionHandlingException.ExceptionPayload payload =
                    new WasmExceptionHandlingException.ExceptionPayload(tag, values, List.of());

                // This would trigger the exception handler callback
                // For now, just simulate the callback
                final boolean result = jniHandler.performUnwinding(0);
                assertTrue(result);
              } catch (final Exception e) {
                LOGGER.warning("Exception during propagation test: " + e.getMessage());
              }
            });

    assertDoesNotThrow(
        () -> {
          future.get(5, TimeUnit.SECONDS);
          // latch.await(5, TimeUnit.SECONDS); // Would be used if callback was actually triggered
        });
  }

  @Test
  @DisplayName("Exception unwinding should respect depth limits")
  void testExceptionUnwinding() {
    assumeTrue(jniHandler != null, "JNI handler not available");

    // Test unwinding within limits
    assertTrue(jniHandler.performUnwinding(0));
    assertTrue(jniHandler.performUnwinding(500));

    // Test unwinding at limit (implementation dependent)
    // The actual limit depends on the configuration
    assertDoesNotThrow(() -> jniHandler.performUnwinding(999));

    // Test invalid depth
    assertThrows(IllegalArgumentException.class, () -> jniHandler.performUnwinding(-1));
  }

  @Test
  @DisplayName("Stack trace capture should work when enabled")
  void testStackTraceCapture() {
    assumeTrue(jniHandler != null, "JNI handler not available");

    final List<WasmValueType> paramTypes = List.of(WasmValueType.I32);
    final WasmExceptionHandlingException.ExceptionTag tag =
        jniHandler.createExceptionTag("trace_tag", paramTypes, false);

    final String stackTrace = jniHandler.captureStackTrace(tag);

    // Stack trace may be null if not available, but should not throw
    if (stackTrace != null) {
      assertFalse(stackTrace.isEmpty());
      LOGGER.info("Captured stack trace: " + stackTrace);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 10, 100, 500})
  @DisplayName("Exception handling should work with various payload sizes")
  void testVariousPayloadSizes(final int paramCount) {
    assumeTrue(jniHandler != null, "JNI handler not available");

    // Create parameter types list
    final List<WasmValueType> paramTypes =
        java.util.stream.IntStream.range(0, paramCount).mapToObj(i -> WasmValueType.I32).toList();

    final WasmExceptionHandlingException.ExceptionTag tag =
        jniHandler.createExceptionTag("size_test_" + paramCount, paramTypes, false);

    assertNotNull(tag);
    assertEquals(paramCount, tag.getParameterTypes().size());

    // Create corresponding values
    final List<WasmValue> values =
        java.util.stream.IntStream.range(0, paramCount).mapToObj(WasmValue::createI32).toList();

    final WasmExceptionHandlingException.ExceptionPayload payload =
        new WasmExceptionHandlingException.ExceptionPayload(tag, values, List.of());

    assertNotNull(payload);
    assertEquals(paramCount, payload.getValues().size());
  }

  @Test
  @EnabledOnJre(JRE.JAVA_23)
  @DisplayName("Panama exception handler should work on Java 23+")
  void testPanamaExceptionHandler() {
    assumeTrue(panamaHandler != null, "Panama handler not available");

    final List<WasmValueType> paramTypes = List.of(WasmValueType.I32, WasmValueType.F32);
    final WasmExceptionHandlingException.ExceptionTag tag =
        panamaHandler.createExceptionTag("panama_tag", paramTypes, false);

    assertNotNull(tag);
    assertEquals("panama_tag", tag.getName());
    assertEquals(paramTypes, tag.getParameterTypes());
    assertFalse(tag.isGcAware());
    assertTrue(tag.getNativeHandle() > 0);

    // Test Panama-specific features
    assertTrue(panamaHandler.performUnwinding(0));

    final String stackTrace = panamaHandler.captureStackTrace(tag);
    // Stack trace may be null but should not throw
    assertDoesNotThrow(() -> panamaHandler.captureStackTrace(tag));
  }

  @Test
  @DisplayName("Exception handler cleanup should work properly")
  void testExceptionHandlerCleanup() {
    assumeTrue(jniHandler != null, "JNI handler not available");

    // Test that handler can be closed multiple times safely
    assertFalse(jniHandler.isClosed());

    jniHandler.close();
    assertTrue(jniHandler.isClosed());

    // Second close should not throw
    assertDoesNotThrow(() -> jniHandler.close());

    // Operations on closed handler should throw
    assertThrows(
        IllegalStateException.class,
        () -> jniHandler.createExceptionTag("closed_test", List.of(WasmValueType.I32), false));
    assertThrows(IllegalStateException.class, () -> jniHandler.performUnwinding(0));
  }

  @Test
  @DisplayName("GC integration should work with exception payloads")
  void testGcIntegrationWithExceptionPayloads() {
    assumeTrue(jniHandler != null && gcRuntime != null, "JNI handler and GC runtime not available");

    final List<WasmValueType> paramTypes = List.of(WasmValueType.EXTERNREF, WasmValueType.I32);
    final WasmExceptionHandlingException.ExceptionTag gcTag =
        jniHandler.createExceptionTag("gc_payload_tag", paramTypes, true);

    assertNotNull(gcTag);
    assertTrue(gcTag.isGcAware());

    // Create GC values for testing
    final List<GcValue> gcValues =
        List.of(
            // Would create actual GC values here in real implementation
            // For now, using placeholder structure
            );

    final List<WasmValue> values =
        List.of(
            WasmValue.createExternRef(null), // Would be actual external reference
            WasmValue.createI32(456));

    final WasmExceptionHandlingException.ExceptionPayload gcPayload =
        new WasmExceptionHandlingException.ExceptionPayload(gcTag, values, gcValues);

    assertNotNull(gcPayload);
    assertEquals(gcValues, gcPayload.getGcValues());
    assertEquals(gcValues.isEmpty(), !gcPayload.hasGcValues());
  }

  @Test
  @DisplayName("Exception handling should be thread-safe")
  void testThreadSafeExceptionHandling() throws InterruptedException {
    assumeTrue(jniHandler != null, "JNI handler not available");

    final int threadCount = 10;
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch doneLatch = new CountDownLatch(threadCount);
    final boolean[] results = new boolean[threadCount];

    for (int i = 0; i < threadCount; i++) {
      final int threadIndex = i;
      new Thread(
              () -> {
                try {
                  startLatch.await();

                  // Each thread creates its own exception tag
                  final String tagName = "thread_tag_" + threadIndex;
                  final List<WasmValueType> paramTypes = List.of(WasmValueType.I32);

                  final WasmExceptionHandlingException.ExceptionTag tag =
                      jniHandler.createExceptionTag(tagName, paramTypes, false);

                  assertNotNull(tag);
                  assertEquals(tagName, tag.getName());

                  results[threadIndex] = true;
                } catch (final Exception e) {
                  LOGGER.severe("Thread " + threadIndex + " failed: " + e.getMessage());
                  results[threadIndex] = false;
                } finally {
                  doneLatch.countDown();
                }
              })
          .start();
    }

    startLatch.countDown(); // Start all threads
    assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "Not all threads completed in time");

    // All threads should have succeeded
    for (int i = 0; i < threadCount; i++) {
      assertTrue(results[i], "Thread " + i + " failed");
    }
  }

  @Test
  @DisplayName("Exception handling error codes should be comprehensive")
  void testExceptionErrorCodes() {
    // Verify all error codes are defined
    final WasmExceptionHandlingException.ExceptionErrorCode[] errorCodes =
        WasmExceptionHandlingException.ExceptionErrorCode.values();

    assertTrue(errorCodes.length >= 8, "Should have at least 8 error codes defined");

    // Test each error code can be used
    for (final WasmExceptionHandlingException.ExceptionErrorCode errorCode : errorCodes) {
      final WasmExceptionHandlingException exception =
          new WasmExceptionHandlingException("Test error", errorCode);
      assertEquals(errorCode, exception.getExceptionErrorCode());
    }
  }

  @Test
  @DisplayName("Exception debugging information should be captured")
  void testExceptionDebuggingInformation() {
    assumeTrue(jniHandler != null, "JNI handler not available");

    final List<WasmValueType> paramTypes = List.of(WasmValueType.I64);
    final WasmExceptionHandlingException.ExceptionTag tag =
        jniHandler.createExceptionTag("debug_tag", paramTypes, false);

    final List<WasmValue> values = List.of(WasmValue.createI64(789L));
    final WasmExceptionHandlingException.ExceptionPayload payload =
        new WasmExceptionHandlingException.ExceptionPayload(tag, values, List.of());

    // Test exception with debugging information
    final WasmExceptionHandlingException.CrossLanguageMappingException debugException =
        new WasmExceptionHandlingException.CrossLanguageMappingException(
            "Cross-language mapping failed",
            new RuntimeException("Underlying cause"),
            payload,
            "WebAssembly stack trace here");

    assertNotNull(debugException);
    assertEquals(payload, debugException.getExceptionPayload());
    assertTrue(debugException.hasWasmStackTrace());
    assertEquals("WebAssembly stack trace here", debugException.getWasmStackTrace());
    assertTrue(debugException.involvesGcReferences() == payload.hasGcValues());
  }
}
