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

package ai.tegmentum.wasmtime4j.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for AsyncExecutionContext class.
 *
 * <p>Verifies execution context for asynchronous WebAssembly function calls.
 */
@DisplayName("AsyncExecutionContext Tests")
class AsyncExecutionContextTest {

  private Executor defaultExecutor;

  @BeforeEach
  void setUp() {
    defaultExecutor = ForkJoinPool.commonPool();
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create context with all parameters")
    void shouldCreateContextWithAllParameters() {
      Optional<Duration> timeout = Optional.of(Duration.ofSeconds(30));

      AsyncExecutionContext context =
          new AsyncExecutionContext("testFunction", timeout, defaultExecutor, "exec-123");

      assertNotNull(context, "Context should not be null");
      assertEquals("testFunction", context.getFunctionName(), "Function name should match");
      assertEquals(timeout, context.getTimeout(), "Timeout should match");
      assertEquals(defaultExecutor, context.getExecutor(), "Executor should match");
      assertEquals("exec-123", context.getExecutionId(), "Execution ID should match");
      assertNotNull(context.getStartTime(), "Start time should not be null");
      assertFalse(context.isCancelled(), "Should not be cancelled initially");
      assertFalse(context.isCompleted(), "Should not be completed initially");
      assertFalse(context.getError().isPresent(), "Should not have error initially");
    }

    @Test
    @DisplayName("should create context with no timeout")
    void shouldCreateContextWithNoTimeout() {
      AsyncExecutionContext context =
          new AsyncExecutionContext("noTimeoutFunc", Optional.empty(), defaultExecutor, "exec-456");

      assertEquals(Optional.empty(), context.getTimeout(), "Timeout should be empty");
    }

    @Test
    @DisplayName("should reject null function name")
    void shouldRejectNullFunctionName() {
      assertThrows(
          NullPointerException.class,
          () -> new AsyncExecutionContext(null, Optional.empty(), defaultExecutor, "exec-789"),
          "Should throw for null function name");
    }

    @Test
    @DisplayName("should reject null timeout optional")
    void shouldRejectNullTimeoutOptional() {
      assertThrows(
          NullPointerException.class,
          () -> new AsyncExecutionContext("func", null, defaultExecutor, "exec-789"),
          "Should throw for null timeout optional");
    }

    @Test
    @DisplayName("should reject null executor")
    void shouldRejectNullExecutor() {
      assertThrows(
          NullPointerException.class,
          () -> new AsyncExecutionContext("func", Optional.empty(), null, "exec-789"),
          "Should throw for null executor");
    }

    @Test
    @DisplayName("should reject null execution ID")
    void shouldRejectNullExecutionId() {
      assertThrows(
          NullPointerException.class,
          () -> new AsyncExecutionContext("func", Optional.empty(), defaultExecutor, null),
          "Should throw for null execution ID");
    }
  }

  @Nested
  @DisplayName("GetElapsedTime Tests")
  class GetElapsedTimeTests {

    @Test
    @DisplayName("should return positive elapsed time")
    void shouldReturnPositiveElapsedTime() throws InterruptedException {
      AsyncExecutionContext context =
          new AsyncExecutionContext("func", Optional.empty(), defaultExecutor, "exec-001");

      Thread.sleep(10); // Small delay

      Duration elapsed = context.getElapsedTime();

      assertTrue(elapsed.toMillis() >= 10, "Elapsed time should be at least 10ms");
    }

    @Test
    @DisplayName("should return elapsed time relative to start")
    void shouldReturnElapsedTimeRelativeToStart() {
      AsyncExecutionContext context =
          new AsyncExecutionContext("func", Optional.empty(), defaultExecutor, "exec-002");

      Instant start = context.getStartTime();
      Duration elapsed = context.getElapsedTime();

      assertTrue(!elapsed.isNegative(), "Elapsed should not be negative");
      assertTrue(
          elapsed.compareTo(Duration.between(start, Instant.now().plusSeconds(1))) <= 0,
          "Elapsed should be reasonable");
    }
  }

  @Nested
  @DisplayName("IsTimedOut Tests")
  class IsTimedOutTests {

