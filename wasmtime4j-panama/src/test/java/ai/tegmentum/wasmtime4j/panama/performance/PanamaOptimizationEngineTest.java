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

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaOptimizationEngine} class.
 *
 * <p>PanamaOptimizationEngine provides advanced Panama optimization with method handle caching,
 * memory pooling, and zero-copy operations.
 */
@DisplayName("PanamaOptimizationEngine Tests")
class PanamaOptimizationEngineTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaOptimizationEngine.class.getModifiers()),
          "PanamaOptimizationEngine should be public");
      assertTrue(
          Modifier.isFinal(PanamaOptimizationEngine.class.getModifiers()),
          "PanamaOptimizationEngine should be final");
    }

    @Test
    @DisplayName("should have ZeroCopyOperation interface")
    void shouldHaveZeroCopyOperationInterface() {
      assertNotNull(
          PanamaOptimizationEngine.ZeroCopyOperation.class,
          "ZeroCopyOperation interface should exist");
      assertTrue(
          PanamaOptimizationEngine.ZeroCopyOperation.class.isInterface(),
          "ZeroCopyOperation should be an interface");
    }

    @Test
    @DisplayName("should have BulkOperation interface")
    void shouldHaveBulkOperationInterface() {
      assertNotNull(
          PanamaOptimizationEngine.BulkOperation.class, "BulkOperation interface should exist");
      assertTrue(
          PanamaOptimizationEngine.BulkOperation.class.isInterface(),
          "BulkOperation should be an interface");
    }
  }

  @Nested
  @DisplayName("Singleton Method Tests")
  class SingletonMethodTests {

    @Test
    @DisplayName("should have getInstance method")
    void shouldHaveGetInstanceMethod() throws NoSuchMethodException {
      final Method method = PanamaOptimizationEngine.class.getMethod("getInstance");
      assertNotNull(method, "getInstance method should exist");
      assertEquals(
          PanamaOptimizationEngine.class,
          method.getReturnType(),
          "Should return PanamaOptimizationEngine");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Method Handle Method Tests")
  class MethodHandleMethodTests {

    @Test
    @DisplayName("should have getOptimizedMethodHandle method")
    void shouldHaveGetOptimizedMethodHandleMethod() throws NoSuchMethodException {
      final Method method =
          PanamaOptimizationEngine.class.getMethod(
              "getOptimizedMethodHandle",
              SymbolLookup.class,
              String.class,
              FunctionDescriptor.class);
      assertNotNull(method, "getOptimizedMethodHandle method should exist");
      assertEquals(MethodHandle.class, method.getReturnType(), "Should return MethodHandle");
    }

    @Test
    @DisplayName("should have executeOptimized method")
    void shouldHaveExecuteOptimizedMethod() throws NoSuchMethodException {
      final Method method =
          PanamaOptimizationEngine.class.getMethod(
              "executeOptimized", MethodHandle.class, Object[].class);
      assertNotNull(method, "executeOptimized method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object");
    }
  }

  @Nested
  @DisplayName("Allocation Method Tests")
  class AllocationMethodTests {

    @Test
    @DisplayName("should have allocateOptimized(long) method")
    void shouldHaveAllocateOptimizedLongMethod() throws NoSuchMethodException {
      final Method method =
          PanamaOptimizationEngine.class.getMethod("allocateOptimized", long.class);
      assertNotNull(method, "allocateOptimized(long) method should exist");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
    }

    @Test
    @DisplayName("should have allocateOptimized(MemoryLayout) method")
    void shouldHaveAllocateOptimizedLayoutMethod() throws NoSuchMethodException {
      final Method method =
          PanamaOptimizationEngine.class.getMethod("allocateOptimized", MemoryLayout.class);
      assertNotNull(method, "allocateOptimized(MemoryLayout) method should exist");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
    }
  }

  @Nested
  @DisplayName("Zero Copy Method Tests")
  class ZeroCopyMethodTests {

    @Test
    @DisplayName("should have executeZeroCopy method")
    void shouldHaveExecuteZeroCopyMethod() throws NoSuchMethodException {
      final Method method =
          PanamaOptimizationEngine.class.getMethod(
              "executeZeroCopy", long.class, PanamaOptimizationEngine.ZeroCopyOperation.class);
      assertNotNull(method, "executeZeroCopy method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return generic type");
    }

    @Test
    @DisplayName("should have executeBulk method")
    void shouldHaveExecuteBulkMethod() throws NoSuchMethodException {
      final Method method =
          PanamaOptimizationEngine.class.getMethod(
              "executeBulk", int.class, long.class, PanamaOptimizationEngine.BulkOperation.class);
      assertNotNull(method, "executeBulk method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return generic type");
    }
  }

  @Nested
  @DisplayName("Memory Operation Method Tests")
  class MemoryOperationMethodTests {

    @Test
    @DisplayName("should have optimizedCopy method")
    void shouldHaveOptimizedCopyMethod() throws NoSuchMethodException {
      final Method method =
          PanamaOptimizationEngine.class.getMethod(
              "optimizedCopy", MemorySegment.class, MemorySegment.class, long.class);
      assertNotNull(method, "optimizedCopy method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have optimizeLayout method")
    void shouldHaveOptimizeLayoutMethod() throws NoSuchMethodException {
      final Method method = PanamaOptimizationEngine.class.getMethod("optimizeLayout", Class.class);
      assertNotNull(method, "optimizeLayout method should exist");
    }
  }

  @Nested
  @DisplayName("Statistics Method Tests")
  class StatisticsMethodTests {

    @Test
    @DisplayName("should have getOptimizationStats method")
    void shouldHaveGetOptimizationStatsMethod() throws NoSuchMethodException {
      final Method method = PanamaOptimizationEngine.class.getMethod("getOptimizationStats");
      assertNotNull(method, "getOptimizationStats method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Control Method Tests")
  class ControlMethodTests {

    @Test
    @DisplayName("should have setOptimizationEnabled static method")
    void shouldHaveSetOptimizationEnabledMethod() throws NoSuchMethodException {
      final Method method =
          PanamaOptimizationEngine.class.getMethod("setOptimizationEnabled", boolean.class);
      assertNotNull(method, "setOptimizationEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have isOptimizationEnabled static method")
    void shouldHaveIsOptimizationEnabledMethod() throws NoSuchMethodException {
      final Method method = PanamaOptimizationEngine.class.getMethod("isOptimizationEnabled");
      assertNotNull(method, "isOptimizationEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have reset method")
    void shouldHaveResetMethod() throws NoSuchMethodException {
      final Method method = PanamaOptimizationEngine.class.getMethod("reset");
      assertNotNull(method, "reset method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have shutdown method")
    void shouldHaveShutdownMethod() throws NoSuchMethodException {
      final Method method = PanamaOptimizationEngine.class.getMethod("shutdown");
      assertNotNull(method, "shutdown method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
