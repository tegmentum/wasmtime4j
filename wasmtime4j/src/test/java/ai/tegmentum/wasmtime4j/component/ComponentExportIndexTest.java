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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentExportIndex}.
 *
 * <p>Since {@code ComponentExportIndex} is an interface, these tests verify the interface contract
 * using a minimal test implementation. Real implementations are tested via integration tests in
 * wasmtime4j-jni and wasmtime4j-panama modules.
 *
 * @since 1.0.0
 */
@DisplayName("ComponentExportIndex Tests")
class ComponentExportIndexTest {

  /**
   * Minimal test implementation of {@link ComponentExportIndex} that tracks validity state and
   * stores a native handle value.
   */
  private static final class TestComponentExportIndex implements ComponentExportIndex {

    private final long nativeHandle;
    private boolean valid;

    TestComponentExportIndex(final long nativeHandle) {
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
  @DisplayName("getNativeHandle() tests")
  class GetNativeHandleTests {

    @Test
    @DisplayName("should return the native handle value provided at construction")
    void shouldReturnNativeHandle() {
      final long expectedHandle = 42L;
      final ComponentExportIndex index = new TestComponentExportIndex(expectedHandle);

      final long actualHandle = index.getNativeHandle();

      assertEquals(
          expectedHandle,
          actualHandle,
          "getNativeHandle() should return the handle provided at construction, "
              + "expected: "
              + expectedHandle
              + " but got: "
              + actualHandle);
    }

    @Test
    @DisplayName("should return zero handle when constructed with zero")
    void shouldReturnZeroHandle() {
      final ComponentExportIndex index = new TestComponentExportIndex(0L);

      assertEquals(
          0L, index.getNativeHandle(), "getNativeHandle() should return 0 when constructed with 0");
    }

    @Test
    @DisplayName("should return negative handle when constructed with negative value")
    void shouldReturnNegativeHandle() {
      final long negativeHandle = -1L;
      final ComponentExportIndex index = new TestComponentExportIndex(negativeHandle);

      assertEquals(
          negativeHandle,
          index.getNativeHandle(),
          "getNativeHandle() should return negative value when constructed with one");
    }

    @Test
    @DisplayName("should return max long handle when constructed with Long.MAX_VALUE")
    void shouldReturnMaxLongHandle() {
      final ComponentExportIndex index = new TestComponentExportIndex(Long.MAX_VALUE);

      assertEquals(
          Long.MAX_VALUE,
          index.getNativeHandle(),
          "getNativeHandle() should handle Long.MAX_VALUE correctly");
    }
  }

  @Nested
  @DisplayName("isValid() tests")
  class IsValidTests {

    @Test
    @DisplayName("should be valid immediately after construction")
    void shouldBeValidAfterConstruction() {
      final ComponentExportIndex index = new TestComponentExportIndex(1L);

      assertTrue(index.isValid(), "isValid() should return true immediately after construction");
    }

    @Test
    @DisplayName("should become invalid after close is called")
    void shouldBecomeInvalidAfterClose() {
      final ComponentExportIndex index = new TestComponentExportIndex(1L);

      index.close();

      assertFalse(index.isValid(), "isValid() should return false after close() has been called");
    }
  }

  @Nested
  @DisplayName("close() tests")
  class CloseTests {

    @Test
    @DisplayName("should invalidate the index when called")
    void shouldInvalidateOnClose() {
      final ComponentExportIndex index = new TestComponentExportIndex(99L);
      assertTrue(index.isValid(), "Precondition: index should be valid before close");

      index.close();

      assertFalse(
          index.isValid(), "close() should invalidate the index so isValid() returns false");
    }

    @Test
    @DisplayName("should be safe to call close multiple times")
    void shouldBeSafeToCloseMultipleTimes() {
      final ComponentExportIndex index = new TestComponentExportIndex(99L);

      index.close();
      index.close();
      index.close();

      assertFalse(index.isValid(), "isValid() should remain false after multiple close() calls");
    }
  }

  @Nested
  @DisplayName("AutoCloseable contract tests")
  class AutoCloseableTests {

    @Test
    @DisplayName("should work with try-with-resources")
    void shouldWorkWithTryWithResources() {
      final TestComponentExportIndex index = new TestComponentExportIndex(123L);

      try (ComponentExportIndex autoCloseable = index) {
        assertTrue(
            autoCloseable.isValid(), "Index should be valid inside try-with-resources block");
        assertEquals(
            123L,
            autoCloseable.getNativeHandle(),
            "getNativeHandle() should return correct value inside try-with-resources");
      }

      assertFalse(index.isValid(), "Index should be invalid after try-with-resources block exits");
    }
  }
}
