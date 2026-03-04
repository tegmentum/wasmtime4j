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
package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Tests for async component model types: ErrorContext, StreamAny, FutureAny, AccessorTask, and
 * ConcurrentScope.
 *
 * <p>These tests verify the creation, lifecycle, and contract of the async component model types
 * that correspond to Wasmtime's type-erased async value handles.
 *
 * @since 1.1.0
 */
@DisplayName("Async Component Model Types Tests")
public final class AsyncComponentTypesTest {

  private static final Logger LOGGER = Logger.getLogger(AsyncComponentTypesTest.class.getName());

  // =====================================================================
  // ErrorContext Tests
  // =====================================================================

  @Nested
  @DisplayName("ErrorContext Tests")
  class ErrorContextTests {

    @Test
    @DisplayName("should create ErrorContext with valid handle")
    void shouldCreateWithValidHandle(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ErrorContext ctx = ErrorContext.create(42L);

      assertNotNull(ctx, "ErrorContext should not be null");
      assertEquals(42L, ctx.getHandle(), "Handle should be 42");
      assertTrue(ctx.isValid(), "Newly created ErrorContext should be valid");

      LOGGER.info("ErrorContext creation test passed");
    }

    @Test
    @DisplayName("should reject zero handle")
    void shouldRejectZeroHandle(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> ErrorContext.create(0L),
              "Should reject zero handle");

      assertTrue(ex.getMessage().contains("positive"), "Error message should mention 'positive'");

      LOGGER.info("Zero handle rejection test passed");
    }

    @Test
    @DisplayName("should reject negative handle")
    void shouldRejectNegativeHandle(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> ErrorContext.create(-1L),
              "Should reject negative handle");

      assertTrue(ex.getMessage().contains("positive"), "Error message should mention 'positive'");

