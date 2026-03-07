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
 * Tests for the {@link FutureAny} interface and its implementations.
 *
 * <p>Verifies factory methods, handle lifecycle, close actions, typed futures, and equality
 * semantics.
 */
@DisplayName("FutureAny Tests")
class FutureAnyTest {

  @Nested
  @DisplayName("Factory Method: create(handle)")
  class CreateWithHandleTests {

    @Test
    @DisplayName("should create a valid future with the given handle")
    void shouldCreateValidFuture() {
      final FutureAny future = FutureAny.create(1L);
      assertEquals(1L, future.getHandle(), "Handle should match the value passed to create");
      assertTrue(future.isValid(), "Newly created future should be valid");
      assertTrue(future.getPayloadType().isEmpty(), "Default future should have no payload type");
    }

    @Test
    @DisplayName("should reject zero handle")
    void shouldRejectZeroHandle() {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> FutureAny.create(0L));
      assertTrue(ex.getMessage().contains("positive"), "Error should mention 'positive': " + ex);
    }

    @Test
    @DisplayName("should reject negative handle")
    void shouldRejectNegativeHandle() {
      assertThrows(IllegalArgumentException.class, () -> FutureAny.create(-5L));
    }
  }

  @Nested
  @DisplayName("Factory Method: create(handle, closeAction)")
  class CreateWithCloseActionTests {

    @Test
    @DisplayName("should invoke close action exactly once on close")
    void shouldInvokeCloseActionOnce() {
      final AtomicInteger closeCount = new AtomicInteger(0);
      final FutureAny future = FutureAny.create(42L, closeCount::incrementAndGet);

      assertTrue(future.isValid(), "Future should be valid before close");
      future.close();
      assertFalse(future.isValid(), "Future should be invalid after close");
      assertEquals(1, closeCount.get(), "Close action should be invoked exactly once");

      // Idempotent close
      future.close();
      assertEquals(1, closeCount.get(), "Second close should not invoke action again");
    }

    @Test
    @DisplayName("should allow null close action without error")
    void shouldAllowNullCloseAction() {
      final FutureAny future = FutureAny.create(1L, null);
      future.close();
      assertFalse(future.isValid(), "Future should be invalid after close");
    }
  }

  @Nested
  @DisplayName("Factory Method: createTyped(handle, payloadType, closeAction)")
  class CreateTypedTests {

    @Test
    @DisplayName("should carry payload type information")
    void shouldCarryPayloadType() {
      final ComponentTypeDescriptor type =
          ComponentTypeDescriptor.fromComponentType(ComponentType.S32);
      final FutureAny future = FutureAny.createTyped(100L, type, null);

      assertEquals(100L, future.getHandle(), "Handle should match");
      assertTrue(future.isValid(), "Future should be valid");
      final Optional<ComponentTypeDescriptor> payloadType = future.getPayloadType();
      assertTrue(payloadType.isPresent(), "Typed future should have payload type");
      assertEquals(ComponentType.S32, payloadType.get().getType(), "Payload type should be S32");
    }

    @Test
    @DisplayName("should reject null payloadType")
    void shouldRejectNullPayloadType() {
      assertThrows(IllegalArgumentException.class, () -> FutureAny.createTyped(1L, null, null));
    }

    @Test
    @DisplayName("should reject zero handle")
    void shouldRejectZeroHandle() {
      final ComponentTypeDescriptor type =
          ComponentTypeDescriptor.fromComponentType(ComponentType.U8);
      assertThrows(IllegalArgumentException.class, () -> FutureAny.createTyped(0L, type, null));
    }

    @Test
    @DisplayName("should invoke close action on typed future")
    void shouldInvokeCloseActionOnTyped() {
      final AtomicInteger closeCount = new AtomicInteger(0);
      final ComponentTypeDescriptor type =
          ComponentTypeDescriptor.fromComponentType(ComponentType.STRING);
      final FutureAny future = FutureAny.createTyped(7L, type, closeCount::incrementAndGet);

      future.close();
      assertEquals(1, closeCount.get(), "Close action should be invoked on typed future");
      assertFalse(future.isValid(), "Typed future should be invalid after close");
    }
  }

  @Nested
  @DisplayName("Lifecycle and Validity")
  class LifecycleTests {

    @Test
    @DisplayName("close should be idempotent")
    void closeShouldBeIdempotent() {
      final AtomicInteger closeCount = new AtomicInteger(0);
      final FutureAny future = FutureAny.create(1L, closeCount::incrementAndGet);

      future.close();
      future.close();
      future.close();

      assertEquals(1, closeCount.get(), "Close action invoked multiple times");
      assertFalse(future.isValid(), "Future should remain invalid after multiple closes");
    }
  }

  @Nested
  @DisplayName("Equality and HashCode")
  class EqualityTests {

    @Test
    @DisplayName("default futures with same handle should be equal")
    void defaultFuturesEqual() {
      final FutureAny a = FutureAny.create(99L);
      final FutureAny b = FutureAny.create(99L);
      assertEquals(a, b, "Futures with same handle should be equal");
      assertEquals(a.hashCode(), b.hashCode(), "Equal futures should have same hashCode");
    }

    @Test
    @DisplayName("default futures with different handles should not be equal")
    void defaultFuturesNotEqual() {
      final FutureAny a = FutureAny.create(1L);
      final FutureAny b = FutureAny.create(2L);
      assertNotEquals(a, b, "Futures with different handles should not be equal");
    }

    @Test
    @DisplayName("typed futures with same handle should be equal")
    void typedFuturesEqual() {
      final ComponentTypeDescriptor type =
          ComponentTypeDescriptor.fromComponentType(ComponentType.U32);
      final FutureAny a = FutureAny.createTyped(50L, type, null);
      final FutureAny b = FutureAny.createTyped(50L, type, null);
      assertEquals(a, b, "Typed futures with same handle should be equal");
      assertEquals(a.hashCode(), b.hashCode(), "Equal typed futures should have same hashCode");
    }

    @Test
    @DisplayName("default and typed futures with same handle should not be equal")
    void defaultAndTypedNotEqual() {
      final ComponentTypeDescriptor type =
          ComponentTypeDescriptor.fromComponentType(ComponentType.U32);
      final FutureAny defaultFuture = FutureAny.create(50L);
      final FutureAny typedFuture = FutureAny.createTyped(50L, type, null);
      assertNotEquals(
          defaultFuture,
          typedFuture,
          "Default and typed futures should not be equal even with same handle");
    }
  }

  @Nested
  @DisplayName("toString")
  class ToStringTests {

    @Test
    @DisplayName("default future toString should include handle and validity")
    void defaultToString() {
      final FutureAny future = FutureAny.create(42L);
      final String str = future.toString();
      assertTrue(str.contains("42"), "toString should contain handle: " + str);
      assertTrue(str.contains("true"), "toString should show valid=true: " + str);

      future.close();
      final String closed = future.toString();
      assertTrue(
          closed.contains("false"), "toString should show valid=false after close: " + closed);
    }

    @Test
    @DisplayName("typed future toString should include type information")
    void typedToString() {
      final ComponentTypeDescriptor type =
          ComponentTypeDescriptor.fromComponentType(ComponentType.F64);
      final FutureAny future = FutureAny.createTyped(10L, type, null);
      final String str = future.toString();
      assertTrue(str.contains("10"), "toString should contain handle: " + str);
      assertTrue(str.toLowerCase().contains("f64"), "toString should contain type info: " + str);
    }
  }
}
