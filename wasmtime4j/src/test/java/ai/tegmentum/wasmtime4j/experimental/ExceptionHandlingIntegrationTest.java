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

package ai.tegmentum.wasmtime4j.experimental;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/** Comprehensive integration tests for WebAssembly exception handling. */
final class ExceptionHandlingIntegrationTest {

  private ExceptionHandler handler;

  @BeforeEach
  void setUp(final TestInfo testInfo) {
    // Enable exception handling feature for tests
    ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.EXCEPTION_HANDLING);

    final ExceptionHandler.ExceptionHandlingConfig config =
        ExceptionHandler.ExceptionHandlingConfig.builder()
            .enableNestedTryCatch(true)
            .enableExceptionUnwinding(true)
            .maxUnwindDepth(500)
            .validateExceptionTypes(true)
            .enableStackTraces(true)
            .enableExceptionPropagation(true)
            .build();

    handler = new ExceptionHandler(config);
    System.out.println("Running test: " + testInfo.getDisplayName());
  }

  @AfterEach
  void tearDown() {
    if (handler != null) {
      handler.close();
    }
    ExperimentalFeatures.reset();
  }

  @Test
  void testExceptionHandlerCreation() {
    assertNotNull(handler);
    assertFalse(handler.isClosed());
    assertTrue(handler.getNativeHandle() > 0);
    assertTrue(handler.getHandlerId() > 0);
  }

  @Test
  void testExceptionHandlerConfiguration() {
    final ExceptionHandler.ExceptionHandlingConfig config = handler.getConfig();
    assertNotNull(config);
    assertTrue(config.isNestedTryCatchEnabled());
    assertTrue(config.isExceptionUnwindingEnabled());
    assertEquals(500, config.getMaxUnwindDepth());
    assertTrue(config.isExceptionTypeValidationEnabled());
    assertTrue(config.isStackTracesEnabled());
    assertTrue(config.isExceptionPropagationEnabled());
  }

  @Test
  void testBasicExceptionTagCreation() {
    final List<WasmValueType> parameterTypes = Arrays.asList(WasmValueType.I32, WasmValueType.F64);
    final ExceptionHandler.ExceptionTag tag =
        handler.createExceptionTag("test_exception", parameterTypes);

    assertNotNull(tag);
    assertEquals("test_exception", tag.getName());
    assertEquals(parameterTypes, tag.getParameterTypes());
    assertTrue(tag.getNativeHandle() > 0);
    assertEquals(handler.getHandlerId(), tag.getHandlerId());
  }

  @Test
  void testMultipleExceptionTagCreation() {
    final ExceptionHandler.ExceptionTag tag1 =
        handler.createExceptionTag("tag1", List.of(WasmValueType.I32));
    final ExceptionHandler.ExceptionTag tag2 =
        handler.createExceptionTag("tag2", List.of(WasmValueType.F64));
    final ExceptionHandler.ExceptionTag tag3 =
        handler.createExceptionTag("tag3", List.of(WasmValueType.I64, WasmValueType.F32));

    assertNotNull(tag1);
    assertNotNull(tag2);
    assertNotNull(tag3);

    assertEquals("tag1", tag1.getName());
    assertEquals("tag2", tag2.getName());
    assertEquals("tag3", tag3.getName());

    // Verify tags are different
    assertFalse(tag1.equals(tag2));
    assertFalse(tag2.equals(tag3));
    assertFalse(tag1.equals(tag3));

    // Verify all tags are listed
    final List<ExceptionHandler.ExceptionTag> allTags = handler.listExceptionTags();
    assertEquals(3, allTags.size());
    assertTrue(allTags.contains(tag1));
    assertTrue(allTags.contains(tag2));
    assertTrue(allTags.contains(tag3));
  }

  @Test
  void testDuplicateExceptionTagCreation() {
    handler.createExceptionTag("duplicate_tag", List.of(WasmValueType.I32));

    // Should throw exception for duplicate tag name
    assertThrows(
        IllegalArgumentException.class,
        () -> handler.createExceptionTag("duplicate_tag", List.of(WasmValueType.F64)));
  }

  @Test
  void testExceptionTagRetrieval() {
    final ExceptionHandler.ExceptionTag tag =
        handler.createExceptionTag("retrieve_test", List.of(WasmValueType.I32));

    final ExceptionHandler.ExceptionTag retrievedTag = handler.getExceptionTag("retrieve_test");
    assertNotNull(retrievedTag);
    assertEquals(tag, retrievedTag);

    // Test non-existent tag
    final ExceptionHandler.ExceptionTag nonExistent = handler.getExceptionTag("non_existent");
    assertEquals(null, nonExistent);
  }

  @Test
  void testBasicExceptionThrowing() {
    final ExceptionHandler.ExceptionTag tag =
        handler.createExceptionTag("throw_test", List.of(WasmValueType.I32, WasmValueType.F64));
    final List<WasmValue> payload = Arrays.asList(WasmValue.i32(42), WasmValue.f64(3.14));

    final ExceptionHandler.WasmException exception =
        assertThrows(
            ExceptionHandler.WasmException.class, () -> handler.throwException(tag, payload));

    assertNotNull(exception);
    assertEquals(tag, exception.getTag());
    assertEquals(payload, exception.getPayload());
    assertTrue(exception.getMessage().contains("throw_test"));
  }

  @Test
  void testExceptionThrowingWithStackTrace() {
    final ExceptionHandler.ExceptionTag tag =
        handler.createExceptionTag("stack_trace_test", List.of(WasmValueType.I32));
    final List<WasmValue> payload = List.of(WasmValue.i32(123));

    final ExceptionHandler.WasmException exception =
        assertThrows(
            ExceptionHandler.WasmException.class, () -> handler.throwException(tag, payload));

    assertNotNull(exception.getWasmStackTrace());
    assertTrue(exception.getWasmStackTrace().contains("wasm function"));
  }

  @Test
  void testExceptionCatching() {
    final ExceptionHandler.ExceptionTag tag =
        handler.createExceptionTag("catch_test", List.of(WasmValueType.I32));
    final List<WasmValue> originalPayload = List.of(WasmValue.i32(456));

    final ExceptionHandler.WasmException exception =
        assertThrows(
            ExceptionHandler.WasmException.class,
            () -> handler.throwException(tag, originalPayload));

    final List<WasmValue> caughtPayload = handler.catchException(exception, tag);
    assertEquals(originalPayload, caughtPayload);
  }

  @Test
  void testExceptionCatchingWithWrongTag() {
    final ExceptionHandler.ExceptionTag tag1 =
        handler.createExceptionTag("tag1", List.of(WasmValueType.I32));
    final ExceptionHandler.ExceptionTag tag2 =
        handler.createExceptionTag("tag2", List.of(WasmValueType.I32));

    final ExceptionHandler.WasmException exception =
        assertThrows(
            ExceptionHandler.WasmException.class,
            () -> handler.throwException(tag1, List.of(WasmValue.i32(789))));

    // Should throw exception when trying to catch with wrong tag
    assertThrows(IllegalStateException.class, () -> handler.catchException(exception, tag2));
  }

  @Test
  void testExceptionHandlerRegistration() {
    final ExceptionHandler.ExceptionTag tag =
        handler.createExceptionTag("handler_test", List.of(WasmValueType.I32));

    final boolean[] handlerCalled = {false};
    final ExceptionHandler.ExceptionHandlerFunction handlerFunction =
        (exceptionTag, payload) -> {
          handlerCalled[0] = true;
          assertEquals(tag, exceptionTag);
          assertEquals(1, payload.size());
          assertEquals(999, payload.get(0).i32());
          return true; // Continue execution
        };

    assertDoesNotThrow(() -> handler.registerExceptionHandler(tag, handlerFunction));
  }

  @Test
  void testExceptionUnwinding() {
    // Test basic unwinding
    assertTrue(handler.performUnwinding(0));
    assertTrue(handler.performUnwinding(100));
    assertTrue(handler.performUnwinding(499));

    // Should return false when reaching max depth
    assertFalse(handler.performUnwinding(500));
    assertFalse(handler.performUnwinding(600));
  }

  @Test
  void testExceptionValidation() {
    final ExceptionHandler.ExceptionTag tag =
        handler.createExceptionTag(
            "validation_test", Arrays.asList(WasmValueType.I32, WasmValueType.F64));

    // Valid payload
    final List<WasmValue> validPayload = Arrays.asList(WasmValue.i32(42), WasmValue.f64(3.14));
    assertDoesNotThrow(() -> handler.throwException(tag, validPayload));

    // Invalid payload size
    final List<WasmValue> invalidSizePayload = List.of(WasmValue.i32(42));
    assertThrows(
        IllegalArgumentException.class, () -> handler.throwException(tag, invalidSizePayload));

    // Invalid payload types
    final List<WasmValue> invalidTypePayload =
        Arrays.asList(WasmValue.f32(1.0f), WasmValue.f64(3.14));
    assertThrows(
        IllegalArgumentException.class, () -> handler.throwException(tag, invalidTypePayload));
  }

  @Test
  void testExceptionValidationDisabled() {
    // Create handler with validation disabled
    final ExceptionHandler.ExceptionHandlingConfig config =
        ExceptionHandler.ExceptionHandlingConfig.builder().validateExceptionTypes(false).build();

    try (final ExceptionHandler noValidationHandler = new ExceptionHandler(config)) {
      final ExceptionHandler.ExceptionTag tag =
          noValidationHandler.createExceptionTag("no_validation", List.of(WasmValueType.I32));

      // With validation disabled, this should not throw even with wrong types
      final List<WasmValue> invalidPayload = List.of(WasmValue.f32(3.14f));
      assertDoesNotThrow(() -> noValidationHandler.throwException(tag, invalidPayload));
    }
  }

  @Test
  void testExceptionHandlerWithWrongTag() {
    final ExceptionHandler.ExceptionTag tag =
        handler.createExceptionTag("wrong_handler_tag", List.of(WasmValueType.I32));

    // Create another handler
    final ExceptionHandler.ExceptionHandlingConfig config =
        ExceptionHandler.ExceptionHandlingConfig.builder().build();

    try (final ExceptionHandler otherHandler = new ExceptionHandler(config)) {
      final ExceptionHandler.ExceptionTag otherTag =
          otherHandler.createExceptionTag("other_tag", List.of(WasmValueType.I32));

      // Should throw exception when using tag from different handler
      assertThrows(
          IllegalArgumentException.class,
          () -> handler.throwException(otherTag, List.of(WasmValue.i32(123))));
    }
  }

  @Test
  void testEmptyPayloadException() {
    final ExceptionHandler.ExceptionTag tag =
        handler.createExceptionTag("empty_payload", Collections.emptyList());

    final ExceptionHandler.WasmException exception =
        assertThrows(
            ExceptionHandler.WasmException.class,
            () -> handler.throwException(tag, Collections.emptyList()));

    assertNotNull(exception);
    assertEquals(tag, exception.getTag());
    assertTrue(exception.getPayload().isEmpty());
  }

  @Test
  void testLargePayloadException() {
    // Create tag with many parameters
    final List<WasmValueType> types =
        Arrays.asList(
            WasmValueType.I32,
            WasmValueType.I64,
            WasmValueType.F32,
            WasmValueType.F64,
            WasmValueType.I32,
            WasmValueType.I64,
            WasmValueType.F32,
            WasmValueType.F64);

    final ExceptionHandler.ExceptionTag tag = handler.createExceptionTag("large_payload", types);

    final List<WasmValue> payload =
        Arrays.asList(
            WasmValue.i32(1),
            WasmValue.i64(2L),
            WasmValue.f32(3.0f),
            WasmValue.f64(4.0),
            WasmValue.i32(5),
            WasmValue.i64(6L),
            WasmValue.f32(7.0f),
            WasmValue.f64(8.0));

    final ExceptionHandler.WasmException exception =
        assertThrows(
            ExceptionHandler.WasmException.class, () -> handler.throwException(tag, payload));

    assertNotNull(exception);
    assertEquals(tag, exception.getTag());
    assertEquals(payload, exception.getPayload());
    assertEquals(8, exception.getPayload().size());
  }

  @Test
  void testExceptionHandlerClosure() {
    assertTrue(handler.getNativeHandle() > 0);
    assertFalse(handler.isClosed());

    handler.close();

    assertTrue(handler.isClosed());

    // Operations should fail after closure
    assertThrows(
        IllegalStateException.class,
        () -> handler.createExceptionTag("after_close", List.of(WasmValueType.I32)));

    assertThrows(IllegalStateException.class, () -> handler.listExceptionTags());

    assertThrows(IllegalStateException.class, () -> handler.performUnwinding(0));

    // Multiple closes should be safe
    assertDoesNotThrow(() -> handler.close());
  }

  @Test
  void testExceptionTagInvalidInputs() {
    // Null name
    assertThrows(
        IllegalArgumentException.class,
        () -> handler.createExceptionTag(null, List.of(WasmValueType.I32)));

    // Empty name
    assertThrows(
        IllegalArgumentException.class,
        () -> handler.createExceptionTag("", List.of(WasmValueType.I32)));

    // Whitespace-only name
    assertThrows(
        IllegalArgumentException.class,
        () -> handler.createExceptionTag("   ", List.of(WasmValueType.I32)));

    // Null parameter types
    assertThrows(IllegalArgumentException.class, () -> handler.createExceptionTag("test", null));
  }

  @Test
  void testExceptionThrowingInvalidInputs() {
    final ExceptionHandler.ExceptionTag tag =
        handler.createExceptionTag("invalid_inputs", List.of(WasmValueType.I32));

    // Null tag
    assertThrows(
        IllegalArgumentException.class,
        () -> handler.throwException(null, List.of(WasmValue.i32(123))));

    // Null payload
    assertThrows(IllegalArgumentException.class, () -> handler.throwException(tag, null));
  }

  @Test
  void testExceptionConfigurationValidation() {
    // Invalid max unwind depth
    assertThrows(
        IllegalArgumentException.class,
        () -> ExceptionHandler.ExceptionHandlingConfig.builder().maxUnwindDepth(0));

    assertThrows(
        IllegalArgumentException.class,
        () -> ExceptionHandler.ExceptionHandlingConfig.builder().maxUnwindDepth(-1));

    // Valid configurations
    assertDoesNotThrow(
        () -> ExceptionHandler.ExceptionHandlingConfig.builder().maxUnwindDepth(1).build());

    assertDoesNotThrow(
        () -> ExceptionHandler.ExceptionHandlingConfig.builder().maxUnwindDepth(10000).build());
  }

  @Test
  void testExceptionFeatureDisabled() {
    ExperimentalFeatures.disableFeature(ExperimentalFeatures.Feature.EXCEPTION_HANDLING);

    final ExceptionHandler.ExceptionHandlingConfig config =
        ExceptionHandler.ExceptionHandlingConfig.builder().build();

    assertThrows(UnsupportedOperationException.class, () -> new ExceptionHandler(config));
  }

  @Test
  void testConcurrentExceptionOperations() throws InterruptedException {
    final ExceptionHandler.ExceptionTag tag =
        handler.createExceptionTag("concurrent_test", List.of(WasmValueType.I32));

    final int threadCount = 10;
    final Thread[] threads = new Thread[threadCount];
    final boolean[] results = new boolean[threadCount];

    for (int i = 0; i < threadCount; i++) {
      final int threadIndex = i;
      threads[i] =
          new Thread(
              () -> {
                try {
                  final List<WasmValue> payload = List.of(WasmValue.i32(threadIndex));
                  handler.throwException(tag, payload);
                  results[threadIndex] = false; // Should not reach here
                } catch (final ExceptionHandler.WasmException e) {
                  results[threadIndex] =
                      e.getTag().equals(tag) && e.getPayload().get(0).i32() == threadIndex;
                } catch (final Exception e) {
                  results[threadIndex] = false;
                }
              });
    }

    // Start all threads
    for (final Thread thread : threads) {
      thread.start();
    }

    // Wait for all threads to complete
    for (final Thread thread : threads) {
      thread.join(5000); // 5 second timeout
    }

    // Verify all threads succeeded
    for (int i = 0; i < threadCount; i++) {
      assertTrue(results[i], "Thread " + i + " failed");
    }
  }
}
