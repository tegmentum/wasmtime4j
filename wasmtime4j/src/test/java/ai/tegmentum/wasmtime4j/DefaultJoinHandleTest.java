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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link DefaultJoinHandle} class.
 *
 * <p>Verifies join, cancel, isDone, and toFuture behavior backed by a CompletableFuture.
 */
@DisplayName("DefaultJoinHandle Tests")
class DefaultJoinHandleTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should throw IllegalArgumentException for null future")
    void shouldThrowForNullFuture() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new DefaultJoinHandle<String>(null),
          "Constructor should reject null future");
    }

    @Test
    @DisplayName("should accept a valid CompletableFuture")
    void shouldAcceptValidFuture() {
      final CompletableFuture<String> future = new CompletableFuture<>();
      final DefaultJoinHandle<String> handle = new DefaultJoinHandle<>(future);

      assertNotNull(handle, "Handle should be created successfully");
    }
  }

  @Nested
  @DisplayName("Join Tests")
  class JoinTests {

    @Test
    @DisplayName("should return value from completed future")
    void shouldReturnValueFromCompletedFuture() throws WasmException, InterruptedException {
      final CompletableFuture<String> future = CompletableFuture.completedFuture("result");
      final DefaultJoinHandle<String> handle = new DefaultJoinHandle<>(future);

      final String result = handle.join();

      assertEquals("result", result, "join should return the completed value");
    }

    @Test
    @DisplayName("should return null from future completed with null")
    void shouldReturnNullFromNullFuture() throws WasmException, InterruptedException {
      final CompletableFuture<String> future = CompletableFuture.completedFuture(null);
      final DefaultJoinHandle<String> handle = new DefaultJoinHandle<>(future);

      final String result = handle.join();

      assertEquals(null, result, "join should return null when future completed with null");
    }

    @Test
    @DisplayName("should throw WasmException when future failed with WasmException")
    void shouldThrowWasmExceptionFromFuture() {
      final CompletableFuture<String> future = new CompletableFuture<>();
      final WasmException cause = new WasmException("wasm error");
      future.completeExceptionally(cause);

      final DefaultJoinHandle<String> handle = new DefaultJoinHandle<>(future);

      final WasmException thrown =
          assertThrows(
              WasmException.class,
              () -> handle.join(),
              "join should throw WasmException when future failed with WasmException");

      assertSame(cause, thrown, "Should throw the original WasmException");
    }

    @Test
    @DisplayName("should unwrap WasmException from CompletionException")
    void shouldUnwrapWasmExceptionFromCompletionException() {
      final CompletableFuture<String> future = new CompletableFuture<>();
      final WasmException innerCause = new WasmException("inner wasm error");
      future.completeExceptionally(new CompletionException(innerCause));

      final DefaultJoinHandle<String> handle = new DefaultJoinHandle<>(future);

      final WasmException thrown =
          assertThrows(
              WasmException.class,
              () -> handle.join(),
              "join should unwrap WasmException from CompletionException");

      assertSame(innerCause, thrown, "Should throw the inner WasmException");
    }

    @Test
    @DisplayName("should wrap non-WasmException in WasmException")
    void shouldWrapNonWasmExceptionInWasmException() {
      final CompletableFuture<String> future = new CompletableFuture<>();
      final RuntimeException cause = new RuntimeException("runtime error");
      future.completeExceptionally(cause);

      final DefaultJoinHandle<String> handle = new DefaultJoinHandle<>(future);

      final WasmException thrown =
          assertThrows(
              WasmException.class,
              () -> handle.join(),
              "join should wrap non-WasmException in WasmException");

      assertSame(cause, thrown.getCause(), "Wrapped WasmException should have original cause");
      assertTrue(
          thrown.getMessage().contains("runtime error"),
          "Message should contain the original error message");
    }

    @Test
    @DisplayName("should throw WasmException when future is cancelled")
    void shouldThrowWasmExceptionWhenCancelled() {
      final CompletableFuture<String> future = new CompletableFuture<>();
      future.cancel(true);

      final DefaultJoinHandle<String> handle = new DefaultJoinHandle<>(future);

      final WasmException thrown =
          assertThrows(
              WasmException.class,
              () -> handle.join(),
              "join should throw WasmException when future is cancelled");

      assertTrue(thrown.getMessage().contains("cancelled"), "Message should mention cancellation");
    }
  }

  @Nested
  @DisplayName("ToFuture Tests")
  class ToFutureTests {

    @Test
    @DisplayName("should return the underlying CompletableFuture")
    void shouldReturnUnderlyingFuture() {
      final CompletableFuture<Integer> future = CompletableFuture.completedFuture(42);
      final DefaultJoinHandle<Integer> handle = new DefaultJoinHandle<>(future);

      assertSame(future, handle.toFuture(), "toFuture should return the underlying future");
    }
  }

  @Nested
  @DisplayName("Cancel Tests")
  class CancelTests {

    @Test
    @DisplayName("should return true when cancelling a pending future")
    void shouldReturnTrueWhenCancellingPendingFuture() {
      final CompletableFuture<String> future = new CompletableFuture<>();
      final DefaultJoinHandle<String> handle = new DefaultJoinHandle<>(future);

      final boolean cancelled = handle.cancel();

      assertTrue(cancelled, "cancel should return true for a pending future");
      assertTrue(handle.isDone(), "isDone should return true after cancellation");
    }

    @Test
    @DisplayName("should return false when cancelling an already completed future")
    void shouldReturnFalseWhenCancellingCompletedFuture() {
      final CompletableFuture<String> future = CompletableFuture.completedFuture("done");
      final DefaultJoinHandle<String> handle = new DefaultJoinHandle<>(future);

      final boolean cancelled = handle.cancel();

      assertFalse(cancelled, "cancel should return false for an already completed future");
    }
  }

  @Nested
  @DisplayName("IsDone Tests")
  class IsDoneTests {

    @Test
    @DisplayName("should return false for a pending future")
    void shouldReturnFalseForPendingFuture() {
      final CompletableFuture<String> future = new CompletableFuture<>();
      final DefaultJoinHandle<String> handle = new DefaultJoinHandle<>(future);

      assertFalse(handle.isDone(), "isDone should return false for a pending future");
    }

    @Test
    @DisplayName("should return true for a completed future")
    void shouldReturnTrueForCompletedFuture() {
      final CompletableFuture<String> future = CompletableFuture.completedFuture("done");
      final DefaultJoinHandle<String> handle = new DefaultJoinHandle<>(future);

      assertTrue(handle.isDone(), "isDone should return true for a completed future");
    }

    @Test
    @DisplayName("should return true for a failed future")
    void shouldReturnTrueForFailedFuture() {
      final CompletableFuture<String> future = new CompletableFuture<>();
      future.completeExceptionally(new RuntimeException("fail"));
      final DefaultJoinHandle<String> handle = new DefaultJoinHandle<>(future);

      assertTrue(handle.isDone(), "isDone should return true for a failed future");
    }

    @Test
    @DisplayName("should return true for a cancelled future")
    void shouldReturnTrueForCancelledFuture() {
      final CompletableFuture<String> future = new CompletableFuture<>();
      future.cancel(true);
      final DefaultJoinHandle<String> handle = new DefaultJoinHandle<>(future);

      assertTrue(handle.isDone(), "isDone should return true for a cancelled future");
    }
  }
}
