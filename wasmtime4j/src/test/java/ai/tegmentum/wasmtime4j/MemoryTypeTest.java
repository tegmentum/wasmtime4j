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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.type.MemoryType;
import ai.tegmentum.wasmtime4j.type.WasmType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MemoryType} interface.
 *
 * <p>MemoryType represents the type information of a WebAssembly memory including page counts.
 */
@DisplayName("MemoryType Tests")
class MemoryTypeTest {

  /** Test implementation of MemoryType for testing purposes. */
  private static class TestMemoryType implements MemoryType {
    private final long minimum;
    private final Long maximum;
    private final boolean is64Bit;
    private final boolean shared;

    TestMemoryType(
        final long minimum, final Long maximum, final boolean is64Bit, final boolean shared) {
      this.minimum = minimum;
      this.maximum = maximum;
      this.is64Bit = is64Bit;
      this.shared = shared;
    }

    @Override
    public long getMinimum() {
      return minimum;
    }

    @Override
    public Optional<Long> getMaximum() {
      return Optional.ofNullable(maximum);
    }

    @Override
    public boolean is64Bit() {
      return is64Bit;
    }

    @Override
    public boolean isShared() {
      return shared;
    }
  }

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(Modifier.isPublic(MemoryType.class.getModifiers()), "MemoryType should be public");
      assertTrue(MemoryType.class.isInterface(), "MemoryType should be an interface");
    }

    @Test
    @DisplayName("should extend WasmType")
    void shouldExtendWasmType() {
      assertTrue(
          WasmType.class.isAssignableFrom(MemoryType.class), "MemoryType should extend WasmType");
    }

    @Test
    @DisplayName("should have getMinimum method")
    void shouldHaveGetMinimumMethod() throws NoSuchMethodException {
      final Method method = MemoryType.class.getMethod("getMinimum");
      assertNotNull(method, "getMinimum method should exist");
      assertEquals(long.class, method.getReturnType(), "getMinimum should return long");
    }

    @Test
    @DisplayName("should have getMaximum method")
    void shouldHaveGetMaximumMethod() throws NoSuchMethodException {
      final Method method = MemoryType.class.getMethod("getMaximum");
      assertNotNull(method, "getMaximum method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getMaximum should return Optional");
    }

    @Test
    @DisplayName("should have is64Bit method")
    void shouldHaveIs64BitMethod() throws NoSuchMethodException {
      final Method method = MemoryType.class.getMethod("is64Bit");
      assertNotNull(method, "is64Bit method should exist");
      assertEquals(boolean.class, method.getReturnType(), "is64Bit should return boolean");
    }

    @Test
    @DisplayName("should have isShared method")
    void shouldHaveIsSharedMethod() throws NoSuchMethodException {
      final Method method = MemoryType.class.getMethod("isShared");
      assertNotNull(method, "isShared method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isShared should return boolean");
    }
  }

  @Nested
  @DisplayName("Default getKind Method Tests")
  class DefaultGetKindMethodTests {

    @Test
    @DisplayName("getKind should return MEMORY")
    void getKindShouldReturnMemory() {
      final MemoryType memoryType = new TestMemoryType(1, 10L, false, false);

      assertEquals(WasmTypeKind.MEMORY, memoryType.getKind(), "getKind should return MEMORY");
    }

    @Test
    @DisplayName("getKind should be a default method")
    void getKindShouldBeDefaultMethod() throws NoSuchMethodException {
      final Method method = MemoryType.class.getMethod("getKind");
      assertTrue(method.isDefault(), "getKind should be a default method");
    }
  }

  @Nested
  @DisplayName("Memory Configuration Tests")
  class MemoryConfigurationTests {

    @Test
    @DisplayName("should handle minimum pages correctly")
    void shouldHandleMinimumPagesCorrectly() {
      final MemoryType memoryType = new TestMemoryType(16, 256L, false, false);

      assertEquals(16L, memoryType.getMinimum(), "Minimum should be 16 pages");
    }

    @Test
    @DisplayName("should handle maximum pages correctly")
    void shouldHandleMaximumPagesCorrectly() {
      final MemoryType memoryType = new TestMemoryType(1, 100L, false, false);

      assertTrue(memoryType.getMaximum().isPresent(), "Maximum should be present");
      assertEquals(100L, memoryType.getMaximum().get(), "Maximum should be 100 pages");
    }

    @Test
    @DisplayName("should handle no maximum (unlimited)")
    void shouldHandleNoMaximum() {
      final MemoryType memoryType = new TestMemoryType(1, null, false, false);

      assertFalse(memoryType.getMaximum().isPresent(), "Maximum should not be present");
    }

    @Test
    @DisplayName("should handle 32-bit memory")
    void shouldHandle32BitMemory() {
      final MemoryType memoryType = new TestMemoryType(1, 10L, false, false);

      assertFalse(memoryType.is64Bit(), "Memory should be 32-bit");
    }

    @Test
    @DisplayName("should handle 64-bit memory")
    void shouldHandle64BitMemory() {
      final MemoryType memoryType = new TestMemoryType(1, 10L, true, false);

      assertTrue(memoryType.is64Bit(), "Memory should be 64-bit");
    }

    @Test
    @DisplayName("should handle private memory")
    void shouldHandlePrivateMemory() {
      final MemoryType memoryType = new TestMemoryType(1, 10L, false, false);

      assertFalse(memoryType.isShared(), "Memory should not be shared");
    }

    @Test
    @DisplayName("should handle shared memory")
    void shouldHandleSharedMemory() {
      final MemoryType memoryType = new TestMemoryType(1, 10L, false, true);

      assertTrue(memoryType.isShared(), "Memory should be shared");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle zero minimum pages")
    void shouldHandleZeroMinimumPages() {
      final MemoryType memoryType = new TestMemoryType(0, 10L, false, false);

      assertEquals(0L, memoryType.getMinimum(), "Minimum should be 0 pages");
    }

    @Test
    @DisplayName("should handle large page counts")
    void shouldHandleLargePageCounts() {
      final long largeMin = 65536L;
      final long largeMax = 2147483648L;
      final MemoryType memoryType = new TestMemoryType(largeMin, largeMax, true, false);

      assertEquals(largeMin, memoryType.getMinimum(), "Minimum should handle large values");
      assertEquals(largeMax, memoryType.getMaximum().get(), "Maximum should handle large values");
    }

    @Test
    @DisplayName("should handle minimum equals maximum")
    void shouldHandleMinimumEqualsMaximum() {
      final MemoryType memoryType = new TestMemoryType(10, 10L, false, false);

      assertEquals(10L, memoryType.getMinimum(), "Minimum should be 10");
      assertEquals(10L, memoryType.getMaximum().get(), "Maximum should be 10");
    }

    @Test
    @DisplayName("should handle combined 64-bit and shared memory")
    void shouldHandleCombined64BitAndSharedMemory() {
      final MemoryType memoryType = new TestMemoryType(1, 1000L, true, true);

      assertTrue(memoryType.is64Bit(), "Memory should be 64-bit");
      assertTrue(memoryType.isShared(), "Memory should be shared");
      assertEquals(WasmTypeKind.MEMORY, memoryType.getKind(), "Kind should be MEMORY");
    }
  }

  @Nested
  @DisplayName("WasmType Integration Tests")
  class WasmTypeIntegrationTests {

    @Test
    @DisplayName("MemoryType should implement WasmType")
    void memoryTypeShouldImplementWasmType() {
      final MemoryType memoryType = new TestMemoryType(1, 10L, false, false);

      assertTrue(memoryType instanceof WasmType, "MemoryType should be instance of WasmType");
    }

    @Test
    @DisplayName("getKind should return MEMORY for all configurations")
    void getKindShouldReturnMemoryForAllConfigurations() {
      final MemoryType basic = new TestMemoryType(1, null, false, false);
      final MemoryType withMax = new TestMemoryType(1, 100L, false, false);
      final MemoryType mem64 = new TestMemoryType(1, null, true, false);
      final MemoryType shared = new TestMemoryType(1, null, false, true);

      assertEquals(WasmTypeKind.MEMORY, basic.getKind(), "Basic memory should be MEMORY kind");
      assertEquals(WasmTypeKind.MEMORY, withMax.getKind(), "Memory with max should be MEMORY kind");
      assertEquals(WasmTypeKind.MEMORY, mem64.getKind(), "64-bit memory should be MEMORY kind");
      assertEquals(WasmTypeKind.MEMORY, shared.getKind(), "Shared memory should be MEMORY kind");
    }
  }
}
