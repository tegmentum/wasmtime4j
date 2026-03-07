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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentExportItem}.
 *
 * @since 1.1.0
 */
@DisplayName("ComponentExportItem Tests")
class ComponentExportItemTest {

  /**
   * Minimal test implementation of {@link ComponentExportIndex} used to verify {@link
   * ComponentExportItem} behavior without requiring native resources.
   */
  private static final class StubExportIndex implements ComponentExportIndex {

    private final long nativeHandle;
    private boolean valid;

    StubExportIndex(final long nativeHandle) {
      this.nativeHandle = nativeHandle;
      this.valid = true;
    }

    @Override
    public long getNativeHandle() {
      return nativeHandle;
    }

    @Override
    public boolean isValid() {
      return valid;
    }

    @Override
    public void close() {
      valid = false;
    }
  }

  @Nested
  @DisplayName("Constructor tests")
  class ConstructorTests {

    @Test
    @DisplayName("should construct with valid kind and export index")
    void shouldConstructWithValidArguments() {
      final StubExportIndex index = new StubExportIndex(1L);

      final ComponentExportItem item =
          new ComponentExportItem(ComponentItemKind.COMPONENT_FUNC, index);

      assertNotNull(item, "Constructor should produce a non-null ComponentExportItem");
      assertEquals(
          ComponentItemKind.COMPONENT_FUNC,
          item.getKind(),
          "getKind() should return the kind passed to the constructor");
      assertSame(
          index,
          item.getExportIndex(),
          "getExportIndex() should return the exact same index instance passed to the constructor");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when kind is null")
    void shouldThrowWhenKindIsNull() {
      final StubExportIndex index = new StubExportIndex(1L);

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new ComponentExportItem(null, index),
              "Constructor should throw IllegalArgumentException when kind is null");

      assertEquals(
          "kind cannot be null",
          exception.getMessage(),
          "Exception message should indicate kind cannot be null, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when exportIndex is null")
    void shouldThrowWhenExportIndexIsNull() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new ComponentExportItem(ComponentItemKind.MODULE, null),
              "Constructor should throw IllegalArgumentException when exportIndex is null");

      assertEquals(
          "exportIndex cannot be null",
          exception.getMessage(),
          "Exception message should indicate exportIndex cannot be null, got: "
              + exception.getMessage());
    }