    @Test
    @DisplayName("should return false when no timeout")
    void shouldReturnFalseWhenNoTimeout() {
      AsyncExecutionContext context =
          new AsyncExecutionContext("func", Optional.empty(), defaultExecutor, "exec-003");

      assertFalse(context.isTimedOut(), "Should not be timed out when no timeout");
    }

    @Test
    @DisplayName("should return false when within timeout")
    void shouldReturnFalseWhenWithinTimeout() {
      AsyncExecutionContext context =
          new AsyncExecutionContext(
              "func", Optional.of(Duration.ofMinutes(5)), defaultExecutor, "exec-004");

      assertFalse(context.isTimedOut(), "Should not be timed out initially");
    }

    @Test
    @DisplayName("should return true when timeout exceeded")
    void shouldReturnTrueWhenTimeoutExceeded() throws InterruptedException {
      AsyncExecutionContext context =
          new AsyncExecutionContext(
              "func", Optional.of(Duration.ofMillis(5)), defaultExecutor, "exec-005");

      Thread.sleep(20); // Wait past timeout

      assertTrue(context.isTimedOut(), "Should be timed out after timeout duration");
    }
  }

  @Nested
  @DisplayName("Cancel Tests")
  class CancelTests {

    @Test
    @DisplayName("should cancel execution")
    void shouldCancelExecution() {
      AsyncExecutionContext context =
          new AsyncExecutionContext("func", Optional.empty(), defaultExecutor, "exec-006");

      assertFalse(context.isCancelled(), "Should not be cancelled initially");

      context.cancel();

      assertTrue(context.isCancelled(), "Should be cancelled after cancel()");
    }

    @Test
    @DisplayName("should remain cancelled after multiple calls")
    void shouldRemainCancelledAfterMultipleCalls() {
      AsyncExecutionContext context =
          new AsyncExecutionContext("func", Optional.empty(), defaultExecutor, "exec-007");

      context.cancel();
      context.cancel();
      context.cancel();

      assertTrue(context.isCancelled(), "Should still be cancelled");
    }
  }

  @Nested
  @DisplayName("MarkCompleted Tests")
  class MarkCompletedTests {

    @Test
    @DisplayName("should mark execution as completed")
    void shouldMarkExecutionAsCompleted() {
      AsyncExecutionContext context =
          new AsyncExecutionContext("func", Optional.empty(), defaultExecutor, "exec-008");

      assertFalse(context.isCompleted(), "Should not be completed initially");

      context.markCompleted();

      assertTrue(context.isCompleted(), "Should be completed after markCompleted()");
    }
  }

  @Nested
  @DisplayName("SetError Tests")
  class SetErrorTests {

    @Test
    @DisplayName("should set error and mark completed")
    void shouldSetErrorAndMarkCompleted() {
      AsyncExecutionContext context =
          new AsyncExecutionContext("func", Optional.empty(), defaultExecutor, "exec-009");

      RuntimeException error = new RuntimeException("Test error");
      context.setError(error);

      assertTrue(context.getError().isPresent(), "Should have error");
      assertEquals(error, context.getError().get(), "Error should match");
      assertTrue(context.isCompleted(), "Should be completed after error");
    }

    @Test
    @DisplayName("should handle null error")
    void shouldHandleNullError() {
      AsyncExecutionContext context =
          new AsyncExecutionContext("func", Optional.empty(), defaultExecutor, "exec-010");

      context.setError(null);

      assertFalse(context.getError().isPresent(), "Should not have error for null");
      assertTrue(context.isCompleted(), "Should be completed even with null error");
    }
  }

  @Nested
  @DisplayName("Metadata Tests")
  class MetadataTests {

    @Test
    @DisplayName("should set and get metadata")
    void shouldSetAndGetMetadata() {
      AsyncExecutionContext context =
          new AsyncExecutionContext("func", Optional.empty(), defaultExecutor, "exec-011");

      context.setMetadata("key1", "value1");

      Optional<Object> value = context.getMetadata("key1");
      assertTrue(value.isPresent(), "Metadata should be present");
      assertEquals("value1", value.get(), "Metadata value should match");
    }

