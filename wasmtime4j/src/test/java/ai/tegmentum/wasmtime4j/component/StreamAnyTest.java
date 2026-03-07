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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link StreamAny} interface and its implementations.
 *
 * <p>Verifies factory methods, handle lifecycle, close actions, typed streams, and equality
 * semantics.
 */
@DisplayName("StreamAny Tests")
class StreamAnyTest {

  @Nested
  @DisplayName("Factory Method: create(handle)")
  class CreateWithHandleTests {

    @Test
    @DisplayName("should create a valid stream with the given handle")
    void shouldCreateValidStream() {
      final StreamAny stream = StreamAny.create(1L);
      assertEquals(1L, stream.getHandle(), "Handle should match the value passed to create");
      assertTrue(stream.isValid(), "Newly created stream should be valid");
      assertTrue(stream.getElementType().isEmpty(), "Default stream should have no element type");
    }

    @Test
    @DisplayName("should reject zero handle")
    void shouldRejectZeroHandle() {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> StreamAny.create(0L));
      assertTrue(ex.getMessage().contains("positive"), "Error should mention 'positive': " + ex);
    }

    @Test
    @DisplayName("should reject negative handle")
    void shouldRejectNegativeHandle() {
      assertThrows(IllegalArgumentException.class, () -> StreamAny.create(-5L));
    }
  }

  @Nested
  @DisplayName("Factory Method: create(handle, closeAction)")
  class CreateWithCloseActionTests {

    @Test
    @DisplayName("should invoke close action exactly once on close")
    void shouldInvokeCloseActionOnce() {
      final AtomicInteger closeCount = new AtomicInteger(0);
      final StreamAny stream = StreamAny.create(42L, closeCount::incrementAndGet);

      assertTrue(stream.isValid(), "Stream should be valid before close");
      stream.close();
      assertFalse(stream.isValid(), "Stream should be invalid after close");
      assertEquals(1, closeCount.get(), "Close action should be invoked exactly once");

      // Idempotent close
      stream.close();
      assertEquals(1, closeCount.get(), "Second close should not invoke action again");
    }

    @Test
    @DisplayName("should allow null close action without error")
    void shouldAllowNullCloseAction() {
      final StreamAny stream = StreamAny.create(1L, null);
      stream.close();
      assertFalse(stream.isValid(), "Stream should be invalid after close");
    }
  }

  @Nested
  @DisplayName("Factory Method: createTyped(handle, elementType, closeAction)")
  class CreateTypedTests {

    @Test
    @DisplayName("should carry element type information")
    void shouldCarryElementType() {
      final ComponentTypeDescriptor type =
          ComponentTypeDescriptor.fromComponentType(ComponentType.S32);
      final StreamAny stream = StreamAny.createTyped(100L, type, null);

      assertEquals(100L, stream.getHandle(), "Handle should match");
      assertTrue(stream.isValid(), "Stream should be valid");
      final Optional<ComponentTypeDescriptor> elementType = stream.getElementType();
      assertTrue(elementType.isPresent(), "Typed stream should have element type");
      assertEquals(ComponentType.S32, elementType.get().getType(), "Element type should be S32");
    }

    @Test
    @DisplayName("should reject null elementType")
    void shouldRejectNullElementType() {
      assertThrows(IllegalArgumentException.class, () -> StreamAny.createTyped(1L, null, null));
    }

    @Test
    @DisplayName("should reject zero handle")
    void shouldRejectZeroHandle() {
      final ComponentTypeDescriptor type =
          ComponentTypeDescriptor.fromComponentType(ComponentType.U8);
      assertThrows(IllegalArgumentException.class, () -> StreamAny.createTyped(0L, type, null));
    }

    @Test
    @DisplayName("should invoke close action on typed stream")
    void shouldInvokeCloseActionOnTyped() {
      final AtomicInteger closeCount = new AtomicInteger(0);
      final ComponentTypeDescriptor type =
          ComponentTypeDescriptor.fromComponentType(ComponentType.STRING);
      final StreamAny stream = StreamAny.createTyped(7L, type, closeCount::incrementAndGet);

      stream.close();
      assertEquals(1, closeCount.get(), "Close action should be invoked on typed stream");
      assertFalse(stream.isValid(), "Typed stream should be invalid after close");
    }
  }

  @Nested
  @DisplayName("Lifecycle and Validity")
  class LifecycleTests {

    @Test
    @DisplayName("close should be idempotent")
    void closeShouldBeIdempotent() {
      final AtomicInteger closeCount = new AtomicInteger(0);
      final StreamAny stream = StreamAny.create(1L, closeCount::incrementAndGet);

      stream.close();
      stream.close();
      stream.close();

      assertEquals(1, closeCount.get(), "Close action invoked multiple times");
      assertFalse(stream.isValid(), "Stream should remain invalid after multiple closes");
    }
  }

  @Nested
  @DisplayName("Equality and HashCode")
  class EqualityTests {

    @Test
    @DisplayName("default streams with same handle should be equal")
    void defaultStreamsEqual() {
      final StreamAny a = StreamAny.create(99L);
      final StreamAny b = StreamAny.create(99L);
      assertEquals(a, b, "Streams with same handle should be equal");
      assertEquals(a.hashCode(), b.hashCode(), "Equal streams should have same hashCode");
    }

    @Test
    @DisplayName("default streams with different handles should not be equal")
    void defaultStreamsNotEqual() {
      final StreamAny a = StreamAny.create(1L);
      final StreamAny b = StreamAny.create(2L);
      assertNotEquals(a, b, "Streams with different handles should not be equal");
    }

    @Test
    @DisplayName("typed streams with same handle should be equal")
    void typedStreamsEqual() {
      final ComponentTypeDescriptor type =
          ComponentTypeDescriptor.fromComponentType(ComponentType.U32);
      final StreamAny a = StreamAny.createTyped(50L, type, null);
      final StreamAny b = StreamAny.createTyped(50L, type, null);
      assertEquals(a, b, "Typed streams with same handle should be equal");
      assertEquals(a.hashCode(), b.hashCode(), "Equal typed streams should have same hashCode");
    }

    @Test
    @DisplayName("default and typed streams with same handle should not be equal")
    void defaultAndTypedNotEqual() {
      final ComponentTypeDescriptor type =
          ComponentTypeDescriptor.fromComponentType(ComponentType.U32);
      final StreamAny defaultStream = StreamAny.create(50L);
      final StreamAny typedStream = StreamAny.createTyped(50L, type, null);
      assertNotEquals(
          defaultStream,
          typedStream,
          "Default and typed streams should not be equal even with same handle");
    }
  }

  @Nested
  @DisplayName("toString")
  class ToStringTests {

    @Test
    @DisplayName("default stream toString should include handle and validity")
    void defaultToString() {
      final StreamAny stream = StreamAny.create(42L);
      final String str = stream.toString();
      assertTrue(str.contains("42"), "toString should contain handle: " + str);
      assertTrue(str.contains("true"), "toString should show valid=true: " + str);

      stream.close();
      final String closed = stream.toString();
      assertTrue(
          closed.contains("false"), "toString should show valid=false after close: " + closed);
    }

    @Test
    @DisplayName("typed stream toString should include type information")
    void typedToString() {
      final ComponentTypeDescriptor type =
          ComponentTypeDescriptor.fromComponentType(ComponentType.F64);
      final StreamAny stream = StreamAny.createTyped(10L, type, null);
      final String str = stream.toString();
      assertTrue(str.contains("10"), "toString should contain handle: " + str);
      assertTrue(str.toLowerCase().contains("f64"), "toString should contain type info: " + str);
    }
  }
}