    @Test
    @DisplayName("should construct successfully with each ComponentItemKind value")
    void shouldConstructWithEachKind() {
      for (final ComponentItemKind kind : ComponentItemKind.values()) {
        final StubExportIndex index = new StubExportIndex(kind.ordinal());
        final ComponentExportItem item = new ComponentExportItem(kind, index);

        assertEquals(
            kind,
            item.getKind(),
            "getKind() should return " + kind + " for item constructed with that kind");
      }
    }
  }

  @Nested
  @DisplayName("getKind() tests")
  class GetKindTests {

    @Test
    @DisplayName("should return COMPONENT_FUNC for function export")
    void shouldReturnComponentFunc() {
      final ComponentExportItem item =
          new ComponentExportItem(ComponentItemKind.COMPONENT_FUNC, new StubExportIndex(1L));

      assertEquals(
          ComponentItemKind.COMPONENT_FUNC,
          item.getKind(),
          "getKind() should return COMPONENT_FUNC");
    }

    @Test
    @DisplayName("should return RESOURCE for resource export")
    void shouldReturnResource() {
      final ComponentExportItem item =
          new ComponentExportItem(ComponentItemKind.RESOURCE, new StubExportIndex(1L));

      assertEquals(ComponentItemKind.RESOURCE, item.getKind(), "getKind() should return RESOURCE");
    }
  }

  @Nested
  @DisplayName("getExportIndex() tests")
  class GetExportIndexTests {

    @Test
    @DisplayName("should return the exact export index instance passed to constructor")
    void shouldReturnSameInstance() {
      final StubExportIndex index = new StubExportIndex(42L);
      final ComponentExportItem item = new ComponentExportItem(ComponentItemKind.CORE_FUNC, index);

      assertSame(
          index,
          item.getExportIndex(),
          "getExportIndex() should return the exact same object reference");
    }

    @Test
    @DisplayName("should return an export index with the correct native handle")
    void shouldReturnIndexWithCorrectHandle() {
      final long expectedHandle = 999L;
      final StubExportIndex index = new StubExportIndex(expectedHandle);
      final ComponentExportItem item = new ComponentExportItem(ComponentItemKind.MODULE, index);

      assertEquals(
          expectedHandle,
          item.getExportIndex().getNativeHandle(),
          "The export index's native handle should be "
              + expectedHandle
              + " but got: "
              + item.getExportIndex().getNativeHandle());
    }
  }

  @Nested
  @DisplayName("kindFromCode() tests")
  class KindFromCodeTests {

    @Test
    @DisplayName("should return COMPONENT_FUNC for code 0")
    void shouldReturnComponentFuncForCode0() {
      assertEquals(
          ComponentItemKind.COMPONENT_FUNC,
          ComponentExportItem.kindFromCode(0),
          "kindFromCode(0) should return COMPONENT_FUNC");
    }

    @Test
    @DisplayName("should return CORE_FUNC for code 1")
    void shouldReturnCoreFuncForCode1() {
      assertEquals(
          ComponentItemKind.CORE_FUNC,
          ComponentExportItem.kindFromCode(1),
          "kindFromCode(1) should return CORE_FUNC");
    }

    @Test
    @DisplayName("should return MODULE for code 2")
    void shouldReturnModuleForCode2() {
      assertEquals(
          ComponentItemKind.MODULE,
          ComponentExportItem.kindFromCode(2),
          "kindFromCode(2) should return MODULE");
    }

    @Test
    @DisplayName("should return COMPONENT for code 3")
    void shouldReturnComponentForCode3() {
      assertEquals(
          ComponentItemKind.COMPONENT,
          ComponentExportItem.kindFromCode(3),
          "kindFromCode(3) should return COMPONENT");
    }

    @Test
    @DisplayName("should return COMPONENT_INSTANCE for code 4")
    void shouldReturnComponentInstanceForCode4() {
      assertEquals(
          ComponentItemKind.COMPONENT_INSTANCE,
          ComponentExportItem.kindFromCode(4),
          "kindFromCode(4) should return COMPONENT_INSTANCE");
    }

    @Test
    @DisplayName("should return TYPE for code 5")
    void shouldReturnTypeForCode5() {
      assertEquals(
          ComponentItemKind.TYPE,
          ComponentExportItem.kindFromCode(5),
          "kindFromCode(5) should return TYPE");
    }

    @Test
    @DisplayName("should return RESOURCE for code 6")
    void shouldReturnResourceForCode6() {
      assertEquals(
          ComponentItemKind.RESOURCE,
          ComponentExportItem.kindFromCode(6),
          "kindFromCode(6) should return RESOURCE");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for negative code")
    void shouldThrowForNegativeCode() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> ComponentExportItem.kindFromCode(-1),
              "kindFromCode(-1) should throw IllegalArgumentException");

      assertTrue(
          exception.getMessage().contains("-1"),
          "Exception message should contain the invalid code '-1', got: " + exception.getMessage());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for code 7 (out of range)")
    void shouldThrowForCode7() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> ComponentExportItem.kindFromCode(7),
              "kindFromCode(7) should throw IllegalArgumentException");

      assertTrue(
          exception.getMessage().contains("7"),
          "Exception message should contain the invalid code '7', got: " + exception.getMessage());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for large invalid code")
    void shouldThrowForLargeCode() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentExportItem.kindFromCode(100),
          "kindFromCode(100) should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("close() tests")
  class CloseTests {

    @Test
    @DisplayName("should close the underlying export index")
    void shouldCloseExportIndex() {
      final StubExportIndex index = new StubExportIndex(1L);
      assertTrue(index.isValid(), "Precondition: index should be valid before close");

      final ComponentExportItem item =
          new ComponentExportItem(ComponentItemKind.COMPONENT_FUNC, index);
      item.close();

      assertFalse(
          index.isValid(),
          "close() on ComponentExportItem should close the underlying export index");
    }

    @Test
    @DisplayName("should be safe to call close multiple times")
    void shouldBeSafeToCloseMultipleTimes() {
      final StubExportIndex index = new StubExportIndex(1L);
      final ComponentExportItem item = new ComponentExportItem(ComponentItemKind.CORE_FUNC, index);

      item.close();
      item.close();
      item.close();

      assertFalse(
          index.isValid(), "Export index should remain invalid after multiple close() calls");
    }

    @Test
    @DisplayName("should work with try-with-resources")
    void shouldWorkWithTryWithResources() {
      final StubExportIndex index = new StubExportIndex(77L);

      try (ComponentExportItem item = new ComponentExportItem(ComponentItemKind.TYPE, index)) {
        assertNotNull(item.getExportIndex(), "Export item should have a valid export index");
        assertTrue(index.isValid(), "Export index should be valid inside try-with-resources block");
      }

      assertFalse(
          index.isValid(), "Export index should be closed after try-with-resources block exits");
    }
  }

  @Nested
  @DisplayName("toString() tests")
  class ToStringTests {

    @Test
    @DisplayName("should contain the kind name for COMPONENT_FUNC")
    void shouldContainKindNameForComponentFunc() {
      final ComponentExportItem item =
          new ComponentExportItem(ComponentItemKind.COMPONENT_FUNC, new StubExportIndex(1L));

      final String result = item.toString();

      assertEquals(
          "ComponentExportItem{kind=COMPONENT_FUNC}",
          result,
          "toString() should be 'ComponentExportItem{kind=COMPONENT_FUNC}', got: " + result);
    }

    @Test
    @DisplayName("should contain the kind name for RESOURCE")
    void shouldContainKindNameForResource() {
      final ComponentExportItem item =
          new ComponentExportItem(ComponentItemKind.RESOURCE, new StubExportIndex(1L));

      final String result = item.toString();

      assertEquals(
          "ComponentExportItem{kind=RESOURCE}",
          result,
          "toString() should be 'ComponentExportItem{kind=RESOURCE}', got: " + result);
    }

    @Test
    @DisplayName("should produce correct toString for each ComponentItemKind")
    void shouldProduceCorrectToStringForEachKind() {
      for (final ComponentItemKind kind : ComponentItemKind.values()) {
        final ComponentExportItem item = new ComponentExportItem(kind, new StubExportIndex(1L));
        final String expected = "ComponentExportItem{kind=" + kind.name() + "}";

        assertEquals(
            expected,
            item.toString(),
            "toString() for kind "
                + kind
                + " should be '"
                + expected
                + "', got: "
                + item.toString());
      }
    }
  }
}