    @Test
    @DisplayName("should return empty for missing metadata")
    void shouldReturnEmptyForMissingMetadata() {
      AsyncExecutionContext context =
          new AsyncExecutionContext("func", Optional.empty(), defaultExecutor, "exec-012");

      Optional<Object> value = context.getMetadata("nonexistent");

      assertFalse(value.isPresent(), "Should return empty for missing key");
    }

    @Test
    @DisplayName("should get typed metadata")
    void shouldGetTypedMetadata() {
      AsyncExecutionContext context =
          new AsyncExecutionContext("func", Optional.empty(), defaultExecutor, "exec-013");

      context.setMetadata("intKey", 42);
      context.setMetadata("stringKey", "hello");

      Optional<Integer> intValue = context.getMetadata("intKey", Integer.class);
      assertTrue(intValue.isPresent(), "Integer metadata should be present");
      assertEquals(42, intValue.get(), "Integer value should match");

      Optional<String> stringValue = context.getMetadata("stringKey", String.class);
      assertTrue(stringValue.isPresent(), "String metadata should be present");
      assertEquals("hello", stringValue.get(), "String value should match");
    }

    @Test
    @DisplayName("should return empty for wrong type")
    void shouldReturnEmptyForWrongType() {
      AsyncExecutionContext context =
          new AsyncExecutionContext("func", Optional.empty(), defaultExecutor, "exec-014");

      context.setMetadata("stringKey", "hello");

      Optional<Integer> value = context.getMetadata("stringKey", Integer.class);

      assertFalse(value.isPresent(), "Should return empty for wrong type");
    }

    @Test
    @DisplayName("should get all metadata")
    void shouldGetAllMetadata() {
      AsyncExecutionContext context =
          new AsyncExecutionContext("func", Optional.empty(), defaultExecutor, "exec-015");

      context.setMetadata("key1", "value1");
      context.setMetadata("key2", 42);
      context.setMetadata("key3", true);

      Map<String, Object> allMetadata = context.getAllMetadata();

      assertEquals(3, allMetadata.size(), "Should have 3 metadata entries");
      assertEquals("value1", allMetadata.get("key1"));
      assertEquals(42, allMetadata.get("key2"));
      assertEquals(true, allMetadata.get("key3"));
    }

    @Test
    @DisplayName("should reject null metadata key")
    void shouldRejectNullMetadataKey() {
      AsyncExecutionContext context =
          new AsyncExecutionContext("func", Optional.empty(), defaultExecutor, "exec-016");

      assertThrows(
          NullPointerException.class,
          () -> context.setMetadata(null, "value"),
          "Should throw for null key");
    }
  }

  @Nested
  @DisplayName("ShouldTerminate Tests")
  class ShouldTerminateTests {

    @Test
    @DisplayName("should not terminate initially")
    void shouldNotTerminateInitially() {
      AsyncExecutionContext context =
          new AsyncExecutionContext(
              "func", Optional.of(Duration.ofMinutes(5)), defaultExecutor, "exec-017");

      assertFalse(context.shouldTerminate(), "Should not terminate initially");
    }

    @Test
    @DisplayName("should terminate when cancelled")
    void shouldTerminateWhenCancelled() {
      AsyncExecutionContext context =
          new AsyncExecutionContext("func", Optional.empty(), defaultExecutor, "exec-018");

      context.cancel();

      assertTrue(context.shouldTerminate(), "Should terminate when cancelled");
    }

    @Test
    @DisplayName("should terminate when timed out")
    void shouldTerminateWhenTimedOut() throws InterruptedException {
      AsyncExecutionContext context =
          new AsyncExecutionContext(
              "func", Optional.of(Duration.ofMillis(5)), defaultExecutor, "exec-019");

      Thread.sleep(20);

      assertTrue(context.shouldTerminate(), "Should terminate when timed out");
    }

