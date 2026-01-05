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

package ai.tegmentum.wasmtime4j.panama.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AdvancedArenaManager} class.
 *
 * <p>AdvancedArenaManager provides sophisticated memory segment optimization for Panama FFI.
 */
@DisplayName("AdvancedArenaManager Tests")
class AdvancedArenaManagerTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(AdvancedArenaManager.class.getModifiers()),
          "AdvancedArenaManager should be public");
      assertTrue(
          Modifier.isFinal(AdvancedArenaManager.class.getModifiers()),
          "AdvancedArenaManager should be final");
    }

    @Test
    @DisplayName("should have MemoryPressure enum")
    void shouldHaveMemoryPressureEnum() {
      assertNotNull(AdvancedArenaManager.MemoryPressure.class, "MemoryPressure enum should exist");
      assertTrue(
          AdvancedArenaManager.MemoryPressure.class.isEnum(), "MemoryPressure should be an enum");
    }

    @Test
    @DisplayName("should have AllocationStrategy enum")
    void shouldHaveAllocationStrategyEnum() {
      assertNotNull(
          AdvancedArenaManager.AllocationStrategy.class, "AllocationStrategy enum should exist");
      assertTrue(
          AdvancedArenaManager.AllocationStrategy.class.isEnum(),
          "AllocationStrategy should be an enum");
    }
  }

  @Nested
  @DisplayName("Singleton Method Tests")
  class SingletonMethodTests {

    @Test
    @DisplayName("should have getInstance method")
    void shouldHaveGetInstanceMethod() throws NoSuchMethodException {
      final Method method = AdvancedArenaManager.class.getMethod("getInstance");
      assertNotNull(method, "getInstance method should exist");
      assertEquals(
          AdvancedArenaManager.class, method.getReturnType(), "Should return AdvancedArenaManager");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Allocation Method Tests")
  class AllocationMethodTests {

    @Test
    @DisplayName("should have allocateOptimized(long) method")
    void shouldHaveAllocateOptimizedLongMethod() throws NoSuchMethodException {
      final Method method = AdvancedArenaManager.class.getMethod("allocateOptimized", long.class);
      assertNotNull(method, "allocateOptimized(long) method should exist");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
    }

    @Test
    @DisplayName("should have allocateOptimized(MemoryLayout) method")
    void shouldHaveAllocateOptimizedLayoutMethod() throws NoSuchMethodException {
      final Method method =
          AdvancedArenaManager.class.getMethod("allocateOptimized", MemoryLayout.class);
      assertNotNull(method, "allocateOptimized(MemoryLayout) method should exist");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
    }

    @Test
    @DisplayName("should have executeZeroCopy method")
    void shouldHaveExecuteZeroCopyMethod() throws NoSuchMethodException {
      final Method method =
          AdvancedArenaManager.class.getMethod(
              "executeZeroCopy", MemorySegment.class, long.class, Function.class);
      assertNotNull(method, "executeZeroCopy method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return generic type");
    }

    @Test
    @DisplayName("should have allocateBulkOptimized method")
    void shouldHaveAllocateBulkOptimizedMethod() throws NoSuchMethodException {
      final Method method =
          AdvancedArenaManager.class.getMethod(
              "allocateBulkOptimized", long.class, MemoryLayout.class);
      assertNotNull(method, "allocateBulkOptimized method should exist");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
    }

    @Test
    @DisplayName("should have releaseOptimized method")
    void shouldHaveReleaseOptimizedMethod() throws NoSuchMethodException {
      final Method method =
          AdvancedArenaManager.class.getMethod("releaseOptimized", MemorySegment.class);
      assertNotNull(method, "releaseOptimized method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Statistics Method Tests")
  class StatisticsMethodTests {

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = AdvancedArenaManager.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getCurrentMemoryUsage method")
    void shouldHaveGetCurrentMemoryUsageMethod() throws NoSuchMethodException {
      final Method method = AdvancedArenaManager.class.getMethod("getCurrentMemoryUsage");
      assertNotNull(method, "getCurrentMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getPoolHitRate method")
    void shouldHaveGetPoolHitRateMethod() throws NoSuchMethodException {
      final Method method = AdvancedArenaManager.class.getMethod("getPoolHitRate");
      assertNotNull(method, "getPoolHitRate method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }
  }

  @Nested
  @DisplayName("Control Method Tests")
  class ControlMethodTests {

    @Test
    @DisplayName("should have setEnabled static method")
    void shouldHaveSetEnabledMethod() throws NoSuchMethodException {
      final Method method = AdvancedArenaManager.class.getMethod("setEnabled", boolean.class);
      assertNotNull(method, "setEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have isEnabled static method")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      final Method method = AdvancedArenaManager.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have shutdown method")
    void shouldHaveShutdownMethod() throws NoSuchMethodException {
      final Method method = AdvancedArenaManager.class.getMethod("shutdown");
      assertNotNull(method, "shutdown method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("MemoryPressure should have expected values")
    void memoryPressureShouldHaveExpectedValues() {
      final AdvancedArenaManager.MemoryPressure[] values =
          AdvancedArenaManager.MemoryPressure.values();
      assertTrue(values.length >= 4, "Should have at least 4 memory pressure levels");
      assertNotNull(
          AdvancedArenaManager.MemoryPressure.valueOf("LOW"), "LOW should be a valid value");
      assertNotNull(
          AdvancedArenaManager.MemoryPressure.valueOf("MEDIUM"), "MEDIUM should be a valid value");
      assertNotNull(
          AdvancedArenaManager.MemoryPressure.valueOf("HIGH"), "HIGH should be a valid value");
      assertNotNull(
          AdvancedArenaManager.MemoryPressure.valueOf("CRITICAL"),
          "CRITICAL should be a valid value");
    }

    @Test
    @DisplayName("AllocationStrategy should have expected values")
    void allocationStrategyShouldHaveExpectedValues() {
      final AdvancedArenaManager.AllocationStrategy[] values =
          AdvancedArenaManager.AllocationStrategy.values();
      assertTrue(values.length >= 4, "Should have at least 4 allocation strategies");
      assertNotNull(
          AdvancedArenaManager.AllocationStrategy.valueOf("POOL_REUSE"),
          "POOL_REUSE should be a valid value");
      assertNotNull(
          AdvancedArenaManager.AllocationStrategy.valueOf("NEW_OPTIMIZED"),
          "NEW_OPTIMIZED should be a valid value");
      assertNotNull(
          AdvancedArenaManager.AllocationStrategy.valueOf("DIRECT"),
          "DIRECT should be a valid value");
      assertNotNull(
          AdvancedArenaManager.AllocationStrategy.valueOf("ZERO_COPY"),
          "ZERO_COPY should be a valid value");
    }
  }
}
