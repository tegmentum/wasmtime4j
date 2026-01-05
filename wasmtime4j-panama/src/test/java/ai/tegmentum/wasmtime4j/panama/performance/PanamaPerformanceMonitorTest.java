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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaPerformanceMonitor} class.
 *
 * <p>PanamaPerformanceMonitor provides Panama-specific performance monitoring and profiling
 * infrastructure for WebAssembly operations.
 */
@DisplayName("PanamaPerformanceMonitor Tests")
class PanamaPerformanceMonitorTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaPerformanceMonitor.class.getModifiers()),
          "PanamaPerformanceMonitor should be public");
      assertTrue(
          Modifier.isFinal(PanamaPerformanceMonitor.class.getModifiers()),
          "PanamaPerformanceMonitor should be final");
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() {
      Constructor<?>[] constructors = PanamaPerformanceMonitor.class.getDeclaredConstructors();
      assertTrue(constructors.length > 0, "Should have at least one constructor");
      for (Constructor<?> constructor : constructors) {
        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");
      }
    }

    @Test
    @DisplayName("should have SIMPLE_PANAMA_OPERATION_TARGET_NS constant")
    void shouldHaveSimplePanamaOperationTargetNsConstant() throws NoSuchFieldException {
      final Field field =
          PanamaPerformanceMonitor.class.getField("SIMPLE_PANAMA_OPERATION_TARGET_NS");
      assertNotNull(field, "SIMPLE_PANAMA_OPERATION_TARGET_NS should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(long.class, field.getType(), "Should be long");
    }

    @Test
    @DisplayName("should have MonitoredOperation interface")
    void shouldHaveMonitoredOperationInterface() {
      assertNotNull(
          PanamaPerformanceMonitor.MonitoredOperation.class,
          "MonitoredOperation interface should exist");
      assertTrue(
          PanamaPerformanceMonitor.MonitoredOperation.class.isInterface(),
          "MonitoredOperation should be an interface");
    }
  }

  @Nested
  @DisplayName("Enable/Disable Method Tests")
  class EnableDisableMethodTests {

    @Test
    @DisplayName("should have setEnabled static method")
    void shouldHaveSetEnabledMethod() throws NoSuchMethodException {
      final Method method = PanamaPerformanceMonitor.class.getMethod("setEnabled", boolean.class);
      assertNotNull(method, "setEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have isEnabled static method")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      final Method method = PanamaPerformanceMonitor.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Operation Monitoring Method Tests")
  class OperationMonitoringMethodTests {

    @Test
    @DisplayName("should have startOperation with category and details")
    void shouldHaveStartOperationWithDetails() throws NoSuchMethodException {
      final Method method =
          PanamaPerformanceMonitor.class.getMethod("startOperation", String.class, String.class);
      assertNotNull(method, "startOperation method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have startOperation with category only")
    void shouldHaveStartOperationCategoryOnly() throws NoSuchMethodException {
      final Method method =
          PanamaPerformanceMonitor.class.getMethod("startOperation", String.class);
      assertNotNull(method, "startOperation method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have endOperation method")
    void shouldHaveEndOperationMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPerformanceMonitor.class.getMethod("endOperation", String.class, long.class);
      assertNotNull(method, "endOperation method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have monitor method")
    void shouldHaveMonitorMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPerformanceMonitor.class.getMethod(
              "monitor", String.class, PanamaPerformanceMonitor.MonitoredOperation.class);
      assertNotNull(method, "monitor method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return generic type");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Arena Tracking Method Tests")
  class ArenaTrackingMethodTests {

    @Test
    @DisplayName("should have recordArenaAllocation method")
    void shouldHaveRecordArenaAllocationMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPerformanceMonitor.class.getMethod(
              "recordArenaAllocation", Arena.class, long.class);
      assertNotNull(method, "recordArenaAllocation method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have recordArenaDeallocation method")
    void shouldHaveRecordArenaDeallocationMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPerformanceMonitor.class.getMethod("recordArenaDeallocation", Arena.class);
      assertNotNull(method, "recordArenaDeallocation method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Memory Segment Tracking Method Tests")
  class MemorySegmentTrackingMethodTests {

    @Test
    @DisplayName("should have recordMemorySegmentAllocation method")
    void shouldHaveRecordMemorySegmentAllocationMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPerformanceMonitor.class.getMethod(
              "recordMemorySegmentAllocation", Arena.class, MemorySegment.class);
      assertNotNull(method, "recordMemorySegmentAllocation method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Method Handle Tracking Method Tests")
  class MethodHandleTrackingMethodTests {

    @Test
    @DisplayName("should have recordMethodHandleCall method")
    void shouldHaveRecordMethodHandleCallMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPerformanceMonitor.class.getMethod("recordMethodHandleCall", String.class);
      assertNotNull(method, "recordMethodHandleCall method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Zero Copy Tracking Method Tests")
  class ZeroCopyTrackingMethodTests {

    @Test
    @DisplayName("should have recordZeroCopyOperation method")
    void shouldHaveRecordZeroCopyOperationMethod() throws NoSuchMethodException {
      final Method method = PanamaPerformanceMonitor.class.getMethod("recordZeroCopyOperation");
      assertNotNull(method, "recordZeroCopyOperation method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("FFI Overhead Method Tests")
  class FfiOverheadMethodTests {

    @Test
    @DisplayName("should have getAverageFfiOverhead method")
    void shouldHaveGetAverageFfiOverheadMethod() throws NoSuchMethodException {
      final Method method = PanamaPerformanceMonitor.class.getMethod("getAverageFfiOverhead");
      assertNotNull(method, "getAverageFfiOverhead method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have meetsPerformanceTarget method")
    void shouldHaveMeetsPerformanceTargetMethod() throws NoSuchMethodException {
      final Method method = PanamaPerformanceMonitor.class.getMethod("meetsPerformanceTarget");
      assertNotNull(method, "meetsPerformanceTarget method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Statistics Method Tests")
  class StatisticsMethodTests {

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = PanamaPerformanceMonitor.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have getPanamaMetrics method")
    void shouldHaveGetPanamaMetricsMethod() throws NoSuchMethodException {
      final Method method = PanamaPerformanceMonitor.class.getMethod("getPanamaMetrics");
      assertNotNull(method, "getPanamaMetrics method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have getActiveArenaStats method")
    void shouldHaveGetActiveArenaStatsMethod() throws NoSuchMethodException {
      final Method method = PanamaPerformanceMonitor.class.getMethod("getActiveArenaStats");
      assertNotNull(method, "getActiveArenaStats method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have getPerformanceIssues method")
    void shouldHaveGetPerformanceIssuesMethod() throws NoSuchMethodException {
      final Method method = PanamaPerformanceMonitor.class.getMethod("getPerformanceIssues");
      assertNotNull(method, "getPerformanceIssues method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Reset Method Tests")
  class ResetMethodTests {

    @Test
    @DisplayName("should have reset method")
    void shouldHaveResetMethod() throws NoSuchMethodException {
      final Method method = PanamaPerformanceMonitor.class.getMethod("reset");
      assertNotNull(method, "reset method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }
}