      LOGGER.info("Negative handle rejection test passed");
    }

    @Test
    @DisplayName("should become invalid after close")
    void shouldBecomeInvalidAfterClose(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ErrorContext ctx = ErrorContext.create(1L);
      assertTrue(ctx.isValid(), "Should be valid before close");

      ctx.close();
      assertFalse(ctx.isValid(), "Should be invalid after close");

      // Handle value is still accessible
      assertEquals(1L, ctx.getHandle(), "Handle should still return the value after close");

      LOGGER.info("Close invalidation test passed");
    }

    @Test
    @DisplayName("should be idempotent on multiple close calls")
    void shouldBeIdempotentOnMultipleClose(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ErrorContext ctx = ErrorContext.create(5L);
      ctx.close();
      ctx.close(); // Second close should not throw
      assertFalse(ctx.isValid(), "Should remain invalid after multiple close calls");

      LOGGER.info("Idempotent close test passed");
    }

    @Test
    @DisplayName("should have correct toString format")
    void shouldHaveCorrectToString(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ErrorContext ctx = ErrorContext.create(99L);
      final String str = ctx.toString();

      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("ErrorContext"), "toString should contain type name");
      assertTrue(str.contains("99"), "toString should contain handle value");
      assertTrue(str.contains("true"), "toString should contain validity status");

      ctx.close();
      final String closedStr = ctx.toString();
      assertTrue(closedStr.contains("false"), "toString should reflect closed state");

      LOGGER.info("toString format test passed");
    }

    @Test
    @DisplayName("should implement equals based on handle")
    void shouldImplementEqualsBasedOnHandle(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ErrorContext ctx1 = ErrorContext.create(10L);
      final ErrorContext ctx2 = ErrorContext.create(10L);
      final ErrorContext ctx3 = ErrorContext.create(20L);

      assertEquals(ctx1, ctx2, "ErrorContexts with same handle should be equal");
      assertNotEquals(ctx1, ctx3, "ErrorContexts with different handles should not be equal");
      assertEquals(ctx1.hashCode(), ctx2.hashCode(), "Equal ErrorContexts should have same hash");

      LOGGER.info("Equals and hashCode test passed");
    }

    @Test
    @DisplayName("should work with try-with-resources")
    void shouldWorkWithTryWithResources(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ErrorContext ctx;
      try (ErrorContext autoCtx = ErrorContext.create(7L)) {
        ctx = autoCtx;
        assertTrue(ctx.isValid(), "Should be valid inside try block");
      }
      assertFalse(ctx.isValid(), "Should be invalid after try-with-resources block");

      LOGGER.info("Try-with-resources test passed");
    }

    @Test
    @DisplayName("should integrate with ComponentVal.errorContext")
    void shouldIntegrateWithComponentVal(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (ErrorContext ctx = ErrorContext.create(42L)) {
        final ComponentVal val = ComponentVal.errorContext(ctx.getHandle());

        assertNotNull(val, "ComponentVal should not be null");
        assertTrue(val.isErrorContext(), "ComponentVal should be error context type");
        assertEquals(
            42L,
            val.asErrorContextHandle(),
            "ComponentVal handle should match ErrorContext handle");
      }

      LOGGER.info("ComponentVal integration test passed");
    }
  }

  // =====================================================================
  // StreamAny Tests
  // =====================================================================

  @Nested
  @DisplayName("StreamAny Tests")
  class StreamAnyTests {

    @Test
    @DisplayName("should create StreamAny with valid handle")
    void shouldCreateWithValidHandle(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final StreamAny stream = StreamAny.create(100L);

      assertNotNull(stream, "StreamAny should not be null");
      assertEquals(100L, stream.getHandle(), "Handle should be 100");
      assertTrue(stream.isValid(), "Newly created StreamAny should be valid");

      LOGGER.info("StreamAny creation test passed");
    }

    @Test
    @DisplayName("should reject zero handle")
    void shouldRejectZeroHandle(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertThrows(
          IllegalArgumentException.class, () -> StreamAny.create(0L), "Should reject zero handle");

      LOGGER.info("Zero handle rejection test passed");
    }

    @Test
    @DisplayName("should reject negative handle")
    void shouldRejectNegativeHandle(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertThrows(
          IllegalArgumentException.class,
          () -> StreamAny.create(-5L),
          "Should reject negative handle");

      LOGGER.info("Negative handle rejection test passed");
    }

    @Test
    @DisplayName("should become invalid after close")
    void shouldBecomeInvalidAfterClose(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final StreamAny stream = StreamAny.create(1L);
      assertTrue(stream.isValid(), "Should be valid before close");

      stream.close();
      assertFalse(stream.isValid(), "Should be invalid after close");
      assertEquals(1L, stream.getHandle(), "Handle should still be accessible after close");

      LOGGER.info("Close invalidation test passed");
    }

    @Test
    @DisplayName("should work with try-with-resources")
    void shouldWorkWithTryWithResources(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final StreamAny stream;
      try (StreamAny autoStream = StreamAny.create(3L)) {
        stream = autoStream;
        assertTrue(stream.isValid(), "Should be valid inside try block");
      }
      assertFalse(stream.isValid(), "Should be invalid after try-with-resources block");

      LOGGER.info("Try-with-resources test passed");
    }

    @Test
    @DisplayName("should implement equals based on handle")
    void shouldImplementEqualsBasedOnHandle(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final StreamAny s1 = StreamAny.create(50L);
      final StreamAny s2 = StreamAny.create(50L);
      final StreamAny s3 = StreamAny.create(60L);

      assertEquals(s1, s2, "StreamAnys with same handle should be equal");
      assertNotEquals(s1, s3, "StreamAnys with different handles should not be equal");
      assertEquals(s1.hashCode(), s2.hashCode(), "Equal StreamAnys should have same hash");

      LOGGER.info("Equals and hashCode test passed");
    }

    @Test
    @DisplayName("should have correct toString format")
    void shouldHaveCorrectToString(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final StreamAny stream = StreamAny.create(77L);
      final String str = stream.toString();

      assertTrue(str.contains("StreamAny"), "toString should contain type name");
      assertTrue(str.contains("77"), "toString should contain handle value");

      LOGGER.info("toString format test passed");
    }

    @Test
    @DisplayName("should integrate with ComponentVal.stream")
    void shouldIntegrateWithComponentVal(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (StreamAny stream = StreamAny.create(42L)) {
        final ComponentVal val = ComponentVal.stream(stream.getHandle());

        assertNotNull(val, "ComponentVal should not be null");
        assertTrue(val.isStream(), "ComponentVal should be stream type");
        assertEquals(
            42L, val.asStreamHandle(), "ComponentVal handle should match StreamAny handle");
      }

      LOGGER.info("ComponentVal integration test passed");
    }
  }

  // =====================================================================
  // FutureAny Tests
  // =====================================================================

  @Nested
  @DisplayName("FutureAny Tests")
  class FutureAnyTests {

    @Test
    @DisplayName("should create FutureAny with valid handle")
    void shouldCreateWithValidHandle(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FutureAny future = FutureAny.create(200L);

      assertNotNull(future, "FutureAny should not be null");
      assertEquals(200L, future.getHandle(), "Handle should be 200");
      assertTrue(future.isValid(), "Newly created FutureAny should be valid");

      LOGGER.info("FutureAny creation test passed");
    }

    @Test
    @DisplayName("should reject zero handle")
    void shouldRejectZeroHandle(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertThrows(
          IllegalArgumentException.class, () -> FutureAny.create(0L), "Should reject zero handle");

      LOGGER.info("Zero handle rejection test passed");
    }

    @Test
    @DisplayName("should reject negative handle")
    void shouldRejectNegativeHandle(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertThrows(
          IllegalArgumentException.class,
          () -> FutureAny.create(-10L),
          "Should reject negative handle");

      LOGGER.info("Negative handle rejection test passed");
    }

    @Test
    @DisplayName("should become invalid after close")
    void shouldBecomeInvalidAfterClose(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FutureAny future = FutureAny.create(1L);
      assertTrue(future.isValid(), "Should be valid before close");

      future.close();
      assertFalse(future.isValid(), "Should be invalid after close");
      assertEquals(1L, future.getHandle(), "Handle should still be accessible after close");

      LOGGER.info("Close invalidation test passed");
    }

    @Test
    @DisplayName("should work with try-with-resources")
    void shouldWorkWithTryWithResources(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FutureAny future;
      try (FutureAny autoFuture = FutureAny.create(9L)) {
        future = autoFuture;
        assertTrue(future.isValid(), "Should be valid inside try block");
      }
      assertFalse(future.isValid(), "Should be invalid after try-with-resources block");

      LOGGER.info("Try-with-resources test passed");
    }

    @Test
    @DisplayName("should implement equals based on handle")
    void shouldImplementEqualsBasedOnHandle(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FutureAny f1 = FutureAny.create(30L);
      final FutureAny f2 = FutureAny.create(30L);
      final FutureAny f3 = FutureAny.create(40L);

      assertEquals(f1, f2, "FutureAnys with same handle should be equal");
      assertNotEquals(f1, f3, "FutureAnys with different handles should not be equal");
      assertEquals(f1.hashCode(), f2.hashCode(), "Equal FutureAnys should have same hash");

      LOGGER.info("Equals and hashCode test passed");
    }

    @Test
    @DisplayName("should have correct toString format")
    void shouldHaveCorrectToString(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FutureAny future = FutureAny.create(88L);
      final String str = future.toString();

      assertTrue(str.contains("FutureAny"), "toString should contain type name");
      assertTrue(str.contains("88"), "toString should contain handle value");

      LOGGER.info("toString format test passed");
    }

    @Test
    @DisplayName("should integrate with ComponentVal.future")
    void shouldIntegrateWithComponentVal(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (FutureAny future = FutureAny.create(42L)) {
        final ComponentVal val = ComponentVal.future(future.getHandle());

        assertNotNull(val, "ComponentVal should not be null");
        assertTrue(val.isFuture(), "ComponentVal should be future type");
        assertEquals(
            42L, val.asFutureHandle(), "ComponentVal handle should match FutureAny handle");
      }

      LOGGER.info("ComponentVal integration test passed");
    }
  }

  // =====================================================================
  // AccessorTask Tests
  // =====================================================================

  @Nested
  @DisplayName("AccessorTask Tests")
  class AccessorTaskTests {

    @Test
    @DisplayName("should be a functional interface usable as lambda")
    void shouldBeUsableAsLambda(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AccessorTask<String> task = scope -> "hello from task";

      assertNotNull(task, "AccessorTask lambda should not be null");

      LOGGER.info("Lambda creation test passed");
    }

    @Test
    @DisplayName("should execute with a ConcurrentScope and return result")
    void shouldExecuteWithScopeAndReturnResult(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AccessorTask<Integer> task = scope -> 42;

      // Use a minimal ConcurrentScope stub for testing the task contract
      final ConcurrentScope stubScope = (func, args) -> List.of();
      final int result = task.execute(stubScope);

      assertEquals(42, result, "Task should return the value from the lambda");

      LOGGER.info("Execute with scope test passed");
    }

    @Test
    @DisplayName("should support parameterized return types")
    void shouldSupportParameterizedReturnTypes(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AccessorTask<List<String>> task = scope -> List.of("a", "b", "c");

      final ConcurrentScope stubScope = (func, args) -> List.of();
      final List<String> result = task.execute(stubScope);

      assertEquals(3, result.size(), "Task should return a list with 3 elements");
      assertEquals("a", result.get(0), "First element should be 'a'");

      LOGGER.info("Parameterized return type test passed");
    }

    @Test
    @DisplayName("should propagate WasmException from task")
    void shouldPropagateWasmException(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AccessorTask<Void> task =
          scope -> {
            throw new ai.tegmentum.wasmtime4j.exception.WasmException("test error");
          };

      final ConcurrentScope stubScope = (func, args) -> List.of();

      final ai.tegmentum.wasmtime4j.exception.WasmException ex =
          assertThrows(
              ai.tegmentum.wasmtime4j.exception.WasmException.class,
              () -> task.execute(stubScope),
              "Should propagate WasmException");

      assertEquals("test error", ex.getMessage(), "Exception message should match");

      LOGGER.info("WasmException propagation test passed");
    }
  }

  // =====================================================================
  // ConcurrentScope Tests
  // =====================================================================

  @Nested
  @DisplayName("ConcurrentScope Tests")
  class ConcurrentScopeTests {

    @Test
    @DisplayName("should be implementable as a lambda")
    void shouldBeImplementableAsLambda(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // ConcurrentScope is an interface with a single method, so it can be implemented as a lambda
      final ConcurrentScope scope = (func, args) -> List.of(ComponentVal.s32(42));

      assertNotNull(scope, "ConcurrentScope lambda should not be null");

      LOGGER.info("Lambda implementation test passed");
    }

    @Test
    @DisplayName("should return results from callConcurrent")
    void shouldReturnResultsFromCallConcurrent(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ConcurrentScope scope =
          (func, args) -> List.of(ComponentVal.s32(100), ComponentVal.string("ok"));

      final List<ComponentVal> results = scope.callConcurrent(null, List.of());

      assertEquals(2, results.size(), "Should return 2 results");
      assertTrue(results.get(0).isS32(), "First result should be s32");
      assertEquals(100, results.get(0).asS32(), "First result should be 100");
      assertTrue(results.get(1).isString(), "Second result should be string");
      assertEquals("ok", results.get(1).asString(), "Second result should be 'ok'");

      LOGGER.info("callConcurrent results test passed");
    }

    @Test
    @DisplayName("should work together with AccessorTask")
    void shouldWorkTogetherWithAccessorTask(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Simulate the pattern: AccessorTask receives a ConcurrentScope
      final ConcurrentScope scope = (func, args) -> List.of(ComponentVal.s32(args.size()));

      final AccessorTask<List<ComponentVal>> task =
          s -> s.callConcurrent(null, List.of(ComponentVal.s32(1), ComponentVal.s32(2)));

      final List<ComponentVal> results = task.execute(scope);

      assertEquals(1, results.size(), "Should return 1 result");
      assertEquals(2, results.get(0).asS32(), "Result should be the arg count (2)");

      LOGGER.info("AccessorTask + ConcurrentScope integration test passed");
    }
  }
}