    @Test
    @DisplayName("should terminate when error set")
    void shouldTerminateWhenErrorSet() {
      AsyncExecutionContext context =
          new AsyncExecutionContext("func", Optional.empty(), defaultExecutor, "exec-020");

      context.setError(new RuntimeException("error"));

      assertTrue(context.shouldTerminate(), "Should terminate when error set");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should produce non-null toString output")
    void shouldProduceNonNullToStringOutput() {
      AsyncExecutionContext context =
          new AsyncExecutionContext("func", Optional.empty(), defaultExecutor, "exec-021");

      assertNotNull(context.toString(), "toString should not return null");
    }

    @Test
    @DisplayName("should include class name in toString")
    void shouldIncludeClassNameInToString() {
      AsyncExecutionContext context =
          new AsyncExecutionContext("func", Optional.empty(), defaultExecutor, "exec-022");

      assertTrue(
          context.toString().contains("AsyncExecutionContext"),
          "toString should contain class name");
    }

    @Test
    @DisplayName("should include key fields in toString")
    void shouldIncludeKeyFieldsInToString() {
      AsyncExecutionContext context =
          new AsyncExecutionContext("myFunction", Optional.empty(), defaultExecutor, "exec-999");

      String str = context.toString();
      assertTrue(str.contains("functionName="), "toString should contain functionName");
      assertTrue(str.contains("myFunction"), "toString should contain function name value");
      assertTrue(str.contains("executionId="), "toString should contain executionId");
      assertTrue(str.contains("exec-999"), "toString should contain execution ID value");
      assertTrue(str.contains("elapsed="), "toString should contain elapsed");
      assertTrue(str.contains("cancelled="), "toString should contain cancelled");
      assertTrue(str.contains("completed="), "toString should contain completed");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should track typical async execution lifecycle")
    void shouldTrackTypicalAsyncExecutionLifecycle() throws InterruptedException {
      AsyncExecutionContext context =
          new AsyncExecutionContext(
              "computeResult",
              Optional.of(Duration.ofSeconds(30)),
              defaultExecutor,
              "exec-lifecycle");

      // Initial state
      assertFalse(context.isCancelled());
      assertFalse(context.isCompleted());
      assertFalse(context.isTimedOut());
      assertFalse(context.shouldTerminate());

      // Set some metadata during execution
      context.setMetadata("inputSize", 1024);
      context.setMetadata("batchId", "batch-001");

      Thread.sleep(5);

      // Verify metadata
      assertEquals(1024, context.getMetadata("inputSize", Integer.class).get());
      assertEquals("batch-001", context.getMetadata("batchId", String.class).get());

      // Verify elapsed time tracking
      assertTrue(context.getElapsedTime().toMillis() >= 5, "Elapsed should be at least 5ms");

      // Complete execution
      context.markCompleted();

      assertTrue(context.isCompleted());
      assertFalse(context.isCancelled());
    }

    @Test
    @DisplayName("should handle cancellation scenario")
    void shouldHandleCancellationScenario() {
      AsyncExecutionContext context =
          new AsyncExecutionContext(
              "longRunningTask", Optional.of(Duration.ofHours(1)), defaultExecutor, "exec-cancel");

      // User cancels the execution
      context.cancel();

      assertTrue(context.isCancelled());
      assertTrue(context.shouldTerminate());
      assertFalse(context.isCompleted());
    }

    @Test
    @DisplayName("should handle error scenario")
    void shouldHandleErrorScenario() {
      AsyncExecutionContext context =
          new AsyncExecutionContext("failingTask", Optional.empty(), defaultExecutor, "exec-error");

      RuntimeException error = new RuntimeException("Execution failed");
      context.setError(error);

      assertTrue(context.isCompleted());
      assertTrue(context.getError().isPresent());
      assertEquals("Execution failed", context.getError().get().getMessage());
      assertTrue(context.shouldTerminate());
    }

    @Test
    @DisplayName("should handle concurrent metadata access")
    void shouldHandleConcurrentMetadataAccess() throws InterruptedException {
      AsyncExecutionContext context =
          new AsyncExecutionContext(
              "concurrentTask", Optional.empty(), defaultExecutor, "exec-concurrent");

      Thread[] threads = new Thread[10];
      for (int i = 0; i < threads.length; i++) {
        final int index = i;
        threads[i] =
            new Thread(
                () -> {
                  context.setMetadata("key-" + index, index);
                  context.getMetadata("key-" + index);
                });
      }

      for (Thread thread : threads) {
        thread.start();
      }

      for (Thread thread : threads) {
        thread.join();
      }

      // All metadata should be set
      Map<String, Object> allMetadata = context.getAllMetadata();
      assertEquals(10, allMetadata.size(), "All 10 metadata entries should be present");
    }
  }
}
