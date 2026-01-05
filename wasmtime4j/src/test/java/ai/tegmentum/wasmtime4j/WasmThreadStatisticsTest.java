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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasmThreadStatistics class.
 *
 * <p>WasmThreadStatistics provides performance statistics and monitoring information for
 * WebAssembly thread execution. This test verifies the class structure and method signatures.
 */
@DisplayName("WasmThreadStatistics Class Tests")
class WasmThreadStatisticsTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(WasmThreadStatistics.class.getModifiers()),
          "WasmThreadStatistics should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasmThreadStatistics.class.getModifiers()),
          "WasmThreadStatistics should be public");
    }

    @Test
    @DisplayName("should not be an interface")
    void shouldNotBeAnInterface() {
      assertTrue(
          !WasmThreadStatistics.class.isInterface(),
          "WasmThreadStatistics should not be an interface");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have 6-argument constructor")
    void shouldHave6ArgumentConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          WasmThreadStatistics.class.getConstructor(
              long.class, long.class, long.class, long.class, long.class, long.class);
      assertNotNull(constructor, "6-argument constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  // ========================================================================
  // Getter Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Getter Methods Tests")
  class GetterMethodsTests {

    @Test
    @DisplayName("should have getFunctionsExecuted method")
    void shouldHaveGetFunctionsExecutedMethod() throws NoSuchMethodException {
      final Method method = WasmThreadStatistics.class.getMethod("getFunctionsExecuted");
      assertNotNull(method, "getFunctionsExecuted method should exist");
      assertEquals(long.class, method.getReturnType(), "getFunctionsExecuted should return long");
    }

    @Test
    @DisplayName("should have getTotalExecutionTime method")
    void shouldHaveGetTotalExecutionTimeMethod() throws NoSuchMethodException {
      final Method method = WasmThreadStatistics.class.getMethod("getTotalExecutionTime");
      assertNotNull(method, "getTotalExecutionTime method should exist");
      assertEquals(long.class, method.getReturnType(), "getTotalExecutionTime should return long");
    }

    @Test
    @DisplayName("should have getTotalExecutionTimeMillis method")
    void shouldHaveGetTotalExecutionTimeMillisMethod() throws NoSuchMethodException {
      final Method method = WasmThreadStatistics.class.getMethod("getTotalExecutionTimeMillis");
      assertNotNull(method, "getTotalExecutionTimeMillis method should exist");
      assertEquals(
          long.class, method.getReturnType(), "getTotalExecutionTimeMillis should return long");
    }

    @Test
    @DisplayName("should have getAverageExecutionTime method")
    void shouldHaveGetAverageExecutionTimeMethod() throws NoSuchMethodException {
      final Method method = WasmThreadStatistics.class.getMethod("getAverageExecutionTime");
      assertNotNull(method, "getAverageExecutionTime method should exist");
      assertEquals(
          long.class, method.getReturnType(), "getAverageExecutionTime should return long");
    }

    @Test
    @DisplayName("should have getAtomicOperations method")
    void shouldHaveGetAtomicOperationsMethod() throws NoSuchMethodException {
      final Method method = WasmThreadStatistics.class.getMethod("getAtomicOperations");
      assertNotNull(method, "getAtomicOperations method should exist");
      assertEquals(long.class, method.getReturnType(), "getAtomicOperations should return long");
    }

    @Test
    @DisplayName("should have getMemoryAccesses method")
    void shouldHaveGetMemoryAccessesMethod() throws NoSuchMethodException {
      final Method method = WasmThreadStatistics.class.getMethod("getMemoryAccesses");
      assertNotNull(method, "getMemoryAccesses method should exist");
      assertEquals(long.class, method.getReturnType(), "getMemoryAccesses should return long");
    }

    @Test
    @DisplayName("should have getWaitNotifyOperations method")
    void shouldHaveGetWaitNotifyOperationsMethod() throws NoSuchMethodException {
      final Method method = WasmThreadStatistics.class.getMethod("getWaitNotifyOperations");
      assertNotNull(method, "getWaitNotifyOperations method should exist");
      assertEquals(
          long.class, method.getReturnType(), "getWaitNotifyOperations should return long");
    }

    @Test
    @DisplayName("should have getPeakMemoryUsage method")
    void shouldHaveGetPeakMemoryUsageMethod() throws NoSuchMethodException {
      final Method method = WasmThreadStatistics.class.getMethod("getPeakMemoryUsage");
      assertNotNull(method, "getPeakMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "getPeakMemoryUsage should return long");
    }

    @Test
    @DisplayName("should have getPeakMemoryUsageKB method")
    void shouldHaveGetPeakMemoryUsageKBMethod() throws NoSuchMethodException {
      final Method method = WasmThreadStatistics.class.getMethod("getPeakMemoryUsageKB");
      assertNotNull(method, "getPeakMemoryUsageKB method should exist");
      assertEquals(long.class, method.getReturnType(), "getPeakMemoryUsageKB should return long");
    }

    @Test
    @DisplayName("should have getPeakMemoryUsageMB method")
    void shouldHaveGetPeakMemoryUsageMBMethod() throws NoSuchMethodException {
      final Method method = WasmThreadStatistics.class.getMethod("getPeakMemoryUsageMB");
      assertNotNull(method, "getPeakMemoryUsageMB method should exist");
      assertEquals(long.class, method.getReturnType(), "getPeakMemoryUsageMB should return long");
    }
  }

  // ========================================================================
  // Computed Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Computed Methods Tests")
  class ComputedMethodsTests {

    @Test
    @DisplayName("should have getOperationsPerSecond method")
    void shouldHaveGetOperationsPerSecondMethod() throws NoSuchMethodException {
      final Method method = WasmThreadStatistics.class.getMethod("getOperationsPerSecond");
      assertNotNull(method, "getOperationsPerSecond method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getOperationsPerSecond should return double");
    }

    @Test
    @DisplayName("should have getMemoryAccessRate method")
    void shouldHaveGetMemoryAccessRateMethod() throws NoSuchMethodException {
      final Method method = WasmThreadStatistics.class.getMethod("getMemoryAccessRate");
      assertNotNull(method, "getMemoryAccessRate method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getMemoryAccessRate should return double");
    }
  }

  // ========================================================================
  // Static Factory Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Methods Tests")
  class StaticFactoryMethodsTests {

    @Test
    @DisplayName("should have static empty method")
    void shouldHaveEmptyMethod() throws NoSuchMethodException {
      final Method method = WasmThreadStatistics.class.getMethod("empty");
      assertNotNull(method, "empty method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "empty should be static");
      assertEquals(
          WasmThreadStatistics.class,
          method.getReturnType(),
          "empty should return WasmThreadStatistics");
    }
  }

  // ========================================================================
  // Instance Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Instance Methods Tests")
  class InstanceMethodsTests {

    @Test
    @DisplayName("should have combine method")
    void shouldHaveCombineMethod() throws NoSuchMethodException {
      final Method method =
          WasmThreadStatistics.class.getMethod("combine", WasmThreadStatistics.class);
      assertNotNull(method, "combine method should exist");
      assertEquals(
          WasmThreadStatistics.class,
          method.getReturnType(),
          "combine should return WasmThreadStatistics");
    }
  }

  // ========================================================================
  // Object Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Object Methods Tests")
  class ObjectMethodsTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = WasmThreadStatistics.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(boolean.class, method.getReturnType(), "equals should return boolean");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = WasmThreadStatistics.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
      assertEquals(int.class, method.getReturnType(), "hashCode should return int");
    }

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = WasmThreadStatistics.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "toString should return String");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          Set.of(
              "getFunctionsExecuted",
              "getTotalExecutionTime",
              "getTotalExecutionTimeMillis",
              "getAverageExecutionTime",
              "getAtomicOperations",
              "getMemoryAccesses",
              "getWaitNotifyOperations",
              "getPeakMemoryUsage",
              "getPeakMemoryUsageKB",
              "getPeakMemoryUsageMB",
              "getOperationsPerSecond",
              "getMemoryAccessRate",
              "empty",
              "combine",
              "equals",
              "hashCode",
              "toString");

      Set<String> actualMethods =
          Arrays.stream(WasmThreadStatistics.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected),
            "WasmThreadStatistics should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have at least 17 declared methods")
    void shouldHaveAtLeast17DeclaredMethods() {
      assertTrue(
          WasmThreadStatistics.class.getDeclaredMethods().length >= 17,
          "WasmThreadStatistics should have at least 17 methods (found "
              + WasmThreadStatistics.class.getDeclaredMethods().length
              + ")");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend Object directly")
    void shouldExtendObjectDirectly() {
      assertEquals(
          Object.class,
          WasmThreadStatistics.class.getSuperclass(),
          "WasmThreadStatistics should extend Object");
    }

    @Test
    @DisplayName("should not implement any interface")
    void shouldNotImplementAnyInterface() {
      assertEquals(
          0,
          WasmThreadStatistics.class.getInterfaces().length,
          "WasmThreadStatistics should not implement any interface");
    }
  }
}
