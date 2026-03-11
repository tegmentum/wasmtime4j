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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ResourceAny}.
 *
 * @since 1.1.0
 */
@DisplayName("ResourceAny")
class ResourceAnyTest {

  @Nested
  @DisplayName("resourceNew")
  class ResourceNew {

    @Test
    @DisplayName("rejects null store")
    void rejectsNullStore() {
      assertThrows(IllegalArgumentException.class, () -> ResourceAny.resourceNew(null, 1, 42));
    }
  }

  @Nested
  @DisplayName("fromNative")
  class FromNative {

    @Test
    @DisplayName("creates resource with native handle")
    void createsWithNativeHandle() {
      final ResourceAny res = ResourceAny.fromNative(5, true, 100, 999L, null);
      assertEquals(5, res.getTypeId());
      assertTrue(res.isOwned());
      assertFalse(res.isBorrowed());
      assertEquals(999L, res.getNativeHandle());
    }

    @Test
    @DisplayName("creates borrowed resource")
    void createsBorrowed() {
      final ResourceAny res = ResourceAny.fromNative(3, false, 50, 0, null);
      assertFalse(res.isOwned());
      assertTrue(res.isBorrowed());
    }
  }

  @Nested
  @DisplayName("resourceRep")
  class ResourceRep {

    @Test
    @DisplayName("returns rep value")
    void returnsRepValue() throws WasmException {
      final ResourceAny res = ResourceAny.fromNative(1, true, 42, 0, null);
      assertEquals(42, res.resourceRep(null));
    }

    @Test
    @DisplayName("throws after drop")
    void throwsAfterDrop() throws WasmException {
      final ResourceAny res = ResourceAny.fromNative(1, true, 42, 0, null);
      res.resourceDrop(null);
      assertThrows(WasmException.class, () -> res.resourceRep(null));
    }
  }

  @Nested
  @DisplayName("resourceDrop")
  class ResourceDrop {

    @Test
    @DisplayName("drops resource")
    void dropsResource() throws WasmException {
      final ResourceAny res = ResourceAny.fromNative(1, true, 42, 0, null);
      res.resourceDrop(null);
    }

    @Test
    @DisplayName("throws on double drop")
    void throwsOnDoubleDrop() throws WasmException {
      final ResourceAny res = ResourceAny.fromNative(1, true, 42, 0, null);
      res.resourceDrop(null);
      assertThrows(WasmException.class, () -> res.resourceDrop(null));
    }

    @Test
    @DisplayName("invokes lifecycle callback")
    void invokesLifecycleCallback() throws WasmException {
      final AtomicBoolean dropped = new AtomicBoolean(false);
      final ResourceAny.ResourceLifecycleCallback lifecycle = nativeHandle -> dropped.set(true);
      final ResourceAny res = ResourceAny.fromNative(1, true, 42, 123L, lifecycle);
      res.resourceDrop(null);
      assertTrue(dropped.get());
    }

    @Test
    @DisplayName("does not invoke lifecycle for zero native handle")
    void noLifecycleForZeroHandle() throws WasmException {
      final AtomicBoolean dropped = new AtomicBoolean(false);
      final ResourceAny.ResourceLifecycleCallback lifecycle = nativeHandle -> dropped.set(true);
      final ResourceAny res = ResourceAny.fromNative(1, true, 42, 0, lifecycle);
      res.resourceDrop(null);
      assertFalse(dropped.get());
    }
  }

  @Nested
  @DisplayName("getNativeHandle")
  class GetNativeHandle {

    @Test
    @DisplayName("returns native handle when set")
    void returnsNativeHandle() {
      final ResourceAny res = ResourceAny.fromNative(1, true, 42, 999L, null);
      assertEquals(999L, res.getNativeHandle());
    }

    @Test
    @DisplayName("returns rep when native handle is zero")
    void returnsRepWhenZero() {
      final ResourceAny res = ResourceAny.fromNative(1, true, 42, 0, null);
      assertEquals(42, res.getNativeHandle());
    }
  }

  @Nested
  @DisplayName("toString")
  class ToStringTests {

    @Test
    @DisplayName("includes type id and owned status")
    void includesDetails() {
      final ResourceAny res = ResourceAny.fromNative(5, true, 42, 100L, null);
      final String str = res.toString();
      assertTrue(str.contains("typeId=5"));
      assertTrue(str.contains("owned=true"));
      assertTrue(str.contains("rep=42"));
    }
  }
}
