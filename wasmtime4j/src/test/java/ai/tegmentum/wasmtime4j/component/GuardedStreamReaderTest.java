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
 * Tests for the {@link GuardedStreamReader} RAII guard.
 *
 * <p>Verifies construction, ownership semantics, close delegation, and use-after-close protection.
 */
@DisplayName("GuardedStreamReader Tests")
class GuardedStreamReaderTest {

  @Nested
  @DisplayName("Construction")
  class ConstructionTests {

    @Test
    @DisplayName("should accept a valid StreamAny")
    void shouldAcceptValidStream() {
      final StreamAny stream = StreamAny.create(1L);
      final GuardedStreamReader guard = new GuardedStreamReader(stream);
      assertTrue(guard.isActive(), "Guard should be active after construction");
      assertSame(stream, guard.getStream(), "getStream should return the wrapped stream");
    }

    @Test
    @DisplayName("should reject null stream")
    void shouldRejectNullStream() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new GuardedStreamReader(null),
          "Should reject null stream");
    }
  }

  @Nested
  @DisplayName("getStream()")
  class GetStreamTests {

    @Test
    @DisplayName("should return the wrapped stream while active")
    void shouldReturnStreamWhileActive() {
      final StreamAny stream = StreamAny.create(42L);
      try (final GuardedStreamReader guard = new GuardedStreamReader(stream)) {
        assertSame(stream, guard.getStream());
      }
    }

    @Test
    @DisplayName("should throw after close")
    void shouldThrowAfterClose() {
      final StreamAny stream = StreamAny.create(1L);
      final GuardedStreamReader guard = new GuardedStreamReader(stream);
      guard.close();
      assertThrows(
          IllegalStateException.class, guard::getStream, "getStream should throw after close");
    }

    @Test
    @DisplayName("should throw after ownership transfer")
    void shouldThrowAfterOwnershipTransfer() {
      final StreamAny stream = StreamAny.create(1L);
      final GuardedStreamReader guard = new GuardedStreamReader(stream);
      guard.intoStream();
      assertThrows(
          IllegalStateException.class, guard::getStream, "getStream should throw after intoStream");
    }
  }

  @Nested
  @DisplayName("getElementType()")
  class GetElementTypeTests {

    @Test
    @DisplayName("should delegate to underlying stream element type")
    void shouldDelegateElementType() {
      final ComponentTypeDescriptor type =
          ComponentTypeDescriptor.fromComponentType(ComponentType.U16);
      final StreamAny stream = StreamAny.createTyped(5L, type, null);
      final GuardedStreamReader guard = new GuardedStreamReader(stream);

      assertTrue(guard.getElementType().isPresent(), "Should have element type");
      assertEquals(
          ComponentType.U16, guard.getElementType().get().getType(), "Element type should match");
    }

    @Test
    @DisplayName("should return empty for untyped stream")
    void shouldReturnEmptyForUntyped() {
      final StreamAny stream = StreamAny.create(1L);
      final GuardedStreamReader guard = new GuardedStreamReader(stream);
      assertTrue(guard.getElementType().isEmpty(), "Untyped stream should have empty element type");
    }
  }

  @Nested
  @DisplayName("close()")
  class CloseTests {

    @Test
    @DisplayName("should close the underlying stream")
    void shouldCloseUnderlyingStream() {
      final AtomicInteger closeCount = new AtomicInteger(0);
      final StreamAny stream = StreamAny.create(1L, closeCount::incrementAndGet);
      final GuardedStreamReader guard = new GuardedStreamReader(stream);

      guard.close();

      assertFalse(guard.isActive(), "Guard should not be active after close");
      assertFalse(stream.isValid(), "Stream should be closed");
      assertEquals(1, closeCount.get(), "Stream close action should be invoked");
    }

    @Test
    @DisplayName("should be idempotent")
    void closeShouldBeIdempotent() {
      final AtomicInteger closeCount = new AtomicInteger(0);
      final StreamAny stream = StreamAny.create(1L, closeCount::incrementAndGet);
      final GuardedStreamReader guard = new GuardedStreamReader(stream);

      guard.close();
      guard.close();
      guard.close();

      assertEquals(1, closeCount.get(), "Close action should only be invoked once");
    }

    @Test
    @DisplayName("try-with-resources should close the stream")
    void tryWithResourcesShouldClose() {
      final AtomicInteger closeCount = new AtomicInteger(0);
      final StreamAny stream = StreamAny.create(1L, closeCount::incrementAndGet);

      try (final GuardedStreamReader guard = new GuardedStreamReader(stream)) {
        assertTrue(guard.isActive(), "Guard should be active inside try block");
      }

      assertEquals(1, closeCount.get(), "Stream should be closed by try-with-resources");
      assertFalse(stream.isValid(), "Stream should be invalid after try-with-resources");
    }
  }

  @Nested
  @DisplayName("intoStream() - Ownership Transfer")
  class IntoStreamTests {

    @Test
    @DisplayName("should transfer ownership and prevent guard from closing stream")
    void shouldTransferOwnership() {
      final AtomicInteger closeCount = new AtomicInteger(0);
      final StreamAny stream = StreamAny.create(1L, closeCount::incrementAndGet);

      StreamAny transferred;
      try (final GuardedStreamReader guard = new GuardedStreamReader(stream)) {
        transferred = guard.intoStream();
      }

      // Guard closed but stream should still be valid because ownership was transferred
      assertSame(stream, transferred, "Should return the same stream");
      assertTrue(transferred.isValid(), "Transferred stream should still be valid");
      assertEquals(0, closeCount.get(), "Close action should NOT be invoked");

      // Clean up
      transferred.close();
      assertEquals(1, closeCount.get(), "Close action should be invoked when caller closes");
    }

    @Test
    @DisplayName("should throw on second call to intoStream")
    void shouldThrowOnSecondIntoStream() {
      final StreamAny stream = StreamAny.create(1L);
      final GuardedStreamReader guard = new GuardedStreamReader(stream);
      guard.intoStream();
      assertThrows(
          IllegalStateException.class, guard::intoStream, "Second intoStream should throw");
    }

    @Test
    @DisplayName("should throw after close")
    void shouldThrowAfterClose() {
      final StreamAny stream = StreamAny.create(1L);
      final GuardedStreamReader guard = new GuardedStreamReader(stream);
      guard.close();
      assertThrows(
          IllegalStateException.class, guard::intoStream, "intoStream should throw after close");
    }
  }

  @Nested
  @DisplayName("isActive()")
  class IsActiveTests {

    @Test
    @DisplayName("should be true before close or transfer")
    void shouldBeTrueBeforeCloseOrTransfer() {
      final GuardedStreamReader guard = new GuardedStreamReader(StreamAny.create(1L));
      assertTrue(guard.isActive());
    }

    @Test
    @DisplayName("should be false after close")
    void shouldBeFalseAfterClose() {
      final GuardedStreamReader guard = new GuardedStreamReader(StreamAny.create(1L));
      guard.close();
      assertFalse(guard.isActive());
    }

    @Test
    @DisplayName("should be false after intoStream")
    void shouldBeFalseAfterIntoStream() {
      final GuardedStreamReader guard = new GuardedStreamReader(StreamAny.create(1L));
      guard.intoStream();
      assertFalse(guard.isActive());
    }
  }
}
