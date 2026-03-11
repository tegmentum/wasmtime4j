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
package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests for {@link Store} default methods.
 *
 * <p>These tests use Mockito mocks to exercise default method behavior without requiring a native
 * runtime.
 */
@DisplayName("Store Default Method Tests")
@SuppressWarnings({"unchecked", "deprecation"})
class StoreTest {

  @Nested
  @DisplayName("runConcurrent() Default Method")
  class RunConcurrentTests {

    @Test
    @DisplayName("should throw for null task")
    void shouldThrowForNullTask() {
      Store store = Mockito.mock(Store.class, Mockito.CALLS_REAL_METHODS);
      assertThrows(IllegalArgumentException.class, () -> store.runConcurrent(null));
    }

    @Test
    @DisplayName("should execute task and return result")
    void shouldExecuteTaskAndReturnResult() throws ExecutionException, InterruptedException {
      Store store = Mockito.mock(Store.class, Mockito.CALLS_REAL_METHODS);
      CompletableFuture<String> future = store.runConcurrent(s -> "done");
      assertNotNull(future);
      assertEquals("done", future.get());
    }

    @Test
    @DisplayName("should propagate exception from task")
    void shouldPropagateExceptionFromTask() {
      Store store = Mockito.mock(Store.class, Mockito.CALLS_REAL_METHODS);
      CompletableFuture<String> future =
          store.runConcurrent(
              s -> {
                throw new RuntimeException("task failed");
              });
      assertThrows(ExecutionException.class, future::get);
    }
  }

  @Nested
  @DisplayName("spawn() Default Method")
  class SpawnTests {

    @Test
    @DisplayName("should throw for null task")
    void shouldThrowForNullTask() {
      Store store = Mockito.mock(Store.class, Mockito.CALLS_REAL_METHODS);
      @SuppressWarnings("deprecation")
      final Runnable callSpawn = () -> store.spawn(null);
      assertThrows(IllegalArgumentException.class, callSpawn::run);
    }
  }

  @Nested
  @DisplayName("submitTask() Default Method")
  class SubmitTaskTests {

    @Test
    @DisplayName("should throw for null task")
    void shouldThrowForNullTask() {
      Store store = Mockito.mock(Store.class, Mockito.CALLS_REAL_METHODS);
      assertThrows(IllegalArgumentException.class, () -> store.submitTask(null));
    }

    @Test
    @DisplayName("should execute task and produce result")
    void shouldExecuteTaskAndProduceResult() throws Exception {
      Store store = Mockito.mock(Store.class, Mockito.CALLS_REAL_METHODS);
      JoinHandle<Integer> handle = store.submitTask(s -> 42);
      assertNotNull(handle);
      assertEquals(42, handle.join());
    }
  }
}
