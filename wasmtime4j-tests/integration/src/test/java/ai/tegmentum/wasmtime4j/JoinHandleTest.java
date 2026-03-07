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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for {@link JoinHandle} and {@link ConcurrentTask}.
 *
 * <p>Validates that store.submitTask() correctly submits tasks, returns join handles, and supports
 * join/cancel/isDone/toFuture operations across both JNI and Panama implementations.
 */
class JoinHandleTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(JoinHandleTest.class.getName());

  @AfterEach
  void tearDown() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testSubmitTaskAndJoin(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing submitTask and join");
    try (Engine engine = Engine.create();
        Store store = Store.create(engine)) {

      final JoinHandle<Integer> handle = store.submitTask(s -> 40 + 2);

      assertNotNull(handle, "submitTask should return a non-null handle");
      final Integer result = handle.join();
      assertEquals(42, result, "Task result should be 42");
      assertTrue(handle.isDone(), "Handle should be done after join");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testSubmitTaskToFuture(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing submitTask toFuture");
    try (Engine engine = Engine.create();
        Store store = Store.create(engine)) {

      final JoinHandle<String> handle = store.submitTask(s -> "future-result");

      final CompletableFuture<String> future = handle.toFuture();
      assertNotNull(future, "toFuture should return a non-null future");

      final String result = future.get();
      assertEquals("future-result", result, "Future result should match");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testSubmitTaskIsDone(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing submitTask isDone lifecycle");
    try (Engine engine = Engine.create();
        Store store = Store.create(engine)) {

      final JoinHandle<String> handle = store.submitTask(s -> "hello");

      // Wait for completion
      final String result = handle.join();
      assertEquals("hello", result);
      assertTrue(handle.isDone(), "Handle should be done after join");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testSubmitTaskWithException(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing submitTask that throws exception");
    try (Engine engine = Engine.create();
        Store store = Store.create(engine)) {

      final JoinHandle<Void> handle =
          store.submitTask(
              s -> {
                throw new WasmException("intentional failure");
              });

      assertThrows(
          WasmException.class, handle::join, "Joining a failed task should throw WasmException");
      assertTrue(handle.isDone(), "Handle should be done after exception");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testSubmitTaskCancel(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing submitTask cancel on completed task");
    try (Engine engine = Engine.create();
        Store store = Store.create(engine)) {

      final JoinHandle<Integer> handle = store.submitTask(s -> 99);

      // Wait for completion first
      handle.join();

      // Cancel after completion should return false
      final boolean cancelled = handle.cancel();
      assertFalse(cancelled, "Cancelling a completed task should return false");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testSubmitTaskNullThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing submitTask with null task throws");
    try (Engine engine = Engine.create();
        Store store = Store.create(engine)) {

      assertThrows(
          IllegalArgumentException.class,
          () -> store.submitTask(null),
          "submitTask(null) should throw IllegalArgumentException");
    }
  }
}
