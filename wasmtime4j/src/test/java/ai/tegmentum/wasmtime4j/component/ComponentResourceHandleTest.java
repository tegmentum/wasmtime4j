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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentResourceHandle}.
 *
 * @since 1.1.0
 */
@DisplayName("ComponentResourceHandle")
class ComponentResourceHandleTest {

  @Nested
  @DisplayName("own factory")
  class OwnFactory {

    @Test
    @DisplayName("creates owned handle")
    void createsOwnedHandle() {
      final ComponentResourceHandle handle = ComponentResourceHandle.own("file", 42);
      assertEquals("file", handle.getResourceType());
      assertEquals(42, handle.getIndex());
      assertTrue(handle.isOwned());
      assertFalse(handle.isBorrowed());
    }

    @Test
    @DisplayName("rejects null resource type")
    void rejectsNullResourceType() {
      assertThrows(IllegalArgumentException.class, () -> ComponentResourceHandle.own(null, 0));
    }
  }

  @Nested
  @DisplayName("borrow factory")
  class BorrowFactory {

    @Test
    @DisplayName("creates borrowed handle")
    void createsBorrowedHandle() {
      final ComponentResourceHandle handle = ComponentResourceHandle.borrow("stream", 7);
      assertEquals("stream", handle.getResourceType());
      assertEquals(7, handle.getIndex());
      assertFalse(handle.isOwned());
      assertTrue(handle.isBorrowed());
    }
  }

  @Nested
  @DisplayName("ownWithHost")
  class OwnWithHost {

    @Test
    @DisplayName("wraps host object")
    void wrapsHostObject() {
      final String hostObj = "myResource";
      final ComponentResourceHandle handle = ComponentResourceHandle.ownWithHost("res", 1, hostObj);
      assertTrue(handle.isOwned());
      assertEquals("myResource", handle.getHostObject(String.class));
    }

    @Test
    @DisplayName("throws on wrong type cast")
    void throwsOnWrongType() {
      final ComponentResourceHandle handle =
          ComponentResourceHandle.ownWithHost("res", 1, "string");
      assertThrows(ClassCastException.class, () -> handle.getHostObject(Integer.class));
    }
  }

  @Nested
  @DisplayName("borrowWithHost")
  class BorrowWithHost {

    @Test
    @DisplayName("creates borrowed handle with host object")
    void createsBorrowedWithHost() {
      final Integer hostObj = 42;
      final ComponentResourceHandle handle =
          ComponentResourceHandle.borrowWithHost("counter", 3, hostObj);
      assertFalse(handle.isOwned());
      assertTrue(handle.isBorrowed());
      assertEquals(42, handle.getHostObject(Integer.class));
    }
  }

  @Nested
  @DisplayName("getHostObject")
  class GetHostObject {

    @Test
    @DisplayName("throws when no host object")
    void throwsWhenNoHostObject() {
      final ComponentResourceHandle handle = ComponentResourceHandle.own("file", 0);
      assertThrows(IllegalStateException.class, () -> handle.getHostObject(Object.class));
    }
  }

  @Nested
  @DisplayName("getNativeHandle")
  class GetNativeHandle {

    @Test
    @DisplayName("returns -1 for non-native handles")
    void returnsMinusOneForNonNative() {
      final ComponentResourceHandle handle = ComponentResourceHandle.own("res", 5);
      assertEquals(-1, handle.getNativeHandle());
    }

    @Test
    @DisplayName("returns native handle when set")
    void returnsNativeHandle() {
      final ComponentResourceHandle handle =
          ComponentResourceHandle.ownWithNativeHandle("res", 5, 12345L);
      assertEquals(12345L, handle.getNativeHandle());
      assertTrue(handle.isOwned());
    }

    @Test
    @DisplayName("borrowWithNativeHandle returns native handle")
    void borrowWithNativeHandle() {
      final ComponentResourceHandle handle =
          ComponentResourceHandle.borrowWithNativeHandle("res", 3, 99L);
      assertEquals(99L, handle.getNativeHandle());
      assertTrue(handle.isBorrowed());
    }
  }

  @Nested
  @DisplayName("equals and hashCode")
  class EqualsAndHashCode {

    @Test
    @DisplayName("equal handles are equal")
    void equalHandlesAreEqual() {
      final ComponentResourceHandle h1 = ComponentResourceHandle.own("file", 42);
      final ComponentResourceHandle h2 = ComponentResourceHandle.own("file", 42);
      assertEquals(h1, h2);
      assertEquals(h1.hashCode(), h2.hashCode());
    }

    @Test
    @DisplayName("different index means not equal")
    void differentIndexNotEqual() {
      final ComponentResourceHandle h1 = ComponentResourceHandle.own("file", 1);
      final ComponentResourceHandle h2 = ComponentResourceHandle.own("file", 2);
      assertNotEquals(h1, h2);
    }

    @Test
    @DisplayName("different ownership means not equal")
    void differentOwnershipNotEqual() {
      final ComponentResourceHandle h1 = ComponentResourceHandle.own("file", 1);
      final ComponentResourceHandle h2 = ComponentResourceHandle.borrow("file", 1);
      assertNotEquals(h1, h2);
    }

    @Test
    @DisplayName("different type means not equal")
    void differentTypeNotEqual() {
      final ComponentResourceHandle h1 = ComponentResourceHandle.own("file", 1);
      final ComponentResourceHandle h2 = ComponentResourceHandle.own("stream", 1);
      assertNotEquals(h1, h2);
    }

    @Test
    @DisplayName("not equal to null")
    void notEqualToNull() {
      final ComponentResourceHandle h1 = ComponentResourceHandle.own("file", 1);
      assertNotEquals(null, h1);
    }

    @Test
    @DisplayName("equal to self")
    void equalToSelf() {
      final ComponentResourceHandle h1 = ComponentResourceHandle.own("file", 1);
      assertEquals(h1, h1);
    }
  }

  @Nested
  @DisplayName("toString")
  class ToString {

    @Test
    @DisplayName("own handle toString")
    void ownToString() {
      final ComponentResourceHandle handle = ComponentResourceHandle.own("file", 42);
      assertEquals("own<file>(42)", handle.toString());
    }

    @Test
    @DisplayName("borrow handle toString")
    void borrowToString() {
      final ComponentResourceHandle handle = ComponentResourceHandle.borrow("stream", 7);
      assertEquals("borrow<stream>(7)", handle.toString());
    }
  }
}
