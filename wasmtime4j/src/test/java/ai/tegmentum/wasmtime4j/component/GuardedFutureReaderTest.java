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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link GuardedFutureReader} RAII guard.
 *
 * <p>Verifies construction, ownership semantics, close delegation, and use-after-close protection.
 */
@DisplayName("GuardedFutureReader Tests")
class GuardedFutureReaderTest {

  @Nested
  @DisplayName("Construction")
  class ConstructionTests {

    @Test
    @DisplayName("should accept a valid FutureAny")
    void shouldAcceptValidFuture() {
      final FutureAny future = FutureAny.create(1L);
      final GuardedFutureReader guard = new GuardedFutureReader(future);
      assertTrue(guard.isActive(), "Guard should be active after construction");
      assertSame(future, guard.getFuture(), "getFuture should return the wrapped future");
    }

    @Test
    @DisplayName("should reject null future")
    void shouldRejectNullFuture() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new GuardedFutureReader(null),
          "Should reject null future");
    }
  }

  @Nested
  @DisplayName("getFuture()")
  class GetFutureTests {

    @Test
    @DisplayName("should return the wrapped future while active")
    void shouldReturnFutureWhileActive() {
      final FutureAny future = FutureAny.create(42L);
      try (final GuardedFutureReader guard = new GuardedFutureReader(future)) {
        assertSame(future, guard.getFuture());
      }
    }

    @Test
    @DisplayName("should throw after close")
    void shouldThrowAfterClose() {
      final FutureAny future = FutureAny.create(1L);
      final GuardedFutureReader guard = new GuardedFutureReader(future);
      guard.close();
      assertThrows(
          IllegalStateException.class, guard::getFuture, "getFuture should throw after close");
    }

    @Test
    @DisplayName("should throw after ownership transfer")
    void shouldThrowAfterOwnershipTransfer() {
      final FutureAny future = FutureAny.create(1L);
      final GuardedFutureReader guard = new GuardedFutureReader(future);
      guard.intoFuture();
      assertThrows(
          IllegalStateException.class, guard::getFuture, "getFuture should throw after intoFuture");
    }
  }

  @Nested
  @DisplayName("getPayloadType()")
  class GetPayloadTypeTests {

    @Test
    @DisplayName("should delegate to underlying future payload type")
    void shouldDelegatePayloadType() {
      final ComponentTypeDescriptor type =
          ComponentTypeDescriptor.fromComponentType(ComponentType.U16);
      final FutureAny future = FutureAny.createTyped(5L, type, null);
      final GuardedFutureReader guard = new GuardedFutureReader(future);

      assertTrue(guard.getPayloadType().isPresent(), "Should have payload type");
      assertEquals(
          ComponentType.U16, guard.getPayloadType().get().getType(), "Payload type should match");
    }

    @Test
    @DisplayName("should return empty for untyped future")
    void shouldReturnEmptyForUntyped() {
      final FutureAny future = FutureAny.create(1L);
      final GuardedFutureReader guard = new GuardedFutureReader(future);
      assertTrue(guard.getPayloadType().isEmpty(), "Untyped future should have empty payload type");
    }
  }

  @Nested
  @DisplayName("close()")
  class CloseTests {

    @Test
    @DisplayName("should close the underlying future")
    void shouldCloseUnderlyingFuture() {
      final AtomicInteger closeCount = new AtomicInteger(0);
      final FutureAny future = FutureAny.create(1L, closeCount::incrementAndGet);
      final GuardedFutureReader guard = new GuardedFutureReader(future);

      guard.close();

      assertFalse(guard.isActive(), "Guard should not be active after close");
      assertFalse(future.isValid(), "Future should be closed");
      assertEquals(1, closeCount.get(), "Future close action should be invoked");
    }

    @Test
    @DisplayName("should be idempotent")
    void closeShouldBeIdempotent() {
      final AtomicInteger closeCount = new AtomicInteger(0);
      final FutureAny future = FutureAny.create(1L, closeCount::incrementAndGet);
      final GuardedFutureReader guard = new GuardedFutureReader(future);

      guard.close();
      guard.close();
      guard.close();

      assertEquals(1, closeCount.get(), "Close action should only be invoked once");
    }

    @Test
    @DisplayName("try-with-resources should close the future")
    void tryWithResourcesShouldClose() {
      final AtomicInteger closeCount = new AtomicInteger(0);
      final FutureAny future = FutureAny.create(1L, closeCount::incrementAndGet);

      try (final GuardedFutureReader guard = new GuardedFutureReader(future)) {
        assertTrue(guard.isActive(), "Guard should be active inside try block");
      }

      assertEquals(1, closeCount.get(), "Future should be closed by try-with-resources");
      assertFalse(future.isValid(), "Future should be invalid after try-with-resources");
    }
  }

  @Nested
  @DisplayName("intoFuture() - Ownership Transfer")
  class IntoFutureTests {

    @Test
    @DisplayName("should transfer ownership and prevent guard from closing future")
    void shouldTransferOwnership() {
      final AtomicInteger closeCount = new AtomicInteger(0);
      final FutureAny future = FutureAny.create(1L, closeCount::incrementAndGet);

      FutureAny transferred;
      try (final GuardedFutureReader guard = new GuardedFutureReader(future)) {
        transferred = guard.intoFuture();
      }

      // Guard closed but future should still be valid because ownership was transferred
      assertSame(future, transferred, "Should return the same future");
      assertTrue(transferred.isValid(), "Transferred future should still be valid");
      assertEquals(0, closeCount.get(), "Close action should NOT be invoked");

      // Clean up
      transferred.close();
      assertEquals(1, closeCount.get(), "Close action should be invoked when caller closes");
    }

    @Test
    @DisplayName("should throw on second call to intoFuture")
    void shouldThrowOnSecondIntoFuture() {
      final FutureAny future = FutureAny.create(1L);
      final GuardedFutureReader guard = new GuardedFutureReader(future);
      guard.intoFuture();
      assertThrows(
          IllegalStateException.class, guard::intoFuture, "Second intoFuture should throw");
    }

    @Test
    @DisplayName("should throw after close")
    void shouldThrowAfterClose() {
      final FutureAny future = FutureAny.create(1L);
      final GuardedFutureReader guard = new GuardedFutureReader(future);
      guard.close();
      assertThrows(
          IllegalStateException.class, guard::intoFuture, "intoFuture should throw after close");
    }
  }

  @Nested
  @DisplayName("isActive()")
  class IsActiveTests {

    @Test
    @DisplayName("should be true before close or transfer")
    void shouldBeTrueBeforeCloseOrTransfer() {
      final GuardedFutureReader guard = new GuardedFutureReader(FutureAny.create(1L));
      assertTrue(guard.isActive());
    }

    @Test
    @DisplayName("should be false after close")
    void shouldBeFalseAfterClose() {
      final GuardedFutureReader guard = new GuardedFutureReader(FutureAny.create(1L));
      guard.close();
      assertFalse(guard.isActive());
    }

    @Test
    @DisplayName("should be false after intoFuture")
    void shouldBeFalseAfterIntoFuture() {
      final GuardedFutureReader guard = new GuardedFutureReader(FutureAny.create(1L));
      guard.intoFuture();
      assertFalse(guard.isActive());
    }
  }
}
