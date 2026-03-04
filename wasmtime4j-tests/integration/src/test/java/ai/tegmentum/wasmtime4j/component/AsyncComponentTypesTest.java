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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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

  // =====================================================================
  // StreamResult Tests
  // =====================================================================

  @Nested
  @DisplayName("StreamResult Tests")
  class StreamResultTests {

    @Test
    @DisplayName("should have exactly three values")
    void shouldHaveThreeValues(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final StreamResult[] values = StreamResult.values();
      assertEquals(3, values.length, "StreamResult should have exactly 3 values");

      LOGGER.info("Value count test passed");
    }

    @Test
    @DisplayName("should have COMPLETED, CANCELLED, and DROPPED in order")
    void shouldHaveCorrectValuesInOrder(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertEquals(0, StreamResult.COMPLETED.ordinal(), "COMPLETED should be ordinal 0");
      assertEquals(1, StreamResult.CANCELLED.ordinal(), "CANCELLED should be ordinal 1");
      assertEquals(2, StreamResult.DROPPED.ordinal(), "DROPPED should be ordinal 2");

      LOGGER.info("Ordinal order test passed");
    }

    @Test
    @DisplayName("should support valueOf for all names")
    void shouldSupportValueOf(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertEquals(StreamResult.COMPLETED, StreamResult.valueOf("COMPLETED"));
      assertEquals(StreamResult.CANCELLED, StreamResult.valueOf("CANCELLED"));
      assertEquals(StreamResult.DROPPED, StreamResult.valueOf("DROPPED"));

      LOGGER.info("valueOf test passed");
    }

    @Test
    @DisplayName("should throw for invalid valueOf")
    void shouldThrowForInvalidValueOf(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertThrows(
          IllegalArgumentException.class,
          () -> StreamResult.valueOf("INVALID"),
          "Should throw for invalid name");

      LOGGER.info("Invalid valueOf test passed");
    }

    @Test
    @DisplayName("should have correct toString values")
    void shouldHaveCorrectToString(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertEquals("COMPLETED", StreamResult.COMPLETED.toString());
      assertEquals("CANCELLED", StreamResult.CANCELLED.toString());
      assertEquals("DROPPED", StreamResult.DROPPED.toString());

      LOGGER.info("toString test passed");
    }
  }

  // =====================================================================
  // GuardedStreamReader Tests
  // =====================================================================

  @Nested
  @DisplayName("GuardedStreamReader Tests")
  class GuardedStreamReaderTests {

    @Test
    @DisplayName("should create guard with valid stream")
    void shouldCreateWithValidStream(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final StreamAny stream = StreamAny.create(1L);
      final GuardedStreamReader guard = new GuardedStreamReader(stream);

      assertTrue(guard.isActive(), "Guard should be active after creation");
      assertEquals(stream, guard.getStream(), "getStream should return the wrapped stream");

      guard.close();

      LOGGER.info("Guard creation test passed");
    }

    @Test
    @DisplayName("should reject null stream")
    void shouldRejectNullStream(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertThrows(
          IllegalArgumentException.class,
          () -> new GuardedStreamReader(null),
          "Should reject null stream");

      LOGGER.info("Null rejection test passed");
    }

    @Test
    @DisplayName("should close underlying stream when guard is closed")
    void shouldCloseStreamOnGuardClose(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final StreamAny stream = StreamAny.create(2L);
      assertTrue(stream.isValid(), "Stream should be valid before guard close");

      final GuardedStreamReader guard = new GuardedStreamReader(stream);
      guard.close();

      assertFalse(stream.isValid(), "Stream should be invalid after guard close");
      assertFalse(guard.isActive(), "Guard should be inactive after close");

      LOGGER.info("Close propagation test passed");
    }

    @Test
    @DisplayName("should transfer ownership via intoStream")
    void shouldTransferOwnershipViaIntoStream(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final StreamAny stream = StreamAny.create(3L);
      final GuardedStreamReader guard = new GuardedStreamReader(stream);

      final StreamAny transferred = guard.intoStream();
      assertEquals(stream, transferred, "intoStream should return the same stream");
      assertFalse(guard.isActive(), "Guard should be inactive after intoStream");

      // Guard close should be a no-op — stream should remain valid
      guard.close();
      assertTrue(transferred.isValid(), "Stream should remain valid after guard close");

      transferred.close();

      LOGGER.info("Ownership transfer test passed");
    }

    @Test
    @DisplayName("should throw on getStream after close")
    void shouldThrowOnGetStreamAfterClose(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final GuardedStreamReader guard = new GuardedStreamReader(StreamAny.create(4L));
      guard.close();

      assertThrows(
          IllegalStateException.class, guard::getStream, "getStream should throw after close");

      LOGGER.info("Use-after-close test passed");
    }

    @Test
    @DisplayName("should throw on intoStream after close")
    void shouldThrowOnIntoStreamAfterClose(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final GuardedStreamReader guard = new GuardedStreamReader(StreamAny.create(5L));
      guard.close();

      assertThrows(
          IllegalStateException.class, guard::intoStream, "intoStream should throw after close");

      LOGGER.info("intoStream after close test passed");
    }

    @Test
    @DisplayName("should be idempotent on double close")
    void shouldBeIdempotentOnDoubleClose(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final StreamAny stream = StreamAny.create(6L);
      final GuardedStreamReader guard = new GuardedStreamReader(stream);

      guard.close();
      guard.close(); // Second close should be safe
      assertFalse(guard.isActive(), "Guard should remain inactive");
      assertFalse(stream.isValid(), "Stream should remain invalid");

      LOGGER.info("Double close idempotency test passed");
    }

    @Test
    @DisplayName("should work with try-with-resources")
    void shouldWorkWithTryWithResources(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final StreamAny stream = StreamAny.create(7L);
      try (GuardedStreamReader guard = new GuardedStreamReader(stream)) {
        assertTrue(guard.isActive(), "Guard should be active inside try");
        assertNotNull(guard.getStream(), "getStream should return non-null inside try");
      }
      assertFalse(stream.isValid(), "Stream should be closed after try-with-resources");

      LOGGER.info("Try-with-resources test passed");
    }
  }

  // =====================================================================
  // GuardedFutureReader Tests
  // =====================================================================

  @Nested
  @DisplayName("GuardedFutureReader Tests")
  class GuardedFutureReaderTests {

    @Test
    @DisplayName("should create guard with valid future")
    void shouldCreateWithValidFuture(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FutureAny future = FutureAny.create(1L);
      final GuardedFutureReader guard = new GuardedFutureReader(future);

      assertTrue(guard.isActive(), "Guard should be active after creation");
      assertEquals(future, guard.getFuture(), "getFuture should return the wrapped future");

      guard.close();

      LOGGER.info("Guard creation test passed");
    }

    @Test
    @DisplayName("should reject null future")
    void shouldRejectNullFuture(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertThrows(
          IllegalArgumentException.class,
          () -> new GuardedFutureReader(null),
          "Should reject null future");

      LOGGER.info("Null rejection test passed");
    }

    @Test
    @DisplayName("should close underlying future when guard is closed")
    void shouldCloseFutureOnGuardClose(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FutureAny future = FutureAny.create(2L);
      assertTrue(future.isValid(), "Future should be valid before guard close");

      final GuardedFutureReader guard = new GuardedFutureReader(future);
      guard.close();

      assertFalse(future.isValid(), "Future should be invalid after guard close");
      assertFalse(guard.isActive(), "Guard should be inactive after close");

      LOGGER.info("Close propagation test passed");
    }

    @Test
    @DisplayName("should transfer ownership via intoFuture")
    void shouldTransferOwnershipViaIntoFuture(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FutureAny future = FutureAny.create(3L);
      final GuardedFutureReader guard = new GuardedFutureReader(future);

      final FutureAny transferred = guard.intoFuture();
      assertEquals(future, transferred, "intoFuture should return the same future");
      assertFalse(guard.isActive(), "Guard should be inactive after intoFuture");

      // Guard close should be a no-op — future should remain valid
      guard.close();
      assertTrue(transferred.isValid(), "Future should remain valid after guard close");

      transferred.close();

      LOGGER.info("Ownership transfer test passed");
    }

    @Test
    @DisplayName("should throw on getFuture after close")
    void shouldThrowOnGetFutureAfterClose(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final GuardedFutureReader guard = new GuardedFutureReader(FutureAny.create(4L));
      guard.close();

      assertThrows(
          IllegalStateException.class, guard::getFuture, "getFuture should throw after close");

      LOGGER.info("Use-after-close test passed");
    }

    @Test
    @DisplayName("should throw on intoFuture after close")
    void shouldThrowOnIntoFutureAfterClose(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final GuardedFutureReader guard = new GuardedFutureReader(FutureAny.create(5L));
      guard.close();

      assertThrows(
          IllegalStateException.class, guard::intoFuture, "intoFuture should throw after close");

      LOGGER.info("intoFuture after close test passed");
    }

    @Test
    @DisplayName("should be idempotent on double close")
    void shouldBeIdempotentOnDoubleClose(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FutureAny future = FutureAny.create(6L);
      final GuardedFutureReader guard = new GuardedFutureReader(future);

      guard.close();
      guard.close(); // Second close should be safe
      assertFalse(guard.isActive(), "Guard should remain inactive");
      assertFalse(future.isValid(), "Future should remain invalid");

      LOGGER.info("Double close idempotency test passed");
    }

    @Test
    @DisplayName("should work with try-with-resources")
    void shouldWorkWithTryWithResources(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final FutureAny future = FutureAny.create(7L);
      try (GuardedFutureReader guard = new GuardedFutureReader(future)) {
        assertTrue(guard.isActive(), "Guard should be active inside try");
        assertNotNull(guard.getFuture(), "getFuture should return non-null inside try");
      }
      assertFalse(future.isValid(), "Future should be closed after try-with-resources");

      LOGGER.info("Try-with-resources test passed");
    }
  }

  // =====================================================================
  // Close Callback Tests (StreamAny, FutureAny, ErrorContext)
  // =====================================================================

  @Nested
  @DisplayName("Close Callback Tests")
  class CloseCallbackTests {

    @Test
    @DisplayName("StreamAny close should invoke callback exactly once")
    void streamAnyCloseShouldInvokeCallback(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AtomicInteger callCount = new AtomicInteger(0);
      final StreamAny stream = StreamAny.create(1L, callCount::incrementAndGet);

      assertTrue(stream.isValid(), "Stream should be valid before close");
      assertEquals(0, callCount.get(), "Callback should not be called before close");

      stream.close();
      assertFalse(stream.isValid(), "Stream should be invalid after close");
      assertEquals(1, callCount.get(), "Callback should be called exactly once");

      // Second close should be idempotent — no additional callback invocation
      stream.close();
      assertEquals(1, callCount.get(), "Callback should still be called exactly once");

      LOGGER.info("StreamAny close callback test passed");
    }

    @Test
    @DisplayName("FutureAny close should invoke callback exactly once")
    void futureAnyCloseShouldInvokeCallback(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AtomicInteger callCount = new AtomicInteger(0);
      final FutureAny future = FutureAny.create(1L, callCount::incrementAndGet);

      assertTrue(future.isValid(), "Future should be valid before close");
      assertEquals(0, callCount.get(), "Callback should not be called before close");

      future.close();
      assertFalse(future.isValid(), "Future should be invalid after close");
      assertEquals(1, callCount.get(), "Callback should be called exactly once");

      future.close();
      assertEquals(1, callCount.get(), "Callback should still be called exactly once");

      LOGGER.info("FutureAny close callback test passed");
    }

    @Test
    @DisplayName("ErrorContext close should invoke callback exactly once")
    void errorContextCloseShouldInvokeCallback(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AtomicInteger callCount = new AtomicInteger(0);
      final ErrorContext ctx = ErrorContext.create(1L, callCount::incrementAndGet);

      assertTrue(ctx.isValid(), "ErrorContext should be valid before close");
      assertEquals(0, callCount.get(), "Callback should not be called before close");

      ctx.close();
      assertFalse(ctx.isValid(), "ErrorContext should be invalid after close");
      assertEquals(1, callCount.get(), "Callback should be called exactly once");

      ctx.close();
      assertEquals(1, callCount.get(), "Callback should still be called exactly once");

      LOGGER.info("ErrorContext close callback test passed");
    }

    @Test
    @DisplayName("StreamAny with null callback should work like no-op close")
    void streamAnyNullCallbackShouldBeNoOp(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final StreamAny stream = StreamAny.create(10L, null);
      assertTrue(stream.isValid(), "Stream should be valid before close");

      stream.close();
      assertFalse(stream.isValid(), "Stream should be invalid after close");

      LOGGER.info("Null callback test passed");
    }

    @Test
    @DisplayName("Close callback should be thread-safe via AtomicBoolean")
    void closeCallbackShouldBeThreadSafe(final TestInfo testInfo) throws InterruptedException {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AtomicInteger callCount = new AtomicInteger(0);
      final StreamAny stream = StreamAny.create(1L, callCount::incrementAndGet);

      // Launch multiple threads that all try to close the stream simultaneously
      final int threadCount = 10;
      final Thread[] threads = new Thread[threadCount];
      final AtomicBoolean startFlag = new AtomicBoolean(false);

      for (int i = 0; i < threadCount; i++) {
        threads[i] =
            new Thread(
                () -> {
                  while (!startFlag.get()) {
                    Thread.yield();
                  }
                  stream.close();
                });
        threads[i].start();
      }

      // Release all threads at once
      startFlag.set(true);

      for (final Thread thread : threads) {
        thread.join(5000);
      }

      assertEquals(
          1,
          callCount.get(),
          "Callback should be invoked exactly once even with concurrent closes");
      assertFalse(stream.isValid(), "Stream should be invalid");

      LOGGER.info("Thread safety test passed");
    }

    @Test
    @DisplayName("GuardedStreamReader should invoke stream close callback")
    void guardedStreamReaderShouldInvokeCallback(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AtomicBoolean callbackInvoked = new AtomicBoolean(false);
      final StreamAny stream = StreamAny.create(1L, () -> callbackInvoked.set(true));

      try (GuardedStreamReader guard = new GuardedStreamReader(stream)) {
        assertTrue(guard.isActive(), "Guard should be active inside try");
        assertFalse(callbackInvoked.get(), "Callback should not be invoked while guard is active");
      }
      assertTrue(callbackInvoked.get(), "Callback should be invoked after guard close");

      LOGGER.info("GuardedStreamReader callback test passed");
    }

    @Test
    @DisplayName("GuardedFutureReader should invoke future close callback")
    void guardedFutureReaderShouldInvokeCallback(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AtomicBoolean callbackInvoked = new AtomicBoolean(false);
      final FutureAny future = FutureAny.create(1L, () -> callbackInvoked.set(true));

      try (GuardedFutureReader guard = new GuardedFutureReader(future)) {
        assertTrue(guard.isActive(), "Guard should be active inside try");
        assertFalse(callbackInvoked.get(), "Callback should not be invoked while guard is active");
      }
      assertTrue(callbackInvoked.get(), "Callback should be invoked after guard close");

      LOGGER.info("GuardedFutureReader callback test passed");
    }

    @Test
    @DisplayName("GuardedStreamReader intoStream should NOT invoke callback")
    void guardedStreamReaderIntoStreamShouldNotInvokeCallback(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AtomicBoolean callbackInvoked = new AtomicBoolean(false);
      final StreamAny stream = StreamAny.create(1L, () -> callbackInvoked.set(true));

      final StreamAny transferred;
      try (GuardedStreamReader guard = new GuardedStreamReader(stream)) {
        transferred = guard.intoStream();
      }
      assertFalse(
          callbackInvoked.get(), "Callback should NOT be invoked when ownership was transferred");
      assertTrue(transferred.isValid(), "Transferred stream should still be valid");

      // Now close the transferred stream manually
      transferred.close();
      assertTrue(callbackInvoked.get(), "Callback should be invoked on manual close");

      LOGGER.info("intoStream no-callback test passed");
    }
  }
}
